package net.indiespot.path;

public class Edge {
	private static long id_gen;

	public final long id;
	public final Node src, dst;
	public Object attachment;

	public Edge(Node src, Node dst, Object attachment) {
		this.id = ++id_gen;
		this.src = src;
		this.dst = dst;
		this.attachment = attachment;

		this.src.outEdges.add(this);
		this.dst.inEdges.add(this);
	}

	public void destroy() {
		boolean found = this.src.outEdges.remove(this);
		if (!found) {
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

	@Override
	public String toString() {
		return "Edge#" + id + "[" + src + " -> " + dst + "]";
	}
}
