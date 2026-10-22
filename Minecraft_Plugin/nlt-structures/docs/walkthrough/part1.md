# About this document

This document explains the nlT-Structures plugin file by file. It brings together the step-by-step walkthrough from our conversation, updated to the code as it is now: some files changed after they were first explained, so their explanations and line numbers have been brought up to date here.

**How to read it**

- Every grey box is a real excerpt from the project, copied automatically from the source file when this PDF was made. The number in front of each line is the line number in that file, so you can open the file and find the same line.
- A box marked with `⋮` skips some lines in between.
- Explanations refer to those line numbers, for example "**Line 45**" or "**Lines 103–105**".
- Java ideas are explained the first time they appear. Step 2 starts with a short table of the basics.

**The steps**

| Step | Files | What it covers |
|---|---|---|
| 1 | `settings.gradle.kts`, `build.gradle.kts`, `plugin.yml`, `monuments.yml` | How the plugin is built, what Paper needs to know, the settings |
| 2 | `NltStructuresPlugin`, `StructureModule`, `Monuments`, `MonumentConfig`, the `info` package | Startup, structure modules, reading the settings, `status.txt` and `commands.txt` |
| 3 | `Messages`, `messages.yml`, `StructureCommand`, `MonumentCommand` | The chat messages and the `/nltstructures` commands |
| 4 | `Durations`, `Weighted`, `ResetScheduler`, `MonumentArena`, `StructureLibrary`, `CurrentMonument`, `MonumentResetter` | How a check and a reset happen |
| 5 | the `layout` package | Generating a random maze layout |
| 6 | `BuildStep`, `BuildPlanner`, `MonumentBuilder` | Turning a layout into blocks |
| 7 | `ElderGuardianListener`, `MonumentHoe`, `MonumentHoeListener` | The reward and the Monument Hoe rules |
| 8 | `src/test` | How the automated tests work |

**Where the files are**

```
nlt-structures/
├── build.gradle.kts, settings.gradle.kts      the build
├── src/main/resources/                         plugin.yml, messages.yml, monuments/monuments.yml,
│                                               structures/monument/ (the .nbt templates)
├── src/main/java/com/nltarzan/structures/      the plugin's code
│   ├── NltStructuresPlugin.java, StructureModule.java
│   ├── command/        the shared /nltstructures command
│   ├── config/         Messages: the texts from messages.yml
│   ├── info/           status.txt and commands.txt
│   ├── util/           Durations, Weighted
│   └── monument/       everything Ocean Monument
│       ├── command/  config/  layout/  build/  listener/
└── src/test/java/com/nltarzan/structures/      the tests
```

# Step 1: setup files

These four files don't contain game logic. They say how the plugin is built, what Paper needs to know about it, and what you can configure.

## settings.gradle.kts

@code settings.gradle.kts 1-1

- **Line 1:** the project's name. Gradle uses it as the project name in its output and in IntelliJ. (The jar gets its own name, `nlT-Structures-0.1.0.jar`, in `build.gradle.kts` line 45, matching nlT-Races.) That's all this file does for now. If the plugin ever grows into separate modules (one per structure, say), they'd be listed here.

## build.gradle.kts

The build recipe. Gradle runs it whenever you type `.\gradlew.bat build` or `.\gradlew.bat runServer`. It's written in Kotlin (`.kts`), which reads much like Java.

### Lines 1–6: tools for the build

@code build.gradle.kts 1-6

- **Line 1:** makes the type `RunServer` available, for the 26.3 test server further down (line 70). It works like `import` in Java.
- **Line 4:** the `java` plugin. Gradle learns to compile Java, run tests and build a jar. This creates the tasks `build`, `test`, `jar` and `clean`.
- **Line 5:** the **run-paper** plugin (version 3.1.0). It adds `runServer`, which downloads Paper, adds your plugin and starts a test server.

### Lines 8–13: project details

@code build.gradle.kts 8-13

- **Line 8:** who made it, written as a reversed domain name. It matches the Java packages (`com.nltarzan.structures…`). It only really matters if you ever publish the plugin as a library for other developers.
- **Line 9:** the plugin version. It appears in the jar name, in `plugin.yml` (via line 49) and in `/plugins`. Raise it with each release, for example `0.2.0`.
- **Line 13:** a variable used twice: as the `api-version` in `plugin.yml`, and as the version of the test server. Lines 11–12 explain that `26.2` means "26.2 or newer", so 26.3 works too.

### Lines 15–18: where libraries come from

@code build.gradle.kts 15-18

- **Line 16:** Maven Central, the main public store for Java libraries. JUnit (for the tests) comes from here.
- **Line 17:** PaperMC's own store. The Paper API comes from here.

### Lines 20–29: libraries

@code build.gradle.kts 20-29

