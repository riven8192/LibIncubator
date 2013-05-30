package net.indiespot.gc;

public class GcClass {

	public static GcClass[] CLASSES = new GcClass[16];

	public final int id;
	public final int sizeof;
	public final String name;
	public final int referenceFieldCount;
	public final int overallFieldCount;
	public final int arrayFlags;

	public static final int CLASS = 0;
	public static final int BOOLEAN_ARRAY = 1 << 0;
	public static final int BYTE_ARRAY = 1 << 1;
	public static final int SHORT_ARRAY = 1 << 2;
	public static final int INT_ARRAY = 1 << 3;
	public static final int REF_ARRAY = 1 << 4;

	public boolean isArray() {
		return arrayFlags != CLASS;
	}

	public GcClass(int id, String name, int referenceFieldCount, int fieldCount, int arrayFlags) {
		if (id <= 0) {
			throw new IllegalArgumentException();
		}

		// all reference-fields are first

		this.id = id;
		if (this.isArray()) {
			this.sizeof = 1 /* classid */+ 1/* gcflag */+ 1/* forward */+ 1 /* length */;
		} else {
			this.sizeof = 1 /* classid */+ 1/* gcflag */+ 1/* forward */+ fieldCount;
		}
		this.name = name;
		this.referenceFieldCount = referenceFieldCount;
		this.overallFieldCount = fieldCount;
		this.arrayFlags = arrayFlags;

		if (CLASSES[id] != null) {
			throw new IllegalStateException();
		}
		CLASSES[id] = this;
	}
}
