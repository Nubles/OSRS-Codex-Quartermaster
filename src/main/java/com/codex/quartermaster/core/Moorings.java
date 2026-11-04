package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where each of the player's boats is moored, as the game's per-boat variables say. The game
 * keeps up to five boats; each has an owned flag, a type, a port and a three-part name.
 *
 * The port and type numbers are looked up in the game's own dock and boat tables, by the id
 * column those tables carry. That the variables hold those ids is how the Dude, Where's My Boat?
 * plugin reads them too; the wiki does not document it, so the panel says so.
 */
public final class Moorings
{
	/** One boat the player owns. */
	public static final class Berth
	{
		private final int slot;
		private final String name;
		private final String type;
		private final String port;

		public Berth(int slot, String name, String type, String port)
		{
			this.slot = slot;
			this.name = name;
			this.type = type;
			this.port = port;
		}

		/** 1 to 5, the game's own numbering of the player's boats. */
		public int getSlot()
		{
			return slot;
		}

		/** The boat's name as the player chose it, or "Boat 2" when it has none. */
		public String getName()
		{
			return name;
		}

		/** "Raft", "Skiff", "Sloop", or null when the game's table has no name for it. */
		public String getType()
		{
			return type;
		}

		/** The port's name, or null when the game's dock table has no row for it. */
		public String getPort()
		{
			return port;
		}
	}

	/** What the game says of one boat slot, before names are looked up. */
	public static final class Raw
	{
		final int owned;
		final int type;
		final int port;
		final int[] name;

		public Raw(int owned, int type, int port, int name1, int name2, int name3)
		{
			this.owned = owned;
			this.type = type;
			this.port = port;
			this.name = new int[]{name1, name2, name3};
		}

		/**
		 * Whether the slot holds a boat. The owned flag alone has been seen unset for a boat that is
		 * there, so a chosen name or a port past the first also count.
		 */
		public boolean present()
		{
			return owned > 0 || port > 0 || name[0] + name[1] + name[2] > 0;
		}
	}

	/** Looks names up in the game's tables; a miss is null. */
	public interface Tables
	{
		String dock(int portId);

		String boat(int typeId);

		/**
		 * One part of a boat's name.
		 *
		 * @param part 0 for the prefix, 1 the descriptor, 2 the noun
		 * @param index the option chosen, from 0
		 */
		String namePart(int part, int index);
	}

	private final List<Berth> berths;

	private Moorings(List<Berth> berths)
	{
		this.berths = Collections.unmodifiableList(berths);
	}

	public static final Moorings NONE = new Moorings(Collections.emptyList());

	/** @param slots the game's five boat slots, first to fifth */
	public static Moorings of(List<Raw> slots, Tables tables)
	{
		List<Berth> out = new ArrayList<>();
		for (int i = 0; i < slots.size(); i++)
		{
			Raw raw = slots.get(i);
			if (raw == null || !raw.present())
			{
				continue;
			}
			out.add(new Berth(i + 1, name(raw, i + 1, tables), tables.boat(raw.type), tables.dock(raw.port)));
		}
		return new Moorings(out);
	}

	/**
	 * A boat's name from its three parts. Each variable holds the option chosen plus one; a part
	 * left at its first option, the blank one, adds nothing.
	 */
	static String name(Raw raw, int slot, Tables tables)
	{
		StringBuilder sb = new StringBuilder();
		for (int p = 0; p < 3; p++)
		{
			int index = raw.name[p] - 1;
			if (index <= 0)
			{
				continue;
			}
			String part = tables.namePart(p, index);
			if (part != null && !part.trim().isEmpty())
			{
				if (sb.length() > 0)
				{
					sb.append(' ');
				}
				sb.append(part.trim());
			}
		}
		return sb.length() == 0 ? "Boat " + slot : sb.toString();
	}

	public List<Berth> getBerths()
	{
		return berths;
	}

	public boolean isEmpty()
	{
		return berths.isEmpty();
	}
}
