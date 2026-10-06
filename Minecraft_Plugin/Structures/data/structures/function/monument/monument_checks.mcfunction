# ======================= Monument checks =======================
# monument_checks.mcfunction
# Execute as:   Server
# Triggered:    By monument_initial.mcfunction
#
# Description:  Check the area of the Regenerating Ocean Monument for any Elder Guardians.
#               If no Elder Guardians are within the area start generating a new Ocean Monument.
#               Remove the force load to preserve server performance.
# ===============================================================


# Check if all Elder Guardians have been killed
# Store the results in a scoreboard
execute at @e[tag=ResettingStructure,tag=OceanMonument] if entity @n[type=minecraft:elder_guardian,distance=..40] run scoreboard players set #monument cleared 0
execute at @e[tag=ResettingStructure,tag=OceanMonument] unless entity @n[type=minecraft:elder_guardian,distance=..40] run scoreboard players set #monument cleared 1
execute if score #monument cleared matches 0 run say §cThe Monument has not been cleared.
execute if score #monument cleared matches 1 run say §aThe Monument has cleared.

# When all Elder Guardians have been killed, run the randomizer and place a new Ocean Monument
execute if score #monument cleared matches 1 store result score #monument random run random value 1..100
#execute if score #monument cleared matches 1 run tellraw @a [{text:"Rolled number "},{"score": {"name": "#monument", "objective": "random"} }]
execute if score #monument cleared matches 1 run function structures:monument/monument_place

# Ocean Monument force load is removed to preserve server performance
forceload remove 192 -289 255 -352