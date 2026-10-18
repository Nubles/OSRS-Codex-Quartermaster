package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WarningTest
{
	@Test
	public void saysItOnceUntilTheCountChangesOrTimePasses()
	{
		Warning w = new Warning();
		assertNull(w.check("k", "Vorkath", 4, 3, "prayer restores", 0));
		assertEquals("3 Vorkath trips left, counting this one; prayer restores runs out first.",
			w.check("k", "Vorkath", 3, 3, "prayer restores", 0));
		assertNull("a quick bank shuffle", w.check("k", "Vorkath", 3, 3, "prayer restores", 60_000));
		assertNotNull(w.check("k", "Vorkath", 2, 3, "prayer restores", 120_000));
		assertNotNull(w.check("k", "Vorkath", 2, 3, "prayer restores", 120_000 + Warning.AGAIN_MS));
		assertNotNull("another kit", w.check("j", "Zulrah", 2, 3, "food", 120_000 + Warning.AGAIN_MS));
		assertNull(w.check("k", "Vorkath", Plan.UNLIMITED, Integer.MAX_VALUE, "food", 0));
	}

	@Test
	public void wordsTheLastTripAndTheEmptyBank()
	{
		assertEquals("this is your last Zulrah trip before food runs out.", Warning.text("Zulrah", 1, "food"));
		assertEquals("not enough food for a full Zulrah trip.", Warning.text("Zulrah", 0, "food"));
	}
}
