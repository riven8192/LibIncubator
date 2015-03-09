package net.indiespot.distribution;

import net.indiespot.diff.Id;

public class Resource extends Id {
	private int amount;
	private int capacity;

	public Resource(int amount, int capacity) {
		if (amount < 0)
			throw new IllegalArgumentException();
		if (capacity <= 0)
			throw new IllegalArgumentException();
		if (capacity < amount)
			throw new IllegalArgumentException();
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

	public void shrink(int value) {
		if (value < 0)
			throw new IllegalArgumentException();
		if (value > capacity)
			throw new IllegalArgumentException();

		if (capacity - value < amount) {
			amount = capacity - value;
		}

		capacity -= value;
	}

	public void expand(int value) {
		if (value < 0)
			throw new IllegalArgumentException();
		capacity += value;
	}

	//

	public int supply() {
		return amount;
	}

	public int demand() {
		return capacity - amount;
	}

	public int supply(int val) {
		if (val < 0)
			throw new IllegalArgumentException();
		int txn = Math.min(val, this.demand());
		if (txn < 0 || amount + txn > capacity)
			throw new IllegalStateException();
		amount += txn;
		return txn;
	}

	public int demand(int val) {
		if (val < 0)
			throw new IllegalArgumentException();
		int txn = Math.min(val, this.supply());
		if (txn < 0 || amount - txn < 0)
			throw new IllegalStateException();
		amount -= txn;
		return txn;
	}

	public static int txn(Resource src, Resource dst) {
		return txn(src, Integer.MAX_VALUE, dst);
	}

	public static int txn(Resource src, int cap, Resource dst) {
		int supply = src.supply();
		int demand = dst.demand();
		if ((supply | demand) < 0)
			throw new IllegalArgumentException();
		if ((supply | demand) == 0)
			return 0;

		int txn = Math.min(cap, Math.min(supply, demand));
		if (src.demand(txn) != txn)
			throw new IllegalStateException();
		if (dst.supply(txn) != txn)
			throw new IllegalStateException();
		return txn;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + amount + "/" + capacity + "]";
	}
}
