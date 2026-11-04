package com.codex.quartermaster.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One thing a trip uses up, counted in its own unit: food in hitpoints healed, potions in doses,
 * bolts one by one. A supply can be met by several items, so four doses of prayer potion and four
 * of super restore count the same, and a shark and a manta ray both count as food.
 */
public final class Supply
{
	private static final String ITEM = "item:";

	private final String key;
	private final String name;
	private final String unit;
	private final int icon;
	private final Map<Integer, Integer> units;
	/** Each item's full form: the 4-dose potion for its lower doses, a whole cake for a slice. */
	private final Map<Integer, Integer> full;
	/** For a dosed item of its own, its partly used forms, counted for their share; else null. */
	private final Variants.Family family;

	Supply(String key, String name, String unit, int icon, Map<Integer, Integer> units, Map<Integer, Integer> full)
	{
		this(key, name, unit, icon, units, full, null);
	}

	private Supply(String key, String name, String unit, int icon, Map<Integer, Integer> units, Map<Integer, Integer> full,
		Variants.Family family)
	{
		this.family = family;
		this.key = key;
		this.name = name;
		this.unit = unit;
		this.icon = icon;
		this.units = Collections.unmodifiableMap(new LinkedHashMap<>(units));
		this.full = Collections.unmodifiableMap(new LinkedHashMap<>(full));
	}

	/**
	 * A supply that is one item and nothing else, such as a teleport tablet or a rune. A dosed item
	 * also counts its partly used forms for their share, once {@link Variants} has learnt them: the
	 * item is still what is withdrawn and bought.
	 */
	public static Supply item(int itemId)
	{
		Map<Integer, Integer> one = Collections.singletonMap(itemId, 1);
		return new Supply(itemKey(itemId), null, null, itemId, one, Collections.singletonMap(itemId, itemId), Variants.of(itemId));
	}

	public static String itemKey(int itemId)
	{
		return ITEM + itemId;
	}

	public String getKey()
	{
		return key;
	}

	/** @return the supply's name, or null for a single item, which is named after the item */
	public String getName()
	{
		return name;
	}

	/** @return "hp", "doses" and the like, or null when it is counted in items */
	public String getUnit()
	{
		return unit;
	}

	public int getIcon()
	{
		return icon;
	}

	public boolean isItem()
	{
		return key.startsWith(ITEM);
	}

	/** Every item that counts, with how many units one of it is worth. */
	public Map<Integer, Integer> getUnits()
	{
		return units;
	}

	/** The form of an item a player buys or withdraws whole: a potion's 4-dose, a cake for a slice. */
	public int fullForm(int itemId)
	{
		Integer f = full.get(itemId);
		return f == null ? itemId : f;
	}

	/** Whether this item is bought as it is, rather than being a part of something else. */
	public boolean isFullForm(int itemId)
	{
		return units.containsKey(itemId) && fullForm(itemId) == itemId;
	}

	/** The partly used forms that count toward this supply for their share, or empty. */
	public Map<Integer, Integer> getPartial()
	{
		return family == null ? Collections.emptyMap() : family.getPartial();
	}

	/** Whether an item counts toward this supply, whole or for its share. */
	public boolean counts(int itemId)
	{
		return units.containsKey(itemId) || getPartial().containsKey(itemId);
	}

	/**
	 * How much of this supply a set of items holds, in units. Partly used forms are added up by
	 * their doses and count only in whole items' worth, rounded down: two super strength(2) are one
	 * super strength(4), and a lone (3) is not yet one.
	 */
	public long count(Map<Integer, Long> items)
	{
		long total = family == null ? 0 : partialWorth(items);
		for (Map.Entry<Integer, Integer> e : units.entrySet())
		{
			Long qty = items.get(e.getKey());
			if (qty != null && qty > 0)
			{
				// A count near the top of a long is held there rather than wrapping below zero.
				long units = qty > Long.MAX_VALUE / e.getValue() ? Long.MAX_VALUE : qty * e.getValue();
				total = units > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + units;
			}
		}
		return total;
	}

	private long partialWorth(Map<Integer, Long> items)
	{
		long doses = 0;
		for (Map.Entry<Integer, Integer> e : family.getPartial().entrySet())
		{
			Long qty = items.get(e.getKey());
			if (qty != null && qty > 0)
			{
				// Held from overflowing as count() is; no real stack comes near.
				doses = qty > (Long.MAX_VALUE - doses) / e.getValue() ? Long.MAX_VALUE : doses + qty * e.getValue();
			}
		}
		return doses / family.getDoses();
	}
}
