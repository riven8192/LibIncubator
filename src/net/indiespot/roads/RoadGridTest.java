package net.indiespot.roads;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import craterstudio.math.Vec2;
import net.indiespot.roads.RoadBitGrid.RoadType;
import net.indiespot.roads.RoadJourneyStepper.State;
import net.indiespot.roads.verlet.Vec3;
import net.indiespot.roads.verlet.VerletParticle;
import net.indiespot.roads.verlet.VerletSpring;

public class RoadGridTest {
	public static void main(String[] args) {

		List<RoadTile> tiles = new ArrayList<>();
		tiles.add(new RoadTile(5 + 0, 5 + 0));
		tiles.add(new RoadTile(5 + 1, 5 + 0));
		tiles.add(new RoadTile(5 + 2, 5 + 0));
		tiles.add(new RoadTile(5 + 2, 5 + -1));
		tiles.add(new RoadTile(5 + 3, 5 + -1));
		tiles.add(new RoadTile(5 + 3, 5 + -2));
		tiles.add(new RoadTile(5 + 3, 5 + -3));
		tiles.add(new RoadTile(5 + 2, 5 + -3));
		tiles.add(new RoadTile(5 + 2, 5 + -4));
		tiles.add(new RoadTile(5 + 1, 5 + -4));
		tiles.add(new RoadTile(5 + 0, 5 + -4));
		tiles.add(new RoadTile(5 - 1, 5 + -4));
		tiles.add(new RoadTile(5 - 2, 5 + -4));
		tiles.add(new RoadTile(5 - 3, 5 + -4));
		tiles.add(new RoadTile(5 - 3, 5 + -3));
		tiles.add(new RoadTile(5 - 3, 5 + -2));
		tiles.add(new RoadTile(5 - 3, 5 + -1));
		tiles.add(new RoadTile(5 - 3, 5 + 0));
		tiles.add(new RoadTile(5 - 2, 5 + 0));
		tiles.add(new RoadTile(5 - 1, 5 + 0));

		final RoadBitGrid bitGrid = new RoadBitGrid();
		for (RoadTile tile : tiles) {
			bitGrid.set(tile.x, tile.y);
		}

		final RoadTileGrid grid = new RoadTileGrid();
		for (RoadTile tile : tiles) {
			grid.put(tile);
		}

		List<RoadTile> route = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			for (RoadTile tile : tiles) {
				route.add(tile);
			}
		}

		final RoadJourney journey = new RoadJourney(route);
		RoadJourneyVehicle stepper1 = new RoadJourneyVehicle(journey, 5 + 4);
		RoadJourneyVehicle stepper2 = new RoadJourneyVehicle(journey, 5 + 4);
		RoadJourneyVehicle stepper3 = new RoadJourneyVehicle(journey, 5 + 4);

		//

		class Vehicle {
			private final RoadJourneyVehicle vehicle;

			private float minSubTilesPerStep = 0.010f;
			private float maxSubTilesPerStep = 0.060f;
			private float accSubTilesPerStep = 0.001f;

			public Vehicle(RoadJourneyVehicle vehicle) {
				this.vehicle = vehicle;
			}

			public void tick() {

				float min = minSubTilesPerStep;
				float max = maxSubTilesPerStep;
				float acc = accSubTilesPerStep;

				RoadTile tile = vehicle.head().getTile();
				switch (bitGrid.getRoadType(tile.x, tile.y)) {
				case STRAIGHT:
					max *= 1.00f;
					acc *= 1.00f;
					break;
				case CORNER:
					max *= 0.40f;
					acc *= 0.40f;
					break;
				case INTERSECTION:
					max *= 0.30f;
					acc *= 0.30f;
					break;
				default:
					break;
				}

				State state = vehicle.step();

				if (state == State.MOVING)
					vehicle.subTilesPerStep += acc;
				else if (state == State.BLOCKED)
					vehicle.subTilesPerStep *= 0.75f;

				vehicle.subTilesPerStep = Math
						.max(min, vehicle.subTilesPerStep);
				vehicle.subTilesPerStep = Math
						.min(max, vehicle.subTilesPerStep);
			}
		}

