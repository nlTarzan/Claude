
# =================== Maze generation - Reset ===================
# reset.mcfunction
# Execute as:   Server
# Triggered:    Schedule or manually when testing
#
# Description:  A central armor stand is placed to determine where to place the rest.
#               Places an empty monument with Penthouse and Entrance.
#               Wings are randomized, included their pathways.
#               The rest of the Markers are summoned as nodes for the maze.
# ===============================================================

# A Marker is manually placed to indicate where the maze should be generated. This is used to determine where to place the rest of the structure.
# Use the following command at the location where you want the maze to be generated. Y level should be the same as the Y coordinate of the bottom floor of the monument.
# This marker should be at the "perfect" center of the monument. So X and Y coordinates should have .0 at the end. The marker should be on a corner where 4 blocks meet since the Monument has a 2x2 block center.
# /summon marker 224.0 40.0 -320.0 {Tags:["ResettingStructure","OceanMonument"]}


# These scoreboards turn the testing mode on/off
# scoreboard players set #generating test 0
# scoreboard players set #generating_full test 0
# scoreboard players set #kill test 0

# ===============================================================

# Place empty Ocean Monument
# Position is set to the center of the Monument on the bottom floor
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/monument_empty_nw ~-29 ~-1 ~-29
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/monument_empty_ne ~ ~-1 ~-29
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/monument_empty_sw ~-29 ~-1 ~
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/monument_empty_se ~ ~-1 ~

# Remove any leftover nodes from previous maze generation
execute positioned as @e[tag=ResettingStructure,tag=OceanMonument] run kill @e[tag=node,distance=..40]

# Place ground floor maze Markers
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~ ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~ ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~ ~3 {Tags:[node,entrance,used], data:{name:"Entrance"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~ ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~ ~3 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~ ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~ ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~ ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~ ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~ ~-5 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~ ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~ ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~ ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~ ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~ ~-13 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~ ~-21 {Tags:[node,core], data:{name:"Core"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~ ~-21 {Tags:[node,core], data:{name:"Core"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~ ~-21 {Tags:[node,core], data:{name:"Core"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~ ~-21 {Tags:[node,core], data:{name:"Core"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~ ~-21 {Tags:[node], data:{name:"Node"}}

# Place 1st floor Markers
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~4 ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~4 ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~4 ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~4 ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~4 ~3 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~4 ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~4 ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~4 ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~4 ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~4 ~-5 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~4 ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~4 ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~4 ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~4 ~-13 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~4 ~-13 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~4 ~-21 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~4 ~-21 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~4 ~-21 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~4 ~-21 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~4 ~-21 {Tags:[node], data:{name:"Node"}}

# Place 2nd floor Markers
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~8 ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~8 ~3 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~8 ~3 {Tags:[node], data:{name:"Node"}}

execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-8 ~8 ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~8 ~-5 {Tags:[node], data:{name:"Node"}}
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~8 ~8 ~-5 {Tags:[node], data:{name:"Node"}}

# ==================================================
# Place entrance
execute at @n[tag=entrance] run place template structures:monument/maze/room/1x1x1_start ~-3.5 ~ ~-3.5

# ==================================================
# Place Wings
# East Wing
execute store result score #wing random run random value 1..6
execute if score #wing random matches 1..3 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/room/wing_open ~9 ~ ~6
execute if score #wing random matches 1..2 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~ ~11 {Tags:[node,wing,used], data:{name:"Wing"}}
execute if score #wing random matches 2..3 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~4 ~11 {Tags:[node,wing,used], data:{name:"Wing"}}
execute if score #wing random matches 4..6 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/room/wing_closed ~9 ~ ~6
execute if score #wing random matches 4..6 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~16 ~4 ~11 {Tags:[node,wing,used], data:{name:"Wing"}}
# West Wing
execute store result score #wing random run random value 1..6
execute if score #wing random matches 4..6 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/room/wing_open ~-24 ~ ~6
execute if score #wing random matches 4..5 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~ ~11 {Tags:[node,wing,used], data:{name:"Wing"}}
execute if score #wing random matches 5..6 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~4 ~11 {Tags:[node,wing,used], data:{name:"Wing"}}
execute if score #wing random matches 1..3 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/room/wing_closed ~-24 ~ ~6
execute if score #wing random matches 1..3 positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~-16 ~4 ~11 {Tags:[node,wing,used], data:{name:"Wing"}}

# ==================================================
# Place Top Room
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run place template structures:monument/maze/room/penthouse ~-8 ~11 ~-8
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run summon marker ~ ~12 ~3 {Tags:[node,penthouse,used], data:{name:"Penthouse"}}

# Set scoreboard to indicate a new maze can be generated
execute if score #generating test matches 1 run tellraw @a "Maze has been reset. Ready for generation."
scoreboard players set #maze active 0

# ==============================================================
# Notes
# The structure file maze_nodes.nbt has been made obsolete. The maze nodes are now generated by this function.