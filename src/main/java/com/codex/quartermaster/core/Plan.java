package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * How far a bank stretches for one kit: each supply with how many trips it lasts, the one that
 * runs out first, what a trip costs, and the shopping list to reach a number of trips.
 */
public final class Plan
{
	/** A supply that is brought along and comes back, so it never runs out. */
	public static final int UNLIMITED = Integer.MAX_VALUE;

	private final Kit kit;
	private final List<Line> lines;
	private final int target;

	Plan(Kit kit, List<Line> lines, int target)
	{
		this.kit = kit;
		this.lines = Collections.unmodifiableList(lines);
		this.target = target;
	}

	public Kit getKit()
	{
		return kit;
	}

	public List<Line> getLines()
	{
		return lines;
	}

	public int getTarget()
	{
		return target;
	}

	/** The line for one supply, or null when the kit does not take it. */
	public Line line(String supply)
	{
		for (Line l : lines)
		{
			if (l.getSupply().getKey().equals(supply))
			{
				return l;
			}
		}
		return null;
	}

	/** @return trips the bank supports, or {@link #UNLIMITED} when nothing in the kit is used up */
	public int trips()
	{
		Line first = limiting();
		return first == null ? UNLIMITED : first.getTrips();
	}

	/** The supply that runs out first, or null when nothing is used up. */
	public Line limiting()
	{
		Line first = null;
		for (Line l : lines)
		{
			if (!l.isKept() && (first == null || l.getTrips() < first.getTrips()))
			{
				first = l;
			}
		}
		return first;
	}

	/** What one trip uses, at today's cheapest prices. */
	public long tripCost()
	{
		long sum = 0;
		for (Line l : lines)
		{
			sum += l.getTripCost();
		}
		return sum;
	}

	/** What restocking to the target costs. */
	public long restockCost()
	{
		long sum = 0;
		for (Line l : lines)
		{
			sum += l.getBuyCost();
		}
		return sum;
	}

	/** Every purchase on the shopping list, biggest bill first. */
	public List<Buy> shopping()
	{
		List<Buy> all = new ArrayList<>();
		for (Line l : lines)
		{
			all.addAll(l.getBuys());
		}
		all.sort((a, b) -> Long.compare(b.getCost(), a.getCost()));
		return all;
	}

	/** Supplies short of the target that the Grand Exchange cannot sell. */
	public List<Line> unbuyable()
	{
		List<Line> out = new ArrayList<>();
		for (Line l : lines)
		{
			if (l.getShort() > 0 && l.getBuys().isEmpty())
			{
				out.add(l);
			}
		}
		return out;
	}

	/** One supply of the kit against the bank. */
	public static final class Line
	{
		private final Supply supply;
		private final long have;
		private final double perTrip;
		private final boolean measured;
		private final int trips;
		private final long shortfall;
		private final List<Buy> buys;
		private final long tripCost;
		private final Map<Integer, Long> held;
		private final long stored;
		private final int buyItem;

		Line(Supply supply, long have, double perTrip, boolean measured, int trips, long shortfall, List<Buy> buys,
			long tripCost, Map<Integer, Long> held, long stored, int buyItem)
		{
			this.stored = stored;
			this.buyItem = buyItem;
			this.supply = supply;
			this.have = have;
			this.perTrip = perTrip;
			this.measured = measured;
			this.trips = trips;
			this.shortfall = shortfall;
			this.buys = Collections.unmodifiableList(buys);
			this.tripCost = tripCost;
			this.held = Collections.unmodifiableMap(held);
		}

		public Supply getSupply()
		{
			return supply;
		}

		/** Units held across the bank and what is carried. */
		public long getHave()
		{
			return have;
		}

		/** Units a trip uses, as planned or as measured. */
		public double getPerTrip()
		{
			return perTrip;
		}

		public boolean isMeasured()
		{
			return measured;
		}

		public int getTrips()
		{
			return trips;
		}

		/** Brought along and never used up. */
		public boolean isKept()
		{
			return trips == UNLIMITED;
		}

		/** Units missing to reach the target. */
		public long getShort()
		{
			return shortfall;
		}

		public List<Buy> getBuys()
		{
			return buys;
		}

		public long getBuyCost()
		{
			long sum = 0;
			for (Buy b : buys)
			{
				sum += b.getCost();
			}
			return sum;
		}

		public long getTripCost()
		{
			return tripCost;
		}

		/** Units of this supply in the bank's potion storage, already counted in {@link #getHave()}. */
		public long getStored()
		{
			return stored;
		}

		/** The item this supply is restocked with, or -1 when nothing that counts has a price. */
		public int getBuyItem()
		{
			return buyItem;
		}

		/** How many of each item that counts toward this supply are held, most first. */
		public Map<Integer, Long> getHeld()
		{
			return held;
		}
	}

	/** One line of the shopping list. */
	public static final class Buy
	{
		private final int itemId;
		private final long quantity;
		private final long price;

		Buy(int itemId, long quantity, long price)
		{
			this.itemId = itemId;
			this.quantity = quantity;
			this.price = price;
		}

		public int getItemId()
		{
			return itemId;
		}

		public long getQuantity()
		{
			return quantity;
		}

		public long getPrice()
		{
			return price;
		}

		public long getCost()
		{
			return quantity * price;
		}
	}
}
