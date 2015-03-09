package net.indiespot.java4k;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.indiespot.java4k.entries.OddEntry1;

@SuppressWarnings("serial")
public abstract class Java4kRev1 extends JPanel {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				init(new OddEntry1(), 800, 600);
			}
		});
	}

	protected static long now() {
		return System.nanoTime() / 1_000_000L;
	}

	static long started_at = now();

	protected static int elapsed() {
		return (int) (now() - started_at);
	}

	protected String name = "";
	protected int w, h;
	protected Mouse mouse = new Mouse();

	private JFrame frame;
	private long lastSecondTimestamp = now();
	private int updateCounter;
	private int renderCounter;
	private long updateNanos;
	private long renderNanos;

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		this.renderWrapper(g);

		if (now() > lastSecondTimestamp + 1000L) {
			int avgUpdateTook = (updateCounter == 0) ? 0 : (int) (updateNanos / updateCounter / 100_000L);
			int avgRenderTook = (renderCounter == 0) ? 0 : (int) (renderNanos / renderCounter / 100_000L);

			String stats = "";
			stats += "update: " + (avgUpdateTook / 10) + "." + (avgUpdateTook % 10) + "ms/" + updateCounter + "Hz";
			stats += ", render: " + (avgRenderTook / 10) + "." + (avgRenderTook % 10) + "ms/" + renderCounter + "fps";
			frame.setTitle(name + " - Java4K (rev 1) - " + stats);

			renderCounter = 0;
			updateCounter = 0;
			updateNanos = 0;
			renderNanos = 0;

			lastSecondTimestamp += 1000L;
		}
	}

	final void renderWrapper(Graphics g) {
		this.w = this.getWidth();
		this.h = this.getHeight();

		renderNanos -= System.nanoTime();
		this.render((Graphics2D) g);
		renderNanos += System.nanoTime();

		renderCounter++;
	}

	final void updateWrapper() {
		updateNanos -= System.nanoTime();
		this.update();
		updateNanos += System.nanoTime();

		updateCounter++;
	}

	public void update() {

	}

	public abstract void render(Graphics2D g);

	public static class Mouse {
		public int x, y;
		public Point pressed;
		public Point released;
		public Point clicked;
		public Rectangle dragArea;
		public Dimension dragMove;
	}

	private static void init(final Java4kRev1 java4k, int w, int h) {
		JFrame frame = new JFrame();
		{
			java4k.frame = frame;
			java4k.setPreferredSize(new Dimension(w, h));
			java4k.setBackground(Color.BLACK);

			frame.setTitle(java4k.name);
			frame.getContentPane().add(java4k, BorderLayout.CENTER);
		}
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		MouseAdapter mouseHandler = createMouseHandler(java4k);
		java4k.addMouseListener(mouseHandler);
		java4k.addMouseMotionListener(mouseHandler);

		forceHighResAccuracyMediaTickerorSoTheySay();
		createMainLoopThread(java4k);
	}

	private static void awaitEDT() {
		blockEDT(new Runnable() {
			@Override
			public void run() {
				// no-op
			}
		});
	}

	private static void blockEDT(Runnable task) {
		try {
			SwingUtilities.invokeAndWait(task);
		} catch (InvocationTargetException exc) {
			throw new IllegalStateException(exc.getCause());
		} catch (InterruptedException exc) {
			throw new IllegalStateException(exc);
		}
	}

	private static void forceHighResAccuracyMediaTickerorSoTheySay() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(Long.MAX_VALUE);
					} catch (InterruptedException exc) {
						// yadda yadda
					}
				}
			}
		}).start();
	}

	private static MouseAdapter createMouseHandler(final Java4kRev1 java4k) {
		return new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				java4k.mouse.x = e.getX();
				java4k.mouse.y = e.getY();
				java4k.mouse.dragArea = null;
				java4k.mouse.dragMove = null;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				java4k.mouse.x = e.getX();
				java4k.mouse.y = e.getY();
				java4k.mouse.pressed = e.getPoint();
				java4k.mouse.dragArea = null;
				java4k.mouse.dragMove = null;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				java4k.mouse.x = e.getX();
				java4k.mouse.y = e.getY();
				java4k.mouse.released = e.getPoint();
				java4k.mouse.dragArea = null;
				java4k.mouse.dragMove = null;
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				java4k.mouse.x = e.getX();
				java4k.mouse.y = e.getY();
				java4k.mouse.clicked = e.getPoint();
				java4k.mouse.dragArea = null;
				java4k.mouse.dragMove = null;
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int dx = e.getX() - java4k.mouse.x;
				int dy = e.getY() - java4k.mouse.y;
				java4k.mouse.x = e.getX();
				java4k.mouse.y = e.getY();

				int xMin = Math.min(java4k.mouse.pressed.x, java4k.mouse.x);
				int yMin = Math.min(java4k.mouse.pressed.y, java4k.mouse.y);
				int xMax = Math.max(java4k.mouse.pressed.x, java4k.mouse.x);
				int yMax = Math.max(java4k.mouse.pressed.y, java4k.mouse.y);

				if (java4k.mouse.dragArea == null)
					java4k.mouse.dragArea = new Rectangle();
				java4k.mouse.dragArea.setBounds(xMin, yMin, xMax - xMin, yMax - yMin);
				java4k.mouse.dragMove = new Dimension(dx, dy);
			}
		};
	}

	private static void createMainLoopThread(final Java4kRev1 java4k) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int renderCounter = 0;
				int updateCounter = 0;
				final long[] intervals = { 16, 17, 17 }; // 16.6666ms

				long nextUpdate = now();
				long nextRender = nextUpdate;

				while (true) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException exc) {
						// yadda yadda
					}

					final long now = now();
					if (now < nextUpdate)
						continue;

					do {
						blockEDT(new Runnable() {
							@Override
							public void run() {
								java4k.updateWrapper();
							}
						});
						nextUpdate += intervals[++updateCounter % intervals.length];
					} while (now >= nextUpdate);

					if (now >= nextRender) {
						java4k.repaint();
						awaitEDT();

						while (now > nextRender)
							// skip frames
							nextRender += intervals[++renderCounter % intervals.length];
					}
				}
			}
		}).start();
	}
}