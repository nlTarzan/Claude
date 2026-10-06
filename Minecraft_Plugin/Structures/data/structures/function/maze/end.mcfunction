# ======================= End Generation ========================
# end_generation.mcfunction
# Execute as:   Node with tag "current"
# Triggered:    By maze_step
#
# Description:  Remove storage to indicate maze has been generated.
#               During testing place Emeralds blocks to show the path
#               Place floors and trigger the placement of rooms.
# ===============================================================


# Checks to see state of maze generation
execute if score #maze active matches 0 run return fail
execute if score #maze active matches 2 run return fail

# ===============================================================

# Set scoreboard to indicate generation is complete
scoreboard players set #maze active 2

# Print message
execute if score #generating test matches 1 run tellraw @a "Maze Generated. Ready to place rooms."

#==================================================
# Place Floors
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run fill ~-22 ~3 ~-25 ~21 ~3 ~6 prismarine
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run fill ~-15 ~7 ~-15 ~14 ~7 ~6 prismarine

# Depending on orientation, this might need to lead to different functions.
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] as @e[tag=node,tag=core,sort=random,limit=1] unless score #generating test matches 1 run function structures:maze/place/core

