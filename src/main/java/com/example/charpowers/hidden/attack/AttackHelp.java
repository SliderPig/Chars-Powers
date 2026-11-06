package com.example.charpowers.hidden.attack;

import com.example.charpowers.hidden.HiddenConfig;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.HitMath;
import com.example.charpowers.hidden.model.MeleeRule;
import com.example.charpowers.hidden.model.MobRule;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.Rank;
import com.example.charpowers.hidden.model.SpotRule;
import com.example.charpowers.hidden.ranks.RankHelp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Shared bits for the attack powers (#302-381), so each power is only a few lines.
// Damage is written in hearts. Every hit goes through hit(), so the attackDamageScale setting and the
// attacker's boosts (Iron Fist, Execute, Berserker...) apply to it.
public final class AttackHelp {

    // --- registering ---

    // A key power that mobs use when what they fight is within `mobRange` (and in sight).
    public static void attack(Rank r, int n, String id, int cooldownTicks, double mobRange, Supplier<Item> icon, Consumer<PowerUser> use) {
        HiddenPowers.register(PowerDef.key(id, n, r, MobRule.ATTACK, cooldownTicks), icon, new Move(mobRange) {
            @Override public void onKey(PowerUser u) { use.accept(u); }
        });
    }

    public static void attack(Rank r, int n, String id, int cooldownTicks, Supplier<Item> icon, Move fx) {
        HiddenPowers.register(PowerDef.key(id, n, r, MobRule.ATTACK, cooldownTicks), icon, fx);
    }

    // An always-on attack power (works on mobs too).
    public static void passive(Rank r, int n, String id, Supplier<Item> icon, PowerEffect fx) {
        HiddenPowers.register(PowerDef.always(id, n, r, MobRule.PASSIVE), icon, fx);
    }

    // A key power's effect that knows how close a mob must be to use it.
    public abstract static class Move implements PowerEffect {
        private final double range;

        protected Move(double mobRange) { this.range = mobRange; }

        @Override public double mobRange() { return range; }
    }

    // --- key presses (so Blood Pact pays once per press, not once per thing hit) ---

    private static final Map<LivingEntity, Long> PRESS = new WeakHashMap<>();

    // Called just before any key power runs (player key or mob use).
    public static void markPress(LivingEntity e) { PRESS.put(e, e.level().getGameTime()); }

    public static long pressOf(LivingEntity e, long now) {
        Long p = PRESS.get(e);
        return p == null ? now : p;
    }

    // --- aiming ---

    // What the holder is attacking: for a player the foe under the crosshair, for a mob what it fights (in range).
    @Nullable
    public static LivingEntity aim(PowerUser u, double range) {
        if (u.mob() != null) {
            LivingEntity t = u.mob().getTarget();
            return t != null && t.isAlive() && t.distanceTo(u.entity()) <= range && RankHelp.isFoe(u, t) ? t : null;
        }
        LivingEntity t = Fx.lookTarget(u.entity(), range);
        return t != null && RankHelp.isFoe(u, t) ? t : null;
    }

    // The spot the holder points at: a player's crosshair block (or as far as `range`), a mob's target's feet.
    public static Vec3 lookSpot(PowerUser u, double range) {
        LivingEntity e = u.entity();
        if (u.mob() != null && u.mob().getTarget() != null && u.mob().getTarget().distanceTo(e) <= range) {
            return u.mob().getTarget().position();
        }
        Vec3 eye = e.getEyePosition();
        Vec3 end = eye.add(e.getViewVector(1f).scale(range));
        BlockHitResult hit = e.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    // The direction the holder faces (a mob: toward what it fights).
    public static Vec3 facing(PowerUser u) {
        LivingEntity e = u.entity();
        if (u.mob() != null && u.mob().getTarget() != null) {
            return u.mob().getTarget().getEyePosition().subtract(e.getEyePosition()).normalize();
        }
        return e.getViewVector(1f);
    }

    // --- hitting ---

    // True only while hit() is landing a hit, so boosts know it came from a power and Counter / Double Tap
    // never answer a power's hit (no endless hit-back loops).
    public static boolean POWER_HIT = false;

    public static boolean hit(PowerUser u, LivingEntity t, float hearts) {
        return hit(u, t, hearts, false);
    }

    public static boolean hit(PowerUser u, LivingEntity t, float hearts, boolean ignoreArmour) {
        if (!RankHelp.isFoe(u, t)) return false;
        float dmg = HitMath.powerDamage(hearts, HiddenConfig.ATTACK_DAMAGE_SCALE.get());
        LivingEntity me = u.entity();
        DamageSource src;
        if (ignoreArmour) src = t.damageSources().indirectMagic(me, me);
        else if (me instanceof Player p) src = t.damageSources().playerAttack(p);
        else src = t.damageSources().mobAttack(me);
        t.invulnerableTime = 0; // multi-hit powers land every hit
        boolean before = POWER_HIT;
        POWER_HIT = true;
        try {
            return t.hurt(src, dmg);
        } finally {
            POWER_HIT = before;
        }
    }

    // Every foe within `r` blocks of `c`.
    public static List<LivingEntity> around(PowerUser u, Vec3 c, double r) {
        return u.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> RankHelp.isFoe(u, e) && e.getBoundingBox().getCenter().distanceTo(c) <= r + e.getBbWidth() / 2);
    }

    // Every foe in front of the holder within `range`, inside a cone `degrees` wide.
    public static List<LivingEntity> cone(PowerUser u, double range, double degrees) {
        Vec3 eye = u.entity().getEyePosition();
        Vec3 dir = facing(u);
        double minDot = Math.cos(Math.toRadians(degrees / 2));
        return around(u, eye, range).stream().filter(e -> {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            return to.lengthSqr() < 1 || to.normalize().dot(dir) >= minDot;
        }).toList();
    }

    // Every foe touched by a straight line from `from` going `length` blocks along `dir`, `width` blocks thick.
    public static List<LivingEntity> line(PowerUser u, Vec3 from, Vec3 dir, double length, double width) {
        Vec3 to = from.add(dir.normalize().scale(length));
        AABB area = new AABB(from, to).inflate(width);
        return u.level().getEntitiesOfClass(LivingEntity.class, area, e -> {
            if (!RankHelp.isFoe(u, e)) return false;
            AABB box = e.getBoundingBox().inflate(width / 2);
            return box.contains(from) || box.clip(from, to).isPresent();
        });
    }

    // --- moving ---

    // Teleports the holder behind `t` (or beside / in front, if a wall is behind it), facing it.
    // Never into blocks, never behind the Ender Dragon. False = nowhere safe, nothing happened.
    public static boolean teleportBehind(PowerUser u, LivingEntity t) {
        if (t instanceof EnderDragon) return false;
        LivingEntity me = u.entity();
        Vec3 back = Vec3.directionFromRotation(0, t.getYRot()).scale(-1.5 - t.getBbWidth() / 2);
        Vec3 side = new Vec3(-back.z, 0, back.x);
        Vec3[] around = {back, side, side.scale(-1), back.scale(-1)};
        List<Vec3> spots = new ArrayList<>();
        for (Vec3 off : around) for (int dy : new int[]{0, 1, -1}) spots.add(t.position().add(off).add(0, dy, 0));
        int[][] candidates = new int[spots.size()][];
        for (int i = 0; i < spots.size(); i++) candidates[i] = new int[]{i};
        Vec3 from = t.getBoundingBox().getCenter();
        int[] pick = SpotRule.pick(candidates, c -> openFor(me, spots.get(c[0])) && clearPath(me, from, spots.get(c[0]).add(0, 0.9, 0)));
        if (pick == null) return false;
        Vec3 to = spots.get(pick[0]);
        Vec3 look = t.getEyePosition().subtract(to.add(0, me.getEyeHeight(), 0));
        float yaw = (float) (Math.toDegrees(Math.atan2(-look.x, look.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))));
        Fx.particles(u.level(), net.minecraft.core.particles.ParticleTypes.PORTAL, me.position().add(0, 1, 0), 20, 0.4);
        if (me instanceof ServerPlayer p) p.teleportTo(u.level(), to.x, to.y, to.z, yaw, pitch);
        else {
            me.moveTo(to.x, to.y, to.z, yaw, pitch);
            me.setYHeadRot(yaw);
        }
        me.fallDistance = 0;
        Fx.sound(me, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.4f);
        return true;
    }

    // Room for `e` standing at `at`, with something solid to stand on within 3 blocks below.
    private static boolean openFor(LivingEntity e, Vec3 at) {
        if (!roomFor(e, at)) return false;
        BlockPos feet = BlockPos.containing(at);
        for (int k = 1; k <= 3; k++) {
            BlockPos below = feet.below(k);
            if (!e.level().getBlockState(below).getCollisionShape(e.level(), below).isEmpty()) return true;
        }
        return false;
    }

    // `e`'s body fits at `at` (no blocks in the way).
    public static boolean roomFor(LivingEntity e, Vec3 at) {
        return e.level().noCollision(e, e.getBoundingBox().move(at.subtract(e.position())));
    }

    // No block between two points (so a teleport never goes through a wall).
    public static boolean clearPath(LivingEntity e, Vec3 from, Vec3 to) {
        return e.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS;
    }

    // The highest spot up to `up` blocks above `base` that the holder fits in and can get to straight up from `base`.
    public static Vec3 highestRoom(LivingEntity e, Vec3 base, int up) {
        for (int h = up; h > 0; h--) {
            Vec3 at = base.add(0, h, 0);
            if (roomFor(e, at) && clearPath(e, base.add(0, 0.1, 0), at.add(0, e.getBbHeight(), 0))) return at;
        }
        return base;
    }

    // Is this a real close-up punch/swing by `by` (not a power's hit, not an arrow, not thorns)?
    public static boolean isPunch(LivingEntity by, DamageSource s) {
        return MeleeRule.isPunch(POWER_HIT, s.getDirectEntity() == by && s.getEntity() == by,
                s.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) || s.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK));
    }

    // Bosses can be hurt but never grabbed, lifted or thrown.
    public static boolean isBoss(Entity e) {
        return e instanceof EnderDragon || e instanceof WitherBoss || e instanceof Warden || e instanceof ElderGuardian;
    }

    // Holds someone still: can't walk or jump (no particles).
    // Players: Jump Boost 128 turns into -128 on their PC (no jumping there). Mobs jump on the server, where 128 would
    // launch them into the sky, so for mobs any upward push is cancelled every tick instead.
    public static void hold(PowerUser u, LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false, false));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        if (t instanceof Player) {
            t.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false, false));
            return;
        }
        for (int i = 1; i <= ticks; i++) {
            Later.after(u, i, () -> {
                if (t.isAlive() && t.getDeltaMovement().y > 0) t.setDeltaMovement(Vec3.ZERO);
            });
        }
    }

    public static void effect(LivingEntity t, MobEffect e, int ticks, int level) {
        t.addEffect(new MobEffectInstance(e, ticks, level, false, true, true));
    }

    // Knocks `t` away from `from` (sideways strength + a little up), like a big punch.
    public static void knock(LivingEntity t, Vec3 from, double strength, double up) {
        if (strength <= 0 && up <= 0) return;
        Vec3 away = t.position().subtract(from).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(strength);
        RankHelp.launch(t, t.getDeltaMovement().add(away.x, up, away.z));
    }

    // --- showing things (never flashing) ---

    // A warning on the ground before something big lands: a steady dark red ring and a low charge-up sound,
    // repeated every 5 ticks for `ticks`, so whoever is standing there can get out of the way.
    public static void warn(PowerUser u, Vec3 at, double radius, int ticks) {
        u.level().playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_CHARGE, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.6f);
        for (int t = 0; t < ticks; t += 5) Later.after(u, t + 1, () -> ring(u.level(), at, radius));
    }

    private static final DustParticleOptions WARN = new DustParticleOptions(new Vector3f(0.45f, 0f, 0f), 1.6f);

    public static void ring(ServerLevel l, Vec3 at, double radius) {
        int n = (int) Math.max(12, radius * 8);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            Fx.particles(l, WARN, at.add(Math.cos(a) * radius, 0.15, Math.sin(a) * radius), 1, 0);
        }
        Fx.particles(l, WARN, at.add(0, 0.15, 0), 4, radius / 3);
    }

    // A trail of particles from one spot to another (things thrown or shot by a power).
    public static void trail(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions p) {
        Vec3 d = to.subtract(from);
        int steps = (int) Math.min(120, Math.max(1, d.length() * 2));
        for (int i = 0; i <= steps; i++) Fx.particles(l, p, from.add(d.scale(i / (double) steps)), 1, 0.02);
    }

    public static void sound(PowerUser u, Vec3 at, SoundEvent s, float volume, float pitch) {
        u.level().playSound(null, at.x, at.y, at.z, s, net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
    }

    // The top of the ground at x/z, searching from a bit above `nearY` down (for meteors and landings).
    public static Vec3 ground(ServerLevel l, double x, double z, double nearY) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, nearY + 12, z).mutable();
        for (int i = 0; i < 40 && p.getY() > l.getMinBuildHeight(); i++, p.move(0, -1, 0)) {
            if (!l.getBlockState(p).getCollisionShape(l, p).isEmpty() && l.getBlockState(p.above()).getCollisionShape(l, p.above()).isEmpty()) {
                return new Vec3(x, p.getY() + 1, z);
            }
        }
        return new Vec3(x, nearY, z);
    }

    // --- display entities (floating blocks, swords, spikes); temporary, never saved ---

    @Nullable
    public static Display.BlockDisplay blockDisplay(ServerLevel l, BlockState state, Vec3 at) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(l);
        if (d == null) return null;
        d.setPos(at.x, at.y, at.z);
        CompoundTag tag = d.saveWithoutId(new CompoundTag());
        tag.put("block_state", NbtUtils.writeBlockState(state));
        d.load(tag);
        d.addTag(com.example.charpowers.hidden.HiddenEvents.TEMP_TAG);
        l.addFreshEntity(d);
        return d;
    }

    @Nullable
    public static Display.ItemDisplay itemDisplay(ServerLevel l, ItemStack stack, Vec3 at) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(l);
        if (d == null) return null;
        d.setPos(at.x, at.y, at.z);
        CompoundTag tag = d.saveWithoutId(new CompoundTag());
        tag.put("item", stack.save(new CompoundTag()));
        d.load(tag);
        d.addTag(com.example.charpowers.hidden.HiddenEvents.TEMP_TAG);
        l.addFreshEntity(d);
        return d;
    }

    private AttackHelp() {}
}
