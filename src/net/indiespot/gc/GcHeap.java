package net.indiespot.gc;

public class GcHeap {
	public final GcMemory memory;
	public final int heapIndex, address, space;
	private int used;

	public GcHeap(GcMemory memory, int heapIndex, int address, int space) {
		this.memory = memory;
		this.heapIndex = heapIndex;
		this.address = address;
		this.space = space;
	}

	public boolean isEmpty() {
		return this.used == 0;
	}

	public int malloc(int sizeof) {
		int end = this.used + sizeof;
		if (end > this.space) {
			throw new OutOfMemoryError();
		}
		int pointer = this.address + this.used;
		this.used = end;
		return pointer;
	}

	public int malloc(GcClass clazz) {
		if (clazz.isArray()) {
			throw new IllegalStateException();
		}
		int pointer = this.malloc(clazz.sizeof);
		GcObject.setClass(memory, pointer, clazz);
		GcObject.setGcFlag(memory, pointer, 0);
		GcObject.setForwardPointer(memory, pointer, 0);
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

	public void free() {
		used = 0;
	}

	public int used() {
		return used;
	}

	@Override
	public String toString() {
		if (this.isEmpty()) {
			return "GcHeap[" + heapIndex + ", empty]";
		}
		return "GcHeap[" + heapIndex + ", used=" + used + "/" + space + "]";
	}
}
