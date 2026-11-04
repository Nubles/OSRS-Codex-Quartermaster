package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Partly used forms of a kit's own dosed items. Names and the families are as the wiki and
 * RuneLite's item_variations.json give them: "super strength" files 157, 159, 161 and 2440
 * together, with 157 first, so the table's own base is a 3-dose and only the names tell the doses.
 */
public class VariantsTest
{
	/** RuneLite's item_variations.json, "super strength", in its own order. */
	private static final List<Integer> SUPER_STRENGTH = Arrays.asList(ItemID._3DOSE2STRENGTH, ItemID._2DOSE2STRENGTH,
		ItemID._1DOSE2STRENGTH, ItemID._4DOSE2STRENGTH);
	private static final Map<Integer, String> NAMES = new HashMap<>();

	static
	{
		// Wiki: Super strength(1) 161, (2) 159, (3) 157, (4) 2440.
		NAMES.put(ItemID._4DOSE2STRENGTH, "Super strength(4)");
		NAMES.put(ItemID._3DOSE2STRENGTH, "Super strength(3)");
		NAMES.put(ItemID._2DOSE2STRENGTH, "Super strength(2)");
		NAMES.put(ItemID._1DOSE2STRENGTH, "Super strength(1)");
		NAMES.put(ItemID.RING_OF_DUELING_8, "Ring of dueling(8)");
		NAMES.put(ItemID.RING_OF_DUELING_3, "Ring of dueling(3)");
		NAMES.put(ItemID.MEAT_PIE, "Meat pie");
		NAMES.put(ItemID.HALF_A_MEAT_PIE, "Half a meat pie");
	}

	private static final Variants.Lookup LOOKUP = new Variants.Lookup()
	{
		@Override
		public String name(int itemId)
		{
			return NAMES.get(itemId);
		}

		@Override
		public boolean wearable(int itemId)
		{
			return itemId == ItemID.RING_OF_DUELING_8 || itemId == ItemID.RING_OF_DUELING_3;
		}
	};

	@Before
	@After
	public void forget()
	{
		Variants.clear();
	}

	@Test
	public void readsDosesFromNames()
	{
		assertEquals(4, Variants.doses("Super strength(4)"));
		assertEquals(2, Variants.doses("Waterskin(2)"));
		assertEquals(-1, Variants.doses("Rune pouch (l)"));
		assertEquals(-1, Variants.doses("Meat pie"));
		assertEquals("Super strength", Variants.stem("Super strength(1)"));
	}

	@Test
	public void superStrengthFamilyByNameNotTableOrder()
	{
		Variants.Family f = Variants.family(ItemID._4DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP);
		assertNotNull(f);
		assertEquals(4, f.getDoses());
		assertEquals(Integer.valueOf(3), f.getPartial().get(ItemID._3DOSE2STRENGTH));
		assertEquals(Integer.valueOf(1), f.getPartial().get(ItemID._1DOSE2STRENGTH));
		assertFalse(f.getPartial().containsKey(ItemID._4DOSE2STRENGTH));
	}

	@Test
	public void gearChargesAndPiesAreNotDoses()
	{
		assertNull("a ring's number is charges", Variants.family(ItemID.RING_OF_DUELING_8,
			Arrays.asList(ItemID.RING_OF_DUELING_8, ItemID.RING_OF_DUELING_3), LOOKUP));
		assertNull("half a pie is not named as a dose", Variants.family(ItemID.MEAT_PIE,
			Arrays.asList(ItemID.MEAT_PIE, ItemID.HALF_A_MEAT_PIE), LOOKUP));
		assertNull("a 1-dose has nothing smaller", Variants.family(ItemID._1DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP));
	}

	@Test
	public void unlearntItemsCountExactlyAsBefore()
	{
		Supply s = Supplies.get(Supply.itemKey(ItemID._4DOSE2STRENGTH));
		assertEquals(1, s.count(items(ItemID._4DOSE2STRENGTH, 1, ItemID._2DOSE2STRENGTH, 4)));
	}

