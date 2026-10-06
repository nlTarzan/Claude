# Step 7: the Elder Guardian reward and the Monument Hoe

These three files are **listeners**: classes whose methods Paper calls when something happens in the game. A few new ideas first:

| What you see | Meaning |
|---|---|
| `implements Listener` | "This class contains event methods." Registered in `Monuments.enable()`. |
| `@EventHandler` | Marks a method Paper should call. Its parameter type decides *which* event, e.g. `BlockBreakEvent` means "a player broke a block". |
| `event.setCancelled(true)` | Stops the action: the block isn't broken, the hit doesn't land, and so on. |
| `priority = …` | When this method runs compared to other plugins: `LOWEST` → `LOW` → `NORMAL` → `HIGH` → `HIGHEST` → `MONITOR`. Later ones see what earlier ones decided and get the final say. `MONITOR` is only for watching the outcome, never for changing it. |
| `ignoreCancelled = true` | Skip this method if an earlier plugin (like GriefDefender) already cancelled the event. |
| `UUID` | A player's permanent ID. Names can change; UUIDs never do. |

## ElderGuardianListener.java: handing out the hoe

This replaces your datapack's `monument_elder_guardian_killed` advancement and `monument_elder_kill.mcfunction`.

@code src/main/java/com/nltarzan/structures/monument/listener/ElderGuardianListener.java 16-49

- **Lines 16–24:** a listener that knows the monument and the hoe helper. The same `hoe` object is shared with the other listener, so both recognize hoes the same way.
- **Line 27:** called whenever **any** mob dies anywhere, so the first lines filter hard.
- **Line 28:** stop unless it's an Elder Guardian and `reward.enabled` is on. `instanceof ElderGuardian elder` checks the type and names it `elder` in one go.
- **Line 31:** the player who killed it. This also works with arrows and tridents; it's `null` if no player was responsible.
- **Lines 32–35:** stop if no player killed it, the monument isn't available (world not loaded, paused, or switched off), or the Elder Guardian wasn't at the monument (the 40-block sphere or the monument box).
- **Lines 36–39:** are any **other** Elder Guardians still alive at the monument? Then this wasn't the last one. `anyMatch` is true if at least one entry passes the test; `!other.equals(elder)` skips the one that's dying right now.
- **Line 43:** the sponge count saved when the monument was placed, or, if unknown, counted right now. `orElseGet(arena::countSponges)` means "use the saved number, or else run `countSponges`".
- **Lines 44–46:** no sponges, no hoe.
- **Lines 47–48:** create the hoe with that durability and the monument's ID, and put it in the killer's inventory. `addItem` returns whatever didn't fit, so with a full inventory the hoe is dropped at the player's feet instead of being lost. No chat message is sent; the hoe just appears.

## MonumentHoe.java: creating and recognizing the hoe

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoe.java 28-59

- **Lines 30–36:** two **keys** for data stored on the item. A `NamespacedKey` is a name with the plugin's name in front, so it can't clash with other plugins: `resettingstructures:structures` (the "this is a Monument Hoe" tag) and `resettingstructures:monument` (the ID of the monument the hoe came from).
- **Line 43:** a plain iron hoe.
- **Line 44:** `editMeta` opens the item's **meta**: its name, lore, durability and stored data. `Damageable.class` asks for the meta type that has durability.
- **Line 45:** sets the hoe's **maximum durability** to the number of sponges, replacing iron's normal 250. Each use adds 1 damage, and at max damage it breaks, so it breaks exactly on the last sponge.
- **Lines 46–48:** the name "Monument Hoe" in dark aqua (custom names are italic by default; `ITALIC, false` turns that off) and the gray lore line.
- **Lines 49–50:** stores the tag and the monument ID in the item's **persistent data container**, which travels with the item through chests, Ender Chests, bundles and restarts.
- **Lines 54–57:** the vanilla `can_break` component, which makes the tooltip read **"Can break: Sponge, Wet Sponge"**. Read it from the inside out: a set of two block types, a rule "one of these blocks", the list of rules, and put it on the item. Vanilla only *enforces* this in Adventure mode; in Survival it's just the tooltip, and the listener below does the real enforcing.

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoe.java 61-76

- **Lines 62–64, "is this a Monument Hoe?":** yes when the item exists, isn't empty, and has the `structures` tag. Recognizing it by the tag, not the name, means renaming can't fool it.
- **Lines 67–71:** the monument ID stored on a hoe; empty for hoes made before IDs existed.
- **Lines 73–76:** whether a block is a sponge, wet or dry.

## MonumentHoeListener.java: the hoe rules

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoeListener.java 45-78

