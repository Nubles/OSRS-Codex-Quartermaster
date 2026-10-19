package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * The saved {@code quarters} survives damage: whatever the profile holds, reading it never throws,
 * every kit still plans to sensible figures, and damage to one part does not cost the rest.
 */
public class QuartersDamagedTest
{
	private static final Gson GSON = new Gson();
	private static final long NOW = 1_760_000_000_000L;
	private static final Map<Integer, Long> PACKED = items(ItemID.SHARK, 12, ItemID._4DOSEPRAYERRESTORE, 4,
		ItemID._4DOSERANGERSPOTION, 1, ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 150);
	private static final Map<Integer, Long> BACK = items(ItemID.SHARK, 3, ItemID._4DOSEPRAYERRESTORE, 1,
		ItemID._1DOSEPRAYERRESTORE, 1, ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 60);

	/** A well used account: a saved kit with trips behind it, a bank, potion storage and a target. */
	static Quarters good()
	{
		Quarters q = new Quarters();
		q.observeBank(items(ItemID.SHARK, 400, ItemID._4DOSEPRAYERRESTORE, 60, ItemID.COINS, 5_000_000,
			ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 2_000), NOW - 3_600_000);
		q.observeStorage(items(ItemID._1DOSEPRAYERRESTORE, 40));
		q.capture("Vorkath", PACKED, id -> true);
		q.capture("Zulrah", items(ItemID.SHARK, 10, ItemID.ANTIVENOM4, 1), id -> true);
		q.select("kit:1");
		for (int i = 0; i < 3; i++)
		{
			q.startTrip(PACKED, NOW + i * 600_000L);
			assertNotNull(q.endTrip(BACK, NOW + i * 600_000L + 300_000, Fixtures::price));
		}
		q.setTarget(25);
		q.setIron(true);
		return q;
	}

	@Test
	public void everyDamagedFormReadsAndPlans()
	{
		String good = GSON.toJson(good());
		int tried = 0;
		for (String damaged : Damaged.of(good))
		{
			try
			{
				use(Quarters.read(GSON, damaged));
			}
			catch (RuntimeException | AssertionError | StackOverflowError e)
			{
				throw new AssertionError("quarters " + Damaged.show(damaged) + ": " + e, e);
			}
			tried++;
		}
		assertTrue(tried > 500);
	}

	/** Everything the plugin does with an account's quarters, each figure checked for sense. */
	static void use(Quarters q)
	{
		assertNotNull(q);
		assertTrue(q.getTarget() >= 1 && q.getTarget() <= Quarters.TARGET_MAX);
		assertTrue(q.getBankSeen() >= 0);
		Set<String> ids = new HashSet<>();
		for (Kit kit : q.allKits())
		{
			assertTrue("kit ids are unique: " + kit.getId(), ids.add(kit.getId()));
			checked(q.plan(kit, PACKED, Fixtures::price, true));
			checked(q.plan(kit, PACKED, Fixtures::price, false));
			Usage u = q.usage(kit.getId());
			if (u != null)
			{
				assertTrue(u.getTrips() >= 0);
				assertTrue(u.typicalDuration() >= 0);
				assertTrue(u.averageCost() >= -1);
				Returns.of(u.getRecent());
			}
		}
		checked(q.planAll(PACKED, Fixtures::price, true));
		Kit active = q.activeKit();
		Packing packing = Packing.check(active, PACKED, q.getBank(), q.getStored(), q.usage(active.getId()));
		BankView.of(packing);
		Tuning.of(active, q.usage(active.getId()), PACKED, q.getBank());
		KitBook book = KitBook.of(q, PACKED, id -> "Item", true);
		for (KitBook.Entry e : book.getKits())
		{
			assertTrue(e.getTripsLeft() >= -1);
		}
		GSON.toJson(book);

		// A trip and a new kit still work, and the new kit's id is its own.
		Kit added = q.capture("New", PACKED, id -> true);
		assertNotNull(added);
		assertEquals(1, q.getSavedKits().stream().filter(k -> k.getId().equals(added.getId())).count());
		q.startTrip(PACKED, NOW);
		q.endTrip(BACK, NOW + 300_000, Fixtures::price);
		Quarters again = Quarters.read(GSON, GSON.toJson(q));
		assertEquals(q.getSavedKits().size(), again.getSavedKits().size());
	}

	private static void checked(Plan plan)
	{
		assertTrue(plan.trips() >= 0);
		assertTrue(plan.restockCost() >= 0);
		assertTrue(plan.tripCost() >= 0);
		for (Plan.Line line : plan.getLines())
		{
			assertTrue("have " + line.getHave(), line.getHave() >= 0);
			assertTrue(line.getPerTrip() >= 0);
			assertTrue(line.getTrips() >= 0);
			assertTrue(line.getShort() >= 0);
			assertTrue(line.getTripCost() >= 0);
			for (Plan.Buy b : line.getBuys())
			{
				assertTrue(b.getQuantity() > 0);
				assertTrue(b.getCost() >= 0);
			}
		}
	}

