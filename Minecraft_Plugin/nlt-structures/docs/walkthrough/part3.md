# Step 3: the commands

`StructureCommand.java` is the shared `/nltstructures` root. `MonumentCommand.java` holds everything after `/nltstructures monument`. Neither of them has any chat text in it: every message comes from `messages.yml`, through `Messages.java`. That's explained first.

## Messages.java and messages.yml: every text the plugin sends

This works exactly like in nlT-Races, so both plugins' `messages.yml` files can be edited the same way.

@code src/main/resources/messages.yml 1-20

- **Lines 1–4:** how to write a message. The texts use **MiniMessage**, Adventure's tag format: `<red>…</red>` colors the text between the tags, `<bold>` makes it bold. Tags the plugin fills in, like `<command>` or `<seed>`, look the same. To show a real `<`, write `\<`.
- **Line 8:** the file's version. When an update rewords existing messages, this number goes up; a server with an older copy then gets the new file, and its old one is kept in the `backups` folder.
- **Line 10:** the `[nlT-Structures]` prefix in front of every chat message, in gold like nlT-Races' `[nlT-Races]`, so players can tell the two plugins apart.
- **Lines 12–20:** the messages of the root command. A key like `general.usage` means "`usage` inside `general`".

@code src/main/java/com/nltarzan/structures/config/Messages.java 17-50

- **Lines 24–27:** the file name, the version key and the MiniMessage reader, which turns tag text into chat components.
- **Lines 32–35:** reads the file once when the plugin starts.
- **Lines 37–50 (`reload`):** called at startup and by `/nltstructures reload`.
  - **Line 40:** older versions kept a single `messages-backup.yml` next to `messages.yml`. If one is still there, it moves into the `backups` folder (`Backups`, below).
  - **Lines 41–42:** no `messages.yml` yet: copy the default one out of the jar.
  - **Lines 43–44:** an outdated copy (a lower `messages-version`): replace it, keeping a backup (lines 67–77).
  - **Lines 46–49:** read the file, with the jar's copy as **defaults**. A key that's missing from the server's copy (for example a message added in a later version) then falls back to the default text instead of showing nothing.

@code src/main/java/com/nltarzan/structures/config/Messages.java 62-114

- **Lines 63–65:** the server's copy is outdated when its `messages-version` is lower than the jar's.
- **Lines 68–77:** move the old file into the `backups` folder, then copy the new one out of the jar and say where the old one went. If the backup fails, the old file is kept and nothing is replaced, so your texts are never lost.
- **Lines 84–98 (`get`):** turns a key into chat text.
  - **Lines 85–88:** a key that isn't in the file at all shows `[missing message: …]` in red, which makes a typo easy to spot.
  - **Lines 89–96:** the placeholders come in pairs, name and value: `get("monument.placed", "placed", "monument_03")` fills `<placed>` with `monument_03`. Text values are inserted as plain text, so a `<` in a world name can't be mistaken for a tag. A value that's already a `Component` keeps its own color; the status uses that (see `status` below).
- **Lines 101–103 (`send`):** the prefix plus the message, to one player or the console.
- **Lines 106–108 (`sendRaw`):** without the prefix, for list lines like the help entries and status lines.
- **Lines 111–113 (`broadcast`):** with the prefix, to everyone who has a permission, and to the console. The reset announcements use it.

## Backups.java and ConfigUpdater.java: keeping the files up to date

These two work exactly like their namesakes in nlT-Races. When an update changes `messages.yml` or `monuments.yml`, your old copy is kept, and your settings carry over.

@code src/main/java/com/nltarzan/structures/config/Backups.java 14-61

