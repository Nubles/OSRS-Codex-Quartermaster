package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Kit from target: the best trip the bank can pack for a target, supply by supply. For each
 * supply the target's kit takes, the bank's items that count are packed fewest slots first: the
 * food that heals most a bite, the fullest potions. A supply packed from a weaker item than the
 * best one sold takes more inventory slots, and is told as a downgrade with what buying the best
 * would cost; a supply the bank cannot cover for one trip is told with what to buy to cover it.
 * The result can be saved as a kit of its own, item by item.
 */
public final class FromTarget
{
	/** How a supply stands for one trip of the target. */
	public enum Status
	{
		/** Packed in as few slots as the best item sold would take. */
		READY,
		/** Packed, but from a weaker item that takes more slots than the best. */
		DOWNGRADE,
		/** The bank cannot cover one trip. */
		BUY
	}

	/** One inventory slot of the trip packed. */
	public static final class Slot
	{
		private final int itemId;
		private final long quantity;
		private final Status status;
		private final boolean bought;

		Slot(int itemId, long quantity, Status status, boolean bought)
		{
			this.itemId = itemId;
			this.quantity = quantity;
			this.status = status;
			this.bought = bought;
		}

		public int getItemId()
		{
			return itemId;
		}

		public long getQuantity()
		{
			return quantity;
		}

		public Status getStatus()
		{
			return status;
		}

		/** Not in the bank: it is what to buy. */
		public boolean isBought()
		{
			return bought;
		}
	}

	/** One supply of the target's kit, as the bank can pack it. */
	public static final class Row
	{
		private final Supply supply;
		private final long target;
		private final boolean kept;
		private final Map<Integer, Long> take;
		private final int slots;
		private final int bestSlots;
		private final Status status;
		private final int buyItem;
		private final long buyQuantity;
		private final long buyCost;

		Row(Supply supply, long target, boolean kept, Map<Integer, Long> take, int slots, int bestSlots, Status status,
			int buyItem, long buyQuantity, long buyCost)
		{
			this.supply = supply;
			this.target = target;
			this.kept = kept;
			this.take = Collections.unmodifiableMap(new LinkedHashMap<>(take));
			this.slots = slots;
			this.bestSlots = bestSlots;
			this.status = status;
			this.buyItem = buyItem;
			this.buyQuantity = buyQuantity;
			this.buyCost = buyCost;
		}

		public Supply getSupply()
		{
			return supply;
		}

		/** Units one trip takes: hitpoints, doses, bolts or items. */
		public long getTarget()
		{
			return target;
		}

		/** A tool that comes back every trip. */
		public boolean isKept()
		{
			return kept;
		}

		/** What to take from the bank, item to count, in packing order. */
		public Map<Integer, Long> getTake()
		{
			return take;
		}

		/** Inventory slots the supply takes as packed, bought items included. */
		public int getSlots()
		{
			return slots;
		}

		/** Slots it would take in the best item sold. */
		public int getBestSlots()
		{
			return bestSlots;
		}

		/** Slots a downgrade costs over the best. */
		public int getExtraSlots()
		{
			return Math.max(0, slots - bestSlots);
		}

		public Status getStatus()
		{
			return status;
		}

		/**
		 * The item to buy: for a shortfall, what covers it; for a downgrade, the best item, enough
		 * for a whole trip. -1 when there is nothing to buy or nothing has a price.
		 */
		public int getBuyItem()
		{
			return buyItem;
		}

		public long getBuyQuantity()
		{
			return buyQuantity;
		}

		public long getBuyCost()
		{
			return buyCost;
		}
	}

	/** The trip packed for one target. */
	public static final class Build
	{
		private final Kit target;
		private final List<Row> rows;
		private final List<Slot> inventory;
		private final int overflow;

		Build(Kit target, List<Row> rows, List<Slot> inventory, int overflow)
		{
			this.target = target;
			this.rows = Collections.unmodifiableList(rows);
			this.inventory = Collections.unmodifiableList(inventory);
			this.overflow = overflow;
		}

		public Kit getTarget()
		{
			return target;
		}

		public List<Row> getRows()
		{
			return rows;
		}

		/** The inventory packed, at most {@link Loadout#SLOTS} slots, in the kit's order. */
		public List<Slot> getInventory()
		{
			return inventory;
		}

		/** Slots past a full inventory. */
		public int getOverflow()
		{
			return overflow;
		}

		public int slots()
		{
			return inventory.size() + overflow;
		}

