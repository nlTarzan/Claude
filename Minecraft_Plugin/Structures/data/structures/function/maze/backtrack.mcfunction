# ================= Maze generation - Backtrack =================
# backtrack.mcfunction
# Execute as:   Node with tag "current"
# Triggered:    By maze_step
#
# Description:  Remove "current" tag from itself
#               Check the nodes around it for the previous node and make that one current.
#               Executes maze_step.mcfunction as the "current" node to continue checking for possible paths.
# ===============================================================


# Testing feedback
execute if score #generating test matches 1 run tellraw @a "Maze Generating... Backtracking."
execute if score #generating test matches 1 as @s[tag=start] run tellraw @a "I'm back at start"

# Checks to see state of maze generation
execute if score #maze active matches 0 run return fail
execute if score #maze active matches 2 run return fail

# ===============================================================
# Update the current node
tag @s remove current
execute at @s[tag=prev_e] positioned ~8 ~ ~ as @e[tag=node,distance=..2] run tag @s add current
execute at @s[tag=prev_s] positioned ~ ~ ~8 as @e[tag=node,distance=..2] run tag @s add current
execute at @s[tag=prev_w] positioned ~-8 ~ ~ as @e[tag=node,distance=..2] run tag @s add current
execute at @s[tag=prev_n] positioned ~ ~ ~-8 as @e[tag=node,distance=..2] run tag @s add current
execute at @s[tag=prev_u] positioned ~ ~4 ~ as @e[tag=node,distance=..2] run tag @s add current
execute at @s[tag=prev_d] positioned ~ ~-4 ~ as @e[tag=node,distance=..2] run tag @s add current

# Testing only
execute if score #generating test matches 1 at @s[tag=prev_e] positioned ~8 ~ ~ as @e[tag=node,distance=..2] run tellraw @a "Back East"
execute if score #generating test matches 1 at @s[tag=prev_s] positioned ~ ~ ~8 as @e[tag=node,distance=..2] run tellraw @a "Back South"
execute if score #generating test matches 1 at @s[tag=prev_w] positioned ~-8 ~ ~ as @e[tag=node,distance=..2] run tellraw @a "Back West"
execute if score #generating test matches 1 at @s[tag=prev_n] positioned ~ ~ ~-8 as @e[tag=node,distance=..2] run tellraw @a "Back North"
execute if score #generating test matches 1 at @s[tag=prev_u] positioned ~ ~4 ~ as @e[tag=node,distance=..2] run tellraw @a "Back Up"
execute if score #generating test matches 1 at @s[tag=prev_d] positioned ~ ~-4 ~ as @e[tag=node,distance=..2] run tellraw @a "Back Down"

# Continue to the next step
execute if score #generating test matches 1 as @e[tag=node,tag=current] run tellraw @a "Backtracking - Try taking another step."
execute as @e[tag=node,tag=current] run function structures:maze/step
