package net.indiespot.path;

import java.util.Collections;
import java.util.Set;

public class Dijkstra {
	public static DijkstraPathFinder scan(Node origin, CostFunction cf, VisitFunction vf) {
		return findPath(Collections.singleton(origin), null, cf, vf);
	}

	public static DijkstraPathFinder findPath(Node origin, TargetFunction tf, CostFunction cf, VisitFunction vf) {
		return findPath(Collections.singleton(origin), tf, cf, vf);
	}

	public static DijkstraPathFinder findPath(Set<Node> origins, TargetFunction tf, CostFunction cf, VisitFunction vf) {
		for (DijkstraPathFinder pf = new DijkstraPathFinder(origins, tf, cf, vf);;) {
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

}
