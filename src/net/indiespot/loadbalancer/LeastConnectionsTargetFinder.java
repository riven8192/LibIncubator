package net.indiespot.loadbalancer;

import java.util.Collection;
import java.util.Comparator;

public class LeastConnectionsTargetFinder implements TargetFinder
{
	@Override
	public Target findTarget(Collection<Target> targets, Identity identity)
	{
		Comparator<Target> comp = new Comparator<Target>()
		{
			@Override
			public int compare(Target a, Target b)
			{
				double aLoad = a.connections / ((double) a.weight);
				double bLoad = b.connections / ((double) b.weight);
				return Double.compare(aLoad, bLoad);
			}
		};

		Target best = null;
		for(Target target : targets)
			if(target.unavailableAt == 0L)
				if(target.weight > 0)
					if(best == null || comp.compare(target, best) <= 0)
						best = target;
		return best;
	}
}
