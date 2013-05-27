package tracks;

import craterstudio.func.Callback;
import craterstudio.math.Vec2;

public class TrackUnitPair {
	public TrackUnit head, tail;
	public final float length;
	public TrackUnitPair next;

	public TrackUnitPair(TrackUnit head, TrackUnit tail, float length) {
		this.head = head;
		this.tail = tail;
		this.length = length;
	}

	public void visitUnits(Callback<TrackUnit> callback) {
		callback.callback(head);
		for (TrackUnitPair conn = this; conn != null; conn = conn.next) {
			callback.callback(conn.tail);
		}
	}

	public boolean getDirection() {
		return head.forward;
	}

	public void setDirection(final boolean forward) {
		this.visitUnits(new Callback<TrackUnit>() {
			@Override
			public void callback(TrackUnit unit) {
				unit.forward = forward;
			}
		});
	}

	public void setPosition(final float toMove) {
		this.visitUnits(new Callback<TrackUnit>() {
			@Override
			public void callback(TrackUnit unit) {
				unit.move(toMove);
			}
		});
	}

	public void move(float toMove) {
		boolean x1 = head.forward;
		this.move(toMove, true);
		boolean x2 = head.forward;

		if (x1 != x2) {
			System.out.println("swap direction");
			this.setDirection(x2);
		}
	}

	private void move(float toMove, boolean moveHead) {
		if (moveHead) {
			head.move(toMove);
		}
		tail.move(toMove);

		final float errorMargin = 2.0f;
		final float minLength = length - errorMargin;
		final float maxLength = length + errorMargin;

		float currLength2 = Vec2.distanceSquared(head.pos, tail.pos);
		if (currLength2 < minLength * minLength || //
		   currLength2 > maxLength * maxLength) {

			// mind the accuracy!
			tail.move(-0.5f);
			float testLength2 = Vec2.distanceSquared(head.pos, tail.pos);
			tail.move(+0.5f);

			float adjustFactor = 1.5f;
			float adjust;
			if (currLength2 < minLength * minLength) {
				adjust = adjustFactor * errorMargin * (testLength2 > currLength2 ? -1 : +1);
			} else {
				adjust = adjustFactor * errorMargin * (testLength2 < currLength2 ? -1 : +1);
			}

			if (next != null) {
				next.move(adjust, false);
			}
			tail.move(adjust);
		}

		if (next != null) {
			next.move(toMove, false);
		}
	}

	public void verify() {
		if (next != null) {
			if (next.head != tail) {
				throw new IllegalStateException();
			}
		}
	}
}
