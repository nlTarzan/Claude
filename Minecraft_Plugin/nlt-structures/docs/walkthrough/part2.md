# Step 2: startup, structure modules and settings

## A few Java basics first

These come back in every file, so they're explained once here:

| What you see | Meaning |
|---|---|
| `package com.nltarzan.structures;` | The folder the class lives in. It must match the folder path under `src/main/java`. |
| `import org.bukkit.World;` | Lets the file use a class from another package by its short name (`World` instead of `org.bukkit.World`). |
| `public final class X` | `public`: other packages can use it. `final`: no other class can extend it. |
| `extends JavaPlugin` | "X is a kind of JavaPlugin": it inherits everything JavaPlugin can do, like `getServer()`, `getLogger()` and `getDataFolder()`. |
| `implements Something` | "X promises to have the methods listed in the interface `Something`." |
| `private final Foo foo;` | A **field**: data the object keeps. `private`: only this class can see it. `final`: set once, then never changed. |
| `static` | Belongs to the class itself, not to one object, e.g. a constant like `ADMIN_PERMISSION`. |
| `@Override` | "This replaces a method from the parent class or interface." The compiler checks that the method really exists there. |
| `/** ... */` and `//` | Comments. `/** */` is documentation that editors show when you hover over a name. |
| `record` | A short way to write a class that only holds values. Java generates the constructor and getters for you. |
| `List<Weighted<Long>>` | Generics: "a list of `Weighted`s, each holding a `Long`". The type in `< >` says what's inside. |
| `null` | "Nothing". A variable that holds no object. |

## NltStructuresPlugin.java: where the plugin starts

This is the class `plugin.yml` names under `main:`. Paper creates exactly one of it and calls `onEnable()` when the server starts and `onDisable()` when it stops.

@code src/main/java/com/nltarzan/structures/NltStructuresPlugin.java 1-24

- **Line 1:** the package. It matches the folder `src/main/java/com/nltarzan/structures/`.
- **Lines 3–10:** what this file uses: the `/nltstructures` command, the chat messages, the info files, the `Monuments` module, Bukkit's `World` and `JavaPlugin`, and Java's `List`.
- **Lines 12–16:** documentation. `{@link …}` turns into a clickable link in editors.
- **Line 17:** the class. `extends JavaPlugin` is what makes it a Paper plugin.
- **Line 20:** the permission name, stored once as a constant. The command and the broadcasts both use `NltStructuresPlugin.ADMIN_PERMISSION`, so renaming it means changing only this line (and `plugin.yml`). It's written like nlT-Races' `nltraces.admin`.
- **Line 22:** the root command, also stored once. Every usage message and help text builds on it, e.g. `/nltstructures monument`.
- **Line 24:** the list of structure modules. It starts as an empty list (`List.of()`), so `onDisable` works safely even if `onEnable` failed early.

@code src/main/java/com/nltarzan/structures/NltStructuresPlugin.java 26-46

- **Line 27:** called by Paper once, when the plugin starts.
- **Line 28:** makes sure the plugin's folder `plugins/nlT-Structures/` exists.
- **Line 29:** loads `messages.yml`, every text the plugin sends in chat (step 3). There's one for the whole plugin, shared by every structure.
- **Line 30:** creates the writer for `status.txt` and `commands.txt`.
- **Line 33:** the structures the plugin manages. Right now that's only the Ocean Monument. It gets the plugin itself (`this`), the messages, and a way to say "my status changed": `infoFiles::writeStatus` is a **method reference**, short for "call `writeStatus` on `infoFiles`". A future mansion is one more entry in this list.
- **Line 35:** the root command `/nltstructures`, which gets the plugin (for its version in the help) and the messages.
- **Lines 36–40:** for every structure:
  - **Line 37:** start it: load its config and structure files, start its reset timer, register its listeners.
  - **Line 38:** add it to the info files.
  - **Line 39:** hook up its subcommand, e.g. `/nltstructures monument`.
