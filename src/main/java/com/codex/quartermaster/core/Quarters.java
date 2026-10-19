package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Everything the Quartermaster keeps for one account: the bank as last seen, the kits the player
 * saved, which kit is chosen, the restock target, and what each kit has been measured using.
 *
 * <p>A trip runs from closing the bank to opening it again. What was carried out and what comes
 * back are compared, and the difference, counted per supply of the kit, is what the trip used.
 */
public class Quarters
{
	/** Trips shorter than this were a bank shuffle, not a trip. */
	static final long MIN_TRIP_MS = 20_000;
	/** How much of a saved kit must be packed for it to be recognised. */
	static final double MATCH = 0.75;
	public static final int DEFAULT_TARGET = 10;

	private Map<Integer, Long> bank = new HashMap<>();
	private long bankSeen;
	/** The Codex plugin whose saved bank {@link #bank} was taken from, or null when seen here. */
	private String bankFrom;
	private List<Kit> kits = new ArrayList<>();
	private String active;
	private int target = DEFAULT_TARGET;
	private Map<String, Usage> usage = new HashMap<>();
	private int saved;
	/** Potion doses in the bank's potion storage, by each potion's 1-dose item. */
	private Map<Integer, Long> stored = new HashMap<>();
	/** An iron account, remembered so the panel stays right while logged out. */
	private boolean iron;

	// A trip in progress is not saved: what happens between a logout and the next login is not known.
	private transient Map<Integer, Long> tripStart;
	private transient long tripStartAt;
	private transient String tripKit;

	public Map<Integer, Long> getBank()
	{
		return bank;
	}

	/** @return when the bank was last seen, or 0 when never */
	public long getBankSeen()
	{
		return bankSeen;
	}

	public boolean hasBank()
	{
		return bankSeen > 0;
	}

	public void observeBank(Map<Integer, Long> items, long now)
	{
		bank = new HashMap<>(items);
		bankSeen = now;
		bankFrom = null;
	}

	/**
	 * How much newer another plugin's bank must be before it replaces the one seen here. Both
	 * plugins see the same bank visit a moment apart; only a visit this plugin missed counts.
	 */
	static final long ADOPT_MARGIN_MS = 60_000;

	/**
	 * Takes the bank another Codex plugin saved, when it holds everything and is clearly newer
	 * than the bank this plugin last saw: the Quartermaster was off, or new, at that bank visit.
	 *
	 * @param from the plugin's name, shown with the bank's age
	 * @return whether it was taken
	 */
	public boolean adoptBank(SharedBank shared, String from)
	{
		if (shared == null || !shared.isComplete() || shared.getSavedAt() <= 0
			|| shared.getSavedAt() <= bankSeen + (hasBank() ? ADOPT_MARGIN_MS : 0))
		{
			return false;
		}
		bank = new HashMap<>(shared.getItems());
		bankSeen = shared.getSavedAt();
		bankFrom = from;
		return true;
	}

	/** The plugin the bank was taken from, or null when the Quartermaster saw it itself. */
	public String getBankFrom()
	{
		return bankFrom;
	}

	/** The doses in the bank's potion storage, read whenever the bank is open. */
	public void observeStorage(Map<Integer, Long> doses)
	{
		stored = new HashMap<>(doses);
	}

	public boolean isIron()
	{
		return iron;
	}

	public void setIron(boolean iron)
	{
		this.iron = iron;
	}

	public Map<Integer, Long> getStored()
	{
		return stored;
	}

	public int getTarget()
	{
		return target;
	}

	public void setTarget(int target)
	{
		this.target = Math.max(1, target);
	}

	/** The player's own kits in the order they were saved, then the presets. */
	public List<Kit> allKits()
	{
		List<Kit> all = new ArrayList<>(kits);
		all.addAll(Presets.all());
		return all;
	}

	public List<Kit> getSavedKits()
	{
		return Collections.unmodifiableList(kits);
	}

	public Kit kit(String id)
	{
		for (Kit k : allKits())
		{
			if (k.getId().equals(id))
			{
				return k;
			}
		}
		return null;
	}

	public Kit activeKit()
	{
		Kit k = active == null ? null : kit(active);
		return k != null ? k : allKits().get(0);
	}

	/**
	 * The kit the player chose, or null when none is: then the panel shows the first kit, but
	 * that is only a default, not the player's choice.
	 */
	public String chosenKitId()
	{
		return active != null && kit(active) != null ? active : null;
	}

	public void select(String id)
	{
		if (kit(id) != null)
		{
			active = id;
		}
	}

	public Usage usage(String kitId)
	{
		return usage.get(kitId);
	}

