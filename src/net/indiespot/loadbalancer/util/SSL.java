package net.indiespot.loadbalancer.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSL
{
	private static final boolean VERBOSE = false;

	public static void filterServerCiphers(SSLServerSocket socket)
	{
		String[] cipherArray = socket.getSupportedCipherSuites();
		{
			Set<String> allowCiphers = new HashSet<>();
			allowCiphers.add("SSL_RSA_WITH_RC4_128_SHA"); // MSIE
			allowCiphers.add("TLS_RSA_WITH_AES_128_CBC_SHA"); // Googlebot
			boolean allowSHA1 = true;

			cipherArray = filterCiphers(cipherArray, allowSHA1, allowCiphers);
		}
		socket.setEnabledCipherSuites(cipherArray);
	}

	private static String[] filterClientCiphers(String[] ciphers)
	{
		Set<String> allowCiphers = null;
		boolean allowSHA1 = true;
		return filterCiphers(ciphers, allowSHA1, allowCiphers);
	}

	public static void filterClientCiphers(SSLContext ctx)
	{
		String[] cipherArray = ctx.getSupportedSSLParameters().getCipherSuites();
		cipherArray = filterClientCiphers(cipherArray);
		ctx.getSupportedSSLParameters().setCipherSuites(cipherArray);
	}

	public static SSLSocket filterClientCiphers(SSLSocket socket)
	{
		String[] cipherArray = socket.getEnabledCipherSuites();
		cipherArray = filterClientCiphers(cipherArray);
		socket.setEnabledCipherSuites(cipherArray);
		return socket;
	}

	public static SSLSocket disableRenegotiation(SSLSocket socket)
	{
		socket.addHandshakeCompletedListener(new HandshakeCompletedListener()
		{
			@Override
			public void handshakeCompleted(HandshakeCompletedEvent event)
			{
				String[] ciphers = new String[] {};
				event.getSocket().setEnabledCipherSuites(ciphers);
			}
		});
		return socket;
	}

	public static SSLSocketFactory filterSocketFactory(final SSLSocketFactory factory)
	{
		return new SSLSocketFactory()
		{
			@Override
			public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
			{
				return filterClientCiphers((SSLSocket) factory.createSocket(address, port, localAddress, localPort));
			}

			@Override
			public Socket createSocket(String address, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException
			{
				return filterClientCiphers((SSLSocket) factory.createSocket(address, port, localAddress, localPort));
			}

			@Override
			public Socket createSocket(InetAddress host, int port) throws IOException
			{
				return filterClientCiphers((SSLSocket) factory.createSocket(host, port));
			}

			@Override
			public Socket createSocket(String host, int port) throws IOException, UnknownHostException
			{
				return filterClientCiphers((SSLSocket) factory.createSocket(host, port));
			}

			@Override
			public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
			{
				return filterClientCiphers((SSLSocket) factory.createSocket(s, host, port, autoClose));
			}

			@Override
			public String[] getSupportedCipherSuites()
			{
				return filterClientCiphers(factory.getSupportedCipherSuites());
			}

			@Override
			public String[] getDefaultCipherSuites()
			{
				return filterClientCiphers(factory.getDefaultCipherSuites());
			}
		};
	}

	private static String[] filterCiphers(String[] cipherArray, boolean allowSHA1, Set<String> allowCiphers)
	{
		Set<String> unsafeAlgorithms = new HashSet<>();

		for(String p : new String[] { "NULL", "MD5", "EMPTY", "RC4", "DES", "3DES", "DES40" })
		{
			unsafeAlgorithms.add(p);
		}

		// must support "SHA" (SHA1) for java clients :[
		if(!allowSHA1)
		{
			unsafeAlgorithms.add("SHA");
		}

		List<String> ciphers = new ArrayList<>();
		for(String cipher : cipherArray)
		{
			if(VERBOSE)
			{
				System.out.println("cipher: " + cipher);
			}

			Set<String> parts = new HashSet<String>();
			for(String p : Text.split(cipher, '_'))
			{
				parts.add(p);
			}
			parts.remove("TLS");
			parts.remove("SSL");
			parts.remove("WITH");

			// found denied tokens?
			{
				Set<String> copy = new HashSet<>(parts);
				copy.retainAll(unsafeAlgorithms);
				if(!copy.isEmpty())
				{
					continue;
				}
			}

			// must have DH key exchange
			if(true && //
					!parts.contains("ECDHE") && //
					!parts.contains("DHE") && //
					!parts.contains("DH"))
			{
				continue;
			}

			// must have either RSA or DDS (MSIE10)
			if(true && //
					!parts.contains("RSA") && //
					!parts.contains("DSS"))
			{
				continue;
			}

			if(VERBOSE)
			{
				System.out.println("\t\taccepted: " + cipher);
			}
			ciphers.add(cipher);
		}

		if(allowCiphers != null)
		{
			ciphers.addAll(allowCiphers);
		}

		if(VERBOSE)
		{
			for(String c : ciphers)
			{
				System.out.println("accepted cipher: " + c);
			}
		}

		return ciphers.toArray(new String[ciphers.size()]);
	}
}
