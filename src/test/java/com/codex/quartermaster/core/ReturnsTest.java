package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ReturnsTest
{
	private static long price(int id)
	{
		switch (id)
		{
			case ItemID.SHARK:
				return 900;
			case ItemID._4DOSEPRAYERRESTORE:
				return 8_000;
			case ItemID._3DOSEPRAYERRESTORE:
				return 6_000;
			case ItemID.DRAGON_BONES:
				return 2_500;
			case ItemID.LAVA_DRAGON_BONES:
				return 9_000;
			default:
				return 0;
		}
	}

	@Test
	public void lootIsWhatCameBackThatDidNotSetOut()
	{
		Quarters q = new Quarters();
		Map<Integer, Long> start = items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 2, ItemID.COINS, 5_000);
		Kit kit = q.capture("Vorkath", start, id -> true);
		// Two sharks eaten, a dose drunk (its potion comes home as a three-dose and a vial), and the drops.
		Map<Integer, Long> end = items(ItemID.SHARK, 8, ItemID._4DOSEPRAYERRESTORE, 1, ItemID._3DOSEPRAYERRESTORE, 1,
			ItemID.VIAL_EMPTY, 1, ItemID.DRAGON_BONES, 4, ItemID.LAVA_DRAGON_BONES, 2, ItemID.COINS, 45_000,
			ItemID.PLATINUM, 3);
		assertEquals(4 * 2_500 + 2 * 9_000 + 40_000 + 3_000, Returns.loot(kit, start, end, ReturnsTest::price));
		// A shark picked up is a supply, not loot.
		assertEquals(0, Returns.loot(kit, start, items(ItemID.SHARK, 11), ReturnsTest::price));
	}

	@Test
	public void eachTripKeepsItsLootAndTheKitAddsThemUp()
	{
		Quarters q = new Quarters();
		Map<Integer, Long> start = items(ItemID.SHARK, 10);
		Kit kit = q.capture("Mine", start, id -> true);
		for (int i = 0; i < 3; i++)
		{
			q.startTrip(start, i * 1_200_000L);
			Trip t = q.endTrip(items(ItemID.SHARK, 4, ItemID.LAVA_DRAGON_BONES, 10), i * 1_200_000L + 1_200_000L, ReturnsTest::price);
			assertEquals(Long.valueOf(90_000), t.getLoot());
			assertEquals(Long.valueOf(90_000 - 5_400), t.net());
		}
		Returns r = Returns.of(q.usage(kit.getId()).getRecent());
		assertTrue(r.isKnown());
		assertEquals(90_000, r.lootPerTrip());
		assertEquals(5_400, r.costPerTrip());
		assertEquals(84_600, r.netPerTrip());
		// Twenty-minute trips: three an hour.
		assertEquals(253_800, r.netPerHour());
	}

	@Test
	public void tripsFromBeforeLootWasKeptAreLeftOut()
	{
		Trip old = new Gson().fromJson("{\"start\":0,\"end\":600000,\"used\":{\"FOOD\":120},\"cost\":5000}", Trip.class);
		assertNull(old.getLoot());
		assertNull(old.net());
		Returns r = Returns.of(Arrays.asList(old));
		assertEquals(0, r.getTrips());
		assertEquals(0, r.netPerHour());
	}
}
