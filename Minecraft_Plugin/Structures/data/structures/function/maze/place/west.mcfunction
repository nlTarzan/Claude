# ======================== West Hallways ========================
# west.mcfunction
# Execute as:   Node with tag prev_w
# Triggered:    By horizontal.mcfunction
#
# Description:  Run as unused node to the West of active node to check for placement.
#               Place structure and mark the relevant nodes as used.         
#               Horizontal hallways are stored with their long side going East-West
#               When placing these they need to be rotated correctly
# ===============================================================


execute if score #hallway random matches 1 at @s positioned ~-8 ~ ~ as @e[tag=node,tag=!used,distance=..2] run place template structures:monument/maze/room/2x1x1_lantern ~11.5 ~ ~3.5 180
execute if score #hallway random matches 1 at @s positioned ~-8.5 ~-0.5 ~-0.5 as @e[tag=node,tag=!used,distance=..2] run tag @e[tag=node,dx=9,dy=1,dz=1] add long
execute if score #hallway random matches 1 at @s positioned ~-8.5 ~-0.5 ~-0.5 as @e[tag=node,tag=!used,distance=..2] run tag @e[tag=node,dx=9,dy=1,dz=1] add used
execute if score #hallway random matches 2 at @s positioned ~-8 ~ ~ as @e[tag=node,tag=!used,distance=..2] run place template structures:monument/maze/room/2x1x1_u ~11.5 ~ ~3.5 180
execute if score #hallway random matches 2 at @s positioned ~-8.5 ~-0.5 ~-0.5 as @e[tag=node,tag=!used,distance=..2] run tag @e[tag=node,dx=9,dy=1,dz=1] add long
execute if score #hallway random matches 2 at @s positioned ~-8.5 ~-0.5 ~-0.5 as @e[tag=node,tag=!used,distance=..2] run tag @e[tag=node,dx=9,dy=1,dz=1] add used

# Testing feedback
execute if score #hallway random matches 1 at @s positioned ~-8 ~ ~ as @e[tag=node,tag=!used,distance=..2] run tellraw @a "West Lantern Hallway placed."
execute if score #hallway random matches 2 at @s positioned ~-8 ~ ~ as @e[tag=node,tag=!used,distance=..2] run tellraw @a "West U-shape Hallway placed."
