package com.example.charpowers.items.client;

import net.minecraft.client.renderer.RenderType;

// What a trail looks like: how long it lasts and its layers, bottom one first. from and to say which part of the blade a layer covers: 0 is the base, 1 the tip.
// Numbers past 1 stick out beyond the tip, for a glow. width is for a trail with no blade, like a flying shard: half the width across it, in blocks.
enum TrailStyle {
    GOLD(8f, 0, new Layer(RibbonTypes.FADE_SOFT, 0.82f, 0.5f, 0.05f, 0.34f, 0.12f, 1.1f),
            new Layer(RibbonTypes.ADD_STREAK, 1f, 0.62f, 0.14f, 1f, 0.25f, 1.02f),
            new Layer(RibbonTypes.ADD_SOFT, 1f, 0.82f, 0.4f, 0.8f, 0.72f, 1.14f),
            new Layer(RibbonTypes.ADD_SOFT, 1f, 0.97f, 0.72f, 1f, 0.92f, 1.03f)),
    DARK(10f, 0, new Layer(RibbonTypes.FADE_WISP, 0.02f, 0.01f, 0.04f, 0.95f, 0f, 1.2f),
            new Layer(RibbonTypes.FADE_SOFT, 0.1f, 0.03f, 0.16f, 0.65f, 0.2f, 1.34f),
            new Layer(RibbonTypes.ADD_SOFT, 0.34f, 0.12f, 0.5f, 0.5f, 0.9f, 1.05f)),
    SOUL(13f, 0, new Layer(RibbonTypes.FADE_WISP, 0.2f, 0.03f, 0.42f, 0.7f, 0.02f, 1.24f),
            new Layer(RibbonTypes.ADD_WISP, 0.42f, 0.12f, 0.9f, 0.55f, -0.04f, 1.44f),
            new Layer(RibbonTypes.ADD_STREAK, 0.7f, 0.22f, 1f, 1f, 0.2f, 1.01f),
            new Layer(RibbonTypes.ADD_SOFT, 0.86f, 0.5f, 1f, 0.9f, 0.68f, 1.12f),
            new Layer(RibbonTypes.ADD_SOFT, 1f, 0.93f, 1f, 1f, 0.9f, 1.02f)),
    ICE(9f, 0.14f, new Layer(RibbonTypes.FADE_SOFT, 0.7f, 0.9f, 1f, 0.5f, 0f, 1f),
            new Layer(RibbonTypes.ADD_SOFT, 0.75f, 0.95f, 1f, 0.8f, 0.28f, 0.72f)),
    EMBER(6f, 0, new Layer(RibbonTypes.ADD_STREAK, 1f, 0.42f, 0.06f, 0.75f, 0f, 1.1f),
            new Layer(RibbonTypes.ADD_SOFT, 1f, 0.72f, 0.26f, 0.45f, 0.2f, 0.9f)),
    GROUND_GOLD(15f, 0, new Layer(RibbonTypes.ADD_SOFT, 1f, 0.6f, 0.1f, 0.85f, -0.55f, 1.55f),
            new Layer(RibbonTypes.ADD_STREAK, 1f, 0.8f, 0.3f, 1f, 0f, 1f),
            new Layer(RibbonTypes.ADD_SOFT, 1f, 0.96f, 0.7f, 1f, 0.32f, 0.68f)),
    KNIGHT(7f, 0, new Layer(RibbonTypes.FADE_SOFT, 0.42f, 0.34f, 0.86f, 0.4f, 0.2f, 1.08f),
            new Layer(RibbonTypes.ADD_STREAK, 0.75f, 0.7f, 1f, 0.9f, 0.3f, 1f),
            new Layer(RibbonTypes.ADD_SOFT, 0.95f, 0.92f, 1f, 0.9f, 0.85f, 1.03f)),
    CRIMSON(8f, 0, new Layer(RibbonTypes.FADE_SOFT, 0.5f, 0.02f, 0.06f, 0.55f, 0.15f, 1.1f),
            new Layer(RibbonTypes.ADD_STREAK, 1f, 0.2f, 0.18f, 0.9f, 0.25f, 1f),
            new Layer(RibbonTypes.ADD_SOFT, 1f, 0.7f, 0.6f, 0.8f, 0.86f, 1.03f)),
    GRAVITY(10f, 0, new Layer(RibbonTypes.FADE_WISP, 0.04f, 0.06f, 0.2f, 0.7f, 0.05f, 1.2f),
            new Layer(RibbonTypes.ADD_STREAK, 0.3f, 0.6f, 1f, 0.9f, 0.2f, 1.02f),
            new Layer(RibbonTypes.ADD_SOFT, 0.6f, 0.9f, 1f, 0.8f, 0.82f, 1.05f)),
    SPEAR(7f, 0, new Layer(RibbonTypes.ADD_STREAK, 0.5f, 1f, 0.72f, 0.8f, 0.5f, 1.02f),
            new Layer(RibbonTypes.ADD_SOFT, 0.85f, 1f, 0.9f, 0.8f, 0.85f, 1.04f)),
    SCYTHE(11f, 0, new Layer(RibbonTypes.FADE_WISP, 0.1f, 0.04f, 0.2f, 0.75f, 0f, 1.15f),
            new Layer(RibbonTypes.ADD_STREAK, 0.6f, 0.36f, 1f, 0.85f, 0.15f, 1f),
            new Layer(RibbonTypes.ADD_SOFT, 0.9f, 0.8f, 1f, 0.8f, 0.85f, 1.03f)),
    COCYTUS(8f, 0, new Layer(RibbonTypes.ADD_STREAK, 0.55f, 0.85f, 1f, 0.85f, 0f, 1.1f),
            new Layer(RibbonTypes.ADD_SOFT, 0.85f, 0.97f, 1f, 0.8f, 0.3f, 1f)),
    SOLGO(10f, 0, new Layer(RibbonTypes.FADE_WISP, 0.06f, 0.02f, 0.12f, 0.8f, 0f, 1.12f),
            new Layer(RibbonTypes.ADD_STREAK, 0.45f, 0.12f, 0.75f, 0.85f, 0.1f, 1f),
            new Layer(RibbonTypes.ADD_SOFT, 0.9f, 0.75f, 1f, 0.8f, 0.88f, 1.03f));

    final float life, width;
    final Layer[] layers;

    TrailStyle(float life, float width, Layer... layers) {
        this.life = life;
        this.width = width;
        this.layers = layers;
    }

    record Layer(RenderType type, float r, float g, float b, float a, float from, float to) {
        boolean additive() {
            return type == RibbonTypes.ADD_SOFT || type == RibbonTypes.ADD_STREAK || type == RibbonTypes.ADD_GLOW || type == RibbonTypes.ADD_WISP;
        }
    }
}
