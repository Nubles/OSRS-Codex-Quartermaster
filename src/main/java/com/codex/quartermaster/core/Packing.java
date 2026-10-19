package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

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
		private final long inStorage;

		Item(Supply supply, long target, long carried, boolean tool, Map<Integer, Long> withdraw, long inStorage)
		{
			this.inStorage = inStorage;
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

		/** Doses of it in the bank's potion storage, which the bank grid does not show. */
		public long getInStorage()
		{
			return inStorage;
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
		return check(kit, carried, bank, Collections.emptyMap(), null);
	}

	/**
	 * @param stored potion doses in the bank's potion storage, by 1-dose item
	 * @param usage what the kit has been measured using, or null. A preset's supply that trusted
	 *     trips show is never used, such as anti-venom for a player in a serpentine helm, is not
	 *     asked for. A saved kit is what the player chose to pack, so all of it is.
	 */
	public static Packing check(Kit kit, Map<Integer, Long> carried, Map<Integer, Long> bank, Map<Integer, Long> stored,
		Usage usage)
	{
		boolean trusted = kit.isPreset() && usage != null && usage.isTrusted();
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
			double measured = trusted ? usage.perTrip(supply.getKey()) : -1;
			if (!tool && measured >= 0 && measured < Planner.KEPT)
			{
				continue;
			}
			long target = tool ? 1 : need.getPerTrip();
			long have = supply.count(carried);
			Map<Integer, Long> withdraw = have >= target ? Collections.emptyMap() : withdraw(supply, target - have, carried, bank);
			items.add(new Item(supply, target, have, tool, withdraw, supply.count(stored)));
		}
		return new Packing(kit, items);
	}

	/**
	 * Which bank items make up a shortfall. What is already carried comes first, so a player
	 * packing sharks is pointed at more sharks and one packing super restores at more of those;
	 * within that, full forms, most held first, so 4-dose potions come before a stray 1-dose;
	 * then anything else that counts.
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
		// The potions or food already carried, by full form, so a leftover 1-dose prayer potion
		// points at 4-dose prayer potions rather than at other leftovers.
		Set<Integer> families = new HashSet<>();
		for (int id : supply.getUnits().keySet())
		{
			if (carried.getOrDefault(id, 0L) > 0)
			{
				families.add(supply.fullForm(id));
			}
		}
		forms.sort((a, b) ->
		{
			boolean ca = families.contains(supply.fullForm(a));
			boolean cb = families.contains(supply.fullForm(b));
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

	/**
	 * Whether anything the kit counts was taken out of the bank during the visit. A visit that only
	 * deposits loot leaves with what came back from the trip, and saying what is missing from that
	 * would be wrong: the player was not packing.
	 *
	 * @param opened what was carried when the bank opened
	 * @param closed what is carried as it closes
	 */
	public static boolean withdrewFor(Kit kit, Map<Integer, Long> opened, Map<Integer, Long> closed)
	{
		for (Need need : kit.getNeeds())
		{
			Supply supply = Supplies.get(need.getSupply());
			if (supply != null && supply.count(closed) > supply.count(opened))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Inventory slots the withdrawals take: one a stack for stackable items, none when that stack
	 * is already in the inventory, and one each for anything else.
	 *
	 * @param inInventory items already in the inventory
	 * @param stackable whether an item stacks
	 */
	public static long slotsNeeded(Map<Integer, Long> withdrawals, Set<Integer> inInventory, IntPredicate stackable)
	{
		long slots = 0;
		for (Map.Entry<Integer, Long> e : withdrawals.entrySet())
		{
			if (stackable.test(e.getKey()))
			{
				slots += inInventory.contains(e.getKey()) ? 0 : 1;
			}
			else
			{
				slots += e.getValue();
			}
		}
		return slots;
	}

	/**
	 * Whether the bank is worth marking: the player is packing this kit, judged by it being
	 * underway or by the inventory having room for everything still to take. A bank visit with a
	 * full inventory of loot to deposit is not marked until there is room.
	 */
	public boolean worthMarking(long freeSlots, long slotsNeeded)
	{
		return !withdrawals().isEmpty() && (isUnderway() || freeSlots >= slotsNeeded);
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
