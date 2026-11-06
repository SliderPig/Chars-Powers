package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.lantern.BubbleEntity;
import com.example.charpowers.lantern.ConstructEntity;
import com.example.charpowers.lantern.GreenLantern;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.entity.PalladiumAttributes;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

// Green Lantern lab: flat ground, one target, a camera exactly side-on and one from the front, both a few blocks from the action.
// Every construct runs twice, once per camera, and a screenshot is taken at the moment it hits. Only compiled for the smoke run.
final class LanternLab {
    private static ArmorStand target, side, front, back;
    private static Vec3 base;
    private static Zombie zombie;

    private record Ev(int at, Runnable run) {}

    private LanternLab() {}

    private static Runnable srv(Consumer<ServerPlayer> what) {
        return () -> {
            var server = Minecraft.getInstance().getSingleplayerServer();
            server.execute(() -> {
                try {
                    what.accept(server.getPlayerList().getPlayers().get(0));
                } catch (Throwable t) {
                    CharPowers.LOG.error("[LanternLab] step failed", t);
                }
            });
        };
    }

    private static void server(int ticks, Consumer<ServerPlayer> what) {
        ClientChecks.later(ticks, srv(what));
    }

    private static ArmorStand stand(ServerLevel level, Vec3 at, boolean visible) {
        ArmorStand s = new ArmorStand(EntityType.ARMOR_STAND, level);
        s.moveTo(at.x, at.y, at.z, 0f, 0f);
        s.setNoGravity(true);
        s.setInvulnerable(true);
        s.setShowArms(visible);
        s.setInvisible(!visible);
        level.addFreshEntity(s);
        return s;
    }

    // Everything back to the start: no constructs, full battery, the target and cameras in place.
    private static Runnable reset(float pitch, double cx, double cz, double dist) {
        return srv(p -> {
            ServerLevel level = p.serverLevel();
            for (var c : level.getEntitiesOfClass(ConstructEntity.class, new AABB(base, base).inflate(80))) c.discard();
            for (var b : level.getEntitiesOfClass(BubbleEntity.class, new AABB(base, base).inflate(80))) b.discard();
            GreenLantern.POWER.data(p).putDouble("battery", 200);
            p.connection.teleport(base.x, base.y, base.z, 0f, pitch);
            if (!target.isAlive()) target = stand(level, base.add(0, 0, 6), true);
            target.teleportTo(base.x, base.y, base.z + 6);
            target.setDeltaMovement(Vec3.ZERO);
            zombie.teleportTo(base.x + 5, base.y, base.z + 9);
            side.teleportTo(base.x + cx + dist, base.y, base.z + cz);
            front.teleportTo(base.x + cx, base.y, base.z + cz + dist);
            back.teleportTo(base.x - 0.5, base.y, base.z - 2.5);
        });
    }

    // The client looks through one of the two camera stands, facing the middle of the action.
    private static Runnable view(String cam) {
        return () -> {
            Minecraft mc = Minecraft.getInstance();
            var c = mc.level.getEntity((cam.equals("side") ? side : cam.equals("back") ? back : front).getId());
            if (c == null) {
                CharPowers.LOG.error("[LanternLab] camera stand not on the client yet");
                return;
            }
            float yaw = cam.equals("side") ? 90f : cam.equals("back") ? 0f : 180f, pitch = cam.equals("back") ? 6f : 0f;
            c.setYRot(yaw);
            c.setXRot(pitch);
            c.yRotO = yaw;
            c.xRotO = pitch;
            if (c instanceof LivingEntity l) {
                l.setYHeadRot(yaw);
                l.yHeadRotO = yaw;
                l.yBodyRot = yaw;
                l.yBodyRotO = yaw;
            }
            mc.setCameraEntity(c);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = true;
        };
    }

