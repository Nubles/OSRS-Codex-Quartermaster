package com.codex.quartermaster.overlay;

import com.codex.quartermaster.core.InventoryCheck;
import com.codex.quartermaster.ui.Theme;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Marks the inventory beside the bank against the chosen kit: what the kit does not take is rimmed
 * in red and tabbed "out", what is packed past the kit's amount in ember with how many to put back,
 * and a stack still short in gold with how many more. The bank's own marks say what to take; these
 * say what to put back. It only draws; nothing is deposited for the player.
 */
public class InventoryMarks extends WidgetItemOverlay
{
	private static final Color TAB_FILL = Theme.alpha(Theme.INK_DEEP, 225);

	private final ItemManager itemManager;
	private volatile InventoryCheck check = InventoryCheck.NONE;
	/** Each slot's tab text, made when the marks change rather than every frame. */
	private volatile Map<Integer, String> labels = Collections.emptyMap();

	public InventoryMarks(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		// The inventory as the bank shows it, where depositing happens.
		drawAfterLayer(InterfaceID.Bankside.ITEMS);
	}

	/** @param marks the inventory against the kit, or null to mark nothing */
	public void show(InventoryCheck marks)
	{
		InventoryCheck shown = marks == null ? InventoryCheck.NONE : marks;
		if (shown.getMarks().equals(check.getMarks()))
		{
			return;
		}
		Map<Integer, String> text = new HashMap<>();
		shown.getMarks().forEach((slot, m) -> text.put(slot, label(m)));
		labels = text;
		check = shown;
	}

	/** "out" for a slot to empty, "-60" for part of a stack, "+60" for a stack to fill. */
	static String label(InventoryCheck.Mark m)
	{
		String n = m.getAmount() > 9999 ? "9999+" : String.valueOf(m.getAmount());
		switch (m.getKind())
		{
			case SHORT:
				return "+" + n;
			case SURPLUS:
				return m.isWhole() && m.getAmount() == 1 ? "out" : "-" + n;
			default:
				return "out";
		}
	}

	static Color colour(InventoryCheck.Kind kind)
	{
		switch (kind)
		{
			case SHORT:
				return Theme.GOLD_BRIGHT;
			case SURPLUS:
				return Theme.EMBER;
			default:
				return Theme.BLOOD;
		}
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		// Drawn on a copy, so the font and colour RuneLite hands over come back untouched.
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			mark(g, itemId, widgetItem);
		}
		finally
		{
			g.dispose();
		}
	}

	private void mark(Graphics2D g, int itemId, WidgetItem widgetItem)
	{
		Widget w = widgetItem.getWidget();
		InventoryCheck.Mark mark = w == null ? null : check.at(w.getIndex());
		// The slot must still hold what was marked: a withdrawal can land before the marks catch up.
		if (mark == null || mark.getItemId() != itemId)
		{
			return;
		}
		Rectangle b = widgetItem.getCanvasBounds();
		Color c = colour(mark.getKind());
		if (mark.getKind() != InventoryCheck.Kind.SHORT)
		{
			// Faded under the outline, so what goes back reads as already on its way out.
			g.setColor(Theme.alpha(c, 46));
			g.fillRect(b.x, b.y, b.width, b.height);
		}
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), c);
		g.drawImage(outline, b.x, b.y, null);
		String text = labels.get(w.getIndex());
		if (text != null)
		{
			Tab.draw(g, text, b, c, TAB_FILL);
		}
	}

	/** The small tab in a slot's lower right corner, shared in look with the bank's marks. */
	static final class Tab
	{
		private Tab()
		{
		}

		static void draw(Graphics2D g, String text, Rectangle b, Color ink, Color fill)
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			int w = fm.stringWidth(text) + 4;
			int h = fm.getAscent() + 1;
			int x = b.x + b.width - w;
			int y = b.y + b.height - h;
			g.setColor(fill);
			g.fillRoundRect(x, y, w, h, 3, 3);
			g.setColor(Theme.alpha(ink, 200));
			g.drawRoundRect(x, y, w - 1, h - 1, 3, 3);
			g.setColor(ink);
			g.drawString(text, x + 2, y + fm.getAscent());
		}
	}
}
