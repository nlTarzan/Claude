
# 1. Installing and updating

**Requirements**

- Paper 26.3 or newer (or a Paper fork such as Purpur), running on Java 25. It doesn't run on plain Spigot.
- A permissions plugin such as **LuckPerms**, to give admins the plugin's permission. Nobody has it by default, not even operators.
- Optional: a protection plugin such as **GriefDefender**. The plugin works alongside it; see section 9 (The Monument Hoe).

**Installing**

1. Put `nlT-Structures-<version>.jar` in the server's `plugins` folder.
2. Start the server.
3. The plugin creates `plugins/nlT-Structures/` with `messages.yml`, `status.txt`, `commands.txt` and a `monuments` folder.

**Nothing is built yet.** On a new server the monument is switched off, so the plugin never places a monument at a location nobody checked. Section 4 (Tutorial: set up the monument) shows how to set the location and switch it on.

**Updating**

1. Stop the server.
2. Delete the old nlT-Structures jar from `plugins` and put the new one there.
3. Start the server.

`monuments.yml` and `messages.yml` update themselves, keeping your settings, and the monument's saved state and the countdown to the next check are kept; see section 10 (Files). Always stop and start the server; don't use `/reload` or a plugin manager to swap the jar.


# 2. Permissions

| Permission | Who it's for | What it allows |
|---|---|---|
| `nltstructures.admin` | Admins | Every `/nltstructures` command, and receiving the monument's broadcast messages |

Nobody has this permission by default, **not even operators**. Give it with a permissions plugin. With LuckPerms:

```
/lp user <name> permission set nltstructures.admin true
```

The server console can always use the commands, so you can run the line above from the console without the `/`.

Players need no permission. Fighting the Elder Guardians, receiving the Monument Hoe and mining sponges work for everyone.


# 3. How it works

The plugin manages one Ocean Monument. Once players have killed all its Elder Guardians and left, it replaces the monument with a fresh one, so it can be raided again.

## The reset cycle

1. The plugin waits a random time (by default 1, 2 or 3 Minecraft days, sometimes much longer).
2. It then **checks** the monument: it loads the area and counts the Elder Guardians.
3. What happens next depends on what it finds:

| What it finds | What happens |
|---|---|
| At least one Elder Guardian is alive | Nothing. The monument isn't cleared yet |
| No Elder Guardians, but a player is at the monument | The reset is postponed. A new check follows after a shorter wait (5 to 15 minutes by default) |
| No Elder Guardians and nobody there | **The monument is reset**: a new one is placed, with new Elder Guardians and sponges |

4. A new random wait starts, and the cycle repeats.

"At the monument" means within the check radius of the centre (40 blocks by default), or anywhere inside the monument itself.

The countdown to the next check is saved. A server restart doesn't start the wait from zero, and a check that came due while the server was off runs 10 seconds after it starts. The countdown follows the world's game time, so it only runs while the server is running.

## What a reset places

There are two reset modes. You choose one in `monuments.yml` (`reset.mode`).

| Mode | What it places |
|---|---|
| `pre-built` (default) | One of 10 ready-made monuments, picked at random. Each can be given its own chance |
| `generated` | A new random maze of rooms, different every time. Every generated monument has a **seed**: a number that gives exactly that layout again |

Both replace the whole monument: a box of 58 × 22 × 58 blocks around the centre. **Anything else built inside that box is overwritten by a reset.**

Every new monument has three Elder Guardians: one in the top room and one in each wing. Their health and attack damage are set in `monuments.yml`. Before a new monument is placed, the Guardians left in the area are removed, so they don't pile up over many resets.

## The Monument Hoe

The player who kills the **last** Elder Guardian gets a Monument Hoe: an iron hoe that mines the monument's sponges. It has exactly one use per sponge in that monument. Sponges in the monument can only be mined with it, and it stops working once the monument has been reset. All its rules are in section 9 (The Monument Hoe).

## Active, inactive and paused

