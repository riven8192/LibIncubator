package net.indiespot.vbo;

import java.nio.ByteBuffer;

public class PickBestVBO implements VBO {
	private final int glTarget, glUsage;
	private VBO currentVBO, unsyncMappedVBO, dataBufferVBO;
	private int frameCounter;

	public PickBestVBO(int glTarget, int glUsage) {
		this.glTarget = glTarget;
		this.glUsage = glUsage;
		this.needsInit = true;
	}

	private static final int test_types = 2;
	private static final int test_count = 3 * test_types;
	private static final int test_size = 5;

	private long[] dbTook, umTook;
	private int testIndex;
	private static int vbo_gen;
	private final int vboId = ++vbo_gen;

	public String name() {
		return String.valueOf(vboId);
	}

	private boolean needsInit;

	private void init() {
		if (dataBufferVBO != null) {
			dataBufferVBO.delete();
			dataBufferVBO = null;
		}
		if (unsyncMappedVBO != null) {
			unsyncMappedVBO.delete();
			unsyncMappedVBO = null;
		}

		dataBufferVBO = new DataBufferVBO(glTarget, glUsage);
		unsyncMappedVBO = new UnsyncMappedVBO(glTarget, glUsage);
		currentVBO = null;
		frameCounter = 0;
		dbTook = new long[test_size];
		umTook = new long[test_size];

		needsInit = false;
	}

	@Override
	public void nextFrame() {
		if (needsInit) {
			this.init();
		}

		if (true) {
			currentVBO = unsyncMappedVBO;
		} else {
			testIndex = frameCounter / test_size;
			if (testIndex < test_count) {
				if (testIndex % test_types == 0) {
					currentVBO = dataBufferVBO;
				} else {
					currentVBO = unsyncMappedVBO;
				}
			} else if (frameCounter == test_count * test_size) {
				long dataBufferTook = Long.MAX_VALUE;
				long unsyncMappedTook = Long.MAX_VALUE;
				for (int i = 0; i < test_size; i++) {
					dataBufferTook = Math.min(dataBufferTook, dbTook[i]);
					unsyncMappedTook = Math.min(unsyncMappedTook, umTook[i]);
				}
				dbTook = null;
				umTook = null;

				if (dataBufferTook < unsyncMappedTook) {
					currentVBO = dataBufferVBO;
					unsyncMappedVBO.delete();
					unsyncMappedVBO = null;
				} else {
					currentVBO = unsyncMappedVBO;
					dataBufferVBO.delete();
					dataBufferVBO = null;
				}

				System.out.println("VBO " + name() + " - picked: " + currentVBO.getClass().getSimpleName() + //
				        " (buffer=" + (dataBufferTook / 1000 / test_size) + "us, unsync=" + (unsyncMappedTook / 1000 / test_size) + "us, factor: " + (double) dataBufferTook / unsyncMappedTook + ")");
			}
		}

		currentVBO.nextFrame();
		VBO other = other();
		if (other != null) {
			//other.nextFrame();
		}
		frameCounter++;
	}

	private VBO other() {
		if (currentVBO == unsyncMappedVBO) {
			return dataBufferVBO;
		}
		if (currentVBO == dataBufferVBO) {
			return unsyncMappedVBO;
		}
		return null;
	}

	@Override
	public void bind() {
		currentVBO.bind();
	}

	@Override
	public int currentBufferHandle() {
		return currentVBO.currentBufferHandle();
	}

	private long mapTime;

	@Override
	public ByteBuffer map(int off, int len) {
		if (testIndex < test_count) {
			mapTime = System.nanoTime();
		}
		return currentVBO.map(off, len);
	}

	@Override
	public void unmap() {
		currentVBO.unmap();

		if (testIndex < test_count) {
			if (testIndex % test_types == 0) {
				dbTook[frameCounter % test_size] = System.nanoTime() - mapTime;
			} else {
				umTook[frameCounter % test_size] = System.nanoTime() - mapTime;
			}
		}
	}

	@Override
	public void delete() {
		if (dataBufferVBO != null) {
			dataBufferVBO.delete();
			dataBufferVBO = null;
		}
		if (unsyncMappedVBO != null) {
			unsyncMappedVBO.delete();
			unsyncMappedVBO = null;
		}
		currentVBO = null;
	}
}
