package com.codex.quartermaster.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What trips bring back. A trip's loot is what is carried home less what was carried out, at
 * today's prices: drops, coins, and the like. Supplies are left out, since a four-dose potion
 * drunk once comes home as a three-dose one, and so are empty vials. Loot kept in a looting bag
 * is not seen, so a trip that fills one reads low.
 */
public final class Returns
{
	static final int COINS = 995;
	static final int PLATINUM = 13204;
	static final int VIAL = 229;

	private final int trips;
	private final long loot;
	private final long cost;
	private final long time;

	private Returns(int trips, long loot, long cost, long time)
	{
		this.trips = trips;
		this.loot = loot;
		this.cost = cost;
		this.time = time;
	}

	/**
	 * Every item that is not a supply counts both ways: what came home is added and what set out
	 * and did not come back is taken off. So coins paid at the Grand Exchange or a shop mid-trip
	 * pay for what they bought, and gear whose id changes as it degrades or loses charges (a
	 * Barrows piece is priced whole at every stage) nets to nothing.
	 *
	 * @param start what the trip set out with
	 * @param end what came back
	 * @return the value of what came back less what set out, supplies aside, in coins
	 */
	public static long loot(Kit kit, Map<Integer, Long> start, Map<Integer, Long> end, Prices prices)
	{
		Set<Integer> kitItems = new HashSet<>();
		for (Need n : kit.getNeeds())
		{
			Supply s = Supplies.get(n.getSupply());
			if (s != null)
			{
				kitItems.addAll(s.getUnits().keySet());
			}
		}
		Set<Integer> ids = new HashSet<>(start.keySet());
		ids.addAll(end.keySet());
		long total = 0;
		for (int id : ids)
		{
			long change = end.getOrDefault(id, 0L) - start.getOrDefault(id, 0L);
			if (change == 0 || id == VIAL || kitItems.contains(id) || Supplies.pooled(id))
			{
				continue;
			}
			long each = id == COINS ? 1 : id == PLATINUM ? 1000 : Math.max(0, prices.price(id));
			total += change * each;
		}
		return total;
	}

	/** The trips that know their loot, summed. */
	public static Returns of(List<Trip> trips)
	{
		int n = 0;
		long loot = 0;
		long cost = 0;
		long time = 0;
		for (Trip t : trips)
		{
			if (t.getLoot() == null)
			{
				continue;
			}
			n++;
			loot += t.getLoot();
			cost += t.getCost();
			time += t.duration();
		}
		return new Returns(n, loot, cost, time);
	}

	/** Trips counted. */
	public int getTrips()
	{
		return trips;
	}

	/** Worth trusting: at least two trips that know their loot. */
	public boolean isKnown()
	{
		return trips >= Usage.TRUSTED;
	}

	public long lootPerTrip()
	{
		return trips == 0 ? 0 : loot / trips;
	}

	public long costPerTrip()
	{
		return trips == 0 ? 0 : cost / trips;
	}

	/** Loot less supplies, a trip. */
	public long netPerTrip()
	{
		return trips == 0 ? 0 : (loot - cost) / trips;
	}

	/** Loot less supplies an hour out of the bank, or 0 with no time measured. */
	public long netPerHour()
	{
		return time <= 0 ? 0 : (long) ((loot - cost) * 3_600_000.0 / time);
	}
}
