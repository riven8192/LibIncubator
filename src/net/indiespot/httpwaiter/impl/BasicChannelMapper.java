package net.indiespot.httpwaiter.impl;

import net.indiespot.httpwaiter.TokenManager;

public class BasicChannelMapper implements TokenManager {
	@Override
	public String createPostTokenForChannel(String channel, String userId) {
		return null;
	}

	@Override
	public boolean verifyPostTokenForChannel(String channel, String userId, String token) {
		return false;
	}
	
	@Override
	public void revokePostTokenForChannel(String channel, String userId, String token) {

	}
}
