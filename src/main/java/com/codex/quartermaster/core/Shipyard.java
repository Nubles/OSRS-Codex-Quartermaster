package com.codex.quartermaster.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import net.runelite.api.gameval.ItemID;

/**
 * The boat's core parts as the shipyard builds them, tier by tier, and what the bank still needs
 * for each. Every level and material here is copied from the wiki's Hull, Keel, Helm and
 * Mast and sails pages; facilities are left out, as are fees and stats.
 */
public final class Shipyard
{
	private Shipyard()
	{
	}

	/** The boats a player can own, with the Sailing level the wiki's Boat page gives for each. */
	public enum Boat
	{
		RAFT("Raft", 1),
		SKIFF("Skiff", 15),
		SLOOP("Sloop", 50);

		private final String label;
		private final int sailing;

		Boat(String label, int sailing)
		{
			this.label = label;
			this.sailing = sailing;
		}

		public String getLabel()
		{
			return label;
		}

		public int getSailing()
		{
			return sailing;
		}

		/** The boat the game names, such as "Skiff", or null for a boat this does not plan. */
		public static Boat named(String name)
		{
			if (name == null)
			{
				return null;
			}
			for (Boat b : values())
			{
				if (b.label.equalsIgnoreCase(name.trim()))
				{
					return b;
				}
			}
			return null;
		}
	}

	/** A core part, in the order the shipyard's schematics list them. */
	public enum Part
	{
		HULL("Hull"),
		KEEL("Keel"),
		HELM("Helm"),
		SAILS("Mast and sails");

		private final String label;

