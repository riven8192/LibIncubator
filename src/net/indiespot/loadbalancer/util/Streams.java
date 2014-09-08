package net.indiespot.loadbalancer.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Streams {

	private static final long worker_thread_stacksize = 256 * 1024; // FIXME
	private static final AtomicLong worker_thread_counter = new AtomicLong();
	private static final ThreadGroup streams_group = new ThreadGroup("Streams");
	private static final ThreadFactory thread_factory = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable task) {
			String name = "io-worker-" + worker_thread_counter.incrementAndGet();
			Thread thread = new Thread(streams_group, task, name, worker_thread_stacksize);
			// System.out.println("Streams: " + name);
			return thread;
		}
	};

	public static ThreadFactory getStreamThreadFactory() {
		return thread_factory;
	}

	public static Thread spawn(Runnable task) {
		Thread t = thread_factory.newThread(task);
		t.start();
		return t;
	}

	private static final ThreadPool stream_workers = new ThreadPool(1024, 3600_000L, thread_factory);

	public static AtomicBoolean pool(final Runnable task) {
		final AtomicBoolean latch = new AtomicBoolean(false);

		stream_workers.execute(new Runnable() {
			@Override
			public void run() {
				try {
					task.run();
				} finally {
					latch.set(true);
				}
			}
		});

		return latch;
	}

	//

	public static byte[] loadResource(String path) {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (in == null) {
			throw new IllegalStateException("resource not found: " + path);
		}
		try {
			byte[] data = Streams.readFully(in);
			return data;
		} catch (Exception exc) {
			throw new IllegalStateException(exc);
		}
	}

	public static String loadResourceAsString(String path) {
		try {
			return Text.utf8(loadResource(path));
		} catch (Exception exc) {
			throw new IllegalStateException(exc);
		}
	}

	//

	public static final void writeFile(File file, byte[] data) throws IOException {
		try (OutputStream out = new FileOutputStream(file)) {
			out.write(data);
		}
	}

	public static final byte[] readFile(File file) throws IOException {
		return readFully(new FileInputStream(file));
	}

	//

	public static final byte[] readFully(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(in, out);
		return out.toByteArray();
	}

	public static final byte[] readFully(final InputStream in, final int maxSize) throws IOException {
		InputStream limitedRangeInputStream = new InputStream() {
			private int remaining = maxSize;

			@Override
			public int read() throws IOException {
				if (remaining <= 0) {
					return -1;
				}
				int got = in.read();
				remaining--;
				return got;
			}

			@Override
			public int read(byte[] buf) throws IOException {
				return this.read(buf, 0, buf.length);
			}

			@Override
			public int read(byte[] buf, int off, int len) throws IOException {
				len = Math.min(len, remaining);
				if (remaining <= 0) {
					return -1;
				}

				int got = in.read(buf, off, len);
				if (got == -1) {
					return -1;
				}

				remaining -= got;
				return got;
			}
		};

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(limitedRangeInputStream, out);
		return out.toByteArray();
	}

	//

	public static final void async(final InputStream input, final OutputStream output) {
		Streams.pool(new Runnable() {
			@Override
			public void run() {
				try {
					copy(input, output);
				} catch (IOException exc) {
					exc.printStackTrace();
				}
			}
		});
	}

	//

	public static final void copyFile(File src, File dst) throws IOException {
		if (dst.isDirectory()) {
			throw new IOException("destination file is directory: " + dst.getAbsolutePath());
		}
		File dstDir = dst.getParentFile();
		if (!dstDir.exists() && !dstDir.mkdirs()) {
			throw new IOException("failed to create destination directory for: " + dst.getAbsolutePath());
		}

		File tmp = new File(dst.getAbsolutePath() + "." + System.nanoTime() + ".tmp");
		if (!tmp.createNewFile()) {
			throw new IOException("failed to create intermediary file: " + tmp);
		}

		dst.delete();

		InputStream fis = new FileInputStream(src);
		try {
			OutputStream fos = new FileOutputStream(tmp);
			try {
				copy(fis, fos);
			} finally {
				fos.close();
			}
		} finally {
			fis.close();
		}

		if (!tmp.renameTo(dst)) {
			if (!tmp.delete()) {
				tmp.deleteOnExit();
			}

			throw new IOException("failed to rename intermediary file: " + tmp);
		}

		// only do this if there was no IOE
		dst.setLastModified(src.lastModified());
	}

	public static final long copy(InputStream input, OutputStream output) throws IOException {
		return copy(input, output, false, null);
	}

	public static final long copy(InputStream input, OutputStream output, boolean consumeIOException, long[] accum) throws IOException {
		byte[] buffer = new byte[4096];

		long copied = 0;

		try {
			while (true) {
				int got;
				try {
					got = input.read(buffer);
				} catch (IOException exc) {
					if (!consumeIOException) {
						throw exc;
					}
					got = -1;
				}
				if (got == -1) {
					break;
				}

				try {
					output.write(buffer, 0, got);
				} catch (IOException exc) {
					if (!consumeIOException)
						throw exc;
					break;
				}

				if (accum != null)
					accum[0] += got;
				else
					copied += got;
			}
		} finally {
			try {
				input.close();
			} catch (IOException exc) {
				if (!consumeIOException)
					throw exc;
			} finally {
				try {
					output.close();
				} catch (IOException exc) {
					if (!consumeIOException)
						throw exc;
				}
			}
		}

		return copied;
	}

	public static void close(Closeable c) {
		close(c, false);
	}

	public static void close(Closeable c, boolean printExc) {
		if (c == null) {
			return;
		}

		try {
			c.close();
		} catch (IOException exc) {
			if (printExc) {
				exc.printStackTrace();
			}
		}
	}

	public static String stdinAsk(String q) {
		System.out.print(q);
		System.out.print(": \r\n");
		System.out.flush();
		return stdinReadLine();
	}

	public static String stdinReadLine() {
		try {
			StringBuilder sb = new StringBuilder();
			while (true) {
				int b = System.in.read();
				if (b == -1) {
					break;
				}
				char c = (char) b;
				if (c == '\r' || c == '\n') {
					if (sb.length() == 0) {
						continue;
					}
					break;
				}
				sb.append(c);
			}
			return sb.toString();
		} catch (IOException exc) {
			throw new IllegalStateException(exc);
		}
	}
}
