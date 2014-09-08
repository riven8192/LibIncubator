package net.indiespot.path;

public class Edge {
	private static long id_gen;

	public final long id;
	public final Node from, to;
	public Object attachment;

	public Edge(Node from, Node to, Object attachment) {
		this.id = ++id_gen;
		this.from = from;
		this.to = to;
		this.attachment = attachment;

		this.from.edges.add(this);
		this.to.trace.add(this);
	}

	public void destroy() {
		boolean found = this.from.edges.remove(this);
		if(!found) {
			throw new IllegalStateException();
		}
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Edge) && ((Edge) obj).id == this.id;
	}
}
