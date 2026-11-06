package com.example.charpowers.lantern;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.lantern.model.ConstructPlace;
import com.example.charpowers.lantern.model.LanternRules;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.threetag.palladium.power.ability.AbilityUtil;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A ring that makes constructs from the power of your will. The design is docs/specs/2026-10-23-green-lantern-design.md,
// the models and animations are my own. Everything is run from one tick loop that only does anything while something's out.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class GreenLantern extends OwnPower {
    public static final GreenLantern POWER = new GreenLantern();
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.19f, 0.85f, 0.27f), 1f);
    private static final String BATTERY = "battery";
    private static final Path OATH_FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/oath.txt");

    private static final Map<UUID, Lantern> ACTIVE = new HashMap<>();
    private static final Map<UUID, Trial> TRIALS = new HashMap<>();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Nuke> NUKES = new ArrayList<>();
    private static final Map<UUID, SinestroMob> FIGHTS = new HashMap<>();
    private static final String SINESTRO_NIGHT = "sinestroNight", OATH = "oath";
    private static final java.util.Set<UUID> CHOOSING = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean constructing;
    private final Map<UUID, Double> lastSent = new HashMap<>();

    // What one player has out right now.
    private static final class Lantern {
        ConstructEntity sword, gun, hand;
        LivingEntity held;
        boolean holding, heldKey, firing;
        int spin, combo, swingCooldown, daggerGap, handAge, rain;
        Vec3 rainAt;
        final List<ConstructEntity> forming = new ArrayList<>();
        final List<Turret> turrets = new ArrayList<>();
        BubbleEntity bubble;
        long lastUsed;

        boolean idle() {
            return sword == null && gun == null && hand == null && forming.isEmpty() && turrets.isEmpty() && bubble == null && rain == 0;
        }
    }

    private static final class Turret {
        final ConstructEntity entity;
        LivingEntity target;
        int spin;

        Turret(ConstructEntity entity) {
            this.entity = entity;
        }
    }

    private static final class Trial {
        final Warden warden;

        Trial(Warden warden) {
            this.warden = warden;
        }
    }

    private static final class Ring {
        final UUID player;
        final Warden warden;
        int age;

        Ring(UUID player, Warden warden) {
            this.player = player;
            this.warden = warden;
        }
    }

    private static final class Nuke {
        final UUID owner;
        final ServerLevel level;
        final Vec3 at;
        int fuse = LanternRules.NUKE_FUSE, ring;
        final Deque<LivingEntity> hit = new ArrayDeque<>();

        Nuke(UUID owner, ServerLevel level, Vec3 at) {
            this.owner = owner;
            this.level = level;
            this.at = at;
        }
    }

    private GreenLantern() {
        super(Mastery.GREEN_LANTERN, Items.EMERALD);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "A ring comes down out of the sky and finds your hand. You have the ability to overcome great fear."; }

    @Override
    public String awakenedMessage() { return "Sinestro is beaten and your oath is said over him. Green Lantern has awakened: Fist Field, Sword Rain and a third turret."; }

    // Yours if you've made one, otherwise the mod's.
    public String oathOf(Player p) {
        String own = data(p).getString(OATH);
        return own.isEmpty() ? oath() : own;
    }

    @Override
    public void unlock(ServerPlayer p) {
        super.unlock(p);
        if (owns(p)) askOath(p);
    }

    // Asks for an oath: the next thing they say in chat is it, and nobody else sees it.
    public void askOath(ServerPlayer p) {
        tell(p, "Your oath: \"" + oathOf(p) + "\"");
        tell(p, "Hold the lantern in your off hand and say it to recharge. Type your own oath in chat now to make it yours, or say keep.");
        CHOOSING.add(p.getUUID());
    }

    private static String oath() {
        try {
            if (Files.exists(OATH_FILE)) return Files.readString(OATH_FILE).trim();
            Files.createDirectories(OATH_FILE.getParent());
            Files.writeString(OATH_FILE, LanternRules.DEFAULT_OATH);
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't read {}", OATH_FILE, e);
        }
        return LanternRules.DEFAULT_OATH;
    }

    private Lantern lantern(ServerPlayer p) {
        return ACTIVE.computeIfAbsent(p.getUUID(), u -> new Lantern());
    }

    // ---- battery

    public double maxBattery(Player p) {
        return LanternRules.maxBattery(mastery(p));
    }

    public double battery(Player p) {
        CompoundTag d = data(p);
        return d.contains(BATTERY) ? d.getDouble(BATTERY) : maxBattery(p);
    }

    private void setBattery(ServerPlayer p, double value) {
        data(p).putDouble(BATTERY, Math.max(0, Math.min(maxBattery(p), value)));
        sendState(p);
        SinestroMob s = FIGHTS.get(p.getUUID());
        if (s != null && value <= 0) s.batteryGone();
    }

    // Every hit a construct makes goes through here, so Sinestro knows it was one.
    private void hit(ServerPlayer p, LivingEntity target, float damage) {
        constructing = true;
        try {
            target.hurt(p.damageSources().playerAttack(p), damage * Setting.LANTERN_DAMAGE.f() / 100);
        } finally {
            constructing = false;
        }
    }

    static boolean fromConstruct() {
        return constructing;
    }

    static void fightOver(UUID player) {
        FIGHTS.remove(player);
    }

    public boolean inFight(Player p) {
        return FIGHTS.containsKey(p.getUUID());
    }

    private boolean spend(ServerPlayer p, double baseCost) {
        double cost = baseCost * Setting.LANTERN_COST.d() / 100;
        double have = battery(p);
        if (have < cost) {
            whisper(p, "Battery low. Lantern in your off hand, then say: " + oathOf(p));
            return false;
        }
        setBattery(p, have - cost);
        lantern(p).lastUsed = p.level().getGameTime();
        com.example.charpowers.anim.AnimTriggers.ringSpent();
        return true;
    }

    private void sendState(ServerPlayer p) {
        Lantern l = ACTIVE.get(p.getUUID());
        CompoundTag tag = new CompoundTag();
        lastSent.put(p.getUUID(), battery(p));
        tag.putDouble("battery", battery(p));
        tag.putDouble("max", maxBattery(p));
        tag.putBoolean("sword", l != null && l.sword != null);
        tag.putBoolean("gun", l != null && l.gun != null);
        tag.putString("oath", oathOf(p));
        Net.toOwner(p, id.toString(), tag);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        Lantern l = ACTIVE.get(p.getUUID());
        if (l != null && p.level().getGameTime() - l.lastUsed < 200) Mastery.add(p, id, 1);
        if (battery(p) != lastSent.getOrDefault(p.getUUID(), -1.0)) sendState(p);
        // with the lantern out and the ring not full, the words are right there
        if (p.getOffhandItem().is(SuitItems.POWER_BATTERY.get()) && battery(p) < maxBattery(p)) whisper(p, "Say: " + oathOf(p));
        // at mastery 100, at nightfall, he comes for you. Once a night: if he won, he's back tomorrow.
        if (questReady(p) && p.level().dimension() == Level.OVERWORLD && p.level().isNight() && !FIGHTS.containsKey(p.getUUID())) {
            long night = p.level().getDayTime() / 24000;
            if (!data(p).contains(SINESTRO_NIGHT) || data(p).getLong(SINESTRO_NIGHT) != night) {
                data(p).putLong(SINESTRO_NIGHT, night);
                FIGHTS.put(p.getUUID(), SinestroMob.comeFor(p));
            }
        }
    }

    // ---- the keys

    @Override
    public void onKey(ServerPlayer p, String ability) {
        Lantern l = lantern(p);
        // the sword and the minigun take your whole attention
        if ((l.sword != null && !"construct_sword".equals(ability)) || (l.gun != null && !"minigun".equals(ability))) {
            whisper(p, "Put it away first.");
            return;
        }
        l.lastUsed = p.level().getGameTime();
        switch (ability) {
            case "rising_fist" -> risingFist(p);
            case "backhand_slap" -> slap(p);
            case "hand_slam" -> slam(p);
            case "hand_throw" -> snatch(p, l);
            case "construct_sword" -> sword(p, l);
            case "launch_swords" -> {
                l.heldKey = true;
                com.example.charpowers.anim.AnimTriggers.ringSpent();
            }
            case "minigun" -> minigun(p, l);
            case "turret" -> turret(p, l);
            case "bubble" -> bubble(p, l);
            case "nuke" -> nuke(p);
            case "fist_field" -> fistField(p);
            case "sword_rain" -> swordRain(p, l);
            default -> {}
        }
    }

    @Override
    public void onKeyUp(ServerPlayer p, String ability) {
        Lantern l = ACTIVE.get(p.getUUID());
        if (l == null) return;
        if ("hand_throw".equals(ability)) l.holding = false;
        if ("launch_swords".equals(ability)) {
            l.heldKey = false;
            fireDaggers(p, l);
        }
    }

    // What the PC tells us: the sword's left clicks, the gun's right button.
    @Override
    public void onClientAction(ServerPlayer p, String action) {
        if (!owns(p)) return;
        Lantern l = lantern(p);
        switch (action) {
            case "swing" -> swing(p, l);
            case "fire:1" -> l.firing = l.gun != null;
            case "fire:0" -> l.firing = false;
            default -> {}
        }
    }

    private static Vec3 lookSpot(ServerPlayer p, double range) {
        HitResult hit = p.pick(range, 0f, false);
        return hit.getLocation();
    }

    private void hitAround(ServerPlayer p, Vec3 at, double radius, float damage, Vec3 push) {
        AABB box = new AABB(at, at).inflate(radius);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && x.position().distanceToSqr(at) <= radius * radius + 4)) {
            hit(p, e, LanternRules.damage(damage, mastery(p)));
            e.setDeltaMovement(push);
            e.hurtMarked = true;
        }
    }

    // A fist comes up through the ground and launches everything on it. Sometimes it's rude about it.
    private void risingFist(ServerPlayer p) {
        if (!spend(p, LanternRules.HAND_COST)) return;
        Vec3 spot = lookSpot(p, LanternRules.LOOK_RANGE);
        if (p.getRandom().nextInt(LanternRules.MIDDLE_FINGER_ONE_IN) == 0) whisper(p, "It's the middle one.");
        double[] fp = ConstructPlace.spawn(ConstructPlace.FIST, p.getYRot(), spot.x, spot.y, spot.z);
        ConstructEntity fist = ConstructEntity.make(p.level(), "fist", "Emerge", fp[0], fp[1], fp[2], p.getYRot(), LanternRules.FIST_LIFE);
        ServerLevel level = p.serverLevel();
        Fx.particles(level, GREEN, spot.add(0, 0.1, 0), 20, 1.2);
        fist.at(LanternRules.FIST_HIT, () -> {
            hitAround(p, spot, LanternRules.handRadius(mastery(p)), LanternRules.FIST_DAMAGE, new Vec3(0, 1.5, 0));
            level.playSound(null, BlockPos.containing(spot), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.4f);
            LanternSounds.play(level, spot, LanternSounds.THUMP, 1f, 1f);
        });
    }

    private void slap(ServerPlayer p) {
        LivingEntity target = Fx.lookTarget(p, LanternRules.LOOK_RANGE);
        if (target == null) {
            whisper(p, "Look at what you want to slap.");
            return;
        }
        if (!spend(p, LanternRules.HAND_COST)) return;
        Vec3 side = p.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        // the hand comes from your left and sweeps to your right, so it is made facing the way you look and lands on them
        double[] sp = ConstructPlace.spawn(ConstructPlace.SLAP, p.getYRot(), target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        Vec3 at = new Vec3(sp[0], sp[1], sp[2]);
        ConstructEntity hand = ConstructEntity.make(p.level(), "slap", "slap", at.x, at.y, at.z, p.getYRot(), LanternRules.SLAP_LIFE);
        hand.at(LanternRules.SLAP_HIT - 10, () -> LanternSounds.play(p.level(), at, LanternSounds.SWING, 1f, 1.1f));
        hand.at(LanternRules.SLAP_HIT, () -> {
            LanternSounds.play(p.level(), target.position(), LanternSounds.THUMP, 0.8f, 1.5f);
            if (!target.isAlive()) return;
            hit(p, target, LanternRules.damage(LanternRules.SLAP_DAMAGE, mastery(p)));
            target.setDeltaMovement(side.scale(3).add(0, 0.5, 0));
            target.hurtMarked = true;
            p.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.5f, 0.7f);
        });
    }

    private void slam(ServerPlayer p) {
        LivingEntity target = Fx.lookTarget(p, LanternRules.LOOK_RANGE);
        Vec3 spot = target != null ? target.position() : lookSpot(p, LanternRules.LOOK_RANGE);
        if (!spend(p, LanternRules.HAND_COST)) return;
        double[] mp = ConstructPlace.spawn(ConstructPlace.SLAM, p.getYRot(), spot.x, spot.y, spot.z);
        ConstructEntity hand = ConstructEntity.make(p.level(), "slam", "slam", mp[0], mp[1], mp[2], p.getYRot(), LanternRules.SLAM_LIFE);
        hand.at(LanternRules.SLAM_HIT - 8, () -> LanternSounds.play(p.level(), spot, LanternSounds.SWING, 1f, 0.8f));
        hand.at(LanternRules.SLAM_HIT, () -> {
            LanternSounds.play(p.level(), spot, LanternSounds.THUMP, 1.2f, 0.8f);
            hitAround(p, spot, LanternRules.handRadius(mastery(p)), LanternRules.SLAM_DAMAGE, new Vec3(0, -1.2, 0));
            Fx.particles(p.serverLevel(), GREEN, spot.add(0, 0.2, 0), 40, 2);
            p.level().playSound(null, BlockPos.containing(spot), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.6f);
        });
    }

    // Press: the hand snatches them. A quick tap throws straight away; keep holding and they stay in the hand, then let go.
    private void snatch(ServerPlayer p, Lantern l) {
        if (l.hand != null) return;
        LivingEntity target = Fx.lookTarget(p, LanternRules.LOOK_RANGE);
        if (target == null) {
            whisper(p, "Look at what you want to grab.");
            return;
        }
        if (!spend(p, LanternRules.HAND_COST)) return;
        Vec3 side = p.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        double[] gp = ConstructPlace.spawn(ConstructPlace.GRAB, p.getYRot(), target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        l.hand = ConstructEntity.make(p.level(), "throw", "grab", gp[0], gp[1], gp[2], p.getYRot(), 0);
        l.holding = true;
        l.handAge = 0;
        l.hand.at(LanternRules.GRAB_HIT - 5, () -> LanternSounds.play(p.level(), target.position(), LanternSounds.SWING, 0.8f, 1.2f));
        l.hand.at(LanternRules.GRAB_HIT, () -> {
            LanternSounds.play(p.level(), target.position(), LanternSounds.THUMP, 0.6f, 1.6f);
            l.held = target;
            hit(p, target, LanternRules.damage(LanternRules.THROW_DAMAGE, mastery(p)));
            l.hand.play("hold");
        });
    }

    private void tickHand(ServerPlayer p, Lantern l) {
        l.handAge++;
        if (l.held != null) {
            if (!l.held.isAlive()) {
                l.held = null;
                l.hand.endIn(1);
                l.hand = null;
                return;
            }
            Vec3 front = p.getEyePosition().add(p.getLookAngle().scale(3.5));
            double[] hp = ConstructPlace.spawn(ConstructPlace.HOLD, p.getYRot(), front.x, front.y + l.held.getBbHeight() * 0.5, front.z);
            l.hand.setPos(hp[0], hp[1], hp[2]);
            l.hand.setYRot(p.getYRot());
            if (l.held instanceof ServerPlayer held) held.connection.teleport(front.x, front.y, front.z, held.getYRot(), held.getXRot());
            else l.held.setPos(front.x, front.y, front.z);
            l.held.setDeltaMovement(Vec3.ZERO);
            l.held.fallDistance = 0;
            if (l.handAge % 20 == 0) setBattery(p, battery(p) - LanternRules.CARRY_PER_SECOND);
            if (!l.holding || battery(p) <= 0) release(p, l);
        } else if (l.hand != null && l.handAge > LanternRules.GRAB_HIT + 5) {
            l.hand.endIn(1);
            l.hand = null;
        }
    }

    private void release(ServerPlayer p, Lantern l) {
        LivingEntity target = l.held;
        ConstructEntity hand = l.hand;
        l.held = null;
        l.hand = null;
        hand.play("throw");
        hand.endIn(LanternRules.THROW_LIFE);
        com.example.charpowers.anim.AnimNet.play(p, "lantern_throw");
        Vec3 way = p.getLookAngle();
        // They stay in the hand until the throw lets go (before, they dropped to the ground first and were launched from there)
        target.setNoGravity(true);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0;
        hand.at(hand.age() + LanternRules.THROW_RELEASE - 4, () -> LanternSounds.play(p.level(), hand.position(), LanternSounds.SWING, 1f, 0.9f));
        hand.at(hand.age() + LanternRules.THROW_RELEASE, () -> {
            target.setNoGravity(false);
            if (!target.isAlive()) return;
            target.setDeltaMovement(way.scale(2.6).add(0, 0.4, 0));
            target.hurtMarked = true;
        });
    }

    // ---- the greatsword

    private void sword(ServerPlayer p, Lantern l) {
        if (l.sword != null) {
            l.sword.play("vanish");
            l.sword.endIn(8);
            l.sword = null;
            sendState(p);
            return;
        }
        if (!spend(p, LanternRules.SWORD_COST)) return;
        Vec3 at = swordSpot(p);
        l.sword = ConstructEntity.make(p.level(), "greatsword", "form", at.x, at.y, at.z, p.getYRot(), 0);
        l.sword.at(10, () -> {
            if (l.sword != null) l.sword.play("idle");
        });
        sendState(p);
    }

    private static Vec3 swordSpot(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 right = flat.cross(new Vec3(0, 1, 0));
        return p.position().add(flat.scale(1.6)).add(right.scale(1.1)).add(0, 0.8 + Math.sin(p.tickCount * 0.08) * 0.1, 0);
    }

    private void swing(ServerPlayer p, Lantern l) {
        if (l.sword == null || l.swingCooldown > 0) return;
        l.swingCooldown = LanternRules.SWING_GAP;
        String[] order = {"sweep_left", "sweep_right", "overhead"};
        String anim = order[l.combo % 3];
        boolean heavy = l.combo % 3 == 2;
        l.combo++;
        l.sword.playOver(anim);
        com.example.charpowers.anim.AnimNet.play(p, "sword_" + anim);
        l.lastUsed = p.level().getGameTime();
        ConstructEntity sword = l.sword;
        sword.at(sword.age() + (heavy ? LanternRules.OVERHEAD_HIT : LanternRules.SWEEP_HIT), () -> {
            Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
            float damage = LanternRules.damage(heavy ? LanternRules.SWORD_DAMAGE * 1.5f : LanternRules.SWORD_DAMAGE, mastery(p));
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5.5), x -> x != p && x.isAlive())) {
                if (e.getEyePosition().subtract(eye).normalize().dot(look) > 0.25) {
                    hit(p, e, damage);
                    e.knockback(0.6, -look.x, -look.z);
                }
            }
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, heavy ? 0.6f : 0.9f);
            LanternSounds.play(p.level(), p.position(), LanternSounds.SWING, 1f, heavy ? 0.7f : 1f);
        });
    }

    // ---- the swords you launch

    private void tickDaggers(ServerPlayer p, Lantern l) {
        if (l.heldKey && l.forming.size() < LanternRules.maxDaggers(mastery(p)) && ++l.daggerGap >= LanternRules.DAGGER_EVERY) {
            l.daggerGap = 0;
            l.forming.add(ConstructEntity.make(p.level(), "dagger", "form", p.getX(), p.getY() + 2, p.getZ(), p.getYRot(), 0));
        }
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 right = flat.cross(new Vec3(0, 1, 0));
        for (int i = 0; i < l.forming.size(); i++) {
            double[] fan = LanternRules.fan(i, l.forming.size());
            Vec3 at = p.position().add(flat.scale(-1)).add(right.scale(fan[0])).add(0, fan[1] + 0.9, 0);
            ConstructEntity d = l.forming.get(i);
            d.setPos(at.x, at.y, at.z);
            d.setYRot(p.getYRot());
            d.setXRot(p.getXRot());
            if (d.age() == 6) d.play("hover");
        }
    }

    private void fireDaggers(ServerPlayer p, Lantern l) {
        if (l.forming.isEmpty()) return;
        if (!spend(p, LanternRules.daggerCost(l.forming.size()))) {
            for (ConstructEntity d : l.forming) d.discard();
            l.forming.clear();
            return;
        }
        Vec3 aim = lookSpot(p, 64);
        for (ConstructEntity d : l.forming) d.launch(aim.subtract(d.position()).normalize().scale(2.2), p, t -> daggerHit(p, t));
        l.forming.clear();
        p.level().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private void daggerHit(ServerPlayer p, LivingEntity target) {
        target.invulnerableTime = 0;
        hit(p, target, LanternRules.damage(LanternRules.DAGGER_DAMAGE, mastery(p)));
    }

    // ---- what awakening adds: a ring of fists round you, and swords falling on where you look

    private void fistField(ServerPlayer p) {
        if (!spend(p, LanternRules.AWAKENED_COST)) return;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            Vec3 spot = p.position().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
            float out = (float) Math.toDegrees(Math.atan2(-Math.cos(angle), Math.sin(angle))); // each fist faces away from you
            double[] fp = ConstructPlace.spawn(ConstructPlace.FIST, out, spot.x, spot.y, spot.z);
            ConstructEntity fist = ConstructEntity.make(p.level(), "fist", "Emerge", fp[0], fp[1], fp[2], out, LanternRules.FIST_LIFE);
            fist.at(LanternRules.FIST_HIT, () -> hitAround(p, spot, LanternRules.handRadius(mastery(p)), LanternRules.FIST_DAMAGE, new Vec3(0, 1.5, 0)));
        }
    }

    private void swordRain(ServerPlayer p, Lantern l) {
        if (l.rain > 0 || !spend(p, LanternRules.AWAKENED_COST)) return;
        l.rain = LanternRules.RAIN_SWORDS;
        l.rainAt = lookSpot(p, LanternRules.LOOK_RANGE);
    }

    private void tickRain(ServerPlayer p, Lantern l) {
        l.rain--;
        double angle = p.getRandom().nextDouble() * Math.PI * 2, reach = p.getRandom().nextDouble() * 6;
        Vec3 at = l.rainAt.add(Math.cos(angle) * reach, 14, Math.sin(angle) * reach);
        ConstructEntity d = ConstructEntity.make(p.level(), "dagger", "form", at.x, at.y, at.z, 0f, 0);
        d.launch(new Vec3(0, -1.6, 0), p, t -> daggerHit(p, t));
    }

    // ---- the minigun

    private void minigun(ServerPlayer p, Lantern l) {
        if (l.gun != null) {
            l.gun.play("Unsummon");
            l.gun.endIn(4);
            l.gun = null;
            l.firing = false;
            sendState(p);
            return;
        }
        if (!spend(p, LanternRules.GUN_COST)) return;
        Vec3 at = gunSpot(p);
        l.gun = ConstructEntity.make(p.level(), "minigun", "Summon", at.x, at.y, at.z, p.getYRot(), 0);
        l.gun.holdBy(p);
        l.spin = 0;
        sendState(p);
    }

    // Roughly where it is in your hands. Every PC draws it exactly there itself (client/anim/HeldGun), this is for the rest of the game.
    private static Vec3 gunSpot(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        return p.getEyePosition().add(look.scale(0.94)).add(right.scale(0.41)).add(0, -1.27, 0);
    }

    private void tickGun(ServerPlayer p, Lantern l) {
        Vec3 at = gunSpot(p);
        l.gun.setPos(at.x, at.y, at.z);
        l.gun.setYRot(p.getYRot());
        l.gun.setXRot(p.getXRot());
        if (!l.firing || battery(p) <= 0) {
            if (l.spin > 0) l.gun.play("Cool_Down");
            l.spin = 0;
            return;
        }
        if (l.spin++ == 0) l.gun.play("Warm_Up");
        if (l.spin < LanternRules.SPIN_UP) return;
        if (l.spin == LanternRules.SPIN_UP) l.gun.play("Shooting");
        if (l.spin % 20 == 0) setBattery(p, battery(p) - LanternRules.GUN_PER_SECOND);
        l.lastUsed = p.level().getGameTime();
        bullet(p, p.getEyePosition(), p.getLookAngle(), LanternRules.damage(LanternRules.GUN_TICK_DAMAGE, mastery(p)), p);
    }

    // Where the barrel ends, near enough: it tips round your back hand, below and to the right of your eyes.
    private static Vec3 gunMuzzle(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        return p.getEyePosition().add(0, -0.7, 0).add(right.scale(0.61)).add(look.scale(2.6));
    }

    // Bullets are instant hit-checks with a drawn line, not flying things: 80 entities a second would lag the server.
    private void bullet(ServerPlayer owner, Vec3 from, Vec3 way, float damage, Entity shooter) {
        Vec3 to = from.add(way.scale(LanternRules.GUN_RANGE));
        BlockHitResult wall = shooter.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (wall.getType() != HitResult.Type.MISS) to = wall.getLocation();
        EntityHitResult struck = ProjectileUtil.getEntityHitResult(shooter.level(), shooter, from, to, new AABB(from, to).inflate(1), x -> x instanceof LivingEntity && x != owner && x.isAlive());
        if (struck != null) {
            to = struck.getLocation();
            struck.getEntity().invulnerableTime = 0;
            hit(owner, (LivingEntity) struck.getEntity(), damage);
        }
        ServerLevel level = owner.serverLevel();
        if (shooter.tickCount % 2 == 0) {
            // your own gun's shots are drawn from its barrel, the hit-check stays on your crosshair
            Vec3 start = shooter == owner ? gunMuzzle(owner) : from.add(way.scale(1.5));
            double length = to.distanceTo(start);
            Vec3 along = to.subtract(start).normalize();
            for (double t = 0; t < length; t += 4) Fx.particles(level, GREEN, start.add(along.scale(t)), 1, 0);
        }
    }

    // ---- the turret

    private void turret(ServerPlayer p, Lantern l) {
        if (l.turrets.size() >= LanternRules.maxTurrets(mastery(p), awakened(p))) {
            Turret oldest = l.turrets.remove(0);
            oldest.entity.play("Unsummon");
            oldest.entity.endIn(6);
        }
        if (!spend(p, LanternRules.TURRET_COST)) return;
        Vec3 spot = lookSpot(p, LanternRules.LOOK_RANGE);
        Turret t = new Turret(ConstructEntity.make(p.level(), "turret", "Summon", spot.x, spot.y, spot.z, 0f, 0));
        t.entity.at(8, () -> t.entity.play("Idle"));
        l.turrets.add(t);
    }

    private void tickTurrets(ServerPlayer p, Lantern l, int tick) {
        Iterator<Turret> it = l.turrets.iterator();
        while (it.hasNext()) {
            Turret t = it.next();
            ConstructEntity e = t.entity;
            if (e.level() != p.level() || e.distanceTo(p) > Setting.LANTERN_TURRET_RANGE.i()) {
                e.play("Unsummon");
                e.endIn(6);
                it.remove();
                continue;
            }
            if (tick % 5 == 0 && (t.target == null || !t.target.isAlive() || t.target.distanceTo(e) > Setting.LANTERN_TURRET_RANGE.i())) {
                LivingEntity found = null;
                double nearest = Double.MAX_VALUE;
                Vec3 head = e.position().add(0, 1, 0);
                for (LivingEntity m : p.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(Setting.LANTERN_TURRET_RANGE.i()), x -> x instanceof Enemy && x.isAlive())) {
                    double far = m.distanceToSqr(e);
                    if (far < nearest && p.level().clip(new ClipContext(head, m.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
                        nearest = far;
                        found = m;
                    }
                }
                if (found == null && t.target != null) {
                    e.play("Idle");
                    e.aim(0f, 0f);
                    t.spin = 0;
                }
                t.target = found;
            }
            if (t.target == null) continue;
            Vec3 head = e.position().add(0, 1, 0);
            Vec3 way = t.target.getBoundingBox().getCenter().subtract(head).normalize();
            e.aim((float) Math.toDegrees(Math.atan2(-way.x, way.z)), (float) Math.toDegrees(-Math.asin(way.y)));
            if (t.spin++ == 0) e.play("Warm_Up");
            if (t.spin == LanternRules.SPIN_UP) e.play("Shooting");
            if (t.spin >= LanternRules.SPIN_UP) bullet(p, head, way, LanternRules.damage(LanternRules.TURRET_TICK_DAMAGE, mastery(p)), e);
        }
    }

    // ---- the bubble and the bomb

    private void bubble(ServerPlayer p, Lantern l) {
        if (l.bubble != null) {
            popBubble(l);
            return;
        }
        if (battery(p) < LanternRules.BUBBLE_PER_SECOND) {
            whisper(p, "Your battery's too low.");
            return;
        }
        Vec3 spot = lookSpot(p, LanternRules.LOOK_RANGE);
        l.bubble = BubbleEntity.make(p.level(), spot, (float) LanternRules.bubbleRadius(mastery(p)));
        com.example.charpowers.anim.AnimTriggers.ringSpent();
        p.level().playSound(null, BlockPos.containing(spot), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
    }

    private static void popBubble(Lantern l) {
        if (l.bubble == null) return;
        ServerLevel level = (ServerLevel) l.bubble.level();
        Fx.particles(level, GREEN, l.bubble.position().add(0, 2, 0), 60, l.bubble.radius() / 2);
        level.playSound(null, l.bubble.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.8f);
        l.bubble.discard();
        l.bubble = null;
    }

    private void nuke(ServerPlayer p) {
        if (!spend(p, LanternRules.nukeCost(mastery(p)))) return;
        Vec3 spot = lookSpot(p, LanternRules.LOOK_RANGE);
        NUKES.add(new Nuke(p.getUUID(), p.serverLevel(), spot));
        p.level().playSound(null, BlockPos.containing(spot), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 3f, 0.5f);
        p.level().playSound(null, BlockPos.containing(spot), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 3f, 0.8f);
    }

    // Spread over ticks, so a crowd can't freeze the server. Breaks no blocks. Hurts everyone, you too, unless they're in a bubble.
    private static void tickNukes(TickEvent.ServerTickEvent e) {
        Iterator<Nuke> it = NUKES.iterator();
        while (it.hasNext()) {
            Nuke n = it.next();
            ServerPlayer owner = e.getServer().getPlayerList().getPlayer(n.owner);
            if (n.fuse > 0) {
                Fx.particles(n.level, GREEN, n.at.add(0, 0.5, 0), 6, 0.5);
                if (--n.fuse == 0) {
                    n.level.playSound(null, BlockPos.containing(n.at), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 8f, 0.5f);
                    n.level.playSound(null, BlockPos.containing(n.at), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 8f, 0.6f);
                    n.hit.addAll(n.level.getEntitiesOfClass(LivingEntity.class, new AABB(n.at, n.at).inflate(Setting.LANTERN_NUKE_RADIUS.i()), LivingEntity::isAlive));
                }
                continue;
            }
            // a ring of green going outwards, no flash
            if (n.ring < 40) {
                n.ring++;
                double reach = n.ring * 2.5;
                for (int a = 0; a < 48; a++) {
                    double angle = Math.PI * 2 * a / 48;
                    Fx.particles(n.level, GREEN, n.at.add(Math.cos(angle) * reach, 1.5, Math.sin(angle) * reach), 2, 0.4);
                }
            }
            for (int i = 0; i < LanternRules.NUKE_PER_TICK && !n.hit.isEmpty(); i++) {
                LivingEntity t = n.hit.poll();
                double far = t.position().distanceTo(n.at);
                if (!t.isAlive() || far > Setting.LANTERN_NUKE_RADIUS.i() || BubbleEntity.inAny(t)) continue;
                t.invulnerableTime = 0;
                constructing = true;
                try {
                    t.hurt(owner != null ? n.level.damageSources().explosion(owner, owner) : n.level.damageSources().explosion(null, null), LanternRules.nukeDamage(far));
                } finally {
                    constructing = false;
                }
            }
            if (n.hit.isEmpty() && n.ring >= 40) it.remove();
        }
    }

    // ---- the loop

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!NUKES.isEmpty()) tickNukes(e);
        if (!RINGS.isEmpty()) tickRings(e);
        if (ACTIVE.isEmpty()) return;
        int tick = e.getServer().getTickCount();
        Iterator<Map.Entry<UUID, Lantern>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Lantern> entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            Lantern l = entry.getValue();
            if (p == null || !p.isAlive() || !POWER.owns(p) || HiddenRuntime.switchedOff(p)) {
                clear(l);
                it.remove();
                continue;
            }
            if (l.idle() && !l.heldKey && tick % 100 == 0 && p.level().getGameTime() - l.lastUsed > 200) {
                it.remove();
                continue;
            }
            if (l.swingCooldown > 0) l.swingCooldown--;
            if (l.sword != null) {
                Vec3 at = swordSpot(p);
                l.sword.setPos(at.x, at.y, at.z);
                l.sword.setYRot(p.getYRot());
            }
            if (l.hand != null) POWER.tickHand(p, l);
            if (l.gun != null) POWER.tickGun(p, l);
            if (l.heldKey || !l.forming.isEmpty()) POWER.tickDaggers(p, l);
            if (l.rain > 0 && tick % 2 == 0) POWER.tickRain(p, l);
            if (!l.turrets.isEmpty()) POWER.tickTurrets(p, l, tick);
            if (l.bubble != null && tick % 20 == 0) {
                POWER.setBattery(p, POWER.battery(p) - LanternRules.BUBBLE_PER_SECOND);
                if (POWER.battery(p) <= 0) popBubble(l);
            }
        }
    }

    private static void clear(Lantern l) {
        if (l.sword != null) l.sword.discard();
        if (l.gun != null) l.gun.discard();
        if (l.hand != null) l.hand.discard();
        if (l.bubble != null) l.bubble.discard();
        l.forming.forEach(Entity::discard);
        l.turrets.forEach(t -> t.entity.discard());
    }

    // ---- how you get it: get a Warden under 5 hearts, then have the will to walk away

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof Warden warden) || !(e.getSource().getEntity() instanceof ServerPlayer p) || POWER.owns(p)) return;
        if (warden.getHealth() - e.getAmount() < LanternRules.TRIAL_HEALTH && warden.getHealth() - e.getAmount() > 0) TRIALS.put(p.getUUID(), new Trial(warden));
    }

    @Override
    public void watchSecond(ServerPlayer p) {
        Trial trial = TRIALS.get(p.getUUID());
        if (trial == null) return;
        Warden w = trial.warden;
        if (!w.isAlive() || w.level() != p.level()) {
            TRIALS.remove(p.getUUID());
            return;
        }
        if (w.distanceTo(p) < LanternRules.TRIAL_WALK) return;
        TRIALS.remove(p.getUUID());
        w.setNoAi(true);
        w.setPose(Pose.DIGGING);
        w.playSound(SoundEvents.WARDEN_DIG, 5f, 1f);
        RINGS.add(new Ring(p.getUUID(), w));
        tell(p, "You walked away from the hardest thing in the world. Something saw that.");
    }

    // The ring comes down in a spiral, then it's on your hand.
    private static void tickRings(TickEvent.ServerTickEvent e) {
        Iterator<Ring> it = RINGS.iterator();
        while (it.hasNext()) {
            Ring r = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(r.player);
            if (++r.age == 100 || p == null) r.warden.discard();
            if (p == null) {
                it.remove();
                continue;
            }
            double height = (120 - r.age) * 0.4;
            double angle = r.age * 0.5;
            Fx.particles(p.serverLevel(), GREEN, p.position().add(Math.cos(angle) * 1.2, 1 + height, Math.sin(angle) * 1.2), 6, 0.1);
            if (r.age % 20 == 0) p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1f, 1f + r.age * 0.005f);
            if (r.age >= 120) {
                it.remove();
                POWER.unlock(p);
                Fx.give(p, new ItemStack(SuitItems.POWER_BATTERY.get()));
            }
        }
    }

    // Your oath in chat. Others still read it. With the lantern in your off hand it recharges you, over a beaten Sinestro it ends him.
    @SubscribeEvent
    public static void onChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        String said = e.getRawText();
        if (CHOOSING.remove(p.getUUID())) {
            e.setCanceled(true);
            if (p.getServer() != null) p.getServer().execute(() -> POWER.choseOath(p, said));
            return;
        }
        if (POWER.owns(p) && p.getServer() != null && LanternRules.oathSaid(said, POWER.oathOf(p))) p.getServer().execute(() -> POWER.oathSaid(p));
    }

    private void choseOath(ServerPlayer p, String said) {
        String clean = said.trim();
        if (!clean.equalsIgnoreCase("keep") && !clean.isEmpty()) data(p).putString(OATH, clean.length() > 200 ? clean.substring(0, 200) : clean);
        tell(p, "Your oath is: \"" + oathOf(p) + "\" Crouch and use the lantern to change it.");
        sendState(p);
    }

    public void oathSaid(ServerPlayer p) {
        SinestroMob s = FIGHTS.get(p.getUUID());
        if (s != null && s.isBeaten() && s.distanceTo(p) <= 32) {
            fightOver(p.getUUID());
            s.discard();
            setBattery(p, maxBattery(p));
            awaken(p);
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            return;
        }
        if (!p.getOffhandItem().is(SuitItems.POWER_BATTERY.get())) return;
        setBattery(p, maxBattery(p));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.3f);
        Fx.particles(p.serverLevel(), GREEN, p.position().add(0, 1, 0), 60, 0.8);
    }

    // Nothing teleports across a bubble's wall.
    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl e) {
        if (!BubbleEntity.ALL.isEmpty() && BubbleEntity.crossing(e.getEntity().level(), e.getPrev(), e.getTarget())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit e) {
        if (!BubbleEntity.ALL.isEmpty() && BubbleEntity.crossing(e.getEntity().level(), e.getPrev(), e.getTarget())) e.setCanceled(true);
    }

    // Construct tools: an empty hand mines like an iron pickaxe, axe or shovel, whichever the block wants.
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        if (!p.getMainHandItem().isEmpty() || !AbilityUtil.hasPower(p, POWER.id) || !toolBlock(e.getState())) return;
        e.setNewSpeed(Math.max(e.getNewSpeed(), Tiers.IRON.getSpeed()));
    }

    @SubscribeEvent
    public static void onHarvest(PlayerEvent.HarvestCheck e) {
        Player p = e.getEntity();
        if (p.getMainHandItem().isEmpty() && toolBlock(e.getTargetBlock()) && AbilityUtil.hasPower(p, POWER.id) && TierSortingRegistry.isCorrectTierForDrops(Tiers.IRON, e.getTargetBlock())) e.setCanHarvest(true);
    }

    private static boolean toolBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Warden w) TRIALS.values().removeIf(t -> t.warden == w);
        // SinestroMob sees you've died by itself, on its next tick
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Lantern l = ACTIVE.remove(e.getEntity().getUUID());
        if (l != null) clear(l);
        TRIALS.remove(e.getEntity().getUUID());
        POWER.lastSent.remove(e.getEntity().getUUID());
        CHOOSING.remove(e.getEntity().getUUID());
        SinestroMob s = FIGHTS.remove(e.getEntity().getUUID());
        if (s != null) s.discard();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ACTIVE.values().forEach(GreenLantern::clear);
        ACTIVE.clear();
        TRIALS.clear();
        FIGHTS.clear();
        POWER.lastSent.clear();
        RINGS.clear();
        NUKES.clear();
        BubbleEntity.ALL.clear();
    }
}
