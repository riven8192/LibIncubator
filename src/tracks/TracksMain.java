package tracks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;

import craterstudio.func.Callback;
import craterstudio.func.Filter;
import craterstudio.util.HighLevel;
import craterstudio.math.Rotation;
import craterstudio.math.Vec2;

public class TracksMain {

	public static void main(String[] args) {

		TrackBuilder builder = new TrackBuilder();
		builder.init(new Vec2(160, 160), 9.389147f);
		builder.addStraight(97);
		builder.addCurve(128, +90.0f);
		builder.addCurve(32, +135.0f);
		builder.addCurve(128, -270.0f);
		builder.addStraight(256);
		builder.addCurve(48, +45.0f);
		//SwitchTrack st = builder.addSwitch();
		SwitchTrack switcher;
		if (true) {
			switcher = builder.addSwitch();
			{
				TrackBuilder builder2 = new TrackBuilder();
				builder2.init(switcher);
				builder2.addStraight(128);
				builder2.addCurve(96, +225.0f);
				builder2.addCurve(64, -60.0f);
				builder2.addStraight(128);
				builder2.build();
			}
		}
		builder.addCurve(128, -180.0f);
		builder.addStraight(384);
		builder.addCurve(60.5f, -180.0f);
		final Track track1 = builder.build();

		// create the same track, parallel to this one
		final Track track2 = TrackBuilder.buildParallel(track1, -20.0f, new HashSet<Track>());

		switcher.setSwitch(0);

		final TrackUnitPair wagon1;
		{
			wagon1 = ChainBuilder.create(track1, 48, 24, 48, 24, 48);
			wagon1.setDirection(true);
			wagon1.setPosition(-300.0f);
		}

		final TrackUnitPair wagon2;
		{
			wagon2 = ChainBuilder.create(track2, 64, 32, 64, 32, 64, 32, 64, 32, 64, 32, 64, 32, 64, 32, 64);
			wagon2.setDirection(true);
			wagon2.setPosition(900.0f);
		}

		JPanel panel = new JPanel() {

			@Override
			protected void paintComponent(final Graphics g) {
				super.paintComponent(g);

				final Set<Track> visited = new HashSet<>();
				Filter<Track> drawTrack = new Filter<Track>() {

					@Override
					public boolean accept(Track track) {
						do {
							//System.out.println(track);
							if (!visited.add(track)) {
								break;
							}

							if (track instanceof SwitchTrack) {
								for (Track t : ((SwitchTrack) track).tracks) {
									this.accept(t);
								}
							}
						} while ((track = track.next) != null);

						return true;
					}
				};

				for (final Track track : new Track[] {track1, track2}) {
					drawTrack.accept(track);
				}

				Vec2 dst = new Vec2();

				g.setColor(Color.BLACK);
				for (Track t : visited) {
					for (int i = 0, len = (int) (t.length * 1.1f); i < len; i++) {
						t.getPositionAtRatio((float) i / (len - 1), dst);
						g.fillRect((int) dst.x, (int) dst.y, 1, 1);
					}
				}

				for (TrackUnitPair wagon : new TrackUnitPair[] {wagon1, wagon2}) {
					g.setColor(Color.BLUE);
					int dim = 16;
					do {

						g.fillOval((int) wagon.head.pos.x - dim / 2, (int) wagon.head.pos.y - dim / 2, dim, dim);
						dim -= 1;
						g.fillOval((int) wagon.tail.pos.x - dim / 2, (int) wagon.tail.pos.y - dim / 2, dim, dim);
					} while ((wagon = wagon.next) != null);
				}

				for (TrackUnitPair wagon : new TrackUnitPair[] {wagon1, wagon2}) {
					g.setColor(Color.BLACK);
					wagon.visitUnits(new Callback<TrackUnit>() {
						@Override
						public void callback(TrackUnit unit) {
							Vec2 rot = Rotation.fromDegrees(unit.angle).rotate(new Vec2(0, 16));

							g.drawLine((int) (unit.pos.x + rot.x),//
							        (int) (unit.pos.y + rot.y),//
							        (int) (unit.pos.x - rot.x),//
							        (int) (unit.pos.y - rot.y));
						}
					});
				}

				for (TrackUnitPair wagon : new TrackUnitPair[] {wagon1, wagon2}) {
					g.setColor(Color.RED);
					do {
						Rotation r = Rotation.fromVector(new Vec2(wagon.head.pos).sub(wagon.tail.pos));

						Vec2 p1 = new Vec2(wagon.head.pos).add(r.rotate(new Vec2(+8, 12)));
						Vec2 p2 = new Vec2(wagon.head.pos).sub(r.rotate(new Vec2(-8, 12)));
						Vec2 p3 = new Vec2(wagon.tail.pos).sub(r.rotate(new Vec2(+8, 12)));
						Vec2 p4 = new Vec2(wagon.tail.pos).add(r.rotate(new Vec2(-8, 12)));

						g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
						g.drawLine((int) p2.x, (int) p2.y, (int) p3.x, (int) p3.y);
						g.drawLine((int) p3.x, (int) p3.y, (int) p4.x, (int) p4.y);
						g.drawLine((int) p4.x, (int) p4.y, (int) p1.x, (int) p1.y);
					} while ((wagon = wagon.next) != null && (wagon = wagon.next) != null);
				}

				float speedFactor = 2.5f;
				wagon1.move(speedFactor * 2.50f);
				wagon2.move(speedFactor * 0.75f);

				HighLevel.sleep(10);
				this.repaint();
			}
		};

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					wagon1.setDirection(!wagon1.getDirection());
				}

				if (e.getButton() == MouseEvent.BUTTON3) {
					wagon2.setDirection(!wagon2.getDirection());
				}
			}
		});

		panel.setPreferredSize(new Dimension(800, 600));

		JFrame frame = new JFrame("Tracks 'n Trains");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(panel);
		frame.setResizable(true);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