		/** Slots the trip would take packed from the best items sold. */
		public int bestSlots()
		{
			int n = 0;
			for (Row r : rows)
			{
				n += r.bestSlots;
			}
			return n;
		}

		public int count(Status status)
		{
			int n = 0;
			for (Row r : rows)
			{
				n += r.status == status ? 1 : 0;
			}
			return n;
		}

		/** What the shortfalls cost to buy. Downgrades are not counted: the bank covers them. */
		public long shortfallCost()
		{
			long total = 0;
			for (Row r : rows)
			{
				total += r.status == Status.BUY ? r.buyCost : 0;
			}
			return total;
		}

		/** Slots the downgrades cost over the best. */
		public int extraSlots()
		{
			int n = 0;
			for (Row r : rows)
			{
				n += r.status == Status.DOWNGRADE ? r.getExtraSlots() : 0;
			}
			return n;
		}

		/**
		 * The build as a kit of its own: each item packed, and each item to buy, as a supply of
		 * its own with the count a trip takes; a tool that comes back is kept at 0.
		 */
		public Kit toKit(String id, String name)
		{
			Map<Integer, Long> counts = new LinkedHashMap<>();
			Map<Integer, Boolean> kept = new LinkedHashMap<>();
			for (Row r : rows)
			{
				Map<Integer, Long> all = new LinkedHashMap<>(r.take);
				if (r.status == Status.BUY && r.buyItem > 0)
				{
					all.merge(r.buyItem, r.buyQuantity, Long::sum);
				}
				for (Map.Entry<Integer, Long> e : all.entrySet())
				{
					counts.merge(e.getKey(), r.kept ? 0 : e.getValue(), Long::sum);
					kept.merge(e.getKey(), r.kept, Boolean::logicalAnd);
				}
			}
			List<Need> needs = new ArrayList<>();
			counts.forEach((item, n) -> needs.add(new Need(Supply.itemKey(item), (int) Math.min(Quarters.AMOUNT_MAX, n))));
			return new Kit(id, name, needs, false);
		}
	}

	/** Codex Gear Roadmap's config group and the key its target is saved under, read only. */
	public static final String ROADMAP_GROUP = "codexgearroadmap";
	public static final String ROADMAP_TARGET = "target";

	private FromTarget()
	{
	}

	/**
	 * The target to build for: the one picked on the panel, else the preset for Codex Gear
	 * Roadmap's target, else the kit in use if it is a preset, else the first preset.
	 *
	 * @param kits every kit, saved and preset
	 * @param picked the kit id picked on the panel, or null
	 * @param roadmap Gear Roadmap's target monster, or null
	 * @param active the kit in use
	 */
	public static Kit pick(List<Kit> kits, String picked, String roadmap, Kit active)
	{
		Kit first = null;
		Kit named = null;
		for (Kit k : kits)
		{
			if (!k.isPreset())
			{
				continue;
			}
			if (k.getId().equals(picked))
			{
				return k;
			}
			first = first == null ? k : first;
			named = named == null && matches(k, roadmap) ? k : named;
		}
		if (named != null)
		{
			return named;
		}
		return active != null && active.isPreset() ? active : first;
	}

	/** Whether a kit is for the monster Gear Roadmap names: the same name, or that name and a form. */
	public static boolean matches(Kit kit, String roadmap)
	{
		if (roadmap == null || kit == null)
		{
			return false;
		}
		String t = roadmap.trim().toLowerCase(java.util.Locale.ENGLISH);
		String n = kit.getName().toLowerCase(java.util.Locale.ENGLISH);
		return t.equals(n) || t.startsWith(n + " ");
	}

	/**
	 * @param target the kit to pack for, usually a preset
	 * @param held the bank and what is carried, by item
	 * @param prices Grand Exchange prices; 0 for an item that is not sold
	 * @param stackable whether an item stacks in the inventory
	 */
	public static Build of(Kit target, Map<Integer, Long> held, Prices prices, IntPredicate stackable)
	{
		List<Row> rows = new ArrayList<>();
		List<Slot> inventory = new ArrayList<>();
		int overflow = 0;
		for (Need need : target.getNeeds())
		{
			Supply supply = Supplies.get(need.getSupply());
			if (supply == null)
			{
				continue;
			}
			Row row = row(supply, need.getPerTrip(), held, prices, stackable);
			rows.add(row);
			List<Slot> slots = new ArrayList<>();
			long past = 0;
			for (Map.Entry<Integer, Long> e : row.take.entrySet())
			{
				past += addSlots(slots, e.getKey(), e.getValue(), row.status, false, stackable);
			}
			if (row.status == Status.BUY && row.buyItem > 0)
			{
				past += addSlots(slots, row.buyItem, row.buyQuantity, Status.BUY, true, stackable);
			}
			overflow = (int) Math.min(Integer.MAX_VALUE, overflow + past);
			for (Slot s : slots)
			{
				if (inventory.size() < Loadout.SLOTS)
				{
					inventory.add(s);
				}
				else
				{
					overflow++;
				}
			}
		}
		return new Build(target, rows, inventory, overflow);
	}

