package com.nltarzan.races.race;

import org.bukkit.block.Block;

/** The position of a linked button block. */
public record BlockPos(String world, int x, int y, int z) {

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /** Stored in yml as "world,x,y,z". */
    public String serialize() {
        return world + "," + x + "," + y + "," + z;
    }

    public static BlockPos parse(String raw) {
        String[] p = raw.split(",");
        if (p.length != 4) {
            throw new IllegalArgumentException("Invalid block position '" + raw + "'");
        }
        return new BlockPos(p[0].trim(), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()),
                Integer.parseInt(p[3].trim()));
    }

    public String describe() {
        return world + " " + x + ", " + y + ", " + z;
    }
}
