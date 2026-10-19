package com.codex.quartermaster.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What is still to pack, as the bank can show it through RuneLite's Bank Tags plugin: the bank is
 * filtered to only the items the packing check says to withdraw, as a tag tab would be. An item
 * leaves the view once enough of it is in the pack, so what the bank shows is what is left to take.
 * It is a view and nothing more: the player's own tags are never touched and nothing is withdrawn.
 *
 * <p>The view answers to {@link #TAG} in the bank search too ({@code tag:quartermaster}).
 * Immutable, so the client thread can read it while the plugin hands a new one over.</p>
 */
public final class BankView
{
	public static final String TAG = "quartermaster";
	public static final BankView EMPTY = new BankView(Collections.emptySet());

	/** Why the bank could not be shown filtered, if it could not. */
	public enum Refusal
	{
		/** The bank window is not open. */
		BANK_CLOSED,
		/** RuneLite's Bank Tags plugin is turned off. */
		TAGS_OFF,
		/** Everything is packed: there is nothing to show. */
		NOTHING
	}

	private final Set<Integer> ids;

	private BankView(Set<Integer> ids)
	{
		this.ids = Collections.unmodifiableSet(ids);
	}

	/** @param packing the chosen kit against what is carried at the bank, or null away from it */
	public static BankView of(Packing packing)
	{
		if (packing == null)
		{
			return EMPTY;
		}
		Map<Integer, Long> take = packing.withdrawals();
		Set<Integer> ids = new HashSet<>();
		take.forEach((id, n) ->
		{
			if (n > 0)
			{
				ids.add(id);
			}
		});
		return ids.isEmpty() ? EMPTY : new BankView(ids);
	}

	/** @param itemId a bank item's id, noted and placeholder forms folded in */
	public boolean contains(int itemId)
	{
		return ids.contains(itemId);
	}

	public boolean isEmpty()
	{
		return ids.isEmpty();
	}

	/**
	 * @param bankOpen the bank window is open
	 * @param tagsOn RuneLite's Bank Tags plugin is on
	 * @return null when the bank can be shown filtered now, else why not
	 */
	public Refusal refusal(boolean bankOpen, boolean tagsOn)
	{
		if (!tagsOn)
		{
			return Refusal.TAGS_OFF;
		}
		if (!bankOpen)
		{
			return Refusal.BANK_CLOSED;
		}
		return isEmpty() ? Refusal.NOTHING : null;
	}

	/** The line the chat box says when the bank could not be shown filtered. */
	public static String explain(Refusal refusal)
	{
		switch (refusal)
		{
			case TAGS_OFF:
				return "turn on RuneLite's Bank Tags plugin to show what to pack in your bank.";
			case NOTHING:
				return "everything is packed.";
			default:
				return "open your bank first. Searching tag:" + TAG + " in the bank shows what to pack too.";
		}
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof BankView && ((BankView) o).ids.equals(ids);
	}

	@Override
	public int hashCode()
	{
		return ids.hashCode();
	}
}
