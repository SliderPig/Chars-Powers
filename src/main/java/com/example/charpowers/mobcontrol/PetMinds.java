package com.example.charpowers.mobcontrol;

import com.example.charpowers.CharPowers;
import com.example.charpowers.own.Minions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

// Makes a pet really a pet. Goal-based mobs (zombie, skeleton, spider...) already lose their hunting urges in Minions, but
// brain-based ones (warden, piglins, hoglins...) pick their victims from memories and anger, which no goal change touches.
// So every tick the pets' memories are cleaned: the owner and the owner's other pets are never a target, and nothing else
// is either unless the owner ordered it. Whatever a pet does to its owner or the owner's other pets is also stopped at the
// damage event (sonic boom, creeper blast, arrows, slime touch and so on).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class PetMinds {
    static final String PET = "pet";
    private static final int FOLLOW_FROM = 6, TELEPORT_FROM = 48, HOVER_TELEPORT = 24, ORDER_RANGE = 48, GUARD_RADIUS = 10, GUARD_LEASH = 16;

    // pet -> what it was told to fight
    private static final Map<Mob, LivingEntity> ORDERS = new WeakHashMap<>();
    private static final Set<UUID> HAD_PETS = new HashSet<>();

    private PetMinds() {}

    static boolean usesBrain(Mob m) {
        return m instanceof Warden || m instanceof AbstractPiglin || m instanceof Hoglin || m instanceof Zoglin
                || m instanceof Goat || m instanceof Axolotl || m instanceof Frog || m instanceof Sniffer;
    }

    // Friends of a pet: its owner and every other mob that owner keeps.
    static boolean friendly(Mob pet, Entity other) {
        UUID owner = Minions.ownerOf(pet);
        if (owner == null || other == null) return false;
        if (other.getUUID().equals(owner)) return true;
        return other instanceof Mob m && Minions.isMinion(m) && owner.equals(Minions.ownerOf(m));
    }

    // Right after a mob became a pet: a warden has to be stopped from digging into the ground (it does that after 100 ticks
    // once started, and the cooldown that prevents it only helps if it is there before the brain looks).
    public static void adopt(Mob mob) {
        if (mob instanceof Warden w) {
            // setDigCooldown only refreshes a cooldown that already exists, so put it there directly
            w.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, net.minecraft.util.Unit.INSTANCE, 1200L);
            if (w.getPose() == net.minecraft.world.entity.Pose.DIGGING) w.setPose(net.minecraft.world.entity.Pose.STANDING);
            if (w.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.DIG)) w.getBrain().setActiveActivityIfPossible(net.minecraft.world.entity.schedule.Activity.IDLE);
        }
    }

    // Tell a mob to go for something, in a way brain mobs obey as well.
    public static void aim(Mob mob, LivingEntity target) {
        if (mob == target || friendly(mob, target)) return;
        mob.setTarget(target);
        if (mob instanceof Warden w) {
            w.increaseAngerAt(target, 150, false);
            w.setAttackTarget(target);
        } else if (usesBrain(mob)) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    // The owner's order: all pets go for it.
    public static void order(UUID owner, LivingEntity target) {
        for (Mob pet : Minions.of(owner, PET)) {
            if (pet == target || friendly(pet, target) || Minions.STAY.equals(Minions.stance(pet))) continue;
            ORDERS.put(pet, target);
            aim(pet, target);
        }
    }

    // Drops whatever it was told to fight (Stay and Guard start from a calm pet).
    public static void stand(Mob pet) {
        ORDERS.remove(pet);
        pet.setTarget(null);
        mem(pet.getBrain(), MemoryModuleType.ATTACK_TARGET).ifPresent(t -> pet.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET));
    }

    // Brain.getMemory throws for memories the mob's brain doesn't have (a zombie has none), so ask carefully.
    private static <U> java.util.Optional<U> mem(Brain<?> brain, MemoryModuleType<U> type) {
        try {
            return brain.getMemory(type);
        } catch (IllegalStateException e) {
            return java.util.Optional.empty();
        }
    }

    // A mob that must leave this entity alone: it forgets them.
    public static void calmDown(Mob mob, Entity who) {
        if (mob.getTarget() == who) mob.setTarget(null);
        Brain<?> brain = mob.getBrain();
        mem(brain, MemoryModuleType.ATTACK_TARGET).filter(t -> t == who).ifPresent(t -> brain.eraseMemory(MemoryModuleType.ATTACK_TARGET));
        mem(brain, MemoryModuleType.ROAR_TARGET).filter(t -> t == who).ifPresent(t -> brain.eraseMemory(MemoryModuleType.ROAR_TARGET));
        mem(brain, MemoryModuleType.ANGRY_AT).filter(id -> id.equals(who.getUUID())).ifPresent(id -> brain.eraseMemory(MemoryModuleType.ANGRY_AT));
        if (mob instanceof Warden w) w.clearAnger(who);
        if (mob instanceof NeutralMob nm && who.getUUID().equals(nm.getPersistentAngerTarget())) nm.stopBeingAngry();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        long time = e.getServer().getTickCount();
        if (!Minions.noneLive()) {
            for (Mob pet : Minions.all(PET)) {
                if (pet.isAlive() && !pet.level().isClientSide()) keep(pet, time);
            }
        }
        if (time % 20 == 0) sendLists(e);
    }

    private static void keep(Mob pet, long time) {
        UUID ownerId = Minions.ownerOf(pet);
        if (ownerId == null) return;
        Player owner = pet.level().getPlayerByUUID(ownerId);
        LivingEntity order = ORDERS.get(pet);
        if (order != null && (!order.isAlive() || order.level() != pet.level() || pet.distanceToSqr(order) > ORDER_RANGE * ORDER_RANGE)) {
            ORDERS.remove(pet);
            order = null;
            pet.setTarget(null);
            pet.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        if (!Minions.FOLLOW.equals(Minions.stance(pet))) order = holdPost(pet, order, time);
        final LivingEntity ordered = order;
        // friends are never a target
        if (friendly(pet, pet.getTarget())) pet.setTarget(null);
        Brain<?> brain = pet.getBrain();
        var attack = mem(brain, MemoryModuleType.ATTACK_TARGET);
        if (attack.isPresent() && (friendly(pet, attack.get()) || attack.get() != ordered)) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
        mem(brain, MemoryModuleType.ROAR_TARGET).filter(t -> t != ordered).ifPresent(t -> brain.eraseMemory(MemoryModuleType.ROAR_TARGET));
        mem(brain, MemoryModuleType.ANGRY_AT).filter(id -> ordered == null || !id.equals(ordered.getUUID())).ifPresent(id -> brain.eraseMemory(MemoryModuleType.ANGRY_AT));
        if (pet instanceof NeutralMob nm && ordered == null && nm.getPersistentAngerTarget() != null) nm.stopBeingAngry();
        if (pet instanceof Warden w) {
            // left alone a warden digs into the ground and is gone for good: keep its dig cooldown topped up
            if (time % 20 == 0 || mem(brain, MemoryModuleType.DIG_COOLDOWN).isEmpty() || w.getPose() == net.minecraft.world.entity.Pose.DIGGING) adopt(w);
            // the warden only keeps its attack target while it is angry at it, so anger is for the ordered target only
            for (int i = 0; i < 8; i++) {
                var angry = w.getAngerManagement().getActiveEntity();
                if (angry.isEmpty() || angry.get() == ordered) break;
                w.clearAnger(angry.get());
            }
            if (ordered != null) {
                if (time % 10 == 0) w.increaseAngerAt(ordered, 30, false);
                if (mem(brain, MemoryModuleType.ATTACK_TARGET).isEmpty()) w.setAttackTarget(ordered);
            }
        } else if (ordered != null && usesBrain(pet) && mem(brain, MemoryModuleType.ATTACK_TARGET).isEmpty()) {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, ordered);
        } else if (ordered != null && pet.getTarget() == null) {
            pet.setTarget(ordered);
        }
        if (owner == null || ordered != null || pet.getTarget() != null || time % 2 != 0) return;
        if (!Minions.FOLLOW.equals(Minions.stance(pet))) return;
        follow(pet, owner);
    }

    // Stay and Guard: the pet keeps to the spot it was told at. Guarding also goes for hostile mobs that come near the spot,
    // then walks back. Returns the order it is carrying out (a new one if it picked a hostile).
    private static LivingEntity holdPost(Mob pet, LivingEntity order, long time) {
        net.minecraft.core.BlockPos spot = Minions.spot(pet);
        boolean guard = Minions.GUARD.equals(Minions.stance(pet));
        if (order != null && guard && order.distanceToSqr(spot.getX(), spot.getY(), spot.getZ()) > GUARD_LEASH * GUARD_LEASH) {
            ORDERS.remove(pet);
            pet.setTarget(null);
            pet.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            order = null;
        }
        if (order == null && guard && time % 10 == 0) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(spot).inflate(GUARD_RADIUS);
            LivingEntity near = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : pet.level().getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive() && !friendly(pet, e) && !Minions.isMinion(e))) {
                double d = e.distanceToSqr(pet);
                if (d < best) {
                    best = d;
                    near = e;
                }
            }
            if (near != null) {
                ORDERS.put(pet, near);
                aim(pet, near);
                return near;
            }
        }
        if (order == null && time % 20 == 0 && pet.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) > 9) {
            if (usesBrain(pet)) pet.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, 1.0f, 1));
            else pet.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.0);
        }
        return order;
    }

    // Brain mobs and flyers don't use the follow goal, so they are walked over here.
    private static void follow(Mob pet, Player owner) {
        if (pet.level() != owner.level()) return;
        double d = pet.distanceToSqr(owner);
        if (d > TELEPORT_FROM * TELEPORT_FROM) {
            pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return;
        }
        if (pet instanceof Phantom || pet instanceof Slime || pet instanceof Shulker) {
            if (d > HOVER_TELEPORT * HOVER_TELEPORT) pet.teleportTo(owner.getX() + 1, owner.getY(), owner.getZ() + 1);
            return;
        }
        if (d <= FOLLOW_FROM * FOLLOW_FROM) return;
        if (usesBrain(pet)) pet.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(owner, 1.1f, 3));
        else if (pet instanceof Ghast) pet.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + 2, owner.getZ(), 1.0);
    }

    // The other half: nothing a pet does hurts its owner or the owner's other pets, whatever the weapon.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent e) {
        if (Minions.noneLive() || !(e.getSource().getEntity() instanceof Mob by) || !Minions.isMinion(by)) return;
        if (friendly(by, e.getEntity())) {
            e.setCanceled(true);
            calmDown(by, e.getEntity());
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (Minions.noneLive() || !(e.getEntity() instanceof Mob mob) || !Minions.isMinion(mob)) return;
        if (friendly(mob, e.getNewTarget())) e.setCanceled(true);
    }

    private static void sendLists(TickEvent.ServerTickEvent e) {
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            List<Mob> pets = Minions.of(p.getUUID(), PET);
            if (pets.isEmpty()) {
                if (HAD_PETS.remove(p.getUUID())) PetNet.send(p, List.of());
                continue;
            }
            HAD_PETS.add(p.getUUID());
            List<PetNet.Entry> out = new ArrayList<>();
            for (Mob m : pets) {
                boolean fights = ORDERS.containsKey(m) || m.getTarget() != null;
                String stance = Minions.stance(m);
                byte state = (byte) (fights ? 1 : Minions.STAY.equals(stance) ? 2 : Minions.GUARD.equals(stance) ? 3 : 0);
                var key = ForgeRegistries.ENTITY_TYPES.getKey(m.getType());
                out.add(new PetNet.Entry(m.getId(), m.getName().getString(), key == null ? "minecraft:pig" : key.toString(),
                        m.getHealth(), m.getMaxHealth(), state));
            }
            PetNet.send(p, out);
        }
    }
}
