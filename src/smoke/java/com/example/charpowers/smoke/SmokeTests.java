package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.MidnightReflection;
import net.minecraftforge.event.TickEvent;
import com.example.charpowers.cloning.jobs.ChopJob;
import com.example.charpowers.cloning.jobs.MineJob;
import com.example.charpowers.armour.InvincibleArmour;
import com.example.charpowers.armour.SuitItems;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.miner.TheMiner;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.OwnPowers;
import com.example.charpowers.own.Pocket;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Headless checks of things unit tests can't see: only compiled for "gradlew runGameTestServer -Psmoke", never in the jar.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class SmokeTests {
    private static ServerPlayer player(GameTestHelper h, String name, BlockPos at) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p);
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        level.addNewPlayer(p);
        return p;
    }

    // A host clones can really belong to: logged in properly, on the player list.
    private static ServerPlayer loggedIn(GameTestHelper h, String name, BlockPos at) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        return p;
    }

    @GameTest(template = "empty")
    public static void everyHiddenPowerHasAPicture(GameTestHelper h) {
        int n = 0;
        for (String id : com.example.charpowers.hidden.HiddenPowers.ids()) {
            if (!com.example.charpowers.hidden.HiddenIcons.drawn(id)) throw new IllegalStateException(id + " has no picture");
            if (!(com.example.charpowers.hidden.HiddenIcons.icon(id) instanceof net.threetag.palladium.util.icon.TexturedIcon)) throw new IllegalStateException(id + " is not using its picture");
            if (com.example.charpowers.hidden.HiddenIcons.stack(id).getItem() != com.example.charpowers.icons.PowerIconItem.ICON.get()) throw new IllegalStateException(id + " has no icon stack");
            n++;
        }
        if (n < 381) throw new IllegalStateException("only " + n + " hidden powers");
        if (!com.example.charpowers.sound.PowerSounds.TIME_STOP.isPresent()) throw new IllegalStateException("the sounds did not register");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyPowerFileLoads(GameTestHelper h) {
        List<String> bad = new ArrayList<>();
        PowerManager powers = PowerManager.getInstance(h.getLevel());
        for (OwnPower own : OwnPowers.all()) {
            Power power = powers.getPower(own.id);
            if (power == null) bad.add("no power file loaded for " + own.id);
            else if (power.getAbilities().isEmpty()) bad.add(own.id + " has no abilities");
        }
        if (h.getLevel().getServer().getLevel(Pocket.LEVEL) == null) bad.add("the pocket dimension didn't load");
        if (!Blocks.STONE.defaultBlockState().is(TheMiner.NATURAL)) bad.add("the natural blocks tag is wrong");
        // the suit's own recipes need Gravestone, which isn't in this test: they should quietly stand aside
        if (h.getLevel().getRecipeManager().byKey(new ResourceLocation(CharPowers.MODID, "suit_module")).isEmpty()) bad.add("module recipe missing");
        if (!bad.isEmpty()) throw new GameTestAssertException(String.join("; ", bad));
        h.succeed();
    }

    // Gives every power to a player and runs its timer and every key, with nothing else set up. Nothing should crash.
    @GameTest(template = "empty", timeoutTicks = 800)
    public static void everyPowerRunsWithoutCrashing(GameTestHelper h) {
        ServerPlayer p = player(h, "tester", h.absolutePos(new BlockPos(1, 2, 1)));
        List<String> bad = new ArrayList<>();
        for (OwnPower own : OwnPowers.all()) {
            try {
                own.give(p);
                if (!own.owns(p)) bad.add(own.id + " wasn't given");
                own.everySecond(p);
                for (AbilityInstance a : AbilityUtil.getInstances(p, own.id)) {
                    own.onKey(p, a.getConfiguration().getId());
                }
                own.everySecond(p);
                Mastery.setLevel(p, own.id, 100);
                own.everySecond(p);
                for (AbilityInstance a : AbilityUtil.getInstances(p, own.id)) {
                    own.onKey(p, a.getConfiguration().getId());
                }
                own.awaken(p);
                own.everySecond(p);
                SuperpowerUtil.removeSuperpower(p, own.id);
                own.watchSecond(p);
            } catch (RuntimeException | LinkageError e) {
                e.printStackTrace();
                bad.add(own.id + ": " + e);
            }
        }
        if (!bad.isEmpty()) throw new GameTestAssertException(String.join("; ", bad));
        // give the queued work (jobs, rooms, orders) time to run too
        h.runAfterDelay(300, () -> {
            p.discard();
            h.succeed();
        });
    }

    private static void fill(ServerLevel level, BlockPos a, BlockPos b, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(a, b)) level.setBlock(pos, state, 2);
    }

    // Char's own setup: a 3x3 glass wall on the edge of some sand just above the water, a player staring at it at midnight.
    @GameTest(template = "empty", timeoutTicks = 1700)
    public static void staringAtGlassAtMidnightGivesCloning(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(2, 230, 482));
        fill(level, base.offset(-4, -1, -4), base.offset(4, -1, 2), Blocks.SAND.defaultBlockState());
        fill(level, base.offset(-4, -5, 3), base.offset(4, -1, 9), Blocks.WATER.defaultBlockState());
        fill(level, base.offset(-1, 0, 2), base.offset(1, 2, 2), Blocks.GLASS.defaultBlockState());
        level.setDayTime(18_000);
        ServerPlayer host = loggedIn(h, "reflector", base);
        if (SuperpowerUtil.hasSuperpower(host, Mastery.CLONING)) throw new GameTestAssertException("already had it");
        // a logged-in test player isn't ticked by its fake connection, so the once-a-second check is run by hand, 62 times
        for (int second = 1; second <= 62; second++) {
            int tick = second * 20;
            h.runAtTickTime(tick, () -> {
                host.tickCount = tick;
                MidnightReflection.onTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, host));
            });
        }
        h.runAtTickTime(1300, () -> {
            boolean got = SuperpowerUtil.hasSuperpower(host, Mastery.CLONING);
            host.discard();
            if (!got) throw new GameTestAssertException("stared for a minute at midnight and got nothing");
            h.succeed();
        });
    }

    private static CloneEntity cloneFor(ServerPlayer host, BlockPos at) {
        SuperpowerUtil.addSuperpower(host, Mastery.CLONING);
        CloneManager.pressSummon(host);
        List<CloneEntity> clones = new ArrayList<>(CloneManager.clonesOf(host.getUUID()));
        if (clones.isEmpty()) throw new GameTestAssertException("no clone came out");
        CloneEntity clone = clones.get(0);
        clone.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        clone.getInventory().add(new ItemStack(Items.DIAMOND_PICKAXE));
        return clone;
    }

    // A cave with a mouth on the surface and ores in its walls, and one ore hidden in the rock. The clone has to go in, take the
    // two it can see, and leave the one it can't.
    @GameTest(template = "empty", timeoutTicks = 3000)
    public static void aCloneGoesIntoACaveAndMinesWhatShows(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos top = h.absolutePos(new BlockPos(2, 230, 2));
        fill(level, top.offset(-14, -20, -14), top.offset(0, -1, 14), Blocks.STONE.defaultBlockState());
        fill(level, top.offset(3, -20, -14), top.offset(18, 3, 14), Blocks.STONE.defaultBlockState());
        fill(level, top.offset(1, -20, -14), top.offset(2, -1, 14), Blocks.STONE.defaultBlockState());
        for (int i = 0; i <= 6; i++) fill(level, top.offset(3 + i, -1 - i, 0), top.offset(3 + i, 1 - i, 0), Blocks.AIR.defaultBlockState());
        fill(level, top.offset(10, -8, -2), top.offset(14, -6, 2), Blocks.AIR.defaultBlockState());
        fill(level, top.offset(9, -7, 0), top.offset(9, -5, 0), Blocks.AIR.defaultBlockState());
        BlockPos coal = top.offset(15, -7, 0), diamond = top.offset(12, -9, 1), hidden = top.offset(12, -13, 0);
        level.setBlock(coal, Blocks.COAL_ORE.defaultBlockState(), 2);
        level.setBlock(diamond, Blocks.DIAMOND_ORE.defaultBlockState(), 2);
        level.setBlock(hidden, Blocks.IRON_ORE.defaultBlockState(), 2);
        ServerPlayer host = loggedIn(h, "caver", top.above(3));
        CloneEntity clone = cloneFor(host, top);
        clone.setJob(new MineJob());
        long started = level.getGameTime();
        h.succeedWhen(() -> {
            if (level.getBlockState(coal).is(Blocks.COAL_ORE) || level.getBlockState(diamond).is(Blocks.DIAMOND_ORE)) throw new GameTestAssertException("ores in the cave not mined yet");
            CharPowers.LOG.info("[Smoke] clone mined the cave ores in {} ticks, it's at {}", level.getGameTime() - started, clone.blockPosition().toShortString());
            boolean inCave = clone.getBlockY() < top.getY() - 4;
            boolean hiddenStill = level.getBlockState(hidden).is(Blocks.IRON_ORE);
            CloneManager.dismissAll(host);
            host.discard();
            if (!inCave) throw new GameTestAssertException("it didn't go down into the cave");
            if (!hiddenStill) throw new GameTestAssertException("it took the ore hidden in the rock");
        });
    }

    // Flat rock, nothing in sight: it strip mines. An ore touching its tunnel is taken, one 5 blocks inside the rock is not.
    @GameTest(template = "empty", timeoutTicks = 3600)
    public static void aCloneStripMinesAndIgnoresHiddenOre(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(62, 230, 2));
        fill(level, base.offset(-14, -4, -3), base.offset(14, 8, 30), Blocks.STONE.defaultBlockState());
        fill(level, base.offset(0, 0, 0), base.offset(0, 1, 0), Blocks.AIR.defaultBlockState());
        BlockPos seen = base.offset(0, -1, 8), hidden = base.offset(1, 6, 8);
        level.setBlock(seen, Blocks.IRON_ORE.defaultBlockState(), 2);
        level.setBlock(hidden, Blocks.DIAMOND_ORE.defaultBlockState(), 2);
        fill(level, base.offset(0, 4, -2), base.offset(0, 5, -2), Blocks.AIR.defaultBlockState()); // room for the host, or it suffocates
        ServerPlayer host = loggedIn(h, "striper", base.offset(0, 4, -2));
        CloneEntity clone = cloneFor(host, base);
        clone.setJob(new MineJob().startInStripMode());
        long started = level.getGameTime();
        h.succeedWhen(() -> {
            if (level.getBlockState(seen).is(Blocks.IRON_ORE)) throw new GameTestAssertException("the ore beside the tunnel isn't mined yet");
            CharPowers.LOG.info("[Smoke] clone strip mined the ore in {} ticks, it's at {}", level.getGameTime() - started, clone.blockPosition().toShortString());
            boolean hiddenStill = level.getBlockState(hidden).is(Blocks.DIAMOND_ORE);
            int dug = 0;
            for (int z = 1; z <= 8; z++) if (level.getBlockState(base.offset(0, 0, z)).isAir()) dug++;
            CloneManager.dismissAll(host);
            host.discard();
            if (!hiddenStill) throw new GameTestAssertException("it took the ore hidden in the rock");
            if (dug < 6) throw new GameTestAssertException("no proper main tunnel, only " + dug + " of 8 blocks dug");
        });
    }

    // Motherlode has to take every kind of ore, copper and other mods' too.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void motherlodeTakesEveryOre(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 122));
        fill(level, mid.offset(-6, -6, -6), mid.offset(6, -1, 6), Blocks.STONE.defaultBlockState());
        ServerPlayer p = loggedIn(h, "lode", mid);
        p.setNoGravity(false);
        List<BlockPos> spots = new ArrayList<>();
        int x = -5;
        for (ResourceLocation id : List.of(new ResourceLocation("copper_ore"), new ResourceLocation("deepslate_copper_ore"), new ResourceLocation("iron_ore"), new ResourceLocation("gold_ore"),
                new ResourceLocation("coal_ore"), new ResourceLocation("diamond_ore"), new ResourceLocation("palladium", "lead_ore"), new ResourceLocation("palladium", "titanium_ore"), new ResourceLocation("palladium", "vibranium_ore"))) {
            BlockPos at = mid.offset(x++, -3, 2);
            level.setBlock(at, net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id).defaultBlockState(), 2);
            spots.add(at);
        }
        TheMiner.POWER.give(p);
        TheMiner.POWER.onKey(p, "motherlode");
        h.runAfterDelay(60, () -> {
            List<String> left = new ArrayList<>();
            for (BlockPos at : spots) {
                BlockState state = level.getBlockState(at);
                if (!state.is(Blocks.STONE) && !state.is(Blocks.DEEPSLATE)) left.add(state.getBlock() + " tagged ore=" + state.is(net.minecraftforge.common.Tags.Blocks.ORES));
            }
            CharPowers.LOG.info("[Smoke] motherlode left: {} and gave: {}", left, p.getInventory().items.stream().filter(s -> !s.isEmpty()).map(ItemStack::toString).toList());
            p.discard();
            if (!left.isEmpty()) throw new GameTestAssertException("still there: " + left);
            h.succeed();
        });
    }

    // A clone walks to a tree and brings it all down, pillaring or climbing as it needs to.
    @GameTest(template = "empty", timeoutTicks = 3000)
    public static void aCloneChopsATree(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ground = h.absolutePos(new BlockPos(2, 230, 62));
        fill(level, ground.offset(-16, -1, -16), ground.offset(16, -1, 16), Blocks.GRASS_BLOCK.defaultBlockState());
        ServerPlayer host = loggedIn(h, "chopper", ground.above(3));
        BlockPos trunk = ground.offset(8, 0, 2);
        List<BlockPos> logs = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            logs.add(trunk.above(y));
            level.setBlock(trunk.above(y), Blocks.OAK_LOG.defaultBlockState(), 2);
        }
        fill(level, trunk.offset(-1, 7, -1), trunk.offset(1, 8, 1), Blocks.OAK_LEAVES.defaultBlockState());
        level.setBlock(trunk.above(7), Blocks.OAK_LOG.defaultBlockState(), 2);
        SuperpowerUtil.addSuperpower(host, Mastery.CLONING);
        CloneManager.pressSummon(host);
        CloneEntity clone = new ArrayList<>(CloneManager.clonesOf(host.getUUID())).get(0);
        clone.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        clone.getInventory().add(new ItemStack(Items.DIAMOND_AXE));
        clone.setJob(new ChopJob(ground));
        long started = level.getGameTime();
        h.onEachTick(() -> {
            if ((level.getGameTime() - started) % 200 == 0) {
                CharPowers.LOG.info("[Smoke] chop clone at {} removed={} alive={} hp={} job={} host={} floor={} trunk={}", clone.blockPosition().toShortString(), clone.isRemoved(), clone.isAlive(), clone.getHealth(),
                        clone.job == null ? "none" : clone.job.id(), CloneManager.host(clone) != null, level.getBlockState(ground.below()).getBlock(), level.getBlockState(trunk).getBlock());
            }
        });
        h.succeedWhen(() -> {
            for (BlockPos log : logs) {
                if (level.getBlockState(log).is(Blocks.OAK_LOG)) throw new GameTestAssertException("log still there at " + log.toShortString());
            }
            CharPowers.LOG.info("[Smoke] clone chopped the tree in {} ticks, it's at {}", level.getGameTime() - started, clone.blockPosition().toShortString());
            CloneManager.dismissAll(host);
            host.discard();
        });
    }

    // Green Lantern: everything comes out, the numbers come down, the hands connect, and the bubble keeps things out.
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void greenLanternWorks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 182));
        fill(level, mid.offset(-20, -2, -20), mid.offset(20, -1, 20), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "lantern", mid);
        p.setNoGravity(false);
        h.runAfterDelay(150, () -> lanternSteps(h, level, mid, p));
    }

    private static void lanternSteps(GameTestHelper h, ServerLevel level, BlockPos mid, ServerPlayer p) {
        CharPowers.LOG.info("[Smoke] lantern area ticking: {}", level.isPositionEntityTicking(mid));
        var power = com.example.charpowers.lantern.GreenLantern.POWER;
        power.give(p);
        if (power.battery(p) != 100) throw new GameTestAssertException("starts with " + power.battery(p));
        net.minecraft.world.entity.animal.Pig pig = new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG, level);
        pig.moveTo(mid.getX() + 0.5, mid.getY(), mid.getZ() + 5.5, 0f, 0f);
        pig.setNoAi(true);
        level.addFreshEntity(pig);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pig.getEyePosition());
        float full = pig.getHealth();
        net.minecraft.world.entity.animal.Cow[] cowHolder = new net.minecraft.world.entity.animal.Cow[1];
        power.onKey(p, "hand_slam");
        double afterSlam = power.battery(p);
        if (afterSlam != 95) throw new GameTestAssertException("a slam should cost 5, battery is " + afterSlam);
        h.runAfterDelay(25, () -> {
            CharPowers.LOG.info("[Smoke] pig after the slam: alive {} hp {} of {}", pig.isAlive(), pig.getHealth(), full);
            if (pig.getHealth() >= full && pig.isAlive()) throw new GameTestAssertException("the slam never hit the pig");
            power.onKey(p, "construct_sword");
            power.onClientAction(p, "swing");
            power.onKey(p, "rising_fist"); // refused: no other abilities while the sword's out
        });
        h.runAfterDelay(50, () -> {
            if (power.battery(p) != 85) throw new GameTestAssertException("sword cost wrong, battery is " + power.battery(p));
            power.onKey(p, "construct_sword");
            power.onKey(p, "minigun");
            power.onClientAction(p, "fire:1");
        });
        h.runAfterDelay(90, () -> {
            power.onClientAction(p, "fire:0");
            power.onKey(p, "minigun");
            power.onKey(p, "launch_swords");
        });
        h.runAfterDelay(120, () -> {
            power.onKeyUp(p, "launch_swords");
            power.onKey(p, "turret");
            power.onKey(p, "bubble");
        });
        h.runAfterDelay(170, () -> {
            if (com.example.charpowers.lantern.BubbleEntity.ALL.isEmpty()) throw new GameTestAssertException("no bubble");
            var bubble = com.example.charpowers.lantern.BubbleEntity.ALL.get(0);
            CharPowers.LOG.info("[Smoke] lantern battery now {}, bubble at {} radius {}", power.battery(p), bubble.blockPosition().toShortString(), bubble.radius());
            power.onKey(p, "bubble");
            power.data(p).putDouble("battery", 100);
            cowHolder[0] = new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW, level);
            cowHolder[0].moveTo(p.getX() + 3, p.getY(), p.getZ() + 3, 0f, 0f);
            cowHolder[0].setNoAi(true);
            level.addFreshEntity(cowHolder[0]);
            power.onKey(p, "nuke");
        });
        h.runAfterDelay(300, () -> {
            if (!com.example.charpowers.lantern.BubbleEntity.ALL.isEmpty()) throw new GameTestAssertException("bubble didn't pop");
            CharPowers.LOG.info("[Smoke] after everything: battery {} player hp {}", power.battery(p), p.getHealth());
            if (cowHolder[0].isAlive() && cowHolder[0].getHealth() >= cowHolder[0].getMaxHealth()) throw new GameTestAssertException("the nuke should hurt everyone");
            net.minecraft.world.level.Level l = level;
            int left = l.getEntitiesOfClass(com.example.charpowers.lantern.ConstructEntity.class, p.getBoundingBox().inflate(64)).size();
            CharPowers.LOG.info("[Smoke] constructs still out: {}", left);
            p.discard();
            h.succeed();
        });
    }

    // The Suit Off toggle exists as a key ability and Flight only unlocks while it is not enabled. (A test player has no live
    // Palladium abilities, so this reads the loaded power file; the live flip is for the real game.)
    @GameTest(template = "empty")
    public static void greenLanternSuitToggle(GameTestHelper h) {
        Power power = PowerManager.getInstance(h.getLevel()).getPower(com.example.charpowers.lantern.GreenLantern.POWER.id);
        if (power == null) throw new GameTestAssertException("no Green Lantern power file");
        var toggle = power.getAbilities().stream().filter(a -> a.getId().equals("suit_off")).findFirst().orElse(null);
        var flight = power.getAbilities().stream().filter(a -> a.getId().equals("gl_flight")).findFirst().orElse(null);
        if (toggle == null || flight == null) throw new GameTestAssertException("suit_off or gl_flight missing");
        if (toggle.getEnablingConditions().isEmpty()) throw new GameTestAssertException("suit_off has no key");
        if (flight.getUnlockingConditions().isEmpty()) throw new GameTestAssertException("flight does not depend on the suit");
        h.succeed();
    }

    // How you get it: hurt a Warden badly, walk away, and the ring comes for you.
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void greenLanternUnlocks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 242));
        fill(level, mid.offset(-40, -2, -6), mid.offset(40, -1, 6), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 3; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "brave", mid);
        p.setNoGravity(false);
        var power = com.example.charpowers.lantern.GreenLantern.POWER;
        var warden = new net.minecraft.world.entity.monster.warden.Warden(net.minecraft.world.entity.EntityType.WARDEN, level);
        warden.moveTo(mid.getX() + 2.5, mid.getY(), mid.getZ() + 0.5, 0f, 0f);
        warden.setNoAi(true);
        level.addFreshEntity(warden);
        warden.setHealth(15);
        warden.hurt(p.damageSources().playerAttack(p), 8);
        if (warden.getHealth() >= 10) throw new GameTestAssertException("warden's at " + warden.getHealth());
        h.runAfterDelay(10, () -> p.teleportTo(level, mid.getX() + 36.5, mid.getY(), mid.getZ() + 0.5, 0f, 0f));
        // a test player doesn't run its own timer, so this stands in for the check once a second
        h.runAfterDelay(20, () -> power.watchSecond(p));
        h.succeedWhen(() -> {
            if (!power.owns(p)) throw new GameTestAssertException("no ring yet");
            CharPowers.LOG.info("[Smoke] the ring came. Warden alive {}, lantern in bag {}", warden.isAlive(), p.getInventory().countItem(com.example.charpowers.armour.SuitItems.POWER_BATTERY.get()));
            if (p.getInventory().countItem(com.example.charpowers.armour.SuitItems.POWER_BATTERY.get()) != 1) throw new GameTestAssertException("no lantern");
            p.discard();
        });
    }

    // The quest: he comes at night, only constructs hurt him, he kneels instead of dying, and the oath over him awakens you.
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void sinestroQuest(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 302));
        fill(level, mid.offset(-20, -2, -20), mid.offset(20, -1, 20), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "oathtaker", mid);
        p.setNoGravity(false);
        var power = com.example.charpowers.lantern.GreenLantern.POWER;
        power.give(p);
        Mastery.setLevel(p, power.id, 100);
        com.example.charpowers.lantern.SinestroMob[] him = new com.example.charpowers.lantern.SinestroMob[1];
        boolean[] aiming = new boolean[1];
        h.onEachTick(() -> {
            if (aiming[0]) p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, him[0].getBoundingBox().getCenter());
        });
        h.runAfterDelay(140, () -> level.setDayTime(18000));
        h.runAfterDelay(150, () -> {
            power.everySecond(p);
            if (!power.inFight(p)) throw new GameTestAssertException("he never came");
            var found = level.getEntitiesOfClass(com.example.charpowers.lantern.SinestroMob.class, p.getBoundingBox().inflate(64));
            if (found.isEmpty()) throw new GameTestAssertException("no Sinestro to be seen");
            him[0] = found.get(0);
            float full = him[0].getHealth();
            him[0].hurt(p.damageSources().playerAttack(p), 20);
            if (him[0].getHealth() != full) throw new GameTestAssertException("a plain hit hurt him");
            CharPowers.LOG.info("[Smoke] Sinestro at {} hp {}", him[0].blockPosition().toShortString(), full);
        });
        h.runAfterDelay(300, () -> {
            CharPowers.LOG.info("[Smoke] Sinestro after 150 ticks is at {}", him[0].blockPosition().toShortString());
            him[0].setHealth(2);
            aiming[0] = true;
            power.onKey(p, "minigun");
            power.onClientAction(p, "fire:1");
        });
        h.runAfterDelay(360, () -> {
            aiming[0] = false;
            power.onClientAction(p, "fire:0");
            power.onKey(p, "minigun");
            if (!him[0].isBeaten()) throw new GameTestAssertException("he should be kneeling, he has hp " + him[0].getHealth());
            if (!him[0].isAlive()) throw new GameTestAssertException("he should kneel, not die");
            power.oathSaid(p);
            if (!power.awakened(p)) throw new GameTestAssertException("the oath didn't awaken you");
            if (him[0].isAlive()) throw new GameTestAssertException("he should be gone");
            power.onKey(p, "fist_field");
            power.onKey(p, "sword_rain");
        });
        h.runAfterDelay(430, () -> {
            CharPowers.LOG.info("[Smoke] Green Lantern awakened, battery {}", power.battery(p));
            p.discard();
            h.succeed();
        });
    }

    // Book Keeping: write a page, make a mess, die, throw things away. Restoring has to put it all back.
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void bookKeepingRewinds(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(62, 230, 2));
        fill(level, mid.offset(-10, -2, -10), mid.offset(10, -1, 10), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "keeper", mid);
        p.setNoGravity(false);
        var power = com.example.charpowers.book.BookKeeping.POWER;
        power.give(p);
        BlockPos floor = mid.offset(3, -1, 0), chestAt = mid.offset(-2, 0, 0), placed = mid.offset(0, 0, 3);
        net.minecraft.world.entity.animal.Pig pig = new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG, level);
        net.minecraft.world.entity.item.ItemEntity[] thrown = new net.minecraft.world.entity.item.ItemEntity[1];
        h.runAfterDelay(120, () -> {
            level.setBlock(chestAt, Blocks.CHEST.defaultBlockState(), 2);
            ((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestAt)).setItem(0, new ItemStack(Items.DIAMOND, 3));
            pig.moveTo(mid.getX() + 4.5, mid.getY(), mid.getZ() + 4.5, 0f, 0f);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            p.getInventory().add(new ItemStack(Items.APPLE, 5));
            p.setExperienceLevels(7);
            power.write(p, 0);

            // the mess
            p.gameMode.destroyBlock(floor);
            if (!level.getBlockState(floor).isAir()) throw new GameTestAssertException("the block didn't break");
            p.gameMode.useItemOn(p, level, ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(chestAt), net.minecraft.core.Direction.UP, chestAt, false));
            ((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestAt)).setItem(0, ItemStack.EMPTY);
            pig.hurt(p.damageSources().playerAttack(p), 100);
            if (pig.isAlive()) throw new GameTestAssertException("the pig lived");
            thrown[0] = p.drop(new ItemStack(Items.GOLD_INGOT), true);
            if (thrown[0] == null || !thrown[0].getItem().hasTag()) throw new GameTestAssertException("the thrown item has no mark");
            p.getInventory().clearContent();
            p.setExperienceLevels(0);
            p.teleportTo(level, mid.getX() + 6.5, mid.getY(), mid.getZ() + 6.5, 0f, 0f);
        });
        h.runAfterDelay(140, () -> {
            // dying: into the book, and back when you walk near
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 2));
            p.die(p.damageSources().generic());
            if (power.deathsHeld(p) != 1) throw new GameTestAssertException("the book didn't hold the death: " + power.deathsHeld(p));
            p.revive();
            p.setHealth(20);
            power.everySecond(p);
            if (power.deathsHeld(p) != 0 || p.getInventory().countItem(Items.IRON_INGOT) != 2) throw new GameTestAssertException("the death's items didn't come back");
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 1));
            p.die(p.damageSources().generic());
            p.revive();
            p.setHealth(20);
            power.restore(p, 0);
            if (power.deathsHeld(p) != 0) throw new GameTestAssertException("restoring should delete held deaths");
        });
        h.runAfterDelay(170, () -> {
            List<String> wrong = new ArrayList<>();
            if (!level.getBlockState(floor).is(Blocks.STONE)) wrong.add("floor is " + level.getBlockState(floor));
            if (((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestAt)).getItem(0).getCount() != 3) wrong.add("the chest's diamonds aren't back");
            if (p.getInventory().countItem(Items.APPLE) != 5) wrong.add("apples: " + p.getInventory().countItem(Items.APPLE));
            if (p.getInventory().countItem(Items.IRON_INGOT) != 0) wrong.add("iron from the future: " + p.getInventory().countItem(Items.IRON_INGOT));
            if (p.experienceLevel != 7) wrong.add("xp level " + p.experienceLevel);
            if (p.distanceToSqr(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5) > 2) wrong.add("didn't go back, at " + p.blockPosition().toShortString());
            if (thrown[0].isAlive()) wrong.add("the thrown item is still there");
            int pigs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, p.getBoundingBox().inflate(16), net.minecraft.world.entity.LivingEntity::isAlive).size();
            if (pigs != 1) wrong.add("pigs: " + pigs);
            CharPowers.LOG.info("[Smoke] book restored. Wrong: {}", wrong);
            if (!wrong.isEmpty()) throw new GameTestAssertException(String.join("; ", wrong));
            p.discard();
            h.succeed();
        });
    }

    // Commands and Slow Down have to visibly take hold of a mob, not only of other players.
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void commandsHoldMobs(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 602));
        fill(level, mid.offset(-8, -2, -8), mid.offset(8, -1, 8), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "ruler", mid);
        var zombie = new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
        var authority = com.example.charpowers.authority.SuperiorAuthority.POWER;
        var time = com.example.charpowers.timecontrol.TimeControl.POWER;
        h.runAfterDelay(100, () -> {
            zombie.moveTo(mid.getX() + 5.5, mid.getY(), mid.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(zombie);
            authority.give(p);
            time.give(p);
            authority.onKey(p, "stop");
        });
        h.runAfterDelay(110, () -> {
            if (!zombie.isNoAi()) throw new GameTestAssertException("Stop didn't take hold of the zombie");
        });
        h.runAfterDelay(120, () -> {
            time.onKey(p, "slow_down");
        });
        h.runAfterDelay(150, () -> {
            var speed = zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speed.getValue() >= speed.getBaseValue()) throw new GameTestAssertException("Slow Down didn't slow the zombie");
            if (!zombie.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN)) throw new GameTestAssertException("Slow Down has no effect on it");
            CharPowers.LOG.info("[Smoke] commands and slow hold a zombie: speed {} of {}", speed.getValue(), speed.getBaseValue());
            p.discard();
            h.succeed();
        });
    }

    // A gate opened on command: in, kill everything, through every door, beat the boss, get paid, come home.
    @GameTest(template = "empty", timeoutTicks = 1200, batch = "gates")
    public static void gateRunsFromStartToFinish(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 662));
        fill(level, mid.offset(-8, -2, -8), mid.offset(8, -1, 8), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "hunter", mid);
        var power = com.example.charpowers.game.GamePlayer.POWER;
        int[] stage = {0}, rooms = {0};
        h.runAfterDelay(100, () -> {
            power.opButton(p);
            power.give(p);
            power.onClientAction(p, "gate:0");
            var gates = power.data(p).getList("gates", net.minecraft.nbt.Tag.TAG_COMPOUND);
            if (gates.isEmpty()) throw new GameTestAssertException("no gate opened");
            BlockPos gate = BlockPos.of(gates.getCompound(gates.size() - 1).getLong("pos"));
            p.teleportTo(level, gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5, 0f, 0f);
            power.everySecond(p);
            if (!com.example.charpowers.own.Pocket.in(p)) throw new GameTestAssertException("the gate didn't take you in");
            power.advanceGates(); // the first room's mobs
        });
        h.runAfterDelay(160, () -> {
            ServerLevel pocket = p.serverLevel();
            var around = pocket.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(32));
            CharPowers.LOG.info("[Smoke] gate room 1 has {} mobs", around.size());
            if (around.isEmpty()) throw new GameTestAssertException("the room's empty");
            around.forEach(net.minecraft.world.entity.Mob::discard);
            power.advanceGates(); // door opens
            // walk through: rank E has 2 rooms, so the next is the boss
            power.gateJumpToRoom(p, 1);
            power.advanceGates();
            power.advanceGates();
            var boss = pocket.getEntitiesOfClass(com.example.charpowers.game.mob.GateBoss.class, p.getBoundingBox().inflate(40));
            // the boss wakes once you're a few steps into its room, and boss rooms are different sizes for each kind of gate
            for (int step = 0; step < 4 && boss.isEmpty(); step++) {
                p.teleportTo(pocket, p.getX() + 3, p.getY(), p.getZ(), -90f, 0f);
                power.advanceGates();
                boss = pocket.getEntitiesOfClass(com.example.charpowers.game.mob.GateBoss.class, p.getBoundingBox().inflate(40));
            }
            if (boss.isEmpty()) throw new GameTestAssertException("no boss in the last room");
            int coins = power.data(p).getInt("coins");
            boss.get(0).setHealth(1);
            boss.get(0).hurt(p.damageSources().playerAttack(p), 50);
            if (power.data(p).getInt("coins") <= coins) throw new GameTestAssertException("the gate didn't pay");
            for (int i = 0; i < 10; i++) power.advanceGates();
            if (com.example.charpowers.own.Pocket.in(p)) throw new GameTestAssertException("you should be home");
            CharPowers.LOG.info("[Smoke] gate cleared: {} coins, {} points", power.data(p).getInt("coins"), power.data(p).getInt("points"));
            power.opButton(p);
            p.discard();
            h.succeed();
        });
    }

    // Mobs really use the powers written for players, none of them crashes on a mob, and each kind of move does something.
    @GameTest(template = "empty", timeoutTicks = 200, batch = "mobBrain")
    public static void mobsUseCarriedPowers(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 782));
        fill(level, mid.offset(-16, -2, -16), mid.offset(16, -1, 16), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer prey = loggedIn(h, "prey", mid.offset(6, 0, 0));
        prey.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.runAfterDelay(20, () -> {
            int carried = 0;
            for (var def : com.example.charpowers.hidden.HiddenPowers.all()) {
                if (def.mobRule() != com.example.charpowers.hidden.model.MobRule.CARRY) continue;
                carried++;
                if (!com.example.charpowers.hidden.model.MobMoves.known(def.id())) throw new GameTestAssertException("no move for " + def.id());
                var z = zombieWith(level, mid, def.id());
                z.setTarget(prey);
                z.setHealth(5);
                z.setOnGround(true);
                com.example.charpowers.hidden.MobBrain.act(z, userOf(z, def.id()), def.id());
                z.discard();
            }
            if (carried != com.example.charpowers.hidden.model.MobMoves.count()) throw new GameTestAssertException("the list of moves has " + com.example.charpowers.hidden.model.MobMoves.count() + " but " + carried + " powers are carried");

            var healer = zombieWith(level, mid, "bread_lover");
            healer.setHealth(5);
            com.example.charpowers.hidden.MobBrain.act(healer, userOf(healer, "bread_lover"), "bread_lover");
            if (healer.getHealth() <= 5) throw new GameTestAssertException("the healer didn't heal");
            healer.discard();

            var leaper = zombieWith(level, mid, "frog_legs");
            leaper.setTarget(prey);
            leaper.setOnGround(true);
            leaper.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            com.example.charpowers.hidden.MobBrain.act(leaper, userOf(leaper, "frog_legs"), "frog_legs");
            if (leaper.getDeltaMovement().y <= 0) throw new GameTestAssertException("the leaper didn't leap");
            leaper.discard();

            var thrower = zombieWith(level, mid, "pet_rock");
            thrower.setTarget(prey);
            com.example.charpowers.hidden.MobBrain.act(thrower, userOf(thrower, "pet_rock"), "pet_rock");
            if (level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Snowball.class, thrower.getBoundingBox().inflate(3)).isEmpty()) throw new GameTestAssertException("nothing was thrown");
            thrower.discard();

            var senser = zombieWith(level, mid, "echolocation");
            senser.setTarget(null);
            com.example.charpowers.hidden.MobBrain.act(senser, userOf(senser, "echolocation"), "echolocation");
            if (senser.getTarget() != prey) throw new GameTestAssertException("it didn't sense the player, target " + senser.getTarget());
            senser.discard();

            var caller = zombieWith(level, mid, "taunt");
            var friend = zombieWith(level, mid.offset(2, 0, 0), "");
            caller.setTarget(prey);
            friend.setTarget(null);
            com.example.charpowers.hidden.MobBrain.act(caller, userOf(caller, "taunt"), "taunt");
            if (friend.getTarget() != prey) throw new GameTestAssertException("the friend wasn't called");
            caller.discard();
            friend.discard();

            var thief = zombieWith(level, mid.offset(5, 0, 0), "pickpocket");
            prey.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            thief.setTarget(prey);
            com.example.charpowers.hidden.MobBrain.act(thief, userOf(thief, "pickpocket"), "pickpocket");
            if (!prey.getMainHandItem().isEmpty()) throw new GameTestAssertException("it didn't knock the item out");
            thief.discard();
            CharPowers.LOG.info("[Smoke] {} carried powers all have a move", carried);
            prey.discard();
            h.succeed();
        });
    }

    private static net.minecraft.world.entity.monster.Zombie zombieWith(ServerLevel level, BlockPos at, String power) {
        var z = new net.minecraft.world.entity.monster.Zombie(level);
        z.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(z);
        com.example.charpowers.hidden.MobPowers.set(z, power.isEmpty() ? null : power);
        return z;
    }

    private static com.example.charpowers.hidden.fx.PowerUser userOf(net.minecraft.world.entity.Mob m, String power) {
        return new com.example.charpowers.hidden.fx.PowerUser(m, null, com.example.charpowers.hidden.MobPowers.state(m, power));
    }

    // Spawner mobs never get a power, wild ones still can, and a crowd is much rarer.
    @GameTest(template = "empty", timeoutTicks = 200, batch = "farms")
    public static void farmMobsAreLeftAlone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 842));
        fill(level, mid.offset(-20, -2, -20), mid.offset(20, -1, 20), Blocks.STONE.defaultBlockState());
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        h.runAfterDelay(20, () -> {
            boolean was = com.example.charpowers.hidden.HiddenConfig.HIDDEN_POWERS_ENABLED.get();
            com.example.charpowers.hidden.HiddenConfig.HIDDEN_POWERS_ENABLED.set(true);
            int fromSpawner = spawnAndCount(level, mid, net.minecraft.world.entity.MobSpawnType.SPAWNER, 800);
            int wild = spawnAndCount(level, mid, net.minecraft.world.entity.MobSpawnType.NATURAL, 800);
            java.util.List<net.minecraft.world.entity.monster.Zombie> crowd = new java.util.ArrayList<>();
            for (int i = 0; i < 12; i++) crowd.add(zombieWith(level, mid.offset(i % 4, 0, i / 4), ""));
            int crowded = spawnAndCount(level, mid, net.minecraft.world.entity.MobSpawnType.NATURAL, 800);
            crowd.forEach(net.minecraft.world.entity.monster.Zombie::discard);
            com.example.charpowers.hidden.HiddenConfig.HIDDEN_POWERS_ENABLED.set(was);
            CharPowers.LOG.info("[Smoke] powers out of 800: spawner {}, wild {}, crowd {}", fromSpawner, wild, crowded);
            if (fromSpawner != 0) throw new GameTestAssertException("spawner mobs got " + fromSpawner + " powers");
            if (wild == 0) throw new GameTestAssertException("wild mobs got no powers at all");
            if (crowded >= wild) throw new GameTestAssertException("a crowd wasn't rarer: " + crowded + " against " + wild);
            h.succeed();
        });
    }

    private static int spawnAndCount(ServerLevel level, BlockPos at, net.minecraft.world.entity.MobSpawnType how, int times) {
        int powers = 0;
        for (int i = 0; i < times; i++) {
            var z = net.minecraft.world.entity.EntityType.ZOMBIE.spawn(level, at.offset(10, 0, 0), how);
            if (z == null) throw new GameTestAssertException("no zombie spawned");
            if (com.example.charpowers.hidden.MobPowers.get(z).isPresent()) powers++;
            z.discard();
        }
        return powers;
    }

    // The Player's weapons: only theirs.
    @GameTest(template = "empty", timeoutTicks = 100, batch = "weapons")
    public static void weaponsAreOnlyForThePlayer(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer owner = loggedIn(h, "wielder", h.absolutePos(new BlockPos(2, 230, 722)));
        ServerPlayer other = loggedIn(h, "stranger", h.absolutePos(new BlockPos(6, 230, 722)));
        var power = com.example.charpowers.game.GamePlayer.POWER;
        power.opButton(owner);
        power.give(owner);
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(CharPowers.MODID, "crimson_edge"));
        if (!(item instanceof com.example.charpowers.game.SystemWeapon)) throw new GameTestAssertException("the weapon isn't there");
        // Punchy goes by these tags for its animations
        var get = (java.util.function.Function<String, ItemStack>) id -> new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(CharPowers.MODID, id)));
        if (!new ItemStack(item).is(net.minecraft.tags.ItemTags.SWORDS)) throw new GameTestAssertException("Crimson Edge isn't a sword");
        if (!get.apply("cocytus_hammer").is(net.minecraft.tags.ItemTags.AXES)) throw new GameTestAssertException("the hammer has no heavy swing");
        if (!get.apply("grand_spear").is(net.minecraft.tags.ItemTags.create(new ResourceLocation("minecraft", "spears")))) throw new GameTestAssertException("the spear isn't a spear");
        var pig = new net.minecraft.world.entity.animal.Pig(net.minecraft.world.entity.EntityType.PIG, level);
        pig.moveTo(owner.getX() + 1, owner.getY(), owner.getZ(), 0f, 0f);
        level.addFreshEntity(pig);
        other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(item));
        float full = pig.getHealth();
        pig.hurt(other.damageSources().playerAttack(other), 8);
        if (pig.getHealth() < full - 1) throw new GameTestAssertException("it worked in someone else's hands: " + pig.getHealth());
        power.opButton(owner);
        owner.discard();
        other.discard();
        h.succeed();
    }

    // The syringe: the power comes out and stays in it, then goes back in. Clones go with the power.
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void syringeStoresAPower(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = loggedIn(h, "needle", h.absolutePos(new BlockPos(2, 230, 542)));
        var power = OwnPowers.get(Mastery.CLONING);
        power.give(p);
        ItemStack syringe = new ItemStack(com.example.charpowers.armour.SuitItems.POWER_SYRINGE.get());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, syringe);
        syringe.use(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (power.owns(p)) throw new GameTestAssertException("the power should be gone");
        if (!com.example.charpowers.own.PowerSyringe.filled(syringe)) throw new GameTestAssertException("the syringe should hold it");
        syringe.use(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!power.owns(p)) throw new GameTestAssertException("the power should be back");
        if (com.example.charpowers.own.PowerSyringe.filled(syringe)) throw new GameTestAssertException("the syringe should be empty");
        p.discard();
        h.succeed();
    }

    // The op menu: it does what it says, and nothing at all for someone who isn't an op.
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void opMenuWorks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = loggedIn(h, "boss", h.absolutePos(new BlockPos(2, 230, 482)));
        var teacher = com.example.charpowers.teacher.Teacher.POWER;
        com.example.charpowers.own.OpMenu.action(p, "flip:teacher");
        if (!com.example.charpowers.own.Setting.enabled(teacher.id)) throw new GameTestAssertException("a non-op switched a power off");
        // the test server gives ops permission level 0, so this is a real level 4 op
        level.getServer().getPlayerList().getOps().add(new net.minecraft.server.players.ServerOpListEntry(p.getGameProfile(), 4, false));
        com.example.charpowers.own.OpMenu.action(p, "give:teacher");
        if (!teacher.owns(p)) throw new GameTestAssertException("the menu's Give didn't work");
        com.example.charpowers.own.OpMenu.action(p, "flip:teacher");
        if (com.example.charpowers.own.Setting.enabled(teacher.id) || teacher.owns(p)) throw new GameTestAssertException("switching it off didn't work");
        com.example.charpowers.own.OpMenu.action(p, "flip:teacher");
        double before = com.example.charpowers.own.Setting.LANTERN_DAMAGE.d();
        com.example.charpowers.own.OpMenu.action(p, "add:green_lantern:damage:50");
        if (com.example.charpowers.own.Setting.LANTERN_DAMAGE.d() != before + 50) throw new GameTestAssertException("the number didn't change");
        com.example.charpowers.own.OpMenu.action(p, "reset:green_lantern");
        com.example.charpowers.own.OpMenu.action(p, "general:attack_scale:0.5");
        com.example.charpowers.own.OpMenu.action(p, "general:attack_scale:-0.5");
        com.example.charpowers.own.OpMenu.action(p, "level:teacher:5");
        com.example.charpowers.own.OpMenu.open(p);
        level.getServer().getPlayerList().deop(p.getGameProfile());
        p.discard();
        h.succeed();
    }

    // Game Player, from the altar to the shop.
    @GameTest(template = "empty", timeoutTicks = 900, batch = "gamePlayer")
    public static void gamePlayerWorks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 422));
        fill(level, mid.offset(-16, -3, -16), mid.offset(16, -1, 16), Blocks.STONE.defaultBlockState());
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
        ServerPlayer p = loggedIn(h, "gamer", mid);
        p.setNoGravity(false);
        var power = com.example.charpowers.game.GamePlayer.POWER;
        Object[] boss = new Object[1];
        h.runAfterDelay(120, () -> {
            power.opButton(p); // only one Player in a world, and another test may have been it
            // the gate only you can see, the room behind it, and the altar
            power.trigger(p);
            if (!power.data(p).contains("gate")) throw new GameTestAssertException("no gate");
            BlockPos gate = BlockPos.of(power.data(p).getLong("gate"));
            if (!level.getBlockState(gate).isAir()) throw new GameTestAssertException("a gate shouldn't change the world");
            p.teleportTo(level, gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5, 0f, 0f);
            power.watchSecond(p);
            if (!com.example.charpowers.own.Pocket.in(p)) throw new GameTestAssertException("the gate didn't take you anywhere");
            BlockPos on = BlockPos.of(power.data(p).getLong("altar"));
            if (!p.serverLevel().getBlockState(on).is(Blocks.LAPIS_BLOCK)) throw new GameTestAssertException("the altar is " + p.serverLevel().getBlockState(on));
            p.teleportTo(p.serverLevel(), on.getX() + 0.5, on.getY() + 1, on.getZ() + 0.5, 0f, 0f);
            p.setShiftKeyDown(true);
            power.watchSecond(p);
            if (power.owns(p) || p.getHealth() >= 20) throw new GameTestAssertException("the altar should take your life first");
            for (int i = 0; i < 12 && !power.owns(p); i++) power.watchSecond(p);
            if (!power.owns(p)) throw new GameTestAssertException("the altar didn't work");
            if (com.example.charpowers.own.Pocket.in(p)) throw new GameTestAssertException("you should be back in the world");
            p.setShiftKeyDown(false);
            power.everySecond(p);
            p.teleportTo(level, mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0f, 0f);

            // only one Player: nobody else's game even rolls now
            var second = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "second"));
            power.watchSecond(second);

            // points buy stats
            power.trigger(p);
            double damage = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            power.onClientAction(p, "stat:0");
            if (p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) <= damage) throw new GameTestAssertException("strength did nothing");
            if (power.data(p).getInt("points") != 95) throw new GameTestAssertException("the first point should cost 5");

            // the first story quest: break 20 blocks
            for (int i = 0; i < 20; i++) p.gameMode.destroyBlock(mid.offset(i % 5 + 2, -1, i / 5 + 2));
            if (power.data(p).getInt("story") != 1) throw new GameTestAssertException("the story didn't move on, it's at " + power.data(p).getInt("story"));

            // Storage keeps what you leave in it
            power.onClientAction(p, "storage");
            if (!(p.containerMenu instanceof net.minecraft.world.inventory.ChestMenu)) throw new GameTestAssertException("storage didn't open");
            p.containerMenu.getSlot(10).set(new ItemStack(Items.EMERALD, 7));
            p.closeContainer();
            power.onClientAction(p, "storage");
            if (p.containerMenu.getSlot(10).getItem().getCount() != 7) throw new GameTestAssertException("storage lost its items");
            p.closeContainer();

            // skills
            power.onClientAction(p, "skill:0");
            power.onKey(p, "gp_dash");
            power.send(p, true);

            // the boss only you can see, behind its own gate
            Mastery.setLevel(p, power.id, 100);
            power.trigger(p);
            BlockPos last = BlockPos.of(power.data(p).getLong("gate"));
            p.teleportTo(level, last.getX() + 0.5, last.getY(), last.getZ() + 0.5, 0f, 0f);
            power.everySecond(p);
            if (!com.example.charpowers.own.Pocket.in(p)) throw new GameTestAssertException("the boss gate didn't take you anywhere");
            var mobNow = power.bossOf(p);
            if (mobNow == null) throw new GameTestAssertException("no boss came");
            boss[0] = mobNow;
            var other = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "bystander"));
            if (mobNow.broadcastToPlayer(other)) throw new GameTestAssertException("someone else could see it");
            if (!mobNow.broadcastToPlayer(p)) throw new GameTestAssertException("you can't see it");
            if (mobNow.hurt(other.damageSources().playerAttack(other), 50)) throw new GameTestAssertException("someone else hurt it");
        });
        h.runAfterDelay(140, () -> {
            var mob = (com.example.charpowers.game.GameBossMob) boss[0];
            mob.setHealth(1);
            mob.invulnerableTime = 0;
            mob.hurt(p.damageSources().playerAttack(p), 50);
            if (!power.awakened(p)) throw new GameTestAssertException("beating it should awaken you, it has hp " + mob.getHealth());
            for (int i = 0; i < 4; i++) power.everySecond(p);
            if (com.example.charpowers.own.Pocket.in(p)) throw new GameTestAssertException("you should be back in the world after the boss");

            // the shop
            power.data(p).putInt("coins", 100);
            power.onClientAction(p, "shop:7");
            if (p.getInventory().countItem(Items.DIAMOND) != 1 || power.data(p).getInt("coins") != 40) throw new GameTestAssertException("the shop didn't sell a diamond");
            CharPowers.LOG.info("[Smoke] Game Player all the way: points {}, coins {}", power.data(p).getInt("points"), power.data(p).getInt("coins"));
            power.opButton(p);
            p.discard();
            h.succeed();
        });
    }

    // The Miner: stairs leave dirt on the ground (not in the bag) and the ore still reaches you.
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void minerLeavesJunkOnTheGround(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 160));
        fill(level, mid.offset(-4, -6, -2), mid.offset(4, 3, 8), Blocks.DIRT.defaultBlockState());
        level.setBlock(mid.offset(0, -1, 1), Blocks.DIAMOND_ORE.defaultBlockState(), 2);
        ServerPlayer p = loggedIn(h, "junk", mid);
        p.setNoGravity(false);
        TheMiner.POWER.give(p);
        TheMiner.POWER.onKey(p, "miner_stairs");
        h.runAfterDelay(120, () -> {
            int dirtInBag = p.getInventory().countItem(Items.DIRT);
            int diamonds = p.getInventory().countItem(Items.DIAMOND);
            int onGround = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(mid).inflate(40),
                    e -> e.getItem().is(Items.DIRT)).stream().mapToInt(e -> e.getItem().getCount()).sum();
            CharPowers.LOG.info("[Smoke] miner stairs: dirt in bag {}, dirt on ground {}, diamonds {}", dirtInBag, onGround, diamonds);
            p.discard();
            if (dirtInBag != 0) throw new GameTestAssertException("dirt went into the bag: " + dirtInBag);
            if (onGround == 0) throw new GameTestAssertException("no dirt left on the ground");
            if (diamonds == 0) throw new GameTestAssertException("the diamond did not reach the bag");
            h.succeed();
        });
    }

    // Motherlode: the rarest ore comes first even though a common one is nearer, and water and lava beside them are gone.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void motherlodeGoesByRarityAndClearsFluids(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 200));
        fill(level, mid.offset(-8, -8, -8), mid.offset(8, -1, 8), Blocks.STONE.defaultBlockState());
        level.setBlock(mid.offset(1, -3, 0), Blocks.COPPER_ORE.defaultBlockState(), 2);
        level.setBlock(mid.offset(6, -3, 0), Blocks.DIAMOND_ORE.defaultBlockState(), 2);
        level.setBlock(mid.offset(6, -3, 1), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(mid.offset(6, -4, 0), Blocks.LAVA.defaultBlockState(), 2);
        ServerPlayer p = loggedIn(h, "rare", mid);
        p.setNoGravity(false);
        TheMiner.POWER.give(p);
        TheMiner.POWER.onKey(p, "motherlode");
        h.runAfterDelay(60, () -> {
            ItemStack first = p.getInventory().items.stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
            boolean water = level.getBlockState(mid.offset(6, -3, 1)).is(Blocks.WATER), lava = level.getBlockState(mid.offset(6, -4, 0)).is(Blocks.LAVA);
            CharPowers.LOG.info("[Smoke] motherlode first item {}, water left {}, lava left {}", first, water, lava);
            p.discard();
            if (!first.is(Items.DIAMOND)) throw new GameTestAssertException("first item was " + first);
            if (water || lava) throw new GameTestAssertException("fluids still there: water " + water + " lava " + lava);
            h.succeed();
        });
    }

    // Vocal Expression's awakened words really do things.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void awakenedWordsDoWhatTheySay(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 240));
        fill(level, mid.offset(-8, -3, -8), mid.offset(8, -1, 8), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlock(mid.offset(5, -1, 5), Blocks.WATER.defaultBlockState(), 2);
        ServerPlayer p = loggedIn(h, "speaker", mid);
        net.minecraft.world.entity.animal.Pig pig = net.minecraft.world.entity.EntityType.PIG.create(level);
        pig.moveTo(mid.getX() + 3.5, mid.getY(), mid.getZ() + 0.5);
        level.addFreshEntity(pig);
        net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5);
        p.setHealth(4f);
        com.example.charpowers.vocal.TrueWords.run(p, "HEAL", level, null, at);
        com.example.charpowers.vocal.TrueWords.run(p, "BLOOM", level, null, at);
        com.example.charpowers.vocal.TrueWords.run(p, "FREEZE", level, null, new net.minecraft.world.phys.Vec3(mid.getX() + 5.5, mid.getY() - 1, mid.getZ() + 5.5));
        com.example.charpowers.vocal.TrueWords.run(p, "JUMP", level, null, pig.position());
        com.example.charpowers.vocal.TrueWords.run(p, "PULL", level, null, at);
        h.runAfterDelay(5, () -> {
            boolean flower = false;
            for (BlockPos pos : BlockPos.betweenClosed(mid.offset(-3, 0, -3), mid.offset(3, 1, 3))) flower |= level.getBlockState(pos).is(net.minecraft.tags.BlockTags.SMALL_FLOWERS);
            boolean ice = level.getBlockState(mid.offset(5, -1, 5)).is(Blocks.ICE);
            CharPowers.LOG.info("[Smoke] words: health {}, flower {}, ice {}, pig moved {}", p.getHealth(), flower, ice, pig.getDeltaMovement());
            float health = p.getHealth();
            p.discard();
            pig.discard();
            if (health <= 4f) throw new GameTestAssertException("HEAL did nothing");
            if (!flower) throw new GameTestAssertException("BLOOM grew no flowers");
            if (!ice) throw new GameTestAssertException("FREEZE left the water");
            h.succeed();
        });
    }

    // Life Enjoyer, awakened: holding the key and breaking a block takes a square of blocks, and a whole tree in shapeless mode.
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void ultimineBreaksTheShape(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos mid = h.absolutePos(new BlockPos(2, 230, 280));
        fill(level, mid.offset(-6, -2, -6), mid.offset(6, -1, 6), Blocks.STONE.defaultBlockState());
        for (int y = 0; y < 6; y++) level.setBlock(mid.offset(4, y, 4), Blocks.OAK_LOG.defaultBlockState(), 2);
        level.setBlock(mid.offset(5, 2, 4), Blocks.OAK_LOG.defaultBlockState(), 2);
        ServerPlayer p = loggedIn(h, "ultimate", mid.offset(0, 0, -4));
        p.setNoGravity(false);
        com.example.charpowers.life.LifeEnjoyer.POWER.give(p);
        Mastery.setAwakened(p, Mastery.LIFE_ENJOYER);
        p.getInventory().items.set(0, new ItemStack(Items.DIAMOND_PICKAXE));
        p.getInventory().selected = 0;
        com.example.charpowers.life.ultimine.UltimineServer.hold(p, true, com.example.charpowers.life.model.Ultimine.Shape.SMALL_SQUARE.ordinal());
        p.gameMode.destroyBlock(mid.below());
        p.getInventory().items.set(0, new ItemStack(Items.DIAMOND_AXE));
        com.example.charpowers.life.ultimine.UltimineServer.hold(p, true, com.example.charpowers.life.model.Ultimine.Shape.SHAPELESS.ordinal());
        p.gameMode.destroyBlock(mid.offset(4, 0, 4));
        h.runAfterDelay(10, () -> {
            int gone = 0;
            for (BlockPos pos : BlockPos.betweenClosed(mid.offset(-1, -1, -1), mid.offset(1, -1, 1))) if (level.getBlockState(pos).isAir()) gone++;
            int logs = 0;
            for (BlockPos pos : BlockPos.betweenClosed(mid.offset(3, 0, 3), mid.offset(5, 5, 5))) if (level.getBlockState(pos).is(Blocks.OAK_LOG)) logs++;
            int near = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(2.5)).size();
            CharPowers.LOG.info("[Smoke] ultimine: square gone {} of 9, logs left {}, drops at feet {}, axe damage {}", gone, logs, near, p.getMainHandItem().getDamageValue());
            p.discard();
            if (gone != 9) throw new GameTestAssertException("square: " + gone + " of 9 broke");
            if (logs != 0) throw new GameTestAssertException(logs + " logs left");
            if (near == 0) throw new GameTestAssertException("no drops at your feet");
            h.succeed();
        });
    }

    // Mob Control pets: every common hostile, kept as a pet, leaves its owner alone (even when its brain is told to hunt them),
    // and then a warden and a zombie fight what they're told to.
    @GameTest(template = "empty", timeoutTicks = 500, batch = "pets")
    public static void petsLeaveTheirOwnerAlone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(new BlockPos(2, 230, 962));
        for (int cx = -2; cx <= 2; cx++) for (int cz = -2; cz <= 2; cz++) level.setChunkForced((base.getX() >> 4) + cx, (base.getZ() >> 4) + cz, true); // or the pets unload
        fill(level, base.offset(-12, -1, -12), base.offset(12, -1, 12), Blocks.STONE.defaultBlockState());
        ServerPlayer owner = loggedIn(h, "keeper", base);
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var types = List.of(net.minecraft.world.entity.EntityType.ZOMBIE, net.minecraft.world.entity.EntityType.SKELETON, net.minecraft.world.entity.EntityType.CREEPER,
                net.minecraft.world.entity.EntityType.SPIDER, net.minecraft.world.entity.EntityType.PIGLIN, net.minecraft.world.entity.EntityType.PIGLIN_BRUTE,
                net.minecraft.world.entity.EntityType.HOGLIN, net.minecraft.world.entity.EntityType.RAVAGER, net.minecraft.world.entity.EntityType.WITHER_SKELETON,
                net.minecraft.world.entity.EntityType.ENDERMAN, net.minecraft.world.entity.EntityType.PHANTOM, net.minecraft.world.entity.EntityType.GUARDIAN,
                net.minecraft.world.entity.EntityType.GHAST, net.minecraft.world.entity.EntityType.BLAZE, net.minecraft.world.entity.EntityType.SLIME,
                net.minecraft.world.entity.EntityType.SHULKER, net.minecraft.world.entity.EntityType.WARDEN);
        List<net.minecraft.world.entity.Mob> pets = new ArrayList<>();
        int n = 0;
        for (var type : types) {
            var mob = (net.minecraft.world.entity.Mob) type.create(level);
            double angle = n++ * Math.PI * 2 / types.size();
            mob.moveTo(base.getX() + 0.5 + Math.cos(angle) * 4, base.getY(), base.getZ() + 0.5 + Math.sin(angle) * 4, 0f, 0f);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
            com.example.charpowers.own.Minions.claim(mob, owner, "pet");
            com.example.charpowers.own.Minions.settle(mob);
            com.example.charpowers.mobcontrol.PetMinds.adopt(mob);
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 100000)); // the sun would burn the undead
            mob.setInvulnerable(true); // fish out of water, burning sun and so on are not what is being tested
            pets.add(mob);
        }
        // now tell every one of them to hunt the owner, the way the game itself would
        for (var mob : pets) {
            mob.setTarget(owner);
            if (mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin || mob instanceof net.minecraft.world.entity.monster.hoglin.Hoglin) mob.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, owner);
            if (mob instanceof net.minecraft.world.entity.monster.warden.Warden w) {
                w.increaseAngerAt(owner, 200, false);
                w.setAttackTarget(owner);
            }
        }
        h.runAfterDelay(120, () -> {
            for (var mob : pets) {
                String what = mob.getType().toString();
                if (!mob.isAlive()) throw new GameTestAssertException(what + " is gone: " + mob.getRemovalReason() + " health " + mob.getHealth() + " last damage " + mob.getLastDamageSource() + " at " + mob.blockPosition() + " owner at " + owner.blockPosition() + " pose " + mob.getPose() + " age " + mob.tickCount + " activities " + mob.getBrain().getActiveActivities());
                if (mob.getTarget() == owner) throw new GameTestAssertException(what + " still targets its owner");
                if ((mob instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin || mob instanceof net.minecraft.world.entity.monster.hoglin.Hoglin || mob instanceof net.minecraft.world.entity.monster.warden.Warden)
                        && mob.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).filter(t -> t == owner).isPresent())
                    throw new GameTestAssertException(what + " still has its owner in its attack memory");
                owner.hurt(mob.damageSources().mobAttack(mob), 6);
                owner.hurt(mob.damageSources().indirectMagic(mob, mob), 6);
                owner.hurt(mob.damageSources().explosion(mob, mob), 6);
            }
            if (owner.getHealth() < owner.getMaxHealth()) throw new GameTestAssertException("the owner was hurt by their own pets: " + owner.getHealth());
            // part two: orders. A warden and a zombie each go for a target.
            for (var mob : pets) mob.discard();
            var warden = net.minecraft.world.entity.EntityType.WARDEN.create(level);
            warden.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 3.5, 0f, 0f);
            level.addFreshEntity(warden);
            var zombiePet = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            zombiePet.moveTo(base.getX() + 0.5, base.getY(), base.getZ() - 3.5, 0f, 0f);
            level.addFreshEntity(zombiePet);
            com.example.charpowers.own.Minions.claim(warden, owner, "pet");
            com.example.charpowers.own.Minions.settle(warden);
            com.example.charpowers.mobcontrol.PetMinds.adopt(warden);
            com.example.charpowers.own.Minions.claim(zombiePet, owner, "pet");
            com.example.charpowers.own.Minions.settle(zombiePet);
            zombiePet.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 100000));
            var victim = net.minecraft.world.entity.EntityType.COW.create(level);
            victim.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 6.5, 0f, 0f);
            level.addFreshEntity(victim);
            var victim2 = net.minecraft.world.entity.EntityType.COW.create(level);
            victim2.moveTo(base.getX() + 0.5, base.getY(), base.getZ() - 6.5, 0f, 0f);
            level.addFreshEntity(victim2);
            float cowHealth = victim.getMaxHealth();
            // the owner hits a cow: both pets are told to go for it
            victim.hurt(owner.damageSources().playerAttack(owner), 1);
            com.example.charpowers.mobcontrol.PetMinds.order(owner.getUUID(), victim);
            h.succeedWhen(() -> {
                if (victim.isAlive() && victim.getHealth() >= cowHealth - 1) throw new GameTestAssertException("nothing attacked the cow yet");
                if (owner.getHealth() < owner.getMaxHealth()) throw new GameTestAssertException("the owner got hurt during the fight");
                owner.discard();
            });
        });
    }
    // The suit flies to you piece by piece, makes you slow (V at mastery 1, I at 100) and flies away again when you crouch and press.
    @GameTest(template = "empty", timeoutTicks = 800)
    public static void suitFliesInAndOut(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(2, 230, 182));
        // the test player has no real connection, so keep the chunks round it loaded and ticking by hand
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, true);
        ServerPlayer p = loggedIn(h, "armoured", at);
        InvincibleArmour.POWER.give(p);
        p.getInventory().setItem(0, new ItemStack(SuitItems.SUIT.get()));
        InvincibleArmour.POWER.onKey(p, "call_suit");
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(at).inflate(80);
        h.runAfterDelay(5, () -> {
            if (SuitItems.fullSuit(p)) throw new GameTestAssertException("the suit was on at once");
            if (level.getEntitiesOfClass(com.example.charpowers.armour.SuitFlight.class, around).size() != 1) throw new GameTestAssertException("no flying suit");
            if (p.getInventory().items.get(0).is(SuitItems.SUIT.get())) throw new GameTestAssertException("the suit stayed in the bag");
        });
        h.runAfterDelay(260, () -> {
            if (!SuitItems.fullSuit(p)) throw new GameTestAssertException("the suit never arrived: flyers=" + level.getEntitiesOfClass(com.example.charpowers.armour.SuitFlight.class, around).size() + " bag=" + p.getInventory().items.stream().anyMatch(i -> i.is(SuitItems.SUIT.get())) + " chest=" + p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST) + " removed=" + p.isRemoved() + " dim=" + p.level().dimension().location() + " in=" + level.dimension().location());
            if (!level.getEntitiesOfClass(com.example.charpowers.armour.SuitFlight.class, around).isEmpty()) throw new GameTestAssertException("the flyer is still there");
            // a test player has no real connection, so its tick event is sent by hand
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, p));
            var slow = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            if (slow == null || slow.getAmplifier() != 4 || slow.isVisible()) throw new GameTestAssertException("expected hidden slowness V, got " + slow);
            Mastery.setLevel(p, Mastery.INVINCIBLE_ARMOUR, 100);
        });
        h.runAfterDelay(300, () -> {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, p));
            var slow = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            if (slow == null || slow.getAmplifier() != 0) throw new GameTestAssertException("expected slowness I at mastery 100, got " + slow);
            p.setShiftKeyDown(true);
            InvincibleArmour.POWER.onKey(p, "call_suit");
        });
        h.runAfterDelay(305, () -> {
            if (SuitItems.fullSuit(p)) throw new GameTestAssertException("the suit stayed on");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, p));
            if (p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) != null) throw new GameTestAssertException("still slow without the suit");
            if (level.getEntitiesOfClass(com.example.charpowers.armour.SuitFlight.class, around).size() != 1) throw new GameTestAssertException("no leaving suit");
        });
        h.runAfterDelay(500, () -> {
            boolean back = p.getInventory().items.stream().anyMatch(i -> i.is(SuitItems.SUIT.get()));
            boolean gone = level.getEntitiesOfClass(com.example.charpowers.armour.SuitFlight.class, around).isEmpty();
            p.discard();
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, false);
            if (!gone || !back) throw new GameTestAssertException("the suit should be back in the bag: gone=" + gone + " back=" + back);
            h.succeed();
        });
    }
}