    // One construct, from one side: reset, wait for the camera, then play the events at their ticks (0 = the first key).
    private static void run(String name, String cam, float pitch, double cx, double cz, double dist, List<Ev> events, String... shots) {
        ClientChecks.later(3, reset(pitch, cx, cz, dist));
        ClientChecks.later(8, view(cam));
        ClientChecks.later(8, () -> {});
        List<Ev> all = new ArrayList<>(events);
        for (int i = 0; i + 1 < shots.length; i += 2) {
            int at = Integer.parseInt(shots[i]);
            String file = "L_" + name + "_" + shots[i + 1] + "_" + cam;
            all.add(new Ev(at, srv(p -> {
                StringBuilder sb = new StringBuilder("[LanternLab] " + file + ": target " + fmt(target.position()) + " player yaw " + p.getYRot());
                for (var c : p.serverLevel().getEntitiesOfClass(ConstructEntity.class, new AABB(base, base).inflate(80))) {
                    sb.append(" | ").append(c.kind()).append(" ").append(fmt(c.position())).append(" yaw ").append(c.getYRot()).append(" age ").append(c.age());
                }
                CharPowers.LOG.info(sb.toString());
            })));
            all.add(new Ev(at, () -> net.minecraft.client.Screenshot.grab(Minecraft.getInstance().gameDirectory, file + ".png", Minecraft.getInstance().getMainRenderTarget(), c -> {})));
        }
        all.sort(Comparator.comparingInt(Ev::at));
        int last = -1;
        for (Ev e : all) {
            ClientChecks.later(Math.max(1, e.at - last), e.run);
            last = e.at;
        }
    }

    private static void both(String name, float pitch, double cx, double cz, double dist, List<Ev> events, String... shots) {
        run(name, "side", pitch, cx, cz, dist, events, shots);
        run(name, "back", pitch, cx, cz, dist, events, shots);
    }

    private static String fmt(Vec3 v) {
        return String.format("(%.2f, %.2f, %.2f)", v.x - base.x, v.y - base.y, v.z - base.z);
    }

    private static Ev key(int at, String ability) {
        return new Ev(at, srv(p -> GreenLantern.POWER.onKey(p, ability)));
    }

    private static Ev up(int at, String ability) {
        return new Ev(at, srv(p -> GreenLantern.POWER.onKeyUp(p, ability)));
    }

    private static Ev act(int at, String action) {
        return new Ev(at, srv(p -> GreenLantern.POWER.onClientAction(p, action)));
    }

    static void script() {
        server(40, p -> {
            ServerLevel level = p.serverLevel();
            p.setGameMode(GameType.SURVIVAL); // a creative player's hits get past "invulnerable" and break the target
            p.getAbilities().flying = false;
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            p.getInventory().clearContent();
            GreenLantern.POWER.give(p);
            Mastery.setLevel(p, GreenLantern.POWER.id, 100);
            GreenLantern.POWER.awaken(p);
            base = new Vec3(Math.floor(p.getX()) + 0.5, p.getY(), Math.floor(p.getZ()) + 0.5);
            p.connection.teleport(base.x, base.y, base.z, 0f, 6f);
            target = stand(level, base.add(0, 0, 6), true);
            side = stand(level, base.add(8, 0, 6), false);
            front = stand(level, base.add(0, 0, 14), false);
            back = stand(level, base.add(-0.5, 0, -2.5), false);
            zombie = new Zombie(EntityType.ZOMBIE, level);
            zombie.moveTo(base.x + 5, base.y, base.z + 9, 0f, 0f);
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            level.addFreshEntity(zombie);
        });
        suitCheck();
        hands();
        others();
        ClientChecks.later(20, () -> Minecraft.getInstance().stop());
    }

    private static String flight(ServerPlayer p) {
        var f = AbilityUtil.getInstance(p, GreenLantern.POWER.id, "gl_flight");
        return "flight speed " + p.getAttributeValue(PalladiumAttributes.FLIGHT_SPEED.get()) + (f == null ? " (no instance)" : " unlocked " + f.isUnlocked() + " enabled " + f.isEnabled());
    }

    private static void toggleSuit(ServerPlayer p) {
        var t = AbilityUtil.getInstance(p, GreenLantern.POWER.id, "suit_off");
        if (t == null) {
            CharPowers.LOG.error("[LanternLab] no suit_off instance");
            return;
        }
        t.keyPressed(p, true);
        t.keyPressed(p, false);
    }

