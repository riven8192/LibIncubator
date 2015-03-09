package net.indiespot.distribution;

public class Generator {
	public int production;

	public Generator(int production) {
		if (production < 0)
			throw new IllegalArgumentException();
		this.production = production;
	}

	public Resource generate() {
		return new Resource(production, production);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[+" + production + "]";
	}
}
