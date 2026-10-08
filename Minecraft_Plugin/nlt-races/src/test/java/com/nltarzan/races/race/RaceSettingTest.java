package com.nltarzan.races.race;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceSettingTest {

    @Test
    void everyDefaultIsWithinItsLimits() {
        for (RaceSetting setting : RaceSetting.values()) {
            assertTrue(setting.min() <= setting.defaultValue() && setting.defaultValue() <= setting.max(),
                    setting.key() + " default " + setting.defaultValue() + " is outside " + setting.min()
                            + ".." + setting.max());
            assertEquals(setting.defaultValue(), setting.clamp(setting.defaultValue()));
        }
    }

    @Test
    void keysAreUniqueAndFoundWhateverTheCase() {
        Set<String> keys = new HashSet<>();
        for (RaceSetting setting : RaceSetting.values()) {
            assertTrue(keys.add(setting.key()), "duplicate key " + setting.key());
            assertEquals(setting, RaceSetting.byKey(setting.key()));
            assertEquals(setting, RaceSetting.byKey(setting.key().toUpperCase(Locale.ROOT)));
        }
        assertNull(RaceSetting.byKey("laps"));
    }

    @Test
    void valuesAreClampedToTheLimits() {
        assertEquals(1, RaceSetting.COUNTDOWN.clamp(0));
        assertEquals(30, RaceSetting.COUNTDOWN.clamp(99));
        assertEquals(10, RaceSetting.COUNTDOWN.clamp(10));
    }
}
