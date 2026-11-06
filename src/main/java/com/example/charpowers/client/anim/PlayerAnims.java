package com.example.charpowers.client.anim;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.client.anim.model.Body;
import com.example.charpowers.client.anim.model.GunMotion;
import com.example.charpowers.client.anim.model.GunNumbers;
import com.example.charpowers.client.anim.model.GunPose;
import com.example.charpowers.client.anim.model.PoseDef;
import com.example.charpowers.client.anim.model.PoseFile;
import com.example.charpowers.client.anim.model.Solver;
import com.example.charpowers.lantern.ConstructEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.threetag.palladium.entity.PalladiumPlayerExtension;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// What every player on screen is doing right now. Empty most of the time: nothing here costs anything until someone plays a pose.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class PlayerAnims {
    private static final ResourceLocation DEFAULT_FILE = new ResourceLocation(CharPowers.MODID, "player_poses.json");
    public static final String GUN_POSE = "minigun_hold", SUIT_WALK = "suit_walk";
    // one full stride of the game's walk animation, in its walk position units
    private static final double STRIDE = Math.PI * 2 / 0.6662;

    private static final Map<Integer, State> PLAYERS = new HashMap<>();
    private static PoseFile table;
    private static int suitCheck;

    static final class Playing {
        final PoseDef def;
        final double start;
        double stopAt = -1;
        double frozenAt = -1;

        Playing(PoseDef def, double start) {
            this.def = def;
            this.start = start;
        }

        double running(double now) {
            return frozenAt >= 0 ? frozenAt : now - start;
        }

        boolean finished(double now) {
            return frozenAt < 0 && def.finished(running(now), stopAt);
        }
    }

    static final class State {
        Playing walk, hold, shot;
        Playing leaving; // the pose the new one is taking over from, so nothing snaps
        ConstructEntity gun;
        double gunSeen;
        double fire, fireO;

        boolean empty() {
            return walk == null && hold == null && shot == null;
        }
    }

    private PlayerAnims() {}

    // ---- the table of poses

    public static PoseFile table() {
        if (table == null) reload();
        return table;
    }

    public static Collection<String> names() {
        return table().poses.keySet();
    }

    // Reads the file in the mod, then the one in config/charpowers if there is one: any pose named there replaces the mod's.
    public static String reload() {
        String problem = null;
        PoseFile file = null;
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(DEFAULT_FILE);
            if (resource.isPresent()) {
                try (Reader r = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                    file = PoseFile.read(r);
                }
            }
        } catch (Exception e) {
            problem = e.getMessage();
            CharPowers.LOG.error("Couldn't read the player poses", e);
        }
        if (file == null) file = empty();
        Path own = FMLPaths.CONFIGDIR.get().resolve("charpowers/player_poses.json");
        if (Files.exists(own)) {
            try (Reader r = Files.newBufferedReader(own)) {
                PoseFile mine = PoseFile.read(r);
                file.poses.putAll(mine.poses);
                if (mine.hasGun) file = file.withGun(mine.gun);
            } catch (Exception e) {
                problem = e.getMessage();
                CharPowers.LOG.error("Couldn't read {}", own, e);
            }
        }
        table = file;
        // anything playing has the old keyframes: let it finish rather than swap mid-swing
        return problem;
    }

    private static PoseFile empty() {
        return PoseFile.read(new java.io.StringReader("{\"poses\":{}}"));
    }

    // ---- the clock

    static double now(float partial) {
        return Minecraft.getInstance().gui.getGuiTicks() + partial;
    }

    static double now() {
        return now(Minecraft.getInstance().getFrameTime());
    }

    // ---- starting and stopping

    public static void onMessage(int entity, String pose) {
        if (pose.isEmpty()) stop(entity);
        else play(entity, pose);
    }

    public static boolean play(int entity, String name) {
        if (!AnimSettings.on()) return false;
        PoseDef def = table().poses.get(name);
        if (def == null) return false;
        State s = PLAYERS.computeIfAbsent(entity, k -> new State());
        s.leaving = s.shot;
        s.shot = new Playing(def, now());
        return true;
    }

    public static void stop(int entity) {
        State s = PLAYERS.get(entity);
        if (s == null || s.shot == null) return;
        double now = now();
        s.shot.frozenAt = -1;
        if (s.shot.stopAt < 0) s.shot.stopAt = s.shot.running(now);
    }

    // Test and screenshot use: holds a pose at one moment until it's stopped.
    public static boolean freeze(int entity, String name, double tick) {
        PoseDef def = table().poses.get(name);
        if (def == null) return false;
        State s = PLAYERS.computeIfAbsent(entity, k -> new State());
        Playing playing = new Playing(def, now());
        playing.frozenAt = tick;
        s.leaving = null;
        if (name.equals(GUN_POSE)) s.hold = playing;
        else s.shot = playing;
        return true;
    }

    // Everything off for this player, right now.
    public static void clear(int entity) {
        PLAYERS.remove(entity);
    }

    public static void clearAll() {
        PLAYERS.clear();
    }

    public static boolean anyActive() {
        return !PLAYERS.isEmpty();
    }

    static State state(int entity) {
        return PLAYERS.get(entity);
    }

    // ---- the minigun: while it's out the construct calls this each tick on the PC. Nothing needs a message: it's all on the entity.

    public static void gunTick(ConstructEntity gun) {
        if (!AnimSettings.on()) return;
        int holder = gun.holder();
        if (holder < 0) return;
        Entity e = gun.level().getEntity(holder);
        if (!(e instanceof AbstractClientPlayer)) return;
        State s = PLAYERS.computeIfAbsent(holder, k -> new State());
        double now = now(0);
        s.gun = gun;
        s.gunSeen = now;
        String state = gun.animation();
        boolean leaving = "Unsummon".equals(state);
        if (s.hold == null && !leaving) {
            PoseDef def = table().poses.get(GUN_POSE);
            if (def != null) s.hold = new Playing(def, now);
        }
        if (s.hold != null && leaving && s.hold.stopAt < 0) s.hold.stopAt = s.hold.running(now);
        double want = "Shooting".equals(state) ? 1 : "Warm_Up".equals(state) ? 0.4 : 0;
        s.fireO = s.fire;
        s.fire += (want - s.fire) * (want > s.fire ? 0.35 : 0.2);
        if (s.fire < 0.001) s.fire = 0;
    }

    // Who's holding this gun, if the gun should be drawn in their hands.
    // Even with no pose playing (animations off, or it hasn't ticked yet) the gun goes where the hands would be.
    public static AbstractClientPlayer holderOf(ConstructEntity gun) {
        int holder = gun.holder();
        if (holder < 0 || gun.level() == null || "Unsummon".equals(gun.animation()) && !PLAYERS.containsKey(holder)) return null;
        return gun.level().getEntity(holder) instanceof AbstractClientPlayer p && p.isAlive() ? p : null;
    }

    public static boolean holdingGun(Player p) {
        State s = PLAYERS.get(p.getId());
        return s != null && s.hold != null && s.gun != null;
    }

    // Both hands on the gun, or round a book: nothing else is in them.
    public static boolean handsFull(Player p) {
        State s = PLAYERS.get(p.getId());
        return s != null && (s.hold != null && s.gun != null || s.shot != null && s.shot.def.prop != null);
    }

    // Where the gun is this frame on the player model. Both the gun and the arms ask this, so they can't disagree.
    static GunPose gunPose(AbstractClientPlayer p, State s, double armY, double netYaw, double pitch, float partial) {
        double now = now(partial);
        double fire = s == null ? 0 : Mth.lerp(partial, s.fireO, s.fire);
        double walk = Math.min(1, p.walkAnimation.speed(partial));
        GunMotion motion = s == null ? GunMotion.STILL : GunMotion.at(now, fire, p.walkAnimation.position(partial), walk);
        return GunPose.of(table().gun, armY, netYaw, pitch, motion);
    }

    // ---- working out the body

    private static final Solver SOLVER = new Solver();

    // Applies everything this player is playing to the body. first: the first-person arms, which only want poses that show there.
    static void solve(AbstractClientPlayer p, State s, Body body, Solver.View view, float partial, boolean first) {
        double now = now(partial);
        boolean mirror = p.getMainArm() == HumanoidArm.LEFT;
        if (s.walk != null && !first) {
            double speed = Math.min(1, p.walkAnimation.speed(partial) * 1.4);
            double key = (p.walkAnimation.position(partial) % STRIDE) / STRIDE * s.walk.def.length;
            SOLVER.apply(s.walk.def, key < 0 ? key + s.walk.def.length : key, speed, view, body, mirror, null, null);
        }
        if (s.hold != null) {
            GunNumbers numbers = table().gun;
            GunPose gun = gunPose(p, s, view.armY, view.netYaw, view.pitch, partial);
            double fire = Mth.lerp(partial, s.fireO, s.fire);
            view.shakeLean = fire * 0.012 * Math.sin(now * 5.1);
            view.shakeTwist = fire * 0.012 * Math.sin(now * 3.9 + 1);
            double running = s.hold.running(now);
            SOLVER.apply(s.hold.def, running, s.hold.def.weight(running, s.hold.stopAt), view, body, false, gun, numbers);
        }
        if (s.leaving != null && s.shot != null && (!first || s.leaving.def.firstPerson)) {
            double running = s.leaving.running(now);
            SOLVER.apply(s.leaving.def, running, s.leaving.def.weight(running, s.leaving.stopAt), view, body, mirror, null, null);
        }
        if (s.shot != null && (!first || s.shot.def.firstPerson)) {
            double running = s.shot.running(now);
            SOLVER.apply(s.shot.def, running, s.shot.def.weight(running, s.shot.stopAt), view, body, mirror, null, null);
        }
    }

    // How much of the first-person hands should be out of the way: 0 normal hands, 1 all real arms.
    public static float handsAway(float partial) {
        if (PLAYERS.isEmpty()) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        State s = PLAYERS.get(mc.player.getId());
        if (s == null) return 0;
        double now = now(partial), away = 0;
        if (s.hold != null) away = s.hold.def.weight(s.hold.running(now), s.hold.stopAt);
        if (s.shot != null && s.shot.def.firstPerson) away = Math.max(away, s.shot.def.weight(s.shot.running(now), s.shot.stopAt));
        return (float) away;
    }

    // How far this prop is grown: 0 not there, 1 all there.
    static float propWeight(AbstractClientPlayer p, String prop, float partial) {
        State s = PLAYERS.get(p.getId());
        if (s == null || s.shot == null || !prop.equals(s.shot.def.prop)) return 0;
        return (float) s.shot.def.weight(s.shot.running(now(partial)), s.shot.stopAt);
    }

    // In the air on a Palladium flight, or swimming, or gliding: the game's own pose is the one to keep.
    static double bodyFree(AbstractClientPlayer p, float partial) {
        if (p.isFallFlying() || p.isSwimming() || p.isVisuallySwimming() || p.isSleeping() || p.isAutoSpinAttack()) return 0;
        if (p instanceof PalladiumPlayerExtension flier) {
            var flight = flier.palladium$getFlightHandler();
            if (flight != null) return 1 - Math.min(1, Math.max(0, flight.getFlightAnimation(partial)));
        }
        return 1;
    }

    // ---- keeping things tidy

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        if (++suitCheck >= 10) {
            suitCheck = 0;
            checkSuits(mc);
        }
        if (PLAYERS.isEmpty()) return;
        double now = now(0);
        for (Iterator<Map.Entry<Integer, State>> it = PLAYERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, State> entry = it.next();
            State s = entry.getValue();
            Entity who = mc.level.getEntity(entry.getKey());
            if (!(who instanceof AbstractClientPlayer p) || !p.isAlive()) {
                it.remove();
                continue;
            }
            if (s.gun != null && now - s.gunSeen > 3) {
                // the construct is gone without a goodbye: it went out of sight, or the player did
                s.gun = null;
                if (s.hold != null && s.hold.frozenAt < 0 && s.hold.stopAt < 0) s.hold.stopAt = s.hold.running(now);
            }
            if (s.hold != null && s.hold.frozenAt < 0 && s.gun == null && s.hold.stopAt < 0) s.hold.stopAt = s.hold.running(now);
            if (s.hold != null && s.hold.finished(now) && s.hold.stopAt >= 0) s.hold = null;
            if (s.shot != null) {
                PoseDef def = s.shot.def;
                if (s.shot.frozenAt < 0 && s.shot.stopAt < 0) {
                    boolean moved = def.stopOnMove && p.walkAnimation.speed() > 0.25f;
                    boolean hit = def.stopOnHit && p.hurtTime > 0;
                    if (moved || hit) s.shot.stopAt = s.shot.running(now);
                }
                if (s.leaving != null && s.shot.running(now) >= def.blendIn) s.leaving = null;
                if (s.shot.finished(now)) s.shot = null;
            }
            if (s.shot == null) s.leaving = null;
            if (s.empty()) it.remove();
        }
    }

    // Every half a second: who's wearing the suit, so they walk heavy. Just a look at each player's chest slot.
    private static void checkSuits(Minecraft mc) {
        if (!AnimSettings.on()) return;
        PoseDef def = table().poses.get(SUIT_WALK);
        if (def == null) return;
        for (AbstractClientPlayer p : mc.level.players()) {
            boolean suited = SuitItems.fullSuit(p);
            State s = PLAYERS.get(p.getId());
            if (suited && (s == null || s.walk == null)) {
                PLAYERS.computeIfAbsent(p.getId(), k -> new State()).walk = new Playing(def, now(0));
            } else if (!suited && s != null && s.walk != null) {
                s.walk = null;
                if (s.empty()) PLAYERS.remove(p.getId());
            }
        }
    }

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut e) {
        PLAYERS.clear();
    }
}
