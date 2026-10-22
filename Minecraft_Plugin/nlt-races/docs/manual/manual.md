
# 1. Installing and updating

**Requirements**

- Paper 26.2 or newer (or a Paper fork such as Purpur), running on Java 25. It doesn't run on plain Spigot.
- Optional: **PlaceholderAPI**, for the leaderboard placeholders.
- Optional: a hologram plugin such as **DecentHolograms**, to show the leaderboards in the world.
- A permissions plugin such as **LuckPerms**, to let players use the race buttons.

**Installing**

1. Put `nlT-Races-<version>.jar` in the server's `plugins` folder.
2. Start the server. On the first start, the server downloads one library the plugin needs (HikariCP), so it needs internet access then.
3. The plugin creates `plugins/nlT-Races/` with `config.yml`, `messages.yml` and a `races` folder.

Times are saved in a SQLite file (`races.db`) by default, which needs no setup. To use MySQL or MariaDB instead, fill in the `database` section of `config.yml` and restart.

**Updating**

1. Stop the server.
2. Delete the old nlT-Races jar from `plugins` and put the new one there.
3. Start the server.

`config.yml` and `messages.yml` update themselves; see section 11 (Files). Always stop and start the server; don't use `/reload` or a plugin manager to swap the jar.


# 2. Permissions

| Permission | Who it's for | What it allows |
|---|---|---|
| `nltraces.user` | Players | Using race buttons (prep, join, start, lobby), `/races lobby`, and cancelling a race they lead |
| `nltraces.admin` | Admins | Every command. Includes `nltraces.user` |

Both are given to operators by default. **Regular players can't press race buttons until you give them `nltraces.user`.** With LuckPerms:

```
/lp group default permission set nltraces.user true
```


# 3. How races work

Every race (also called a course) has a **format**, a **travel mode** and a **rules** setting.

## Formats

| Format | Players | How it works |
|---|---|---|
| `solo` | 1 at a time | A time trial. The player presses the start button, is taken to the start, and the timer starts when they cross the start checkpoint. |
| `group` | 2 or more | One player prepares the race and becomes its leader, others join, and the leader starts it. After a countdown everyone races from a starting grid. The top 3 go to the podium. |
| `points` | 2 or more | Starts like a group race. Instead of a track, a zone marked by a beam appears at a random spot. The first player to reach it scores a point, then a new zone appears. |

## Travel modes

| Travel | Meaning |
|---|---|
| `foot` | Running only. No riding, no elytra. |
| `elytra` | Flying. Players bring their own elytra and rockets. |
| `vehicle` | The plugin gives every racer a vehicle and removes it afterwards. You choose the type when creating the race. |
| `free` | Anything goes: no travel rules. |

Vehicle types: `boat`, `minecart`, `horse`, `donkey`, `mule`, `camel`, `pig`, `strider`, `happy-ghast`, `nautilus`.

## Rules

- `strict` (default): getting off your vehicle, or landing in an elytra race, disqualifies you at once.
- `lenient`: you get a few seconds (`remount-time`) to get back on or take off again.

## What a race is made of

| Part | Used by | What it is |
|---|---|---|
| Lobby | all | Where players go after finishing, leaving or being disqualified |
| Start location | solo | Where the player is placed when they press Start |
| Starting grid | group, points | One spot per player |
| Podium 1, 2, 3 | group, points | Where the top 3 are placed at the end |
| Start and finish checkpoints | solo, group | Areas the racer must pass through. The time runs from the start checkpoint (solo) or from GO (group) to the finish |
| Middle checkpoints | solo, group (optional) | Areas that must be passed in order, so nobody can cut the track |
| Point zones | points | The spots where a scoring zone can appear (at least 2) |
| Buttons | all | Blocks players use: **start**, **lobby**, and for group and points races **prep** and **join** |
| Gate lever | all (optional) | A lever the plugin switches on at the start, to open a redstone gate |

Any block can be a button: a real button, a pressure plate, a sign, or any other block. Players right-click it, or step on it if it's a pressure plate.

## What players do

