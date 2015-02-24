package net.indiespot.httpwaiter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Waiter {
	static final ArrayBlockingQueue<Runnable> runnable_queue = new ArrayBlockingQueue<>(1000);
	static final ArrayBlockingQueue<Request> request_queue = new ArrayBlockingQueue<>(1000);
	static final ArrayBlockingQueue<Response> response_queue = new ArrayBlockingQueue<>(1000);

	static final ChannelStorage channel_storage;
	static final ChannelListeners channel_listeners;
	static final GroupManager group_manager;
	static final ResourceLoader resource_loader;
	static final TokenManager token_manager;

	static {
		try {
			channel_storage = (ChannelStorage) Class.forName(System.getProperty("channel.storage.impl")).newInstance();
			channel_listeners = (ChannelListeners) Class.forName(System.getProperty("channel.listeners.impl")).newInstance();
			group_manager = (GroupManager) Class.forName(System.getProperty("group.manager.impl")).newInstance();
			resource_loader = (ResourceLoader) Class.forName(System.getProperty("resource.loader.impl")).newInstance();
			token_manager = (TokenManager) Class.forName(System.getProperty("token.manager.impl")).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

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
				// String version = parts[2];

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
				} else if (action.startsWith("/offset/")) {
					String[] actionParts = action.split("/");
					if (actionParts.length <= 2) {
						this.sendBadRequest(request);
						return;
					}

					String channel = actionParts[2];

					int idx = channel_storage.getLatestMessageIndex(channel);

					String firstLine = "HTTP/1.1 200 OK";
					Map<String, String> headerLines = new HashMap<>();
					headerLines.put("Content-Type", "text/plain");

					byte[] message = utf8(String.valueOf(idx));
					
					enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					return;
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
						if (actionParts.length <= 4) {
							this.sendBadRequest(request);
							return;
						}

						String postToken = actionParts[3];
						byte[] message = utf8(actionParts[4]);

						String userId = "123";
						if (!token_manager.verifyPostTokenForChannel(channel, userId, postToken)) {
							this.sendBadRequest(request);
							return;
						}

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
						byte[] message = resource_loader.load(action);
						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/css");
						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					} else if (action.equals("/channel.js")) {
						byte[] message = resource_loader.load(action);
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
						StringBuilder user = new StringBuilder();
						for (char c : actionParts[2].toCharArray())
							if (false || //
									(c >= 'a' && c <= 'z') || //
									(c >= 'A' && c <= 'Z') || //
									(c >= '0' && c <= '9') || //
									(c == '-' || c == '_' || c == '.' || c == '@'))
								user.append(c);

						byte[] message = resource_loader.load("channel_view.html");
						message = utf8(utf8(message).replace("${user}", user));

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/html; charset=utf-8");
						enqueue(response_queue, new Response(request.conn, firstLine, headerLines, message));
					} else if (action.startsWith("/post/")) {
						String[] actionParts = action.split("/");
						if (actionParts.length <= 2) {
							this.sendBadRequest(request);
							return;
						}
						StringBuilder user = new StringBuilder();
						for (char c : actionParts[2].toCharArray())
							if (false || //
									(c >= 'a' && c <= 'z') || //
									(c >= 'A' && c <= 'Z') || //
									(c >= '0' && c <= '9') || //
									(c == '-' || c == '_' || c == '.' || c == '@'))
								user.append(c);

						byte[] message = resource_loader.load("channel_post.html");
						message = utf8(utf8(message).replace("${user}", user));

						String firstLine = "HTTP/1.1 200 OK";
						Map<String, String> headerLines = new HashMap<>();
						headerLines.put("Content-Type", "text/html; charset=utf-8");
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