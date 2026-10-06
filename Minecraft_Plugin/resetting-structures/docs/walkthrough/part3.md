# Step 3: the commands

`StructureCommand.java` is the shared `/structure` root. `MonumentCommand.java` holds everything after `/structure monument`.

## StructureCommand.java: the /structure root

This class only works out *which structure* is meant, then hands the rest of the command to that structure's own handler.

@code src/main/java/com/nltarzan/structures/command/StructureCommand.java 1-21

- **Line 1:** the package. It sits outside `monument`, because it's shared by every structure.
- **Line 3:** for the permission constant.
- **Lines 4–5:** Paper's command system. `BasicCommand` is Paper's simple way to make a command: you get the typed words as a list of text. `CommandSourceStack` is "who ran it, and where": a player, the console or a command block.
- **Lines 6–7:** Adventure, the text library Paper uses for messages. `Component` is a piece of chat text; `NamedTextColor` is a color like `RED`.
- **Line 21:** `implements BasicCommand` means "this class *is* a Paper command". `BasicCommand` demands `execute`, and optionally `suggest` and `permission`.

@code src/main/java/com/nltarzan/structures/command/StructureCommand.java 23-40

- **Lines 24–28:** an interface of our own: what every structure's command must offer. `execute` runs the command; `suggest` returns the tab-completion options. Both receive `String[] args`, the words typed **after** the structure name, so for `/structure monument reset generated` the monument's command receives `["reset", "generated"]`. `String[]` is an **array**: a fixed-size list.
- **Line 30:** a **map** is a lookup table from a key to a value; here from a structure name to its command. `LinkedHashMap` remembers the order things were added, so the usage message and tab completion list structures in a fixed order.
- **Lines 32–35:** adds a structure, stored under its lowercase name so `/structure MONUMENT` works too. `return this` hands the same object back, so calls can be chained.
- **Lines 37–40:** tells Paper that `/structure` needs `structures_admin`. Paper then does all the checking itself: players without it can't run it, don't get it in tab completion, and their client doesn't even see that the command exists.

@code src/main/java/com/nltarzan/structures/command/StructureCommand.java 42-61

- **Line 43:** Paper calls this when someone runs `/structure ...`.
- **Line 44:** looks up the structure named by the first word (`args[0]`; arrays count from 0). If nothing was typed, or the name is unknown, `command` becomes `null`.
- **Lines 45–49:** no valid structure, so send a red usage message and stop. `String.join("|", structures.keySet())` builds `monument` from the registered names; with a mansion later it would show `monument|mansion`.
- **Line 50:** pass everything *after* the structure name to that structure's command. `Arrays.copyOfRange(args, 1, args.length)` drops the first word: `["monument", "status"]` becomes `["status"]`.
- **Lines 54–61:** tab completion. On the first word, suggest structure names starting with what's typed so far (`stream()` walks through them, `filter` keeps the matches, `toList` collects them). After that, ask that structure's own `suggest` for the rest.

## MonumentCommand.java: /structure monument ...

### Lines 29–94: the class, its constants and HELP

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 34-57

- **Line 34:** `implements StructureCommand.Subcommand`, so the root command can hand work to it.
- **Line 36:** the command's beginning as a constant, used in every usage message and help text.
- **Line 38:** the y-level an Ocean Monument is normally built at. `setcenter` reminds you to double check when you pick a different one.
- **Lines 40–41:** the eight subcommands, for tab completion and the usage message. They're package-private (no `private`), so the test `MonumentCommandTest` can read them.
- **Lines 47–94 (`HELP`):** one `CommandHelp` per way of using the command: the usage as typed, an explanation, and sometimes examples (line 57). `Monuments` hands this list to `InfoFiles`, which writes it into `commands.txt`. The test `MonumentCommandTest` fails if a subcommand is missing from `HELP`, or explained without existing (lines 43–46).

### Lines 96–126: picking the subcommand

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 96-126

