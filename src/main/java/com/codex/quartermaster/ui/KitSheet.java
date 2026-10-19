package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Loadout;
import com.codex.quartermaster.core.Plan;
import com.codex.quartermaster.core.Returns;
import com.codex.quartermaster.core.Usage;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The kit card: a kit drawn as the inventory one trip packs, beside what the bank supports and
 * what the trips earn, in the panel's ink and gold. It is made to be pasted into a chat to show a
 * friend a setup, so it carries its own heading and needs nothing around it.
 */
public final class KitSheet
{
	static final int WIDTH = 330;
	private static final int PAD = 14;
	private static final int SLOT_W = 42;
	private static final int SLOT_H = 36;
	private static final int COLS = 4;
	private static final int ROWS = 7;
	private static final int HEADER = 66;
	/** The game's inventory backdrop. */
	private static final Color PACK = new Color(62, 53, 41);
	private static final Color PACK_EDGE = new Color(43, 37, 29);

	private KitSheet()
	{
	}

	/** One figure in the column beside the inventory. */
	static final class Fact
	{
		final String label;
		final String value;
		final String note;
		final Color color;

		Fact(String label, String value, String note, Color color)
		{
			this.label = label;
			this.value = value;
			this.note = note;
			this.color = color;
		}
	}

	/** The figures the card shows for a kit, most telling first, leaving out what is not known. */
	static List<Fact> facts(Plan plan, View view, Returns returns)
	{
		List<Fact> out = new ArrayList<>();
		int trips = plan.trips();
		Plan.Line first = plan.limiting();
		if (trips == Plan.UNLIMITED)
		{
			out.add(new Fact("Banked", "Ample", "nothing is used up", Theme.GOLD));
		}
		else
		{
			String note = first == null ? null : trips == 0 ? "out of " + view.name(first).toLowerCase(Locale.ENGLISH)
				: view.name(first).toLowerCase(Locale.ENGLISH) + " first";
			out.add(new Fact("Banked", Format.trips(trips) + (trips == 1 ? " trip" : " trips"), note, Theme.trips(trips, view.warn)));
		}
		out.add(new Fact("Per trip", Format.coins(plan.tripCost()), view.ironman ? "GE value" : "in supplies", Theme.GOLD));
		if (returns != null && returns.isKnown())
		{
			long hour = returns.netPerHour();
			out.add(new Fact("Net an hour", Format.signed(hour), "loot less supplies", hour >= 0 ? Theme.GREEN : Theme.EMBER));
		}
		if (view.measured >= Usage.TRUSTED && view.tripMs > 0)
		{
			out.add(new Fact("A trip", Format.duration(view.tripMs), "over " + view.measured + " trips", Theme.TEAL));
		}
		return out;
	}

	/**
	 * Draws the card. Icons not loaded yet are left out rather than waited for; the panel has
	 * shown the kit's items, so they almost always are.
	 */
	public static BufferedImage render(Plan plan, Loadout loadout, View view, ItemIcons icons)
	{
		Returns returns = view.returns.get(plan.getKit().getId());
		List<Fact> facts = facts(plan, view, returns);
		int gridW = COLS * SLOT_W + 8;
		int gridH = ROWS * SLOT_H + 8;
		int bodyY = HEADER + 12;
		int footY = bodyY + gridH + 12;
		int height = footY + 26;
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);

