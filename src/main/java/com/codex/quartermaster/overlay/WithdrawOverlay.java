package com.codex.quartermaster.overlay;

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
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines the bank items still to withdraw for the chosen kit, each with a small gilt tab saying
 * how many to take, so packing is a matter of following the gold. The marks go as the items come
 * out. It only draws; nothing is clicked or moved for the player.
 */
public class WithdrawOverlay extends WidgetItemOverlay
{
	private static final Color OUTLINE = Theme.GOLD_BRIGHT;
	private static final Color TAB_FILL = Theme.alpha(Theme.INK_DEEP, 220);
	private static final Color TAB_RIM = Theme.alpha(Theme.GOLD, 200);

	private final ItemManager itemManager;
	private volatile Map<Integer, Long> withdraw = Collections.emptyMap();
	/** Each mark's text, made when the marks change rather than every frame. */
	private volatile Map<Integer, String> labels = Collections.emptyMap();

	public WithdrawOverlay(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		// Only your own bank: showOnBank() would also draw over group storage, which is not yours.
		drawAfterLayer(InterfaceID.Bankmain.ITEMS);
	}

	/** @param items bank items to mark with how many to take, or empty to mark nothing */
	public void show(Map<Integer, Long> items)
	{
		Map<Integer, Long> shown = items == null ? Collections.emptyMap() : items;
		if (shown.equals(withdraw))
		{
			return;
		}
		Map<Integer, String> text = new HashMap<>();
		shown.forEach((id, count) -> text.put(id, "x" + (count > 999 ? "999+" : String.valueOf(count))));
		labels = text;
		withdraw = shown;
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem widgetItem)
	{
		Long count = withdraw.get(itemId);
		// A placeholder has nothing to withdraw.
		if (count == null || count <= 0 || widgetItem.getQuantity() <= 0)
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), OUTLINE);
		g.drawImage(outline, bounds.x, bounds.y, null);

		String text = labels.get(itemId);
		if (text == null)
		{
			return;
		}
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(text) + 4;
		int h = fm.getAscent() + 1;
		int x = bounds.x + bounds.width - w;
		int y = bounds.y + bounds.height - h;
		g.setColor(TAB_FILL);
		g.fillRoundRect(x, y, w, h, 3, 3);
		g.setColor(TAB_RIM);
		g.drawRoundRect(x, y, w - 1, h - 1, 3, 3);
		g.setColor(Theme.GOLD_BRIGHT);
		g.drawString(text, x + 2, y + fm.getAscent());
	}
}
