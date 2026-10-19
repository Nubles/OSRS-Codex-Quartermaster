package com.codex.quartermaster;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(CodexQuartermasterConfig.GROUP)
public interface CodexQuartermasterConfig extends Config
{
	String GROUP = "codexquartermaster";

	/** Where the plaque shows. */
	enum PlaqueMode
	{
		AT_THE_BANK("At the bank"),
		ALWAYS("Always"),
		NEVER("Never");

		private final String label;

		PlaqueMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(
		name = "Planning",
		description = "How the Quartermaster counts your trips",
		position = 0
	)
	String planning = "planning";

	@ConfigSection(
		name = "At the bank",
		description = "Help while you pack a kit",
		position = 1
	)
	String atTheBank = "atTheBank";

	@ConfigSection(
		name = "Display",
		description = "The plaque in the game view and the kit card",
		position = 2
	)
	String display = "display";

	@ConfigItem(
		keyName = "warnTrips",
		name = "Warn at trips left",
		description = "Say so in the chat box when you leave the bank with this many trips of supplies or fewer left",
		position = 2,
		section = planning
	)
	@Range(min = 0, max = 50)
	default int warnTrips()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Supply plaque",
		description = "When to show the plaque with how many trips your bank supports and what runs out first",
		position = 0,
		section = display
	)
	default PlaqueMode plaque()
	{
		return PlaqueMode.AT_THE_BANK;
	}

	@ConfigItem(
		keyName = "measured",
		name = "Plan from your trips",
		description = "Once a kit has been measured over two trips, plan with what you really used instead of the kit's own amounts",
		position = 0,
		section = planning
	)
	default boolean measured()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recognise",
		name = "Recognise packed kits",
		description = "Choose one of your saved kits by itself when you leave the bank carrying it",
		position = 1,
		section = planning
	)
	default boolean recognise()
	{
		return true;
	}

	@ConfigItem(
		keyName = "packCheck",
		name = "Packing check",
		description = "List what the chosen kit still needs at the bank, and say so in the chat box if you leave with something forgotten",
		position = 0,
		section = atTheBank
	)
	default boolean packCheck()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightBank",
		name = "Mark what to withdraw",
		description = "Outline the bank items the chosen kit still needs, with how many to take, while you pack it",
		position = 1,
		section = atTheBank
	)
	default boolean highlightBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nameOnCard",
		name = "Name on cards",
		description = "Write your character's name on the kit card; turn off to share the card without it",
		position = 1,
		section = display
	)
	default boolean nameOnCard()
	{
		return true;
	}
}