- **Lines 98–100:** the constructor stores the `Monuments` object, so every subcommand can reach the settings, the timer, the current monument and so on.
- **Line 104:** who ran it, a player or the console. `CommandSender` covers both.
- **Line 105:** the first word, or `status` when nothing was typed. That's why `/structure monument` on its own shows the status.
- **Lines 106–125:** a `switch` that runs a different method per word:
  - **Line 108:** `active` needs the next word (`on`, `off` or nothing), so it gets `args`.
  - **Line 109:** `check` is a normal check, not forced; the two `null`s mean no variant and no seed.
  - **Line 110:** `reset` needs the remaining words for a variant or seed, so it gets `args`.
  - **Line 111:** `setcenter` gets the player's position (`source.getLocation()`, where the command was run) and the remaining words, which may be coordinates.
  - **Lines 114–122:** `reload`. If `reloadSettings()` succeeds, the reply is green. If not, it's red, names the problem, and says whether the monument stays paused or still uses the previous settings.
  - **Lines 123–124:** any other word gets a short usage message pointing to `commands.txt`.

### Lines 128–155: tab completion

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 128-155

- **Lines 130–133:** first word: the eight subcommands, filtered on what's typed so far.
- **Lines 134–137:** second word after `reset`: `generated` plus every pre-built variant name. `Stream.concat` joins the two lists; `equalsIgnoreCase` compares text without caring about upper or lower case.
- **Lines 138–143:** second word after `broadcast` or `active`: `on` or `off`. After `layout`: `current` or `random`. `||` means "or".
- **Lines 144–150:** the coordinates after `setcenter`, for a player: your own position, rounded to the nearest block corner just like `setcenter` without coordinates would. The second word gets your x, the third your y and the fourth your z (`xyz[args.length - 2]` picks the right one), like vanilla commands such as `/tp` suggest.
- **Lines 151–153:** third word after `layout`: example newest chances.
- **Line 154:** nothing to suggest anywhere else, for example a seed, which can be any number.

### Lines 157–176: broadcast

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 157-176

- **Line 159:** a `boolean` holds `true` or `false`.
- **Lines 160–161:** no word after `broadcast`, so flip the current setting. `!` means "the opposite of".
- **Lines 162–165:** `on` or `off` sets it explicitly.
- **Lines 166–169:** anything else gets a usage message.
- **Lines 170–173:** save it. `setAnnounce` returns `false` when `monuments.yml` has a problem, because it then refuses to save over the broken file; the reply explains that (`configProblem`, lines 326–330).
- **Lines 174–175:** confirm, in green when on and yellow when off.

### Lines 178–224: layout

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 183-195

- **`layout current`:**
  - **Line 185:** asks what's built. The answer is an `Optional`, which is empty when the plugin hasn't placed a monument yet (line 186).
  - **Line 188:** `instanceof Built.PreBuilt(String variant)` checks "is it a pre-built one?" and takes its variant name out in one go (a record pattern).
  - **Line 191:** for a generated one, `generated.layout()` rebuilds the layout from the saved seed and settings, and prints it.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 197-224

- **Line 197:** start with a random seed. A `long` is a very large whole number, which is what seeds are.
- **Line 198:** start with the maze settings from `monuments.yml`.
- **Lines 200–202:** a second word that isn't `random` is read as the seed. `Long.parseLong` turns text into a number and throws a `NumberFormatException` if it isn't one.
- **Lines 203–209:** a third word is the newest chance, which must be between 0 and 1. A number outside that range deliberately throws the same error (line 206), so both mistakes end up in one place.
- **Lines 210–214:** `catch` handles that error: show the usage message and stop. This is how `try`/`catch` turns a crash into a friendly message.
- **Line 215:** generate the layout and print it.
- **Lines 218–224:** writes the map to the console. `monuments.getLogger()::info` means "for each line, call `info(line)` on the logger". A player also gets a short chat note with the seed and newest chance.

### Lines 226–248: reset

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 226-248

