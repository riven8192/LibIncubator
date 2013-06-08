package net.indiespot.vbo;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL15.*;

public class DataBufferVBO implements VBO {
	private final int glTarget, glUsage;
	private final int bufferHandle;
	private int requestedSize;
	private int allocatedSize;
	private ByteBuffer scratch;

	public DataBufferVBO(int glTarget, int glUsage) {
		this.glTarget = glTarget; // GL_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER
		this.glUsage = glUsage; // GL_STATIC_DRAW, GL_STREAM_DRAW

		requestedSize = 0;
		allocatedSize = 0;

		bufferHandle = glGenBuffers();
	}

	@Override
	public void nextFrame() {
		// no-op
	}

	@Override
	public void bind() {
		glBindBuffer(glTarget, bufferHandle);
	}

	@Override
	public int currentBufferHandle() {
		return bufferHandle;
	}

	@Override
	public int ensureSize(int size) {
		assert size > 0;

		requestedSize = size;
		if (size > allocatedSize) {
			allocatedSize = size;
			glBufferData(glTarget, allocatedSize, glUsage);
			scratch = BufferUtils.createByteBuffer(allocatedSize);
		}

		return allocatedSize;
	}

	@Override
	public int trimToSize() {
		if (requestedSize != allocatedSize) {
			allocatedSize = requestedSize;
			glBufferData(glTarget, allocatedSize, glUsage);
			scratch = BufferUtils.createByteBuffer(allocatedSize);
		}

		return allocatedSize;
	}

	@Override
	public int size() {
		return allocatedSize;
	}

	@Override
	public ByteBuffer map() {
		scratch.clear();
		return scratch;
	}

	@Override
	public void unmap() {
		scratch.clear();

		{
			// orphan (roughly halves duration)
			glBufferData(glTarget, allocatedSize, glUsage);
		}

		glBufferData(glTarget, scratch, glUsage);
	}

	@Override
	public void delete() {
		glDeleteBuffers(bufferHandle);
	}
}