**Solo race**

1. Press the **start** button. You're taken to the start (already on your vehicle in a vehicle race).
2. Cross the start line; the timer starts.
3. Pass every checkpoint in order and cross the finish. You're taken back to the lobby.

Only one player can be on a solo course at a time. Others see *"This course is being used"* until it's free.

**Group or points race**

1. One player presses **prep**. They're now the race leader, and everyone in that world is told a race is starting.
2. Other players press **join**. They're placed on the starting grid.
3. The leader presses **start**. The leader is placed on the grid and the countdown runs.
4. At GO, race. In a group race, cross the start line, pass the checkpoints in order and finish. In a points race, reach the beam before the others.
5. At the end, the top 3 are placed on the podium and everyone gets the results in chat.

Good to know:

- The **lobby** button or `/races lobby` takes you out of a race.
- The leader can cancel their race by pressing **prep** again or with `/races cancel`.
- Elytra races: you must be wearing an elytra to join. After the start you have a few seconds to take off.
- Pig and strider races: your main hand must be empty when you join, because you're given the steering stick.
- A group race ends when everyone has finished, or 60 seconds after the first finisher (`dnf-after-first-finish`). Anyone still racing then is marked DNF (did not finish).


# 4. Tutorial: set up your first race

This sets up a solo foot race called `parkour-1`. Group and points races work the same way with a few more steps.

All commands start with `/nltraces`. The shorter `/races` and `/nltr` do the same; this manual uses `/races`.

## Step 1: build the course

Build the track first. Decide where the lobby is, where the start button goes, and where the start and finish lines are.

## Step 2: create the race

```
/races create parkour-1 solo foot
```

- The **name** (`parkour-1`) is the race's ID. It may only contain lowercase letters, numbers and `-`, up to 32 characters. You can give it a nicer display name later.
- For a vehicle race, add the vehicle: `/races create boat-rally group vehicle boat`.

The setup guide starts straight away.

## Step 3: follow the setup guide

The guide asks for one item at a time. **Right-click the block** it asks for; every click is saved immediately. Under each step are clickable buttons:

| Button | What it does |
|---|---|
| `[◀ Back]` | Goes back one step. Your next click replaces that item. |
| `[↶ Undo]` | On steps where you place several items: removes the last one you placed. |
| `[Next ▶]` | Skips to the next step, keeping what's there. |
| `[Finish ■]` | Stops the guide. Everything you set is kept. |

The steps for each format:

| Format | Steps, in order |
|---|---|
| Solo | lobby → start button → start location → start checkpoint → middle checkpoints → finish checkpoint → lobby button |
| Group | lobby → podium 1, 2, 3 → prep, join and start buttons → grid spots → start checkpoint → middle checkpoints → finish checkpoint → lobby button |
| Points | lobby → podium 1, 2, 3 → prep, join and start buttons → grid spots → point zones → lobby button |

How to click each kind of item:

- **Locations** (lobby, start location, grid spots, podiums): right-click the block players should stand on. They'll face the direction you're looking, rounded to the nearest 45°.
- **Checkpoints**: right-click two opposite corners of the area. If both corners are at the same height (a line on the floor or on water), the checkpoint covers those blocks and the 3 blocks above them, so walking, riding or boating over it all count.
- **Grid spots, middle checkpoints, point zones**: add as many as you need, then click `[Next ▶]`. Click grid spots in starting order: spot 1 is the leader's.
- **Buttons**: right-click the block that should be the button.
- **In the air** (for elytra courses): hold any item and right-click the air. The block you're standing on, or the spot at your feet if you're hovering, is used.

While you set up, the blocks you click are shown as coloured marker blocks. Only you see them and the world isn't changed. Your right-clicks also don't press buttons or place blocks during setup.

**Solo races:** the start location must be *outside* the start checkpoint, just behind the start line. The timer starts when the player enters the checkpoint.

**Vehicle races:** make sure the start suits the vehicle: water for boats and the nautilus, rails for minecarts, lava for striders.

## Step 4: check the race

```
/races info parkour-1
```

