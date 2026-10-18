package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Works out a {@link Plan}: how many trips the items held support, and what to buy for more. */
public final class Planner
{
	private Planner()
	{
	}

	/**
	 * @param held every item in the bank and carried, by id
	 * @param usage what this kit has been measured using, or null
	 * @param measured plan from the measured amounts once they are trusted
	 * @param target trips to restock to
	 */
	public static Plan plan(Kit kit, Usage usage, Map<Integer, Long> held, Prices prices, int target, boolean measured)
	{
		boolean useMeasured = measured && usage != null && usage.isTrusted();
		List<Plan.Line> lines = new ArrayList<>();
		for (Need need : kit.getNeeds())
		{
			Supply supply = Supplies.get(need.getSupply());
			if (supply == null)
			{
				continue;
			}
			double perTrip = need.getPerTrip();
			boolean fromTrips = false;
			if (useMeasured && usage.perTrip(supply.getKey()) >= 0)
			{
				perTrip = usage.perTrip(supply.getKey());
				fromTrips = true;
			}
			long have = supply.count(held);
			// Measured amounts are averages; a fraction of a unit a trip is noise, not a supply.
			boolean kept = perTrip < 0.5;
			int trips = kept ? Plan.UNLIMITED : (int) Math.min(Integer.MAX_VALUE - 1, (long) Math.floor(have / perTrip));

			long shortfall = kept ? 0 : Math.max(0, (long) Math.ceil(target * perTrip - have));
			int cheapest = cheapest(supply, prices);
			double unitPrice = cheapest < 0 ? 0 : prices.price(cheapest) / (double) supply.getUnits().get(cheapest);
			List<Plan.Buy> buys = new ArrayList<>();
			if (shortfall > 0 && cheapest >= 0)
			{
				int per = supply.getUnits().get(cheapest);
				long quantity = (shortfall + per - 1) / per;
				buys.add(new Plan.Buy(cheapest, quantity, prices.price(cheapest)));
			}
			long tripCost = kept || cheapest < 0 ? 0 : Math.round(perTrip * unitPrice);
			lines.add(new Plan.Line(supply, have, kept ? 0 : perTrip, fromTrips, trips, shortfall, buys, tripCost, heldOf(supply, held)));
		}
		return new Plan(kit, lines, target);
	}

	/** @return the item that buys this supply most cheaply per unit, or -1 when none has a price */
	static int cheapest(Supply supply, Prices prices)
	{
		int best = -1;
		double bestEach = 0;
		for (Map.Entry<Integer, Integer> e : supply.getUnits().entrySet())
		{
			long price = prices.price(e.getKey());
			if (price <= 0)
			{
				continue;
			}
			double each = price / (double) e.getValue();
			if (best < 0 || each < bestEach)
			{
				best = e.getKey();
				bestEach = each;
			}
		}
		return best;
	}

	/** What units of a supply cost at the cheapest item that buys them, or 0 without a price. */
	static long cost(Supply supply, long units, Prices prices)
	{
		int best = cheapest(supply, prices);
		return best < 0 ? 0 : Math.round(units * prices.price(best) / (double) supply.getUnits().get(best));
	}

	private static Map<Integer, Long> heldOf(Supply supply, Map<Integer, Long> held)
	{
		List<Map.Entry<Integer, Long>> found = new ArrayList<>();
		for (int id : supply.getUnits().keySet())
		{
			Long q = held.get(id);
			if (q != null && q > 0)
			{
				found.add(Map.entry(id, q));
			}
		}
		found.sort((a, b) -> Long.compare(b.getValue() * supply.getUnits().get(b.getKey()), a.getValue() * supply.getUnits().get(a.getKey())));
		Map<Integer, Long> out = new LinkedHashMap<>();
		for (Map.Entry<Integer, Long> e : found)
		{
			out.put(e.getKey(), e.getValue());
		}
		return out;
	}
}
