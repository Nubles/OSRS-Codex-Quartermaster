package com.codex.quartermaster.core;

/**
 * The low supply line said in the chat box when setting out. It is said once for a kit at a
 * number of trips left: a quick dash back into the bank says nothing new, so it is not repeated
 * until the count changes or a quarter of an hour has passed.
 */
public final class Warning
{
	static final long AGAIN_MS = 15 * 60_000L;

	private String last;
	private long lastAt;

	/**
	 * @param trips trips the bank supports, counting what is carried
	 * @param warnAt say nothing above this many trips
	 * @param supply what runs out first, in lower case
	 * @return the line to say, or null when there is nothing new to say
	 */
	public String check(String kitId, String kitName, int trips, int warnAt, String supply, long now)
	{
		if (trips > warnAt || trips == Plan.UNLIMITED)
		{
			return null;
		}
		String key = kitId + ":" + trips;
		if (key.equals(last) && now - lastAt < AGAIN_MS)
		{
			return null;
		}
		last = key;
		lastAt = now;
		return text(kitName, trips, supply);
	}

	/** What is carried counts, so one trip left is the one setting out now. */
	static String text(String kitName, int trips, String supply)
	{
		if (trips == 0)
		{
			return "not enough " + supply + " for a full " + kitName + " trip.";
		}
		if (trips == 1)
		{
			return "this is your last " + kitName + " trip before " + supply + " runs out.";
		}
		return trips + " " + kitName + " trips left, counting this one; " + supply + " runs out first.";
	}
}
