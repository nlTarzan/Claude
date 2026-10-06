# Step 6: from layout to blocks

Step 5 produced a **layout**. Step 6 turns it into blocks in the world, in two separate parts:

| Part | What it does | Touches the world? |
|---|---|---|
| `BuildStep` | the "vocabulary": place a template, fill a box, spawn an Elder Guardian | no |
| `BuildPlanner` | turns a layout into a **list** of those steps, all positions relative to the center | no |
| `MonumentBuilder` | carries out the list in the world, a bit per tick | yes |

Why split it this way: the planner doesn't need a server, so the tests can check thousands of plans (every room covers exactly its own units, no rooms overlap, the rotations match your datapack's hallway offsets, the sponge odds come out right). Only `MonumentBuilder` needs a real server, and it's deliberately simple: it just does what the list says.

## BuildStep.java: the three kinds of step

@code src/main/java/com/nltarzan/structures/monument/build/BuildStep.java 7-46

- **Line 7:** a sealed interface: a build step can only be one of the three records below.
- **Lines 10–14:** a position **relative to the monument's center**, like `~` in a command. `plus` gives a new offset moved by some amount, for example `node.plus(-4, 0, -4)` means `~-4 ~ ~-4` from a node. Because positions are relative, the same plan works at any location.
- **Lines 17–22:** the four rotations, the same as `/place template`'s.
- **Lines 25–30:** the only blocks the builder places itself; everything else comes from templates. These are the plugin's own enums, not Bukkit's, which keeps the planner free of Bukkit so it can be tested without a server. The builder translates them.
- **Line 36, `Place`:** like `/place template <template> <origin> <rotation>`. The origin is the template's 0,0,0 corner.
- **Line 40, `Fill`:** like `/fill <from> <to> <block>`, with both corners included.
- **Line 44, `SpawnElder`:** spawn an Elder Guardian at an exact position. It uses decimals, because a mob can stand anywhere.

## BuildPlanner.java: from layout to steps

### Where the units are

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 32-40

- **Line 33:** a fixed number mixed into the seed for the build's own random choices (rotations and sponge rolls), so they don't repeat the layout's random choices but are still the same every time for the same seed. `0x` means hexadecimal; `5EED_B11D` is just a playful "seed build".
- **Lines 38–40, a unit's node:** exactly your datapack's node markers. x 0–4 gives −16, −8, 0, 8, 16; floor 0–2 gives 0, 4, 8; row 0–3 gives 3, −5, −13, −21 (the front is the south side, +z). The entrance, unit (2, 0, 0), is at `~0 ~0 ~3`, the same as the datapack's `summon marker ~ ~ ~3 {Tags:[node,entrance]}`.

A unit covers **node−4 to node+3** on x and z (8 blocks each way). Its floor level is the node's y. The node itself is a block corner, like the datapack's markers.

### The plan, in your datapack's order

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 42-67

- **Line 43:** the build's random generator. `^` (XOR) mixes the salt into the seed.
- **Lines 47–55, your `maze/reset.mcfunction`, offset for offset:** the empty shell (four 29×22×29 quadrants 1 below the center), the entrance room on its unit, the wings (`wing_open` or `wing_closed` as the layout rolled; east at `~9 ~ ~6`, west at `~-24 ~ ~6`), and the top room at `~-8 ~11 ~-8`.
- **Lines 58–59, your `maze/end.mcfunction`:** the prismarine floors between floor 0 and 1 (y+3) and between floor 1 and 2 (y+7).
- **Lines 62–67:** every room's template. The entrance was already placed, so it's left out. Sorting puts the core first, like your `place/core.mcfunction`.

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 69-102

- **Lines 69–81:** the extras inside rooms, after all templates are placed (so no template overwrites them): the core's outer walls, the sponges, and the center pillars. `default -> {}` means "do nothing for every other room type".
- **Lines 85–90, vertical openings:** `roof_open` is an 8×8 floor slab with a 2×2 water hole. It goes in the **floor of the upper unit**, 1 below its node (`~-4 ~-1 ~-4`), exactly like your datapack's `roof_open ~-3.5 ~-1 ~-3.5`. The opening to the penthouse works the same way.
- **Lines 91–95, horizontal openings:** a water doorway, including the ones into the wings.
- **Lines 98–100:** the three Elder Guardians, at your `core.mcfunction` positions: the top room, then the east and west wings. They're the very last step, so the monument is complete before they appear.

### Placing and rotating a room

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 108-139

- **Lines 109–120, the room's corner:** start at the largest possible number, then take the smallest x and z (node −4) and the lowest floor over all the room's units.
- **Lines 129–131:** rooms without an axis are placed as-is, at the room's corner.
- **Lines 133–134:** a 2-wide room's size in the world: 16 along its axis, 8 across.
- **Lines 135–137:** your 2-wide templates are saved running east–west. Along X they're placed as-is or turned 180°; along Z, turned 90° one way or the other. A coin flip picks between the two, so one-sided designs like the U-hall face both ways, just as your datapack's four direction files did.
- **Line 138:** where to put the origin so the rotated template lands exactly on the room.

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 141-152

