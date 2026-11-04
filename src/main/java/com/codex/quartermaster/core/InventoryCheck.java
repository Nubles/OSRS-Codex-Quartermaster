package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * The inventory against the kit, slot by slot, for marking at the bank: what the kit does not take
 * (loot, a potion from another trip), what is packed past the kit's amount, and a stack that is
 * short. The bank marks say what to take; these say what to put back, so between them the
 * inventory ends up as exactly one trip.
 *
 * <p>Kits count supplies, not slots, so nothing is marked for being in the "wrong" slot: a shark is
 * a shark wherever it sits. Gear and untradeable things that a kit never holds (a looting bag, a
 * weapon to switch to) are never marked, nor are coins.</p>
 */
public final class InventoryCheck
{
	/** What a slot is marked for. */
	public enum Kind
	{
		/** The kit does not take it: deposit it. */
		STRAY,
		/** More of a supply than the kit packs: deposit this much of it. */
		SURPLUS,
		/** A stack the kit wants more of: this much more to withdraw onto it. */
		SHORT
	}

	/** One marked inventory slot. */
	public static final class Mark
	{
		private final Kind kind;
		private final int itemId;
		private final long amount;
		private final boolean whole;

		Mark(Kind kind, int itemId, long amount, boolean whole)
		{
			this.kind = kind;
			this.itemId = itemId;
			this.amount = amount;
			this.whole = whole;
		}

		public Kind getKind()
		{
			return kind;
		}

		public int getItemId()
		{
			return itemId;
		}

		/** How many to deposit, or for a short stack how many more to withdraw. */
		public long getAmount()
		{
			return amount;
		}

		/** The whole slot goes, freeing it; false for part of a stack, or a short one. */
		public boolean isWhole()
		{
			return whole;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Mark))
			{
				return false;
			}
			Mark m = (Mark) o;
			return m.kind == kind && m.itemId == itemId && m.amount == amount && m.whole == whole;
		}

		@Override
		public int hashCode()
		{
			return ((kind.hashCode() * 31 + itemId) * 31 + Long.hashCode(amount)) * 2 + (whole ? 1 : 0);
		}
	}

	public static final InventoryCheck NONE = new InventoryCheck(Collections.emptyMap());

	private final Map<Integer, Mark> marks;

	private InventoryCheck(Map<Integer, Mark> marks)
	{
		this.marks = Collections.unmodifiableMap(marks);
	}

	/**
	 * @param packing the kit against everything carried, worn gear and pouches included
	 * @param ids the inventory's item in each slot, -1 for an empty one
	 * @param quantities how many are in each slot
	 * @param supply whether an item not in any pooled supply could be one a kit holds: stackable,
	 *     or tradeable and not worn, as a kit is saved from
	 * @param stackable whether an item stacks in one slot
	 */
	public static InventoryCheck of(Packing packing, int[] ids, int[] quantities, IntPredicate supply, IntPredicate stackable)
	{
		if (packing == null || ids == null || quantities == null)
		{
			return NONE;
		}
		int slots = Math.min(ids.length, quantities.length);
		Map<Integer, Mark> marks = new LinkedHashMap<>();
		Map<Packing.Item, List<Integer>> bySupply = new HashMap<>();
		for (int slot = 0; slot < slots; slot++)
		{
			int id = ids[slot];
			int qty = quantities[slot];
			if (id < 0 || qty <= 0)
			{
				continue;
			}
			Packing.Item owner = null;
			for (Packing.Item item : packing.getItems())
			{
				if (item.getSupply().counts(id))
				{
					owner = item;
					break;
				}
			}
			if (owner != null)
			{
				bySupply.computeIfAbsent(owner, k -> new ArrayList<>()).add(slot);
				continue;
			}
			if (Supplies.never(id) || !Supplies.pooled(id) && !supply.test(id) || inKit(packing.getKit(), id))
			{
				continue;
			}
			marks.put(slot, new Mark(Kind.STRAY, id, qty, true));
		}
		for (Packing.Item item : packing.getItems())
		{
			List<Integer> held = bySupply.get(item);
			if (held == null)
			{
				continue;
			}
			Supply s = item.getSupply();
			if (item.getCarried() > item.getTarget())
			{
				long excess = item.getCarried() - item.getTarget();
				// The last slots filled are the ones to put back, and partly used forms are left be:
				// they count for their share only, so putting one back is not a whole one less.
				for (int i = held.size() - 1; i >= 0 && excess > 0; i--)
				{
					int slot = held.get(i);
					Integer per = s.getUnits().get(ids[slot]);
					if (per == null || per <= 0)
					{
						continue;
					}
					long take = Math.min(quantities[slot], excess / per);
					if (take > 0)
					{
						marks.put(slot, new Mark(Kind.SURPLUS, ids[slot], take, take == quantities[slot]));
						excess -= take * per;
					}
				}
			}
			else if (!item.isPacked())
			{
				for (int slot : held)
				{
					Long more = item.getWithdraw().get(ids[slot]);
					if (more != null && more > 0 && stackable.test(ids[slot]))
					{
						marks.put(slot, new Mark(Kind.SHORT, ids[slot], more, false));
					}
				}
			}
		}
		return marks.isEmpty() ? NONE : new InventoryCheck(marks);
	}

	/** The mark on an inventory slot, or null. */
	public Mark at(int slot)
	{
		return marks.get(slot);
	}

	public Map<Integer, Mark> getMarks()
	{
		return marks;
	}

	public boolean isEmpty()
	{
		return marks.isEmpty();
	}

	/** Everything to deposit of one kind, by item, in slot order. */
	public Map<Integer, Long> toDeposit(Kind kind)
	{
		Map<Integer, Long> out = new LinkedHashMap<>();
		for (Mark m : marks.values())
		{
			if (m.kind == kind)
			{
				out.merge(m.itemId, m.amount, Long::sum);
			}
		}
		return out;
	}

	/** Inventory slots that depositing the marked items empties. */
	public int slotsFreed()
	{
		int n = 0;
		for (Mark m : marks.values())
		{
			if (m.kind != Kind.SHORT && m.whole)
			{
				n++;
			}
		}
		return n;
	}

	/** Whether the kit takes an item although the check leaves it out, as one trips show is never used. */
	private static boolean inKit(Kit kit, int id)
	{
		for (Need n : kit.getNeeds())
		{
			Supply s = Supplies.get(n.getSupply());
			if (s != null && s.counts(id))
			{
				return true;
			}
		}
		return false;
	}
}
