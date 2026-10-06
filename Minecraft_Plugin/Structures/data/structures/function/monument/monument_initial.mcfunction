# =================== Ocean Monument initial ====================
# monument_initial.mcfunction
# Execute as:   Server
# Triggered:    By schedule.mcfunction
#
# Description:  Area around the Ocean Monument is force loaded to be able to check for entities
#               If no Elder Guardians are within the area start generating a new Ocean Monument.
#               Remove the force load to preserve server performance.
# ===============================================================

# Force load the area around the Monument
forceload add 192 -289 255 -352

# Schedule the checking of the Monument
schedule function structures:monument/monument_checks 5t