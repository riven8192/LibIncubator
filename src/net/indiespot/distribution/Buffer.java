package net.indiespot.distribution;

public class Buffer extends Resource {
	public int maxInboundFlow = Integer.MAX_VALUE;

	public Buffer(int capacity) {
		super(0, capacity);
	}

	@Override
	public int demand() {
		return Math.min(maxInboundFlow, super.demand());
	}
}
