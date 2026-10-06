# =================== Placing Ocean Monument ====================
# monument_place.mcfunction
# Execute as:   Server
# Triggered:    By monument_checks.mcfunction
#
# Description:  Place an Ocean Monument based on scoreboard: random
#               These Monument have been saved as a structure for each quadrant.
#               The Wing Quadrant include an Elder Guardian each.
#               Once the Monument has been placed, the third Elder Guardian is summoned.
# ===============================================================


# Place empty Ocean Monument
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 0 run place template structures:monument/pre-gen/monument_00_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 0 run place template structures:monument/pre-gen/monument_00_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 0 run place template structures:monument/pre-gen/monument_00_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 0 run place template structures:monument/pre-gen/monument_00_nw ~-29 ~ ~-29
execute if score #monument random matches 0 run say §aAn empty Monument has been placed.

# Place Ocean Monument 1
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 1..10 run place template structures:monument/pre-gen/monument_01_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 1..10 run place template structures:monument/pre-gen/monument_01_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 1..10 run place template structures:monument/pre-gen/monument_01_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 1..10 run place template structures:monument/pre-gen/monument_01_nw ~-29 ~ ~-29
execute if score #monument random matches 1..10 run say §aMonument nr.1 has been placed.

# Place Ocean Monument 2
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 11..20 run place template structures:monument/pre-gen/monument_02_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 11..20 run place template structures:monument/pre-gen/monument_02_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 11..20 run place template structures:monument/pre-gen/monument_02_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 11..20 run place template structures:monument/pre-gen/monument_02_nw ~-29 ~ ~-29
execute if score #monument random matches 11..20 run say §aMonument nr.2 has been placed.

# Place Ocean Monument 3
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 21..30 run place template structures:monument/pre-gen/monument_03_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 21..30 run place template structures:monument/pre-gen/monument_03_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 21..30 run place template structures:monument/pre-gen/monument_03_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 21..30 run place template structures:monument/pre-gen/monument_03_nw ~-29 ~ ~-29
execute if score #monument random matches 21..30 run say §aMonument nr.3 has been placed.

# Place Ocean Monument 4
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 31..40 run place template structures:monument/pre-gen/monument_04_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 31..40 run place template structures:monument/pre-gen/monument_04_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 31..40 run place template structures:monument/pre-gen/monument_04_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 31..40 run place template structures:monument/pre-gen/monument_04_nw ~-29 ~ ~-29
execute if score #monument random matches 31..40 run say §aMonument nr.4 has been placed.

# Place Ocean Monument 5
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 41..50 run place template structures:monument/pre-gen/monument_05_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 41..50 run place template structures:monument/pre-gen/monument_05_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 41..50 run place template structures:monument/pre-gen/monument_05_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 41..50 run place template structures:monument/pre-gen/monument_05_nw ~-29 ~ ~-29
execute if score #monument random matches 41..50 run say §aMonument nr.5 has been placed.

# Place Ocean Monument 6
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 51..60 run place template structures:monument/pre-gen/monument_06_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 51..60 run place template structures:monument/pre-gen/monument_06_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 51..60 run place template structures:monument/pre-gen/monument_06_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 51..60 run place template structures:monument/pre-gen/monument_06_nw ~-29 ~ ~-29
execute if score #monument random matches 51..60 run say §aMonument nr.6 has been placed.

# Place Ocean Monument 7
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 61..70 run place template structures:monument/pre-gen/monument_07_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 61..70 run place template structures:monument/pre-gen/monument_07_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 61..70 run place template structures:monument/pre-gen/monument_07_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 61..70 run place template structures:monument/pre-gen/monument_07_nw ~-29 ~ ~-29
execute if score #monument random matches 61..70 run say §aMonument nr.7 has been placed.

# Place Ocean Monument 8
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 71..80 run place template structures:monument/pre-gen/monument_08_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 71..80 run place template structures:monument/pre-gen/monument_08_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 71..80 run place template structures:monument/pre-gen/monument_08_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 71..80 run place template structures:monument/pre-gen/monument_08_nw ~-29 ~ ~-29
execute if score #monument random matches 71..80 run say §aMonument nr.8 has been placed.

# Place Ocean Monument 9
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 81..90 run place template structures:monument/pre-gen/monument_09_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 81..90 run place template structures:monument/pre-gen/monument_09_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 81..90 run place template structures:monument/pre-gen/monument_09_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 81..90 run place template structures:monument/pre-gen/monument_09_nw ~-29 ~ ~-29
execute if score #monument random matches 81..90 run say §aMonument nr.9 has been placed.

# Place Ocean Monument 10
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 91..100 run place template structures:monument/pre-gen/monument_10_se ~ ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 91..100 run place template structures:monument/pre-gen/monument_10_sw ~-29 ~ ~
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 91..100 run place template structures:monument/pre-gen/monument_10_ne ~ ~ ~-29
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] if score #monument random matches 91..100 run place template structures:monument/pre-gen/monument_10_nw ~-29 ~ ~-29
execute if score #monument random matches 91..100 run say §aMonument nr.10 has been placed.

# Place an Elder Guardian in the top center room
# This was done due to having either 2, 3 or 4 Elder Guardians when saving 4 different structures for a Monument
execute at @e[type=marker,tag=ResettingStructure,tag=OceanMonument] run summon minecraft:elder_guardian ~ 52.5 ~ {Health:100,attributes:[{id:attack_damage,base:10f},{id:max_health,base:100f}]}