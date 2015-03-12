package net.indiespot.opengl;

import static org.lwjgl.opengl.ARBDrawIndirect.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.ARBMultiDrawIndirect.glMultiDrawElementsIndirect;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class MultiDrawElements {
	public final int stride;

	public ByteBuffer vertexData;
	public ShortBuffer indexData;
	public IntBuffer commandData;

	public MultiDrawElements(int stride) {
		this.stride = stride;
	}

	private int vertexDataId;
	private int indexDataId;
	private int commandDataId;

	public void glInit() {
		vertexDataId = glGenBuffers();
		indexDataId = glGenBuffers();
		commandDataId = glGenBuffers();
	}

	private int lastVertexPosition;
	private int lastIndexPosition;

	public void reset() {
		vertexData.clear();
		indexData.clear();
		commandData.clear();

		lastVertexPosition = 0;
		lastIndexPosition = 0;
	}

	public void scheduleDrawCommand() {
		this.scheduleDrawCommand(1, 0);
	}

	public void scheduleDrawCommand(int instanceCount) {
		this.scheduleDrawCommand(instanceCount, 0);
	}

	public void scheduleDrawCommand(int instanceCount, int baseInstance) {
		if (vertexData.position() % stride != 0)
			throw new IllegalStateException();

		int elementCount = indexData.position() - lastIndexPosition;
		int writtenVertexData = vertexData.position() - lastVertexPosition;

		if (elementCount % 3 != 0)
			throw new IllegalStateException("awww, no tris?");

		if (writtenVertexData % stride != 0)
			throw new IllegalStateException();

		int baseVertex = lastVertexPosition / stride;
		int firstIndex = lastIndexPosition;

		lastVertexPosition = vertexData.position();
		lastIndexPosition = indexData.position();

		commandData.put(elementCount);
		commandData.put(instanceCount);
		commandData.put(firstIndex);
		commandData.put(baseVertex);
		commandData.put(baseInstance);
	}

	private static final int command_stride = 5 << 2;

	public void glBindBuffers() {
		glBindBuffer(GL_ARRAY_BUFFER, vertexDataId);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexDataId);
		glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandDataId);
	}

	public void glUploadBufferData() {
		vertexData.flip();
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STREAM_DRAW);

		indexData.flip();
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STREAM_DRAW);

		commandData.flip();
		glBufferData(GL_DRAW_INDIRECT_BUFFER, commandData, GL_STATIC_DRAW);
	}

	public void glMultiDraw() {
		int commandCount = (commandData.remaining() << 2) / command_stride;
		glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, 0L, commandCount, command_stride);
	}
}
