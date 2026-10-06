# ===================== Horizontal hallways======================
# small.mcfunction
# Execute as:   Node without used tag
# Triggered:    By core.mcfunction & itself)
#
# Description:  
#               
#               
#               
# ===============================================================

execute store result score #small random run random value 1..3

# Depending on scoreboard attempt to spawn a hallway in a cardinal direction
execute if score #small random matches 1 at @s run place template structures:monument/maze/room/1x1x1_dark ~-3.5 ~ ~-3.5
execute if score #small random matches 2 at @s run place template structures:monument/maze/room/1x1x1_lanterns ~-3.5 ~ ~-3.5
execute if score #small random matches 3 at @s run place template structures:monument/maze/room/1x1x1_pillars ~-3.5 ~ ~-3.5

# Feedback for testing
execute if score #small random matches 1 as @s run tellraw @a "Placed Dark Room"
execute if score #small random matches 2 as @s run tellraw @a "Placed Lantern Room"
execute if score #small random matches 3 as @s run tellraw @a "Placed Pillar Room"

# Mark rooms as small and used
tag @s add small
tag @s add used

# Run itself again if more nodes exist without the used tag
execute as @e[tag=node,tag=!used,sort=random,limit=1] run function structures:maze/place/small