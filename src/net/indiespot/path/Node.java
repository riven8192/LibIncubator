package net.indiespot.path;

import java.util.ArrayList;
import java.util.List;

public class Node {
	private static long id_gen;

	public final long id;
	public Object attachment;
	public final List<Edge> edges;
	public final List<Edge> trace;

	public Node(Object attachment) {
		this.id = ++id_gen;
		this.attachment = attachment;
		this.edges = new ArrayList<>();
		this.trace = new ArrayList<>();
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Node) && ((Node) obj).id == id;
	}

	@Override
	public String toString() {
		return "Node#" + id;
	}
}
