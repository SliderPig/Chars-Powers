package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;

import java.util.List;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;
import static com.example.charpowers.hidden.model.Rank.B;
import static com.example.charpowers.hidden.ranks.RankHelp.*;

// The B-rank hidden powers (really good), numbers 266-280 in my list.
final class BPowers {
    // the mob types Disguise can pick from (one is chosen for you, for good)
    static final String[] DISGUISES = {"minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:spider",
            "minecraft:enderman", "minecraft:witch", "minecraft:slime", "minecraft:drowned", "minecraft:husk", "minecraft:stray",
            "minecraft:pillager", "minecraft:cave_spider", "minecraft:phantom", "minecraft:blaze", "minecraft:zombified_piglin"};

    static void registerAll() {
        // #266 Ender Pocket: open your ender chest anywhere
        key(B, 266, "ender_pocket", CARRY, 20, () -> Items.ENDER_CHEST, u -> {
            ServerPlayer p = u.player();
            if (p == null) return;
            var chest = p.getEnderChestInventory();
            chest.setActiveChest(null); // not a real chest block this time
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> ChestMenu.threeRows(id, inv, chest), Component.translatable("container.enderchest")));
            Fx.sound(p, SoundEvents.ENDER_CHEST_OPEN, 0.6f, 1f);
        });
        // #267 Recall: teleport to your bed, once a Minecraft day
        key(B, 267, "recall", CARRY, 20, () -> Items.RED_BED, u -> {
            ServerPlayer p = u.player();
            if (p == null) return;
            if (!dailyReady(u, "recall")) { tell(u, "Recall works once a Minecraft day."); return; }
            BlockPos bed = p.getRespawnPosition();
            ServerLevel there = bed == null ? null : p.server.getLevel(p.getRespawnDimension());
            var spot = there == null ? java.util.Optional.<Vec3>empty()
                    : Player.findRespawnPositionAndUseSpawnBlock(there, bed, p.getRespawnAngle(), p.isRespawnForced(), true);
            if (spot.isEmpty()) { tell(u, "You don't have a bed to go back to."); return; }
            useDaily(u, "recall");
            Fx.particles(u.level(), ParticleTypes.PORTAL, p.position().add(0, 1, 0), 30, 0.4);
            Vec3 v = spot.get();
            p.teleportTo(there, v.x, v.y, v.z, p.getRespawnAngle(), 0);
            p.fallDistance = 0;
            Fx.sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
        });
        // #268 Mark & Return: drop a mark, then teleport back to it within 5 minutes; 10 minute cooldown
        key(B, 268, "mark_return", CARRY, 20, () -> Items.RECOVERY_COMPASS, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                ServerPlayer p = u.player();
                if (p == null) return;
                if (u.state().contains("markDim") && running(u, "markUntil")) {
                    ServerLevel there = p.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                            new ResourceLocation(u.state().getString("markDim"))));
                    if (there == null) { clear(u); return; }
                    Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, p.position().add(0, 1, 0), 30, 0.4);
                    p.teleportTo(there, u.state().getDouble("mx"), u.state().getDouble("my"), u.state().getDouble("mz"), p.getYRot(), p.getXRot());
                    p.fallDistance = 0;
                    Fx.sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.2f);
                    clear(u);
                    startFor(u, "markCd", 12000);
                    return;
                }
                if (running(u, "markCd")) { tell(u, "Mark & Return is resting (10 minutes after each return)."); return; }
                u.state().putString("markDim", p.level().dimension().location().toString());
                u.state().putDouble("mx", p.getX());
                u.state().putDouble("my", p.getY());
                u.state().putDouble("mz", p.getZ());
                startFor(u, "markUntil", 6000);
                Fx.sound(p, SoundEvents.LODESTONE_COMPASS_LOCK, 1f, 1f);
                tell(u, "Mark set. Press again within 5 minutes to come back.");
            }
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0 || u.player() == null) return;
                if (!u.state().contains("markDim")) { hud(u, "mark", ""); return; }
                if (!running(u, "markUntil")) { clear(u); tell(u, "Your mark faded."); return; }
                long left = (u.state().getLong("markUntil") - u.now()) / 20;
                hud(u, "mark", "Mark: " + left / 60 + ":" + String.format("%02d", left % 60));
                if (u.state().getString("markDim").equals(u.level().dimension().location().toString())) {
                    u.level().sendParticles(u.player(), ParticleTypes.END_ROD, true, u.state().getDouble("mx"), u.state().getDouble("my") + 0.5,
                            u.state().getDouble("mz"), 3, 0.2, 0.4, 0.2, 0);
                }
            }
            @Override public void stop(PowerUser u) { hud(u, "mark", ""); }
            private void clear(PowerUser u) {
                u.state().remove("markDim");
                u.state().putLong("markUntil", 0);
                hud(u, "mark", "");
            }
        });
        // #269 Stone Skin: turn to stone for 5 s: you can't move, you take no damage, and mobs lose interest. 1 minute cooldown.
        key(B, 269, "stone_skin", ESCAPE, 1200, () -> Items.STONE, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                startFor(u, "stone", 100);
                boost(e, Attributes.MOVEMENT_SPEED, "stone_skin", -1.0);
                e.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 250, false, false, false)); // a huge jump boost "wraps round" to no jumping
                for (Mob m : e.level().getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(24), m -> m.getTarget() == e)) m.setTarget(null);
                Fx.sound(e, SoundEvents.STONE_PLACE, 1.2f, 0.6f);
                Fx.particles(u.level(), new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                        e.position().add(0, 1, 0), 30, 0.4);
            }
            @Override public void tick(PowerUser u) {
                if (running(u, "stone")) {
                    if (u.mob() != null) u.mob().getNavigation().stop();
                    Vec3 v = u.entity().getDeltaMovement();
                    if (v.horizontalDistanceSqr() > 1.0E-4) launch(u.entity(), new Vec3(0, Math.min(0, v.y), 0));
                } else if (u.state().getLong("stone") != 0) {
                    end(u);
                }
            }
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) { if (running(u, "stone") && !e.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) e.setCanceled(true); }
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) { if (running(u, "stone")) e.setCanceled(true); }
            @Override public void onKnockback(PowerUser u, LivingKnockBackEvent e) { if (running(u, "stone")) e.setCanceled(true); }
            @Override public void stop(PowerUser u) { end(u); }
            private void end(PowerUser u) {
                u.state().putLong("stone", 0);
                boost(u.entity(), Attributes.MOVEMENT_SPEED, "stone_skin", 0);
                MobEffectInstance j = u.entity().getEffect(MobEffects.JUMP);
                if (j != null && j.getAmplifier() == 250) u.entity().removeEffect(MobEffects.JUMP);
            }
        });
        // #270 Disguise: one random mob type (e.g. zombies) never attacks you
        always(B, 270, "disguise", PASSIVE, () -> Items.ZOMBIE_HEAD, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (!u.state().getString("type").isEmpty()) return;
                String t = DISGUISES[u.entity().getRandom().nextInt(DISGUISES.length)];
                u.state().putString("type", t);
                if (u.player() != null) {
                    var type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(t));
                    u.player().sendSystemMessage(Component.literal("Disguise: every " + type.getDescription().getString()
                            + " thinks you're one of them.").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                }
            }
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) {
                if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getEntity().getType()).toString().equals(u.state().getString("type"))) e.setCanceled(true);
            }
        });
        // #271 Phase: walk through a 1-block-thick wall, 1 minute cooldown
        key(B, 271, "phase", ESCAPE, 1200, () -> Items.PHANTOM_MEMBRANE, u -> {
            LivingEntity e = u.entity();
            Vec3 dir = DPowers.awayOrForward(u).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1.0E-4) { refund(u); return; }
            dir = dir.normalize();
            if (clearPath(e, e.position().add(0, 0.5, 0), e.position().add(0, 0.5, 0).add(dir.scale(1.2)))) {
                tell(u, "Walk up to a wall first.");
                refund(u);
                return;
            }
            for (double d = 1.5; d <= 2.6; d += 0.5) {
                Vec3 at = e.position().add(dir.scale(d));
                if (fits(e, at)) {
                    Fx.particles(u.level(), ParticleTypes.PORTAL, e.position().add(0, 1, 0), 20, 0.3);
                    teleport(e, at);
                    Fx.sound(e, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 0.6f);
                    return;
                }
            }
            tell(u, "That wall is too thick.");
            refund(u);
        });
        // #272 Ghost Wolf: under 3 hearts, a see-through ghost wolf fights for you for 30 s, once every 10 minutes
        always(B, 272, "ghost_wolf", PASSIVE, () -> Items.BONE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                LivingEntity e = u.entity();
                Entity w = find(u, "wolf");
                if (w != null) {
                    if (!running(u, "wolfUntil")) { forget(u, "wolf"); return; }
                    if (u.now() % 10 == 0 && w instanceof Wolf wolf) {
                        if (wolf.getTarget() == null || !wolf.getTarget().isAlive()) {
                            List<LivingEntity> f = hostiles(u, 12);
                            if (!f.isEmpty()) wolf.setTarget(f.get(0));
                            else if (wolf.distanceTo(e) > 6) wolf.getNavigation().moveTo(e, 1.3);
                        }
                    }
                    return;
                }
                if (e.getHealth() >= 6.0f || !e.isAlive() || running(u, "wolfCd")) return;
                Wolf wolf = EntityType.WOLF.create(u.level());
                if (wolf == null) return;
                wolf.moveTo(e.getX() + 1, e.getY(), e.getZ(), e.getYRot(), 0);
                ghostly(wolf);
                makeMine(u, wolf);
                if (u.player() != null) wolf.tame(u.player());
                LivingEntity foe = e.getLastHurtByMob();
                if (foe != null && isFoe(u, foe)) wolf.setTarget(foe);
                u.level().addFreshEntity(wolf);
                u.state().putUUID("wolf", wolf.getUUID());
                startFor(u, "wolfUntil", 600);
                startFor(u, "wolfCd", 12000);
                Fx.sound(wolf, SoundEvents.WOLF_HOWL, 1f, 0.8f);
            }
            @Override public void stop(PowerUser u) { forget(u, "wolf"); }
        });
        // #273 Hunter's Mark: mark a mob; it takes 20% more damage from everyone for 30 s
        key(B, 273, "hunters_mark", ATTACK, 600, () -> Items.TARGET, u -> {
            LivingEntity t = target(u, 24);
            if (t == null) { refund(u); return; }
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false));
            RankEvents.mark(t, u.now() + 600);
            Fx.sound(u.entity(), SoundEvents.CROSSBOW_LOADING_END, 1f, 1.5f);
        });
        // #274 Big Leap: leap 15 blocks where you're looking, 30 s cooldown
        key(B, 274, "big_leap", ESCAPE, 600, () -> Items.RABBIT_FOOT, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Vec3 dir = DPowers.awayOrForward(u).multiply(1, 0, 1);
                if (dir.lengthSqr() < 1.0E-4) { refund(u); return; }
                dir = dir.normalize().scale(1.4);
                launch(u.entity(), new Vec3(dir.x, 0.65, dir.z));
                startFor(u, "soft", 120);
                Fx.particles(u.level(), ParticleTypes.CLOUD, u.entity().position(), 15, 0.5);
                Fx.sound(u.entity(), SoundEvents.GOAT_LONG_JUMP, 1f, 1f);
            }
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) { if (running(u, "soft")) e.setDamageMultiplier(0); }
        });
        // #275 Life Link: link to a mob for 20 s: half the damage you take goes to it instead. 2 minute cooldown.
        key(B, 275, "life_link", ATTACK, 2400, () -> Items.LEAD, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity t = target(u, 16);
                if (t == null) { refund(u); return; }
                u.state().putUUID("link", t.getUUID());
                startFor(u, "linkUntil", 400);
                Fx.sound(u.entity(), SoundEvents.LEASH_KNOT_PLACE, 1f, 0.8f);
            }
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                Entity l = find(u, "link");
                if (!(l instanceof LivingEntity linked) || !running(u, "linkUntil") || l.level() != u.level()) return;
                float half = e.getAmount() / 2;
                e.setAmount(e.getAmount() - half);
                linked.hurt(u.entity().damageSources().magic(), half);
            }
            @Override public void tick(PowerUser u) {
                Entity l = find(u, "link");
                if (l == null || !running(u, "linkUntil") || u.now() % 5 != 0 || l.level() != u.level()) return;
                Vec3 a = u.entity().position().add(0, 1, 0), b = l.position().add(0, l.getBbHeight() / 2, 0);
                for (int i = 1; i < 8; i++) Fx.particles(u.level(), ParticleTypes.ENCHANT, a.lerp(b, i / 8.0), 1, 0.02);
            }
        });
        // #276 Shadow Swap: swap places with any mob or player you can see within 30 blocks, 1 minute cooldown
        key(B, 276, "shadow_swap", ESCAPE, 1200, () -> Items.ENDER_EYE, u -> {
            LivingEntity t = Fx.lookTarget(u.entity(), 30);
            if (t == null || t instanceof net.minecraft.world.entity.decoration.ArmorStand) { refund(u); return; }
            if (t instanceof Player p && (p.isSpectator() || p.isCreative())) { refund(u); return; }
            swap(u.entity(), t);
        });
        // #277 Swarm Call: 6 angry bees appear and attack your target, 2 minute cooldown
        key(B, 277, "swarm_call", ATTACK, 2400, () -> Items.BEE_NEST, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity t = target(u, 16);
                if (t == null) { refund(u); return; }
                for (int i = 0; i < 6; i++) {
                    Bee b = EntityType.BEE.create(u.level());
                    if (b == null) continue;
                    var r = u.entity().getRandom();
                    b.moveTo(u.entity().getX() + r.nextFloat() * 2 - 1, u.entity().getEyeY(), u.entity().getZ() + r.nextFloat() * 2 - 1, 0, 0);
                    makeMine(u, b);
                    u.level().addFreshEntity(b);
                    b.setRemainingPersistentAngerTime(600);
                    b.setPersistentAngerTarget(t.getUUID());
                    b.setTarget(t);
                    remember(u, "bees", b, u.now() + 600);
                }
                Fx.sound(u.entity(), SoundEvents.BEEHIVE_EXIT, 1f, 1f);
            }
            @Override public void tick(PowerUser u) { forEachRemembered(u, "bees", null, Entity::discard); }
            @Override public void stop(PowerUser u) {
                forEachRemembered(u, "bees", Entity::discard, e -> {});
                u.state().remove("bees");
            }
        });
        // #278 Decoy: leave a fake you that mobs attack for 10 s while you turn invisible for 3 s, 2 minute cooldown
        key(B, 278, "decoy", ESCAPE, 2400, () -> Items.ARMOR_STAND, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                LivingEntity fake = copyOf(u, e.position(), "decoy", 200);
                if (fake == null) { refund(u); return; }
                e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
                for (Mob m : u.level().getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(16), m -> m.getTarget() == e)) m.setTarget(fake);
                Fx.particles(u.level(), ParticleTypes.POOF, e.position().add(0, 1, 0), 15, 0.4);
            }
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) {
                List<Entity> fakes = copies(u, "decoy");
                if (!fakes.isEmpty() && fakes.get(0) instanceof LivingEntity fake) e.setNewTarget(fake);
            }
            @Override public void tick(PowerUser u) { if (u.now() % 10 == 0) copiesTick(u, false); }
            @Override public void stop(PowerUser u) { copiesTick(u, true); }
        });
        // #279 Ricochet: your arrows bounce off a wall once and fly at the nearest mob (Arrows)
        always(B, 279, "ricochet", PASSIVE, () -> Items.ARROW, new PowerEffect() {
            @Override public void onShoot(PowerUser u, Projectile p) { if (p instanceof AbstractArrow a) Arrows.track(a, Arrows.RICOCHET); }
        });
        // #280 Undertow: pull every mob within 8 blocks toward you, 45 s cooldown
        key(B, 280, "undertow", ATTACK, 900, () -> Items.HEART_OF_THE_SEA, u -> {
            Vec3 me = u.entity().position();
            for (LivingEntity t : foes(u, 8)) {
                Vec3 d = me.subtract(t.position());
                if (d.lengthSqr() < 1) continue;
                Vec3 v = d.normalize().scale(Math.min(1.2, 0.3 + d.length() * 0.12));
                launch(t, new Vec3(v.x, 0.25, v.z));
            }
            Fx.sound(u.entity(), SoundEvents.CONDUIT_ACTIVATE, 1f, 1.2f);
            Fx.particles(u.level(), ParticleTypes.NAUTILUS, me.add(0, 1, 0), 40, 3);
        });
    }

    // A see-through look: invisible + an outline (ghost wolf, released souls).
    static void ghostly(Mob m) {
        m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false, false));
        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false, false));
        m.setPersistenceRequired();
    }

    // "Once a Minecraft day" checks for powers that should only use up their day when they actually work.
    static boolean dailyReady(PowerUser u, String key) {
        long day = u.level().getServer().overworld().getGameTime() / 24000L;
        return !(u.state().contains(key) && u.state().getLong(key) == day);
    }

    static void useDaily(PowerUser u, String key) {
        u.state().putLong(key, u.level().getServer().overworld().getGameTime() / 24000L);
    }

    private BPowers() {}
}
