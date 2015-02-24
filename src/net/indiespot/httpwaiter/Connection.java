package net.indiespot.httpwaiter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection {
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

		Waiter.enqueue(Waiter.request_queue, new Request(this, firstLine, headerLines));
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

		out.write(Waiter.utf8(header.toString()));
		out.write(content);
		// out.write(utf8("\r\n"));
		out.flush();
	}
}