package net.indiespot.roads;

public class RoadTile {
	public static enum Dir {
		NORTH, EAST, SOUTH, WEST
	}

	public final int x, y;

	public RoadTile(int x, int y) {
		this.x = x;
		this.y = y;
	}

	private int usage = 0x0000_0000;

	//

	public boolean isEnterAllowed(int bit) {
		verifyBit(bit);
		bit += 16;

		return (usage & (1 << bit)) == 0;
	}

	public void setEnterAllowed(int bit, boolean allowEnter) {
		verifyBit(bit);
		bit += 16;

		if(allowEnter)
			usage &= ~(1 << bit);
		else
			usage |= (1 << bit);
	}

	//

	public boolean isEmpty(int bit) {
		verifyBit(bit);
		return (usage & (1 << bit)) == 0;
	}

	public void enter(int bit) {
		verifyBit(bit);
		if(!this.isEmpty(bit))
			throw new IllegalStateException("not empty");
		if(!this.isEnterAllowed(bit))
			throw new IllegalStateException("not allowed to enter");
		usage |= (1 << bit);
	}

	public void leave(int bit) {
		verifyBit(bit);
		if(this.isEmpty(bit))
			throw new IllegalStateException("already empty");
		usage &= ~(1 << bit);
	}

	//

	public static int[] getBits(int lane, Dir a, Dir b) {
		return DIRDIR_CELLS[lane][a.ordinal()][b.ordinal()];
	}

	public static int coordsToBit(int x, int y) {
		if(x < 0 || x >= 4)
			throw new IllegalStateException();
		if(y < 0 || y >= 4)
			throw new IllegalStateException();
		return (y << 2) + x;
	}

	public static int bitToX(int bit) {
		verifyBit(bit);
		return bit & 3;
	}

	public static int bitToY(int bit) {
		verifyBit(bit);
		return bit >> 2;
	}

	private static void verifyBit(int bit) {
		if(bit < 0 || bit >= 16)
			throw new IllegalArgumentException();
	}

	private static int[][][][] DIRDIR_CELLS = new int[4][Dir.values().length][Dir.values().length][];
	static {
		// 0 deg
		DIRDIR_CELLS[0][Dir.NORTH.ordinal()][Dir.NORTH.ordinal()] = new int[] { 15, 11, 7, 3 };
		DIRDIR_CELLS[0][Dir.EAST.ordinal()][Dir.EAST.ordinal()] = new int[] { 12, 13, 14, 15 };
		DIRDIR_CELLS[0][Dir.SOUTH.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 0, 4, 8, 12 };
		DIRDIR_CELLS[0][Dir.WEST.ordinal()][Dir.WEST.ordinal()] = new int[] { 3, 2, 1, 0 };
		//
		DIRDIR_CELLS[1][Dir.NORTH.ordinal()][Dir.NORTH.ordinal()] = new int[] { 14, 10, 6, 2 };
		DIRDIR_CELLS[1][Dir.EAST.ordinal()][Dir.EAST.ordinal()] = new int[] { 8, 9, 10, 11 };
		DIRDIR_CELLS[1][Dir.SOUTH.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 1, 5, 9, 13 };
		DIRDIR_CELLS[1][Dir.WEST.ordinal()][Dir.WEST.ordinal()] = new int[] { 7, 6, 5, 4 };

		// 90 deg
		DIRDIR_CELLS[0][Dir.NORTH.ordinal()][Dir.EAST.ordinal()] = new int[] { 15 };
		DIRDIR_CELLS[0][Dir.NORTH.ordinal()][Dir.WEST.ordinal()] = new int[] { 15, 11, 7, 3, 2, 1, 0 };
		DIRDIR_CELLS[0][Dir.SOUTH.ordinal()][Dir.EAST.ordinal()] = new int[] { 0, 4, 8, 12, 13, 14, 15 };
		DIRDIR_CELLS[0][Dir.SOUTH.ordinal()][Dir.WEST.ordinal()] = new int[] { 0 };
		DIRDIR_CELLS[0][Dir.EAST.ordinal()][Dir.NORTH.ordinal()] = new int[] { 12, 13, 14, 15, 11, 7, 3 };
		DIRDIR_CELLS[0][Dir.EAST.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 12 };
		DIRDIR_CELLS[0][Dir.WEST.ordinal()][Dir.NORTH.ordinal()] = new int[] { 3 };
		DIRDIR_CELLS[0][Dir.WEST.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 3, 2, 1, 0, 4, 8, 12 };
		//
		DIRDIR_CELLS[1][Dir.NORTH.ordinal()][Dir.EAST.ordinal()] = new int[] { 14, 10, 11 };
		DIRDIR_CELLS[1][Dir.NORTH.ordinal()][Dir.WEST.ordinal()] = new int[] { 14, 10, 6, 5, 4 };
		DIRDIR_CELLS[1][Dir.SOUTH.ordinal()][Dir.EAST.ordinal()] = new int[] { 1, 5, 9, 10, 11 };
		DIRDIR_CELLS[1][Dir.SOUTH.ordinal()][Dir.WEST.ordinal()] = new int[] { 1, 5, 4 };
		DIRDIR_CELLS[1][Dir.EAST.ordinal()][Dir.NORTH.ordinal()] = new int[] { 8, 9, 10, 6, 2 };
		DIRDIR_CELLS[1][Dir.EAST.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 8, 9, 13 };
		DIRDIR_CELLS[1][Dir.WEST.ordinal()][Dir.NORTH.ordinal()] = new int[] { 7, 6, 2 };
		DIRDIR_CELLS[1][Dir.WEST.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 7, 6, 5, 9, 13 };

		// 180 deg
		DIRDIR_CELLS[0][Dir.WEST.ordinal()][Dir.EAST.ordinal()] = new int[] { 3, 2, 1, 0, 4, 8, 12, 13, 14, 15 };
		DIRDIR_CELLS[0][Dir.EAST.ordinal()][Dir.WEST.ordinal()] = new int[] { 12, 13, 14, 15, 11, 7, 3, 2, 1, 0 };
		DIRDIR_CELLS[0][Dir.NORTH.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 15, 11, 7, 3, 2, 1, 0, 4, 8, 12 };
		DIRDIR_CELLS[0][Dir.SOUTH.ordinal()][Dir.NORTH.ordinal()] = new int[] { 0, 4, 8, 12, 13, 14, 15, 11, 7, 3 };
		//
		DIRDIR_CELLS[1][Dir.WEST.ordinal()][Dir.EAST.ordinal()] = new int[] { 7, 6, 5, 9, 10, 11 };
		DIRDIR_CELLS[1][Dir.EAST.ordinal()][Dir.WEST.ordinal()] = new int[] { 8, 9, 10, 6, 5, 4 };
		DIRDIR_CELLS[1][Dir.NORTH.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 14, 10, 6, 5, 9, 13 };
		DIRDIR_CELLS[1][Dir.SOUTH.ordinal()][Dir.NORTH.ordinal()] = new int[] { 1, 5, 9, 10, 6, 2 };
	}
}