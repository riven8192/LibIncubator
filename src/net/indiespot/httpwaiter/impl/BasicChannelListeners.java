package net.indiespot.httpwaiter.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.indiespot.httpwaiter.ChannelListeners;
import net.indiespot.httpwaiter.Connection;

public class BasicChannelListeners implements ChannelListeners {
	private final Map<String, List<Connection>> channel2listeners = new HashMap<>();

	@Override
	public void register(String channel, Connection connection) {
		List<Connection> listeners = channel2listeners.get(channel);
		if (listeners == null)
			channel2listeners.put(channel, listeners = new ArrayList<>());
		listeners.add(connection);
	}

	@Override
	public void unregister(String channel, Connection connection) {
		List<Connection> listeners = channel2listeners.get(channel);
		if (listeners == null)
			throw new IllegalStateException();
		if (!listeners.remove(connection))
			throw new IllegalStateException();
	}

	@Override
	public List<Connection> getListeners(String channel) {
		return new ArrayList<>(channel2listeners.get(channel));
	}
}