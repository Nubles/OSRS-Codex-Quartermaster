package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static com.codex.quartermaster.core.Fixtures.kit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class BankViewTest
{
	private static final Kit VORKATH = kit("FOOD", 200, "PRAYER", 16, "RANGING", 4, Supply.itemKey(ItemID.SPADE), 0);

	@Test
	public void theBankShowsOnlyWhatIsStillToTake()
	{
		Map<Integer, Long> bank = items(ItemID.SHARK, 50, ItemID.LOBSTER, 500, ItemID._4DOSEPRAYERRESTORE, 40, ItemID.SPADE, 1);
		// Sharks and the prayer potions are short; the ranging potion and the spade are packed.
		Packing p = Packing.check(VORKATH, items(ItemID.SHARK, 4, ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1), bank);
		BankView view = BankView.of(p);
		assertTrue(view.contains(ItemID.SHARK));
		assertTrue(view.contains(ItemID._4DOSEPRAYERRESTORE));
		assertFalse("the bigger pile of another food is not what to take", view.contains(ItemID.LOBSTER));
		assertFalse("packed already", view.contains(ItemID.SPADE));
		assertNull(view.refusal(true, true));
	}

	@Test
	public void packedOrAwayFromTheBankThereIsNothingToShow()
	{
		Packing packed = Packing.check(VORKATH, items(ItemID.SHARK, 10, ItemID._4DOSEPRAYERRESTORE, 4,
			ItemID._4DOSERANGERSPOTION, 1, ItemID.SPADE, 1), items(ItemID.SHARK, 50));
		assertTrue(BankView.of(packed).isEmpty());
		assertEquals(BankView.Refusal.NOTHING, BankView.of(packed).refusal(true, true));
		assertTrue(BankView.of(null).isEmpty());
	}

	@Test
	public void theBankMustBeOpenAndBankTagsOn()
	{
		Packing p = Packing.check(VORKATH, items(), items(ItemID.SHARK, 50));
		assertEquals(BankView.Refusal.BANK_CLOSED, BankView.of(p).refusal(false, true));
		assertEquals(BankView.Refusal.TAGS_OFF, BankView.of(p).refusal(true, false));
		assertTrue(BankView.explain(BankView.Refusal.BANK_CLOSED).contains("tag:quartermaster"));
	}

	@Test
	public void aViewRememberedForTheNextVisitIsClosedToo()
	{
		// Bank Tags clears the open tag as the bank closes, before the Quartermaster hears of it,
		// but keeps its name to open again next visit.
		assertTrue(BankView.shouldClose(null, BankView.TAG));
		assertTrue(BankView.shouldClose(BankView.TAG, ""));
		assertFalse(BankView.shouldClose("vorkath", "vorkath"));
		assertFalse(BankView.shouldClose(null, null));
	}
}
