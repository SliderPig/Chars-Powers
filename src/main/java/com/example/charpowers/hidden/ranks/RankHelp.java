package com.example.charpowers.hidden.ranks;

import com.example.charpowers.Net;
import com.example.charpowers.hidden.HiddenEvents;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.FoeRule;
import com.example.charpowers.hidden.model.MobRule;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.Rank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.example.charpowers.hidden.f.FHelp.*;

// Shared bits for ranks E to Z, so each power is only a few lines. (F has its own, f/FHelp; we reuse its timers.)
public final class RankHelp {

    // --- registering ---

    static void key(Rank r, int n, String id, MobRule rule, int cooldown, Supplier<Item> icon, Consumer<PowerUser> use) {
        HiddenPowers.register(PowerDef.key(id, n, r, rule, cooldown), icon, new PowerEffect() {
            @Override public void onKey(PowerUser u) { use.accept(u); }
        });
    }

    static void key(Rank r, int n, String id, MobRule rule, int cooldown, Supplier<Item> icon, PowerEffect fx) {
        HiddenPowers.register(PowerDef.key(id, n, r, rule, cooldown), icon, fx);
    }

    static void always(Rank r, int n, String id, MobRule rule, Supplier<Item> icon, PowerEffect fx) {
        HiddenPowers.register(PowerDef.always(id, n, r, rule), icon, fx);
    }

    // An always-on power that does nothing on the server: it all happens on the player's own PC
    // (movement and things on screen, see client/ClientMovement and client/ClientHud).
    static void onPc(Rank r, int n, String id, Supplier<Item> icon) {
        always(r, n, id, MobRule.CARRY, icon, new PowerEffect() {});
    }

