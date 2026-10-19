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
	/** Loot less supplies, or null for a trip from before loot was kept. */
	private final Long net;

	TripRow(Trip trip, String used)
	{
		this.when = Format.day(trip.getStart()) + ", " + Format.time(trip.getStart()) + "  -  " + Format.duration(trip.duration());
		this.used = used;
		this.cost = Format.coins(trip.getCost());
		this.net = trip.net();
		setOpaque(false);
		setPreferredSize(new Dimension(10, 28));
		setToolTipText("<html>" + Ui.escape(when) + "<br>Used " + Ui.escape(used) + "<br>Cost " + cost
			+ (trip.getLoot() == null ? "" : "<br>Loot " + Format.coins(trip.getLoot()) + ", net " + Format.signed(net)) + "</html>");
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		String top = net == null ? cost : Format.signed(net);
		int cw = fm.stringWidth(top);
		Ui.shadowed(g, top, w - cw, fm.getAscent(), net == null ? Theme.GOLD : net >= 0 ? Theme.GREEN : Theme.EMBER);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(when, small, w - cw - 8), 0, fm.getAscent() - 1);
		int sw = 0;
		if (net != null)
		{
			// The supplies' cost moves under the net, so both stay readable.
			String spent = "-" + cost;
			sw = small.stringWidth(spent) + 6;
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(spent, w - sw + 6, 24);
		}
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(used, small, w - sw), 0, 24);
		g.setColor(Theme.STROKE_SOFT);
		g.drawLine(0, getHeight() - 1, w, getHeight() - 1);
		g.dispose();
	}
}
