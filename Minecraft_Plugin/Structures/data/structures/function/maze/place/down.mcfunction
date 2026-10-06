# ======================== Down columns =========================
# down.mcfunction
# Execute as:   Server
# Triggered:    By vertical
#
# Description:  Run as unused node above the active node to check for placement.
#               Place structure and mark the relevant nodes as used 
# ===============================================================


execute at @s positioned ~ ~-4 ~ as @e[tag=node,tag=!used,distance=..2] run place template structures:monument/maze/room/1x1x2 ~-3.5 ~ ~-3.5
execute at @s positioned ~-0.5 ~-4 ~-0.5 as @e[tag=node,tag=!used,distance=..2] run tag @e[tag=node,dx=1,dy=5,dz=1] add tall
execute at @s positioned ~-0.5 ~-4 ~-0.5 as @e[tag=node,tag=!used,distance=..2] run tag @e[tag=node,dx=1,dy=5,dz=1] add used
