# Notes for Claude

This repo is worked on from more than one PC. These notes carry the context that isn't obvious from the code, so a session on any PC can pick up where the last one stopped. Keep them up to date when decisions change.

## The user

- Publishes as **nlTarzan**: use it for author fields (`author: nlTarzan` in plugin.yml) and `com.nltarzan` as the Java package root. Never use the Windows user name or email.
- Is learning Java by going through the code file by file, so explanations (and the walkthrough PDF) are written for a beginner.
- Runs the plugins on a real Paper server that uses **LuckPerms** and **GriefDefender**.

## Repository layout

One git repo for the whole `Claude` folder (`main`, remote `origin` = https://github.com/nlTarzan/Claude). Every project is its own folder, listed in the root `README.md`.

| Folder | What it is |
|---|---|
| `Minecraft_Plugin/Structures` | The original "Structures" datapack, no longer used on the server (kept for reference). Its monument `.nbt` files were moved to `nlt-structures/src/main/resources/structures/monument/` on 2026-10-22 |
| `Minecraft_Plugin/nlt-structures` | **nlT-Structures** Paper plugin (was "ResettingStructures" / `resetting-structures`, renamed 2026-10-08) |
| `Minecraft_Plugin/nlt-races` | **nlT-Races** Paper plugin. Design and decisions are in its `DESIGN.md` |

## The two plugins must stay consistent

The user wants nlT-Structures and nlT-Races to look and work the same wherever they overlap. When changing one, check whether the other should follow.

| Aspect | Convention (both plugins) |
|---|---|
| Plugin name / jar | `nlT-Races` / `nlT-Races-<version>.jar`, `nlT-Structures` / `nlT-Structures-<version>.jar` |
| Commands | `/nltraces` (aliases `/races`, `/nltr`), `/nltstructures` (aliases `/structures`, `/nlts`). No arguments or `help` shows the help list; `reload` reloads |
| Permissions | `nltraces.admin` / `nltraces.user`; `nltstructures.admin` (default false, not even ops) |
| Messages | All chat text in `messages.yml` (MiniMessage), loaded by a `Messages` class with the same API in both plugins; gold `[nlT-...]` prefix; `messages-version` key triggers replacing an outdated file |
| Config updates | A `ConfigUpdater` adds new settings and drops unused ones in the server's config at startup and on reload, keeping the admin's values; replaced files go to `backups/<name>-<yyyy-MM-dd_HH-mm-ss>.yml` via a `Backups` class. nlT-Races updates `config.yml`; nlT-Structures updates each structure's file (`monuments/monuments.yml`) and never touches a file that can't be read or has no valid location, and keeps "own" sections (`variants`) as the admin has them |
| Build | Gradle 9.8.0 wrapper, Java 25, Paper API `26.3.build.209-stable` (moved up from 26.2 on 2026-10-22, so 26.3 is the minimum), `api-version` filled in from `minecraftVersion` in `build.gradle.kts`, run-paper 3.1.0 for test servers (`gradlew runServer`, one task, in `run/` for nlT-Races and `run/<minecraftVersion>` for nlT-Structures), JUnit 6 tests |
| Docs | Admin manual in `docs/manual/` (`manual.md` -> `build-pdf.ps1` -> `docs/<name>-manual.pdf`). nlT-Structures also has a beginner code walkthrough in `docs/walkthrough/` |
| `.gitignore` | Identical in both |

Things that are deliberately the same: status/info lines are built in code with only the line layout in `messages.yml`; item names (the Monument Hoe) stay in code.

## nlT-Structures design decisions

- Every structure is its own module: package `com.nltarzan.structures.<structure>`, a `StructureModule` class like `Monuments`, its own folder in the plugin folder holding config and saved data (`monuments/monuments.yml`, `state.yml`, `current.yml`), and a `/nltstructures <structure> ...` subcommand. Mansions and End Cities are planned.
- Each module supplies status lines and command help for the generated, read-only `status.txt` and `commands.txt`.
- A broken or location-less config must never cause a build elsewhere: at startup the structure is paused, on reload the previous settings stay.
- Every structure has `active [on|off]` (no argument = toggle). Off on a new server, so nothing is built before the admin checks the location. Saved in `state.yml`; off also disables that structure's player rules (hoe rules) and forgets the countdown. New structures must get this too.
- Players see as little text as possible: only the Monument Hoe messages. Broadcasts go only to `nltstructures.admin`.
- With GriefDefender the plugin only ever cancels block breaks, never un-cancels them.
- 2x1x2 rooms keep vanilla odds (about 1 in 5 monuments) on purpose.
- The walkthrough's `@code` line ranges must be updated when code moves: run `Remap.java` against the previous build's HTML (`build/walkthrough/walkthrough.html`, not in git: run `build-pdf.ps1` once on a fresh clone to create it) before rebuilding. See `docs/walkthrough/README.md`.

## Working on Windows: things that went wrong before

- **Gradle can't run inside Claude's sandboxed shell** ("Unable to establish loopback connection"), even with the sandbox off. Ask the user to run `.\gradlew.bat build` themselves, or compile with `javac` against the jars in `~/.gradle/caches/modules-2/files-2.1` to check code (quote paths: the home folder has a space).
- **Bash heredocs containing apostrophes** fail in Claude's shell. Write files with the Write tool instead.
- **Renaming a project folder fails while something has it as its current directory** (VS Code, or Claude's own PowerShell session after a `Set-Location` into it). Move every shell out first.
- The PDFs are built with Java 25 (Temurin, `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`) and headless Microsoft Edge, through each `build-pdf.ps1`.
- Not in git (recreate on a new PC): `build/`, `.gradle/`, and the local test servers in `run/` (`gradlew runServer` creates a fresh one). Both plugins' `runServer` downloads its helper plugins (LuckPerms; nlT-Races also PlaceholderAPI) through run-paper's `downloadPlugins`, since 2026-10-22 for nlT-Races. A `run/plugins` folder with hand-installed copies from before then must have those jars removed, or Paper finds each plugin twice.

## Open items

- None right now.

## Fixed, worth knowing

- 2026-10-22: nlT-Races' `ConfigUpdater` now loads `config.yml` strictly. A file with a YAML mistake is left alone: the plugin disables itself on startup, and `/nltraces reload` keeps the previous settings. Before, a loose load made it look empty and the file was replaced with the defaults.
- Running the unit tests with plain `javac`/`java` (when Gradle can't run): this PC's Gradle cache holds old Bukkit/Spigot jars and other jars with their own `config.yml`. Leave `org.bukkit`/`org.spigotmc` jars off the classpath and put the compiled classes and resources first, or tests fail for reasons unrelated to the code. The `dunky` PC had only Java 8 on `PATH`, so `gradlew` refused to start; Temurin 25 was installed there on 2026-10-22 with `winget install --id EclipseAdoptium.Temurin.25.JDK -e --custom "ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"` (sets `JAVA_HOME` and `PATH`; terminals opened before the install, including the Claude app's terminal panel, need a restart). `gradlew build runServer` works there since. That PC has no Edge in `Program Files (x86)`, so the PDFs can't be rebuilt there: rebuild them on the other PC.
