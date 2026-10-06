# ========================= Server Boot =========================
# load.mcfunction
# Execute as:   Server
# Triggered:    On server boot
#
# Description:  Create the scoreboard objectives.
#               Run the schedule.mcfunction
# ===============================================================


# Sets up the scoreboards and create dummy objective
scoreboard objectives add random dummy
scoreboard objectives add cleared dummy
scoreboard objectives add active dummy
scoreboard objectives add count dummy
scoreboard objectives add test dummy

# Automatically starts the schedule
function structures:schedule
