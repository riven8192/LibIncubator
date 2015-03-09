package net.indiespot.java4k.entries;

import java.awt.*;

import net.indiespot.java4k.Java4kRev2;

public class OddEntry2 extends Java4kRev2 {
	@Override
	public void render(Graphics2D g) {
		// print instructions
		g.setColor(new Color(128, 64, 128));
		g.drawString("Drag the mouse a little...", 8, 20);

		// draw something fascinating
		g.setColor(new Color(0, 64, 128));
		int b, r, a, c, q, t;
		for (r = 150; r >= 30; r -= 15) {
			t = ((elapsed() + 130_000) / ((200 - r) / 25));
			b = r / 3;
			a = (800 - r) / 2;
			c = (600 - r) / 2;

			q = (int) (t % (r + b));
			g.drawLine(a + Math.max(0, q - b), c, a + Math.min(r, q), c);

			q = (r + b) - (int) ((t + r) % (r + b));
			g.drawLine(a, c + Math.max(0, q - b), a, c + Math.min(r, q));

			q = (r + b) - (int) ((t + r - b) % (r + b));
			g.drawLine(a + Math.max(0, q - b), c + r, a + Math.min(r, q), c + r);

			q = (int) ((t - r) % (r + b));
			g.drawLine(a + r, c + Math.max(0, q - b), a + r, c + Math.min(r, q));
		}

		if (mouse.dragArea != null) {
			g.setColor(new Color(128, 64, 128));
			Rectangle w = mouse.dragArea;
			g.drawRect(w.x, w.y, w.width, w.height);
		}
	}
}