    // Same, for PC powers that slow your fall (Glide, Umbrella, Wall Slide, Wall Run): the server works out fall damage
    // by itself, so here a slow, gentle fall (less than about 0.25 blocks a tick) doesn't count as falling.
    static void onPcSoftFall(Rank r, int n, String id, Supplier<Item> icon) {
        always(r, n, id, MobRule.CARRY, icon, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                LivingEntity e = u.entity();
                double dy = e.getY() - u.state().getDouble("lastY");
                u.state().putDouble("lastY", e.getY());
                if (!e.onGround() && dy < 0 && dy > -0.25) e.fallDistance = 0;
            }
        });
    }

    // --- talking to the holder ---

    // A short message over the hotbar (players only).
    static void tell(PowerUser u, String text) {
        if (u.player() != null) u.player().displayClientMessage(Component.literal(text), true);
    }

    // A line on the player's screen (repeat it at least every 2 s or it fades out). Empty text = remove it.
    static void hud(PowerUser u, String key, String text) {
        if (u.player() != null) Net.sendTo(u.player(), new Net.Hud(key, text));
    }

    // The key did nothing (no target, wrong place...): give the cooldown back so the player can try again soon.
    public static void refund(PowerUser u) { u.state().putLong("cd", u.now() + 10); }

    // --- who to use a power on ---

    // Things a power may hit: everything living (players, animals, villagers, bosses, other mobs) except the holder,
    // their own helpers and pets, anyone on their /team and a teammate's pet. The same rule for players and mobs
    // (user: "everything unless they're on a team", "no discriminatory").
    public static boolean isFoe(PowerUser u, Entity e) {
        LivingEntity me = u.entity();
        boolean living = e instanceof LivingEntity;
        boolean creative = e instanceof Player p && (p.isCreative() || p.isSpectator());
        boolean alliedPet = e instanceof TamableAnimal t && t.isTame() && t.getOwner() != null && t.getOwner().isAlliedTo(me);
        return FoeRule.hittable(new FoeRule.Facts(living, e == me, living && ((LivingEntity) e).isAlive(),
                e instanceof ArmorStand, creative, isMine(u, e), e.isAlliedTo(me), alliedPet));
    }

    static List<LivingEntity> foes(PowerUser u, double range) {
        return u.level().getEntitiesOfClass(LivingEntity.class, u.entity().getBoundingBox().inflate(range),
                e -> isFoe(u, e) && e.distanceTo(u.entity()) <= range);
    }

    // Monsters (and anything already fighting the holder).
    static List<LivingEntity> hostiles(PowerUser u, double range) {
        return foes(u, range).stream().filter(e -> !u.isPlayer() || e instanceof Enemy
                || (e instanceof Mob m && m.getTarget() == u.entity())).toList();
    }

    // What the holder is aiming at: for a player the living thing under the crosshair, for a mob what it fights.
    @Nullable
    static LivingEntity target(PowerUser u, double range) {
        LivingEntity t = Fx.lookTarget(u.entity(), range);
        return t != null && (isFoe(u, t) || !u.isPlayer()) && t != u.entity() ? t : null;
    }

    // Helpers a power summons for its holder (ghost wolf, bees, ghosts) remember who they belong to.
    public static final String OWNER = "charpowers.owner";

    public static void makeMine(PowerUser u, Entity e) {
        e.getPersistentData().putUUID(OWNER, u.entity().getUUID());
        markTemp(e);
    }

    public static boolean isMine(PowerUser u, Entity e) {
        CompoundTag d = e.getPersistentData();
        if (d.hasUUID(OWNER) && d.getUUID(OWNER).equals(u.entity().getUUID())) return true;
        return e instanceof TamableAnimal t && u.entity().getUUID().equals(t.getOwnerUUID());
    }

    // --- moving things ---

    public static void launch(Entity e, Vec3 velocity) {
        e.setDeltaMovement(velocity);
        e.hurtMarked = true; // tells the player's PC about the new speed
        e.hasImpulse = true;
    }

    // Moves someone to a spot (players see it too) and forgets any fall they were in.
    public static void teleport(Entity e, Vec3 to) {
        e.teleportTo(to.x, to.y, to.z);
        e.fallDistance = 0;
    }

    // Would this entity fit at that spot (feet position)?
    static boolean fits(Entity e, Vec3 feet) {
        AABB box = e.getBoundingBox().move(feet.subtract(e.position()));
        return e.level().noCollision(e, box) && !e.level().containsAnyLiquid(box);
    }

    // The furthest spot up to `max` blocks along `dir` (sideways only) that can be reached without going through
    // a wall, or null.
    @Nullable
    static Vec3 spotAlong(Entity e, Vec3 dir, double max) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1.0E-4) return null;
        flat = flat.normalize();
        Vec3 start = e.position();
        Vec3 best = null;
        for (double d = 0.5; d <= max + 1.0E-3; d += 0.5) {
            Vec3 at = start.add(flat.scale(d));
            if (!clearPath(e, start.add(0, 0.5, 0), at.add(0, 0.5, 0))) break;
            if (fits(e, at)) best = at;
            else if (fits(e, at.add(0, 1, 0)) && clearPath(e, at.add(0, 0.5, 0), at.add(0, 1.5, 0))) best = at.add(0, 1, 0);
        }
        return best;
    }

    static boolean clearPath(Entity e, Vec3 from, Vec3 to) {
        BlockHitResult hit = e.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.MISS;
    }

    // The block the holder is looking at (players) or the block under its target (mobs), within range.
    @Nullable
    static BlockHitResult lookBlock(PowerUser u, double range) {
        LivingEntity e = u.entity();
        Vec3 eye = e.getEyePosition();
        Vec3 end;
        if (u.mob() != null && u.mob().getTarget() != null) end = u.mob().getTarget().position().add(0, -0.5, 0);
        else end = eye.add(e.getViewVector(1).scale(range));
        BlockHitResult hit = e.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    // Two entities trade places.
    static void swap(LivingEntity a, LivingEntity b) {
        Vec3 pa = a.position(), pb = b.position();
        teleport(a, pb);
        teleport(b, pa);
        Fx.sound(a, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.2f);
        Fx.particles((ServerLevel) a.level(), net.minecraft.core.particles.ParticleTypes.PORTAL, pa.add(0, 1, 0), 20, 0.4);
        Fx.particles((ServerLevel) a.level(), net.minecraft.core.particles.ParticleTypes.PORTAL, pb.add(0, 1, 0), 20, 0.4);
    }

    // --- attributes (speed etc.) ---

    static UUID uuidFor(String name) {
        return UUID.nameUUIDFromBytes(("charpowers:" + name).getBytes(StandardCharsets.UTF_8));
    }

    // Puts (or changes) a "multiply by 1 + amount" boost on an attribute. amount 0 = take it off.
    public static void boost(LivingEntity e, Attribute attr, String name, double amount) {
        AttributeInstance a = e.getAttribute(attr);
        if (a == null) return;
        UUID id = uuidFor(name);
        AttributeModifier now = a.getModifier(id);
        if (amount == 0) {
            if (now != null) a.removeModifier(id);
            return;
        }
        if (now != null && Math.abs(now.getAmount() - amount) < 1.0E-4) return;
        if (now != null) a.removeModifier(id);
        a.addTransientModifier(new AttributeModifier(id, "charpowers " + name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    // --- effects ---

    // Night vision that never flickers: Minecraft's night vision pulses during its last 10 seconds, which the
    // my friend can't have (no flashing!). So it's given with plenty to spare and taken off when time's up.
    static void steadyNightVision(PowerUser u, int ticks) {
        u.entity().addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks + 400, 0, false, false, true));
        startFor(u, "nv", ticks);
        u.state().putBoolean("nvOn", true);
    }

    // Call from tick(): takes the night vision off when its time is up.
    static void nightVisionTick(PowerUser u) {
        if (u.state().getBoolean("nvOn") && !running(u, "nv")) stopNightVision(u);
    }

    static void stopNightVision(PowerUser u) {
        if (!u.state().getBoolean("nvOn")) return;
        u.state().putBoolean("nvOn", false);
        MobEffectInstance nv = u.entity().getEffect(MobEffects.NIGHT_VISION);
        if (nv != null && !nv.isVisible()) u.entity().removeEffect(MobEffects.NIGHT_VISION);
    }

    static void effect(LivingEntity e, net.minecraft.world.effect.MobEffect effect, int ticks, int level) {
        e.addEffect(new MobEffectInstance(effect, ticks, level, false, true, true));
    }

    // --- copies of the holder (Decoy, Mirage, Afterimage, Shed Skin) ---

    // A copy that stands still: for a player an armour stand wearing their head and armour,
    // for a mob another one of the same kind that doesn't move. Not saved; `ticks` later it's gone (see copiesTick).
    @Nullable
    static LivingEntity copyOf(PowerUser u, Vec3 at, String key, int ticks) {
        LivingEntity me = u.entity();
        LivingEntity copy;
        if (u.player() != null) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(u.level());
            if (stand == null) return null;
            CompoundTag t = stand.saveWithoutId(new CompoundTag());
            t.putBoolean("ShowArms", true);
            t.putBoolean("NoBasePlate", true);
            stand.load(t);
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            // the whole profile (with the skin), not just the name, so it shows your real face straight away
            head.getOrCreateTag().put("SkullOwner", net.minecraft.nbt.NbtUtils.writeGameProfile(new CompoundTag(), u.player().getGameProfile()));
            stand.setItemSlot(EquipmentSlot.HEAD, head);
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                stand.setItemSlot(s, me.getItemBySlot(s).copy());
            }
            copy = stand;
        } else {
            Entity made = me.getType().create(u.level());
            if (!(made instanceof Mob m)) return null;
            m.setNoAi(true);
            m.setPersistenceRequired();
            for (EquipmentSlot s : EquipmentSlot.values()) {
                m.setItemSlot(s, me.getItemBySlot(s).copy());
                m.setDropChance(s, 0f);
            }
            copy = m;
        }
        copy.moveTo(at.x, at.y, at.z, me.getYRot(), me.getXRot());
        copy.setYHeadRot(me.getYHeadRot());
        copy.setInvulnerable(true);
        copy.setSilent(true);
        makeMine(u, copy);
        u.level().addFreshEntity(copy);
        CompoundTag list = u.state().getCompound("copies");
        list.putLong(copy.getUUID() + "|" + key, u.now() + ticks);
        u.state().put("copies", list);
        return copy;
    }

    // Removes copies whose time is up (call from tick()); all = every copy, right now (power taken off).
    static void copiesTick(PowerUser u, boolean all) {
        if (!u.state().contains("copies")) return;
        CompoundTag list = u.state().getCompound("copies");
        for (String k : list.getAllKeys().toArray(new String[0])) {
            if (!all && u.now() < list.getLong(k)) continue;
            removeCopy(u, k);
            list.remove(k);
        }
    }

    static List<Entity> copies(PowerUser u, String key) {
        List<Entity> out = new ArrayList<>();
        if (!u.state().contains("copies")) return out;
        for (String k : u.state().getCompound("copies").getAllKeys()) {
            if (!k.endsWith("|" + key)) continue;
            Entity e = entity(u, k.substring(0, k.indexOf('|')));
            if (e != null) out.add(e);
        }
        return out;
    }

    static void removeCopies(PowerUser u, String key) {
        if (!u.state().contains("copies")) return;
        CompoundTag list = u.state().getCompound("copies");
        for (String k : list.getAllKeys().toArray(new String[0])) {
            if (!k.endsWith("|" + key)) continue;
            removeCopy(u, k);
            list.remove(k);
        }
    }

    private static void removeCopy(PowerUser u, String k) {
        Entity e = entity(u, k.substring(0, k.indexOf('|')));
        if (e != null) {
            Fx.particles(u.level(), net.minecraft.core.particles.ParticleTypes.POOF, e.position().add(0, 1, 0), 8, 0.3);
            e.discard();
        }
    }

    @Nullable
    static Entity entity(PowerUser u, String uuid) {
        try {
            return u.level().getEntity(UUID.fromString(uuid));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // --- lists of entity ids kept in a power's state (frozen mobs, lifted mobs...) ---

    static void remember(PowerUser u, String list, Entity e, long until) {
        CompoundTag t = u.state().getCompound(list);
        t.putLong(e.getUUID().toString(), until);
        u.state().put(list, t);
    }

    // Calls `each` for every remembered entity that's still around; `done` for ones whose time is up (then forgets them).
    static void forEachRemembered(PowerUser u, String list, Consumer<Entity> each, Consumer<Entity> done) {
        if (!u.state().contains(list)) return;
        CompoundTag t = u.state().getCompound(list);
        for (String k : t.getAllKeys().toArray(new String[0])) {
            Entity e = entity(u, k);
            boolean over = u.now() >= t.getLong(k) || e == null || !e.isAlive();
            if (over) {
                if (e != null) done.accept(e);
                t.remove(k);
            } else if (each != null) {
                each.accept(e);
            }
        }
    }

    // --- small checks ---

    static boolean inDarkCave(LivingEntity e) {
        BlockPos p = e.blockPosition();
        return !e.level().canSeeSky(p) && e.level().getMaxLocalRawBrightness(p) == 0;
    }

    static boolean isDay(ServerLevel l) { return l.isDay(); }

    // Gives a player an item, or drops it at a mob's feet (same as Fx.giveOrDrop, here for short).
    static void give(PowerUser u, ItemStack s) { Fx.giveOrDrop(u, s); }

    static boolean isPlayer(PowerUser u) { return u.player() != null; }

    @Nullable
    static ServerPlayer player(PowerUser u) { return u.player(); }

    private RankHelp() {}
}
