package com.example.charpowers.hidden.f;

import com.example.charpowers.hidden.ModSounds;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.fx.TextLists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;

// The F-rank powers you use with a key (or a mob uses now and then). Numbers match my list.
final class FKeyPowers {
    static void registerAll() {
        // #1 Loud Meow
        key(1, "loud_meow", RANDOM, () -> Items.CAT_SPAWN_EGG, u -> Fx.sound(u.entity(), SoundEvents.CAT_AMBIENT, 2.0f, 1.0f));
        // #2 Moo
        key(2, "moo", RANDOM, () -> Items.COW_SPAWN_EGG, u -> Fx.sound(u.entity(), SoundEvents.COW_AMBIENT, 1.5f, 1.0f));
        // #3 Sneeze: pushes whoever is in front 1 block
        key(3, "sneeze", ATTACK, () -> Items.WHITE_DYE, u -> {
            Fx.sound(u.entity(), ModSounds.SNEEZE, 1.0f, 1.0f);
            Fx.particles(u.level(), ParticleTypes.SNEEZE, u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(0.6)), 8, 0.15);
            LivingEntity t = Fx.lookTarget(u.entity(), 3);
            if (t != null) Fx.push(t, u.entity().position(), 0.45);
        });
        // #4 Burp: costs half a hunger point (1 food point)
        key(4, "burp", RANDOM, () -> Items.BEETROOT_SOUP, u -> {
            Fx.sound(u.entity(), ModSounds.BURP, 1.2f, 0.8f);
            if (u.player() != null) u.player().getFoodData().setFoodLevel(Math.max(0, u.player().getFoodData().getFoodLevel() - 1));
        });
        // #5 Confused Chicken: a chicken that runs off and is gone after a minute
        key(5, "confused_chicken", RANDOM, 200, () -> Items.EGG, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                forget(u, "chicken");
                Chicken c = EntityType.CHICKEN.create(u.level());
                if (c == null) return;
                c.moveTo(u.entity().getX(), u.entity().getY(), u.entity().getZ(), u.entity().getYRot(), 0);
                markTemp(c);
                c.setPersistenceRequired();
                u.level().addFreshEntity(c);
                c.getNavigation().moveTo(c.getX() + c.getRandom().nextInt(16) - 8, c.getY(), c.getZ() + c.getRandom().nextInt(16) - 8, 1.4);
                u.state().putUUID("chicken", c.getUUID());
                startFor(u, "chickenUntil", 1200);
            }
            @Override public void tick(PowerUser u) { if (u.state().hasUUID("chicken") && !running(u, "chickenUntil")) forget(u, "chicken"); }
            @Override public void stop(PowerUser u) { forget(u, "chicken"); }
        });
        // #10 Pet Rock: once a day, a cobblestone named Rocky
        key(10, "pet_rock", CARRY, () -> Items.COBBLESTONE, u -> {
            if (Fx.daily(u, "rock")) Fx.giveOrDrop(u, named(Items.COBBLESTONE, "Rocky"));
            else tell(u, "Rocky is already here today.");
        });
        // #13 Flop: flop onto the ground for 3 seconds
        key(13, "flop", RANDOM, () -> Items.LIGHT_BLUE_CARPET, lieDown(60));
        // #15 Applause
        key(15, "applause", RANDOM, () -> Items.PAPER, new PowerEffect() {
            @Override public void onKey(PowerUser u) { u.state().putInt("claps", 6); }
            @Override public void tick(PowerUser u) {
                int left = u.state().getInt("claps");
                if (left > 0 && u.now() % 4 == 0) {
                    u.state().putInt("claps", left - 1);
                    Fx.sound(u.entity(), ModSounds.CLAPPING, 1.0f, 0.8f + u.entity().getRandom().nextFloat() * 0.4f);
                }
            }
        });
        // #16 Spin
        key(16, "spin", RANDOM, () -> Items.COMPASS, spinning(10, 36f));
        // #17 Bubble Blower
        key(17, "bubble_blower", RANDOM, () -> Items.GLASS_BOTTLE, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "bubbles", 40); }
            @Override public void tick(PowerUser u) {
                if (!running(u, "bubbles") || u.now() % 2 != 0) return;
                Vec3 front = u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(0.8));
                Fx.particles(u.level(), ParticleTypes.BUBBLE_POP, front, 3, 0.2);
                if (u.now() % 8 == 0) Fx.sound(u.entity(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.6f, 1.4f);
            }
        });
        // #19 Fish Slap: half a heart with a cod
        key(19, "fish_slap", ATTACK, () -> Items.COD, u -> {
            LivingEntity t = Fx.lookTarget(u.entity(), 3.5);
            if (t == null) return;
            Fx.sound(t, SoundEvents.COD_FLOP, 1.2f, 1.0f);
            Fx.particles(u.level(), ParticleTypes.SPLASH, t.getEyePosition(), 10, 0.3);
            Fx.tinyHit(t, u.entity(), 1.0f);
            u.entity().swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        });
        // #23 Thunder Clap: the sound only, no lightning (no flashing, ever)
        key(23, "thunder_clap", RANDOM, () -> Items.LIGHTNING_ROD, u -> Fx.sound(u.entity(), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f));
        // #24 Leaf Shower
        key(24, "leaf_shower", RANDOM, () -> Items.CHERRY_LEAVES, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "leaves", 60); }
            @Override public void tick(PowerUser u) {
                if (running(u, "leaves") && u.now() % 3 == 0) Fx.particles(u.level(), ParticleTypes.CHERRY_LEAVES, Fx.head(u.entity()).add(0, 1, 0), 6, 1.2);
            }
        });
        // #26 Bad Singer: random wrong notes
        key(26, "bad_singer", RANDOM, () -> Items.NOTE_BLOCK, notes(5, false));
        // #27 Play Dead: fall over for 5 s (mobs still attack you)
        key(27, "play_dead", RANDOM, () -> Items.SKELETON_SKULL, lieDown(100));
        // #28 Firework Sneeze: sparkles, no rocket, no flash
        key(28, "firework_sneeze", RANDOM, () -> Items.FIREWORK_STAR, u -> {
            Fx.sound(u.entity(), ModSounds.SNEEZE, 1.0f, 1.2f);
            Fx.sound(u.entity(), SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6f, 1.2f);
            Fx.particles(u.level(), ParticleTypes.FIREWORK, u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(0.6)), 15, 0.25);
        });
        // #32 Coin Flip: heal 2 hearts or lose 1 (never kills you)
        key(32, "coin_flip", RANDOM, () -> Items.GOLD_NUGGET, u -> {
            Fx.sound(u.entity(), SoundEvents.CHAIN_PLACE, 1.0f, 1.8f);
            if (u.entity().getRandom().nextBoolean()) {
                u.entity().heal(4.0f);
                Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(u.entity()), 3, 0.3);
                tell(u, "Heads! +2 hearts");
            } else {
                u.entity().setHealth(Math.max(1.0f, u.entity().getHealth() - 2.0f));
                Fx.sound(u.entity(), SoundEvents.PLAYER_HURT, 0.6f, 1.0f);
                tell(u, "Tails... -1 heart");
            }
        });
        // #34 Ghost Whisper: a spooky "boo" behind a random player near you, only they hear it
        key(34, "ghost_whisper", RANDOM, () -> Items.PHANTOM_MEMBRANE, u -> {
            var near = Fx.near(u.entity(), 16).stream().filter(p -> p instanceof ServerPlayer).toList();
            if (near.isEmpty()) return;
            ServerPlayer p = (ServerPlayer) near.get(u.entity().getRandom().nextInt(near.size()));
            Fx.soundFor(p, ModSounds.BOO.get(), p.position(), 0.8f, 1.0f);
        });
        // #35 Party Popper: confetti, once a minute
        key(35, "party_popper", RANDOM, 200, () -> Items.FIREWORK_ROCKET, u -> confetti(u, Fx.head(u.entity()), 30));
        // #38 Sneaky Snack: once a day, a cookie
        key(38, "sneaky_snack", CARRY, () -> Items.COOKIE, dailyItem("snack", () -> new ItemStack(Items.COOKIE), "You already had your sneaky snack today."));
        // #41 Baa
        key(41, "baa", RANDOM, () -> Items.WHITE_WOOL, u -> Fx.sound(u.entity(), SoundEvents.SHEEP_AMBIENT, 1.5f, 1.0f));
        // #42 Kazoo
        key(42, "kazoo", RANDOM, () -> Items.BAMBOO, u -> Fx.sound(u.entity(), ModSounds.KAZOO, 1.2f, 1.0f));
        // #44 Rain Dance: a spin and a tiny cloud. It doesn't rain.
        key(44, "rain_dance", RANDOM, () -> Items.WATER_BUCKET, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startSpin(u, 20, 18f); startFor(u, "cloud", 60); }
            @Override public void tick(PowerUser u) {
                spinTick(u);
                if (running(u, "cloud") && u.now() % 4 == 0) Fx.particles(u.level(), ParticleTypes.CLOUD, Fx.head(u.entity()).add(0, 0.8, 0), 4, 0.3);
            }
        });
        // #45 Sparkle Smile
        key(45, "sparkle_smile", RANDOM, () -> Items.AMETHYST_SHARD, u -> {
            Fx.sound(u.entity(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.6f);
            Fx.particles(u.level(), ParticleTypes.END_ROD, u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(0.4)), 4, 0.1);
        });
        // #46 Self High Five
        key(46, "self_high_five", RANDOM, () -> Items.RABBIT_FOOT, u -> {
            Fx.sound(u.entity(), ModSounds.CLAPPING, 1.2f, 1.2f);
            Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(u.entity()), 1, 0.1);
        });
        // #50 Hairball: cough up a string, once a minute
        key(50, "hairball", RANDOM, 1200, () -> Items.STRING, u -> {
            Fx.sound(u.entity(), SoundEvents.CAT_HISS, 0.8f, 1.6f);
            Fx.giveOrDrop(u, new ItemStack(Items.STRING));
        });
        // #51 Purr: cats near you sit down
        // (only for 5 s, and tamed cats are never ordered to sit: someone's pet must keep following them)
        key(51, "purr", RANDOM, () -> Items.CAT_SPAWN_EGG, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Fx.sound(u.entity(), SoundEvents.CAT_PURR, 1.5f, 1.0f);
                var list = new net.minecraft.nbt.ListTag();
                for (Cat c : u.level().getEntitiesOfClass(Cat.class, u.entity().getBoundingBox().inflate(8))) {
                    c.getNavigation().stop();
                    if (c.isTame() || c.isInSittingPose()) continue;
                    c.setInSittingPose(true);
                    list.add(net.minecraft.nbt.NbtUtils.createUUID(c.getUUID()));
                }
                u.state().put("sat", list);
                startFor(u, "satUntil", 100);
            }
            @Override public void tick(PowerUser u) {
                if (!u.state().contains("sat") || running(u, "satUntil")) return;
                standUp(u);
            }
            @Override public void stop(PowerUser u) { if (u.state().contains("sat")) standUp(u); }
            private void standUp(PowerUser u) {
                var list = u.state().getList("sat", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
                for (int i = 0; i < list.size(); i++) {
                    if (u.level().getEntity(net.minecraft.nbt.NbtUtils.loadUUID(list.get(i))) instanceof Cat c) c.setInSittingPose(false);
                }
                u.state().remove("sat");
            }
        });
        // #54 Bubble Wrap
        key(54, "bubble_wrap", RANDOM, () -> Items.SLIME_BALL, new PowerEffect() {
            @Override public void onKey(PowerUser u) { u.state().putInt("pops", 8); }
            @Override public void tick(PowerUser u) {
                int left = u.state().getInt("pops");
                if (left > 0 && u.now() % 3 == 0) {
                    u.state().putInt("pops", left - 1);
                    Fx.sound(u.entity(), ModSounds.BUBBLE_WRAP, 0.8f, 0.8f + u.entity().getRandom().nextFloat() * 0.6f);
                }
            }
        });
        // #55 Ice Cube: one ice block, once a day
        key(55, "ice_cube", CARRY, () -> Items.ICE, dailyItem("ice", () -> new ItemStack(Items.ICE), "Your ice cube for today already melted... I mean, you already had it."));
        // #56 Rose Giver: give a poppy to the player you look at (or the nearest), once a minute
        key(56, "rose_giver", RANDOM, 1200, () -> Items.POPPY, u -> {
            LivingEntity t = Fx.lookTarget(u.entity(), 8);
            Player to = t instanceof Player p ? p : Fx.nearestPlayer(u.entity(), 8);
            if (!(to instanceof ServerPlayer sp)) return;
            Fx.give(sp, new ItemStack(Items.POPPY));
            Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(sp), 2, 0.2);
            sp.sendSystemMessage(Component.literal(holderName(u) + " gave you a poppy!"));
        });
        // #60 Tail Chaser: spin in circles for 3 s
        key(60, "tail_chaser", RANDOM, () -> Items.LEAD, spinning(60, 24f));
        // #61 Disco: a slow, soft colour fade for 5 s. NEVER flashing (one colour step a second).
        key(61, "disco", RANDOM, () -> Items.JUKEBOX, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "disco", 100); }
            @Override public void tick(PowerUser u) {
                if (!running(u, "disco") || u.now() % 4 != 0) return;
                LivingEntity e = u.entity();
                Fx.particles(u.level(), slowColour(u.now(), 1.2f), e.position().add(0, e.getBbHeight() / 2, 0), 6, 0.5);
            }
        });
        // #64 Wet Dog: shake off water
        key(64, "wet_dog", RANDOM, () -> Items.WOLF_SPAWN_EGG, u -> {
            Fx.sound(u.entity(), SoundEvents.WOLF_SHAKE, 1.0f, 1.0f);
            Fx.particles(u.level(), ParticleTypes.SPLASH, u.entity().position().add(0, u.entity().getBbHeight() / 2, 0), 30, 0.5);
            u.entity().clearFire();
        });
        // #66 Dad Joke: a random terrible joke (edit config/charpowers/dad_jokes.txt)
        key(66, "dad_joke", RANDOM, () -> Items.BOOK, u -> Fx.say(u, TextLists.random("dad_jokes", TextLists.DAD_JOKES, u.entity().getRandom())));
        // #67 Nose Boop: boop the mob you look at and it sneezes (no damage)
        key(67, "nose_boop", ATTACK, () -> Items.FEATHER, u -> {
            LivingEntity t = Fx.lookTarget(u.entity(), 4);
            if (t == null) return;
            Fx.sound(t, ModSounds.SNEEZE, 1.0f, 1.4f);
            Fx.particles(u.level(), ParticleTypes.SNEEZE, t.getEyePosition(), 8, 0.15);
            u.entity().swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        });
        // #69 Snail Mail: your next chat message arrives 5 minutes later
        key(69, "snail_mail", CARRY, () -> Items.PAPER, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                u.state().putBoolean("armed", true);
                tell(u, "Your next chat message will arrive in 5 minutes. Slowly.");
            }
            @Override public boolean swallowsChat(PowerUser u) {
                return u.state().getBoolean("armed") && !u.state().contains("mail");
            }
            @Override public void onChat(PowerUser u, String text, boolean swallowed) {
                if (!u.state().getBoolean("armed") || u.state().contains("mail")) return;
                u.state().putBoolean("armed", false);
                u.state().putString("mail", text);
                startFor(u, "mailAt", 6000);
                tell(u, "Sent by snail. It'll get there in 5 minutes.");
            }
            @Override public void tick(PowerUser u) {
                if (!u.state().contains("mail") || running(u, "mailAt") || u.player() == null) return;
                Fx.playerSay(u.player(), u.state().getString("mail") + " (sent by snail)");
                u.state().remove("mail");
            }
        });
        // #70 Fortune Cookie: once a day, a cookie named with a fortune (edit config/charpowers/fortunes.txt)
        key(70, "fortune_cookie", CARRY, () -> Items.COOKIE, dailyItem("fortune",
                () -> named(Items.COOKIE, TextLists.random("fortunes", TextLists.FORTUNES, net.minecraft.util.RandomSource.create())),
                "One fortune a day. The cookie has spoken."));
        // #77 Sticky Note: your next chat message floats on the block you look at for 1 minute
        key(77, "sticky_note", CARRY, () -> Items.YELLOW_DYE, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                u.state().putBoolean("armed", true);
                tell(u, "Look at a block and type your sticky note in chat.");
            }
            @Override public boolean swallowsChat(PowerUser u) { return u.state().getBoolean("armed"); }
            @Override public void onChat(PowerUser u, String text, boolean swallowed) {
                if (!u.state().getBoolean("armed")) return;
                u.state().putBoolean("armed", false);
                HitResult hit = u.entity().pick(6.0, 1.0f, false);
                Vec3 at = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK
                        ? Vec3.atCenterOf(b.getBlockPos().relative(b.getDirection())) : u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(2));
                floatingText(u, at, text, 1200);
            }
            @Override public void tick(PowerUser u) { if (u.now() % 20 == 0) textTick(u); }
            @Override public void stop(PowerUser u) { clearTexts(u); }
        });
        // #78 Hot Potato: a potato way too hot to hold. Throw it within 5 s or lose half a heart.
        key(78, "hot_potato", ATTACK, 400, () -> Items.BAKED_POTATO, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                if (u.player() != null) {
                    ItemStack s = named(Items.BAKED_POTATO, "Hot Potato");
                    s.getOrCreateTag().putLong("charpowers.hot", u.now() + 100);
                    Fx.give(u.player(), s);
                    tell(u, "HOT HOT HOT! Throw it (Q) quickly!");
                } else if (u.mob() != null && u.mob().getTarget() != null) {
                    // a mob throws it at whatever it's fighting
                    LivingEntity t = u.mob().getTarget();
                    Snowball ball = new Snowball(u.level(), u.mob());
                    ball.setItem(new ItemStack(Items.BAKED_POTATO));
                    Vec3 d = t.getEyePosition().subtract(u.mob().getEyePosition());
                    ball.shoot(d.x, d.y + d.horizontalDistance() * 0.2, d.z, 1.2f, 4f);
                    u.level().addFreshEntity(ball);
                }
            }
            @Override public void tick(PowerUser u) {
                if (u.player() == null || u.now() % 10 != 0) return;
                var inv = u.player().getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.hasTag() && s.getTag().contains("charpowers.hot") && u.now() >= s.getTag().getLong("charpowers.hot")) {
                        inv.setItem(i, ItemStack.EMPTY);
                        Fx.tinyHit(u.entity(), null, 1.0f);
                        tell(u, "Ouch! Too hot!");
                    }
                }
            }
        });
        // #80 Mini Me / #81 Tall Tale: shrink or grow a little for 10 s (needs Pehkui, which the Powers server has)
        key(80, "mini_me", RANDOM, 400, () -> Items.BROWN_MUSHROOM, scaled(0.8f));
        key(81, "tall_tale", RANDOM, 400, () -> Items.BAMBOO, scaled(1.2f));
        // #84 The Floor Is Lava: just a chat message. Nothing happens.
        key(84, "the_floor_is_lava", RANDOM, () -> Items.MAGMA_BLOCK, u -> {
            Component line = Component.literal("THE FLOOR IS LAVA!").withStyle(net.minecraft.ChatFormatting.GOLD);
            for (Player p : Fx.near(u.entity(), 16)) p.sendSystemMessage(line);
            if (u.player() != null) u.player().sendSystemMessage(line);
        });
        // #85 Victory Dance: only after a kill (within 5 s)
        key(85, "victory_dance", RANDOM, () -> Items.GOLDEN_SWORD, new PowerEffect() {
            @Override public void onKill(PowerUser u, LivingEntity victim) { startFor(u, "won", 100); }
            @Override public void onKey(PowerUser u) {
                if (!running(u, "won")) { tell(u, "Win a fight first!"); return; }
                startSpin(u, 12, 30f);
                u.entity().push(0, 0.4, 0);
                u.entity().hurtMarked = true;
                Fx.particles(u.level(), ParticleTypes.NOTE, Fx.head(u.entity()), 4, 0.4);
            }
            @Override public void tick(PowerUser u) { spinTick(u); }
        });
        // #86 Mime: you make no sound at all for 30 s
        key(86, "mime", RANDOM, 900, () -> Items.WHITE_BANNER, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "mime", 600); tell(u, "..."); }
            @Override public void onSound(PowerUser u, net.minecraftforge.event.PlayLevelSoundEvent e) { if (running(u, "mime")) e.setCanceled(true); }
        });
        // #92 Thought Bubble: "..." floats over your head for 5 s
        key(92, "thought_bubble", RANDOM, () -> Items.WHITE_STAINED_GLASS, new PowerEffect() {
            @Override public void onKey(PowerUser u) { floatingText(u, Fx.head(u.entity()).add(0, 0.3, 0), "...", 100); }
            @Override public void tick(PowerUser u) { if (u.now() % 20 == 0) textTick(u); }
            @Override public void stop(PowerUser u) { clearTexts(u); }
        });
        // #93 Dizzy: spin really fast for 2 s, then walk wobbly for 3 s
        key(93, "dizzy", RANDOM, () -> Items.SPIDER_EYE, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startSpin(u, 40, 45f); startFor(u, "wobble", 100); }
            @Override public void tick(PowerUser u) {
                spinTick(u);
                if (running(u, "wobble") && u.state().getInt("spinLeft") <= 0 && u.now() % 5 == 0) {
                    var r = u.entity().getRandom();
                    u.entity().push((r.nextFloat() - 0.5) * 0.15, 0, (r.nextFloat() - 0.5) * 0.15);
                    u.entity().hurtMarked = true;
                }
            }
        });
        // #96 Grass Whistle
        key(96, "grass_whistle", RANDOM, () -> Items.GRASS, u -> Fx.sound(u.entity(), ModSounds.GRASS_WHISTLE, 1.2f, 1.0f));
        // #97 Spoon: once a day, a stick named Spoon
        key(97, "spoon", CARRY, () -> Items.STICK, dailyItem("spoon", () -> named(Items.STICK, "Spoon"), "You already have today's spoon."));
        // #98 Drum Roll: a drum roll, then a cymbal crash
        key(98, "drum_roll", RANDOM, () -> Items.NOTE_BLOCK, new PowerEffect() {
            @Override public void onKey(PowerUser u) { u.state().putInt("roll", 16); Fx.sound(u.entity(), ModSounds.DRUM_ROLL, 1.0f, 1.0f); }
            @Override public void tick(PowerUser u) {
                int left = u.state().getInt("roll");
                if (left <= 0 || u.now() % 2 != 0) return;
                u.state().putInt("roll", left - 1);
                if (left == 1) Fx.sound(u.entity(), SoundEvents.NOTE_BLOCK_HAT.value(), 1.5f, 0.6f);
                else Fx.sound(u.entity(), SoundEvents.NOTE_BLOCK_SNARE.value(), 0.8f, 1.0f);
            }
        });
        // #99 Pufferfish: puff up for 3 s; mobs that bump you get poked away (no damage)
        key(99, "pufferfish", ATTACK, () -> Items.PUFFERFISH, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                startFor(u, "puff", 60);
                Fx.sound(u.entity(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.0f, 1.0f);
            }
            @Override public void tick(PowerUser u) {
                if (!running(u, "puff")) return;
                if (u.now() % 5 == 0) Fx.particles(u.level(), ParticleTypes.BUBBLE_POP, u.entity().position().add(0, u.entity().getBbHeight() / 2, 0), 4, 0.6);
                for (LivingEntity o : u.level().getEntitiesOfClass(LivingEntity.class, u.entity().getBoundingBox().inflate(0.6), o -> o != u.entity())) {
                    Fx.push(o, u.entity().position(), 0.35);
                }
            }
        });
        // #100 No U
        key(100, "no_u", RANDOM, () -> Items.REDSTONE_TORCH, u -> Fx.say(u, "NO U"));
    }

    // --- little building blocks used above ---

    static void tell(PowerUser u, String text) {
        if (u.player() != null) u.player().displayClientMessage(Component.literal(text), true);
    }

    static PowerEffect spinning(int ticks, float degreesPerTick) {
        return new PowerEffect() {
            @Override public void onKey(PowerUser u) { startSpin(u, ticks, degreesPerTick); }
            @Override public void tick(PowerUser u) { spinTick(u); }
        };
    }

    // Lie on the ground. Players lie flat (Lying + PlayerPoseMixin keep the pose steady) and can't walk;
    // mobs just stop where they are (most mobs have no lying-down look).
    static PowerEffect lieDown(int ticks) {
        return new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                startFor(u, "down", ticks);
                u.entity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false));
                Fx.sound(u.entity(), SoundEvents.PLAYER_SMALL_FALL, 1.0f, 0.8f);
                if (u.player() != null) com.example.charpowers.hidden.Lying.start(u.player(), ticks);
            }
            @Override public void tick(PowerUser u) {
                if (running(u, "down") && u.mob() != null) u.mob().getNavigation().stop();
            }
        };
    }

    // A few random (wrong) note-block notes.
    static PowerEffect notes(int count, boolean sad) {
        return new PowerEffect() {
            @Override public void onKey(PowerUser u) { u.state().putInt("notes", count); }
            @Override public void tick(PowerUser u) {
                int left = u.state().getInt("notes");
                if (left <= 0 || u.now() % 5 != 0) return;
                u.state().putInt("notes", left - 1);
                float pitch = sad ? 1.0f - (count - left) * 0.12f : 0.5f + u.entity().getRandom().nextFloat() * 1.5f;
                Fx.sound(u.entity(), SoundEvents.NOTE_BLOCK_HARP.value(), 1.0f, pitch);
                Fx.particles(u.level(), ParticleTypes.NOTE, Fx.head(u.entity()), 1, 0.2);
            }
        };
    }

    static PowerEffect dailyItem(String key, java.util.function.Supplier<ItemStack> item, String already) {
        return new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                if (Fx.daily(u, key)) Fx.giveOrDrop(u, item.get());
                else tell(u, already);
            }
        };
    }

    static void confetti(PowerUser u, Vec3 at, int count) {
        Fx.sound(u.entity(), ModSounds.PARTY_POPPER, 1.0f, 1.0f);
        for (int i = 0; i < count; i++) Fx.particles(u.level(), randomColour(u.entity().getRandom()), at, 1, 0.6);
    }

    // Changes size for 10 s through Pehkui (looked up by name, so nothing breaks if it isn't installed).
    static PowerEffect scaled(float scale) {
        return new PowerEffect() {
            @Override public void onKey(PowerUser u) { if (Pehkui.scale(u.entity(), scale)) startFor(u, "scaled", 200); }
            @Override public void tick(PowerUser u) {
                if (u.state().getLong("scaled") != 0 && !running(u, "scaled")) {
                    Pehkui.scale(u.entity(), 1.0f);
                    u.state().putLong("scaled", 0);
                }
            }
            @Override public void stop(PowerUser u) { if (u.state().getLong("scaled") != 0) Pehkui.scale(u.entity(), 1.0f); }
        };
    }

    private FKeyPowers() {}
}