- **Line 228:** the second word is a variant name or `generated`. Without one it's `null`, which means "follow `reset.mode`".
- **Lines 229–231:** `Generated` with a capital also means `generated`.
- **Line 232:** `Long` with a capital L can also be `null`, meaning "no seed given".
- **Lines 233–246:** a third word is a seed. Seeds only work with `generated`; for a pre-built variant the reply is red and nothing is reset (lines 234–239). A seed that isn't a number gets a usage message.
- **Line 247:** run a **forced** reset (`true`).

### Lines 250–257: status

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 250-257

- **Lines 251–257:** the heading "Ocean Monument" in dark aqua, bold, then for each status line from `Monuments` (the same ones as in `status.txt`) the label in gray with the value in its own color. `.append` glues two pieces of chat text together.

### Lines 259–292: active

This switches the monument on or off. It's built the same way as `broadcast`, so it works the same way: `active on`, `active off`, or just `active` to switch.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 259-292

- **Line 261:** whether it's switched on right now. This asks the timer directly, not `isActive()`: that one is also false while paused, and switching should follow what the admin last chose.
- **Lines 262–272:** work out the wanted state, exactly like `broadcast`: flip it when nothing follows, otherwise `on` or `off`, and a usage message for anything else.
- **Lines 273–277:** already in the wanted state? Just say so, in yellow, and change nothing. Without this, `active on` on an active monument would restart the timer for no reason.
- **Lines 278–282:** switch it. `setActive` refuses to turn **on** while `monuments.yml` is broken; the reply says why.
- **Lines 283–287, on:** a green reply naming the exact location it will build at, so a wrong location is noticed right away. The first check comes after a fresh random wait; `status` shows when.
- **Lines 288–291, off:** a yellow reply: nothing is checked or built until it's turned on again.

### Lines 294–371: running a check, setcenter

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 294-306

- **Lines 294–306, check or reset:**
  - **Line 295:** an immediate gray "Checking…" or "Resetting…", because the actual work can take a while.
  - **Line 296:** asks the timer to run a check now, so it can start a fresh random wait afterwards. The result isn't ready immediately (loading chunks and building take several ticks), so `runNow` returns a **`CompletableFuture`**, a "promise" of a result later. `.thenAccept(result -> ...)` says what to do once it arrives: send the reply.
  - **Lines 297–304:** one message per outcome. The compiler checks that all four outcomes are handled, because `Result` is a sealed type with exactly these four kinds.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 308-361

`setcenter` works in two ways: on its own it uses where you're standing, and with three numbers it uses exactly those.

- **Lines 313–316:** the four values to fill in, by whichever way is used.
- **Lines 317–327, without coordinates** (`args` only holds `setcenter`):
  - **Lines 318–322:** only a player has a position to use, so the console is told to give coordinates.
  - **Lines 324–327:** the player's world, with x and z rounded to the **nearest block corner**, matching the datapack's marker, which stood exactly on the corner where the 4 center blocks meet. `(int)` turns the rounded number into a whole number. y is the height of the block the player's feet are in, which is the level the rooms start from.
- **Lines 328–345, with coordinates** (`setcenter` plus three words):
  - **Lines 329–337:** read the three numbers. Something that isn't a whole number (like `abc` or `40.5`) gets a red message instead. Whole numbers are what you want: the center is a block corner.
  - **Lines 338–340:** a player sets it in the world they're in; the console has no world of its own, so it keeps the world from `monuments.yml`.
  - **Lines 341–344:** that world must be loaded.
- **Lines 346–348:** any other number of words (one or two coordinates, or four) gets the usage message.
- **Lines 351–354:** save it. If `monuments.yml` is broken, nothing is saved, and the reply says why.
- **Lines 355–356:** confirm, including the world, so a center set in the wrong world is noticed right away.
- **Lines 357–360:** a y-level other than 40 is allowed, but gets a gold reminder: "Double check your y-level. An Ocean Monument is usually set at y=40." This catches the most likely mistake: standing one block too high or too low.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 363-371

- **Lines 363–367:** the shared red "Not saved…" message, used by `broadcast` and `setcenter`.
- **Lines 369–371:** sends a gray message.
