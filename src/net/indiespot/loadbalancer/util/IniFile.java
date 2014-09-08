package net.indiespot.loadbalancer.util;

import java.util.HashMap;
import java.util.Map;

public class IniFile
{
	public static Map<String, Map<String, String>> parse(byte[] data)
	{
		Map<String, Map<String, String>> section2props = new HashMap<String, Map<String, String>>();
		Map<String, String> current = null;

		for(String line : Text.splitOnLines(Text.ascii(data)))
		{
			line = line.trim();
			if(line.isEmpty())
				continue;

			if(line.startsWith("[") && line.endsWith("]"))
			{
				section2props.put(line.substring(1, line.length() - 1).trim(), current = new HashMap<>());
			}
			else
			{
				String[] kv = Text.splitPair(line, '=');
				if(kv != null)
					current.put(kv[0].trim(), kv[1].trim());
			}
		}

		return section2props;
	}
}
