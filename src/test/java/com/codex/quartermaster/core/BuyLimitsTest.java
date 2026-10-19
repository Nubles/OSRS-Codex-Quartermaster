package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class BuyLimitsTest
{
	private static final Map<Integer, Integer> LIMITS = new HashMap<>();

	static
	{
		LIMITS.put(ItemID._4DOSEPRAYERRESTORE, 2000);
		LIMITS.put(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 11000);
		LIMITS.put(ItemID.SHARK, 10000);
	}

	private static int limit(int id)
	{
		return LIMITS.getOrDefault(id, 0);
	}

	@Test
	public void aListWithinEveryLimitIsBoughtAtOnce()
	{
		BuyLimits l = BuyLimits.of(Arrays.asList(new Plan.Buy(ItemID.SHARK, 10000, 900),
			new Plan.Buy(ItemID._4DOSEPRAYERRESTORE, 400, 8000)), BuyLimitsTest::limit);
		assertTrue(l.isEmpty());
		assertEquals(1, l.rounds());
		assertEquals(0, l.waitMs());
		assertTrue(BuyLimits.of(Collections.emptyList(), BuyLimitsTest::limit).isEmpty());
	}

	@Test
	public void theSlowestItemSetsTheWaitAndComesFirst()
	{
		// 37,100 bolts at 11,000 a round is 4 rounds; 4,500 prayer potions at 2,000 is 3.
		BuyLimits l = BuyLimits.of(Arrays.asList(new Plan.Buy(ItemID._4DOSEPRAYERRESTORE, 4500, 8000),
			new Plan.Buy(ItemID.SHARK, 600, 900),
			new Plan.Buy(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 37100, 1100)), BuyLimitsTest::limit);
		assertEquals(2, l.getOver().size());
		assertEquals(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, l.getOver().get(0).getItemId());
		assertEquals(4, l.getOver().get(0).getRounds());
		assertEquals(3, l.getOver().get(1).getRounds());
		assertEquals(4, l.rounds());
		// Limits run side by side, so four rounds are three waits of four hours.
		assertEquals(12 * 3_600_000L, l.waitMs());
	}

	@Test
	public void anItemWithNoKnownLimitNeverHoldsTheListUp()
	{
		BuyLimits l = BuyLimits.of(Collections.singletonList(new Plan.Buy(ItemID.ANGLERFISH, 1_000_000, 1700)), BuyLimitsTest::limit);
		assertTrue(l.isEmpty());
		assertEquals(1, BuyLimits.rounds(5, 0));
		assertEquals(1, BuyLimits.rounds(2000, 2000));
		assertEquals(2, BuyLimits.rounds(2001, 2000));
	}
}
