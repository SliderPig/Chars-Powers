package com.example.charpowers.hidden.f;

import com.example.charpowers.hidden.ModSounds;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.PASSIVE;

// The F-rank always-on powers about your body, sounds and little particles. They work on mobs too.
final class FBodyPowers {
    static void registerAll() {
        // #6 Hiccups: every few minutes you hop a little on your own
        always(6, "hiccups", PASSIVE, () -> Items.SUGAR, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                long next = u.state().getLong("hic");
                if (next == 0 || u.now() >= next) {
                    if (next != 0) {
                        Fx.sound(u.entity(), ModSounds.HICCUP, 1.0f, 1.0f);
                        if (u.entity().onGround()) { u.entity().push(0, 0.25, 0); u.entity().hurtMarked = true; }
                    }
                    u.state().putLong("hic", u.now() + 2400 + u.entity().getRandom().nextInt(2401));
                }
            }
        });
        // #7 Rainbow Trail: soft colours at your feet while you move (one colour step a second)
        always(7, "rainbow_trail", PASSIVE, () -> Items.PINK_DYE, new PowerEffect() {
            // (user: the first version was too small to notice: bigger puffs, every tick, a little behind you)
            @Override public void tick(PowerUser u) {
                LivingEntity e = u.entity();
                float walked = e.walkDist - u.state().getFloat("walk");
                u.state().putFloat("walk", e.walkDist);
                Vec3 d = moved(u);
                if (walked < 0.01f && d.lengthSqr() < 0.0004) return;
                Vec3 back = d.lengthSqr() > 1.0E-6 ? d.normalize().scale(-0.4) : Vec3.ZERO;
                Fx.particles(u.level(), slowColour(u.now(), 1.8f), e.position().add(back.x, 0.15, back.z), 4, 0.12);
            }
        });
        // #8 Squeaky Steps
        always(8, "squeaky_steps", PASSIVE, () -> Items.LEATHER_BOOTS, steps((u) -> Fx.sound(u.entity(), ModSounds.SQUEAK, 0.6f, 1.0f + u.entity().getRandom().nextFloat() * 0.3f)));
        // #11 Stinky: little "flies" around you
        always(11, "stinky", PASSIVE, () -> Items.ROTTEN_FLESH, every(10, u -> Fx.particles(u.level(), ParticleTypes.ASH, u.entity().position().add(0, u.entity().getBbHeight() * 0.7, 0), 3, 0.5)));
        // #12 Sweaty: drips while sprinting
        always(12, "sweaty", PASSIVE, () -> Items.WATER_BUCKET, every(4, u -> {
            if (u.entity().isSprinting()) Fx.particles(u.level(), ParticleTypes.DRIPPING_WATER, Fx.head(u.entity()).add(0, -0.3, 0), 2, 0.2);
        }));
        // #18 Personal Snow: a little snow only on you
        always(18, "personal_snow", PASSIVE, () -> Items.SNOWBALL, every(5, u -> Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, Fx.head(u.entity()).add(0, 1.2, 0), 3, 0.4)));
        // #20 Cold Feet: snowflakes puff out where you step
        always(20, "cold_feet", PASSIVE, () -> Items.POWDER_SNOW_BUCKET, steps(u -> Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, u.entity().position().add(0, 0.05, 0), 4, 0.2)));
        // #33 Jelly Legs: after a 3+ block fall you walk slowly for 2 s
        always(33, "jelly_legs", PASSIVE, () -> Items.SLIME_BLOCK, new PowerEffect() {
            @Override public void onFall(PowerUser u, float distance) {
                if (distance >= 3) u.entity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
            }
        });
        // #37 Feather Shed: drop a feather every 10 minutes
        always(37, "feather_shed", PASSIVE, () -> Items.FEATHER, new PowerEffect() {
            @Override public void tick(PowerUser u) { if (u.every("feather", 12000)) u.entity().spawnAtLocation(new ItemStack(Items.FEATHER)); }
        });
        // #39 Mood Clouds: a tiny rain cloud over you when you're under 3 hearts
        always(39, "mood_clouds", PASSIVE, () -> Items.GRAY_WOOL, every(10, u -> {
            if (u.entity().getHealth() >= 6.0f) return;
            Vec3 top = Fx.head(u.entity()).add(0, 0.7, 0);
            Fx.particles(u.level(), ParticleTypes.CLOUD, top, 3, 0.25);
            Fx.particles(u.level(), ParticleTypes.FALLING_WATER, top, 2, 0.2);
        }));
        // #48 Sporey: mushroom spores drift off you
        always(48, "sporey", PASSIVE, () -> Items.RED_MUSHROOM, every(10, u -> Fx.particles(u.level(), ParticleTypes.SPORE_BLOSSOM_AIR, u.entity().position().add(0, u.entity().getBbHeight() / 2, 0), 3, 0.6)));
        // #49 Snore: loud snoring while sleeping (villagers sleep too)
        always(49, "snore", PASSIVE, () -> Items.RED_BED, every(60, u -> { if (u.entity().isSleeping()) Fx.sound(u.entity(), ModSounds.SNORE, 1.0f, 1.0f); }));
        // #52 Yawn: a yawn every 10 minutes at night
        always(52, "yawn", PASSIVE, () -> Items.CLOCK, new PowerEffect() {
            @Override public void tick(PowerUser u) { if (u.level().isNight() && u.every("yawn", 12000)) Fx.sound(u.entity(), ModSounds.YAWN, 1.0f, 1.0f); }
        });
        // #53 Drama Queen: a big gasp every time you get hurt
        always(53, "drama_queen", PASSIVE, () -> Items.GOLDEN_HELMET, new PowerEffect() {
            @Override public void onHurt(PowerUser u, LivingHurtEvent e) { Fx.sound(u.entity(), ModSounds.GASP, 1.2f, 1.0f); }
        });
        // #57 Static Hair: little sparks on your head when it rains
        always(57, "static_hair", PASSIVE, () -> Items.LIGHTNING_ROD, every(10, u -> {
            if (u.level().isRainingAt(BlockPos.containing(Fx.head(u.entity())))) Fx.particles(u.level(), ParticleTypes.ELECTRIC_SPARK, Fx.head(u.entity()), 3, 0.2);
        }));
        // #59 Duck Walk: a quack with every step while crouching
        always(59, "duck_walk", PASSIVE, () -> Items.CHICKEN_SPAWN_EGG, steps(u -> { if (u.entity().isCrouching()) Fx.sound(u.entity(), ModSounds.QUACK, 0.8f, 1.0f); }));
        // #71 Static Shock: bumping into a player makes a little zap
        always(71, "static_shock", PASSIVE, () -> Items.COPPER_INGOT, every(2, u -> {
            if (running(u, "zapped")) return;
            for (Player p : u.level().getEntitiesOfClass(Player.class, u.entity().getBoundingBox().inflate(0.1), p -> p != u.entity())) {
                Fx.sound(u.entity(), ModSounds.ZAP, 0.8f, 1.2f);
                Fx.particles(u.level(), ParticleTypes.ELECTRIC_SPARK, p.position().add(0, 1, 0), 4, 0.2);
                startFor(u, "zapped", 40);
                break;
            }
        }));
        // #72 Chipmunk / #73 Big Voice: every sound you make is high / deep
        always(72, "chipmunk", PASSIVE, () -> Items.HONEY_BOTTLE, pitch(1.6f));
        always(73, "big_voice", PASSIVE, () -> Items.GOAT_HORN, pitch(0.6f));
        // #74 Sad Trombone: once when you drop under 2 hearts
        always(74, "sad_trombone", PASSIVE, () -> Items.BRUSH, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                boolean low = u.entity().getHealth() < 4.0f && u.entity().isAlive();
                if (low && !u.state().getBoolean("played")) Fx.sound(u.entity(), ModSounds.SAD_TROMBONE, 1.2f, 1.0f);
                u.state().putBoolean("played", low);
            }
        });
        // #75 Boing: every jump goes "boing"
        always(75, "boing", PASSIVE, () -> Items.SLIME_BALL, new PowerEffect() {
            @Override public void onJump(PowerUser u) { Fx.sound(u.entity(), ModSounds.BOING, 0.8f, 1.0f); }
        });
        // #76 Party Kill: mobs you kill pop like party poppers
        always(76, "party_kill", PASSIVE, () -> Items.CAKE, new PowerEffect() {
            @Override public void onKill(PowerUser u, LivingEntity victim) { FKeyPowers.confetti(u, victim.position().add(0, victim.getBbHeight() / 2, 0), 25); }
        });
        // #82 Cricket Choir: crickets chirp around you at night, outdoors
        always(82, "cricket_choir", PASSIVE, () -> Items.TALL_GRASS, randomSound("cricket", 200, 400, u ->
                u.level().isNight() && u.level().canSeeSky(BlockPos.containing(Fx.head(u.entity()))), ModSounds.CRICKETS::get));
        // #83 Seagull: near the ocean, seagulls follow you
        always(83, "seagull", PASSIVE, () -> Items.FEATHER, randomSound("gull", 300, 600, u -> {
            var b = u.level().getBiome(u.entity().blockPosition());
            return b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_BEACH);
        }, ModSounds.SEAGULL::get));
        // #88 Crab Walk: walking sideways makes crab clicks
        always(88, "crab_walk", PASSIVE, () -> Items.TROPICAL_FISH, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                Vec3 d = moved(u);
                if (d.lengthSqr() < 0.004 || u.now() % 6 != 0) return;
                Vec3 look = u.entity().getViewVector(1).multiply(1, 0, 1).normalize();
                double side = Math.abs(d.normalize().dot(look));
                if (side < 0.45) Fx.sound(u.entity(), ModSounds.CRAB_CLICK, 0.6f, 1.0f + u.entity().getRandom().nextFloat() * 0.4f);
            }
        });
        // #89 Sock Slide: you slide a little further when you stop on wooden floors
        always(89, "sock_slide", PASSIVE, () -> Items.WHITE_CARPET, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                Vec3 d = moved(u);
                double last = u.state().getDouble("speed");
                u.state().putDouble("speed", d.length());
                if (last > 0.12 && d.length() < 0.03 && onWood(u.entity()) && u.entity().onGround()) {
                    Vec3 dir = new Vec3(u.state().getDouble("dx"), 0, u.state().getDouble("dz")).normalize().scale(last * 1.6);
                    u.entity().push(dir.x, 0, dir.z);
                    u.entity().hurtMarked = true;
                }
                if (d.length() > 0.03) { u.state().putDouble("dx", d.x); u.state().putDouble("dz", d.z); }
            }
        });
        // #91 Ghost Kitty: a cat follows you around. It does nothing.
        always(91, "ghost_kitty", PASSIVE, () -> Items.STRING, companion("kitty", EntityType.CAT, false));
        // #95 Splashy: little splashes under your feet when it rains
        always(95, "splashy", PASSIVE, () -> Items.LILY_PAD, steps(u -> {
            if (u.level().isRainingAt(u.entity().blockPosition())) Fx.particles(u.level(), ParticleTypes.SPLASH, u.entity().position().add(0, 0.1, 0), 8, 0.3);
        }));
        // #14 Dramatic Death: thunder (no lightning) and sad notes when you die
        always(14, "dramatic_death", PASSIVE, () -> Items.WITHER_ROSE, new PowerEffect() {
            @Override public void onDeath(PowerUser u) {
                Fx.sound(u.entity(), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
                for (int i = 0; i < 3; i++) Fx.sound(u.entity(), SoundEvents.NOTE_BLOCK_HARP.value(), 1.0f, 0.9f - i * 0.15f);
            }
        });
        // #21 Grumpy Cats: every cat near you hisses at you
        always(21, "grumpy_cats", PASSIVE, () -> Items.COD, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (!u.every("hiss", 600)) return;
                for (Cat c : u.level().getEntitiesOfClass(Cat.class, u.entity().getBoundingBox().inflate(8))) {
                    c.getLookControl().setLookAt(u.entity());
                    Fx.sound(c, SoundEvents.CAT_HISS, 1.0f, 1.0f);
                }
            }
        });
        // #22 Pig Magnet: pigs follow you around
        always(22, "pig_magnet", PASSIVE, () -> Items.CARROT_ON_A_STICK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 40 != 0) return;
                for (Pig p : u.level().getEntitiesOfClass(Pig.class, u.entity().getBoundingBox().inflate(12))) {
                    if (p.distanceTo(u.entity()) > 3) p.getNavigation().moveTo(u.entity(), 1.0);
                }
            }
        });
        // #62 Bat Buddy: at night a bat flutters by your shoulder
        always(62, "bat_buddy", PASSIVE, () -> Items.BAT_SPAWN_EGG, companion("bat", EntityType.BAT, true));
    }

    // --- building blocks ---

    static PowerEffect every(int ticks, java.util.function.Consumer<PowerUser> run) {
        return new PowerEffect() {
            @Override public void tick(PowerUser u) { if ((u.now() + u.entity().getId()) % ticks == 0) run.accept(u); }
        };
    }

    static PowerEffect steps(java.util.function.Consumer<PowerUser> run) {
        return new PowerEffect() {
            @Override public void tick(PowerUser u) { if (stepped(u)) run.accept(u); }
        };
    }

    static PowerEffect pitch(float factor) {
        return new PowerEffect() {
            @Override public void onSound(PowerUser u, PlayLevelSoundEvent e) { e.setNewPitch(e.getNewPitch() * factor); }
        };
    }

    static PowerEffect randomSound(String key, int min, int max, java.util.function.Predicate<PowerUser> when,
                                   java.util.function.Supplier<net.minecraft.sounds.SoundEvent> sound) {
        return new PowerEffect() {
            @Override public void tick(PowerUser u) {
                long next = u.state().getLong(key);
                if (next != 0 && u.now() < next) return;
                if (next != 0 && when.test(u)) Fx.sound(u.entity(), sound.get(), 0.8f, 1.0f);
                u.state().putLong(key, u.now() + min + u.entity().getRandom().nextInt(max - min + 1));
            }
        };
    }

    static boolean onWood(Entity e) {
        BlockState below = e.level().getBlockState(e.blockPosition().below());
        BlockState at = e.level().getBlockState(e.blockPosition());
        return below.is(BlockTags.PLANKS) || below.is(BlockTags.WOODEN_SLABS) || below.is(BlockTags.WOODEN_STAIRS)
                || at.is(BlockTags.WOODEN_SLABS) || at.is(BlockTags.WOODEN_STAIRS);
    }

    // A harmless animal that sticks with the holder. It can't be hurt, makes no sound and is never saved.
    // nightOnly = only around at night (Bat Buddy).
    static PowerEffect companion(String key, EntityType<? extends Mob> type, boolean nightOnly) {
        return new PowerEffect() {
            @Override public void tick(PowerUser u) {
                boolean want = u.entity().isAlive() && (!nightOnly || u.level().isNight());
                Entity c = find(u, key);
                if (!want) { if (c != null) forget(u, key); return; }
                if (c == null || c.level() != u.level()) {
                    Mob m = type.create(u.level());
                    if (m == null) return;
                    m.moveTo(u.entity().getX(), u.entity().getY(), u.entity().getZ());
                    m.setNoAi(true);
                    m.setInvulnerable(true);
                    m.setSilent(true);
                    m.setPersistenceRequired();
                    markTemp(m);
                    if (m instanceof Cat cat) cat.setInSittingPose(false);
                    if (m instanceof net.minecraft.world.entity.ambient.Bat bat) { bat.setResting(false); bat.setNoGravity(true); }
                    u.level().addFreshEntity(m);
                    u.state().putUUID(key, m.getUUID());
                    c = m;
                }
                LivingEntity h = u.entity();
                Vec3 behind = h.position().subtract(h.getViewVector(1).multiply(1, 0, 1).normalize().scale(1.2));
                Vec3 spot = nightOnly ? h.position().add(0.6, h.getBbHeight() + 0.2 + Math.sin(u.now() / 6.0) * 0.15, 0.4) : behind;
                Vec3 target = new Vec3(spot.x, nightOnly ? spot.y : h.getY(), spot.z);
                double far = c.position().distanceToSqr(target);
                if (far > 100) {
                    c.moveTo(target.x, target.y, target.z, h.getYRot(), 0); // fell far behind (teleport, portal): jump there
                } else if (far > 0.0025) {
                    // glide a bit closer every tick (not a jump), so it flies/walks smoothly beside you
                    Vec3 next = c.position().lerp(target, 0.25);
                    Vec3 step = next.subtract(c.position());
                    c.setPos(next.x, next.y, next.z);
                    c.setDeltaMovement(step);
                    float yaw = step.horizontalDistanceSqr() > 1.0E-4 ? (float) (Math.atan2(-step.x, step.z) * 180 / Math.PI) : h.getYRot();
                    c.setYRot(yaw);
                    if (c instanceof LivingEntity le) { le.setYHeadRot(yaw); le.yBodyRot = yaw; }
                }
                if (c instanceof net.minecraft.world.entity.ambient.Bat bat) bat.setResting(false);
            }
            @Override public void onDeath(PowerUser u) { forget(u, key); }
            @Override public void stop(PowerUser u) { forget(u, key); }
        };
    }

    private FBodyPowers() {}
}
