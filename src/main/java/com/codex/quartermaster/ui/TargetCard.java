package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.FromTarget;
import com.codex.quartermaster.core.Supply;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Kit from target: the target, with arrows to step through the others, the inventory the bank
 * packs for it, each slot rimmed by how it stands, and every supply with what is short or weaker
 * than the best and what buying the best would cost.
 */
class TargetCard extends JComponent
{
	static final int PAD = 10;
	private static final int COLS = 4;
	private static final int ROWS = 7;
	private static final int CELL_W = 40;
	private static final int CELL_H = 36;
	private static final int PICK_Y = 28;
	private static final int PICK_H = 22;
	private static final int GRID_Y = PICK_Y + PICK_H + 10;
	private static final int ROW_H = 30;

	private final FromTarget.Build build;
	private final View view;
	private final ItemIcons icons;
	private final Runnable repaint = this::repaint;
	private final int summaryY;
	private final int rowsY;
	private final int height;
	private int hover;

	/**
	 * @param step moves to the previous (-1) or next (+1) target; null when it cannot
	 */
	TargetCard(FromTarget.Build build, View view, ItemIcons icons, java.util.function.IntConsumer step)
	{
		this.build = build;
		this.view = view;
		this.icons = icons;
		summaryY = GRID_Y + ROWS * CELL_H + 10;
		rowsY = summaryY + 34;
		height = rowsY + build.getRows().size() * ROW_H + 6;
		setOpaque(false);
		setPreferredSize(new Dimension(10, height));
		setMinimumSize(new Dimension(10, height));
		setToolTipText("");
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int h = arrowAt(e.getX(), e.getY());
				if (h != hover)
				{
					hover = h;
					setCursor(h != 0 && step != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = 0;
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				int h = arrowAt(e.getX(), e.getY());
				if (h != 0 && step != null && SwingUtilities.isLeftMouseButton(e))
				{
					step.accept(h);
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	/** -1 over the left arrow, +1 over the right, else 0. */
	int arrowAt(int x, int y)
	{
		if (y < PICK_Y || y >= PICK_Y + PICK_H)
		{
			return 0;
		}
		return x < PAD + PICK_H ? -1 : x > getWidth() - PAD - PICK_H ? 1 : 0;
	}

	static Color color(FromTarget.Status status)
	{
		switch (status)
		{
			case READY:
				return Theme.GREEN;
			case DOWNGRADE:
				return Theme.EMBER;
			default:
				return Theme.BLOOD;
		}
	}

	/** The short tag at a supply's right: "Ready", "+7 slots", "Buy 6". */
	static String tag(FromTarget.Row row)
	{
		switch (row.getStatus())
		{
			case READY:
				return "Ready";
			case DOWNGRADE:
				return "+" + row.getExtraSlots() + (row.getExtraSlots() == 1 ? " slot" : " slots");
			default:
				return row.getBuyItem() < 0 ? "Get it" : "Buy " + Format.amount(row.getBuyQuantity());
		}
	}

	/** The line under a supply: what it packs when ready, else what to buy. */
	String brief(FromTarget.Row row)
	{
		String buy = row.getBuyItem() < 0 ? null : Format.amount(row.getBuyQuantity()) + " " + view.name(row.getBuyItem()).toLowerCase(java.util.Locale.ENGLISH);
		String cost = row.getBuyCost() > 0 ? ", " + Format.coins(row.getBuyCost()) : "";
		switch (row.getStatus())
		{
			case READY:
				return detail(row);
			case DOWNGRADE:
				return "Best: " + buy + cost;
			default:
				return buy == null ? "Not sold: get it yourself" : "Buy " + buy + cost;
		}
	}

	/** What a supply packs, and what would make it whole or better, in one line. */
	String detail(FromTarget.Row row)
	{
		StringBuilder take = new StringBuilder();
		for (Map.Entry<Integer, Long> e : row.getTake().entrySet())
		{
			if (take.length() > 0)
			{
				take.append(", ");
			}
			take.append(Format.amount(e.getValue())).append(" ").append(view.name(e.getKey()).toLowerCase(java.util.Locale.ENGLISH));
		}
		String buy = row.getBuyItem() < 0 ? null : Format.amount(row.getBuyQuantity()) + " " + view.name(row.getBuyItem()).toLowerCase(java.util.Locale.ENGLISH);
		String cost = row.getBuyCost() > 0 ? ", " + Format.coins(row.getBuyCost()) : "";
		switch (row.getStatus())
		{
			case READY:
				return take.length() == 0 ? "From the bank" : take.toString() + (row.isKept() ? ", comes back" : "");
			case DOWNGRADE:
				return take + "; " + buy + " would fit in " + row.getBestSlots() + cost;
			default:
				if (buy == null)
				{
					return take.length() == 0 ? "None banked, and not sold" : "Only " + take;
				}
				return (take.length() == 0 ? "None banked" : "Have " + take) + "; buy " + buy + cost;
		}
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		int gx = gridX(getWidth());
		if (e.getY() >= GRID_Y && e.getY() < GRID_Y + ROWS * CELL_H && e.getX() >= gx && e.getX() < gx + COLS * CELL_W)
		{
			int i = (e.getY() - GRID_Y) / CELL_H * COLS + (e.getX() - gx) / CELL_W;
			List<FromTarget.Slot> inv = build.getInventory();
			if (i < inv.size())
			{
				FromTarget.Slot s = inv.get(i);
				return view.name(s.getItemId()) + (s.getQuantity() > 1 ? " x " + Format.amount(s.getQuantity()) : "")
					+ (s.isBought() ? ": to buy" : s.getStatus() == FromTarget.Status.DOWNGRADE ? ": weaker than the best" : "");
			}
			return null;
		}
		if (e.getY() >= rowsY)
		{
			int r = (e.getY() - rowsY) / ROW_H;
			if (r < build.getRows().size())
			{
				return detail(build.getRows().get(r));
			}
		}
		return null;
	}

	private static int gridX(int w)
	{
		return (w - COLS * CELL_W) / 2;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, height - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, height, Theme.CARD));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 70));
		g.draw(shape);
		// Clipped to the card's corners, then the clip it came with is put back, never cleared.
		Shape old = g.getClip();
		g.clip(shape);
		g.setPaint(new GradientPaint(0, 0, Theme.TEAL, 0, height, Theme.alpha(Theme.GOLD_DIM, 60)));
		g.fillRect(0, 0, 3, height);
		g.setClip(old);

		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Ui.shadowed(g, "Kit from target", PAD, 8 + body.getAscent(), Theme.GOLD);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		if (view.buildFromRoadmap)
		{
			String badge = "Gear Roadmap's";
			g.setColor(Theme.TEAL);
			g.drawString(badge, w - PAD - small.stringWidth(badge), 8 + body.getAscent());
		}

		picker(g, w);
		grid(g, w, small);
		summary(g, w, small);
		rows(g, w, small);
		g.dispose();
	}

