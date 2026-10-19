package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** When the Quartermaster takes the bank Codex Bank Steward saved instead of waiting to see it. */
public class SharedBankAdoptTest
{
	private static final Gson GSON = new Gson();
	private static final long SAVED_AT = 1_760_000_000_000L;

	private static SharedBank steward() throws Exception
	{
		return SharedBank.read(GSON, SharedBankContractTest.fixture("bank-steward-bankV1.json"), null);
	}

	@Test
	public void freshInstallCountsTheStewardsBank() throws Exception
	{
		Quarters q = new Quarters();
		assertFalse(q.hasBank());
		assertTrue(q.adoptBank(steward(), "Codex Bank Steward", SAVED_AT + 60_000));
		assertTrue(q.hasBank());
		assertEquals(SAVED_AT, q.getBankSeen());
		assertEquals("Codex Bank Steward", q.getBankFrom());
		assertEquals(240L, (long) q.getBank().get(ItemID.SHARK));
		// A preset that eats sharks now plans from them: 240 sharks of 20 hp.
		Kit kit = new Kit("kit:x", "Sharks", Collections.singletonList(new Need("FOOD", 400)), false);
		assertEquals(12, q.plan(kit, Collections.emptyMap(), id -> 0, false).trips());

		// Seeing the bank itself puts the Quartermaster's own count back in charge.
		q.observeBank(Collections.singletonMap(ItemID.SHARK, 10L), SAVED_AT + 1);
		assertNull(q.getBankFrom());
	}

	@Test
	public void onlyAClearlyNewerFullBankReplacesTheOneSeenHere() throws Exception
	{
		Quarters q = new Quarters();
		// Both plugins saw the same visit a moment apart: the Quartermaster's own stays.
		q.observeBank(Collections.singletonMap(ItemID.SHARK, 5L), SAVED_AT - 2_000);
		assertFalse(q.adoptBank(steward(), "Codex Bank Steward", SAVED_AT + 60_000));
		assertEquals(5L, (long) q.getBank().get(ItemID.SHARK));
		// A visit the Quartermaster missed is taken.
		q.observeBank(Collections.singletonMap(ItemID.SHARK, 5L), SAVED_AT - Quarters.ADOPT_MARGIN_MS - 1);
		assertTrue(q.adoptBank(steward(), "Codex Bank Steward", SAVED_AT + 60_000));

		// Gear Roadmap's bank holds no supplies, so it would read as empty: never taken.
		Quarters fresh = new Quarters();
		SharedBank gear = SharedBank.read(GSON, null, SharedBankContractTest.fixture("gear-roadmap-bank.json"));
		assertFalse(fresh.adoptBank(gear, "Codex Gear Roadmap", SAVED_AT));
		assertFalse(fresh.adoptBank(null, "nobody", SAVED_AT));
		assertFalse(fresh.hasBank());
	}

	@Test
	public void whereTheBankCameFromIsSavedWithIt() throws Exception
	{
		Quarters q = new Quarters();
		q.adoptBank(steward(), "Codex Bank Steward", SAVED_AT + 60_000);
		Quarters back = GSON.fromJson(GSON.toJson(q), Quarters.class).repair();
		assertEquals("Codex Bank Steward", back.getBankFrom());
		assertEquals(SAVED_AT, back.getBankSeen());
	}
}
