package com.example.charpowers.life.ultimine;

import com.example.charpowers.CharPowers;
import com.example.charpowers.client.Soft;
import com.example.charpowers.cloning.client.CloneClient;
import com.example.charpowers.life.model.Ultimine;
import com.example.charpowers.life.model.Ultimine.P;
import com.example.charpowers.mastery.Mastery;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.threetag.palladium.power.ability.AbilityUtil;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

// The PC side of Ultimine: hold the key, scroll to change shape, see the outline of what will break. Nothing is decided here;
// the server works out the blocks again when you break one.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class UltimineClient {
    // not one of Palladium's ability keys (1-5) or Char's other keys (J O K B N H): the grave accent key, under Escape
    public static final KeyMapping KEY = new KeyMapping("key.charpowers.ultimine", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.charpowers");

    private static int shape, sentShape = -1, max = 128;
    private static boolean sentHeld;
    private static List<P> preview = List.of();

    private UltimineClient() {}

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        private Keys() {}

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KEY);
        }
    }

    public static void setShape(int number) {
        shape = Math.floorMod(number, Ultimine.Shape.values().length);
    }

    static void setMax(int value) {
        max = Math.max(1, value);
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        return p != null && mc.screen == null && KEY.isDown() && AbilityUtil.hasPower(p, Mastery.LIFE_ENJOYER)
                && CloneClient.masteryLevel(Mastery.LIFE_ENJOYER + "#awakened") != null;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        boolean held = active();
        if (held != sentHeld || (held && shape != sentShape)) {
            UltimineServer.CHANNEL.sendToServer(new UltimineServer.State(held, shape));
            sentHeld = held;
            sentShape = shape;
        }
        preview = held ? work(Minecraft.getInstance()) : List.of();
    }

    // Mouse wheel while holding the key changes the shape instead of the hotbar slot.
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!active()) return;
        e.setCanceled(true);
        int count = Ultimine.Shape.values().length;
        shape = Math.floorMod(shape + (e.getScrollDelta() > 0 ? -1 : 1), count);
    }

    private static List<P> work(Minecraft mc) {
        if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return List.of();
        BlockPos o = hit.getBlockPos();
        BlockState first = mc.level.getBlockState(o);
        return Ultimine.cells(Ultimine.Shape.of(shape), new P(o.getX(), o.getY(), o.getZ()), hit.getDirection().get3DDataValue(),
                mc.player.getDirection().get3DDataValue(), mc.player.blockPosition().getY(), max,
                c -> breakable(mc, c), c -> same(mc, first, c));
    }

    private static boolean breakable(Minecraft mc, P c) {
        BlockPos pos = new BlockPos(c.x(), c.y(), c.z());
        BlockState s = mc.level.getBlockState(pos);
        if (s.isAir() || s.getBlock() instanceof LiquidBlock || s.getDestroySpeed(mc.level, pos) < 0 || s.hasBlockEntity()) return false;
        return !s.requiresCorrectToolForDrops() || mc.player.hasCorrectToolForDrops(s);
    }

    private static boolean same(Minecraft mc, BlockState first, P c) {
        BlockState s = mc.level.getBlockState(new BlockPos(c.x(), c.y(), c.z()));
        if (s.is(first.getBlock())) return true;
        if (s.is(BlockTags.LOGS) && first.is(BlockTags.LOGS)) return true;
        if (s.is(Tags.Blocks.ORES) && first.is(Tags.Blocks.ORES)) {
            var a = ForgeRegistries.BLOCKS.getKey(s.getBlock());
            var b = ForgeRegistries.BLOCKS.getKey(first.getBlock());
            return a != null && b != null && com.example.charpowers.cloning.model.OreRules.family(a.getPath())
                    .equals(com.example.charpowers.cloning.model.OreRules.family(b.getPath()));
        }
        return false;
    }

    // Pink outline round every block that is about to go. Drawn straight at the end of the world, the same way Future Sight does.
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || preview.isEmpty()) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.XP.rotationDegrees(e.getCamera().getXRot()));
        ps.mulPose(Axis.YP.rotationDegrees(e.getCamera().getYRot() + 180f));
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
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
        for (P c : preview) faces(b, m, c, 0.16f);
        tess.end();
        RenderSystem.lineWidth(2f);
        b.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (P c : preview) edges(b, m, c, 0.95f);
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

    private static void faces(BufferBuilder b, Matrix4f m, P p, float a) {
        float x0 = p.x() - 0.002f, y0 = p.y() - 0.002f, z0 = p.z() - 0.002f, x1 = p.x() + 1.002f, y1 = p.y() + 1.002f, z1 = p.z() + 1.002f;
        float[][] quads = {{x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1}, {x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1},
                {x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0}, {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
                {x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1}, {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}};
        for (float[] q : quads) {
            for (int i = 0; i < 12; i += 3) corner(b, m, q[i], q[i + 1], q[i + 2], a);
        }
    }

    private static void edges(BufferBuilder b, Matrix4f m, P p, float a) {
        float x0 = p.x(), y0 = p.y(), z0 = p.z(), x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        float[][] lines = {{x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
                {x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}};
        for (float[] l : lines) {
            corner(b, m, l[0], l[1], l[2], a);
            corner(b, m, l[3], l[4], l[5], a);
        }
    }

    // A small rounded tag under the crosshair: the shape and how many blocks it will take.
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post e) {
        if (e.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type() || !active()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        String count = preview.isEmpty() ? "" : "  " + preview.size();
        String text = Component.translatable("charpowers.ultimine.shape." + Ultimine.Shape.of(shape).id).getString() + count;
        float w = com.example.charpowers.client.ui.Ui.width(text, com.example.charpowers.client.ui.Ui.S) + 16, h = 15;
        float x = e.getWindow().getGuiScaledWidth() / 2f - w / 2f, y = e.getWindow().getGuiScaledHeight() / 2f + 14;
        g.drawManaged(() -> {
            Soft.shadow(g, x, y + 1, x + w, y + h + 1, h / 2f, 4, 0x50000000, false);
            Soft.round(g, x, y, x + w, y + h, h / 2f, 0xE0351C3C, 0xE02A1530);
            Soft.ring(g, x, y, x + w, y + h, h / 2f, 1f, 0xD0FF88C8, 0x90FF88C8);
            com.example.charpowers.client.ui.Ui.text(g, text, x + 8, com.example.charpowers.client.ui.Ui.middle(y + h / 2f, com.example.charpowers.client.ui.Ui.S), 0xFFFFD6EC, com.example.charpowers.client.ui.Ui.S);
        });
    }
}
