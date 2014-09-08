package net.indiespot.roads;

import java.util.HashMap;
import java.util.Map;

public class RoadTileGrid {
	private final Map<Long, RoadTile> xy2tile;

	public RoadTileGrid() {
		xy2tile = new HashMap<>();
	}

	public void put(RoadTile tile) {
		Long xy = RoadBitGrid.idxKey(tile.x, tile.y);
		if(xy2tile.containsKey(xy))
			throw new IllegalStateException();
		xy2tile.put(xy, tile);
	}

	public RoadTile get(int x, int y) {
		Long xy = RoadBitGrid.idxKey(x, y);
		return xy2tile.get(xy);
	}

	public RoadTile remove(int x, int y) {
		Long xy = RoadBitGrid.idxKey(x, y);
		if(!xy2tile.containsKey(xy))
			throw new IllegalStateException();
		return xy2tile.remove(xy);
	}
}
