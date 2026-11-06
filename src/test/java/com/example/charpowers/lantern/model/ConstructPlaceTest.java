package com.example.charpowers.lantern.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConstructPlaceTest {
    @Test
    void facingSouthForwardIsPlusZAndRightIsMinusX() {
        assertEquals(1, ConstructPlace.forward(0)[1], 1e-9);
        assertEquals(-1, ConstructPlace.right(0)[0], 1e-9);
    }

    @Test
    void aSlamStartsAboveTheGroundSoThePalmMeetsIt() {
        double[] at = ConstructPlace.spawn(ConstructPlace.SLAM, 0f, 10, 64, 10);
        assertEquals(65.06, at[1], 1e-9);
        // the model's middle is 0.74 to its left and 0.49 behind, so the entity sits 0.74 to the right and 0.49 ahead of the target
        assertEquals(10 - 0.74, at[0], 1e-9);
        assertEquals(10 + 0.49, at[2], 1e-9);
    }
}
