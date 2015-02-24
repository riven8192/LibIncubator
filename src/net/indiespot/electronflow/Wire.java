package net.indiespot.electronflow;

public class Wire {
	public final Node a, b;

	public Wire(Node a, Node b) {
		if (a == b)
			throw new IllegalArgumentException();
		this.a = a;
		this.b = b;
		a.wires.add(this);
		b.wires.add(this);
	}
}
