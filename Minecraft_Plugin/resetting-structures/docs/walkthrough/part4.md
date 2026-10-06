# Step 4: how a reset happens

The files in this step work together. Here's the whole journey of one check:

```
ResetScheduler        only while active: waits a random interval (saved in state.yml)
      │  time's up
      ▼
MonumentResetter      ── MonumentArena.loadChunks()   keep the area loaded, wait for mobs to load
      │                 Elder Guardians alive?  → "not cleared", stop
      │                 players inside?          → "postponed", stop
      │                 otherwise: place a new monument
      │                   pre-built → StructureLibrary (the 4 quadrant templates)
      │                   generated → MonumentBuilder (step 6)
      │                 CurrentMonument.set(…)   remember what's built + count sponges
      │               ── MonumentArena.releaseChunks()
      ▼
ResetScheduler        picks the next interval, saves state.yml, updates status.txt
```

Two small helpers in `util` are used along the way: `Durations` (times like `1d`, `10s`, `5t`) and `Weighted` (weighted random picks).

## util/Durations.java: times like 1d, 10s, 5t

@code src/main/java/com/nltarzan/structures/util/Durations.java 9-47

- **Lines 11–12:** Minecraft runs at 20 ticks per second, and a Minecraft day is 24,000 ticks. The `_` in `24_000` only makes it easier to read.
- **Lines 14–15:** a `private` constructor means nobody can make a `Durations` object. It's only a toolbox of `static` methods.
- **Line 18:** removes stray spaces and makes `2D` work like `2d`.
- **Lines 19–21:** empty text is an error. "Throwing" an `IllegalArgumentException` reports it to whoever called this method; `MonumentConfig` catches it and logs "Skipping … entry".
- **Line 23:** the last character: the unit. A `char` is a single character.
- **Lines 26–35:** if that last character is a letter, look up how many ticks the unit is worth and cut the letter off. An unknown letter, such as `m`, is an error with a hint. Without a letter, the number is in ticks.
- **Lines 37–42:** turn the number part into a decimal, so `1.5d` also works.
- **Lines 43–45:** zero or negative isn't allowed.
- **Line 46:** multiply and round to whole ticks, never less than 1.

@code src/main/java/com/nltarzan/structures/util/Durations.java 49-64

- **The reverse direction:** ticks become readable text like `1h 4m 30s`, used in the logs and the status.
- **Lines 51–54:** split into hours, minutes and seconds. `/` is whole-number division and `%` is the remainder: 3,725 seconds is 1 hour, `3725 % 3600 = 125` seconds left over, which is 2 minutes and 5 seconds.
- **Lines 56–63:** a `StringBuilder` builds up text piece by piece. Hours only appear when there are any, so you get `45s` or `2m 5s` instead of `0h 0m 45s`.

## util/Weighted.java: weighted random picks

@code src/main/java/com/nltarzan/structures/util/Weighted.java 8-31

- **Line 8:** a record holding a value and its weight. `<T>` makes it **generic**: `T` can be any type. That's how the same class serves the intervals (`Weighted<Long>`) and the variants (`Weighted<String>`).
- **Lines 10–14:** a check that runs every time one is created: no negative weights.
- **Line 18:** adds up all the weights. For the reset intervals that's 30+30+30+10 = 100.
- **Lines 19–21:** if everything weighs 0, there's nothing to pick, so the result is an empty `Optional`.
- **Line 23:** a random number from 0 up to (not including) the total, so 0–99.
- **Lines 24–29:** walk through the entries, subtracting each weight, until the number drops below 0. With 30/30/30/10, a roll of 0–29 lands on 1 day, 30–59 on 2 days, 60–89 on 3 days and 90–99 on 72 days. This is exactly your datapack's `random value 1..100` with its `matches 1..30` ranges, but for any list.
- **Line 30:** the loop always returns before it ends; this line is only there because Java requires every path to end with a `return` or a `throw`.

## ResetScheduler.java: the timer

@code src/main/java/com/nltarzan/structures/monument/ResetScheduler.java 17-45

- **Lines 21–22:** the timer is also where "active" lives: on a new server it doesn't run until `/structure monument active on`.
- **Line 27:** after a failed check, try again in 1 minute.
- **Line 29:** a check that came due while the server was off runs 10 seconds after startup.
- **Lines 31–33:** the monument, the `state.yml` file, and a random number generator for picking intervals.
- **Line 35:** the pending timer. A `BukkitTask` is something Paper will run later, and it can be cancelled. When nothing is planned it's `null`.
- **Lines 36–39:** what's saved in `state.yml`: whether the monument is switched on, the world, and the **game tick** at which the next check happens. `-1` means "not known yet".
- **Lines 41–45:** the constructor sets the file to `monuments/state.yml` and reads it straight away.

@code src/main/java/com/nltarzan/structures/monument/ResetScheduler.java 47-113

