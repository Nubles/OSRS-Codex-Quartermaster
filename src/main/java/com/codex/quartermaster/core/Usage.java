package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one kit really uses, learned from the trips the player has made with it. The first trips
 * are averaged plainly; after that each new trip moves the average by an eighth, so a change of
 * gear or method shows within a few trips without one bad trip swinging the plan.
 */
public class Usage
{
	/** Trips before the measured amounts are trusted over the kit's own. */
	public static final int TRUSTED = 2;
	private static final int WINDOW = 8;
	private static final int RECENT = 30;

	private int trips;
	private Map<String, Double> perTrip = new LinkedHashMap<>();
	/** Trips each supply has been measured over, which is fewer than all for one added later. */
	private Map<String, Integer> counts = new LinkedHashMap<>();
	private List<Trip> recent = new ArrayList<>();

	public int getTrips()
	{
		return trips;
	}

	public boolean isTrusted()
	{
		return trips >= TRUSTED;
	}

	/** @return the average used a trip, or a negative number when this supply was never measured */
	public double perTrip(String supply)
	{
		Double v = perTrip.get(supply);
		return v == null ? -1 : v;
	}

	/** Measured trips, newest last. */
	public List<Trip> getRecent()
	{
		return recent;
	}

	public Trip last()
	{
		return recent.isEmpty() ? null : recent.get(recent.size() - 1);
	}

	/** The average supply cost of the measured trips at the prices of their day, or -1 with none. */
	public long averageCost()
	{
		if (recent.isEmpty())
		{
			return -1;
		}
		long sum = 0;
		for (Trip t : recent)
		{
			sum += t.getCost();
		}
		return sum / recent.size();
	}

	/**
	 * How long a trip usually takes: the median of the last ten measured trips, so one trip left
	 * idling at the bank does not stretch it. Zero until two trips are measured.
	 */
	public long typicalDuration()
	{
		if (recent.size() < TRUSTED)
		{
			return 0;
		}
		List<Long> times = new ArrayList<>();
		for (int i = Math.max(0, recent.size() - 10); i < recent.size(); i++)
		{
			times.add(recent.get(i).duration());
		}
		Collections.sort(times);
		int n = times.size();
		return n % 2 == 1 ? times.get(n / 2) : (times.get(n / 2 - 1) + times.get(n / 2)) / 2;
	}

	void record(Kit kit, Trip trip)
	{
		trips++;
		for (Need need : kit.getNeeds())
		{
			String key = need.getSupply();
			long used = trip.getUsed().getOrDefault(key, 0L);
			Double avg = perTrip.get(key);
			// A supply added to the kit after it was first measured starts from its first trip. Saves
			// from before supplies were counted apart have measured every supply on every trip.
			Integer count = counts.get(key);
			int seen = (count != null ? count : avg == null ? 0 : trips - 1) + 1;
			counts.put(key, seen);
			perTrip.put(key, avg == null ? used : avg + (used - avg) / Math.min(seen, WINDOW));
		}
		recent.add(trip);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
		}
	}

	Usage repair()
	{
		if (perTrip == null)
		{
			perTrip = new LinkedHashMap<>();
		}
		if (counts == null)
		{
			counts = new LinkedHashMap<>();
		}
		if (recent == null)
		{
			recent = new ArrayList<>();
		}
		recent.removeIf(t -> t == null || t.getUsed() == null);
		recent.forEach(Trip::repair);
		while (recent.size() > RECENT)
		{
			recent.remove(0);
		}
		// An average that is not a number, or below zero, was never a measurement.
		perTrip.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || !(e.getValue() >= 0));
		perTrip.replaceAll((k, v) -> Math.min(v, Quarters.AMOUNT_MAX));
		counts.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null || e.getValue() < 0);
		trips = Math.max(0, trips);
		return this;
	}
}