`/place template` (and Bukkit's `place`) turn a template **around its origin corner**, so after turning it sticks out in a different direction. Seen from above, with ● as the origin:

```
NONE              CLOCKWISE_90       CLOCKWISE_180          COUNTERCLOCKWISE_90
●───────┐         ┌───●              ┌───────┐              ┌───┐
│       │         │   │              │       │              │   │
└───────┘         │   │              └───────●              │   │
                  └───┘                                     ●───┘
sticks out        sticks out         sticks out             sticks out
east + south      west + south       west + north           east + north
```

This function moves the origin so the turned template still covers exactly the room's box: for CLOCKWISE_90 (sticks out west) the origin moves to the east edge, for COUNTERCLOCKWISE_90 (sticks out north) to the south edge, for CLOCKWISE_180 both. The test `rotatedHallsMatchTheDatapackOffsets` confirms this produces exactly your `west.mcfunction` (`~11.5 ~ ~3.5 180`), `north.mcfunction` (`~-3.5 ~ ~11.5 counterclockwise_90`) and `south.mcfunction` (`~3.5 ~ ~-11.5 clockwise_90`).

### The core's walls, sponges, pillars and doorways

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 154-172

- **Your `place/core.mcfunction`:** a core against the left or right side opens into the passage between the wings.
- **Lines 159–162:** find the core's back-left unit on floor 0, the reference point your datapack used: the smallest x, and among those the largest row. `orElseThrow()` means "there must be one".
- **Lines 164–171:** at the far left or far right, replace the outer side wall and the back wall with water on the bottom floor (3 blocks high), the same fills as your datapack.

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 174-196

- **Line 179:** first clear the template's own 4 fixed sponges, by filling the room's inside (the upper two layers) with water.
- **Lines 180–188:** go through all 36 columns of the 6×6 inside. 1 in 3 stay empty; the others get sponge from the ceiling down, 2 blocks deep with a 1 in 4 chance, otherwise 1 block. That gives vanilla's ⅓ empty, ½ one sponge and ⅙ two sponges.
- **Lines 192–196:** the 2×2 pillar in the room's center (the four blocks around the node): prismarine bricks at the bottom and top, prismarine in the middle.

@code src/main/java/com/nltarzan/structures/monument/build/BuildPlanner.java 198-211

- **Your `place/horizontal_connecters.mcfunction`:** a water doorway between two neighboring units, 2 wide and 2 high, 4 blocks long through both walls.
- **Lines 203–206:** units side by side on x: x runs from `low + 2` to `high − 3` (through A's wall at node+3, B's wall at node−4, and one block of inside on each end); z covers the 2 blocks next to the node; y covers the floor and the layer above it.
- **Lines 208–210:** the same for units side by side on z. `low` and `high` don't depend on which unit comes first, so the doorway is the same from either side.

## MonumentBuilder.java: doing it in the world

@code src/main/java/com/nltarzan/structures/monument/MonumentBuilder.java 26-69

- **Line 29:** at most 15 milliseconds of building per tick. A tick is 50 ms, so the server keeps 35 ms for everything else. 1 millisecond is 1,000,000 nanoseconds; the `L` marks the number as a `long`.
- **Line 41:** returns a promise that's fulfilled when the build is complete. `MonumentResetter` waits for it before counting sponges and releasing the chunks.
- **Line 42:** `Consumer<Location>` is "something you can hand a location to": the resetter's `spawnElderGuardian`, so the Elder Guardians get your configured stats.
- **Line 43:** the steps in a queue, taken from the front one by one.
- **Line 44:** a random generator for templates with several palettes (Bukkit's `place` needs this older `Random` type).
- **Lines 48–67, a timer that runs every tick:**
  - **Line 49:** note the time at the start of this tick (`System.nanoTime()` is a precise stopwatch).
  - **Lines 51–57:** `do … while` runs the body at least once, then repeats while the condition holds. Take the next step and run it, until the 15 ms are used up or the queue is empty. Because it's "at least once", even one very big step always makes progress. In practice a monument takes a handful of ticks.
  - **Lines 58–62:** if a step crashes, stop and break the promise with that error. The check then fails and is retried a minute later. A half-built monument fixes itself: it has no Elder Guardians yet (they're spawned last), so the retry finds it "cleared" and builds a complete one.
  - **Lines 63–66:** queue empty: stop the timer and fulfil the promise.

@code src/main/java/com/nltarzan/structures/monument/MonumentBuilder.java 71-114

- **Lines 74–91, one step:** a `switch` on the kind of step, taking its contents out directly.
  - **`Place`:** take the loaded template and place it at center + offset with the rotation: entities included, no mirroring, a random palette, every block.
  - **`Fill`:** set every block in the box. `Math.min`/`Math.max` make the loops work whichever corner is given first. `setType(material, false)` turns off block updates: faster, and placed water stays exactly where it is.
  - **`SpawnElder`:** hand the exact position to the spawn method that was passed in.
- **Lines 94–96:** turns a relative offset into a real location.
- **Lines 98–114:** translate the plugin's own rotation and block names into Bukkit's. This is the only place the two meet.
