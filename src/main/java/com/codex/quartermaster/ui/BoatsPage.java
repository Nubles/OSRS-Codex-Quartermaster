package com.codex.quartermaster.ui;

import com.codex.quartermaster.core.Moorings;
import com.codex.quartermaster.core.Shipyard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * The Boats page: the boat roadmap, part by part, with the best tier the player's levels allow
 * and what the bank still needs for it, the tier after it and what it waits on, the shopping
 * list for building every part now, and where each boat is moored.
 */
final class BoatsPage
{
	private static final int WIDTH = QuartermasterPanel.WIDTH;

	private BoatsPage()
	{
	}

	static StackPanel build(View view, ItemIcons icons, Segmented picker)
	{
		StackPanel stack = new StackPanel(8, 8);
		Shipyard.Roadmap map = view.boats;
		if (map == null)
		{
			stack.add(QuartermasterPanel.note("Log in to plan your boat", "The roadmap reads your Sailing and Construction levels and counts "
				+ "the materials for each boat part against your bank."));
			return stack;
		}
		stack.add(new SectionHeader("Boat roadmap", "Sailing " + map.getSailing() + " · Con " + map.getConstruction()));
		picker.setSelected(map.getBoat().ordinal(), false);
		stack.add(picker);
		if (!view.bankKnown())
		{
			stack.add(QuartermasterPanel.note("Open your bank once", "Levels are read already; the materials are counted from your bank "
				+ "once it has been seen."));
		}

		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		int parts = map.getSteps().size();
		int ready = map.readyCount();
		boolean counted = view.bankKnown();
		tiles.add(new StatTile(counted ? ready + " of " + parts : "-", "Build now",
			!counted ? "bank not seen" : ready == 1 ? "part ready" : "parts ready", ready > 0 && counted ? Theme.GREEN : Theme.GOLD));
		int next = map.nextSailing();
		String nextPart = nextUnlock(map, next);
		tiles.add(new StatTile(next == 0 ? "-" : "Sailing " + next, "Next tier",
			next == 0 ? "on Construction now" : nextPart, next == 0 ? Theme.TEXT_MUTED : Theme.TEAL));
		stack.add(tiles);

		for (Shipyard.Step step : map.getSteps())
		{
			stack.add(partCard(step, map, view, icons));
		}

		Map<Integer, Long> shopping = map.shoppingList(view::held);
		if (counted && !shopping.isEmpty())
		{
			stack.add(new SectionHeader("To build every part", shopping.size() + (shopping.size() == 1 ? " item" : " items")));
			Card list = new Card(new BorderLayout());
			list.accent(Theme.EMBER);
			StackPanel rows = new StackPanel(2, 0);
			shopping.forEach((id, n) -> rows.add(new BuyRow(id, view.name(id), "have " + Format.amount(view.held(id)),
				"x" + Format.amount(n), Theme.EMBER, icons)));
			list.add(rows, BorderLayout.CENTER);
			stack.add(list);
			stack.add(copyButton(map, shopping, view));
		}

		addMoorings(stack, view);

		stack.add(Ui.wrapped("Levels and materials from the wiki's Hull, Keel, Helm and Mast and sails pages, for the best tier "
				+ "of each part your levels allow; whether it is already fitted is not read. Facilities are not planned. "
				+ "Short of the Construction level, a shipwright can build some parts for a fee.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH));
		return stack;
	}

	/** The part that gets a new tier at the next Sailing level, such as "Hull", or "Hull +1" for two. */
	private static String nextUnlock(Shipyard.Roadmap map, int level)
	{
		String found = null;
		int count = 0;
		for (Shipyard.Step s : map.getSteps())
		{
			if (s.getNext() != null && s.getNext().getDesign().getSailing() == level)
			{
				count++;
				if (found == null)
				{
					found = s.getPart().getLabel();
				}
			}
		}
		if (found == null)
		{
			return "";
		}
		return count > 1 ? found + " +" + (count - 1) : found;
	}

	/** A part: its best tier now and what that still needs, then the tier after it. */
	private static Card partCard(Shipyard.Step step, Shipyard.Roadmap map, View view, ItemIcons icons)
	{
		Shipyard.Check best = step.getBest();
		Card card = new Card(new BorderLayout(0, 4));
		StackPanel body = new StackPanel(2, 0);

		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		head.add(Ui.label(step.getPart().getLabel().toUpperCase(Locale.ENGLISH), Theme.small(), Theme.GOLD), BorderLayout.WEST);
		String chip;
		Color chipColor;
		boolean counted = view.bankKnown();
		if (best == null)
		{
			chip = "locked";
			chipColor = Theme.BLOCKED;
			card.accent(Theme.BLOCKED);
		}
		else if (!counted)
		{
			chip = "open to you";
			chipColor = Theme.GOLD;
			card.accent(Theme.GOLD_DIM);
		}
		else if (best.getStatus() == Shipyard.Status.READY)
		{
			chip = "ready to build";
			chipColor = Theme.GREEN;
			card.accent(Theme.GREEN);
		}
		else
		{
			int n = best.getMissing().size();
			chip = n + (n == 1 ? " item short" : " items short");
			chipColor = Theme.EMBER;
			card.accent(Theme.EMBER);
		}
		head.add(Ui.label(chip, Theme.small(), chipColor), BorderLayout.EAST);
		body.add(head);

		if (best != null)
		{
			body.add(Ui.label(best.getDesign().getName(), Theme.body(), Theme.TEXT));
			body.add(Ui.label(levels(best.getDesign()), Theme.small(), Theme.TEXT_FAINT));
			if (counted && best.getStatus() == Shipyard.Status.MATERIALS)
			{
				addMissing(body, best, view, icons, Theme.EMBER);
			}
		}

		Shipyard.Check next = step.getNext();
		if (next != null)
		{
			JPanel rule = new JPanel(new BorderLayout(0, 2));
			rule.setOpaque(false);
			rule.setBorder(best == null ? null : new CompoundRule());
			// Wrapped: "Ironwood mast and cotton sails" does not fit on one line after "Next:".
			rule.add(Ui.wrapped("Next: " + next.getDesign().getName(), Theme.body(), best == null ? Theme.TEXT : Theme.TEXT_MUTED, WIDTH - 24),
				BorderLayout.NORTH);
			rule.add(Ui.label(waitsOn(next, map), Theme.small(), next.getStatus() == Shipyard.Status.LOCKED ? Theme.TEAL : Theme.BLOCKED),
				BorderLayout.CENTER);
			body.add(rule);
			if (!counted)
			{
				// Nothing to count against until the bank is seen.
			}
			else if (!next.getMissing().isEmpty())
			{
				addMissing(body, next, view, icons, Theme.GOLD_DIM);
			}
			else
			{
				body.add(Ui.label("Its materials are in your bank", Theme.small(), Theme.GREEN));
			}
		}
		else if (best != null)
		{
			body.add(Ui.label("The last tier", Theme.small(), Theme.TEXT_FAINT));
		}
		card.add(body, BorderLayout.CENTER);
		return card;
	}

	/** "Sailing 31 · Construction 23" */
	static String levels(Shipyard.Design d)
	{
		return "Sailing " + d.getSailing() + " · Construction " + d.getConstruction();
	}

	/** What the next tier waits on, with the levels still to gain: "Sailing 48 (+14) · Con 41 (+1)". */
	static String waitsOn(Shipyard.Check next, Shipyard.Roadmap map)
	{
		Shipyard.Design d = next.getDesign();
		StringBuilder sb = new StringBuilder();
		int s = d.getSailing() - map.getSailing();
		int c = d.getConstruction() - map.getConstruction();
		sb.append("Sailing ").append(d.getSailing());
		if (s > 0)
		{
			sb.append(" (+").append(s).append(')');
		}
		sb.append(" · Con ").append(d.getConstruction());
		if (c > 0)
		{
			sb.append(" (+").append(c).append(')');
		}
		return sb.toString();
	}

	private static void addMissing(StackPanel body, Shipyard.Check check, View view, ItemIcons icons, Color color)
	{
		for (Shipyard.Material m : check.getDesign().getMaterials())
		{
			Long short_ = check.getMissing().get(m.getItemId());
			if (short_ == null)
			{
				continue;
			}
			body.add(new BuyRow(m.getItemId(), view.name(m.getItemId()),
				"have " + Format.amount(check.getHeld().get(m.getItemId())) + " of " + Format.amount(m.getQuantity()),
				"x" + Format.amount(short_), color, icons));
		}
	}

	private static JComponent copyButton(Shipyard.Roadmap map, Map<Integer, Long> shopping, View view)
	{
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		CodexButton button = new CodexButton("Copy shopping list", false, null);
		button.setAction(() ->
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(shoppingText(map, shopping, view)), null);
			button.flash("Copied to clipboard");
		});
		buttons.add(button);
		return buttons;
	}