This shows everything that's set. At the bottom it says either *"This race is ready to use"* or lists what's still missing, with the command for each item.

If something is missing, run `/races setup parkour-1` again. It only asks for what's still needed.

## Step 5: test it

Press the start button and run the course. If players can't use the buttons, give them `nltraces.user` (see section 2, Permissions).

## Optional extras

- **Display name:** `/races edit parkour-1 name Parkour Course 1`
- **Gate:** wire a lever to a redstone gate, look at the lever and run `/races link parkour-1 gate`. The plugin switches it on when the race starts and off when it ends. Players can't flip it by hand.
- **Checkpoint particles:** see section 8 (Race settings).


# 5. Tutorial: change an existing race

A race can't be changed while someone is racing on it. Wait for it to finish, or cancel it with `/races cancel <race>`.

## Go through the whole setup again

```
/races setup parkour-1 start
```

This walks through every step and shows the current value of each. Click `[Next ▶]` to keep an item, or right-click a block to replace it.

For grid spots and point zones, the existing ones are shown as markers. `[Next ▶]` keeps them all; your first right-click removes them and starts a new set.

## Change one thing

| To change | Command |
|---|---|
| The lobby | `/races set <race> lobby` then right-click |
| The solo start location | `/races set <race> start` |
| A podium place | `/races set <race> podium <1-3>` |
| Add one grid spot | `/races set <race> grid addslot` |
| Use an area as the grid instead of single spots | `/races set <race> grid region` then right-click 2 corners |
| The maximum number of players | `/races set <race> grid max <number>` |
| Remove the grid | `/races set <race> grid clear` |
| The start or finish checkpoint | `/races set <race> checkpoint start` or `finish` |
| Add a middle checkpoint at the end | `/races set <race> checkpoint add` |
| Remove a middle checkpoint | `/races set <race> checkpoint remove <number>` |
| Remove all middle checkpoints | `/races set <race> checkpoint clear` |
| Add a point zone | `/races set <race> pointzone add [radius]` (default radius 3) |
| Remove a point zone | `/races set <race> pointzone remove <number>` or `clear` |

`/races info <race>` shows the numbers of the checkpoints and point zones. To stop selecting blocks without changing anything, use `/races cancel`.

A race uses either single grid spots or a grid area, not both.

## Move or replace a button

1. Look at the old button and run `/races unlink`.
2. Look at the new block and run `/races link <race> <prep|start|join|lobby|gate>`.

A race can have several buttons for the same action, for example a lobby button at the finish and another halfway.

## Change a setting

```
/races edit <race> <setting> <value>
```

For example `/races edit parkour-1 max-time 300` or `/races edit boat-rally rules lenient`. All settings are listed in section 8 (Race settings).

## Clear the times or delete the race

- `/races cleartimes <race>` deletes all recorded times of a race and keeps the race. Its leaderboards become empty.
- `/races delete <race>` deletes the race **and** all its times.

Both ask you to confirm by clicking a button in chat. Neither can be undone.

## Edit the race file by hand

Each race is a file in `plugins/nlT-Races/races/`. After editing one, run `/races reload`.


# 6. Tutorial: leaderboard holograms

This needs PlaceholderAPI and a hologram plugin. The example uses DecentHolograms.

Check first that the placeholders work. Replace `parkour-1` with your race's ID:

```
/papi parse me %nltraces_parkour-1_day_1%
```

It should show a name and time, or `---` if nobody has a time today.

## A simple daily top 3

```
/dh create parkour-top &6Today's best
/dh line add parkour-top 1 &e1. %nltraces_parkour-1_day_1%
/dh line add parkour-top 1 &72. %nltraces_parkour-1_day_2%
/dh line add parkour-top 1 &73. %nltraces_parkour-1_day_3%
```

## A board that switches between day, week and month

The `cycle` placeholders show the daily top 5, then the weekly, then the monthly, each for 20 seconds. All holograms switch at the same moment.

