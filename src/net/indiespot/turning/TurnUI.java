package net.indiespot.turning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class TurnUI {
	public static void main(String[] args) {

		final Point item = new Point(400, 300);
		final Point target = new Point(350, 400);
		final Rot2D currAngle = Rot2D.fromVector(target.x - item.x, target.y - item.y);
		final Rot2D turnSpeedCW = Rot2D.fromDegrees(1.0);
		final Rot2D turnSpeedCCW = Rot2D.fromDegrees(-1.0);

		@SuppressWarnings("serial")
		JPanel panel = new JPanel() {

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);

				final Rot2D wantedAngle = Rot2D.fromVector(target.x - item.x, target.y - item.y);

				double cross1 = Rot2D.cross(currAngle, wantedAngle);
				if (cross1 > 0.0)
					currAngle.rotate(turnSpeedCW);
				else
					currAngle.rotate(turnSpeedCCW);
				double cross2 = Rot2D.cross(currAngle, wantedAngle);

				if (Math.signum(cross1) != Math.signum(cross2))
					currAngle.load(wantedAngle);
				
				

				g.setColor(Color.BLACK);
				g.fillRect(item.x - 3, item.y - 3, 6, 6);
				g.drawLine(item.x, item.y, //
						(int) (item.x + currAngle.cos * 64), //
						(int) (item.y + currAngle.sin * 64));

				g.setColor(Color.RED);
				g.fillRect(target.x - 1, target.y - 1, 2, 2);

				// lousy game-loop:
				try {
					Thread.sleep(1000 / 50); // ~20fps
				} catch (InterruptedException exc) {
					// ok
				}
				this.repaint();
			}
		};

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					target.x = e.getX();
					target.y = e.getY();
				}
			}
		});

		panel.setPreferredSize(new Dimension(800, 600));

		JFrame frame = new JFrame("Rot2D");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(panel);
		frame.setResizable(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
