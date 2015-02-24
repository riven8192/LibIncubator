#version 430

layout(location = 0) in vec4 aVec;

void main() {
	gl_Position = aVec;
}