		// The parchment-dark ground with a gilt frame.
		RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1, height - 1, 14, 14);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, height, Theme.INK_DEEP));
		g.fill(frame);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.GOLD_DIM, 0.3), 0, HEADER, Theme.alpha(Theme.INK, 0)));
		g.fill(new RoundRectangle2D.Float(1, 1, WIDTH - 2, HEADER, 13, 13));
		g.setStroke(new BasicStroke(1.5f));
		g.setPaint(new GradientPaint(0, 0, Theme.GOLD, 0, height, Theme.GOLD_DIM));
		g.draw(new RoundRectangle2D.Float(1, 1, WIDTH - 2.5f, height - 2.5f, 13, 13));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 40));
		g.draw(new RoundRectangle2D.Float(4.5f, 4.5f, WIDTH - 9, height - 9, 10, 10));

		header(g, plan.getKit().getName(), view.player);
		rule(g, HEADER + 2);

		// The inventory, as the game draws it.
		int gx = PAD;
		RoundRectangle2D pack = new RoundRectangle2D.Float(gx + 0.5f, bodyY + 0.5f, gridW - 1, gridH - 1, 8, 8);
		g.setPaint(new GradientPaint(0, bodyY, PACK, 0, bodyY + gridH, Theme.mix(PACK, Theme.INK_DEEP, 0.35)));
		g.fill(pack);
		g.setColor(PACK_EDGE);
		g.draw(pack);
		g.setColor(Theme.alpha(Theme.GOLD, 60));
		g.draw(new RoundRectangle2D.Float(gx - 1.5f, bodyY - 1.5f, gridW + 3, gridH + 3, 10, 10));
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		List<Loadout.Slot> slots = loadout.getSlots();
		for (int i = 0; i < slots.size(); i++)
		{
			Loadout.Slot slot = slots.get(i);
			int sx = gx + 4 + (i % COLS) * SLOT_W;
			int sy = bodyY + 4 + (i / COLS) * SLOT_H;
			BufferedImage icon = icons == null ? null : icons.icon(slot.getItemId(), () ->
			{
			});
			if (icon != null)
			{
				Composite was = g.getComposite();
				if (!slot.isBanked())
				{
					// Shown for want of one in the bank: faded, like a placeholder.
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
				}
				g.drawImage(icon, sx + (SLOT_W - icon.getWidth()) / 2, sy + (SLOT_H - icon.getHeight()) / 2, null);
				g.setComposite(was);
			}
			if (slot.getQuantity() > 1)
			{
				String q = Format.stack(slot.getQuantity());
				Ui.shadowed(g, q, sx + 3, sy + small.getAscent(), Format.stackColor(slot.getQuantity()));
			}
		}
		if (loadout.getOverflow() > 0)
		{
			String more = "+" + Format.amount(loadout.getOverflow()) + " more";
			Ui.shadowed(g, more, gx + gridW - 6 - small.stringWidth(more), bodyY + gridH - 6, Theme.EMBER);
		}

		// The figures beside it.
		int fx = gx + gridW + 14;
		int fw = WIDTH - fx - PAD;
		int fy = bodyY + 2;
		int each = Math.min(64, gridH / Math.max(1, facts.size()));
		for (Fact f : facts)
		{
			fact(g, f, fx, fy, fw, each);
			fy += each;
		}

		// The foot: where it came from and when.
		rule(g, footY);
		g.setFont(Theme.small());
		String left = view.measured >= Usage.TRUSTED ? "Measured over " + view.measured + " trips"
			: plan.getKit().isPreset() ? "Typical amounts" : "As packed";
		String right = "Codex Quartermaster  -  " + Format.shortDay(view.now);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(left, small, WIDTH - PAD * 2 - small.stringWidth(right) - 10), PAD, footY + 16);
		g.setColor(Theme.GOLD_DIM);
		g.drawString(right, WIDTH - PAD - small.stringWidth(right), footY + 16);
		g.dispose();
		return image;
	}

	private static void header(Graphics2D g, String kit, String player)
	{
		int md = 46;
		int mx = PAD;
		int my = (HEADER - md) / 2 + 3;
		g.setPaint(new RadialGradientPaint(mx + md / 2f, my + md / 3f, md * 0.7f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.35), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(mx, my, md, md));
		g.setStroke(new BasicStroke(2f));
		g.setPaint(new GradientPaint(mx, my, Theme.GOLD_BRIGHT, mx, my + md, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(mx + 1, my + 1, md - 2, md - 2));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 70));
		g.draw(new Ellipse2D.Float(mx + 5, my + 5, md - 10, md - 10));
		Sigil.paint(g, mx + 10, my + 10, md - 20);

		int tx = mx + md + 12;
		int room = WIDTH - tx - PAD;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		String kicker = player == null ? "KIT CARD" : Ui.clip(player.toUpperCase(Locale.ENGLISH), small, room - 80) + "'S KIT";
		g.drawString(kicker, tx, my + 12);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		String name = kit.toUpperCase(Locale.ENGLISH);
		if (bold.stringWidth(name) * 2 + 2 <= room)
		{
			// Short names in the crest's doubled figure lettering, the way the game draws big text.
			Crest.paintLarge(g, name, tx, my + 18, Theme.GOLD);
		}
		else
		{
			Ui.shadowed(g, Ui.clip(kit, bold, room), tx, my + 34, Theme.GOLD_BRIGHT);
		}
	}

	private static void fact(Graphics2D g, Fact f, int x, int y, int w, int h)
	{
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(f.label.toUpperCase(Locale.ENGLISH), x, y + small.getAscent());
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(f.value, bold, w), x, y + small.getHeight() + bold.getAscent() + 1, f.color);
		if (f.note != null)
		{
			g.setFont(Theme.small());
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(f.note, small, w), x, y + small.getHeight() + bold.getHeight() + small.getAscent() + 1);
		}
		// A hairline under each figure, fading out, to keep the column in step with the grid.
		int ly = y + h - 8;
		if (h >= 50)
		{
			g.setPaint(new GradientPaint(x, 0, Theme.alpha(Theme.GOLD, 70), x + w, 0, Theme.alpha(Theme.GOLD, 0)));
			g.drawLine(x, ly, x + w, ly);
		}
	}

	private static void rule(Graphics2D g, int y)
	{
		g.setPaint(new GradientPaint(PAD, 0, Theme.alpha(Theme.GOLD, 0), WIDTH / 2f, 0, Theme.alpha(Theme.GOLD, 140), true));
		g.drawLine(PAD, y, WIDTH - PAD, y);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(WIDTH / 2f, y - 3);
		diamond.lineTo(WIDTH / 2f + 3, y);
		diamond.lineTo(WIDTH / 2f, y + 3);
		diamond.lineTo(WIDTH / 2f - 3, y);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
	}

	/** The card as something the system clipboard can hold. */
	public static Transferable transferable(Image image)
	{
		return new Transferable()
		{
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[]{DataFlavor.imageFlavor};
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return DataFlavor.imageFlavor.equals(flavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
			{
				if (!isDataFlavorSupported(flavor))
				{
					throw new UnsupportedFlavorException(flavor);
				}
				return image;
			}
		};
	}
}
