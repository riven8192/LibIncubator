package net.indiespot.distribution;

public class Consumer extends Resource {
	public int startupLevel;
	public int consumption;
	private boolean isActive;

	public Consumer(int startupLevel, int consumption, int capacity) {
		super(0, capacity);
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
		if (!isActive && this.amount() >= startupLevel) {
			isActive = true;
		}

		if (isActive && this.demand(consumption) != consumption) {
			isActive = false; // fully drained
		}

		return isActive;
	}

	@Override
	public String toString() {
		return super.toString() + "[-" + consumption + ", @" + startupLevel + ", active:" + isActive + "]";
	}
}
