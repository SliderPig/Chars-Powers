package com.example.charpowers.icons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// Draws a power's picture for PowerIconItem. In a menu: just the picture. In the world: a thin round medallion with the
// picture on both faces, always at full brightness, so a Death Steal icon turned edge-on is only a thin pink line.
public final class PowerIconRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WHITE = new ResourceLocation("textures/misc/white.png");
    private static final int FULL_BRIGHT = 0xF000F0, SIDES = 28;
    private static final float DEPTH = 1 / 32f, RADIUS = 0.5f, PICTURE = 0.37f;
    private static PowerIconRenderer instance;

    private PowerIconRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static PowerIconRenderer instance() {
        if (instance == null) instance = new PowerIconRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ResourceLocation texture = PowerIconItem.textureOf(stack);
        if (texture == null) return;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        if (context == ItemDisplayContext.GUI) {
            menuPicture(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)));
        } else {
            medallion(pose, buffers.getBuffer(RenderType.text(WHITE)));
            VertexConsumer pic = buffers.getBuffer(RenderType.text(texture));
            worldPicture(pose, pic, DEPTH + 0.002f, 1f);
            worldPicture(pose, pic, -DEPTH - 0.002f, -1f);
        }
        pose.popPose();
    }

    private static void menuPicture(PoseStack pose, VertexConsumer out) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float[][] corners = {{-0.5f, -0.5f, 0, 1}, {0.5f, -0.5f, 1, 1}, {0.5f, 0.5f, 1, 0}, {-0.5f, 0.5f, 0, 0}};
        for (float[] c : corners) {
            out.vertex(m, c[0], c[1], 0).color(255, 255, 255, 255).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(n, 0, 0, 1).endVertex();
        }
    }

    // A square of picture facing along z. `dir` -1 is the back face, turned so it doesn't read mirrored.
    private static void worldPicture(PoseStack pose, VertexConsumer out, float z, float dir) {
        Matrix4f m = pose.last().pose();
        float h = PICTURE;
        corner(out, m, -h * dir, -h, z, 0, 1);
        corner(out, m, h * dir, -h, z, 1, 1);
        corner(out, m, h * dir, h, z, 1, 0);
        corner(out, m, -h * dir, h, z, 0, 0);
    }

    private static void corner(VertexConsumer out, Matrix4f m, float x, float y, float z, float u, float v) {
        out.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).uv2(FULL_BRIGHT).endVertex();
    }

    // A flat plum disc with a pink rim.
    private static void medallion(PoseStack pose, VertexConsumer out) {
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < SIDES; i++) {
            double a0 = i * Math.PI * 2 / SIDES, a1 = (i + 1) * Math.PI * 2 / SIDES;
            float x0 = (float) Math.cos(a0) * RADIUS, y0 = (float) Math.sin(a0) * RADIUS;
            float x1 = (float) Math.cos(a1) * RADIUS, y1 = (float) Math.sin(a1) * RADIUS;
            // the two faces: each slice is a quad with its last corner twice
            flat(out, m, x0, y0, x1, y1, DEPTH);
            flat(out, m, x1, y1, x0, y0, -DEPTH);
            // the rim
            rim(out, m, x0, y0, -DEPTH);
            rim(out, m, x1, y1, -DEPTH);
            rim(out, m, x1, y1, DEPTH);
            rim(out, m, x0, y0, DEPTH);
        }
    }

    private static void flat(VertexConsumer out, Matrix4f m, float x0, float y0, float x1, float y1, float z) {
        float[][] corners = {{0, 0}, {x0, y0}, {x1, y1}, {x1, y1}};
        for (float[] c : corners) {
            out.vertex(m, c[0], c[1], z).color(46, 30, 54, 255).uv(0.5f, 0.5f).uv2(FULL_BRIGHT).endVertex();
        }
    }

    private static void rim(VertexConsumer out, Matrix4f m, float x, float y, float z) {
        out.vertex(m, x, y, z).color(255, 128, 180, 255).uv(0.5f, 0.5f).uv2(FULL_BRIGHT).endVertex();
    }
}
