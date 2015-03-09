package net.indiespot.distribution;

import net.indiespot.path.Edge;

public class Connection2D {
	public static Connection2D oneWay(Node2D a, Node2D b, float resistancePerUnit) {
		Connection2D wire = new Connection2D(a, b, resistancePerUnit);
		new Edge(a, b, wire);
		return wire;
	}

	public static Connection2D twoWay(Node2D a, Node2D b, float resistancePerUnit) {
		Connection2D wire = new Connection2D(a, b, resistancePerUnit);
		new Edge(a, b, wire);
		new Edge(b, a, wire);
		return wire;
	}

	public final Node2D a, b;
	public float costPerUnit;
	public int accumulatedTransfer;

	private Connection2D(Node2D a, Node2D b, float costPerUnit) {
		if (a == null)
			throw new IllegalArgumentException();
		if (b == null)
			throw new IllegalArgumentException();
		if (a == b)
			throw new IllegalArgumentException();
		if (costPerUnit <= 0.0f)
			throw new IllegalArgumentException();
		this.a = a;
		this.b = b;
		this.costPerUnit = costPerUnit;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + a + " <-> " + b + "]";
	}
}
