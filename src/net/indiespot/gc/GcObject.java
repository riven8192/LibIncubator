package net.indiespot.gc;

public class GcObject {

	private static final int CLASSID_OFFSET = 0;
	private static final int GCFLAG_OFFSET = 1;
	private static final int FORWARD_OFFSET = 2;
	private static final int FIELDS_OFFSET = 3;

	public static boolean isProbableObject(GcMemory memory, int pointer, int gcFlag) {
		int classid = memory.data[pointer + CLASSID_OFFSET];
		if (classid <= 0 || classid >= GcClass.CLASSES.length) {
			return false;
		}

		int flag = GcObject.getGcFlag(memory, pointer);
		if (flag != gcFlag) {
			return false;
		}

		return true;
	}

	public static GcClass getClass(GcMemory memory, int pointer) {
		int id = memory.data[pointer + CLASSID_OFFSET];
		if (GcClass.CLASSES[id] == null) {
			throw new NullPointerException("class of #" + pointer + " -> classes[" + id + "] -> null");
		}
		return GcClass.CLASSES[id];
	}

	public static void setClass(GcMemory memory, int pointer, GcClass clazz) {
		memory.data[pointer + CLASSID_OFFSET] = clazz.id;
	}

	//

	public static int getGcFlag(GcMemory memory, int pointer) {
		return memory.data[pointer + GCFLAG_OFFSET];
	}

	public static void setGcFlag(GcMemory memory, int pointer, int gcFlag) {
		memory.data[pointer + GCFLAG_OFFSET] = gcFlag;
	}

	//

	public static int getForwardPointer(GcMemory memory, int pointer) {
		return memory.data[pointer + FORWARD_OFFSET];
	}

	public static void setForwardPointer(GcMemory memory, int pointer, int forward) {
		memory.data[pointer + FORWARD_OFFSET] = forward;
	}

	//

	public static int getFieldValue(GcMemory memory, int pointer, int fieldIndex) {
		return memory.data[pointer + FIELDS_OFFSET + fieldIndex];
	}

	public static void setFieldValue(GcMemory memory, int pointer, int fieldIndex, int value) {
		memory.data[pointer + FIELDS_OFFSET + fieldIndex] = value;
	}

	//

	public static int copyTo(GcMemory memory, int oldPointer, GcHeap targetHeap) {
		GcClass clazz = GcObject.getClass(memory, oldPointer);

		int newPointer = targetHeap.malloc(clazz.sizeof);
		System.arraycopy(memory.data, oldPointer, memory.data, newPointer, clazz.sizeof);

		GcObject.setForwardPointer(memory, oldPointer, newPointer);
		GcObject.setForwardPointer(memory, newPointer, 0);

		return newPointer;
	}

	//

	public static String toString(GcMemory memory, int pointer) {
		GcClass clazz = getClass(memory, pointer);

		StringBuilder sb = new StringBuilder();
		sb.append(clazz.name).append('#').append(pointer).append("[\r\n");
		for (int i = 0; i < clazz.referenceFieldCount; i++) {
			int ref = getFieldValue(memory, pointer, i);
			sb.append("\tref[").append(i).append(']').append('=').append(ref == 0 ? "NULL" : String.valueOf(ref)).append("\r\n");
		}
		for (int i = clazz.referenceFieldCount; i < clazz.overallFieldCount; i++) {
			int val = getFieldValue(memory, pointer, i);
			sb.append("\tval[").append(i).append(']').append('=').append(val).append("\r\n");
		}
		return sb.append(']').toString();
	}
}
