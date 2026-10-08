# Step 8: how the tests work

## What a test is, and why this plugin has them

A **test** is a small piece of code that runs part of the plugin and checks the result: "if I give `parseTicks` the text `10s`, I must get 200 back". If the result is wrong, the test **fails** and tells you exactly what went wrong.

- **No server needed.** The whole suite (46 tests) runs in about 4.5 seconds. Testing the same things in-game would take hours.
- **Thousands of cases at once.** One test generates 3,000 monuments and checks every one of them.
- **They guard what already works.** If a later change accidentally breaks something, like the sponge guarantee or the rotation math, a test fails straight away instead of a player finding it.

## Where they live

```
src/test/java/com/nltarzan/structures/
├── util/          DurationsTest, WeightedTest
├── config/        MessagesFileTest
├── info/          InfoFilesTest
└── monument/      MonumentArenaTest
    ├── build/     BuildPlannerTest
    ├── command/   MonumentCommandTest
    ├── config/    MonumentConfigTest
    └── layout/    MonumentLayoutTest
```

- **Mirrored packages:** the test folders have the same package names as the code they test, so a test counts as being in that package and can use package-private things like `GrowingTreeGenerator`.
- **Not in the plugin:** tests never end up in the jar (that's what `testImplementation` in `build.gradle.kts` arranges).

## JUnit basics, using the smallest test

The tests use **JUnit**, the standard Java test framework.

@code src/test/java/com/nltarzan/structures/util/DurationsTest.java 1-40

- **Lines 5–6:** `import static` lets you write `assertEquals(...)` instead of `Assertions.assertEquals(...)`.
- **Line 8:** a test class, named after what it tests plus `Test`.
- **Line 10:** `@Test` marks a method as a test. JUnit finds every method marked like this and runs it.
- **Line 11:** the method name says what it checks, in plain words; when it fails, that name is the first thing you see.
- **Lines 12–16, `assertEquals(expected, actual)`:** if the two differ, the test stops and fails with "expected: <200> but was: <…>". If every line passes, the test passes.
- **Lines 27–31, `assertThrows(…)`:** passes only if the code **does** throw that error. `() -> …` is a lambda, so JUnit can run the code itself and catch the error. These lines prove that bad input like `5m` is refused instead of quietly turning into a wrong number.

### The checks you'll see

| Check | Passes when |
|---|---|
| `assertEquals(a, b)` | `a` and `b` are equal |
| `assertNotEquals(a, b)` | they're different |
| `assertTrue(x)` / `assertFalse(x)` | `x` is true / false |
| `assertNull(x)` / `assertNotNull(x)` | `x` is / isn't "nothing" |
| `assertSame(a, b)` | `a` and `b` are the very same object (not just equal) |
| `assertThrows(Error.class, () -> …)` | the code throws that kind of error |

Every check can take a last argument: **a message** shown when it fails. The tests use it a lot to say *where* the failure was, for example `"chance 0.5 seed 1234: no sponge room"`.

## Two kinds of tests

### 1. Exact examples: "this input must give exactly this output"

@code src/test/java/com/nltarzan/structures/monument/build/BuildPlannerTest.java 57-68

- **`rotatedHallsMatchTheDatapackOffsets`** takes your datapack's hallway commands and checks the plugin computes **the same** positions.

@code src/test/java/com/nltarzan/structures/monument/config/MonumentConfigTest.java 39-50

- **`missingOrBrokenLocationIsCaught`** feeds in small broken pieces of `monuments.yml` and checks each one is caught with the right message. `\n` is a line break inside the text, so each string is a small YAML file.

### 2. Mass tests: "check thousands of generated monuments against the rules"

The maze is random, so one example proves little. These tests generate **thousands** of layouts and check every rule on every one.

@code src/test/java/com/nltarzan/structures/monument/layout/MonumentLayoutTest.java 95-116

- **Lines 95–96, a parameterized test:** `@ParameterizedTest` with `@ValueSource` runs the same test **once per value**: here 5 times, for newest chances 0, 0.25, 0.5, 0.75 and 1. Each run shows up separately, so you'd see "fails at 0.25 only".
- **Line 99:** 3,000 seeds per value, so 15,000 layouts in total.
- **Line 101, the `where` text:** every check gets it, so a failure tells you the exact chance and seed, which you could then look at with `/nltstructures monument layout <seed> <chance>`.
- **Lines 104–113, the rules checked for every layout:** everything can be reached from the entrance; the entrance ceiling is closed; it's a perfect maze (N cells joined by exactly N − 1 open walls); there's at least one sponge room; every unit has exactly one room; and each room obeys its own rules (`checkRoom`).

The seeds are fixed numbers (`0, 1, 2, …`), not random ones, so every run tests exactly the same layouts and a failure can always be reproduced. A test that only *sometimes* fails would be nearly impossible to fix.

### Statistical tests: "roughly", with a margin

@code src/test/java/com/nltarzan/structures/util/WeightedTest.java 27-37

- **The margin:** 30% of 10,000 is 3,000. Anything between 2,700 and 3,300 passes, so normal random variation never causes a failure, but a real bug would (weights being ignored gives about 5,000).

The same idea is used for the bell curve (`newestChanceFollowsTheBellCurve`), the sponge columns (`spongeColumnsFollowVanillaChances`), the wings (`openWingsUseBothFloors`) and the maze character (`newestChanceGoesFromCorridorsToBranches`).

## What each test file guards

| Test file | Tests | What it protects |
|---|---|---|
| `DurationsTest` | 4 | `1d`, `10s`, `5t` and decimals are read correctly; bad times are refused; times print as `1h 4m 30s` |
| `WeightedTest` | 4 | weighted picks follow the weights; weight 0 is never picked; negative weights are refused |
| `MonumentConfigTest` | 4 | the default `monuments.yml` is valid; every way of breaking `location:` is caught; broken YAML is detected |
| `MessagesFileTest` | 1 | the default `messages.yml` is valid YAML, has its `messages-version` and prefix, and every text can be read as MiniMessage. nlT-Races has the same test for its own `messages.yml` |
| `MonumentCommandTest` | 2 | `commands.txt` lists exactly the subcommands that exist, each with an explanation and correct examples |
| `InfoFilesTest` | 2 | `status.txt` and `commands.txt` have the read-only header, aligned values, every command, and no line wider than 90 characters |
| `MonumentArenaTest` | 4 | the monument's far corners count as inside; the sphere still works; a small radius still covers the monument |
| `MonumentLayoutTest` | 18 | the grid matches vanilla; open wings; all layout rules on 15,000 layouts; the bell curve; the sponge guarantee; the same seed gives the same layout; every room type appears in both directions |
| `BuildPlannerTest` | 7 | node positions match your datapack's markers; the rotation math matches your hallway files; every template covers exactly its units; no overlaps; the right build order; doorways; sponge chances |

## Running them

| Command | What it does |
|---|---|
| `.\gradlew.bat build` | Compiles, **runs all tests**, and builds the jar. If any test fails, the build fails. |
| `.\gradlew.bat test` | Only runs the tests. |
| `.\gradlew.bat test --tests "*DurationsTest"` | Only one test class (`*` means "any package"). |
| `.\gradlew.bat test --tests "*MonumentLayoutTest.sameSeed*"` | Only one test method. |

Gradle skips tests if nothing changed since the last run; add `--rerun` to force them to run again. After a run, open `build\reports\tests\test\index.html` in a browser: it lists every test, green or red, with the time it took and the full failure message.

## What a failure looks like

A real one from this project. When `InfoFilesTest` was added, it failed with:

```
InfoFilesTest > commandsFileListsEveryMonumentCommand() FAILED
    AssertionFailedError: line too long: READ-ONLY: generated by the plugin. Rewritten by the plugin
    on every start, so it always matches the installed version. ==> expected: <true> but was: <false>
```

- **Which test:** `commandsFileListsEveryMonumentCommand`.
- **Which check:** the message `line too long: …` came from the check that every line in `commands.txt` is at most 90 characters.
- **What was wrong:** it even quotes the offending line, the header. Splitting it into two lines made the test pass.

An earlier failure worked the same way: a test about the newest chance failed with "chances should spread over the range", which revealed that `java.util.Random` gives almost identical first values for nearby seeds and led to the switch to `SplittableRandom`.

## What the tests can't check

Anything that needs a running server isn't covered: the listeners (the hoe rules, the Elder Guardian reward), `MonumentBuilder` actually placing blocks, chunk loading, commands typed in-game, and GriefDefender working alongside the plugin. That's why the tests concentrate on everything that *decides* what happens (the layout, the plan, the positions, the config checks), and the server-side code is kept as simple as possible. **Those in-game parts still need testing on the server.**

## Adding a test yourself

Say you want to be sure that `2.5s` becomes 50 ticks. Add this to `DurationsTest.java`, next to the other tests:

```
    @Test
    void decimalSeconds() {
        assertEquals(50, Durations.parseTicks("2.5s"));
    }
```

Then run `.\gradlew.bat test --tests "*DurationsTest"`. Change the 50 to 51 and run it again to see what a failure looks like.