		class VerletVehicle {
			private final Vehicle vehicle;
			List<VerletParticle> roadNodes = new ArrayList<>();
			List<VerletParticle> swingNodes = new ArrayList<>();
			List<VerletParticle> vehicleNodes = new ArrayList<>();

			List<VerletSpring> roadSwingSprings = new ArrayList<>();
			List<VerletSpring> swingVehicleSprings = new ArrayList<>();
			List<VerletSpring> vehicleVehicleSprings = new ArrayList<>();

			public VerletVehicle(int segmentCount, Vehicle vehicle) {
				this.vehicle = vehicle;

				float x = 0;

				for (int i = 0; i < segmentCount + 1; i++) {
					VerletParticle roadNode = new VerletParticle();
					roadNode.invWeight = 0.00f;
					roadNode.setPosition(x += 1, 0, 0);
					roadNodes.add(roadNode);

					VerletParticle swingNode = new VerletParticle();
					swingNode.invWeight = 0.01f;
					swingNode.setPosition(x += 1, 0, 0);
					swingNodes.add(swingNode);

					VerletParticle vehicleNode = new VerletParticle();
					vehicleNode.invWeight = 1.00f;
					vehicleNode.setPosition(x += 1, 0, 0);
					vehicleNodes.add(vehicleNode);

					VerletSpring roadSwing = new VerletSpring(roadNode,
							swingNode);
					roadSwing.how = VerletSpring.ENFORCE_FIXED_LENGTH;
					roadSwing.len = 0.025f;
					roadSwing.stf = 0.01f + 0.03f;
					roadSwingSprings.add(roadSwing);

					VerletSpring swingVehicle = new VerletSpring(swingNode,
							vehicleNode);
					swingVehicle.how = VerletSpring.ENFORCE_FIXED_LENGTH;
					swingVehicle.len = 0.025f;
					swingVehicle.stf = 0.02f + 0.02f;
					swingVehicleSprings.add(swingVehicle);

					if (i > 0) {
						VerletParticle prevVehicleNode = vehicleNodes
								.get(vehicleNodes.size() - 1 - 1);
						VerletSpring vehicleVehicle = new VerletSpring(
								prevVehicleNode, vehicleNode);
						vehicleVehicle.how = VerletSpring.ENFORCE_FIXED_LENGTH;
						vehicleVehicle.len = 0.5f;
						vehicleVehicle.stf = 1.0f;
						vehicleVehicleSprings.add(vehicleVehicle);
					}
				}
			}

			public void setVelocity(float subTilesPerStep) {
				for (VerletSpring vs : roadSwingSprings)
					vs.stf = subTilesPerStep * 0.5f;
				for (VerletSpring vs : swingVehicleSprings)
					vs.stf = subTilesPerStep * 1.0f;
			}

			public void tick() {
				final float dragFactor = 0.66f;

				int h = 0;
				for (VerletParticle vp : roadNodes) {
					vp.tick();
					Vec2 prev = vehicle.vehicle.getHistory(h);
					if (prev != null) {
						vp.setPosition(prev.x, prev.y, 0.0f);
						vp.setVelocity(0.0f, 0.0f, 0.0f);
					}
					h += 2;
				}

				for (VerletParticle vp : swingNodes)
					vp.tick();
				for (VerletParticle vp : vehicleNodes)
					vp.tick();

				for (VerletParticle vp : roadNodes)
					vp.mulVelocity(dragFactor);
				for (VerletParticle vp : swingNodes)
					vp.mulVelocity(dragFactor);
				for (VerletParticle vp : vehicleNodes)
					vp.mulVelocity(dragFactor);

				for (VerletSpring vs : roadSwingSprings)
					vs.tick();
				for (VerletSpring vs : swingVehicleSprings)
					vs.tick();
				for (VerletSpring vs : vehicleVehicleSprings)
					vs.tick();

				for (VerletParticle vp : roadNodes)
					vp.mulVelocity(dragFactor);
				for (VerletParticle vp : swingNodes)
					vp.mulVelocity(dragFactor);
				for (VerletParticle vp : vehicleNodes)
					vp.mulVelocity(dragFactor);
			}
		}

