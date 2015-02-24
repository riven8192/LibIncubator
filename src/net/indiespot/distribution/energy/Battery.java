package net.indiespot.distribution.energy;

import net.indiespot.distribution.Resource;
import net.indiespot.distribution.ResourceType;

public class Battery extends Resource {
	public int maxInboundFlow = Integer.MAX_VALUE;

	public Battery(int capacity) {
		super(ResourceType.ELECTRICITY, 0, capacity);
	}

	@Override
	public int demand() {
		return Math.min(maxInboundFlow, super.demand());
	}
}
