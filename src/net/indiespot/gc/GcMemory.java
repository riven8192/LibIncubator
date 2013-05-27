package net.indiespot.gc;

public class GcMemory {
	public final int[] data;
	private int nextAddress;
	private int heapCounter;
	public final int heapSize;

	public GcMemory(int memorySize, int heapSize) {
		this.nextAddress = 0;
		this.heapSize = heapSize;
		this.data = new int[memorySize];

		GcHeap immortal = this.createHeap();

		// for SEG_FAULTS on 0x0000
		immortal.malloc(1);

		// probably keep class-data in it too
	}

	public GcHeap createHeap() {
		if (nextAddress + heapSize > data.length) {
			throw new OutOfMemoryError();
		}

		try {
			return new GcHeap(this, this.heapCounter++, nextAddress, heapSize);
		} finally {
			nextAddress += heapSize;
		}
	}
}
