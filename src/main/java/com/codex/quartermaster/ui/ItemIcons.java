package com.codex.quartermaster.ui;

import java.awt.image.BufferedImage;

/** Where the panel gets item pictures; the plugin answers from RuneLite's item manager. */
public interface ItemIcons
{
	/**
	 * @param onLoaded run once if the picture arrives later than this call; null for none
	 * @return the picture, possibly still blank, or null
	 */
	BufferedImage icon(int itemId, Runnable onLoaded);
}
