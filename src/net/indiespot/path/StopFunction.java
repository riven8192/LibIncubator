package net.indiespot.path;

public interface StopFunction {
	public boolean shouldStop(Node best, float cost);
}
