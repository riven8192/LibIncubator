package net.indiespot.electronflow;

import java.util.ArrayList;
import java.util.List;

public class World {
	public List<Node> nodes = new ArrayList<>();

	public void tick(int n) {
		for (Node node : nodes) {
			node.in = node.out = node.blocked = 0;
		}

		for (Node node : nodes) {
			if (node instanceof Generator) {
				((Generator) node).generate();
			}
		}

		for (int i = 0; i < n; i++) {
			for (Node node : nodes) {
				node.distribute();
			}
		}

		for (Node node : nodes) {
			if (node instanceof Consumer) {
				((Consumer) node).consume();
			}
		}

		for (int i = 0; i < n; i++) {
			for (Node node : nodes) {
				node.distribute();
			}
		}
	}
}
