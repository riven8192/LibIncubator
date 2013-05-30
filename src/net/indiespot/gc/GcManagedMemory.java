package net.indiespot.gc;

import java.util.Arrays;

public class GcManagedMemory {

	private final GcIntList[] free = new GcIntList[32];
	private final GcIntList[] used = new GcIntList[32];

	private final GcMemory memory;
	private final int addressOff, addressEnd;
	private int addressAt;

	public GcManagedMemory(GcMemory memory, int off, int len) {
		this.memory = memory;
		this.addressOff = off;
		this.addressEnd = off + len;
		this.addressAt = off;

		for (int i = 0; i < free.length; i++) {
			this.free[i] = new GcIntList();
			this.used[i] = new GcIntList();
		}
	}

	private static final boolean store_pot_index_for_fast_free = true;

	public int malloc(int sizeof) {
		if (store_pot_index_for_fast_free) {
			sizeof += 1;
		}
		int pot = roundUpToPowerOfTwo(sizeof);
		int index = log2(pot);

		int pointer;
		if (this.free[index].size() > 0) {
			pointer = this.free[index].removeLast();
		} else {
			if (this.addressAt + pot > this.addressEnd) {
				throw new OutOfMemoryError();
			} else {
				pointer = this.addressAt;
				this.addressAt += pot;
			}
		}

		if (store_pot_index_for_fast_free) {
			memory.data[pointer] = index;
			pointer += 1;
		}
		this.used[index].add(pointer);
		return pointer;
	}

	public int malloc(GcClass clazz, int arrayLength) {
		if (!clazz.isArray()) {
			throw new IllegalStateException();
		}
		int pointer = this.malloc(clazz.sizeof + arrayLength);
		GcObject.setClass(memory, pointer, clazz);
		GcObject.setGcFlag(memory, pointer, 0);
		GcObject.setForwardPointer(memory, pointer, 0);
		GcArray.setLength(memory, pointer, arrayLength);
		return pointer;
	}

	public void free(int pointer) {
		if (store_pot_index_for_fast_free) {
			int pot = memory.data[pointer - 1];
			for (int k = this.used[pot].size() - 1; k >= 0; k--) {
				if (this.used[pot].get(k) == pointer) {
					this.used[pot].remove(k);
					this.free[pot].add(pointer);
					return;
				}
			}
		} else {
			for (int pot = 0; pot < this.used.length; pot++) {
				for (int k = this.used[pot].size() - 1; k >= 0; k--) {
					if (this.used[pot].get(k) == pointer) {
						this.used[pot].remove(k);
						this.free[pot].add(pointer);
						return;
					}
				}
			}
		}

		throw new IllegalStateException("pointer not in use");
	}

	public void tidy() {
		outer: {
			// is everything freed?
			for (int pot = 0; pot < this.free.length; pot++) {
				if (this.used[pot].size() != 0) {
					break outer;
				}
			}

			// discard administration
			for (int pot = 0; pot < this.free.length; pot++) {
				this.used[pot].clear();
			}

			//
			this.addressAt = this.addressOff;
			System.out.println("easy peesy");
			return;
		}

		boolean[] freeIndices = new boolean[addressEnd - addressOff];

		for (int pot = 0; pot < this.free.length; pot++) {
			for (int k = this.free[pot].size() - 1; k >= 0; k--) {
				int pointer = this.free[pot].get(k);
				int sizeof = 1 << pot;
				if (store_pot_index_for_fast_free) {
					pointer -= 1;
				}
				for (int i = 0; i < sizeof; i++) {
					freeIndices[pointer + i - addressOff] = true;
				}
			}
		}

		System.out.println(Arrays.toString(freeIndices));
	}

	public void gc(int gcFlag) {
		for (int pot = 0; pot < this.used.length; pot++) {
			for (int k = this.used[pot].size() - 1; k >= 0; k--) {
				if (gcFlag != GcObject.getGcFlag(this.memory, this.used[pot].get(k))) {
					this.free[pot].add(this.used[pot].remove(k));
				}
			}
		}
	}

	public static int roundUpToPowerOfTwo(int v) {
		if (v <= 0) {
			throw new IllegalStateException("invalid value: " + v);
		}

		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		return v + 1;
	}

	public static int log2(int v) {
		for (int i = 31; i >= 0; i--) {
			if (1 << i == v) {
				return i;
			}
		}
		return -1;
	}
}