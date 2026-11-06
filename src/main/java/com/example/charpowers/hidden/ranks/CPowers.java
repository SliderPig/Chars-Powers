package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;
import static com.example.charpowers.hidden.model.Rank.C;
import static com.example.charpowers.hidden.ranks.RankHelp.*;

// The C-rank hidden powers (actually useful), numbers 231-265 in my list.
final class CPowers {
    private static boolean busy; // stops Chain Miner / Cleave from setting themselves off again

    static void registerAll() {
        // #231 Dash: dash 4 blocks forward, 10 s cooldown (a mob dashes at what it fights)
        key(C, 231, "dash", ATTACK, 200, () -> Items.SUGAR, u -> {
            Vec3 dir = towardOrForward(u).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1.0E-4) { refund(u); return; }
            dir = dir.normalize().scale(1.6);
            launch(u.entity(), new Vec3(dir.x, 0.1, dir.z));
            Fx.particles(u.level(), ParticleTypes.CLOUD, u.entity().position().add(0, 0.2, 0), 8, 0.3);
            Fx.sound(u.entity(), SoundEvents.PHANTOM_FLAP, 0.8f, 1.6f);
        });
        // #232 Ground Pound: while falling, slam down for small damage around you
        key(C, 232, "ground_pound", ATTACK, 200, () -> Items.ANVIL, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                if (e.onGround()) {
                    if (u.mob() == null) { tell(u, "Jump first, then slam!"); refund(u); return; }
                    launch(e, new Vec3(0, 0.8, 0)); // a mob jumps up, then comes down hard
                    u.state().putLong("slamAt", u.now() + 8);
                    return;
                }
                slam(u);
            }
            @Override public void tick(PowerUser u) {
                long at = u.state().getLong("slamAt");
                if (at != 0 && u.now() >= at) { u.state().putLong("slamAt", 0); slam(u); }
                if (!u.state().getBoolean("pound") || !u.entity().onGround()) return;
                u.state().putBoolean("pound", false);
                Fx.sound(u.entity(), SoundEvents.GENERIC_EXPLODE, 0.6f, 1.4f);
                Fx.particles(u.level(), ParticleTypes.EXPLOSION, u.entity().position(), 3, 0.8);
                for (LivingEntity t : foes(u, 3)) {
                    t.hurt(u.isPlayer() ? u.entity().damageSources().playerAttack(u.player()) : u.entity().damageSources().mobAttack(u.entity()), 4.0f);
                    Fx.push(t, u.entity().position(), 0.6);
                }
            }
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) { if (u.state().getBoolean("pound")) e.setDamageMultiplier(0); }
            private void slam(PowerUser u) {
                launch(u.entity(), new Vec3(0, -1.6, 0));
                u.state().putBoolean("pound", true);
            }
        });
        // #233 Glide: hold jump while falling to fall slowly (on your PC)
        onPcSoftFall(C, 233, "glide", () -> Items.ELYTRA);
        // #234 Wall Run: run along a wall for 1 second (on your PC)
        onPcSoftFall(C, 234, "wall_run", () -> Items.STONE_BRICK_WALL);
        // #235 Super Jump: crouch 2 s, then press the key to jump 4 blocks high
        key(C, 235, "super_jump", ATTACK, 100, () -> Items.SLIME_BLOCK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                boolean crouch = u.entity().isCrouching() && u.entity().onGround();
                u.state().putInt("crouch", crouch ? u.state().getInt("crouch") + 1 : 0);
            }
            @Override public void onKey(PowerUser u) {
                if (u.mob() == null && u.state().getInt("crouch") < 40) { tell(u, "Crouch for 2 seconds first."); refund(u); return; }
                if (!u.entity().onGround()) { refund(u); return; }
                Vec3 v = u.entity().getDeltaMovement();
                launch(u.entity(), new Vec3(v.x, 0.95, v.z));
                startFor(u, "soft", 100);
                Fx.particles(u.level(), ParticleTypes.CLOUD, u.entity().position(), 12, 0.4);
                Fx.sound(u.entity(), SoundEvents.SLIME_JUMP, 1f, 0.8f);
            }
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) { if (running(u, "soft")) e.setDamageMultiplier(0); }
        });
        // #236 Fire Fist: 10% chance your hits set them on fire
        always(C, 236, "fire_fist", PASSIVE, () -> Items.BLAZE_POWDER, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) { if (u.entity().getRandom().nextInt(10) == 0) target.setSecondsOnFire(4); }
        });
        // #237 Frost Fist: your hits slow them down for 2 s
        always(C, 237, "frost_fist", PASSIVE, () -> Items.BLUE_ICE, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
                Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, target.getEyePosition(), 6, 0.3);
            }
        });
        // #238 Soul Snack: heal half a heart for every kill
        always(C, 238, "soul_snack", PASSIVE, () -> Items.SOUL_TORCH, new PowerEffect() {
            @Override public void onKill(PowerUser u, LivingEntity victim) {
                u.entity().heal(1.0f);
                Fx.particles(u.level(), ParticleTypes.SOUL, victim.position().add(0, 1, 0), 4, 0.3);
            }
        });
        // #239 Second Wind: once a day, dropping to 1 heart gives 5 s of strong regen
        always(C, 239, "second_wind", PASSIVE, () -> Items.GHAST_TEAR, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.entity().isAlive() && u.entity().getHealth() <= 2.0f && Fx.daily(u, "wind")) {
                    effect(u.entity(), MobEffects.REGENERATION, 100, 2);
                    Fx.sound(u.entity(), SoundEvents.PLAYER_BREATH, 1f, 1f);
                    tell(u, "Second wind!");
                }
            }
        });
        // #240 Shadow Crouch: stand still crouching for 3 s and you turn invisible (until you move). Mobs: stand still.
        always(C, 240, "shadow_crouch", PASSIVE, () -> Items.BLACK_DYE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 5 != 0) return;
                LivingEntity e = u.entity();
                boolean still = !moving(u) && (u.mob() != null || e.isCrouching());
                int t = still ? u.state().getInt("still") + 5 : 0;
                u.state().putInt("still", t);
                if (t >= 60) {
                    e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false, true));
                    u.state().putBoolean("hid", true);
                } else if (u.state().getBoolean("hid")) {
                    unhide(u);
                }
            }
            @Override public void stop(PowerUser u) { if (u.state().getBoolean("hid")) unhide(u); }
            private void unhide(PowerUser u) {
                u.state().putBoolean("hid", false);
                MobEffectInstance i = u.entity().getEffect(MobEffects.INVISIBILITY);
                if (i != null && !i.isVisible() && i.getDuration() <= 20) u.entity().removeEffect(MobEffects.INVISIBILITY);
            }
        });
        // #241 Pocket Table: open a crafting table anywhere
        key(C, 241, "pocket_table", CARRY, 20, () -> Items.CRAFTING_TABLE, u -> {
            ServerPlayer p = u.player();
            if (p == null) return;
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(pl.level(), pl.blockPosition())) {
                @Override public boolean stillValid(Player player) { return true; } // there's no real table to walk away from
            }, Component.translatable("container.crafting")));
        });
        // #242 Fixer: your held item repairs 1 durability a minute
        always(C, 242, "fixer", PASSIVE, () -> Items.ANVIL, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (!u.every("fix", 1200)) return;
                ItemStack held = u.entity().getMainHandItem();
                if (held.isDamaged()) held.setDamageValue(held.getDamageValue() - 1);
            }
        });
        // #243 Good Haggler: villagers give you 5% off
        always(C, 243, "good_haggler", CARRY, () -> Items.EMERALD, new PowerEffect() {
            @Override public void onUseEntity(PowerUser u, PlayerInteractEvent.EntityInteract e) {
                if (!(e.getTarget() instanceof Villager v) || v.isTrading() || v.isBaby() || v.getOffers().isEmpty()) return;
                VillagerProfession job = v.getVillagerData().getProfession();
                if (job == VillagerProfession.NONE || job == VillagerProfession.NITWIT) return;
                // Minecraft resets these discounts when you close the trade screen
                for (MerchantOffer o : v.getOffers()) o.addToSpecialPriceDiff(-Math.round(o.getBaseCostA().getCount() * 0.05f));
            }
        });
        // #244 Chain Miner: mining an ore also mines up to 3 touching ores of the same kind
        always(C, 244, "chain_miner", CARRY, () -> Items.DIAMOND_PICKAXE, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                ServerPlayer p = u.player();
                BlockState s = e.getState();
                if (busy || p == null || !s.is(Tags.Blocks.ORES) || !p.hasCorrectToolForDrops(s)) return;
                busy = true;
                try {
                    int left = 3;
                    for (BlockPos n : BlockPos.betweenClosed(e.getPos().offset(-1, -1, -1), e.getPos().offset(1, 1, 1))) {
                        if (left == 0) break;
                        if (n.equals(e.getPos()) || !u.level().getBlockState(n).is(s.getBlock())) continue;
                        if (p.gameMode.destroyBlock(n.immutable())) left--; // a normal break: your tool, enchantments, wear
                    }
                } finally {
                    busy = false;
                }
            }
        });
        // #245 Adrenaline: taking a big hit (3+ hearts) makes you faster and mine faster for 5 s
        always(C, 245, "adrenaline", PASSIVE, () -> Items.REDSTONE, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (e.getAmount() < 6.0f) return;
                effect(u.entity(), MobEffects.MOVEMENT_SPEED, 100, 1);
                effect(u.entity(), MobEffects.DIG_SPEED, 100, 1);
            }
        });
        // #246 Rage Meter: every hit you take fills a meter; when it's full, your next hit does double damage
        always(C, 246, "rage_meter", PASSIVE, () -> Items.NETHER_WART, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (e.getAmount() <= 0 || e.getSource().getEntity() == u.entity()) return;
                int r = Math.min(10, u.state().getInt("rage") + 1);
                u.state().putInt("rage", r);
                if (r == 10) tell(u, "RAGE FULL! Your next hit does double damage.");
            }
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (u.state().getInt("rage") < 10) return;
                u.state().putInt("rage", 0);
                e.setAmount(e.getAmount() * 2);
                Fx.particles(u.level(), ParticleTypes.ANGRY_VILLAGER, target.getEyePosition(), 5, 0.3);
            }
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                int r = u.state().getInt("rage");
                hud(u, "rage", r == 0 ? "" : "Rage " + "|".repeat(r) + ".".repeat(10 - r));
            }
            @Override public void stop(PowerUser u) { hud(u, "rage", ""); }
        });
        // #247 Second Breakfast: eating when you're full saves the food for later (up to 5 hunger points)
        always(C, 247, "second_breakfast", CARRY, () -> Items.PUMPKIN_PIE, new PowerEffect() {
            @Override public void onRightClick(PowerUser u, ItemStack held, BlockState block) {
                ServerPlayer p = u.player();
                if (p == null || block != null || !held.isEdible() || p.getFoodData().needsFood()) return;
                FoodProperties f = held.getFoodProperties(p);
                int stored = u.state().getInt("stored");
                if (f == null || f.canAlwaysEat() || stored >= 10) return;
                u.state().putInt("stored", Math.min(10, stored + f.getNutrition()));
                if (!p.getAbilities().instabuild) held.shrink(1);
                Fx.sound(p, SoundEvents.GENERIC_EAT, 0.8f, 1.2f);
                tell(u, "Saved for later.");
            }
            @Override public void tick(PowerUser u) {
                ServerPlayer p = u.player();
                if (p == null || u.now() % 20 != 0) return;
                int stored = u.state().getInt("stored");
                int food = p.getFoodData().getFoodLevel();
                if (stored > 0 && food <= 16) {
                    int n = Math.min(stored, 20 - food);
                    p.getFoodData().eat(n, 0.3f);
                    u.state().putInt("stored", stored - n);
                }
                hud(u, "breakfast", stored > 0 ? "Saved food: " + stored : "");
            }
            @Override public void stop(PowerUser u) { hud(u, "breakfast", ""); }
        });
        // #248 Spark Skin: 5% chance a tiny spark zaps whoever hits you (1 heart). No lightning bolt: no flashing.
        always(C, 248, "spark_skin", PASSIVE, () -> Items.LIGHTNING_ROD, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (e.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) return;
                if (!(e.getSource().getDirectEntity() instanceof LivingEntity by) || by == u.entity() || u.entity().getRandom().nextInt(20) != 0) return;
                by.hurt(u.entity().damageSources().thorns(u.entity()), 2.0f);
                Fx.particles(u.level(), ParticleTypes.ELECTRIC_SPARK, by.getEyePosition(), 12, 0.3);
                Fx.sound(by, com.example.charpowers.hidden.ModSounds.ZAP, 1f, 1f);
            }
        });
        // #249 Ore Chime: a soft chime when diamonds are within 5 blocks
        always(C, 249, "ore_chime", CARRY, () -> Items.DIAMOND_ORE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 40 != 0 || running(u, "chimed")) return;
                BlockPos me = u.entity().blockPosition();
                for (BlockPos p : BlockPos.betweenClosed(me.offset(-5, -5, -5), me.offset(5, 5, 5))) {
                    if (u.level().getBlockState(p).is(Tags.Blocks.ORES_DIAMOND)) {
                        u.player().playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
                        startFor(u, "chimed", 100);
                        return;
                    }
                }
            }
        });
        // #250 Lava Wader: 2 s of fire immunity when you fall in lava
        always(C, 250, "lava_wader", PASSIVE, () -> Items.LAVA_BUCKET, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                boolean in = u.entity().isInLava();
                if (in && !u.state().getBoolean("was") && !running(u, "waderCd")) {
                    u.entity().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
                    startFor(u, "waderCd", 200);
                }
                u.state().putBoolean("was", in);
            }
        });
        // #251 Air Pocket: a bubble of air around your head for 10 s, 1 minute cooldown
        key(C, 251, "air_pocket", CARRY, 1200, () -> Items.GLASS, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "air", 200); Fx.sound(u.entity(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f); }
            @Override public void tick(PowerUser u) {
                if (!running(u, "air")) return;
                u.entity().setAirSupply(u.entity().getMaxAirSupply());
                if (u.now() % 10 == 0 && u.entity().isInWater()) Fx.particles(u.level(), ParticleTypes.BUBBLE, u.entity().getEyePosition(), 6, 0.4);
            }
        });
        // #252 Battle Cry: nearby hostile mobs get Weakness for 5 s, 1 minute cooldown
        key(C, 252, "battle_cry", ATTACK, 1200, () -> Items.GOAT_HORN, u -> {
            Fx.sound(u.entity(), SoundEvents.RAVAGER_ROAR, 0.8f, 1.3f);
            for (LivingEntity t : hostiles(u, 10)) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true, true));
        });
        // #253 Bone Armour: every skeleton you kill gives you 1% less arrow damage (up to 20%); it resets when you die
        always(C, 253, "bone_armour", PASSIVE, () -> Items.BONE_BLOCK, new PowerEffect() {
            @Override public void onKill(PowerUser u, LivingEntity victim) {
                if (!(victim instanceof AbstractSkeleton)) return;
                int b = Math.min(20, u.state().getInt("bones") + 1);
                u.state().putInt("bones", b);
                tell(u, "Bone Armour: " + b + "% less arrow damage");
            }
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) {
                if (e.getSource().getDirectEntity() instanceof AbstractArrow) e.setAmount(e.getAmount() * (1 - u.state().getInt("bones") / 100f));
            }
            @Override public void onDeath(PowerUser u) { u.state().putInt("bones", 0); }
        });
        // #254 Pinball: mobs you knock into a wall bounce off it and take extra damage
        always(C, 254, "pinball", PASSIVE, () -> Items.SLIME_BALL, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) {
                if (target != u.entity()) remember(u, "pin", target, u.now() + 12);
            }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "pin", e -> {
                    if (!e.horizontalCollision || !(e instanceof LivingEntity le)) return;
                    le.invulnerableTime = 0;
                    le.hurt(u.isPlayer() ? u.entity().damageSources().playerAttack(u.player()) : u.entity().damageSources().mobAttack(u.entity()), 2.0f);
                    Vec3 v = e.getDeltaMovement();
                    launch(e, new Vec3(-v.x * 1.5, 0.3, -v.z * 1.5));
                    Fx.sound(e, SoundEvents.SLIME_BLOCK_HIT, 1f, 1.2f);
                    Fx.particles(u.level(), ParticleTypes.CRIT, e.position().add(0, 1, 0), 8, 0.3);
                    remember(u, "pin", e, u.now()); // one bounce per hit
                }, e -> {});
            }
        });
        // #255 Homing Arrows: your arrows curve slightly toward mobs (Arrows steers them)
        always(C, 255, "homing_arrows", PASSIVE, () -> Items.TARGET, new PowerEffect() {
            @Override public void onShoot(PowerUser u, Projectile p) { if (p instanceof AbstractArrow a) Arrows.track(a, Arrows.HOMING); }
        });
        // #256 Bounce Pad: a slime block appears where you look for 10 s, 30 s cooldown
        key(C, 256, "bounce_pad", CARRY, 600, () -> Items.SLIME_BLOCK, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                BlockHitResult hit = lookBlock(u, 8);
                if (hit == null) { refund(u); return; }
                BlockPos at = hit.getBlockPos().relative(hit.getDirection());
                if (!u.level().getBlockState(at).isAir()) { refund(u); return; }
                u.level().setBlock(at, Blocks.SLIME_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                DPowers.placed(u, "pads", at, 200);
                Fx.sound(u.entity(), SoundEvents.SLIME_BLOCK_PLACE, 1f, 1f);
            }
            @Override public void tick(PowerUser u) { if (u.now() % 10 == 0) DPowers.undoPlaced(u, "pads", Blocks.SLIME_BLOCK, Blocks.AIR, false); }
            @Override public void stop(PowerUser u) { DPowers.undoPlaced(u, "pads", Blocks.SLIME_BLOCK, Blocks.AIR, true); }
        });
        // #257 Cleave: axe hits also hit one mob next to the target
        always(C, 257, "cleave", PASSIVE, () -> Items.IRON_AXE, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (busy || e.getSource().getDirectEntity() != u.entity() || !(u.entity().getMainHandItem().getItem() instanceof AxeItem)) return;
                for (LivingEntity next : u.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2),
                        o -> o != target && isFoe(u, o))) {
                    busy = true;
                    try {
                        next.hurt(e.getSource(), e.getAmount() * 0.6f);
                    } finally {
                        busy = false;
                    }
                    Fx.particles(u.level(), ParticleTypes.SWEEP_ATTACK, next.getEyePosition(), 1, 0);
                    return; // just one
                }
            }
        });
        // #258 Overheal: healing past full gives you up to 2 extra yellow hearts
        always(C, 258, "overheal", PASSIVE, () -> Items.GOLDEN_APPLE, new PowerEffect() {
            @Override public void onHeal(PowerUser u, LivingHealEvent e) {
                LivingEntity le = u.entity();
                float extra = le.getHealth() + e.getAmount() - le.getMaxHealth();
                if (extra > 0 && le.getAbsorptionAmount() < 4) le.setAbsorptionAmount(Math.min(4, le.getAbsorptionAmount() + extra));
            }
        });
        // #259 Retreat: leap backwards 5 blocks and get a speed boost for 3 s, 20 s cooldown
        key(C, 259, "retreat", ESCAPE, 400, () -> Items.FEATHER, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Vec3 back = towardOrForward(u).multiply(-1, 0, -1);
                if (back.lengthSqr() < 1.0E-4) { refund(u); return; }
                back = back.normalize().scale(1.3);
                launch(u.entity(), new Vec3(back.x, 0.45, back.z));
                effect(u.entity(), MobEffects.MOVEMENT_SPEED, 60, 1);
                startFor(u, "soft", 60);
                Fx.sound(u.entity(), SoundEvents.PHANTOM_FLAP, 0.8f, 1.2f);
            }
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) { if (running(u, "soft")) e.setDamageMultiplier(0); }
        });
        // #260 Taunt: every mob within 10 blocks comes after YOU for 5 s (great for saving a friend), 1 minute cooldown
        key(C, 260, "taunt", CARRY, 1200, () -> Items.BELL, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Fx.sound(u.entity(), SoundEvents.VINDICATOR_CELEBRATE, 1f, 1.2f);
                for (LivingEntity t : foes(u, 10)) {
                    if (t instanceof Mob m && (m instanceof Enemy || m instanceof NeutralMob)) {
                        m.setTarget(u.entity());
                        remember(u, "taunted", m, u.now() + 100);
                    }
                }
            }
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0) return;
                forEachRemembered(u, "taunted", e -> { if (e instanceof Mob m && m.getTarget() != u.entity()) m.setTarget(u.entity()); }, e -> {});
            }
        });
        // #261 Meat Shield: grab the nearest small mob and hold it in front of you for 5 s; it takes your hits. 1 minute cooldown.
        key(C, 261, "meat_shield", CARRY, 1200, () -> Items.PORKCHOP, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Mob best = null;
                double bestD = 25;
                for (Mob m : u.level().getEntitiesOfClass(Mob.class, u.entity().getBoundingBox().inflate(5))) {
                    if (m == u.entity() || m.getBbHeight() > 1.0f || m.getBbWidth() > 1.0f || isMine(u, m) || (m instanceof TamableAnimal t && t.isTame())) continue;
                    double d = m.distanceToSqr(u.entity());
                    if (d < bestD) { bestD = d; best = m; }
                }
                if (best == null) { tell(u, "No small mob close enough to grab."); refund(u); return; }
                u.state().putUUID("shield", best.getUUID());
                startFor(u, "shieldUntil", 100);
            }
            @Override public void tick(PowerUser u) {
                Entity s = find(u, "shield");
                if (s == null) return;
                if (!running(u, "shieldUntil")) { u.state().remove("shield"); return; }
                Vec3 front = u.entity().position().add(u.entity().getViewVector(1).multiply(1, 0, 1).normalize().scale(0.9)).add(0, 0.6, 0);
                s.teleportTo(front.x, front.y, front.z);
                s.setDeltaMovement(Vec3.ZERO);
                s.fallDistance = 0;
            }
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) {
                Entity s = find(u, "shield");
                if (s == null || !running(u, "shieldUntil") || e.getSource().getEntity() == null || !(s instanceof LivingEntity le)) return;
                e.setCanceled(true);
                le.hurt(e.getSource(), e.getAmount());
            }
        });
        // #262 Fortune Finger: 1 in 5 ores drop extra, like a Fortune pickaxe
        always(C, 262, "fortune_finger", CARRY, () -> Items.LAPIS_LAZULI, DPowers.extraOreDrops(5));
        // #263 Shield Bash: blocking with a shield shoves the attacker far back (RankEvents)
        always(C, 263, "shield_bash", PASSIVE, () -> Items.SHIELD, new PowerEffect() {});
        // #264 Lava Skipper: sprint onto lava and run across it for 2 s (on your PC)
        onPc(C, 264, "lava_skipper", () -> Items.MAGMA_BLOCK);
        // #265 Quick Draw: bows draw and crossbows load 30% faster (RankEvents, also on your PC)
        always(C, 265, "quick_draw", PASSIVE, () -> Items.CROSSBOW, new PowerEffect() {});
    }

    // A mob heads for what it's fighting; a player goes the way they face.
    static Vec3 towardOrForward(PowerUser u) {
        LivingEntity e = u.entity();
        if (u.mob() != null && u.mob().getTarget() != null) return u.mob().getTarget().position().subtract(e.position());
        return e.getViewVector(1);
    }

    static boolean isProjectile(LivingHurtEvent e) { return e.getSource().is(DamageTypeTags.IS_PROJECTILE); }

    private CPowers() {}
}
