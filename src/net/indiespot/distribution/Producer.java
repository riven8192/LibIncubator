package net.indiespot.distribution;

public class Producer extends Resource {
	public int startupLevel;
	public int production;
	private boolean isActive;

	public Producer(int startupLevel, int consumption, int capacity) {
		super(0, capacity);
		if (startupLevel < 0)
			throw new IllegalArgumentException();
		if (consumption < 0)
			throw new IllegalArgumentException();
		this.startupLevel = startupLevel;
		this.production = consumption;
	}

	public boolean isActive() {
		return isActive;
	}

	public boolean tick() {
		if (!isActive && this.amount() <= startupLevel) {
			isActive = true;
		}

		if (isActive && this.supply(production) != production) {
			isActive = false; // fully stuffed
		}

		return isActive;
	}

	@Override
	public String toString() {
		return super.toString() + "[+" + production + ", @" + startupLevel + ", active:" + isActive + "]";
	}
}
