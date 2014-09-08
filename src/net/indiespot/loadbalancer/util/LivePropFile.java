package net.indiespot.loadbalancer.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LivePropFile {
	private final LiveFile liveFile;
	private Map<String, Map<String, String>> propData;

	public LivePropFile(File file, long interval) {
		liveFile = new LiveFile(file, interval) {
			@Override
			public void onNewData(byte[] data) {
				Map<String, Map<String, String>> tmpPropData = new HashMap<>();
				Map<String, String> currentGroup = new HashMap<>();
				tmpPropData.put(null, currentGroup);
				String caption = null;
				for (String line : Text.splitOnLines(Text.utf8(data))) {
					line = Text.beforeIfAny(line, '#');
					line = line.trim();
					if (line.isEmpty()) {
						continue;
					}
					if (line.startsWith("[") && line.endsWith("]")) {
						caption = line.substring(1, line.length() - 1);
						tmpPropData.put(caption, currentGroup = new HashMap<>());
						continue;
					}

					String[] keyval = Text.splitPair(line, '=');
					if (keyval == null) {
						currentGroup.put(line.trim(), null);
						continue;
					}
					String key = keyval[0].trim();
					String val = keyval[1].trim();
					currentGroup.put(key, val);
				}
				propData = tmpPropData;

				onNewProps(propData);
			}
		};
	}

	protected void onNewProps(Map<String, Map<String, String>> groupMaps) {

	}

	public Map<String, String> data() {
		liveFile.data();
		return propData.get(null);
	}

	public Map<String, String> data(String groupName) {
		liveFile.data();
		return propData.get(groupName);
	}

	public boolean getBoolean(String groupName, String key, boolean def) {
		String val = data(groupName).get(key);
		return (val == null) ? def : Boolean.parseBoolean(val);
	}

	public int getInt(String groupName, String key, int def) {
		try {
			return Integer.parseInt(data(groupName).get(key));
		} catch (NullPointerException | NumberFormatException exc) {
			return def;
		}
	}
}
