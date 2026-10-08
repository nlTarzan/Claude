# Step 5: the maze generator

These files only work out a **layout**: which walls are open and which room goes where. They never touch the world, which is why they can be tested so thoroughly (thousands of layouts per test run). Step 6 turns a layout into blocks.

## The big picture

The grid, seen from above, entrance at the bottom:

```
Floors 0 and 1 (5 x 4 units)        Floor 2 (3 x 2 units)
row 3  [0][1][2][3][4]   ← back
row 2  [0][1][2][3][4]
row 1  [0][1][2][3][4]               row 1     [1][2][3]
row 0  [0][1][2][3][4]   ← front     row 0     [1][2][3]
              ↑ entrance (x 2, row 0, floor 0)
```

- **Units:** each `[ ]` is one 8×8 unit, your datapack's "node". That's 20 + 20 + 6 = **46 units**.
- **Dummy units:** stand-ins that join the maze but get no room. The **wings** sit in front of the front corners (row −1 at x 0 and x 4 on floor 1, and also floor 0 for an open wing). The **penthouse** sits above the top floor's front center (floor 3).

How a layout is made (`MonumentLayout.generate`):

```
1. draw this monument's newest chance from the bell curve          MazeSettings
2. roll the wings (open/closed) and the core's position            MonumentLayout
3. close every wall, then carve a maze by opening walls            GrowingTreeGenerator
4. give every unit a room: core, entrance, big, hall, tall, ...    RoomFitter
5. too few sponge rooms? rewire a unit into one, fit rooms again   SpongeRooms
```

## GridDirection.java: the six directions

@code src/main/java/com/nltarzan/structures/monument/layout/GridDirection.java 9-50

- **Lines 9–15:** an `enum` whose choices carry data. Each direction stores how a step that way changes x, the floor and the row. `BACK` means one row further from the entrance: north in your datapack.
- **Line 17:** the four sideways directions.
- **Lines 19–39:** where the three numbers are stored (an enum constructor runs once per choice, when Java loads the enum), plus getters.
- **Lines 41–50:** the other side of a wall: opening EAST from one unit is the same wall as WEST from its neighbor.

## Cell.java: one unit

@code src/main/java/com/nltarzan/structures/monument/layout/Cell.java 8-33

- **Lines 10–15:** what a cell is: a real unit, or a dummy for a wing or the penthouse.
- **Line 17:** a number from 0 upward, unique within the grid. Other code uses it as an index into arrays like "visited" (`visited[cell.id()]`), which is very fast.
- **Lines 18–21:** its position and kind.
- **Lines 23–25:** three arrays with one slot per direction (6), in the enum's order. `direction.ordinal()` is a direction's position in that order (EAST = 0 … DOWN = 5).
  - `neighbors`: the cell next to it in each direction, or `null` at the edge.
  - `open`: whether the wall that way is open.
  - `blocked`: walls that may never open. Only one is blocked: the entrance's ceiling, as in your datapack.

  These have no `private` or `public`: that's **package-private**. Only classes in the same package (`layout`) can change them directly, which forces everyone else to change walls through `MonumentGrid.setOpen`, which always updates both sides together.
- **Line 27:** also package-private, so only `MonumentGrid` can create cells.

@code src/main/java/com/nltarzan/structures/monument/layout/Cell.java 58-83

- **Lines 58–78:** small questions: is it a dummy, who's next to it, is that wall open, and how many open walls it has. A cell with one open wall is a **dead end**.
- **Lines 80–83:** how a cell looks in error messages, e.g. `(2,0,0)` or `PENTHOUSE`.

(The comment at the top of this file, line 4, still says "three dummy units"; since open wings were added there can be up to five.)

## MonumentGrid.java: the grid and its walls

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentGrid.java 21-39

- **Lines 23–25:** the grid's size.
- **Lines 28–32:** a wall, described as "this cell's wall in that direction". `other()` is the cell on the far side.
- **Lines 34–39:** all cells, the 46 real ones only, a lookup from position to cell, the entrance, and the wing designs.

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentGrid.java 46-86