```
/dh create parkour-cycle &e%nltraces_cycle_title%
/dh line add parkour-cycle 1 &e%nltraces_parkour-1_cycle_1_name% &7- &f%nltraces_parkour-1_cycle_1_value%
/dh line add parkour-cycle 1 &b%nltraces_parkour-1_cycle_2_name% &7- &f%nltraces_parkour-1_cycle_2_value%
/dh line add parkour-cycle 1 &6%nltraces_parkour-1_cycle_3_name% &7- &f%nltraces_parkour-1_cycle_3_value%
/dh line add parkour-cycle 1 &7Your best: %nltraces_parkour-1_player_pb_value%
```

- `%nltraces_cycle_title%` has no race in it, so one title hologram can sit above the boards of several courses.
- The switching time and the three titles are set under `placeholders.cycle` in `config.yml`.
- The `player_...` placeholders show the result of whoever is looking at the hologram.

All placeholders are listed in section 9 (All placeholders).


# 7. All commands

`/nltraces`, `/races` and `/nltr` are the same command. All commands need `nltraces.admin` unless noted. Tab completion suggests race names and options everywhere.

## Managing races

| Command | What it does |
|---|---|
| `/races help` | Lists the commands you can use |
| `/races create <race> <format> <travel> [vehicle]` | Creates a race and starts its setup guide. Format: `solo`, `group`, `points`. Travel: `foot`, `elytra`, `vehicle`, `free` |
| `/races list` | Lists all races, with whether each is ready |
| `/races info <race>` | Shows a race's setup and what's still missing |
| `/races edit <race> <setting> <value>` | Changes a setting; see section 8 (Race settings) |
| `/races delete <race>` | Deletes a race and all its times (asks to confirm) |
| `/races cleartimes <race>` | Deletes all recorded times of a race; the race stays (asks to confirm) |
| `/races reload` | Re-reads `config.yml`, `messages.yml` and the race files. Refused while a race is running. Database settings need a restart |

## Setting up

| Command | What it does |
|---|---|
| `/races setup <race>` | Starts the setup guide for whatever is still missing |
| `/races setup <race> start` | Goes through every step of the guide again |
| `/races setup <race> back` · `next` · `finish` | The guide's buttons, as commands |
| `/races set <race> lobby` | Sets the lobby by right-clicking |
| `/races set <race> start` | Sets the solo start location |
| `/races set <race> podium <1-3>` | Sets a podium place |
| `/races set <race> grid addslot` · `region` · `max <n>` · `clear` | Changes the starting grid |
| `/races set <race> checkpoint start` · `finish` · `add` · `remove <n>` · `clear` | Changes the checkpoints |
| `/races set <race> pointzone add [radius]` · `remove <n>` · `clear` | Changes the point zones |
| `/races link <race> <prep\|start\|join\|lobby\|gate>` | Links the block you're looking at (within 5 blocks) as a button. `gate` must be a lever |
| `/races unlink` | Unlinks the block you're looking at |

## During races

| Command | Permission | What it does |
|---|---|---|
| `/races lobby` | `nltraces.user` | Leaves your race and takes you to its lobby. The race goes on without you |
| `/races cancel` | `nltraces.user`, as leader | Cancels the race you lead |
| `/races cancel` | `nltraces.admin` | Stops selecting blocks, if you were setting something up |
| `/races cancel <race>` | `nltraces.admin` | Cancels a running race. Everyone goes to the lobby and nothing is recorded |


# 8. Race settings

Change these with `/races edit <race> <setting> <value>`.

## General settings

| Setting | Values | Meaning |
|---|---|---|
| `name` | any text up to 48 characters, or `reset` | The display name players see in messages. Commands and placeholders keep using the ID |
| `format` | `solo`, `group`, `points` | The race format |
| `travel` | `foot`, `elytra`, `vehicle`, `free` | The travel mode |
| `vehicle` | `boat`, `minecart`, `horse`, `donkey`, `mule`, `camel`, `pig`, `strider`, `happy-ghast`, `nautilus` | The vehicle, for vehicle races |
| `boat` | `oak`, `spruce`, `birch`, `jungle`, `acacia`, `dark-oak`, `mangrove`, `cherry`, `pale-oak`, `bamboo` | The wood of the boats (`bamboo` is a raft) |
| `rules` | `strict`, `lenient` | See section 10 (Rules) |
| `checkpoint-particles` | `on`, `off`, `auto` | Shows each racer particles at their next checkpoint. `auto` (default) is on when the race has middle checkpoints |

