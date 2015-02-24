package net.indiespot.httpwaiter;

public interface TokenManager {
	public String createPostTokenForChannel(String channel, String userId);

	public boolean verifyPostTokenForChannel(String channel, String userId, String token);

	public void revokePostTokenForChannel(String channel, String userId, String token);
}