package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Trip;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** A measured trip: when it set out, how long it ran, what it used and what that cost. */
class TripRow extends JComponent
{
	private final String when;
	private final String used;
	private final String cost;

	TripRow(Trip trip, String used)
	{
		this.when = Format.day(trip.getStart()) + ", " + Format.time(trip.getStart()) + "  -  " + Format.duration(trip.duration());
		this.used = used;
		this.cost = Format.coins(trip.getCost());
		setOpaque(false);
		setPreferredSize(new Dimension(10, 28));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		int cw = fm.stringWidth(cost);
		Ui.shadowed(g, cost, w - cw, fm.getAscent(), Theme.GOLD);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(when, small, w - cw - 8), 0, fm.getAscent() - 1);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(used, small, w), 0, 24);
		g.setColor(Theme.STROKE_SOFT);
		g.drawLine(0, getHeight() - 1, w, getHeight() - 1);
		g.dispose();
	}
}
