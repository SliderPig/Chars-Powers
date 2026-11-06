package com.example.charpowers.items.client;

import com.example.charpowers.items.CharItems;
import com.example.charpowers.items.SwordInStone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

// The sword stuck in the stone: its real model, point down, leaning very slightly, with a soft golden glow breathing round the hilt.
public final class StoneSwordRenderer implements BlockEntityRenderer<SwordInStone.Entity> {
    public StoneSwordRenderer(BlockEntityRendererProvider.Context c) {}

    @Override
    public void render(SwordInStone.Entity e, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (e.getBlockState().getValue(SwordInStone.PULLED)) return;
        pose.pushPose();
        // the model's middle sits above the stone, by as much as puts its point down in the slit
        pose.translate(0.5, 1.22, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(180f + 4f));
        pose.mulPose(Axis.YP.rotationDegrees(35f));
        pose.mulPose(Axis.XP.rotationDegrees(3f));
        pose.scale(0.74f, 0.74f, 0.74f);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(CharItems.LONG_SWORD.get()), ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, e.getLevel(), 0);
        pose.popPose();
        float time = e.getLevel() == null ? 0 : e.getLevel().getGameTime() + partial;
        float breathe = 0.75f + 0.25f * Mth.sin(time * 0.06f);
        pose.pushPose();
        pose.translate(0.5, 1.75, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        VertexConsumer v = buffers.getBuffer(RibbonTypes.ADD_GLOW);
        Matrix4f m = pose.last().pose();
        float size = 1.15f + 0.12f * breathe;
        float r = 0.55f * breathe, g = 0.38f * breathe, b = 0.1f * breathe;
        v.vertex(m, -size, -size * 1.5f, 0).color(r, g, b, 1f).uv(0, 1).endVertex();
        v.vertex(m, size, -size * 1.5f, 0).color(r, g, b, 1f).uv(1, 1).endVertex();
        v.vertex(m, size, size * 1.5f, 0).color(r, g, b, 1f).uv(1, 0).endVertex();
        v.vertex(m, -size, size * 1.5f, 0).color(r, g, b, 1f).uv(0, 0).endVertex();
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SwordInStone.Entity e) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