- **Line 21:** the **Paper API**: everything in the code starting with `org.bukkit` or `io.papermc`. `compileOnly` means "needed to compile, but don't pack it into the jar", because the server already has Paper. `26.2.build.132-stable` is the exact build, the same one nlT-Races uses.
- **Lines 22–23:** the tests also get the Paper API, because they read YAML files and use text colors. `testImplementation` means it's only used for the tests; it never ends up in the plugin.
- **Lines 25–28:** **JUnit 6**, for the tests in `src/test`.
  - **Line 25:** a "BOM" that fixes all JUnit parts to version 6.1.3, so lines 26–28 don't need a version of their own.
  - **Line 26:** the basics: `@Test` and `assertEquals`.
  - **Line 27:** tests that run several times with different values (`@ParameterizedTest`), for example once per newest chance.
  - **Line 28:** the part that actually starts the tests. Gradle 9 requires it to be listed separately.

### Lines 31–46: Java version, tests and the jar name

@code build.gradle.kts 31-46

- **Line 32:** always compile with Java 25, whatever `java` your PATH points to.
- **Line 36:** read source files as UTF-8, so special characters in comments can't be misread.
- **Line 37:** produce code for Java 25 and only allow Java 25 features. Paper 26.x needs Java 25 itself.
- **Line 41:** tells Gradle the tests are JUnit tests. Without it, `test` finds nothing.
- **Line 45:** the jar's file name: `nlT-Structures-0.1.0.jar`, written like the plugin's name and like nlT-Races' `nlT-Races-0.1.0.jar`. `${project.version}` fills in the version from line 9.

### Lines 48–54: filling in plugin.yml

@code build.gradle.kts 48-54

`processResources` copies everything in `src/main/resources` into the jar: `plugin.yml`, `messages.yml`, `monuments/monuments.yml` and the monument structures in `structures/monument/` (the 44 pre-built quadrant files in `pre-gen/`, the 19 maze templates in `maze/`). Here it does one thing more:

- **Line 49:** the values to fill into `plugin.yml`: the version (line 9) and the API version (line 13).
- **Line 50:** tells Gradle this step depends on those values, so a version change makes it run again instead of reusing an old result.
- **Lines 51–53:** replaces the placeholders `${version}` and `${apiVersion}` in `plugin.yml` with those values.

The `.nbt` files came from your old "Structures" datapack and now live in the plugin itself, so the plugin no longer needs the datapack. If you change a structure in-game, save it with a structure block and copy the new `.nbt` over the old one in `src/main/resources/structures/monument/`. The folder is called `structures/monument/`, which fits the plan for more structures: a mansion would go in `structures/mansion/`.

### Lines 56–76: test servers

@code build.gradle.kts 56-76

- **Lines 59–63:** *every* test server task (`withType<RunServer>`) downloads LuckPerms v5.5.71 from Modrinth into its plugins folder.
- **Lines 65–68:** the standard `runServer` task.
  - **Line 66:** Paper 26.2.
  - **Line 67:** the server lives in `run/26.2`: world, `server.properties`, `eula.txt` and `plugins/nlT-Structures/`.
- **Lines 70–76:** a self-made second task, `runServer26_3`.
  - **Lines 71–72:** how it appears in Gradle's task list.
  - **Line 73:** Paper 26.3.
  - **Line 74:** its own folder, so the two worlds never mix.
  - **Line 75:** adds your plugin jar. `runServer` does this automatically; a self-made task has to be told. `flatMap` means "whatever file the jar task produces", so the jar is built first.

**The files Gradle brought along:**

- **The Gradle wrapper** (`gradlew`, `gradlew.bat`, the `gradle/wrapper` folder): it downloads exactly Gradle 9.8.0 on first use, so nobody needs to install Gradle.
- **`.gitignore`:** lists what Git should skip if you ever put the project in a repository: `build/`, `run/` and IDE files.

## src/main/resources/plugin.yml

Paper reads this file first to learn what the plugin is. If it's missing or wrong, the plugin doesn't load.

@code src/main/resources/plugin.yml 1-11

- **Line 1:** the plugin's name. It shows in `/plugins` and in log lines (`[nlT-Structures] …`), and it names the plugin's folder: `plugins/nlT-Structures/`.
- **Line 2:** the version, filled in by the build (`0.1.0`).
- **Line 3:** the class Paper starts first: `NltStructuresPlugin` in the package `com.nltarzan.structures`. It must be a class that extends `JavaPlugin`. That's the start of step 2.
- **Line 4:** the minimum Minecraft version (`26.2`). An older server refuses to load the plugin.
- **Line 5:** a description, shown in `/version nlT-Structures`.
- **Line 6:** the author.
- **Lines 8–11:** the permissions the plugin defines.
  - **Line 9:** the one permission, `nltstructures.admin`.
  - **Line 10:** what it's for. LuckPerms shows this text, for example in its web editor.
  - **Line 11:** `default: false` means nobody has it unless it's granted, **not even ops**. The alternative, `default: op`, would hand it to every op automatically.

