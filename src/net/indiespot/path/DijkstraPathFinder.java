package net.indiespot.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

public class DijkstraPathFinder {
	public static enum State {
		RUNNING, REACHED, FAILED, STOPPED
	}

	public static enum Instruction {
		CONTINUE, ABORT
	}

	private final TargetFunction tf;
	private final CostFunction cf;
	private final VisitFunction vf;
	private final Map<Node, Float> opened;
	private final Map<Node, Float> closed;
	private Node foundTarget;

	public DijkstraPathFinder(Set<Node> origins, TargetFunction tf, CostFunction cf, VisitFunction vf) {
		if (cf == null)
			throw new NullPointerException();

		this.tf = tf;
		this.cf = cf;
		this.vf = vf;

		opened = new HashMap<>();
		closed = new HashMap<>();

		for (Node origin : origins) {
			opened.put(origin, Float.valueOf(0.0f));
		}
	}

	public State step() {
		Node best = null;
		float cost = Float.MAX_VALUE;
		for (Entry<Node, Float> entry : opened.entrySet()) {
			if (best == null || entry.getValue().floatValue() < cost) {
				best = entry.getKey();
				cost = entry.getValue().floatValue();
			}
		}

		if (best == null) {
			return State.FAILED;
		}

		opened.remove(best);
		closed.put(best, cost);

		if (vf != null) {
			if (vf.onReached(this, best, cost) == Instruction.ABORT)
				return State.STOPPED;
		}

		if (tf != null && tf.isTarget(best)) {
			foundTarget = best;
			return State.REACHED;
		}

		for (Edge edge : best.outEdges) {
			if (closed.containsKey(edge.dst)) {
				continue;
			}
			float costTo = cost + cf.calculateCost(edge);
			if (!opened.containsKey(edge.dst) || costTo < opened.get(edge.dst).floatValue()) {
				opened.put(edge.dst, Float.valueOf(costTo));
			}
		}

		return State.RUNNING;
	}

	public List<Node> getPath() {
		if (foundTarget == null)
			throw new NoSuchElementException("target not yet reached");
		return this.getPath(foundTarget);
	}

	public List<Node> getPath(Node from) {
		if (!closed.containsKey(from))
			throw new NoSuchElementException("node not yet reached");

		List<Node> nodes = new ArrayList<>();
		for (Edge edge : this.backtrack(from))
			nodes.add(edge.src);
		nodes.add(from);
		return nodes;
	}

	public List<Edge> getPathInfo() {
		if (foundTarget == null)
			throw new NoSuchElementException("target not yet reached");
		return this.getPathInfo(foundTarget);
	}

	public List<Edge> getPathInfo(Node from) {
		if (!closed.containsKey(from))
			throw new NoSuchElementException("node not yet reached");
		return this.backtrack(from);
	}

	private List<Edge> backtrack(Node at) {
		List<Edge> path = new ArrayList<>();

		while (closed.get(at).floatValue() != 0.0f) {
			Edge bestTarget = null;
			float bestTargetCost = Float.MAX_VALUE;

			for (Edge backEdge : at.inEdges) {
				Float costAtTarget = closed.get(backEdge.src);
				if (costAtTarget == null) {
					continue;
				}

				float targetCost = costAtTarget.floatValue() + cf.calculateCost(backEdge);
				if (bestTarget == null || targetCost < bestTargetCost) {
					bestTarget = backEdge;
					bestTargetCost = targetCost;
				}
			}
			if (bestTarget == null) {
				throw new IllegalStateException();
			}

			path.add(bestTarget);
			at = bestTarget.src;
		}

		Collections.reverse(path);
		return path;
	}
}