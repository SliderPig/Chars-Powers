package com.example.charpowers.cloning.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import org.junit.jupiter.api.Test;

class StripPlanTest {
    @Test
    void mainTunnelWithBranchesEveryThreeBlocks() {
        StripPlan p = new StripPlan(new Cell(0, -54, 0), 1, 0);
        assertEquals(16 * 5, p.size());
        assertEquals(new Cell(3, -54, 0), p.leg(0).to());
        assertFalse(p.leg(0).branch());
        assertTrue(p.leg(1).branch());
        assertEquals(new Cell(3, -54, -12), p.leg(1).to());
        assertEquals(new Cell(3, -54, 12), p.leg(3).to());
        assertEquals(new Cell(48, -54, 0), p.leg(p.size() - 1).to());
    }

    @Test
    void nothingGoesPastTheLimits() {
        StripPlan p = new StripPlan(new Cell(10, 15, 10), 0, -1);
        for (int i = 0; i < p.size(); i++) {
            Cell c = p.leg(i).to();
            assertTrue(Math.abs(c.x() - 10) <= StripPlan.BRANCH && 10 - c.z() <= StripPlan.MAIN && c.z() <= 10);
        }
    }

    @Test
    void nextStripIsBesideThisOne() {
        StripPlan p = new StripPlan(new Cell(0, -54, 0), 1, 0);
        assertEquals(new Cell(0, -54, 27), p.nextOrigin());
    }
}
