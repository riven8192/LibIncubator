package net.indiespot.httpwaiter.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.indiespot.httpwaiter.ChannelStorage;

public class MemoryChannelStorage implements ChannelStorage {
	private final Map<String, List<byte[]>> channel2messages = new HashMap<>();

	@Override
	public int getLatestMessageIndex(String channel) {
		List<byte[]> messages = channel2messages.get(channel);
		if (messages == null)
			return 0;
		return messages.size();
	}

	@Override
	public int addMessage(String channel, byte[] message) {
		List<byte[]> messages = channel2messages.get(channel);
		if (messages == null)
			channel2messages.put(channel, messages = new ArrayList<>());
		messages.add(message);
		return messages.size() - 1;
	}

	@Override
	public byte[] getMessage(String channel, int idx) {
		List<byte[]> messages = channel2messages.get(channel);
		if (messages == null)
			return null;
		if (idx < 0 || idx >= messages.size())
			return null;
		return messages.get(idx);
	}
}