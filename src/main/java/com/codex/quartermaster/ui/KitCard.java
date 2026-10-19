package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Plan;
import com.codex.quartermaster.core.Returns;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** A kit at a glance: its name, how many trips the bank supports, and what runs out first. Click to choose it. */
class KitCard extends Card
{
	KitCard(Plan plan, View view, boolean chosen, Runnable choose, Runnable forget)
	{
		this(plan, view, chosen, choose, forget, null);
	}

	/** @param rename asks for a new name, or null for a kit that keeps its own */
	KitCard(Plan plan, View view, boolean chosen, Runnable choose, Runnable forget, Runnable rename)
	{
		super(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(7, 12, 7, 10));
		int trips = plan.trips();
		Color color = trips == Plan.UNLIMITED ? Theme.TEXT_MUTED : Theme.trips(trips, view.warn);
		accent(chosen ? Theme.GOLD : Theme.alpha(color, 160));
		highlighted(chosen);
		Plan.Line first = plan.limiting();
		String note;
		if (first == null)
		{
			note = "Nothing is used up";
		}
		else if (trips == 0)
		{
			note = "Out of " + view.name(first).toLowerCase(java.util.Locale.ENGLISH);
		}
		else
		{
			note = view.name(first) + " first";
		}
		JPanel body = new JPanel(new BorderLayout(0, 2));
		body.setOpaque(false);
		body.add(new Face(plan.getKit().getName(), note,
			trips == Plan.UNLIMITED ? "-" : Format.trips(trips), color, chosen), BorderLayout.CENTER);
		Returns returns = view.returns.get(plan.getKit().getId());
		if (returns != null && returns.isKnown())
		{
			// What the kit's trips earn: loot home less the supplies they burn.
			long hour = returns.netPerHour();
			body.add(Ui.label("Earns " + Format.signed(hour) + " an hour, " + Format.signed(returns.netPerTrip()) + " a trip",
				Theme.small(), hour >= 0 ? Theme.GREEN : Theme.EMBER), BorderLayout.SOUTH);
		}
		add(body, BorderLayout.CENTER);
		// Chosen by clicking anywhere but the forget button, which is added after so it keeps its own clicks.
		onClick(choose);
		if (forget != null || rename != null)
		{
			JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
			row.setOpaque(false);
			if (rename != null)
			{
				row.add(new CodexButton("Rename", false, rename));
			}
			if (forget != null)
			{
				row.add(new CodexButton("Forget", false, forget));
			}
			add(row, BorderLayout.SOUTH);
		}
	}

	private static final class Face extends JComponent
	{
		private final String name;
		private final String note;
		private final String trips;
		private final Color color;
		private final boolean chosen;

		Face(String name, String note, String trips, Color color, boolean chosen)
		{
			this.name = name;
			this.note = note;
			this.trips = trips;
			this.color = color;
			this.chosen = chosen;
			setPreferredSize(new Dimension(10, 30));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			g.setFont(Theme.small());
			int tw = Math.max(bold.stringWidth(trips), g.getFontMetrics().stringWidth("trips"));
			g.setFont(Theme.title());
			Ui.shadowed(g, trips, w - bold.stringWidth(trips), bold.getAscent() + 1, color);
			g.setFont(Theme.small());
			g.setColor(Theme.TEXT_FAINT);
			FontMetrics small = g.getFontMetrics();
			g.drawString("trips", w - small.stringWidth("trips"), 28);
			g.setFont(Theme.body());
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, Ui.clip(name, fm, w - tw - 10), 0, fm.getAscent() + 1, chosen ? Theme.GOLD_BRIGHT : Theme.TEXT);
			g.setFont(Theme.small());
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(note, small, w - tw - 10), 0, 28);
			g.dispose();
		}
	}
}
