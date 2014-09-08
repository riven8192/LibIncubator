package net.indiespot.loadbalancer.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class Text {
	public static void main(String[] args) {
		System.out.println(formatInt(-1, 3));
		System.out.println(formatInt(0, 3));
		System.out.println(formatInt(+1, 3));
		System.out.println(formatInt(+100, 3));
		System.out.println(formatInt(+999, 3));
		System.out.println(formatInt(+1000, 3));
		System.out.println(formatInt(+1001, 3));
	}

	public static String formatInt(int value, int digits) {
		boolean isNeg = value < 0;
		if (isNeg) {
			value *= -1;
		}

		final String zeros = "000000000000000000000000000000000000000000000000";

		String s = String.valueOf(value);

		StringBuilder sb = new StringBuilder();

		if (isNeg) {
			sb.append('-');
		}
		if (s.length() < digits) {
			sb.append(zeros.substring(0, digits - s.length()));
		}
		sb.append(value);
		return sb.toString();
	}

	public static String formatNanos(long nanos) {
		nanos += 50_000L; // round sub

		long millis = nanos / 1_000_000;
		long sub = (nanos % 1_000_000) / 100_000L;
		return millis + "." + sub + "ms";
	}

	public static String trimToNull(String value) {
		value = value.trim();
		return value.isEmpty() ? null : value;
	}

	public static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public static boolean contains(String value, char find) {
		return value.indexOf(find) != -1;
	}

	public static int count(String value, char find) {
		int matches = 0;
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) == find) {
				matches++;
			}
		}
		return matches;
	}

	public static String remove(String value, char find) {
		char[] hold = new char[value.length() - count(value, find)];
		for (int i = 0, k = 0; i < value.length(); i++) {
			if (value.charAt(i) != find) {
				hold[k++] = value.charAt(i);
			}
		}
		return new String(hold);
	}

	public static String removeWhitespace(String value) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			if (!Character.isWhitespace(value.charAt(i))) {
				sb.append(value.charAt(i));
			}
		}
		return sb.toString();
	}

	public static int indexOfWhitespace(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isWhitespace(value.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	public static final String convertWhiteSpaceTo(String s, char c) {
		char[] cs = s.toCharArray();
		for (int i = 0; i < cs.length; i++) {
			if (cs[i] <= ' ') {
				cs[i] = c;
			}
		}
		return new String(cs);
	}

	public static final String replace(String s, char a, char b) {
		if (s.indexOf(a) == -1) {
			return s; // early escape
		}

		char[] array = s.toCharArray();
		for (int i = 0; i < array.length; i++) {
			if (array[i] == a) {
				array[i] = b;
			}
		}
		return new String(array);
	}

	public static final String normalizeLinebreaks(String value) {
		value = Text.replace(value, "\r\n", "\n");
		value = Text.replace(value, '\r', '\n');

		return value;
	}

	// hex

	public static final String hex(byte[] raw) {
		char[] table = "0123456789abcdef".toCharArray();
		char[] hex = new char[raw.length << 1];
		for (int k = 0; k < raw.length; k++) {
			int h = raw[k];
			hex[(k << 1) | 1] = table[(h & 0x0F) >> 0];
			hex[(k << 1) | 0] = table[(h & 0xF0) >> 4];
		}
		return new String(hex);
	}

	// ascii

	public static final String ascii(byte[] raw, int off, int len) {
		char[] str = new char[len];
		for (int i = 0; i < len; i++) {
			str[i] = (char) raw[off + i];
		}
		return new String(str);
	}

	public static final String ascii(byte[] raw) {
		char[] str = new char[raw.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = (char) raw[i];
		}
		return new String(str);
	}

	public static final byte[] ascii(String str) {
		byte[] raw = new byte[str.length()];
		for (int i = 0; i < raw.length; i++) {
			raw[i] = (byte) str.charAt(i);
		}
		return raw;
	}

	public static final String ascii(ByteBuffer raw) {
		char[] str = new char[raw.remaining()];
		for (int i = 0; i < str.length; i++) {
			str[i] = (char) raw.get(raw.position() + i);
		}
		return new String(str);
	}

	// utf8

	public static final String utf8(byte[] raw) {
		return Text.utf8(raw, 0, raw.length);
	}

	public static final String utf8(byte[] raw, int off, int len) {
		try {
			return new String(raw, off, len, "UTF-8");
		} catch (UnsupportedEncodingException exc) {
			throw new IllegalStateException();
		}
	}

	public static final byte[] utf8(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException exc) {
			throw new IllegalStateException();
		}
	}

	// between

	public static final String between(String value, char start, char end) {
		value = after(value, start);
		return (value == null) ? null : before(value, end);
	}

	public static final String between(String value, String start, String end) {
		value = after(value, start);
		return (value == null) ? null : before(value, end);
	}

	// before (String, char)

	public static final String before(String value, char find) {
		return before(value, find, null);
	}

	public static final String beforeIfAny(String value, char find) {
		return before(value, find, value);
	}

	public static final String beforeLast(String value, char find) {
		return beforeLast(value, find, null);
	}

	public static final String before(String value, char find, String defaultValue) {
		int indexOf = value.indexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(0, indexOf);
	}

	public static final String beforeLast(String value, char find, String defaultValue) {
		int indexOf = value.lastIndexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(0, indexOf);
	}

	// before (String, String)

	public static final String before(String value, String find) {
		return before(value, find, null);
	}

	public static final String beforeLast(String value, String find) {
		return beforeLast(value, find, null);
	}

	public static final String before(String value, String find, String defaultValue) {
		int indexOf = value.indexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(0, indexOf);
	}

	public static final String beforeLast(String value, String find, String defaultValue) {
		int indexOf = value.lastIndexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(0, indexOf);
	}

	// after (String, char)

	public static final String after(String value, char find) {
		return after(value, find, null);
	}

	public static final String afterLast(String value, char find) {
		return afterLast(value, find, null);
	}

	public static final String after(String value, char find, String defaultValue) {
		int indexOf = value.indexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(indexOf + 1);
	}

	public static final String afterLast(String value, char find, String defaultValue) {
		int indexOf = value.lastIndexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(indexOf + 1);
	}

	// after (String, String)

	public static final String after(String value, String find) {
		return after(value, find, null);
	}

	public static final String afterLast(String value, String find) {
		return afterLast(value, find, null);
	}

	public static final String after(String value, String find, String defaultValue) {
		int indexOf = value.indexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(indexOf + find.length());
	}

	public static final String afterLast(String value, String find, String defaultValue) {
		int indexOf = value.lastIndexOf(find);
		return (indexOf == -1) ? defaultValue : value.substring(indexOf + find.length());
	}

	// split

	public static final String[] splitPair(String input, char delim) {
		int io = input.indexOf(delim);
		if (io == -1) {
			return null;
		}
		return new String[] { input.substring(0, io), input.substring(io + 1) };
	}

	public static final String[] splitPair(String input, String delim) {
		int io = input.indexOf(delim);
		if (io == -1) {
			return null;
		}
		return new String[] { input.substring(0, io), input.substring(io + delim.length()) };
	}

	public static final String replace(String input, String search, String replace) {
		StringBuilder sb = new StringBuilder();

		int off = 0;

		while (true) {
			int index = input.indexOf(search, off);
			if (index == -1) {
				break;
			}
			sb.append(input.substring(off, index));
			sb.append(replace);
			off = index + search.length();
		}
		return sb.append(input.substring(off)).toString();
	}

	public static final List<String> splitOnLines(String value) {
		return Text.split(normalizeLinebreaks(value), '\n');
	}

	public static final List<String> split(String input, char delim) {
		List<String> parts = new ArrayList<String>();
		int fromIndex = 0;
		for (int index; (index = input.indexOf(delim, fromIndex)) != -1;) {
			parts.add(input.substring(fromIndex, index));
			fromIndex = index + 1;
		}
		parts.add(input.substring(fromIndex));
		return parts;
	}

	public static final List<String> split(String input, String delim) {
		List<String> parts = new ArrayList<String>();
		int fromIndex = 0;
		for (int index; (index = input.indexOf(delim, fromIndex)) != -1;) {
			parts.add(input.substring(fromIndex, index));
			fromIndex = index + delim.length();
		}
		parts.add(input.substring(fromIndex));
		return parts;
	}

	public static final String join(List<String> values, char delim) {
		int len = Math.max(0, values.size() - 1);
		for (int i = 0; i < values.size(); i++) {
			len += values.get(i).length();
		}

		StringBuilder sb = new StringBuilder(len);
		for (String value : values) {
			if (sb.length() != 0) {
				sb.append(delim);
			}
			sb.append(value);
		}
		return sb.toString();
	}

	public static final String join(List<String> values, String delim) {
		int len = Math.max(0, (values.size() - 1) * delim.length());
		for (int i = 0; i < values.size(); i++) {
			len += values.get(i).length();
		}

		StringBuilder sb = new StringBuilder(len);
		for (String value : values) {
			if (sb.length() != 0) {
				sb.append(delim);
			}
			sb.append(value);
		}
		return sb.toString();
	}

	public static final String join(String[] values, char delim) {
		int len = Math.max(0, values.length - 1);
		for (int i = 0; i < values.length; i++) {
			len += values[i].length();
		}

		StringBuilder sb = new StringBuilder(len);
		for (String value : values) {
			if (sb.length() != 0) {
				sb.append(delim);
			}
			sb.append(value);
		}
		return sb.toString();
	}

	public static final String join(String[] values, String delim) {
		int len = Math.max(0, (values.length - 1) * delim.length());
		for (int i = 0; i < values.length; i++) {
			len += values[i].length();
		}

		StringBuilder sb = new StringBuilder(len);
		for (String value : values) {
			if (sb.length() != 0) {
				sb.append(delim);
			}
			sb.append(value);
		}
		return sb.toString();
	}

	public static final String join(List<String> values) {
		int len = 0;
		for (int i = 0; i < values.size(); i++) {
			len += values.get(i).length();
		}

		StringBuilder sb = new StringBuilder(len);
		for (String value : values) {
			sb.append(value);
		}
		return sb.toString();
	}

	public static final String join(String... values) {
		int len = 0;
		for (int i = 0; i < values.length; i++) {
			len += values[i].length();
		}

		StringBuilder sb = new StringBuilder(len);
		for (String value : values) {
			sb.append(value);
		}
		return sb.toString();
	}

	// gen

	private static final char[] generatecode_default_chars;
	static {
		generatecode_default_chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567890".toCharArray();
	}

	public static String generateRandomCode(int len) {
		return generateRandomCode(len, generatecode_default_chars, new SecureRandom());
	}

	public static String generateRandomCode(int len, char[] chars, Random r) {
		char[] dst = new char[len];
		for (int i = 0; i < dst.length; i++) {
			dst[i] = chars[r.nextInt(chars.length)];
		}
		return new String(dst);
	}

	public static String hash(String a) {
		return Hash.toHex(Hash.sha256(Text.utf8(a)));
	}

	public static String hash(String a, String b) {
		return hash(hash(a) + ":" + hash(b));
	}

	// date/time

	private static volatile long datetime_timestamp;
	private static volatile String datetime_text;

	public static String datetime() {
		long now = System.currentTimeMillis();
		if (now - datetime_timestamp >= 1000L) {
			datetime_text = datetime(Calendar.getInstance());
			datetime_timestamp = now;
		}
		return datetime_text;
	}

	public static String datetime(Calendar c) {
		return date(c) + ' ' + time(c);
	}

	public static String date(Calendar c) {
		int y = c.get(Calendar.YEAR);
		int m = c.get(Calendar.MONTH) + 1;
		int d = c.get(Calendar.DAY_OF_MONTH);

		StringBuilder sb = new StringBuilder();

		if (y < 10) {
			sb.append('0');
		}
		if (y < 100) {
			sb.append('0');
		}
		if (y < 1000) {
			sb.append('0');
		}
		sb.append(y);

		sb.append('-');

		if (m < 10) {
			sb.append('0');
		}
		sb.append(m);

		sb.append('-');

		if (d < 10) {
			sb.append('0');
		}
		sb.append(d);

		return sb.toString();
	}

	public static String time(Calendar c) {
		int h = c.get(Calendar.HOUR_OF_DAY);
		int m = c.get(Calendar.MINUTE);
		int s = c.get(Calendar.SECOND);

		StringBuilder sb = new StringBuilder();

		if (h < 10) {
			sb.append('0');
		}
		sb.append(h);

		sb.append(':');

		if (m < 10) {
			sb.append('0');
		}
		sb.append(m);

		sb.append(':');

		if (s < 10) {
			sb.append('0');
		}
		sb.append(s);

		return sb.toString();
	}
}