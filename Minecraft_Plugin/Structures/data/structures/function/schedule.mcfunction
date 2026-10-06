# ==================== Schedule of functions ====================
# The whole system is triggered from here
# Intervals are randomized to prevent players from camping structures
# ===============================================================
# Schedules can be set with a time measure in ticks, seconds or Minecraft days.
#                              1 second  =        20 ticks
#              1 mcday  =  1,200 seconds =    24,000 ticks
# 1 IRL day = 72 mcdays = 86,400 seconds = 1,728,000 ticks
# ===============================================================
# Schedules:
# - Schedule (itself)

# Functions to run:
# - Regenerating Ocean Monument
# - say/tellraw command as feedback while testing. Use # after testing is done.
# ===============================================================

# ===================== Randomize schedule ======================
execute store result score #schedule random run random value 1..100
#tellraw @a [{text:"Schedule random nr. set to "},{"score": {"name": "#schedule", "objective": "random"} }]

# ====================== Testing / Initial ======================
# Run schedule in 5 seconds, this should only happen on initial launch as the scoreboard random for #schedule should never be 0
execute if score #schedule random matches 0 run say §7Next schedule is set for 5 seconds

# Run schedule in 10 seconds, should be # when uploaded to the server
execute if score #schedule random matches 1..30 run schedule function structures:monument/monument_initial 10s
execute if score #schedule random matches 1..30 run schedule function structures:schedule 10s
execute if score #schedule random matches 1..30 run say §7Next schedule is set for 10 seconds

# Run schedule in 15 seconds, should be # when uploaded to the server
execute if score #schedule random matches 31..60 run schedule function structures:monument/monument_initial 15s
execute if score #schedule random matches 31..60 run schedule function structures:schedule 15s
execute if score #schedule random matches 31..60 run say §7Next schedule is set for 15 seconds

# Run schedule in 20 seconds, should be # when uploaded to the server
execute if score #schedule random matches 61..90 run schedule function structures:monument/monument_initial 20s
execute if score #schedule random matches 61..90 run schedule function structures:schedule 20s
execute if score #schedule random matches 61..90 run say §7Next schedule is set for 20 seconds

# Run schedule in 25 seconds, should be # when uploaded to the server
execute if score #schedule random matches 91..100 run schedule function structures:monument/monument_initial 25s
execute if score #schedule random matches 91..100 run schedule function structures:schedule 25s
execute if score #schedule random matches 91..100 run say §7Next schedule is set for 25 seconds

# ========================= Scheduling ==========================
# Run schedule in 1 Minecraft day
#execute if score #schedule random matches 1..30 run schedule function structures:monument_initial 1d
#execute if score #schedule random matches 1..30 run schedule function structures:schedule 1d
#execute if score #schedule random matches 1..30 run say §7Next schedule is set for 1 Minecraft day

# Run schedule in 2 Minecraft days
#execute if score #schedule random matches 31..60 run schedule function structures:monument_initial 2d
#execute if score #schedule random matches 31..60 run schedule function structures:schedule 2d
#execute if score #schedule random matches 31..60 run say §7Next schedule is set for 2 Minecraft days

# Run schedule in 3 Minecraft days
#execute if score #schedule random matches 61..90 run schedule function structures:monument_initial 3d
#execute if score #schedule random matches 61..90 run schedule function structures:schedule 3d
#execute if score #schedule random matches 61..90 run say §7Next schedule is set for 3 Minecraft days

# Run schedule in 1 IRL day
#execute if score #schedule random matches 91..100 run schedule function structures:monument_initial 72d
#execute if score #schedule random matches 91..100 run schedule function structures:schedule 72d
#execute if score #schedule random matches 91..100 run say §7Next schedule is set for 1 IRL day

