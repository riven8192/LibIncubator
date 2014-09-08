package net.indiespot.loadbalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class RoundRobinTargetFinder implements TargetFinder
{
	@Override
	public Target findTarget(Collection<Target> targets, Identity identity)
	{
		List<Target> list = new ArrayList<>();
		for(Target target : targets)
			if(target.unavailableAt == 0L)
				for(int i = 0; i < target.weight; i++)
					list.add(target);
		if(list.isEmpty())
			return null;
		return list.get(new Random().nextInt(list.size()));
	}
}
