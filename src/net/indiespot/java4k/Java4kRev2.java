package net.indiespot.java4k;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.indiespot.java4k.entries.OddEntry2;

@SuppressWarnings("serial")
public abstract class Java4kRev2 extends Canvas {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				init(new OddEntry2(), 800, 600);
			}
		});
	}

	protected static long now() {
		return System.nanoTime() / 1_000_000L;
	}

	static final String title_infix = " - Java4K (rev 2) - ";
	static final long started_at = now();

	protected static int elapsed() {
		return (int) (now() - started_at);
	}

	protected String name;
	protected int w, h;
	protected final Mouse mouse = new Mouse();

	private JFrame frame;
	private long lastSecondTimestamp = now();
	private int updateCounter;
	private int renderCounter;
	private long updateNanos;
	private long renderNanos;

	private final void gatherStats() {
		if (frame.getTitle().isEmpty()) {
			String title = (name == null) ? this.getClass().getSimpleName() : name;
			title += title_infix;
			title += "started";
			frame.setTitle(title);

		}
		if (now() > lastSecondTimestamp + 1000L) {
			int avgUpdateTook = (updateCounter == 0) ? 0 : (int) (updateNanos / updateCounter / 100_000L);
			int avgRenderTook = (renderCounter == 0) ? 0 : (int) (renderNanos / renderCounter / 100_000L);

			String title = (name == null) ? this.getClass().getSimpleName() : name;
			title += title_infix;
			title += "update: " + (avgUpdateTook / 10) + "." + (avgUpdateTook % 10) + "ms/" + updateCounter + "Hz";
			title += ", ";
			title += "render: " + (avgRenderTook / 10) + "." + (avgRenderTook % 10) + "ms/" + renderCounter + "fps";
			frame.setTitle(title);

			renderCounter = 0;
			updateCounter = 0;

			updateNanos = 0;
			renderNanos = 0;

			lastSecondTimestamp += 1000L;
		}
	}

	final void updateWrapper() {
		updateNanos -= System.nanoTime();
		this.update();
		updateNanos += System.nanoTime();

		updateCounter++;
	}

	final void renderWrapper(BufferStrategy bufferStrategy) {
		w = this.getWidth();
		h = this.getHeight();

		renderNanos -= System.nanoTime();
		{
			while (true) {
				Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
				{
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, w, h);

					this.render((Graphics2D) g);
				}
				g.dispose();

				if (bufferStrategy.contentsLost()) {
					continue;
				}

				bufferStrategy.show();
				break;
			}
		}
		renderNanos += System.nanoTime();

		renderCounter++;
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

	private static void init(final Java4kRev2 java4k, int w, int h) {
		JFrame frame = new JFrame();
		{
			java4k.frame = frame;
			java4k.setPreferredSize(new Dimension(w, h));
			java4k.setBackground(Color.BLACK);
			java4k.setIgnoreRepaint(true);

			frame.setTitle(java4k.name);
			frame.getContentPane().add(java4k, BorderLayout.CENTER);
		}
		frame.setIgnoreRepaint(true);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		MouseAdapter mouseHandler = createMouseHandler(java4k);
		java4k.addMouseListener(mouseHandler);
		java4k.addMouseMotionListener(mouseHandler);

		forceHighResAccuracyMediaTickerOrSoTheySay();
		createMainLoopThread(java4k);
	}

	private static void forceHighResAccuracyMediaTickerOrSoTheySay() {
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

	private static MouseAdapter createMouseHandler(final Java4kRev2 java4k) {
		return new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				synchronized (java4k) {
					java4k.mouse.x = e.getX();
					java4k.mouse.y = e.getY();
					java4k.mouse.dragArea = null;
					java4k.mouse.dragMove = null;
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				synchronized (java4k) {
					java4k.mouse.x = e.getX();
					java4k.mouse.y = e.getY();
					java4k.mouse.pressed = e.getPoint();
					java4k.mouse.dragArea = null;
					java4k.mouse.dragMove = null;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				synchronized (java4k) {
					java4k.mouse.x = e.getX();
					java4k.mouse.y = e.getY();
					java4k.mouse.released = e.getPoint();
					java4k.mouse.dragArea = null;
					java4k.mouse.dragMove = null;
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				synchronized (java4k) {
					java4k.mouse.x = e.getX();
					java4k.mouse.y = e.getY();
					java4k.mouse.clicked = e.getPoint();
					java4k.mouse.dragArea = null;
					java4k.mouse.dragMove = null;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				synchronized (java4k) {
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
			}
		};
	}

	private static void createMainLoopThread(final Java4kRev2 java4k) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final long intervalNanos = 16_666_667L; // 16.666667ms

				long nextUpdate = System.nanoTime();
				long nextRender = nextUpdate;

				java4k.createBufferStrategy(2);
				final BufferStrategy bufferStrategy = java4k.getBufferStrategy();

				try {
					while (true) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException exc) {
							// yadda yadda
						}

						final long now = System.nanoTime();
						if (now < nextUpdate)
							continue;

						synchronized (java4k) {
							do {
								java4k.updateWrapper();
								nextUpdate += intervalNanos;
							} while (now >= nextUpdate);

							if (now >= nextRender) {
								java4k.renderWrapper(bufferStrategy);

								do {
									nextRender += intervalNanos;
								} while (now > nextRender); // skip frames
							}

							java4k.gatherStats();
						}
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}

				bufferStrategy.dispose();
			}
		}).start();
	}
}
