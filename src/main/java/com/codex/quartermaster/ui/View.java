package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Packing;
import com.codex.quartermaster.core.Plan;
import com.codex.quartermaster.core.Supply;
import com.codex.quartermaster.core.Trip;
import com.codex.quartermaster.core.Tuning;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Everything the panel shows, worked out on the client thread and handed over whole. */
public final class View
{
	/** What the panel has to show. */
	public enum State
	{
		LOGGED_OUT,
		WAITING_FOR_BANK,
		READY
	}

	final State state;
	final String player;
	final Plan active;
	final List<Plan> plans;
	final List<Trip> trips;
	final int measured;
	final long bankSeen;
	final boolean bankOpen;
	final Map<Integer, String> names;
	final int warn;
	final long now;
	/** An iron account, which gathers its supplies rather than buying them. */
	boolean ironman;
	/** The chosen kit against what is carried, while packing at the bank; null otherwise. */
	Packing packing;
	/** How long a trip with the chosen kit usually takes, or 0 until measured. */
	long tripMs;
	/** The Codex plugin whose saved bank is shown, or null when the Quartermaster saw it itself. */
	String bankFrom;
	/** One plan restocking every saved kit together, or null with fewer than two. */
	Plan all;
	/** The chosen kit is a preset the player picked, so it is restocked with the saved ones. */
	boolean chosenPreset;
	/** How the chosen saved kit's amounts should change, from its trips; empty when they are fine. */
	List<Tuning.Advice> tuning = Collections.emptyList();

	/**
	 * @param active the chosen kit's plan, or null when no account is open
	 * @param plans every kit's plan, the chosen one among them
	 * @param trips the chosen kit's measured trips, newest last
	 * @param measured how many trips the chosen kit has been measured over
	 */
	public View(State state, String player, Plan active, List<Plan> plans, List<Trip> trips, int measured,
		long bankSeen, boolean bankOpen, Map<Integer, String> names, int warn, long now)
	{
		this.state = state;
		this.player = player;
		this.active = active;
		this.plans = plans == null ? Collections.emptyList() : plans;
		this.trips = trips == null ? Collections.emptyList() : trips;
		this.measured = measured;
		this.bankSeen = bankSeen;
		this.bankOpen = bankOpen;
		this.names = names == null ? Collections.emptyMap() : names;
		this.warn = warn;
		this.now = now;
	}

	public static View loggedOut()
	{
		return new View(State.LOGGED_OUT, null, null, null, null, 0, 0, false, null, 3, System.currentTimeMillis());
	}

	public View all(Plan plan, boolean presetChosen)
	{
		all = plan;
		chosenPreset = presetChosen;
		return this;
	}

	public View tuning(List<Tuning.Advice> advice)
	{
		tuning = advice == null ? Collections.emptyList() : advice;
		return this;
	}

	public View bankFrom(String plugin)
	{
		bankFrom = plugin;
		return this;
	}

	public View ironman(boolean value)
	{
		ironman = value;
		return this;
	}

	/**
	 * @param packing the packing check to show at the bank, or null
	 * @param tripMs a typical trip's length, or 0
	 */
	public View packing(Packing packing, long tripMs)
	{
		this.packing = packing;
		this.tripMs = tripMs;
		return this;
	}

	String name(int itemId)
	{
		String n = names.get(itemId);
		return n == null ? "Item " + itemId : n;
	}

	/** A supply's name: its own for pooled supplies, the item's for single items. */
	String name(Plan.Line line)
	{
		String n = line.getSupply().getName();
		return n != null ? n : name(line.getSupply().getIcon());
	}

	/** A supply's name: its own for pooled supplies, the item's for single items. */
	String name(Supply supply)
	{
		return supply.getName() != null ? supply.getName() : name(supply.getIcon());
	}

	/** "doses", "hp", or nothing for single items. */
	static String unit(Plan.Line line)
	{
		return unit(line.getSupply());
	}

	static String unit(Supply supply)
	{
		String u = supply.getUnit();
		return u == null ? "" : " " + u;
	}
}
