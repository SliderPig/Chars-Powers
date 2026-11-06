package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CooldownSyncTest {
    // barLeft = the slot's cooldown on Palladium's bar, powerLeft = the equipped power's own cooldown left
    @Test void swappedInAReadyPowerClearsTheBar() { assertTrue(CooldownSync.needsSync(11000, 0)); }
    @Test void autoThrowStartsTheBar()            { assertTrue(CooldownSync.needsSync(0, 12000)); }
    @Test void sameTimerIsLeftAlone()             { assertFalse(CooldownSync.needsSync(100, 99)); }
    @Test void bothReadyIsLeftAlone()             { assertFalse(CooldownSync.needsSync(0, 0)); }
}
