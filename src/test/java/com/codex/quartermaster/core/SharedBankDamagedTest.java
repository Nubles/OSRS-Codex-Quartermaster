package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

/**
 * What Codex Bank Steward and Codex Gear Roadmap save is read safely however it is damaged: no
 * exception, no count below one or past a bank slot's most, and a damaged bank is never taken
 * over a good one.
 */
public class SharedBankDamagedTest
{
	private static final Gson GSON = new Gson();
	private static final long NOW = 1_760_000_100_000L;

	@Test
	public void everyDamagedStewardBankReadsSafely() throws Exception
	{
		String good = SharedBankContractTest.fixture("bank-steward-bankV1.json");
		String roadmap = SharedBankContractTest.fixture("gear-roadmap-bank.json");
		for (String damaged : Damaged.of(good))
		{
			check(damaged, null);
			check(damaged, roadmap);
		}
	}

	@Test
	public void everyDamagedRoadmapBankReadsSafely() throws Exception
	{
		String good = SharedBankContractTest.fixture("gear-roadmap-bank.json");
		for (String damaged : Damaged.of(good))
		{
			check(null, damaged);
			check("{\"version\":1}", damaged);
		}
	}

	private static void check(String steward, String roadmap)
	{
		try
		{
			SharedBank bank = SharedBank.read(GSON, steward, roadmap);
			if (bank == null)
			{
				return;
			}
			assertTrue(bank.getSavedAt() >= 0);
			assertTrue(bank.getItems().size() <= 2001);
			for (Map.Entry<Integer, Long> e : bank.getItems().entrySet())
			{
				assertTrue(e.getKey() > 0);
				// Gear Roadmap's coins may count platinum tokens, so only they can pass a slot's most.
				assertTrue(e.getValue() > 0 && (e.getKey() == 995 || e.getValue() <= Integer.MAX_VALUE));
			}
			// Taking it on a fresh install, then planning a preset from it.
			Quarters q = new Quarters();
			q.adoptBank(bank, "Codex Bank Steward", NOW);
			QuartersDamagedTest.use(q);
		}
		catch (RuntimeException | AssertionError | StackOverflowError e)
		{
			throw new AssertionError("steward " + Damaged.show(steward) + " roadmap " + Damaged.show(roadmap) + ": " + e, e);
		}
	}

	@Test
	public void aStewardBankOfTwelveHundredItemsIsQuick()
	{
		StringBuilder items = new StringBuilder();
		for (int id = 1; id <= 1_200; id++)
		{
			items.append(id == 1 ? "" : ",").append('"').append(id * 7).append("\":").append(id * 13);
		}
		String json = "{\"version\":1,\"savedAt\":" + (NOW - 1) + ",\"items\":{" + items + "},\"truncated\":false}";
		long t0 = System.nanoTime();
		SharedBank bank = SharedBank.read(GSON, json, null);
		Quarters q = new Quarters();
		assertTrue(q.adoptBank(bank, "Codex Bank Steward", NOW));
		long ms = (System.nanoTime() - t0) / 1_000_000;
		assertEquals(1_200, q.getBank().size());
		assertTrue("read in " + ms + " ms", ms < 200);
		assertEquals(Collections.emptyMap(), new Quarters().getBank());
	}
}