	/** The target's name between two arrows. */
	private void picker(Graphics2D g, int w)
	{
		RoundRectangle2D strip = new RoundRectangle2D.Float(PAD + 0.5f, PICK_Y + 0.5f, w - PAD * 2 - 1, PICK_H - 1, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(strip);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(strip);
		for (int side : new int[]{-1, 1})
		{
			int cx = side < 0 ? PAD + PICK_H / 2 : w - PAD - PICK_H / 2;
			int cy = PICK_Y + PICK_H / 2;
			Path2D p = new Path2D.Float();
			p.moveTo(cx + side * -2, cy - 4);
			p.lineTo(cx + side * 2, cy);
			p.lineTo(cx + side * -2, cy + 4);
			g.setColor(hover == side ? Theme.GOLD_BRIGHT : Theme.GOLD_DIM);
			g.setStroke(new BasicStroke(1.6f));
			g.draw(p);
			g.setStroke(new BasicStroke(1f));
		}
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		String name = Ui.clip(build.getTarget().getName(), bold, w - PAD * 2 - PICK_H * 2);
		Ui.shadowed(g, name, (w - bold.stringWidth(name)) / 2, PICK_Y + (PICK_H + bold.getAscent()) / 2 - 1, Theme.GOLD_BRIGHT);
	}

	/** The inventory, four by seven as the game draws it, each slot rimmed by how it stands. */
	private void grid(Graphics2D g, int w, FontMetrics small)
	{
		int gx = gridX(w);
		RoundRectangle2D ground = new RoundRectangle2D.Float(gx - 4, GRID_Y - 4, COLS * CELL_W + 8, ROWS * CELL_H + 8, 10, 10);
		g.setColor(Theme.INK_DEEP);
		g.fill(ground);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(ground);
		List<FromTarget.Slot> inv = build.getInventory();
		for (int i = 0; i < COLS * ROWS; i++)
		{
			int x = gx + (i % COLS) * CELL_W;
			int y = GRID_Y + (i / COLS) * CELL_H;
			if (i >= inv.size())
			{
				g.setColor(Theme.alpha(Theme.STROKE_SOFT, 80));
				g.fillRect(x + CELL_W / 2 - 1, y + CELL_H / 2 - 1, 2, 2);
				continue;
			}
			FromTarget.Slot s = inv.get(i);
			Color rim = s.isBought() ? Theme.BLOOD : s.getStatus() == FromTarget.Status.DOWNGRADE ? Theme.EMBER : null;
			RoundRectangle2D cell = new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, CELL_W - 3, CELL_H - 3, 6, 6);
			g.setColor(rim == null ? Theme.alpha(Theme.CARD, 220) : Theme.alpha(rim, s.isBought() ? 26 : 34));
			g.fill(cell);
			if (rim != null)
			{
				g.setColor(Theme.alpha(rim, s.isBought() ? 150 : 170));
				if (s.isBought())
				{
					g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 2f}, 0f));
				}
				g.draw(cell);
				g.setStroke(new BasicStroke(1f));
			}
			BufferedImage icon = icons == null ? null : icons.icon(s.getItemId(), repaint);
			if (icon != null)
			{
				Composite was = g.getComposite();
				if (s.isBought())
				{
					// Not in the bank: drawn faintly, as the bank draws a placeholder.
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
				}
				g.drawImage(icon, x + (CELL_W - icon.getWidth()) / 2, y + (CELL_H - icon.getHeight()) / 2, null);
				g.setComposite(was);
			}
			if (s.getQuantity() > 1)
			{
				String q = Format.stack(s.getQuantity());
				g.setColor(java.awt.Color.BLACK);
				g.drawString(q, x + 4, y + small.getAscent() + 2);
				g.setColor(Format.stackColor(s.getQuantity()));
				g.drawString(q, x + 3, y + small.getAscent() + 1);
			}
		}
	}

	/** Slots used, what downgrades cost in slots, and what the shortfall costs. */
	private void summary(Graphics2D g, int w, FontMetrics small)
	{
		int slots = build.slots();
		String used = slots + " of 28 slots";
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		Color c = slots > 28 ? Theme.BLOOD : Theme.TEXT;
		Ui.shadowed(g, used, PAD, summaryY + bold.getAscent(), c);
		g.setFont(Theme.small());
		int buy = build.count(FromTarget.Status.BUY);
		int down = build.count(FromTarget.Status.DOWNGRADE);
		String right = buy > 0 && build.shortfallCost() > 0 ? "Buy " + Format.coins(build.shortfallCost()) : buy > 0 ? buy + " to get" : "All banked";
		g.setColor(buy > 0 ? Theme.BLOOD : Theme.GREEN);
		g.drawString(right, w - PAD - small.stringWidth(right), summaryY + bold.getAscent());
		String line = down == 0 ? (buy == 0 ? "Every supply in as few slots as it can be"
			: "Packed as tight as the best") : down + (down == 1 ? " downgrade costs " : " downgrades cost ") + build.extraSlots()
			+ (build.extraSlots() == 1 ? " slot" : " slots");
		g.setColor(down == 0 ? Theme.TEXT_MUTED : Theme.EMBER);
		g.drawString(Ui.clip(line, small, w - PAD * 2), PAD, summaryY + bold.getHeight() + small.getAscent());
	}

	/** Every supply: its name and tag, then what it packs and what would better it. */
	private void rows(Graphics2D g, int w, FontMetrics small)
	{
		List<FromTarget.Row> rows = build.getRows();
		for (int i = 0; i < rows.size(); i++)
		{
			FromTarget.Row r = rows.get(i);
			int y = rowsY + i * ROW_H;
			g.setColor(Theme.alpha(Theme.STROKE_SOFT, 120));
			g.drawLine(PAD, y, w - PAD, y);
			Color c = color(r.getStatus());
			g.setColor(c);
			g.fillOval(PAD, y + 7, 6, 6);
			g.setFont(Theme.body());
			FontMetrics body = g.getFontMetrics();
			String tag = tag(r);
			g.setFont(Theme.small());
			int tw = small.stringWidth(tag);
			g.setFont(Theme.body());
			Supply supply = r.getSupply();
			g.setColor(Theme.TEXT);
			g.drawString(Ui.clip(view.name(supply), body, w - PAD * 2 - 14 - tw - 6), PAD + 12, y + 3 + body.getAscent());
			g.setFont(Theme.small());
			Ui.shadowed(g, tag, w - PAD - tw, y + 3 + body.getAscent(), c);
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(brief(r), small, w - PAD * 2 - 12), PAD + 12, y + 3 + body.getHeight() + small.getAscent() - 2);
		}
	}
}