| State | Meaning | How to change it |
|---|---|---|
| **Active** | Checks, resets and the hoe rules all work | `/nltstructures monument active off` |
| **Inactive** | Nothing is checked or built, and the hoe rules are off. A new server starts like this | `/nltstructures monument active on` |
| **Paused** | `monuments.yml` had a mistake when the server started, so the plugin doesn't know the location. Nothing is checked or built until it's fixed | Fix the file, then `/nltstructures monument reload` |

`/nltstructures monument status` shows which state the monument is in.

## What players do

1. Go to the monument and kill the three Elder Guardians.
2. Whoever kills the last one gets the **Monument Hoe**. If their inventory is full, it drops at their feet.
3. Mine the sponges with the hoe. It lasts for exactly the number of sponges in the monument.
4. Leave. Once everyone has left and the next check has run, the monument resets for the next group.

Good to know:

- The hoe only works inside the monument it came from. Using it outside, or after a reset, makes it crumble away.
- The hoe can't be repaired, enchanted or renamed.
- The monument doesn't reset while someone is still at it.


# 4. Tutorial: set up the monument

This sets the monument's location and switches it on. You need `nltstructures.admin` (see section 2, Permissions).

All commands start with `/nltstructures`. The shorter `/structures` and `/nlts` do the same; this manual uses `/nlts`.

## Step 1: choose the monument

Pick the Ocean Monument the plugin should manage: an existing one, or a spot in the ocean where you want one. Remember that the first reset replaces everything inside the 58 × 22 × 58 box around its centre.

## Step 2: set the centre

Stand in the middle of the monument, on the corner where the 4 centre blocks meet, at the level the rooms start. For a monument at the normal height that's y=40. Then run:

```
/nlts monument setcenter
```

Your position is rounded to the nearest block corner and saved, together with the world you're in.

- If you know the coordinates, you can give them instead: `/nlts monument setcenter 224 40 -320`. From the console this is the only way, and it uses the world in `monuments.yml`.
- If the y-level isn't 40, the plugin reminds you to double check it. Another level is allowed.

## Step 3: check the location

```
/nlts monument status
```

Look at the **Location** line. It should show the world and coordinates you just set.

## Step 4: switch it on

```
/nlts monument active on
```

The reply names the location the plugin will build at. From now on the monument is checked on its timer. This is saved, so it stays on after a restart.

## Step 5: place the first monument

Switching on doesn't build anything yet; the first check comes after a random wait. To place a monument right away:

```
/nlts monument reset
```

This places a new monument even if Elder Guardians are alive or players are inside. Fly around it to check that it sits where you expect. If it's off by a block, move the centre with `setcenter` and run `reset` again.

## Step 6: test the cycle

1. Kill the three Elder Guardians. The player who kills the last one gets the Monument Hoe.
2. Mine a sponge with the hoe.
3. Leave the monument and run `/nlts monument check`. With nobody there, it reports that a new monument was placed.

## Optional extras

- **Generated monuments:** try one with `/nlts monument reset generated`. To use them for every reset, see section 6 (Tutorial: change the settings).
- **Fewer messages:** `/nlts monument broadcast off` turns off the messages admins get on every check.


# 5. Tutorial: manage the monument

## See what's going on

```
/nlts monument status
```

| Line | What it shows |
|---|---|
| Problem | Only when `monuments.yml` has a mistake: what's wrong |
| Location | The world and coordinates of the centre |
| Monument | What's built now: a pre-built variant, or a generated one with its seed |
| Sponges | How many sponges the monument had when it was placed |
| Elder Guardians | How many are alive. Only shown while the monument's area is loaded |
| Last check | When the last check ran and how it ended |
| Reset mode | `Pre-built` or `Generated` |
| Active | `On`, or `Off` with what to do |
| Next check | How long until the next check, and around what time |
| Broadcasts | Whether admins get the check messages |

The same information is always in `plugins/nlT-Structures/status.txt`, so you can read it without joining the server.

## Check or reset right away

