package net.indiespot.distribution.energy;

import java.util.ArrayList;
import java.util.List;

import net.indiespot.distribution.Resource;
import net.indiespot.distribution.ResourceType;
import net.indiespot.path.CostFunction;
import net.indiespot.path.Dijkstra;
import net.indiespot.path.DijkstraPathFinder;
import net.indiespot.path.Edge;
import net.indiespot.path.Node;
import net.indiespot.path.VisitFunction;
import net.indiespot.path.DijkstraPathFinder.Instruction;

public class EnergyTest {
	public static void main(String[] args) {
		Pole p1 = new Pole(0, 0, new Generator(ResourceType.ELECTRICITY, 30));
		Pole p2 = new Pole(1, 0, new Resource(ResourceType.ELECTRICITY, 0, 10));
		Pole p3 = new Pole(1, 1, new Resource(ResourceType.ELECTRICITY, 0, 10));
		Pole p4 = new Pole(1, 10, null);
		Battery c = new Battery(4);
		c.maxInboundFlow = 1;
		Pole p5 = new Pole(1, 11, c);
		Pole p6 = new Pole(1, 12, null);
		Pole p7 = new Pole(1, 13, new Resource(ResourceType.ELECTRICITY, 0, 10));

		Wire.twoWay(p1, p2, 1.0f);
		Wire.twoWay(p2, p3, 1.0f);
		Wire.twoWay(p1, p3, 10.0f);

		Wire.twoWay(p3, p4, 10.0f);

		Wire.oneWay(p4, p5, 10.0f);
		Wire.twoWay(p5, p6, 10.0f);
		Wire.twoWay(p6, p7, 10.0f);

		List<Pole> poles = new ArrayList<>();
		poles.add(p1);
		poles.add(p2);
		poles.add(p3);
		poles.add(p4);
		poles.add(p5);
		poles.add(p6);
		poles.add(p7);

		//

		List<Pole> generators = new ArrayList<>();
		List<Pole> batteries = new ArrayList<>();

		for (Pole pole : poles) {
			if (pole.attachment instanceof Generator) {
				generators.add(pole);
			}
			if (pole.attachment instanceof Battery) {
				batteries.add(pole);
			}
		}

		for (int i = 0; i < 5; i++) {
			System.out.println("---");

			for (Pole pole : generators) {
				distribute(pole, ((Generator) pole.attachment).generate());
			}

			System.out.println("-");

			for (Pole pole : batteries) {
				distribute(pole, (Battery) pole.attachment);
			}
		}
	}

	private static final float BARRIER = Integer.MAX_VALUE;

	private static void distribute(final Pole pole, final Resource source) {

		CostFunction costFunction = new CostFunction() {
			@Override
			public float calculateCost(Edge edge) {
				Wire wire = (Wire) edge.attachment;

				if (edge.src.attachment != source)
					if (edge.src.attachment instanceof Battery)
						return BARRIER; // battery has no output,
										// unless it's the source of
										// the distribution

				int dx = wire.a.x - wire.b.x;
				int dy = wire.a.y - wire.b.y;
				float length = (float) Math.sqrt(dx * dx + dy * dy);

				return length * wire.resistancePerUnit;
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

				Pole pole = (Pole) node;
				System.out.println("visit: " + node + ", cost: " + cost + ", attachment: " + pole.attachment);

				if (pole.attachment instanceof Resource) {
					Resource consumer = (Resource) pole.attachment;
					int transfer = Resource.txn(source, consumer);
					System.out.println("\t transfer: amount: " + transfer + " ( " + source + " -> " + consumer + ")");

					if (transfer > 0) {
						for (Edge edge : finder.getPathInfo(node)) {
							((Wire) edge.attachment).accumulatedTransfer += transfer;
						}
					}
				}

				return Instruction.CONTINUE;
			}
		};

		Dijkstra.findPath(pole, null, costFunction, visitFunction);
	}
}