	static String shoppingText(Shipyard.Roadmap map, Map<Integer, Long> shopping, View view)
	{
		StringBuilder text = new StringBuilder(map.getBoat().getLabel()).append(" parts at Sailing ").append(map.getSailing())
			.append(", Construction ").append(map.getConstruction()).append('\n');
		shopping.forEach((id, n) -> text.append(view.name(id)).append(" x ").append(n).append('\n'));
		return text.toString().trim();
	}

	/** Each boat the player owns and the port it was left at. */
	private static void addMoorings(StackPanel stack, View view)
	{
		Moorings moorings = view.moorings;
		if (moorings.isEmpty())
		{
			return;
		}
		int n = moorings.getBerths().size();
		stack.add(new SectionHeader("Moorings", n + (n == 1 ? " boat" : " boats")));
		Card card = new Card(new BorderLayout());
		card.accent(Theme.TEAL);
		StackPanel rows = new StackPanel(6, 0);
		for (Moorings.Berth b : moorings.getBerths())
		{
			JPanel row = new JPanel(new BorderLayout(0, 0));
			row.setOpaque(false);
			JPanel top = new JPanel(new BorderLayout(6, 0));
			top.setOpaque(false);
			top.add(Ui.label(b.getName(), Theme.body(), Theme.TEXT), BorderLayout.CENTER);
			if (b.getType() != null)
			{
				top.add(Ui.label(b.getType(), Theme.small(), Theme.TEXT_FAINT), BorderLayout.EAST);
			}
			row.add(top, BorderLayout.NORTH);
			row.add(Ui.label(b.getPort() == null ? "Port not known" : "at " + b.getPort(), Theme.small(),
				b.getPort() == null ? Theme.TEXT_FAINT : Theme.TEAL), BorderLayout.CENTER);
			rows.add(row);
		}
		card.add(rows, BorderLayout.CENTER);
		stack.add(card);
		stack.add(Ui.wrapped("Ports and boat types are named from the game's own tables. Reading each boat's port this way follows the "
			+ "Dude, Where's My Boat? plugin and is unconfirmed on the wiki.", Theme.small(), Theme.TEXT_FAINT, WIDTH));
	}

	/** A faint line above the next tier, so it reads apart from the tier built now. */
	private static final class CompoundRule extends javax.swing.border.CompoundBorder
	{
		CompoundRule()
		{
			super(new MatteBorder(1, 0, 0, 0, Theme.STROKE_SOFT), new EmptyBorder(5, 0, 0, 0));
		}
	}
}
