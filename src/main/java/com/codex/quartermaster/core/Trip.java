package com.codex.quartermaster.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** One measured trip: when it ran, what each supply it took was used down by, and what that cost. */
public class Trip
{
	private long start;
	private long end;
	private Map<String, Long> used = new LinkedHashMap<>();
	private long cost;

	public Trip()
	{
	}

	Trip(long start, long end, Map<String, Long> used, long cost)
	{
		this.start = start;
		this.end = end;
		this.used = new LinkedHashMap<>(used);
		this.cost = cost;
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

	public long duration()
	{
		return Math.max(0, end - start);
	}
}
