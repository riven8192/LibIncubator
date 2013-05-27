package tracks;

import craterstudio.math.Rotation;
import craterstudio.math.Vec2;

public class CurvedTrack extends Track {
	public Vec2 origin;
	public float radius;
	public float startAngle, curveAngle;

	private CurvedTrack() {
		//
	}

	public CurvedTrack(Vec2 p, float radius, float startAngle, float curveAngle) {
		this.radius = radius;
		this.startAngle = startAngle;
		this.curveAngle = curveAngle;

		this.length = this.length();

		Rotation rot = Rotation.fromDegrees(startAngle - (curveAngle >= 0 ? +90 : -90));
		this.origin = rot.rotate(new Vec2(-radius, 0)).add(p);
	}

	@Override
	float length() {
		return (float) (Math.abs(curveAngle) / 360.0 * radius * (Math.PI * 2.0));
	}

	private final Rotation rot = new Rotation();

	@Override
	public void getPositionAtRatio(float ratio, Vec2 dst) {
		float angle = startAngle + ratio * curveAngle - (curveAngle >= 0 ? +90 : -90);
		rot.setFromDegrees(angle).rotate(dst.load(radius, 0)).add(origin);
	}

	@Override
	public float getAngleAtRatio(float ratio) {
		return startAngle + ratio * curveAngle;
	}

	@Override
	public Track parallel(float offset) {
		CurvedTrack that = new CurvedTrack();
		that.origin = new Vec2(this.origin);
		that.radius = this.radius - (this.curveAngle >= 0 ? +offset : -offset);
		that.startAngle = this.startAngle;
		that.curveAngle = this.curveAngle;
		that.length = that.length();
		return that;
	}
}
