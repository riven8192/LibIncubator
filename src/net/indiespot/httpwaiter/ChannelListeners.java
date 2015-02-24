package net.indiespot.httpwaiter;

import java.util.List;

public interface ChannelListeners {
	public void register(String channel, Connection connection);

	public void unregister(String channel, Connection connection);

	public List<Connection> getListeners(String channel);
}