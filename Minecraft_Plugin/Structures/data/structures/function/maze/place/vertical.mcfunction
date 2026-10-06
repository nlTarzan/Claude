# ======================= Core Placement ========================
# vertical.mcfunction
# Execute as:   Server
# Triggered:    By core
#
# Description:  Pick a random amount of vertical rooms to place.
#               Place them one by one
#               
#
# ===============================================================

execute if score #vertical random matches 0 run return fail

execute if score #generating test matches 1 run tellraw @a [{"score":{"name":"#vertical","objective":"random"}},{"text":" verticals to be placed."}]

# If scoreboard is uneven try placing a downwards 1x1x2 room
execute if score #vertical random matches 1 as @e[tag=prev_u,tag=!used,sort=random,limit=1] run function structures:maze/place/up
execute if score #vertical random matches 3 as @e[tag=prev_u,tag=!used,sort=random,limit=1] run function structures:maze/place/up
execute if score #vertical random matches 5 as @e[tag=prev_u,tag=!used,sort=random,limit=1] run function structures:maze/place/up

# If scoreboard is uneven try placing a upwards 1x1x2 room
execute if score #vertical random matches 2 as @e[tag=prev_d,tag=!used,sort=random,limit=1] run function structures:maze/place/down
execute if score #vertical random matches 4 as @e[tag=prev_d,tag=!used,sort=random,limit=1] run function structures:maze/place/down
execute if score #vertical random matches 6 as @e[tag=prev_d,tag=!used,sort=random,limit=1] run function structures:maze/place/down

# Decrement the score and run again
scoreboard players remove #vertical random 1
execute unless score #vertical random matches 0 run function structures:maze/place/vertical