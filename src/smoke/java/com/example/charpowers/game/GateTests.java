package com.example.charpowers.game;

import com.example.charpowers.CharPowers;
import com.example.charpowers.game.mob.GateBoss;
import com.example.charpowers.game.mob.GateKind;
import com.example.charpowers.game.mob.GateMob;
import com.example.charpowers.game.mob.GateMobs;
import com.example.charpowers.game.model.MobMove;
import net.minecraft.world.entity.MobSpawnType;
import java.util.ArrayList;
import java.util.List;
import com.example.charpowers.own.Pocket;
import com.example.charpowers.own.TeamBridge;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

// Gates for real, headless: the bars hold, friends come along, and every boss fights without a crash.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class GateTests {
    // Logged in properly, on the player list, so the run and the team code can find them by id.
    private static ServerPlayer player(GameTestHelper h, String name) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        p.teleportTo(level, at.getX() + 0.5, 100, at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        return p;
    }

    // The way forward is shut: bars with a barrier behind them, so nobody (a fake player at every spot of the door) gets through, and a push-back as backup.
    @GameTest(template = "empty", timeoutTicks = 400, batch = "gateRaid")
    public static void shutDoorsReallyHold(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer p = player(h, "walker");
        ServerLevel pocket = Pocket.level(p);
        if (pocket == null) throw new GameTestAssertException("no pocket dimension");
        GateRun run = new GateRun(p, pocket, 2, false, null);
        BlockPos doorMid = BlockPos.containing(run.entrance()).offset(0, 0, 0);
        p.teleportTo(pocket, run.entrance().x, run.entrance().y, run.entrance().z, 0f, 0f);
        run.second(); // room 0 wakes up
        int hx = run.rooms.get(0).halfX();
        BlockPos room = BlockPos.containing(run.entrance()).offset(-3 + 0, 0, 0); // the first room's middle x is the entrance minus the walk-in minus -halfX
        int midX = (int) Math.floor(run.entrance().x) - (-hx + 3);
        for (int z = -1; z <= 1; z++) {
            for (int y = 0; y < 3; y++) {
                BlockPos bar = new BlockPos(midX + hx, doorMid.getY() + y, doorMid.getZ() + z);
                if (!pocket.getBlockState(bar).is(Blocks.IRON_BARS)) throw new GameTestAssertException("no bars at " + bar.toShortString() + ": " + pocket.getBlockState(bar));
                if (!pocket.getBlockState(bar.east()).is(Blocks.BARRIER)) throw new GameTestAssertException("no barrier behind " + bar.toShortString());
            }
        }
        // every standing position across the doorway, and a body-sized box moving through, collides
        for (double z = -1.4; z <= 1.4; z += 0.2) {
            AABB body = p.getDimensions(p.getPose()).makeBoundingBox(new Vec3(midX + hx + 0.5, doorMid.getY(), doorMid.getZ() + 0.5 + z));
            if (pocket.noCollision(body)) throw new GameTestAssertException("walked through at z " + z);
        }
        // and if someone does get past (a push), they are put back
        p.teleportTo(pocket, midX + hx + 3, doorMid.getY(), doorMid.getZ() + 0.5, 0f, 0f);
        for (int i = 0; i < 8; i++) run.tick();
        if (p.getX() > midX + hx) throw new GameTestAssertException("not pushed back, x " + p.getX());
        run.end();
        h.succeed();
    }

    // Friends: teammates are found without Char's Teams (vanilla team), a friend joins and leaves a run, scaling follows the party, death takes you off the list.
    @GameTest(template = "empty", timeoutTicks = 400, batch = "gateRaid")
    public static void friendsJoinTheRaid(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer host = player(h, "hostess"), friend = player(h, "buddy"), stranger = player(h, "stranger");
        var scoreboard = h.getLevel().getServer().getScoreboard();
        var team = scoreboard.addPlayerTeam("gateTeam");
        scoreboard.addPlayerToTeam(host.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(friend.getScoreboardName(), team);
        if (!TeamBridge.sameTeam(host.getUUID(), friend.getUUID())) throw new GameTestAssertException("same team not seen");
        if (TeamBridge.sameTeam(host.getUUID(), stranger.getUUID())) throw new GameTestAssertException("stranger on the team");
        if (!TeamBridge.teammates(host.getUUID()).contains(friend.getUUID())) throw new GameTestAssertException("teammates missing");
        if (!TeamBridge.teammates(stranger.getUUID()).isEmpty()) throw new GameTestAssertException("a lone player has teammates");

        var power = GamePlayer.POWER;
        power.enterGate(host, 1, false);
        GateRun run = power.runOf(host.getUUID());
        if (run == null || !Pocket.in(host)) throw new GameTestAssertException("host did not go in");
        int soloMobs = com.example.charpowers.game.model.GatePlan.count(1, 0, 1, com.example.charpowers.game.model.GatePlan.Kind.ARENA);
        power.joinRun(run, friend);
        if (!Pocket.in(friend) || run.partySize() != 2) throw new GameTestAssertException("friend did not join, party " + run.partySize());
        if (com.example.charpowers.game.model.GatePlan.count(1, 0, 2, com.example.charpowers.game.model.GatePlan.Kind.ARENA) <= soloMobs) throw new GameTestAssertException("no scaling");
        for (int i = 0; i < 3; i++) run.second();
        // the friend dies: off the list, the run goes on for the host
        GamePlayer.onDied(new LivingDeathEvent(friend, friend.damageSources().generic()));
        if (power.runOf(friend.getUUID()) != null || run.has(friend.getUUID())) throw new GameTestAssertException("dead friend still in the run");
        if (!run.has(host.getUUID())) throw new GameTestAssertException("host lost");
        // the host leaves too (crouch ring is for real play; here the plain command)
        power.leaveRun(host);
        run.second();
        if (Pocket.in(host)) throw new GameTestAssertException("host should be home");
        run.end();
        scoreboard.removePlayerTeam(team);
        h.succeed();
    }

    // Every boss fights for a while without throwing: walks, tells and attacks, in a party of two.
    @GameTest(template = "empty", timeoutTicks = 700, batch = "gateRaid")
    public static void everyBossFights(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer p = player(h, "fighter");
        ServerLevel pocket = Pocket.level(p);
        BlockPos base = Pocket.roomOf(p.getUUID()).offset(0, 0, 60);
        flatten(pocket, base, 30);
        p.teleportTo(pocket, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 0f);
        p.setInvulnerable(true);
        List<GateBoss> bosses = new ArrayList<>();
        int n = 0;
        for (GateKind k : GateKind.values()) {
            if (!k.boss) continue;
            GateBoss b = GateBoss.spawn(pocket, k, Vec3.atBottomCenterOf(base.offset((n % 4) * 9 - 13, 0, -10 - (n / 4) * 9)), 3, 2, m -> {}, () -> {});
            b.addToParty(p);
            b.setTarget(p);
            bosses.add(b);
            n++;
        }
        h.runAfterDelay(550, () -> {
            for (GateBoss b : bosses) {
                if (!b.isAlive()) throw new GameTestAssertException(b.kind.title + " is gone");
                b.discard();
            }
            h.succeed();
        });
    }

    private static void flatten(ServerLevel level, BlockPos base, int half) {
        for (int x = -half; x <= half; x++) {
            for (int z = -half - 20; z <= half; z++) {
                level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    // Every ordinary creature runs each of its moves, which tell and land without throwing; and it has at least two.
    @GameTest(template = "empty", timeoutTicks = 500, batch = "gateRaid")
    public static void everyCreatureUsesItsMoves(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer p = player(h, "target");
        ServerLevel pocket = Pocket.level(p);
        BlockPos base = Pocket.roomOf(p.getUUID()).offset(0, 0, 160);
        flatten(pocket, base, 30);
        p.teleportTo(pocket, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 0f);
        p.setInvulnerable(true);
        List<GateMob> mobs = new ArrayList<>();
        int n = 0, moves = 0;
        for (GateKind k : GateKind.values()) {
            if (k.boss) continue;
            if (!(GateMobs.mobType(k).create(pocket) instanceof GateMob m)) throw new GameTestAssertException("cannot make " + k.id);
            m.moveTo(base.getX() + 0.5 + (n % 8) * 3 - 11, base.getY(), base.getZ() - 6 - (n / 8) * 3, 0f, 0f);
            m.finalizeSpawn(pocket, pocket.getCurrentDifficultyAt(base), MobSpawnType.MOB_SUMMONED, null, null);
            m.setPersistenceRequired();
            m.setTarget(p);
            pocket.addFreshEntity(m);
            if (m.moves().size() < 2) throw new GameTestAssertException(k.id + " has fewer than two moves");
            mobs.add(m);
            n++;
        }
        h.runAfterDelay(20, () -> {
            for (GateMob m : mobs) {
                for (MobMove move : m.moves()) m.runMoveNow(move, p);
                if (m.kind == GateKind.SKELETON_KNIGHT || m.kind == GateKind.GOBLIN) m.makeElite(com.example.charpowers.game.model.Elite.BERSERK);
            }
        });
        h.runAfterDelay(400, () -> {
            for (GateMob m : mobs) {
                if (!m.isAlive()) throw new GameTestAssertException(m.kind.title + " is gone");
                m.discard();
            }
            CharPowers.LOG.info("[Smoke] {} creatures ran every one of their moves", mobs.size());
            h.succeed();
        });
    }

    // Mob and boss entities all make, with gear, and a spawn egg item exists for each.
    @GameTest(template = "empty", timeoutTicks = 100, batch = "gateRaid")
    public static void everyCreatureExists(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (GateKind k : GateKind.values()) {
            var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(new net.minecraft.resources.ResourceLocation(CharPowers.MODID, k.id));
            if (type == null || type.create(level) == null) throw new GameTestAssertException("no entity for " + k.id);
            var egg = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(CharPowers.MODID, k.id + "_spawn_egg"));
            if (egg == null || egg == net.minecraft.world.item.Items.AIR) throw new GameTestAssertException("no egg for " + k.id);
        }
        h.succeed();
    }
}
