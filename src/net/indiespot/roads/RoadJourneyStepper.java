package net.indiespot.roads;

import java.util.List;

public class RoadJourneyStepper {
	public static enum State {
		MOVING, SUB_MOVE, BLOCKED, ARRIVED;
	}

	public final RoadJourney journey;
	private List<int[]> laneBits;

	private int tileIndex;
	private int tileCell;
	private final boolean doEnter;
	private int moveCounter;
	private int lane;

	public RoadJourneyStepper(RoadJourney journey, boolean doEnter) {
		this.journey = journey;
		this.doEnter = doEnter;
		this.tileCell = -1;
		this.lane = 0;
		this.laneBits = journey.tileBits0;
	}

	public void switchLanes() {
		if(laneBits == journey.tileBits0)
			laneBits = journey.tileBits1;
		else
			laneBits = journey.tileBits0;
	}

	public RoadTile getTile() {
		return journey.tiles.get(tileIndex);
	}

	public int getTileBit() {
		int[] bits = laneBits.get(tileIndex);
		return tileCell == -1 ? -1 : bits[tileCell];
	}

	public int counter() {
		return moveCounter;
	}

	public State step() {
		int[] bits = laneBits.get(tileIndex);
		if(tileCell == bits.length - 1) {
			if(++tileIndex == journey.tiles.size())
				if(true)
					tileIndex = 0;
				else
					return State.ARRIVED;
			bits = laneBits.get(tileIndex);
			tileCell = -1;
		}

		RoadTile tile = journey.tiles.get(tileIndex);
		int nextCell = bits[tileCell + 1];
		if(doEnter) {
			if(!tile.isEmpty(nextCell))
				return State.BLOCKED;
			if(!tile.isEnterAllowed(nextCell))
				return State.BLOCKED;
			tile.enter(nextCell);
		}
		else {
			tile.leave(nextCell);
		}
		tileCell++;
		moveCounter++;
		return State.MOVING;
	}
}