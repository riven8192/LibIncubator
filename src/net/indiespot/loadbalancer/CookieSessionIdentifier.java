package net.indiespot.loadbalancer;

import java.net.Socket;
import java.util.List;

import net.indiespot.loadbalancer.util.Text;

public class CookieSessionIdentifier implements SessionIdentifier
{
	private static final String identity_cookie = "CWLBCID";

	@Override
	public String identify(Socket client, List<String> requestHeader)
	{
		// find identity cookie
		String identityCookieValue = null;
		for(int i = requestHeader.size() - 1; i >= 0; i--)
		{
			String line = requestHeader.get(i);
			if(!line.startsWith("Cookie: "))
				continue;

			String cookies = line.substring(8);
			for(String cookie : Text.split(cookies, ';'))
				if((cookie = cookie.trim()).startsWith(identity_cookie + "="))
					identityCookieValue = cookie.substring(identity_cookie.length() + 1);
		}

		// remove proxy headers
		for(int i = requestHeader.size() - 1; i >= 0; i--)
			if(requestHeader.get(i).startsWith("X-Forwarded-")) // "For", "Proto"
				requestHeader.remove(i);

		// add proxy headers
		requestHeader.add("X-Forwarded-For: " + client.getInetAddress().getHostAddress());
		requestHeader.add("X-Forwarded-Proto: https");

		return identityCookieValue;
	}

	@Override
	public void persist(Identity identity, String initialIdentityToken, Socket client, List<String> responseHeader)
	{
		if(initialIdentityToken == null)
		{
			responseHeader.add("Set-Cookie: " + identity_cookie + "=" + identity.token + "; Path=/; Expires=Wed, 09 Jun 2021 10:18:14 GMT");
		}
	}
}
