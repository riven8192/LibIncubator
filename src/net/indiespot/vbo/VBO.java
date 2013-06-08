package net.indiespot.vbo;

import java.nio.ByteBuffer;

public interface VBO {

	public void nextFrame();

	public void bind();

	public int currentBufferHandle();

	public int ensureSize(int size);

	public int trimToSize();

	public int size();

	public ByteBuffer map();

	public void unmap();

	public void delete();

}
