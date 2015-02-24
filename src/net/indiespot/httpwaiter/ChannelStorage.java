package net.indiespot.httpwaiter;

public interface ChannelStorage {
	public int getLatestMessageIndex(String channel);

	public int addMessage(String channel, byte[] message);

	public byte[] getMessage(String channel, int idx);
}