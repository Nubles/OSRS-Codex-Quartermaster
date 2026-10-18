package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class QuartersTest
{
	private static final Map<Integer, Long> VORKATH = items(ItemID.SHARK, 12, ItemID._4DOSEPRAYERRESTORE, 4,
		ItemID._4DOSERANGERSPOTION, 1, ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 150, ItemID.COINS, 50_000, ItemID.SPADE, 1,
		ItemID.POH_TABLET_TELEPORTTOHOUSE, 1);

	@Test
	public void captureLeavesCoinsOutAndKeepsSingleTools()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Vorkath", VORKATH, id -> true, id -> id != ItemID.SPADE);
		assertNull(kit.need(Supply.itemKey(ItemID.COINS)));
		assertEquals(0, kit.need(Supply.itemKey(ItemID.SPADE)).getPerTrip());
		assertEquals(240, kit.need("FOOD").getPerTrip());
		assertEquals(16, kit.need("PRAYER").getPerTrip());
		// A stackable single, such as one teleport tablet, is still used up.
		assertEquals(1, kit.need(Supply.itemKey(ItemID.POH_TABLET_TELEPORTTOHOUSE)).getPerTrip());

		q.observeBank(items(ItemID.SHARK, 100), 1);
		Plan plan = q.plan(kit, VORKATH, Fixtures::price, true);
		assertTrue(plan.line(Supply.itemKey(ItemID.SPADE)).isKept());
		assertFalse("a single spade must not make this a one trip kit",
			plan.limiting().getSupply().getKey().equals(Supply.itemKey(ItemID.SPADE)));
	}

	@Test
	public void captureOfNothingUsefulIsNull()
	{
		Quarters q = new Quarters();
		assertNull(q.capture("Empty", items(ItemID.COINS, 1000), id -> true));
		assertNull(q.capture("Gear", items(ItemID.SPADE, 1), id -> false));
		assertTrue(q.getSavedKits().isEmpty());
	}

	@Test
	public void kitIdsStayUniqueAfterForgetting()
	{
		Quarters q = new Quarters();
		Kit a = q.capture("A", items(ItemID.SHARK, 1), id -> true);
		assertTrue(q.forget(a.getId()));
		Kit b = q.capture("B", items(ItemID.SHARK, 1), id -> true);
		assertFalse(a.getId().equals(b.getId()));
	}

	@Test
	public void forgettingTheChosenKitFallsBackAndDropsItsTrips()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 20), id -> true);
		q.startTrip(items(ItemID.SHARK, 20), 0);
		q.endTrip(items(ItemID.SHARK, 10), 60_000, Fixtures::price);
		assertNotNull(q.usage(kit.getId()));
		assertTrue(q.forget(kit.getId()));
		assertNull(q.usage(kit.getId()));
		assertTrue(q.activeKit().isPreset());
		assertFalse("presets cannot be forgotten", q.forget("preset:vorkath"));
	}

	@Test
	public void droppingKeepsAtLeastOneSupplyAndNeverTouchesPresets()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 20, ItemID.SPADE, 1), id -> true);
		assertTrue(q.drop(kit.getId(), Supply.itemKey(ItemID.SPADE)));
		assertEquals(1, kit.getNeeds().size());
		assertFalse(q.drop(kit.getId(), "FOOD"));
		assertFalse(q.drop("preset:vorkath", "FOOD"));
	}

	@Test
	public void selectAndTargetIgnoreNonsense()
	{
		Quarters q = new Quarters();
		q.select("preset:zulrah");
		q.select("kit:404");
		assertEquals("preset:zulrah", q.activeKit().getId());
		q.setTarget(0);
		assertEquals(1, q.getTarget());
	}

	@Test
	public void aFoodOnlyPresetDoesNotHideYourKit()
	{
		Quarters q = new Quarters();
		Kit mine = q.capture("Vorkath", VORKATH, id -> true);
		q.select("preset:wintertodt");
		assertSame(mine, q.recognise(VORKATH));
		assertEquals(mine.getId(), q.activeKit().getId());
	}

	@Test
	public void yourKitWinsATieWithAPreset()
	{
		Quarters q = new Quarters();
		Kit mine = q.capture("Barrows mine", items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 3, ItemID.TELETAB_BARROWS, 1),
			id -> true);
		q.select("preset:barrows");
		assertSame(mine, q.recognise(items(ItemID.MANTARAY, 10, ItemID._4DOSE2RESTORE, 2, ItemID.TELETAB_BARROWS, 1)));
	}

	@Test
	public void recognitionNeedsMostOfTheKit()
	{
		Quarters q = new Quarters();
		Kit a = q.capture("A", VORKATH, id -> true);
		q.capture("B", items(ItemID.MANTARAY, 10), id -> true);
		// Food alone is a perfect match for B, and only a sliver of A.
		assertNull(q.recognise(items(ItemID.MANTARAY, 2, ItemID._4DOSERANGERSPOTION, 1)));
		assertEquals("kit:2", q.activeKit().getId());
		assertSame(a, q.recognise(VORKATH));
		// Already chosen: nothing to switch.
		assertNull(q.recognise(VORKATH));
	}

	@Test
	public void matchMarksDownSuppliesTheKitDoesNotTake()
	{
		Kit food = Fixtures.kit("FOOD", 100);
		assertEquals(1.0, Quarters.match(food, items(ItemID.SHARK, 3)), 1e-9);
		assertEquals(1 / 3.0, Quarters.match(food, items(ItemID.SHARK, 3, ItemID._4DOSERANGERSPOTION, 1,
			ItemID._4DOSEPRAYERRESTORE, 1)), 1e-9);
		// Super antifire counts toward both antifire pools, so a kit with either does not count it as extra.
		Kit fire = Fixtures.kit("ANTIFIRE", 4);
		assertEquals(1.0, Quarters.match(fire, items(ItemID._4DOSE3ANTIDRAGON, 1)), 1e-9);
		assertEquals(0, Quarters.match(Fixtures.kit(), items(ItemID.SHARK, 1)), 1e-9);
	}

	@Test
	public void aTripIsMeasuredAgainstTheKitItSetOutWith()
	{
		Quarters q = new Quarters();
		Kit a = q.capture("A", items(ItemID.SHARK, 20), id -> true);
		Kit b = q.capture("B", items(ItemID._4DOSEPRAYERRESTORE, 4), id -> true);
		q.select(a.getId());
		q.startTrip(items(ItemID.SHARK, 20), 0);
		q.select(b.getId());
		Trip t = q.endTrip(items(ItemID.SHARK, 5), 60_000, Fixtures::price);
		assertEquals(300L, (long) t.getUsed().get("FOOD"));
		assertEquals(1, q.usage(a.getId()).getTrips());
		assertNull(q.usage(b.getId()));
		assertEquals(15 * 900, t.getCost());
	}

	@Test
	public void unpackedOrUnchangedTripsAreNotMeasured()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 20, ItemID._4DOSEPRAYERRESTORE, 4), id -> true);
		// Set out with neither supply of the kit: a skilling trip, not this kit.
		q.startTrip(items(ItemID.SPADE, 1), 0);
		assertNull(q.endTrip(items(), 60_000, Fixtures::price));
		// Came back with everything: nothing used.
		q.startTrip(items(ItemID.SHARK, 20, ItemID._4DOSEPRAYERRESTORE, 4), 0);
		assertNull(q.endTrip(items(ItemID.SHARK, 20, ItemID._4DOSEPRAYERRESTORE, 4), 60_000, Fixtures::price));
		// No trip under way at all.
		assertNull(q.endTrip(items(), 60_000, Fixtures::price));
		assertFalse(q.onTrip());
		assertNull(q.usage(kit.getId()));
	}

	@Test
	public void pickingUpMoreThanYouTookIsNotNegativeUse()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 2), id -> true);
		q.startTrip(items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 2), 0);
		Trip t = q.endTrip(items(ItemID.SHARK, 14, ItemID._4DOSEPRAYERRESTORE, 1), 60_000, Fixtures::price);
		assertEquals(0L, (long) t.getUsed().get("FOOD"));
		assertEquals(4L, (long) t.getUsed().get("PRAYER"));
		assertEquals(kit.getId(), q.activeKit().getId());
	}

	@Test
	public void savedDataRoundTripsAndOldSavesStillLoad()
	{
		Gson gson = new Gson();
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 20), id -> true);
		q.observeBank(items(ItemID.SHARK, 200), 1234);
		q.observeStorage(items(ItemID._1DOSEPRAYERRESTORE, 40));
		q.setTarget(25);
		q.startTrip(items(ItemID.SHARK, 20), 0);
		q.endTrip(items(ItemID.SHARK, 10), 60_000, Fixtures::price);
		q.startTrip(items(ItemID.SHARK, 20), 100_000);

		String json = gson.toJson(q);
		assertFalse("a trip under way is not saved", json.contains("tripStart"));
		Quarters back = gson.fromJson(json, Quarters.class).repair();
		assertEquals(25, back.getTarget());
		assertEquals(1234, back.getBankSeen());
		assertEquals(kit.getId(), back.activeKit().getId());
		assertEquals(1, back.usage(kit.getId()).getTrips());
		assertEquals(40L, (long) back.getStored().get(ItemID._1DOSEPRAYERRESTORE));
		assertFalse(back.onTrip());

		// A save from the first release: no potion storage, a trip under way, an unknown supply and a broken kit.
		String old = "{\"bank\":{\"385\":100},\"bankSeen\":5,\"kits\":[{\"id\":\"kit:1\",\"name\":\"Old\",\"needs\":["
			+ "{\"supply\":\"FOOD\",\"perTrip\":200},{\"supply\":\"GONE\",\"perTrip\":3},null]},{\"name\":\"no id\"}],"
			+ "\"active\":\"kit:1\",\"target\":0,\"usage\":{\"kit:1\":{\"trips\":3,\"perTrip\":{\"FOOD\":180.0},\"recent\":[null]},"
			+ "\"kit:9\":null},\"saved\":1,\"tripStart\":{\"385\":10},\"tripStartAt\":7,\"tripKit\":\"kit:1\"}";
		Quarters repaired = gson.fromJson(old, Quarters.class).repair();
		assertEquals(1, repaired.getSavedKits().size());
		assertEquals(1, repaired.activeKit().getNeeds().size());
		assertEquals(Quarters.DEFAULT_TARGET, repaired.getTarget());
		assertTrue(repaired.getStored().isEmpty());
		assertFalse(repaired.onTrip());
		assertTrue(repaired.usage("kit:1").getRecent().isEmpty());
		assertEquals(180.0, repaired.usage("kit:1").perTrip("FOOD"), 1e-9);
		// 100 sharks are 2,000 hp at 180 a trip.
		assertEquals(11, repaired.plan(repaired.activeKit(), items(), Fixtures::price, true).trips());
		// New kits continue the numbering.
		assertEquals("kit:2", repaired.capture("New", items(ItemID.SHARK, 1), id -> true).getId());
	}
}
