package net.indiespot.vbo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL15.*;

public class DataBufferVBO implements VBO {
	private final int glTarget, glUsage;
	private final int bufferHandle;
	private ByteBuffer scratch;

	public DataBufferVBO(int glTarget, int glUsage) {
		this.glTarget = glTarget; // GL_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER
		this.glUsage = glUsage; // GL_STATIC_DRAW, GL_STREAM_DRAW

		bufferHandle = glGenBuffers();

		glBufferData(glTarget, 0, glUsage);
		scratch = BufferUtils.createByteBuffer(0);
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

	private int maxEnd;

	@Override
	public ByteBuffer map(int off, int len) {
		maxEnd = Math.max(maxEnd, off + len);
		if (maxEnd > scratch.capacity()) {
			scratch = BufferUtils.createByteBuffer(maxEnd);
		}

		scratch.clear();
		scratch.position(off);
		scratch.limit(off + len);
		return scratch.slice().order(ByteOrder.nativeOrder());
	}

	@Override
	public void unmap() {
		scratch.clear();

		{
			// orphan (roughly halves duration)
			glBufferData(glTarget, maxEnd, glUsage);
		}

		glBufferData(glTarget, scratch, glUsage);
	}

	@Override
	public void delete() {
		glDeleteBuffers(bufferHandle);
	}
}
