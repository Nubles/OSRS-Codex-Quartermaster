package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Plan;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;
import javax.swing.JComponent;

/**
 * One supply against the bank: the item in its slot, what is held and what a trip takes, how many
 * trips that lasts on the right, and a row of ration pips, one a trip up to the restock target, so
 * the supply that runs out first stands out at a glance.
 */
class SupplyRow extends JComponent
{
	private static final int MAX_PIPS = 25;

	private final Plan.Line line;
	private final String name;
	private final int target;
	private final int warn;
	private final boolean first;
	private final ItemIcons icons;

	SupplyRow(Plan.Line line, View view, int target, boolean first, ItemIcons icons)
	{
		this.line = line;
		this.name = view.name(line);
		this.target = target;
		this.warn = view.warn;
		this.first = first;
		this.icons = icons;
		setOpaque(false);
		setPreferredSize(new Dimension(10, Well.SIZE + 12));
		StringBuilder tip = new StringBuilder("<html><b>").append(Ui.escape(name)).append("</b>");
		for (Map.Entry<Integer, Long> e : line.getHeld().entrySet())
		{
			tip.append("<br>").append(Ui.escape(view.name(e.getKey()))).append(": ").append(String.format("%,d", e.getValue()));
		}
		if (line.getHeld().isEmpty())
		{
			tip.append("<br>None held");
		}
		if (!line.isKept())
		{
			tip.append("<br>").append(line.isMeasured() ? "Measured from your trips" : "Planned for this kit");
		}
		setToolTipText(tip.append("</html>").toString());
	}

	private int icon()
	{
		// Show the item the player has most of, so the row looks like their own bank.
		return line.getHeld().isEmpty() ? line.getSupply().getIcon() : line.getHeld().keySet().iterator().next();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		boolean kept = line.isKept();
		Color color = kept ? Theme.TEXT_MUTED : Theme.trips(line.getTrips(), warn);
		Well.paint(g, 0, 4, icons == null ? null : icons.icon(icon(), this::repaint), first ? Theme.alpha(color, 200) : null);

		int tx = Well.SIZE + 12;
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		String trips = kept ? "kept" : Format.trips(line.getTrips());
		g.setFont(kept ? Theme.small() : Theme.title());
		FontMetrics tf = g.getFontMetrics();
		int tw = tf.stringWidth(trips);
		int base = 4 + bold.getAscent();
		Ui.shadowed(g, trips, w - tw, base, color);

		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(name, fm, w - tx - tw - 8), tx, base, first ? Theme.TEXT : Theme.mix(Theme.TEXT, Theme.TEXT_MUTED, 0.25));

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String note = kept
			? "comes back every trip"
			: Format.amount(line.getHave()) + View.unit(line) + "  -  " + Format.perTrip(line.getPerTrip()) + " a trip" + (line.isMeasured() ? "*" : "");
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(note, small, w - tx), tx, base + 13);

		if (!kept)
		{
			paintPips(g, tx, Well.SIZE + 5, w - tx, color);
		}
		g.dispose();
	}

	private void paintPips(Graphics2D g, int x, int y, int width, Color color)
	{
		int have = Math.min(line.getTrips(), target);
		if (target > MAX_PIPS)
		{
			RoundRectangle2D track = new RoundRectangle2D.Float(x, y, width, 4, 4, 4);
			g.setColor(Theme.INK_DEEP);
			g.fill(track);
			float fill = width * have / (float) target;
			if (fill > 0)
			{
				g.setPaint(new GradientPaint(x, 0, Theme.mix(color, Theme.INK, 0.4), x + width, 0, color));
				g.fill(new RoundRectangle2D.Float(x, y, Math.max(3, fill), 4, 4, 4));
			}
			return;
		}
		float gap = 2;
		float pip = (width - gap * (target - 1)) / target;
		for (int i = 0; i < target; i++)
		{
			float px = x + i * (pip + gap);
			RoundRectangle2D r = new RoundRectangle2D.Float(px, y, pip, 4, 2, 2);
			if (i < have)
			{
				g.setPaint(new GradientPaint(0, y, Theme.mix(color, Color.WHITE, 0.2), 0, y + 4, color));
			}
			else
			{
				g.setColor(Theme.INK_DEEP);
			}
			g.fill(r);
		}
	}
}
