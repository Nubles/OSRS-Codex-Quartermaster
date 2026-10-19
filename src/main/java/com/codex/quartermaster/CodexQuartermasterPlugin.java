package com.codex.quartermaster;

import com.codex.quartermaster.core.BankView;
import com.codex.quartermaster.core.Kit;
import com.codex.quartermaster.core.KitBook;
import com.codex.quartermaster.core.Loadout;
import com.codex.quartermaster.core.Packing;
import com.codex.quartermaster.core.Plan;
import com.codex.quartermaster.core.Prices;
import com.codex.quartermaster.core.Quarters;
import com.codex.quartermaster.core.Reminder;
import com.codex.quartermaster.core.Returns;
import com.codex.quartermaster.core.SharedBank;
import com.codex.quartermaster.core.Trip;
import com.codex.quartermaster.core.Tuning;
import com.codex.quartermaster.core.Usage;
import com.codex.quartermaster.core.Warning;
import com.codex.quartermaster.overlay.SupplyPlaque;
import com.codex.quartermaster.overlay.WithdrawOverlay;
import com.codex.quartermaster.ui.Actions;
import com.codex.quartermaster.ui.Format;
import com.codex.quartermaster.ui.ItemIcons;
import com.codex.quartermaster.ui.LoadingIcons;
import com.codex.quartermaster.ui.QuartermasterPanel;
import com.codex.quartermaster.ui.Sigil;
import com.codex.quartermaster.ui.Theme;
import com.codex.quartermaster.ui.View;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.bank.BankSearch;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Quartermaster",
	description = "Pick the content you are doing and see how many trips your bank supports, what runs out first and what restocking costs at Grand Exchange prices",
	tags = {"supplies", "bank", "trips", "restock", "potions", "food", "boss", "grand exchange", "codex"}
)
@PluginDependency(BankTagsPlugin.class)
public class CodexQuartermasterPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexQuartermasterPlugin.class);
	private static final String QUARTERS_KEY = "quarters";
	/** Ticks between saves while something has changed. */
	private static final int SAVE_TICKS = 50;
	/** Ticks between recounts at today's prices, about a minute. */
	private static final int REPRICE_TICKS = 100;
	private static final int INVENTORY_SLOTS = 28;
	private static final int[] POTION_STORE_VARS = {
		VarPlayerID.POTIONSTORE_BASE_VAR_1, VarPlayerID.POTIONSTORE_BASE_VAR_2, VarPlayerID.POTIONSTORE_BASE_VAR_3,
		VarPlayerID.POTIONSTORE_BASE_VAR_4, VarPlayerID.POTIONSTORE_BASE_VAR_5, VarPlayerID.POTIONSTORE_BASE_VAR_6
	};
	private static final int[] POUCH_RUNES = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] POUCH_AMOUNTS = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
	};
	private static final int[] POUCHES = {
		ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Gson gson;

	@Inject
	private CodexQuartermasterConfig config;

	/** RuneLite's Bank Tags, through which the bank can be shown filtered to what is still to pack. */
	@Inject
	private TagManager tagManager;

	@Inject
	private BankTagsService bankTags;

	@Inject
	private BankSearch bankSearch;

	/** The player's own bank tab open when Show in bank was pressed, or null; client thread only. */
	private String tabBefore;

	/** What is still to pack, as last published; read by Bank Tags on the client thread. */
	private BankView packView = BankView.EMPTY;

	private final SupplyPlaque plaque = new SupplyPlaque();
	private WithdrawOverlay withdrawOverlay;
	/** The kit book as last written, so it is only written again when it changes. */
	private String lastBook;
	private final Map<Integer, String> names = new HashMap<>();
	/** Each shopping item's Grand Exchange buy limit; absent until the client knows it. */
	private final Map<Integer, Integer> limits = new HashMap<>();
	private final Prices prices = id -> itemManager.getItemPrice(id);

	private QuartermasterPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Quarters quarters;
	/** What was carried when last seen, so the panel still counts it while logged out. */
	private Map<Integer, Long> carried = Collections.emptyMap();
	private boolean bankOpen;
	/** The potion storage needs reading on the next tick the bank is open. */
	private boolean readStorage;
	/**
	 * The varps the potion storage keeps its counts in, one per potion, taken from the storage
	 * widget's own triggers as RuneLite's bank tags do. Null until the bank has been seen open.
	 */
	private Set<Integer> storageVars;
	/** Ticks after the bank closes in which a storage change, such as a last withdrawal, is still read. */
	private int storageGrace;
	/** Quiver ammo or rune pouch runes changed, which no item container reports. */
	private boolean recarry;
	private final Warning warning = new Warning();
	private final Reminder reminder = new Reminder();
	/** What was carried when the bank opened, to tell a packing visit from one that only deposits; null when not seen. */
	private Map<Integer, Long> openedWith;
	private boolean dirty;
	private boolean unsaved;
	private int ticks;

	@Provides
	CodexQuartermasterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexQuartermasterConfig.class);
	}

	@Override
	protected void startUp()
	{
		ItemIcons icons = new LoadingIcons(itemManager::getImage);
		panel = new QuartermasterPanel(icons, new PanelActions());
		navButton = NavigationButton.builder()
			.tooltip("Codex Quartermaster")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
		withdrawOverlay = new WithdrawOverlay(itemManager);
		overlayManager.add(withdrawOverlay);
		packView = BankView.EMPTY;
		tagManager.registerTag(BankView.TAG, id -> packView.contains(itemManager.canonicalize(id)));
		clientThread.invokeLater(() ->
		{
			// A client that quit without warning can leave Bank Tags set to open the packing view on
			// the next visit, before the Quartermaster has worked it out: the bank would open empty.
			closeView();
			loadProfile();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				carried = readCarried();
				// Turned on with the bank already open: its opening was missed, but it is open.
				Widget open = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
				bankOpen = open != null && !open.isHidden();
				readStorage = bankOpen;
				ItemContainer bank = client.getItemContainer(InventoryID.BANK);
				if (bank != null && quarters != null)
				{
					quarters.observeBank(contents(bank), System.currentTimeMillis());
					unsaved = true;
				}
			}
			publish();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(plaque);
		overlayManager.remove(withdrawOverlay);
		withdrawOverlay.show(null);
		clientToolbar.removeNavigation(navButton);
		tagManager.unregisterTag(BankView.TAG);
		clientThread.invokeLater(() ->
		{
			// A view left open would show an empty bank with the Quartermaster gone.
			closeView();
			packView = BankView.EMPTY;
			save();
			quarters = null;
			profile = null;
			lastBook = null;
			names.clear();
			limits.clear();
			bankOpen = false;
		});
		panel = null;
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		// ConfigManager writes its changes to disk in this same event, before a save queued on the
		// client thread runs, so a save made here has to be sent on by hand or it is never written.
		CompletableFuture<Void> done = new CompletableFuture<>();
		clientThread.invokeLater(() ->
		{
			try
			{
				// The next start would open the bank on a packing view not worked out yet.
				closeView();
				if (save())
				{
					configManager.sendConfig();
				}
			}
			finally
			{
				done.complete(null);
			}
		});
		event.waitFor(done);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			bankOpen = false;
			storageGrace = 0;
			readStorage = false;
			openedWith = null;
			if (quarters != null)
			{
				// What changes while logged out, on another client, is not what the trip used.
				quarters.voidTrip();
			}
			save();
			publish();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		save();
		if (configManager.getRSProfileKey() == null)
		{
			// Logging out can leave no profile until the next login. The account's quarters stay in
			// hand, saved, so the panel still counts its bank as last seen.
			return;
		}
		loadProfile();
		bankOpen = false;
		storageGrace = 0;
		readStorage = false;
		openedWith = null;
		carried = client.getGameState() == GameState.LOGGED_IN ? readCarried() : Collections.emptyMap();
		publish();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CodexQuartermasterConfig.GROUP.equals(event.getGroup()))
		{
			dirty = true;
		}
		else if (SharedBank.STEWARD_GROUP.equals(event.getGroup()) && SharedBank.STEWARD_KEY.equals(event.getKey()))
		{
			// Codex Bank Steward saved the bank; it only counts for the profile in hand.
			clientThread.invokeLater(() ->
			{
				if (quarters != null && profile != null && profile.equals(configManager.getRSProfileKey()) && adoptSharedBank())
				{
					// No ticks run while logged out, so the panel is redrawn here.
					publish();
				}
			});
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (client.getGameState() != GameState.LOGGED_IN || quarters == null)
		{
			return;
		}
		int id = event.getContainerId();
		if (id == InventoryID.BANK)
		{
			quarters.observeBank(contents(event.getItemContainer()), System.currentTimeMillis());
			unsaved = true;
			dirty = true;
		}
		else if (id == InventoryID.INV || id == InventoryID.WORN)
		{
			carried = readCarried();
			dirty = true;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// The per-potion varps change when doses go in or out; the base vars when doses are rearranged.
		int id = event.getVarpId();
		Set<Integer> vars = storageVars;
		boolean storage = vars != null && vars.contains(id);
		for (int varp : POTION_STORE_VARS)
		{
			storage |= id == varp;
		}
		if (storage && (bankOpen || storageGrace > 0))
		{
			readStorage = true;
		}
		if (id == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO || id == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT)
		{
			recarry = true;
		}
		for (int i = 0; i < POUCH_AMOUNTS.length; i++)
		{
			recarry |= event.getVarbitId() == POUCH_AMOUNTS[i] || event.getVarbitId() == POUCH_RUNES[i];
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != InterfaceID.BANKMAIN || quarters == null)
		{
			return;
		}
		bankOpen = true;
		readStorage = true;
		// The bank has just opened, so what is carried is still what came back from the trip.
		openedWith = readCarried();
		Trip trip = quarters.endTrip(openedWith, System.currentTimeMillis(), prices);
		if (trip != null)
		{
			unsaved = true;
		}
		dirty = true;
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() != InterfaceID.BANKMAIN || !event.isUnload() || quarters == null)
		{
			return;
		}
		bankOpen = false;
		// Bank Tags remembers the open tab for the next visit; the packing view lasts one visit.
		// The event comes before the bank is gone and before Bank Tags' own handler (subscribers
		// run in class-name order), so closing now would lay out a bank that is closing: wait
		// until it has.
		clientThread.invokeLater(this::closeView);
		if (!setsOut(client.getGameState()))
		{
			// The bank closes as the game logs out or loses its connection: that is no trip.
			openedWith = null;
			dirty = true;
			return;
		}
		// A withdrawal made just before closing reaches the storage varps a tick later.
		storageGrace = 2;
		carried = readCarried();
		if (config.recognise() && quarters.recognise(carried) != null)
		{
			unsaved = true;
		}
		quarters.startTrip(carried, System.currentTimeMillis());
		warn();
		remind();
		openedWith = null;
		dirty = true;
	}

	/**
	 * Whether the bank closing in this state sets out on a trip: in play, or hopping or loading a
	 * new area on the way. Logging out or losing the connection closes it too, and that is no trip.
	 */
	static boolean setsOut(GameState state)
	{
		return state == GameState.LOGGED_IN || state == GameState.HOPPING || state == GameState.LOADING;
	}

	/**
	 * Closes the bank's filtered view if it is the Quartermaster's, open now or remembered by Bank
	 * Tags for the next visit. Bank Tags forgets the open view once the bank has closed but
	 * remembers its name for the next visit, so both are checked. The player's own tab from before
	 * Show in bank comes back in its place: opened again if the bank is still open, else remembered
	 * for the next visit. Client thread only.
	 */
	private void closeView()
	{
		String remembered = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BankView.REMEMBERED_TAB_KEY);
		String back = BankView.tabAfterClose(tabBefore);
		// Once the view is closed, or the player has moved on to a tab of their own, there is
		// nothing more to give back.
		tabBefore = null;
		if (!BankView.shouldClose(bankTags.getActiveTag(), remembered))
		{
			return;
		}
		if (!back.isEmpty() && client.getWidget(InterfaceID.Bankmain.UNIVERSE) != null)
		{
			// As if the player pressed their own tab again.
			bankTags.openBankTag(back, BankTagsService.OPTION_ALLOW_MODIFICATIONS);
			return;
		}
		bankTags.closeBankTag();
		if (!back.isEmpty())
		{
			configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BankView.REMEMBERED_TAB_KEY, back);
		}
	}

	/** Shows the bank filtered to what is still to pack, or says in the chat box why it cannot. */
	private void showInBank()
	{
		// Bank Tags is on unless turned off, the way RuneLite itself decides it.
		boolean tagsOn = !"false".equals(configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, "banktagsplugin"));
		BankView.Refusal refusal = packView.refusal(bankOpen && client.getGameState() == GameState.LOGGED_IN, tagsOn);
		if (refusal != null)
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(new ChatMessageBuilder()
					.append(ChatColorType.HIGHLIGHT).append("Codex Quartermaster: ")
					.append(ChatColorType.NORMAL).append(BankView.explain(refusal))
					.build())
				.build());
			return;
		}
		String open = bankTags.getActiveTag();
		if (!BankView.TAG.equals(open))
		{
			tabBefore = open;
		}
		bankTags.openBankTag(BankView.TAG, BankTagsService.OPTION_NO_LAYOUT);
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (quarters != null && event.getActor() == client.getLocalPlayer())
		{
			// What is lost on death is not what the trip used.
			quarters.voidTrip();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (quarters == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		ticks++;
		checkIron();
		if (recarry)
		{
			recarry = false;
			carried = readCarried();
			dirty = true;
		}
		if (readStorage && (bankOpen || storageGrace > 0))
		{
			readStorage = false;
			readPotionStorage();
		}
		if (storageGrace > 0 && !bankOpen)
		{
			storageGrace--;
		}
		if (unsaved && ticks % SAVE_TICKS == 0)
		{
			save();
		}
		if (dirty || ticks % REPRICE_TICKS == 0)
		{
			publish();
		}
	}

	/** When setting out with the chosen kit packed and few trips left, says what runs out and what restocking costs. */
	private void warn()
	{
		Kit kit = quarters.activeKit();
		if (!quarters.hasBank() || Quarters.packed(kit, carried) < 0.5)
		{
			return;
		}
		Plan plan = quarters.plan(kit, carried, prices, config.measured());
		Plan.Line first = plan.limiting();
		if (first == null)
		{
			return;
		}
		String text = warning.check(kit.getId(), kit.getName(), plan.trips(), config.warnTrips(),
			supplyName(first).toLowerCase(Locale.ENGLISH), System.currentTimeMillis());
		if (text == null)
		{
			return;
		}
		ChatMessageBuilder line = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT).append("Codex Quartermaster: ")
			.append(ChatColorType.NORMAL).append(text);
		if (plan.restockCost() > 0 && !ironman())
		{
			line.append(" Restocking to " + plan.getTarget() + " trips costs " + Format.coins(plan.restockCost()) + ".");
		}
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(line.build())
			.build());
	}

	/**
	 * When leaving the bank with the chosen kit mostly packed but a supply not packed at all, such
	 * as the spade for Barrows or the anti-venom for Zulrah, says what was left behind.
	 */
	private void remind()
	{
		Kit kit = quarters.activeKit();
		// A preset's packing list is a guess at a typical trip, so only a kit the player packed and
		// saved themselves is held to it out loud.
		if (!config.packCheck() || !quarters.hasBank() || kit.isPreset())
		{
			return;
		}
		// A visit that only deposited loot, or took nothing for this kit, was not packing it.
		if (openedWith == null || !Packing.withdrewFor(kit, openedWith, carried))
		{
			return;
		}
		Packing packing = Packing.check(kit, carried, quarters.getBank(), quarters.getStored(), quarters.usage(kit.getId()));
		List<String> forgotten = new ArrayList<>();
		if (packing.isUnderway())
		{
			for (Packing.Item item : packing.forgotten())
			{
				forgotten.add(supplyName(item.getSupply()).toLowerCase(Locale.ENGLISH));
			}
		}
		String text = reminder.check(kit.getName(), forgotten, System.currentTimeMillis());
		if (text == null)
		{
			return;
		}
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Codex Quartermaster: ")
				.append(ChatColorType.NORMAL).append(text)
				.build())
			.build());
	}

	/** Iron accounts cannot use the Grand Exchange, so restocking is gathered, not bought. */
	private boolean ironman()
	{
		return quarters != null && quarters.isIron();
	}

	/** Notes whether the account is an iron one, which is only known while logged in. */
	private void checkIron()
	{
		boolean iron = client.getVarbitValue(VarbitID.IRONMAN) > 0;
		if (quarters != null && quarters.isIron() != iron)
		{
			quarters.setIron(iron);
			unsaved = true;
			dirty = true;
		}
	}

	/**
	 * Reads the doses kept in the bank's potion storage, which the bank itself does not list.
	 * The game's own script says how many doses each stored potion has; they are filed under the
	 * potion's 1-dose item so they pool with the rest. Only called on the client thread with the
	 * bank open.
	 */
	private void readPotionStorage()
	{
		if (quarters == null)
		{
			return;
		}
		EnumComposition potions = client.getEnum(EnumID.POTIONSTORE_POTIONS);
		if (potions == null)
		{
			return;
		}
		if (storageVars == null)
		{
			Widget w = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
			int[] trigger = w == null ? null : w.getVarTransmitTrigger();
			if (trigger != null)
			{
				Set<Integer> vars = new HashSet<>();
				for (int v : trigger)
				{
					vars.add(v);
				}
				storageVars = vars;
			}
		}
		Map<Integer, Long> doses = new HashMap<>();
		for (int potionEnum : potions.getIntVals())
		{
			EnumComposition potion = client.getEnum(potionEnum);
			if (potion == null)
			{
				continue;
			}
			client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnum);
			int stored = client.getIntStack()[0];
			int oneDose = potion.getIntValue(1);
			if (stored > 0 && oneDose > 0)
			{
				doses.merge(oneDose, (long) stored, Long::sum);
			}
		}
		if (!doses.equals(quarters.getStored()))
		{
			quarters.observeStorage(doses);
			unsaved = true;
			dirty = true;
		}
	}

	private Map<Integer, Long> contents(ItemContainer container)
	{
		Map<Integer, Long> items = new HashMap<>();
		if (container == null)
		{
			return items;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			items.merge(itemManager.canonicalize(item.getId()), (long) item.getQuantity(), Long::sum);
		}
		return items;
	}

	/** The inventory, worn gear and the runes in a carried rune pouch. */
	private Map<Integer, Long> readCarried()
	{
		Map<Integer, Long> items = contents(client.getItemContainer(InventoryID.INV));
		contents(client.getItemContainer(InventoryID.WORN)).forEach((id, q) -> items.merge(id, q, Long::sum));
		addQuiverAmmo(items);
		boolean pouch = false;
		for (int id : POUCHES)
		{
			pouch |= items.containsKey(id);
		}
		if (pouch)
		{
			EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
			for (int i = 0; i < POUCH_RUNES.length; i++)
			{
				int type = client.getVarbitValue(POUCH_RUNES[i]);
				int amount = client.getVarbitValue(POUCH_AMOUNTS[i]);
				int itemId = type > 0 ? runes.getIntValue(type) : -1;
				if (itemId > 0 && amount > 0)
				{
					items.merge(itemId, (long) amount, Long::sum);
				}
			}
		}
		return items;
	}

	/**
	 * Dizana's quiver holds a second stack of ammo outside the worn items. It goes where the quiver
	 * goes, so it is carried when the quiver is.
	 */
	private void addQuiverAmmo(Map<Integer, Long> items)
	{
		int ammo = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		int amount = client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);
		if (ammo <= 0 || amount <= 0)
		{
			return;
		}
		for (int id : items.keySet())
		{
			if (itemManager.getItemComposition(id).getIntValue(ParamID.QUIVER_AMMO_AVAILABLE) == 1)
			{
				items.merge(itemManager.canonicalize(ammo), (long) amount, Long::sum);
				return;
			}
		}
	}

	/**
	 * Whether a carried item is a supply a saved kit should count. Stackables are, worn gear and
	 * other equipment are not, and anything else is when it can be bought again.
	 */
	private boolean isSupply(int itemId)
	{
		ItemComposition item = itemManager.getItemComposition(itemId);
		if (item.isStackable())
		{
			return true;
		}
		ItemStats stats = itemManager.getItemStats(itemId);
		if (stats != null && stats.isEquipable())
		{
			return false;
		}
		return item.isTradeable();
	}

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		quarters = null;
		if (profile == null)
		{
			return;
		}
		try
		{
			String saved = configManager.getRSProfileConfiguration(CodexQuartermasterConfig.GROUP, QUARTERS_KEY);
			if (saved != null && !saved.isEmpty())
			{
				Quarters read = gson.fromJson(saved, Quarters.class);
				quarters = read == null ? null : read.repair();
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved quarters, starting afresh", e);
		}
		if (quarters == null)
		{
			quarters = new Quarters();
		}
		lastBook = configManager.getRSProfileConfiguration(CodexQuartermasterConfig.GROUP, KitBook.KEY);
		// Saves from before the kit book existed get one at the next save.
		unsaved = lastBook == null;
		adoptSharedBank();
		dirty = true;
	}

	/**
	 * Takes the bank Codex Bank Steward saved when it is newer than the one seen here, as when
	 * the Quartermaster was just installed. Gear Roadmap's saved bank, the suite's fallback, holds
	 * gear and no supplies, so counting from it would claim an empty bank: it is not used.
	 *
	 * @return whether the bank changed
	 */
	private boolean adoptSharedBank()
	{
		if (bankOpen)
		{
			return false;
		}
		SharedBank shared = SharedBank.read(gson,
			configManager.getRSProfileConfiguration(SharedBank.STEWARD_GROUP, SharedBank.STEWARD_KEY),
			configManager.getRSProfileConfiguration(SharedBank.ROADMAP_GROUP, SharedBank.ROADMAP_KEY));
		if (quarters.adoptBank(shared, "Codex Bank Steward"))
		{
			unsaved = true;
			dirty = true;
			return true;
		}
		return false;
	}

	/** Writes to the profile if anything changed, and says whether it did. */
	private boolean save()
	{
		if (profile == null || quarters == null || !unsaved)
		{
			return false;
		}
		unsaved = false;
		configManager.setConfiguration(CodexQuartermasterConfig.GROUP, profile, QUARTERS_KEY, gson.toJson(quarters));
		// The kits in the shape other Codex plugins read; written only when it changes.
		String book = gson.toJson(KitBook.of(quarters, carried, this::name, config.measured()));
		if (!book.equals(lastBook))
		{
			configManager.setConfiguration(CodexQuartermasterConfig.GROUP, profile, KitBook.KEY, book);
			lastBook = book;
		}
		return true;
	}

	/** Works out every kit's plan and hands the panel and the plaque what they show. */
	private void publish()
	{
		dirty = false;
		QuartermasterPanel target = panel;
		long now = System.currentTimeMillis();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		if (quarters == null)
		{
			plaque.show(null, null, null);
			withdrawOverlay.show(null);
			if (target != null)
			{
				SwingUtilities.invokeLater(() -> target.update(View.loggedOut()));
			}
			return;
		}
		boolean measured = config.measured();
		Kit active = quarters.activeKit();
		List<Plan> plans = new ArrayList<>();
		Plan chosen = null;
		for (Kit kit : quarters.allKits())
		{
			Plan plan = quarters.plan(kit, carried, prices, measured);
			plans.add(plan);
			if (kit == active)
			{
				chosen = plan;
			}
			for (Plan.Line line : plan.getLines())
			{
				name(line.getSupply().getIcon());
				line.getHeld().keySet().forEach(this::name);
				line.getBuys().forEach(b -> named(b.getItemId()));
			}
		}
		Usage usage = quarters.usage(active.getId());
		Packing packing = quarters.hasBank() ? Packing.check(active, carried, quarters.getBank(), quarters.getStored(), usage) : null;
		if (packing != null)
		{
			for (Packing.Item item : packing.getItems())
			{
				name(item.getSupply().getIcon());
				item.getWithdraw().keySet().forEach(this::name);
			}
		}
		boolean packAtBank = loggedIn && bankOpen && packing != null && !packing.getItems().isEmpty() && config.packCheck();
		withdrawOverlay.show(loggedIn && bankOpen && packing != null && config.highlightBank() && packingNow(packing)
			? packing.withdrawals() : null);
		BankView before = packView;
		packView = loggedIn && bankOpen ? BankView.of(packing) : BankView.EMPTY;
		if (packView.isEmpty())
		{
			// Packed: the whole bank comes back.
			closeView();
		}
		else if (!packView.equals(before) && BankView.TAG.equals(bankTags.getActiveTag()))
		{
			// The bank laid itself out before this heard of the withdrawal: lay the view out again
			// so a supply packed in full leaves it. Only a layout: opening the tag again would close
			// the chat box, and with it a Withdraw-X amount the player is typing for the next supply.
			bankSearch.layoutBank();
		}
		List<Trip> trips = usage == null ? Collections.emptyList() : new ArrayList<>(usage.getRecent());
		// Every saved kit restocked together, when there are several to restock.
		Plan all = null;
		if (quarters.restockKits().size() >= 2)
		{
			all = quarters.planAll(carried, prices, measured);
			for (Plan.Line line : all.getLines())
			{
				name(line.getSupply().getIcon());
				line.getBuys().forEach(b -> named(b.getItemId()));
			}
		}
		Map<String, Returns> returns = new HashMap<>();
		for (Kit k : quarters.allKits())
		{
			Usage u = quarters.usage(k.getId());
			if (u != null)
			{
				returns.put(k.getId(), Returns.of(u.getRecent()));
			}
		}
		List<Tuning.Advice> tuning = Tuning.of(active, usage, carried, quarters.getBank());
		tuning.forEach(a -> name(a.getItem()));
		Player player = client.getLocalPlayer();
		View.State state = !quarters.hasBank() ? View.State.WAITING_FOR_BANK : View.State.READY;
		View view = new View(state, loggedIn && player != null ? player.getName() : null, chosen, plans, trips,
			usage == null ? 0 : usage.getTrips(), quarters.getBankSeen(), bankOpen, new HashMap<>(names), config.warnTrips(), now)
			.bankFrom(quarters.getBankFrom())
			.all(all, active.isPreset() && active.getId().equals(quarters.chosenKitId()))
			.tuning(tuning)
			.returns(returns)
			.limits(new HashMap<>(limits))
			.ironman(ironman())
			.packing(packAtBank ? packing : null, usage == null ? 0 : usage.typicalDuration());

		CodexQuartermasterConfig.PlaqueMode mode = config.plaque();
		boolean showPlaque = loggedIn && quarters.hasBank() && chosen != null
			&& (mode == CodexQuartermasterConfig.PlaqueMode.ALWAYS || mode == CodexQuartermasterConfig.PlaqueMode.AT_THE_BANK && bankOpen);
		if (showPlaque)
		{
			int n = chosen.trips();
			Plan.Line first = chosen.limiting();
			String figure = n == Plan.UNLIMITED ? "Ample" : n + (n == 1 ? " trip" : " trips");
			String note = active.getName();
			String tail = first == null ? null : n == 0 ? "out of " + supplyName(first).toLowerCase(Locale.ENGLISH)
				: supplyName(first) + " first";
			List<Packing.Item> missing = packAtBank && packing.isUnderway() ? packing.missing() : Collections.emptyList();
			if (!missing.isEmpty())
			{
				// At the bank, mid-pack, what is still to withdraw matters more than the kit's name.
				note = "To pack: " + supplyName(missing.get(0).getSupply()) + (missing.size() > 1 ? " and " + (missing.size() - 1) + " more" : "");
				tail = null;
			}
			plaque.show(figure, note, tail, n == Plan.UNLIMITED ? Theme.GOLD : Theme.trips(n, config.warnTrips()));
		}
		else
		{
			plaque.show(null, null, null);
		}
		if (target != null)
		{
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	/**
	 * Whether the bank is worth marking for the packing check: the kit is part-packed, or the
	 * inventory has room for what is still to take. A visit to drop off a full inventory of loot
	 * is left unmarked until there is room. Client thread only.
	 */
	private boolean packingNow(Packing packing)
	{
		Map<Integer, Long> withdrawals = packing.withdrawals();
		if (withdrawals.isEmpty())
		{
			return false;
		}
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		Set<Integer> held = new HashSet<>();
		int used = 0;
		if (inv != null)
		{
			for (Item item : inv.getItems())
			{
				if (item.getId() >= 0 && item.getQuantity() > 0)
				{
					used++;
					held.add(item.getId());
				}
			}
		}
		long need = Packing.slotsNeeded(withdrawals, held, id -> itemManager.getItemComposition(id).isStackable());
		return packing.worthMarking(INVENTORY_SLOTS - used, need);
	}

	private String supplyName(Plan.Line line)
	{
		return supplyName(line.getSupply());
	}

	private String supplyName(com.codex.quartermaster.core.Supply supply)
	{
		return supply.getName() != null ? supply.getName() : name(supply.getIcon());
	}

	private String name(int itemId)
	{
		return names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName());
	}

	/** Names a shopping item and notes its Grand Exchange buy limit, read once per item. */
	private void named(int itemId)
	{
		name(itemId);
		if (!limits.containsKey(itemId))
		{
			// Item stats load in the background at start-up: an item asked about before they are
			// in is asked again next time, rather than remembered as having no limit.
			ItemStats stats = itemManager.getItemStats(itemId);
			if (stats != null && stats.getGeLimit() > 0)
			{
				limits.put(itemId, stats.getGeLimit());
			}
		}
	}

	/** The panel's buttons; each change is made on the client thread, where the quarters live. */
	private final class PanelActions implements Actions
	{
		@Override
		public void choose(String kitId)
		{
			clientThread.invokeLater(() ->
			{
				if (quarters != null)
				{
					quarters.select(kitId);
					unsaved = true;
					publish();
				}
			});
		}

		@Override
		public void target(int trips)
		{
			clientThread.invokeLater(() ->
			{
				if (quarters != null)
				{
					quarters.setTarget(trips);
					unsaved = true;
					publish();
				}
			});
		}

		@Override
		public void capture()
		{
			Quarters q = quarters;
			if (q == null || client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}
			String suggested = "My kit " + (q.getSavedKits().size() + 1);
			Object answer = JOptionPane.showInputDialog(panel, "Name this kit, such as the boss it is for:", "Save a kit",
				JOptionPane.PLAIN_MESSAGE, null, null, suggested);
			if (answer == null || answer.toString().trim().isEmpty())
			{
				return;
			}
			String name = answer.toString().trim();
			clientThread.invokeLater(() ->
			{
				if (quarters == null)
				{
					return;
				}
				carried = readCarried();
				Kit kit = quarters.capture(Quarters.fitName(name), carried,
					CodexQuartermasterPlugin.this::isSupply, id -> itemManager.getItemComposition(id).isStackable());
				if (kit == null)
				{
					chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(new ChatMessageBuilder()
							.append(ChatColorType.HIGHLIGHT).append("Codex Quartermaster: ")
							.append(ChatColorType.NORMAL).append("nothing you are carrying is a supply. Withdraw your food, potions and the like, then save the kit.")
							.build())
						.build());
					return;
				}
				unsaved = true;
				save();
				publish();
			});
		}

		@Override
		public void rename(String kitId, String current)
		{
			Object answer = JOptionPane.showInputDialog(panel, "A new name for this kit:", "Rename kit",
				JOptionPane.PLAIN_MESSAGE, null, null, current);
			if (answer == null || answer.toString().trim().isEmpty())
			{
				return;
			}
			String name = answer.toString();
			clientThread.invokeLater(() ->
			{
				if (quarters != null && quarters.rename(kitId, name))
				{
					unsaved = true;
					save();
					publish();
				}
			});
		}

		@Override
		public void amount(String kitId, String supply, String label, int current)
		{
			Object answer = JOptionPane.showInputDialog(panel, "<html>" + label + " a trip takes.<br>Enter 0 when it comes back every trip.</html>",
				"Set the amount", JOptionPane.PLAIN_MESSAGE, null, null, String.valueOf(current));
			if (answer == null)
			{
				return;
			}
			int amount;
			try
			{
				amount = Integer.parseInt(answer.toString().trim().replace(",", ""));
			}
			catch (NumberFormatException e)
			{
				return;
			}
			clientThread.invokeLater(() ->
			{
				if (quarters != null && quarters.setAmount(kitId, supply, amount))
				{
					unsaved = true;
					save();
					publish();
				}
			});
		}

		@Override
		public void showInBank()
		{
			clientThread.invokeLater(CodexQuartermasterPlugin.this::showInBank);
		}

		@Override
		public void loadout(String kitId, Consumer<Loadout> then)
		{
			clientThread.invokeLater(() ->
			{
				Kit kit = quarters == null ? null : quarters.kit(kitId);
				if (kit == null)
				{
					return;
				}
				// Stackability is the item's own, which only the client thread may ask.
				Loadout loadout = Loadout.of(kit, quarters.getBank(), id -> itemManager.getItemComposition(id).isStackable());
				SwingUtilities.invokeLater(() -> then.accept(loadout));
			});
		}

		@Override
		public void tune(String kitId)
		{
			clientThread.invokeLater(() ->
			{
				if (quarters == null)
				{
					return;
				}
				Kit kit = quarters.kit(kitId);
				// Worked out afresh, so the amounts packed are what the panel showed or newer.
				if (kit != null && quarters.tune(kitId, Tuning.of(kit, quarters.usage(kitId), carried, quarters.getBank())))
				{
					unsaved = true;
					save();
					publish();
				}
			});
		}

		@Override
		public void drop(String kitId, String supply)
		{
			clientThread.invokeLater(() ->
			{
				if (quarters != null && quarters.drop(kitId, supply))
				{
					unsaved = true;
					save();
					publish();
				}
			});
		}

		@Override
		public void forget(String kitId)
		{
			int ok = JOptionPane.showConfirmDialog(panel, "Forget this kit and the trips measured with it?", "Forget kit",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (ok != JOptionPane.OK_OPTION)
			{
				return;
			}
			clientThread.invokeLater(() ->
			{
				if (quarters != null && quarters.forget(kitId))
				{
					unsaved = true;
					save();
					publish();
				}
			});
		}
	}
}