    // Suit Off for real: the flight attribute with the suit on, off, and on again, and how you look in each.
    private static void suitCheck() {
        ClientChecks.later(60, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        server(5, p -> CharPowers.LOG.info("[LanternLab] suit ON: {}", flight(p)));
        ClientChecks.shot(20, "L_suit_on");
        server(1, LanternLab::toggleSuit);
        server(15, p -> CharPowers.LOG.info("[LanternLab] suit OFF: toggle enabled {}, {}", AbilityUtil.isEnabled(p, GreenLantern.POWER.id, "suit_off"), flight(p)));
        ClientChecks.shot(5, "L_suit_off");
        server(1, LanternLab::toggleSuit);
        server(15, p -> CharPowers.LOG.info("[LanternLab] suit ON again: toggle enabled {}, {}", AbilityUtil.isEnabled(p, GreenLantern.POWER.id, "suit_off"), flight(p)));
    }

    // Frames right through each animation, from behind the player's shoulder and side-on.
    private static void hands() {
        both("slam", 6f, 0, 6, 8, List.of(key(0, "hand_slam")), "3", "f1", "7", "f2", "11", "f3", "15", "f4", "19", "f5", "24", "f6");
        both("slap", 6f, 0, 6, 8, List.of(key(0, "backhand_slap")), "4", "f1", "8", "f2", "12", "f3", "16", "f4", "20", "f5", "26", "f6");
        both("fist", 15f, 0, 6, 8, List.of(key(0, "rising_fist")), "3", "f1", "6", "f2", "9", "f3", "12", "f4", "16", "f5", "22", "f6");
        both("fistfield", 0f, 0, 0, 10, List.of(key(0, "fist_field")), "3", "f1", "6", "f2", "9", "f3", "12", "f4", "16", "f5");
        both("throw", 6f, 0, 5, 8, List.of(key(0, "hand_throw"), up(24, "hand_throw")), "3", "f1", "7", "f2", "11", "f3", "20", "f4", "24", "f5", "26", "f6", "28", "f7", "30", "f8", "32", "f9", "34", "g1", "36", "g2", "40", "g3");
    }

    private static void others() {
        both("sweep", 0f, 1, 2, 8, List.of(key(0, "construct_sword"), act(20, "swing"), key(60, "construct_sword")), "19", "f0", "21", "f1", "23", "f2", "25", "f3", "27", "f4", "30", "f5");
        both("overhead", 0f, 1, 2, 8, List.of(key(0, "construct_sword"), act(20, "swing"), act(34, "swing"), act(48, "swing"), key(80, "construct_sword")),
                "47", "f0", "50", "f1", "53", "f2", "56", "f3", "59", "f4", "62", "f5");
        both("daggers", 6f, 0, 3, 8, List.of(key(0, "launch_swords"), up(30, "launch_swords")), "6", "f1", "14", "f2", "22", "f3", "29", "f4", "32", "f5", "35", "f6", "39", "f7");
        both("rain", 15f, 0, 6, 8, List.of(key(0, "sword_rain")), "3", "f1", "7", "f2", "11", "f3", "15", "f4", "20", "f5", "26", "f6");
        both("minigun", 6f, 0, 3, 8, List.of(key(0, "minigun"), act(16, "fire:1"), act(44, "fire:0"), key(56, "minigun")), "2", "f1", "5", "f2", "9", "f3", "14", "f4", "20", "f5", "30", "f6", "42", "f7", "52", "f8");
        both("bubble", 15f, 0, 6, 18, List.of(key(0, "bubble"), key(45, "bubble")), "3", "f1", "7", "f2", "12", "f3", "20", "f4", "30", "f5");
        both("turret", 22f, 0, 4, 8, List.of(key(0, "turret")), "3", "f1", "6", "f2", "9", "f3", "14", "f4", "24", "f5", "60", "f6");
    }
}