- **Lines 20–21:** the folder (`plugins/nlT-Structures/backups`) and the date-and-time format used in the names.
- **Lines 27–29 (`move`):** moves a file into the folder, stamped with the current time.
- **Lines 35–40 (`moveLegacy`):** moves a `messages-backup.yml` from an older version into the folder, stamped with the time that file was last changed, so its name says how old it really is.
- **Lines 42–60:** does the moving.
  - **Lines 43–46:** split the name into its base and extension, dropping `-backup`: `monuments.yml` gives `monuments` and `.yml`.
  - **Lines 50–53:** the new name, like `monuments-2026-10-22_14-30-05.yml`. If that name is already taken (two backups in the same second), a number is added, so a backup is never overwritten.
  - **Lines 54–55:** move it, and return where it went, for the log message.
  - **Lines 56–59:** if moving fails, log a warning and return `null`. Every caller then leaves the original file alone.

@code src/main/java/com/nltarzan/structures/config/ConfigUpdater.java 43-90

`update` is called for `monuments.yml` at startup and by `/nltstructures monument reload` (`Monuments.updateConfig`, step 2).

- **Lines 45–47:** no file yet: nothing to update. The default is copied out of the jar elsewhere.
- **Lines 48–56, the safety rules:** read the file strictly (`load` throws on a YAML mistake, see `Monuments.loadConfig` in step 2), and check it for a problem such as a missing location. Either way, the file is **left alone**. This matters: a file that couldn't be read would look empty, so every setting would seem to be missing, and the updater would write the defaults, including the default location. The monument would then be built somewhere else. Instead, the monument pauses and says what's wrong, as it always did.
- **Lines 57–60:** read the default file from the jar. This copy becomes the new file, so its comments and layout are the up-to-date ones.
- **Lines 62–67:** list the settings the jar's copy has and yours doesn't (**added**), and the ones yours has and the jar's doesn't (**removed**, settings a newer version no longer uses). The own sections, like `variants`, are left out of both lists (lines 124–129); only a whole own section that's missing counts as added (lines 64–65).
- **Lines 68–70:** nothing added or removed: the file is up to date and isn't touched, not even rewritten.
- **Line 72:** copy your values into the jar's copy (`keepValues`, below).
- **Lines 73–78:** move your file into the backups folder. If that fails, stop and keep your file; the missing settings then simply use their defaults.
- **Lines 79–85:** write the merged file where yours was.
- **Lines 86–89:** log what happened: which settings were added and removed, and where your previous copy is.

@code src/main/java/com/nltarzan/structures/config/ConfigUpdater.java 92-129

