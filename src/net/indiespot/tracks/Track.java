package net.indiespot.tracks;

import craterstudio.math.Vec2;

public abstract class Track {

	private static int ID_COUNTER;
	private final int id = ++ID_COUNTER;

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return this.id == ((Track) obj).id;
	}

	//

	public float length;
	public Track prev, next;

	public float getAngleAtStart() {
		return this.getAngleAtRatio(0.0f);
	}

	public float getAngleAtEnd() {
		return this.getAngleAtRatio(1.0f);
	}

	public Vec2 getPositionAtStart() {
		Vec2 dst = new Vec2();
		this.getPositionAtRatio(0.0f, dst);
		return dst;
	}

	public Vec2 getPositionAtEnd() {
		Vec2 dst = new Vec2();
		this.getPositionAtRatio(1.0f, dst);
		return dst;
	}

	abstract float length();

	abstract void getPositionAtRatio(float ratio, Vec2 dst);

	abstract float getAngleAtRatio(float ratio);

	public abstract Track parallel(float offset);

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "#" + id;
	}
}