- **Lines 41–42:** register the command with Paper: its name (`COMMAND` without the `/`), a description shown in `/help`, the aliases `/structures` and `/nlts`, and the command object. An alias is another name for exactly the same command. If another plugin also registers `/structures`, `/nltstructures` and `/nlts` still work, and so does `/nlt-structures:structures`: Paper also registers every command with the plugin's name in front.
- **Lines 44–45:** write `commands.txt` and `status.txt` for the first time.

@code src/main/java/com/nltarzan/structures/NltStructuresPlugin.java 48-57

- **Line 49:** called by Paper once, when the server stops or the plugin is turned off.
- **Lines 50–52:** stops every structure, for the monument that's its reset timer.
- **Lines 53–55:** for every world, releases any chunks this plugin was keeping loaded. During a check, the monument's chunks are held loaded with "chunk tickets"; this makes sure none are left behind.

## StructureModule.java: what every structure provides

@code src/main/java/com/nltarzan/structures/StructureModule.java 9-33

An **interface** is a list of methods that a class promises to have. Every structure (now the Ocean Monument, later mansions and end cities) implements this one, so the main class can handle them all the same way:

- **Line 17:** `name()`: the heading in the status, `status.txt` and `commands.txt`, e.g. "Ocean Monument".
- **Line 20:** `commandName()`: the word after `/nltstructures`, e.g. `monument`.
- **Line 22:** `command()`: the object that handles `/nltstructures monument …`.
- **Line 25:** `status()`: the current status, one `StatusLine` per fact.
- **Line 28:** `commandHelp()`: every command with an explanation, for `commands.txt`.
- **Lines 30 and 32:** `enable()` and `disable()`: start and stop.

## Monuments.java: everything Ocean Monument

This class is the monument's own "mini plugin". It owns its config file, its saved data and all the parts that do the work. A mansion would later get a `Mansions` class like it.

### Lines 37–77: the class and its fields

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 37-77

- **Lines 42–44:** the safety rule: a broken `monuments.yml` never leads to a monument being built somewhere else.
- **Lines 46–47:** the same holds while the monument is switched off with `/nltstructures monument active off`, and a new server starts switched off. So whoever installs the plugin first checks the location, instead of getting a monument at the default one.
- **Line 49:** `implements StructureModule`, so it promises the methods above.
- **Line 51:** the config file's name as a constant.
- **Lines 52–53:** two clock formats for the status: `14:03`, or with the date when it isn't today.
- **Lines 56–57:** a small private record for the last check: when it happened and how it ended.
- **Lines 59–65:**
  - `plugin`: the plugin, needed for Bukkit calls.
  - `messages`: the plugin's `messages.yml`. The command, the resetter and the hoe rules all send their texts through it.
  - `command`: the `/nltstructures monument` command, made once.
  - `statusChanged`: what to call when the status changes (rewrites `status.txt`).
  - `configFile`: `plugins/nlT-Structures/monuments/monuments.yml`.
  - `folder`: `plugins/nlT-Structures/monuments/`, which holds everything of the monument: `monuments.yml`, `state.yml` and `current.yml`.
  - `config`: the file's contents as loaded, which can also be edited and saved.
- **Lines 67–72:**
  - `settings`: the config turned into ready-to-use values (`MonumentConfig`, below).
  - `configProblem`: what's wrong with `monuments.yml`, or `null` when it's fine.
  - `paused`: true when no valid `monuments.yml` was loaded since the server started.
  - `lastCheck`: the outcome of the last check.
- **Lines 74–77:** the parts, each explained in step 4:
  - **`structures`:** all `.nbt` templates, loaded into memory.
  - **`resetter`:** does one check, and places a new monument when it's cleared.
  - **`scheduler`:** the timer that decides *when* checks happen.
  - **`currentMonument`:** remembers what's built right now, its sponge count and its ID.

### Lines 79–149: constructor, name, start and stop

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 79-149

