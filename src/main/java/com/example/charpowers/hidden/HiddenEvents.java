package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.attack.AttackHelp;
import com.example.charpowers.hidden.attack.Later;
import com.example.charpowers.hidden.attack.Wreck;
import com.example.charpowers.hidden.fx.Boost;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.model.HitMath;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Sends game events (getting hurt, killing, making a sound...) to whatever hidden powers are involved,
// for mobs and players alike.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class HiddenEvents {

    // Every power the entity has working right now, each with its saved scratch space.
    public static List<Active> active(LivingEntity e) {
        List<Active> out = new ArrayList<>();
        if (e instanceof Mob m && MobPowers.holds(m)) {
            MobPowers.get(m).ifPresent(id -> out.add(new Active(id, new PowerUser(m, null, MobPowers.state(m, id)))));
        } else if (e instanceof ServerPlayer p) {
            out.addAll(HiddenRuntime.active(p));
        }
        return withLent(out);
    }

    // Adds the powers that Copycat / Everything are lending right now, as if they were equipped too.
    static List<Active> withLent(List<Active> own) {
        if (own.isEmpty()) return own;
        List<Active> out = new ArrayList<>(own);
        for (Active a : own) {
            try {
                for (String id : a.effect().lends(a.user())) {
                    if (HiddenPowers.def(id).isPresent()) out.add(new Active(id, a.user().lent(id)));
                }
            } catch (RuntimeException ex) {
                CharPowers.LOG.error("Hidden power {} failed to lend", a.id(), ex);
            }
        }
        return out;
    }

    // True if this entity has this power working right now (equipped, a mob's own, or lent). Server only.
    public static boolean has(LivingEntity e, String id) {
        if (e == null || e.level().isClientSide) return false;
        for (Active a : active(e)) if (a.id().equals(id)) return true;
        return false;
    }

    public record Active(String id, PowerUser user) {
        public PowerEffect effect() { return HiddenPowers.effect(id); }
    }

    private static void each(LivingEntity e, Consumer<Active> run) {
        if (e == null || e.level().isClientSide) return;
        for (Active a : active(e)) {
            try {
                run.accept(a);
            } catch (RuntimeException ex) {
                CharPowers.LOG.error("Hidden power {} failed on {}", a.id(), e, ex);
            }
        }
    }

    // A mob lost its power (/hiddenpower set): let the power tidy up anything it spawned.
    static void stopFor(Mob m, String id) {
        PowerUser u = new PowerUser(m, null, MobPowers.state(m, id));
        PowerEffect fx = HiddenPowers.effect(id);
        for (String lent : fx.lends(u)) HiddenPowers.effect(lent).stop(u.lent(lent));
        fx.stop(u);
    }

    // Things powers spawn for a short while (a confused chicken, Ghost Kitty, a floating note) carry this tag.
    // They are never meant to be saved: if one is loaded from disk later, it's thrown away.
    public static final String TEMP_TAG = "charpowers.temp";

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide) return;
        if (e.loadedFromDisk() && e.getEntity().getTags().contains(TEMP_TAG)) {
            e.setCanceled(true);
            return;
        }
        if (e.getEntity() instanceof Mob m && !m.getTags().contains(TEMP_TAG)) MobPowers.onJoin(m);
        // something new was shot or thrown (not an old arrow loaded from disk)
        if (!e.loadedFromDisk() && e.getEntity() instanceof Projectile pr && pr.getOwner() instanceof LivingEntity owner) {
            each(owner, a -> a.effect().onShoot(a.user(), pr));
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof Mob gone) Later.cancel(gone);
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof Mob m) || !MobPowers.holds(m)) return;
        MobPowers.get(m).ifPresent(id -> stopFor(m, id));
        MobPowers.onLeave(m);
    }

    // Quitting a world: nothing half-done may carry over into the next world loaded (singleplayer).
    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent e) {
        Later.clearAll();
        Wreck.clearAll();
        com.example.charpowers.hidden.attack.AttackPowers.clearAll();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.level instanceof ServerLevel l) {
            MobPowerTicker.tick(l);
            Later.tick(l);
            Wreck.tick(l);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        // Boosts first (flat extras, then multipliers), on normal hits and on every hit a key power lands.
        if (e.getSource().getEntity() instanceof LivingEntity by && by != e.getEntity()
                && (AttackHelp.POWER_HIT || AttackHelp.isPunch(by, e.getSource()))) {
            long now = by.level().getGameTime();
            Boost b = new Boost(AttackHelp.POWER_HIT ? AttackHelp.pressOf(by, now) : now, AttackHelp.POWER_HIT);
            each(by, a -> a.effect().boostHit(a.user(), e.getEntity(), b));
            if (b.flatHearts != 0 || b.multiplier != 1f) e.setAmount(HitMath.boosted(e.getAmount(), b.flatHearts, b.multiplier));
        }
        if (e.getSource().getEntity() instanceof LivingEntity by && by != e.getEntity()) {
            each(by, a -> a.effect().onHit(a.user(), e.getEntity(), e));
        }
        each(e.getEntity(), a -> a.effect().onHurt(a.user(), e));
        if (e.getSource().getEntity() instanceof LivingEntity by) each(by, a -> a.effect().onAttack(a.user(), e.getEntity()));
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        each(e.getEntity(), a -> { if (!e.isCanceled()) a.effect().onAttacked(a.user(), e); });
    }

    // Runs first: a power like Nine Lives can stop the death before anything else hears about it.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeathFirst(LivingDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (le.level().isClientSide || e.isCanceled()) return;
        for (Active a : active(le)) {
            try {
                if (a.effect().preventsDeath(a.user(), e)) {
                    e.setCanceled(true);
                    return;
                }
            } catch (RuntimeException ex) {
                CharPowers.LOG.error("Hidden power {} failed on death", a.id(), ex);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent e) {
        if (e.isCanceled()) return;
        Later.cancel(e.getEntity());
        each(e.getEntity(), a -> a.effect().onDeath(a.user()));
        if (e.getSource().getEntity() instanceof LivingEntity by) each(by, a -> a.effect().onKill(a.user(), e.getEntity()));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (e.getSource().getEntity() instanceof LivingEntity by && by != e.getEntity()) each(by, a -> a.effect().onDrops(a.user(), e));
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent e) {
        each(e.getEntity(), a -> a.effect().onJump(a.user()));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        each(e.getEntity(), a -> a.effect().onFallEvent(a.user(), e));
        each(e.getEntity(), a -> a.effect().onFall(a.user(), e.getDistance()));
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent e) {
        each(e.getEntity(), a -> a.effect().onKnockback(a.user(), e));
    }

    @SubscribeEvent
    public static void onAte(LivingEntityUseItemEvent.Finish e) {
        ItemStack food = e.getItem();
        if (food.isEdible() || food.getItem() instanceof MilkBucketItem || food.getItem() instanceof PotionItem) {
            each(e.getEntity(), a -> a.effect().onAte(a.user(), food));
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        each(e.getEntity(), a -> a.effect().onHeal(a.user(), e));
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        each(e.getEntity(), a -> a.effect().onWake(a.user()));
    }

    @SubscribeEvent
    public static void onTargeted(LivingChangeTargetEvent e) {
        LivingEntity t = e.getNewTarget();
        if (t != null && t != e.getEntity()) each(t, a -> { if (!e.isCanceled()) a.effect().onTargeted(a.user(), e); });
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added e) {
        each(e.getEntity(), a -> a.effect().onEffectAdded(a.user(), e));
    }

    // Chat comes in on a side thread: only decide here whether to hide the message,
    // then do the rest on the server's main thread (changing the world from a side thread is unsafe).
    @SubscribeEvent
    public static void onChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        boolean swallow = false;
        for (Active a : active(p)) {
            try {
                if (a.effect().swallowsChat(a.user())) swallow = true;
            } catch (RuntimeException ex) {
                CharPowers.LOG.error("Hidden power {} failed on chat", a.id(), ex);
            }
        }
        if (swallow) e.setCanceled(true);
        String text = e.getRawText();
        boolean swallowed = e.isCanceled();
        p.server.execute(() -> each(p, a -> a.effect().onChat(a.user(), text, swallowed)));
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (Wreck.BREAKING) return; // an attack power asking about a crater block: no mining powers on those
        if (e.getPlayer() instanceof ServerPlayer p) each(p, a -> a.effect().onBreak(a.user(), e));
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        each(e.getEntity(), a -> a.effect().onRightClick(a.user(), e.getItemStack(), null));
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        each(e.getEntity(), a -> a.effect().onRightClick(a.user(), e.getItemStack(), e.getLevel().getBlockState(e.getPos())));
        each(e.getEntity(), a -> { if (!e.isCanceled()) a.effect().onUseBlock(a.user(), e); });
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract e) {
        each(e.getEntity(), a -> { if (!e.isCanceled()) a.effect().onUseEntity(a.user(), e); });
    }

    // Lets powers change the sounds their holder makes (Chipmunk, Big Voice, Mime, Noisy Eater).
    // Most entity sounds are played "at a position", so we look for a holder standing exactly there.
    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtEntity e) {
        if (e.getEntity() instanceof LivingEntity le) each(le, a -> a.effect().onSound(a.user(), e));
    }

    @SubscribeEvent
    public static void onSoundAt(PlayLevelSoundEvent.AtPosition e) {
        if (e.getLevel().isClientSide) return;
        Vec3 pos = e.getPosition();
        for (LivingEntity le : e.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(0.05))) {
            if (le.position().distanceToSqr(pos) < 0.0025) each(le, a -> a.effect().onSound(a.user(), e));
        }
    }

    private HiddenEvents() {}
}
