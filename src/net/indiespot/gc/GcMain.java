package net.indiespot.gc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GcMain {

	private static List<GcHeap> createHeaps(GcMemory memory) {
		List<GcHeap> heaps = new ArrayList<>();
		while (true) {
			try {
				heaps.add(memory.createHeap());
			} catch (OutOfMemoryError err) {
				break;
			}
		}
		return heaps;
	}

	private static int newObject(List<GcHeap> heaps, GcClass clazz) {
		while (true) {
			GcHeap heap = heaps.get(0);

			try {
				return heap.malloc(clazz);
			} catch (OutOfMemoryError err) {
				heaps.remove(0);
				heaps.add(heap);
			}
		}
	}

	private static int newArray(List<GcHeap> heaps, GcClass clazz, int arrayLength) {
		while (true) {
			GcHeap heap = heaps.get(0);

			try {
				return heap.malloc(clazz, arrayLength);
			} catch (OutOfMemoryError err) {
				heaps.remove(0);
				heaps.add(heap);
			}
		}
	}

	public static void main(String[] args) {
		if (true) {
			for (int i = 1; i <= 1025; i++) {
				int pot = GcManagedMemory.roundUpToPowerOfTwo(i);
				System.out.println(i + " -> " + pot + " -> " + GcManagedMemory.log2(pot));
			}

			return;
		}

		int memorySize;
		int heapSize;
		int objectCount;

		boolean runOnce = true;

		if (runOnce) {
			// force [immortal,alive,empty] heap
			memorySize = (3 * 4) * 1024;
			heapSize = 4 * 1024;
			objectCount = 16;
		} else {
			memorySize = 1024 * 1024;
			heapSize = 4 * 1024;
			objectCount = 16 * 1024;
		}

		GcMemory memory = new GcMemory(memorySize, heapSize);

		List<GcHeap> heaps = createHeaps(memory);
		System.out.println("heaps: " + heaps.size());

		GcClass clazz = new GcClass(13, "Obj", 3, 4, false);
		GcClass arrayClazz = new GcClass(14, "Arr", 0, 0, true);

		GcIntList objs = new GcIntList();

		System.out.println("creating objects...");
		for (int i = 0; i < objectCount; i++) {
			objs.add(newObject(heaps, clazz));
		}

		System.out.println("creating array...");
		int array = newArray(heaps, arrayClazz, 27);
		GcArray.setArrayElement(memory, array, 0, objs.get(0));
		GcArray.setArrayElement(memory, array, 1, objs.get(1));

		Random r = new Random(21345);

		System.out.println("connecting objects...");
		for (int i = 0; i < 2 * objectCount; i++) {
			int obj1 = objs.get(r.nextInt(objs.size()));
			int obj2 = objs.get(r.nextInt(objs.size()));

			int fields = GcObject.getClass(memory, obj1).referenceFieldCount;
			GcObject.setFieldValue(memory, obj1, r.nextInt(fields), obj2);
		}

		System.out.println("peeking at heaps...");
		List<GcHeap> aliveHeaps = new ArrayList<>();
		List<GcHeap> emptyHeaps = new ArrayList<>();

		for (GcHeap heap : heaps) {
			if (heap.isEmpty()) {
				emptyHeaps.add(heap);
			} else {
				aliveHeaps.add(heap);
			}
		}

		System.out.println("making up roots...");
		GcIntList roots = new GcIntList();
		roots.add(objs.get(0));
		roots.add(array);

		for (int i = 0; (runOnce ? i < 1 : true); i++) {
			GcHeap alive = aliveHeaps.remove(0);
			GcHeap empty = emptyHeaps.remove(0);

			Gc gc = new Gc(memory);

			System.out.println();
			System.out.println("GC heaps: " + alive.heapIndex + " => " + empty.heapIndex);

			int trace1 = gc.trace(roots);
			{
				long t0 = System.nanoTime();
				gc.copyCollect(roots, alive, empty);
				long t1 = System.nanoTime();
				System.out.println("gc took:      " + (t1 - t0) / 1000 + "us");
			}
			int trace2 = gc.trace(roots);

			System.out.println("trace1: " + trace1);
			System.out.println("trace2: " + trace2);

			// swap heaps in variables for readability
			GcHeap tmp = alive;
			alive = empty;
			empty = tmp;

			//

			emptyHeaps.add(empty);

			if (alive.isEmpty()) {
				// copy-collector might not have found anything worth copying
				emptyHeaps.add(alive);
			} else {
				aliveHeaps.add(alive);
			}

		}

		System.out.println();
		int arrayPointer = roots.get(1);
		int ref0 = GcArray.getArrayElement(memory, arrayPointer, 0);
		int ref1 = GcArray.getArrayElement(memory, arrayPointer, 1);
		System.out.println(ref0);
		System.out.println(ref1);
	}

	public static void main2(String[] args) {

		int memorySize = 12 * 1024;
		int heapSize = 4 * 1024;

		GcMemory memory = new GcMemory(memorySize, heapSize);

		List<GcHeap> heaps = createHeaps(memory);
		System.out.println("heaps: " + heaps.size());

		GcClass clazz = new GcClass(13, "Obj", 3, 4, false);

		System.out.println("creating objects...");

		int obj1 = newObject(heaps, clazz);
		int obj2 = newObject(heaps, clazz);
		int obj4 = newObject(heaps, clazz);
		int obj3 = newObject(heaps, clazz);

		GcObject.setFieldValue(memory, obj1, 0, obj3);
		GcObject.setFieldValue(memory, obj1, 1, obj2);
		GcObject.setFieldValue(memory, obj2, 0, obj3);
		GcObject.setFieldValue(memory, obj3, 0, obj1);
		GcObject.setFieldValue(memory, obj4, 0, obj1);

		System.out.println(GcObject.toString(memory, obj1));
		System.out.println(GcObject.toString(memory, obj2));
		System.out.println(GcObject.toString(memory, obj3));
		System.out.println(GcObject.toString(memory, obj4));

		GcHeap alive = heaps.get(0);
		GcHeap empty = heaps.get(1);

		System.out.println(alive);
		System.out.println(empty);

		System.out.println("making up roots...");
		GcIntList roots = new GcIntList();
		roots.add(obj3);

		Gc gc = new Gc(memory);

		System.out.println();
		System.out.println("GC heaps: " + alive.heapIndex + " => " + empty.heapIndex);

		{
			long t0 = System.nanoTime();
			gc.copyCollect(roots, alive, empty);
			long t1 = System.nanoTime();
			System.out.println("gc took:      " + (t1 - t0) / 1000 + "us");
		}

		obj3 = roots.get(0);
		System.out.println(obj3);

		System.out.println("done.");
	}
}