package com.codex.quartermaster.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.codex.quartermaster.core.Moorings;
import com.codex.quartermaster.core.Shipyard;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class BoatsPageTest
{
	private static View view(boolean bankSeen, Map<Integer, Long> held)
	{
		View v = new View(bankSeen ? View.State.READY : View.State.WAITING_FOR_BANK, "Me", null, null, null, 0,
			bankSeen ? 1 : 0, false, null, 3, 2);
		Shipyard.Roadmap map = Shipyard.roadmap(Shipyard.Boat.SKIFF, 34, 40, id -> held.getOrDefault(id, 0L));
		return v.boats(map, false, Moorings.NONE, held);
	}

	private static int count(Container c, Class<?> type)
	{
		int n = 0;
		for (Component ch : c.getComponents())
		{
			n += type.isInstance(ch) ? 1 : 0;
			if (ch instanceof Container)
			{
				n += count((Container) ch, type);
			}
		}
		return n;
	}

	private static boolean says(Container c, String text)
	{
		for (Component ch : c.getComponents())
		{
			if (ch instanceof JLabel && ((JLabel) ch).getText().contains(text))
			{
				return true;
			}
			if (ch instanceof Container && says((Container) ch, text))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void beforeTheBankIsSeenNoMaterialIsCountedAsMissing()
	{
		StackPanel page = BoatsPage.build(view(false, Collections.emptyMap()), null, new Segmented(false, "Raft", "Skiff", "Sloop"));
		assertEquals("no shopping rows without a bank", 0, count(page, BuyRow.class));
		assertTrue(says(page, "Open your bank once"));
	}

	@Test
	public void withTheBankEachShortMaterialHasARow()
	{
		Map<Integer, Long> held = new HashMap<>();
		held.put(ItemID.SAILING_BOAT_HULL_PART_TEAK, 10L);
		StackPanel page = BoatsPage.build(view(true, held), null, new Segmented(false, "Raft", "Skiff", "Sloop"));
		assertTrue(count(page, BuyRow.class) > 0);
		assertTrue(says(page, "Teak hull"));
		assertTrue(says(page, "Sailing 48 (+14)"));
	}

	@Test
	public void loggedOutAsksToLogIn()
	{
		StackPanel page = BoatsPage.build(View.loggedOut(), null, new Segmented(false, "Raft", "Skiff", "Sloop"));
		assertTrue(says(page, "Log in to plan your boat"));
	}

	@Test
	public void longTabLabelsGetRoomForTheirWords()
	{
		Segmented tabs = new Segmented(true, "Supplies", "Restock", "Kits", "Boats");
		FontMetrics fm = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics().getFontMetrics(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		float[] edges = tabs.edges(fm, 209);
		assertEquals(209, edges[4], 0.01);
		String[] labels = {"Supplies", "Restock", "Kits", "Boats"};
		for (int i = 0; i < 4; i++)
		{
			assertTrue(labels[i] + " fits its segment", edges[i + 1] - edges[i] >= fm.stringWidth(labels[i]));
		}
		// Short labels such as the trip targets keep even segments.
		float[] even = new Segmented(false, "5", "10", "25", "50", "100").edges(fm, 200);
		assertEquals(40, even[1], 0.01);
	}
}
