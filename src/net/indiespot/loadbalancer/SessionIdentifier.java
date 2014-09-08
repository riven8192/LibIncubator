package net.indiespot.loadbalancer;

import java.net.Socket;
import java.util.List;

public interface SessionIdentifier
{
	public String identify(Socket client, List<String> requestHeader);

	public void persist(Identity identity, String identifiedWith, Socket client, List<String> responseHeader);
}
