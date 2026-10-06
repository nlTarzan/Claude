# =================== Elder Guardian checks =====================
# monument_elder_kill.mcfunction
# Execute as:   Player
# Triggered:    Advancement
#
# Description:  Revokes the advancement from the player so it can be triggered again.
#               Checks if there are any other Elder Guardians still in the area.
#               When no Elder Guardians are alive, give hoe with the properties to be able to mine Sponges. (TODO)
#               This hoe needs to be removed from the players inventory when they leave the Monument (TODO)
# ===============================================================


# These commands are run whenever an Elder Guardian is killed
advancement revoke @s only structures:monument_elder_guardian_killed
execute if entity @n[type=minecraft:elder_guardian,distance=..60] run return fail

# It will only fully execute the commands if the player is within range of the RegOceanMonument Marker
execute unless entity @n[type=minecraft:marker,distance=..60,tag=RegOceanMonument] run return fail

# Give player a special hoe capable of mining Sponges inside the Regenerating Ocean Monument
give @s netherite_hoe 1