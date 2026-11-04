package com.codex.quartermaster.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The partly used forms of a saved kit's own items, so a kit that took two super strength(4) is
 * still packed when one of them has been sipped to a (2) and the other is full: the doses are
 * counted. Pooled supplies already count every dose; this covers the potions and other dosed
 * items a kit saves as items of their own.
 *
 * <p>Honest rather than fuzzy: a partly used form counts for its share, never as a whole one, so
 * two super strength(2) are one potion, not two. Which items are forms of one another comes from
 * RuneLite's item variation table (the one Bank Tags and Inventory Setups use) and is confirmed
 * here by name: a family is only counted by doses when the names say so, "Super strength(3)"
 * beside "Super strength(4)". Gear is left out, since its numbers are charges, not portions, and
 * a family named otherwise, such as a meat pie and half a meat pie, stays item by item.</p>
 *
 * <p>The table is learnt in the game, where item names can be read, and is empty until then: an
 * item not yet learnt is counted exactly, as before. Immutable maps are swapped in whole, so any
 * thread can read it.</p>
 */
public final class Variants
{
	/** A dosed name: "Super strength(4)", "Waterskin(2)". The number is the portions left. */
	private static final Pattern DOSES = Pattern.compile("^(.*\\S)\\s?\\((\\d{1,2})\\)$");

	/** One family's dose counts: the full item, its portions, and each partly used form's. */
	public static final class Family
	{
		private final int full;
		private final int doses;
		private final Map<Integer, Integer> partial;

		Family(int full, int doses, Map<Integer, Integer> partial)
		{
			this.full = full;
			this.doses = doses;
			this.partial = Collections.unmodifiableMap(new LinkedHashMap<>(partial));
		}

		/** The item a kit saved: the form a trip packs and the bank is pointed at. */
		public int getFull()
		{
			return full;
		}

		/** Portions in one full item: 4 for a super strength(4). */
		public int getDoses()
		{
			return doses;
		}

		/** Each partly used form with its portions left: super strength(2) is 2. */
		public Map<Integer, Integer> getPartial()
		{
			return partial;
		}
	}

	/** Item names and whether each is worn, as the game tells them. */
	public interface Lookup
	{
		String name(int itemId);

		boolean wearable(int itemId);
	}

	private static volatile Map<Integer, Family> families = Collections.emptyMap();
	/** Each partly used form, with the full item it is a part of. */
	private static volatile Map<Integer, Integer> partOf = Collections.emptyMap();

	private Variants()
	{
	}

	/** The portions a name says are left, or -1 when it names none. */
	static int doses(String name)
	{
		if (name == null)
		{
			return -1;
		}
		Matcher m = DOSES.matcher(name.trim());
		return m.matches() ? Integer.parseInt(m.group(2)) : -1;
	}

	/** The name without its portions: "Super strength" for "Super strength(4)". */
	static String stem(String name)
	{
		Matcher m = DOSES.matcher(name == null ? "" : name.trim());
		return m.matches() ? m.group(1) : null;
	}

	/**
	 * Works out an item's partly used forms from the ids RuneLite files with it.
	 *
	 * @param itemId the item a kit saved
	 * @param variations every id RuneLite's variation table files with it, itself included
	 * @return the family, or null when the item is not a dosed one or has no smaller forms
	 */
	public static Family family(int itemId, Iterable<Integer> variations, Lookup lookup)
	{
		String name = lookup.name(itemId);
		int doses = doses(name);
		String stem = stem(name);
		// One portion is a whole item already; and a worn item's number is charges, not portions.
		if (doses < 2 || stem == null || lookup.wearable(itemId))
		{
			return null;
		}
		Map<Integer, Integer> partial = new LinkedHashMap<>();
		for (int id : variations)
		{
			if (id == itemId || lookup.wearable(id))
			{
				continue;
			}
			String other = lookup.name(id);
			int left = doses(other);
			// Same potion by name, fewer portions left. Empty forms such as a vial hold nothing.
			if (left >= 1 && left < doses && stem.equalsIgnoreCase(stem(other)))
			{
				partial.put(id, left);
			}
		}
		return partial.isEmpty() ? null : new Family(itemId, doses, partial);
	}

	/** Adds a family to the table, replacing any for the same item. */
	public static synchronized void learn(Family family)
	{
		if (family == null || families.containsKey(family.full))
		{
			return;
		}
		Map<Integer, Family> f = new HashMap<>(families);
		f.put(family.full, family);
		Map<Integer, Integer> p = new HashMap<>(partOf);
		family.partial.keySet().forEach(id -> p.put(id, family.full));
		families = Collections.unmodifiableMap(f);
		partOf = Collections.unmodifiableMap(p);
	}

	/** Forgets every family; for tests. */
	static synchronized void clear()
	{
		families = Collections.emptyMap();
		partOf = Collections.emptyMap();
	}

	/** The family of a full item, or null when none is known. */
	public static Family of(int itemId)
	{
		return families.get(itemId);
	}

	/** The full item a partly used form belongs to, or -1. */
	public static int fullOf(int itemId)
	{
		Integer full = partOf.get(itemId);
		return full == null ? -1 : full;
	}
}
