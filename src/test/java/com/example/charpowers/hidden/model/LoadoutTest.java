package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class LoadoutTest {
    static final PowerDef MEOW = new PowerDef("loud_meow", 1, Rank.F, Kind.KEY, MobRule.RANDOM, 200);
    static final PowerDef SLAP = new PowerDef("fish_slap", 19, Rank.F, Kind.KEY, MobRule.ATTACK, 200);
    static final PowerDef TRAIL = new PowerDef("rainbow_trail", 7, Rank.F, Kind.ALWAYS_ON, MobRule.PASSIVE, 0);
    static final PowerDef SQUEAK = new PowerDef("squeaky_steps", 8, Rank.F, Kind.ALWAYS_ON, MobRule.PASSIVE, 0);

    @Test
    void equipRules() {
        Loadout l = new Loadout();
        assertFalse(l.equip(MEOW, true, 0), "not owned");
        l.add("loud_meow");
        l.add("rainbow_trail");
        assertFalse(l.equip(MEOW, false, 0), "key power in an always-on slot");
        assertFalse(l.equip(TRAIL, true, 0), "always-on power in a key slot");
        assertFalse(l.equip(MEOW, true, 5));
        assertFalse(l.equip(TRAIL, false, 10));
        assertFalse(l.equip(MEOW, true, -1));
        assertTrue(l.equip(MEOW, true, 0));
        assertEquals("loud_meow", l.key(0));
        assertTrue(l.equip(MEOW, true, 3));
        assertNull(l.key(0), "moved, not copied");
        assertEquals("loud_meow", l.key(3));
        assertTrue(l.equip(TRAIL, false, 9));
        assertEquals("rainbow_trail", l.always(9));
    }

    @Test
    void addTwiceDoesNothing() {
        Loadout l = new Loadout();
        assertTrue(l.add("loud_meow"));
        assertFalse(l.add("loud_meow"));
        assertEquals(1, l.owned().size());
    }

    @Test
    void removeRandomEquippedOnlyFromSlots() {
        Loadout l = new Loadout();
        l.add("loud_meow"); l.add("fish_slap"); l.add("rainbow_trail");
        assertTrue(l.removeRandomEquipped(new SplittableRandom(1)).isEmpty());
        l.equip(SLAP, true, 2);
        assertEquals("fish_slap", l.removeRandomEquipped(new SplittableRandom(1)).orElseThrow());
        assertEquals(2, l.owned().size());
        assertNull(l.key(2));
    }

    @Test
    void removeEquippedLeavesSpares() {
        Loadout l = new Loadout();
        l.add("loud_meow"); l.add("fish_slap"); l.add("rainbow_trail"); l.add("squeaky_steps");
        l.equip(MEOW, true, 0);
        l.equip(SQUEAK, false, 4);
        assertEquals(2, l.removeEquipped().size());
        assertEquals(2, l.owned().size());
        assertTrue(l.owned().contains("fish_slap"));
        assertTrue(l.equipped().isEmpty());
    }

    @Test
    void clearAllEmptiesEverything() {
        Loadout l = new Loadout();
        l.add("loud_meow");
        l.equip(MEOW, true, 1);
        l.clearAll();
        assertTrue(l.owned().isEmpty());
        assertNull(l.key(1));
    }

    @Test
    void takeOneOwnedAlsoLeavesItsSlot() {
        Loadout l = new Loadout();
        l.add("loud_meow");
        l.equip(MEOW, true, 1);
        assertTrue(l.remove("loud_meow"));
        assertNull(l.key(1));
    }

    @Test
    void mapRoundTrip() {
        Loadout l = new Loadout();
        l.add("loud_meow"); l.add("rainbow_trail"); l.add("fish_slap");
        l.equip(MEOW, true, 4);
        l.equip(TRAIL, false, 7);
        Loadout back = Loadout.fromMap(l.toMap());
        assertEquals(l.owned(), back.owned());
        for (int i = 0; i < Loadout.KEY_SLOTS; i++) assertEquals(l.key(i), back.key(i));
        for (int i = 0; i < Loadout.ALWAYS_SLOTS; i++) assertEquals(l.always(i), back.always(i));
    }

    @Test
    void fromMapDropsSlotsThatArentOwned() {
        // a broken or hand-edited save must not equip something the player doesn't have
        var m = new java.util.HashMap<String, Object>();
        m.put("owned", java.util.List.of("loud_meow"));
        m.put("keys", java.util.List.of("", "fish_slap", "", "", ""));
        m.put("always", java.util.List.of());
        Loadout l = Loadout.fromMap(m);
        assertNull(l.key(1));
    }
}
