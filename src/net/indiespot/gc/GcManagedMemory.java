package net.indiespot.gc;

public class GcManagedMemory {

	private final GcIntList[] free = new GcIntList[32];
	private final GcIntList[] used = new GcIntList[32];

	private final GcMemory memory;
	private int addressOff, addressEnd;

	public GcManagedMemory(GcMemory memory, int off, int len) {
		this.memory = memory;
		this.addressOff = off;
		this.addressEnd = off + len;

		for (int i = 0; i < free.length; i++) {
			this.free[i] = new GcIntList();
			this.used[i] = new GcIntList();
		}
	}

	public int malloc(int sizeof) {
		int pot = roundUpToPowerOfTwo(sizeof);
		int index = log2(pot);

		int pointer;
		if (this.free[index].size() == 0) {
			if (this.addressOff + pot > this.addressEnd) {
				throw new OutOfMemoryError();
			}
			pointer = this.addressOff;
			this.addressOff += pot;
		} else {
			pointer = this.free[index].removeLast();
		}

		this.used[index].add(pointer);
		return pointer;
	}

	public int malloc(GcClass clazz, int arrayLength) {
		if (!clazz.isArray) {
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
		for (int pot = 0; pot < this.used.length; pot++) {
			for (int k = this.used[pot].size() - 1; k >= 0; k--) {
				if (this.used[pot].get(k) == pointer) {
					this.free[pot].add(this.used[pot].remove(k));
					return;
				}
			}
		}

		throw new IllegalStateException("pointer not in use");
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