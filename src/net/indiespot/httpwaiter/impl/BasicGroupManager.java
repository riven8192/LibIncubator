package net.indiespot.httpwaiter.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.indiespot.httpwaiter.GroupManager;

public class BasicGroupManager implements GroupManager {

	private final Map<String, Set<String>> group2members = new HashMap<>();

	@Override
	public void add(String group, String member) {
		Set<String> members = group2members.get(group);
		if (members == null)
			group2members.put(group, members = new HashSet<>());
		members.add(member);
	}

	@Override
	public void remove(String group, String member) {
		Set<String> members = group2members.get(group);
		if (members == null)
			throw new IllegalStateException();
		if (!members.remove(member))
			throw new IllegalStateException();
		if (members.isEmpty())
			group2members.remove(group);
	}

	@Override
	public Set<String> list(String group) {
		return group2members.get(group);
	}
}