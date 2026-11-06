package com.example.charpowers.items.client;

import com.example.charpowers.CharPowers;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

// How trails are drawn: glowing light that adds up (ADD) or a soft see-through smear (FADE, which is the only way anything can look dark). Extends RenderType only to reach its protected parts.
final class RibbonTypes extends RenderType {
    static final ResourceLocation SOFT = tex("soft"), STREAK = tex("streak"), WISP = tex("wisp"), GLOW = tex("glow");

    static final RenderType ADD_SOFT = make("add_soft", SOFT, true), ADD_STREAK = make("add_streak", STREAK, true), ADD_GLOW = make("add_glow", GLOW, true);
    static final RenderType FADE_SOFT = make("fade_soft", SOFT, false), FADE_WISP = make("fade_wisp", WISP, false), ADD_WISP = make("add_wisp", WISP, true);

    private RibbonTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(CharPowers.MODID, "textures/misc/ribbon_" + name + ".png");
    }

    private static RenderType make(String name, ResourceLocation texture, boolean add) {
        return create("charpowers_ribbon_" + name, DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 2048, false, false,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(add ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .createCompositeState(false));
    }
}
