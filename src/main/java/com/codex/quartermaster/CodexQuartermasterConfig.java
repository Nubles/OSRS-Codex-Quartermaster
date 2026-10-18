package com.codex.quartermaster;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(CodexQuartermasterConfig.GROUP)
public interface CodexQuartermasterConfig extends Config
{
	String GROUP = "codexquartermaster";

	/** Where the plaque shows. */
	enum PlaqueMode
	{
		AT_THE_BANK,
		ALWAYS,
		NEVER
	}

	@ConfigItem(
		keyName = "warnTrips",
		name = "Warn at trips left",
		description = "When you set out with this many trips of supplies or fewer left, say so in the chat box",
		position = 0
	)
	@Range(min = 0, max = 50)
	default int warnTrips()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Supply plaque",
		description = "Show a small plaque in the game view with how many trips your bank supports and what runs out first",
		position = 1
	)
	default PlaqueMode plaque()
	{
		return PlaqueMode.AT_THE_BANK;
	}

	@ConfigItem(
		keyName = "measured",
		name = "Plan from your trips",
		description = "Once a kit has been measured over a couple of trips, plan with what you really used instead of the kit's own amounts",
		position = 2
	)
	default boolean measured()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recognise",
		name = "Recognise packed kits",
		description = "When you close the bank carrying one of your saved kits, choose it automatically",
		position = 3
	)
	default boolean recognise()
	{
		return true;
	}
}
