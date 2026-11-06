package com.example.charpowers.items;

import net.minecraftforge.common.ForgeConfigSpec;

// config/charpowers-items.toml
public final class ItemConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SOLGO_SPAWNS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SOLGO_SPAWNS = b.comment("Solgo can turn up at night in dark forests and old growth woods. One at a time, rarely, with a quiet warning first.").define("solgoSpawns", true);
        SPEC = b.build();
    }

    private ItemConfig() {}
}