		Part(String label)
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}
	}

	/** One item a design takes, and how many. */
	public static final class Material
	{
		private final int itemId;
		private final int quantity;

		Material(int itemId, int quantity)
		{
			this.itemId = itemId;
			this.quantity = quantity;
		}

		public int getItemId()
		{
			return itemId;
		}

		public int getQuantity()
		{
			return quantity;
		}
	}

	/** One tier of one part for one boat, such as the oak skiff hull. */
	public static final class Design
	{
		private final Boat boat;
		private final Part part;
		private final int tier;
		private final String name;
		private final int sailing;
		private final int construction;
		private final List<Material> materials;

		Design(Boat boat, Part part, int tier, String name, int sailing, int construction, List<Material> materials)
		{
			this.boat = boat;
			this.part = part;
			this.tier = tier;
			this.name = name;
			this.sailing = sailing;
			this.construction = construction;
			this.materials = Collections.unmodifiableList(materials);
		}

		public Boat getBoat()
		{
			return boat;
		}

		public Part getPart()
		{
			return part;
		}

		/** 0 for the first tier (wooden or bronze) up to 6 (rosewood or dragon). */
		public int getTier()
		{
			return tier;
		}

		public String getName()
		{
			return name;
		}

		public int getSailing()
		{
			return sailing;
		}

		public int getConstruction()
		{
			return construction;
		}

		public List<Material> getMaterials()
		{
			return materials;
		}

		/** Both levels are met. */
		public boolean unlocked(int sailingLevel, int constructionLevel)
		{
			return sailingLevel >= sailing && constructionLevel >= construction;
		}
	}

	private static final String[] WOODS = {"Wooden", "Oak", "Teak", "Mahogany", "Camphor", "Ironwood", "Rosewood"};
	private static final String[] METALS = {"Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune", "Dragon"};

	private static final int[] LOGS = {ItemID.LOGS, ItemID.OAK_LOGS, ItemID.TEAK_LOGS, ItemID.MAHOGANY_LOGS,
		ItemID.CAMPHOR_LOGS, ItemID.IRONWOOD_LOGS, ItemID.ROSEWOOD_LOGS};
	private static final int[] PLANKS = {ItemID.WOODPLANK, ItemID.PLANK_OAK, ItemID.PLANK_TEAK, ItemID.PLANK_MAHOGANY,
		ItemID.PLANK_CAMPHOR, ItemID.PLANK_IRONWOOD, ItemID.PLANK_ROSEWOOD};
	/** Bronze, iron, steel, mithril, adamantite, rune and dragon nails. */
	private static final int[] NAILS = {ItemID.NAILS_BRONZE, ItemID.NAILS_IRON, ItemID.NAILS, ItemID.NAILS_MITHRIL,
		ItemID.NAILS_ADAMANT, ItemID.NAILS_RUNE, ItemID.NAILS_DRAGON};
	/** A helm's metal: bars, then dragon metal sheets for the dragon helm. */
	private static final int[] HELM_METAL = {ItemID.BRONZE_BAR, ItemID.IRON_BAR, ItemID.STEEL_BAR, ItemID.MITHRIL_BAR,
		ItemID.ADAMANTITE_BAR, ItemID.RUNITE_BAR, ItemID.DRAGON_SHEET};
	/** Linen for the wooden and oak masts, canvas for teak to camphor, cotton for ironwood and rosewood. */
	private static final int[] BOLTS = {ItemID.BOLT_OF_LINEN, ItemID.BOLT_OF_LINEN, ItemID.BOLT_OF_CANVAS, ItemID.BOLT_OF_CANVAS,
		ItemID.BOLT_OF_CANVAS, ItemID.BOLT_OF_COTTON, ItemID.BOLT_OF_COTTON};
	private static final String[] SAILCLOTH = {"linen", "linen", "canvas", "canvas", "canvas", "cotton", "cotton"};
	private static final int[] HULL_PARTS = {ItemID.SAILING_BOAT_HULL_PART_WOODEN, ItemID.SAILING_BOAT_HULL_PART_OAK,
		ItemID.SAILING_BOAT_HULL_PART_TEAK, ItemID.SAILING_BOAT_HULL_PART_MAHOGANY, ItemID.SAILING_BOAT_HULL_PART_CAMPHOR,
		ItemID.SAILING_BOAT_HULL_PART_IRONWOOD, ItemID.SAILING_BOAT_HULL_PART_ROSEWOOD};
	private static final int[] LARGE_HULL_PARTS = {ItemID.SAILING_BOAT_LARGE_HULL_PART_WOODEN, ItemID.SAILING_BOAT_LARGE_HULL_PART_OAK,
		ItemID.SAILING_BOAT_LARGE_HULL_PART_TEAK, ItemID.SAILING_BOAT_LARGE_HULL_PART_MAHOGANY, ItemID.SAILING_BOAT_LARGE_HULL_PART_CAMPHOR,
		ItemID.SAILING_BOAT_LARGE_HULL_PART_IRONWOOD, ItemID.SAILING_BOAT_LARGE_HULL_PART_ROSEWOOD};
	private static final int[] KEEL_PARTS = {ItemID.SAILING_BOAT_KEEL_PART_BRONZE, ItemID.SAILING_BOAT_KEEL_PART_IRON,
		ItemID.SAILING_BOAT_KEEL_PART_STEEL, ItemID.SAILING_BOAT_KEEL_PART_MITHRIL, ItemID.SAILING_BOAT_KEEL_PART_ADAMANT,
		ItemID.SAILING_BOAT_KEEL_PART_RUNE, ItemID.SAILING_BOAT_KEEL_PART_DRAGON};
	private static final int[] LARGE_KEEL_PARTS = {ItemID.SAILING_BOAT_LARGE_KEEL_PART_BRONZE, ItemID.SAILING_BOAT_LARGE_KEEL_PART_IRON,
		ItemID.SAILING_BOAT_LARGE_KEEL_PART_STEEL, ItemID.SAILING_BOAT_LARGE_KEEL_PART_MITHRIL, ItemID.SAILING_BOAT_LARGE_KEEL_PART_ADAMANT,
		ItemID.SAILING_BOAT_LARGE_KEEL_PART_RUNE, ItemID.SAILING_BOAT_LARGE_KEEL_PART_DRAGON};

	/**
	 * The bar every hull and keel from the third tier up adds: five lead bars for teak (steel) to
	 * camphor (adamant), five cupronickel bars for ironwood (rune) and rosewood (dragon); none below.
	 */
	private static int strengtheningBar(int tier)
	{
		return tier < 2 ? -1 : tier < 5 ? ItemID.LEAD_BAR : ItemID.CUPRONICKEL_BAR;
	}

	// Levels by tier, as the wiki's tables give them. Sloop hulls, keels and masts below the sloop's
	// own level 50 are listed at 50 there; helms keep the same levels on every boat.
	private static final int[] HULL_SAILING = {1, 20, 31, 48, 67, 81, 93};
	private static final int[] HULL_CONSTRUCTION = {1, 8, 23, 41, 59, 75, 84};
	private static final int[] KEEL_SAILING = {15, 22, 39, 54, 66, 85, 97};
	private static final int[] KEEL_CONSTRUCTION = {1, 17, 32, 50, 62, 78, 87};
	private static final int[] HELM_SAILING = {1, 17, 38, 55, 72, 87, 96};
	private static final int[] HELM_CONSTRUCTION = {1, 14, 30, 47, 59, 81, 86};
	private static final int[] SAILS_SAILING = {1, 24, 36, 52, 68, 83, 94};
	private static final int[] SAILS_CONSTRUCTION = {1, 11, 26, 45, 60, 77, 85};

	private static final List<Design> DESIGNS = new ArrayList<>();

	static
	{
		for (int t = 0; t < 7; t++)
		{
			String wood = WOODS[t];
			String metal = METALS[t];
			int bar = strengtheningBar(t);

			// Hulls: a raft's is logs and rope, a skiff's hull parts, a sloop's large hull parts.
			List<Material> raft = list(LOGS[t], 10, ItemID.ROPE, 6, ItemID.SWAMP_TAR, 10, bar, 5);
			List<Material> skiff = list(HULL_PARTS[t], 10, NAILS[t], 300, ItemID.SWAMP_TAR, 20, bar, 5);
			List<Material> sloop = list(LARGE_HULL_PARTS[t], 16, NAILS[t], 600, ItemID.SWAMP_TAR, 25, bar, 5);
			String hull = wood + " hull";
			add(Boat.RAFT, Part.HULL, t, hull, HULL_SAILING[t], HULL_CONSTRUCTION[t], raft);
			add(Boat.SKIFF, Part.HULL, t, hull, Math.max(Boat.SKIFF.sailing, HULL_SAILING[t]), HULL_CONSTRUCTION[t], skiff);
			add(Boat.SLOOP, Part.HULL, t, hull, Math.max(Boat.SLOOP.sailing, HULL_SAILING[t]), HULL_CONSTRUCTION[t], sloop);

			// Keels: none on a raft.
			add(Boat.SKIFF, Part.KEEL, t, metal + " skiff keel", KEEL_SAILING[t], KEEL_CONSTRUCTION[t],
				list(KEEL_PARTS[t], 10, bar, 5));
			add(Boat.SLOOP, Part.KEEL, t, metal + " sloop keel", Math.max(Boat.SLOOP.sailing, KEEL_SAILING[t]), KEEL_CONSTRUCTION[t],
				list(LARGE_KEEL_PARTS[t], 16, bar, 5));

			// Helms: planks and the metal, two, three and four planks by boat.
			for (Boat b : Boat.values())
			{
				int planks = b.ordinal() + 2;
				add(b, Part.HELM, t, metal + " " + b.label.toLowerCase() + " helm", HELM_SAILING[t], HELM_CONSTRUCTION[t],
					list(PLANKS[t], planks, HELM_METAL[t], planks * 2));
			}

			// Mast and sails: logs, nails and bolts of cloth.
			String mast = wood + " mast and " + SAILCLOTH[t] + " sails";
			add(Boat.RAFT, Part.SAILS, t, mast, SAILS_SAILING[t], SAILS_CONSTRUCTION[t],
				list(LOGS[t], 5, NAILS[t], 20, BOLTS[t], 5));
			add(Boat.SKIFF, Part.SAILS, t, mast, SAILS_SAILING[t], SAILS_CONSTRUCTION[t],
				list(LOGS[t], 10, NAILS[t], 40, BOLTS[t], 5));
			add(Boat.SLOOP, Part.SAILS, t, mast, Math.max(Boat.SLOOP.sailing, SAILS_SAILING[t]), SAILS_CONSTRUCTION[t],
				list(LOGS[t], 15, NAILS[t], 60, BOLTS[t], 10));
		}
		DESIGNS.sort((a, b) -> a.boat != b.boat ? a.boat.compareTo(b.boat)
			: a.part != b.part ? a.part.compareTo(b.part) : Integer.compare(a.tier, b.tier));
	}

	private static void add(Boat boat, Part part, int tier, String name, int sailing, int construction, List<Material> materials)
	{
		DESIGNS.add(new Design(boat, part, tier, name, sailing, construction, materials));
	}

	/** Pairs of item and quantity; an item of -1 is left out. */
	private static List<Material> list(int... pairs)
	{
		List<Material> out = new ArrayList<>();
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			if (pairs[i] >= 0)
			{
				out.add(new Material(pairs[i], pairs[i + 1]));
			}
		}
		return out;
	}

	/** Every design, boat by boat, part by part, lowest tier first. */
	public static List<Design> designs()
	{
		return Collections.unmodifiableList(DESIGNS);
	}

	/** One part's designs for one boat, lowest tier first; empty for a raft's keel. */
	public static List<Design> designs(Boat boat, Part part)
	{
		List<Design> out = new ArrayList<>();
		for (Design d : DESIGNS)
		{
			if (d.boat == boat && d.part == part)
			{
				out.add(d);
			}
		}
		return out;
	}

	/** The parts a boat has: every part but the keel on a raft. */
	public static List<Part> parts(Boat boat)
	{
		List<Part> out = new ArrayList<>();
		for (Part p : Part.values())
		{
			if (!designs(boat, p).isEmpty())
			{
				out.add(p);
			}
		}
		return out;
	}

	/** Where a design stands for the player. */
	public enum Status
	{
		/** Both levels met and every material held. */
		READY,
		/** Both levels met, some materials still to get. */
		MATERIALS,
		/** The Sailing level is met but not the Construction level. */
		CONSTRUCTION,
		/** The Sailing level is not met. */
		LOCKED
	}

	/** One design against the player's levels and items: each material held against what it takes. */
	public static final class Check
	{
		private final Design design;
		private final Status status;
		private final Map<Integer, Long> held;
		private final Map<Integer, Long> missing;

		Check(Design design, Status status, Map<Integer, Long> held, Map<Integer, Long> missing)
		{
			this.design = design;
			this.status = status;
			this.held = held;
			this.missing = missing;
		}

		public Design getDesign()
		{
			return design;
		}

		public Status getStatus()
		{
			return status;
		}

		/** What is held of each material, in the design's order. */
		public Map<Integer, Long> getHeld()
		{
			return held;
		}

		/** What is still to get of each material that falls short, in the design's order. */
		public Map<Integer, Long> getMissing()
		{
			return missing;
		}
	}

	/** One part's line on the roadmap: the best design the levels allow, and the one after it. */
	public static final class Step
	{
		private final Part part;
		private final Check best;
		private final Check next;

		Step(Part part, Check best, Check next)
		{
			this.part = part;
			this.best = best;
			this.next = next;
		}

		public Part getPart()
		{
			return part;
		}

		/** The highest tier both levels allow, or null when none does yet. */
		public Check getBest()
		{
			return best;
		}

		/** The tier after the best, or null when the best is the last. */
		public Check getNext()
		{
			return next;
		}
	}

	/** A boat's roadmap: a step for each of its parts, and the shopping list for what can be built now. */
	public static final class Roadmap
	{
		private final Boat boat;
		private final int sailing;
		private final int construction;
		private final List<Step> steps;

		Roadmap(Boat boat, int sailing, int construction, List<Step> steps)
		{
			this.boat = boat;
			this.sailing = sailing;
			this.construction = construction;
			this.steps = Collections.unmodifiableList(steps);
		}

		public Boat getBoat()
		{
			return boat;
		}

		public int getSailing()
		{
			return sailing;
		}

		public int getConstruction()
		{
			return construction;
		}

		public List<Step> getSteps()
		{
			return steps;
		}

		/** How many parts can be built right now, levels and materials both. */
		public int readyCount()
		{
			int n = 0;
			for (Step s : steps)
			{
				if (s.best != null && s.best.status == Status.READY)
				{
					n++;
				}
			}
			return n;
		}

		/**
		 * What is still to get for every best design together. A material two parts share, such
		 * as swamp tar or lead bars, is counted against what is held once, for both.
		 */
		public Map<Integer, Long> shoppingList(ToLongFunction<Integer> held)
		{
			Map<Integer, Long> need = new LinkedHashMap<>();
			for (Step s : steps)
			{
				if (s.best == null)
				{
					continue;
				}
				for (Material m : s.best.design.materials)
				{
					need.merge(m.itemId, (long) m.quantity, Long::sum);
				}
			}
			Map<Integer, Long> out = new LinkedHashMap<>();
			need.forEach((id, n) ->
			{
				long have = Math.max(0, held.applyAsLong(id));
				if (have < n)
				{
					out.put(id, n - have);
				}
			});
			return out;
		}

		/**
		 * The nearest Sailing level that unlocks a new tier of any part, or 0 when every part is at
		 * its last tier or waits only on Construction.
		 */
		public int nextSailing()
		{
			int best = 0;
			for (Step s : steps)
			{
				if (s.next != null && s.next.design.sailing > sailing && (best == 0 || s.next.design.sailing < best))
				{
					best = s.next.design.sailing;
				}
			}
			return best;
		}
	}

	/** One design against the levels and what is held. */
	public static Check check(Design design, int sailing, int construction, ToLongFunction<Integer> held)
	{
		Map<Integer, Long> have = new LinkedHashMap<>();
		Map<Integer, Long> missing = new LinkedHashMap<>();
		for (Material m : design.materials)
		{
			long h = Math.max(0, held.applyAsLong(m.itemId));
			have.put(m.itemId, h);
			if (h < m.quantity)
			{
				missing.put(m.itemId, m.quantity - h);
			}
		}
		Status status = sailing < design.sailing ? Status.LOCKED
			: construction < design.construction ? Status.CONSTRUCTION
			: missing.isEmpty() ? Status.READY : Status.MATERIALS;
		return new Check(design, status, Collections.unmodifiableMap(have), Collections.unmodifiableMap(missing));
	}

	/**
	 * The roadmap for a boat: for each part, the highest tier both levels allow, and the tier
	 * after it with what it waits on.
	 *
	 * @param held how many of an item the bank and inventory hold together
	 */
	public static Roadmap roadmap(Boat boat, int sailing, int construction, ToLongFunction<Integer> held)
	{
		List<Step> steps = new ArrayList<>();
		for (Part part : parts(boat))
		{
			List<Design> tiers = designs(boat, part);
			int best = -1;
			for (int i = 0; i < tiers.size(); i++)
			{
				if (tiers.get(i).unlocked(sailing, construction))
				{
					best = i;
				}
			}
			Check bestCheck = best < 0 ? null : check(tiers.get(best), sailing, construction, held);
			Check next = best + 1 < tiers.size() ? check(tiers.get(best + 1), sailing, construction, held) : null;
			steps.add(new Step(part, bestCheck, next));
		}
		return new Roadmap(boat, sailing, construction, steps);
	}

	/** The best boat a Sailing level allows, for a player who has not been seen aboard one. */
	public static Boat bestFor(int sailing)
	{
		Boat best = Boat.RAFT;
		for (Boat b : Boat.values())
		{
			if (sailing >= b.sailing)
			{
				best = b;
			}
		}
		return best;
	}
}