- **Line 50:** a temporary list of players who **just** mined a sponge with the hoe, used to tell "durability loss from a sponge" (allowed) apart from every other durability loss (blocked).
- **Line 58:** `PlayerInteractEvent` fires on every left or right click, at a block or into the air, once per hand. `LOWEST` priority runs it before anything else, so the smuggling check happens first. There's no `ignoreCancelled`, because clicking into the air arrives already marked "cancelled" by Bukkit and must still count as using the hoe.
- **Lines 60–62:** no hoe rules while `monuments.yml` is broken or the monument is switched off, so nobody loses a hoe over a server mistake.
- **Lines 63–66:** only care about the Monument Hoe, in whichever hand was used.
- **Line 68:** *where* it was used: the clicked block, or the player's position when clicking into the air.
- **Lines 69–73, the smuggling rule:** if it's not usable there (outside the monument, or a hoe from an earlier monument), cancel the click and delete every Monument Hoe the player carries. A hoe brought out in an Ender Chest or bundle is deleted the first time it's clicked with.
- **Lines 74–77:** inside the monument, a right-click doesn't till dirt or do other hoe actions. `setUseItemInHand(DENY)` blocks only the *item's* action, so doors and buttons still work.

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoeListener.java 80-103

- **Line 80:** `HIGH` priority runs after GriefDefender and most protection plugins; `ignoreCancelled = true` does nothing if GriefDefender already said no. Together these keep the rule: the plugin only ever *adds* cancellations, it never allows something GriefDefender blocked.
- **Line 87:** blocks are broken with the main hand.
- **Lines 88–97, breaking with the hoe:** not usable here → cancel and delete all hoes (a backup; the click check normally catches it first). Inside, but not a sponge → cancel, with "The Monument Hoe only mines sponges." on the **action bar** (the line above the hotbar). Inside and a sponge → the break goes ahead.
- **Lines 98–102, breaking without the hoe:** a sponge inside the monument is cancelled, with "Sponges can only be mined with the Monument Hoe."

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoeListener.java 105-127

Minecraft first fires "block broken", then right after it "item took damage". The second event doesn't say *why*, so the first method leaves a note and the second reads it:

- **Lines 106–114, `onSpongeMined`:** `MONITOR` with `ignoreCancelled` only runs if the sponge break *actually happened*. It puts the player on the list (line 111) and schedules removing them on the next tick (line 112), so the note can never be left lying around.
- **Lines 117–127, `onDurability`:** fires whenever the hoe would lose durability. `minedSponge.remove(...)` removes the player from the list *and* says whether they were on it. On the list: allow exactly 1 damage. Not on the list (hitting a mob, anything else): cancel, and the hoe loses nothing.

The hoe therefore loses durability for exactly one thing, mining a sponge, one use each, so it lasts exactly as many sponges as the monument has.

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoeListener.java 129-173

- **Lines 130–143:** hitting something with the hoe outside the monument also counts as using it, so the hoes are deleted. Inside, hitting is allowed and costs no durability.
- **Lines 147–173:** the "Prepare" events fire when a result would appear in an output slot. Setting it to `null` leaves the slot empty. That blocks every way to repair the hoe (two hoes in an anvil, grindstone or crafting grid), to enchant it (Mending or Unbreaking would break the one-use-per-sponge rule), or to rename it.

@code src/main/java/com/nltarzan/structures/monument/listener/MonumentHoeListener.java 175-211

- **Lines 176–180, usable** means three things: the monument exists, the spot is **inside the monument box** (the exact 58×22×58 blocks), and the hoe belongs to **this** monument. A hoe from an earlier monument has a different ID (hoes without an ID count as 0), so it isn't usable, even inside.
- **Lines 183–202, deleting every Monument Hoe the player has on them:** every inventory slot (hotbar, main inventory, armor, off-hand) and an item held on the mouse cursor. If anything was removed, play the item-break sound and send the red message. Hoes still in an Ender Chest or bundle are caught as soon as they're taken out and used.
- **Lines 204–211:** used by the anvil, grindstone and crafting checks.

## Known, accepted limitations

- **While the monument is paused** (a broken `monuments.yml` at server start) **or switched off** (`active off`, and on a new server), the hoe rules are off, so only GriefDefender's flag applies and anyone could mine the sponges by hand. Both states are clearly shown in the console, `status.txt` and the status command. Switched off, this is deliberate: the location may not have been checked yet, and it could be someone's build.
- **Two hoes are possible** if two Elder Guardians die in the very same tick, because each sees the other as already dead. With one in the penthouse and one in each wing this is very unlikely.

Both were reviewed and deliberately left in.
