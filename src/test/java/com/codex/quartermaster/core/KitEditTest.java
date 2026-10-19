package com.codex.quartermaster.core;

import static com.codex.quartermaster.core.Fixtures.items;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class KitEditTest
{
	@Test
	public void aSavedKitCanBeRenamedButNotAPreset()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("My kit 1", items(ItemID.SHARK, 10), id -> true);
		assertTrue(q.rename(kit.getId(), "  Vorkath, the long trip that never seems to end  "));
		// Cut at a whole word, not mid-word ("...that neve"), and never on a word like "that".
		assertEquals("Vorkath, the long trip", kit.getName());
		assertFalse("blank", q.rename(kit.getId(), "   "));
		assertFalse("unchanged", q.rename(kit.getId(), kit.getName()));
		Kit preset = q.allKits().get(q.allKits().size() - 1);
		assertTrue(preset.isPreset());
		String was = preset.getName();
		assertFalse(q.rename(preset.getId(), "Mine now"));
		assertEquals(was, preset.getName());
	}

	@Test
	public void aLongNameIsCutWhereItStillReads()
	{
		assertEquals("Vorkath", Quarters.fitName("  Vorkath  "));
		assertEquals("Zulrah mage", Quarters.fitName("Zulrah \t mage"));
		// Exactly the cap is kept whole.
		assertEquals("Theatre of Blood, entry mode, me", Quarters.fitName("Theatre of Blood, entry mode, me"));
		// An opened bracket the cap would leave unclosed goes, with what it held.
		assertEquals("Phantom Muspah", Quarters.fitName("Phantom Muspah (shielded, ranged)"));
		// A closed one stays.
		assertEquals("Muspah (ranged) with thralls", Quarters.fitName("Muspah (ranged) with thralls and a cannon"));
		// Never ends on a comma or a joining word.
		assertEquals("Vardorvis blood moon set", Quarters.fitName("Vardorvis blood moon set, ranged, and more"));
		// A cut that falls on a space keeps the whole word before it.
		assertEquals("Corporeal Beast mass, five of us", Quarters.fitName("Corporeal Beast mass, five of us tonight"));
		// One word longer than the cap is cut where the cap falls.
		assertEquals("Supercalifragilisticexpialidocio", Quarters.fitName("Supercalifragilisticexpialidocious"));
		assertEquals(Quarters.NAME_MAX, Quarters.fitName("Supercalifragilisticexpialidocious").length());
	}

	@Test
	public void anAmountCanBeSetAndZeroMeansItComesBack()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 10, ItemID.SPADE, 1), id -> true);
		assertTrue(q.setAmount(kit.getId(), "FOOD", 160));
		assertEquals(160, kit.need("FOOD").getPerTrip());
		assertTrue(q.setAmount(kit.getId(), Supply.itemKey(ItemID.SPADE), 0));
		assertEquals(0, kit.need(Supply.itemKey(ItemID.SPADE)).getPerTrip());
		assertFalse("out of range", q.setAmount(kit.getId(), "FOOD", -1));
		assertFalse("not in the kit", q.setAmount(kit.getId(), "PRAYER", 4));
	}

	@Test
	public void tripsAlreadyMeasuredKeepWhatTheySetOutWith()
	{
		Quarters q = new Quarters();
		Kit kit = q.capture("Mine", items(ItemID.SHARK, 10), id -> true);
		// A trip from before trips noted what they set out with.
		Trip old = new Trip(0, 600_000, Collections.singletonMap("FOOD", 100L), 0);
		q.usageOrNew(kit.getId()).getRecent().add(old);
		q.setAmount(kit.getId(), "FOOD", 60);
		assertEquals(200, old.took("FOOD", -1));
	}
}
