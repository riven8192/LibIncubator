package net.indiespot.loadbalancer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import net.indiespot.loadbalancer.util.BinaryLineReader;
import net.indiespot.loadbalancer.util.HighLevel;
import net.indiespot.loadbalancer.util.IniFile;
import net.indiespot.loadbalancer.util.LiveFile;
import net.indiespot.loadbalancer.util.SSL;
import net.indiespot.loadbalancer.util.Streams;
import net.indiespot.loadbalancer.util.Text;

public class SslOffloading {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage:");
			System.out.println("\t[conf-dir]   -- used to lookup listen.ini and targets.ini");
			System.exit(1);
			return;
		}

		String conf_dir = args[0];

		// -----

		targets_config = new LiveFile(new File(conf_dir, "targets.ini"), 5_000L) {
			@Override
			public void onNewData(byte[] data) {
				try {
					for (Entry<String, Map<String, String>> entry : IniFile.parse(data).entrySet()) {
						String targetName = entry.getKey();
						Map<String, String> props = entry.getValue();

						String httpHostname = props.get("pool");
						InetSocketAddress addr = InetSocketAddress.createUnresolved(//
						   props.get("host"), Integer.parseInt(props.get("port")));

						Target target = null;
						synchronized (mutex) {
							Set<Target> targets = httphost2targets.get(httpHostname);
							if (targets == null)
								httphost2targets.put(httpHostname, targets = new HashSet<>());

							for (Target t : targets) {
								if (t.name.equals(targetName) && t.addr.equals(addr)) {
									target = t;
								}
							}

							if (target == null)
								target = new Target(targetName, addr);
							target.weight = Integer.parseInt(props.get("weight"));
							targets.add(target);
						}

						System.out.println("\tConfiguring target " + targetName + ": " + addr.getHostString() + "@" + addr.getPort() + " " + target.weight + "x for pool: " + httpHostname);
					}
				} catch (Exception exc) {
					System.out.println("ERROR while parsing targets file:");
					exc.printStackTrace();
				}
			}
		};

		Thread t1 = new Thread(new CleanupSessionTargetPreferences());
		t1.setName("cleanup-session-target-preferences");
		t1.setDaemon(true);
		t1.start();

		Thread t2 = new Thread(new CleanupUnavailableTargetFlags());
		t2.setName("cleanup-unavailable-target-flags");
		t2.setDaemon(true);
		t2.start();

		// -----

		byte[] listenConfig = Streams.readFile(new File(conf_dir, "listen.ini"));
		for (Entry<String, Map<String, String>> entry : IniFile.parse(listenConfig).entrySet()) {
			Map<String, String> props = entry.getValue();

			final String section = entry.getKey();
			final String host = props.get("host");
			final int port = Integer.parseInt(props.get("port"));
			final int timeout = Integer.parseInt(props.get("timeout")) * 1000;
			final String keystore = props.get("keystore");
			final String jksPassword = props.get("jks_password");
			final String crtPassword = props.get("crt_password");

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						listen(section, keystore, jksPassword, crtPassword, host, port, timeout);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	public static void listen(String section, String keystorePath, String jksPassword, String crtPassword, String bindAddr, int port, final int timeout) throws Exception {
		File keystoreFile = new File(keystorePath);
		char[] jksPasswd = jksPassword.toCharArray();
		char[] crtPasswd = crtPassword.toCharArray();
		SSLContext ctx = createSSLContext(keystoreFile, jksPasswd, crtPasswd);

		int backlog = 100;
		ServerSocket ss = ctx.getServerSocketFactory().createServerSocket(port, backlog, InetAddress.getByName(bindAddr));
		System.out.println("[" + section + "] SSL listening: " + ss);

		if (ss instanceof SSLServerSocket) {
			SSL.filterServerCiphers((SSLServerSocket) ss);
		}

		while (true) {
			final Socket client = SSL.disableRenegotiation((SSLSocket) ss.accept());

			Streams.pool(new Runnable() {
				@Override
				public void run() {
					try {
						tunnelTraffic(client, timeout);
					} catch (IOException | InterruptedException exc) {
						exc.printStackTrace();
					} finally {
						Streams.close(client);
					}
				}
			});
		}
	}

	private static final SessionIdentifier identifier = new CookieSessionIdentifier();
	private static final TargetFinder target_finder = new LeastConnectionsTargetFinder();
	private static final int socket_timeout_during_handshake = 5_000;
	private static final int socket_timeout_during_first_httpheader = 10_000;
	private static final int unavailable_target_expiration = 60 * 1000; // 1min
	private static final int prefered_target_expiration = 8 * 3600 * 1000; // 8u
	private static final Object mutex = new Object();
	private static LiveFile targets_config;
	private static final Map<String, Set<Target>> httphost2targets = new HashMap<>();
	private static final Map<String, Identity> token2identity = new HashMap<>();

	private static class CleanupSessionTargetPreferences implements Runnable {
		@Override
		public void run() {
			while (true) {
				HighLevel.sleep(prefered_target_expiration / 3);

				final long now = System.currentTimeMillis();

				synchronized (mutex) {
					try {
						Set<String> forgetTokens = new HashSet<>();
						for (Entry<String, Identity> entry : token2identity.entrySet())
							if (now - entry.getValue().activeAt > prefered_target_expiration)
								forgetTokens.add(entry.getKey());

						for (String token : forgetTokens) {
							token2identity.remove(token);
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		}
	}

	private static class CleanupUnavailableTargetFlags implements Runnable {
		@Override
		public void run() {
			while (true) {
				HighLevel.sleep(unavailable_target_expiration / 3);

				final long now = System.currentTimeMillis();

				synchronized (mutex) {
					try {
						for (Set<Target> targets : httphost2targets.values()) {
							for (Target target : targets) {
								if (target.unavailableAt == 0L)
									continue;
								if (now - target.unavailableAt < unavailable_target_expiration)
									continue;
								target.unavailableAt = 0L;

								System.out.println("[" + Text.datetime() + "] Target blocking expired: " + target.name);
							}
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		}
	}

	private static Target findAvailableTarget(Identity identity, List<String> requestHeader) {
		String hostname = getHostname(requestHeader);
		if (hostname == null)
			throw new NoSuchElementException("http host not found");

		Target preference = identity.hostname2preference.get(hostname);
		if ( // no preference yet
		preference == null || //
		   // preference for unavailable target
		   preference.unavailableAt != 0L || //
		   // preference for undesired target
		   preference.weight == 0) {
			// find new target
			Set<Target> targets = httphost2targets.get(hostname);
			if (targets == null)
				throw new NoSuchElementException("no targets for host: " + hostname);
			preference = target_finder.findTarget(targets, identity);
			if (preference == null)
				throw new NoSuchElementException("no targets available for host: " + hostname);

			identity.hostname2preference.put(hostname, preference);
		}

		identity.activeAt = System.currentTimeMillis();

		return preference;
	}

	private static final String getHostname(List<String> requestHeader) {
		String hostname = null;
		for (String line : requestHeader)
			if (line.startsWith("Host: "))
				hostname = line.substring(6);
		if (hostname != null)
			hostname = Text.beforeIfAny(hostname, ':');
		return hostname;
	}

	private static void tunnelTraffic(final Socket client, int timeout) throws IOException, InterruptedException {
		client.setSoTimeout(socket_timeout_during_handshake);
		client.setTcpNoDelay(true);
		final InputStream clientIn = new BufferedInputStream(client.getInputStream());
		final OutputStream clientOut = client.getOutputStream();

		long handshakeTook;
		try {
			long t0 = System.currentTimeMillis();
			((SSLSocket) client).startHandshake();
			long t1 = System.currentTimeMillis();
			handshakeTook = t1 - t0;
		} catch (SSLException exc) {
			System.out.println("[" + Text.datetime() + "] FINE Dropping failed ssl connection");
			return;
		}

		client.setSoTimeout(socket_timeout_during_first_httpheader);

		List<String> requestHeader = readHeader(client, clientIn);
		if (requestHeader == null) {
			System.out.println("[" + Text.datetime() + "] FINE Dropping pro-active connection");
			return;
		}

		client.setSoTimeout(timeout);

		String initialIdentityToken = identifier.identify(client, requestHeader);
		String currentIdentityToken = (initialIdentityToken == null) ? UUID.randomUUID().toString() : initialIdentityToken;

		// find a target
		Socket connect = null;
		Identity _identity = null;
		final Identity identity;
		Target preference = null;
		try {
			targets_config.data();

			while (connect == null) {
				synchronized (mutex) {
					_identity = token2identity.get(currentIdentityToken);
					if (_identity == null)
						token2identity.put(currentIdentityToken, _identity = new Identity(currentIdentityToken));
					preference = findAvailableTarget(_identity, requestHeader);
				}

				Target target = preference;
				try {
					connect = new Socket(target.addr.getHostString(), target.addr.getPort());
				} catch (IOException exc) {
					synchronized (mutex) {
						boolean wasUnavailable = (target.unavailableAt != 0L);
						target.unavailableAt = System.currentTimeMillis();
						if (!wasUnavailable)
							System.out.println("[" + Text.datetime() + "] [" + _identity.token + "] WARN Target unavailable: " + target.addr);
					}
					connect = null;
				}
			}
			identity = _identity;
		} catch (NoSuchElementException exc) {
			byte[] content = Text.ascii("<h1>No services available</h1>Could not find a service to handle your request.");

			StringBuilder responseHeader = new StringBuilder();
			responseHeader.append("HTTP/1.0 500 ServiceUnavailable\r\n");
			responseHeader.append("Content-Type: text/html\r\n");
			responseHeader.append("Content-Length: " + content.length + "\r\n");
			responseHeader.append("\r\n");
			clientOut.write(Text.ascii(responseHeader.toString()));
			clientOut.write(content);

			System.out.println("[" + Text.datetime() + "] [" + (_identity == null ? "" : _identity.token) + "] SEVERE All services unavailable");
			return;
		}

		// configure the connection to the target
		final Target target = preference;
		synchronized (mutex) {
			target.connections++;
		}
		final long connectedAt = System.currentTimeMillis();
		final AtomicLong clientSentBytes = new AtomicLong();
		final AtomicLong clientRecvBytes = new AtomicLong();
		try {
			final Socket targetSocket = connect;
			targetSocket.setSoTimeout(timeout);
			targetSocket.setTcpNoDelay(true);
			final InputStream targetIn = targetSocket.getInputStream();
			final OutputStream targetOut = targetSocket.getOutputStream();
			targetOut.write(composeHeader(requestHeader));

			final long requestSent = System.currentTimeMillis();

			AtomicBoolean latch = Streams.pool(new Runnable() {
				@Override
				public void run() {
					final long[] accum = new long[1];

					try {
						Streams.copy(clientIn, targetOut, false, accum);
					} catch (IOException exc) {
						System.out.println("[" + Text.datetime() + "] [" + identity.token + "] ERRR " + client.getInetAddress().getHostAddress() + " => " + target.addr.getHostString() + " (type=" + exc.getClass().getName() + ", msg=" + exc.getMessage() + ")");
					} finally {
						clientSentBytes.set(accum[0]);
					}
				}
			});

			long[] accum = new long[1];
			try {
				List<String> responseHeader = readHeader(targetSocket, targetIn);
				long responseReceived = System.currentTimeMillis();
				System.out.println("[" + Text.datetime() + "] [" + identity.token + "] CONN " + client.getInetAddress().getHostAddress() + " => " + target.addr.getHostString() + " (conn=" + target.connections + ", handshake=" + handshakeTook + "ms, latency=" + (responseReceived - requestSent) + "ms)");

				if (responseHeader != null) {
					identifier.persist(identity, initialIdentityToken, client, responseHeader);
					clientOut.write(composeHeader(responseHeader));
					Streams.copy(targetIn, clientOut, false, accum);
				}
			} catch (IOException exc) {
				System.out.println("[" + Text.datetime() + "] [" + identity.token + "] ERRR " + client.getInetAddress().getHostAddress() + " => " + target.addr.getHostString() + " (type=" + exc.getClass().getName() + ", msg=" + exc.getMessage() + ")");
			} finally {
				clientRecvBytes.set(accum[0]);
			}

			// wait for both streams to end
			while (!latch.get()) {
				HighLevel.sleep(250);
			}
			HighLevel.sleep(1000);
		} finally {
			long now = System.currentTimeMillis();
			long connectionTook = now - connectedAt;
			synchronized (mutex) {
				target.connections--;
				identity.activeAt = now;
			}
			System.out.println("[" + Text.datetime() + "] [" + identity.token + "] DROP " + client.getInetAddress().getHostAddress() + " => " + target.addr.getHostString() + " (conn=" + target.connections + ", recv=" + ((clientRecvBytes.longValue() + 1023) / 1024) + "K, sent=" + ((clientSentBytes.longValue() + 1023) / 1024) + "K, took=" + (connectionTook / 1000) + "s)");
		}
	}

	private static List<String> readHeader(Socket client, InputStream in) throws IOException {
		List<String> headerLines = new ArrayList<>();
		while (true) {
			String line = BinaryLineReader.readLineAsString(in);
			if (line == null || line.isEmpty())
				break;
			headerLines.add(line);
		}
		if (headerLines.isEmpty())
			return null;
		return headerLines;
	}

	private static byte[] composeHeader(List<String> header) {
		StringBuilder out = new StringBuilder();
		for (String line : header)
			out.append(line).append("\r\n");
		return Text.ascii(out.append("\r\n").toString());
	}

	private static SSLContext createSSLContext(File keystoreFile, char[] storepass, char[] certpass) throws SSLException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, NoSuchProviderException {
		KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(new FileInputStream(keystoreFile), storepass);

		KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
		keyManager.init(keystore, certpass);

		X509TrustManager x509TrustManager = findX509TrustManager(keystore);
		KeyManager[] keyManagers = keyManager.getKeyManagers();

		SSLContext context = SSLContext.getInstance("TLS", "SunJSSE");
		context.init(keyManagers, new TrustManager[] { x509TrustManager }, new SecureRandom());

		return context;
	}

	private static X509TrustManager findX509TrustManager(KeyStore keystore) throws NoSuchProviderException, NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
		tmf.init(keystore);

		for (TrustManager tm : tmf.getTrustManagers())
			if (tm instanceof X509TrustManager)
				return (X509TrustManager) tm;

		throw new IllegalStateException("X509TrustManager not found");
	}
}
