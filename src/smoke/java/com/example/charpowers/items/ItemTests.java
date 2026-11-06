package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.model.ComboRules;
import com.example.charpowers.items.model.SprayRules.Liquid;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

// Char's new weapons and Solgo in a real headless world.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class ItemTests {
    private static ServerPlayer player(GameTestHelper h, String name, BlockPos where) {
        ServerLevel level = h.getLevel();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        BlockPos at = h.absolutePos(where);
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        return p;
    }

    private static void gone(GameTestHelper h, ServerPlayer p) {
        h.getLevel().getServer().getPlayerList().remove(p);
    }

    private static void hold(ServerPlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.getAttributes().addTransientAttributeModifiers(stack.getAttributeModifiers(EquipmentSlot.MAINHAND));
    }

    private static void look(ServerPlayer p, Vec3 at) {
        Vec3 d = at.subtract(p.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.setYHeadRot(yaw);
        p.yRotO = yaw;
        p.xRotO = pitch;
    }

    private static Zombie zombie(GameTestHelper h, BlockPos where) {
        Zombie z = h.spawn(EntityType.ZOMBIE, where);
        z.setNoAi(true);
        z.setPersistenceRequired();
        return z;
    }

    private static void need(boolean ok, String why) {
        if (!ok) throw new GameTestAssertException(why);
    }

    @GameTest(template = "empty")
    public static void recipesAreThere(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        for (String id : new String[]{"fork", "spray_bottle", "flame_hammer"}) {
            Optional<? extends Recipe<?>> r = recipes.byKey(new ResourceLocation(CharPowers.MODID, id));
            need(r.isPresent(), "no recipe " + id);
            need(r.get().getResultItem(h.getLevel().registryAccess()).getItem() == net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(CharPowers.MODID, id)), "recipe " + id + " makes the wrong thing");
            need(!r.get().getIngredients().isEmpty(), "recipe " + id + " has no ingredients");
        }
        var hammer = recipes.byKey(new ResourceLocation(CharPowers.MODID, "flame_hammer")).get();
        long blocks = hammer.getIngredients().stream().filter(i -> !i.isEmpty() && i.getItems().length > 0 && net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(i.getItems()[0].getItem()).toString().equals("palladium:vibranium_block")).count();
        long rods = hammer.getIngredients().stream().filter(i -> i.test(new ItemStack(Items.BLAZE_ROD))).count();
        need(blocks == 4, "the hammer wants " + blocks + " vibranium blocks");
        need(rods == 2, "the hammer wants " + rods + " blaze rods");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "itemsPlayer", timeoutTicks = 200)
    public static void dropsComeFromEachSource(GameTestHelper h) {
        ServerPlayer p = player(h, "looter", new BlockPos(1, 2, 1));
        need(!SolgoSpawning.dropFor("charpowers:frost_king", null, true, 0.1).isEmpty(), "no staff from the Frost King");
        need(SolgoSpawning.dropFor("charpowers:frost_king", null, true, 0.9).isEmpty(), "staff when the roll failed");
        need(SolgoSpawning.dropFor("charpowers:frost_king", null, false, 0.1).isEmpty(), "staff with no player involved");
        need(SolgoSpawning.dropFor("charpowers:dark_elf", null, true, 0.01).is(CharItems.DAGGER.get()), "no dagger from a Dark Elf");
        need(SolgoSpawning.dropFor("charpowers:dark_elf", null, true, 0.5).isEmpty(), "dagger when the roll failed");
        need(SolgoSpawning.dropFor("minecraft:zombie", p, true, 0).isEmpty(), "zombies drop nothing of ours");
        // kills Solgo for real: first kill always gives the katana, whatever the roll
        Solgo s = Solgo.spawn(h.getLevel(), p.getX() + 2, p.getY(), p.getZ(), false);
        s.setLastHurtByPlayer(p);
        s.hurt(p.damageSources().playerAttack(p), 100000f);
        need(s.isDeadOrDying(), "Solgo should be dead");
        h.runAtTickTime(4, () -> {
            var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, s.getBoundingBox().inflate(6), i -> i.getItem().is(CharItems.KATANA.get()));
            need(!items.isEmpty(), "Solgo's first kill left no katana");
            need(!SolgoSpawning.dropFor("charpowers:solgo", p, true, 0.1).isEmpty(), "25% roll should still give it");
            need(SolgoSpawning.dropFor("charpowers:solgo", p, true, 0.9).isEmpty(), "after the first kill it's only sometimes");
            items.forEach(ItemEntity::discard);
            s.discard();
            gone(h, p);
            h.succeed();
        });
    }

    // The real Frost King and Dark Elf are there, and killing the King really goes through the drops. 35% a kill, so 16 kills almost surely give one.
    @GameTest(template = "empty", batch = "itemsPlayer8", timeoutTicks = 200)
    public static void frostKingReallyDropsTheStaff(GameTestHelper h) {
        ServerPlayer p = player(h, "kingslayer", new BlockPos(1, 2, 1));
        for (String id : new String[]{"frost_king", "dark_elf"}) {
            need(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(CharPowers.MODID, id)), "no " + id);
        }
        Vec3 at = p.position().add(3, 0, 3);
        for (int i = 0; i < 16; i++) {
            var king = com.example.charpowers.game.mob.GateBoss.spawn(h.getLevel(), com.example.charpowers.game.mob.GateKind.FROST_KING, at, 2, 1, m -> {}, () -> {});
            king.setLastHurtByPlayer(p);
            king.hurt(p.damageSources().playerAttack(p), 1000000f);
            need(king.isDeadOrDying(), "the Frost King didn't die");
        }
        h.runAtTickTime(4, () -> {
            var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(10), i -> i.getItem().is(CharItems.ICE_STAFF.get()));
            need(!items.isEmpty(), "16 Frost Kings and no staff");
            h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(10)).forEach(ItemEntity::discard);
            gone(h, p);
            h.succeed();
        });
    }

    // A swing's hit waits for the blade: nothing at the click, the blow a few ticks later.
    @GameTest(template = "empty", batch = "itemsPlayer9", timeoutTicks = 100)
    public static void timedHitLandsWhenTheBladeArrives(GameTestHelper h) {
        ServerPlayer p = player(h, "swinger", new BlockPos(1, 2, 1));
        hold(p, new ItemStack(CharItems.FORK.get()));
        Zombie z = zombie(h, new BlockPos(1, 2, 3));
        look(p, z.position().add(0, 1, 0));
        float full = z.getHealth();
        int at = com.example.charpowers.items.model.WeaponStyle.FORK.hitTick(0);
        need(at >= 2, "the fork's jab should take a moment to arrive");
        WeaponHits.request(p, z.getId(), 0);
        need(z.getHealth() == full, "hit straight away");
        h.runAtTickTime(at + 3, () -> {
            need(z.getHealth() < full, "the blow never landed");
            z.discard();
            gone(h, p);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "itemsPlayer2", timeoutTicks = 200)
    public static void iceStaffFreezes(GameTestHelper h) {
        ServerPlayer p = player(h, "caster", new BlockPos(1, 2, 1));
        Zombie z = zombie(h, new BlockPos(1, 2, 7));
        z.setNoAi(false);
        float before = z.getHealth();
        look(p, z.position().add(0, 1, 0));
        IceShard shard = new IceShard(h.getLevel(), p);
        shard.setPos(p.getX(), p.getEyeY() - 0.2, p.getZ());
        shard.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 2.1f, 0f);
        h.getLevel().addFreshEntity(shard);
        h.runAtTickTime(12, () -> {
            need(z.getHealth() < before, "the shard didn't hurt it");
            need(z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "no slowness");
            need(z.getTicksFrozen() > 0, "no frost");
            need(z.isNoAi() && IceShard.stopped(z), "not stopped dead");
            need(shard.isRemoved(), "the shard should have shattered");
        });
        h.runAtTickTime(60, () -> {
            need(!z.isNoAi(), "it never woke up");
            need(!z.getPersistentData().contains("charpowers.iced"), "still marked as iced");
            z.discard();
            gone(h, p);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "itemsPlayer3", timeoutTicks = 100)
    public static void soulCutIgnoresArmour(GameTestHelper h) {
        ServerPlayer p = player(h, "cutter", new BlockPos(1, 2, 1));
        ItemStack katana = new ItemStack(CharItems.KATANA.get());
        hold(p, katana);
        Zombie armoured = zombie(h, new BlockPos(3, 2, 1));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            armoured.setItemSlot(slot, new ItemStack(switch (slot) {
                case HEAD -> Items.NETHERITE_HELMET;
                case CHEST -> Items.NETHERITE_CHESTPLATE;
                case LEGS -> Items.NETHERITE_LEGGINGS;
                default -> Items.NETHERITE_BOOTS;
            }));
        }
        armoured.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4));
        // re-reads attributes the way a tick would, so the armour really counts
        armoured.getAttributes().addTransientAttributeModifiers(armoured.getItemBySlot(EquipmentSlot.CHEST).getAttributeModifiers(EquipmentSlot.CHEST));
        need(armoured.getArmorValue() > 0, "the armour isn't on");
        float before = armoured.getHealth();
        need(AnimaKatana.strike(p, armoured, katana, 1f), "the cut didn't land");
        float taken = before - armoured.getHealth();
        need(Math.abs(taken - 10f) < 0.01f, "soul cut took " + taken + ", should be 10 through netherite and Resistance");
        DamageSource soul = Foes.soul(p);
        need(soul.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) && soul.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD) && soul.is(net.minecraft.tags.DamageTypeTags.BYPASSES_RESISTANCE)
                && soul.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS) && soul.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS), "tags missing from the soul damage type");
        need(soul.getLocalizedDeathMessage(armoured).getString().contains("soul was cut by"), "wrong death message: " + soul.getLocalizedDeathMessage(armoured).getString());
        armoured.discard();
        gone(h, p);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "itemsPlayer4", timeoutTicks = 200)
    public static void sprayOnlyHurtsWithLava(GameTestHelper h) {
        ServerPlayer p = player(h, "sprayer", new BlockPos(1, 2, 1));
        Zombie z = zombie(h, new BlockPos(1, 2, 4));
        look(p, z.position().add(0, 1, 0));
        ServerLevel level = h.getLevel();
        float full = z.getHealth();
        z.setSecondsOnFire(5);
        SprayBottle.sprayOnce(level, p, Liquid.WATER);
        need(z.getHealth() == full, "water hurt it");
        need(!z.isOnFire(), "water didn't put it out");
        z.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        SprayBottle.sprayOnce(level, p, Liquid.MILK);
        need(z.getHealth() == full, "milk hurt it");
        need(!z.hasEffect(MobEffects.GLOWING), "milk didn't clear its effects");
        SprayBottle.sprayOnce(level, p, Liquid.LAVA);
        need(z.getHealth() < full, "lava didn't hurt it");
        need(z.isOnFire(), "lava didn't set it alight");
        // a bottle counts down and goes empty, and never leaves a source block
        ItemStack bottle = new ItemStack(CharItems.SPRAY_BOTTLE.get());
        SprayBottle.set(bottle, Liquid.LAVA, 1);
        need(SprayBottle.liquid(bottle) == Liquid.LAVA && SprayBottle.sprays(bottle) == 1, "bottle didn't fill");
        SprayBottle.set(bottle, Liquid.LAVA, 0);
        need(SprayBottle.liquid(bottle) == Liquid.EMPTY, "empty bottle still holds something");
        z.discard();
        gone(h, p);
        h.succeed();
    }

    @GameTest(template = "empty", batch = "itemsPlayer5", timeoutTicks = 100)
    public static void swordComesOutOnce(GameTestHelper h) {
        ServerPlayer p = player(h, "king", new BlockPos(2, 2, 1));
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 3));
        ServerLevel level = h.getLevel();
        level.setBlock(at, CharItems.SWORD_STONE.get().defaultBlockState(), 3);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        var state = level.getBlockState(at);
        need(state.getDestroySpeed(level, at) < 0, "the stone can be broken while the sword's in it");
        state.use(level, p, InteractionHand.MAIN_HAND, hit);
        need(p.getInventory().countItem(CharItems.LONG_SWORD.get()) == 1, "no sword after pulling");
        var after = level.getBlockState(at);
        need(after.getValue(SwordInStone.PULLED), "the stone doesn't know the sword's gone");
        after.use(level, p, InteractionHand.MAIN_HAND, hit);
        need(p.getInventory().countItem(CharItems.LONG_SWORD.get()) == 1, "pulled a second sword");
        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        gone(h, p);
        h.succeed();
    }

    // The real overworld's noise, on this flat test world: Kings Rock finds river biomes, and its piece builds a rock with the sword on top.
    @GameTest(template = "empty", batch = "kingsRock", timeoutTicks = 200)
    public static void kingsRockGeneratesInARiver(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var access = level.registryAccess();
        Structure rock = access.registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation(CharPowers.MODID, "kings_rock"));
        need(rock != null, "no structure");
        var lists = access.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        BiomeSource source = MultiNoiseBiomeSource.createFromPreset(lists.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(source, settings);
        long seed = 12345L;
        RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
        int found = 0;
        Structure.GenerationStub first = null;
        for (int cx = -8; cx <= 8; cx++) {
            for (int cz = -8; cz <= 8; cz++) {
                var c = new Structure.GenerationContext(access, generator, source, random, level.getStructureManager(), seed, new net.minecraft.world.level.ChunkPos(cx * 48, cz * 48), level, rock.biomes()::contains);
                Optional<Structure.GenerationStub> stub = rock.findValidGenerationPoint(c);
                if (stub.isEmpty()) continue;
                found++;
                BlockPos at = stub.get().position();
                var biome = source.getNoiseBiome(at.getX() >> 2, at.getY() >> 2, at.getZ() >> 2, random.sampler());
                need(biome.is(BiomeTags.IS_RIVER), "a rock in a " + biome.unwrapKey() + " biome");
                if (first == null) first = stub.get();
            }
        }
        CharPowers.LOG.info("[ItemTests] Kings Rock could start in {} of 289 places", found);
        need(found > 10 && found < 289, "Kings Rock found " + found + " of 289 places");
        // builds one for real, away from the other tests
        PiecesContainer pieces = first.getPiecesBuilder().build();
        BlockPos spot = h.absolutePos(new BlockPos(2000, 0, 2000));
        int y = 70;
        KingsRock.Piece.place(level, new BoundingBox(spot.getX() - 40, y - 10, spot.getZ() - 40, spot.getX() + 40, y + 40, spot.getZ() + 40), spot.getX(), y, spot.getZ(), seed);
        BlockPos sword = KingsRock.Piece.swordSpot(spot.getX(), y, spot.getZ(), seed);
        need(level.getBlockState(sword).is(CharItems.SWORD_STONE.get()), "no sword on top: " + level.getBlockState(sword));
        need(level.getBlockEntity(sword) instanceof SwordInStone.Entity, "no block entity for the sword");
        need(!level.getBlockState(sword.below()).isAir(), "the sword's floating");
        need(!pieces.isEmpty(), "no pieces");
        h.succeed();
    }

    @GameTest(template = "empty", batch = "itemsPlayer6", timeoutTicks = 400)
    public static void longSwordCombo(GameTestHelper h) {
        ServerPlayer p = player(h, "duelist", new BlockPos(2, 2, 1));
        ItemStack sword = new ItemStack(CharItems.LONG_SWORD.get());
        hold(p, sword);
        Zombie z = zombie(h, new BlockPos(2, 2, 4));
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        look(p, z.position().add(0, 1, 0));
        // clicks just after each move ends: all four moves
        int[] at = {2, 14, 26, 42};
        for (int i = 0; i < at.length; i++) {
            int want = i;
            h.runAtTickTime(at[i], () -> {
                look(p, z.position().add(0, 1, 0));
                LongSwordMoves.press(p, z.getId());
                need(LongSwordMoves.stepOf(p) == want, "wanted move " + want + " but it's " + LongSwordMoves.stepOf(p));
            });
        }
        h.runAtTickTime(70, () -> {
            float taken = 1000 - z.getHealth();
            // 9 attack, so at least the first three moves' worth, and the sword wears down
            need(taken > 9f * (ComboRules.damage(0) + ComboRules.damage(1)), "only took " + taken);
            need(sword.getDamageValue() > 0, "the sword didn't wear");
            need(p.getCooldowns().isOnCooldown(sword.getItem()), "no wait after the finisher");
            z.discard();
            gone(h, p);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "itemsPlayer7", timeoutTicks = 200)
    public static void flameBurstBurnsAndFireGoesOut(GameTestHelper h) {
        ServerPlayer p = player(h, "smith", new BlockPos(2, 2, 2));
        hold(p, new ItemStack(CharItems.FLAME_HAMMER.get()));
        Zombie z = zombie(h, new BlockPos(5, 2, 2));
        look(p, z.position());
        float full = z.getHealth();
        Vec3 spot = p.position().add(1, 0, 0);
        FlameBurst.slam(p, spot, 7f, true);
        h.runAtTickTime(12, () -> {
            need(z.getHealth() < full, "the flames didn't hurt it");
            need(z.isOnFire(), "the flames didn't light it");
            need(p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem()), "no wait after a slam");
        });
        h.runAtTickTime(110, () -> {
            AABB box = new AABB(h.absolutePos(new BlockPos(-14, 0, -14)), h.absolutePos(new BlockPos(20, 6, 20)));
            boolean fire = BlockPos.betweenClosedStream(box).anyMatch(b -> h.getLevel().getBlockState(b).is(Blocks.FIRE));
            need(!fire, "fire should be gone by now");
            z.discard();
            gone(h, p);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "solgo", timeoutTicks = 600)
    public static void solgoFightsWithoutCrashing(GameTestHelper h) {
        ServerPlayer p = player(h, "prey", new BlockPos(2, 2, 2));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000);
        p.setHealth(5000);
        ServerLevel level = h.getLevel();
        Solgo s = Solgo.spawn(level, p.getX() + 14, p.getY() - 1, p.getZ(), false);
        need(s != null, "no Solgo");
        need(s.getMaxHealth() >= 300, "he should be a boss");
        for (int t = 20; t <= 500; t += 40) {
            int tick = t;
            h.runAtTickTime(tick, () -> {
                // the test player never turns by itself: half the time looking at him, half away
                Vec3 eye = s.position().add(0, 4, 0);
                look(p, tick % 80 < 40 ? eye : p.getEyePosition().add(p.getEyePosition().subtract(eye)));
            });
        }
        h.runAtTickTime(250, () -> s.hurt(p.damageSources().playerAttack(p), s.getMaxHealth() * 0.55f));
        h.runAtTickTime(255, () -> need(s.phaseTwo() || s.isDeadOrDying() || s.getHealth() > s.getMaxHealth() * 0.5f, "phase two never started"));
        h.runAtTickTime(500, () -> {
            need(s.isAlive(), "he died by himself");
            need(p.getHealth() < 5000, "he never landed a hit in 500 ticks");
            need(s.phaseTwo(), "should be in phase two");
            s.discard();
            gone(h, p);
            h.succeed();
        });
    }
}
