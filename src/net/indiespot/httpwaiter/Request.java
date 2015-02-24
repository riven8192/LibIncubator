package net.indiespot.httpwaiter;

import java.util.Map;

public class Request {
	final Connection conn;
	final String firstLine;
	final Map<String, String> headerLines;

	public Request(Connection conn, String firstLine, Map<String, String> headerLines) {
		this.conn = conn;
		this.firstLine = firstLine;
		this.headerLines = headerLines;
	}
}