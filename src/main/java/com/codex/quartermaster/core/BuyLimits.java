package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * The Grand Exchange's buy limits against a shopping list. The exchange lets an account buy only
 * so many of an item every four hours (2,000 prayer potions, 10,000 sharks), so a big restock of
 * one supply can take several sittings however much gold there is. Limits run side by side, item
 * by item, so the list takes as many four-hour rounds as its slowest item needs.
 */
public final class BuyLimits
{
	/** How long the exchange counts a buy limit over. */
	public static final long WINDOW_MS = 4L * 60 * 60 * 1000;

	/** One item on the list that one round of buying cannot cover. */
	public static final class Over
	{
		private final int itemId;
		private final long quantity;
		private final int limit;

		Over(int itemId, long quantity, int limit)
		{
			this.itemId = itemId;
			this.quantity = quantity;
			this.limit = limit;
		}

		public int getItemId()
		{
			return itemId;
		}

		public long getQuantity()
		{
			return quantity;
		}

		/** How many the exchange sells an account every four hours. */
		public int getLimit()
		{
			return limit;
		}

		/** The four-hour rounds this item alone needs, 2 or more. */
		public int getRounds()
		{
			return rounds(quantity, limit);
		}
	}

	private final List<Over> over;

	private BuyLimits(List<Over> over)
	{
		this.over = Collections.unmodifiableList(over);
	}

	/**
	 * @param limit each item's buy limit; 0 or less for an item with no known limit, which is
	 *     never counted against the list
	 */
	public static BuyLimits of(List<Plan.Buy> shopping, IntUnaryOperator limit)
	{
		List<Over> over = new ArrayList<>();
		for (Plan.Buy buy : shopping)
		{
			int cap = limit.applyAsInt(buy.getItemId());
			if (cap > 0 && buy.getQuantity() > cap)
			{
				over.add(new Over(buy.getItemId(), buy.getQuantity(), cap));
			}
		}
		// The item that holds the list up longest first.
		over.sort((a, b) -> a.getRounds() != b.getRounds() ? Integer.compare(b.getRounds(), a.getRounds())
			: Long.compare(b.getQuantity() * a.getLimit(), a.getQuantity() * b.getLimit()));
		return new BuyLimits(over);
	}

	/** Rounds of buying {@code quantity} at {@code limit} every four hours; 1 with no limit. */
	public static int rounds(long quantity, int limit)
	{
		if (limit <= 0 || quantity <= limit)
		{
			return 1;
		}
		return (int) Math.min(Integer.MAX_VALUE, (quantity + limit - 1) / limit);
	}

	/** The items over their limit, the slowest first; empty when the list can be bought at once. */
	public List<Over> getOver()
	{
		return over;
	}

	public boolean isEmpty()
	{
		return over.isEmpty();
	}

	/** The four-hour rounds the whole list takes: as many as its slowest item. */
	public int rounds()
	{
		return over.isEmpty() ? 1 : over.get(0).getRounds();
	}

	/** How long from the first purchase to the last: the rounds after the first, four hours each. */
	public long waitMs()
	{
		return (rounds() - 1) * WINDOW_MS;
	}
}