	@Test
	public void oneDamagedFieldCostsOnlyThatField()
	{
		String good = GSON.toJson(good());
		// The target saved as words: the kits, trips and bank all stay.
		Quarters q = Quarters.read(GSON, good.replace("\"target\":25", "\"target\":\"twenty\""));
		assertEquals(2, q.getSavedKits().size());
		assertEquals(3, q.usage("kit:1").getTrips());
		assertEquals(400, (long) q.getBank().get(ItemID.SHARK));
		assertEquals(Quarters.DEFAULT_TARGET, q.getTarget());
		assertTrue(q.isIron());

		// A bank count too big for any number loses that item, not the bank or the kits.
		q = Quarters.read(GSON, good.replace("\"" + ItemID.SHARK + "\":400", "\"" + ItemID.SHARK + "\":1e40"));
		assertEquals(2, q.getSavedKits().size());
		assertEquals(60, (long) q.getBank().get(ItemID._4DOSEPRAYERRESTORE));
		assertFalse(q.getBank().containsKey(ItemID.SHARK));

		// One kit that cannot be read leaves the other.
		q = Quarters.read(GSON, good.replace("\"name\":\"Zulrah\"", "\"name\":{\"x\":1}"));
		assertEquals(1, q.getSavedKits().size());
		assertEquals("Vorkath", q.getSavedKits().get(0).getName());
		assertEquals("kit:1", q.chosenKitId());
	}

	@Test
	public void numbersOutOfRangeAreBroughtBack()
	{
		String good = GSON.toJson(good());
		Quarters q = Quarters.read(GSON, good.replace("\"target\":25", "\"target\":2147483647"));
		assertEquals(Quarters.TARGET_MAX, q.getTarget());
		q.setTarget(Integer.MAX_VALUE);
		assertEquals(Quarters.TARGET_MAX, q.getTarget());

		// A bank count below zero is no stock, and one near the top of a long does not wrap round.
		q = Quarters.read(GSON, good.replace("\"" + ItemID.SHARK + "\":400", "\"" + ItemID.SHARK + "\":-400"));
		assertFalse(q.getBank().containsKey(ItemID.SHARK));
		q = Quarters.read(GSON, good.replace("\"" + ItemID.SHARK + "\":400", "\"" + ItemID.SHARK + "\":9223372036854775807"));
		Plan plan = q.plan(q.kit("kit:1"), PACKED, Fixtures::price, false);
		assertTrue(plan.line("FOOD").getHave() > 0);
		assertTrue(plan.line("FOOD").getTrips() > 1_000_000);

		// A kit amount below zero or past the cap is held to the range the panel allows.
		q = Quarters.read(GSON, good.replace("\"perTrip\":240", "\"perTrip\":-240"));
		assertEquals(0, q.kit("kit:1").need("FOOD").getPerTrip());
		q = Quarters.read(GSON, good.replace("\"perTrip\":240", "\"perTrip\":2147483647"));
		assertEquals(Quarters.AMOUNT_MAX, q.kit("kit:1").need("FOOD").getPerTrip());
	}

	@Test
	public void aKitCountBehindItsKitsStillMakesNewIds()
	{
		String good = GSON.toJson(good());
		for (String saved : new String[]{"\"saved\":0", "\"saved\":-7"})
		{
			Quarters q = Quarters.read(GSON, good.replace("\"saved\":2", saved));
			Kit added = q.capture("Third", PACKED, id -> true);
			assertEquals("kit:3", added.getId());
			assertEquals(3, q.getSavedKits().size());
			// Forgetting the new kit leaves the two older ones.
			assertTrue(q.forget(added.getId()));
			assertEquals(2, q.getSavedKits().size());
		}
		// Two kits saved under one id: the second gets an id of its own.
		Quarters q = Quarters.read(GSON, good.replace("\"id\":\"kit:2\"", "\"id\":\"kit:1\""));
		assertEquals(2, q.getSavedKits().size());
		assertFalse(q.getSavedKits().get(0).getId().equals(q.getSavedKits().get(1).getId()));
	}

	@Test
	public void aBankSeenInTheFutureDoesNotBlockNewerBanks()
	{
		String good = GSON.toJson(good());
		Quarters q = Quarters.read(GSON, good.replace("\"bankSeen\":" + (NOW - 3_600_000), "\"bankSeen\":9223372036854775807"));
		SharedBank shared = SharedBank.read(GSON, "{\"version\":1,\"savedAt\":" + NOW + ",\"items\":{\"385\":7},\"truncated\":false}", null);
		assertTrue("a bank time past now is not believed", q.adoptBank(shared, "Codex Bank Steward", NOW + 120_000));
		assertEquals(7, (long) q.getBank().get(ItemID.SHARK));

		// And a shared bank dated in the future is not taken over the one seen here.
		q = good();
		shared = SharedBank.read(GSON, "{\"version\":1,\"savedAt\":9000000000000,\"items\":{\"385\":7},\"truncated\":false}", null);
		assertFalse(q.adoptBank(shared, "Codex Bank Steward", NOW));
		assertEquals(400, (long) q.getBank().get(ItemID.SHARK));
	}