- **Lines 48–68, `start()`:** called on startup and on every reload.
  - **Line 49:** first cancel any running timer, so there are never two at once.
  - **Lines 50–52:** switched off? Then stop here: nothing gets planned. Because this check sits inside `start()` itself, every caller (startup, `reload`, `setcenter`) automatically respects it.
  - **Lines 53–58:** if the monument's world isn't loaded, warn and stop.
  - **Line 60:** the world's **game time**, a tick counter that only goes up and only counts while the server runs. That's why the wait "pauses" while the server is off, just like your datapack's `/schedule`.
  - **Lines 61–67:** three cases. No saved time (or it belongs to another world): pick a fresh random interval. The saved time is still ahead: wait the remaining ticks, which keeps the countdown going across restarts. The saved time has passed: check in 10 seconds.
- **Lines 70–75:** cancels the pending timer, if there is one.
- **Lines 77–95, switched on or off** (`/structure monument active`):
  - **Lines 87–89, on:** save it, then start the timer, which picks a fresh random wait.
  - **Lines 90–94, off:** stop the timer and forget the countdown (`-1`) before saving. Otherwise turning it on again later would find an old, long-overdue countdown and check just 10 seconds later.
- **Lines 97–104, `runNow`:** for `check` and `reset`. If a check is already busy, reply "already running". Otherwise cancel the waiting timer and check now. `completedFuture` is a promise that's already fulfilled.
- **Lines 106–113:** for the status: how many ticks are left, or `-1` for "nothing planned".

@code src/main/java/com/nltarzan/structures/monument/ResetScheduler.java 115-151

- **Lines 115–134, one check plus planning the next one:**
  - **Line 116:** let the resetter do the check. `whenComplete` runs once it's finished, which can be many ticks later for a generated build.
  - **Lines 117–119:** don't plan anything while the server is shutting down, or the monument is paused or was switched off in the meantime.
  - **Lines 120–130:** choose the next wait based on the outcome: failed → retry in a minute; players inside → the short `occupied-intervals`; anything else → a normal interval.
  - **Line 132:** report the outcome to `Monuments` for the status's "Last check" line and `status.txt`. This happens after scheduling, so the status also shows the new next check.
- **Lines 136–151, `schedule`:**
  - **Lines 142–144:** work out the game tick at which the next check happens, and save it right away, so a crash doesn't lose the countdown.
  - **Lines 145–148:** ask Paper to run a check after `delayTicks`. The part between `() -> {` and `}` is the code that runs later: forget the old task, then run a normal, unforced check.
  - **Line 149:** log it, e.g. "Next monument check in 1h 4m 30s".

@code src/main/java/com/nltarzan/structures/monument/ResetScheduler.java 153-182

- **Lines 153–155:** a weighted random interval. 1 day is a safety net that can't actually happen, because `MonumentConfig` always provides a usable list.
- **Lines 157–159:** the monument's world, looked up by name.
- **Lines 161–182:** read and write `state.yml`, which holds three values: `active`, `world` and `next-check-at`.
  - **Line 162:** no `state.yml` (a new server): everything keeps its starting value, so the monument is switched off.
  - **Line 169:** an older `state.yml` without `active` but with a countdown came from a server where the monument was already running, so it counts as on. Your own server keeps running after the update without you doing anything.

## MonumentArena.java: the area around the monument

@code src/main/java/com/nltarzan/structures/monument/MonumentArena.java 19-56

- **Line 22:** wait at most 200 ticks (10 seconds) for the area's mobs to load.
- **Lines 25–28:** the monument box, in blocks from the center: 29 west/north to 28 east/south, 1 below to 20 above. That's the 58×22×58 blocks of the four quadrants.
- **Lines 30–34:** the world, the center, the radius from `check-radius`, and the list of chunks to keep loaded.
- **Lines 36–37:** a tiny record for one chunk's position.
- **Lines 45–55:** work out every chunk covering the radius **and** the whole monument box, whichever reaches further. `>> 4` divides by 16 (rounding down, correct for negative coordinates too), turning a block coordinate into a chunk coordinate. This replaces your datapack's hard-coded `forceload add 192 -289 255 -352`.

@code src/main/java/com/nltarzan/structures/monument/MonumentArena.java 58-106

- **Lines 63–65:** the center. It hands out a *copy* (`clone()`), because a `Location` can be changed, and the arena's own center must stay put.
- **Lines 72–81, "at the monument":** within the radius (a sphere around the center) **or** anywhere in the monument box. The box matters for the far corners, which are more than 40 blocks from the center (about 40.3 at the bottom corners, 45.6 at the top). Comparing the squared distance with `radius * radius` avoids a slow square root and gives the same answer.
- **Lines 84–91, all living entities of one kind at the monument**, e.g. `entities(ElderGuardian.class)` or `entities(Player.class)`. This replaces the datapack's `@e[type=elder_guardian,distance=..40]`. `<T extends Entity>` means it works for any entity type. Line 88 asks Paper for everything in a box big enough for both the radius and the monument box; line 89 keeps only the living ones that really count.
- **Lines 97–106, inside the monument itself:** the exact box, used for the hoe rules.

