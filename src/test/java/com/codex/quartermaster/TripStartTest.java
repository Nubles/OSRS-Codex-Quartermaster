package com.codex.quartermaster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.GameState;
import org.junit.Test;

public class TripStartTest
{
	@Test
	public void theBankClosingAsTheGameLogsOutIsNoTrip()
	{
		assertTrue(CodexQuartermasterPlugin.setsOut(GameState.LOGGED_IN));
		// Hopping straight from the bank still sets out.
		assertTrue(CodexQuartermasterPlugin.setsOut(GameState.HOPPING));
		assertTrue(CodexQuartermasterPlugin.setsOut(GameState.LOADING));
		// Otherwise the logged-out changes would be counted as the next trip's use.
		assertFalse(CodexQuartermasterPlugin.setsOut(GameState.LOGIN_SCREEN));
		assertFalse(CodexQuartermasterPlugin.setsOut(GameState.CONNECTION_LOST));
		assertFalse(CodexQuartermasterPlugin.setsOut(GameState.LOGGING_IN));
	}
}