	@Test
	public void aHugeTripHistoryIsCutToWhatIsKept()
	{
		Quarters q = good();
		String trip = GSON.toJson(q.usage("kit:1").getRecent().get(0));
		StringBuilder many = new StringBuilder("[");
		for (int i = 0; i < 12_000; i++)
		{
			many.append(i == 0 ? "" : ",").append(trip);
		}
		String json = GSON.toJson(q);
		int at = json.indexOf("\"recent\":[") + "\"recent\":".length();
		int end = json.indexOf("]}", at) + 1;
		long t0 = System.nanoTime();
		Quarters big = Quarters.read(GSON, json.substring(0, at) + many.append("]") + json.substring(end));
		use(big);
		long ms = (System.nanoTime() - t0) / 1_000_000;
		assertTrue(big.usage("kit:1").getRecent().size() <= 30);
		assertTrue("read and planned in " + ms + " ms", ms < 2_000);
	}

	@Test
	public void nothingSavedIsAFreshStart()
	{
		for (String nothing : new String[]{null, "", "null", "   "})
		{
			Quarters q = Quarters.read(GSON, nothing);
			assertTrue(q.getSavedKits().isEmpty());
			assertFalse(q.hasBank());
			use(q);
		}
	}

	@Test
	public void bankTagsValuesOfAnyKindAreSafe()
	{
		// What Bank Tags remembers, read from core's "banktags" group, is only ever compared.
		for (String value : Damaged.anyValue())
		{
			assertFalse(BankView.shouldClose(value, value));
			assertEquals(value == null ? "" : value, BankView.tabAfterClose(value));
		}
		assertTrue(BankView.shouldClose(null, BankView.TAG));
	}

	@Test
	public void aLargeAccountPlansInAFewMilliseconds()
	{
		// A maxed account: 1,500 kinds of item, a dozen kits of its own with full trip histories.
		Quarters q = good();
		Map<Integer, Long> bank = new java.util.HashMap<>(q.getBank());
		for (int id = 1; bank.size() < 1_500; id += 3)
		{
			bank.putIfAbsent(id, (long) id * 11);
		}
		bank.put(ItemID.COINS, (long) Integer.MAX_VALUE);
		q.observeBank(bank, NOW);
		for (int k = 0; k < 10; k++)
		{
			Kit kit = q.capture("Kit " + k, PACKED, id -> true);
			for (int i = 0; i < 30; i++)
			{
				q.startTrip(PACKED, NOW + i * 600_000L);
				q.endTrip(BACK, NOW + i * 600_000L + 300_000, Fixtures::price);
			}
			assertEquals(30, q.usage(kit.getId()).getRecent().size());
		}
		Runnable publish = () ->
		{
			// What the plugin works out on the client thread each time it publishes.
			for (Kit kit : q.allKits())
			{
				q.plan(kit, PACKED, Fixtures::price, true);
				Usage u = q.usage(kit.getId());
				if (u != null)
				{
					Returns.of(u.getRecent());
				}
			}
			Kit active = q.activeKit();
			BankView.of(Packing.check(active, PACKED, q.getBank(), q.getStored(), q.usage(active.getId())));
			q.planAll(PACKED, Fixtures::price, true);
			Tuning.of(active, q.usage(active.getId()), PACKED, q.getBank());
		};
		Runnable save = () ->
		{
			GSON.toJson(q);
			GSON.toJson(KitBook.of(q, PACKED, id -> "Item", true));
		};
		double publishMs = time(publish);
		double saveMs = time(save);
		System.out.printf("Quartermaster, 1,500-item bank, %d kits: publish %.2f ms, save %.2f ms%n",
			q.allKits().size(), publishMs, saveMs);
		// Measured at about 1 ms and 3 ms; the bounds leave room for a slow, busy build machine.
		assertTrue("publish took " + publishMs + " ms", publishMs < 25);
		assertTrue("save took " + saveMs + " ms", saveMs < 50);
	}

	/** The mean time of a run once warmed up, in milliseconds. */
	static double time(Runnable run)
	{
		for (int i = 0; i < 20; i++)
		{
			run.run();
		}
		int runs = 50;
		long t0 = System.nanoTime();
		for (int i = 0; i < runs; i++)
		{
			run.run();
		}
		return (System.nanoTime() - t0) / 1e6 / runs;
	}
}