- **Lines 82–89:** the **constructor**, which runs once when `new Monuments(...)` is called. It stores the plugin, the messages and the callback (`this.plugin` is the field; `plugin` alone is the parameter), makes the command (line 85, after the messages, because the command takes them from here), and works out the two paths: first the `monuments/` folder, then the config file inside it. A `File` is only a path; nothing is created yet.
- **Lines 91–99:** the name and command word from `StructureModule`.
- **Lines 102–132, `enable()`:**
  - **Line 103:** create the `monuments/` folder if it doesn't exist.
  - **Lines 104–107:** older versions kept `monuments.yml` directly in `plugins/nlT-Structures/`. If it's still there, move it into `monuments/` (`moveOldConfig`, below).
  - **Lines 108–112:** if there's no `monuments.yml` at all, copy the default one out of the jar: `monuments/monuments.yml` inside the jar ends up as `monuments/monuments.yml` in the plugin's folder. Not when an old one couldn't be moved, though: the monument is then paused for lack of a config, which is safer than a fresh default with a location that may be wrong.
  - **Line 113:** bring `monuments.yml` up to date: add settings a newer version of the plugin introduced and drop ones it no longer uses, keeping the admin's values (`updateConfig`, below, and `ConfigUpdater` in step 3).
  - **Line 114:** read `monuments.yml` strictly (see `loadConfig` below).
  - **Lines 115–116:** load all structure templates, including the configured pre-built variants.
  - **Line 117:** load `current.yml`.
  - **Lines 118–119:** create the resetter and the timer. They get `this` (the `Monuments` object), so they can reach the settings, the structures and so on.
  - **Lines 120–121:** start the timer, but **not** when paused. The timer itself also does nothing while the monument is inactive (step 4).
  - **Lines 122–126:** inactive (always the case on a new server)? Then log a warning on every startup, explaining how to check the location and turn the monument on.
  - **Line 129:** create the hoe helper, which knows how to make and recognize a Monument Hoe.
  - **Lines 130–131:** register the two **listeners**, classes whose methods Paper calls when something happens in the game (step 7). Both get the same `hoe` object.
- **Lines 135–139:** stop the timer when the plugin shuts down, so no check fires in the middle of shutting down.
- **Lines 142–144:** hands out the `/nltstructures monument` command made in the constructor. It's the same object every time, because the root command asks for it on every use.
- **Lines 147–149:** the command help for `commands.txt`, which lives in `MonumentCommand`.

### Lines 151–220: reload, broadcast, setcenter, active, last check

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 157-220

- **Lines 157–166, `/nltstructures monument reload`:** bring the file up to date first (line 158, just like at startup), then re-read it. Only if it's fine, reload the templates (in case the variants changed) and restart the timer; the timer keeps the saved countdown, so a reload doesn't reset the wait. Then update `status.txt`, and return whether it worked.
- **Lines 173–182, `/nltstructures monument broadcast`:**
  - **Lines 174–176:** refuse while `monuments.yml` has a problem. Saving would overwrite the file you're fixing.
  - **Lines 177–181:** change `reset.announce`, save the file, rebuild the settings so it takes effect immediately, and update the status.
- **Lines 189–200, `/nltstructures monument setcenter`:** the same safety check, then write the new position into the `location:` section, save, and reload so the timer and checks use the new spot.
- **Lines 207–214, `/nltstructures monument active on|off`:**
  - **Lines 208–210:** turning **on** is refused while `monuments.yml` has a problem, because the location isn't known then. Turning off always works.
  - **Line 211:** the timer does the actual work and saves the choice in `state.yml` (step 4).
  - **Line 212:** update `status.txt`.
- **Lines 217–220:** called by the timer after every check: remember the time and outcome for the status, and update `status.txt`.

### Lines 222–233: the monument area

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 222-233