| To do this | Command |
|---|---|
| Run a normal check now. It only resets when the monument is cleared and empty | `/nlts monument check` |
| Place a new monument now, whatever the state | `/nlts monument reset` |
| Place one specific pre-built monument | `/nlts monument reset monument_03` |
| Build a generated monument | `/nlts monument reset generated` |
| Build a generated monument you've seen before | `/nlts monument reset generated <seed>` |

After a check or a reset, a new random wait starts.

A forced `reset` doesn't wait for players to leave. Anyone inside is in the middle of it when the blocks change, so warn them first.

## Preview generated layouts

These print a map of a generated layout in the **server console**, without building anything.

| To see | Command |
|---|---|
| A random layout | `/nlts monument layout` |
| The layout of a seed | `/nlts monument layout <seed>` |
| A random layout with long corridors | `/nlts monument layout random 1` |
| A random layout with many short branches | `/nlts monument layout random 0` |
| The generated monument that's built now | `/nlts monument layout current` |

The last number is the "newest chance", from 0 to 1; see section 8 (All settings). When you find a layout you like, build it with `/nlts monument reset generated <seed>`.

## Switch the monument off for a while

```
/nlts monument active off
```

Nothing is checked or built until you run `/nlts monument active on` again. While it's off the hoe rules are off too. Switching it back on starts a fresh random wait.

## Turn the broadcast messages on or off

```
/nlts monument broadcast off
```

Without `on` or `off` it switches. The messages are listed in section 7 (All commands).

## Move the monument

1. Run `/nlts monument active off`.
2. Stand on the new centre and run `/nlts monument setcenter`.
3. Check it with `/nlts monument status`, then run `/nlts monument active on` and `/nlts monument reset`.

The old monument stays where it is; the plugin just stops managing it.


# 6. Tutorial: change the settings

The settings are in `plugins/nlT-Structures/monuments/monuments.yml`. Every setting is explained in the file and in section 8 (All settings).

1. Open `monuments.yml` and change what you want.
2. Save it and run `/nlts monument reload`.

If the file has a mistake, the reload says so, the details are in the console, and the plugin keeps using the previous settings. Fix the file and reload again.

## Change how often it's checked

```
reset:
  intervals:
    - { delay: 1d, weight: 30 }
    - { delay: 2d, weight: 30 }
    - { delay: 3d, weight: 30 }
    - { delay: 72d, weight: 10 }
```

After every check one line is picked at random. A line with a higher `weight` is picked more often: here each of the first three has a 30 in 100 chance and the last a 10 in 100 chance.

| Unit | Meaning | Real time |
|---|---|---|
| `t` | Game ticks | 20 ticks = 1 second |
| `s` | Seconds | |
| `d` | Minecraft days | 1d = 20 minutes, 72d = 1 real day |

For testing, use short times such as `10s`, or just run `/nlts monument check`.

`occupied-intervals` works the same way. It's used when the monument is cleared but a player is still there, and should be shorter than `intervals`.

## Use generated monuments

Set `reset.mode` to `generated` and reload. Every reset then builds a new random maze. `/nlts monument reset monument_03` still places a pre-built one when you ask for it.

## Change which pre-built monuments appear

```
variants:
  monument_00: 0
  monument_01: 10
  monument_02: 10
```

The number is the chance weight. Give a monument a higher number to see it more often, or `0` to never pick it at random. A monument with weight 0 can still be placed by name with `/nlts monument reset <variant>`. `monument_00` is the empty monument, which is why its weight is 0.

## Make the Elder Guardians stronger or weaker

```
elder-guardian:
  health: 100
  attack-damage: 10
```

The new values apply to Elder Guardians spawned by the next reset. A vanilla Elder Guardian has 80 health and 8 attack damage.

## Turn off the Monument Hoe reward

Set `reward.enabled` to `false`. Nobody gets a hoe any more. The sponges in the monument still can't be mined by hand while the monument is active.


# 7. All commands