	static Row row(Supply supply, int perTrip, Map<Integer, Long> held, Prices prices, IntPredicate stackable)
	{
		boolean kept = perTrip <= 0;
		long target = kept ? 1 : perTrip;
		Map<Integer, Integer> units = supply.getUnits();
		// Fewest slots first: most units an item, then what the bank holds most of.
		List<Integer> candidates = new ArrayList<>();
		for (int id : units.keySet())
		{
			if (held.getOrDefault(id, 0L) > 0 && units.get(id) > 0)
			{
				candidates.add(id);
			}
		}
		candidates.sort(Comparator.comparing((Integer id) -> -units.get(id))
			.thenComparing(id -> -held.getOrDefault(id, 0L)).thenComparing(id -> id));
		Map<Integer, Long> take = new LinkedHashMap<>();
		long left = target;
		for (int id : candidates)
		{
			if (left <= 0)
			{
				break;
			}
			int each = units.get(id);
			long n = Math.min(held.get(id), (left + each - 1) / each);
			take.put(id, n);
			left -= n * each;
		}
		int slots = 0;
		for (Map.Entry<Integer, Long> e : take.entrySet())
		{
			slots += slotsFor(e.getKey(), e.getValue(), stackable);
		}

		int best = best(supply, prices);
		int bestSlots = best < 0 ? slots : slotsFor(best, (target + units.get(best) - 1) / units.get(best), stackable);
		if (left > 0)
		{
			int buy = Planner.buyItem(supply, held, Collections.emptyMap(), prices);
			if (buy < 0 && best >= 0)
			{
				buy = best;
			}
			long qty = buy < 0 ? 0 : (left + units.get(buy) - 1) / units.get(buy);
			long cost = buy < 0 ? 0 : qty * prices.price(buy);
			slots += buy < 0 ? 0 : take.containsKey(buy) && stackable.test(buy) ? 0 : slotsFor(buy, qty, stackable);
			return new Row(supply, target, kept, take, slots, best < 0 ? slots : bestSlots, Status.BUY, buy, qty, cost);
		}
		if (best >= 0 && slots > bestSlots)
		{
			long qty = (target + units.get(best) - 1) / units.get(best);
			return new Row(supply, target, kept, take, slots, bestSlots, Status.DOWNGRADE, best, qty, qty * prices.price(best));
		}
		return new Row(supply, target, kept, take, slots, Math.min(slots, bestSlots), Status.READY, -1, 0, 0);
	}

	/**
	 * The best item a supply is sold as: most units an item among its full forms with a price,
	 * the cheapest a unit when several tie. -1 when none is sold.
	 */
	static int best(Supply supply, Prices prices)
	{
		int best = -1;
		for (Map.Entry<Integer, Integer> e : supply.getUnits().entrySet())
		{
			int id = e.getKey();
			if (!supply.isFullForm(id) || prices.price(id) <= 0)
			{
				continue;
			}
			if (best < 0)
			{
				best = id;
				continue;
			}
			int u = e.getValue();
			int bu = supply.getUnits().get(best);
			if (u > bu || u == bu && prices.price(id) < prices.price(best))
			{
				best = id;
			}
		}
		return best;
	}

	private static int slotsFor(int id, long count, IntPredicate stackable)
	{
		if (count <= 0)
		{
			return 0;
		}
		return stackable.test(id) ? 1 : (int) Math.min(Integer.MAX_VALUE, count);
	}

	/** @return slots past those added: counted, not looped, so a kit of 2,000 sharks is not drawn */
	private static long addSlots(List<Slot> slots, int id, long count, Status status, boolean bought, IntPredicate stackable)
	{
		if (count <= 0)
		{
			return 0;
		}
		if (stackable.test(id))
		{
			slots.add(new Slot(id, count, status, bought));
			return 0;
		}
		long n = Math.min(count, Loadout.SLOTS);
		for (long i = 0; i < n; i++)
		{
			slots.add(new Slot(id, 1, status, bought));
		}
		return count - n;
	}
}
