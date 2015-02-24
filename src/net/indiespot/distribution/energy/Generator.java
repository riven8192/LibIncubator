package net.indiespot.distribution.energy;

import net.indiespot.distribution.Resource;
import net.indiespot.distribution.ResourceType;

public class Generator {
	public ResourceType type;
	public int production;

	public Generator(ResourceType type, int production) {
		if (production < 0)
			throw new IllegalArgumentException();
		this.type = type;
		this.production = production;
	}

	public Resource generate() {
		return new Resource(type, production, production);
	}
}
