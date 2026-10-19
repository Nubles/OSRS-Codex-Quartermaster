package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The kits as other plugins may read them: a stable, documented shape saved beside the
 * Quartermaster's own data, under the RS profile key {@link #KEY} of the Quartermaster's config
 * group. The internal "quarters" entry may change between versions; this one only grows, and a
 * breaking change would get a new key.
 *
 * <p>Every need lists the items that count toward it with their units each, so a reader can tell
 * which items a kit uses without copying the Quartermaster's supply tables. Item ids are the
 * game's canonical ones, as the bank holds them.
 */
public final class KitBook
{
	public static final String KEY = "kitsV1";
	public static final int VERSION = 1;

	private int version = VERSION;
	private String active;
	private int target;
	private List<Entry> kits = new ArrayList<>();

	/** One kit. */
	public static final class Entry
	{
		private String id;
		private String name;
		private boolean preset;
		/** Trips the bank supports, or -1 when nothing in it is used up or the bank was never seen. */
		private int tripsLeft;
		/** The supply that runs out first, or null. */
		private String limiting;
		private List<Line> needs = new ArrayList<>();

		public String getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public boolean isPreset()
		{
			return preset;
		}

		public int getTripsLeft()
		{
			return tripsLeft;
		}

		public String getLimiting()
		{
			return limiting;
		}

		public List<Line> getNeeds()
		{
			return needs;
		}
	}

	/** One supply of a kit. */
	public static final class Line
	{
		private String supply;
		private String name;
		private String unit;
		private int perTrip;
		/** What a trip really used on average, once measured over enough trips; null until then. */
		private Double measured;
		private Map<Integer, Integer> items = new LinkedHashMap<>();

		public String getSupply()
		{
			return supply;
		}

		public String getName()
		{
			return name;
		}

		public String getUnit()
		{
			return unit;
		}

		public int getPerTrip()
		{
			return perTrip;
		}

		public Double getMeasured()
		{
			return measured;
		}

		public Map<Integer, Integer> getItems()
		{
			return items;
		}
	}

	/**
	 * @param names an item's name, for single-item supplies that have none of their own
	 * @param measured trips left are counted from measured amounts, as the panel does
	 */
	public static KitBook of(Quarters quarters, Map<Integer, Long> carried, java.util.function.IntFunction<String> names,
		boolean measured)
	{
		KitBook book = new KitBook();
		book.active = quarters.activeKit().getId();
		book.target = quarters.getTarget();
		for (Kit kit : quarters.allKits())
		{
			Entry e = new Entry();
			e.id = kit.getId();
			e.name = kit.getName();
			e.preset = kit.isPreset();
			Usage usage = quarters.usage(kit.getId());
			boolean trusted = usage != null && usage.isTrusted();
			for (Need need : kit.getNeeds())
			{
				Supply s = Supplies.get(need.getSupply());
				if (s == null)
				{
					continue;
				}
				Line line = new Line();
				line.supply = s.getKey();
				line.name = s.getName() != null ? s.getName() : names.apply(s.getIcon());
				line.unit = s.getUnit();
				line.perTrip = need.getPerTrip();
				if (trusted && usage.perTrip(s.getKey()) >= 0)
				{
					line.measured = Math.round(usage.perTrip(s.getKey()) * 100) / 100.0;
				}
				line.items.putAll(s.getUnits());
				e.needs.add(line);
			}
			e.tripsLeft = -1;
			if (quarters.hasBank())
			{
				// Prices do not change trips, so none are needed here.
				Plan plan = quarters.plan(kit, carried, id -> 0, measured);
				Plan.Line first = plan.limiting();
				if (first != null)
				{
					e.tripsLeft = plan.trips();
					e.limiting = first.getSupply().getKey();
				}
			}
			book.kits.add(e);
		}
		return book;
	}

	public int getVersion()
	{
		return version;
	}

	public String getActive()
	{
		return active;
	}

	public int getTarget()
	{
		return target;
	}

	public List<Entry> getKits()
	{
		return kits;
	}
}
