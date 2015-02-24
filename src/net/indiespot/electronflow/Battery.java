package net.indiespot.electronflow;

public class Battery extends Node {
	public int storageUsage, storageCapacity;
	public int energyThreshold;

	public void distribute() {
		if (energy < energyThreshold && storageUsage > 0) {
			int transfer = Math.min(energyThreshold - energy, storageUsage - 0);
			this.energy += transfer;
			this.storageUsage -= transfer;
		} else if (energy > energyThreshold && storageUsage < storageCapacity) {
			int transfer = Math.min(energy - energyThreshold, storageCapacity - storageUsage);
			this.energy -= transfer;
			this.storageUsage += transfer;
		}

		super.distribute();
	}
}
