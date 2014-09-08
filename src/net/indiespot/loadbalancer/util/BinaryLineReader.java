package net.indiespot.loadbalancer.util;

import java.io.*;
import java.util.*;

public class BinaryLineReader {

	public static final String readLineAsString(InputStream in) {
		byte[] raw = readLineInc(in, 64 * 1024);
		if (raw == null) {
			return null;
		}

		// must end with "\r\n"
		if ((raw[raw.length - 2] & 0xFF) != '\r') {
			throw new IllegalStateException("no eol: \"" + Text.ascii(raw) + "\"");
		}
		if ((raw[raw.length - 1] & 0xFF) != '\n') {
			throw new IllegalStateException("no eol: \"" + Text.ascii(raw) + "\"");
		}

		byte[] raw2 = new byte[raw.length - 2];
		System.arraycopy(raw, 0, raw2, 0, raw2.length);
		return Text.utf8(raw2);
	}

	public static final String readLineAsString(InputStream in, byte[] fill) {
		int pos = readLineInc(in, fill);
		if (pos == -1) {
			return null;
		}

		// must end with "\r\n"
		if ((fill[pos - 2] & 0xFF) != '\r') {
			throw new IllegalStateException("no eol: \"" + Text.ascii(Text.utf8(Arrays.copyOf(fill, pos))) + "\"");
		}
		if ((fill[pos - 1] & 0xFF) != '\n') {
			throw new IllegalStateException("no eol: \"" + Text.ascii(Text.utf8(Arrays.copyOf(fill, pos))) + "\"");
		}

		return Text.utf8(Arrays.copyOf(fill, pos - 2));
	}

	public static final int readLineInc(InputStream in, byte[] fill) {
		boolean wasReturnChar = false;
		int pos = 0;

		while (true) {
			int b;

			try {
				b = in.read();
			} catch (IOException exc) {
				b = -1;
			}

			if (b == -1) {
				if (pos == 0) {
					return -1;
				}
				break;
			}

			fill[pos++] = (byte) b;

			if (wasReturnChar && b == (byte) '\n') {
				break;
			}

			wasReturnChar = (b == (byte) '\r');
			if (wasReturnChar) {
				continue;
			}

			// last, to ensure '\r' is always followed by next char
			if (pos >= fill.length) {
				break;
			}
		}

		return pos;
	}

	public static final byte[] readLineInc(InputStream in, int upToBytes) {
		ByteList list = new ByteList();

		boolean wasReturnChar = false;

		while (true) {
			int b;

			try {
				b = in.read();
			} catch (IOException exc) {
				b = -1;
			}

			if (b == -1) {
				if (list.size() == 0) {
					return null;
				}
				break;
			}

			list.add((byte) b);

			if (wasReturnChar && b == (byte) '\n') {
				break;
			}

			wasReturnChar = (b == (byte) '\r');
			if (wasReturnChar) {
				continue;
			}

			// last, to ensure '\r' is always followed by next char
			if (list.size() >= upToBytes) {
				break;
			}
		}

		return list.toArray();
	}
}