@code src/main/java/com/nltarzan/structures/monument/MonumentArena.java 108-125

- **Counts the sponges for the hoe's durability:** every block in the box (about 74,000) is checked for wet or dry sponge. `count++` adds one. The chunks have to be loaded, which they are during a reset.

@code src/main/java/com/nltarzan/structures/monument/MonumentArena.java 127-166

- **Lines 131–150, loading the area** (replaces the datapack's `forceload add` followed by a 5-tick wait):
  - **Lines 132–134:** a **plugin chunk ticket** keeps a chunk loaded for as long as the plugin holds it, the same idea as `forceload`.
  - **Line 136:** a promise that's fulfilled once everything is loaded.
  - **Lines 137–138:** small arrays of one element. It's a Java workaround: the code inside the timer can't change ordinary local variables, but it can change what's *inside* an array.
  - **Lines 139–148:** a timer that runs every tick. Once every chunk *and its mobs* are loaded, stop and fulfil the promise. This matters because mobs load slightly after blocks; counting too early would find "0 Elder Guardians" and reset an uncleared monument. After 200 ticks without success, give up with an error; the check then fails safely and is retried a minute later.
- **Lines 153–157:** let go of the chunks again (the datapack's `forceload remove`).
- **Lines 159–166:** true only when every chunk and its mobs are loaded.

## StructureLibrary.java: the .nbt templates

@code src/main/java/com/nltarzan/structures/monument/StructureLibrary.java 24-47

- **Lines 26–27:** where the templates are inside the jar (from `build.gradle.kts`).
- **Lines 30–33:** the full list of 19 maze templates: the 8 fixed pieces, plus one per room type from `RoomType` (step 5), so a new room type is loaded automatically.
- **Line 35:** the loaded maze templates, by name. A `Structure` is Bukkit's in-memory version of an `.nbt` file, ready to place.
- **Lines 38–39:** one pre-built monument: its name and its four quadrants.
- **Lines 42–43:** the pre-built monuments, by name (for `reset monument_03`) and with their weights (for random picks).

@code src/main/java/com/nltarzan/structures/monument/StructureLibrary.java 49-78

- **Called on startup and on every reload.** It loads all files once, so placing them later is instant.
- **Lines 53–63:** for each variant in `monuments.yml`, load its four quadrant files. If one is missing, that variant is skipped with a warning and the others still work.
- **Lines 67–77:** load all 19 maze templates. If even one is missing, generated monuments are switched off as a whole; a half-built monument with holes would be worse. The status then shows "(generated templates missing)".

@code src/main/java/com/nltarzan/structures/monument/StructureLibrary.java 80-117

- **Lines 80–104:** ways to ask for what's loaded: `mazeReady()` (all 19 maze templates loaded), `maze(name)` (one maze template), `get(name)` (one variant), `names()` (all variant names, for tab completion) and `weighted()` (the variants with their weights).
- **Lines 110–117:** opens a file inside the jar and lets Bukkit turn it into a `Structure`. `try (…)` is "try-with-resources": the file is closed automatically afterwards, even if something goes wrong. `throws IOException` warns callers that this can fail.

## CurrentMonument.java: what's built right now

@code src/main/java/com/nltarzan/structures/monument/CurrentMonument.java 20-73

- **Lines 23–32:** a **sealed interface** allows only the listed kinds. A monument is either pre-built (with its variant name) or generated (with its seed and maze settings). A generated one doesn't store its whole layout: the seed plus the settings are enough to regenerate exactly the same layout, which is what `layout current` uses (lines 28–30).
- **Lines 34–38:** the logger, `monuments/current.yml`, what's built, the sponge count (`-1` means unknown), and the monument's random ID.
- **Lines 51–53:** the monument's ID. The Monument Hoe stores it, so a hoe from an earlier monument is recognized and deleted.
- **Lines 56–63:** what's built and the sponge count, as `Optional`s that are empty when unknown.
- **Lines 68–73:** called by the resetter after placing a monument: store what was built and the sponge count, give the new monument a fresh random ID (which makes all older hoes invalid), and save.

@code src/main/java/com/nltarzan/structures/monument/CurrentMonument.java 75-131

- **Lines 75–101, writing `current.yml`:** the ID, the sponge count, and either the variant or the seed with all the maze settings. All the settings are saved because you might change `monuments.yml` later, but the layout that's built depends on the settings at the time. `case null` handles "nothing built yet".
- **Lines 107–131, reading it at startup:**
  - **Lines 108–113:** no file yet: give the monument in the world an ID and save it straight away, so the ID survives restarts and hoes stay valid.
  - **Lines 117–127:** rebuild "what's built" from the file. If the saved maze settings are invalid, `MazeSettings` throws an error, which is caught: a warning is logged, and what's built becomes unknown.
  - **Lines 128–130:** a file from before IDs existed gets one added and saved.

## MonumentResetter.java: one check, and placing the new monument

@code src/main/java/com/nltarzan/structures/monument/MonumentResetter.java 37-73

- **Line 40:** the special word that means "a generated monument", used by `reset generated`.
- **Line 43:** a pre-built quadrant is 29 blocks wide; the other three quadrants are placed 29 blocks further west and/or north.
- **Lines 45–59:** the four possible outcomes of a check, each a small record carrying the details: how many Elder Guardians are alive, how many players are inside, what was placed, or why it failed.
- **Line 62:** the builder for generated monuments (step 6).
- **Line 64:** whether a check is busy, so two can never overlap.

@code src/main/java/com/nltarzan/structures/monument/MonumentResetter.java 80-110

- **Lines 81–96:** reasons to stop before starting: a check is already running, the monument is paused, it's switched off (so `check` and `reset` are refused too, with a hint to run `active on`), or the world isn't loaded.
- **Line 98:** mark it as busy.
- **Lines 99–109:** a chain of steps that run one after another, each once the previous one is finished:
  - **Line 99:** load the area and wait for its mobs.
  - **Line 100:** then look at the monument and, if needed, place a new one (`evaluate`). `thenCompose` is used because `evaluate` itself returns a promise: a generated build takes several ticks.
  - **Lines 101–105:** if *anything* in the chain fails, turn it into a `Failed` result with the reason, and log the full error. Line 102 unwraps the actual error from the "wrapper" error Java puts around it.
  - **Lines 106–109:** **always**, whether it succeeded or failed, release the chunks and clear the busy flag.

@code src/main/java/com/nltarzan/structures/monument/MonumentResetter.java 112-132

- **The heart of your datapack's `monument_checks`.**
- **Lines 115–119:** any Elder Guardians alive (and not forced)? Then broadcast "not cleared" to admins and stop.
- **Lines 120–128:** not forced and no Elder Guardians: are players inside? Then postpone. Otherwise, broadcast "cleared". A forced `reset` skips both checks.
- **Line 130:** what to place: no specific request → follow `reset.mode`; `generated` → a generated monument; anything else → that pre-built variant.
- **Line 131:** place it.

@code src/main/java/com/nltarzan/structures/monument/MonumentResetter.java 134-161

- **Your datapack's `monument_place`.**
- **Lines 136–138:** the requested variant, or a weighted random one.
- **Line 145:** remove leftover Guardians, if `clear-guardians` is on.
- **Line 148:** the quadrants go 1 block below the center, like the generated monument's empty shell.
- **Lines 149–152:** the four quadrants, at the same offsets as your datapack's `~`, `~-29 ~ ~`, `~ ~ ~-29` and `~-29 ~ ~-29`.
- **Line 155:** the third Elder Guardian in the top room. The two wing ones come with the quadrant files.
- **Line 157:** remember what's built, and count the sponges for the hoe.
- **Lines 158–160:** log it, tell admins, and report "placed".

@code src/main/java/com/nltarzan/structures/monument/MonumentResetter.java 163-180

- **Line 164:** without all 19 templates, refuse.
- **Lines 167–169:** the requested seed or a random one, then generate the layout (step 5). `var` lets Java work out the type itself.
- **Line 172:** turn the layout into build steps (`BuildPlanner`) and let the builder place them over several ticks. It also gets a way to spawn the three Elder Guardians: `this::spawnElderGuardian` passes this class's own spawn method along. `thenApply` runs once building has finished.
- **Lines 173–178:** the same as for pre-built, plus the seed and newest chance in the description.

@code src/main/java/com/nltarzan/structures/monument/MonumentResetter.java 182-221

- **Lines 182–187:** remove every Guardian at the monument. Elder Guardians count as a kind of Guardian, so leftovers from a forced reset go too.
- **Lines 189–192:** place one template, like `/place template`: entities included, no rotation or mirroring, a random palette (`-1`), and every block (`1f`).
- **Lines 195–203:** spawns an Elder Guardian with your stats. The part after `guardian ->` runs *before* it appears in the world, so it never shows up with vanilla stats for a moment. Max health and attack damage are set, its health is filled to the new max, and it never despawns.
- **Lines 205–210:** set an attribute's base value, skipping it safely if the mob somehow doesn't have that attribute.
- **Lines 212–214:** wraps an immediate answer in an already-fulfilled promise.
- **Lines 217–221:** the admin broadcast, only when `announce` is on, and only to players with `structures_admin` (and the console).
