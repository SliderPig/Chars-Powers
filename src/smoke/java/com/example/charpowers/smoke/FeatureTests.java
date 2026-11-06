package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.jobs.FarmJob;
import com.example.charpowers.cloning.jobs.GuardJob;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.orders.OrderServer;
import com.example.charpowers.own.Minions;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.threetag.palladium.power.ability.AbilityUtil;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Headless checks for the order wheel, Guard, Farm and the things built with them. Only compiled for runGameTestServer -Psmoke.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class FeatureTests {
    private static ServerPlayer loggedIn(GameTestHelper h, String name, BlockPos at) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        return p;
    }

    // takes the test player off the server's list, so later tests (the op menu) do not meet them
    private static void leave(ServerPlayer p) {
        p.getServer().getPlayerList().remove(p);
    }

    private static void fill(ServerLevel level, BlockPos a, BlockPos b, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(a, b)) level.setBlock(pos, state, 2);
    }

    private static void force(ServerLevel level, BlockPos mid) {
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) level.setChunkForced((mid.getX() >> 4) + cx, (mid.getZ() >> 4) + cz, true);
        }
    }

    private static CloneEntity summon(ServerPlayer host) {
        SuperpowerUtil.addSuperpower(host, Mastery.CLONING);
        CloneManager.pressSummon(host);
        return new ArrayList<>(CloneManager.clonesOf(host.getUUID())).get(0);
    }

    // The wheel's orders reach the right things, and an order that does not fit what you have is dropped.
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wheelOrdersReachTheirTargets(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ground = h.absolutePos(new BlockPos(2, 230, 1500));
        fill(level, ground.offset(-12, -1, -12), ground.offset(12, -1, 12), Blocks.STONE.defaultBlockState());
        force(level, ground);
        ServerPlayer host = loggedIn(h, "wheeler", ground.above());
        CloneEntity clone = summon(host);
        clone.moveTo(ground.getX() + 2.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        Zombie pet = new Zombie(EntityType.ZOMBIE, level);
        pet.moveTo(ground.getX() - 3.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(pet);
        Minions.claim(pet, host, "pet");
        Minions.settle(pet);
        Zombie minion = new Zombie(EntityType.ZOMBIE, level);
        minion.moveTo(ground.getX() - 3.5, ground.getY(), ground.getZ() + 4.5, 0f, 0f);
        level.addFreshEntity(minion);
        Minions.claim(minion, host, "undead");
        Minions.settle(minion);
        long t0 = 0; // runAtTickTime counts from the start of the test
        h.runAtTickTime(t0 + 10, () -> OrderServer.give(host, "clones", "guard"));
        h.runAtTickTime(t0 + 12, () -> {
            if (!"guard".equals(clone.job.id())) throw new GameTestAssertException("clone job is " + clone.job.id());
            OrderServer.give(host, "pets", "farm"); // not a pet order: dropped
            OrderServer.give(host, "clones", "farm"); // too soon after the last one: dropped
            if (!"guard".equals(clone.job.id())) throw new GameTestAssertException("a bad or rushed order changed the job to " + clone.job.id());
        });
        h.runAtTickTime(t0 + 30, () -> OrderServer.give(host, "clones", "farm"));
        h.runAtTickTime(t0 + 32, () -> {
            if (!(clone.job instanceof FarmJob)) throw new GameTestAssertException("clone job is " + clone.job.id());
        });
        h.runAtTickTime(t0 + 50, () -> OrderServer.give(host, "pets", "stay"));
        h.runAtTickTime(t0 + 52, () -> {
            if (!Minions.STAY.equals(Minions.stance(pet))) throw new GameTestAssertException("pet stance " + Minions.stance(pet));
        });
        h.runAtTickTime(t0 + 70, () -> OrderServer.give(host, "pets", "guard"));
        h.runAtTickTime(t0 + 72, () -> {
            if (!Minions.GUARD.equals(Minions.stance(pet))) throw new GameTestAssertException("pet stance " + Minions.stance(pet));
        });
        h.runAtTickTime(t0 + 90, () -> OrderServer.give(host, "minions", "stay"));
        h.runAtTickTime(t0 + 92, () -> {
            if (!Minions.STAY.equals(Minions.stance(minion))) throw new GameTestAssertException("minion stance " + Minions.stance(minion));
            if (!Minions.STAY.equals(Minions.stance(minion)) || !Minions.GUARD.equals(Minions.stance(pet))) throw new GameTestAssertException("stances leaked between kinds");
        });
        h.runAtTickTime(t0 + 110, () -> OrderServer.give(host, "minions", "follow"));
        h.runAtTickTime(t0 + 130, () -> OrderServer.give(host, "clones", "home"));
        h.runAtTickTime(t0 + 135, () -> {
            if (!CloneManager.clonesOf(host.getUUID()).isEmpty()) throw new GameTestAssertException("clone did not come home");
            if (!Minions.FOLLOW.equals(Minions.stance(minion))) throw new GameTestAssertException("minion did not follow again");
            pet.discard();
            minion.discard();
            leave(host);
            h.succeed();
        });
    }

    // Guard: a hostile mob comes near the post, the clone kills it and walks back to the post.
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void aCloneGuardsItsPost(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ground = h.absolutePos(new BlockPos(2, 230, 1560));
        fill(level, ground.offset(-16, -1, -16), ground.offset(16, -1, 16), Blocks.STONE.defaultBlockState());
        force(level, ground);
        ServerPlayer host = loggedIn(h, "guarder", ground.offset(0, 1, 12));
        CloneEntity clone = summon(host);
        clone.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        clone.getInventory().add(new ItemStack(Items.DIAMOND_SWORD));
        clone.setJob(new GuardJob(ground));
        // far outside the guard area: must be ignored
        Husk far = new Husk(EntityType.HUSK, level);
        far.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 15.5, 0f, 0f);
        far.setNoAi(true);
        level.addFreshEntity(far);
        Husk near = new Husk(EntityType.HUSK, level);
        near.moveTo(ground.getX() + 6.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        near.setNoAi(true);
        level.addFreshEntity(near);
        long started = level.getGameTime();
        h.succeedWhen(() -> {
            if (near.isAlive()) throw new GameTestAssertException("the husk by the post is still alive");
            if (!far.isAlive()) throw new GameTestAssertException("it went after a mob outside its guard area");
            if (clone.distanceToSqr(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5) > 25) throw new GameTestAssertException("not back at its post: " + clone.blockPosition().toShortString());
            CharPowers.LOG.info("[Smoke] guard killed the husk and was back at its post after {} ticks", level.getGameTime() - started);
            far.discard();
            CloneManager.dismissAll(host);
            leave(host);
        });
    }

    // Farm: ripe wheat is picked and planted again, a seedling is left, the field is never dug up.
    @GameTest(template = "empty", timeoutTicks = 2400)
    public static void aCloneFarms(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ground = h.absolutePos(new BlockPos(2, 230, 1620));
        fill(level, ground.offset(-14, -1, -14), ground.offset(14, -1, 14), Blocks.GRASS_BLOCK.defaultBlockState());
        force(level, ground);
        ServerPlayer host = loggedIn(h, "farmer", ground.offset(0, 1, -8));
        List<BlockPos> ripe = new ArrayList<>();
        BlockState wheat7 = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        for (int x = 3; x <= 7; x++) {
            BlockPos soil = ground.offset(x, -1, 2);
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 2);
            level.setBlock(soil.above(), wheat7, 2);
            ripe.add(soil.above());
        }
        BlockPos seedlingSoil = ground.offset(9, -1, 2);
        level.setBlock(seedlingSoil, Blocks.FARMLAND.defaultBlockState(), 2);
        level.setBlock(seedlingSoil.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 2), 2);
        level.setBlock(ground.offset(5, -1, 4), Blocks.WATER.defaultBlockState(), 2);
        CloneEntity clone = summon(host);
        clone.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        clone.getInventory().add(new ItemStack(Items.WHEAT_SEEDS, 8));
        clone.setJob(new FarmJob(ground.offset(5, 0, 2)));
        long started = level.getGameTime();
        h.succeedWhen(() -> {
            for (BlockPos at : ripe) {
                BlockState s = level.getBlockState(at);
                if (!s.is(Blocks.WHEAT) || s.getValue(CropBlock.AGE) != 0) throw new GameTestAssertException("not picked and planted again at " + at.toShortString() + ": " + s);
                if (!level.getBlockState(at.below()).is(Blocks.FARMLAND)) throw new GameTestAssertException("the field was dug at " + at.below().toShortString());
            }
            BlockState seedling = level.getBlockState(seedlingSoil.above());
            if (!seedling.is(Blocks.WHEAT) || seedling.getValue(CropBlock.AGE) != 2) throw new GameTestAssertException("the seedling was touched: " + seedling);
            int wheat = 0;
            for (ItemStack s : clone.getInventory().items) if (s.is(Items.WHEAT)) wheat += s.getCount();
            if (wheat < 5) throw new GameTestAssertException("only " + wheat + " wheat picked up");
            CharPowers.LOG.info("[Smoke] clone farmed 5 wheat and replanted in {} ticks", level.getGameTime() - started);
            CloneManager.dismissAll(host);
            leave(host);
        });
    }

    // Feeding a hurt pet its food heals it and uses the food up; wrong food is refused; a ravager pet can be ridden.
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void petsEatAndCarryYou(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ground = h.absolutePos(new BlockPos(2, 230, 1700));
        fill(level, ground.offset(-8, -1, -8), ground.offset(8, -1, 8), Blocks.STONE.defaultBlockState());
        force(level, ground);
        ServerPlayer host = loggedIn(h, "feeder", ground.above());
        com.example.charpowers.mobcontrol.MobControl.POWER.give(host);
        Zombie pet = new Zombie(EntityType.ZOMBIE, level);
        pet.moveTo(ground.getX() + 2.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(pet);
        Minions.claim(pet, host, "pet");
        Minions.settle(pet);
        pet.setHealth(5f);
        net.minecraft.world.entity.monster.Ravager ravager = new net.minecraft.world.entity.monster.Ravager(EntityType.RAVAGER, level);
        ravager.moveTo(ground.getX() - 3.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(ravager);
        Minions.claim(ravager, host, "pet");
        Minions.settle(ravager);
        ItemStack bread = new ItemStack(Items.BREAD, 2);
        if (com.example.charpowers.mobcontrol.PetCare.feed(host, pet, bread) && bread.getCount() != 2) throw new GameTestAssertException("bread was used on a meat eater");
        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH, 2);
        if (!com.example.charpowers.mobcontrol.PetCare.feed(host, pet, flesh)) throw new GameTestAssertException("rotten flesh refused");
        if (flesh.getCount() != 1) throw new GameTestAssertException("food not used up: " + flesh.getCount());
        if (pet.getHealth() <= 5f) throw new GameTestAssertException("not healed: " + pet.getHealth());
        host.setPos(ground.getX() - 3.5, ground.getY(), ground.getZ() + 0.5);
        if (!com.example.charpowers.mobcontrol.MobControl.rideAsPet(host, ravager)) throw new GameTestAssertException("could not ride the ravager");
        if (host.getVehicle() != ravager) throw new GameTestAssertException("not riding it");
        host.stopRiding();
        pet.discard();
        ravager.discard();
        leave(host);
        h.succeed();
    }

    // The cat: only for someone with a power who switched it on; it follows, cannot be hurt or targeted, and goes when switched off.
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void yourCatTrailsYouAndStaysOutOfTheWay(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos ground = h.absolutePos(new BlockPos(2, 230, 1800));
        fill(level, ground.offset(-40, -1, -12), ground.offset(40, -1, 12), Blocks.STONE.defaultBlockState());
        force(level, ground);
        force(level, ground.offset(32, 0, 0));
        ServerPlayer host = loggedIn(h, "catperson", ground.above());
        com.example.charpowers.cat.CompanionCats.look(host);
        if (com.example.charpowers.cat.CompanionCats.catOf(host.getUUID()) != null) throw new GameTestAssertException("a cat without a power");
        com.example.charpowers.mobcontrol.MobControl.POWER.give(host);
        com.example.charpowers.cat.CompanionCats.look(host);
        if (com.example.charpowers.cat.CompanionCats.catOf(host.getUUID()) != null) throw new GameTestAssertException("a cat before it was switched on");
        com.example.charpowers.cat.CompanionCats.setOn(host, true);
        com.example.charpowers.cat.CompanionCats.look(host);
        com.example.charpowers.cat.CompanionCat cat = com.example.charpowers.cat.CompanionCats.catOf(host.getUUID());
        if (cat == null) throw new GameTestAssertException("no cat after switching on");
        if (!cat.getTags().contains(com.example.charpowers.cat.CompanionCat.TAG)) throw new GameTestAssertException("cat not tagged");
        if (cat.shouldBeSaved()) throw new GameTestAssertException("the cat would be saved with the world");
        float hp = cat.getHealth();
        cat.hurt(host.damageSources().playerAttack(host), 5f);
        cat.hurt(host.damageSources().generic(), 5f);
        if (cat.getHealth() < hp) throw new GameTestAssertException("the cat was hurt");
        Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
        zombie.moveTo(ground.getX() + 3.5, ground.getY(), ground.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(zombie);
        if (net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().test(zombie, cat)) throw new GameTestAssertException("a zombie can target the cat");
        zombie.discard();
        // walk away 30 blocks: the cat hops over beside you
        long t0 = 0; // runAtTickTime counts from the start of the test
        h.runAtTickTime(t0 + 5, () -> host.teleportTo(level, ground.getX() + 31.5, ground.getY() + 1, ground.getZ() + 0.5, 0f, 0f));
        h.runAtTickTime(t0 + 80, () -> {
            if (cat.isRemoved() || cat.distanceToSqr(host) > 100) throw new GameTestAssertException("the cat did not come along: " + cat.distanceToSqr(host));
            com.example.charpowers.cat.CompanionCats.cheer(host);
            com.example.charpowers.cat.CompanionCats.setOn(host, false);
            com.example.charpowers.cat.CompanionCats.look(host);
        });
        h.runAtTickTime(t0 + 85, () -> {
            if (!cat.isRemoved()) throw new GameTestAssertException("the cat stayed after switching off");
            leave(host);
            h.succeed();
        });
    }
}
