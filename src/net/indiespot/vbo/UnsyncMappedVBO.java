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
	private final int[] bufferHandles;
	private int requestedSize, allocatedSize;
	private int currentBufferIndex;

	public UnsyncMappedVBO(int glTarget, int glUsage) {
		this.glTarget = glTarget; // GL_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER
		this.glUsage = glUsage; // GL_STATIC_DRAW, GL_STREAM_DRAW

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
	public int ensureSize(int size) {
		assert size > 0;

		requestedSize = size;

		if (size > allocatedSize) {
			allocatedSize = size;
			for (int i = 0; i < bufferHandles.length; i++) {
				glBindBuffer(glTarget, bufferHandles[i]);
				glBufferData(glTarget, allocatedSize, glUsage);
			}
			this.bind();
		}

		return allocatedSize;
	}

	@Override
	public int trimToSize() {
		if (requestedSize != allocatedSize) {
			allocatedSize = requestedSize;
			for (int i = 0; i < bufferHandles.length; i++) {
				glBindBuffer(glTarget, bufferHandles[i]);
				glBufferData(glTarget, allocatedSize, glUsage);
			}
			this.bind();
		}

		return allocatedSize;
	}

	@Override
	public int size() {
		return allocatedSize;
	}

	@Override
	public ByteBuffer map() {
		long offset = 0;
		long length = requestedSize;

		if (GLContext.getCapabilities().OpenGL30) {
			int flags = GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL30.GL_MAP_INVALIDATE_RANGE_BIT;
			return GL30.glMapBufferRange(glTarget, offset, length, flags, null);
		}

		if (GLContext.getCapabilities().GL_ARB_map_buffer_range) {
			int flags = ARBMapBufferRange.GL_MAP_WRITE_BIT | ARBMapBufferRange.GL_MAP_UNSYNCHRONIZED_BIT | ARBMapBufferRange.GL_MAP_INVALIDATE_RANGE_BIT;
			return ARBMapBufferRange.glMapBufferRange(glTarget, offset, length, flags, null);
		}

		return GL15.glMapBuffer(glTarget, GL15.GL_WRITE_ONLY, null);
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
