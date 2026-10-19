package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class TuningTest
{
	private static Map<String, Long> used(Object... pairs)
	{
		Map<String, Long> m = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2)
		{
			m.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
		}
		return m;
	}

	/** A saved kit of 14 sharks (280 hp), 16 prayer doses and a spade, with trips that used {@code food} hp and {@code prayer} doses each. */
	private static Usage trips(Kit kit, long[] food, long[] prayer)
	{
		Usage u = new Usage();
		for (int i = 0; i < food.length; i++)
		{
			u.record(kit, new Trip(0, 600_000, used("FOOD", food[i], "PRAYER", prayer[i], Supply.itemKey(ItemID.SPADE), 0),
				used("FOOD", 280, "PRAYER", 16, Supply.itemKey(ItemID.SPADE), 1), 0));
		}
		return u;
	}

	private static Kit kit()
	{
		return Fixtures.kit("FOOD", 280, "PRAYER", 16, Supply.itemKey(ItemID.SPADE), 0);
	}

	@Test
	public void packsWhatNineTripsInTenUsed()
	{
		Kit kit = kit();
		// Food: never more than 9 sharks in nine trips of ten; one bad trip ate 12.
		Usage u = trips(kit, new long[]{120, 140, 180, 100, 160, 120, 140, 180, 240, 120},
			new long[]{12, 16, 16, 14, 16, 16, 15, 16, 16, 16});
		List<Tuning.Advice> advice = Tuning.of(kit, u, items(ItemID.SHARK, 14, ItemID._4DOSEPRAYERRESTORE, 4));
		assertEquals(2, advice.size());
		Tuning.Advice food = advice.get(0);
		assertEquals("FOOD", food.getSupply().getKey());
		assertEquals(ItemID.SHARK, food.getItem());
		assertEquals(180, food.getCovers());
		assertEquals(180, food.getSuggested());
		assertEquals(-5, food.itemsChange());
		// Prayer used all 16 doses on seven trips: it ran out, so take a potion more.
		Tuning.Advice prayer = advice.get(1);
		assertTrue(prayer.isMore());
		assertEquals(7, prayer.getRanOut());
		assertEquals(20, prayer.getSuggested());
		assertEquals(1, prayer.itemsChange());
		// Five sharks fewer and a potion more: four slots free. The spade, a tool, gets no advice.
		assertEquals(4, Tuning.slotsFreed(advice));
	}

	@Test
	public void theOneTeleportUsedToLeaveIsNotRunningOut()
	{
		// One house tablet a trip, broken to leave every time: using the only one is the plan, not
		// a shortage. Advising two would flip back to one as soon as trips used one of two.
		String tab = Supply.itemKey(ItemID.POH_TABLET_TELEPORTTOHOUSE);
		Kit kit = Fixtures.kit("FOOD", 280, tab, 1);
		Usage u = new Usage();
		for (int i = 0; i < 9; i++)
		{
			u.record(kit, new Trip(0, 600_000, used("FOOD", 270, tab, 1), used("FOOD", 280, tab, 1), 0));
		}
		assertTrue(Tuning.of(kit, u, items(ItemID.SHARK, 14, ItemID.POH_TABLET_TELEPORTTOHOUSE, 1)).isEmpty());
	}

	@Test
	public void boltsAndSingleItemsFreeNoSlots()
	{
		Supply bolts = Supplies.get("DRAGON_BOLTS");
		Supply tabs = Supplies.get(Supply.itemKey(ItemID.POH_TABLET_TELEPORTTOHOUSE));
		Supply food = Supplies.get("FOOD");
		List<Tuning.Advice> advice = Arrays.asList(
			new Tuning.Advice(bolts, 150, 118, 118, 10, 0, ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 1),
			new Tuning.Advice(tabs, 5, 2, 2, 10, 0, ItemID.POH_TABLET_TELEPORTTOHOUSE, 1),
			new Tuning.Advice(food, 280, 160, 160, 10, 0, ItemID.SHARK, 20));
		// Bolts stack in one slot and tablets stack too: only the six sharks free a slot each.
		assertEquals(6, Tuning.slotsFreed(advice));
	}

	@Test
	public void noAdviceUntilEnoughTripsOrWhenTheKitFits()
	{
		Kit kit = kit();
		assertTrue(Tuning.of(kit, trips(kit, new long[]{100, 100, 100, 100}, new long[]{8, 8, 8, 8}), items()).isEmpty());
		// Using 13 or 14 sharks and 13 or 14 doses a trip: within one shark and one potion of the kit.
		assertTrue(Tuning.of(kit, trips(kit, new long[]{260, 280 - 1, 260, 270, 260}, new long[]{14, 13, 14, 14, 13}), items()).isEmpty());
		Kit preset = new Kit("preset:x", "X", kit.getNeeds(), true);
		assertTrue("presets are not the player's to tune", Tuning.of(preset,
			trips(preset, new long[]{20, 20, 20, 20, 20}, new long[]{1, 1, 1, 1, 1}), items()).isEmpty());
	}

	@Test
	public void tripsFromBeforeTheSupplyJoinedAreSkippedAndOldTripsUseTheKitsAmount()
	{
		Kit kit = kit();
		Usage u = new Usage();
		// Five old trips that never recorded what they set out with: the kit's 16 doses stand in.
		for (int i = 0; i < 5; i++)
		{
			u.record(kit, new Trip(0, 600_000, used("FOOD", 100, "PRAYER", 16), 0));
		}
		List<Tuning.Advice> advice = Tuning.of(kit, u, items());
		Tuning.Advice prayer = advice.get(1);
		assertEquals(5, prayer.getRanOut());
		// And they survive a save as they were.
		Usage back = new Gson().fromJson(new Gson().toJson(u), Usage.class).repair();
		assertEquals(16, back.getRecent().get(0).took("PRAYER", 16));
		assertEquals(5, Tuning.of(kit, back, items()).get(1).getRanOut());
	}

	@Test
	public void applyingTheAdviceChangesOnlyTheSavedKit()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 14, ItemID._4DOSEPRAYERRESTORE, 4), id -> true);
		assertEquals(280, kit.need("FOOD").getPerTrip());
		Usage u = trips(kit, new long[]{120, 120, 120, 120, 120}, new long[]{4, 4, 4, 4, 4});
		List<Tuning.Advice> advice = Tuning.of(kit, u, items(ItemID.SHARK, 14));
		assertTrue(q.tune(kit.getId(), advice));
		assertEquals(120, q.kit(kit.getId()).need("FOOD").getPerTrip());
		assertEquals(4, q.kit(kit.getId()).need("PRAYER").getPerTrip());
		assertFalse("nothing left to change", q.tune(kit.getId(), advice));
		assertFalse(q.tune(Presets.all().get(0).getId(), advice));
	}

	@Test
	public void restocksEverySavedKitFromTheOneBank()
	{
		Quarters q = new Quarters();
		q.capture("Zulrah", items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 2), id -> true);
		q.capture("Vorkath", items(ItemID.SHARK, 5, ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 50), id -> true);
		q.observeBank(items(ItemID.SHARK, 60, ItemID._4DOSEPRAYERRESTORE, 4, ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 500), 1);
		assertEquals(2, q.restockKits().size());
		Plan all = q.planAll(Collections.emptyMap(), Fixtures::price, false);
		// 15 sharks a round of both kits (300 hp): 60 sharks last 4 rounds; prayer 8 doses: 2 rounds.
		assertEquals(4, all.line("FOOD").getTrips());
		assertEquals(2, all.trips());
		// To 10 rounds: 150 sharks less 60 held is 90 to buy.
		assertEquals(90, all.line("FOOD").getBuys().get(0).getQuantity());
		assertEquals(Planner.ALL_KITS, all.getKit().getId());

		// A preset counts once the player chose it.
		q.select(Presets.all().get(0).getId());
		assertEquals(3, q.restockKits().size());
		assertEquals(Arrays.asList("Zulrah", "Vorkath", Presets.all().get(0).getName()),
			Arrays.asList(q.restockKits().get(0).getName(), q.restockKits().get(1).getName(), q.restockKits().get(2).getName()));
	}

	@Test
	public void oldTripsAreNotCalledDryOnceTheKitIsTuned()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 14), id -> true);
		// Five trips from before trips noted what they set out with, eating 7 or 8 sharks of 14.
		for (int i = 0; i < 5; i++)
		{
			q.usageOrNew(kit.getId()).record(kit, new Trip(0, 600_000, used("FOOD", i % 2 == 0 ? 140 : 160), 0));
		}
		Usage u = q.usage(kit.getId());
		List<Tuning.Advice> advice = Tuning.of(kit, u, items(ItemID.SHARK, 14));
		assertEquals(160, advice.get(0).getSuggested());
		assertTrue(q.tune(kit.getId(), advice));
		// Those trips set out with 14 sharks, not the 8 the kit now takes: none of them ran dry.
		assertTrue(Tuning.of(q.kit(kit.getId()), u, items(ItemID.SHARK, 8)).isEmpty());
	}

	@Test
	public void atTheBankWithAnEmptyPackTheAdviceCountsWhatTheBankHolds()
	{
		Kit kit = Fixtures.kit("FOOD", 22 * 14);
		Usage u = new Usage();
		for (int i = 0; i < 5; i++)
		{
			u.record(kit, new Trip(0, 600_000, used("FOOD", 22 * 6), used("FOOD", 22 * 14), 0));
		}
		// Nothing carried, a bank of anglerfish: the advice is in anglerfish, not the supply's own icon.
		List<Tuning.Advice> advice = Tuning.of(kit, u, items(), items(ItemID.ANGLERFISH, 200));
		assertEquals(ItemID.ANGLERFISH, advice.get(0).getItem());
		assertEquals(-8, advice.get(0).itemsChange());
	}
}
