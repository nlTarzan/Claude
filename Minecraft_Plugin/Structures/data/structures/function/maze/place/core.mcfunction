# ======================= Core Placement ========================
# maze_placement_core.mcfunction
# Execute as:   Node with tag "core"
# Triggered:    By maze_end_generation
#
# Description:  Place Core structure and mark the nodes it occupies as used.
#               If Core is in one of the corners, remove the lower walls in the corners.
#               Place Entrance structure and mark it's node as used.
#               Place 2 randomized Wings and the Top room structures.
#               If possible place larger room structures and mark it's nodes as used.
#               If a node only has a "prev_u" neighbor and is an endpoint, then place a Treasure Room.
#               Fill the remaining unused nodes with randomized small rooms
#
# Notes:        Currently designed for North facing Ocean Monument.
#               You move North when entering the Ocean Monument.
# ===============================================================


# ==================================================
# Place Core
execute at @s run place template structures:monument/maze/room/2x2x2_core ~-3.5 ~ ~-3.5
execute at @s positioned ~-0.5 ~-0.5 ~-0.5 run tag @e[tag=node,dx=9,dy=5,dz=9] add big
execute at @s positioned ~-0.5 ~-0.5 ~-0.5 run tag @e[tag=node,dx=9,dy=5,dz=9] add used

# If in the North West corner, remove outer walls on the bottom floor
execute at @s unless entity @e[tag=node,dx=-8,distance=7..8] run fill ~-3.5 ~ ~-3.5 ~-3.5 ~2 ~8.5 water
execute at @s unless entity @e[tag=node,dx=-8,distance=7..8] run fill ~-3.5 ~ ~-3.5 ~8.5 ~2 ~-3.5 water

# If in the North East corner, remove outer walls on the bottom floor
execute at @s unless entity @e[tag=node,dx=16,distance=15..16] run fill ~11.5 ~ ~-3.5 ~11.5 ~2 ~8.5 water
execute at @s unless entity @e[tag=node,dx=16,distance=15..16] run fill ~-0.5 ~ ~-3.5 ~11.5 ~2 ~-3.5 water

# ==================================================
# Place Treasure room
scoreboard players set #treasure count 10
execute as @e[tag=prev_d,tag=!used,sort=random,limit=1] run function structures:maze/place/treasure

# ==================================================
# Place large rooms
#execute at @e[tag=node,tag=prev_u] if entity @n[tag=node,tag=prev_n,dy=4,distance=3..4] if entity @n[tag=node,tag=prev_s,dz=8,distance=7..8]

# Place 2x1 vertical rooms
execute store result score #vertical random run random value 0..6
execute unless score #vertical random matches 0 run function structures:maze/place/vertical

# Place 2x1 horizontal rooms
execute store result score #horizontal random run random value 4..16
execute unless score #horizontal random matches 0 run function structures:maze/place/horizontal

# ==================================================
# Place vertical connectors
execute at @e[tag=prev_d,tag=!tall,tag=!big] run place template structures:monument/maze/roof_open ~-3.5 ~-1 ~-3.5
execute at @e[tag=prev_u,tag=!tall,tag=!big] run place template structures:monument/maze/roof_open ~-3.5 ~3 ~-3.5

# ==================================================
# Fill remaining rooms with random small rooms
execute as @e[tag=!used,sort=random,limit=1] run function structures:maze/place/small

# ==================================================
# Place horizontal connecters
execute as @e[tag=long] run function structures:maze/place/horizontal_connecters

# Summon Elder Guardians
execute positioned as @e[tag=ResettingStructure,tag=OceanMonument] run summon minecraft:elder_guardian ~ ~12 ~
execute positioned as @e[tag=ResettingStructure,tag=OceanMonument] run summon minecraft:elder_guardian ~16 ~4 ~11
execute positioned as @e[tag=ResettingStructure,tag=OceanMonument] run summon minecraft:elder_guardian ~-16 ~4 ~11

# ==================================================
# Kill all Marker nodes
execute unless score #generating test matches 1 positioned as @e[tag=ResettingStructure,tag=OceanMonument] run kill @e[tag=node,distance=..40]