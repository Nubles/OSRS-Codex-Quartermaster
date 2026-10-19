package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Packing;
import com.codex.quartermaster.core.Plan;
import com.codex.quartermaster.core.Supplies;
import com.codex.quartermaster.core.Supply;
import com.codex.quartermaster.core.Trip;
import com.codex.quartermaster.core.Usage;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Quartermaster side panel: the crest, then three pages. Supplies is the chosen kit against
 * the bank, Restock is the shopping list to reach a number of trips, and Kits is every kit at a
 * glance, where a new one is saved from what is carried.
 */
public class QuartermasterPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"supplies", "restock", "kits"};
	private static final int[] TARGETS = {5, 10, 25, 50, 100};
	private static final int RECENT = 5;
	private static final Map<String, String> SHORT = new HashMap<>();

	static
	{
		SHORT.put("FOOD", "hp");
		SHORT.put("BREW", "brew");
		SHORT.put("PRAYER", "prayer");
		SHORT.put("RANGING", "ranging");
		SHORT.put("MAGIC", "magic");
		SHORT.put("COMBAT", "combat");
		SHORT.put("SUPER_ANTIFIRE", "antifire");
		SHORT.put("ANTIFIRE", "antifire");
		SHORT.put("ANTIVENOM", "anti-venom");
		SHORT.put("STAMINA", "stamina");
		SHORT.put("DRAGON_BOLTS", "bolts");
	}

	private final ItemIcons icons;
	private final Actions actions;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Supplies", "Restock", "Kits");
	private final Segmented target = new Segmented(false, "5", "10", "25", "50", "100");
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane supplies = scroller();
	private final JScrollPane restock = scroller();
	private final JScrollPane kits = scroller();

	private View view = View.loggedOut();
	/** Which pages still show an older view; a page is only rebuilt when it is looked at. */
	private final boolean[] stale = {true, true, true};

	public QuartermasterPanel(ItemIcons icons, Actions actions)
	{
		super(false);
		this.icons = icons;
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		content.add(supplies, TABS[0]);
		content.add(restock, TABS[1]);
		content.add(kits, TABS[2]);
		add(content, BorderLayout.CENTER);

		tabs.onChange(this::showTab);
		target.setSelected(1, false);
		target.onChange(i -> actions.target(TARGETS[i]));
		refresh(true);
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		if (view.active != null)
		{
			for (int i = 0; i < TARGETS.length; i++)
			{
				if (TARGETS[i] == view.active.getTarget())
				{
					target.setSelected(i, false);
				}
			}
		}
		updateCrest();
		java.util.Arrays.fill(stale, true);
		refresh(false);
	}

	/** Opens one tab. */
	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
		refresh(true);
	}

	@Override
	public void onActivate()
	{
		refresh(true);
	}

	/**
	 * Rebuilds the page on show if it is out of date. Eating a shark changes the view, so while the
	 * panel is closed nothing is rebuilt at all, and while it is open only the page in view is.
	 */
	private void refresh(boolean force)
	{
		if (!force && !isShowing())
		{
			return;
		}
		int page = tabs.getSelected();
		if (!stale[page])
		{
			return;
		}
		stale[page] = false;
		if (page == 0)
		{
			rebuildSupplies();
		}
		else if (page == 1)
		{
			rebuildRestock();
		}
		else
		{
			rebuildKits();
		}
	}

	private void updateCrest()
	{
		Plan plan = view.active;
		if (plan == null)
		{
			crest.update("QUARTERMASTER", null, false, "Log in to begin", null, Theme.TEXT_MUTED, Theme.GOLD);
			return;
		}
		String heading = plan.getKit().getName().toUpperCase(Locale.ENGLISH);
		if (view.state == View.State.WAITING_FOR_BANK)
		{
			crest.update(heading, null, false, "Open your bank to begin", null, Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.3), Theme.GOLD);
			return;
		}
		int trips = plan.trips();
		Plan.Line first = plan.limiting();
		if (first == null)
		{
			crest.update(heading, trips, true, "Nothing in this kit is used up", "Nothing is used up", Theme.TEXT_MUTED, Theme.GOLD);
			return;
		}
		Color color = Theme.trips(trips, view.warn);
		String supply = view.name(first);
		String status = trips == 0 ? "Out of " + supply.toLowerCase(Locale.ENGLISH) : supply + " runs out first";
		String shortStatus = trips == 0 ? "Out of " + supply.toLowerCase(Locale.ENGLISH) : supply + " first";
		crest.update(heading, trips, false, status, shortStatus, trips <= view.warn ? color : Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.35),
			trips <= view.warn ? color : Theme.GOLD, plan.getTarget());
	}

	private boolean waiting(StackPanel stack)
	{
		if (view.active == null)
		{
			stack.add(note("The stores are closed", "Log in and Codex Quartermaster counts how many trips your bank supports for the content you choose, what runs out first, and what restocking costs at Grand Exchange prices."));
			return true;
		}
		if (view.state == View.State.WAITING_FOR_BANK)
		{
			stack.add(note("Open your bank once", "The Quartermaster counts from your bank. After the first look it remembers the bank as last seen, so the panel works anywhere."));
			return true;
		}
		return false;
	}

	// The Supplies tab.

	private void rebuildSupplies()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (waiting(stack))
		{
			replace(supplies, stack);
			return;
		}
		Plan plan = view.active;
		if (view.packing != null)
		{
			addPacking(stack, view.packing);
		}
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(Format.coins(plan.tripCost()), "Per trip", view.ironman ? "GE value" : "GE prices", Theme.GOLD));
		boolean measured = view.measured >= Usage.TRUSTED;
		int left = plan.trips();
		if (measured && view.tripMs > 0 && left != Plan.UNLIMITED)
		{
			// How much play the bank holds, at the pace the player really goes.
			tiles.add(new StatTile(left == 0 ? "-" : Format.span(left * view.tripMs), "Lasts",
				"at " + Format.duration(view.tripMs) + " a trip", left == 0 ? Theme.TEXT_MUTED : Theme.TEAL));
		}
		else
		{
			tiles.add(new StatTile(measured ? String.valueOf(view.measured) : "-", "Measured",
				measured ? "trips learned" : view.measured == 1 ? "1 trip so far" : "make a trip", measured ? Theme.TEAL : Theme.TEXT_MUTED));
		}
		stack.add(tiles);

		stack.add(new SectionHeader("Supplies", plan.getKit().getName()));
		Card list = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(2, 0);
		List<Plan.Line> lines = new ArrayList<>(plan.getLines());
		lines.sort((a, b) -> Integer.compare(a.getTrips(), b.getTrips()));
		Plan.Line first = plan.limiting();
		boolean editable = !plan.getKit().isPreset() && plan.getKit().getNeeds().size() > 1;
		for (Plan.Line line : lines)
		{
			SupplyRow row = new SupplyRow(line, view, plan.getTarget(), line == first, icons, editable);
			if (editable)
			{
				String kitId = plan.getKit().getId();
				String key = line.getSupply().getKey();
				JPopupMenu menu = new JPopupMenu();
				JMenuItem remove = new JMenuItem("Remove " + view.name(line) + " from this kit");
				remove.addActionListener(e -> actions.drop(kitId, key));
				menu.add(remove);
				row.setComponentPopupMenu(menu);
			}
			rows.add(row);
		}
		list.add(rows, BorderLayout.CENTER);
		stack.add(list);
		String foot = measured
			? "* measured from your own " + view.measured + " trips"
			: plan.getKit().isPreset() ? "Typical amounts until your own trips are measured" : "As packed until your own trips are measured";
		stack.add(Ui.wrapped(foot, Theme.small(), Theme.TEXT_FAINT, WIDTH));

		if (!view.trips.isEmpty())
		{
			long sum = 0;
			for (Trip t : view.trips)
			{
				sum += t.getCost();
			}
			stack.add(new SectionHeader("Recent trips", "avg " + Format.coins(sum / view.trips.size())));
			Card recent = new Card(new BorderLayout());
			StackPanel tripRows = new StackPanel(4, 0);
			for (int i = view.trips.size() - 1; i >= 0 && i >= view.trips.size() - RECENT; i--)
			{
				Trip t = view.trips.get(i);
				tripRows.add(new TripRow(t, used(t)));
			}
			recent.add(tripRows, BorderLayout.CENTER);
			stack.add(recent);
		}
		if (!view.bankOpen && view.bankSeen > 0)
		{
			stack.add(Ui.label("Bank as last seen " + Format.ago(view.now - view.bankSeen), Theme.small(), Theme.TEXT_FAINT));
		}
		replace(supplies, stack);
	}

	/**
	 * At the bank, the chosen kit against what is carried: what is still to withdraw and how many
	 * of which bank item, or a quiet line when everything is in.
	 */
	private void addPacking(StackPanel stack, Packing packing)
	{
		int total = packing.getItems().size();
		int packed = packing.packedCount();
		stack.add(new SectionHeader("Packing", packed + " of " + total + " packed"));
		if (packing.isComplete())
		{
			Card done = new Card(new BorderLayout(0, 2));
			done.accent(Theme.GREEN);
			done.add(Ui.label("Packed for " + packing.getKit().getName(), Theme.body(), Theme.GREEN), BorderLayout.NORTH);
			done.add(Ui.wrapped("Everything one trip takes is in your inventory.", Theme.small(), Theme.TEXT_MUTED, WIDTH - 12),
				BorderLayout.CENTER);
			stack.add(done);
			return;
		}
		Card card = new Card(new BorderLayout());
		card.accent(packing.isUnderway() ? Theme.EMBER : Theme.GOLD_DIM);
		StackPanel rows = new StackPanel(2, 0);
		for (Packing.Item item : packing.missing())
		{
			// The item to take leads, since that is what to look for in the bank.
			Map.Entry<Integer, Long> take = item.getWithdraw().isEmpty() ? null : item.getWithdraw().entrySet().iterator().next();
			int icon = take == null ? item.getSupply().getIcon() : take.getKey();
			String name = take == null ? view.name(item.getSupply()) : view.name(take.getKey());
			String amount = take == null ? "-" : "x" + Format.amount(take.getValue());
			rows.add(new BuyRow(icon, name, packNote(item), amount, item.isForgotten() ? Theme.BLOOD : Theme.EMBER, icons));
		}
		card.add(rows, BorderLayout.CENTER);
		stack.add(card);
	}

	/** What a packing row is short of, and anything else to take for it: "8 doses short", "then 2 Lobster". */
	private String packNote(Packing.Item item)
	{
		if (item.getWithdraw().isEmpty())
		{
			return "none in your bank";
		}
		if (item.getWithdraw().size() > 1)
		{
			Iterator<Map.Entry<Integer, Long>> it = item.getWithdraw().entrySet().iterator();
			it.next();
			Map.Entry<Integer, Long> then = it.next();
			return "then " + Format.amount(then.getValue()) + " " + view.name(then.getKey());
		}
		if (item.isTool())
		{
			return item.isForgotten() ? "not packed; it comes back" : "comes back every trip";
		}
		return item.isForgotten() ? "not packed" : Format.amount(item.getMissing()) + View.unit(item.getSupply()) + " short";
	}

	private String used(Trip trip)
	{
		StringBuilder text = new StringBuilder();
		List<Map.Entry<String, Long>> entries = new ArrayList<>(trip.getUsed().entrySet());
		// Pooled supplies first, they say the most in the least room.
		entries.sort((a, b) -> Boolean.compare(!SHORT.containsKey(a.getKey()), !SHORT.containsKey(b.getKey())));
		for (Map.Entry<String, Long> e : entries)
		{
			if (e.getValue() <= 0)
			{
				continue;
			}
			if (text.length() > 0)
			{
				text.append(", ");
			}
			String label = SHORT.get(e.getKey());
			if (label == null)
			{
				Supply s = Supplies.get(e.getKey());
				label = s == null ? "" : view.name(s.getIcon());
			}
			text.append(Format.amount(e.getValue())).append(' ').append(label);
		}
		return text.length() == 0 ? "nothing used" : text.toString();
	}

	// The Restock tab.

	private void rebuildRestock()
	{
		StackPanel stack = new StackPanel(8, 8);
		stack.add(new SectionHeader("Restock to", "trips"));
		stack.add(target);
		if (waiting(stack))
		{
			replace(restock, stack);
			return;
		}
		Plan plan = view.active;
		int trips = plan.trips();
		List<Plan.Line> missing = new ArrayList<>();
		for (Plan.Line line : plan.getLines())
		{
			if (line.getShort() > 0)
			{
				missing.add(line);
			}
		}
		// What runs out soonest first.
		missing.sort((a, b) -> Integer.compare(a.getTrips(), b.getTrips()));
		List<Plan.Buy> shopping = view.ironman ? java.util.Collections.emptyList() : plan.shopping();
		List<Plan.Line> unbuyable = view.ironman ? missing : plan.unbuyable();

		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		if (view.ironman)
		{
			tiles.add(new StatTile(missing.isEmpty() ? "Nothing" : String.valueOf(missing.size()), "To gather",
				missing.isEmpty() ? "already stocked" : missing.size() == 1 ? "supply short" : "supplies short", Theme.GOLD));
		}
		else
		{
			tiles.add(new StatTile(shopping.isEmpty() ? "Nothing" : Format.coins(plan.restockCost()), "Restock",
				shopping.isEmpty() ? "already stocked" : shopping.size() + (shopping.size() == 1 ? " item" : " items"), Theme.GOLD));
		}
		String after = view.ironman ? "once gathered" : unbuyable.isEmpty() ? "after buying" : "some not sold";
		tiles.add(new StatTile(trips == Plan.UNLIMITED ? "-" : Format.trips(trips) + " to " + Math.max(trips, plan.getTarget()),
			"Trips", after, Theme.trips(Math.min(trips, plan.getTarget()), view.warn)));
		stack.add(tiles);

		if (missing.isEmpty())
		{
			stack.add(note("Stocked for " + plan.getTarget() + " trips", "Your bank already holds enough for "
				+ plan.getTarget() + " trips of " + plan.getKit().getName() + ". Pick a higher target to plan a bigger restock."));
			replace(restock, stack);
			return;
		}
		if (!shopping.isEmpty())
		{
			stack.add(new SectionHeader("Shopping list", Format.coins(plan.restockCost())));
			Card list = new Card(new BorderLayout());
			StackPanel rows = new StackPanel(2, 0);
			for (Plan.Buy buy : shopping)
			{
				rows.add(new BuyRow(buy.getItemId(), view.name(buy.getItemId()),
					String.format(Locale.ENGLISH, "%,d", buy.getQuantity()) + " at " + Format.coins(buy.getPrice()) + " ea",
					Format.coins(buy.getCost()), Theme.GOLD, icons));
			}
			list.add(rows, BorderLayout.CENTER);
			stack.add(list);
			stack.add(copyButton(plan));
		}
		if (!unbuyable.isEmpty())
		{
			stack.add(new SectionHeader(view.ironman ? "To make or gather" : "Not on the exchange"));
			Card list = new Card(new BorderLayout());
			list.accent(view.ironman ? Theme.TEAL : Theme.EMBER);
			StackPanel rows = new StackPanel(2, 0);
			for (Plan.Line line : unbuyable)
			{
				rows.add(new BuyRow(line.getBuyItem() >= 0 ? line.getBuyItem() : line.getSupply().getIcon(), view.name(line),
					Format.amount(line.getShort()) + View.unit(line) + " more", gatherCount(line),
					view.ironman ? Theme.TEAL : Theme.EMBER, icons));
			}
			list.add(rows, BorderLayout.CENTER);
			stack.add(list);
			if (view.ironman)
			{
				stack.add(copyButton(plan));
			}
		}
		stack.add(Ui.wrapped(view.ironman
				? "Counted in the potion or food you use most, so a 4-dose potion stands for four doses."
				: "Each supply is bought as the item you already use most of, at the client's Grand Exchange prices.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH));
		replace(restock, stack);
	}

	/** How many of the usual item make up what a line is short, such as "x103", or "-" when unknown. */
	private static String gatherCount(Plan.Line line)
	{
		Integer per = line.getBuyItem() >= 0 ? line.getSupply().getUnits().get(line.getBuyItem()) : null;
		if (per == null || per <= 0)
		{
			return Format.amount(line.getShort());
		}
		return "x" + Format.amount((line.getShort() + per - 1) / per);
	}

	private JComponent copyButton(Plan plan)
	{
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		CodexButton button = new CodexButton(view.ironman ? "Copy the list" : "Copy shopping list", false, null);
		button.setAction(() ->
		{
			copy(plan);
			button.flash("Copied to clipboard");
		});
		buttons.add(button);
		return buttons;
	}

	private void copy(Plan plan)
	{
		StringBuilder text = new StringBuilder(plan.getKit().getName()).append(view.ironman ? " supplies for " : " restock for ")
			.append(plan.getTarget()).append(" trips\n");
		if (view.ironman)
		{
			for (Plan.Line line : plan.getLines())
			{
				if (line.getShort() > 0)
				{
					int item = line.getBuyItem() >= 0 ? line.getBuyItem() : line.getSupply().getIcon();
					Integer per = line.getSupply().getUnits().get(item);
					long count = per == null ? line.getShort() : (line.getShort() + per - 1) / per;
					text.append(view.name(item)).append(" x ").append(count).append('\n');
				}
			}
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.toString().trim()), null);
			return;
		}
		for (Plan.Buy buy : plan.shopping())
		{
			text.append(view.name(buy.getItemId())).append(" x ").append(buy.getQuantity()).append('\n');
		}
		text.append("Total ").append(Format.coins(plan.restockCost())).append(" gp");
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.toString()), null);
	}

	// The Kits tab.

	private void rebuildKits()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (view.active == null)
		{
			waiting(stack);
			replace(kits, stack);
			return;
		}
		Card save = new Card(new BorderLayout(0, 6));
		save.accent(Theme.TEAL);
		save.add(Ui.label("Pack it, then save it", Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		save.add(Ui.wrapped("Withdraw what you take on a trip and save it as a kit. The Quartermaster then recognises it whenever you pack it, and learns what you really use.",
			Theme.small(), Theme.TEXT_MUTED, WIDTH - 12), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		if (view.player != null)
		{
			buttons.add(new CodexButton("Save my inventory as a kit", true, actions::capture));
		}
		else
		{
			buttons.add(Ui.label("Log in to save a kit", Theme.small(), Theme.TEXT_FAINT));
		}
		save.add(buttons, BorderLayout.SOUTH);
		stack.add(save);

		boolean anySaved = false;
		for (Plan plan : view.plans)
		{
			if (!plan.getKit().isPreset())
			{
				if (!anySaved)
				{
					stack.add(new SectionHeader("Your kits"));
					anySaved = true;
				}
				String id = plan.getKit().getId();
				boolean chosen = id.equals(view.active.getKit().getId());
				stack.add(new KitCard(plan, view, chosen, () -> actions.choose(id), chosen ? () -> actions.forget(id) : null));
			}
		}
		stack.add(new SectionHeader("Presets"));
		for (Plan plan : view.plans)
		{
			if (plan.getKit().isPreset())
			{
				String id = plan.getKit().getId();
				stack.add(new KitCard(plan, view, id.equals(view.active.getKit().getId()), () -> actions.choose(id), null));
			}
		}
		replace(kits, stack);
	}

	static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label(title, Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 12), BorderLayout.CENTER);
		return card;
	}

	private static void replace(JScrollPane scroll, JComponent view)
	{
		int position = scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(view);
		scroll.revalidate();
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(Theme.INK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
