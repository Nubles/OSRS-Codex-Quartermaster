package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * Reads exactly what the other Codex plugins save. {@code bank-steward-bankV1.json} is a copy of
 * the fixture Bank Steward's own BankSnapshotTest proves its writer produces;
 * {@code gear-roadmap-bank.json} was written by Gear Roadmap's own {@code Bank} class with Gson.
 */
public class SharedBankContractTest
{
	private static final Gson GSON = new Gson();

	static String fixture(String name) throws Exception
	{
		try (InputStream in = SharedBankContractTest.class.getResourceAsStream(name))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; )
			{
				out.write(buf, 0, n);
			}
			return new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
		}
	}

	@Test
	public void groupsAndKeysMatchTheWriters()
	{
		// CodexBankStewardConfig.GROUP and BankSnapshot.KEY; CodexGearRoadmapConfig.GROUP and BANK_KEY.
		assertEquals("codexbanksteward", SharedBank.STEWARD_GROUP);
		assertEquals("bankV1", SharedBank.STEWARD_KEY);
		assertEquals("codexgearroadmap", SharedBank.ROADMAP_GROUP);
		assertEquals("bank", SharedBank.ROADMAP_KEY);
	}

	@Test
	public void bankStewardSnapshotHoldsEveryItemWithItsCount() throws Exception
	{
		SharedBank bank = SharedBank.read(GSON, fixture("bank-steward-bankV1.json"), null);
		assertSame(SharedBank.Source.BANK_STEWARD, bank.getSource());
		assertTrue(bank.isComplete());
		assertEquals(1_760_000_000_000L, bank.getSavedAt());
		assertEquals(1, bank.count(26950));
		assertEquals(1, bank.count(11666));
		assertEquals(240, bank.count(385));
		assertEquals(1_234_567, bank.count(995));
		assertEquals(0, bank.count(4151));
	}

	@Test
	public void gearRoadmapBankIsTheFallbackAndOnlyHalfTheStory() throws Exception
	{
		String roadmap = fixture("gear-roadmap-bank.json");
		SharedBank bank = SharedBank.read(GSON, null, roadmap);
		assertSame(SharedBank.Source.GEAR_ROADMAP, bank.getSource());
		assertFalse(bank.isComplete());
		assertEquals(0, bank.getSavedAt());
		assertEquals(1, bank.count(26948));
		assertEquals(1, bank.count(8839));
		assertEquals(1_234_567, bank.count(995));
		assertEquals("it never saves a fresh sceptre", 0, bank.count(26950));

		// Bank Steward's wins when both are there, and an unreadable one falls through.
		assertSame(SharedBank.Source.BANK_STEWARD, SharedBank.read(GSON, fixture("bank-steward-bankV1.json"), roadmap).getSource());
		assertSame(SharedBank.Source.GEAR_ROADMAP, SharedBank.read(GSON, "{\"version\":1}", roadmap).getSource());
		assertSame(SharedBank.Source.GEAR_ROADMAP, SharedBank.read(GSON, "not json {", roadmap).getSource());
	}

	@Test
	public void nothingSavedIsNoBank()
	{
		assertNull(SharedBank.read(GSON, null, null));
		assertNull(SharedBank.read(GSON, "", "  "));
		assertNull(SharedBank.read(GSON, "[1,2]", "{\"coins\":5}"));
		// Odd values inside a readable snapshot are skipped, not fatal.
		SharedBank odd = SharedBank.read(GSON, "{\"items\":{\"x\":1,\"995\":\"many\",\"385\":-2,\"391\":3},\"truncated\":true}", null);
		assertEquals(1, odd.getItems().size());
		assertEquals(3, odd.count(391));
		assertFalse("a truncated snapshot may miss items", odd.isComplete());
	}
}
