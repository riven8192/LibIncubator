package net.indiespot.vbo;

import java.nio.ByteBuffer;

public interface VBO {

	public void nextFrame();

	public void bind();

	public int currentBufferHandle();

	public ByteBuffer map(int off, int len);

	public void unmap();

	public void delete();

}