`/nltstructures`, `/structures` and `/nlts` are the same command. If another plugin also has a `/structures` command, use `/nltstructures` or `/nlts`. Every command needs `nltstructures.admin`. Tab completion suggests the options everywhere.

The full explanation of every command is also in `plugins/nlT-Structures/commands.txt`, which the plugin rewrites on every start.

## General

| Command | What it does |
|---|---|
| `/nlts` or `/nlts help` | Lists every command, with a short explanation |
| `/nlts reload` | Re-reads `messages.yml`. The monument's settings have their own reload, below |

## The monument

| Command | What it does |
|---|---|
| `/nlts monument` or `/nlts monument status` | Shows the monument's status; see section 5 (Tutorial: manage the monument) |
| `/nlts monument active [on\|off]` | Switches the checks, resets and hoe rules on or off. Without `on` or `off` it switches. Saved through restarts |
| `/nlts monument check` | Checks the monument right away, like the timer does. It only resets when every Elder Guardian is dead and nobody is there |
| `/nlts monument reset` | Places a new monument right away, even when Elder Guardians are alive or players are inside. What it places follows `reset.mode` |
| `/nlts monument reset <variant>` | Places one specific pre-built monument, for example `monument_03` |
| `/nlts monument reset generated [seed]` | Builds a generated monument, whatever `reset.mode` says. With a seed you get exactly that layout |
| `/nlts monument layout [seed\|random] [newest-chance]` | Prints a generated layout in the server console, without building anything |
| `/nlts monument layout current` | Prints the layout of the generated monument that's built now |
| `/nlts monument setcenter` | In-game only: moves the centre to the block corner you're standing on |
| `/nlts monument setcenter <x> <y> <z>` | Moves the centre to these coordinates. In-game it uses your world; from the console, the world in `monuments.yml` |
| `/nlts monument broadcast [on\|off]` | Turns the broadcast messages on or off. Without `on` or `off` it switches |
| `/nlts monument reload` | Re-reads `monuments.yml` and the structure files |

## Broadcast messages

While broadcasts are on, everyone with `nltstructures.admin` and the console get a message after each check:

| Message | When |
|---|---|
| The Monument has not been cleared. | A check found Elder Guardians alive |
| The Monument has been cleared, it will reset once everyone has left. | Cleared, but a player is still there |
| The Monument has been cleared. | Cleared and empty; the reset follows |
| The Ocean Monument has been reset. | A new monument was placed |

Players without the permission never see these.


# 8. All settings

These are in `plugins/nlT-Structures/monuments/monuments.yml`. After changing them, run `/nlts monument reload`.

## Location

| Setting | Default | Meaning |
|---|---|---|
| `location.world` | `world` | The world the monument is in |
| `location.x`, `y`, `z` | 224, 40, -320 | The centre: the corner where the 4 centre blocks meet, at the level the rooms start. Whole numbers. Easiest to set with `/nlts monument setcenter` |
| `location.check-radius` | 40 | Elder Guardians and players within this many blocks of the centre count as "at the monument". Anything inside the monument itself always counts |

The location must be complete and valid. If it isn't, nothing is built until it's fixed; the plugin never falls back to a default location.

## Reset

| Setting | Default | Meaning |
|---|---|---|
| `reset.mode` | `pre-built` | `pre-built` or `generated`; see section 3 (How it works) |
| `reset.intervals` | 1d, 2d, 3d (weight 30 each), 72d (weight 10) | The possible waits between checks, each with a chance weight |
| `reset.occupied-intervals` | 300s, 600s, 900s | The possible waits when the monument is cleared but a player is still there |
| `reset.clear-guardians` | `true` | Remove the Guardians left in the area before a new monument is placed |
| `reset.announce` | `true` | Send the broadcast messages. Also changed by `/nlts monument broadcast` |

Times are written as a number and a unit: `t` (ticks), `s` (seconds) or `d` (Minecraft days of 20 minutes). A number without a unit is ticks.

## Pre-built monuments

