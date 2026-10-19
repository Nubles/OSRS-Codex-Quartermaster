package com.codex.quartermaster.core;

import java.util.List;

/**
 * The "you left without" line said when closing the bank with a kit mostly packed but a supply
 * not packed at all. A quick dash back in to grab it and out again says nothing new, so the same
 * line is not repeated within a few minutes.
 */
public final class Reminder
{
	static final long AGAIN_MS = 3 * 60_000L;

	private String last;
	private long lastAt;

	/**
	 * @param names what was forgotten, in lower case, most urgent first
	 * @return the line to say, or null when there is nothing new to say
	 */
	public String check(String kitName, List<String> names, long now)
	{
		if (names.isEmpty())
		{
			last = null;
			return null;
		}
		String text = text(kitName, names);
		if (text.equals(last) && now - lastAt < AGAIN_MS)
		{
			return null;
		}
		last = text;
		lastAt = now;
		return text;
	}

	static String text(String kitName, List<String> names)
	{
		StringBuilder list = new StringBuilder();
		int shown = Math.min(names.size(), 3);
		for (int i = 0; i < shown; i++)
		{
			if (i > 0)
			{
				list.append(i == shown - 1 && names.size() <= 3 ? " and " : ", ");
			}
			list.append(names.get(i));
		}
		if (names.size() > 3)
		{
			list.append(" and ").append(names.size() - 3).append(" more");
		}
		return "you left the bank without " + list + " for " + kitName + ".";
	}
}
