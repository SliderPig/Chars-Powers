package com.example.charpowers.hidden.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PowerListTest {
    private static final PowerList.Entry SLAM = new PowerList.Entry("ground_slam", "Ground Slam", Rank.S, true);
    private static final PowerList.Entry METEOR = new PowerList.Entry("meteor", "Meteor", Rank.A, true);
    private static final PowerList.Entry BLEED = new PowerList.Entry("bleed", "Bleed", Rank.D, false);
    private static final PowerList.Entry FLICK = new PowerList.Entry("flick", "Flick", Rank.F, true);
    private static final PowerList.Entry ANKLE = new PowerList.Entry("ankle_biter", "ankle Biter", Rank.D, false);
    private static final List<PowerList.Entry> ALL = List.of(FLICK, BLEED, SLAM, METEOR, ANKLE);

    private static List<String> ids(List<PowerList.Entry> l) { return l.stream().map(PowerList.Entry::id).toList(); }

    @Test void rankDownPutsSFirst() {
        assertEquals(List.of("ground_slam", "meteor", "ankle_biter", "bleed", "flick"), ids(PowerList.view(ALL, PowerList.Sort.RANK_DOWN, "")));
    }

    @Test void rankUpPutsFFirst() {
        assertEquals(List.of("flick", "ankle_biter", "bleed", "meteor", "ground_slam"), ids(PowerList.view(ALL, PowerList.Sort.RANK_UP, "")));
    }

    @Test void nameIsAlphabetical() {
        assertEquals(List.of("ankle_biter", "bleed", "flick", "ground_slam", "meteor"), ids(PowerList.view(ALL, PowerList.Sort.NAME, "")));
    }

    @Test void keysFirstThenRank() {
        assertEquals(List.of("ground_slam", "meteor", "flick", "ankle_biter", "bleed"), ids(PowerList.view(ALL, PowerList.Sort.KEYS_FIRST, "")));
    }

    @Test void searchIgnoresCase() {
        assertEquals(List.of("ground_slam"), ids(PowerList.view(ALL, PowerList.Sort.NAME, "SLAM")));
    }

    @Test void noMatchIsEmpty() {
        assertTrue(PowerList.view(ALL, PowerList.Sort.NAME, "zzz").isEmpty());
    }

    @Test void nextCycles() {
        assertEquals(PowerList.Sort.RANK_UP, PowerList.Sort.RANK_DOWN.next());
        assertEquals(PowerList.Sort.NAME, PowerList.Sort.RANK_UP.next());
        assertEquals(PowerList.Sort.KEYS_FIRST, PowerList.Sort.NAME.next());
        assertEquals(PowerList.Sort.RANK_DOWN, PowerList.Sort.KEYS_FIRST.next());
    }

    @Test void scrollClamps() {
        assertEquals(2, PowerList.clampScroll(50, 10, 8));
        assertEquals(0, PowerList.clampScroll(-3, 10, 8));
        assertEquals(0, PowerList.clampScroll(4, 3, 8));
    }
}
