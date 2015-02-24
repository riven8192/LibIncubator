package net.indiespot.electronflow;

public class Consumer extends Node {
	public int demand;
	public int countdownUntilRestart;

	public boolean isOnline() {
		return countdownUntilRestart < 0;
	}

	public void consume() {
		if (--countdownUntilRestart >= 0) {
			return;

		}
		if (energy >= demand) {
			energy -= demand;

			// up :o)
		} else {
			// down :o(
			countdownUntilRestart = 250 + (int) (Math.random() * 250);
		}
	}
}
