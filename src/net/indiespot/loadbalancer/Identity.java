package net.indiespot.loadbalancer;

import java.util.HashMap;
import java.util.Map;

public class Identity
{
	public final String token;
	//public Target preference;
	public long activeAt;
	public final Map<String, Target> hostname2preference;

	public Identity(String token)
	{
		this.token = token;
		this.hostname2preference = new HashMap<>(1);
	}
}