	@Test
	public void partlyUsedFormsCountForTheirShareRoundedDown()
	{
		Variants.learn(Variants.family(ItemID._4DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP));
		Supply s = Supplies.get(Supply.itemKey(ItemID._4DOSE2STRENGTH));
		// Two (2)s are one potion; a lone (3) is not yet one.
		assertEquals(2, s.count(items(ItemID._4DOSE2STRENGTH, 1, ItemID._2DOSE2STRENGTH, 2)));
		assertEquals(0, s.count(items(ItemID._3DOSE2STRENGTH, 1)));
		assertEquals(2, s.count(items(ItemID._3DOSE2STRENGTH, 2, ItemID._2DOSE2STRENGTH, 1)));
		assertTrue(s.counts(ItemID._1DOSE2STRENGTH));
		// Still bought and withdrawn as the full potion.
		assertEquals(1, s.getUnits().size());
	}

	@Test
	public void sippedPotionsStillPackTheKit()
	{
		Variants.learn(Variants.family(ItemID._4DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP));
		Kit k = kit(Supply.itemKey(ItemID._4DOSE2STRENGTH), 2);
		Packing p = Packing.check(k, items(ItemID._4DOSE2STRENGTH, 1, ItemID._3DOSE2STRENGTH, 1, ItemID._1DOSE2STRENGTH, 1),
			items(ItemID._4DOSE2STRENGTH, 10));
		assertTrue("4 + 3 + 1 doses is two potions", p.isComplete());
		Packing short1 = Packing.check(k, items(ItemID._4DOSE2STRENGTH, 1, ItemID._2DOSE2STRENGTH, 1),
			items(ItemID._4DOSE2STRENGTH, 10, ItemID._1DOSE2STRENGTH, 50));
		assertEquals("the bank is pointed at the full potion", Long.valueOf(1),
			short1.withdrawals().get(ItemID._4DOSE2STRENGTH));
		assertNull(short1.withdrawals().get(ItemID._1DOSE2STRENGTH));
	}

	@Test
	public void aTripMeasuresDosesDrunk()
	{
		Variants.learn(Variants.family(ItemID._4DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP));
		Supply s = Supplies.get(Supply.itemKey(ItemID._4DOSE2STRENGTH));
		// Out with two full, back with two (2)s: one potion's worth drunk, where counting ids said two.
		long out = s.count(items(ItemID._4DOSE2STRENGTH, 2));
		long back = s.count(items(ItemID._2DOSE2STRENGTH, 2));
		assertEquals(1, out - back);
	}

	@Test
	public void sippedPotionsComingBackAreNotLoot()
	{
		Variants.learn(Variants.family(ItemID._4DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP));
		Kit k = kit(Supply.itemKey(ItemID._4DOSE2STRENGTH), 2);
		long loot = Returns.loot(k, items(ItemID._4DOSE2STRENGTH, 2), items(ItemID._2DOSE2STRENGTH, 2), id -> 5_000);
		assertEquals(0, loot);
	}

	@Test
	public void captureFoldsASippedPotionIntoItsFullOnes()
	{
		Variants.learn(Variants.family(ItemID._4DOSE2STRENGTH, SUPER_STRENGTH, LOOKUP));
		Quarters q = Quarters.read(new com.google.gson.Gson(), null);
		Kit k = q.capture("Mine", items(ItemID._4DOSE2STRENGTH, 2, ItemID._2DOSE2STRENGTH, 2), id -> true, id -> false);
		assertEquals(1, k.getNeeds().size());
		assertEquals(3, k.need(Supply.itemKey(ItemID._4DOSE2STRENGTH)).getPerTrip());
		// Without a full one carried, the sipped potion is kept as its own item, as before.
		Kit lone = q.capture("Lone", items(ItemID._3DOSE2STRENGTH, 2), id -> true, id -> false);
		assertEquals(2, lone.need(Supply.itemKey(ItemID._3DOSE2STRENGTH)).getPerTrip());
	}
}
