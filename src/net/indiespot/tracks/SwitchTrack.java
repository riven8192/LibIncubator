package net.indiespot.tracks;

import java.util.ArrayList;
import java.util.List;

import craterstudio.math.Rotation;
import craterstudio.math.Vec2;

public class SwitchTrack extends Track {
	public final Vec2 pos;
	public final float angle;

	public SwitchTrack(Vec2 pos, float angle) {
		this.pos = new Vec2(pos);
		this.angle = angle;
	}

	public List<Track> tracks = new ArrayList<>();

	public void add(Track track) {
		tracks.add(track);
	}

	public void setSwitch(int index) {
		if (!tracks.contains(next)) {
			throw new IllegalStateException();
		}
		next = tracks.get(index);
	}

	@Override
	float length() {
		return 0.0f;
	}

	@Override
	public float getAngleAtRatio(float ratio) {
		return angle;
	}

	@Override
	public void getPositionAtRatio(float ratio, Vec2 dst) {
		dst.load(pos);
	}

	@Override
	public Track parallel(float offset) {
		Vec2 off = Rotation.fromDegrees(angle).rotate(new Vec2(0, offset));
		SwitchTrack that = new SwitchTrack(pos.add(off), angle);
		return that;
	}
}