- **Lines 224–226:** no area while the monument isn't active (`isActive()`, at the end of the file): when paused there's no known location, and when switched off the default location may well be someone's build. Because the hoe rules and the Elder Guardian reward both ask for the area, they're off too.
- **Line 227:** look up the world by name.
- **Lines 228–230:** if that world isn't loaded (or the name is wrong), return `null`. Every caller checks for that.
- **Lines 231–232:** otherwise create a `MonumentArena`: the center, the radius and the 58×22×58 box, with helpers like "which entities are at the monument" and "count the sponges" (step 4). A new one each time is cheap, and it means a changed center takes effect immediately.

### Lines 235–312: the status

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 235-288

This builds the list of status lines used by both `/nltstructures monument status` (in color) and `status.txt` (plain text). Each `StatusLine` has a label, a value and a color.

- **Lines 238–242:** a red **Problem** line first, when `monuments.yml` has a mistake.
- **Lines 243–245:** the location, or "Unknown" while paused.
- **Lines 246–251:** what's built: "Pre-built monument_03", "Generated, seed 123", or "Not placed by the plugin yet". `get()` returns an `Optional` (a box that may be empty); `.map(...)` turns its content into a line, `.orElse(...)` is used when it's empty.
- **Lines 252–253:** the sponge count, only when known.
- **Lines 255–260:** the number of Elder Guardians alive, red when above 0 and green at 0. Only shown when the area is loaded, because the count isn't reliable otherwise. `>> 4` turns a block coordinate into a chunk coordinate (divide by 16).
- **Lines 261–263:** the last check (below).
- **Lines 265–268:** the reset mode, with a red note when the generated templates didn't load. `a ? b : c` means "if a then b, otherwise c".
- **Lines 271–274:** **Active**: "On" in green, or "Off" in gold with what to do about it.
- **Lines 275–283:** the next check: "Paused", "Not scheduled", or "in 1h 4m 30s (around 15:08)". While the monument is switched off there's no next check, so this line is left out (the Active line already explains why).
- **Lines 284–286:** broadcasts on (green) or off (red).

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 290-312

- **Lines 290–302:** the last check's line, by outcome: not cleared (red), cleared but players inside (yellow), reset (green) or failed (red). The `switch` with `case Result.NotCleared(int alive)` takes the number out of the result directly; this is called a **record pattern**.
- **Lines 305–307:** a time as `14:03`, or with the date when it isn't today.
- **Lines 310–312:** the clock time some seconds from now. It's an estimate, because game time only runs while the server does.

### Lines 314–398: updating, reading, moving and saving monuments.yml safely

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 314-321

- **Lines 318–321:** hands `monuments.yml` to `ConfigUpdater` (step 3), which adds the settings a newer version of the plugin introduced and drops the ones it no longer uses, keeping your values. It's told three things: where the file is, that `variants` is your own section (you choose its names, so it's kept exactly as you have it), and how to recognise a file with a problem (`MonumentConfig::locationProblem`, a **method reference** like `infoFiles::writeStatus` earlier). A file that can't be read or has no valid location is never touched; `loadConfig` then reports it and pauses, as before.

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 328-365

- **Lines 329–338:** read the file strictly. `yaml.load` throws an error on a YAML mistake (for example a missing space after a colon), instead of quietly returning an empty file. If it reads fine, check the `location:` section (`MonumentConfig.locationProblem`, below). `try`/`catch` catches the error and turns it into a problem description; `details` keeps the full error text for the console.
- **Lines 340–346:** no problem: use it, clear any old problem and un-pause.
- **Lines 348–364:** a problem:
  - **Lines 349–356, at startup** (no earlier settings): use the defaults for everything except the location, **pause** the monument, and log a clear error.
  - **Lines 357–360, on a reload:** keep the previous working settings, and log the error.
  - **Lines 361–363:** log the full error details too.

Without this, a broken file would make every coordinate 0, and the next check would build a monument at 0, 0, 0.

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 367-382

- **Moving an old `monuments.yml` into `monuments/`**, for servers that ran an older version of the plugin.
- **Lines 369–374:** there's already one in `monuments/`? Then that one wins, and a warning says the old one can be deleted.
- **Lines 375–377:** otherwise move the file and log it. Your settings stay exactly as they were.
- **Lines 378–381:** if moving fails (for example because another program has the file open), log an error saying to move it by hand. The monument then stays paused until that's done and `reload` is run.

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 384-398

