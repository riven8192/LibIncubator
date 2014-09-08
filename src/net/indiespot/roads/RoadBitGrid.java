package net.indiespot.roads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoadBitGrid {
	public static void main(String[] args) {
		RoadBitGrid rg = new RoadBitGrid();

		if(!rg.canSet(0, 0))
			throw new IllegalStateException();

		rg.set(dx_8n[1], dy_8n[1]);
		rg.set(dx_8n[7], dy_8n[7]);
		rg.set(dx_8n[0], dy_8n[0]);
		if(!rg.canSet(0, 0))
			throw new IllegalStateException();
	}

	public static enum RoadType {
		STRAIGHT, INTERSECTION_PASS, CORNER, INTERSECTION, DEAD_END, NONE;

		private static RoadType[] values = RoadType.values();

		public static RoadType slowest(RoadType a, RoadType b) {
			return values[Math.max(a.ordinal(), b.ordinal())];
		}
	}

	public RoadBitGrid() {

	}

	private Set<Long> xy = new HashSet<>();

	private static final int[] dx_8n = new int[] { -1, -1, -1, 0, +1, +1, +1, 0 };
	private static final int[] dy_8n = new int[] { -1, 0, +1, +1, +1, 0, -1, -1 };

	public boolean canSet(int x, int y) {
		if(this.isSet(x, y))
			return false;
		int count = 0;
		for(int i = 0, j = 5; i < 8 + 3; i++, j++) {
			if(this.isSet(x + dx_8n[i % 8], y + dy_8n[i % 8]))
				count++;
			if(i > 2 && this.isSet(x + dx_8n[j % 8], y + dy_8n[j % 8]))
				count--;
			if(count == 3) // three consecutive neighboring tiles are set
				return false;
		}
		return true;
	}

	public void set(int x, int y) {
		if(!this.canSet(x, y))
			throw new IllegalStateException();
		xy.add(idxKey(x, y));
	}

	public boolean isSet(int x, int y) {
		return xy.contains(idxKey(x, y));
	}

	public RoadType getRoadType(int x, int y) {
		if(!this.isSet(x, y)) {
			return RoadType.NONE;
		}

		// count neighbors on each axis
		int xAxis = 0;
		int yAxis = 0;
		for(int d = -1; d <= 1; d += 2) {
			if(this.isSet(x + d, y))
				xAxis++;
			if(this.isSet(x, y + d))
				yAxis++;
		}

		RoadType roadType;
		if(xAxis + yAxis <= 1) // {0, {0,1}}
			roadType = RoadType.DEAD_END;
		else if(xAxis + yAxis >= 3) // {{1,2}, 2}
			roadType = RoadType.INTERSECTION;
		else if(xAxis == yAxis) // {1, 1}
			roadType = RoadType.CORNER;
		else if((xAxis ^ yAxis) == 2) // {0, 2}
			roadType = RoadType.STRAIGHT;
		else
			throw new IllegalStateException();

		return roadType;
	}

	private static long idx(int x, int y) {
		long xl = x & 0xFFFF_FFFFL;
		long yl = y & 0xFFFF_FFFFL;
		return (xl << 32) | yl;
	}

	public static Long idxKey(int x, int y) {
		return Long.valueOf(idx(x, y));
	}

	public List<int[]> nodes() {
		List<int[]> list = new ArrayList<>();

		for(Long tile : xy) {
			long val = tile.longValue();
			int x = (int) (val >>> 32);
			int y = (int) (val & 0xFFFF_FFFFL);
			list.add(new int[] { x, y });
		}

		return list;
	}
}
