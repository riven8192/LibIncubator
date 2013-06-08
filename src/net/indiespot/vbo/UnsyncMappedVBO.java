package net.indiespot.vbo;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import static org.lwjgl.opengl.GL15.*;

public class UnsyncMappedVBO implements VBO {
	// triple buffering in stereo mode is rather rare though..
	private static final int MAX_FRAMEBUFFER_COUNT = 2 * 3;

	private final int glTarget, glUsage;
	private final int[] bufferHandles, bufferSizes;
	private int requestedSize, allocatedSize;
	private int currentBufferIndex;

	public UnsyncMappedVBO(int glTarget, int glUsage) {
		this.glTarget = glTarget; // GL_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER
		this.glUsage = glUsage; // GL_STATIC_DRAW, GL_STREAM_DRAW

		bufferSizes = new int[MAX_FRAMEBUFFER_COUNT];
		bufferHandles = new int[MAX_FRAMEBUFFER_COUNT];
		for (int i = 0; i < this.bufferHandles.length; i++) {
			bufferHandles[i] = glGenBuffers();
		}

		currentBufferIndex = -1;
	}

	@Override
	public void nextFrame() {
		currentBufferIndex = (currentBufferIndex + 1) % MAX_FRAMEBUFFER_COUNT;
	}

	@Override
	public void bind() {
		glBindBuffer(glTarget, currentBufferHandle());
	}

	@Override
	public int currentBufferHandle() {
		return bufferHandles[currentBufferIndex];
	}

	@Override
	public ByteBuffer map(int off, int len) {
		if (off < 0 || len <= 0) {
			throw new IllegalArgumentException();
		}

		final int end = off + len;
		if (end > bufferSizes[currentBufferIndex]) {
			bufferSizes[currentBufferIndex] = end;
			glBufferData(glTarget, bufferSizes[currentBufferIndex], glUsage);
		}

		ByteBuffer mapped;

		if (GLContext.getCapabilities().OpenGL30) {
			int flags = GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT;// | GL30.GL_MAP_INVALIDATE_RANGE_BIT;
			mapped = GL30.glMapBufferRange(glTarget, off, len, flags, null);
		} else if (GLContext.getCapabilities().GL_ARB_map_buffer_range) {
			int flags = ARBMapBufferRange.GL_MAP_WRITE_BIT | ARBMapBufferRange.GL_MAP_UNSYNCHRONIZED_BIT;// | ARBMapBufferRange.GL_MAP_INVALIDATE_RANGE_BIT;
			mapped = ARBMapBufferRange.glMapBufferRange(glTarget, off, len, flags, null);
		} else {
			mapped = GL15.glMapBuffer(glTarget, GL15.GL_WRITE_ONLY, null);
		}

		if (mapped == null) {
			throw new IllegalStateException("mapped buffer is null: length=" + len + ", requestedSize=" + requestedSize + ", allocatedSize=" + allocatedSize);
		}

		return mapped;
	}

	@Override
	public void unmap() {
		glUnmapBuffer(glTarget);
	}

	@Override
	public void delete() {
		for (int i = 0; i < bufferHandles.length; i++) {
			glDeleteBuffers(bufferHandles[i]);
			bufferHandles[i] = -1;
		}
	}
}
