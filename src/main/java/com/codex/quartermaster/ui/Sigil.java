package com.codex.quartermaster.ui;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** The Quartermaster's mark: a gilt-banded supply crate with a teal potion flask standing before it. */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float stroke = Math.max(0.6f, 0.55f * s);

		// The crate, lit from the top left, with two planks and a gilt band across it.
		RoundRectangle2D crate = new RoundRectangle2D.Float(x + 0.8f * s, y + 3.2f * s, 11.2f * s, 10.6f * s, 2.2f * s, 2.2f * s);
		g.setPaint(new GradientPaint(x, y + 3 * s, Theme.mix(Theme.GOLD_DIM, Theme.GOLD, 0.35f), x + 12 * s, y + 14 * s, Theme.mix(Theme.GOLD_DIM, Theme.INK_DEEP, 0.45f)));
		g.fill(crate);
		g.setStroke(new BasicStroke(stroke));
		g.setColor(Theme.INK_DEEP);
		g.draw(crate);
		g.setColor(Theme.alpha(Theme.INK_DEEP, 150));
		g.drawLine(Math.round(x + 1.2f * s), Math.round(y + 6.8f * s), Math.round(x + 11.6f * s), Math.round(y + 6.8f * s));
		g.drawLine(Math.round(x + 1.2f * s), Math.round(y + 10.4f * s), Math.round(x + 11.6f * s), Math.round(y + 10.4f * s));
		g.setPaint(new GradientPaint(x, y + 3 * s, Theme.GOLD_BRIGHT, x, y + 14 * s, Theme.GOLD));
		g.fill(new RoundRectangle2D.Float(x + 5.2f * s, y + 3.2f * s, 2.4f * s, 10.6f * s, 0.8f * s, 0.8f * s));
		g.setColor(Theme.INK_DEEP);
		g.draw(new RoundRectangle2D.Float(x + 5.2f * s, y + 3.2f * s, 2.4f * s, 10.6f * s, 0.8f * s, 0.8f * s));

		// The flask: a round body, a short neck and a cork, filled with teal.
		float fx = x + 11.6f * s;
		float fy = y + 11.4f * s;
		float r = 3.6f * s;
		Path2D flask = new Path2D.Float();
		flask.append(new Ellipse2D.Float(fx - r, fy - r, r * 2, r * 2), false);
		flask.append(new RoundRectangle2D.Float(fx - 1.2f * s, fy - r - 2.6f * s, 2.4f * s, 3.2f * s, 0.6f * s, 0.6f * s), false);
		g.setColor(Theme.INK_DEEP);
		g.setStroke(new BasicStroke(Math.max(1.6f, 1.8f * s)));
		g.draw(flask);
		g.setColor(Theme.alpha(Theme.TEXT, 70));
		g.fill(flask);
		g.setPaint(new GradientPaint(fx, fy - r, Theme.mix(Theme.TEAL, Theme.TEXT, 0.35f), fx, fy + r, Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.3f)));
		g.fill(new Ellipse2D.Float(fx - r + 0.6f * s, fy - r * 0.35f, (r - 0.6f * s) * 2, r * 1.3f));
		g.setColor(Theme.GOLD_DIM);
		g.fill(new RoundRectangle2D.Float(fx - 1.1f * s, fy - r - 3.2f * s, 2.2f * s, 1.4f * s, 0.6f * s, 0.6f * s));
		g.setColor(Theme.alpha(java.awt.Color.WHITE, 150));
		g.fill(new Ellipse2D.Float(fx - r * 0.55f, fy - r * 0.6f, r * 0.45f, r * 0.45f));
	}
}
