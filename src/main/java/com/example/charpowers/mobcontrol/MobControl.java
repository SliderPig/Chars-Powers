package com.example.charpowers.mobcontrol;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.mobcontrol.model.MobRules;
import com.example.charpowers.own.Minions;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.RaidWatch;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Control the mobs you meet. Steering only runs for someone riding or swapped, and pets are ordinary mobs (own/Minions).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class MobControl extends OwnPower {
    public static final MobControl POWER = new MobControl();
    private static final String TAMED = "tamed", PET = "pet";

    private static final Map<UUID, Mob> RIDING = new HashMap<>();
    private static final Map<UUID, Swap> SWAPS = new HashMap<>();

    private static final class Swap {
        final Mob mob;
        final long until;
        float forward, strafe;
        boolean jump;

        Swap(Mob mob, long until) {
            this.mob = mob;
            this.until = until;
        }
    }

    private MobControl() {
        super(Mastery.MOB_CONTROL, Items.LEAD);
    }

    @Override
    public String unlockMessage() { return "You've tamed one of everything. The wild ones have started looking at you differently."; }

    @Override
    public String awakenedMessage() { return "You won without lifting a finger. Bosses can be ridden and kept now, and you can command a whole crowd."; }

    private static MobRules.Tier tierOf(LivingEntity e) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return MobRules.tier(key == null ? "" : key.getPath(), e.getType().is(Tags.EntityTypes.BOSSES));
    }

    private static boolean flies(LivingEntity e) {
        return e instanceof FlyingMob || e instanceof FlyingAnimal || e instanceof Phantom;
    }

    @Override
    public void everySecond(ServerPlayer p) {
        if (!p.hasEffect(MobEffects.NIGHT_VISION) || p.getEffect(MobEffects.NIGHT_VISION).getDuration() < 300) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        }
        if (RIDING.containsKey(p.getUUID()) || SWAPS.containsKey(p.getUUID()) || !Minions.of(p.getUUID(), PET).isEmpty()) Mastery.add(p, id, 1);
        if (questReady(p)) quest(p);
        calmBrains(p);
    }

    // Brain mobs (warden, piglins, hoglins) never ask the target event, so the ones that should leave you alone are made to forget you.
    private void calmBrains(ServerPlayer p) {
        if (HiddenRuntime.switchedOff(p)) return;
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), PetMinds::usesBrain)) {
            if (spared(p, m)) PetMinds.calmDown(m, p);
        }
    }

    // The quest: a raid won with 5 pets doing all the fighting, and you never hit anything.
    private void quest(ServerPlayer p) {
        RaidWatch.Watch raid = RaidWatch.step(p);
        if (raid == null || !raid.over || !raid.won()) return;
        if (raid.didHit) tell(p, "You hit something yourself. That raid doesn't count.");
        else if (Minions.of(p.getUUID(), PET).size() < MobRules.QUEST_PETS) tell(p, "That took 5 pets at your side at the end. That raid doesn't count.");
        else awaken(p);
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        switch (ability) {
            case "command_mob" -> command(p);
            case "ride_mob" -> ride(p);
            case "pet_follower" -> pet(p);
            case "swap_minds" -> swap(p);
            default -> {}
        }
    }

    // Look at someone, press: the nearest mob goes for them. Awakened: all of them.
    private void command(ServerPlayer p) {
        int level = mastery(p);
        LivingEntity target = Fx.lookTarget(p, MobRules.commandRange(level));
        if (target == null) {
            whisper(p, "Look at who they should attack.");
            return;
        }
        List<Mob> mobs = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(MobRules.commandRange(level)), m -> m != target && m.isAlive() && !(m == p.getVehicle()));
        if (mobs.isEmpty()) {
            whisper(p, "No mobs near enough to hear you.");
            return;
        }
        Fx.sound(p, PowerSounds.ORDER_CHIRP, 0.7f, 1f);
        if (awakened(p)) {
            for (Mob m : mobs) PetMinds.aim(m, target);
            whisper(p, mobs.size() + " mobs obey.");
            return;
        }
        Mob nearest = null;
        for (Mob m : mobs) {
            if (nearest == null || m.distanceToSqr(p) < nearest.distanceToSqr(p)) nearest = m;
        }
        PetMinds.aim(nearest, target);
        whisper(p, nearest.getName().getString() + " obeys.");
    }

    private void ride(ServerPlayer p) {
        if (RIDING.remove(p.getUUID()) != null) {
            p.stopRiding();
            return;
        }
        LivingEntity target = Fx.lookTarget(p, Setting.MOB_REACH.i());
        if (!(target instanceof Mob mob)) {
            whisper(p, "Look at the mob you want to ride.");
            return;
        }
        if (!MobRules.canRide(tierOf(mob), flies(mob), mastery(p), awakened(p))) {
            whisper(p, "It won't have you yet.");
            return;
        }
        if (p.startRiding(mob, true)) RIDING.put(p.getUUID(), mob);
    }

    // Riding your own pet by right-clicking it with an empty hand (PetCare). The same rules as the ride key.
    public static boolean rideAsPet(ServerPlayer p, Mob mob) {
        if (!MobRules.canRide(tierOf(mob), flies(mob), POWER.mastery(p), POWER.awakened(p))) {
            whisper(p, "It won't have you yet.");
            return false;
        }
        if (!p.startRiding(mob, true)) return false;
        RIDING.put(p.getUUID(), mob);
        return true;
    }

    // Look at a mob, press: it's your pet. Press on your own pet to let it go.
    private void pet(ServerPlayer p) {
        LivingEntity target = Fx.lookTarget(p, Setting.MOB_REACH.i());
        if (!(target instanceof Mob mob)) {
            whisper(p, "Look at the mob you want to keep.");
            return;
        }
        if (Minions.isKind(mob, PET) && p.getUUID().equals(Minions.ownerOf(mob))) {
            Minions.release(mob);
            whisper(p, mob.getName().getString() + " is free to go.");
            return;
        }
        if (Minions.isMinion(mob)) {
            whisper(p, "That one already answers to someone.");
            return;
        }
        ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (typeKey != null && MobRules.NEVER_KEPT.contains(typeKey.getPath())) {
            whisper(p, "It's far too wild to follow anyone.");
            return;
        }
        if (!MobRules.canKeep(typeKey == null ? "" : typeKey.getPath(), tierOf(mob), mastery(p), awakened(p))) {
            whisper(p, "It won't have you yet.");
            return;
        }
        if (Minions.of(p.getUUID(), PET).size() >= MobRules.maxPets(mastery(p))) {
            whisper(p, "That's as many pets as you can keep.");
            return;
        }
        Minions.claim(mob, p, PET);
        Fx.sound(mob, PowerSounds.CLAIM_CHIRP, 0.8f, 1f);
        Minions.settle(mob);
        mob.setTarget(null);
        PetMinds.calmDown(mob, p);
        PetMinds.adopt(mob);
        whisper(p, mob.getName().getString() + " follows you now.");
    }

    private void swap(ServerPlayer p) {
        Swap current = SWAPS.remove(p.getUUID());
        if (current != null) {
            sendEyes(p, -1);
            return;
        }
        LivingEntity target = Fx.lookTarget(p, MobRules.commandRange(mastery(p)));
        if (!(target instanceof Mob mob) || tierOf(mob) == MobRules.Tier.BOSS) {
            whisper(p, "Look at the mob you want to be.");
            return;
        }
        SWAPS.put(p.getUUID(), new Swap(mob, p.level().getGameTime() + MobRules.swapTicks(mastery(p))));
        sendEyes(p, mob.getId());
    }

    private void sendEyes(ServerPlayer p, int entity) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("swap", entity);
        Net.toOwner(p, id.toString(), tag);
    }

    // While swapped, the keys you press go to the mob. The PC sends them each tick as "steer:forward:strafe:jump".
    @Override
    public void onClientAction(ServerPlayer p, String action) {
        Swap swap = SWAPS.get(p.getUUID());
        if (swap == null || !action.startsWith("steer:")) return;
        String[] parts = action.split(":");
        if (parts.length != 4) return;
        try {
            swap.forward = Math.max(-1f, Math.min(1f, Float.parseFloat(parts[1])));
            swap.strafe = Math.max(-1f, Math.min(1f, Float.parseFloat(parts[2])));
            swap.jump = "1".equals(parts[3]);
        } catch (NumberFormatException ignored) {
            // a bad message just doesn't steer
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!RIDING.isEmpty()) tickRiders(e);
        if (!SWAPS.isEmpty()) tickSwaps(e);
    }

    private static void tickRiders(TickEvent.ServerTickEvent e) {
        Iterator<Map.Entry<UUID, Mob>> it = RIDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Mob> entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            Mob mob = entry.getValue();
            if (p == null || p.getVehicle() != mob || !mob.isAlive()) {
                it.remove();
                continue;
            }
            steer(mob, p, p.zza, p.xxa, false);
        }
    }

    private static void tickSwaps(TickEvent.ServerTickEvent e) {
        Iterator<Map.Entry<UUID, Swap>> it = SWAPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Swap> entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            Swap swap = entry.getValue();
            if (p == null || !p.isAlive() || !swap.mob.isAlive() || p.level().getGameTime() >= swap.until || swap.mob.level() != p.level()) {
                it.remove();
                if (p != null) {
                    POWER.sendEyes(p, -1);
                    whisper(p, "Back in your own head.");
                }
                continue;
            }
            // too far and your PC would lose sight of it
            boolean far = swap.mob.distanceToSqr(p) > MobRules.BODY_LEASH * MobRules.BODY_LEASH;
            Vec3 back = far ? p.position().subtract(swap.mob.position()) : null;
            if (far && swap.forward > 0 && Vec3.directionFromRotation(0, p.getYRot()).dot(back) < 0) swap.forward = 0;
            steer(swap.mob, p, swap.forward, swap.strafe, swap.jump);
        }
    }

    // Moves a mob the way a player's keys say. Its own brain is told to sit this one out.
    private static void steer(Mob mob, ServerPlayer p, float forward, float strafe, boolean jump) {
        mob.getNavigation().stop();
        mob.setTarget(null);
        mob.setYRot(p.getYRot());
        mob.setYHeadRot(p.getYRot());
        mob.yBodyRot = p.getYRot();
        mob.setXRot(p.getXRot());
        double speed = Math.max(0.15, mob.getAttribute(Attributes.MOVEMENT_SPEED) == null ? 0.25 : mob.getAttributeValue(Attributes.MOVEMENT_SPEED)) * 1.4;
        double rad = Math.toRadians(p.getYRot());
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double dx = (-sin * forward + cos * strafe) * speed, dz = (cos * forward + sin * strafe) * speed;
        Vec3 old = mob.getDeltaMovement();
        double dy = old.y;
        if (flies(mob)) {
            dy = forward != 0 ? -Math.sin(Math.toRadians(p.getXRot())) * speed * forward : 0;
            mob.setNoGravity(true);
        } else if ((jump || mob.horizontalCollision) && mob.onGround() && (forward != 0 || strafe != 0 || jump)) {
            dy = 0.5;
        }
        mob.setDeltaMovement(dx, dy, dz);
        mob.hurtMarked = true;
    }

    // Mobs leave you alone. The bigger ones only at higher mastery, and if you hit one first, that's on you.
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getNewTarget() instanceof ServerPlayer p && e.getEntity() instanceof Mob mob && spared(p, mob)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getSource().getEntity() instanceof Mob mob && spared(p, mob)) {
            e.setCanceled(true);
            PetMinds.calmDown(mob, p); // brain mobs (warden, piglins) keep swinging unless they are made to forget you
        }
    }

    private static boolean spared(ServerPlayer p, Mob mob) {
        if (Minions.isMinion(mob) && p.getUUID().equals(Minions.ownerOf(mob))) return true;
        if (!POWER.owns(p) || HiddenRuntime.switchedOff(p) || mob.getLastHurtByMob() == p) return false;
        return MobRules.leavesYouAlone(tierOf(mob), POWER.mastery(p));
    }

    // Pets fight for you: whatever you hit, and whatever hits you.
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (Minions.noneLive()) return;
        LivingEntity hurt = e.getEntity();
        if (e.getSource().getEntity() instanceof ServerPlayer p && !(hurt instanceof Player other && other == p) && !Minions.isKind(hurt, PET)) {
            PetMinds.order(p.getUUID(), hurt);
        }
        if (hurt instanceof ServerPlayer p && e.getSource().getEntity() instanceof LivingEntity by && !Minions.isKind(by, PET)) {
            PetMinds.order(p.getUUID(), by);
        }
    }

    // How to get it: tame one of every tameable animal.
    @SubscribeEvent
    public static void onTame(AnimalTameEvent e) {
        if (!(e.getTamer() instanceof ServerPlayer p) || POWER.owns(p)) return;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getAnimal().getType());
        if (key == null || !MobRules.TAMEABLE.contains(key.getPath())) return;
        ListTag tamed = POWER.data(p).getList(TAMED, Tag.TAG_STRING);
        for (Tag t : tamed) {
            if (t.getAsString().equals(key.getPath())) return;
        }
        tamed.add(StringTag.valueOf(key.getPath()));
        POWER.data(p).put(TAMED, tamed);
        if (tamed.size() >= MobRules.TAMEABLE.size()) POWER.unlock(p);
        else whisper(p, "Tamed " + tamed.size() + " of the " + MobRules.TAMEABLE.size() + " kinds of animal.");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        RIDING.remove(e.getEntity().getUUID());
        SWAPS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        RIDING.clear();
        SWAPS.clear();
    }
}
