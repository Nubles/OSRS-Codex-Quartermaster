package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class PlannerTest
{
	private static final Map<Integer, Long> PRICES = new HashMap<>();

	static
	{
		PRICES.put(ItemID.SHARK, 900L);
		PRICES.put(ItemID.MANTARAY, 1_400L);
		PRICES.put(ItemID._4DOSEPRAYERRESTORE, 8_000L);
		PRICES.put(ItemID._4DOSE2RESTORE, 10_000L);
	}

	private static long price(int id)
	{
		return PRICES.getOrDefault(id, 0L);
	}

	private static Map<Integer, Long> items(long... pairs)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			m.put((int) pairs[i], pairs[i + 1]);
		}
		return m;
	}

	private static Kit kit(Object... pairs)
	{
		Need[] needs = new Need[pairs.length / 2];
		for (int i = 0; i < needs.length; i++)
		{
			needs[i] = new Need((String) pairs[i * 2], (Integer) pairs[i * 2 + 1]);
		}
		return new Kit("kit:test", "Test", Arrays.asList(needs), false);
	}

	@Test
	public void poolsVariantsAndFindsWhatRunsOutFirst()
	{
		Kit kit = kit("FOOD", 200, "PRAYER", 16);
		// 100 sharks and 20 mantas heal 2,440; 10 prayer(4), 5 restore(2) and a prayer(3) make 53 doses.
		Map<Integer, Long> held = items(ItemID.SHARK, 100, ItemID.MANTARAY, 20,
			ItemID._4DOSEPRAYERRESTORE, 10, ItemID._2DOSE2RESTORE, 5, ItemID._3DOSEPRAYERRESTORE, 1);
		Plan plan = Planner.plan(kit, null, held, PlannerTest::price, 10, true);
		assertEquals(2440, plan.getLines().get(0).getHave());
		assertEquals(12, plan.getLines().get(0).getTrips());
		assertEquals(53, plan.getLines().get(1).getHave());
		assertEquals(3, plan.trips());
		assertEquals("PRAYER", plan.limiting().getSupply().getKey());
	}

	@Test
	public void restocksWithTheCheapestItemPerUnit()
	{
		Kit kit = kit("FOOD", 200, "PRAYER", 16);
		Plan plan = Planner.plan(kit, null, items(), PlannerTest::price, 5, true);
		// 1,000 hp: sharks are 45 gp a hitpoint and mantas 64, so 50 sharks.
		Plan.Buy food = plan.getLines().get(0).getBuys().get(0);
		assertEquals(ItemID.SHARK, food.getItemId());
		assertEquals(50, food.getQuantity());
		// 80 doses: prayer potions are cheaper a dose than restores, so 20 of them.
		Plan.Buy prayer = plan.getLines().get(1).getBuys().get(0);
		assertEquals(ItemID._4DOSEPRAYERRESTORE, prayer.getItemId());
		assertEquals(20, prayer.getQuantity());
		assertEquals(50 * 900 + 20 * 8_000, plan.restockCost());
		assertEquals(200 * 45 + 16 * 2_000, plan.tripCost());
	}

	@Test
	public void suppliesWithoutAPriceAreListedAsUnbuyable()
	{
		Kit kit = kit(Supply.itemKey(ItemID.TELEPORTSCROLL_ZULANDRA), 1);
		Plan plan = Planner.plan(kit, null, items(), PlannerTest::price, 3, true);
		assertEquals(0, plan.trips());
		assertTrue(plan.shopping().isEmpty());
		assertEquals(1, plan.unbuyable().size());
	}

	@Test
	public void learnsFromTripsAndStopsCountingWhatComesBack()
	{
		Quarters q = new Quarters();
		Map<Integer, Long> start = items(ItemID.SHARK, 20, ItemID._4DOSEPRAYERRESTORE, 4, ItemID.SPADE, 1);
		Kit kit = q.capture("Mine", start, id -> true);
		assertEquals(3, kit.getNeeds().size());
		q.observeBank(items(ItemID.SHARK, 200, ItemID._4DOSEPRAYERRESTORE, 40), 1);

		for (int i = 0; i < 3; i++)
		{
			q.startTrip(start, 0);
			Trip t = q.endTrip(items(ItemID.SHARK, 8, ItemID._2DOSEPRAYERRESTORE, 1, ItemID.SPADE, 1), 60_000, PlannerTest::price);
			assertEquals(240L, (long) t.getUsed().get("FOOD"));
			assertEquals(14L, (long) t.getUsed().get("PRAYER"));
		}
		Plan plan = q.plan(kit, start, PlannerTest::price, true);
		assertTrue(plan.line(Supply.itemKey(ItemID.SPADE)).isKept());
		assertTrue(plan.line("FOOD").isMeasured());
		// 4,400 hp at 240 a trip, 176 doses at 14 a trip.
		assertEquals(12, plan.trips());
	}

	@Test
	public void shortTripsAndDeathsAreNotMeasured()
	{
		Quarters q = new Quarters();
		Map<Integer, Long> start = items(ItemID.SHARK, 20);
		Kit kit = q.capture("Mine", start, id -> true);
		q.startTrip(start, 0);
		assertNull(q.endTrip(items(ItemID.SHARK, 10), 5_000, PlannerTest::price));
		q.startTrip(start, 0);
		q.voidTrip();
		assertNull(q.endTrip(items(), 90_000, PlannerTest::price));
		assertNull(q.usage(kit.getId()));
	}

	@Test
	public void recognisesThePackedKit()
	{
		Quarters q = new Quarters();
		Kit a = q.capture("A", items(ItemID.SHARK, 20, ItemID.TELETAB_BARROWS, 1), id -> true);
		Kit b = q.capture("B", items(ItemID._4DOSE2RESTORE, 8, ItemID.TELEPORTSCROLL_ZULANDRA, 1), id -> true);
		assertEquals(b.getId(), q.activeKit().getId());
		assertEquals(a, q.recognise(items(ItemID.MANTARAY, 12, ItemID.TELETAB_BARROWS, 2)));
		assertNull(q.recognise(items(ItemID.MANTARAY, 12)));
	}
}
