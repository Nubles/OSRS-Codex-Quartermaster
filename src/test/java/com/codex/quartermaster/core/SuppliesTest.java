package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class SuppliesTest
{
	@Test
	public void lowerDosesBelongToTheirOwnFourDose()
	{
		Supply prayer = Supplies.get("PRAYER");
		assertEquals(ItemID._4DOSEPRAYERRESTORE, prayer.fullForm(ItemID._1DOSEPRAYERRESTORE));
		assertEquals(ItemID._4DOSEPRAYERRESTORE, prayer.fullForm(ItemID._3DOSEPRAYERRESTORE));
		assertEquals(ItemID._4DOSE2RESTORE, prayer.fullForm(ItemID._2DOSE2RESTORE));
		assertEquals(ItemID.SANFEW_SALVE_4_DOSE, prayer.fullForm(ItemID.SANFEW_SALVE_1_DOSE));
		assertTrue(prayer.isFullForm(ItemID._4DOSE2RESTORE));
		assertFalse(prayer.isFullForm(ItemID._3DOSE2RESTORE));

		Supply ranging = Supplies.get("RANGING");
		assertEquals(ItemID._4DOSEDIVINERANGE, ranging.fullForm(ItemID._2DOSEDIVINERANGE));
		assertEquals(ItemID._4DOSEBASTION, ranging.fullForm(ItemID._1DOSEBASTION));
	}

	@Test
	public void everyDoseGroupIsFourThreeTwoOne()
	{
		for (Supply s : Supplies.groups())
		{
			if (!"doses".equals(s.getUnit()))
			{
				continue;
			}
			for (Map.Entry<Integer, Integer> e : s.getUnits().entrySet())
			{
				int full = s.fullForm(e.getKey());
				assertEquals(s.getKey() + " " + e.getKey(), Integer.valueOf(4), s.getUnits().get(full));
			}
		}
	}

	@Test
	public void cakeSlicesAreBoughtAsCakeAndOtherFoodAsItself()
	{
		Supply food = Supplies.get("FOOD");
		assertEquals(ItemID.CAKE, food.fullForm(ItemID.CAKE_SLICE));
		assertEquals(ItemID.CAKE, food.fullForm(ItemID.PARTIAL_CAKE));
		assertEquals(ItemID.SHARK, food.fullForm(ItemID.SHARK));
		assertEquals(ItemID.SEATURTLE, food.fullForm(ItemID.SEATURTLE));
	}

	@Test
	public void eachGroupsIconCountsTowardIt()
	{
		Set<String> keys = new HashSet<>();
		for (Supply s : Supplies.groups())
		{
			assertTrue(s.getKey(), s.getUnits().containsKey(s.getIcon()));
			assertTrue(keys.add(s.getKey()));
			assertNotNull(s.getName());
			assertNotNull(s.getUnit());
		}
	}

	@Test
	public void keysForSingleItemsAndPools()
	{
		assertEquals("FOOD", Supplies.keyFor(ItemID.SHARK));
		// Super antifire is filed under the stronger pool first, but plain antifire still counts it.
		assertEquals("SUPER_ANTIFIRE", Supplies.keyFor(ItemID._4DOSE3ANTIDRAGON));
		assertTrue(Supplies.get("ANTIFIRE").getUnits().containsKey(ItemID._4DOSE3ANTIDRAGON));
		assertEquals("item:" + ItemID.SPADE, Supplies.keyFor(ItemID.SPADE));
		assertTrue(Supplies.get("item:" + ItemID.SPADE).isItem());
		assertNull(Supplies.get("item:spade"));
		assertNull(Supplies.get("NOPE"));
		assertNull(Supplies.get(null));
		assertTrue(Supplies.never(ItemID.COINS));
		assertFalse(Supplies.pooled(ItemID.SPADE));
	}

	@Test
	public void countsUnitsAcrossVariants()
	{
		Supply food = Supplies.get("FOOD");
		assertEquals(20 * 3 + 22 * 2 + 4, food.count(items(ItemID.SHARK, 3, ItemID.MANTARAY, 2, ItemID.CAKE_SLICE, 1, ItemID.SPADE, 5)));
		assertEquals(0, food.count(items(ItemID.SHARK, -4)));
	}
}