- **Lines 384–390:** the first line of an error message, shortened to 120 characters, for the status.
- **Lines 392–398:** write the YAML back to disk. Paper keeps the comments when it does this. If saving fails, a warning is logged instead of crashing.

### Lines 400–451: getters

The rest of the file are **getters**, small methods that hand out a field so the other parts can reach it: `configProblem()`, `isPaused()`, `isActive()`, `plugin()`, `folder()`, `getLogger()`, `getServer()`, `isEnabled()`, `settings()`, `structures()`, `resetter()`, `scheduler()` and `currentMonument()`. `getLogger()`, `getServer()` and `isEnabled()` pass straight through to the plugin, so the other monument classes can write `monuments.getLogger()` without knowing about the plugin.

One of them is more than a getter:

@code src/main/java/com/nltarzan/structures/monument/Monuments.java 410-413

- **Line 412:** the monument only does anything (checks, resets, hoe rules, the reward) when `monuments.yml` is fine **and** it's switched on. Everything else asks this one method, so the rule lives in a single place.

## MonumentConfig.java: reading monuments.yml

This turns the raw YAML into a fixed set of checked values. Bad values get a warning in the console and a safe default, so a mistake in the config can't crash the plugin.

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 16-35

- **Line 16:** a **record**: Java creates a getter for each value automatically (e.g. `settings.worldName()`). It can't be changed after it's made; a reload builds a new one.
- **Lines 17–32:** one value per setting in `monuments.yml`:
  - `location:` → `worldName`, `centerX/Y/Z`, `checkRadius`
  - `reset:` → the two interval lists (each entry a delay in ticks with its weight), `mode`, `clearGuardians`, `announce`
  - `variants:` → a list of names with weights
  - `elder-guardian:` → three numbers
  - `reward:` → one on/off value
  - `maze:` → a `MazeSettings` object (step 5)
- **Lines 34–35:** what to use when an interval list is unusable: 1 Minecraft day for normal checks, 300 seconds when players are inside.

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 37-63

- **Line 37:** `static` because it's called as `MonumentConfig.load(...)`, without an object. It takes the YAML and a logger for warnings.
- **Lines 38–40:** read both interval lists (`loadIntervals`, below).
- **Lines 41–44:** if the longest "players inside" wait isn't shorter than the shortest normal wait, log a warning. The values are still used.
- **Lines 46–62:** builds the record, in the same order as lines 17–32. A YAML path like `"location.x"` means "the `x` under `location:`". The second argument is the default if a setting is missing. `Math.max(1, …)` stops the radius from being 0 or negative. Settings that need more checking go through their own helper methods.

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 65-84

- **The strict check of `location:`**, used by `Monuments.loadConfig`.
- **Lines 72–74:** the section must exist.
- **Lines 75–77:** `world` must be text and not empty.
- **Lines 78–82:** `x`, `y` and `z` must be whole numbers.
- **Line 83:** `null` means "no problem".

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 86-102

- **Lines 87–90:** an **enum** is a fixed list of choices. A reset mode can only be one of these two, which is safer than passing text around.
- **Line 93:** reads the text, `pre-built` by default.
- **Line 94:** `trim()` removes stray spaces; `toLowerCase` makes `Generated` work too; `Locale.ROOT` makes that work the same on any computer's language settings. `switch` picks a result based on the text.
- **Lines 97–100:** anything else (a typo like `generatd`) logs a warning and uses pre-built. `yield` is how a `{ }` block inside a switch hands back its result.

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 104-124

