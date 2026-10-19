package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Whether what is carried is a full kit: each supply against what the kit packs for one trip,
 * and for anything short, which items in the bank to withdraw and how many.
 *
 * <p>The packing list is the kit's own amounts, not the measured ones. A saved kit's amounts are
 * what the player carried when they saved it, and a measured average is what a trip used, which
 * is less than what is taken along. A tool saved as kept, such as a spade, is packed as one.
 */
public final class Packing
{
	/** One supply of the kit against what is carried. */
	public static final class Item
	{
		private final Supply supply;
		private final long target;
		private final long carried;
		private final boolean tool;
		private final Map<Integer, Long> withdraw;

		Item(Supply supply, long target, long carried, boolean tool, Map<Integer, Long> withdraw)
		{
			this.supply = supply;
			this.target = target;
			this.carried = carried;
			this.tool = tool;
			this.withdraw = Collections.unmodifiableMap(withdraw);
		}

		public Supply getSupply()
		{
			return supply;
		}

		/** Units a trip packs. */
		public long getTarget()
		{
			return target;
		}

		/** Units carried now. */
		public long getCarried()
		{
			return carried;
		}

		/** Units still to withdraw, or 0 when packed. */
		public long getMissing()
		{
			return Math.max(0, target - carried);
		}

		public boolean isPacked()
		{
			return carried >= target;
		}

		/** Nothing of it is carried at all. */
		public boolean isForgotten()
		{
			return carried <= 0;
		}

		/** Brought along and not used up, such as a spade or a rune pouch. */
		public boolean isTool()
		{
			return tool;
		}

		/** Bank items to withdraw for it, in the order to take them, with how many of each. */
		public Map<Integer, Long> getWithdraw()
		{
			return withdraw;
		}
	}

	private final Kit kit;
	private final List<Item> items;

	private Packing(Kit kit, List<Item> items)
	{
		this.kit = kit;
		this.items = Collections.unmodifiableList(items);
	}

	/**
	 * @param carried what is carried now, by item
	 * @param bank the bank as last seen, by item
	 */
	public static Packing check(Kit kit, Map<Integer, Long> carried, Map<Integer, Long> bank)
	{
		List<Item> items = new ArrayList<>();
		for (Need need : kit.getNeeds())
		{
			Supply supply = Supplies.get(need.getSupply());
			if (supply == null)
			{
				continue;
			}
			boolean tool = need.getPerTrip() <= 0;
			if (tool && !supply.isItem())
			{
				continue;
			}
			long target = tool ? 1 : need.getPerTrip();
			long have = supply.count(carried);
			Map<Integer, Long> withdraw = have >= target ? Collections.emptyMap() : withdraw(supply, target - have, carried, bank);
			items.add(new Item(supply, target, have, tool, withdraw));
		}
		return new Packing(kit, items);
	}

	/**
	 * Which bank items make up a shortfall. The form already carried comes first, so a player
	 * packing sharks is pointed at more sharks; then full forms, most held first, so 4-dose potions
	 * come before a stray 1-dose; then anything else that counts.
	 */
	static Map<Integer, Long> withdraw(Supply supply, long missing, Map<Integer, Long> carried, Map<Integer, Long> bank)
	{
		List<Integer> forms = new ArrayList<>();
		for (int id : supply.getUnits().keySet())
		{
			if (bank.getOrDefault(id, 0L) > 0)
			{
				forms.add(id);
			}
		}
		forms.sort((a, b) ->
		{
			boolean ca = carried.getOrDefault(a, 0L) > 0;
			boolean cb = carried.getOrDefault(b, 0L) > 0;
			if (ca != cb)
			{
				return ca ? -1 : 1;
			}
			boolean fa = supply.isFullForm(a);
			boolean fb = supply.isFullForm(b);
			if (fa != fb)
			{
				return fa ? -1 : 1;
			}
			return Long.compare(bank.get(b) * supply.getUnits().get(b), bank.get(a) * supply.getUnits().get(a));
		});
		Map<Integer, Long> out = new LinkedHashMap<>();
		long left = missing;
		for (int id : forms)
		{
			if (left <= 0)
			{
				break;
			}
			int per = supply.getUnits().get(id);
			long take = Math.min(bank.get(id), (left + per - 1) / per);
			out.put(id, take);
			left -= take * per;
		}
		return out;
	}

	public Kit getKit()
	{
		return kit;
	}

	public List<Item> getItems()
	{
		return items;
	}

	/** Supplies short of a trip's packing, most urgent first: forgotten ones, then by share packed. */
	public List<Item> missing()
	{
		List<Item> out = new ArrayList<>();
		for (Item i : items)
		{
			if (!i.isPacked())
			{
				out.add(i);
			}
		}
		out.sort((a, b) -> Double.compare(a.getCarried() / (double) a.getTarget(), b.getCarried() / (double) b.getTarget()));
		return out;
	}

	/** Supplies with nothing at all carried, the ones worth a word when leaving the bank. */
	public List<Item> forgotten()
	{
		List<Item> out = new ArrayList<>();
		for (Item i : items)
		{
			if (i.isForgotten())
			{
				out.add(i);
			}
		}
		return out;
	}

	public int packedCount()
	{
		int n = 0;
		for (Item i : items)
		{
			if (i.isPacked())
			{
				n++;
			}
		}
		return n;
	}

	public boolean isComplete()
	{
		return !items.isEmpty() && packedCount() == items.size();
	}

	/**
	 * Whether the player looks to be packing this kit at all: something carried counts toward at
	 * least half its supplies. Below that the bank visit was for something else, and saying what
	 * is missing would only be noise.
	 */
	public boolean isUnderway()
	{
		if (items.isEmpty())
		{
			return false;
		}
		int started = 0;
		for (Item i : items)
		{
			if (!i.isForgotten())
			{
				started++;
			}
		}
		return started * 2 >= items.size();
	}

	/** Every bank item to withdraw, with how many, for highlighting in the bank. */
	public Map<Integer, Long> withdrawals()
	{
		Map<Integer, Long> out = new LinkedHashMap<>();
		for (Item i : items)
		{
			i.getWithdraw().forEach((id, q) -> out.merge(id, q, Long::sum));
		}
		return out;
	}
}
