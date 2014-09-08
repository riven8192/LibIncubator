package net.indiespot.loadbalancer;

import java.util.Collection;

public interface TargetFinder
{
	public Target findTarget(Collection<Target> targets, Identity identity);
}
