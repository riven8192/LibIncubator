package net.indiespot.distribution.energy;

import net.indiespot.distribution.Resource;
import net.indiespot.distribution.ResourceType;

public class Consumer extends Resource {
	public int startupLevel;
	public int consumption;
	private boolean isActive;

	public Consumer(ResourceType type, int startupLevel, int consumption, int capacity) {
		super(type, 0, capacity);
		if (startupLevel < 0)
			throw new IllegalArgumentException();
		if (consumption < 0)
			throw new IllegalArgumentException();
		this.startupLevel = startupLevel;
		this.consumption = consumption;
	}

	public boolean isActive() {
		return isActive;
	}

	public boolean tick() {
		if (!isActive) {
			isActive = this.amount() >= startupLevel;
		}

		if (isActive && this.demand(consumption) > 0) {
			isActive = false; // fully drained
		}
		return isActive;
	}

	@Override
	public String toString() {
		return super.toString() + "[-" + consumption + ", @" + startupLevel + ", active:" + isActive + "]";
	}
}
