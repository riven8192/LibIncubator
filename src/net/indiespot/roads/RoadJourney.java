package net.indiespot.roads;

import java.util.ArrayList;
import java.util.List;

import net.indiespot.roads.RoadTile.Dir;

public class RoadJourney {
	final List<RoadTile> tiles;
	final List<int[]> tileBits0;
	final List<int[]> tileBits1;

	public RoadJourney(List<RoadTile> tiles) {
		this.tiles = tiles;

		List<Dir> dirs = new ArrayList<>();
		for(int i = 1; i < tiles.size(); i++) {
			RoadTile prev = tiles.get(i - 1);
			RoadTile curr = tiles.get(i - 0);

			int dx = curr.x - prev.x;
			int dy = curr.y - prev.y;
			if(dx == 0 && dy == 0)
				throw new IllegalStateException();
			if(dx != 0 && dy != 0)
				throw new IllegalStateException("dx=" + dx + ", dy=" + dy);
			if(Math.abs(dx) > 1 || Math.abs(dy) > 1)
				throw new IllegalStateException("dx=" + dx + ", dy=" + dy);

			Dir dir;
			if(dx == 0)
				dir = (dy > 0) ? Dir.SOUTH : Dir.NORTH;
			else
				dir = (dx > 0) ? Dir.EAST : Dir.WEST;

			if(i == 1 || i == tiles.size() - 1)
				dirs.add(dir);
			dirs.add(dir);
		}

		tileBits0 = new ArrayList<>();
		tileBits1 = new ArrayList<>();
		for(int i = 1; i < dirs.size(); i++) {
			Dir prev = dirs.get(i - 1);
			Dir curr = dirs.get(i - 0);
			tileBits0.add(RoadTile.getBits(0, prev, curr));
			tileBits1.add(RoadTile.getBits(1, prev, curr));
		}
	}
}