	/** The bank and what is carried, together. */
	public Map<Integer, Long> held(Map<Integer, Long> carried)
	{
		Map<Integer, Long> all = new HashMap<>(bank);
		carried.forEach((id, q) -> all.merge(id, q, Long::sum));
		return all;
	}

	/**
	 * The kits worth restocking together: every saved kit, and the chosen preset if one is chosen,
	 * since that is the player's too.
	 */
	public List<Kit> restockKits()
	{
		List<Kit> out = new ArrayList<>(kits);
		String chosen = chosenKitId();
		Kit k = chosen == null ? null : kit(chosen);
		if (k != null && k.isPreset())
		{
			out.add(k);
		}
		return out;
	}

	/** One shopping list for {@link #restockKits()}, each stocked for the restock target. */
	public Plan planAll(Map<Integer, Long> carried, Prices prices, boolean measured)
	{
		return Planner.planAll(restockKits(), usage, held(carried), stored, prices, target, measured);
	}

	public Plan plan(Kit kit, Map<Integer, Long> carried, Prices prices, boolean measured)
	{
		return Planner.plan(kit, usage.get(kit.getId()), held(carried), stored, prices, target, measured);
	}

	/**
	 * Saves what is carried as a new kit. Pooled supplies are counted together; anything else is
	 * kept as its own item when {@code supply} says it is one.
	 *
	 * @return the new kit, or null when nothing carried is a supply
	 */
	public Kit capture(String name, Map<Integer, Long> carried, IntPredicate supply)
	{
		return capture(name, carried, supply, id -> true);
	}

	/**
	 * @param stackable whether an item stacks; a single unstackable item that is not a pooled
	 *     supply, such as a spade, is taken to come back every trip until a trip shows otherwise
	 */
	public Kit capture(String name, Map<Integer, Long> carried, IntPredicate supply, IntPredicate stackable)
	{
		Map<String, Long> counts = new LinkedHashMap<>();
		List<Integer> ids = new ArrayList<>(carried.keySet());
		Collections.sort(ids);
		for (int id : ids)
		{
			Long q = carried.get(id);
			if (q == null || q <= 0 || Supplies.never(id) || !Supplies.pooled(id) && !supply.test(id))
			{
				continue;
			}
			String key = Supplies.keyFor(id);
			boolean tool = !Supplies.pooled(id) && q == 1 && !stackable.test(id);
			counts.merge(key, tool ? 0 : q * Supplies.get(key).getUnits().get(id), Long::sum);
		}
		if (counts.isEmpty())
		{
			return null;
		}
		List<Need> needs = new ArrayList<>();
		counts.forEach((key, units) -> needs.add(new Need(key, (int) Math.min(Integer.MAX_VALUE, units))));
		Kit kit = new Kit("kit:" + (++saved), name, needs, false);
		kits.add(kit);
		active = kit.getId();
		return kit;
	}

	/** Forgets a saved kit and everything measured for it. Presets cannot be forgotten. */
	public boolean forget(String id)
	{
		boolean removed = kits.removeIf(k -> k.getId().equals(id));
		if (removed)
		{
			usage.remove(id);
			if (id.equals(active))
			{
				active = null;
			}
		}
		return removed;
	}

	/**
	 * Sets how much of each supply a saved kit takes, as {@link Tuning} advises. What was measured
	 * stays: it is what trips used, whatever was packed.
	 *
	 * @return whether the kit changed
	 */
	public boolean tune(String kitId, List<Tuning.Advice> advice)
	{
		for (Kit k : kits)
		{
			if (k.getId().equals(kitId))
			{
				boolean changed = false;
				for (Tuning.Advice a : advice)
				{
					changed |= k.setPerTrip(a.getSupply().getKey(), a.getSuggested());
				}
				return changed;
			}
		}
		return false;
	}

	/** Drops a supply from a saved kit, such as a tool that was carried but is not used up. */
	public boolean drop(String kitId, String supply)
	{
		for (Kit k : kits)
		{
			if (k.getId().equals(kitId) && k.getNeeds().size() > 1 && k.remove(supply))
			{
				return true;
			}
		}
		return false;
	}

	/** How much of a kit is packed: the share of its supplies that something carried counts toward. */
	public static double packed(Kit kit, Map<Integer, Long> carried)
	{
		if (kit.getNeeds().isEmpty())
		{
			return 0;
		}
		int found = 0;
		for (Need n : kit.getNeeds())
		{
			Supply s = Supplies.get(n.getSupply());
			if (s != null && s.count(carried) > 0)
			{
				found++;
			}
		}
		return found / (double) kit.getNeeds().size();
	}

