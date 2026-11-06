package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;
import static com.example.charpowers.hidden.model.Rank.D;
import static com.example.charpowers.hidden.ranks.RankHelp.*;

// The D-rank hidden powers (handy), numbers 176-230 in my list.
final class DPowers {
    static void registerAll() {
        // #176 Wall Jump: jump off a wall once before you land (on your PC, ClientMovement)
        onPc(D, 176, "wall_jump", () -> Items.STONE_BRICKS);
        // #177 Hop: one small double jump in the air
        key(D, 177, "hop", ATTACK, 20, () -> Items.RABBIT_FOOT, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                if (u.mob() != null) { if (e.onGround()) launch(e, e.getDeltaMovement().add(0, 0.6, 0)); return; }
                if (e.onGround() || u.state().getBoolean("hopped")) { refund(u); return; }
                u.state().putBoolean("hopped", true);
                Vec3 v = e.getDeltaMovement();
                launch(e, new Vec3(v.x, 0.45, v.z));
                Fx.particles(u.level(), ParticleTypes.CLOUD, e.position(), 6, 0.2);
            }
            @Override public void tick(PowerUser u) { if (u.entity().onGround()) u.state().putBoolean("hopped", false); }
        });
        // #178 Blink: teleport 3 blocks forward, 20 s cooldown (a mob blinks away from what it's fighting)
        key(D, 178, "blink", ESCAPE, 400, () -> Items.ENDER_PEARL, u -> {
            Vec3 dir = awayOrForward(u);
            Vec3 to = spotAlong(u.entity(), dir, 3);
            if (to == null) { refund(u); return; }
            Fx.particles(u.level(), ParticleTypes.PORTAL, u.entity().position().add(0, 1, 0), 15, 0.3);
            teleport(u.entity(), to);
            Fx.sound(u.entity(), SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
        });
        // #179 Swap: swap places with the nearest mob, 30 s cooldown
        key(D, 179, "swap", ESCAPE, 600, () -> Items.CHORUS_FRUIT, u -> {
            LivingEntity other = null;
            double best = Double.MAX_VALUE;
            for (Mob m : u.level().getEntitiesOfClass(Mob.class, u.entity().getBoundingBox().inflate(16))) {
                if (m == u.entity() || !m.isAlive() || isMine(u, m) || (m instanceof TamableAnimal t && t.isTame())) continue;
                double d = m.distanceToSqr(u.entity());
                if (d < best) { best = d; other = m; }
            }
            if (other == null) { refund(u); return; }
            swap(u.entity(), other);
        });
        // #180 Lucky Miner: 2% chance of double ore drops
        always(D, 180, "lucky_miner", CARRY, () -> Items.IRON_PICKAXE, extraOreDrops(50));
        // #181 Creeper Ears: a warning sound the moment a creeper near you starts to hiss
        always(D, 181, "creeper_ears", CARRY, () -> Items.CREEPER_HEAD, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 2 != 0) return;
                CompoundTag heard = u.state().getCompound("heard");
                for (Creeper c : u.level().getEntitiesOfClass(Creeper.class, u.entity().getBoundingBox().inflate(16))) {
                    String k = c.getUUID().toString();
                    if (c.getSwellDir() > 0 && !heard.contains(k)) {
                        heard.putBoolean(k, true);
                        u.player().playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.0f, 0.5f);
                        tell(u, "Creeper!");
                    } else if (c.getSwellDir() <= 0) {
                        heard.remove(k);
                    }
                }
                if (heard.size() > 32) heard = new CompoundTag();
                u.state().put("heard", heard);
            }
        });
        // #182 Echolocation: mobs within 8 blocks glow for 2 s
        key(D, 182, "echolocation", CARRY, 200, () -> Items.BAT_SPAWN_EGG, u -> {
            Fx.sound(u.entity(), SoundEvents.BAT_AMBIENT, 1.0f, 0.6f);
            for (Mob m : u.level().getEntitiesOfClass(Mob.class, u.entity().getBoundingBox().inflate(8), m -> m != u.entity())) {
                m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
            }
        });
        // #183 Glass Hands: breaking glass gives the glass back
        always(D, 183, "glass_hands", CARRY, () -> Items.GLASS, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                BlockState s = e.getState();
                boolean glass = s.is(Tags.Blocks.GLASS) || s.is(Tags.Blocks.GLASS_PANES);
                boolean silk = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, u.entity().getMainHandItem()) > 0;
                if (glass && !silk && !e.getPlayer().isCreative()) Block.popResource(u.level(), e.getPos(), new ItemStack(s.getBlock().asItem()));
            }
        });
        // #184 Water Walker: sprint onto water and walk on it for 3 s (on your PC)
        onPc(D, 184, "water_walker", () -> Items.LILY_PAD);
        // #185 No Phantoms: phantoms never spawn for you (RankEvents)
        always(D, 185, "no_phantoms", CARRY, () -> Items.PHANTOM_MEMBRANE, new PowerEffect() {});
        // #186 Slow Fuse: creepers near you take 1 s longer to blow up
        always(D, 186, "slow_fuse", PASSIVE, () -> Items.GUNPOWDER, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0) return;
                for (Creeper c : u.level().getEntitiesOfClass(Creeper.class, u.entity().getBoundingBox().inflate(12))) {
                    if (!c.getPersistentData().contains("charpowers.fuse")) setFuse(c, true);
                    remember(u, "fuse", c, u.now() + 40);
                }
                forEachRemembered(u, "fuse", null, c -> { if (c instanceof Creeper cr) setFuse(cr, false); });
            }
            @Override public void stop(PowerUser u) {
                forEachRemembered(u, "fuse", c -> { if (c instanceof Creeper cr) setFuse(cr, false); }, c -> {});
                u.state().remove("fuse");
            }
        });
        // #187 Health Peek: see a mob's health when you look at it (on your PC)
        onPc(D, 187, "health_peek", () -> Items.GLISTERING_MELON_SLICE);
        // #188 Double XP: 5% chance XP orbs are worth double (RankEvents)
        always(D, 188, "double_xp", CARRY, () -> Items.EXPERIENCE_BOTTLE, new PowerEffect() {});
        // #189 Thorny Skin: whoever hits you takes half a heart
        always(D, 189, "thorny_skin", PASSIVE, () -> Items.SWEET_BERRIES, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (e.getSource().is(DamageTypes.THORNS)) return; // never thorns-ping-pong
                if (e.getSource().getDirectEntity() instanceof LivingEntity by && by != u.entity()) {
                    by.hurt(u.entity().damageSources().thorns(u.entity()), 1.0f);
                }
            }
        });
        // #190 Danger Heartbeat: you hear a heartbeat when a hostile mob you can't see is within 8 blocks
        always(D, 190, "danger_heartbeat", CARRY, () -> Items.FERMENTED_SPIDER_EYE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 30 != 0) return;
                boolean hidden = u.level().getEntitiesOfClass(Mob.class, u.entity().getBoundingBox().inflate(8),
                        m -> m instanceof Enemy && m.distanceTo(u.entity()) <= 8 && !u.entity().hasLineOfSight(m)).size() > 0;
                if (hidden) u.player().playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0f, 1.0f);
            }
        });
        // #191 Animal Tamer: wolves, cats and horses tame on the first try
        always(D, 191, "animal_tamer", CARRY, () -> Items.BONE, new PowerEffect() {
            @Override public void onUseEntity(PowerUser u, PlayerInteractEvent.EntityInteract e) {
                ServerPlayer p = u.player();
                if (p == null || e.getHand() != InteractionHand.MAIN_HAND) return;
                ItemStack held = e.getItemStack();
                Entity t = e.getTarget();
                if (t instanceof AbstractHorse h && !h.isTamed() && held.isEmpty() && !h.isVehicle()) {
                    h.tameWithName(p); // then Minecraft lets you climb on as normal
                    return;
                }
                boolean wolf = t instanceof Wolf w && !w.isTame() && !w.isAngry() && held.is(Items.BONE);
                boolean cat = t instanceof Cat c && !c.isTame() && (held.is(Items.COD) || held.is(Items.SALMON));
                if (!wolf && !cat) return;
                TamableAnimal pet = (TamableAnimal) t;
                if (!p.getAbilities().instabuild) held.shrink(1);
                pet.tame(p);
                pet.getNavigation().stop();
                pet.setTarget(null);
                pet.setOrderedToSit(true);
                u.level().broadcastEntityEvent(pet, (byte) 7); // the hearts
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        // #192 Treasure Fisher: better chance of treasure when fishing (RankEvents)
        always(D, 192, "treasure_fisher", CARRY, () -> Items.NAUTILUS_SHELL, new PowerEffect() {});
        // #193 Gills: dive in and breathe underwater for 30 s, every 2 minutes
        always(D, 193, "gills", PASSIVE, () -> Items.TURTLE_HELMET, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0 || running(u, "gillsCd") || !u.entity().isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) return;
                effect(u.entity(), MobEffects.WATER_BREATHING, 600, 0);
                startFor(u, "gillsCd", 2400);
                Fx.particles(u.level(), ParticleTypes.BUBBLE, u.entity().getEyePosition(), 10, 0.3);
            }
        });
        // #194 Timber: chop the last log of a tree and all its leaves drop at once
        always(D, 194, "timber", CARRY, () -> Items.IRON_AXE, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                if (!e.getState().is(BlockTags.LOGS)) return;
                u.state().putLong("leavesAt", u.now() + 30); // leaves notice the tree is gone over a few ticks
                u.state().putLong("leavesPos", e.getPos().asLong());
            }
            @Override public void tick(PowerUser u) {
                long at = u.state().getLong("leavesAt");
                if (at == 0 || u.now() < at) return;
                u.state().putLong("leavesAt", 0);
                dropLooseLeaves(u.level(), BlockPos.of(u.state().getLong("leavesPos")), u.entity());
            }
        });
        // #195 Mole: dig straight down through dirt, sand and gravel twice as fast (RankEvents, also on your PC)
        always(D, 195, "mole", CARRY, () -> Items.IRON_SHOVEL, new PowerEffect() {});
        // #196 Snack on the Run: eat while sprinting without slowing down (on your PC)
        onPc(D, 196, "snack_on_the_run", () -> Items.COOKIE);
        // #197 Pocket Light: light up the spot you're looking at for 30 s
        key(D, 197, "pocket_light", CARRY, 600, () -> Items.GLOWSTONE, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                BlockHitResult hit = lookBlock(u, 16);
                BlockPos at = hit != null ? hit.getBlockPos().relative(hit.getDirection()) : BlockPos.containing(u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(3)));
                if (!u.level().getBlockState(at).isAir()) { refund(u); return; }
                u.level().setBlock(at, Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
                Fx.particles(u.level(), ParticleTypes.END_ROD, Vec3.atCenterOf(at), 8, 0.2);
                placed(u, "lights", at, 600);
            }
            @Override public void tick(PowerUser u) { if (u.now() % 20 == 0) undoPlaced(u, "lights", Blocks.LIGHT, Blocks.AIR, false); }
            @Override public void stop(PowerUser u) { undoPlaced(u, "lights", Blocks.LIGHT, Blocks.AIR, true); }
        });
        // #198 Gambler's Fist: 1 in 20 hits do triple damage, 1 in 20 do nothing
        always(D, 198, "gamblers_fist", PASSIVE, () -> Items.GOLD_INGOT, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                int r = u.entity().getRandom().nextInt(20);
                if (r == 0) { e.setAmount(e.getAmount() * 3); Fx.sound(target, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.3f); }
                else if (r == 1) { e.setAmount(0); Fx.sound(target, SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1f); }
            }
        });
        // #199 Umbrella: hold a banner while falling and you float down gently (on your PC)
        onPcSoftFall(D, 199, "umbrella", () -> Items.WHITE_BANNER);
        // #200 Bounce Back: landing on slime or beds bounces you higher (on your PC)
        onPc(D, 200, "bounce_back", () -> Items.SLIME_BLOCK);
        // #201 Climbing Hands: climb up 1 block of wall like a spider (on your PC)
        onPc(D, 201, "climbing_hands", () -> Items.SPIDER_EYE);
        // #202 Shove: push a mob 4 blocks away, 15 s cooldown
        key(D, 202, "shove", ATTACK, 300, () -> Items.PISTON, u -> {
            LivingEntity t = target(u, 4);
            if (t == null) { refund(u); return; }
            Vec3 dir = t.position().subtract(u.entity().position()).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1.0E-4) dir = u.entity().getViewVector(1);
            dir = dir.normalize().scale(1.3);
            launch(t, new Vec3(dir.x, 0.35, dir.z));
            Fx.sound(t, SoundEvents.PISTON_EXTEND, 0.8f, 1.2f);
            u.entity().swing(InteractionHand.MAIN_HAND, true);
        });
        // #203 Ankle Biter: hits on small mobs (chickens, slimes, silverfish) do double damage
        always(D, 203, "ankle_biter", PASSIVE, () -> Items.SILVERFISH_SPAWN_EGG, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                boolean small = (target.getBbHeight() <= 0.8f && target.getBbWidth() <= 0.8f) || (target instanceof Slime s && s.getSize() <= 1);
                if (small && !(target instanceof net.minecraft.world.entity.player.Player)) e.setAmount(e.getAmount() * 2);
            }
        });
        // #204 Fire Starter: light the block you look at on fire, 30 s cooldown (a mob sets what it fights on fire)
        key(D, 204, "fire_starter", ATTACK, 600, () -> Items.FLINT_AND_STEEL, u -> {
            if (u.mob() != null) {
                LivingEntity t = u.mob().getTarget();
                if (t != null) { t.setSecondsOnFire(3); Fx.sound(t, SoundEvents.FLINTANDSTEEL_USE, 1f, 1f); }
                return;
            }
            BlockHitResult hit = lookBlock(u, 6);
            if (hit == null || u.player() == null) { refund(u); return; }
            BlockPos at = hit.getBlockPos().relative(hit.getDirection());
            if (!BaseFireBlock.canBePlacedAt(u.level(), at, hit.getDirection()) || !u.player().mayUseItemAt(at, hit.getDirection(), new ItemStack(Items.FLINT_AND_STEEL))) {
                refund(u);
                return;
            }
            u.level().setBlock(at, BaseFireBlock.getState(u.level(), at), Block.UPDATE_ALL_IMMEDIATE);
            Fx.sound(u.entity(), SoundEvents.FLINTANDSTEEL_USE, 1f, 1f);
        });
        // #205 Ice Breath: freeze the water in front of you for 10 s
        key(D, 205, "ice_breath", CARRY, 300, () -> Items.BLUE_ICE, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                Vec3 look = e.getViewVector(1);
                int n = 0;
                for (int d = 1; d <= 6; d++) {
                    Vec3 c = e.getEyePosition().add(look.scale(d));
                    for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(c).offset(-1, -2, -1), BlockPos.containing(c).offset(1, 0, 1))) {
                        if (u.level().getFluidState(p).is(Fluids.WATER) && u.level().getBlockState(p).is(Blocks.WATER)
                                && u.level().getBlockState(p.above()).isAir()) {
                            u.level().setBlock(p, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
                            placed(u, "ice", p.immutable(), 200);
                            n++;
                        }
                    }
                }
                Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, e.getEyePosition().add(look.scale(1.5)), 30, 0.6);
                Fx.sound(e, SoundEvents.GLASS_PLACE, 1f, 0.6f);
                if (n == 0) { tell(u, "No water to freeze in front of you."); refund(u); }
            }
            @Override public void tick(PowerUser u) { if (u.now() % 10 == 0) undoPlaced(u, "ice", Blocks.ICE, Blocks.WATER, false); }
            @Override public void stop(PowerUser u) { undoPlaced(u, "ice", Blocks.ICE, Blocks.WATER, true); }
        });
        // #206 Sniffer Nose: a chime when a structure chest (never opened) is within 16 blocks
        always(D, 206, "sniffer_nose", CARRY, () -> Items.SNIFFER_EGG, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 60 != 0) return;
                CompoundTag smelled = u.state().getCompound("smelled");
                for (BlockEntity be : blockEntitiesNear(u, 16)) {
                    if (!(be instanceof RandomizableContainerBlockEntity) || !be.saveWithoutMetadata().contains("LootTable")) continue;
                    String k = Long.toString(be.getBlockPos().asLong());
                    if (smelled.contains(k)) continue;
                    smelled.putBoolean(k, true);
                    u.player().playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.2f);
                    tell(u, "You sniff out treasure nearby!");
                    break;
                }
                if (smelled.size() > 64) smelled = new CompoundTag();
                u.state().put("smelled", smelled);
            }
        });
        // #207 Scavenger: mobs you kill drop 1 extra common drop 10% of the time
        always(D, 207, "scavenger", PASSIVE, () -> Items.ROTTEN_FLESH, new PowerEffect() {
            @Override public void onDrops(PowerUser u, LivingDropsEvent e) {
                if (e.getDrops().isEmpty() || u.entity().getRandom().nextInt(10) != 0) return;
                List<ItemEntity> list = new ArrayList<>(e.getDrops());
                ItemEntity pick = list.get(u.entity().getRandom().nextInt(list.size()));
                ItemStack one = pick.getItem().copyWithCount(1);
                if (one.getMaxStackSize() > 1) e.getDrops().add(new ItemEntity(u.level(), pick.getX(), pick.getY(), pick.getZ(), one));
            }
        });
        // #208 Moon Hunter: the fuller the moon, the harder you hit at night (up to 15% more on a full moon)
        always(D, 208, "moon_hunter", PASSIVE, () -> Items.ENDER_EYE, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (u.level().isNight()) e.setAmount(e.getAmount() * (1 + 0.15f * u.level().getMoonBrightness()));
            }
        });
        // #209 Sunbather: regen half a heart every minute in daylight
        always(D, 209, "sunbather", PASSIVE, () -> Items.SUNFLOWER, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                boolean sun = u.level().isDay() && !u.level().isRaining() && u.level().canSeeSky(BlockPos.containing(u.entity().getEyePosition()));
                int t = sun ? u.state().getInt("sun") + 1 : 0;
                u.state().putInt("sun", t);
                if (sun && t % 60 == 0) u.entity().heal(1.0f);
            }
        });
        // #210 Soul Sight: see players' outlines through walls within 16 blocks (on your PC)
        onPc(D, 210, "soul_sight", () -> Items.SOUL_LANTERN);
        // #211 Mob Bait: throw a lure; mobs go after it for 5 s, 1 minute cooldown
        key(D, 211, "mob_bait", CARRY, 1200, () -> Items.ROTTEN_FLESH, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                BlockHitResult hit = lookBlock(u, 12);
                Vec3 at = hit != null ? Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()))
                        : u.entity().position().add(u.entity().getViewVector(1).multiply(1, 0, 1).normalize().scale(6));
                ArmorStand lure = EntityType.ARMOR_STAND.create(u.level());
                if (lure == null) { refund(u); return; }
                CompoundTag t = lure.saveWithoutId(new CompoundTag());
                t.putBoolean("Small", true);
                t.putBoolean("Invisible", true);
                lure.load(t);
                lure.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.ROTTEN_FLESH)); // all you see is a bit of flesh
                lure.moveTo(at.x, at.y, at.z, 0, 0);
                lure.setInvulnerable(true);
                makeMine(u, lure);
                u.level().addFreshEntity(lure);
                u.state().putUUID("lure", lure.getUUID());
                startFor(u, "lureUntil", 100);
                Fx.sound(u.entity(), SoundEvents.SNOWBALL_THROW, 1f, 0.6f);
            }
            @Override public void tick(PowerUser u) {
                Entity lure = find(u, "lure");
                if (lure == null) return;
                if (!running(u, "lureUntil")) {
                    for (Mob m : u.level().getEntitiesOfClass(Mob.class, lure.getBoundingBox().inflate(16), m -> m.getTarget() == lure)) m.setTarget(null);
                    forget(u, "lure");
                    return;
                }
                if (u.now() % 10 != 0) return;
                for (Mob m : u.level().getEntitiesOfClass(Mob.class, lure.getBoundingBox().inflate(12), m -> !isMine(u, m) && !(m instanceof TamableAnimal ta && ta.isTame()))) {
                    if (m instanceof Enemy) m.setTarget((LivingEntity) lure);
                    else m.getNavigation().moveTo(lure, 1.1);
                }
            }
            @Override public void stop(PowerUser u) { forget(u, "lure"); }
        });
        // #212 Smoke Bomb: blind everything near you for 3 s, 1 minute cooldown
        key(D, 212, "smoke_bomb", ESCAPE, 1200, () -> Items.GUNPOWDER, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Vec3 at = u.entity().position().add(0, 1, 0);
                Fx.particles(u.level(), ParticleTypes.CAMPFIRE_COSY_SMOKE, at, 40, 1.2);
                Fx.particles(u.level(), ParticleTypes.LARGE_SMOKE, at, 30, 1.5);
                Fx.sound(u.entity(), SoundEvents.FIRE_EXTINGUISH, 1f, 0.6f);
                for (LivingEntity t : foes(u, 6)) {
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
                    if (t instanceof Mob m) { m.setTarget(null); remember(u, "smoked", m, u.now() + 60); }
                }
            }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "smoked", e -> { if (e instanceof Mob m) m.setTarget(null); }, e -> {});
            }
        });
        // #213 Roll: crouch as you land to take half the fall damage
        always(D, 213, "roll", CARRY, () -> Items.LEATHER_BOOTS, new PowerEffect() {
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) {
                if (u.entity().isCrouching()) e.setDamageMultiplier(e.getDamageMultiplier() * 0.5f);
            }
        });
        // #214 Arrow Catcher: 10% chance to catch an arrow shot at you, and keep it (RankEvents)
        always(D, 214, "arrow_catcher", PASSIVE, () -> Items.ARROW, new PowerEffect() {});
        // #215 Trapper: a cobweb appears where you look for 10 s, 1 minute cooldown (a mob puts one at your feet)
        key(D, 215, "trapper", ATTACK, 1200, () -> Items.COBWEB, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                BlockPos at;
                if (u.mob() != null && u.mob().getTarget() != null) at = u.mob().getTarget().blockPosition();
                else {
                    BlockHitResult hit = lookBlock(u, 10);
                    if (hit == null) { refund(u); return; }
                    at = hit.getBlockPos().relative(hit.getDirection());
                }
                if (!u.level().getBlockState(at).isAir()) { refund(u); return; }
                u.level().setBlock(at, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_ALL);
                placed(u, "webs", at, 200);
            }
            @Override public void tick(PowerUser u) { if (u.now() % 10 == 0) undoPlaced(u, "webs", Blocks.COBWEB, Blocks.AIR, false); }
            @Override public void stop(PowerUser u) { undoPlaced(u, "webs", Blocks.COBWEB, Blocks.AIR, true); }
        });
        // #216 Sling: fling a block from your hand as a shot for 1 heart, 5 s cooldown (RankEvents does the hit)
        key(D, 216, "sling", CARRY, 100, () -> Items.COBBLESTONE, u -> {
            ItemStack held = u.entity().getMainHandItem();
            if (!(held.getItem() instanceof BlockItem) || u.player() == null) { tell(u, "Hold a block to sling it."); refund(u); return; }
            Snowball shot = new Snowball(u.level(), u.entity());
            shot.setItem(held.copyWithCount(1));
            shot.addTag(RankEvents.SLING);
            EPowers.aim(u, shot, 1.6f);
            u.level().addFreshEntity(shot);
            if (!u.player().getAbilities().instabuild) held.shrink(1);
            Fx.sound(u.entity(), SoundEvents.WITCH_THROW, 1f, 1.2f);
        });
        // #217 Trap Sense: tripwires and pressure plates near you sparkle (only you see it)
        always(D, 217, "trap_sense", CARRY, () -> Items.TRIPWIRE_HOOK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 20 != 0) return;
                for (BlockPos p : BlockPos.betweenClosed(u.entity().blockPosition().offset(-8, -4, -8), u.entity().blockPosition().offset(8, 4, 8))) {
                    BlockState s = u.level().getBlockState(p);
                    if (s.is(Blocks.TRIPWIRE) || s.is(Blocks.TRIPWIRE_HOOK) || s.is(BlockTags.PRESSURE_PLATES)) {
                        u.level().sendParticles(u.player(), ParticleTypes.GLOW, true, p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0);
                    }
                }
            }
        });
        // #218 Anchor: nothing can knock you back for 10 s, 1 minute cooldown
        key(D, 218, "anchor", ATTACK, 1200, () -> Items.ANVIL, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "anchor", 200); Fx.sound(u.entity(), SoundEvents.ANVIL_PLACE, 0.6f, 1.2f); }
            @Override public void onKnockback(PowerUser u, LivingKnockBackEvent e) { if (running(u, "anchor")) e.setCanceled(true); }
        });
        // #219 Grapple Rod: hook a block with a fishing rod and reel in to pull yourself to it
        always(D, 219, "grapple_rod", CARRY, () -> Items.FISHING_ROD, new PowerEffect() {
            @Override public void onRightClick(PowerUser u, ItemStack held, BlockState block) {
                ServerPlayer p = u.player();
                if (p == null || block != null || !(held.getItem() instanceof FishingRodItem) || p.fishing == null) return;
                var hook = p.fishing;
                boolean onBlock = !hook.isInWater() && (hook.onGround() || hook.horizontalCollision
                        || !u.level().getBlockState(hook.blockPosition().below()).isAir());
                if (!onBlock || hook.getHookedIn() != null) return;
                Vec3 to = hook.position().subtract(p.position());
                double len = to.length();
                if (len < 1.5) return;
                Vec3 pull = to.normalize().scale(Math.min(2.2, 0.35 + len * 0.12));
                launch(p, pull.add(0, 0.35, 0));
                startFor(u, "soft", 60);
                Fx.sound(p, SoundEvents.FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
            }
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) { if (running(u, "soft")) e.setDamageMultiplier(0); }
        });
        // #220 Grudge: whoever hit you last glows for 10 s
        always(D, 220, "grudge", PASSIVE, () -> Items.GLOW_INK_SAC, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (e.getSource().getEntity() instanceof LivingEntity by && by != u.entity()) {
                    by.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                }
            }
        });
        // #221 Sapper: creepers you kill drop double gunpowder
        always(D, 221, "sapper", PASSIVE, () -> Items.GUNPOWDER, new PowerEffect() {
            @Override public void onDrops(PowerUser u, LivingDropsEvent e) {
                if (!(e.getEntity() instanceof Creeper)) return;
                List<ItemEntity> more = new ArrayList<>();
                for (ItemEntity it : e.getDrops()) if (it.getItem().is(Items.GUNPOWDER)) more.add(new ItemEntity(u.level(), it.getX(), it.getY(), it.getZ(), it.getItem().copy()));
                e.getDrops().addAll(more);
            }
        });
        // #222 Bee Keeper: taking honey never angers bees, and you get 1 extra honeycomb
        always(D, 222, "bee_keeper", CARRY, () -> Items.BEEHIVE, new PowerEffect() {
            @Override public void onUseBlock(PowerUser u, PlayerInteractEvent.RightClickBlock e) {
                BlockState s = u.level().getBlockState(e.getPos());
                if (!(s.getBlock() instanceof BeehiveBlock) || s.getValue(BeehiveBlock.HONEY_LEVEL) < 5) return;
                ItemStack held = e.getItemStack();
                if (held.getItem() instanceof ShearsItem) Block.popResource(u.level(), e.getPos(), new ItemStack(Items.HONEYCOMB));
                if (held.getItem() instanceof ShearsItem || held.is(Items.GLASS_BOTTLE)) startFor(u, "calm", 200);
            }
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) { if (running(u, "calm") && e.getEntity() instanceof Bee) e.setCanceled(true); }
            @Override public void tick(PowerUser u) {
                if (!running(u, "calm")) return;
                for (Bee b : u.level().getEntitiesOfClass(Bee.class, u.entity().getBoundingBox().inflate(16))) {
                    if (b.getTarget() == u.entity()) { b.stopBeingAngry(); b.setTarget(null); }
                }
            }
        });
        // #223 Home Sweet Home: you slowly heal within 16 blocks of your bed
        always(D, 223, "home_sweet_home", CARRY, () -> Items.RED_BED, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                ServerPlayer p = u.player();
                if (p == null || u.now() % 200 != 0) return;
                BlockPos bed = p.getRespawnPosition();
                if (bed != null && p.getRespawnDimension() == p.level().dimension() && bed.closerToCenterThan(p.position(), 16)) p.heal(1.0f);
            }
        });
        // #224 Snowman Maker: make a snow golem out of nothing, once every 5 minutes
        key(D, 224, "snowman_maker", RANDOM, 6000, () -> Items.CARVED_PUMPKIN, u -> {
            SnowGolem g = EntityType.SNOW_GOLEM.create(u.level());
            if (g == null) return;
            Vec3 at = u.entity().position().add(u.entity().getViewVector(1).multiply(1, 0, 1).normalize().scale(2));
            g.moveTo(at.x, u.entity().getY(), at.z, u.entity().getYRot() + 180, 0);
            u.level().addFreshEntity(g);
            Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, at.add(0, 1, 0), 30, 0.5);
            Fx.sound(g, SoundEvents.SNOW_GOLEM_AMBIENT, 1f, 1f);
        });
        // #225 Breadcrumbs: in caves you leave glowing footprints for 30 s, so you can find your way back (only you see them)
        always(D, 225, "breadcrumbs", CARRY, () -> Items.GLOW_BERRIES, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                ServerPlayer p = u.player();
                if (p == null) return;
                CompoundTag crumbs = u.state().getCompound("crumbs");
                if (stepped(u) && !u.level().canSeeSky(p.blockPosition()) && p.getY() < u.level().getSeaLevel()) {
                    if (crumbs.size() < 80) crumbs.putLong(Long.toString(BlockPos.containing(p.position().add(0, 0.05, 0)).asLong()), u.now() + 600);
                }
                if (u.now() % 10 == 0) {
                    for (String k : crumbs.getAllKeys().toArray(new String[0])) {
                        if (u.now() >= crumbs.getLong(k)) { crumbs.remove(k); continue; }
                        BlockPos b = BlockPos.of(Long.parseLong(k));
                        u.level().sendParticles(p, ParticleTypes.GLOW, true, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, 1, 0.1, 0, 0.1, 0);
                    }
                }
                u.state().put("crumbs", crumbs);
            }
        });
        // #226 Mirror Step: hit from behind? 20% chance you swap places with whoever hit you
        always(D, 226, "mirror_step", PASSIVE, () -> Items.GLASS_PANE, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (!(e.getSource().getDirectEntity() instanceof LivingEntity by) || by == u.entity()) return;
                Vec3 look = u.entity().getViewVector(1).multiply(1, 0, 1);
                Vec3 toThem = by.position().subtract(u.entity().position()).multiply(1, 0, 1);
                if (look.lengthSqr() < 1.0E-4 || toThem.lengthSqr() < 1.0E-4) return;
                if (look.normalize().dot(toThem.normalize()) < -0.3 && u.entity().getRandom().nextInt(5) == 0) swap(u.entity(), by);
            }
        });
        // #227 Low Profile: while crouching, mobs take twice as long to notice you
        always(D, 227, "low_profile", CARRY, () -> Items.GRAY_CARPET, new PowerEffect() {
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) {
                if (u.entity().isCrouching() && e.getOriginalTarget() != u.entity() && u.entity().getRandom().nextBoolean()) e.setCanceled(true);
            }
        });
        // #228 Pickpocket: steal 1 random item from a mob's hands or armour, 2 minute cooldown
        key(D, 228, "pickpocket", CARRY, 2400, () -> Items.LEATHER, u -> {
            LivingEntity t = target(u, 4);
            if (!(t instanceof Mob m) || u.player() == null) { refund(u); return; }
            List<EquipmentSlot> full = new ArrayList<>();
            for (EquipmentSlot s : EquipmentSlot.values()) if (!m.getItemBySlot(s).isEmpty()) full.add(s);
            if (full.isEmpty()) { tell(u, "Its pockets are empty."); refund(u); return; }
            EquipmentSlot s = full.get(u.entity().getRandom().nextInt(full.size()));
            give(u, m.getItemBySlot(s).copy());
            m.setItemSlot(s, ItemStack.EMPTY);
            Fx.sound(u.entity(), SoundEvents.ITEM_PICKUP, 1f, 1.4f);
            tell(u, "Yoink!");
        });
        // #229 Spare Arrow: shoot your last arrow and one more appears, once a minute
        always(D, 229, "spare_arrow", CARRY, () -> Items.SPECTRAL_ARROW, new PowerEffect() {
            @Override public void onShoot(PowerUser u, Projectile p) { if (p instanceof AbstractArrow && u.player() != null) u.state().putLong("check", u.now() + 1); }
            @Override public void tick(PowerUser u) {
                long at = u.state().getLong("check");
                ServerPlayer p = u.player();
                if (at == 0 || u.now() < at || p == null) return;
                u.state().putLong("check", 0);
                boolean left = false;
                for (int i = 0; i < p.getInventory().getContainerSize() && !left; i++) left = p.getInventory().getItem(i).is(ItemTags.ARROWS);
                if (!left && !p.getAbilities().instabuild && !running(u, "spareCd")) {
                    give(u, new ItemStack(Items.ARROW));
                    startFor(u, "spareCd", 1200);
                    tell(u, "One more arrow!");
                }
            }
        });
        // #230 Debris Nose: in the Nether, a chime when ancient debris is within 6 blocks
        always(D, 230, "debris_nose", CARRY, () -> Items.ANCIENT_DEBRIS, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 60 != 0 || u.level().dimension() != Level.NETHER || running(u, "chimed")) return;
                BlockPos me = u.entity().blockPosition();
                for (BlockPos p : BlockPos.betweenClosed(me.offset(-6, -6, -6), me.offset(6, 6, 6))) {
                    if (u.level().getBlockState(p).is(Blocks.ANCIENT_DEBRIS)) {
                        u.player().playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.6f);
                        tell(u, "You smell ancient debris...");
                        startFor(u, "chimed", 200);
                        return;
                    }
                }
            }
        });
    }

    // --- building blocks ---

    // A mob moves away from what it's fighting; a player goes the way they face.
    static Vec3 awayOrForward(PowerUser u) {
        LivingEntity e = u.entity();
        if (u.mob() != null && u.mob().getTarget() != null) return e.position().subtract(u.mob().getTarget().position());
        return e.getViewVector(1);
    }

    // Ores sometimes drop twice (Lucky Miner, Fortune Finger): 1 in `oneIn` times, the drops come again.
    static PowerEffect extraOreDrops(int oneIn) {
        return new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                BlockState s = e.getState();
                if (!s.is(Tags.Blocks.ORES) || e.getPlayer().isCreative() || u.entity().getRandom().nextInt(oneIn) != 0) return;
                ItemStack tool = u.entity().getMainHandItem();
                if (!e.getPlayer().hasCorrectToolForDrops(s)) return;
                ServerLevel l = u.level();
                List<ItemStack> drops = Block.getDrops(s, l, e.getPos(), l.getBlockEntity(e.getPos()), u.entity(), tool);
                for (ItemStack d : drops) if (!d.is(s.getBlock().asItem())) Block.popResource(l, e.getPos(), d); // not when it drops itself (silk touch)
            }
        };
    }

    static void setFuse(Creeper c, boolean slow) {
        CompoundTag t = new CompoundTag();
        c.addAdditionalSaveData(t);
        CompoundTag mine = c.getPersistentData();
        if (slow) {
            mine.putShort("charpowers.fuse", t.getShort("Fuse"));
            t.putShort("Fuse", (short) (t.getShort("Fuse") + 20));
        } else {
            if (!mine.contains("charpowers.fuse")) return;
            t.putShort("Fuse", mine.getShort("charpowers.fuse"));
            mine.remove("charpowers.fuse");
        }
        c.readAdditionalSaveData(t);
    }

    // Blocks a power put down for a while (lights, ice, cobwebs): remembered so they can be put back after.
    static void placed(PowerUser u, String list, BlockPos at, int ticks) {
        CompoundTag t = u.state().getCompound(list);
        t.putLong(Long.toString(at.asLong()), u.now() + ticks);
        u.state().put(list, t);
    }

    // Puts `was` back where the power's `block` still is, once its time is up (or now, if `all`).
    static void undoPlaced(PowerUser u, String list, Block block, Block was, boolean all) {
        if (!u.state().contains(list)) return;
        CompoundTag t = u.state().getCompound(list);
        for (String k : t.getAllKeys().toArray(new String[0])) {
            if (!all && u.now() < t.getLong(k)) continue;
            BlockPos p = BlockPos.of(Long.parseLong(k));
            if (!u.level().isLoaded(p)) continue; // too far away right now: tidied up when you're back near it
            if (u.level().getBlockState(p).is(block)) u.level().setBlock(p, was.defaultBlockState(), Block.UPDATE_ALL);
            t.remove(k);
        }
    }

    // After the last log goes, leaves that no longer touch a log (they'd decay anyway) all drop at once.
    static void dropLooseLeaves(ServerLevel l, BlockPos start, Entity by) {
        java.util.ArrayDeque<BlockPos> todo = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (Direction d : Direction.values()) todo.add(start.relative(d));
        todo.add(start.above(2));
        int broken = 0;
        while (!todo.isEmpty() && seen.size() < 600 && broken < 300) {
            BlockPos p = todo.poll();
            if (!seen.add(p) || !l.isLoaded(p)) continue;
            BlockState s = l.getBlockState(p);
            if (!(s.getBlock() instanceof LeavesBlock) || s.getValue(LeavesBlock.PERSISTENT)) continue;
            if (s.getValue(LeavesBlock.DISTANCE) < LeavesBlock.DECAY_DISTANCE) continue; // still held by another log
            l.destroyBlock(p, true, by);
            broken++;
            for (Direction d : Direction.values()) todo.add(p.relative(d));
        }
    }

    static List<BlockEntity> blockEntitiesNear(PowerUser u, int range) {
        List<BlockEntity> out = new ArrayList<>();
        BlockPos me = u.entity().blockPosition();
        int cx = me.getX() >> 4, cz = me.getZ() >> 4, cr = (range >> 4) + 1;
        for (int dx = -cr; dx <= cr; dx++) for (int dz = -cr; dz <= cr; dz++) {
            if (!u.level().hasChunk(cx + dx, cz + dz)) continue;
            LevelChunk c = u.level().getChunk(cx + dx, cz + dz);
            for (BlockEntity be : c.getBlockEntities().values()) if (be.getBlockPos().closerThan(me, range)) out.add(be);
        }
        return out;
    }

    private DPowers() {}
}
