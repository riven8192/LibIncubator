package tracks;

import java.util.Set;

import craterstudio.math.Vec2;

public class TrackBuilder {
	private Vec2 lastPos;
	private float lastAngle;
	private Track firstTrack, lastTrack;

	public void init(Vec2 pos, float angle) {
		this.lastPos = new Vec2(pos);
		this.lastAngle = angle;
	}

	public void init(Track track) {
		lastTrack = track;
		lastPos = lastTrack.getPositionAtEnd();
		lastAngle = lastTrack.getAngleAtEnd();
	}

	public TrackBuilder addStraight(float distance) {
		Track track = new StraightTrack(new Vec2(lastPos), lastAngle, distance);
		this.tie(track);
		return this;
	}

	public TrackBuilder addCurve(float radius, float curveAngle) {
		Track track = new CurvedTrack(new Vec2(lastPos), radius, lastAngle, curveAngle);
		this.tie(track);
		return this;
	}

	public SwitchTrack addSwitch() {
		SwitchTrack track = new SwitchTrack(new Vec2(lastPos), lastAngle);
		this.tie(track);
		return track;
	}

	private void tie(Track track) {
		if (lastTrack != null) {
			if (lastTrack instanceof SwitchTrack) {
				((SwitchTrack) lastTrack).add(track);
			}

			lastTrack.next = track;
			track.prev = lastTrack;
		}
		if (firstTrack == null) {
			firstTrack = track;
		}
		lastTrack = track;
		lastPos = lastTrack.getPositionAtEnd();
		lastAngle = lastTrack.getAngleAtEnd();
	}

	public Track build() {
		float dist = Vec2.distance(lastTrack.getPositionAtEnd(), firstTrack.getPositionAtStart());
		if (dist < 5) {
			lastTrack.next = firstTrack;
			firstTrack.prev = lastTrack;
		}
		return firstTrack;
	}

	public static Track buildParallel(Track firstTrack, float offset, Set<Track> visited) {
		Track orig = firstTrack;
		Track copy = orig.parallel(offset);

		final Track orig1 = orig;
		final Track copy1 = copy;

		do {
			if (visited.add(orig)) {
				// System.out.println("eh?");
				if (orig instanceof SwitchTrack) {
					for (Track track : ((SwitchTrack) orig).tracks) {
						((SwitchTrack) copy).add(buildParallel(track, offset, visited));
					}
				}
			}

			orig = orig.next;
			if (orig == null) {
				break;
			}
			if (orig == orig1) {
				copy.next = copy1;
				break;
			}
			copy.next = orig.parallel(offset);
			copy = copy.next;
		} while (true);

		for (Track curr = copy; curr != null && (curr.next == null || curr.next.prev == null);) {
			Track prev = curr;
			curr = curr.next;
			if (curr != null) {
				curr.prev = prev;
			}
		}

		return copy1;
	}
}
