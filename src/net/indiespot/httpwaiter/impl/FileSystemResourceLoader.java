package net.indiespot.httpwaiter.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.indiespot.httpwaiter.ResourceLoader;

public class FileSystemResourceLoader implements ResourceLoader {
	private static final int max_size = 64 * 1024;

	@Override
	public byte[] load(String path) {
		path = path.replace('\\', '/');
		while (path.contains("/../"))
			path = path.replace("/../", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		File file = new File(System.getProperty("resource.path"), path);

		try (InputStream in = new FileInputStream(file)) {
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
