package com.codex.quartermaster;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexQuartermasterPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexQuartermasterPlugin.class);
		RuneLite.main(args);
	}
}
