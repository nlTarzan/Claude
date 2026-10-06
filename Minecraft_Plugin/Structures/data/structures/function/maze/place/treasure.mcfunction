# ======================== Treasure Room ========================
# treasure.mcfunction
# Execute as:   Node with down tag and not used
# Triggered:    By core & itself
#
# Description:  Create selection boxes for each axes and tags them.
#               Chooses 1 nodes with the tag "step" and runs maze_forward.mcfunction.
#               When no nodes are available options proceed to backtrack along the path.
#               If no nodes are available and the current node has the tag "start",
#               end generation by running maze_end_generation.mcfunction.
# ===============================================================


# ===============================================================
scoreboard players remove #treasure count 1
execute if score #treasure count matches 1..10 as @e[tag=prev_d,tag=!used,sort=random,limit=1] run function structures:maze/place/treasure

# Check neighbors, only nodes with a "down" tag can place a treasure room, and only if they are not already used..
execute at @s if entity @e[tag=node,tag=prev_n,dx=0,dy=0,dz=8,distance=8] run return fail
execute at @s if entity @e[tag=node,tag=prev_e,dx=-8,dy=0,dz=0,distance=8] run return fail
execute at @s if entity @e[tag=node,tag=prev_s,dx=0,dy=0,dz=-8,distance=8] run return fail
execute at @s if entity @e[tag=node,tag=prev_w,dx=8,dy=0,dz=0,distance=8] run return fail
execute at @s if entity @e[tag=node,tag=prev_d,dx=0,dy=4,dz=0,distance=4] run return fail

# If previous commands fail, place a Sponge Room
execute at @s run place template structures:monument/maze/room/1x1x1_sponge ~-3.5 ~ ~-3.5

# Rolls for sponges and place them in the room.
# Each of the 36 columns in the room has a 1/3 chance of being empty, a 1/2 chance of having 1 sponge, and a 1/6 chance of having 2 sponges.
#execute at @s run fill ~-3 ~1 ~-3 ~2 ~2 ~2 wet_sponge

# Rolling and placing sponges in the room.
# Row 1
execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-3 ~2 ~-3 ~-3 ~2 ~-3 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-3 ~1 ~-3 ~-3 ~2 ~-3 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-2 ~2 ~-3 ~-2 ~2 ~-3 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-2 ~1 ~-3 ~-2 ~2 ~-3 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-1 ~2 ~-3 ~-1 ~2 ~-3 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-1 ~1 ~-3 ~-1 ~2 ~-3 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~ ~2 ~-3 ~ ~2 ~-3 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~ ~1 ~-3 ~ ~2 ~-3 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~1 ~2 ~-3 ~1 ~2 ~-3 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~1 ~1 ~-3 ~1 ~2 ~-3 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~2 ~2 ~-3 ~2 ~2 ~-3 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~2 ~1 ~-3 ~2 ~2 ~-3 wet_sponge

# Row 2
execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-3 ~2 ~-2 ~-3 ~2 ~-2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-3 ~1 ~-2 ~-3 ~2 ~-2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-2 ~2 ~-2 ~-2 ~2 ~-2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-2 ~1 ~-2 ~-2 ~2 ~-2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-1 ~2 ~-2 ~-1 ~2 ~-2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-1 ~1 ~-2 ~-1 ~2 ~-2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~ ~2 ~-2 ~ ~2 ~-2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~ ~1 ~-2 ~ ~2 ~-2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~1 ~2 ~-2 ~1 ~2 ~-2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~1 ~1 ~-2 ~1 ~2 ~-2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~2 ~2 ~-2 ~2 ~2 ~-2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~2 ~1 ~-2 ~2 ~2 ~-2 wet_sponge

# Row 3
execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-3 ~2 ~-1 ~-3 ~2 ~-1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-3 ~1 ~-1 ~-3 ~2 ~-1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-2 ~2 ~-1 ~-2 ~2 ~-1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-2 ~1 ~-1 ~-2 ~2 ~-1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-1 ~2 ~-1 ~-1 ~2 ~-1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-1 ~1 ~-1 ~-1 ~2 ~-1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~ ~2 ~-1 ~ ~2 ~-1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~ ~1 ~-1 ~ ~2 ~-1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~1 ~2 ~-1 ~1 ~2 ~-1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~1 ~1 ~-1 ~1 ~2 ~-1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~2 ~2 ~-1 ~2 ~2 ~-1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~2 ~1 ~-1 ~2 ~2 ~-1 wet_sponge

# Row 4
execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-3 ~2 ~ ~-3 ~2 ~ wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-3 ~1 ~ ~-3 ~2 ~ wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-2 ~2 ~ ~-2 ~2 ~ wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-2 ~1 ~ ~-2 ~2 ~ wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-1 ~2 ~ ~-1 ~2 ~ wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-1 ~1 ~ ~-1 ~2 ~ wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~ ~2 ~ ~ ~2 ~ wet_sponge
execute if score #sponge random matches 2 at @s run fill ~ ~1 ~ ~ ~2 ~ wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~1 ~2 ~ ~1 ~2 ~ wet_sponge
execute if score #sponge random matches 2 at @s run fill ~1 ~1 ~ ~1 ~2 ~ wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~2 ~2 ~ ~2 ~2 ~ wet_sponge
execute if score #sponge random matches 2 at @s run fill ~2 ~1 ~ ~2 ~2 ~ wet_sponge

# Row 5
execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-3 ~2 ~1 ~-3 ~2 ~1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-3 ~1 ~1 ~-3 ~2 ~1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-2 ~2 ~1 ~-2 ~2 ~1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-2 ~1 ~1 ~-2 ~2 ~1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-1 ~2 ~1 ~-1 ~2 ~1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-1 ~1 ~1 ~-1 ~2 ~1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~ ~2 ~1 ~ ~2 ~1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~ ~1 ~1 ~ ~2 ~1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~1 ~2 ~1 ~1 ~2 ~1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~1 ~1 ~1 ~1 ~2 ~1 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~2 ~2 ~1 ~2 ~2 ~1 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~2 ~1 ~1 ~2 ~2 ~1 wet_sponge

# Row 6
execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-3 ~2 ~2 ~-3 ~2 ~2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-3 ~1 ~2 ~-3 ~2 ~2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-2 ~2 ~2 ~-2 ~2 ~2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-2 ~1 ~2 ~-2 ~2 ~2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~-1 ~2 ~2 ~-1 ~2 ~2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~-1 ~1 ~2 ~-1 ~2 ~2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~ ~2 ~2 ~ ~2 ~2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~ ~1 ~2 ~ ~2 ~2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~1 ~2 ~2 ~1 ~2 ~2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~1 ~1 ~2 ~1 ~2 ~2 wet_sponge

execute store result score #sponge random run random value 0..2
execute if score #sponge random matches 1 at @s run fill ~2 ~2 ~2 ~2 ~2 ~2 wet_sponge
execute if score #sponge random matches 2 at @s run fill ~2 ~1 ~2 ~2 ~2 ~2 wet_sponge

# Mark sponge room as used and add sponge tag to it.
execute as @s run tag @s add sponge
execute at @s run tag @s add used
execute if score #generating test matches 1 run tellraw @a "A Sponge room has been placed!"