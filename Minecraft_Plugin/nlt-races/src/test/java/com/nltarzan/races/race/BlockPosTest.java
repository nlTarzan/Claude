package com.nltarzan.races.race;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockPosTest {

    @Test
    void serializeAndParseRoundTrip() {
        BlockPos pos = new BlockPos("world", -12, 70, 300);
        assertEquals("world,-12,70,300", pos.serialize());
        assertEquals(pos, BlockPos.parse(pos.serialize()));
        assertEquals("world -12, 70, 300", pos.describe());
    }

    @Test
    void brokenTextIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> BlockPos.parse("world,1,2"));
        assertThrows(NumberFormatException.class, () -> BlockPos.parse("world,1,two,3"));
    }
}
