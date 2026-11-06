package com.example.charpowers.hidden.model;

// Which blocks a ground-wrecking power may break. Never: air, plain liquid, anything holding things (chests,
// furnaces, beds, signs, spawners... = a block entity), unbreakable blocks (hardness below 0) and anything as hard
// as obsidian or harder (keeps Nether portals and modded reinforced blocks safe).
public final class BreakRule {
    public static final float TOO_HARD = 50f; // obsidian

    public static boolean canWreck(boolean enabled, boolean air, boolean fluidOnly, boolean hasBlockEntity, float hardness) {
        return enabled && !air && !fluidOnly && !hasBlockEntity && hardness >= 0 && hardness < TOO_HARD;
    }

    private BreakRule() {}
}
