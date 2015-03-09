package net.indiespot.diff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TestDiff {
	public static void main(String[] args) throws Exception {
		Item item = new Item();

		Map<Field, Object> rev1 = props(item);
		System.out.println(rev1);

		item.id = 1337_000L;

		Map<Field, Object> rev2 = props(item);
		System.out.println(rev2);

		item.id = 1337_000L;
		item.name = "duh";

		Map<Field, Object> rev3 = props(item);
		System.out.println(rev3);

		System.out.println(diff(rev1, rev2));
		System.out.println(diff(rev2, rev3));
	}

	public static class Hufter {
		long id = 3;
	}

	public static class Abstract extends Hufter {
		long id = 4;
	}

	public static class Item extends Abstract {
		long id = 5;
		Item parent;
		String name;
	}

	private static Set<Field> diff(Map<Field, Object> rev1, Map<Field, Object> rev2) throws Exception {
		Set<Field> fields = new HashSet<>();

		for (Entry<Field, Object> entry1 : rev1.entrySet()) {
			Field field = entry1.getKey();
			Object value1 = entry1.getValue();
			Object value2 = rev2.get(entry1.getKey());

			if (value1 == value2)
				continue; // handles identity

			if (field.getType().isPrimitive() && value1.equals(value2))
				continue; // handles auto-boxing

			fields.add(field);
		}

		return fields;
	}

	private static Map<Field, Object> props(Object object) throws Exception {
		Map<Field, Object> props = new HashMap<>();
		for (Field field : fields(object.getClass()))
			props.put(field, field.get(object));
		return props;
	}

	private static List<Field> fields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (; type != null; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				field.setAccessible(true);
				fields.add(field);
			}
		}
		return fields;
	}
}
