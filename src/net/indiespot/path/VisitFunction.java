package net.indiespot.path;

import net.indiespot.path.DijkstraPathFinder.Instruction;

public interface VisitFunction {
	public Instruction onReached(DijkstraPathFinder finder, Node node, float cost);
}
