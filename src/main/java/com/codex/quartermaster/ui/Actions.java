package com.codex.quartermaster.ui;

/** What the panel asks the plugin to do. Every call comes on the Swing thread. */
public interface Actions
{
	void choose(String kitId);

	void target(int trips);

	/** Save what is carried now as a new kit. */
	void capture();

	void forget(String kitId);

	/** Take a supply out of a saved kit, for something that was carried but is not used up. */
	void drop(String kitId, String supply);

	/** Pack the amounts the trips advise for a saved kit. */
	void tune(String kitId);
}
