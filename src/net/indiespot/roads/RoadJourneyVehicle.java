package net.indiespot.roads;

import craterstudio.math.Vec2;

public class RoadJourneyVehicle {
	public final RoadJourney journey;
	private final RoadJourneyStepper head;
	private int len, leaveRem;

	public RoadJourneyVehicle(RoadJourney journey, int len) {
		this.journey = journey;
		this.head = new RoadJourneyStepper(journey, true);
		this.len = this.leaveRem = len;
		this.tileHistory = new RoadTile[len];
		this.bitHistory = new int[len];
		this.historyIndex = -1;
	}

	public RoadJourneyStepper head() {
		return head;
	}

	public Vec2 getHistory(int age) {
		if (historyIndex == -1)
			return null;

		int idx = (historyIndex + tileHistory.length - age)
				% tileHistory.length;
		RoadTile tile = tileHistory[idx];
		if (tile == null)
			return null;

		int bit = bitHistory[idx];
		int x = RoadTile.bitToX(bit);
		int y = RoadTile.bitToY(bit);

		return new Vec2(//
				tile.x + 0.125f + x * 0.25f,//
				tile.y + 0.125f + y * 0.25f);
	}

	private RoadTile[] tileHistory;
	private int[] bitHistory;
	private int historyIndex;

	public float subTilesPerStep;
	private float subTilesTraversed;

	public RoadJourneyStepper.State step() {
		subTilesTraversed += subTilesPerStep;
		if (subTilesTraversed < 1.0f)
			return RoadJourneyStepper.State.SUB_MOVE;
		subTilesTraversed -= 1.0f;

		if (len == leaveRem) {
			switch (head.step()) {
			case MOVING:
				historyIndex = ++historyIndex % tileHistory.length;
				if (tileHistory[historyIndex] != null)
					tileHistory[historyIndex].leave(bitHistory[historyIndex]);
				tileHistory[historyIndex] = head.getTile();
				bitHistory[historyIndex] = head.getTileBit();

				break;
			case BLOCKED:
				return RoadJourneyStepper.State.BLOCKED;
			case ARRIVED:
				leaveRem--;
				break;
			case SUB_MOVE:
				throw new IllegalStateException();
			}
		} else if (--leaveRem < 0) {
			return RoadJourneyStepper.State.ARRIVED;
		}

		return RoadJourneyStepper.State.MOVING;
	}

	public void destroy() {
		for (int i = 0; i < tileHistory.length; i++) {
			if (tileHistory[i] != null) {
				tileHistory[i].leave(bitHistory[i]);
				tileHistory[i] = null;
			}
		}
		historyIndex = -1;
	}
}
