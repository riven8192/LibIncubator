package net.indiespot.electronflow;

import java.util.ArrayList;
import java.util.List;

public class Node {
	public int x, y;

	public int energy;
	public int capacity;
	public int in, out, blocked;

	public List<Wire> wires = new ArrayList<>();

	public void distribute() {
		if (energy == 0 || wires.isEmpty())
			return;

		final int minDiff = 1;
		final int maxTransfer = 1;

		while (true) {
			Node target = null;
			for (Wire wire : wires) {
				Node remote = (wire.a != this) ? wire.a : wire.b;
				if (remote.energy == remote.capacity) {
					remote.blocked++;
					continue; // remote node is full
				}
				if (remote.energy >= this.energy - minDiff)
					continue; // cannot spread against a voltage potential
				if (target == null || remote.energy < target.energy)
					target = remote; // we found a new target
			}
			if (target == null)
				return;

			int transfer = 1;
			if (maxTransfer > 1) {
				transfer = this.energy - target.energy;
				transfer = Math.min(transfer, this.energy);
				transfer = Math.min(transfer, target.capacity - target.energy);
				transfer /= 1.5;

				if (transfer < 1)
					transfer = 1;
				else if (transfer > maxTransfer)
					transfer = maxTransfer;
			}

			this.energy -= transfer;
			target.energy += transfer;

			this.out += transfer;
			target.in += transfer;
		}
	}
}
