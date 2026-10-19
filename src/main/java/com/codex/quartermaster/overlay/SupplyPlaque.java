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
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A slim gilt plaque in the game view with how many trips the bank supports for the chosen kit
 * and what runs out first. It draws what the plugin hands it.
 */
public class SupplyPlaque extends Overlay
{
	private static final int PAD = 7;
	/** Past this the note is cut short, so a long kit name cannot stretch the plaque across the game. */
	private static final int MAX_NOTE = 240;

	private volatile Shown shown;
	/** The mark, drawn once. */
	private BufferedImage mark;

	/** What is shown, with its layout worked out on the first frame and kept, so a frame allocates nothing. */
	private static final class Shown
	{
		final String figure;
		final String lead;
		final String tail;
		final Color color;
		String note;
		int width;
		int height;
		RoundRectangle2D shape;
		GradientPaint fill;
		Color rim;

		Shown(String figure, String lead, String tail, Color color)
		{
			this.figure = figure;
			this.lead = lead;
			this.tail = tail;
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
		show(figure, note, null, color);
	}

	/**
	 * @param lead the start of the note, cut short first when room runs out, such as the kit's name
	 * @param tail the end of the note, kept whole, such as what runs out first; or null
	 */
	public void show(String figure, String lead, String tail, Color color)
	{
		Shown was = shown;
		if (figure == null)
		{
			shown = null;
		}
		else if (was == null || !figure.equals(was.figure) || !java.util.Objects.equals(lead, was.lead)
			|| !java.util.Objects.equals(tail, was.tail) || !color.equals(was.color))
		{
			shown = new Shown(figure, lead, tail, color);
		}
	}

	/** The note in a width: the lead gives way before the tail, so "out of prayer" is never the part cut. */
	static String note(String lead, String tail, FontMetrics fm, int max)
	{
		String l = lead == null ? "" : lead;
		if (tail == null || tail.isEmpty())
		{
			return Ui.clip(l, fm, max);
		}
		if (l.isEmpty())
		{
			return Ui.clip(tail, fm, max);
		}
		String sep = "  -  ";
		int room = max - fm.stringWidth(sep + tail);
		if (room < fm.stringWidth("Abc..."))
		{
			return Ui.clip(tail, fm, max);
		}
		return Ui.clip(l, fm, room) + sep + tail;
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
		if (s.note == null)
		{
			s.note = note(s.lead, s.tail, small, MAX_NOTE);
			s.width = PAD + icon + 6 + bold.stringWidth(s.figure) + 10 + small.stringWidth(s.note) + PAD;
			s.height = Math.max(icon, bold.getHeight()) + PAD;
			s.shape = new RoundRectangle2D.Float(0.5f, 0.5f, s.width - 1, s.height - 1, 10, 10);
			s.fill = new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 230), 0, s.height, Theme.alpha(Theme.INK_DEEP, 230));
			s.rim = Theme.alpha(s.color, 170);
		}
		int width = s.width;
		int height = s.height;
		g.setPaint(s.fill);
		g.fill(s.shape);
		g.setColor(s.rim);
		g.draw(s.shape);

		int x = PAD;
		if (mark == null)
		{
			mark = Sigil.icon(icon);
		}
		g.drawImage(mark, x, (height - icon) / 2, null);
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
