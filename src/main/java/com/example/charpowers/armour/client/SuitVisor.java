package com.example.charpowers.armour.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitFlight;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.armour.model.SuitMotion;
import com.example.charpowers.armour.model.SuitRules;
import com.example.charpowers.client.Soft;
import com.example.charpowers.client.ui.Ui;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// The suit's visor: while you wear the whole suit and look out of your own eyes, a see-through display in lime and silver. Everything sits at
// the edges (suit on the left, target and threats on the right, a compass along the top) so the middle, the hotbar and the chat stay clear.
// The readouts are worked out a few times a second and cached; drawing them each frame is just a handful of shapes and words.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class SuitVisor {
    private static final int LIME = 0xFF96FF1E, GLOW = 0xFFD6FF96, SILVER = 0xFFB8C2CA, TEXT = 0xFFF4F8FA, RED = 0xFFFF5A5A, PANEL = 0x7A12161A, DIM = 0xFFA3AEB7;
    private static final float TOP_ROW = 24;

    private record Threat(String name, double distance, double bearing, float health, float max) {}

    // what the readouts say, refreshed a few times a second
    private static double battery, speed, stationDistance, stationBearing = Double.NaN;
    private static int level, armour, air, maxAir = 300;
    private static SuitRules.Stage stage = SuitRules.Stage.FINE;
    private static List<String> installed = List.of(), working = List.of();
    private static boolean slim, wearing;
    private static final List<Threat> THREATS = new ArrayList<>();
    private static Vec3 last = Vec3.ZERO;
    private static LivingEntity target;
    private static int ticks, bootSince = 1000;
    private static String stationDim = "";
    private static long stationPos;

    private SuitVisor() {}

    public static void station(String dimension, long pos) {
        stationDim = dimension;
        stationPos = pos;
    }

    // Whether the visor is being drawn: the whole suit on, first person, not hidden with F1, and not switched off.
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return wearing && mc.options.getCameraType() == CameraType.FIRST_PERSON && !mc.options.hideGui && VisorSettings.has("visor");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        boolean now = p != null && SuitItems.fullSuit(p);
        if (now && !wearing) {
            bootSince = 0;
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(com.example.charpowers.armour.SuitSounds.SERVO.get(), 0.7f, 0.9f));
        }
        wearing = now;
        if (!now || mc.level == null) return;
        ticks++;
        if (bootSince < 1000) bootSince++;
        if (ticks % 2 == 0) {
            Vec3 pos = p.position();
            speed = Mth.lerp(0.4, speed, Math.hypot(pos.x - last.x, pos.z - last.z) * 10);
            last = pos;
        }
        if (ticks % 2 == 0) {
            // what you are looking at, up to 32 blocks away and not through walls
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
            double reach = p.pick(32, 0f, false).getLocation().distanceTo(eye);
            Vec3 end = eye.add(look.scale(reach));
            var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1), x -> x instanceof LivingEntity && !x.isSpectator() && x.isPickable(), reach * reach);
            target = hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
        }
        if (ticks % 5 != 0) return;
        ItemStack suit = p.getItemBySlot(EquipmentSlot.CHEST);
        battery = SuitItems.battery(suit);
        level = Mastery.levelAnySide(p, Mastery.INVINCIBLE_ARMOUR);
        stage = SuitRules.stage(SuitItems.worn(suit), level);
        installed = SuitItems.modulesOn(suit);
        working = battery > 0 ? SuitItems.modulesWorn(p, SuitRules.moduleSlots(level)) : List.of();
        slim = SuitItems.slim(suit);
        armour = p.getArmorValue();
        air = p.getAirSupply();
        maxAir = p.getMaxAirSupply();
        stationBearing = Double.NaN;
        if (!stationDim.isEmpty() && stationDim.equals(mc.level.dimension().location().toString())) {
            BlockPos s = BlockPos.of(stationPos);
            double dx = s.getX() + 0.5 - p.getX(), dz = s.getZ() + 0.5 - p.getZ();
            stationDistance = Math.hypot(dx, dz);
            stationBearing = Math.toDegrees(Math.atan2(dx, -dz));
        }
        if (ticks % 10 == 0) {
            THREATS.clear();
            List<LivingEntity> near = mc.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), x -> x instanceof Enemy && x.isAlive());
            near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
            for (LivingEntity x : near) {
                if (THREATS.size() >= 6) break;
                THREATS.add(new Threat(x.getName().getString(), x.distanceTo(p), Math.toDegrees(Math.atan2(x.getX() - p.getX(), -(x.getZ() - p.getZ()))), x.getHealth(), x.getMaxHealth()));
            }
        }
    }

    private static float boot(int delay) {
        return Mth.clamp((bootSince - delay) / 8f, 0f, 1f);
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) return;
        GuiGraphics g = e.getGuiGraphics();
        float partial = e.getPartialTick();
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON && !mc.options.hideGui) closing(g, mc, partial);
        if (!active()) return;
        g.drawManaged(() -> draw(g, mc, partial));
    }

    // ---- the visor itself

    private static void draw(GuiGraphics g, Minecraft mc, float partial) {
        Player p = mc.player;
        float w = g.guiWidth(), h = g.guiHeight();
        if (VisorSettings.has("frame")) frame(g, w, h, boot(0));
        if (VisorSettings.has("armour")) hitFlash(g, p, w, h);
        if (VisorSettings.has("heading")) heading(g, p, w, boot(4));
        float lx = 8, ly = TOP_ROW + 6;
        float a = boot(8);
        if (VisorSettings.has("battery")) ly = battery(g, lx, ly, a);
        if (VisorSettings.has("modules")) ly = modules(g, lx, ly, a);
        if (VisorSettings.has("state")) ly = state(g, lx, ly, a);
        if (VisorSettings.has("station")) ly = station(g, p, lx, ly, a);
        if (VisorSettings.has("mastery")) ly = mastery(g, lx, ly, boot(12));
        if (VisorSettings.has("quest")) ly = quest(g, lx, ly, boot(12));
        if (VisorSettings.has("speed")) ly = speed(g, p, lx, ly, boot(14));
        if (VisorSettings.has("clock")) clock(g, mc, p, lx, ly, boot(14));
        float rx = w - 8, ry = TOP_ROW + 14;
        if (VisorSettings.has("target")) ry = target(g, mc, rx, ry, boot(10));
        if (VisorSettings.has("threats")) ry = threats(g, rx, ry, boot(12));
        if (VisorSettings.has("armour")) ry = armourRow(g, p, rx, ry, boot(14));
        if (VisorSettings.has("oxygen")) oxygen(g, p, rx, ry, boot(14));
        // the start-up sweep: a line of light going down the screen
        if (bootSince < 16) {
            float y = h * bootSince / 16f;
            g.fillGradient(0, (int) Math.max(0, y - 14), (int) w, (int) y, 0x00FFFFFF & LIME, 0x4096FF1E);
            g.fill(0, (int) y, (int) w, (int) y + 1, 0xA0D6FF96);
        }
    }

    private static void frame(GuiGraphics g, float w, float h, float a) {
        int edge = Soft.alpha(0x60080A0C, a);
        for (int i = 0; i < 14; i++) {
            int c = Soft.alpha(edge, 1f - i / 14f);
            g.fill(0, i * 2, (int) w, i * 2 + 2, Soft.alpha(c, 0.8f));
            g.fill(0, (int) h - i * 2 - 2, (int) w, (int) h - i * 2, Soft.alpha(c, 0.8f));
            g.fill(i * 2, 0, i * 2 + 2, (int) h, Soft.alpha(c, 0.8f));
            g.fill((int) w - i * 2 - 2, 0, (int) w - i * 2, (int) h, Soft.alpha(c, 0.8f));
        }
        Soft.ring(g, 3, 3, w - 3, h - 3, 44, 1f, Soft.alpha(LIME, 0.22f * a), Soft.alpha(SILVER, 0.12f * a));
        // little corner marks
        for (int sx = 0; sx < 2; sx++) {
            for (int sy = 0; sy < 2; sy++) {
                float cx = sx == 0 ? 14 : w - 14, cy = sy == 0 ? 14 : h - 14;
                float dx = sx == 0 ? 1 : -1, dy = sy == 0 ? 1 : -1;
                Soft.line(g, cx, cy, cx + dx * 9, cy, 1f, Soft.alpha(LIME, 0.5f * a));
                Soft.line(g, cx, cy, cx, cy + dy * 9, 1f, Soft.alpha(LIME, 0.5f * a));
            }
        }
    }

    // A red wash on the side you were hit from.
    private static void hitFlash(GuiGraphics g, Player p, float w, float h) {
        if (p.hurtTime <= 0 || p.hurtDuration <= 0) return;
        float strength = p.hurtTime / (float) p.hurtDuration;
        double rel = Math.toRadians(p.getHurtDir() - 90);
        float dx = (float) Math.sin(rel), dy = (float) -Math.cos(rel);
        int c = Soft.alpha(RED, 0.55f * strength);
        int far = Soft.alpha(RED, 0f);
        float depth = 34;
        if (Math.abs(dx) > 0.35f) {
            for (int i = 0; i < 12; i++) {
                int col = Soft.alpha(c, 1f - i / 12f);
                if (dx > 0) g.fill((int) (w - (i + 1) * depth / 12), 0, (int) (w - i * depth / 12), (int) h, col);
                else g.fill((int) (i * depth / 12), 0, (int) ((i + 1) * depth / 12), (int) h, col);
            }
        }
        if (Math.abs(dy) > 0.35f) {
            if (dy < 0) g.fillGradient(0, 0, (int) w, (int) depth, c, far);
            else g.fillGradient(0, (int) (h - depth), (int) w, (int) h, far, c);
        }
    }

    // ---- top: the compass strip, with marks for what the scanner module sees

    private static void heading(GuiGraphics g, Player p, float w, float a) {
        if (a <= 0) return;
        float cx = w / 2f, half = 78, top = 4, hh = 13;
        double yaw = Mth.wrapDegrees(p.getYRot());
        double compass = Mth.positiveModulo(yaw + 180, 360);
        Soft.round(g, cx - half, top, cx + half, top + hh, 6, Soft.alpha(PANEL, a));
        g.enableScissor((int) (cx - half + 3), (int) top, (int) (cx + half - 3), (int) (top + hh + 6));
        for (int d = -90; d <= 90; d += 5) {
            double bearing = Math.floor((compass + d) / 5) * 5;
            double off = bearing - compass;
            if (Math.abs(off) > 85) continue;
            float x = cx + (float) off * half / 85f;
            int b = (int) Mth.positiveModulo(bearing, 360);
            boolean major = b % 45 == 0, mid = b % 15 == 0;
            float len = major ? 5 : mid ? 3.5f : 2f;
            Soft.line(g, x, top + hh - 2, x, top + hh - 2 - len, 1f, Soft.alpha(major ? TEXT : SILVER, a * (major ? 0.9f : 0.5f)));
            if (major) {
                String s = switch (b) {
                    case 0 -> "N";
                    case 45 -> "NE";
                    case 90 -> "E";
                    case 135 -> "SE";
                    case 180 -> "S";
                    case 225 -> "SW";
                    case 270 -> "W";
                    default -> "NW";
                };
                Ui.centre(g, s, x, Ui.middle(top + 4.5f, Ui.XS), b % 90 == 0 ? LIME : SILVER, Ui.XS);
            }
        }
        g.disableScissor();
        // the pointer and what the scanner sees
        Soft.polygon(g, new float[] {cx - 3, cx + 3, cx}, new float[] {top + hh + 1, top + hh + 1, top + hh - 3}, 3, Soft.alpha(LIME, a));
        if (VisorSettings.has("scanner") && working.contains("scanner")) {
            for (Threat t : THREATS) {
                double off = Mth.wrapDegrees(t.bearing - compass);
                if (Math.abs(off) > 85) continue;
                float x = cx + (float) off * half / 85f;
                Soft.polygon(g, new float[] {x - 2.5f, x + 2.5f, x}, new float[] {top + hh + 2, top + hh + 2, top + hh + 6.5f}, 3, Soft.alpha(RED, a));
            }
        }
        if (!Double.isNaN(stationBearing) && VisorSettings.has("station")) {
            double off = Mth.wrapDegrees(stationBearing - compass);
            if (Math.abs(off) <= 85) {
                float x = cx + (float) off * half / 85f;
                Soft.disc(g, x, top + hh + 4, 2f, Soft.alpha(LIME, a * 0.8f));
            }
        }
    }

    // ---- left: the suit

    private static float battery(GuiGraphics g, float x, float y, float a) {
        if (a <= 0) return y;
        float w = 100, h = 22;
        boolean low = battery <= 15;
        float pulse = low ? 0.55f + 0.45f * (float) Math.sin(ticks * 0.5) : 1f;
        Soft.round(g, x, y, x + w, y + h, 8, Soft.alpha(PANEL, a));
        Soft.ring(g, x, y, x + w, y + h, 8, 1f, Soft.alpha(low ? RED : LIME, 0.3f * a * pulse), Soft.alpha(SILVER, 0.12f * a));
        Ui.text(g, "BATTERY", x + 8, y + 4, Soft.alpha(SILVER, a), Ui.XS);
        Ui.right(g, Math.round(battery) + "%", x + w - 8, y + 4, Soft.alpha(low ? RED : TEXT, a), Ui.XS);
        float bx = x + 8, bw = w - 16, by = y + 14;
        Soft.round(g, bx, by, bx + bw, by + 3, 1.5f, Soft.alpha(0x80000000, a));
        float fill = (float) (bw * Mth.clamp(battery / 100, 0, 1));
        if (fill > 1) Soft.round(g, bx, by, bx + Math.max(fill, 3), by + 3, 1.5f, Soft.alpha(low ? RED : LIME, a * (low ? pulse : 1f)));
        return y + h + 4;
    }

    private static float modules(GuiGraphics g, float x, float y, float a) {
        if (a <= 0) return y;
        List<String> all = List.of("shield", "scanner", "strength", "speed");
        float d = 20, mx = x;
        boolean any = false;
        for (String m : all) {
            if (!installed.contains(m)) continue;
            any = true;
            boolean on = working.contains(m);
            Soft.round(g, mx, y, mx + d, y + d, d / 2f, Soft.alpha(PANEL, a));
            Soft.ring(g, mx, y, mx + d, y + d, d / 2f, 1f, Soft.alpha(on ? LIME : DIM, (on ? 0.7f : 0.25f) * a), Soft.alpha(SILVER, 0.1f * a));
            ItemStack icon = new ItemStack(switch (m) {
                case "shield" -> SuitItems.SHIELD.get();
                case "scanner" -> SuitItems.SCANNER.get();
                case "strength" -> SuitItems.STRENGTH.get();
                default -> SuitItems.SPEED.get();
            });
            g.pose().pushPose();
            g.pose().translate(mx + 2.5f, y + 2.5f, 0);
            g.pose().scale(0.9f, 0.9f, 1f);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();
            if (!on) Soft.disc(g, mx + d / 2f, y + d / 2f, d / 2f - 1, Soft.alpha(0xFF0A0D10, 0.6f * a));
            mx += d + 3;
        }
        if (slim) {
            Soft.round(g, mx, y, mx + 26, y + d, d / 2f, Soft.alpha(PANEL, a));
            Ui.centre(g, "SLIM", mx + 13, Ui.middle(y + d / 2f, Ui.XS), Soft.alpha(LIME, a), Ui.XS);
            any = true;
        }
        if (!any) {
            float nw = Ui.width("no modules", Ui.XS) + 16;
            Soft.round(g, x, y + 3, x + nw, y + d - 3, 7, Soft.alpha(PANEL, a));
            Ui.text(g, "no modules", x + 8, Ui.middle(y + d / 2f, Ui.XS), Soft.alpha(DIM, a), Ui.XS);
        }
        return y + d + 4;
    }

    private static float state(GuiGraphics g, float x, float y, float a) {
        if (a <= 0) return y;
        boolean crazy = stage != SuitRules.Stage.FINE;
        float pulse = crazy ? 0.5f + 0.5f * (float) Math.sin(ticks * 0.45) : 0f;
        String s = crazy ? "GOING CRAZY" : "SUIT NORMAL";
        float w = Ui.width(s, Ui.XS) + 20, h = 14;
        Soft.round(g, x, y, x + w, y + h, 7, Soft.alpha(crazy ? Soft.mix(PANEL, 0xFF5A1010, 0.4f + 0.4f * pulse) : PANEL, a));
        Soft.disc(g, x + 8, y + h / 2f, 2.2f, Soft.alpha(crazy ? RED : LIME, a * (crazy ? 0.6f + 0.4f * pulse : 1f)));
        Ui.text(g, s, x + 14, Ui.middle(y + h / 2f, Ui.XS), Soft.alpha(crazy ? RED : SILVER, a), Ui.XS);
        return y + h + 4;
    }

    // Points to the charging station when it is far away or the battery is low.
    private static float station(GuiGraphics g, Player p, float x, float y, float a) {
        if (a <= 0 || Double.isNaN(stationBearing)) return y;
        if (stationDistance < 64 && battery > 25) return y;
        double rel = Math.toRadians(stationBearing - Mth.wrapDegrees(p.getYRot()) - 180);
        float dx = (float) Math.sin(rel), dy = (float) -Math.cos(rel);
        String s = "STATION " + Math.round(stationDistance) + "m";
        float w = Ui.width(s, Ui.XS) + 30, h = 18;
        Soft.round(g, x, y, x + w, y + h, 9, Soft.alpha(PANEL, a));
        Soft.chevron(g, x + 10, y + h / 2f, 4.5f, dx, dy, 1.4f, Soft.alpha(LIME, a));
        Ui.text(g, s, x + 22, Ui.middle(y + h / 2f, Ui.XS), Soft.alpha(TEXT, a), Ui.XS);
        return y + h + 4;
    }

    private static final int[] UNLOCKS = {25, 50, 75, 100};

    private static float mastery(GuiGraphics g, float x, float y, float a) {
        if (a <= 0) return y;
        int next = 100, prev = 1;
        for (int u : UNLOCKS) {
            if (level < u) {
                next = u;
                break;
            }
            prev = u;
        }
        boolean max = level >= 100;
        float w = 118, h = 22;
        Soft.round(g, x, y, x + w, y + h, 8, Soft.alpha(PANEL, a));
        Ui.text(g, "MASTERY " + level, x + 8, y + 4, Soft.alpha(SILVER, a), Ui.XS);
        Ui.right(g, max ? "MAX" : "next " + next, x + w - 8, y + 4, Soft.alpha(DIM, a), Ui.XS);
        float bx = x + 8, bw = w - 16, by = y + 14;
        Soft.round(g, bx, by, bx + bw, by + 3, 1.5f, Soft.alpha(0x80000000, a));
        float part = max ? 1f : (level - prev) / (float) (next - prev);
        if (part > 0.02f) Soft.round(g, bx, by, bx + Math.max(bw * part, 3), by + 3, 1.5f, Soft.alpha(GLOW, a * 0.9f));
        return y + h + 4;
    }

    private static float quest(GuiGraphics g, float x, float y, float a) {
        if (a <= 0 || level < 100) return y;
        String s = "QUEST: LIVE THROUGH A NUKE";
        float w = Ui.width(s, Ui.XS) + 16, h = 14;
        Soft.round(g, x, y, x + w, y + h, 7, Soft.alpha(PANEL, a));
        Ui.text(g, s, x + 8, Ui.middle(y + h / 2f, Ui.XS), Soft.alpha(GLOW, a), Ui.XS);
        return y + h + 4;
    }

    private static float speed(GuiGraphics g, Player p, float x, float y, float a) {
        if (a <= 0) return y;
        String s = String.format("%.1f b/s   Y %d", speed, (int) Math.floor(p.getY()));
        float w = Ui.width(s, Ui.XS) + 16, h = 14;
        Soft.round(g, x, y, x + w, y + h, 7, Soft.alpha(PANEL, a));
        Ui.text(g, s, x + 8, Ui.middle(y + h / 2f, Ui.XS), Soft.alpha(SILVER, a), Ui.XS);
        return y + h + 4;
    }

    private static void clock(GuiGraphics g, Minecraft mc, Player p, float x, float y, float a) {
        if (a <= 0) return;
        long time = (mc.level.getDayTime() + 6000) % 24000;
        String s = String.format("%02d:%02d   %d %d %d", time / 1000, (int) (time % 1000 * 60 / 1000), (int) p.getX(), (int) p.getY(), (int) p.getZ());
        float w = Ui.width(s, Ui.XS) + 16, h = 14;
        Soft.round(g, x, y, x + w, y + h, 7, Soft.alpha(PANEL, a));
        Ui.text(g, s, x + 8, Ui.middle(y + h / 2f, Ui.XS), Soft.alpha(DIM, a), Ui.XS);
    }

    // ---- right: what you look at, what is near, how you are doing

    private static float target(GuiGraphics g, Minecraft mc, float right, float y, float a) {
        if (a <= 0 || target == null || !target.isAlive()) return y;
        LivingEntity t = target;
        float w = 112, h = 30, x = right - w;
        Soft.round(g, x, y, x + w, y + h, 8, Soft.alpha(PANEL, a));
        Soft.ring(g, x, y, x + w, y + h, 8, 1f, Soft.alpha(t instanceof Enemy ? RED : LIME, 0.35f * a), Soft.alpha(SILVER, 0.1f * a));
        String name = t.getName().getString();
        Ui.text(g, name, x + 8, y + 4, Soft.alpha(TEXT, a), Ui.fit(name, w - 16, Ui.XS, Ui.TINY));
        float part = Mth.clamp(t.getHealth() / Math.max(1f, t.getMaxHealth()), 0f, 1f);
        Soft.round(g, x + 8, y + 15, x + w - 8, y + 18, 1.5f, Soft.alpha(0x80000000, a));
        if (part > 0.01f) Soft.round(g, x + 8, y + 15, x + 8 + Math.max(3, (w - 16) * part), y + 18, 1.5f, Soft.alpha(RED, a));
        Ui.text(g, Math.round(t.getHealth()) + " / " + Math.round(t.getMaxHealth()), x + 8, y + 20, Soft.alpha(SILVER, a), Ui.XS);
        Ui.right(g, Math.round(t.distanceTo(mc.player)) + "m", x + w - 8, y + 20, Soft.alpha(DIM, a), Ui.XS);
        return y + h + 4;
    }

    private static float threats(GuiGraphics g, float right, float y, float a) {
        if (a <= 0 || THREATS.isEmpty()) return y;
        int n = Math.min(3, THREATS.size());
        float w = 112, h = 8 + n * 12 + 2, x = right - w;
        Soft.round(g, x, y, x + w, y + h, 8, Soft.alpha(PANEL, a));
        for (int i = 0; i < n; i++) {
            Threat t = THREATS.get(i);
            float ry = y + 6 + i * 12;
            Soft.disc(g, x + 9, ry + 4.5f, 2f, Soft.alpha(RED, a));
            String dist = Math.round(t.distance) + "m";
            Ui.right(g, dist, x + w - 8, ry, Soft.alpha(SILVER, a), Ui.XS);
            Ui.text(g, t.name, x + 15, ry, Soft.alpha(TEXT, a), Ui.fit(t.name, w - 30 - Ui.width(dist, Ui.XS), Ui.XS, Ui.TINY));
        }
        return y + h + 4;
    }

    private static float armourRow(GuiGraphics g, Player p, float right, float y, float a) {
        if (a <= 0) return y;
        float w = 112, h = 22, x = right - w;
        Soft.round(g, x, y, x + w, y + h, 8, Soft.alpha(PANEL, a));
        Ui.text(g, "ARMOUR " + armour, x + 8, y + 4, Soft.alpha(SILVER, a), Ui.XS);
        Ui.right(g, Math.round(p.getHealth()) + " HP", x + w - 8, y + 4, Soft.alpha(TEXT, a), Ui.XS);
        float part = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
        Soft.round(g, x + 8, y + 14, x + w - 8, y + 17, 1.5f, Soft.alpha(0x80000000, a));
        if (part > 0.01f) Soft.round(g, x + 8, y + 14, x + 8 + Math.max(3, (w - 16) * part), y + 17, 1.5f, Soft.alpha(part < 0.3f ? RED : LIME, a));
        return y + h + 4;
    }

    private static void oxygen(GuiGraphics g, Player p, float right, float y, float a) {
        if (a <= 0 || !p.isUnderWater()) return;
        float w = 112, h = 18, x = right - w;
        Soft.round(g, x, y, x + w, y + h, 8, Soft.alpha(PANEL, a));
        Ui.text(g, "OXYGEN", x + 8, y + 3, Soft.alpha(SILVER, a), Ui.XS);
        float part = Mth.clamp(air / (float) Math.max(1, maxAir), 0f, 1f);
        Soft.round(g, x + 8, y + 12, x + w - 8, y + 14.5f, 1.2f, Soft.alpha(0x80000000, a));
        if (part > 0.01f) Soft.round(g, x + 8, y + 12, x + 8 + Math.max(3, (w - 16) * part), y + 14.5f, 1.2f, Soft.alpha(part < 0.25f ? RED : 0xFF6FD6FF, a));
    }

    // ---- the suit closing round you, seen from inside (first person): plates slide in at the sides, the helmet's visor comes down

    private static void closing(GuiGraphics g, Minecraft mc, float partial) {
        SuitFlight own = null;
        for (SuitFlight f : mc.level.getEntitiesOfClass(SuitFlight.class, mc.player.getBoundingBox().inflate(8))) {
            if (f.who() == mc.player.getId()) {
                own = f;
                break;
            }
        }
        float w = g.guiWidth(), h = g.guiHeight();
        float plates = 0, visor = 0;
        if (own != null) {
            double t = own.timeline(partial);
            plates = (float) Mth.clamp(SuitMotion.glide(t) * 1.15 - SuitMotion.glide(t) * SuitMotion.glide(t) * 0.0, 0, 1);
            // the plates draw in as the suit glides over you and slide away when the helmet lands
            double away = Mth.clamp((t - SuitMotion.LAND) / 6.0, 0, 1);
            plates *= (float) (1 - away);
            visor = (float) Mth.clamp(1 - SuitMotion.helmet(t) / SuitMotion.HELMET_HOVER, 0, 1);
            if (t < SuitMotion.CLOSE_END) visor = 0;
            visor *= (float) (1 - Mth.clamp((t - SuitMotion.TOTAL + 2) / 4.0, 0, 1) * 0);
        } else if (wearing && bootSince < 10) {
            visor = 1 - bootSince / 10f;
        }
        if (plates <= 0.01f && visor <= 0.01f) return;
        final float pl = plates, vi = visor;
        g.drawManaged(() -> {
            float side = w * 0.12f * Soft.ease(pl);
            if (side > 1) {
                // the shoulder plates: curved slabs with a lime edge, sliding in from both sides
                arcPlate(g, w, h, side, 0);
                arcPlate(g, w, h, side, 1);
            }
            float top = h * 0.2f * Soft.ease(vi);
            if (top > 1) arcPlate(g, w, h, top, 2);   // the helmet's brow coming down over your eyes
        });
    }

    // A plate whose inner edge curves like the inside of a helmet: 0 = left, 1 = right, 2 = top.
    private static void arcPlate(GuiGraphics g, float w, float h, float depth, int side) {
        int n = 26;
        float[] xs = new float[n + 3], ys = new float[n + 3];
        int k = 0;
        float span = side == 2 ? w : h;
        xs[k] = side == 1 ? w + 20 : -20;
        ys[k++] = -20;
        for (int i = 0; i <= n; i++) {
            float u = -1 + 2f * i / n;
            float along = span / 2f + u * (span / 2f + 20);
            float reach = depth * (float) Math.sqrt(Math.max(0, 1 - u * u * 0.96f));
            if (side == 0) {
                xs[k] = reach;
                ys[k++] = along;
            } else if (side == 1) {
                xs[k] = w - reach;
                ys[k++] = along;
            } else {
                xs[k] = along;
                ys[k++] = reach;
            }
        }
        xs[k] = side == 1 ? w + 20 : side == 2 ? w + 20 : -20;
        ys[k++] = side == 2 ? -20 : h + 20;
        if (side == 2) {
            xs[0] = -20;
            ys[0] = -20;
        }
        if (side != 0) {
            // the fill wants its corners the other way round on the right and the top
            for (int i = 0; i < k / 2; i++) {
                float tx = xs[i], ty = ys[i];
                xs[i] = xs[k - 1 - i];
                ys[i] = ys[k - 1 - i];
                xs[k - 1 - i] = tx;
                ys[k - 1 - i] = ty;
            }
        }
        Soft.polygon(g, xs, ys, k, 0xE0262C33);
        if (side != 0) {
            for (int i = 0; i < k / 2; i++) {
                float tx = xs[i], ty = ys[i];
                xs[i] = xs[k - 1 - i];
                ys[i] = ys[k - 1 - i];
                xs[k - 1 - i] = tx;
                ys[k - 1 - i] = ty;
            }
        }
        for (int i = 1; i < k - 2; i++) Soft.line(g, xs[i], ys[i], xs[i + 1], ys[i + 1], 1.3f, 0xFF96FF1E);
    }
}
