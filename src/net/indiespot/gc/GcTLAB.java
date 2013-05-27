package net.indiespot.gc;

public class GcTLAB extends GcHeap {
	public GcTLAB(GcHeap heap, int size) {
		super(heap.memory, /* heap index */-1, heap.malloc(size), size);
	}
}
