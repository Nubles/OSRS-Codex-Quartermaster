package com.codex.quartermaster.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The bank as another Codex plugin last saw it, read from what that plugin saves in the RuneLite
 * profile. Only read, never written, and absent when neither plugin is installed.
 *
 * <p>Codex Bank Steward's {@code codexbanksteward/bankV1} is preferred: it holds every item with
 * its quantity and when it was seen,
 * {@code {"version":1,"savedAt":1760000000000,"items":{"995":1234567,"26950":1},"truncated":false}}.
 * Codex Gear Roadmap's {@code codexgearroadmap/bank} is the fallback, {@code {"items":[4151],"coins":N}}:
 * only the gear Roadmap knows, with no quantities (each counts as one) and no time.</p>
 */
public final class SharedBank
{
	public static final String STEWARD_GROUP = "codexbanksteward";
	public static final String STEWARD_KEY = "bankV1";
	public static final String ROADMAP_GROUP = "codexgearroadmap";
	public static final String ROADMAP_KEY = "bank";
	private static final int COINS = 995;
	/** Readers accept no more kinds of item than the Steward ever writes. */
	private static final int MAX_ITEMS = 2000;

	/** Where a shared bank came from. */
	public enum Source
	{
		/** Every item and its quantity. */
		BANK_STEWARD,
		/** Only the gear Gear Roadmap tracks, and coins. */
		GEAR_ROADMAP
	}

	private final Source source;
	private final Map<Integer, Long> items;
	private final long savedAt;
	private final boolean complete;

	private SharedBank(Source source, Map<Integer, Long> items, long savedAt, boolean complete)
	{
		this.source = source;
		this.items = Collections.unmodifiableMap(items);
		this.savedAt = savedAt;
		this.complete = complete;
	}

	/**
	 * The best bank on offer: Bank Steward's, else Gear Roadmap's, else null.
	 *
	 * @param steward what {@code codexbanksteward/bankV1} holds for this profile, or null
	 * @param roadmap what {@code codexgearroadmap/bank} holds for this profile, or null
	 */
	public static SharedBank read(Gson gson, String steward, String roadmap)
	{
		SharedBank bank = readSteward(gson, steward);
		return bank != null ? bank : readRoadmap(gson, roadmap);
	}

	static SharedBank readSteward(Gson gson, String json)
	{
		JsonObject root = object(gson, json);
		JsonElement list = root == null ? null : root.get("items");
		if (list == null || !list.isJsonObject())
		{
			return null;
		}
		Map<Integer, Long> items = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : list.getAsJsonObject().entrySet())
		{
			Integer id = integer(e.getKey());
			Long qty = number(e.getValue());
			if (id != null && id > 0 && qty != null && qty > 0 && items.size() < MAX_ITEMS)
			{
				items.put(id, qty);
			}
		}
		Long savedAt = number(root.get("savedAt"));
		JsonElement truncated = root.get("truncated");
		boolean cut = truncated != null && truncated.isJsonPrimitive() && truncated.getAsJsonPrimitive().isBoolean()
			&& truncated.getAsBoolean();
		return new SharedBank(Source.BANK_STEWARD, items, savedAt == null ? 0 : savedAt, !cut);
	}

	static SharedBank readRoadmap(Gson gson, String json)
	{
		JsonObject root = object(gson, json);
		JsonElement list = root == null ? null : root.get("items");
		if (list == null || !list.isJsonArray())
		{
			return null;
		}
		Map<Integer, Long> items = new HashMap<>();
		for (JsonElement e : list.getAsJsonArray())
		{
			Long id = number(e);
			if (id != null && id > 0 && id <= Integer.MAX_VALUE && items.size() < MAX_ITEMS)
			{
				items.put(id.intValue(), 1L);
			}
		}
		Long coins = number(root.get("coins"));
		if (coins != null && coins > 0)
		{
			items.put(COINS, coins);
		}
		return new SharedBank(Source.GEAR_ROADMAP, items, 0, false);
	}

	public Source getSource()
	{
		return source;
	}

	/** Item id to quantity; a Gear Roadmap bank counts each item it lists as one. */
	public Map<Integer, Long> getItems()
	{
		return items;
	}

	public Set<Integer> ids()
	{
		return items.keySet();
	}

	public long count(int itemId)
	{
		return items.getOrDefault(itemId, 0L);
	}

	/** When the bank was seen, in epoch milliseconds; 0 when the source does not say. */
	public long getSavedAt()
	{
		return savedAt;
	}

	/**
	 * Whether every item in the bank is here with its true quantity, so a missing item really is
	 * not in the bank. Only Bank Steward's snapshot is.
	 */
	public boolean isComplete()
	{
		return complete;
	}

	private static JsonObject object(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
		}
		catch (JsonParseException | IllegalStateException | NumberFormatException e)
		{
			return null;
		}
	}

	private static Long number(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		try
		{
			return e.getAsLong();
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static Integer integer(String s)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
