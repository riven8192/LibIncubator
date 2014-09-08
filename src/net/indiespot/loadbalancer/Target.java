package net.indiespot.loadbalancer;

import java.net.InetSocketAddress;

public class Target {
	public Target(String name, InetSocketAddress addr) {
		this.name = name;
		this.addr = addr;
	}

	public final String name;
	public final InetSocketAddress addr;
	public int connections;
	public int weight;
	public long unavailableAt;

	@Override
	public int hashCode() {
		return addr.getHostString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		Target that = (Target)obj;
		return this.addr.getHostString().equals(that.addr.getHostString());
	}
}