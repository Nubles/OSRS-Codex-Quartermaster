package com.codex.quartermaster.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.quartermaster.core.Kit;
import com.codex.quartermaster.core.Loadout;
import com.codex.quartermaster.core.Plan;
import com.codex.quartermaster.core.Quarters;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class KitSheetTest
{
	/** Seven pixels a letter, as near as the game's bold font comes. */
	private static final ToIntFunction<String> BOLD = t -> t.length() * 7;
	private static final int ROOM = 244;

	@Test
	public void aShortNameKeepsTheLargeLetteringOnOneLine()
	{
		KitSheet.Title t = KitSheet.title("Vorkath", BOLD, ROOM);
		assertTrue(t.large);
		assertEquals(Collections.singletonList("VORKATH"), t.lines);
	}

	@Test
	public void aLongNameBreaksBetweenWordsAndStaysLarge()
	{
		// 32 letters: too wide for one doubled line, but two even lines fit.
		KitSheet.Title t = KitSheet.title("Corporeal Beast mass, five of us", BOLD, ROOM);
		assertTrue("large lettering is kept for a long name", t.large);
		assertEquals(Arrays.asList("CORPOREAL BEAST", "MASS, FIVE OF US"), t.lines);
	}

	@Test
	public void onlyANameTooLongForTwoLargeLinesStepsDown()
	{
		KitSheet.Title t = KitSheet.title("Supercalifragilisticexpialidocio", BOLD, ROOM);
		assertFalse(t.large);
		assertEquals(1, t.lines.size());
		KitSheet.Title two = KitSheet.title("Supercalifragilistic expialidocious and more besides", BOLD, ROOM);
		assertFalse(two.large);
		assertEquals(2, two.lines.size());
	}

	private static Map<Integer, Long> items(long... pairs)
	{
		Map<Integer, Long> m = new HashMap<>();
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			m.put((int) pairs[i], pairs[i + 1]);
		}
		return m;
	}

	@Test
	public void aNewKitFillsTheColumnWithWhatIsAlwaysKnown()
	{
		Quarters q = new Quarters();
		Map<Integer, Long> bank = items(ItemID.SHARK, 100, ItemID._4DOSEPRAYERRESTORE, 10);
		q.observeBank(bank, 1);
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 14, ItemID._4DOSEPRAYERRESTORE, 4), id -> true);
		Plan plan = q.plan(kit, Collections.emptyMap(), id -> 1000, true);
		View view = new View(View.State.READY, null, plan, Collections.singletonList(plan), null, 0, 1, false, null, 3, 1);
		Loadout loadout = Loadout.of(kit, q.getBank(), id -> false);
		List<KitSheet.Fact> facts = KitSheet.facts(plan, view, null, loadout);
		// Banked and per trip are all a new kit's own numbers give; the slots and the restock bill fill in.
		assertEquals(KitSheet.FACTS, facts.size());
		assertEquals("Packs", facts.get(2).label);
		assertEquals("18 slots", facts.get(2).value);
		assertEquals("10 left free", facts.get(2).note);
		assertEquals("Restock", facts.get(3).label);
	}
}
