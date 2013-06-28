package net.indiespot.audio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import craterstudio.math.EasyMath;

public class SampleVisualizer {
	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	public static void show(final int w, final int h, float[]... dataSets_) {

		final float[][] dataSets = new float[dataSets_.length][];
		for (int i = 0; i < dataSets.length; i++) {
			dataSets[i] = dataSets_[i].clone();
		}

		float min = Integer.MAX_VALUE;
		float max = Integer.MIN_VALUE;
		for (float[] dataSet : dataSets) {
			for (float f : dataSet) {
				min = Math.min(min, f);
				max = Math.max(max, f);
			}
		}
		final float range = max - min;
		final float uiMin = min - range * 0.05f;
		final float uiMax = max + range * 0.05f;

		System.out.println("UI range: " + min + " .. " + max);

		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				g.setColor(new Color(0, 32, 64));
				g.fillRect(0, 0, this.getWidth(), this.getHeight());

				int xOff = 0;
				int xEnd = this.getWidth();
				int yOff = 0;
				int yEnd = this.getHeight();

				int p = 0;
				for (float[] data : dataSets) {
					switch (p++ % 3) {
						case 0:
							g.setColor(new Color(64, 96, 128));
							break;
						case 1:
							g.setColor(new Color(64, 128, 96));
							break;
						case 2:
							g.setColor(new Color(128, 64, 96));
							break;
					}

					int dataOff = 0;
					int dataEnd = data.length;

					int x2 = 0;
					int y2 = 0;
					for (int i = dataOff; i < dataEnd; i++) {
						int x = (int) EasyMath.interpolate(i, dataOff, dataEnd, xOff, xEnd);
						int y = (int) EasyMath.interpolate(data[i], uiMin, uiMax, yOff, yEnd);
						if (i > dataOff) {
							g.drawLine(x2, y2, x, y);
						}
						x2 = x;
						y2 = y;
					}
				}
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(w, h);
			}
		};

		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(panel);
		frame.setResizable(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
