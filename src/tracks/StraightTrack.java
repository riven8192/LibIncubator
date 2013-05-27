package tracks;

import craterstudio.math.FastMath;
import craterstudio.math.Rotation;
import craterstudio.math.Vec2;

public class StraightTrack extends Track {
	public final Vec2 p1, p2;
	private final float angle;

	public StraightTrack(Vec2 p1, Vec2 p2) {
		this.p1 = p1;
		this.p2 = p2;

		this.length = this.length();
		this.angle = FastMath.atan2Deg(p2.y - p1.y, p2.x - p1.x);
	}

	public StraightTrack(Vec2 p, float angle, float distance) {
		this(new Vec2(p), Rotation.fromDegrees(angle).rotate(new Vec2(distance, 0)).add(p));
	}

	@Override
	float length() {
		return Vec2.distance(p1, p2);
	}

	@Override
	public void getPositionAtRatio(float ratio, Vec2 dst) {
		dst.x = p1.x + ratio * (p2.x - p1.x);
		dst.y = p1.y + ratio * (p2.y - p1.y);
	}

	@Override
	public float getAngleAtRatio(float ratio) {
		return this.angle;
	}

	@Override
	public Track parallel(float offset) {
		Vec2 off = Rotation.fromDegrees(angle).rotate(new Vec2(0, offset));
		StraightTrack that = new StraightTrack(new Vec2(p1).add(off), new Vec2(p2).add(off));
		return that;
	}
}
