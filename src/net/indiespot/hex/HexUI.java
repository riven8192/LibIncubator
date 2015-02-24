package net.indiespot.hex;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GLContext;

import craterstudio.io.Streams;
import craterstudio.text.Text;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;

public class HexUI {
	public static void main(String[] args) throws Exception {
		Display.setDisplayMode(new DisplayMode(800, 800));
		Display.create();
		System.out.println(Display.getAdapter() + " v" + Display.getVersion());
		System.out.println(glGetString(GL_VERSION));

		String vsSource = Text.ascii(Streams.readStream(HexUI.class.getResourceAsStream("test.vs")));
		String fsSource = Text.ascii(Streams.readStream(HexUI.class.getResourceAsStream("test.fs")));

		int vsHandle = createShader(vsSource, GL_VERTEX_SHADER);
		int fsHandle = createShader(fsSource, GL_FRAGMENT_SHADER);

		int progHandle = glCreateProgram();
		glAttachShader(progHandle, vsHandle);
		glAttachShader(progHandle, fsHandle);
		glLinkProgram(progHandle);

		glClearColor(0, 0, 0, 1.0f);
		glUseProgram(progHandle);

		{
			int invViewportLoc = 0;
			glUniform2f(invViewportLoc, 1.0f / Display.getWidth(), 1.0f / Display.getHeight());
		}

		// --

		// full screen triangle
		float points[] = { //
		-1.0f, +3.0f, 0.0f, //
				-1.0f, -1.0f, 0.0f, //
				+3.0f, -1.0f, 0.0f //
		};
		FloatBuffer vertexData = BufferUtils.createFloatBuffer(points.length);
		vertexData.put(points).flip();
		int vertexAttr = 0;
		int vertexDim = 3;
		int vertexStride = vertexDim << 2;

		int vao = glGenVertexArrays();
		glBindVertexArray(vao);
		glEnableVertexAttribArray(0);

		int vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glVertexAttribPointer(vertexAttr, vertexDim, GL_FLOAT, false, vertexStride, 0L);

		// --

		while (!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glBindVertexArray(vao);
			glDrawArrays(GL_TRIANGLES, 0, vertexData.limit() / vertexDim);

			Display.sync(60);
			Display.update();
		}

		Display.destroy();
	}

	public static int createShader(String text, int type) {
		int shader = glCreateShader(type);
		try {
			glShaderSource(shader, text);
		} catch (Exception e) {
			glDeleteShader(shader);
			throw new IllegalStateException(e);
		}
		glCompileShader(shader);

		int success = glGetShaderi(shader, GL_COMPILE_STATUS);
		if (success == 0) {
			int len = glGetShaderi(shader, GL_INFO_LOG_LENGTH);
			String log = glGetShaderInfoLog(shader, len);
			throw new IllegalStateException(log);
		}

		return shader;
	}
}
