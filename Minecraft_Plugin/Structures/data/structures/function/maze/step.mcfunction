# =================== Maze generation - Step ====================
# step.mcfunction
# Execute as:   Node with tag "current"
# Triggered:    By other functions (maze_start, maze_forward, maze_backtrack)
#
# Description:  Tags the current node as visited.
#               Create 3 boxes for each axes and tags them with "step" as possible options.
#               Chooses 1 nodes with the tag "step" and runs maze_forward.mcfunction.
#               When no nodes are available options proceed to backtrack along the path.
#               If no nodes are available and the current node has the tag "start",
#               end generation by running maze_end_generation.mcfunction.
# ===============================================================


# Checks to see state of maze generation
execute if score #maze active matches 0 run return fail
execute if score #maze active matches 2 run return fail

# ===============================================================

# tag "current" node with "visited" if it is not already tagged
tag @e[tag=current] add visited

# Tag neighbors that do not have the "visited" tag and give them the "step" tag
execute at @e[tag=current] positioned ~-8.5 ~-0.5 ~-0.5 run tag @e[tag=node,tag=!visited,dx=17,dy=1,dz=1] add step
execute at @e[tag=current] positioned ~-0.5 ~-0.5 ~-8.5 run tag @e[tag=node,tag=!visited,dx=1,dy=1,dz=17] add step
execute at @e[tag=current,tag=!entrance] positioned ~-0.5 ~-4.5 ~-0.5 run tag @e[tag=node,tag=!visited,dx=1,dy=9,dz=1,tag=!entrance] add step

# Select one neighbor with the "step" tag and move to that as as the current node.
execute if score #generating test matches 1 at @e[tag=current] as @e[tag=node,tag=!visited,tag=step,sort=random,limit=1] run tellraw @a "I can take a step forward"
execute at @e[tag=current] as @e[tag=node,tag=!visited,tag=step,sort=random,limit=1] run return run function structures:maze/forward

# if there are no unvisited neighbors and the current node is not the starting node, backtrack to the current node's previous node
execute if score #generating test matches 1 at @e[tag=current,tag=!start] unless entity @e[tag=node,tag=step,distance=3..8] run tellraw @a "I need to backtrack"
execute as @e[tag=current,tag=!start] at @e[tag=current,tag=!start] unless entity @e[tag=node,tag=!visited,tag=step,distance=3..8] run return run function structures:maze/backtrack

# if there are no unvisited neighbors and the current node is the starting node, end generation.
execute if score #generating test matches 1 at @e[tag=current,tag=start] unless entity @e[tag=node,tag=step,distance=3..8] run tellraw @a "Maze is generated, time to place rooms"
execute as @e[tag=current,tag=start] at @e[tag=current,tag=start] unless entity @e[tag=node,tag=!visited,tag=step,distance=3..8] run return run function structures:maze/end
