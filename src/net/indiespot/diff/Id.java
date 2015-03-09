package net.indiespot.diff;

public class Id {
	public static long gen_id = 1337;

	public final long id;

	public Id() {
		this.id = gen_id++;
	}
}
