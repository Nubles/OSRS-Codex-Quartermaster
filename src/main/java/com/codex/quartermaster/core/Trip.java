package com.codex.quartermaster.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** One measured trip: when it ran, what each supply it took was used down by, and what that cost. */
public class Trip
{
	private long start;
	private long end;
	private Map<String, Long> used = new LinkedHashMap<>();
	/** What each supply set out with; absent on trips measured before it was kept. */
	private Map<String, Long> took;
	private long cost;
	/** What came back that did not set out, at the prices of its day; absent on trips from before it was kept. */
	private Long loot;

	public Trip()
	{
	}

	Trip(long start, long end, Map<String, Long> used, long cost)
	{
		this(start, end, used, null, cost);
	}

	Trip(long start, long end, Map<String, Long> used, Map<String, Long> took, long cost)
	{
		this.start = start;
		this.end = end;
		this.used = new LinkedHashMap<>(used);
		this.took = took == null ? null : new LinkedHashMap<>(took);
		this.cost = cost;
	}

	/** What a supply set out with, or {@code otherwise} for a trip from before that was kept. */
	public long took(String supply, long otherwise)
	{
		Long t = took == null ? null : took.get(supply);
		return t == null ? otherwise : t;
	}

	/** Notes what a supply set out with, for a trip from before trips noted it; a noted amount stands. */
	void tookAtLeast(String supply, long amount)
	{
		if (took == null)
		{
			took = new LinkedHashMap<>();
		}
		took.putIfAbsent(supply, amount);
	}

	public long getStart()
	{
		return start;
	}

	public long getEnd()
	{
		return end;
	}

	public Map<String, Long> getUsed()
	{
		return used;
	}

	public long getCost()
	{
		return cost;
	}

	/** The trip's loot, or null for a trip measured before loot was kept. */
	public Long getLoot()
	{
		return loot;
	}

	Trip loot(long loot)
	{
		this.loot = loot;
		return this;
	}

	/** Loot less supplies, or null when the loot is not known. */
	public Long net()
	{
		return loot == null ? null : loot - cost;
	}

	public long duration()
	{
		return Math.max(0, end - start);
	}
}
