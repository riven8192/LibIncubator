package net.indiespot.distribution.energy;

import net.indiespot.path.Node;

public class Pole extends Node {
	public final int x, y;

	public Pole(int x, int y, Object attachment) {
		super(attachment);
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "Pole[" + x + "," + y + "]";
	}
}