		final Vehicle v1 = new Vehicle(stepper1);
		final Vehicle v2 = new Vehicle(stepper2);
		final Vehicle v3 = new Vehicle(stepper3);
		final VerletVehicle vv1 = new VerletVehicle(3, v1);
		final VerletVehicle vv2 = new VerletVehicle(3, v2);
		final VerletVehicle vv3 = new VerletVehicle(3, v3);
		v1.accSubTilesPerStep *= 2.0f;
		v1.maxSubTilesPerStep *= 2.0f;

		JPanel canvas = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				int scale = 64;
				float hw = 5.0f;

				if (false) {
					for (int y = 0; y < 20; y++) {
						for (int x = 0; x < 20; x++) {
							RoadTile tile = grid.get(x, y);
							if (tile == null)
								continue;
							drawRoadTile(g, tile, scale);
						}
					}

					drawVehicleHead(g, v1.vehicle, scale);
					drawVehicleHead(g, v2.vehicle, scale);
					drawVehicleHead(g, v3.vehicle, scale);
				} else {
					for (int y = 0; y < 20; y++) {
						for (int x = 0; x < 20; x++) {
							RoadTile tile = grid.get(x, y);
							if (tile == null)
								continue;
							drawRoadTile2(g, tile, scale);
						}
					}
				}

				{
					long tm1 = System.nanoTime();
					long t0 = System.nanoTime();
					v1.tick();
					v2.tick();
					v3.tick();
					long t1 = System.nanoTime();

					vv1.setVelocity(v1.vehicle.subTilesPerStep);
					vv2.setVelocity(v2.vehicle.subTilesPerStep);
					vv3.setVelocity(v3.vehicle.subTilesPerStep);

					vv1.tick();
					vv2.tick();
					vv3.tick();
					long t2 = System.nanoTime();
					// System.out.println("3a took: " + (t1 - t0) + "ns");
					// System.out.println("3b took: " + (t2 - t1) / 1000 +
					// "us");
					// System.out.println(t0 - tm1);

					g.setColor(Color.BLUE);
					for (VerletVehicle vv : new VerletVehicle[] { vv1, vv2, vv3 }) {
						Vec3 p1 = vv.vehicleNodes.get(0).now;
						Vec3 p2 = vv.vehicleNodes.get(1).now;
						Vec3 p3 = vv.vehicleNodes.get(2).now;
						Vec3 p4 = vv.vehicleNodes.get(3).now;

						this.drawBus(g, p1, p2, scale, hw);
						this.drawBus(g, p2, p3, scale, hw);
						this.drawBus(g, p3, p4, scale, hw);

						if (false) {
							for (VerletSpring vs : vv.roadSwingSprings) {
								this.drawSpring(g, vs, scale);
							}
							for (VerletSpring vs : vv.swingVehicleSprings) {
								this.drawSpring(g, vs, scale);
							}
							for (VerletSpring vs : vv.vehicleVehicleSprings) {
								this.drawSpring(g, vs, scale);
							}
						}
					}
				}

				try {
					Thread.sleep(10);
				} catch (Exception exc) {

				}

