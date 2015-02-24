package net.indiespot.electronflow;

public class Generator extends Node {
	public int production;

	public void generate() {
		this.blocked += Math.max(0, energy + production - capacity);
		energy = production;
		capacity = production;
	}
}
