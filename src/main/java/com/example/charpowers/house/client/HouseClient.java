package com.example.charpowers.house.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.client.OwnClient;
import com.example.charpowers.client.Soft;
import com.example.charpowers.house.HouseSite;
import com.example.charpowers.house.model.HouseTurn;
import com.example.charpowers.mastery.Mastery;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

// The player's side of House Builder: the picker opens from the server's message, then a ghost of the plot follows where you look.
// Scroll turns it, click builds, crouch cancels. The ghost goes red and says why if the server would refuse.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class HouseClient {
    private static final int RED = 0xFFFF7A7A, PINK = 0xFFFFA8D8, TEXT = 0xFFF6E8F4, MUTED = 0xFFC9B2CB, BACK = 0xE6241826;

    private static String id, name;
    private static int w, h, d, turns;
    private static boolean active, placed, ok;
    private static int cx, cz, around, floor, sinceClick, sinceCheck;
    private static int checkedX, checkedZ, checkedTurns;
    private static String why;

    static {
        OwnClient.on(Mastery.HOUSE_BUILDER.toString(), HouseClient::receive);
    }

    private HouseClient() {}

    private static void receive(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (tag.getBoolean("built")) {
            active = false;
        } else if (tag.getBoolean("open")) {
            if (active) {
                active = false;
            } else {
                mc.setScreen(new HousePickerScreen(tag));
            }
        }
    }

    // The picker chose: follow the player's eyes.
    public static void start(String houseId, String houseName, int width, int height, int depth) {
        id = houseId;
        name = houseName;
        w = width;
        h = height;
        d = depth;
        active = true;
        placed = false;
        sinceClick = 0;
        checkedX = Integer.MIN_VALUE;
        Minecraft mc = Minecraft.getInstance();
        turns = mc.player == null ? 0 : HouseTurn.facingPlayer(mc.player.getDirection().get2DDataValue());
    }

    public static boolean active() {
        return active;
    }

    // For the walkthrough in the real game.
    public static void pressConfirmForCheck() {
        confirm();
    }

    public static void stopForCheck() {
        active = false;
    }

    public static void turnForCheck() {
        turns = (turns + 1) & 3;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen instanceof PauseScreen) {
            active = false;
            return;
        }
        if (mc.screen != null) return;
        if (mc.options.keyShift.isDown()) {
            active = false;
            mc.player.displayClientMessage(Component.literal("House cancelled.").withStyle(st -> st.withColor(0xFFA8D8).withItalic(true)), true);
            return;
        }
        sinceClick++;
        HitResult hit = mc.player.pick(64, 1f, false);
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos at = block.getBlockPos();
            cx = at.getX();
            cz = at.getZ();
            around = at.getY();
            placed = true;
        } else {
            placed = false;
            return;
        }
        if (checkedX != cx || checkedZ != cz || checkedTurns != turns || ++sinceCheck >= 20) {
            sinceCheck = 0;
            checkedX = cx;
            checkedZ = cz;
            checkedTurns = turns;
            HouseSite.Result r = HouseSite.check(mc.level, cx, cz, around, w, h, d, turns, true);
            ok = r.ok();
            why = r.why();
            floor = r.y();
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!active || Minecraft.getInstance().screen != null) return;
        turns = (turns + (e.getScrollDelta() > 0 ? 1 : 3)) & 3;
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!active || Minecraft.getInstance().screen != null) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        if (e.isUseItem() || e.isAttack()) confirm();
    }

    private static void confirm() {
        if (!placed || sinceClick < 8) return;
        sinceClick = 0;
        Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.HOUSE_BUILDER.toString(), "build:" + id + ":" + cx + ":" + around + ":" + cz + ":" + turns));
    }

    // ---- the ghost: a soft sheet on the ground, thin lines up to the roof, a bright mark where the front door will be

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !active || !placed) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.XP.rotationDegrees(e.getCamera().getXRot()));
        ps.mulPose(Axis.YP.rotationDegrees(e.getCamera().getYRot() + 180f));
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        int[] rect = HouseTurn.rect(w, d, cx, cz, turns);
        float x0 = rect[0], z0 = rect[1], x1 = rect[2] + 1, z1 = rect[3] + 1, y0 = floor + 1.05f, y1 = floor + h;
        float r = ok ? 1f : 1f, g = ok ? 0.55f : 0.25f, b = ok ? 0.82f : 0.25f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(buf, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, 0.34f);
        // the front edge and a chevron pointing out of the door
        Direction front = Direction.from2DDataValue(turns);
        float mx = HouseTurn.worldX(w / 2, d - 1, w, d, cx, turns) + 0.5f, mz = HouseTurn.worldZ(w / 2, d - 1, w, d, cz, turns) + 0.5f;
        float fx = front.getStepX(), fz = front.getStepZ(), sx = -fz, sz = fx;
        float tipx = mx + fx * 3.2f, tipz = mz + fz * 3.2f;
        float bx = mx + fx * 1.2f, bz = mz + fz * 1.2f;
        quad(buf, m, bx + sx * 1.6f, y0, bz + sz * 1.6f, tipx, y0, tipz, tipx, y0, tipz, bx - sx * 1.6f, y0, bz - sz * 1.6f, r, g, b, 0.75f);
        tess.end();
        RenderSystem.lineWidth(2f);
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        float[][] corners = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}};
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i], c = corners[(i + 1) % 4];
            line(buf, m, a[0], y0, a[1], c[0], y0, c[1], r, g, b, 0.95f);
            line(buf, m, a[0], y1, a[1], c[0], y1, c[1], r, g, b, 0.45f);
            line(buf, m, a[0], y0, a[1], a[0], y1, a[1], r, g, b, 0.7f);
        }
        tess.end();
        RenderSystem.lineWidth(1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void quad(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x2, y2, z2).color(r, g, bl, a).endVertex();
        b.vertex(m, x3, y3, z3).color(r, g, bl, a).endVertex();
        b.vertex(m, x4, y4, z4).color(r, g, bl, a).endVertex();
    }

    private static void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x2, y2, z2).color(r, g, bl, a).endVertex();
    }

    // ---- the little pill at the top of the screen

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        GuiGraphics gr = e.getGuiGraphics();
        double gui = mc.getWindow().getGuiScale();
        String hint = "Scroll: turn    Click: build    Crouch: cancel";
        String low = !placed ? "Look at the ground" : ok ? hint : why;
        int colour = !placed || ok ? MUTED : RED;
        var font = mc.font;
        int sw = mc.getWindow().getWidth();
        // the normal Minecraft font at whole real pixels, smaller only if a long reason wouldn't fit
        int scale = Math.max(1, (int) Math.round(gui));
        while (scale > 1 && Math.max(font.width(name), font.width(low)) * scale + 40 * scale > sw) scale--;
        int k = scale;
        int width = Math.max(font.width(name), font.width(low)) * k + 24 * k;
        int height = 30 * k;
        int x0 = (sw - width) / 2, y0 = 8 * k;
        gr.pose().pushPose();
        gr.pose().scale((float) (1 / gui), (float) (1 / gui), 1);
        gr.drawManaged(() -> {
            Soft.fill(gr, x0, y0, x0 + width, y0 + height, height / 2, BACK);
            Soft.outline(gr, x0, y0, x0 + width, y0 + height, height / 2, 0x66FFA8D8);
            text(gr, name, sw / 2, y0 + 5 * k, k, PINK);
            text(gr, low, sw / 2, y0 + 17 * k, k, colour);
        });
        gr.pose().popPose();
    }

    private static void text(GuiGraphics gr, String s, int x, int y, int k, int colour) {
        var font = Minecraft.getInstance().font;
        gr.pose().pushPose();
        gr.pose().translate(x - font.width(s) * k / 2, y, 0);
        gr.pose().scale(k, k, 1);
        gr.drawString(font, s, 0, 0, colour, false);
        gr.pose().popPose();
    }
}
