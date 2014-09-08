package net.indiespot.httpwaiter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Waiter {
	static class Request {
		final Connection conn;
		final String firstLine;
		final Map<String, String> headerLines;

		public Request(Connection conn, String firstLine, Map<String, String> headerLines) {
			this.conn = conn;
			this.firstLine = firstLine;
			this.headerLines = headerLines;
		}
	}

	static class Response {
		final Connection conn;
		final String firstLine;
		final Map<String, String> headerLines;
		final byte[] content;

		public Response(Connection conn, String firstLine, Map<String, String> headerLines, byte[] content) {
			this.conn = conn;
			this.firstLine = firstLine;
			this.headerLines = headerLines;
			this.content = content;
		}
	}

	//

	private static interface ChannelStorage {
		public int addMessage(String channel, byte[] message);

		public byte[] getMessage(String channel, int idx);
	}

	private static class MemoryChannelStorage implements ChannelStorage {
		private final Map<String, List<byte[]>> channel2messages = new HashMap<>();

		@Override
		public int addMessage(String channel, byte[] message) {
			List<byte[]> messages = channel2messages.get(channel);
			if (messages == null)
				channel2messages.put(channel, messages = new ArrayList<>());
			messages.add(message);
			return messages.size() - 1;
		}

		@Override
		public byte[] getMessage(String channel, int idx) {
			List<byte[]> messages = channel2messages.get(channel);
			if (messages == null)
				return null;
			if (idx < 0 || idx >= messages.size())
				return null;
			return messages.get(idx);
		}
	}

	//

	private static interface ChannelListeners {
		public void register(String channel, Connection connection);

		public void unregister(String channel, Connection connection);

		public List<Connection> getListeners(String channel);
	}

	private static class BasicChannelListeners implements ChannelListeners {
		private final Map<String, List<Connection>> channel2listeners = new HashMap<>();

		@Override
		public void register(String channel, Connection connection) {
			List<Connection> listeners = channel2listeners.get(channel);
			if (listeners == null)
				channel2listeners.put(channel, listeners = new ArrayList<>());
			listeners.add(connection);
		}

		@Override
		public void unregister(String channel, Connection connection) {
			List<Connection> listeners = channel2listeners.get(channel);
			if (listeners == null)
				throw new IllegalStateException();
			if (!listeners.remove(connection))
				throw new IllegalStateException();
		}

		@Override
		public List<Connection> getListeners(String channel) {
			return new ArrayList<>(channel2listeners.get(channel));
		}
	}

	//

	private static interface GroupManager {
		public void add(String group, String member);

		public void remove(String group, String member);

		public Set<String> list(String group);
	}

	private static class BasicGroupManager implements GroupManager {

		private final Map<String, Set<String>> group2members = new HashMap<>();

		@Override
		public void add(String group, String member) {
			Set<String> members = group2members.get(group);
			if (members == null)
				group2members.put(group, members = new HashSet<>());
			members.add(member);
		}

		@Override
		public void remove(String group, String member) {
			Set<String> members = group2members.get(group);
			if (members == null)
				throw new IllegalStateException();
			if (!members.remove(member))
				throw new IllegalStateException();
		}

		@Override
		public Set<String> list(String group) {
			return group2members.get(group);
		}
	}

	//

	static final ArrayBlockingQueue<Runnable> runnable_queue = new ArrayBlockingQueue<>(1000);
	static final ArrayBlockingQueue<Request> request_queue = new ArrayBlockingQueue<>(1000);
	static final ArrayBlockingQueue<Response> response_queue = new ArrayBlockingQueue<>(1000);

	static final ChannelStorage channel_storage = new MemoryChannelStorage();
	static final ChannelListeners channel_listeners = new BasicChannelListeners();
	static final GroupManager group_manager = new BasicGroupManager();

	private static void register(Connection conn, String channel) {
		if (conn.channel != null)
			throw new IllegalStateException();
		conn.channel = channel;
		channel_listeners.register(conn.channel, conn);
	}

	private static void unregister(Connection conn) {
		if (conn.channel == null)
			throw new IllegalStateException();
		channel_listeners.unregister(conn.channel, conn);
		conn.channel = null;
	}

	public static void main(String[] args) throws IOException {
		new Thread(new Runnable() {
			@Override
			public void run() {

				while (true) {
					while (true) {
						Runnable task = dequeuePoll(runnable_queue);
						if (task == null)
							break;

						try {
							task.run();
						} catch (Exception exc) {
							exc.printStackTrace();
						}
					}

					Request request = dequeue(request_queue);
					System.out.println("handling request: " + request.firstLine);

					try {
						this.onRequest(request);
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}

			static final int max_bulkmsg_size = 64 * 1024;

			private void sendBadRequest(Request request) {
				String firstLine = "HTTP/1.1 400 Bad Request";
				Map<String, String> headerLines = new HashMap<>();
				headerLines.put("Content-Type", "text/plain");
				byte[] message = utf8("Bad Request");
				enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
			}

			private void sendNotFound(Request request) {
				String firstLine = "HTTP/1.1 404 NotFound";
				Map<String, String> headerLines = new HashMap<>();
				headerLines.put("Content-Type", "text/plain");
				byte[] message = utf8("File Not Found");
				enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
			}

			private void sendOK(Request request) {
				String firstLine = "HTTP/1.1 200 OK";
				Map<String, String> headerLines = new HashMap<>();
				headerLines.put("Content-Type", "text/plain");
				byte[] message = utf8("OK");
				enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
			}

			private void onRequest(Request request) {
				String[] parts = request.firstLine.split(" ");
				String method = parts[0];
				String action = parts[1];
				String version = parts[2];

				if (action.startsWith("/group/")) {
					String[] actionParts = action.split("/");
					if (actionParts.length <= 2) {
						this.sendBadRequest(request);
						return;
					}

					String group = actionParts[2];

					if (method.equals("GET")) {
						Set<String> members = group_manager.list(group);
						if (members == null) {
							this.sendNotFound(request);
							return;
						}

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/plain");

						ByteArrayOutputStream baos = new ByteArrayOutputStream();

						try {
							Iterator<String> it = members.iterator();
							for (int i = 0; i < members.size(); i++) {
								if (i != 0)
									baos.write(utf8("\n"));
								baos.write(utf8(it.next()));
							}
						} catch (IOException e) {
							throw new IllegalStateException(e);
						}
						byte[] message = baos.toByteArray();

						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
						return;
					} else if (method.equals("PUT")) {
						String member = actionParts[3];
						group_manager.add(group, member);
						this.sendOK(request);
						return;
					} else if (method.equals("DELETE")) {
						String member = actionParts[3];
						group_manager.remove(group, member);
						this.sendOK(request);
						return;
					}
				} else if (action.startsWith("/channel/")) {
					String[] actionParts = action.split("/");
					if (actionParts.length <= 2) {
						this.sendBadRequest(request);
						return;
					}

					String channel = actionParts[2];

					if (method.equals("GET")) {
						final int offMsgId = Integer.parseInt(actionParts[3]);

						if (channel_storage.getMessage(channel, offMsgId) != null) {
							String firstLine = "HTTP/1.1 200 OK";
							Map<String, String> headerLines = new HashMap<>();
							headerLines.put("Content-Type", "text/plain");
							ByteArrayOutputStream baos = new ByteArrayOutputStream();

							try {
								for (int i = 0; baos.size() < max_bulkmsg_size; i++) {
									byte[] message = channel_storage.getMessage(channel, offMsgId + i);
									if (message == null)
										break;

									if (i != 0)
										baos.write(utf8("\n"));
									baos.write(message);
								}
							} catch (IOException e) {
								throw new IllegalStateException(e);
							}
							byte[] message = baos.toByteArray();

							enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
						} else {
							register(request.conn, channel);
						}

						return;
					}

					if (method.equals("POST")) {
						if (actionParts.length <= 3) {
							this.sendBadRequest(request);
							return;
						}

						byte[] message = utf8(actionParts[3]);

						int msgId = channel_storage.addMessage(channel, message);

						List<Connection> listeners = channel_listeners.getListeners(channel);
						if (listeners != null) {
							String firstLine = "HTTP/1.1 200 OK";
							Map<String, String> headerLines = new HashMap<>();
							headerLines.put("Content-Type", "text/plain");

							for (Connection listener : listeners) {
								unregister(listener);

								if (listener.isOpen) {
									enqueue(response_queue, new Response(listener, firstLine, headerLines, message));
								}
							}
						}

						{
							String firstLine = "HTTP/1.1 200 OK";
							Map<String, String> headerLines = new HashMap<>();
							headerLines.put("Content-Type", "text/plain");
							message = utf8(String.valueOf(msgId));
							enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
						}
					}
				} else if (method.equals("GET")) {
					if (action.equals("/channel.css")) {
						byte[] message;
						{
							File f = new File("D:/channel.css");
							if (!f.exists())
								f = new File("./channel.css");

							try (InputStream in = new FileInputStream(f)) {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								byte[] tmp = new byte[1024];
								for (int got; (got = in.read(tmp)) != -1;) {
									baos.write(tmp, 0, got);
								}
								message = baos.toByteArray();
							} catch (IOException e) {
								e.printStackTrace();
								return;
							}
						}

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/css");
						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					} else if (action.equals("/channel.js")) {
						byte[] message;
						{
							File f = new File("D:/channel.js");
							if (!f.exists())
								f = new File("./channel.js");

							try (InputStream in = new FileInputStream(f)) {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								byte[] tmp = new byte[1024];
								for (int got; (got = in.read(tmp)) != -1;) {
									baos.write(tmp, 0, got);
								}
								message = baos.toByteArray();
							} catch (IOException e) {
								e.printStackTrace();
								return;
							}
						}

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/javascript");
						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					} else if (action.startsWith("/view/")) {
						String[] actionParts = action.split("/");
						if (actionParts.length <= 2) {
							this.sendBadRequest(request);
							return;
						}
						String user = actionParts[2];

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/html");

						byte[] message;
						{
							File f = new File("D:/channel_view.html");
							if (!f.exists())
								f = new File("./channel_view.html");

							try (InputStream in = new FileInputStream(f)) {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								byte[] tmp = new byte[1024];
								for (int got; (got = in.read(tmp)) != -1;) {
									baos.write(tmp, 0, got);
								}
								message = baos.toByteArray();

								String html = utf8(message);
								html = html.replace("${user}", user);

								message = utf8(html);
							} catch (IOException e) {
								e.printStackTrace();
								return;
							}
						}

						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					} else if (action.startsWith("/post/")) {
						String[] actionParts = action.split("/");
						if (actionParts.length <= 2) {
							this.sendBadRequest(request);
							return;
						}
						String user = actionParts[2];

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/html");

						byte[] message;
						{
							File f = new File("D:/channel_post.html");
							if (!f.exists())
								f = new File("./channel_post.html");

							try (InputStream in = new FileInputStream(f)) {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								byte[] tmp = new byte[1024];
								for (int got; (got = in.read(tmp)) != -1;) {
									baos.write(tmp, 0, got);
								}
								message = baos.toByteArray();

								String html = utf8(message);
								html = html.replace("${user}", user);

								message = utf8(html);
							} catch (IOException e) {
								e.printStackTrace();
								return;
							}
						}

						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					} else {
						this.sendNotFound(request);
						return;
					}
				}
			}
		}).start();

		final ExecutorService pool = Executors.newFixedThreadPool(16);

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					final Response response = dequeue(response_queue);
					if (!response.conn.isOpen) {
						continue;
					}
					if (response.conn.isSending) {
						enqueue(response_queue, response);
						continue;
					}
					response.conn.isSending = true;

					System.out.println("popped response");

					pool.submit(new Runnable() {
						@Override
						public void run() {
							try {
								System.out.println("sending response");
								try {
									response.conn.writeResponse(response.firstLine, response.headerLines, response.content);
								} catch (IOException exc) {
									exc.printStackTrace();
									throw exc;
								}
								System.out.println("sent response");

								response.conn.isSending = false;

								response.conn.readHeader();
							} catch (IOException exc) {
								System.out.println("closed connection");
								response.conn.isOpen = false;

								enqueue(runnable_queue, new Runnable() {
									@Override
									public void run() {
										if (response.conn.channel != null) {
											unregister(response.conn);
										}
									}
								});

								try {
									response.conn.socket.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					});
				}
			}
		}).start();

		try (ServerSocket listen = new ServerSocket(8421)) {
			while (true) {
				final Socket client = listen.accept();

				pool.submit(new Runnable() {
					@Override
					public void run() {
						try {
							new Connection(client);
						} catch (IOException exc) {
							// ignore
						}
					}
				});
			}
		}
	}

	static class Connection {
		static final AtomicInteger ID_GEN = new AtomicInteger();
		volatile boolean isOpen;
		volatile boolean isSending;
		final int id;
		final Socket socket;
		final BufferedReader br;
		final OutputStream out;
		String channel;

		public Connection(Socket socket) throws IOException {
			final int bufSize = 1024;

			this.id = ID_GEN.incrementAndGet();

			this.socket = socket;
			this.socket.setSoTimeout(10_000);
			this.socket.setReceiveBufferSize(bufSize);
			this.socket.setSendBufferSize(bufSize);

			this.br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), bufSize);
			this.out = new BufferedOutputStream(socket.getOutputStream(), bufSize);

			this.isOpen = true;
			this.isSending = false;

			this.readHeader();
		}

		void readHeader() throws IOException {
			String firstLine = null;
			Map<String, String> headerLines = new HashMap<>();

			while (true) {
				String line = br.readLine();
				System.out.println("request[" + id + "]: " + line);

				if (line == null)
					throw new EOFException();

				if (line.isEmpty())
					break;

				if (firstLine == null) {
					firstLine = line;
				} else {
					int io = line.indexOf(':');
					if (io == -1)
						throw new IllegalStateException();
					headerLines.put(line.substring(0, io).trim(), line.substring(io + 1).trim());
				}
			}

			enqueue(request_queue, new Request(this, firstLine, headerLines));
		}

		void writeResponse(String firstLine, Map<String, String> headerLines, byte[] content) throws IOException {
			StringBuilder header = new StringBuilder();
			header.append(firstLine).append("\r\n");
			for (Entry<String, String> entry : headerLines.entrySet()) {
				header.append(entry.getKey() + ": " + entry.getValue() + "\r\n");
			}
			header.append("Content-Length: " + content.length + "\r\n");
			header.append("\r\n");

			System.out.println("response[" + id + "]:");
			System.out.println("----");
			System.out.println(header.toString());
			System.out.println("----");

			out.write(utf8(header.toString()));
			out.write(content);
			// out.write(utf8("\r\n"));
			out.flush();
		}
	}

	static String utf8(byte[] b) {
		try {
			return new String(b, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	static byte[] utf8(String s) {
		try {
			return s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	static <T> T dequeuePoll(ArrayBlockingQueue<T> queue) {
		return queue.poll();
	}

	static <T> T dequeue(ArrayBlockingQueue<T> queue) {
		while (true) {
			try {
				return queue.take();
			} catch (InterruptedException e) {
				// retry
			}
		}
	}

	static <T> void enqueue(ArrayBlockingQueue<T> queue, T item) {
		while (true) {
			try {
				queue.put(item);
				return;
			} catch (InterruptedException e) {
				// retry
			}
		}
	}
}