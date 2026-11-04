package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class InventoryCheckTest
{
	private static final Kit VORKATH = kit("FOOD", 200, "PRAYER", 16, "DRAGON_BOLTS", 100, Supply.itemKey(ItemID.SPADE), 0);
	private static final IntPredicate STACKS = id -> id == ItemID.DRAGON_BOLTS_ENCHANTED_RUBY || id == ItemID.COINS
		|| id == ItemID.DRAGON_BOLTS_ENCHANTED_DIAMOND;
	/** Tradeable and not worn: what a kit could hold. The looting bag is neither. */
	private static final IntPredicate SUPPLY = id -> id != ItemID.LOOTING_BAG && id != ItemID.ABYSSAL_WHIP;

	/** An inventory from item and quantity pairs, one slot a pair, the rest empty. */
	private static int[][] inventory(int... pairs)
	{
		int[] ids = new int[28];
		int[] qty = new int[28];
		java.util.Arrays.fill(ids, -1);
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			ids[i / 2] = pairs[i];
			qty[i / 2] = pairs[i + 1];
		}
		return new int[][]{ids, qty};
	}

	private static Map<Integer, Long> carried(int[][] inv)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i < inv[0].length; i++)
		{
			if (inv[0][i] >= 0)
			{
				m.merge(inv[0][i], (long) inv[1][i], Long::sum);
			}
		}
		return m;
	}

	private static InventoryCheck check(int[][] inv, Map<Integer, Long> bank)
	{
		Packing p = Packing.check(VORKATH, carried(inv), bank);
		return InventoryCheck.of(p, inv[0], inv[1], SUPPLY, STACKS);
	}

	@Test
	public void lootIsMarkedToDepositButGearAndCoinsAreNot()
	{
		int[][] inv = inventory(ItemID.SHARK, 1, ItemID.DRAGON_BONES, 1, ItemID.DRAGONHIDE_BLUE, 1, ItemID.COINS, 5000,
			ItemID.LOOTING_BAG, 1, ItemID.ABYSSAL_WHIP, 1, ItemID.SPADE, 1);
		InventoryCheck c = check(inv, items());
		assertEquals(InventoryCheck.Kind.STRAY, c.at(1).getKind());
		assertEquals(InventoryCheck.Kind.STRAY, c.at(2).getKind());
		assertNull("coins are never a stray", c.at(3));
		assertNull("a looting bag is no supply", c.at(4));
		assertNull("a switch weapon is no supply", c.at(5));
		assertNull("the spade is in the kit", c.at(6));
		assertNull(c.at(0));
		assertEquals(2, c.slotsFreed());
	}

	@Test
	public void overPackedFoodMarksTheLastSlotsPastTheKit()
	{
		// 12 sharks are 240 hp for a 200 hp kit: the last two go back, not the first.
		int[] pairs = new int[24];
		for (int i = 0; i < 12; i++)
		{
			pairs[i * 2] = ItemID.SHARK;
			pairs[i * 2 + 1] = 1;
		}
		InventoryCheck c = check(inventory(pairs), items());
		assertEquals(2, c.getMarks().size());
		assertEquals(InventoryCheck.Kind.SURPLUS, c.at(11).getKind());
		assertEquals(InventoryCheck.Kind.SURPLUS, c.at(10).getKind());
		assertNull(c.at(9));
		assertEquals(Long.valueOf(2), c.toDeposit(InventoryCheck.Kind.SURPLUS).get(ItemID.SHARK));
	}

	@Test
	public void overByLessThanAnyItemMarksNothing()
	{
		// 9 sharks, a monkfish and a karambwan: 214 hp for 200. Each item is worth more than the 14
		// over, so putting any one back would leave the kit short: nothing is marked.
		int[] pairs = new int[22];
		for (int i = 0; i < 9; i++)
		{
			pairs[i * 2] = ItemID.SHARK;
			pairs[i * 2 + 1] = 1;
		}
		pairs[18] = ItemID.MONKFISH;
		pairs[19] = 1;
		pairs[20] = ItemID.TBWT_COOKED_KARAMBWAN;
		pairs[21] = 1;
		assertTrue(check(inventory(pairs), items()).isEmpty());
	}

	@Test
	public void partOfAnAmmoStackIsMarkedWithHowMany()
	{
		InventoryCheck c = check(inventory(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 160), items());
		InventoryCheck.Mark m = c.at(0);
		assertEquals(InventoryCheck.Kind.SURPLUS, m.getKind());
		assertEquals(60, m.getAmount());
		assertFalse("the stack stays, smaller", m.isWhole());
		assertEquals(0, c.slotsFreed());
	}

	@Test
	public void aShortStackSaysHowManyMoreFromTheBank()
	{
		InventoryCheck c = check(inventory(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 40),
			items(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 1000));
		InventoryCheck.Mark m = c.at(0);
		assertEquals(InventoryCheck.Kind.SHORT, m.getKind());
		assertEquals(60, m.getAmount());
	}

	@Test
	public void unstackableShortSuppliesAreLeftToTheBankMarks()
	{
		// Four sharks of ten: the bank marks the six to take; the four carried are fine as they are.
		InventoryCheck c = check(inventory(ItemID.SHARK, 1, ItemID.SHARK, 1, ItemID.SHARK, 1, ItemID.SHARK, 1),
			items(ItemID.SHARK, 100));
		assertTrue(c.isEmpty());
	}

	@Test
	public void aSecondSpadeIsOneTooMany()
	{
		InventoryCheck c = check(inventory(ItemID.SPADE, 1, ItemID.SPADE, 1), items());
		assertNull(c.at(0));
		assertEquals(InventoryCheck.Kind.SURPLUS, c.at(1).getKind());
	}

	@Test
	public void aSupplyTripsShowIsNeverUsedIsNotCalledLoot()
	{
		// A preset's anti-venom the packing check leaves out stays the kit's, not a stray.
		Kit k = new Kit("preset:t", "T", java.util.Arrays.asList(new Need("FOOD", 200), new Need("ANTIVENOM", 4)), true);
		int[][] inv = inventory(ItemID.ANTIVENOM4, 1);
		Usage usage = null;
		Packing p = Packing.check(k, carried(inv), items(), java.util.Collections.emptyMap(), usage);
		assertNull(InventoryCheck.of(p, inv[0], inv[1], SUPPLY, STACKS).at(0));
	}

	@Test
	public void nothingToCheckWithoutAPacking()
	{
		assertSame(InventoryCheck.NONE, InventoryCheck.of(null, new int[28], new int[28], SUPPLY, STACKS));
	}
}
