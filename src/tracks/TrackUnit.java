package tracks;

import craterstudio.math.Vec2;

public class TrackUnit {
	private Track currentTrack;
	private float traveledOnTrack;

	Vec2 pos = new Vec2();
	float angle;
	boolean forward = true;

	public TrackUnit(Track track) {
		this.currentTrack = track;
	}

	public void move(float toMove) {
		if (toMove < 0.0f) {
			this.forward ^= true;
			this.move(-toMove);
			this.forward ^= true;
			return;
		}

		while (toMove != 0.0f) {
			if (forward) {
				if (traveledOnTrack + toMove > currentTrack.length) {
					toMove = (traveledOnTrack + toMove) - currentTrack.length;

					if (currentTrack.next == null) {
						forward = false;
					} else {
						currentTrack = currentTrack.next;
						traveledOnTrack = 0.0f;
					}

				} else {
					traveledOnTrack += toMove;
					toMove = 0.0f;
				}
			} else {
				if (traveledOnTrack < toMove) {
					toMove -= traveledOnTrack;

					if (currentTrack.prev == null) {
						forward = true;
					} else {
						currentTrack = currentTrack.prev;
						traveledOnTrack = currentTrack.length;
					}
				} else {
					traveledOnTrack -= toMove;
					toMove = 0.0f;
				}
			}
		}

		float ratio = traveledOnTrack / currentTrack.length;
		currentTrack.getPositionAtRatio(ratio, pos);
		angle = currentTrack.getAngleAtRatio(ratio);
	}
}