- **Line 105:** the built-in defaults (peak 0.65, spread 0.15, 0–1, at least 1 sponge room), used for any missing setting.
- **Lines 106–107:** read min and max and force them between 0 and 1 (`clamp`, lines 122–124).
- **Lines 108–111:** if max is below min, use min for both.
- **Lines 112–116:** if the peak lies outside min..max, move it to the nearest edge.
- **Lines 117–118:** spread and the sponge minimum can't be negative.
- **Line 119:** builds the maze settings. `MazeSettings` checks the values once more itself.

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 126-153

- **Used for both interval lists.**
- **Line 128:** start with an empty list.
- **Line 129:** go through each `- { delay: ..., weight: ... }` line. `getMapList` gives each one as a map of names to values.
- **Lines 130–131:** take out `delay` and `weight`.
- **Line 133:** turn `1d`, `10s` or `5t` into ticks (`Durations.parseTicks`, step 4).
- **Line 134:** the weight, or 1 if it's left out.
- **Lines 136–138:** a bad entry is skipped with a warning naming it; the others still work.
- **Lines 140–143:** if nothing usable is left, use the fallback. `stream().noneMatch(...)` means "none of the entries has a weight above 0". `w -> w.weight() > 0` is a **lambda**, a small inline function.
- **Line 144:** `List.copyOf` makes the list unchangeable.
- **Lines 147–153:** the shortest and longest delay with a weight above 0, for the warning on lines 41–44.

@code src/main/java/com/nltarzan/structures/monument/config/MonumentConfig.java 155-171

- **Line 156:** the `variants:` section.
- **Lines 157–160:** if it's missing, warn and return an empty list. Pre-built resets then fail with a clear message instead of crashing.
- **Line 162:** each name under `variants:`. `false` means "only the names directly under it, not deeper".
- **Lines 163–167:** read the weight; a negative weight becomes 0, with a warning.

## The info package: status.txt and commands.txt

### StatusLine.java and CommandHelp.java

@code src/main/java/com/nltarzan/structures/info/StatusLine.java 9-10

- **One fact in a status**, e.g. "Sponges: 87", with the color the status command uses. `status.txt` uses only the label and value.

@code src/main/java/com/nltarzan/structures/info/CommandHelp.java 12-21

- **One command for `commands.txt`:** how it's typed, what it does, and example uses.
- **Lines 14–16:** stores an unchangeable copy of the examples.
- **Lines 18–20:** a second constructor for commands without examples; it calls the first one with an empty list.

### InfoFiles.java

@code src/main/java/com/nltarzan/structures/info/InfoFiles.java 23-59

- **Lines 25–26:** the two file names.
- **Line 28:** the width of a line in the files (90 characters).
- **Line 29:** the date format for "Updated 2026-10-06 14:03:12".
- **Lines 32–37:** two small records: one structure's section in `status.txt`, and one structure's group in `commands.txt`.
- **Lines 39–48:** the plugin, and the list of structures added by the main class.
- **Lines 50–53:** collect every structure's status and write `status.txt`.
- **Lines 55–59:** collect every structure's command help and write `commands.txt`.

@code src/main/java/com/nltarzan/structures/info/InfoFiles.java 61-107

- **Lines 62–74, the text of `status.txt`:** the header, then per structure its name and its lines. Line 68 finds the longest label, so `pad` can line all values up in one column.
- **Lines 77–107, the text of `commands.txt`:** the header, the permission note, the aliases (line 83), how to read `<required>` / `[optional]` / `a|b`, the root command's own `help` and `reload` (lines 86–91), and then per structure a ruled heading followed by each command, its wrapped explanation and its examples.

@code src/main/java/com/nltarzan/structures/info/InfoFiles.java 109-148

- **Lines 109–115:** the header both files share, with the READ-ONLY warning.
- **Lines 118–131:** word-wraps a text to 90 characters: words are added to a line until the next one wouldn't fit, then a new line starts with the indent.
- **Lines 133–135:** pads a text with spaces to a fixed width.
- **Lines 137–148, writing a file:** if it exists, make it writable again (line 141), write the text as UTF-8 (line 143), then mark it read-only (line 144), so an editor warns before anyone saves over it. A failure is logged as a warning.