| Setting | Default | Meaning |
|---|---|---|
| `variants.<name>` | 10 for `monument_01` to `monument_10`, 0 for `monument_00` | The chance weight of each pre-built monument. 0 means it's never picked at random |

The pre-built monuments are part of the plugin's jar. You can change their weights, but new ones can't be added through this file.

## Elder Guardians and the reward

| Setting | Default | Meaning |
|---|---|---|
| `elder-guardian.health` | 100 | Health of every Elder Guardian the plugin spawns (vanilla: 80) |
| `elder-guardian.attack-damage` | 10 | Their attack damage (vanilla: 8) |
| `elder-guardian.y-offset` | 11.5 | Pre-built monuments only: how high above the centre the top room's Elder Guardian is spawned. Normally left alone |
| `reward.enabled` | `true` | Give the player who kills the last Elder Guardian a Monument Hoe |

## Generated monuments

These only matter for generated monuments.

| Setting | Default | Meaning |
|---|---|---|
| `maze.newest-chance.peak` | 0.65 | The most common "newest chance" |
| `maze.newest-chance.spread` | 0.15 | How far most monuments are from the peak: about 2 in 3 are within peak ± spread |
| `maze.newest-chance.min`, `max` | 0.0, 1.0 | The lowest and highest value a monument can get |
| `maze.min-sponge-rooms` | 1 | Every monument gets at least this many sponge rooms |

The **newest chance** decides the shape of the maze, and every generated monument draws its own:

- Close to **1**: long, winding corridors with few dead ends.
- Close to **0**: lots of short branches and dead ends.

To get the same kind of maze every time, set `min` and `max` to the same number. Preview what a value does with `/nlts monument layout random <newest-chance>`.


# 9. The Monument Hoe

The Monument Hoe is an iron hoe named "Monument Hoe". Its tooltip says it can break Sponge and Wet Sponge.

## Getting it

- The player who kills the **last** Elder Guardian of the monument gets one. Kills of the first two give nothing.
- Its durability is the number of sponges the monument had when it was placed. Each sponge mined costs one use, so one hoe is enough for the whole monument.
- If the player's inventory is full, the hoe drops at their feet.
- No hoe is given when the monument has no sponges, or when `reward.enabled` is `false`.

## The rules

| What the player does | Result |
|---|---|
| Mines a sponge in the monument with the hoe | Allowed. The hoe loses one use |
| Mines a sponge in the monument without the hoe | Blocked: "Sponges can only be mined with the Monument Hoe." |
| Mines another block with the hoe, inside the monument | Blocked: "The Monument Hoe only mines sponges." |
| Hits a mob with the hoe, inside the monument | Allowed. The hoe loses no durability |
| Right-clicks with the hoe, inside the monument | The hoe does nothing (no tilling). Doors and buttons still work |
| Uses or hits with the hoe outside the monument | Every Monument Hoe the player carries crumbles away |
| Uses a hoe from an earlier monument, anywhere | Every Monument Hoe the player carries crumbles away |
| Puts the hoe in an anvil, grindstone, enchanting table or crafting grid | No result appears: it can't be repaired, enchanted or renamed |

"Inside the monument" means inside the 58 × 22 × 58 box of the monument itself, not the wider check radius.

A hoe taken out of the monument in an Ender Chest or a bundle isn't removed straight away. It crumbles the first time someone uses it.

## Protection plugins

The plugin only ever blocks actions; it never allows something a protection plugin such as GriefDefender has blocked. If your protection plugin stops players from breaking blocks at the monument, they can't mine the sponges with the hoe either. Allow breaking sponges there, and let nlT-Structures make sure it only happens with the hoe.

## When the rules are off

While the monument is **inactive** or **paused**, the mining rules don't apply: sponges can be mined by hand and hoes aren't removed. Only your protection plugin protects the monument then. A Monument Hoe still can't be repaired, enchanted or renamed.


# 10. Files

Everything is in `plugins/nlT-Structures/`.