- **Lines 49–57:** create the 46 real units, skipping positions that don't exist (the top floor is smaller).
- **Lines 60–68:** the dummies. Each wing has a unit in front of a front corner on floor 1; an **open** wing gets a second unit under it, on floor 0, so it can connect on both floors (your datapack's wing nodes at `~` and `~4`). The penthouse sits above the top floor's middle front unit. These positions are chosen so each dummy only touches its own unit.
- **Lines 70–74:** for every cell and every direction, look up the neighbor by position. A missing one becomes `null`.
- **Lines 76–78:** remember the entrance, and permanently block its ceiling.
- **Lines 81–86:** which positions are real units: all 5×4 on floors 0 and 1, and x 1–3, rows 0–1 on floor 2. `||` means "or", `&&` means "and".

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentGrid.java 112-158

- **Line 113:** the cell at a position, or `null`.
- **Lines 118–120:** a wall can ever open if there's a neighbor and it isn't blocked.
- **Lines 123–134:** **the only way walls change.** Line 131 sets this side and line 132 the neighbor's side (the opposite direction), so both always agree.
- **Lines 137–145:** open or close everything at once. The maze generator starts with "all closed".
- **Lines 148–158:** every wall exactly once. It only looks EAST, BACK and UP from each cell: the WEST, FRONT and DOWN walls are the same walls seen from the other side.

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentGrid.java 160-199

- **Lines 160–179:** "can everything still be reached from the entrance?" The tests use this on every layout, and so does the sponge guarantee.
- **Lines 181–199:** how many steps each unit is from the entrance, walking through open walls. This is a **breadth-first search**, like water spreading out from the entrance one step at a time:
  - **Line 184:** everything starts at "not reached" (`-1`).
  - **Lines 186–187:** the entrance is 0 steps. Put it in a **queue**, a waiting line: first in, first out.
  - **Lines 188–197:** take the next cell from the queue. Every neighbor through an open wall that hasn't been reached gets "one step further", and joins the queue.

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentGrid.java 219-237

- **Lines 219–226:** close a wall and mark it blocked on both sides.
- **Lines 228–233:** create a cell, using the current number of cells as its ID (0, 1, 2…).
- **Lines 235–237:** turns three small numbers into one number, to use as a map key. `+ 16` keeps them positive (the wings are at row −1); `<<` shifts each into its own part of the big number and `|` combines them. Think of writing a date as 20261006: three numbers in one.

## MazeSettings.java: the maze settings and the bell curve

@code src/main/java/com/nltarzan/structures/monument/layout/MazeSettings.java 18-56

- **Line 18:** the settings from `maze:` in `monuments.yml`.
- **Line 20:** the defaults.
- **Lines 24–37:** checks every time one is created; impossible values are an error.
- **Lines 40–46:** copies with one thing changed (a record can't be changed, so you make a new one). Line 40 makes a fixed newest chance (the curve squeezed to one value), used by `/nltstructures monument layout <seed> 0.9` and the tests.
- **Lines 49–56, drawing one monument's newest chance:**
  - **Line 51:** `nextGaussian()` gives a random number on a standard bell curve: usually near 0, about 2 in 3 between −1 and +1. Multiplying by the spread and adding the peak moves the curve to 0.65 ± 0.15.
  - **Lines 52–54:** outside min..max? Draw again, up to 100 times.
  - **Line 55:** in the extremely unlikely case all 100 draws missed, force the value inside. `Math.clamp(v, min, max)` means "v, but never below min or above max".

## GrowingTreeGenerator.java: carving the maze

@code src/main/java/com/nltarzan/structures/monument/layout/GrowingTreeGenerator.java 25-57

- **Line 26:** start with every wall closed.
- **Lines 27–28:** `visited` marks the cells already in the maze; `active` lists the cells in the maze that might still have unvisited neighbors.
- **Lines 30–33:** begin at a random real unit.
- **Line 36:** keep going until no active cells are left; by then every cell, dummies included, is in the maze.
- **Line 37, the one line that gives the maze its character:** `nextDouble()` is a random number from 0 to 1. Below the newest chance, continue from the **newest** cell, which makes long corridors; otherwise from a **random** cell in the list, which makes branches.
- **Lines 40–45:** which directions lead to a cell that isn't in the maze yet?
- **Lines 46–49:** none, so this cell is finished; take it off the list. `continue` jumps back to the start of the loop.
- **Lines 51–55:** pick one of those directions at random, open that wall, and add the new cell to the maze and the list.

Because each new cell is reached by exactly one opened wall, the result is a perfect maze: exactly one route between any two units.

## RoomType.java and Room.java: rooms

@code src/main/java/com/nltarzan/structures/monument/layout/RoomType.java 8-27

- **Every room type,** with its letter for the console map and its template file. The comments give the vanilla name (DXY, DYZ, DX, DZ, DY). Adding a room design means adding a line here; `StructureLibrary` then loads it automatically.

@code src/main/java/com/nltarzan/structures/monument/layout/Room.java 12-20

- **One placed room:** its type, the units it covers, the axis (EAST = along X, BACK = along Z, for 2-wide rooms; otherwise `null`), and whether a small room gets the 2×2 center pillar.
- **Line 15:** stores an unchangeable copy of the list.
- **Lines 18–20:** the **anchor**, the first unit, which is always the lowest-x/floor/row one. The builder measures from there.

## RoomFitter.java: giving every unit a room

@code src/main/java/com/nltarzan/structures/monument/layout/RoomFitter.java 16-66

- **Line 16:** the two directions a 2-wide room can run.
- **Line 20:** which units already have a room. The dummies start as "claimed" (lines 27–29), because they never get one.
- **Lines 39–43:** first the fixed rooms: the **core** (8 units), the **entrance**, and any sponge rooms the guarantee asked for (claimed first so a tall room from below can't take them).
- **Lines 45–51:** go through all units in a **random order** (`shuffle`) and give every unit that's still free the first room that fits. This is vanilla's method.
- **Lines 56–66:** the core's 8 units: 2 wide, 2 deep, 2 high, starting at the anchor.

@code src/main/java/com/nltarzan/structures/monument/layout/RoomFitter.java 68-108

Vanilla's priority list, tried in this order. Each `return` means "done, skip the rest".

- **Line 69:** the unit above, if the wall to it is open and it's free.
- **Lines 72–79, the 2×2 room (pipes or dark):** for each axis, in random order: the unit beside it, the unit above, and the unit above the side one must all be free and connected by open walls. Then the 4 units become one big room, 50/50 pipes or dark.
- **Lines 81–87, the 2-wide hall (U or lantern):** a free, open neighbor along either axis.
- **Lines 89–92, the tall room:** the unit above is free and open.
- **Lines 94–97, the sponge room:** only the floor is open.
- **Lines 100–107, a small room:** one of three designs, equally likely. Pillars and dark rooms get the center pillar with a 50% chance, but only when the floor and ceiling are closed and at least two walls are open, as vanilla does. `random.nextBoolean()` is a coin flip.

@code src/main/java/com/nltarzan/structures/monument/layout/RoomFitter.java 110-138

- **Lines 111–114, the key rule:** a room may only span units that the maze connected **with an open wall** and that are still free. That's why rooms never cut through the maze.
- **Lines 125–127:** the two axes in random order, so X and Z are equally likely.
- **Lines 129–131:** a coin flip between two designs.
- **Lines 133–138:** mark a room's units as taken, and add it to the list.

## SpongeRooms.java: guaranteeing a sponge room

@code src/main/java/com/nltarzan/structures/monument/layout/SpongeRooms.java 32-54

- **Lines 35–41, candidates:** units with a unit below them (so a floor opening is possible), that aren't the core, the entrance, an existing sponge spot, or directly above one.
- **Lines 43–44, sorting:** first dead ends (`openCount() != 1` sorts the others last, because `false` sorts before `true`), then the furthest from the entrance (`reversed()`).
- **Lines 46–52, two rounds:** round 1 only accepts units that can be rewired without breaking anything; round 2 also allows repairs. It returns the first unit that works, or `null`.

@code src/main/java/com/nltarzan/structures/monument/layout/SpongeRooms.java 56-81

- **Lines 58–65:** remember the unit's walls, then close all of them except the floor, and open the floor.
- **Line 68:** does everything still reach the entrance? Then it worked. If not, and repairs are allowed, try to reconnect.
- **Lines 72–80:** it didn't work, so put everything back exactly as it was.

@code src/main/java/com/nltarzan/structures/monument/layout/SpongeRooms.java 87-111

- **The repair:** closing a unit in the middle of the maze cuts part of the maze off; this reconnects it.
- **Line 89:** `while (true)` repeats until one of the `return`s is reached.
- **Lines 90–93:** everything reachable again? Done.
- **Lines 94–103:** collect closed walls with the connected part on one side and a cut-off part on the other. Walls touching a sponge spot don't count, because those must keep only their floor open.
- **Lines 104–106:** no such wall: give up.
- **Lines 107–109:** open one at random, remember it (so it can be undone), and check again.

## MonumentLayout.java: putting it all together

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentLayout.java 20-65

- **Lines 20–24:** a finished layout: its seed, newest chance, grid, rooms, and a quick lookup from unit to room.
- **Lines 31–36:** fill that lookup. Dummy cells have no room, so they stay `null`. The constructor is `private`, so a layout can only be made through `generate`.
- **Line 41:** one random generator, created from the seed, is used for *every* random choice. Same seed and same settings means exactly the same layout, which is what makes `reset generated <seed>` and `layout current` possible. `SplittableRandom` mixes nearby seeds properly (the older `java.util.Random` gave almost identical first values for seeds 0, 1, 2…).
- **Line 42:** this monument's newest chance.
- **Line 45:** each wing open or closed, 50/50.
- **Line 47:** the core at a random x from 0 to 3 (it's 2 wide), in rows 2–3, on floors 0–1.
- **Lines 49–50:** carve the maze, then fit the rooms.
- **Lines 53–63:** while there are too few sponge rooms, find a unit to rewire and fit all rooms again with that sponge spot reserved. A `Set` is a collection without duplicates; `LinkedHashSet` also keeps the order, so the result stays identical for the same seed.

@code src/main/java/com/nltarzan/structures/monument/layout/MonumentLayout.java 93-105

- **The walls the builder must actually open in the world:** every open wall **between two different rooms**, including the links to the wings and the penthouse. An open wall *inside* a room needs nothing, because the room template is already one open space.

## LayoutPrinter.java: the console map

@code src/main/java/com/nltarzan/structures/monument/layout/LayoutPrinter.java 20-39

- **Line 22:** the heading with the seed, newest chance and wing designs.
- **Lines 26–33:** floors from top to bottom, and rows from back to front, so the entrance ends up at the bottom of each floor. Each row gives two lines: the wall line above it and the room line.
- **Lines 34–37:** room counts and the legend.

@code src/main/java/com/nltarzan/structures/monument/layout/LayoutPrinter.java 48-85

- **Lines 49–56, a wall line** like `+---+   +---+`: a corner, then `---` for a closed wall or three spaces for an opening or a room's inside.
- **Lines 58–69, a room line** like `| C   C | Uv  U |`: `|` or a space between neighbors, then each unit's 3-character label.
- **Lines 71–77, one unit's label:** a space, the room letter, then `^` (opening up), `v` (down), `*` (both), or `#` for a small room with the center pillar.
- **Lines 80–85:** no line is drawn between two units of the same room, or where the maze opened the wall.

@code src/main/java/com/nltarzan/structures/monument/layout/LayoutPrinter.java 117-125

- **The room count:** an `EnumMap` is a map whose keys are enum values, kept in the enum's order. `merge(type, 1, Integer::sum)` means "add 1 to this type's count, starting from 0". `Collectors.joining(", ", "Rooms: ", "")` glues the parts together with commas, starting with `Rooms: `.
