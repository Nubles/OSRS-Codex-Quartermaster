package com.codex.quartermaster.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MooringsTest
{
	/** Stand-in tables: dock 0 is Port Sarim, 26 Deepfin Point; types 0 to 2 the three boats. */
	private static final Moorings.Tables TABLES = new Moorings.Tables()
	{
		@Override
		public String dock(int portId)
		{
			return portId == 0 ? "Port Sarim" : portId == 26 ? "Deepfin Point" : null;
		}

		@Override
		public String boat(int typeId)
		{
			return typeId == 0 ? "Raft" : typeId == 1 ? "Skiff" : typeId == 2 ? "Sloop" : null;
		}

		@Override
		public String namePart(int part, int index)
		{
			String[][] options = {{"", "The"}, {"", "Salty", "Lucky"}, {"", "Gull", "Barnacle"}};
			return index < options[part].length ? options[part][index] : null;
		}
	};

	@Test
	public void eachOwnedBoatIsListedWithItsPortAndName()
	{
		List<Moorings.Raw> slots = Arrays.asList(
			new Moorings.Raw(1, 0, 0, 0, 0, 0),
			new Moorings.Raw(1, 1, 26, 2, 3, 3),
			null,
			new Moorings.Raw(0, 0, 0, 0, 0, 0),
			new Moorings.Raw(0, 2, 99, 0, 0, 0));
		Moorings m = Moorings.of(slots, TABLES);
		assertEquals(3, m.getBerths().size());
		Moorings.Berth first = m.getBerths().get(0);
		assertEquals("Boat 1", first.getName());
		assertEquals("Raft", first.getType());
		assertEquals("Port Sarim", first.getPort());
		Moorings.Berth second = m.getBerths().get(1);
		// Each name variable holds the option plus one.
		assertEquals("The Lucky Barnacle", second.getName());
		assertEquals("Deepfin Point", second.getPort());
		Moorings.Berth fifth = m.getBerths().get(2);
		assertEquals(5, fifth.getSlot());
		assertNull("a port the table does not know stays unknown", fifth.getPort());
	}

	@Test
	public void anEmptySlotIsLeftOut()
	{
		assertTrue(Moorings.of(Arrays.asList(new Moorings.Raw(0, 0, 0, 0, 0, 0)), TABLES).isEmpty());
		assertTrue(Moorings.NONE.isEmpty());
	}

	@Test
	public void aBlankFirstOptionAddsNothingToTheName()
	{
		assertEquals("Salty Gull", Moorings.name(new Moorings.Raw(1, 0, 0, 1, 2, 2), 1, TABLES));
	}
}
