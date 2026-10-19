package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * A kit laid out as the inventory one trip packs, slot by slot, for the kit card a player can
 * paste to a friend. Each supply is shown as the bank items the packing check would point at, so
 * a player who banks anglerfish sees anglerfish, not the sharks of a preset; a supply with none
 * banked is shown as its usual item. A stack takes one slot, anything else one slot an item.
 */
public final class Loadout
{
	/** Slots in an inventory. */
	public static final int SLOTS = 28;

	/** One inventory slot. */
	public static final class Slot
	{
		private final int itemId;
		private final long quantity;
		private final boolean banked;

		Slot(int itemId, long quantity, boolean banked)
		{
			this.itemId = itemId;
			this.quantity = quantity;
			this.banked = banked;
		}

		public int getItemId()
		{
			return itemId;
		}

		/** How many are in the slot: more than one only for a stack. */
		public long getQuantity()
		{
			return quantity;
		}

		/** Whether the bank holds this item; false when it is the supply's usual item, shown for want of one. */
		public boolean isBanked()
		{
			return banked;
		}
	}

	private final List<Slot> slots;
	private final int overflow;

	private Loadout(List<Slot> slots, int overflow)
	{
		this.slots = Collections.unmodifiableList(slots);
		this.overflow = overflow;
	}

	/** The slots, at most {@link #SLOTS}, in the kit's own order. */
	public List<Slot> getSlots()
	{
		return slots;
	}

	/** Slots the kit needs past a full inventory, which the card notes rather than draws. */
	public int getOverflow()
	{
		return overflow;
	}

	/**
	 * @param bank the bank as last seen, by item
	 * @param stackable whether an item stacks in the inventory
	 */
	public static Loadout of(Kit kit, Map<Integer, Long> bank, IntPredicate stackable)
	{
		List<Slot> slots = new ArrayList<>();
		int overflow = 0;
		Packing packing = Packing.check(kit, Collections.emptyMap(), bank == null ? Collections.emptyMap() : bank);
		for (Packing.Item item : packing.getItems())
		{
			Supply supply = item.getSupply();
			Map<Integer, Long> withdraw = item.getWithdraw();
			// What the bank holds toward a trip, then the supply's usual item, faded, for any of the
			// trip it cannot make up, so a kit the bank runs short of still looks short.
			List<Slot> take = new ArrayList<>();
			long covered = 0;
			for (Map.Entry<Integer, Long> e : withdraw.entrySet())
			{
				take.add(new Slot(e.getKey(), e.getValue(), true));
				Integer per = supply.getUnits().get(e.getKey());
				covered += e.getValue() * (per == null ? 1 : per);
			}
			long missing = item.getTarget() - covered;
			if (withdraw.isEmpty() || missing > 0)
			{
				int id = supply.getIcon();
				Integer per = supply.getUnits().get(id);
				take.add(new Slot(id, per == null || per <= 0 ? missing : (missing + per - 1) / per, false));
			}
			for (Slot e : take)
			{
				int id = e.itemId;
				boolean inBank = e.banked;
				long count = Math.max(1, e.quantity);
				if (stackable.test(id))
				{
					if (slots.size() < SLOTS)
					{
						slots.add(new Slot(id, count, inBank));
					}
					else
					{
						overflow++;
					}
					continue;
				}
				for (long i = 0; i < count; i++)
				{
					if (slots.size() < SLOTS)
					{
						slots.add(new Slot(id, 1, inBank));
					}
					else
					{
						// Counted, not looped past a sane size: a kit of 2,000 sharks is not drawn.
						overflow += (int) Math.min(Integer.MAX_VALUE - (long) overflow, count - i);
						break;
					}
				}
			}
		}
		return new Loadout(slots, overflow);
	}
}
