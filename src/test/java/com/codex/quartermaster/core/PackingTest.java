package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class PackingTest
{
	private static final Kit VORKATH = kit("FOOD", 200, "PRAYER", 16, "RANGING", 4, Supply.itemKey(ItemID.SPADE), 0);

	@Test
	public void fullyPackedKit()
	{
		Packing p = Packing.check(VORKATH, items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 4,
			ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1), items());
		assertTrue(p.isComplete());
		assertTrue(p.missing().isEmpty());
		assertTrue(p.withdrawals().isEmpty());
	}

	@Test
	public void forgottenToolIsPackedAsOne()
	{
		Map<Integer, Long> bank = items(ItemID.SPADE, 1);
		Packing p = Packing.check(VORKATH, items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 4,
			ItemID._4DOSERANGERSPOTION, 1), bank);
		assertFalse(p.isComplete());
		assertTrue(p.isUnderway());
		assertEquals(1, p.forgotten().size());
		Packing.Item spade = p.forgotten().get(0);
		assertTrue(spade.isTool());
		assertEquals(1, spade.getTarget());
		assertEquals(Long.valueOf(1), p.withdrawals().get(ItemID.SPADE));
	}

	@Test
	public void withdrawsTheFormAlreadyCarriedFirst()
	{
		// Carrying sharks: point at sharks, not the bigger pile of lobsters.
		Map<Integer, Long> bank = items(ItemID.SHARK, 50, ItemID.LOBSTER, 500, ItemID._4DOSEPRAYERRESTORE, 40);
		Packing p = Packing.check(VORKATH, items(ItemID.SHARK, 4, ItemID._4DOSEPRAYERRESTORE, 4,
			ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1), bank);
		// 200 hp packs, 80 carried: 120 more is six sharks.
		assertEquals(Long.valueOf(6), p.withdrawals().get(ItemID.SHARK));
		assertNull(p.withdrawals().get(ItemID.LOBSTER));
	}

	@Test
	public void prefersFullDosesAndSpillsOverWhenShort()
	{
		Map<Integer, Long> bank = items(ItemID._1DOSEPRAYERRESTORE, 30, ItemID._4DOSE2RESTORE, 2, ItemID._3DOSEPRAYERRESTORE, 1);
		Packing p = Packing.check(kit("PRAYER", 16), items(), bank);
		Map<Integer, Long> w = p.withdrawals();
		Iterator<Integer> order = w.keySet().iterator();
		// The two 4-dose restores come first; the biggest pile of lesser doses makes up the rest.
		assertEquals(Integer.valueOf(ItemID._4DOSE2RESTORE), order.next());
		assertEquals(Long.valueOf(2), w.get(ItemID._4DOSE2RESTORE));
		long doses = 0;
		for (Map.Entry<Integer, Long> e : w.entrySet())
		{
			doses += e.getValue() * Supplies.get("PRAYER").getUnits().get(e.getKey());
		}
		assertTrue(doses >= 16);
		assertTrue("never more than the bank holds", w.getOrDefault(ItemID._3DOSEPRAYERRESTORE, 0L) <= 1);
	}

	@Test
	public void notUnderwayWhenBankingSomethingElse()
	{
		Packing p = Packing.check(VORKATH, items(ItemID.SHARK, 2), items());
		assertFalse(p.isUnderway());
		assertEquals(3, p.forgotten().size());
	}

	@Test
	public void missingIsMostUrgentFirst()
	{
		Packing p = Packing.check(VORKATH, items(ItemID.SHARK, 9, ItemID._4DOSEPRAYERRESTORE, 1, ItemID.SPADE, 1), items());
		assertEquals("RANGING", p.missing().get(0).getSupply().getKey());
		assertEquals("PRAYER", p.missing().get(1).getSupply().getKey());
		assertEquals("FOOD", p.missing().get(2).getSupply().getKey());
	}

	@Test
	public void reminderNamesWhatWasForgottenOnce()
	{
		Reminder r = new Reminder();
		String text = r.check("Vorkath", Arrays.asList("spade", "anti-venom"), 0);
		assertEquals("you left the bank without spade and anti-venom for Vorkath.", text);
		assertNull("not again on a quick dash back", r.check("Vorkath", Arrays.asList("spade", "anti-venom"), 60_000));
		assertEquals(text, r.check("Vorkath", Arrays.asList("spade", "anti-venom"), Reminder.AGAIN_MS + 1));
		assertNull(r.check("Vorkath", Collections.emptyList(), Reminder.AGAIN_MS + 2));
		assertEquals("you left the bank without a, b, c and 2 more for X.",
			Reminder.text("X", Arrays.asList("a", "b", "c", "d", "e")));
	}

	@Test
	public void typicalDurationIsTheMedian()
	{
		Usage u = new Usage();
		Kit k = kit("FOOD", 100);
		assertEquals(0, u.typicalDuration());
		u.record(k, new Trip(0, 600_000, items2("FOOD", 100), 0));
		assertEquals(0, u.typicalDuration());
		u.record(k, new Trip(0, 1_200_000, items2("FOOD", 100), 0));
		u.record(k, new Trip(0, 9_000_000, items2("FOOD", 100), 0));
		assertEquals(1_200_000, u.typicalDuration());
	}

	private static Map<String, Long> items2(String key, long v)
	{
		return Collections.singletonMap(key, v);
	}

	@Test
	public void kitBookHasAStableShape()
	{
		Quarters q = new Quarters();
		q.capture("My Vorkath", items(ItemID.SHARK, 10, ItemID.SPADE, 1), id -> true, id -> id != ItemID.SPADE);
		q.observeBank(items(ItemID.SHARK, 90), 1);
		String json = new Gson().toJson(KitBook.of(q, items(), id -> id == ItemID.SPADE ? "Spade" : "?", true));
		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		assertEquals(1, root.get("version").getAsInt());
		assertEquals("kit:1", root.get("active").getAsString());
		assertEquals(Quarters.DEFAULT_TARGET, root.get("target").getAsInt());
		JsonArray kits = root.getAsJsonArray("kits");
		assertEquals(1 + Presets.all().size(), kits.size());
		JsonObject mine = kits.get(0).getAsJsonObject();
		assertEquals("My Vorkath", mine.get("name").getAsString());
		assertFalse(mine.get("preset").getAsBoolean());
		// 90 sharks banked, 200 hp a trip: 9 trips; the spade comes back.
		assertEquals(9, mine.get("tripsLeft").getAsInt());
		assertEquals("FOOD", mine.get("limiting").getAsString());
		JsonObject food = mine.getAsJsonArray("needs").get(0).getAsJsonObject();
		assertEquals("FOOD", food.get("supply").getAsString());
		assertEquals("hp", food.get("unit").getAsString());
		assertEquals(200, food.get("perTrip").getAsInt());
		assertFalse(food.has("measured"));
		assertEquals(20, food.getAsJsonObject("items").get(String.valueOf(ItemID.SHARK)).getAsInt());
		JsonObject spade = mine.getAsJsonArray("needs").get(1).getAsJsonObject();
		assertEquals("Spade", spade.get("name").getAsString());
		assertEquals(1, spade.getAsJsonObject("items").get(String.valueOf(ItemID.SPADE)).getAsInt());
		assertTrue(kits.get(1).getAsJsonObject().get("preset").getAsBoolean());
	}

	@Test
	public void kitBookNamesNoActiveKitUntilThePlayerChoosesOne()
	{
		// A fresh install shows its first preset, but nobody chose it: readers such as Bank Steward
		// would otherwise hold that preset's supplies back for every player.
		Quarters q = new Quarters();
		assertEquals(Presets.all().get(0).getId(), q.activeKit().getId());
		JsonObject fresh = new JsonParser().parse(new Gson().toJson(KitBook.of(q, items(), id -> "?", true))).getAsJsonObject();
		assertFalse(fresh.has("active"));
		assertNull(KitBook.of(q, items(), id -> "?", true).getActive());

		// Picking the very preset that was shown makes it the player's choice.
		q.select(Presets.all().get(0).getId());
		assertEquals(Presets.all().get(0).getId(), KitBook.of(q, items(), id -> "?", true).getActive());

		// Forgetting the chosen saved kit leaves no choice again.
		Kit mine = q.capture("Mine", items(ItemID.SHARK, 10), id -> true);
		assertEquals(mine.getId(), KitBook.of(q, items(), id -> "?", true).getActive());
		q.forget(mine.getId());
		assertNull(KitBook.of(q, items(), id -> "?", true).getActive());
	}

	@Test
	public void presetSupplyNeverUsedIsNotAskedFor()
	{
		// A player in a serpentine helm never takes the preset's anti-venom.
		Kit preset = new Kit("preset:z", "Zulrah", Arrays.asList(new Need("FOOD", 100), new Need("ANTIVENOM", 4)), true);
		Usage u = new Usage();
		for (int i = 0; i < 2; i++)
		{
			Map<String, Long> used = new java.util.HashMap<>();
			used.put("FOOD", 80L);
			used.put("ANTIVENOM", 0L);
			u.record(preset, new Trip(0, 600_000, used, 0));
		}
		Packing p = Packing.check(preset, items(ItemID.SHARK, 5), items(), Collections.emptyMap(), u);
		assertEquals(1, p.getItems().size());
		assertTrue(p.isComplete());
		// A saved kit is held to everything the player packed, measured or not.
		Kit saved = new Kit("kit:1", "Mine", preset.getNeeds(), false);
		assertEquals(2, Packing.check(saved, items(ItemID.SHARK, 5), items(), Collections.emptyMap(), u).getItems().size());
	}

	@Test
	public void potionStorageIsNamedWhenTheBankHasNone()
	{
		Packing p = Packing.check(kit("PRAYER", 16), items(), items(), items(ItemID._1DOSEPRAYERRESTORE, 40), null);
		Packing.Item prayer = p.getItems().get(0);
		assertTrue(prayer.getWithdraw().isEmpty());
		assertEquals(40, prayer.getInStorage());
	}

	@Test
	public void aLeftoverDoseDoesNotSteerTowardOtherLeftovers()
	{
		// Back from a trip with a 1-dose prayer potion: take 4-dose ones, not the bank's stray 1-doses.
		Map<Integer, Long> bank = items(ItemID._1DOSEPRAYERRESTORE, 3, ItemID._4DOSEPRAYERRESTORE, 40);
		Packing p = Packing.check(kit("PRAYER", 16), items(ItemID._1DOSEPRAYERRESTORE, 1), bank);
		Map<Integer, Long> w = p.withdrawals();
		assertEquals(Integer.valueOf(ItemID._4DOSEPRAYERRESTORE), w.keySet().iterator().next());
		assertEquals(Long.valueOf(4), w.get(ItemID._4DOSEPRAYERRESTORE));
		assertNull(w.get(ItemID._1DOSEPRAYERRESTORE));
	}

	@Test
	public void aDepositOnlyVisitIsNotPacking()
	{
		// Back from a trip with leftovers, banking only the loot: nothing for the kit came out, so
		// the food eaten on the trip is not "forgotten".
		Map<Integer, Long> back = items(ItemID._4DOSEPRAYERRESTORE, 1, ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1,
			ItemID.DRAGON_BONES, 2);
		Map<Integer, Long> leaving = items(ItemID._4DOSEPRAYERRESTORE, 1, ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1);
		Packing p = Packing.check(VORKATH, leaving, items(ItemID.SHARK, 50));
		assertTrue(p.isUnderway());
		assertEquals(1, p.forgotten().size());
		assertFalse(Packing.withdrewFor(VORKATH, back, leaving));
		// Taking a prayer potion out is packing, and then the missing food is worth a word.
		assertTrue(Packing.withdrewFor(VORKATH, back, items(ItemID._4DOSEPRAYERRESTORE, 2, ItemID._4DOSERANGERSPOTION, 1,
			ItemID.SPADE, 1)));
	}

	@Test
	public void bankIsMarkedOnlyWhilePacking()
	{
		Map<Integer, Long> bank = items(ItemID.SHARK, 50, ItemID._4DOSEPRAYERRESTORE, 40, ItemID._4DOSERANGERSPOTION, 9,
			ItemID.SPADE, 1);
		// An inventory full of loot: nothing carried for the kit and no room, so no marks.
		Packing loot = Packing.check(VORKATH, items(), bank);
		assertFalse(loot.isUnderway());
		long need = Packing.slotsNeeded(loot.withdrawals(), Collections.emptySet(), id -> false);
		// 10 sharks, 4 prayer potions, 1 ranging potion and a spade.
		assertEquals(16, need);
		assertFalse(loot.worthMarking(0, need));
		// Deposited: room for the lot, so the marks show.
		assertTrue(loot.worthMarking(28, need));
		// Part-packed with little room left still shows what remains.
		Packing half = Packing.check(VORKATH, items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 4), bank);
		assertTrue(half.worthMarking(1, 2));
		// Stackables take one slot, or none when the stack is already carried.
		Map<Integer, Long> runes = items(ItemID.DEATHRUNE, 500, ItemID.SHARK, 3);
		assertEquals(4, Packing.slotsNeeded(runes, Collections.emptySet(), id -> id == ItemID.DEATHRUNE));
		assertEquals(3, Packing.slotsNeeded(runes, Collections.singleton(ItemID.DEATHRUNE), id -> id == ItemID.DEATHRUNE));
		// Nothing to take is never marked.
		Packing done = Packing.check(VORKATH, items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 4,
			ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1), bank);
		assertFalse(done.worthMarking(28, 0));
	}
}