				this.repaint();
			}

			private void drawSpring(Graphics g, VerletSpring spring, float scale) {
				Vec2 p1 = new Vec2();
				Vec2 p2 = new Vec2();
				p1.x = (int) (spring.a.now.x * scale);
				p1.y = (int) (spring.a.now.y * scale);
				p2.x = (int) (spring.b.now.x * scale);
				p2.y = (int) (spring.b.now.y * scale);
				g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
			}

			private void drawBus(Graphics g, Vec3 head, Vec3 tail, float scale,
					float r) {
				float nx, ny;
				{
					float dx = head.x - tail.x;
					float dy = head.y - tail.y;
					float dd = (float) Math.sqrt(dx * dx + dy * dy);
					nx = dx / dd;
					ny = dy / dd;
				}

				// rotate 90deg
				float dx = ny;
				float dy = -nx;

				Vec2 p1 = new Vec2();
				Vec2 p2 = new Vec2();
				Vec2 p3 = new Vec2();
				Vec2 p4 = new Vec2();
				p1.x = (int) (head.x * scale + dx * +r);
				p1.y = (int) (head.y * scale + dy * +r);
				p2.x = (int) (head.x * scale + dx * -r);
				p2.y = (int) (head.y * scale + dy * -r);
				p3.x = (int) (tail.x * scale + dx * +r);
				p3.y = (int) (tail.y * scale + dy * +r);
				p4.x = (int) (tail.x * scale + dx * -r);
				p4.y = (int) (tail.y * scale + dy * -r);

				Polygon p = new Polygon();
				p.addPoint((int) p1.x, (int) p1.y);
				p.addPoint((int) p2.x, (int) p2.y);
				p.addPoint((int) p4.x, (int) p4.y);
				p.addPoint((int) p3.x, (int) p3.y);
				g.fillPolygon(p);
			}

			private void drawRoadTile2(Graphics g, RoadTile tile, int scale) {
				for (int y = 0; y < 4; y++) {
					for (int x = 0; x < 4; x++) {
						Color c = Color.GRAY;
						g.setColor(c);
						g.fillRect(tile.x * 64 + x * 16, tile.y * 64 + y * 16,
								16, 16);
					}
				}
			}

			private void drawRoadTile(Graphics g, RoadTile tile, int scale) {
				for (int y = 0; y < 4; y++) {
					for (int x = 0; x < 4; x++) {
						Color c;
						int bit = RoadTile.coordsToBit(x, y);
						if (!tile.isEmpty(bit) && !tile.isEnterAllowed(bit)) {
							c = Color.ORANGE;
						} else if (!tile.isEmpty(bit)) {
							c = Color.GREEN;
						} else if (!tile.isEnterAllowed(bit)) {
							c = Color.RED;
						} else {
							c = Color.GRAY;
						}

						g.setColor(c);
						g.fillRect(tile.x * scale + x * scale / 4, tile.y
								* scale + y * scale / 4, scale / 4, scale / 4);
					}
				}
			}

			private void drawVehicleHead(Graphics g,
					RoadJourneyVehicle vehicle, int scale) {
				RoadTile tile = vehicle.head().getTile();
				int bit = vehicle.head().getTileBit();
				if (bit == -1)
					return;

				int x = RoadTile.bitToX(bit);
				int y = RoadTile.bitToY(bit);

				g.setColor(Color.WHITE);
				g.fillRect(tile.x * scale + x * scale / 4, tile.y * scale + y
						* scale / 4, scale / 4, scale / 4);

				Vec2 coords = vehicle.getHistory(0);
				if (coords == null)
					return;
				int r = 5;
				g.setColor(Color.BLACK);
				g.fillOval((int) (coords.x * scale) - r,
						(int) (coords.y * scale) - r, r * 2, r * 2);
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(800, 600);
			}
		};

		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 1) {
					RoadTile t = grid.get(5 + 3, 5 + -1);
					t.setEnterAllowed(14, !t.isEnterAllowed(14));
				} else if (e.getButton() == 3) {
					RoadTile tile = v1.vehicle.head().getTile();
					if (bitGrid.getRoadType(tile.x, tile.y) == RoadType.STRAIGHT) {
						v1.vehicle.head().switchLanes();
					}
				}
			}
		});

		JFrame frame = new JFrame();
		frame.setTitle("RoadGrid");
		frame.getContentPane().add(canvas);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
