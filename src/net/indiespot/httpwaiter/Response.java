package net.indiespot.httpwaiter;

import java.util.Map;

public class Response {
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