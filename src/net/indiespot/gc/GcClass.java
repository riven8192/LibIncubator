package net.indiespot.gc;

public class GcClass {

	public static GcClass[] CLASSES = new GcClass[16];

	public final int id;
	public final int sizeof;
	public final String name;
	public final int referenceFieldCount;
	public final int overallFieldCount;
	public final boolean isArray;

	public GcClass(int id, String name, int referenceFieldCount, int fieldCount, boolean isArray) {
		if (id <= 0) {
			throw new IllegalArgumentException();
		}

		// all reference-fields are first

		this.id = id;
		if (this.isArray) {
			this.sizeof = 1 /* classid */+ 1/* gcflag */+ 1/* forward */+ 1 /* length */;
		} else {
			this.sizeof = 1 /* classid */+ 1/* gcflag */+ 1/* forward */+ fieldCount;
		}
		this.name = name;
		this.referenceFieldCount = referenceFieldCount;
		this.overallFieldCount = fieldCount;
		this.isArray = isArray;

		if (CLASSES[id] != null) {
			throw new IllegalStateException();
		}
		CLASSES[id] = this;
	}
}
