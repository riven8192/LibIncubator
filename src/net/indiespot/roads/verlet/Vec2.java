package net.indiespot.roads.verlet;

public class Vec2 {
	public float x, y;

	public Vec2() {
		this(0.0f, 0.0f);
	}

	public Vec2(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public static float distance(Vec2 a, Vec2 b) {
		float dx = a.x - b.x;
		float dy = a.y - b.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}
}
