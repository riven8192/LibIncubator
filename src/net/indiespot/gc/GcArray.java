package net.indiespot.gc;

public class GcArray {
	private static final int LENGTH_OFFSET = 3;
	private static final int ELEMENT_OFFSET = 4;

	//

	public static int getLength(GcMemory memory, int pointer) {
		return memory.data[pointer + LENGTH_OFFSET];
	}

	public static void setLength(GcMemory memory, int pointer, int length) {
		memory.data[pointer + LENGTH_OFFSET] = length;
	}

	//

	public static int getArrayElement(GcMemory memory, int pointer, int fieldIndex) {
		return memory.data[pointer + ELEMENT_OFFSET + fieldIndex];
	}

	public static void setArrayElement(GcMemory memory, int pointer, int fieldIndex, int elementPointer) {
		memory.data[pointer + ELEMENT_OFFSET + fieldIndex] = elementPointer;
	}

	//

	public static int copyTo(GcMemory memory, int oldPointer, GcHeap targetHeap) {
		GcClass clazz = GcObject.getClass(memory, oldPointer);

		int len = GcArray.getLength(memory, oldPointer);
		int newPointer = targetHeap.malloc(clazz.sizeof + len);
		System.arraycopy(memory.data, oldPointer, memory.data, newPointer, clazz.sizeof + len);

		GcObject.setForwardPointer(memory, oldPointer, newPointer);
		GcObject.setForwardPointer(memory, newPointer, 0);

		return newPointer;
	}

	//

	public static String toString(GcMemory memory, int pointer) {
		GcClass clazz = GcObject.getClass(memory, pointer);

		StringBuilder sb = new StringBuilder();
		sb.append(clazz.name).append('#').append(pointer).append("[\r\n");
		for (int i = 0; i < getLength(memory, pointer); i++) {
			int ref = getArrayElement(memory, pointer, i);
			sb.append("\t[").append(i).append(']').append('=').append(ref == 0 ? "NULL" : ("#" + String.valueOf(ref))).append("\r\n");
		}
		return sb.append(']').toString();
	}
}
