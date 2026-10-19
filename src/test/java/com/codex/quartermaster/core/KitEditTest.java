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
		assertEquals("Vorkath, the long trip that neve", kit.getName());
		assertFalse("blank", q.rename(kit.getId(), "   "));
		assertFalse("unchanged", q.rename(kit.getId(), kit.getName()));
		Kit preset = q.allKits().get(q.allKits().size() - 1);
		assertTrue(preset.isPreset());
		String was = preset.getName();
		assertFalse(q.rename(preset.getId(), "Mine now"));
		assertEquals(was, preset.getName());
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
