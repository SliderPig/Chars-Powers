package com.example.charpowers.futuresight.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.mastery.Mastery;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

// Future Sight on the PC: ores glowing for a moment through walls, and looking through someone else's eyes.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class SightClient {
    private static final int GLOW_TICKS = 100;
    private static long[] glow = new long[0];
    private static long glowUntil;
    private static boolean borrowing;

    static {
        OwnClient.on(Mastery.FUTURE_SIGHT.toString(), SightClient::receive);
    }

    private SightClient() {}

    private static void receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (tag.contains("glow")) {
            glow = tag.getLongArray("glow");
            glowUntil = mc.level.getGameTime() + GLOW_TICKS;
        }
        if (tag.contains("eyes")) {
            if (borrowing) {
                giveBack(mc);
                return;
            }
            Entity target = mc.level.getEntity(tag.getInt("eyes"));
            if (target != null) {
                mc.setCameraEntity(target);
                borrowing = true;
            }
        }
    }

    private static void giveBack(Minecraft mc) {
        borrowing = false;
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !borrowing) return;
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (mc.player == null) borrowing = false;
        else if (camera == null || camera.isRemoved() || !camera.isAlive() || !mc.player.isAlive()) giveBack(mc);
    }

    // Soft pink cubes with bright edges, on top of everything. Drawn directly at the very end of the world: going through a render
    // type at an earlier stage showed nothing at all in the real game.
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || glow.length == 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long left = glowUntil - mc.level.getGameTime();
        if (left <= 0) {
            glow = new long[0];
            return;
        }
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.XP.rotationDegrees(e.getCamera().getXRot()));
        ps.mulPose(Axis.YP.rotationDegrees(e.getCamera().getYRot() + 180f));
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        float fade = Math.min(1f, left / 20f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder b = tess.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (long spot : glow) faces(b, m, BlockPos.of(spot), 0.22f * fade);
        tess.end();
        RenderSystem.lineWidth(2f);
        b.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (long spot : glow) edges(b, m, BlockPos.of(spot), 0.9f * fade);
        tess.end();
        RenderSystem.lineWidth(1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void corner(BufferBuilder b, Matrix4f m, float x, float y, float z, float alpha) {
        b.vertex(m, x, y, z).color(1f, 0.53f, 0.78f, alpha).endVertex();
    }

    private static void faces(BufferBuilder b, Matrix4f m, BlockPos p, float a) {
        float x0 = p.getX(), y0 = p.getY(), z0 = p.getZ(), x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        float[][] quads = {{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1},
                {x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
                {x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
        for (float[] q : quads) {
            for (int i = 0; i < 12; i += 3) corner(b, m, q[i], q[i + 1], q[i + 2], a);
        }
    }

    private static void edges(BufferBuilder b, Matrix4f m, BlockPos p, float a) {
        float x0 = p.getX(), y0 = p.getY(), z0 = p.getZ(), x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        float[][] lines = {{x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
                {x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}};
        for (float[] l : lines) {
            corner(b, m, l[0], l[1], l[2], a);
            corner(b, m, l[3], l[4], l[5], a);
        }
    }
}
