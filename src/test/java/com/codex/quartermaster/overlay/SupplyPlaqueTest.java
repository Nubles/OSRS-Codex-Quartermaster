package com.codex.quartermaster.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class SupplyPlaqueTest
{
	private static FontMetrics metrics()
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		return g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
	}

	@Test
	public void aLongKitNameGivesWayToWhatRunsOut()
	{
		FontMetrics fm = metrics();
		String note = SupplyPlaque.note("Phantom Muspah (shielded, ranged) with a very long name", "out of prayer restores", fm, 240);
		assertTrue(note, note.endsWith("  -  out of prayer restores"));
		assertTrue(note, note.startsWith("Phantom"));
		assertTrue(fm.stringWidth(note) <= 240);
	}

	@Test
	public void aShortNoteIsWhole()
	{
		assertEquals("Vorkath  -  Food first", SupplyPlaque.note("Vorkath", "Food first", metrics(), 240));
		assertEquals("To pack: Shark", SupplyPlaque.note("To pack: Shark", null, metrics(), 240));
	}

	@Test
	public void noRoomForTheNameLeavesTheStatus()
	{
		FontMetrics fm = metrics();
		String tail = "out of extended super antifire potions";
		String note = SupplyPlaque.note("Vorkath", tail, fm, fm.stringWidth(tail) + 4);
		assertEquals(tail, note);
	}
}
