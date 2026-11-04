package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class FromTargetTest
{
	private static final Prices PRICES = Fixtures::price;
	private static final Set<Integer> STACKS = new HashSet<>(Arrays.asList(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY,
		ItemID.DRAGON_BOLTS_ENCHANTED_DIAMOND, ItemID.POH_TABLET_TELEPORTTOHOUSE, ItemID.TELETAB_BARROWS));
	private static final IntPredicate STACKABLE = STACKS::contains;

	private static FromTarget.Row only(Kit kit, Map<Integer, Long> held)
	{
		return FromTarget.of(kit, held, PRICES, STACKABLE).getRows().get(0);
	}

	@Test
	public void foodThatTakesNoMoreSlotsThanTheBestIsReady()
	{
		// 200 hp: ten sharks (20 each) or ten manta rays (22 each), the same ten slots.
		FromTarget.Row row = only(Fixtures.kit("FOOD", 200), Fixtures.items(ItemID.SHARK, 400));
		assertEquals(FromTarget.Status.READY, row.getStatus());
		assertEquals(Collections.singletonMap(ItemID.SHARK, 10L), row.getTake());
		assertEquals(10, row.getSlots());
		assertEquals(0, row.getExtraSlots());
	}

	@Test
	public void weakerFoodIsADowngradeWithTheSlotsItCostsAndTheBestToBuy()
	{
		// Lobsters heal 12: 17 of them for 200 hp, against 10 of the best.
		FromTarget.Row row = only(Fixtures.kit("FOOD", 200), Fixtures.items(ItemID.LOBSTER, 500));
		assertEquals(FromTarget.Status.DOWNGRADE, row.getStatus());
		assertEquals(17, row.getSlots());
		assertEquals(10, row.getBestSlots());
		assertEquals(7, row.getExtraSlots());
		// Manta ray and anglerfish both heal 22; manta is cheaper.
		assertEquals(ItemID.MANTARAY, row.getBuyItem());
		assertEquals(10, row.getBuyQuantity());
		assertEquals(14_000, row.getBuyCost());
	}

	@Test
	public void theBestFoodHeldIsPackedFirstAndTheRestMakesUpTheTrip()
	{
		FromTarget.Row row = only(Fixtures.kit("FOOD", 200), Fixtures.items(ItemID.MANTARAY, 4, ItemID.SHARK, 50, ItemID.LOBSTER, 500));
		Map<Integer, Long> want = new LinkedHashMap<>();
		want.put(ItemID.MANTARAY, 4L);
		want.put(ItemID.SHARK, 6L);
		assertEquals(want, row.getTake());
		assertEquals(FromTarget.Status.READY, row.getStatus());
	}

	@Test
	public void aShortfallSaysWhatToBuyInTheItemAlreadyUsed()
	{
		FromTarget.Row row = only(Fixtures.kit("FOOD", 200), Fixtures.items(ItemID.SHARK, 4));
		assertEquals(FromTarget.Status.BUY, row.getStatus());
		assertEquals(ItemID.SHARK, row.getBuyItem());
		// 4 sharks give 80 hp; 120 more is 6 sharks.
		assertEquals(6, row.getBuyQuantity());
		assertEquals(5_400, row.getBuyCost());
		assertEquals(10, row.getSlots());
	}

	@Test
	public void lowDosePotionsAreADowngradeAndFullOnesAreNot()
	{
		Kit prayer = Fixtures.kit("PRAYER", 12);
		assertEquals(FromTarget.Status.READY, only(prayer, Fixtures.items(ItemID._4DOSEPRAYERRESTORE, 20)).getStatus());
		FromTarget.Row ones = only(prayer, Fixtures.items(ItemID._1DOSEPRAYERRESTORE, 40));
		assertEquals(FromTarget.Status.DOWNGRADE, ones.getStatus());
		assertEquals(12, ones.getSlots());
		assertEquals(3, ones.getBestSlots());
	}

	@Test
	public void stacksTakeOneSlotAndToolsComeBack()
	{
		Kit kit = Fixtures.kit("DRAGON_BOLTS", 150, Supply.itemKey(ItemID.SPADE), 0);
		FromTarget.Build b = FromTarget.of(kit, Fixtures.items(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 1000, ItemID.SPADE, 1), PRICES, STACKABLE);
		assertEquals(2, b.slots());
		assertEquals(FromTarget.Status.READY, b.getRows().get(0).getStatus());
		assertTrue(b.getRows().get(1).isKept());
		assertEquals(FromTarget.Status.READY, b.getRows().get(1).getStatus());
		// No spade: one to get, but it is not sold here, so nothing has a price.
		FromTarget.Build none = FromTarget.of(kit, Fixtures.items(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 1000), PRICES, STACKABLE);
		assertEquals(FromTarget.Status.BUY, none.getRows().get(1).getStatus());
	}

	@Test
	public void theInventoryIsLaidOutInKitOrderMarkingWhatIsBought()
	{
		Kit kit = Fixtures.kit("FOOD", 60, Supply.itemKey(ItemID.TELETAB_BARROWS), 1);
		FromTarget.Build b = FromTarget.of(kit, Fixtures.items(ItemID.SHARK, 2), PRICES, STACKABLE);
		assertEquals(4, b.getInventory().size());
		assertFalse(b.getInventory().get(0).isBought());
		assertFalse(b.getInventory().get(1).isBought());
		assertTrue(b.getInventory().get(2).isBought());
		assertEquals(ItemID.TELETAB_BARROWS, b.getInventory().get(3).getItemId());
		assertTrue(b.getInventory().get(3).isBought());
		assertEquals(900 + 300, b.shortfallCost());
		assertEquals(2, b.count(FromTarget.Status.BUY));
	}

	@Test
	public void aBigTripOverflowsTheInventoryWithoutDrawingEverySlot()
	{
		FromTarget.Build b = FromTarget.of(Fixtures.kit("FOOD", 20_000), Fixtures.items(ItemID.SHARK, 5_000), PRICES, STACKABLE);
		assertEquals(Loadout.SLOTS, b.getInventory().size());
		assertEquals(1_000 - Loadout.SLOTS, b.getOverflow());
	}

	@Test
	public void theBuildSavesAsAKitItemByItem()
	{
		Kit kit = Fixtures.kit("FOOD", 200, "PRAYER", 12, Supply.itemKey(ItemID.SPADE), 0, Supply.itemKey(ItemID.TELETAB_BARROWS), 1);
		FromTarget.Build b = FromTarget.of(kit, Fixtures.items(ItemID.MANTARAY, 4, ItemID.SHARK, 50, ItemID._4DOSEPRAYERRESTORE, 1,
			ItemID.SPADE, 1), PRICES, STACKABLE);
		Kit saved = b.toKit("kit:7", "Vorkath from bank");
		assertEquals("kit:7", saved.getId());
		assertFalse(saved.isPreset());
		Map<String, Integer> needs = new LinkedHashMap<>();
		for (Need n : saved.getNeeds())
		{
			needs.put(n.getSupply(), n.getPerTrip());
		}
		Map<String, Integer> want = new LinkedHashMap<>();
		want.put(Supply.itemKey(ItemID.MANTARAY), 4);
		want.put(Supply.itemKey(ItemID.SHARK), 6);
		// One 4-dose held, two more to buy.
		want.put(Supply.itemKey(ItemID._4DOSEPRAYERRESTORE), 3);
		want.put(Supply.itemKey(ItemID.SPADE), 0);
		want.put(Supply.itemKey(ItemID.TELETAB_BARROWS), 1);
		assertEquals(want, needs);
		// Every need is one Quartermaster can count.
		for (Need n : saved.getNeeds())
		{
			assertTrue(Supplies.get(n.getSupply()) != null);
		}
	}

	@Test
	public void everyPresetBuildsFromAnEmptyBankAsAShoppingList()
	{
		for (Kit preset : Presets.all())
		{
			FromTarget.Build b = FromTarget.of(preset, Collections.emptyMap(), PRICES, STACKABLE);
			assertEquals(preset.getName(), preset.getNeeds().size(), b.getRows().size());
			assertEquals(preset.getName(), b.getRows().size(), b.count(FromTarget.Status.BUY));
		}
	}

	@Test
	public void theTargetIsThePickThenGearRoadmapsThenThePresetInUseThenTheFirst()
	{
		Quarters q = new Quarters();
		java.util.List<Kit> kits = q.allKits();
		Kit vorkath = q.kit("preset:vorkath");
		Kit zulrah = q.kit("preset:zulrah");
		Kit nex = q.kit("preset:nex");
		assertEquals(nex, FromTarget.pick(kits, "preset:nex", "Zulrah", vorkath));
		assertEquals(zulrah, FromTarget.pick(kits, null, "Zulrah", nex));
		// Gear Roadmap names monsters with their forms; the kit's name leads.
		assertEquals(zulrah, FromTarget.pick(kits, null, "Zulrah (tanzanite)", nex));
		assertEquals(nex, FromTarget.pick(kits, null, "Kraken", nex));
		assertEquals(vorkath, FromTarget.pick(kits, null, null, null));
		// A saved kit is never a target, even when picked.
		Kit mine = Fixtures.kit("FOOD", 100);
		java.util.List<Kit> withMine = new java.util.ArrayList<>(kits);
		withMine.add(0, mine);
		assertEquals(vorkath, FromTarget.pick(withMine, mine.getId(), null, mine));
		assertFalse(FromTarget.matches(vorkath, "Vorkathian"));
	}

	@Test
	public void gearRoadmapsTargetIsReadFromItsOwnGroupAndKey()
	{
		// Codex Gear Roadmap saves its target as plain config, group "codexgearroadmap", key "target".
		assertEquals("codexgearroadmap", FromTarget.ROADMAP_GROUP);
		assertEquals("target", FromTarget.ROADMAP_TARGET);
	}

	@Test
	public void adoptingABuildSavesAndChoosesANewKit()
	{
		Quarters q = new Quarters();
		FromTarget.Build b = FromTarget.of(q.kit("preset:barrows"), Fixtures.items(ItemID.SHARK, 100, ItemID._4DOSEPRAYERRESTORE, 10), PRICES, STACKABLE);
		Kit kit = q.adopt(b, "Barrows from bank");
		assertEquals("Barrows from bank", kit.getName());
		assertEquals(kit, q.activeKit());
		assertEquals(1, q.getSavedKits().size());
		Kit again = q.adopt(b, "Second");
		assertFalse(kit.getId().equals(again.getId()));
	}
}
