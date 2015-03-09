package net.indiespot.distribution;

import java.util.ArrayList;
import java.util.List;

import net.indiespot.path.CostFunction;
import net.indiespot.path.Dijkstra;
import net.indiespot.path.DijkstraPathFinder;
import net.indiespot.path.Edge;
import net.indiespot.path.Node;
import net.indiespot.path.VisitFunction;
import net.indiespot.path.DijkstraPathFinder.Instruction;

public class EnergyTest {
	public static void main(String[] args) {
		Node2D p1 = new Node2D(0, 0, new Generator(30));
		Node2D p2 = new Node2D(1, 0, new Resource(0, 10));
		Node2D p3 = new Node2D(1, 1, new Resource(0, 10));
		Node2D p4 = new Node2D(1, 10, null);
		Buffer c = new Buffer(4);
		c.maxInboundFlow = 1;
		Node2D p5 = new Node2D(1, 11, c);
		Node2D p6 = new Node2D(1, 12, null);
		Node2D p7 = new Node2D(1, 13, new Consumer(3, 2, 10));

		Connection2D.twoWay(p1, p2, 1.0f);
		Connection2D.twoWay(p2, p3, 1.0f);
		Connection2D.twoWay(p1, p3, 10.0f);

		Connection2D.twoWay(p3, p4, 10.0f);

		Connection2D.oneWay(p4, p5, 10.0f);
		Connection2D.twoWay(p5, p6, 10.0f);
		Connection2D.twoWay(p6, p7, 10.0f);

		List<Node2D> poles = new ArrayList<>();
		poles.add(p1);
		poles.add(p2);
		poles.add(p3);
		poles.add(p4);
		poles.add(p5);
		poles.add(p6);
		poles.add(p7);

		//

		List<Node2D> generators = new ArrayList<>();
		List<Node2D> batteries = new ArrayList<>();
		List<Consumer> consumers = new ArrayList<>();

		for (Node2D pole : poles) {
			if (pole.attachment instanceof Generator)
				generators.add(pole);
			else if (pole.attachment instanceof Buffer)
				batteries.add(pole);
			else if (pole.attachment instanceof Consumer)
				consumers.add((Consumer) pole.attachment);
		}

		for (int i = 0; i < 10; i++) {
			System.out.println("---");

			for (Node2D pole : generators) {
				Generator generator = (Generator) pole.attachment;
				Resource generated = generator.generate();
				if (generated != null)
					distribute(pole, generated);
			}

			System.out.println("-");

			for (Node2D pole : batteries) {
				Buffer battery = (Buffer) pole.attachment;
				distribute(pole, battery);
			}

			System.out.println("-");

			for (Consumer consumer : consumers) {
				consumer.tick();
			}
		}
	}

	private static final float BARRIER = Integer.MAX_VALUE;

	private static void distribute(final Node2D pole, final Resource source) {

		CostFunction costFunction = new CostFunction() {
			@Override
			public float calculateCost(Edge edge) {
				Connection2D wire = (Connection2D) edge.attachment;

				if (edge.src.attachment != source)
					if (edge.src.attachment instanceof Buffer)
						return BARRIER; // buffer has no output,
										// unless it's the source of
										// the distribution

				int dx = wire.a.x - wire.b.x;
				int dy = wire.a.y - wire.b.y;
				float length = (float) Math.sqrt(dx * dx + dy * dy);

				return length * wire.costPerUnit;
			}
		};

		VisitFunction visitFunction = new VisitFunction() {
			@Override
			public Instruction onReached(DijkstraPathFinder finder, Node node, float cost) {
				if (node == pole) // self
					return Instruction.CONTINUE;
				if (cost >= BARRIER)
					return Instruction.ABORT; // blocked
				if (source.isEmpty())
					return Instruction.ABORT; // drained

				Node2D pole = (Node2D) node;
				System.out.println("visit: " + node + ", cost: " + cost + ", attachment: " + pole.attachment);

				if (pole.attachment instanceof Resource) {
					Resource consumer = (Resource) pole.attachment;
					int transfer = Resource.txn(source, consumer);
					System.out.println("\t transfer: amount: " + transfer + " ( " + source + " -> " + consumer + ")");

					if (transfer > 0) {
						for (Edge edge : finder.getPathInfo(node)) {
							((Connection2D) edge.attachment).accumulatedTransfer += transfer;
						}
					}
				}

				return Instruction.CONTINUE;
			}
		};

		Dijkstra.scan(pole, costFunction, visitFunction);
	}
}
