# nlT-Races — Design

Status: agreed 2026-10-08. Change this file when decisions change.

## Technical setup

- **Platform:** Paper API `26.2.build.132-stable` (minimum Paper 26.2). It runs on Paper and Paper forks such as Purpur, not on plain Spigot. It's a classic `plugin.yml` plugin using the Paper API, with Bukkit-style commands. Changed from Spigot on 2026-10-08.
- **Java:** Java 25, which Paper 26.x requires and its API is compiled for. Built with Gradle 9.1 through the wrapper (`gradlew`).
- **Messages:** MiniMessage format (`<red>`, `<gradient>`, hover/click), sent with Paper's built-in Adventure library. Values such as player names are inserted as plain text.
- **Package:** `com.nltarzan.races`. Plugin name: `nlT-Races`.
- **Database:** SQLite by default, MySQL/MariaDB optional. HikariCP is downloaded by the server through `libraries:` in `plugin.yml`. All database work runs off the main thread.
- **PlaceholderAPI:** optional (softdepend).

## Races

Each race (course) is stored in `races/<name>.yml`. Every race is in its own location, with its own checkpoints and buttons.

| Field | Values |
|---|---|
| format | `SOLO`, `GROUP`, `POINTS` |
| travel | `FOOT`, `ELYTRA`, `VEHICLE` (+ vehicle type), `FREE` |
| rules | `STRICT`, `LENIENT` |
| locations | lobby, solo start, grid (region or manual slots, plus max players), podium 1/2/3 |
| checkpoints | Start, middle (in order), Finish. Not used for Points |
| point zones | fixed locations + radius. Points only |
| buttons | linked blocks for prep / start / join / lobby |
| settings | `countdown` (3 s), `remount-time` (5 s), `max-time` (solo), `dnf-after-first-finish` (60 s, group), `time-limit` + `points-to-win` (points) |

Editor rules (decided in step 2):

- **Race names:** only `a-z`, `0-9` and `-`. No `_`, because placeholders use `_` as a separator.
- **Grid:** a race uses either a grid region or manual grid slots, not both. `grid max` defaults to 8.
- **Point zones:** a points race needs at least 2, so a new random zone can be picked. The default radius is 3 blocks.
- **Ready check:** a race is "ready" once it has everything its format needs: a lobby, a vehicle type for vehicle races, a solo start or grid + podium 1–3, start/finish checkpoints or point zones, and the prep/start/join buttons its format uses. `/nltraces info <race>` lists whatever is missing.
- **Defaults:** `max-time` 600 s, `time-limit` 300 s, `points-to-win` 10.
- **Reload:** `reload` was brought forward from step 9, so hand-edited race files can be applied without a restart.
- **Commands reworked (2026-10-08):** `pos1`/`pos2`, `setlobby`, `setstart`, `setpodium`, `grid`, `checkpoint` and `pointzone` were replaced by `set` and the right-click `setup` guide. The old `set` is now `edit`.

A race is locked whenever it isn't idle, including during a solo run. Pressing its buttons then shows *"This course is being used, please wait or try a different course."*

## Flows

### Solo
- **Start button:** teleports the player to the start location, already mounted if it's a vehicle race.
- **Timing:** the timer starts when the player crosses the Start checkpoint. Middle checkpoints must be passed in order, then the Finish.
- **Finish:** the player is teleported to the lobby.
- **Time limit:** going over `max-time` means DNF and a teleport to the lobby.

### Group
- **Prep button:** announced to players in the same world. Whoever presses it is the race leader and stays where they are.
- **Join button:** non-leaders are teleported to a free grid slot, already mounted.
- **Start button:** leader only. The leader is placed on the grid, then the countdown runs for everyone. The timer starts at GO.
- **Valid run:** players must still cross the Start checkpoint, then the middle checkpoints in order, then the Finish.
- **Finishing:** finishers are teleported to the lobby.
- **End of race:** the race ends when the last player finishes, or `dnf-after-first-finish` after the first finisher. Anyone still racing then is DNF. At the end, the top 3 are teleported to the podium.

