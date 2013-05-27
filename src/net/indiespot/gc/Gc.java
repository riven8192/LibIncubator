package net.indiespot.gc;

import java.util.concurrent.atomic.AtomicInteger;

public class Gc {
	private static final boolean verbose = !true;
	private static final AtomicInteger flag_provider = new AtomicInteger(321986506);

	private final GcMemory memory;
	public int flag;

	public Gc(GcMemory memory) {
		this.memory = memory;
	}

	private GcHeap sourceHeap, targetHeap;

	public void copyCollect(GcIntList roots, GcHeap sourceHeap, GcHeap targetHeap) {
		if (sourceHeap.heapIndex == 0 || sourceHeap.isEmpty()) {
			throw new IllegalStateException("invalid source heap #" + sourceHeap.heapIndex);
		}
		if (targetHeap.heapIndex == 0 || !targetHeap.isEmpty()) {
			throw new IllegalStateException("invalid target heap #" + targetHeap.heapIndex);
		}

		this.sourceHeap = sourceHeap;
		this.targetHeap = targetHeap;

		this.flag = flag_provider.incrementAndGet();

		for (int i = 0; i < roots.size(); i++) {
			int newRoot = this.mark(roots.get(i));
			if (newRoot > 0) {
				roots.set(i, newRoot);
			}
		}

		sourceHeap.free();
	}

	public void mergeCopyCollect(GcIntList roots, GcHeap sourceHeap1, GcHeap sourceHeap2, final GcHeap targetHeap1, final GcHeap targetHeap2) {
		copyCollect(roots, sourceHeap1, targetHeap1);
		copyCollect(roots, sourceHeap2, targetHeap2);

		if (targetHeap1.used() + targetHeap2.used() > sourceHeap1.space) {
			throw new IllegalStateException();
		}

		final GcHeap small1 = targetHeap1;
		final GcHeap small2 = targetHeap2;
		new Gc(memory) {
			@Override
			boolean shouldMoveObject(int object) {
				return (object / memory.heapSize == small1.heapIndex) || //
				   (object / memory.heapSize == small2.heapIndex);
			}
		}.copyCollect(roots, small1/* dummy */, sourceHeap1);
	}

	public int trace(GcIntList roots) {
		this.flag = flag_provider.incrementAndGet();

		int sum = 0;
		for (int i = 0; i < roots.size(); i++) {
			sum += this.trace(roots.get(i));
		}
		return sum;
	}

	private int trace(int object) {
		if (GcObject.getGcFlag(memory, object) == this.flag) {
			return 0;
		}
		GcObject.setGcFlag(memory, object, this.flag);

		int sum = 1;

		GcClass clazz = GcObject.getClass(memory, object);

		if (clazz.isArray) {
			int arrayLength = GcArray.getLength(memory, object);
			for (int arrayIndex = 0; arrayIndex < arrayLength; arrayIndex++) {
				int target = GcArray.getArrayElement(memory, object, arrayIndex);
				if (target != 0x00) {
					sum += this.trace(target);
				}
			}
		} else {
			int fieldCount = clazz.referenceFieldCount;
			for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
				int target = GcObject.getFieldValue(memory, object, fieldIndex);
				if (target != 0x00) {
					sum += this.trace(target);
				}
			}
		}

		return sum;
	}

	boolean shouldMoveObject(int object) {
		return object / memory.heapSize == sourceHeap.heapIndex;
	}

	private int mark(int object) {
		if (GcObject.getGcFlag(memory, object) == this.flag) {
			// already processed this object
			return GcObject.getForwardPointer(memory, object);
		}
		GcObject.setGcFlag(memory, object, this.flag);

		GcClass clazz = GcObject.getClass(memory, object);
		String descr = clazz.isArray ? "array" : "object";

		if (verbose) {
			System.out.println("\t" + descr + " #" + object);
		}

		if (this.shouldMoveObject(object)) {

			if (clazz.isArray) {
				object = GcArray.copyTo(memory, object, targetHeap);
			} else {
				object = GcObject.copyTo(memory, object, targetHeap);
			}

			if (verbose) {
				System.out.println("\t" + descr + " #" + object + " (moved)");
			}
		} else {
			if (verbose) {
				System.out.println("\t" + descr + " #" + object + " (not in source heap)");
			}
		}

		if (clazz.isArray) {
			int arrayLength = GcArray.getLength(memory, object);
			for (int arrayIndex = 0; arrayIndex < arrayLength; arrayIndex++) {
				int target = GcArray.getArrayElement(memory, object, arrayIndex);
				if (target == 0x00) {
					continue;
				}

				if (verbose) {
					System.out.println("\t\tARRAY reference #" + object + "[" + arrayIndex + "] = " + target);
				}

				int newTarget = this.mark(target);
				System.out.println(target + " -> " + newTarget);
				if (verbose) {
					System.out.println("\tback at object #" + object);
				}
				if (newTarget > 0) {
					if (verbose) {
						System.out.println("\t\tARRAY reference #" + object + "[" + arrayIndex + "] = " + newTarget + " (rewritten)");
					}
					GcArray.setArrayElement(memory, object, arrayIndex, newTarget);
				}
			}
		} else {
			int fieldCount = clazz.referenceFieldCount;
			for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
				int target = GcObject.getFieldValue(memory, object, fieldIndex);
				if (target == 0x00) {
					continue;
				}

				if (verbose) {
					System.out.println("\t\treference #" + object + "[" + fieldIndex + "] = " + target);
				}

				int newTarget = this.mark(target);
				if (verbose) {
					System.out.println("\tback at object #" + object);
				}
				if (newTarget > 0) {
					if (verbose) {
						System.out.println("\t\treference #" + object + "[" + fieldIndex + "] = " + newTarget + " (rewritten)");
					}
					GcObject.setFieldValue(memory, object, fieldIndex, newTarget);
				}
			}
		}

		return object;
	}
}