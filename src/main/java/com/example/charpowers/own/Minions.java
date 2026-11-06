package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Mobs that belong to a player: Necromancy's summons, Mob Control's pets. Their urge to hunt is taken out (again each time
// they load, as the game gives it back), so they leave everyone alone until told otherwise, and they follow their owner.
// Nothing here runs per tick. A claimed mob just has one small extra AI goal.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Minions {
    private static final String TAG = "charpowers.minion", OWNER = "charpowers.minion.owner", KIND = "charpowers.minion.kind";
    private static final int FOLLOW_FROM = 6, TELEPORT_FROM = 48;

    private static final Map<UUID, List<Mob>> LIVE = new HashMap<>();

    private Minions() {}

    public static boolean isMinion(Entity e) {
        return e instanceof Mob && e.getTags().contains(TAG);
    }

    public static boolean isKind(Entity e, String kind) {
        return isMinion(e) && kind.equals(e.getPersistentData().getString(KIND));
    }

    public static UUID ownerOf(Mob m) {
        CompoundTag d = m.getPersistentData();
        return d.hasUUID(OWNER) ? d.getUUID(OWNER) : null;
    }

    // Before the mob joins the world, or for one already in it (then call settle too).
    public static void claim(Mob m, ServerPlayer owner, String kind) {
        m.addTag(TAG);
        CompoundTag d = m.getPersistentData();
        d.putUUID(OWNER, owner.getUUID());
        d.putString(KIND, kind);
        m.setPersistenceRequired();
        m.setCanPickUpLoot(false);
    }

    // For a mob already in the world when it was claimed.
    public static void settle(Mob m) {
        setUp(m);
    }

    // Lets it go. It won't hunt again until it next loads from the world.
    public static void release(Mob m) {
        UUID owner = ownerOf(m);
        m.removeTag(TAG);
        m.getPersistentData().remove(OWNER);
        m.getPersistentData().remove(KIND);
        m.getPersistentData().remove("charpowers.minion.stance");
        m.getPersistentData().remove("charpowers.minion.spot");
        m.goalSelector.removeAllGoals(g -> g instanceof FollowGoal);
        m.setTarget(null);
        List<Mob> list = owner == null ? null : LIVE.get(owner);
        if (list != null) {
            list.remove(m);
            if (list.isEmpty()) LIVE.remove(owner);
        }
    }

    public static List<Mob> of(UUID owner, String kind) {
        List<Mob> out = new ArrayList<>();
        for (Mob m : LIVE.getOrDefault(owner, List.of())) {
            if (kind.equals(m.getPersistentData().getString(KIND))) out.add(m);
        }
        return out;
    }

    public static List<Mob> all(String kind) {
        List<Mob> out = new ArrayList<>();
        for (List<Mob> list : LIVE.values()) {
            for (Mob m : list) {
                if (kind.equals(m.getPersistentData().getString(KIND))) out.add(m);
            }
        }
        return out;
    }

    // What a minion has been told to do with its time: follow (the default), stay where it is, or guard that spot.
    public static final String FOLLOW = "follow", STAY = "stay", GUARD = "guard";

    public static String stance(Mob m) {
        String s = m.getPersistentData().getString("charpowers.minion.stance");
        return s.isEmpty() ? FOLLOW : s;
    }

    // Stay and guard remember the spot they were told at.
    public static void setStance(Mob m, String stance) {
        CompoundTag d = m.getPersistentData();
        d.putString("charpowers.minion.stance", stance);
        if (FOLLOW.equals(stance)) {
            d.remove("charpowers.minion.spot");
        } else {
            d.putLong("charpowers.minion.spot", m.blockPosition().asLong());
            m.getNavigation().stop();
        }
        if (!FOLLOW.equals(stance)) m.setTarget(null);
    }

    public static net.minecraft.core.BlockPos spot(Mob m) {
        CompoundTag d = m.getPersistentData();
        return d.contains("charpowers.minion.spot") ? net.minecraft.core.BlockPos.of(d.getLong("charpowers.minion.spot")) : m.blockPosition();
    }

    public static boolean noneLive() {
        return LIVE.isEmpty();
    }

    public static void pointAt(UUID owner, String kind, LivingEntity target) {
        for (Mob m : of(owner, kind)) {
            if (m != target) m.setTarget(target);
        }
    }

    private static void setUp(Mob m) {
        UUID owner = ownerOf(m);
        if (owner == null) return;
        m.targetSelector.removeAllGoals(g -> true);
        if (m instanceof PathfinderMob pm) {
            m.goalSelector.addGoal(5, new FollowGoal(m, owner));
            boolean fights = m.goalSelector.getAvailableGoals().stream().anyMatch(w -> {
                Goal g = w.getGoal();
                return g instanceof MeleeAttackGoal || g instanceof RangedAttackGoal || g instanceof RangedBowAttackGoal;
            });
            if (!fights && m.getAttribute(Attributes.ATTACK_DAMAGE) != null) m.goalSelector.addGoal(3, new MeleeAttackGoal(pm, 1.2, true));
        }
        List<Mob> list = LIVE.computeIfAbsent(owner, u -> new ArrayList<>());
        if (!list.contains(m)) list.add(m);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && isMinion(e.getEntity())) setUp((Mob) e.getEntity());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() || !isMinion(e.getEntity())) return;
        Mob m = (Mob) e.getEntity();
        UUID owner = ownerOf(m);
        List<Mob> list = owner == null ? null : LIVE.get(owner);
        if (list == null) return;
        list.remove(m);
        if (list.isEmpty()) LIVE.remove(owner);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        LIVE.clear();
    }

    private static final class FollowGoal extends Goal {
        private final Mob mob;
        private final UUID owner;

        FollowGoal(Mob mob, UUID owner) {
            this.mob = mob;
            this.owner = owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getTarget() != null || !FOLLOW.equals(stance(mob))) return false;
            var player = mob.level().getPlayerByUUID(owner);
            return player != null && mob.distanceToSqr(player) > FOLLOW_FROM * FOLLOW_FROM;
        }

        @Override
        public void start() {
            var player = mob.level().getPlayerByUUID(owner);
            if (player == null) return;
            if (mob.distanceToSqr(player) > TELEPORT_FROM * TELEPORT_FROM) mob.teleportTo(player.getX(), player.getY(), player.getZ());
            else mob.getNavigation().moveTo(player, 1.1);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.getTarget() == null && FOLLOW.equals(stance(mob)) && !mob.getNavigation().isDone();
        }
    }
}
