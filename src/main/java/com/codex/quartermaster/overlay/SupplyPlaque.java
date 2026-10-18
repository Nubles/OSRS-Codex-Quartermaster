package com.codex.quartermaster.overlay;

import com.codex.quartermaster.ui.Sigil;
import com.codex.quartermaster.ui.Theme;
import com.codex.quartermaster.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A slim gilt plaque in the game view with how many trips the bank supports for the chosen kit
 * and what runs out first. It draws what the plugin hands it.
 */
public class SupplyPlaque extends Overlay
{
	private static final int PAD = 7;

	private volatile Shown shown;

	private static final class Shown
	{
		final String figure;
		final String note;
		final Color color;

		Shown(String figure, String note, Color color)
		{
			this.figure = figure;
			this.note = note;
			this.color = color;
		}
	}

	public SupplyPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/** @param figure the big text, such as "14 trips", or null to hide the plaque */
	public void show(String figure, String note, Color color)
	{
		shown = figure == null ? null : new Shown(figure, note, color);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int icon = 14;
		int width = PAD + icon + 6 + bold.stringWidth(s.figure) + 10 + small.stringWidth(s.note) + PAD;
		int height = Math.max(icon, bold.getHeight()) + PAD;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 230), 0, height, Theme.alpha(Theme.INK_DEEP, 230)));
		g.fill(shape);
		g.setColor(Theme.alpha(s.color, 170));
		g.draw(shape);

		int x = PAD;
		Sigil.paint(g, x, (height - icon) / 2f, icon);
		x += icon + 6;
		int base = (height + bold.getAscent()) / 2 - 2;
		g.setFont(Theme.title());
		Ui.shadowed(g, s.figure, x, base, s.color);
		x += bold.stringWidth(s.figure) + 10;
		g.setFont(Theme.small());
		Ui.shadowed(g, s.note, x, base, Theme.TEXT_MUTED);
		return new Dimension(width, height);
	}
}
