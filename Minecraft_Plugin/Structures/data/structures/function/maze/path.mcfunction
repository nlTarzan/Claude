# ========================== Maze path ==========================
# path.mcfunction
# Execute as:   Server
# Triggered:    Manual
#
# Description:  Shows the maze path with emeralds blocks.
#               Start is marked with a redstone block
# ===============================================================


# Testing only
execute at @e[tag=node,tag=prev_e] run fill ~1 ~ ~ ~7 ~ ~ magenta_glazed_terracotta[facing=west]
execute at @e[tag=node,tag=prev_s] run fill ~ ~ ~1 ~ ~ ~7 magenta_glazed_terracotta[facing=north]
execute at @e[tag=node,tag=prev_w] run fill ~-1 ~ ~ ~-7 ~ ~ magenta_glazed_terracotta[facing=east]
execute at @e[tag=node,tag=prev_n] run fill ~ ~ ~-1 ~ ~ ~-7 magenta_glazed_terracotta[facing=south]
execute at @e[tag=node,tag=prev_u] run fill ~ ~2 ~ ~ ~ ~ warped_fence
execute at @e[tag=node,tag=prev_d] run fill ~ ~ ~ ~ ~-2 ~ crimson_fence
execute at @e[tag=node,tag=current] run setblock ~ ~ ~ redstone_block

tellraw @a "The path is shown."