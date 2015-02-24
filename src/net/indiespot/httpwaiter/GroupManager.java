package net.indiespot.httpwaiter;

import java.util.Set;

public interface GroupManager {
	public void add(String group, String member);

	public void remove(String group, String member);

	public Set<String> list(String group);
}