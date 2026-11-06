package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Map;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;
import static com.example.charpowers.hidden.model.Rank.E;
import static com.example.charpowers.hidden.ranks.RankHelp.*;

// The E-rank hidden powers (a tiny bit useful), numbers 101-175 in my list.
// Powers marked onPc(...) happen on the player's own PC: see client/ClientMovement and client/ClientHud.
final class EPowers {
    static void registerAll() {
        // #101 Tailwind: walk the same way for 10 s and you slowly speed up (up to 10% faster)
        always(E, 101, "tailwind", PASSIVE, () -> Items.FEATHER, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                Vec3 d = moved(u);
                Vec3 last = new Vec3(u.state().getDouble("dx"), 0, u.state().getDouble("dz"));
                int streak = u.state().getInt("streak");
                boolean same = d.lengthSqr() > 0.5 && last.lengthSqr() > 0.5 && d.normalize().dot(last.normalize()) > 0.9;
                streak = same ? streak + 1 : 0;
                u.state().putInt("streak", streak);
                u.state().putDouble("dx", d.x);
                u.state().putDouble("dz", d.z);
                boost(u.entity(), Attributes.MOVEMENT_SPEED, "tailwind", Math.min(0.10, Math.max(0, streak - 10) * 0.01));
            }
            @Override public void stop(PowerUser u) { boost(u.entity(), Attributes.MOVEMENT_SPEED, "tailwind", 0); }
        });
        // #102 Golden Heart: one extra gold heart that only refills when you eat a golden carrot
        always(E, 102, "golden_heart", PASSIVE, () -> Items.GOLDEN_CARROT, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.state().getBoolean("init")) return;
                u.state().putBoolean("init", true); // you start with it full
                fill(u);
            }
            @Override public void onAte(PowerUser u, ItemStack food) {
                if (food.is(Items.GOLDEN_CARROT)) {
                    fill(u);
                    Fx.sound(u.entity(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
                }
            }
            private void fill(PowerUser u) { u.entity().setAbsorptionAmount(Math.max(u.entity().getAbsorptionAmount(), 2.0f)); }
        });
        // #103 Cat Landing: no fall damage from 5 blocks or less, and a little "mrrp"
        always(E, 103, "cat_landing", PASSIVE, () -> Items.CAT_SPAWN_EGG, new PowerEffect() {
            @Override public void onFallEvent(PowerUser u, LivingFallEvent e) {
                if (e.getDistance() > 5.5f) return;
                e.setDamageMultiplier(0);
                if (e.getDistance() > 2.5f) Fx.sound(u.entity(), SoundEvents.CAT_AMBIENT, 0.7f, 1.5f);
            }
        });
        // #104 Bubble Burp: underwater, burp to get 3 air bubbles back, once a minute
        key(E, 104, "bubble_burp", CARRY, 1200, () -> Items.PUFFERFISH_BUCKET, u -> {
            LivingEntity e = u.entity();
            if (!e.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) { tell(u, "You can only bubble-burp underwater."); refund(u); return; }
            e.setAirSupply(Math.min(e.getMaxAirSupply(), e.getAirSupply() + 90)); // 30 air = one bubble
            Fx.sound(e, com.example.charpowers.hidden.ModSounds.BURP, 1.0f, 0.7f);
            Fx.particles(u.level(), ParticleTypes.BUBBLE, e.getEyePosition(), 12, 0.3);
        });
        // #105 Spring in Your Step: every 5th jump is twice as high
        always(E, 105, "spring_in_your_step", PASSIVE, () -> Items.RABBIT_FOOT, new PowerEffect() {
            @Override public void onJump(PowerUser u) {
                int n = u.state().getInt("jumps") + 1;
                u.state().putInt("jumps", n);
                if (n % 5 != 0) return;
                Vec3 v = u.entity().getDeltaMovement();
                launch(u.entity(), new Vec3(v.x, v.y + 0.3, v.z));
                Fx.sound(u.entity(), SoundEvents.SLIME_JUMP, 0.6f, 1.4f);
            }
        });
        // #106 Breakfast Bonus: the first food you eat after waking up fills you twice as much
        always(E, 106, "breakfast_bonus", CARRY, () -> Items.BREAD, new PowerEffect() {
            @Override public void onWake(PowerUser u) { u.state().putBoolean("breakfast", true); }
            @Override public void onAte(PowerUser u, ItemStack food) {
                if (!u.state().getBoolean("breakfast") || !food.isEdible() || u.player() == null) return;
                u.state().putBoolean("breakfast", false);
                eatAgain(u, food, 1.0f);
                tell(u, "A good breakfast!");
            }
        });
        // #107 Stair Master: walk up stairs and slabs 30% faster
        always(E, 107, "stair_master", PASSIVE, () -> Items.OAK_STAIRS, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 5 != 0) return;
                boolean on = isOn(u.entity(), BlockTags.STAIRS) || isOn(u.entity(), BlockTags.SLABS);
                boost(u.entity(), Attributes.MOVEMENT_SPEED, "stair_master", on ? 0.3 : 0);
            }
            @Override public void stop(PowerUser u) { boost(u.entity(), Attributes.MOVEMENT_SPEED, "stair_master", 0); }
        });
        // #108 Heavy Boots: water currents can't push you (done in mixin/PlayerFluidMixin, on the server and your PC)
        always(E, 108, "heavy_boots", CARRY, () -> Items.IRON_BOOTS, new PowerEffect() {});
        // #109 Antidote Blood: poison wears off twice as fast
        always(E, 109, "antidote_blood", PASSIVE, () -> Items.HONEY_BOTTLE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                MobEffectInstance p = u.entity().getEffect(MobEffects.POISON);
                if (p == null || p.isInfiniteDuration() || p.getDuration() <= 20) return;
                u.entity().removeEffect(MobEffects.POISON);
                u.entity().addEffect(new MobEffectInstance(MobEffects.POISON, p.getDuration() - 20, p.getAmplifier(), p.isAmbient(), p.isVisible(), p.showIcon()));
            }
        });
        // #110 Cave Eyes: in total darkness your eyes adjust after 5 s (you see a bit until you reach light again)
        always(E, 110, "cave_eyes", CARRY, () -> Items.ENDER_EYE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                nightVisionTick(u);
                if (u.now() % 20 != 0) return;
                LivingEntity e = u.entity();
                boolean dark = e.level().getMaxLocalRawBrightness(BlockPos.containing(e.getEyePosition())) == 0;
                int t = dark ? u.state().getInt("dark") + 1 : 0;
                u.state().putInt("dark", t);
                if (t >= 5) steadyNightVision(u, 60);    // keeps going while it's dark
                else if (!dark) stopNightVision(u);       // reached light again
            }
            @Override public void stop(PowerUser u) { stopNightVision(u); }
        });
        // #111 Bee Friend: bees never sting you
        always(E, 111, "bee_friend", PASSIVE, () -> Items.HONEYCOMB, new PowerEffect() {
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) { if (e.getEntity() instanceof Bee) e.setCanceled(true); }
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) { if (e.getSource().getEntity() instanceof Bee) e.setCanceled(true); }
        });
        // #112 Rubber Ducky: you float on water without swimming
        onPc(E, 112, "rubber_ducky", () -> Items.YELLOW_DYE);
        // #113 Long Arms: pick up items from 1 block further
        always(E, 113, "long_arms", CARRY, () -> Items.STICK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 2 != 0 || u.player().isSpectator()) return;
                LivingEntity e = u.entity();
                for (ItemEntity it : u.level().getEntitiesOfClass(ItemEntity.class, e.getBoundingBox().inflate(2.0, 1.5, 2.0))) {
                    if (!it.hasPickUpDelay() && it.isAlive()) it.setPos(e.getX(), e.getY() + 0.2, e.getZ()); // into normal reach
                }
            }
        });
        // #114 Light Step: you never trample farmland (RankEvents)
        always(E, 114, "light_step", PASSIVE, () -> Items.FARMLAND, new PowerEffect() {});
        // #115 Snowball Pocket: pull a snowball out of nowhere and throw it
        key(E, 115, "snowball_pocket", ATTACK, 20, () -> Items.SNOWBALL, u -> {
            LivingEntity e = u.entity();
            Snowball ball = new Snowball(u.level(), e);
            ball.setItem(new ItemStack(Items.SNOWBALL));
            aim(u, ball, 1.5f);
            u.level().addFreshEntity(ball);
            Fx.sound(e, SoundEvents.SNOWBALL_THROW, 0.6f, 0.5f);
        });
        // #116 Lava Toes: 1 s of fire protection right after you touch lava
        always(E, 116, "lava_toes", PASSIVE, () -> Items.MAGMA_CREAM, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                boolean in = u.entity().isInLava();
                if (in && !u.state().getBoolean("was") && !running(u, "toesCd")) {
                    u.entity().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 0, false, false, true));
                    startFor(u, "toesCd", 100);
                }
                u.state().putBoolean("was", in);
            }
        });
        // #117 Iron Stomach: rotten flesh never makes you hungry
        always(E, 117, "iron_stomach", CARRY, () -> Items.ROTTEN_FLESH, new PowerEffect() {
            @Override public void onAte(PowerUser u, ItemStack food) { if (food.is(Items.ROTTEN_FLESH)) u.entity().removeEffect(MobEffects.HUNGER); }
        });
        // #118 Raw Eater: raw meat fills you like cooked
        always(E, 118, "raw_eater", CARRY, () -> Items.BEEF, new PowerEffect() {
            @Override public void onAte(PowerUser u, ItemStack food) {
                Item cooked = COOKED.get(food.getItem());
                if (cooked == null || u.player() == null) return;
                FoodProperties raw = food.getFoodProperties(u.entity());
                FoodProperties done = new ItemStack(cooked).getFoodProperties(u.entity());
                if (raw == null || done == null) return;
                int more = done.getNutrition() - raw.getNutrition();
                float sat = done.getNutrition() * done.getSaturationModifier() * 2 - raw.getNutrition() * raw.getSaturationModifier() * 2;
                if (more > 0) u.player().getFoodData().eat(more, sat / (2f * more));
                u.entity().removeEffect(MobEffects.HUNGER); // raw chicken's tummy ache too
            }
        });
        // #119 Weather Nose: chat warns you 1 minute before rain
        always(E, 119, "weather_nose", CARRY, () -> Items.CLOCK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 20 != 0) return;
                var server = u.level().getServer();
                if (server.overworld().isRaining()) { u.state().putBoolean("warned", false); return; }
                ServerLevelData w = server.getWorldData().overworldData();
                int until = w.getClearWeatherTime() > 0 ? w.getClearWeatherTime() : w.getRainTime();
                if (until > 0 && until <= 1200 && !u.state().getBoolean("warned")) {
                    u.state().putBoolean("warned", true);
                    u.player().sendSystemMessage(Component.literal("You sniff the air... rain in about a minute.").withStyle(net.minecraft.ChatFormatting.AQUA));
                }
            }
        });
        // #120 Sticky Feet: you never slide on ice
        onPc(E, 120, "sticky_feet", () -> Items.SLIME_BALL);
        // #121 Ice Skater: you go faster on ice
        always(E, 121, "ice_skater", PASSIVE, () -> Items.PACKED_ICE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 5 == 0) boost(u.entity(), Attributes.MOVEMENT_SPEED, "ice_skater", isOn(u.entity(), BlockTags.ICE) ? 0.25 : 0);
            }
            @Override public void stop(PowerUser u) { boost(u.entity(), Attributes.MOVEMENT_SPEED, "ice_skater", 0); }
        });
        // #122 Ladder Monkey: climb ladders and vines 2x faster
        onPc(E, 122, "ladder_monkey", () -> Items.LADDER);
        // #123 Bare-Hand Fisher: punch a fish in the water to catch it
        always(E, 123, "bare_hand_fisher", CARRY, () -> Items.COD, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) {
                if (u.player() == null || !u.entity().getMainHandItem().isEmpty() || !target.isInWater()) return;
                Item fish = target instanceof Cod ? Items.COD : target instanceof Salmon ? Items.SALMON
                        : target instanceof TropicalFish ? Items.TROPICAL_FISH : target instanceof Pufferfish ? Items.PUFFERFISH : null;
                if (fish == null) return;
                target.discard();
                give(u, new ItemStack(fish));
                Fx.particles(u.level(), ParticleTypes.SPLASH, target.position(), 12, 0.3);
                Fx.sound(u.entity(), SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
            }
        });
        // #124 Rabbit Legs: sprint-jumps go further
        always(E, 124, "rabbit_legs", PASSIVE, () -> Items.RABBIT_HIDE, new PowerEffect() {
            @Override public void onJump(PowerUser u) {
                if (!u.entity().isSprinting()) return;
                Vec3 look = u.entity().getViewVector(1).multiply(1, 0, 1).normalize().scale(0.25);
                Vec3 v = u.entity().getDeltaMovement();
                launch(u.entity(), v.add(look.x, 0, look.z));
            }
        });
        // #125 Warm Blood: powder snow never freezes you
        always(E, 125, "warm_blood", PASSIVE, () -> Items.POWDER_SNOW_BUCKET, new PowerEffect() {
            @Override public void tick(PowerUser u) { if (u.entity().getTicksFrozen() > 0) u.entity().setTicksFrozen(0); }
        });
        // #126 XP Magnet: XP orbs come to you from twice as far
        always(E, 126, "xp_magnet", CARRY, () -> Items.EXPERIENCE_BOTTLE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 4 != 0) return;
                Vec3 me = u.entity().position().add(0, u.entity().getEyeHeight() / 2, 0);
                for (ExperienceOrb orb : u.level().getEntitiesOfClass(ExperienceOrb.class, u.entity().getBoundingBox().inflate(16))) {
                    Vec3 to = me.subtract(orb.position());
                    double d = to.length();
                    if (d > 7.5 && d < 16) orb.setDeltaMovement(orb.getDeltaMovement().add(to.normalize().scale(0.12)));
                }
            }
        });
        // #127 Wall Slide: slide down walls slowly instead of falling
        onPcSoftFall(E, 127, "wall_slide", () -> Items.SCAFFOLDING);
        // #128 Dolphin Buddy: dolphins always swim over to you
        always(E, 128, "dolphin_buddy", PASSIVE, () -> Items.DOLPHIN_SPAWN_EGG, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 40 != 0) return;
                for (Dolphin d : u.level().getEntitiesOfClass(Dolphin.class, u.entity().getBoundingBox().inflate(32))) {
                    if (d.distanceTo(u.entity()) > 4) d.getNavigation().moveTo(u.entity(), 1.2);
                }
            }
        });
        // #129 Saddle Sore: horses/donkeys/llamas you ride go 10% faster
        always(E, 129, "saddle_sore", PASSIVE, () -> Items.SADDLE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0) return;
                Entity v = u.entity().getVehicle();
                Entity old = u.state().hasUUID("horse") ? u.level().getEntity(u.state().getUUID("horse")) : null;
                if (old != null && old != v && old instanceof LivingEntity le) boost(le, Attributes.MOVEMENT_SPEED, "saddle_sore", 0);
                if (v instanceof AbstractHorse h) {
                    boost(h, Attributes.MOVEMENT_SPEED, "saddle_sore", 0.10);
                    u.state().putUUID("horse", h.getUUID());
                } else {
                    u.state().remove("horse");
                }
            }
            @Override public void stop(PowerUser u) {
                if (u.state().hasUUID("horse") && u.level().getEntity(u.state().getUUID("horse")) instanceof LivingEntity h) {
                    boost(h, Attributes.MOVEMENT_SPEED, "saddle_sore", 0);
                }
            }
        });
        // #130 Haybale Hugger: crouch next to a hay bale to heal half a heart every minute
        always(E, 130, "haybale_hugger", CARRY, () -> Items.HAY_BLOCK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                if (!u.entity().isCrouching() || !near(u.entity(), Blocks.HAY_BLOCK, 1)) { u.state().putInt("hug", 0); return; }
                int t = u.state().getInt("hug") + 1;
                u.state().putInt("hug", t);
                if (t % 60 == 0) {
                    u.entity().heal(1.0f);
                    Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(u.entity()), 1, 0.2);
                }
            }
        });
        // #131 Lucky Dip: once a day a random common item appears
        always(E, 131, "lucky_dip", CARRY, () -> Items.BUNDLE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 100 != 0 || !Fx.daily(u, "dip")) return;
                Item it = COMMON[u.entity().getRandom().nextInt(COMMON.length)];
                give(u, new ItemStack(it));
                tell(u, "Lucky dip: " + new ItemStack(it).getHoverName().getString() + "!");
            }
        });
        // #132 Fish Whisperer: a loud splash when a fish bites, so you never miss one (RankEvents)
        always(E, 132, "fish_whisperer", CARRY, () -> Items.FISHING_ROD, new PowerEffect() {});
        // #133 Moon Glow: night vision for 10 s, every 5 minutes (never flickers)
        key(E, 133, "moon_glow", CARRY, 6000, () -> Items.GLOW_INK_SAC, new PowerEffect() {
            @Override public void onKey(PowerUser u) { steadyNightVision(u, 200); Fx.sound(u.entity(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.6f); }
            @Override public void tick(PowerUser u) { nightVisionTick(u); }
            @Override public void stop(PowerUser u) { stopNightVision(u); }
        });
        // #134 Good Sleeper: waking up gives you 30 s of regen I
        always(E, 134, "good_sleeper", CARRY, () -> Items.WHITE_BED, new PowerEffect() {
            @Override public void onWake(PowerUser u) { effect(u.entity(), MobEffects.REGENERATION, 600, 0); }
        });
        // #135 Calm Animals: animals don't run away when you hit them
        always(E, 135, "calm_animals", PASSIVE, () -> Items.WHEAT, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) { if (target instanceof Animal) remember(u, "calm", target, u.now() + 10); }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "calm", e -> {
                    if (e instanceof Animal a) { a.setLastHurtByMob(null); a.getNavigation().stop(); }
                }, e -> {});
            }
        });
        // #136 Wool Grower: sheep you shear regrow wool straight away
        always(E, 136, "wool_grower", CARRY, () -> Items.SHEARS, new PowerEffect() {
            @Override public void onUseEntity(PowerUser u, PlayerInteractEvent.EntityInteract e) {
                if (e.getTarget() instanceof Sheep s && e.getItemStack().getItem() instanceof ShearsItem) remember(u, "sheep", s, u.now() + 5);
            }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "sheep", e -> {
                    if (e instanceof Sheep s && s.isSheared()) { s.setSheared(false); Fx.sound(s, SoundEvents.WOOL_PLACE, 1f, 1.2f); }
                }, e -> {});
            }
        });
        // #137 Egg Layer: chickens near you lay eggs twice as often
        always(E, 137, "egg_layer", PASSIVE, () -> Items.EGG, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                for (Chicken c : u.level().getEntitiesOfClass(Chicken.class, u.entity().getBoundingBox().inflate(16))) {
                    if (!c.isBaby() && c.eggTime > 20) c.eggTime -= 20; // a second of waiting skipped every second
                }
            }
        });
        // #138 Green Thumb: bonemeal one crop by touching it, once a minute
        key(E, 138, "green_thumb", CARRY, 1200, () -> Items.BONE_MEAL, u -> {
            var hit = lookBlock(u, 5);
            if (hit == null || u.player() == null) { refund(u); return; }
            BlockPos pos = hit.getBlockPos();
            BlockState s = u.level().getBlockState(pos);
            boolean spreader = s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MOSS_BLOCK) || s.is(BlockTags.NYLIUM);
            if (spreader || !(s.getBlock() instanceof BonemealableBlock)
                    || !BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), u.level(), pos, u.player())) {
                tell(u, "Touch a crop or sapling that can still grow.");
                refund(u);
                return;
            }
            u.level().levelEvent(1505, pos, 0); // the green bonemeal sparkles
        });
        // #139 Stubborn: 5% chance a hit just doesn't push you back
        always(E, 139, "stubborn", PASSIVE, () -> Items.COBBLESTONE_WALL, new PowerEffect() {
            @Override public void onKnockback(PowerUser u, LivingKnockBackEvent e) { if (u.entity().getRandom().nextInt(20) == 0) e.setCanceled(true); }
        });
        // #140 Glow Up: you glow for 10 s (everyone can see you through walls)
        key(E, 140, "glow_up", RANDOM, 400, () -> Items.GLOWSTONE_DUST, u -> {
            u.entity().addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
            Fx.sound(u.entity(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
        });
        // #141 Moonwalk: walking backwards is as fast as forwards (you can sprint backwards)
        onPc(E, 141, "moonwalk", () -> Items.BLACK_CARPET);
        // #142 Seed Finder: grass drops seeds twice as often
        always(E, 142, "seed_finder", CARRY, () -> Items.WHEAT_SEEDS, breaking(s -> s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN),
                8, u -> new ItemStack(Items.WHEAT_SEEDS)));
        // #143 Apple Luck: leaves drop apples twice as often
        always(E, 143, "apple_luck", CARRY, () -> Items.APPLE, breaking(s -> s.is(Blocks.OAK_LEAVES) || s.is(Blocks.DARK_OAK_LEAVES),
                200, u -> new ItemStack(Items.APPLE)));
        // #144 Sapling Luck: leaves drop saplings more often
        always(E, 144, "sapling_luck", CARRY, () -> Items.OAK_SAPLING, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                Item sapling = SAPLINGS.get(e.getState().getBlock());
                if (sapling != null && u.entity().getRandom().nextInt(20) == 0) Block.popResource(u.level(), e.getPos(), new ItemStack(sapling));
            }
        });
        // #145 Bone Collector: skeletons drop 1 extra bone
        always(E, 145, "bone_collector", PASSIVE, () -> Items.BONE, new PowerEffect() {
            @Override public void onDrops(PowerUser u, LivingDropsEvent e) {
                if (e.getEntity() instanceof AbstractSkeleton s) e.getDrops().add(new ItemEntity(u.level(), s.getX(), s.getY(), s.getZ(), new ItemStack(Items.BONE)));
            }
        });
        // #146 Forager: berries and mushrooms you pick give 2
        always(E, 146, "forager", CARRY, () -> Items.SWEET_BERRIES, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                BlockState s = e.getState();
                if (s.is(Blocks.RED_MUSHROOM) || s.is(Blocks.BROWN_MUSHROOM)) Block.popResource(u.level(), e.getPos(), new ItemStack(s.getBlock().asItem()));
            }
            @Override public void onUseBlock(PowerUser u, PlayerInteractEvent.RightClickBlock e) {
                if (e.getHand() != InteractionHand.MAIN_HAND) return;
                BlockState s = u.level().getBlockState(e.getPos());
                if (s.is(Blocks.SWEET_BERRY_BUSH) && s.getValue(SweetBerryBushBlock.AGE) >= 2) Block.popResource(u.level(), e.getPos(), new ItemStack(Items.SWEET_BERRIES));
                else if (s.getBlock() instanceof CaveVines && CaveVines.hasGlowBerries(s)) Block.popResource(u.level(), e.getPos(), new ItemStack(Items.GLOW_BERRIES));
            }
        });
        // #147 Silent Steps: sculk sensors don't hear you walking (RankEvents)
        always(E, 147, "silent_steps", PASSIVE, () -> Items.SCULK_SENSOR, new PowerEffect() {});
        // #148 Quick Shears: shears work twice as fast (RankEvents, also on your PC)
        always(E, 148, "quick_shears", CARRY, () -> Items.SHEARS, new PowerEffect() {});
        // #149 Bread Lover: bread fills you twice as much
        always(E, 149, "bread_lover", CARRY, () -> Items.BREAD, eating(s -> s.is(Items.BREAD)));
        // #150 Milk Mustache: milk also gives 5 s of regen
        always(E, 150, "milk_mustache", CARRY, () -> Items.MILK_BUCKET, new PowerEffect() {
            @Override public void onAte(PowerUser u, ItemStack food) { if (food.getItem() instanceof MilkBucketItem) effect(u.entity(), MobEffects.REGENERATION, 100, 0); }
        });
        // #151 Boomerang Arrows: 1 in 10 arrows you shoot comes back to your inventory after it hits (RankEvents)
        always(E, 151, "boomerang_arrows", PASSIVE, () -> Items.ARROW, new PowerEffect() {
            @Override public void onShoot(PowerUser u, Projectile p) {
                if (p instanceof AbstractArrow a && u.player() != null && u.entity().getRandom().nextInt(10) == 0) Arrows.track(a, Arrows.BOOMERANG);
            }
        });
        // #152 Frog Legs: jump higher out of water (easy to climb out)
        onPc(E, 152, "frog_legs", () -> Items.LILY_PAD);
        // #153 Light Meter: shows the light level where you stand (is it safe from mob spawns?)
        onPc(E, 153, "light_meter", () -> Items.TORCH);
        // #154 Horse Whisperer: see a horse's speed and jump when you look at it
        onPc(E, 154, "horse_whisperer", () -> Items.GOLDEN_APPLE);
        // #155 Pathfinder: walk 10% faster on paths and gravel
        always(E, 155, "pathfinder", PASSIVE, () -> Items.DIRT_PATH, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 5 != 0) return;
                BlockState below = u.level().getBlockState(BlockPos.containing(u.entity().position().add(0, -0.2, 0)));
                boolean path = below.is(Blocks.DIRT_PATH) || below.is(Blocks.GRAVEL);
                boost(u.entity(), Attributes.MOVEMENT_SPEED, "pathfinder", path ? 0.10 : 0);
            }
            @Override public void stop(PowerUser u) { boost(u.entity(), Attributes.MOVEMENT_SPEED, "pathfinder", 0); }
        });
        // #156 Pumpkin Vision: wearing a carved pumpkin doesn't block your view
        onPc(E, 156, "pumpkin_vision", () -> Items.CARVED_PUMPKIN);
        // #157 Chef's Kiss: food you cook in a smoker fills 1 more (RankEvents marks smoker food)
        always(E, 157, "chefs_kiss", CARRY, () -> Items.SMOKER, new PowerEffect() {
            @Override public void onAte(PowerUser u, ItemStack food) {
                if (u.player() != null && food.hasTag() && food.getTag().getBoolean(RankEvents.SMOKED)) u.player().getFoodData().eat(2, 0.5f);
            }
        });
        // #158 Soup Slurper: soups and stews fill you twice as much
        always(E, 158, "soup_slurper", CARRY, () -> Items.MUSHROOM_STEW, eating(s -> s.is(Items.MUSHROOM_STEW) || s.is(Items.RABBIT_STEW)
                || s.is(Items.BEETROOT_SOUP) || s.is(Items.SUSPICIOUS_STEW)));
        // #159 Campfire Cuddle: standing by a campfire heals half a heart a minute
        always(E, 159, "campfire_cuddle", PASSIVE, () -> Items.CAMPFIRE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                boolean warm = BlockPos.betweenClosedStream(u.entity().getBoundingBox().inflate(3))
                        .anyMatch(p -> CampfireBlock.isLitCampfire(u.level().getBlockState(p)));
                int t = warm ? u.state().getInt("warm") + 1 : 0;
                u.state().putInt("warm", t);
                if (warm && t % 60 == 0) {
                    u.entity().heal(1.0f);
                    Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(u.entity()), 1, 0.2);
                }
            }
        });
        // #160 Pocket Sand: throw sand in a mob's face; it misses its next attack. 30 s cooldown.
        key(E, 160, "pocket_sand", ATTACK, 600, () -> Items.SAND, u -> {
            LivingEntity t = target(u, 4);
            if (t == null) { refund(u); return; }
            Fx.particles(u.level(), new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), t.getEyePosition(), 25, 0.3);
            Fx.sound(t, SoundEvents.SAND_BREAK, 1.0f, 1.0f);
            RankEvents.missNext(t, u.now() + 400);
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, true));
        });
        // #161 Tree Hugger: crouch next to a tree for 10 s to get an apple, once a day
        always(E, 161, "tree_hugger", CARRY, () -> Items.OAK_LOG, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                boolean hugging = u.entity().isCrouching() && nextToLog(u.entity());
                int t = hugging ? u.state().getInt("hug") + 1 : 0;
                u.state().putInt("hug", t);
                if (t >= 10 && Fx.daily(u, "apple")) {
                    give(u, new ItemStack(Items.APPLE));
                    Fx.particles(u.level(), ParticleTypes.HAPPY_VILLAGER, Fx.head(u.entity()), 6, 0.4);
                    tell(u, "The tree hugs you back. Have an apple.");
                }
            }
        });
        // #162 Copper Polish: right-click copper with an empty hand to scrape it clean like an axe
        always(E, 162, "copper_polish", CARRY, () -> Items.COPPER_BLOCK, new PowerEffect() {
            @Override public void onUseBlock(PowerUser u, PlayerInteractEvent.RightClickBlock e) {
                if (e.getHand() != InteractionHand.MAIN_HAND || !e.getItemStack().isEmpty() || u.player() == null) return;
                BlockPos pos = e.getPos();
                BlockState s = u.level().getBlockState(pos);
                var older = WeatheringCopper.getPrevious(s);
                BlockState now = null;
                int event = 3005;
                if (older.isPresent()) {
                    now = older.get();
                    Fx.sound(u.entity(), SoundEvents.AXE_SCRAPE, 1.0f, 1.0f);
                } else {
                    Block plain = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(s.getBlock());
                    if (plain != null) {
                        now = plain.withPropertiesOf(s);
                        event = 3004;
                        Fx.sound(u.entity(), SoundEvents.AXE_WAX_OFF, 1.0f, 1.0f);
                    }
                }
                if (now == null || !u.player().mayUseItemAt(pos, e.getFace(), ItemStack.EMPTY)) return;
                u.level().setBlock(pos, now, Block.UPDATE_ALL_IMMEDIATE);
                u.level().levelEvent(null, event, pos, 0);
                u.player().swing(InteractionHand.MAIN_HAND, true);
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        // #163 Ink Squirt: squirt ink at the mob in front of you; it's blind for 2 s. 30 s cooldown.
        key(E, 163, "ink_squirt", ATTACK, 600, () -> Items.INK_SAC, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity t = target(u, 5);
                if (t == null) { refund(u); return; }
                Fx.particles(u.level(), ParticleTypes.SQUID_INK, t.getEyePosition(), 20, 0.25);
                Fx.sound(u.entity(), SoundEvents.SQUID_SQUIRT, 1.0f, 1.0f);
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true, true));
                if (t instanceof net.minecraft.world.entity.Mob) remember(u, "inked", t, u.now() + 40); // mobs ignore blindness: make it lose you
            }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "inked", e -> { if (e instanceof net.minecraft.world.entity.Mob m) m.setTarget(null); }, e -> {});
            }
        });
        // #164 Lucky Clover: 1 in 50 grass you break drops a random dye
        always(E, 164, "lucky_clover", CARRY, () -> Items.GREEN_DYE, breaking(s -> s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS),
                50, u -> new ItemStack(DyeItem.byColor(DyeColor.values()[u.entity().getRandom().nextInt(DyeColor.values().length)]))));
        // #165 Squid Kid: swim faster in dark water
        always(E, 165, "squid_kid", PASSIVE, () -> Items.INK_SAC, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0) return;
                LivingEntity e = u.entity();
                boolean dark = e.isInWater() && e.level().getMaxLocalRawBrightness(e.blockPosition()) < 8;
                boost(e, ForgeMod.SWIM_SPEED.get(), "squid_kid", dark ? 0.5 : 0);
            }
            @Override public void stop(PowerUser u) { boost(u.entity(), ForgeMod.SWIM_SPEED.get(), "squid_kid", 0); }
        });
        // #166 Thick Skin: cactus and berry bushes don't hurt you
        always(E, 166, "thick_skin", PASSIVE, () -> Items.CACTUS, new PowerEffect() {
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) {
                if (e.getSource().is(DamageTypes.CACTUS) || e.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) e.setCanceled(true);
            }
        });
        // #167 Ember Feet: magma blocks don't hurt you
        always(E, 167, "ember_feet", PASSIVE, () -> Items.MAGMA_BLOCK, new PowerEffect() {
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) { if (e.getSource().is(DamageTypes.HOT_FLOOR)) e.setCanceled(true); }
        });
        // #168 Clean Paws / #169 Honey Paws: mud, soul sand and honey don't slow you down
        onPc(E, 168, "clean_paws", () -> Items.SOUL_SAND);
        onPc(E, 169, "honey_paws", () -> Items.HONEY_BLOCK);
        // #170 Wind Reader: arrows you shoot fly 10% further
        always(E, 170, "wind_reader", PASSIVE, () -> Items.BOW, new PowerEffect() {
            @Override public void onShoot(PowerUser u, Projectile p) {
                if (p instanceof AbstractArrow) p.setDeltaMovement(p.getDeltaMovement().scale(1.05)); // ~10% more range
            }
        });
        // #171 Silkworm: cobwebs drop string without shears
        always(E, 171, "silkworm", CARRY, () -> Items.COBWEB, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                ItemStack tool = u.entity().getMainHandItem();
                boolean dropsAnyway = tool.getItem() instanceof ShearsItem || tool.getItem() instanceof SwordItem;
                if (e.getState().is(Blocks.COBWEB) && !dropsAnyway) Block.popResource(u.level(), e.getPos(), new ItemStack(Items.STRING));
            }
        });
        // #172 Potion Lover: good potions last 10% longer on you
        always(E, 172, "potion_lover", PASSIVE, () -> Items.POTION, new PowerEffect() {
            @Override public void onEffectAdded(PowerUser u, MobEffectEvent.Added e) {
                MobEffectInstance i = e.getEffectInstance();
                if (!i.getEffect().isBeneficial() || !i.isVisible() || i.isInfiniteDuration() || i.getDuration() < 40) return;
                i.update(new MobEffectInstance(i.getEffect(), (int) (i.getDuration() * 1.1), i.getAmplifier(), i.isAmbient(), i.isVisible(), i.showIcon()));
            }
        });
        // #173 Lantern Carrier: holding a lantern makes zombies keep 3 blocks away
        always(E, 173, "lantern_carrier", PASSIVE, () -> Items.LANTERN, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 2 != 0) return;
                LivingEntity e = u.entity();
                if (!holding(e, Items.LANTERN) && !holding(e, Items.SOUL_LANTERN)) return;
                for (Zombie z : u.level().getEntitiesOfClass(Zombie.class, e.getBoundingBox().inflate(3), z -> z != e && z.distanceTo(e) < 3)) {
                    Fx.push(z, e.position(), 0.25);
                }
            }
        });
        // #174 Snow Angel: lie in snow (crouch 3 s) to heal 1 heart, once every 5 minutes
        always(E, 174, "snow_angel", CARRY, () -> Items.SNOW, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                LivingEntity e = u.entity();
                BlockState at = u.level().getBlockState(e.blockPosition());
                BlockState below = u.level().getBlockState(e.blockPosition().below());
                boolean snow = at.is(Blocks.SNOW) || at.is(Blocks.POWDER_SNOW) || below.is(Blocks.SNOW_BLOCK) || below.is(Blocks.SNOW);
                int t = snow && e.isCrouching() ? u.state().getInt("angel") + 1 : 0;
                u.state().putInt("angel", t);
                if (t >= 3 && !running(u, "angelCd")) {
                    e.heal(2.0f);
                    startFor(u, "angelCd", 6000);
                    Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, e.position().add(0, 0.3, 0), 30, 0.8);
                    tell(u, "Snow angel!");
                }
            }
        });
        // #175 Pet Medic: right-click your tamed pet with an empty hand to heal it 1 heart, once a minute
        always(E, 175, "pet_medic", CARRY, () -> Items.GLISTERING_MELON_SLICE, new PowerEffect() {
            @Override public void onUseEntity(PowerUser u, PlayerInteractEvent.EntityInteract e) {
                if (e.getHand() != InteractionHand.MAIN_HAND || !e.getItemStack().isEmpty() || running(u, "medicCd")) return;
                if (!(e.getTarget() instanceof TamableAnimal pet) || !u.entity().getUUID().equals(pet.getOwnerUUID())) return;
                if (pet.getHealth() >= pet.getMaxHealth()) return;
                pet.heal(2.0f);
                startFor(u, "medicCd", 1200);
                Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(pet), 3, 0.3);
            }
        });
    }

    // --- building blocks ---

    static final Item[] COMMON = {Items.STICK, Items.STRING, Items.BONE, Items.FLINT, Items.FEATHER, Items.LEATHER, Items.PAPER,
            Items.WHEAT_SEEDS, Items.APPLE, Items.BREAD, Items.COAL, Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.ARROW, Items.TORCH,
            Items.SLIME_BALL, Items.SUGAR, Items.EGG, Items.CLAY_BALL, Items.BOWL};

    static final Map<Item, Item> COOKED = Map.of(Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP,
            Items.CHICKEN, Items.COOKED_CHICKEN, Items.MUTTON, Items.COOKED_MUTTON, Items.RABBIT, Items.COOKED_RABBIT,
            Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.POTATO, Items.BAKED_POTATO);

    static final Map<Block, Item> SAPLINGS = Map.of(Blocks.OAK_LEAVES, Items.OAK_SAPLING, Blocks.SPRUCE_LEAVES, Items.SPRUCE_SAPLING,
            Blocks.BIRCH_LEAVES, Items.BIRCH_SAPLING, Blocks.JUNGLE_LEAVES, Items.JUNGLE_SAPLING, Blocks.ACACIA_LEAVES, Items.ACACIA_SAPLING,
            Blocks.DARK_OAK_LEAVES, Items.DARK_OAK_SAPLING, Blocks.CHERRY_LEAVES, Items.CHERRY_SAPLING, Blocks.MANGROVE_LEAVES, Items.MANGROVE_PROPAGULE,
            Blocks.AZALEA_LEAVES, Items.AZALEA, Blocks.FLOWERING_AZALEA_LEAVES, Items.FLOWERING_AZALEA);

    // Eat the same food's filling once more (Breakfast Bonus, Bread Lover, Soup Slurper).
    static void eatAgain(PowerUser u, ItemStack food, float share) {
        if (u.player() == null) return;
        FoodProperties f = food.getFoodProperties(u.entity());
        if (f != null) u.player().getFoodData().eat(Math.max(1, Math.round(f.getNutrition() * share)), f.getSaturationModifier());
    }

    static PowerEffect eating(java.util.function.Predicate<ItemStack> which) {
        return new PowerEffect() {
            @Override public void onAte(PowerUser u, ItemStack food) { if (which.test(food)) eatAgain(u, food, 1.0f); }
        };
    }

    // Breaking a matching block: 1 in `oneIn` times an extra item pops out.
    static PowerEffect breaking(java.util.function.Predicate<BlockState> which, int oneIn, java.util.function.Function<PowerUser, ItemStack> drop) {
        return new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                if (which.test(e.getState()) && u.entity().getRandom().nextInt(oneIn) == 0) Block.popResource(u.level(), e.getPos(), drop.apply(u));
            }
        };
    }

    static boolean isOn(LivingEntity e, net.minecraft.tags.TagKey<Block> tag) {
        BlockPos feet = BlockPos.containing(e.position().add(0, 0.01, 0));
        return e.level().getBlockState(feet).is(tag) || e.level().getBlockState(BlockPos.containing(e.position().add(0, -0.2, 0))).is(tag);
    }

    static boolean near(LivingEntity e, Block b, int r) {
        return BlockPos.betweenClosedStream(e.getBoundingBox().inflate(r)).anyMatch(p -> e.level().getBlockState(p).is(b));
    }

    static boolean nextToLog(LivingEntity e) {
        BlockPos p = e.blockPosition();
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (e.level().getBlockState(p.relative(d)).is(BlockTags.LOGS) || e.level().getBlockState(p.relative(d).above()).is(BlockTags.LOGS)) return true;
        }
        return false;
    }

    static boolean holding(LivingEntity e, Item item) { return e.getMainHandItem().is(item) || e.getOffhandItem().is(item); }

    // Throws a projectile the way the holder faces (players) or at its target (mobs).
    static void aim(PowerUser u, Projectile p, float speed) {
        LivingEntity e = u.entity();
        p.setPos(e.getX(), e.getEyeY() - 0.1, e.getZ());
        if (u.mob() != null && u.mob().getTarget() != null) {
            LivingEntity t = u.mob().getTarget();
            Vec3 d = t.getEyePosition().subtract(p.position());
            p.shoot(d.x, d.y + d.horizontalDistance() * 0.15, d.z, speed, 2f);
        } else {
            p.shootFromRotation(e, e.getXRot(), e.getYRot(), 0, speed, 1f);
        }
    }

    private EPowers() {}
}
