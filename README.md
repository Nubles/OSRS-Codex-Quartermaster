# Codex Quartermaster

**How many trips your bank supports, what runs out first, and what restocking costs.** Pick the content you are doing, or save your own inventory as a kit, and Codex Quartermaster counts your bank against it supply by supply: food in hitpoints healed, potions in doses, ammo and runes one by one. After a couple of trips it stops guessing and plans from what you actually used.

It is the seventh plugin in the **Codex** suite, after [Codex Navigator](https://github.com/Nubles/OSRS-Codex-Navigator), [Codex Guardian](https://github.com/Nubles/OSRS-Codex-Guardian), [Codex Log Insight](https://github.com/Nubles/OSRS-Codex-Log-Insight), [Codex Gear Roadmap](https://github.com/Nubles/OSRS-Codex-Gear-Roadmap), [Codex Session Chronicle](https://github.com/Nubles/OSRS-Codex-Session-Chronicle) and [Codex Ledger](https://github.com/Nubles/OSRS-Codex-Ledger), and shares their look: warm ink, aged gold and the game's own fonts.

<p align="center">
  <img src="docs/screenshots/supplies.png" width="225" alt="Supplies">
  <img src="docs/screenshots/restock.png" width="225" alt="Restock">
  <img src="docs/screenshots/kits.png" width="225" alt="Kits">
</p>

## What it does

- **Supplies.** The trips your bank supports for the chosen kit in large gilt figures, and what runs out first. Every supply gets a row with what you hold, what a trip uses and how many trips that lasts, with a row of ration pips up to your restock target, so the short one stands out. Items that come back every trip (a spade, a rune pouch) are marked as kept and never count against you. Below that, what a trip costs at today's prices and your recent trips with how long they ran, what they used and what that cost.
- **Restock.** Pick a target of 5, 10, 25, 50 or 100 trips and get the shopping list: each supply bought as whichever item is cheapest per dose or hitpoint (a plain ranging potion over a divine one, sharks over mantas when that is cheaper per heal), with quantities and the total bill. Copy the list to the clipboard for the Grand Exchange. Supplies the exchange does not sell are listed separately.
- **Kits.** Every kit at a glance with its trips and what runs out first. Nine presets ship with the plugin (Vorkath, Zulrah, Barrows, General Graardor, Vardorvis, Nex, Theatre of Blood, a slayer task and Wintertodt). Withdraw what you take on a trip and press **Save my inventory as a kit** to add your own; when you pack it again, the Quartermaster recognises it and switches to it.
- **Learning from your trips.** A trip runs from closing the bank to opening it again. What you set out with and what comes back are compared, supply by supply, and after two trips the plan uses your real averages instead of the kit's amounts. Deaths and quick bank shuffles are not counted.
- **Low supply warning.** When you set out with only a few trips left (three by default), a line in the chat box says which supply runs out and what restocking costs.
- **Plaque.** A slim plaque in the game view with the trips left and what runs out first, shown at the bank, always, or never.

<p align="center">
  <img src="docs/screenshots/supplies-preset.png" width="225" alt="A preset">
  <img src="docs/screenshots/restock-preset.png" width="225" alt="Restocking a preset">
  <img src="docs/screenshots/plaque.png" alt="Plaque">
</p>

## How it counts

Supplies that stand in for one another are pooled. Food is counted in hitpoints healed, so a shark is 20 and a manta ray 22. Prayer potions, super restores and sanfew serums count as prayer doses; ranging, divine ranging, bastion and divine bastion potions as ranging doses; and so on for magic, super combat, antifire, anti-venom, stamina and Saradomin brews. Enchanted ruby and diamond dragon bolts count together. Anything else a saved kit carries is counted as its own item, including the runes in a rune pouch.

Your bank is read whenever it is open and remembered as last seen, so the panel works anywhere. What you carry (inventory, worn gear and rune pouch) is added to it. Prices are RuneLite's Grand Exchange prices. Everything is saved per account in your RuneLite profile.

Preset amounts are a typical trip of several kills, not a rule. They are only the starting point until your own trips are measured.

## Rules posture

Codex Quartermaster only reads what the game already shows you: your bank, inventory, worn gear and rune pouch, and RuneLite's own item prices. It does not click, change menus, or send anything over the network.

## Development

```bash
./gradlew build   # compile and run tests
./gradlew run     # launch RuneLite with the plugin loaded
```

The planner and trip measuring (`core/`) are plain Java with unit tests; the panel and overlay are in `ui/` and `overlay/`.

## Licence

BSD 2-Clause, see [LICENSE](LICENSE).
