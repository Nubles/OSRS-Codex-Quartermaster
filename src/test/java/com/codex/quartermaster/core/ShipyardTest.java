package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ShipyardTest
{
	/**
	 * Every row of the wiki's Hull, Keel, Helm and Mast and sails tables, as the pages give them:
	 * boat | name | Sailing | Construction | materials. The shipyard's tables must say the same.
	 */
	private static final String[] WIKI = {
		"Raft|Wooden hull|1|1|Logs x10; Rope x6; Swamp tar x10",
		"Raft|Oak hull|20|8|Oak logs x10; Rope x6; Swamp tar x10",
		"Raft|Teak hull|31|23|Teak logs x10; Rope x6; Swamp tar x10; Lead bar x5",
		"Raft|Mahogany hull|48|41|Mahogany logs x10; Rope x6; Swamp tar x10; Lead bar x5",
		"Raft|Camphor hull|67|59|Camphor logs x10; Rope x6; Swamp tar x10; Lead bar x5",
		"Raft|Ironwood hull|81|75|Ironwood logs x10; Rope x6; Swamp tar x10; Cupronickel bar x5",
		"Raft|Rosewood hull|93|84|Rosewood logs x10; Rope x6; Swamp tar x10; Cupronickel bar x5",
		"Skiff|Wooden hull|15|1|Wooden hull parts x10; Bronze nails x300; Swamp tar x20",
		"Skiff|Oak hull|20|8|Oak hull parts x10; Iron nails x300; Swamp tar x20",
		"Skiff|Teak hull|31|23|Teak hull parts x10; Steel nails x300; Swamp tar x20; Lead bar x5",
		"Skiff|Mahogany hull|48|41|Mahogany hull parts x10; Mithril nails x300; Swamp tar x20; Lead bar x5",
		"Skiff|Camphor hull|67|59|Camphor hull parts x10; Adamantite nails x300; Swamp tar x20; Lead bar x5",
		"Skiff|Ironwood hull|81|75|Ironwood hull parts x10; Rune nails x300; Swamp tar x20; Cupronickel bar x5",
		"Skiff|Rosewood hull|93|84|Rosewood hull parts x10; Dragon nails x300; Swamp tar x20; Cupronickel bar x5",
		"Sloop|Wooden hull|50|1|Large wooden hull parts x16; Bronze nails x600; Swamp tar x25",
		"Sloop|Oak hull|50|8|Large oak hull parts x16; Iron nails x600; Swamp tar x25",
		"Sloop|Teak hull|50|23|Large teak hull parts x16; Steel nails x600; Swamp tar x25; Lead bar x5",
		"Sloop|Mahogany hull|50|41|Large mahogany hull parts x16; Mithril nails x600; Swamp tar x25; Lead bar x5",
		"Sloop|Camphor hull|67|59|Large camphor hull parts x16; Adamantite nails x600; Swamp tar x25; Lead bar x5",
		"Sloop|Ironwood hull|81|75|Large ironwood hull parts x16; Rune nails x600; Swamp tar x25; Cupronickel bar x5",
		"Sloop|Rosewood hull|93|84|Large rosewood hull parts x16; Dragon nails x600; Swamp tar x25; Cupronickel bar x5",
		"Skiff|Bronze skiff keel|15|1|Bronze keel parts x10",
		"Skiff|Iron skiff keel|22|17|Iron keel parts x10",
		"Skiff|Steel skiff keel|39|32|Steel keel parts x10; Lead bar x5",
		"Skiff|Mithril skiff keel|54|50|Mithril keel parts x10; Lead bar x5",
		"Skiff|Adamant skiff keel|66|62|Adamant keel parts x10; Lead bar x5",
		"Skiff|Rune skiff keel|85|78|Rune keel parts x10; Cupronickel bar x5",
		"Skiff|Dragon skiff keel|97|87|Dragon keel parts x10; Cupronickel bar x5",
		"Sloop|Bronze sloop keel|50|1|Large bronze keel parts x16",
		"Sloop|Iron sloop keel|50|17|Large iron keel parts x16",
		"Sloop|Steel sloop keel|50|32|Large steel keel parts x16; Lead bar x5",
		"Sloop|Mithril sloop keel|54|50|Large mithril keel parts x16; Lead bar x5",
		"Sloop|Adamant sloop keel|66|62|Large adamant keel parts x16; Lead bar x5",
		"Sloop|Rune sloop keel|85|78|Large rune keel parts x16; Cupronickel bar x5",
		"Sloop|Dragon sloop keel|97|87|Large dragon keel parts x16; Cupronickel bar x5",
		"Raft|Bronze raft helm|1|1|Plank x2; Bronze bar x4",
		"Raft|Iron raft helm|17|14|Oak plank x2; Iron bar x4",
		"Raft|Steel raft helm|38|30|Teak plank x2; Steel bar x4",
		"Raft|Mithril raft helm|55|47|Mahogany plank x2; Mithril bar x4",
		"Raft|Adamant raft helm|72|59|Camphor plank x2; Adamantite bar x4",
		"Raft|Rune raft helm|87|81|Ironwood plank x2; Runite bar x4",
		"Raft|Dragon raft helm|96|86|Rosewood plank x2; Dragon metal sheet x4",
		"Skiff|Bronze skiff helm|1|1|Plank x3; Bronze bar x6",
		"Skiff|Iron skiff helm|17|14|Oak plank x3; Iron bar x6",
		"Skiff|Steel skiff helm|38|30|Teak plank x3; Steel bar x6",
		"Skiff|Mithril skiff helm|55|47|Mahogany plank x3; Mithril bar x6",
		"Skiff|Adamant skiff helm|72|59|Camphor plank x3; Adamantite bar x6",
		"Skiff|Rune skiff helm|87|81|Ironwood plank x3; Runite bar x6",
		"Skiff|Dragon skiff helm|96|86|Rosewood plank x3; Dragon metal sheet x6",
		"Sloop|Bronze sloop helm|1|1|Plank x4; Bronze bar x8",
		"Sloop|Iron sloop helm|17|14|Oak plank x4; Iron bar x8",
		"Sloop|Steel sloop helm|38|30|Teak plank x4; Steel bar x8",
		"Sloop|Mithril sloop helm|55|47|Mahogany plank x4; Mithril bar x8",
		"Sloop|Adamant sloop helm|72|59|Camphor plank x4; Adamantite bar x8",
		"Sloop|Rune sloop helm|87|81|Ironwood plank x4; Runite bar x8",
		"Sloop|Dragon sloop helm|96|86|Rosewood plank x4; Dragon metal sheet x8",
		"Raft|Wooden mast and linen sails|1|1|Logs x5; Bronze nails x20; Bolt of linen x5",
		"Raft|Oak mast and linen sails|24|11|Oak logs x5; Iron nails x20; Bolt of linen x5",
		"Raft|Teak mast and canvas sails|36|26|Teak logs x5; Steel nails x20; Bolt of canvas x5",
		"Raft|Mahogany mast and canvas sails|52|45|Mahogany logs x5; Mithril nails x20; Bolt of canvas x5",
		"Raft|Camphor mast and canvas sails|68|60|Camphor logs x5; Adamantite nails x20; Bolt of canvas x5",
		"Raft|Ironwood mast and cotton sails|83|77|Ironwood logs x5; Rune nails x20; Bolt of cotton x5",
		"Raft|Rosewood mast and cotton sails|94|85|Rosewood logs x5; Dragon nails x20; Bolt of cotton x5",
		"Skiff|Wooden mast and linen sails|1|1|Logs x10; Bronze nails x40; Bolt of linen x5",
		"Skiff|Oak mast and linen sails|24|11|Oak logs x10; Iron nails x40; Bolt of linen x5",
		"Skiff|Teak mast and canvas sails|36|26|Teak logs x10; Steel nails x40; Bolt of canvas x5",
		"Skiff|Mahogany mast and canvas sails|52|45|Mahogany logs x10; Mithril nails x40; Bolt of canvas x5",
		"Skiff|Camphor mast and canvas sails|68|60|Camphor logs x10; Adamantite nails x40; Bolt of canvas x5",
		"Skiff|Ironwood mast and cotton sails|83|77|Ironwood logs x10; Rune nails x40; Bolt of cotton x5",
		"Skiff|Rosewood mast and cotton sails|94|85|Rosewood logs x10; Dragon nails x40; Bolt of cotton x5",
		"Sloop|Wooden mast and linen sails|50|1|Logs x15; Bronze nails x60; Bolt of linen x10",
		"Sloop|Oak mast and linen sails|50|11|Oak logs x15; Iron nails x60; Bolt of linen x10",
		"Sloop|Teak mast and canvas sails|50|26|Teak logs x15; Steel nails x60; Bolt of canvas x10",
		"Sloop|Mahogany mast and canvas sails|52|45|Mahogany logs x15; Mithril nails x60; Bolt of canvas x10",
		"Sloop|Camphor mast and canvas sails|68|60|Camphor logs x15; Adamantite nails x60; Bolt of canvas x10",
		"Sloop|Ironwood mast and cotton sails|83|77|Ironwood logs x15; Rune nails x60; Bolt of cotton x10",
		"Sloop|Rosewood mast and cotton sails|94|85|Rosewood logs x15; Dragon nails x60; Bolt of cotton x10",
	};

	/** The wiki's item names, by the game's item ids. */
	private static final Map<Integer, String> NAMES = new HashMap<>();

	static
	{
		NAMES.put(ItemID.LOGS, "Logs");
		NAMES.put(ItemID.OAK_LOGS, "Oak logs");
		NAMES.put(ItemID.TEAK_LOGS, "Teak logs");
		NAMES.put(ItemID.MAHOGANY_LOGS, "Mahogany logs");
		NAMES.put(ItemID.CAMPHOR_LOGS, "Camphor logs");
		NAMES.put(ItemID.IRONWOOD_LOGS, "Ironwood logs");
		NAMES.put(ItemID.ROSEWOOD_LOGS, "Rosewood logs");
		NAMES.put(ItemID.ROPE, "Rope");
		NAMES.put(ItemID.SWAMP_TAR, "Swamp tar");
		NAMES.put(ItemID.LEAD_BAR, "Lead bar");
		NAMES.put(ItemID.CUPRONICKEL_BAR, "Cupronickel bar");
		NAMES.put(ItemID.NAILS_BRONZE, "Bronze nails");
		NAMES.put(ItemID.NAILS_IRON, "Iron nails");
		NAMES.put(ItemID.NAILS, "Steel nails");
		NAMES.put(ItemID.NAILS_MITHRIL, "Mithril nails");
		NAMES.put(ItemID.NAILS_ADAMANT, "Adamantite nails");
		NAMES.put(ItemID.NAILS_RUNE, "Rune nails");
		NAMES.put(ItemID.NAILS_DRAGON, "Dragon nails");
		NAMES.put(ItemID.WOODPLANK, "Plank");
		NAMES.put(ItemID.PLANK_OAK, "Oak plank");
		NAMES.put(ItemID.PLANK_TEAK, "Teak plank");
		NAMES.put(ItemID.PLANK_MAHOGANY, "Mahogany plank");
		NAMES.put(ItemID.PLANK_CAMPHOR, "Camphor plank");
		NAMES.put(ItemID.PLANK_IRONWOOD, "Ironwood plank");
		NAMES.put(ItemID.PLANK_ROSEWOOD, "Rosewood plank");
		NAMES.put(ItemID.BRONZE_BAR, "Bronze bar");
		NAMES.put(ItemID.IRON_BAR, "Iron bar");
		NAMES.put(ItemID.STEEL_BAR, "Steel bar");
		NAMES.put(ItemID.MITHRIL_BAR, "Mithril bar");
		NAMES.put(ItemID.ADAMANTITE_BAR, "Adamantite bar");
		NAMES.put(ItemID.RUNITE_BAR, "Runite bar");
		NAMES.put(ItemID.DRAGON_SHEET, "Dragon metal sheet");
		NAMES.put(ItemID.BOLT_OF_LINEN, "Bolt of linen");
		NAMES.put(ItemID.BOLT_OF_CANVAS, "Bolt of canvas");
		NAMES.put(ItemID.BOLT_OF_COTTON, "Bolt of cotton");
		String[] woods = {"Wooden", "Oak", "Teak", "Mahogany", "Camphor", "Ironwood", "Rosewood"};
		int[] hull = {ItemID.SAILING_BOAT_HULL_PART_WOODEN, ItemID.SAILING_BOAT_HULL_PART_OAK, ItemID.SAILING_BOAT_HULL_PART_TEAK,
			ItemID.SAILING_BOAT_HULL_PART_MAHOGANY, ItemID.SAILING_BOAT_HULL_PART_CAMPHOR, ItemID.SAILING_BOAT_HULL_PART_IRONWOOD,
			ItemID.SAILING_BOAT_HULL_PART_ROSEWOOD};
		int[] large = {ItemID.SAILING_BOAT_LARGE_HULL_PART_WOODEN, ItemID.SAILING_BOAT_LARGE_HULL_PART_OAK,
			ItemID.SAILING_BOAT_LARGE_HULL_PART_TEAK, ItemID.SAILING_BOAT_LARGE_HULL_PART_MAHOGANY,
			ItemID.SAILING_BOAT_LARGE_HULL_PART_CAMPHOR, ItemID.SAILING_BOAT_LARGE_HULL_PART_IRONWOOD,
			ItemID.SAILING_BOAT_LARGE_HULL_PART_ROSEWOOD};
		String[] metals = {"Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune", "Dragon"};
		int[] keel = {ItemID.SAILING_BOAT_KEEL_PART_BRONZE, ItemID.SAILING_BOAT_KEEL_PART_IRON, ItemID.SAILING_BOAT_KEEL_PART_STEEL,
			ItemID.SAILING_BOAT_KEEL_PART_MITHRIL, ItemID.SAILING_BOAT_KEEL_PART_ADAMANT, ItemID.SAILING_BOAT_KEEL_PART_RUNE,
			ItemID.SAILING_BOAT_KEEL_PART_DRAGON};
		int[] largeKeel = {ItemID.SAILING_BOAT_LARGE_KEEL_PART_BRONZE, ItemID.SAILING_BOAT_LARGE_KEEL_PART_IRON,
			ItemID.SAILING_BOAT_LARGE_KEEL_PART_STEEL, ItemID.SAILING_BOAT_LARGE_KEEL_PART_MITHRIL,
			ItemID.SAILING_BOAT_LARGE_KEEL_PART_ADAMANT, ItemID.SAILING_BOAT_LARGE_KEEL_PART_RUNE,
			ItemID.SAILING_BOAT_LARGE_KEEL_PART_DRAGON};
		for (int i = 0; i < 7; i++)
		{
			NAMES.put(hull[i], woods[i] + " hull parts");
			NAMES.put(large[i], "Large " + woods[i].toLowerCase() + " hull parts");
			NAMES.put(keel[i], metals[i] + " keel parts");
			NAMES.put(largeKeel[i], "Large " + metals[i].toLowerCase() + " keel parts");
		}
	}

	private static String row(Shipyard.Design d)
	{
		List<String> mats = new ArrayList<>();
		for (Shipyard.Material m : d.getMaterials())
		{
			mats.add(NAMES.get(m.getItemId()) + " x" + m.getQuantity());
		}
		return d.getBoat().getLabel() + "|" + d.getName() + "|" + d.getSailing() + "|" + d.getConstruction() + "|" + String.join("; ", mats);
	}

	@Test
	public void everyDesignMatchesTheWikiRowForRow()
	{
		List<String> ours = new ArrayList<>();
		for (Shipyard.Design d : Shipyard.designs())
		{
			ours.add(row(d));
		}
		List<String> wiki = new ArrayList<>();
		Collections.addAll(wiki, WIKI);
		Collections.sort(ours);
		Collections.sort(wiki);
		assertEquals(wiki, ours);
	}

	@Test
	public void aRaftHasNoKeel()
	{
		assertTrue(Shipyard.designs(Shipyard.Boat.RAFT, Shipyard.Part.KEEL).isEmpty());
		assertEquals(3, Shipyard.parts(Shipyard.Boat.RAFT).size());
		assertEquals(4, Shipyard.parts(Shipyard.Boat.SLOOP).size());
	}

	@Test
	public void boatsAreNamedAsTheGameNamesThem()
	{
		assertEquals(Shipyard.Boat.SKIFF, Shipyard.Boat.named("Skiff"));
		assertEquals(Shipyard.Boat.SLOOP, Shipyard.Boat.named(" sloop "));
		assertNull(Shipyard.Boat.named("Will and Anne's boat"));
		assertNull(Shipyard.Boat.named(null));
		assertEquals(Shipyard.Boat.RAFT, Shipyard.bestFor(14));
		assertEquals(Shipyard.Boat.SKIFF, Shipyard.bestFor(15));
		assertEquals(Shipyard.Boat.SLOOP, Shipyard.bestFor(50));
	}

	private static Map<Integer, Long> bank(long... pairs)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			m.put((int) pairs[i], pairs[i + 1]);
		}
		return m;
	}

	private static Shipyard.Step step(Shipyard.Roadmap map, Shipyard.Part part)
	{
		for (Shipyard.Step s : map.getSteps())
		{
			if (s.getPart() == part)
			{
				return s;
			}
		}
		throw new AssertionError(part);
	}

	@Test
	public void theBestTierNeedsBothLevelsAndTheNextSaysWhatItWaitsOn()
	{
		// Sailing 34, Construction 40: teak hull (31/23) is open, mahogany (48/41) is not.
		Map<Integer, Long> held = bank(ItemID.SAILING_BOAT_HULL_PART_TEAK, 10, ItemID.NAILS, 120, ItemID.SWAMP_TAR, 40, ItemID.LEAD_BAR, 5);
		Shipyard.Roadmap map = Shipyard.roadmap(Shipyard.Boat.SKIFF, 34, 40, id -> held.getOrDefault(id, 0L));
		Shipyard.Step hull = step(map, Shipyard.Part.HULL);
		assertEquals("Teak hull", hull.getBest().getDesign().getName());
		assertEquals(Shipyard.Status.MATERIALS, hull.getBest().getStatus());
		assertEquals(Long.valueOf(180), hull.getBest().getMissing().get(ItemID.NAILS));
		assertEquals(1, hull.getBest().getMissing().size());
		assertEquals("Mahogany hull", hull.getNext().getDesign().getName());
		assertEquals(Shipyard.Status.LOCKED, hull.getNext().getStatus());

		// Keel: steel (39) waits on Sailing; iron (22/17) is the best.
		Shipyard.Step keel = step(map, Shipyard.Part.KEEL);
		assertEquals("Iron skiff keel", keel.getBest().getDesign().getName());
		assertEquals(Shipyard.Status.LOCKED, keel.getNext().getStatus());
		// The soonest: the teak mast and canvas sails at 36.
		assertEquals(36, map.nextSailing());
	}

	@Test
	public void sailingMetButConstructionShortIsItsOwnState()
	{
		// Sailing 50, Construction 20: the mahogany hull's Sailing 48 is met, its Construction 41 is not.
		Shipyard.Roadmap map = Shipyard.roadmap(Shipyard.Boat.SKIFF, 50, 20, id -> 0L);
		Shipyard.Step hull = step(map, Shipyard.Part.HULL);
		assertEquals("Oak hull", hull.getBest().getDesign().getName());
		assertEquals("Teak hull", hull.getNext().getDesign().getName());
		assertEquals(Shipyard.Status.CONSTRUCTION, hull.getNext().getStatus());
	}

	@Test
	public void readyWhenEveryMaterialIsHeld()
	{
		Map<Integer, Long> held = bank(ItemID.LOGS, 10, ItemID.ROPE, 6, ItemID.SWAMP_TAR, 10);
		Shipyard.Roadmap map = Shipyard.roadmap(Shipyard.Boat.RAFT, 1, 1, id -> held.getOrDefault(id, 0L));
		assertEquals(Shipyard.Status.READY, step(map, Shipyard.Part.HULL).getBest().getStatus());
		assertEquals(1, map.readyCount());
	}

	@Test
	public void theShoppingListCountsASharedMaterialOnce()
	{
		// A sloop at 50/32: the teak hull and the steel keel take five lead bars each, so the
		// three held leave seven to get, not two.
		Map<Integer, Long> held = bank(ItemID.NAILS_IRON, 1000, ItemID.LEAD_BAR, 3);
		Shipyard.Roadmap map = Shipyard.roadmap(Shipyard.Boat.SLOOP, 50, 32, id -> held.getOrDefault(id, 0L));
		Map<Integer, Long> list = map.shoppingList(id -> held.getOrDefault(id, 0L));
		assertNull("iron nails are held", list.get(ItemID.NAILS_IRON));
		assertEquals("Teak hull", step(map, Shipyard.Part.HULL).getBest().getDesign().getName());
		assertEquals(Long.valueOf(7), list.get(ItemID.LEAD_BAR));
		assertNotNull(list.get(ItemID.SWAMP_TAR));
	}

	@Test
	public void theLastTierHasNoNext()
	{
		Shipyard.Roadmap map = Shipyard.roadmap(Shipyard.Boat.SLOOP, 99, 99, id -> 0L);
		for (Shipyard.Step s : map.getSteps())
		{
			assertNull(s.getNext());
			assertEquals(6, s.getBest().getDesign().getTier());
		}
		assertEquals(0, map.nextSailing());
	}
}
