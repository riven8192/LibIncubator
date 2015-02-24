package net.indiespot.distribution.energy;

import net.indiespot.path.Edge;

public class Wire {
	public static Wire oneWay(Pole a, Pole b, float resistancePerUnit) {
		Wire wire = new Wire(a, b, resistancePerUnit);
		new Edge(a, b, wire);
		return wire;
	}

	public static Wire twoWay(Pole a, Pole b, float resistancePerUnit) {
		Wire wire = new Wire(a, b, resistancePerUnit);
		new Edge(a, b, wire);
		new Edge(b, a, wire);
		return wire;
	}

	public final Pole a, b;
	public float resistancePerUnit;
	public int accumulatedTransfer;

	private Wire(Pole a, Pole b, float resistancePerUnit) {
		this.a = a;
		this.b = b;
		this.resistancePerUnit = resistancePerUnit;
	}

	@Override
	public String toString() {
		return "Wire[" + a + " <-> " + b + "]";
	}
}
