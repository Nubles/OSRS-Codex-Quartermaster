package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class RestockTest
{
	@Test
	public void buysTheFoodYouEatNotTheCheapestHeal()
	{
		// Cake slices and wine are far cheaper a hitpoint, but nobody takes them to a boss.
		Kit kit = kit("FOOD", 200);
		Plan plan = Planner.plan(kit, null, items(ItemID.ANGLERFISH, 30, ItemID.SHARK, 4), Fixtures::price, 5, true);
		Plan.Buy buy = plan.line("FOOD").getBuys().get(0);
		assertEquals(ItemID.ANGLERFISH, buy.getItemId());
		// 1,000 hp less 744 held is 256 hp, which is 12 anglerfish.
		assertEquals(12, buy.getQuantity());
	}

	@Test
	public void withNothingHeldBuysTheSupplysUsualItem()
	{
		Plan plan = Planner.plan(kit("FOOD", 100, "RANGING", 4), null, items(), Fixtures::price, 2, true);
		assertEquals(ItemID.SHARK, plan.line("FOOD").getBuyItem());
		assertEquals(ItemID._4DOSERANGERSPOTION, plan.line("RANGING").getBuyItem());
	}

	@Test
	public void leftoverDosesCountForTheirFourDose()
	{
		// A bank of 3-dose divine ranging potions restocks with 4-dose divine ones.
		Plan plan = Planner.plan(kit("RANGING", 8), null, items(ItemID._3DOSEDIVINERANGE, 10, ItemID._4DOSERANGERSPOTION, 2),
			Fixtures::price, 10, true);
		assertEquals(ItemID._4DOSEDIVINERANGE, plan.line("RANGING").getBuyItem());
		assertEquals((80 - 38 + 3) / 4, plan.line("RANGING").getBuys().get(0).getQuantity());
	}

	@Test
	public void neverBuysOneDosePotions()
	{
		// One-dose prayer potions are cheaper a dose here, but the list buys 4-doses.
		Supply prayer = Supplies.get("PRAYER");
		assertEquals(ItemID._4DOSEPRAYERRESTORE, Planner.cheapest(prayer, Fixtures::price));
		Plan plan = Planner.plan(kit("PRAYER", 16), null, items(ItemID._1DOSEPRAYERRESTORE, 1), Fixtures::price, 5, true);
		assertEquals(ItemID._4DOSEPRAYERRESTORE, plan.line("PRAYER").getBuyItem());
	}

	@Test
	public void anUnpricedFavouriteFallsBackToAPricedItem()
	{
		Fixtures.PRICES.remove(ItemID.SEATURTLE);
		Plan plan = Planner.plan(kit("FOOD", 100), null, items(ItemID.SEATURTLE, 50, ItemID.MANTARAY, 2), Fixtures::price, 20, true);
		assertEquals(ItemID.MANTARAY, plan.line("FOOD").getBuyItem());
	}

	@Test
	public void potionStorageDosesCount()
	{
		Plan plan = Planner.plan(kit("PRAYER", 16), null, items(ItemID._4DOSEPRAYERRESTORE, 4),
			items(ItemID._1DOSEPRAYERRESTORE, 150), Fixtures::price, 10, true);
		Plan.Line line = plan.line("PRAYER");
		assertEquals(166, line.getHave());
		assertEquals(150, line.getStored());
		assertEquals(10, line.getTrips());
		assertTrue(line.getBuys().isEmpty());
		// Stored doses are not listed as items held.
		assertEquals(Collections.singleton(ItemID._4DOSEPRAYERRESTORE), line.getHeld().keySet());
	}

	@Test
	public void shoppingIsBiggestBillFirstAndTotals()
	{
		Plan plan = Planner.plan(kit("FOOD", 100, "PRAYER", 8, "RANGING", 4), null, items(), Fixtures::price, 10, true);
		List<Plan.Buy> list = plan.shopping();
		assertEquals(3, list.size());
		for (int i = 1; i < list.size(); i++)
		{
			assertTrue(list.get(i - 1).getCost() >= list.get(i).getCost());
		}
		long sum = list.stream().mapToLong(Plan.Buy::getCost).sum();
		assertEquals(sum, plan.restockCost());
	}

	@Test
	public void keptSuppliesNeverRunOutOrCost()
	{
		Plan plan = Planner.plan(kit(Supply.itemKey(ItemID.SPADE), 0), null, items(ItemID.SPADE, 1), Fixtures::price, 10, true);
		assertTrue(plan.getLines().get(0).isKept());
		assertEquals(Plan.UNLIMITED, plan.trips());
		assertNull(plan.limiting());
		assertEquals(0, plan.restockCost());
		assertEquals(0, plan.tripCost());
	}

	@Test
	public void aRarelyUsedTeleportStillRunsOut()
	{
		Usage usage = new com.google.gson.Gson().fromJson(
			"{\"trips\":8,\"perTrip\":{\"item:" + ItemID.TELETAB_BARROWS + "\":0.125}}", Usage.class).repair();
		Plan plan = Planner.plan(kit(Supply.itemKey(ItemID.TELETAB_BARROWS), 1), usage, items(ItemID.TELETAB_BARROWS, 3),
			Fixtures::price, 10, true);
		assertEquals(24, plan.trips());
		assertTrue(plan.getLines().get(0).isMeasured());
	}

	@Test
	public void tripsRoundDownAndAHugeBankDoesNotOverflow()
	{
		Plan plan = Planner.plan(kit("FOOD", 1), null, items(ItemID.SHARK, Integer.MAX_VALUE), Fixtures::price, 10, true);
		assertTrue(plan.trips() > 0);
		assertTrue(plan.trips() < Plan.UNLIMITED);
		Plan small = Planner.plan(kit("FOOD", 30), null, items(ItemID.SHARK, 4), Fixtures::price, 10, true);
		assertEquals(2, small.trips());
	}

	@Test
	public void measuredAmountsOnlyOnceTrustedAndOnlyWhenAskedFor()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 20), id -> true);
		q.observeBank(items(ItemID.SHARK, 100), 1);
		q.startTrip(items(ItemID.SHARK, 20), 0);
		q.endTrip(items(ItemID.SHARK, 15), 60_000, Fixtures::price);
		// One trip is not enough to trust: still 400 hp a trip as packed.
		assertEquals(6, q.plan(kit, items(ItemID.SHARK, 20), Fixtures::price, true).trips());
		q.startTrip(items(ItemID.SHARK, 20), 0);
		q.endTrip(items(ItemID.SHARK, 15), 60_000, Fixtures::price);
		assertEquals(24, q.plan(kit, items(ItemID.SHARK, 20), Fixtures::price, true).trips());
		assertEquals(6, q.plan(kit, items(ItemID.SHARK, 20), Fixtures::price, false).trips());
	}
}
