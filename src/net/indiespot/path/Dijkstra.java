package net.indiespot.path;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

public class Dijkstra {
	public static PathFinder findPath(Node origin, TargetFunction tf, CostFunction cf, StopFunction sf) {
		return findPath(Collections.singleton(origin), tf, cf, sf);
	}

	public static PathFinder findPath(Set<Node> origins, TargetFunction tf, CostFunction cf, StopFunction sf) {
		for(PathFinder pf = new PathFinder(origins, tf, cf, sf);;) {
			switch (pf.step()) {
			case RUNNING:
				break;
			case FAILED:
				return null;
			case STOPPED:
				return null;
			case REACHED:
				return pf;
			}
		}
	}

	public static class PathFinder {
		public static enum State {
			RUNNING, REACHED, FAILED, STOPPED
		}

		private final TargetFunction tf;
		private final CostFunction cf;
		private final StopFunction sf;
		private final Map<Node, Float> open;
		private final Map<Node, Float> closed;
		private Node foundTarget;

		public PathFinder(Set<Node> origins, TargetFunction tf, CostFunction cf, StopFunction sf) {
			this.tf = tf;
			this.cf = cf;
			this.sf = sf;

			open = new HashMap<>();
			closed = new HashMap<>();

			for(Node origin : origins) {
				open.put(origin, Float.valueOf(0.0f));
			}
		}

		public State step() {
			Node best = null;
			float cost = Float.MAX_VALUE;
			for(Entry<Node, Float> entry : open.entrySet()) {
				if(best == null || entry.getValue().floatValue() < cost) {
					best = entry.getKey();
					cost = entry.getValue().floatValue();
				}
			}

			if(best == null) {
				return State.FAILED;
			}
			if(sf.shouldStop(best, cost)) {
				return State.STOPPED;
			}

			open.remove(best);
			closed.put(best, cost);

			if(tf.isTarget(best)) {
				foundTarget = best;
				return State.REACHED;
			}

			for(Edge edge : best.edges) {
				if(closed.containsKey(edge.to)) {
					continue;
				}
				float costTo = cost + cf.calculateCost(edge);
				if(!open.containsKey(edge.to) || costTo < open.get(edge.to).floatValue()) {
					open.put(edge.to, Float.valueOf(costTo));
				}
			}

			return State.RUNNING;
		}

		public List<Node> getPath() {
			if(foundTarget == null)
				throw new NoSuchElementException();
			return backtrack(foundTarget, open, closed, cf);
		}

		public List<Entry<Node, Float>> getPathInfo() {
			if(foundTarget == null)
				throw new NoSuchElementException();
			return backtrackInfo(foundTarget, open, closed, cf);
		}
	}

	private static List<Node> backtrack(Node at, Map<Node, Float> open, Map<Node, Float> closed, CostFunction cf) {
		List<Node> path = new ArrayList<>();

		path.add(at);

		while (true) {
			Node to = null;
			float to1 = Float.MAX_VALUE;
			float to2 = Float.MAX_VALUE;

			for(Edge back : at.trace) {
				Float fromCost = closed.get(back.from);
				if(fromCost == null) {
					continue;
				}

				to1 = fromCost.floatValue();
				float backCost = to1 + cf.calculateCost(back);
				if(to == null || backCost < to2) {
					to = back.from;
					to2 = backCost;
				}
			}
			if(to == null) {
				throw new IllegalStateException();
			}
			at = to;

			path.add(at);

			if(to1 == 0.0f) {
				Collections.reverse(path);
				return path;
			}
		}
	}

	private static List<Entry<Node, Float>> backtrackInfo(Node at, Map<Node, Float> open, Map<Node, Float> closed, CostFunction cf) {
		List<Entry<Node, Float>> path = new ArrayList<>();

		path.add(new AbstractMap.SimpleImmutableEntry<Node, Float>(at, closed.get(at)));

		while (true) {
			Node to = null;
			float to1 = Float.MAX_VALUE;
			float to2 = Float.MAX_VALUE;

			for(Edge back : at.trace) {
				Float fromCost = closed.get(back.from);
				if(fromCost == null) {
					continue;
				}

				to1 = fromCost.floatValue();
				float backCost = to1 + cf.calculateCost(back);
				if(to == null || backCost < to2) {
					to = back.from;
					to2 = backCost;
				}
			}
			if(to == null) {
				throw new IllegalStateException();
			}
			at = to;

			path.add(new AbstractMap.SimpleImmutableEntry<Node, Float>(at, to1));

			if(to1 == 0.0f) {
				Collections.reverse(path);
				return path;
			}
		}
	}
}