After changing the format or travel mode, run `/races info <race>`: the race may now need other items, such as a grid or a vehicle type.

## Number settings

| Setting | Default | Range | Used by | Meaning |
|---|---|---|---|---|
| `max-time` | 600 | 10–86400 | solo | Seconds before an unfinished run ends as DNF |
| `countdown` | 3 | 1–30 | group, points | Seconds of countdown before GO |
| `min-players` | 2 | 1–100 | group, points | Players needed before the leader can start, leader included |
| `prep-timeout` | 300 | 30–3600 | group, points | Seconds after prep before the race cancels itself if it isn't started |
| `dnf-after-first-finish` | 60 | 0–3600 | group | Seconds the others have left after the first finisher |
| `time-limit` | 300 | 10–86400 | points | Length of the game in seconds |
| `points-to-win` | 10 | 1–1000 | points | The game ends as soon as someone has this many points |
| `remount-time` | 5 | 1–60 | lenient rules | Seconds to get back on a vehicle or take off again |
| `takeoff-time` | 10 | 1–120 | elytra | Seconds racers may stay on the ground after the start |

The maximum number of players is not a setting here: it's the number of grid spots, or `/races set <race> grid max <n>` (default 8, used with a grid area).


# 9. All placeholders

These need PlaceholderAPI. Replace `<race>` with the race's ID (not its display name).

## Leaderboards

| Placeholder | Shows |
|---|---|
| `%nltraces_<race>_day_<1-5>%` | Today's top 5: position 1 to 5 |
| `%nltraces_<race>_week_<1-5>%` | This week's top 5 |
| `%nltraces_<race>_month_<1-5>%` | This month's top 5 |
| `%nltraces_<race>_cycle_<1-5>%` | The day, week and month top 5 in turn |
| `%nltraces_cycle_title%` | The title of the period the cycle placeholders show now. No race in it |
| `%nltraces_<race>_last_<1-10>%` | The results of the last group or points race: finishers in order, then DQ, then DNF. Empty for solo races |

## The viewing player's own results

| Placeholder | Shows |
|---|---|
| `%nltraces_<race>_player_pb%` | Their all-time best |
| `%nltraces_<race>_player_day%` · `_player_week%` · `_player_month%` | Their best today, this week, this month |
| `%nltraces_<race>_player_cycle%` | Their best in the period the cycle shows now |
| `%nltraces_<race>_player_last%` | Their most recent run |

## Name only or value only

Every placeholder above, except `cycle_title`, shows a name and a result together, like `Steve - 23.456`. Add `_name` or `_value` to get one part:

| Placeholder | Shows |
|---|---|
| `%nltraces_parkour-1_day_1%` | `Steve - 23.456` |
| `%nltraces_parkour-1_day_1_name%` | `Steve` |
| `%nltraces_parkour-1_day_1_value%` | `23.456` |

## How values look

- **Times:** `23.456` under a minute, `1:23.45` from a minute on.
- **Points races:** `7 pts`.
- **Not finished:** `DQ` or `DNF` (only in `last` and `player_last`).
- **No result:** `---`.

The texts `{name} - {value}`, `pts`, `DQ`, `DNF` and `---` can be changed under `placeholders` in `config.yml`.

## How the leaderboards count

- Each player appears once per board, with their best result in that period.
- Only finished runs count. Timed races: the lowest time wins. Points races: the most points in one race wins.
- Days, weeks and months follow the server's time zone, and weeks start on Monday. Both can be changed under `leaderboards` in `config.yml`.
- Boards update right after each race and when a new day starts.


# 10. Rules and disqualification

A disqualified (DQ) player gets a message saying why, loses their vehicle and is taken to the lobby.

