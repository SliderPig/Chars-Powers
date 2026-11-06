package com.example.charpowers.hidden.f;

import com.example.charpowers.hidden.ModSounds;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.fx.TextLists;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.level.BlockEvent;

import javax.annotation.Nullable;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.CARRY;
import static com.example.charpowers.hidden.model.MobRule.PASSIVE;

// The F-rank always-on powers that react to things: chat, eating, mining, books, furnaces, the weather...
final class FEventPowers {
    private static final Block[] SMALL_FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.ALLIUM};

    static void registerAll() {
        // #9 Echo: your chat messages repeat themselves 3 s later
        always(9, "echo", CARRY, () -> Items.BELL, new PowerEffect() {
            @Override public void onChat(PowerUser u, String text, boolean swallowed) {
                if (swallowed) return; // a Snail Mail / Sticky Note message never reached chat, so nothing to echo
                u.state().putString("echo", text);
                startFor(u, "echoAt", 60);
            }
            @Override public void tick(PowerUser u) {
                if (!u.state().contains("echo") || running(u, "echoAt") || u.player() == null) return;
                Component line = Component.literal("<" + u.player().getGameProfile().getName() + "> " + u.state().getString("echo") + "...")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                u.player().server.getPlayerList().broadcastSystemMessage(line, false);
                u.state().remove("echo");
            }
        });
        // #25 Noisy Eater: your eating sounds are 3x louder
        always(25, "noisy_eater", CARRY, () -> Items.APPLE, new PowerEffect() {
            @Override public void onSound(PowerUser u, PlayLevelSoundEvent e) {
                if (e.getSound() != null && e.getSound().value() == SoundEvents.GENERIC_EAT) e.setNewVolume(e.getNewVolume() * 3f);
            }
        });
        // #29 Narrator: now and then chat describes what you're doing like a nature documentary
        always(29, "narrator", CARRY, () -> Items.WRITABLE_BOOK, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() == null) return;
                long next = u.state().getLong("narrate");
                if (next != 0 && u.now() < next) return;
                u.state().putLong("narrate", u.now() + 6000 + u.entity().getRandom().nextInt(6001));
                if (next == 0) return;
                String line = narratorLine(u).replace("{name}", u.player().getGameProfile().getName())
                        .replace("{food}", FOODS[u.entity().getRandom().nextInt(FOODS.length)]);
                Component c = Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                u.player().sendSystemMessage(c);
                for (Player p : Fx.near(u.entity(), 16)) p.sendSystemMessage(c);
            }
        });
        // #30 Backwards Compass: your compass always points exactly the wrong way (done on your own PC, see ClientEffects)
        always(30, "backwards_compass", CARRY, () -> Items.COMPASS, new PowerEffect() {});
        // #31 Pillow Fight: hitting anyone with wool makes feathers burst everywhere
        always(31, "pillow_fight", PASSIVE, () -> Items.WHITE_WOOL, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) {
                if (!u.entity().getMainHandItem().is(ItemTags.WOOL)) return;
                Fx.particles(u.level(), new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)), target.getEyePosition(), 15, 0.4);
                Fx.particles(u.level(), ParticleTypes.CLOUD, target.getEyePosition(), 5, 0.3);
                Fx.sound(target, SoundEvents.WOOL_BREAK, 1.0f, 1.2f);
            }
        });
        // #36 Bird Brain: parrots near you squawk when you chat
        always(36, "bird_brain", CARRY, () -> Items.PARROT_SPAWN_EGG, new PowerEffect() {
            @Override public void onChat(PowerUser u, String text, boolean swallowed) {
                for (Parrot p : u.level().getEntitiesOfClass(Parrot.class, u.entity().getBoundingBox().inflate(16))) {
                    Fx.sound(p, SoundEvents.PARROT_AMBIENT, 1.0f, 1.0f + p.getRandom().nextFloat() * 0.3f);
                }
            }
        });
        // #40 Overly Polite: villagers near you turn and look at you... and nothing else happens
        always(40, "overly_polite", PASSIVE, () -> Items.EMERALD, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 != 0) return;
                for (Villager v : u.level().getEntitiesOfClass(Villager.class, u.entity().getBoundingBox().inflate(8), v -> v != u.entity())) {
                    v.getLookControl().setLookAt(u.entity(), 30f, 30f);
                }
            }
        });
        // #43 Butter Fingers: 1% chance you drop what you're holding when you hit something
        always(43, "butter_fingers", PASSIVE, () -> Items.HONEY_BOTTLE, new PowerEffect() {
            @Override public void onAttack(PowerUser u, LivingEntity target) {
                if (u.entity().getRandom().nextInt(100) != 0) return;
                ItemStack held = u.entity().getMainHandItem();
                if (held.isEmpty()) return;
                if (u.player() != null) u.player().drop(held.copy(), true);
                else u.entity().spawnAtLocation(held.copy());
                u.entity().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        });
        // #47 Lucky Penny: once a day a gold nugget appears
        always(47, "lucky_penny", CARRY, () -> Items.GOLD_NUGGET, dailyGift("penny", () -> new ItemStack(Items.GOLD_NUGGET)));
        // #58 Rumbly Tummy: your stomach growls when you're hungry
        always(58, "rumbly_tummy", CARRY, () -> Items.BOWL, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.player() != null && u.player().getFoodData().getFoodLevel() <= 6 && u.every("tummy", 1200)) {
                    Fx.sound(u.entity(), ModSounds.STOMACH, 1.0f, 1.0f);
                }
            }
        });
        // #63 Pebble Collector: 1% of stone you mine also gives a cobblestone named "Pebble"
        always(63, "pebble_collector", CARRY, () -> Items.COBBLESTONE, new PowerEffect() {
            @Override public void onBreak(PowerUser u, BlockEvent.BreakEvent e) {
                if (e.getState().is(BlockTags.BASE_STONE_OVERWORLD) && u.entity().getRandom().nextInt(100) == 0) {
                    Block.popResource(u.level(), e.getPos(), named(Items.COBBLESTONE, "Pebble"));
                }
            }
        });
        // #65 Flower Steps: now and then a flower pops up where you walk on grass (never on anything else)
        always(65, "flower_steps", PASSIVE, () -> Items.DANDELION, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (!stepped(u) || u.entity().getRandom().nextInt(100) != 0) return;
                BlockPos feet = u.entity().blockPosition();
                if (u.level().getBlockState(feet.below()).is(Blocks.GRASS_BLOCK) && u.level().getBlockState(feet).isAir()) {
                    u.level().setBlock(feet, SMALL_FLOWERS[u.entity().getRandom().nextInt(SMALL_FLOWERS.length)].defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        });
        // #68 Weather Grump: a grumpy "hmph" when it starts raining (if you're outdoors)
        always(68, "weather_grump", PASSIVE, () -> Items.WATER_BUCKET, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                boolean raining = u.level().isRaining();
                boolean was = u.state().getBoolean("rain");
                u.state().putBoolean("rain", raining);
                if (raining && !was && u.state().getBoolean("known") && u.level().canSeeSky(u.entity().blockPosition())) {
                    Fx.sound(u.entity(), ModSounds.HMPH, 1.0f, 1.0f);
                }
                u.state().putBoolean("known", true);
            }
        });
        // #79 Lost Sock: once a day, a leather boot named "Lost Sock" falls out of you
        always(79, "lost_sock", PASSIVE, () -> Items.LEATHER_BOOTS, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 100 == 0 && Fx.daily(u, "sock")) u.entity().spawnAtLocation(named(Items.LEATHER_BOOTS, "Lost Sock"));
            }
        });
        // #87 Paper Cut: opening a book makes a tiny "ow"
        always(87, "paper_cut", CARRY, () -> Items.PAPER, new PowerEffect() {
            @Override public void onRightClick(PowerUser u, ItemStack held, @Nullable BlockState block) {
                boolean book = block == null && (held.is(Items.WRITABLE_BOOK) || held.is(Items.WRITTEN_BOOK));
                boolean lectern = block != null && block.is(Blocks.LECTERN);
                if ((book || lectern) && u.every("cut", 10)) Fx.sound(u.entity(), ModSounds.OW, 0.8f, 1.0f);
            }
        });
        // #90 Pocket Lint: once a day, a string named "Pocket Lint"
        always(90, "pocket_lint", CARRY, () -> Items.STRING, dailyGift("lint", () -> named(Items.STRING, "Pocket Lint")));
        // #94 Microwave Ding: a "ding" when a furnace near you finishes an item
        always(94, "microwave_ding", CARRY, () -> Items.FURNACE, new PowerEffect() {
            @Override public void tick(PowerUser u) {
                if (u.now() % 20 != 0) return;
                var seen = u.state().getCompound("furnaces");
                var now = new net.minecraft.nbt.CompoundTag();
                boolean ding = false;
                BlockPos me = u.entity().blockPosition();
                int cx = me.getX() >> 4, cz = me.getZ() >> 4;
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    if (!u.level().hasChunk(cx + dx, cz + dz)) continue;
                    LevelChunk chunk = u.level().getChunk(cx + dx, cz + dz);
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof AbstractFurnaceBlockEntity f) || be.getBlockPos().distSqr(me) > 64) continue;
                        String key = be.getBlockPos().asLong() + "";
                        int out = f.getItem(2).getCount();
                        if (seen.contains(key) && out > seen.getInt(key)) ding = true;
                        now.putInt(key, out);
                    }
                }
                u.state().put("furnaces", now);
                if (ding) Fx.sound(u.entity(), ModSounds.MICROWAVE_DING, 1.0f, 1.0f);
            }
        });
    }

    private static final String[] FOODS = {"carrot", "potato", "cookie", "apple", "steak", "suspicious stew", "melon slice"};

    // Picks a Narrator line that fits what the player is doing (lines are in config/charpowers/narrator.txt).
    static String narratorLine(PowerUser u) {
        var lines = TextLists.get("narrator", TextLists.NARRATOR);
        LivingEntity e = u.entity();
        String want;
        if (e.isSleeping()) want = "rests";
        else if (e.isSwimming() || e.isInWater()) want = "swims";
        else if (e.isSprinting()) want = "run";
        else if (e.getLastHurtByMob() != null && e.tickCount - e.getLastHurtByMobTimestamp() < 200) want = "fights";
        else if (e.isCrouching()) want = "sneaks";
        else if (e.isUsingItem() && e.getUseItem().isEdible()) want = "eats";
        else if (e.getY() < 40 && !u.level().canSeeSky(e.blockPosition())) want = "underground";
        else if (u.level().isRaining()) want = "rain";
        else if (u.level().isNight()) want = "Night";
        else want = null;
        if (want != null) {
            var fits = lines.stream().filter(l -> l.contains(want)).toList();
            if (!fits.isEmpty()) return fits.get(e.getRandom().nextInt(fits.size()));
        }
        return lines.get(e.getRandom().nextInt(lines.size()));
    }

    static PowerEffect dailyGift(String key, java.util.function.Supplier<ItemStack> item) {
        return new PowerEffect() {
            @Override public void tick(PowerUser u) { if (u.now() % 100 == 0 && Fx.daily(u, key)) Fx.giveOrDrop(u, item.get()); }
        };
    }

    private FEventPowers() {}
}
