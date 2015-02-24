package net.indiespot.turning;

public class Rot2D {
	public static Rot2D fromDegrees(double angle) {
		return fromRadians(Math.toRadians(angle));
	}

	public static Rot2D fromRadians(double angle) {
		return new Rot2D(Math.cos(angle), Math.sin(angle));
	}

	public static Rot2D fromVector(double dx, double dy) {
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		return new Rot2D(dx / length, dy / length);
	}

	public double cos, sin;

	private Rot2D(double cos, double sin) {
		this.cos = cos;
		this.sin = sin;
	}

	public Rot2D load(Rot2D that) {
		this.cos = that.cos;
		this.sin = that.sin;

		return this;
	}

	public Rot2D copy() {
		return new Rot2D(cos, sin);
	}

	public Rot2D rotate(Rot2D that) {
		double cos = (this.cos * that.cos) - (this.sin * that.sin);
		double sin = (this.cos * that.sin) + (this.sin * that.cos);

		this.cos = cos;
		this.sin = sin;

		return this;
	}

	public static double cross(Rot2D a, Rot2D b) {
		return (a.cos * b.sin) - (a.sin * b.cos);
	}

	public static boolean isCCW(Rot2D a, Rot2D b) {
		return cross(a, b) < 0.0f;
	}
}