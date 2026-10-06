# ===================== Horizontal hallways======================
# horizontal.mcfunction
# Execute as:   Server
# Triggered:    By core.mcfunction
#
# Description:  There are 2 options for horizontal hallways.
#               We choose a random number for which to attempt to spawn.
#               Pick a single node one of the horizontal type tags and is still unused and try to place a hallway.
#               Repeat this process till the scoreboard reaches 0
# ===============================================================

execute if score #horizontal random matches 0 run return fail

execute if score #generating test matches 1 run tellraw @a [{"score":{"name":"#horizontal","objective":"random"}},{"text":" horizontals to be placed."}]

execute store result score #hallway random run random value 1..2
execute store result score #direction random run random value 1..4

# Depending on scoreboard attempt to spawn a hallway in a cardinal direction
execute if score #direction random matches 1 as @e[tag=prev_n,tag=!used,sort=random,limit=1] run function structures:maze/place/north
execute if score #direction random matches 2 as @e[tag=prev_e,tag=!used,sort=random,limit=1] run function structures:maze/place/east
execute if score #direction random matches 3 as @e[tag=prev_s,tag=!used,sort=random,limit=1] run function structures:maze/place/south
execute if score #direction random matches 4 as @e[tag=prev_w,tag=!used,sort=random,limit=1] run function structures:maze/place/west

# Decrement the score and run again
scoreboard players remove #horizontal random 1
execute unless score #horizontal random matches 0 run function structures:maze/place/horizontal