- **Lines 97–101:** for every setting in your file that still exists, put your value into the new file. A list, like `reset.intervals`, counts as one value, so it's kept whole.
- **Lines 102–117, own sections:** the keys of `variants:` are names you choose, so the new file gets exactly yours: names only the jar has are removed (lines 109–113), and every name you have is set with your weight (lines 114–116). This is done key by key, instead of replacing the whole section, so the comment above `variants:` stays.
- **Lines 124–129 (`settings`):** every setting as a full path such as `reset.mode` (sections that only group settings, like `reset`, don't count), leaving out everything inside the own sections.

## StructureCommand.java: the /nltstructures root

This class handles `help` and `reload` itself. For anything else it works out *which structure* is meant, then hands the rest of the command to that structure's own handler.

@code src/main/java/com/nltarzan/structures/command/StructureCommand.java 1-26

- **Line 1:** the package. It sits outside `monument`, because it's shared by every structure.
- **Lines 3–6:** the plugin class (for the permission and the command name), the structure modules, the messages, and `CommandHelp` for the help list.
- **Lines 7–8:** Paper's command system. `BasicCommand` is Paper's simple way to make a command: you get the typed words as a list of text. `CommandSourceStack` is "who ran it, and where": a player, the console or a command block.
- **Line 26:** `implements BasicCommand` means "this class *is* a Paper command". `BasicCommand` demands `execute`, and optionally `suggest` and `permission`.

@code src/main/java/com/nltarzan/structures/command/StructureCommand.java 28-54

- **Lines 29–33:** an interface of our own: what every structure's command must offer. `execute` runs the command; `suggest` returns the tab-completion options. Both receive `String[] args`, the words typed **after** the structure name, so for `/nltstructures monument reset generated` the monument's command receives `["reset", "generated"]`. `String[]` is an **array**: a fixed-size list.
- **Line 35:** the root command's own subcommands.
- **Lines 37–39:** the plugin (for its version), the messages, and a **map**: a lookup table from a key to a value, here from a structure name to its module. `LinkedHashMap` remembers the order things were added, so help and tab completion list structures in a fixed order.
- **Lines 46–49:** adds a structure, stored under its lowercase name so `/nltstructures MONUMENT` works too. `return this` hands the same object back, so calls can be chained.
- **Lines 51–54:** tells Paper that `/nltstructures` needs `nltstructures.admin`. Paper then does all the checking itself: players without it can't run it, don't get it in tab completion, and their client doesn't even see that the command exists. The aliases `/structures` and `/nlts` are the same command, so the same goes for them.

@code src/main/java/com/nltarzan/structures/command/StructureCommand.java 56-108

- **Line 57:** Paper calls this when someone runs `/nltstructures ...` (or one of its aliases).
- **Line 59:** the first word (`args[0]`; arrays count from 0), or `help` when nothing was typed, just like `/nltraces` on its own shows its help.
- **Line 61:** `help` lists the commands.
- **Lines 62–65:** `reload` re-reads `messages.yml`. Each structure's settings have their own reload, e.g. `/nltstructures monument reload`, so a mistake in one file never affects the other.
- **Lines 66–73:** anything else must be a structure name. An unknown word gets "Unknown command. Use /nltstructures help." (line 69). Otherwise everything *after* the structure name goes to that structure's command (line 72): `Arrays.copyOfRange(args, 1, args.length)` drops the first word, so `["monument", "status"]` becomes `["status"]`.
- **Lines 78–91 (`sendHelp`):** the help list, in the same layout as `/nltraces help`: a header with the plugin version, one line per command, and a footer pointing to `commands.txt`. The structure lines come from the same `HELP` lists that fill `commands.txt` (line 85), so the two can't disagree. Chat only gets the first sentence of each explanation (lines 93–96 cut the text at the first `". "`); `commands.txt` has the full text.
- **Lines 99–108:** tab completion. On the first word, suggest `help`, `reload` and the structure names that start with what's typed so far (`Stream.concat` joins the two lists, `filter` keeps the matches, `toList` collects them). After that, ask that structure's own `suggest` for the rest.

## MonumentCommand.java: /nltstructures monument ...

### Lines 31–95: the class, its constants and HELP

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 35-58

- **Line 35:** `implements StructureCommand.Subcommand`, so the root command can hand work to it.
- **Line 37:** the command's beginning, `/nltstructures monument`, built from the root command's name. It's used in every usage message, the help texts and the `<command>` tag in `messages.yml`.
- **Line 39:** the y-level an Ocean Monument is normally built at. `setcenter` reminds you to double check when you pick a different one.
- **Lines 41–42:** the eight subcommands, for tab completion and the usage message. They're package-private (no `private`), so the test `MonumentCommandTest` can read them.
- **Lines 48–95 (`HELP`):** one `CommandHelp` per way of using the command: the usage as typed, an explanation, and sometimes examples (line 58). `Monuments` hands this list to `InfoFiles`, which writes it into `commands.txt`, and to `/nltstructures help`. The test `MonumentCommandTest` fails if a subcommand is missing from `HELP`, or explained without existing (lines 44–47).

### Lines 97–127: picking the subcommand

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 97-127

- **Lines 100–103:** the constructor stores the `Monuments` object, so every subcommand can reach the settings, the timer, the current monument and so on, and takes the shared messages from it.
- **Line 107:** who ran it, a player or the console. `CommandSender` covers both.
- **Line 108:** the first word, or `status` when nothing was typed. That's why `/nltstructures monument` on its own shows the status.
- **Lines 109–126:** a `switch` that runs a different method per word:
  - **Line 111:** `active` needs the next word (`on`, `off` or nothing), so it gets `args`.
  - **Line 112:** `check` is a normal check, not forced; the two `null`s mean no variant and no seed.
  - **Line 113:** `reset` needs the remaining words for a variant or seed, so it gets `args`.
  - **Line 114:** `setcenter` gets the player's position (`source.getLocation()`, where the command was run) and the remaining words, which may be coordinates.
  - **Lines 117–124:** `reload`. If `reloadSettings()` succeeds, the reply is `monument.reloaded`. If not, it names the problem, with a different message for whether the monument stays paused or still uses the previous settings.
  - **Line 125:** any other word gets the usage message, listing the eight subcommands.

### Lines 129–156: tab completion

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 129-156

- **Lines 131–134:** first word: the eight subcommands, filtered on what's typed so far.
- **Lines 135–138:** second word after `reset`: `generated` plus every pre-built variant name. `Stream.concat` joins the two lists; `equalsIgnoreCase` compares text without caring about upper or lower case.
- **Lines 139–144:** second word after `broadcast` or `active`: `on` or `off`. After `layout`: `current` or `random`. `||` means "or".
- **Lines 145–151:** the coordinates after `setcenter`, for a player: your own position, rounded to the nearest block corner just like `setcenter` without coordinates would. The second word gets your x, the third your y and the fourth your z (`xyz[args.length - 2]` picks the right one), like vanilla commands such as `/tp` suggest.
- **Lines 152–154:** third word after `layout`: example newest chances.
- **Line 155:** nothing to suggest anywhere else, for example a seed, which can be any number.

### Lines 158–176: broadcast

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 158-176

- **Line 160:** a `boolean` holds `true` or `false`.
- **Lines 161–162:** no word after `broadcast`, so flip the current setting. `!` means "the opposite of".
- **Lines 163–166:** `on` or `off` sets it explicitly.
- **Lines 167–170:** anything else gets a usage message.
- **Lines 171–174:** save it. `setAnnounce` returns `false` when `monuments.yml` has a problem, because it then refuses to save over the broken file; the reply explains that (`configProblem`, lines 348–351).
- **Line 175:** confirm with `broadcasts-on` (green) or `broadcasts-off` (yellow).

### Lines 178–221: layout

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 183-194

- **`layout current`:**
  - **Line 185:** asks what's built. The answer is an `Optional`, which is empty when the plugin hasn't placed a monument yet (line 186).
  - **Line 188:** `instanceof Built.PreBuilt(String variant)` checks "is it a pre-built one?" and takes its variant name out in one go (a record pattern).
  - **Line 191:** for a generated one, `generated.layout()` rebuilds the layout from the saved seed and settings, and prints it.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 196-221

- **Line 196:** start with a random seed. A `long` is a very large whole number, which is what seeds are.
- **Line 197:** start with the maze settings from `monuments.yml`.
- **Lines 199–201:** a second word that isn't `random` is read as the seed. `Long.parseLong` turns text into a number and throws a `NumberFormatException` if it isn't one.
- **Lines 202–208:** a third word is the newest chance, which must be between 0 and 1. A number outside that range deliberately throws the same error (line 205), so both mistakes end up in one place.
- **Lines 209–212:** `catch` handles that error: show the usage message and stop. This is how `try`/`catch` turns a crash into a friendly message.
- **Line 213:** generate the layout and print it.
- **Lines 216–221:** writes the map to the console. `monuments.getLogger()::info` means "for each line, call `info(line)` on the logger". A player also gets a short chat note with the seed and newest chance; `key` picks the "Layout" or "Current layout" wording.

### Lines 223–244: reset

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 223-244

- **Line 225:** the second word is a variant name or `generated`. Without one it's `null`, which means "follow `reset.mode`".
- **Lines 226–228:** `Generated` with a capital also means `generated`.
- **Line 229:** `Long` with a capital L can also be `null`, meaning "no seed given".
- **Lines 230–242:** a third word is a seed. Seeds only work with `generated`; for a pre-built variant the reply is red and nothing is reset (lines 231–235). A seed that isn't a number gets a usage message.
- **Line 243:** run a **forced** reset (`true`).

### Lines 246–253: status

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 246-253

- **Line 248:** the heading "Ocean Monument", with the prefix.
- **Lines 249–252:** for each status line from `Monuments` (the same ones as in `status.txt`) one `status-line`: the label in gray, then the value. The value is passed as a `Component` that already has its own color (line 251), so a problem stays red and "On" stays green whatever `messages.yml` says around it.

### Lines 255–284: active

This switches the monument on or off. It's built the same way as `broadcast`, so it works the same way: `active on`, `active off`, or just `active` to switch.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 255-284

- **Line 257:** whether it's switched on right now. This asks the timer directly, not `isActive()`: that one is also false while paused, and switching should follow what the admin last chose.
- **Lines 258–268:** work out the wanted state, exactly like `broadcast`: flip it when nothing follows, otherwise `on` or `off`, and a usage message for anything else.
- **Lines 269–272:** already in the wanted state? Just say so, in yellow, and change nothing. Without this, `active on` on an active monument would restart the timer for no reason.
- **Lines 273–276:** switch it. `setActive` refuses to turn **on** while `monuments.yml` is broken; the reply says why.
- **Lines 277–280, on:** a green reply naming the exact location it will build at, so a wrong location is noticed right away. The first check comes after a fresh random wait; `status` shows when.
- **Lines 281–283, off:** a yellow reply: nothing is checked or built until it's turned on again.

### Lines 286–363: running a check, setcenter, and sending messages

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 286-296

- **Lines 286–296, check or reset:**
  - **Line 287:** an immediate gray "Checking…" or "Resetting…", because the actual work can take a while.
  - **Line 288:** asks the timer to run a check now, so it can start a fresh random wait afterwards. The result isn't ready immediately (loading chunks and building take several ticks), so `runNow` returns a **`CompletableFuture`**, a "promise" of a result later. `.thenAccept(result -> ...)` says what to do once it arrives: send the reply.
  - **Lines 289–294:** one message per outcome. The compiler checks that all four outcomes are handled, because `Result` is a sealed type with exactly these four kinds.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 298-346

`setcenter` works in two ways: on its own it uses where you're standing, and with three numbers it uses exactly those.

- **Lines 303–306:** the four values to fill in, by whichever way is used.
- **Lines 307–316, without coordinates** (`args` only holds `setcenter`):
  - **Lines 308–311:** only a player has a position to use, so the console is told to give coordinates.
  - **Lines 313–316:** the player's world, with x and z rounded to the **nearest block corner**, matching the datapack's marker, which stood exactly on the corner where the 4 center blocks meet. `(int)` turns the rounded number into a whole number. y is the height of the block the player's feet are in, which is the level the rooms start from.
- **Lines 317–332, with coordinates** (`setcenter` plus three words):
  - **Lines 318–325:** read the three numbers. Something that isn't a whole number (like `abc` or `40.5`) gets a red message instead. Whole numbers are what you want: the center is a block corner.
  - **Lines 326–328:** a player sets it in the world they're in; the console has no world of its own, so it keeps the world from `monuments.yml`.
  - **Lines 329–331:** that world must be loaded.
- **Lines 333–336:** any other number of words (one or two coordinates, or four) gets the usage message.
- **Lines 338–341:** save it. If `monuments.yml` is broken, nothing is saved, and the reply says why.
- **Line 342:** confirm, including the world, so a center set in the wrong world is noticed right away.
- **Lines 343–345:** a y-level other than 40 is allowed, but gets a gold reminder: "Double check your y-level. An Ocean Monument is usually set at y=40." This catches the most likely mistake: standing one block too high or too low.

@code src/main/java/com/nltarzan/structures/monument/command/MonumentCommand.java 348-363

- **Lines 348–351:** the shared red "Not saved…" message, used by `broadcast` and `setcenter`.
- **Lines 353–355:** the shared usage message: `general.usage` with the full command, e.g. `/nltstructures monument active [on|off]`.
- **Lines 358–363:** every other reply goes through here. It adds `monument.` in front of the key, so `send(sender, "placed", ...)` sends `monument.placed`, and always adds a `<command>` tag with `/nltstructures monument`, so the texts in `messages.yml` can name the command without anyone having to pass it in. `Arrays.copyOf` makes the placeholder list two longer to fit it.
