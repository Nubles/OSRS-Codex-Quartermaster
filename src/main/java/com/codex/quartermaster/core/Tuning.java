package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Advice on how much of each supply a saved kit should take, from what its trips really used.
 * A kit is saved from what was carried, and players pack for the worst trip they can imagine:
 * fourteen sharks for a fight that eats seven. The Quartermaster has measured every trip since,
 * so it can say what covers nine trips in ten, and free the rest of the inventory.
 *
 * <p>For each supply the kit uses up, the amount suggested is what the ninth trip in ten used
 * (the 90th percentile of the recent trips), rounded up to whole items of what the player
 * carries. When two trips in ten or more used everything packed, the supply ran out and the
 * advice is to take one more item instead. Tools that come back, and supplies measured on too
 * few trips, get no advice.</p>
 */
public final class Tuning
{
	/** Trips measured before any advice is given. */
	public static final int MIN_TRIPS = 5;
	/** The most recent trips looked at, so an old method does not count. */
	static final int RECENT = 10;
	/** Share of trips that may run a supply dry before more is advised. */
	static final double RAN_OUT = 0.2;

	/** Advice for one supply. */
	public static final class Advice
	{
		private final Supply supply;
		private final int packed;
		private final int suggested;
		private final long covers;
		private final int trips;
		private final int ranOut;
		private final int item;
		private final int unitsEach;

		Advice(Supply supply, int packed, int suggested, long covers, int trips, int ranOut, int item, int unitsEach)
		{
			this.supply = supply;
			this.packed = packed;
			this.suggested = suggested;
			this.covers = covers;
			this.trips = trips;
			this.ranOut = ranOut;
			this.item = item;
			this.unitsEach = unitsEach;
		}

		public Supply getSupply()
		{
			return supply;
		}

		/** What the kit takes now, in the supply's units. */
		public int getPacked()
		{
			return packed;
		}

		/** What it should take, in the supply's units. */
		public int getSuggested()
		{
			return suggested;
		}

		/** The most the ninth trip in ten used, in the supply's units. */
		public long getCovers()
		{
			return covers;
		}

		/** Trips the advice is drawn from. */
		public int getTrips()
		{
			return trips;
		}

		/** Of those, the trips that used everything packed. */
		public int getRanOut()
		{
			return ranOut;
		}

		/** The item counted in: what the player carries most of. */
		public int getItem()
		{
			return item;
		}

		/** Units one of {@link #getItem()} is worth. */
		public int getUnitsEach()
		{
			return unitsEach;
		}

		public boolean isMore()
		{
			return suggested > packed;
		}

		/** Whole items more (positive) or fewer (negative) to pack. */
		public int itemsChange()
		{
			return (int) Math.round((suggested - packed) / (double) unitsEach);
		}
	}

	private Tuning()
	{
	}

	/**
	 * @param carried what is carried now, which says which item a supply is counted in
	 * @return advice for each supply worth changing, in the kit's order; empty for a preset
	 */
	public static List<Advice> of(Kit kit, Usage usage, Map<Integer, Long> carried)
	{
		if (kit.isPreset() || usage == null)
		{
			return Collections.emptyList();
		}
		List<Advice> out = new ArrayList<>();
		List<Trip> recent = usage.getRecent();
		for (Need need : kit.getNeeds())
		{
			Supply s = Supplies.get(need.getSupply());
			if (s == null || need.getPerTrip() <= 0)
			{
				continue;
			}
			List<Long> used = new ArrayList<>();
			int ranOut = 0;
			for (int i = Math.max(0, recent.size() - RECENT); i < recent.size(); i++)
			{
				Trip t = recent.get(i);
				Long u = t.getUsed().get(s.getKey());
				if (u == null)
				{
					// Measured before this supply joined the kit.
					continue;
				}
				used.add(u);
				long took = t.took(s.getKey(), need.getPerTrip());
				if (took > 0 && u >= took)
				{
					ranOut++;
				}
			}
			if (used.size() < MIN_TRIPS)
			{
				continue;
			}
			Collections.sort(used);
			long covers = used.get((int) Math.ceil(used.size() * 0.9) - 1);
			int item = s.fullForm(itemOf(s, carried));
			int each = Math.max(1, s.getUnits().getOrDefault(item, 1));
			int packed = need.getPerTrip();
			int suggested;
			if (ranOut >= Math.max(2, Math.ceil(used.size() * RAN_OUT)))
			{
				suggested = packed + each;
			}
			else
			{
				// Whole items, and never nothing: a supply some trips use is still taken.
				long items = Math.max(1, (covers + each - 1) / each);
				suggested = (int) Math.min(Integer.MAX_VALUE, items * each);
				if (packed - suggested < each)
				{
					continue;
				}
			}
			out.add(new Advice(s, packed, suggested, covers, used.size(), ranOut, item, each));
		}
		return out;
	}

	/** The item a supply is carried as: the one carried in the most units, else the supply's own. */
	static int itemOf(Supply s, Map<Integer, Long> carried)
	{
		int best = s.getIcon();
		long most = 0;
		for (Map.Entry<Integer, Integer> e : s.getUnits().entrySet())
		{
			long units = carried.getOrDefault(e.getKey(), 0L) * e.getValue();
			if (units > most)
			{
				most = units;
				best = e.getKey();
			}
		}
		return best;
	}

	/**
	 * Inventory slots the advice frees: pooled supplies that take a slot each, so not single items
	 * (runes and tablets may stack) and not bolts, which stack in one slot however many there are.
	 */
	public static int slotsFreed(List<Advice> advice)
	{
		int slots = 0;
		for (Advice a : advice)
		{
			if (!a.getSupply().isItem() && !"bolts".equals(a.getSupply().getUnit()))
			{
				slots -= a.itemsChange();
			}
		}
		return slots;
	}
}