	/**
	 * How closely what is carried matches a kit: the share of the kit's supplies that are packed,
	 * marked down for each pooled supply carried that the kit does not take. So a food-only kit is
	 * not a match for a full boss inventory just because it has food in it.
	 */
	public static double match(Kit kit, Map<Integer, Long> carried)
	{
		if (kit.getNeeds().isEmpty())
		{
			return 0;
		}
		List<Supply> supplies = new ArrayList<>();
		int found = 0;
		for (Need n : kit.getNeeds())
		{
			Supply s = Supplies.get(n.getSupply());
			if (s == null)
			{
				continue;
			}
			supplies.add(s);
			if (s.count(carried) > 0)
			{
				found++;
			}
		}
		Set<String> extra = new HashSet<>();
		for (Map.Entry<Integer, Long> e : carried.entrySet())
		{
			int id = e.getKey();
			if (e.getValue() <= 0 || !Supplies.pooled(id))
			{
				continue;
			}
			boolean taken = false;
			for (Supply s : supplies)
			{
				taken |= s.getUnits().containsKey(id);
			}
			if (!taken)
			{
				extra.add(Supplies.keyFor(id));
			}
		}
		return found / (double) (kit.getNeeds().size() + extra.size());
	}

	/**
	 * Picks the saved kit that is packed, when one clearly is and it is not already chosen. A
	 * saved kit wins a tie with a preset, since the player made it for exactly this.
	 *
	 * @return the kit switched to, or null when the choice stays
	 */
	public Kit recognise(Map<Integer, Long> carried)
	{
		Kit current = activeKit();
		double best = match(current, carried);
		boolean presetChosen = current.isPreset();
		Kit pick = null;
		for (Kit k : kits)
		{
			if (k == current)
			{
				continue;
			}
			double score = match(k, carried);
			if (score >= MATCH && (score > best || presetChosen && pick == null && score >= best))
			{
				best = score;
				pick = k;
			}
		}
		if (pick != null)
		{
			active = pick.getId();
		}
		return pick;
	}

	public boolean onTrip()
	{
		return tripStart != null;
	}

	/** The bank closed: what is carried now is what the trip sets out with. */
	public void startTrip(Map<Integer, Long> carried, long now)
	{
		tripStart = new HashMap<>(carried);
		tripStartAt = now;
		tripKit = activeKit().getId();
	}

	/** The trip went wrong in a way that says nothing about supplies, such as a death. */
	public void voidTrip()
	{
		tripStart = null;
		tripKit = null;
	}

	/**
	 * The bank opened again: whatever left the inventory for the kit's supplies was used.
	 *
	 * @return the trip measured, or null when it was too short, used nothing, or was not this kit
	 */
	public Trip endTrip(Map<Integer, Long> carried, long now, Prices prices)
	{
		Map<Integer, Long> start = tripStart;
		Kit kit = tripKit == null ? null : kit(tripKit);
		long began = tripStartAt;
		voidTrip();
		if (start == null || kit == null || now - began < MIN_TRIP_MS || packed(kit, start) < 0.5)
		{
			return null;
		}
		Map<String, Long> used = new LinkedHashMap<>();
		Map<String, Long> took = new LinkedHashMap<>();
		long total = 0;
		long cost = 0;
		for (Need n : kit.getNeeds())
		{
			Supply s = Supplies.get(n.getSupply());
			if (s == null)
			{
				continue;
			}
			long u = Math.max(0, s.count(start) - s.count(carried));
			used.put(s.getKey(), u);
			took.put(s.getKey(), s.count(start));
			total += u;
			cost += Planner.cost(s, u, start, prices);
		}
		if (total == 0)
		{
			return null;
		}
		Trip trip = new Trip(began, now, used, took, cost);
		usage.computeIfAbsent(kit.getId(), k -> new Usage()).record(kit, trip);
		return trip;
	}

	/** Restores what saved data can leave out, after it is read back. */
	public Quarters repair()
	{
		if (bank == null)
		{
			bank = new HashMap<>();
		}
		if (kits == null)
		{
			kits = new ArrayList<>();
		}
		kits.removeIf(k -> k == null || k.getId() == null || k.getName() == null);
		kits.forEach(Kit::repair);
		if (usage == null)
		{
			usage = new HashMap<>();
		}
		usage.values().removeIf(u -> u == null);
		usage.values().forEach(Usage::repair);
		if (stored == null)
		{
			stored = new HashMap<>();
		}
		if (target < 1)
		{
			target = DEFAULT_TARGET;
		}
		return this;
	}
}