| What the player does | Result | Applies to |
|---|---|---|
| Teleports (commands, ender pearls, chorus fruit, portals) | DQ | all races |
| Flies in creative, or changes game mode | DQ | all races |
| Dies, logs out, or leaves the world | DQ | all races |
| Rides anything or uses an elytra | DQ | foot |
| Gets off their vehicle | DQ at once (strict), or after `remount-time` (lenient) | vehicle |
| Lands | DQ at once (strict), or if not flying again within `remount-time` (lenient) | elytra |
| Doesn't take off within `takeoff-time` after the start | DQ | elytra |
| Passes a checkpoint or point zone while not riding or not flying | It doesn't count; a warning shows | vehicle, elytra |
| Skips a checkpoint | The next ones don't count until they go back | solo, group |

`free` races only have the rules for all races.

Other outcomes:

- **DNF** (did not finish): the time ran out, or the player left with the lobby button.
- **Before GO** in a group or points race, a player who leaves or is disqualified is just removed, and nothing is recorded.
- If the leader leaves before the race starts, the race is cancelled.


# 11. Files: config, messages, races, backups

Everything is in `plugins/nlT-Races/`.

| File or folder | What it is |
|---|---|
| `config.yml` | Database, leaderboard periods, placeholder texts, marker blocks and particles. Every setting is explained in the file |
| `messages.yml` | Every message the plugin shows, in MiniMessage (https://docs.advntr.dev/minimessage/format.html) format |
| `races/<race>.yml` | One file per race |
| `races.db` | The recorded times (when using SQLite) |
| `backups/` | Older copies of `config.yml` and `messages.yml` |

After editing `config.yml`, `messages.yml` or a race file, run `/races reload`. Database settings need a restart.

**What happens to the files when you update the plugin**

- **`config.yml`:** new settings are added with their default values, and your own values are kept. A `config.yml` with a mistake in it (such as wrong indentation) is never changed; see section 12 (Troubleshooting).
- **`messages.yml`:** when an update changes the wording of messages, your file is replaced with the new one. If you had changed messages yourself, copy them back from the backup.
- **Backups:** before either file is changed, the old one is moved to `backups/` with the date and time in its name, such as `config-2026-10-10_14-30-05.yml`.
- **Race files and times** are never touched by an update.

In `messages.yml`, `<race>` is a race's display name and `<race-id>` is its ID.


# 12. Troubleshooting

| Problem | What to check |
|---|---|
| Players can't use the buttons | Give them `nltraces.user` |
| "The race isn't fully set up yet" | Run `/races info <race>` to see what's missing |
| "This course is being used" | Someone is racing on it. An admin can stop it with `/races cancel <race>` |
| The solo timer doesn't start | The start location must be outside the start checkpoint, and the player must walk *into* the checkpoint |
| A checkpoint doesn't count | Checkpoints must be passed in order. In vehicle and elytra races the player must be riding or flying when passing it |
| The leader can't start | The race needs `min-players` players (default 2), leader included |
| Players can't join an elytra race | They must be wearing an elytra |
| Players can't join a pig or strider race | Their main hand must be empty |
| A placeholder shows its own text, like `%nltraces_...%` | PlaceholderAPI isn't installed, the race ID is misspelled, or the placeholder is mistyped. Test it with `/papi parse me <placeholder>` |
| A placeholder shows `---` | There's no result for that position yet |
| The cycle title shows `day`, `week` or `month` | The `placeholders.cycle.titles` section is missing from `config.yml`. Run `/races reload` to have it added |
| nlT-Races doesn't start, and the console says `config.yml can't be read` | `config.yml` has a mistake, usually wrong indentation or a missing quote. The console shows the line. Fix it and restart. The plugin doesn't start on default settings, because those could, for example, save times to a new SQLite file instead of your MySQL database |
| `/races reload` says `config.yml can't be read` | Same cause. Until it's fixed, the previous settings stay in use; `messages.yml` and the race files were still reloaded |
| A change to a race is refused | The race is in use. Wait, or cancel it with `/races cancel <race>` |
