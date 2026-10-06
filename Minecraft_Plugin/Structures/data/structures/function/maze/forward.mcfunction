# ================== Maze generation - Forward ==================
# forward.mcfunction
# Execute as random unvisited neighbor to the node with tag "current"
# Triggered:    By maze_step
#
# Description:  Executing node is a randomly selected unvisited neighbor to the current node.
#               Removes step tag from all nodes.
#               It checks the direction of the previous node (with tag=current) and stores this as a tag.
#               Updates the current node to itself
#               Executes maze_step.mcfunction as the "current" node to continue checking for possible paths.
# ===============================================================

# Testing feedback
execute if score #generating test matches 1 run tellraw @a "Maze Generating... Moving forward."

# Checks to see state of maze generation
execute if score #maze active matches 0 run return fail
execute if score #maze active matches 2 run return fail

# ===============================================================

# Remove all step nodes
tag @e remove step

# Store the direction of the "current" node from @s; i.e. if "current" is 6 blocks north of @s, add tag "prev_n"
execute at @s positioned ~8 ~ ~ if entity @e[tag=node,tag=current,distance=..2] run tag @s add prev_e
execute at @s positioned ~ ~ ~8 if entity @e[tag=node,tag=current,distance=..2] run tag @s add prev_s
execute at @s positioned ~-8 ~ ~ if entity @e[tag=node,tag=current,distance=..2] run tag @s add prev_w
execute at @s positioned ~ ~ ~-8 if entity @e[tag=node,tag=current,distance=..2] run tag @s add prev_n
execute at @s positioned ~ ~4 ~ if entity @e[tag=node,tag=current,distance=..2] run tag @s add prev_u
execute at @s positioned ~ ~-4 ~ if entity @e[tag=node,tag=current,distance=..2] run tag @s add prev_d

# Testing feedback direction
execute if score #generating test matches 1 at @s positioned ~8 ~ ~ if entity @e[tag=node,tag=current,distance=..2] run tellraw @a West
execute if score #generating test matches 1 at @s positioned ~ ~ ~8 if entity @e[tag=node,tag=current,distance=..2] run tellraw @a North
execute if score #generating test matches 1 at @s positioned ~-8 ~ ~ if entity @e[tag=node,tag=current,distance=..2] run tellraw @a East
execute if score #generating test matches 1 at @s positioned ~ ~ ~-8 if entity @e[tag=node,tag=current,distance=..2] run tellraw @a South
execute if score #generating test matches 1 at @s positioned ~ ~4 ~ if entity @e[tag=node,tag=current,distance=..2] run tellraw @a Down
execute if score #generating test matches 1 at @s positioned ~ ~-4 ~ if entity @e[tag=node,tag=current,distance=..2] run tellraw @a Up

# Update current node
execute as @e[tag=node,tag=current] run tag @s remove current
tag @s add current

# Continue to the next step
execute if score #generating test matches 1 as @e[tag=node,tag=current] run tellraw @a "Moving forward - Time to take a step."
execute as @e[tag=node,tag=current] run function structures:maze/step
