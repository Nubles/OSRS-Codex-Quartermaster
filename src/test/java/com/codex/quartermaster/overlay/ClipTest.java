package com.codex.quartermaster.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.codex.quartermaster.ui.Card;
import com.codex.quartermaster.ui.Theme;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * Drawing never clears or changes the graphics it is handed: RuneLite's overlay renderer lays
 * every overlay out on one graphics, and a clip cleared to null by one plaque breaks it for all.
 */
public class ClipTest
{
	private static final Rectangle CLIP = new Rectangle(3, 2, 150, 15);

	private static Graphics2D handed(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setClip(CLIP);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
		g.setColor(Color.MAGENTA);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setComposite(AlphaComposite.SrcOver);
		return g;
	}

	private static void assertUntouched(Graphics2D g)
	{
		assertNotNull("the clip is never cleared", g.getClip());
		assertEquals(CLIP, g.getClip().getBounds());
		assertEquals(new Font(Font.SANS_SERIF, Font.PLAIN, 9), g.getFont());
		assertEquals(Color.MAGENTA, g.getColor());
		assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
		assertEquals(AlphaComposite.SrcOver, g.getComposite());
		assertEquals(new AffineTransform(), g.getTransform());
	}

	@Test
	public void thePlaqueLeavesTheGraphicsAsItFoundThem()
	{
		BufferedImage img = new BufferedImage(400, 60, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = handed(img);
		SupplyPlaque plaque = new SupplyPlaque();
		plaque.show("14 trips", "Vorkath", "Prayer restores first", Theme.GOLD);
		assertNotNull(plaque.render(g));
		assertUntouched(g);
		// The leaving check's form of it, after the bank closes.
		plaque.show("Left without", "Ring of recoil and spade", null, Theme.BLOOD);
		assertNotNull(plaque.render(g));
		assertUntouched(g);
		g.dispose();
	}

	@Test
	public void anAccentedCardPaintsNothingPastTheClip()
	{
		Card card = new Card(new BorderLayout());
		card.accent(Theme.EMBER);
		card.setSize(180, 40);
		BufferedImage img = new BufferedImage(200, 60, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = handed(img);
		card.paint(g);
		// Swing itself sets the font and colour of what it paints with; the clip is the card's to keep.
		assertNotNull("the clip is never cleared", g.getClip());
		assertEquals(CLIP, g.getClip().getBounds());
		g.dispose();
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if (!CLIP.contains(x, y))
				{
					assertEquals("painted outside the clip at " + x + "," + y, 0, img.getRGB(x, y) >>> 24);
				}
			}
		}
	}
}
