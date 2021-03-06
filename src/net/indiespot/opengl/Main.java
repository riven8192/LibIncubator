package net.indiespot.opengl;

import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
//import static org.lwjgl.opengl.GL13.*;
//import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
//import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.ARBTextureStorage.*;
import static org.lwjgl.opengl.ARBMultiDrawIndirect.*;
import static org.lwjgl.opengl.ARBDrawIndirect.*;

public class Main {
	public static void main(String[] args) throws LWJGLException {
		Display.setDisplayMode(new DisplayMode(800, 600));
		Display.create(new PixelFormat(8, 16, 8, 8));

		System.out.println(glGetString(GL_VERSION));

		glClearColor(1, 1, 1, 1);

		int texId = glGenTextures();
		{
			int texW = 64;
			int texH = 64;
			ByteBuffer texels = ByteBuffer.allocateDirect(texW * texH * 4 * 2);
			// bright
			for (int i = 0; i < texW * texH; i++) {
				texels.put((byte) (Math.random() * 0x00f + 0x80f));
				texels.put((byte) (Math.random() * 0x80f + 0x80f));
				texels.put((byte) (Math.random() * 0x80f + 0x80f));
				texels.put((byte) 0xff);
			}
			// dark
			for (int i = 0; i < texW * texH; i++) {
				texels.put((byte) (Math.random() * 0x40f + 0x00f));
				texels.put((byte) (Math.random() * 0x00f + 0x00f));
				texels.put((byte) (Math.random() * 0x40f + 0x00f));
				texels.put((byte) 0xff);
			}
			texels.flip();

			glBindTexture(GL_TEXTURE_2D_ARRAY, texId);
			glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1/* levels */, GL_RGBA8, texW, texH, 2);

			texels.position(texW * texH * 4 * 0);
			glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0 /* level */, 0, 0, 0/* z */, texW, texH, 1, GL_RGBA, GL_UNSIGNED_BYTE, texels);
			texels.position(texW * texH * 4 * 1);
			glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0 /* level */, 0, 0, 1/* z */, texW, texH, 1, GL_RGBA, GL_UNSIGNED_BYTE, texels);

			// glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0 /* level */, 0, 0, 0/* z
			// */, texW, texH, 2, GL_RGBA, GL_UNSIGNED_BYTE, texels);

			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		}

		int program;
		{
			int vertexShader;
			{
				List<String> vertexShaderSource = new ArrayList<>();
				vertexShaderSource.add("#version 330                                   ");
				vertexShaderSource.add("                                               ");
				vertexShaderSource.add("layout(location=0) in vec2 pos;                ");
				vertexShaderSource.add("layout(location=1) in vec2 tex;                ");
				vertexShaderSource.add("                                               ");
				vertexShaderSource.add("out vec2 texc;                                 ");
				vertexShaderSource.add("flat out uint arrindex;                             ");
				vertexShaderSource.add("                                               ");
				vertexShaderSource.add("void main(void) {                              ");
				vertexShaderSource.add("   texc = tex;                                 ");
				vertexShaderSource.add("   arrindex = int(gl_InstanceID) & 1;          ");
				vertexShaderSource.add("   vec2 s = vec2(0.05, 0.02) * gl_InstanceID;  ");
				vertexShaderSource.add("   vec2 p = pos + s;                           ");
				vertexShaderSource.add("   gl_Position = vec4(p, 0.0, 1.0);            ");
				vertexShaderSource.add("}                                              ");

				vertexShader = glCreateShader(GL_VERTEX_SHADER);
				glShaderSource(vertexShader, joinLines(vertexShaderSource));
				glCompileShader(vertexShader);
				if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
					throw new RuntimeException("Error creating vertex-shader: " + //
							glGetShaderInfoLog(vertexShader, glGetShaderi(vertexShader, GL_INFO_LOG_LENGTH)));
			}

			int fragmentShader;
			{
				List<String> fragmentShaderSource = new ArrayList<>();
				fragmentShaderSource.add("#version 330                                 ");
				fragmentShaderSource.add("                                             ");
				fragmentShaderSource.add("layout(binding=0) uniform sampler2DArray myarr;   ");
				fragmentShaderSource.add("                                             ");
				fragmentShaderSource.add("in vec2 texc;                                ");
				fragmentShaderSource.add("flat in uint arrindex;                            ");
				fragmentShaderSource.add("                                             ");
				fragmentShaderSource.add("layout(location=0) out vec4 frag;            ");
				fragmentShaderSource.add("                                             ");
				fragmentShaderSource.add("void main(void) {                            ");
				fragmentShaderSource.add("   vec3 coord = vec3(texc, arrindex);        ");
				fragmentShaderSource.add("   frag = texture(myarr, coord);             ");
				fragmentShaderSource.add("}                                            ");

				fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
				glShaderSource(fragmentShader, joinLines(fragmentShaderSource));
				glCompileShader(fragmentShader);
				if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE)
					throw new RuntimeException("Error creating fragment-shader: " + //
							glGetShaderInfoLog(fragmentShader, glGetShaderi(fragmentShader, GL_INFO_LOG_LENGTH)));
			}

			program = glCreateProgram();
			glAttachShader(program, vertexShader);
			glAttachShader(program, fragmentShader);
			glLinkProgram(program);

			if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
				throw new RuntimeException("Error creating shader-program: " + //
						glGetProgramInfoLog(fragmentShader, glGetProgrami(fragmentShader, GL_INFO_LOG_LENGTH)));
		}
		glUseProgram(program);

		MultiDrawElements mde = new MultiDrawElements(16);

		mde.glInit();

		{
			mde.vertexData = BufferUtils.createByteBuffer(100 << 2);
			mde.indexData = BufferUtils.createShortBuffer(100);
			mde.commandData = BufferUtils.createIntBuffer(100);

			mde.reset();

			{
				// tri 1
				{
					FloatBuffer vd = mde.vertexData.asFloatBuffer();
					vd.put(0.25f).put(0.25f).put(0.0f).put(0.0f);
					vd.put(0.75f).put(0.25f).put(1.0f).put(1.0f);
					vd.put(0.25f).put(0.75f).put(0.0f).put(1.0f);
					mde.vertexData.position(mde.vertexData.position() + (vd.position() << 2));

					mde.indexData.put((short) 0).put((short) 1).put((short) 2);

					mde.scheduleDrawCommand(5);
				}

				// tri 2 & 3
				{
					FloatBuffer vd = mde.vertexData.asFloatBuffer();
					vd.put(-0.50f).put(0.20f).put(0.0f).put(0.0f);
					vd.put(-0.50f).put(0.75f).put(1.0f).put(1.0f);
					vd.put(-0.25f).put(0.25f).put(0.0f).put(1.0f);
					vd.put(-0.50f).put(0.75f).put(0.5f).put(1.0f);
					vd.put(-0.00f).put(0.50f).put(0.0f).put(0.5f);
					mde.vertexData.position(mde.vertexData.position() + (vd.position() << 2));

					mde.indexData.put((short) 0).put((short) 1).put((short) 2);
					mde.indexData.put((short) 2).put((short) 3).put((short) 4);

					mde.scheduleDrawCommand(3);
				}

				// tri 4
				{
					FloatBuffer vd = mde.vertexData.asFloatBuffer();
					vd.put(-0.33f).put(-0.90f).put(1.0f).put(1.0f);
					vd.put(-0.33f).put(-0.15f).put(0.0f).put(0.0f);
					vd.put(-0.50f).put(-0.35f).put(1.0f).put(0.0f);
					mde.vertexData.position(mde.vertexData.position() + (vd.position() << 2));

					mde.indexData.put((short) 0).put((short) 1).put((short) 2);

					mde.scheduleDrawCommand(2);
				}
			}

			mde.glBindBuffers();
			mde.glUploadBufferData();

			long offset = 0L;

			int location, size;
			boolean normalized;

			location = 0;
			size = 2;
			normalized = false;
			glEnableVertexAttribArray(location);
			glVertexAttribPointer(location, size, GL_FLOAT, normalized, mde.stride, offset);
			offset += 8L;

			location = 1;
			size = 2;
			normalized = false;
			glEnableVertexAttribArray(location);
			glVertexAttribPointer(location, size, GL_FLOAT, normalized, mde.stride, offset);
			offset += 8L;

			if (offset != mde.stride)
				throw new IllegalStateException();
		}

		while (!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT);

			mde.glBindBuffers();
			mde.glMultiDraw();

			Display.update();
			Display.sync(60);
		}

		Display.destroy();
	}

	private static String joinLines(List<String> lines) {
		StringBuilder sb = new StringBuilder();
		for (String line : lines)
			sb.append(line).append("\r\n");
		String s = sb.toString();
		System.out.println(s);
		return s;
	}
}
