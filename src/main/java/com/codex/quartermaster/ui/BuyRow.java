package com.codex.quartermaster.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** One line of the shopping list: the item in its slot, how many to buy and at what, and the bill. */
class BuyRow extends JComponent
{
	private final int itemId;
	private final String name;
	private final String note;
	private final String amount;
	private final Color color;
	private final ItemIcons icons;
	/** Handed over each time an icon is asked for, so a row waiting on a picture is told once. */
	private final Runnable repaint = this::repaint;

	BuyRow(int itemId, String name, String note, String amount, Color color, ItemIcons icons)
	{
		this.itemId = itemId;
		this.name = name;
		this.note = note;
		this.amount = amount;
		this.color = color;
		this.icons = icons;
		setOpaque(false);
		setPreferredSize(new Dimension(10, Well.SIZE + 4));
		setToolTipText(name + ": " + note);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		Well.paint(g, 0, 2, icons == null || itemId < 0 ? null : icons.icon(itemId, repaint), null);
		int tx = Well.SIZE + 12;
		g.setFont(Theme.body());
		FontMetrics fm = g.getFontMetrics();
		int base = 2 + fm.getAscent() + 1;
		// The name gets the whole line; the quantity and the bill share the line below it.
		if (fm.stringWidth(name) > w - tx)
		{
			// A long name such as "Extended super antifire(4)" steps down to the small font before it is cut.
			g.setFont(Theme.small());
			Ui.shadowed(g, Ui.clip(name, g.getFontMetrics(), w - tx), tx, base, Theme.TEXT);
			g.setFont(Theme.body());
		}
		else
		{
			Ui.shadowed(g, name, tx, base, Theme.TEXT);
		}
		int low = 2 + Well.SIZE - 5;
		int aw = fm.stringWidth(amount);
		Ui.shadowed(g, amount, w - aw, low, color);
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(note, g.getFontMetrics(), w - tx - aw - 6), tx, low);
		g.dispose();
	}
}
