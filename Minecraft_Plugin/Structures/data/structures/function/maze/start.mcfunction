# =================== Maze generation - Start ===================
# start_generation.mcfunction
# Execute as:   Server
# Triggered:    Schedule or manually when testing
#
# Description:  Runs check to see if a new maze should be generated.
#               Set a score to indicate the state of the maze.
#               To start generated a new maze it picks a random node inside the structure.
#               It gets assigned both the start and current tag and the Step function is run.
# ===============================================================


# If there maze is already generating or the previous has not been cleared, do nothing
execute if score #maze active matches 1..2 run return fail

# ===============================================================

# Set scoreboard to indicate the maze is being generated
scoreboard players set #maze active 1

# Choose random starting Node and assign it the current tag
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run tag @e[tag=node,distance=..30,sort=random,limit=1] add start
execute positioned as @n[tag=ResettingStructure,tag=OceanMonument] run tag @e[tag=node,tag=start] add current

# Start generating the maze
execute if score #generating test matches 1 run tellraw @a "Generating maze. This may take a moment..."
execute as @e[tag=node,tag=current] run function structures:maze/step
