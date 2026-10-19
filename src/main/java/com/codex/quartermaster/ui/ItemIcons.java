package com.codex.quartermaster.ui;

import java.awt.image.BufferedImage;
import java.util.Collection;

/** Where the panel gets item pictures; the plugin answers from RuneLite's item manager. */
public interface ItemIcons
{
	/**
	 * @param onLoaded run once if the picture arrives later than this call; null for none
	 * @return the picture, possibly still blank, or null
	 */
	BufferedImage icon(int itemId, Runnable onLoaded);

	/**
	 * Runs {@code then} once every one of these pictures has arrived: at once when they all have,
	 * otherwise when the last comes in. A card drawn to be shared waits on this, so it never goes
	 * out with an empty slot where a picture was still on its way.
	 */
	default void whenLoaded(Collection<Integer> itemIds, Runnable then)
	{
		then.run();
	}
}
