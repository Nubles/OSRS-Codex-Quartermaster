package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.function.IntPredicate;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class LoadoutTest
{
	private static final IntPredicate STACKS = id -> id == ItemID.DRAGON_BOLTS_ENCHANTED_RUBY || id == ItemID.POH_TABLET_TELEPORTTOHOUSE;

	@Test
	public void laysOutWhatTheBankHoldsSlotBySlot()
	{
		Kit kit = kit("FOOD", 200, "PRAYER", 16, Supply.itemKey(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY), 150,
			Supply.itemKey(ItemID.SPADE), 0);
		Loadout l = Loadout.of(kit, items(ItemID.ANGLERFISH, 300, ItemID.SHARK, 20, ItemID._4DOSEPRAYERRESTORE, 40,
			ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 2000, ItemID.SPADE, 1), STACKS);
		// 200 hp is ten anglerfish at 22 hp, the food the bank holds most of; four 4-dose prayer
		// potions; one stack of bolts; the spade.
		int angler = 0;
		int prayer = 0;
		for (Loadout.Slot s : l.getSlots())
		{
			angler += s.getItemId() == ItemID.ANGLERFISH ? 1 : 0;
			prayer += s.getItemId() == ItemID._4DOSEPRAYERRESTORE ? 1 : 0;
			assertTrue(s.isBanked());
		}
		assertEquals(10, angler);
		assertEquals(4, prayer);
		Loadout.Slot bolts = l.getSlots().get(14);
		assertEquals(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, bolts.getItemId());
		assertEquals(150, bolts.getQuantity());
		assertEquals(ItemID.SPADE, l.getSlots().get(15).getItemId());
		assertEquals(16, l.getSlots().size());
		assertEquals(0, l.getOverflow());
	}

	@Test
	public void showsTheUsualItemWhenNoneIsBanked()
	{
		Kit kit = kit("PRAYER", 8);
		Loadout l = Loadout.of(kit, items(), STACKS);
		assertEquals(2, l.getSlots().size());
		assertFalse(l.getSlots().get(0).isBanked());
		assertEquals(1, l.getSlots().get(0).getQuantity());
	}

	@Test
	public void aKitPastTwentyEightSlotsCountsTheRest()
	{
		Kit kit = kit("FOOD", 20_000);
		Loadout l = Loadout.of(kit, items(ItemID.SHARK, 5_000), STACKS);
		assertEquals(Loadout.SLOTS, l.getSlots().size());
		assertEquals(1_000 - Loadout.SLOTS, l.getOverflow());
	}

	@Test
	public void nullBankIsEmpty()
	{
		Loadout l = Loadout.of(kit("FOOD", 40), null, STACKS);
		assertEquals(2, l.getSlots().size());
	}

	@Test
	public void aTripTheBankFallsShortOfIsMadeUpFaded()
	{
		// 200 hp of food with three sharks banked: the three, then seven more sharks faded.
		Loadout l = Loadout.of(kit("FOOD", 200), items(ItemID.SHARK, 3), STACKS);
		assertEquals(10, l.getSlots().size());
		for (int i = 0; i < 10; i++)
		{
			assertEquals(ItemID.SHARK, l.getSlots().get(i).getItemId());
			assertEquals("slot " + i, i < 3, l.getSlots().get(i).isBanked());
		}
	}
}
