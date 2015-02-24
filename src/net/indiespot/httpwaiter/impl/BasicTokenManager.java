package net.indiespot.httpwaiter.impl;

import net.indiespot.httpwaiter.TokenManager;

public class BasicTokenManager implements TokenManager {
	@Override
	public String createPostTokenForChannel(String channel, String userId) {
		return null;
	}

	@Override
	public boolean verifyPostTokenForChannel(String channel, String userId, String token) {
		return true;
	}

	@Override
	public void revokePostTokenForChannel(String channel, String userId, String token) {

	}
}