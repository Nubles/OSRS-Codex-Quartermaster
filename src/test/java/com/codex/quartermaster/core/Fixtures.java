package com.codex.quartermaster.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/** Shared prices, item maps and kits for the core tests. */
final class Fixtures
{
	static final Map<Integer, Long> PRICES = new HashMap<>();

	static
	{
		PRICES.put(ItemID.SHARK, 900L);
		PRICES.put(ItemID.MANTARAY, 1_400L);
		PRICES.put(ItemID.ANGLERFISH, 1_700L);
		PRICES.put(ItemID.LOBSTER, 160L);
		PRICES.put(ItemID.CAKE, 90L);
		PRICES.put(ItemID.CAKE_SLICE, 10L);
		PRICES.put(ItemID.JUG_WINE, 30L);
		PRICES.put(ItemID._4DOSEPRAYERRESTORE, 8_000L);
		PRICES.put(ItemID._1DOSEPRAYERRESTORE, 1_000L);
		PRICES.put(ItemID._4DOSE2RESTORE, 10_000L);
		PRICES.put(ItemID._4DOSERANGERSPOTION, 700L);
		PRICES.put(ItemID._4DOSEDIVINERANGE, 2_200L);
		PRICES.put(ItemID.ANTIVENOM4, 9_800L);
		PRICES.put(ItemID.ANTIVENOM_4, 4_100L);
		PRICES.put(ItemID.DRAGON_BOLTS_ENCHANTED_RUBY, 1_100L);
		PRICES.put(ItemID.TELETAB_BARROWS, 300L);
	}

	private Fixtures()
	{
	}

	static long price(int id)
	{
		return PRICES.getOrDefault(id, 0L);
	}

	static Map<Integer, Long> items(long... pairs)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			m.put((int) pairs[i], pairs[i + 1]);
		}
		return m;
	}

	static Kit kit(Object... pairs)
	{
		Need[] needs = new Need[pairs.length / 2];
		for (int i = 0; i < needs.length; i++)
		{
			needs[i] = new Need((String) pairs[i * 2], (Integer) pairs[i * 2 + 1]);
		}
		return new Kit("kit:test", "Test", Arrays.asList(needs), false);
	}
}
