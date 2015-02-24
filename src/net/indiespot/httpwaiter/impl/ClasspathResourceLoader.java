package net.indiespot.httpwaiter.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.indiespot.httpwaiter.ResourceLoader;

public class ClasspathResourceLoader implements ResourceLoader {
	private static final int max_size = 64 * 1024;

	@Override
	public byte[] load(String path) {
		path = path.replace('\\', '/');
		while (path.contains("/../"))
			path = path.replace("/../", "/");
		while (path.startsWith("/"))
			path = path.substring(1);

		String basePath = System.getProperty("resource.path");
		while (basePath.startsWith("/"))
			basePath = basePath.substring(1);
		while (basePath.endsWith("/"))
			basePath = basePath.substring(0, basePath.length() - 1);

		path = "/" + basePath + "/" + path;

		try (InputStream in = this.getClass().getResourceAsStream(path)) {
			if (in == null) {
				System.err.println("missing resource: '" + path + "'");
				return null;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] tmp = new byte[1024];
			for (int got; (got = in.read(tmp)) != -1;)
				if (baos.size() + got > max_size)
					return null;
				else
					baos.write(tmp, 0, got);
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