There's no `commands:` section. That's the old way of declaring commands; the plugin registers `/nltstructures` and its aliases `/structures` and `/nlts` in code instead (step 2).

## src/main/resources/monuments/monuments.yml

This is the **default** Monument config. The first time the plugin starts, it copies this file to `plugins/nlT-Structures/monuments/monuments.yml`, next to the monument's saved data. Each structure keeps its config and its data together in its own folder. After that it only reads that copy and never overwrites it, so after an update you have to add new settings to your server's copy yourself. Lines starting with `#` are comments, ignored by the plugin.

### Lines 1–6: header

@code src/main/resources/monuments/monuments.yml 1-6

This file is only for Ocean Monuments ("monuments" for short). It's checked regularly, and replaced by a pre-built or generated monument once it's cleared.

### Lines 8–22: location (where the monument is)

@code src/main/resources/monuments/monuments.yml 8-22

- **Lines 9–12:** the center replaces the datapack's marker entity. It's the corner where the 4 center blocks meet. The rooms start at this height; the empty shell and the pre-built quadrants are placed 1 block lower.
- **Lines 13–14:** a reminder that a new server builds nothing until `/nltstructures monument active on` (step 3). That's the moment to check these values.
- **Line 15:** the world, matching the world's folder name. Your normal overworld is `world`.
- **Lines 16–18:** the center's coordinates. `/nltstructures monument setcenter` rewrites them for you, from where you stand or from coordinates you type.
- **Line 22:** a radius in blocks around the center. Elder Guardians and players within it count as "at the monument", and so does anything inside the monument's 58×22×58 box, even its far corners (lines 19–21). The chunks covering both are kept loaded during a check. The hoe rules use only the exact box.

The plugin checks this section strictly. If it's missing or incomplete, the monument is paused instead of being built somewhere else (step 2). And even with a valid location, nothing happens on a new server until an admin turns the monument on.

### Lines 24–49: reset (when and how it resets)

@code src/main/resources/monuments/monuments.yml 24-49

- **Line 29:** what a reset places: `pre-built` (one of the 10 variants) or `generated` (a new maze). Lines 25–28 explain the options.
- **Lines 33–37:** the time between checks. After each check one line is picked at random, using `weight` as its share. Out of 30+30+30+10 = 100 that gives:

| Wait | Chance |
|---|---|
| 1 Minecraft day | 30% |
| 2 Minecraft days | 30% |
| 3 Minecraft days | 30% |
| 72 Minecraft days (1 real day) | 10% |

These are the values that were commented out in your datapack. Line 31 lists the units: `t` ticks, `s` seconds, `d` Minecraft days, where 1 day is 20 real minutes. Line 32 suggests short values like `10s` for testing.

- **Lines 40–43:** used when the monument is cleared but a player is still inside. The next check then comes after 5, 10 or 15 minutes, each equally likely. If these are longer than the normal intervals, the plugin logs a warning.
- **Line 46:** before a new monument is placed, all remaining Guardians in the area are removed, so they don't pile up over many resets.
- **Line 49:** reset messages go to players with `nltstructures.admin`. `/nltstructures monument broadcast` toggles this line and saves the file.

### Lines 51–65: variants (pre-built monuments)

@code src/main/resources/monuments/monuments.yml 51-65

- **Lines 54–65:** each name stands for four files, `<name>_ne`, `_nw`, `_se` and `_sw` `.nbt`. The number is that variant's weight.
  - `monument_00` (the empty monument) stays at 0, as in your datapack, where it could never be rolled.
  - The other ten each get an equal 10% chance.
  - Set one to 0 to switch it off, or raise it to make it more common.

### Lines 67–78: elder-guardian and reward

@code src/main/resources/monuments/monuments.yml 67-78

- **Line 72:** pre-built only. It's how high above `location.y` the third Elder Guardian spawns: 11.5, because the pre-built quadrants are placed one block below the center (lines 70–71).
- **Lines 73–74:** health and attack damage for the Elder Guardians the plugin spawns: one with pre-built monuments, all three with generated ones. Vanilla is 80 and 8; these are your datapack's tougher values.
- **Line 78:** whether the player who kills the last Elder Guardian gets the Monument Hoe.

### Lines 80–97: maze (generated monuments)

@code src/main/resources/monuments/monuments.yml 80-97

- **Lines 83–88:** what the newest chance does: 1.0 gives long winding corridors (depth-first), 0.0 gives lots of short branches (like Prim's).
- **Line 90:** the most common value, the top of the bell curve.
- **Line 91:** how wide the curve is. About 2 in 3 monuments get a value between 0.50 and 0.80.
- **Lines 92–93:** hard limits. A value outside them is drawn again.
- **Line 97:** each generated monument gets at least this many sponge rooms. Lines 95–96 describe how: first a dead end far from the entrance is used; only if there isn't one does another unit get used, with the maze reconnected around it.
