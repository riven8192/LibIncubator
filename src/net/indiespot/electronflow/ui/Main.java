package net.indiespot.electronflow.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.indiespot.electronflow.Battery;
import net.indiespot.electronflow.Consumer;
import net.indiespot.electronflow.Generator;
import net.indiespot.electronflow.Node;
import net.indiespot.electronflow.Wire;
import net.indiespot.electronflow.World;

public class Main {
	public static void main(String[] args) {
		final World world = new World();
		Random rndm = new Random(123);

		for (int i = 0; i < 16; i++) {
			Node node;
			if (i < 2) {
				Generator generator = new Generator();
				generator.production = 350 + rndm.nextInt(25);
				node = generator;
			} else if (rndm.nextFloat() < 0.1f) {
				Battery battery = new Battery();
				battery.storageUsage = 0;
				battery.storageCapacity = 25000;
				battery.energyThreshold = 100;
				node = battery;
				node.capacity = 200;
			} else if (rndm.nextFloat() < 0.8f) {
				Consumer consumer = new Consumer();
				consumer.demand = 25 + rndm.nextInt(25);
				node = consumer;
				node.capacity = 200;
			} else {
				node = new Node();
				node.capacity = 200;
			}
			node.x = 50 + rndm.nextInt(1024 - 200);
			node.y = 50 + rndm.nextInt(768 - 100);
			node.energy = rndm.nextInt(node.capacity + 1);

			world.nodes.add(node);
		}

		for (int i = 0; i < world.nodes.size() * 4; i++) {
			do {
				Node n1 = world.nodes.get(rndm.nextInt(world.nodes.size()));
				Node n2 = world.nodes.get(rndm.nextInt(world.nodes.size()));
				if (n1 != n2) {
					new Wire(n1, n2);
					break;
				}
			} while (true);
		}

		final JPanel canvas = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				g.setColor(Color.BLUE);
				for (Node node : world.nodes) {
					for (Wire wire : node.wires) {
						g.drawLine(wire.a.x, wire.a.y, wire.b.x, wire.b.y);
					}
				}

				for (Node node : world.nodes) {
					String label = node.energy + "/" + node.capacity + " (in " + node.in + ", out " + node.out + ", c " + node.blocked + ")";
					if (node instanceof Generator) {
						g.setColor(Color.YELLOW);
						label += " (+" + ((Generator) node).production + ")";
					} else if (node instanceof Consumer) {
						g.setColor(((Consumer) node).isOnline() ? Color.GREEN : Color.RED);
						if (((Consumer) node).isOnline())
							label += " (-" + ((Consumer) node).demand + ")";
						else
							label += " (T: -" + ((Consumer) node).countdownUntilRestart + "s)";
					} else if (node instanceof Battery) {
						g.setColor(Color.CYAN);
						label += " (" + ((Battery) node).storageUsage + "/" + ((Battery) node).storageCapacity + ")";
					} else {
						g.setColor(Color.WHITE);
					}
					g.drawString(label, node.x - 15, node.y - 5);
					g.fillRect(node.x - 2, node.y - 2, 4, 4);
				}

			}
		};
		canvas.setBackground(Color.BLACK);
		canvas.setPreferredSize(new Dimension(1024, 768));

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(40);

						SwingUtilities.invokeAndWait(new Runnable() {

							@Override
							public void run() {
								long t0 = System.nanoTime();
								world.tick(3);
								long t1 = System.nanoTime();
								System.out.println("took: " + (t1 - t0) / 1000 + "us");
								canvas.repaint();
							}
						});
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		}).start();

		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
