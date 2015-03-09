package net.indiespot.distribution;

import net.indiespot.path.Node;

public class Node2D extends Node {
	public final int x, y;

	public Node2D(int x, int y, Object attachment) {
		super(attachment);
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + x + ", " + y + "]";
	}
}