### Points
- **Start:** prep, join and start work as in Group races.
- **Zones:** one random point zone from the fixed list is active at a time, marked with a particle beam (no blocks are changed). The particle type is configurable.
- **Scoring:** the first player to reach the active zone gets 1 point, then another zone appears.
- **End:** the game ends at `points-to-win` or when `time-limit` runs out.
- **Ranking:** players are ranked by points. Ties go to whoever reached that score first. The top 3 go to the podium.

## Travel modes and DQ rules

A DQ always means a message (editable in `messages.yml`), the vehicle removed, and a teleport to the lobby.

| Rule | Applies to |
|---|---|
| Teleport, ender pearl, chorus fruit | all |
| Creative flight, gamemode change | all |
| Death, logout, leaving the world | all |
| Mounting anything or gliding | Foot |
| Dismounting (strict) / not remounted within `remount-time` (lenient) | Vehicle |
| Remounting someone else's vehicle | Vehicle (blocked) |
| Checkpoint passed while not mounted | Vehicle (doesn't count) |
| Landing (strict) / not airborne again within `remount-time` (lenient) | Elytra, after the Start checkpoint (Points: from GO) |
| Checkpoint passed while not gliding | Elytra (doesn't count) |

### Elytra
- Players bring their own elytra and rockets.
- Joining or starting is refused if the player isn't wearing an elytra.
- Rockets are always allowed.

### Vehicles
- The plugin spawns the vehicle with the player already mounted, and removes it on finish or DQ.
- Types: boat, minecart, horse, donkey, mule, camel, pig, strider, happy ghast, nautilus.
- The plugin supplies the saddle and the steering item where needed.

## Database

| Table | Contents |
|---|---|
| `races` | one row per race: id, course, format, travel, vehicle type, started_at, ended_at |
| `runs` | one row per player per race: race id, course, player UUID and name, status (`FINISHED` / `DQ` / `DNF`), DQ reason, time, points, placing, completed_at |
| `run_splits` | checkpoint times for each run |

- **Bests:** day, week and month bests are worked out from `runs`, so nothing needs resetting.
- **Periods:** they follow the server's time zone, and weeks start on Monday. Both are configurable in `config.yml`.

## Placeholders (PlaceholderAPI)

`<race>` is the race name.

| Placeholder | Content |
|---|---|
| `%nltraces_<race>_last_<1-10>%` | Last group/points race: name + time/points, with DQ/DNF players at the bottom. Not used for solo races |
| `%nltraces_<race>_day_<1-5>%`, `_week_<1-5>%`, `_month_<1-5>%` | One entry per player per period |
| `%nltraces_<race>_player_last%` | The viewing player's most recent run |
| `%nltraces_<race>_player_day%`, `_player_week%`, `_player_month%` | The viewing player's best that day / week / month |
| `%nltraces_<race>_player_pb%` | The viewing player's all-time best |

- **Name/value split:** each placeholder also has `_name` and `_value` versions.
- **Value format:** values show as `1:23.456`, `7 pts`, `DQ` or `DNF`. Empty positions show a configurable placeholder (default `---`).
- **Caching:** placeholders are served from memory and refreshed after each race and when a period rolls over.

## Commands and permissions

- **Player permission:** `nltraces.user`, needed to use the race buttons.
- **Admin permission:** `nltraces.admin`, needed for every command except `lobby` (needs `nltraces.user`) and `cancel` for a leader's own race.
- **Command:** `/nltraces`, with aliases `/races` and `/nltr`.

| Command | Purpose |
|---|---|
| `create <race> <format> <travel> [vehicle]` | Create a race. Players then go straight into `setup` |
| `setup <race> [back\|next\|finish\|start]` | Guided right-click setup (see below) |
| `set <race> lobby \| start \| podium <1-3>` | Set one location by right-clicking |
| `set <race> grid region \| addslot \| max <n> \| clear` | Starting grid |
| `set <race> checkpoint start \| finish \| add \| remove <n> \| clear` | Checkpoints (right-click 2 corners) |
| `set <race> pointzone add [radius] \| remove <n> \| clear` | Point zones |
| `edit <race> <setting> <value>` | format, travel, vehicle, boat (wood type), rules and the number settings |
| `link <race> <prep\|start\|join\|lobby\|gate>` · `unlink` | Link or unlink the block you're looking at (`gate`: a lever, see below) |
| `delete` · `list` · `info` · `reload` | Manage races. `delete` also removes all of the race's recorded times |
| `cleartimes <race> confirm` | Delete all recorded times of a race; the race is kept. Clickable confirm |
| `cancel` · `cancel <race>` | Stop selecting blocks · cancel a race (everyone to the lobby, nothing recorded). Admins: any race. Race leaders (`nltraces.user`): only the race they lead; `cancel` is only offered to them while they lead one |
| `lobby` (players, `nltraces.user`) | Leave your race and go to its lobby; the race goes on without you |

### Right-click setup (decided 2026-10-08)

- **Selecting a block:** right-clicking a block in reach uses that block. Right-clicking the air (only reported by Minecraft while holding an item) uses the block the admin stands on, or the one at their feet when hovering midair, so spots can be set in the air. Button steps still need a real block.
- **Clicked locations:** a selected block gives the middle of its top, facing the way the admin looks. Facing is kept only where players are placed (lobby, solo start, grid spots, podiums). **Point zones are a position only**, saved as `world, x, y, z, radius` with no yaw or pitch. The direction is snapped to the nearest 45° (N, NE, E, …) and the pitch is always 0.
- **Regions:** two clicked corner blocks, both included. If both corners are at the same height (a floor or water surface), the region covers the clicked layer **plus** the 3 blocks above it, so walking over it, riding over it or sitting low in a boat all count. (Changed 2026-10-08; flat checkpoints set before this start one block higher and should be set again.) Grid areas place racers on top of their bottom layer.
- **Saving and cancelling:** every click is saved immediately. While setting up, the admin's right-clicks don't press buttons, open doors or place blocks.
- **Steps per format** (lobby first, lobby button last):
  - **Solo:** lobby, start button, start location, start checkpoint, middle checkpoints (repeat until `next`), finish checkpoint, lobby button.
  - **Group:** lobby, podium 1–3, prep, join and start buttons, grid spots (one per player, repeat until `next`), start checkpoint, middle checkpoints (repeat), finish checkpoint, lobby button.
  - **Points:** lobby, podium 1–3, prep, join and start buttons, grid spots (repeat), point zones (repeat, at least 2), lobby button.
- **Point zones in the guide:** they work exactly like grid spots below. Existing zones are shown as markers; **Next** keeps them, while the first right-click deletes them and starts a new set, with **Undo** for zones placed during this visit. `set <race> pointzone add` (and `grid addslot`) only adds one, and shows the existing ones as markers.
- **Grid spots in the guide:** with no spots yet, clicks add them one by one and the first button is **Undo**, which removes the last spot placed. With spots already saved, they're shown as markers. **Next** keeps them, while right-clicking a block deletes them all and starts a new set. Undo with nothing to remove acts as Back., unless lowered with `set <race> grid max`. A grid region can still be set manually with `set <race> grid region`.
- **Buttons:** each step is a block with a header and clickable `[◀ Back] [Next ▶] [Finish ■]` buttons.
  - `back` returns to the previous step without changing it; the next click replaces its value (on repeating steps, clicks add more). Halfway through a region, `back` returns to its first corner.
  - `next` moves on to the next step.
  - `finish` stops the guide (everything is already saved).
  - `start` goes through every step, showing current values.
- **Continuing a setup:** plain `setup <race>` on a part-finished race shows the overview first, then asks only for what's still missing.
- **New races:** `create` runs the full guide, including optional steps like middle checkpoints. A plain `setup <race>` on a part-finished race asks only for required items that are still missing.
- **Defaults:** number settings keep their defaults and are changed with `edit`. An action-bar line shows the current step while setting up.

## Build steps (test after each)

1. ✅ Skeleton: Gradle wrapper, `plugin.yml`, config, messages, database.
2. ✅ Race editor: commands, regions, locations, button links, saving.
3. ✅ Solo races for Foot and Free: timing, saving runs.
   - **Checkpoint detection:** a checkpoint counts when the player *enters* it. The path moved each tick is checked, and the crossing time is interpolated between ticks. The solo start location must therefore be outside the start checkpoint; `info` flags it if not.
   - **Max time:** `max-time` counts from pressing Start. Running out means DNF (`max-time`).
   - **Leaving:** using a lobby button mid-run is DNF (`left`). Logging out is DQ (`logout`).
   - **Buttons:** pressure plates have a 1-second cooldown. Linked signs don't open the sign editor.
   - **Locking:** while a race runs, edits to it and `/nltraces reload` are refused.
4. ✅ DQ rule engine with the general rules, plus the Foot rules.
   - **Teleports:** cheat teleports (command, plugin, spectate, end gateway, ender pearl, chorus fruit or any other teleporting food, portals) are cancelled, then the racer is DQ'd and sent to the lobby. The plugin's own teleports, dismounting and leaving a bed don't count.
   - **Death:** DQ, and the player respawns at the race's lobby.
   - **Logout:** DQ, saved without a message. The player is moved to the lobby, so they don't log back in on the course.
   - **Flight:** checked every tick. Starting a race turns flight off. Spectators can't start a race.
   - **Message format:** each reason has its own line under `play.dq` in messages.yml, shown as a title and in chat.
5. ✅ Vehicle and Elytra modes, strict and lenient rules.
   - **Boat wood:** boat races have a boat type, `edit <race> boat <wood>`: oak (default), spruce, birch, jungle, acacia, dark-oak, mangrove, cherry, pale-oak or bamboo (a bamboo raft). It's stored as `boat-type` in the race file. They're adult, tamed to the racer, saddled (a harness for the happy ghast), invulnerable and never saved to disk. They're removed whenever the run ends (finish, DQ, DNF, cancel, shutdown).
   - **Steering items:** pig and strider races can only be started (and, in step 6, joined) with an **empty main hand**. The carrot or warped-fungus stick goes into that hand. It can't be dropped or moved, never drops on death, is removed when the run ends, and is removed on join if the server ever stopped mid-race.
   - **Who can ride:** only the owner can ride a race vehicle. Vehicle racers can only mount their own vehicle.
   - **The admin's job:** make sure the start location suits the vehicle (water for boats and nautilus, rails for minecarts, lava for striders).
   - **Elytra:** you can't start without an elytra in the chest slot. Rules apply from the start checkpoint (points races: from GO), which must itself be entered while gliding. Racers start on the ground, so there's a **take-off window**: for `takeoff-time` seconds (default 10) after the rules start, the ground is allowed and the action bar counts down. Anyone who hasn't glided by the end is DQ'd (`no-takeoff`). After the first take-off, the normal strict/lenient landing rules apply.
   - **Checking the rules:** riding or gliding is checked every tick. Strict: getting off or not gliding is an instant DQ (`dismount` / `landed`). Lenient: an action-bar countdown of `remount-time`, then DQ (`remount-timeout` / `takeoff-timeout`). A vanished vehicle is `vehicle-lost`.
   - **Checkpoints:** a checkpoint entered while not riding or gliding doesn't count. A warning shows, at most every 2 s.
6. ✅ Group races: prep, join, grid, countdown, DNF timer, podium.
   - **Prep:** the presser becomes the leader. It's announced to everyone in the leader's world. The leader stays where they are and rules don't apply to them until they press Start.
   - **Join:** joiners are teleported to grid spots 2, 3, … in join order (spot 1 is the leader's). The race is full when the spots run out (or `grid max`). Elytra and empty-hand checks apply, and steering sticks are handed out at Join.
   - **Holding racers until GO:** foot and elytra racers are frozen on their spot but can look around. Paper reports the freeze's "put back" as a teleport, so the teleport rule ignores moves of under 2 blocks for frozen racers. Vehicle racers are spawned already mounted on their spot (at Join; the leader at Start) and held there: ridden animals get zero speed and jump, minecarts zero max speed, and boats are put back if they drift sideways. Only horizontal movement is held, so vehicles can still fall and settle onto water or ground. Moving never causes a DQ.
   - **Getting off before GO:** the same rules as during the race apply (strict: DQ; lenient: remount countdown). A DQ before GO takes the player out of the race and nothing is recorded.
   - **Start (leader only):** needs at least `min-players` (default 2, leader included). The leader is teleported to spot 1, then the `countdown` titles run.
   - **GO:** everyone's timer starts at once and held vehicles get their speed back. The start checkpoint must still be crossed; elytra rules begin there.
   - **Leaving before GO** (lobby button, `/races lobby`, `/tp`, death, logout, …) just removes the player, and nothing is recorded. The race is cancelled only if the leader leaves while it's being prepared, or leaves on purpose (lobby) before GO. A leader DQ'd during the countdown, or leaving after GO, doesn't stop the race. If the leader never presses Start, the race cancels itself after `prep-timeout` (default 300 s).
   - **Finishing:** finishers go to the lobby with their place. The first finisher starts `dnf-after-first-finish`. When everyone is done or time is up, the rest are DNF (`time-up`) and the top 3 are teleported to podiums 1–3.
   - **Results:** everyone who took part gets the results in chat. The race is saved as one `races` row with every run and their placings.
7. ✅ Points races: random zones, beam, scoring.
   - **Starting:** prep, join and start work as in group races. At GO the `time-limit` clock starts, the first random zone appears, and the racers are told the target and time.
   - **Zones:** the active zone is shown to racers as a fake **end gateway** on the block that was clicked when the zone was made with its "just created" beam, sent only to racers and refreshed every 5 s because that beam fades after ~10 s. Nothing in the world changes. It also has a particle ring at its radius (`points-races.ring-particle`, default END_ROD). The next zone is always a different one from the last.
   - **Scoring:** a point counts when a racer comes within the zone's radius of its center, a **sphere** (the particle ring only shows the radius). The path moved each tick is checked. Elytra racers must be gliding and vehicle racers riding (as for checkpoints); elytra rules apply from GO.
   - **Feedback:** a title and sound for the scorer; the others get a chat line and a chime. The action bar shows points, horizontal distance to the zone, and time left.
   - **End:** at `points-to-win` (game ends at once) or when `time-limit` runs out. Everyone still in gets a place by points, ties going to whoever reached that score first. Their `time_ms` is when they reached their final score. The top 3 go to the podium.
   - **Leaving early:** racers who leave or are DQ'd are saved with the points they had, without a place.
8. ✅ PlaceholderAPI with cached leaderboards.
   - **Loading:** each race's leaderboard is built off the main thread from all its saved runs, and kept in memory, so placeholders never touch the database.
   - **Refreshing:** boards are rebuilt after every saved race, on startup and `reload`, and when a day starts (checked every minute; that also covers new weeks and months). A deleted race's board is removed.
   - **Bests:** only FINISHED runs count. Timed races: lowest time, ties to the earlier run. Points races: most points in one race, ties to whoever reached that score sooner.
   - **Names:** each player is shown with their most recent name.
   - **Last race (`last_<1-10>`):** the newest group/points race. Finishers come by placing, then DQ, then DNF. Empty for solo races.
   - **Text:** set under `placeholders:` in config.yml: `empty` (`---`), `entry` (`{name} - {value}`), `points` (`{points} pts`), `dq`, `dnf`. Times look like 1:23.456.
   - **Registration:** placeholders are registered only when PlaceholderAPI is installed (softdepend), with `persist` on.
9. ✅ Polish (awaiting in-game test).
   - **Leader cancel:** a race leader can cancel their own race with `/races cancel`, or by pressing the prep button again, at any stage. For other players the prep button stays locked and `cancel` isn't available.
   - **`cleartimes`:** deletes every recorded race, run and split of a race (with a clickable confirm) and refreshes its leaderboards. It's refused while the race is active.
   - **Tab completion:** `edit <race> <number setting>` suggests the current value. Every subcommand completes race names, options and `confirm` where they apply.

## Messages file updates

`messages.yml` has a `messages-version`. When an update rewords existing messages, that number goes up. On startup, an older server copy is then saved as `messages-backup.yml` and replaced, and a warning is logged.

## Gate levers (decided 2026-10-08)

- **Linking:** any race can have levers linked with `/nltraces link <race> gate`. This is optional, manual-only, and not part of setup or the ready check; the link must be a lever.
- **When they flip:** gate levers switch **on** when the countdown starts (solo: when Start is pressed), giving pistons time to retract before GO. They switch **off** when the race ends or is cancelled.
- **Players:** they can't flip a gate lever by hand.