| File or folder | What it is |
|---|---|
| `messages.yml` | Every message the plugin shows in chat, in MiniMessage (https://docs.advntr.dev/minimessage/format.html) format |
| `status.txt` | The monument's current status. Rewritten by the plugin whenever something changes |
| `commands.txt` | Every command with its full explanation. Rewritten by the plugin on every start |
| `monuments/monuments.yml` | The monument's settings; see section 8 (All settings) |
| `monuments/state.yml` | Whether the monument is active, and when the next check is |
| `monuments/current.yml` | What's built now: the variant or seed, the sponge count and the monument's ID |
| `backups/` | Older copies of `monuments.yml` and `messages.yml` |

**Which files you can edit**

- `monuments.yml`: yes. Run `/nlts monument reload` afterwards.
- `messages.yml`: yes. Run `/nlts reload` afterwards. All messages start with the `[nlT-Structures]` prefix, set near the top of the file.
- `status.txt` and `commands.txt`: no. They're read-only and the plugin overwrites them.
- `state.yml` and `current.yml`: leave these alone. Use the commands instead.

**What happens to the files when you update the plugin**

- **`monuments.yml`:** new settings are added with their default values, settings that are no longer used are removed, and your own values are kept. Your `variants` list is kept exactly as you have it. This also happens on `/nlts monument reload`. A file with a mistake, or without a valid location, is never changed: the monument pauses and says what's wrong, as described in section 3 (How it works).
- **`messages.yml`:** when an update changes the wording of messages, your file is replaced with the new one. If you had changed messages yourself, copy them back from the backup. Messages that are only *added* by an update work without the file being replaced.
- **Backups:** before either file is changed, the old one is moved to `backups/` with the date and time in its name, such as `monuments-2026-10-22_14-30-05.yml`. A `messages-backup.yml` left by an older version is moved there too.
- **`state.yml` and `current.yml`** are never touched by an update, so the monument, its countdown and existing Monument Hoes carry on as before.

The status lines of `/nlts monument status` and the name of the Monument Hoe are not in `messages.yml`; they can't be changed.


# 11. Troubleshooting

| Problem | What to check |
|---|---|
| "Unknown command" or no permission for `/nlts` | You need `nltstructures.admin`. Operators don't have it automatically; see section 2 (Permissions) |
| Nothing is ever built | The monument is inactive, as on every new server. Check the location, then run `/nlts monument active on` |
| The status shows a **Problem** line, or "Paused" | `monuments.yml` has a mistake; the line and the console say what. Fix it and run `/nlts monument reload` |
| `active on` is refused | `monuments.yml` has a mistake. Fix it and reload first |
| `setcenter` or `broadcast` says "Not saved" | The same: the plugin won't save over a file that has a mistake |
| "World ... is not loaded" | `location.world` in `monuments.yml` doesn't match a loaded world. Correct it, or use `setcenter` in the right world |
| A check says "Not cleared" | Elder Guardians are still alive. `/nlts monument reset` forces a reset anyway |
| The monument is cleared but doesn't reset | A player is still within the check radius. It resets at a later check, once everyone has left |
| The new monument is one block off | The centre is in the wrong spot. Stand on the corner where the 4 centre blocks meet, run `setcenter`, then `reset` |
| "The templates for generated monuments didn't load" | The jar is incomplete. Download or build it again |
| A variant is skipped at startup | A name under `variants` in `monuments.yml` has no structure files in the jar. Remove or correct that line |
| Nobody got a Monument Hoe | Only the killer of the *last* Elder Guardian gets one, and only while the monument is active and `reward.enabled` is `true` |
| A player's hoe disappeared | It was used outside the monument, or it came from a monument that has since been reset |
| Players can't mine sponges even with the hoe | A protection plugin is blocking block breaking there; see section 9 (The Monument Hoe) |
| Sponges can be mined by hand | The monument is inactive or paused, so the hoe rules are off |
| A change in `messages.yml` doesn't show | Run `/nlts reload`. Check the console for a YAML error if it still doesn't |
