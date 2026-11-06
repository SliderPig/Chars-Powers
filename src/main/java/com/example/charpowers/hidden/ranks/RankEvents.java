package com.example.charpowers.hidden.ranks;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.HiddenEvents;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.MobPowers;
import com.example.charpowers.hidden.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// Game events that only one or two hidden powers care about, each checked here by name
// (e.g. "does whoever trampled this farmland have Light Step?"). Most powers live in their own rank file instead.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class RankEvents {
    public static final String SMOKED = "charpowers.smoked"; // Chef's Kiss: this food came out of a smoker
    public static final String SLING = "charpowers.sling";   // a block flung with Sling

    // Pocket Sand: these mobs miss their next attack (until the time runs out). Hunter's Mark: these take 20% more.
    private static final Map<Entity, Long> MISS = new WeakHashMap<>();
    private static final Map<Entity, Long> MARKED = new WeakHashMap<>();

    static void missNext(Entity e, long until) { MISS.put(e, until); }

    static void mark(Entity e, long until) { MARKED.put(e, until); }

    // Players: works on the server and (for your own player) on your PC. Mobs: server only.
    static boolean has(Entity e, String id) {
        if (e instanceof Player p) return HiddenRuntime.has(p, id);
        return e instanceof LivingEntity le && HiddenEvents.has(le, id);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.level instanceof ServerLevel l) Arrows.tick(l);
    }

    // #114 Light Step
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent e) {
        if (has(e.getEntity(), "light_step")) e.setCanceled(true);
    }

    // #185 No Phantoms
    @SubscribeEvent
    public static void onPhantoms(PlayerSpawnPhantomsEvent e) {
        if (has(e.getEntity(), "no_phantoms")) e.setResult(Event.Result.DENY);
    }

    // #147 Silent Steps: sculk sensors never hear the footsteps
    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent e) {
        if (e.getVanillaEvent() == GameEvent.STEP && e.getCause() != null && has(e.getCause(), "silent_steps")) e.setCanceled(true);
    }

    // #195 Mole and #148 Quick Shears. Runs on your PC too, or the block would pop back (the server and your PC
    // must agree how fast you break it).
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        BlockState s = e.getState();
        if (e.getPosition().isPresent() && has(p, "mole")) {
            BlockPos at = e.getPosition().get();
            boolean below = at.getY() < p.getY() && Math.abs(at.getX() + 0.5 - p.getX()) < 1.2 && Math.abs(at.getZ() + 0.5 - p.getZ()) < 1.2;
            boolean soft = s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Tags.Blocks.GRAVEL);
            if (below && soft) e.setNewSpeed(e.getNewSpeed() * 2);
        }
        if (p.getMainHandItem().getItem() instanceof ShearsItem && has(p, "quick_shears")) e.setNewSpeed(e.getNewSpeed() * 2);
    }

    // #265 Quick Draw: every 3rd tick of drawing counts twice (so about 30% faster). Also on your PC.
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick e) {
        ItemStack s = e.getItem();
        if (!(s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem)) return;
        if (e.getEntity().tickCount % 3 == 0 && e.getDuration() > 1 && has(e.getEntity(), "quick_draw")) e.setDuration(e.getDuration() - 1);
    }

    // #263 Shield Bash
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent e) {
        LivingEntity me = e.getEntity();
        if (me.level().isClientSide || !has(me, "shield_bash")) return;
        if (e.getDamageSource().getDirectEntity() instanceof LivingEntity by && by != me) {
            RankHelp.launch(by, by.position().subtract(me.position()).multiply(1, 0, 1).normalize().scale(1.5).add(0, 0.35, 0));
            Fx.sound(by, SoundEvents.SHIELD_BLOCK, 1f, 0.6f);
        }
    }

    // #297 Main Character: more critical hits...
    @SubscribeEvent
    public static void onCrit(CriticalHitEvent e) {
        if (e.isVanillaCritical() || e.getEntity().level().isClientSide || !has(e.getEntity(), "main_character")) return;
        if (e.getEntity().getRandom().nextInt(5) == 0) {
            e.setResult(Event.Result.ALLOW);
            e.setDamageModifier(1.5f);
        }
    }

    // ...and rare drops about twice as likely (like Looting II)
    @SubscribeEvent
    public static void onLooting(LootingLevelEvent e) {
        if (e.getDamageSource() != null && e.getDamageSource().getEntity() instanceof LivingEntity by && has(by, "main_character")) {
            e.setLootingLevel(e.getLootingLevel() + 2);
        }
    }

    // #192 Treasure Fisher: 1 in 5 catches also bring up a treasure
    @SubscribeEvent
    public static void onFished(ItemFishedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !has(p, "treasure_fisher") || p.getRandom().nextInt(5) != 0) return;
        FishingHook hook = e.getHookEntity();
        ServerLevel l = p.serverLevel();
        LootParams params = new LootParams.Builder(l)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, p.getMainHandItem())
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withLuck(p.getLuck())
                .create(LootContextParamSets.FISHING);
        for (ItemStack s : l.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING_TREASURE).getRandomItems(params)) Fx.give(p, s);
        p.displayClientMessage(Component.literal("Treasure!"), true);
    }

    // #157 Chef's Kiss: mark food taken out of a smoker
    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.containerMenu instanceof SmokerMenu && e.getSmelting().isEdible() && has(p, "chefs_kiss")) {
            e.getSmelting().getOrCreateTag().putBoolean(SMOKED, true);
        }
    }

    // #188 Double XP
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.PickupXp e) {
        if (has(e.getEntity(), "double_xp") && e.getEntity().getRandom().nextInt(20) == 0) e.getOrb().value *= 2;
    }

    // Arrows: #214 Arrow Catcher, #151 Boomerang Arrows, #216 Sling, and the end of an arrow's tricks
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent e) {
        if (e.getProjectile().level().isClientSide) return;
        HitResult hit = e.getRayTraceResult();
        Entity owner = e.getProjectile().getOwner();
        if (hit instanceof EntityHitResult eh && e.getProjectile().getTags().contains(SLING) && eh.getEntity() instanceof LivingEntity t && t != owner) {
            t.hurt(t.damageSources().thrown(e.getProjectile(), owner), 2.0f);
        }
        if (!(e.getProjectile() instanceof AbstractArrow a)) return;
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity catcher && catcher != owner
                && has(catcher, "arrow_catcher") && catcher.getRandom().nextInt(10) == 0) {
            if (catcher instanceof ServerPlayer p && a.pickup == AbstractArrow.Pickup.ALLOWED) Fx.give(p, arrowItem(a));
            Fx.sound(catcher, SoundEvents.ITEM_PICKUP, 1f, 1.4f);
            e.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            a.discard();
            return;
        }
        if (a.getTags().contains(Arrows.BOOMERANG) && !a.getTags().contains(Arrows.RETURNED) && owner instanceof ServerPlayer p
                && a.pickup == AbstractArrow.Pickup.ALLOWED) {
            Fx.give(p, arrowItem(a));
            a.addTag(Arrows.RETURNED); // Arrows takes it away next tick
            p.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
        if (hit.getType() == HitResult.Type.BLOCK) Arrows.landed(a);
    }

    private static ItemStack arrowItem(AbstractArrow a) {
        return new ItemStack(a instanceof SpectralArrow ? Items.SPECTRAL_ARROW : Items.ARROW);
    }

    // #160 Pocket Sand: the next attack misses
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent e) {
        Entity by = e.getSource().getEntity();
        if (by == null || by.level().isClientSide || !MISS.containsKey(by)) return;
        long until = MISS.remove(by);
        if (by.level().getGameTime() < until) {
            e.setCanceled(true);
            Fx.sound(e.getEntity(), SoundEvents.PLAYER_ATTACK_NODAMAGE, 1f, 1f);
        }
    }

    // #273 Hunter's Mark: 20% more damage from everyone
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        Long until = MARKED.get(e.getEntity());
        if (until == null) return;
        if (e.getEntity().level().getGameTime() >= until) MARKED.remove(e.getEntity());
        else e.setAmount(e.getAmount() * 1.2f);
    }

    // Helpers a power summons (ghost wolf, bees, ghosts from the Soul Jar) never turn on whoever summoned them,
    // on each other, or (when a player summoned them) on any player.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity t = e.getNewTarget();
        LivingEntity me = e.getEntity();
        if (t == null || me.level().isClientSide || !me.getPersistentData().hasUUID(RankHelp.OWNER)) return;
        UUID owner = me.getPersistentData().getUUID(RankHelp.OWNER);
        boolean ownerIsPlayer = me.level().getPlayerByUUID(owner) != null;
        boolean friend = t.getUUID().equals(owner) || (t.getPersistentData().hasUUID(RankHelp.OWNER) && t.getPersistentData().getUUID(RankHelp.OWNER).equals(owner));
        if (friend || (ownerIsPlayer && t instanceof Player)) e.setCanceled(true);
    }

    // #132 Fish Whisperer: when a fish bites your hook, you hear it loud and clear
    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtPosition e) {
        if (e.getLevel().isClientSide || e.getSound() == null || e.getSound().value() != SoundEvents.FISHING_BOBBER_SPLASH) return;
        Vec3 at = e.getPosition();
        for (FishingHook hook : e.getLevel().getEntitiesOfClass(FishingHook.class, new AABB(at, at).inflate(1.0))) {
            if (hook.getPlayerOwner() instanceof ServerPlayer p && has(p, "fish_whisperer")) {
                p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.5f, 1.8f);
                p.displayClientMessage(Component.literal("Bite! Reel in!"), true);
            }
        }
    }

    // Z rank: only one mob in the world may have Everything. When it dies or is removed for good, the place is free.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!e.isCanceled() && e.getEntity() instanceof Mob m && !m.level().isClientSide && MobPowers.get(m).filter("everything"::equals).isPresent()) {
            ZSlot.free(m);
        }
    }

    // If the world was saved in the middle of a power (a paused mob, a floating mob, a slow creeper fuse),
    // the power can't finish it later, so put the mob back to normal when it's loaded again.
    @SubscribeEvent
    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !e.loadedFromDisk() || !(e.getEntity() instanceof Mob m)) return;
        var d = m.getPersistentData();
        if (d.contains("charpowers.wasNoAi")) APowers.unpause(m);
        if (d.contains(APowers.FLOATING)) APowers.land(m);
        if (d.contains("charpowers.fuse") && m instanceof net.minecraft.world.entity.monster.Creeper c) DPowers.setFuse(c, false);
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof Mob m)) return;
        var why = m.getRemovalReason();
        if (why != null && why.shouldDestroy() && MobPowers.get(m).filter("everything"::equals).isPresent()) ZSlot.free(m);
    }

    private RankEvents() {}
}
