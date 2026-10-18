package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class UsageTest
{
	private static Trip trip(long start, long food, long cost)
	{
		Map<String, Long> used = new LinkedHashMap<>();
		used.put("FOOD", food);
		return new Trip(start, start + 60_000, used, cost);
	}

	@Test
	public void averagesPlainlyThenMovesByAnEighth()
	{
		Usage u = new Usage();
		Kit kit = kit("FOOD", 100);
		assertEquals(-1, u.perTrip("FOOD"), 1e-9);
		assertNull(u.last());
		assertEquals(-1, u.averageCost());
		u.record(kit, trip(0, 100, 10));
		assertFalse(u.isTrusted());
		u.record(kit, trip(1, 200, 20));
		assertTrue(u.isTrusted());
		assertEquals(150, u.perTrip("FOOD"), 1e-9);
		for (int i = 0; i < 6; i++)
		{
			u.record(kit, trip(2 + i, 150, 0));
		}
		assertEquals(150, u.perTrip("FOOD"), 1e-9);
		// Past eight trips one odd trip only moves the average by an eighth of the difference.
		u.record(kit, trip(9, 950, 0));
		assertEquals(250, u.perTrip("FOOD"), 1e-9);
	}

	@Test
	public void keepsTheLastThirtyTrips()
	{
		Usage u = new Usage();
		Kit kit = kit("FOOD", 100);
		for (int i = 0; i < 40; i++)
		{
			u.record(kit, trip(i, 100, i));
		}
		assertEquals(40, u.getTrips());
		assertEquals(30, u.getRecent().size());
		assertEquals(39, u.last().getStart());
		assertEquals((10 + 39) / 2, u.averageCost());
	}

	@Test
	public void aSupplyAddedLaterStartsFromItsFirstTrip()
	{
		Usage u = new Usage();
		u.record(kit("FOOD", 100), trip(0, 100, 0));
		u.record(kit("FOOD", 100), trip(1, 100, 0));
		Map<String, Long> used = new LinkedHashMap<>();
		used.put("FOOD", 100L);
		used.put("PRAYER", 12L);
		u.record(kit("FOOD", 100, "PRAYER", 8), new Trip(2, 70_000, used, 0));
		assertEquals(12, u.perTrip("PRAYER"), 1e-9);
		// A supply a trip does not mention was not used on it; this is prayer's second trip, so half.
		u.record(kit("FOOD", 100, "PRAYER", 8), new Trip(3, 70_000, Collections.emptyMap(), 0));
		assertEquals(6, u.perTrip("PRAYER"), 1e-9);
	}

	@Test
	public void oldSavesWithoutCountsAverageOverAllTrips()
	{
		Usage u = new com.google.gson.Gson().fromJson("{\"trips\":3,\"perTrip\":{\"FOOD\":120.0}}", Usage.class).repair();
		u.record(kit("FOOD", 100), trip(0, 200, 0));
		// The fourth trip of FOOD moves the average by a quarter.
		assertEquals(140, u.perTrip("FOOD"), 1e-9);
	}

	@Test
	public void tripDurationNeverNegative()
	{
		assertEquals(0, new Trip(10, 5, Collections.emptyMap(), 0).duration());
	}
}
