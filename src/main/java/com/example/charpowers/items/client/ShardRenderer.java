package com.example.charpowers.items.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.IceShard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

// A small spike of ice drawn from triangles, pointing the way it flies, with a pale trail behind it.
public final class ShardRenderer extends EntityRenderer<IceShard> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CharPowers.MODID, "textures/entity/ice_shard.png");
    private static final int SIDES = 6;
    private static final float LENGTH = 0.85f, BACK = 0.26f, RADIUS = 0.14f;
    private final double[] base = new double[3], tip = new double[3];

    public ShardRenderer(EntityRendererProvider.Context c) {
        super(c);
    }

    @Override
    public ResourceLocation getTextureLocation(IceShard e) {
        return TEXTURE;
    }

    @Override
    public void render(IceShard e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        double x = Mth.lerp(partial, e.xo, e.getX()), y = Mth.lerp(partial, e.yo, e.getY()), z = Mth.lerp(partial, e.zo, e.getZ());
        base[0] = tip[0] = x;
        base[1] = tip[1] = y;
        base[2] = tip[2] = z;
        Trails.sample(e.getId(), TrailStyle.ICE, base, tip, SwingTrack.now(partial));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partial, e.xRotO, e.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 28f));
        VertexConsumer v = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        for (int i = 0; i < SIDES; i++) {
            float a0 = Mth.TWO_PI * i / SIDES, a1 = Mth.TWO_PI * (i + 1) / SIDES;
            float x0 = Mth.cos(a0) * RADIUS, y0 = Mth.sin(a0) * RADIUS, x1 = Mth.cos(a1) * RADIUS, y1 = Mth.sin(a1) * RADIUS;
            float mid = (a0 + a1) / 2;
            // long point at the front, short one behind
            tri(v, m, n, 0, 0, LENGTH, x0, y0, 0, x1, y1, 0, Mth.cos(mid), Mth.sin(mid), 0.3f, 0.5f, 0f, 0f, 1f, 1f, 1f);
            tri(v, m, n, 0, 0, -BACK, x1, y1, 0, x0, y0, 0, Mth.cos(mid), Mth.sin(mid), -0.5f, 0.5f, 1f, 1f, 0f, 0f, 1f);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    private static void tri(VertexConsumer v, Matrix4f m, Matrix3f n, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float nx, float ny, float nz, float u0, float v0, float u1, float v1, float u2, float v2) {
        // quads with one corner doubled, which is what this draw mode wants
        put(v, m, n, ax, ay, az, nx, ny, nz, u0, v0);
        put(v, m, n, bx, by, bz, nx, ny, nz, u1, v1);
        put(v, m, n, cx, cy, cz, nx, ny, nz, u2, v2);
        put(v, m, n, cx, cy, cz, nx, ny, nz, u2, v2);
    }

    private static void put(VertexConsumer v, Matrix4f m, Matrix3f n, float x, float y, float z, float nx, float ny, float nz, float u, float tv) {
        v.vertex(m, x, y, z).color(255, 255, 255, 235).uv(u, tv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, nx, ny, nz).endVertex();
    }
}
