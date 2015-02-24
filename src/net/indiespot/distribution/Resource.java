package net.indiespot.distribution;

public class Resource {
	public final ResourceType type;
	private int amount;
	private int capacity;

	public Resource(ResourceType type, int amount, int capacity) {
		if (amount < 0)
			throw new IllegalArgumentException();
		if (capacity <= 0)
			throw new IllegalArgumentException();
		if (capacity < amount)
			throw new IllegalArgumentException();
		this.type = type;
		this.amount = amount;
		this.capacity = capacity;
	}

	public boolean isEmpty() {
		return amount == 0;
	}

	public boolean isFull() {
		return amount == capacity;
	}

	//

	public int amount() {
		return amount;
	}

	public int capacity() {
		return capacity;
	}

	//

	public int supply() {
		return amount;
	}

	public int demand() {
		return capacity - amount;
	}

	// returns the amount left of 'val'
	public int supply(int val) {
		if (val < 0)
			throw new IllegalArgumentException();
		int txn = Math.min(val, this.demand());
		amount += txn;
		return val - txn;
	}

	// returns the amount left of 'val'
	public int demand(int val) {
		if (val < 0)
			throw new IllegalArgumentException();
		int txn = Math.min(val, this.supply());
		amount -= txn;
		return val - txn;
	}

	public static int txn(Resource src, Resource dst) {
		return txn(src, Integer.MAX_VALUE, dst);
	}

	public static int txn(Resource src, int cap, Resource dst) {
		if (src.type != dst.type)
			throw new IllegalStateException();

		int supply = src.supply();
		int demand = dst.demand();
		if ((supply | demand) == 0)
			return 0;

		int txn = Math.min(cap, Math.min(supply, demand));
		if (src.demand(txn) != 0)
			throw new IllegalStateException();
		if (dst.supply(txn) != 0)
			throw new IllegalStateException();
		return txn;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + type + ", " + amount + "/" + capacity + "]";
	}
}
