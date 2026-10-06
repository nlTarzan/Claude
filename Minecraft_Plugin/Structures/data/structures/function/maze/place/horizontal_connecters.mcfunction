# ======================== Horizontal Connecters ========================
# horizontal_connecters.mcfunction
# Execute as:   Node with tag lon
# Triggered:    By core.mcfunction
#
# Description:  Run as long node to open up pathways.
#               Place water to create the doorways from Long Nodes first.         
#               Afterwards place the remaining water for the other nodes that are not long.
#               
# ===============================================================

# Place water to create the doorways from Long Nodes first.
execute at @s positioned ~ ~ ~8 as @e[tag=prev_n,tag=!long,distance=..2] run fill ~-0.5 ~ ~-2.5 ~0.5 ~1 ~-5.5 water
execute at @s positioned ~-8 ~ ~ as @e[tag=prev_e,tag=!long,distance=..2] run fill ~2.5 ~ ~-0.5 ~5.5 ~1 ~0.5 water
execute at @s positioned ~ ~ ~-8 as @e[tag=prev_s,tag=!long,distance=..2] run fill ~-0.5 ~ ~2.5 ~0.5 ~1 ~5.5 water
execute at @s positioned ~8 ~ ~ as @e[tag=prev_w,tag=!long,distance=..2] run fill ~-2.5 ~ ~-0.5 ~-5.5 ~1 ~0.5 water

# Place remaining water for the other nodes that are not long.
execute at @e[tag=prev_n,tag=!long] run fill ~-0.5 ~ ~-2.5 ~0.5 ~1 ~-5.5 water
execute at @e[tag=prev_e,tag=!long] run fill ~2.5 ~ ~-0.5 ~5.5 ~1 ~0.5 water
execute at @e[tag=prev_s,tag=!long] run fill ~-0.5 ~ ~2.5 ~0.5 ~1 ~5.5 water
execute at @e[tag=prev_w,tag=!long] run fill ~-2.5 ~ ~-0.5 ~-5.5 ~1 ~0.5 water