package com.codex.quartermaster.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void amountsFollowTheSuiteRule()
	{
		// In full below 10,000; whole k below a million; M to one decimal; B to two.
		assertEquals("8,240", Format.amount(8_240));
		assertEquals("10k", Format.amount(10_000));
		assertEquals("41k", Format.amount(41_200));
		assertEquals("96k", Format.amount(95_700));
		assertEquals("412k", Format.amount(412_000));
		assertEquals("1.2M", Format.amount(1_240_000));
		assertEquals("2.4M", Format.amount(2_370_000));
		assertEquals("26.0M", Format.amount(26_000_000));
		assertEquals("98.7M", Format.amount(98_700_000));
		assertEquals("812.4M", Format.amount(812_400_000));
		assertEquals("2.10B", Format.amount(2_100_000_000L));
		// Rounding that reaches the next unit is written in it.
		assertEquals("100k", Format.amount(99_960));
		assertEquals("1.0M", Format.amount(999_600));
		assertEquals("999k", Format.amount(999_499));
		assertEquals("1.00B", Format.amount(999_960_000));
		assertEquals("-45k", Format.coins(-45_200));
		assertEquals("+880k", Format.signed(880_000));
	}

	@Test
	public void tripsAndUse()
	{
		assertEquals("999+", Format.trips(1_000));
		assertEquals("12", Format.trips(12));
		assertEquals("16", Format.perTrip(16.02));
		assertEquals("13.5", Format.perTrip(13.5));
		assertEquals("214", Format.perTrip(213.6));
	}

	@Test
	public void times()
	{
		assertEquals("just now", Format.ago(-5));
		assertEquals("12m ago", Format.ago(12 * 60_000));
		assertEquals("3h ago", Format.ago(3 * 3_600_000));
		assertEquals("2d ago", Format.ago(50 * 3_600_000L));
		assertEquals("under a minute", Format.duration(59_000));
		assertEquals("38m", Format.duration(38 * 60_000));
		assertEquals("2h 14m", Format.duration((2 * 60 + 14) * 60_000));
	}

	@Test
	public void tripColours()
	{
		assertEquals(Theme.BLOOD, Theme.trips(0, 3));
		assertEquals(Theme.EMBER, Theme.trips(3, 3));
		assertEquals(Theme.GOLD, Theme.trips(4, 3));
		assertEquals(Theme.GREEN, Theme.trips(24, 3));
		// Warning switched down to 0 does not make a single trip look plentiful.
		assertEquals(Theme.GOLD, Theme.trips(1, 0));
	}

	@Test
	public void spanTurnsToDaysPastTwoDays()
	{
		assertEquals("6h 40m", Format.span(400 * 60_000L));
		assertEquals("47h 0m", Format.span(47 * 3_600_000L));
		assertEquals("3d 4h", Format.span(76 * 3_600_000L));
	}
}
