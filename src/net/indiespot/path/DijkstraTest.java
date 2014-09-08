package net.indiespot.path;

import java.util.Collections;

public class DijkstraTest {
	private static class Vec2 {
		public final float x, y;

		public Vec2(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	public static void main(String[] args) {

		Node a1 = new Node(new Vec2(0, 0));
		Node b1 = new Node(new Vec2(1, 0));
		Node c1 = new Node(new Vec2(1, 1));
		Node d1 = new Node(new Vec2(0, 1));

		new Edge(a1, b1, Float.valueOf(1.0f));
		new Edge(b1, c1, Float.valueOf(1.0f));
		new Edge(c1, d1, Float.valueOf(1.0f));
		new Edge(d1, a1, Float.valueOf(1.0f));

		//

		Node a2 = new Node(new Vec2(1, 1));
		Node b2 = new Node(new Vec2(2, 1));
		Node c2 = new Node(new Vec2(2, 2));
		Node d2 = new Node(new Vec2(1, 2));

		new Edge(a2, b2, Float.valueOf(1.0f));
		new Edge(b2, c2, Float.valueOf(1.0f));
		new Edge(c2, d2, Float.valueOf(1.0f));
		new Edge(d2, a2, Float.valueOf(1.0f));

		//

		new Edge(c1, a2, Float.valueOf(20.0f));

		//

		final float maxVelocity = 10.0f;

		//

		TargetFunction tf = new TargetFunction() {
			@Override
			public boolean isTarget(Node node) {
				Vec2 v = (Vec2) node.attachment;
				return (v.x == 1 && v.y == 2);
			}
		};

		CostFunction cf = new CostFunction() {
			@Override
			public float calculateCost(Edge edge) {
				float distance;
				{
					Vec2 a = (Vec2) edge.from.attachment;
					Vec2 b = (Vec2) edge.to.attachment;
					float dx = a.x - b.x;
					float dy = a.y - b.y;
					distance = (float) Math.sqrt(dx * dx + dy * dy);
				}

				float halfDistance = distance * 0.5f;
				float accelerateDistance = Math.min(halfDistance, 0.2f);
				float decelerateDistance = Math.min(halfDistance, 0.1f);
				float topVelocityDistance = distance - accelerateDistance
						- decelerateDistance;

				float time = 0.0f;
				time += accelerateDistance / (maxVelocity * 0.5f);
				time += topVelocityDistance / (maxVelocity * 1.0f);
				time += decelerateDistance / (maxVelocity * 0.5f);

				float delay = (edge.attachment == null) ? 0.0f
						: ((Float) edge.attachment).floatValue();
				return time + delay;
			}
		};

		StopFunction sf = new StopFunction() {
			@Override
			public boolean shouldStop(Node best, float cost) {
				return false;
			}
		};

		System.out.println(Dijkstra.findPath(Collections.singleton(a1), tf, cf,
				sf).getPathInfo());
	}
}
