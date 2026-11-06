package com.example.charpowers.game;

import com.example.charpowers.CharPowers;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.Minions;
import com.example.charpowers.own.Pocket;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

// Raise Shadow, headless: bodies only from gate kills, the limit, the boss cooldown, and shadows going when you leave.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class ShadowTests {
    private static ServerPlayer player(GameTestHelper h, String name) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        p.teleportTo(level, at.getX() + 0.5, 100, at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        return p;
    }

    private static Husk killOne(ServerLevel pocket, ServerPlayer p) {
        Husk husk = new Husk(EntityType.HUSK, pocket);
        husk.moveTo(p.getX() + 2, p.getY(), p.getZ(), 0f, 0f);
        husk.setNoAi(true);
        pocket.addFreshEntity(husk);
        husk.hurt(p.damageSources().playerAttack(p), 1000f);
        return husk;
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "gateRaid")
    public static void shadowsFollowTheRules(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer p = player(h, "shadowmaker");
        var power = GamePlayer.POWER;
        power.give(p);
        p.setInvulnerable(true);
        Mastery.setLevel(p, Mastery.GAME_PLAYER, 30);
        ShadowArmy.clearForTest(p.getUUID());
        // a kill outside any gate leaves nothing to raise
        ServerLevel home = h.getLevel();
        Husk outside = new Husk(EntityType.HUSK, home);
        outside.moveTo(p.getX() + 2, p.getY(), p.getZ(), 0f, 0f);
        outside.setNoAi(true);
        home.addFreshEntity(outside);
        outside.hurt(p.damageSources().playerAttack(p), 1000f);
        ShadowArmy.raise(p);
        if (ShadowArmy.count(p.getUUID()) != 0) throw new GameTestAssertException("raised a shadow outside a gate");

        power.enterGate(p, 1, false);
        if (power.runOf(p.getUUID()) == null) throw new GameTestAssertException("not in a gate");
        ServerLevel pocket = (ServerLevel) p.level();
        if (!Pocket.in(p)) throw new GameTestAssertException("not in the pocket");
        for (int i = 0; i < 4; i++) {
            Husk husk = killOne(pocket, p);
            if (husk.isAlive()) throw new GameTestAssertException("the husk survived");
            ShadowArmy.raise(p);
        }
        int count = ShadowArmy.count(p.getUUID());
        if (count != 3) throw new GameTestAssertException("expected exactly 3 shadows at mastery 30, got " + count);
        for (Mob m : Minions.of(p.getUUID(), ShadowArmy.KIND)) {
            if (!(m instanceof Husk)) throw new GameTestAssertException("shadow is a " + m.getType());
            if (m.getTarget() instanceof ServerPlayer) throw new GameTestAssertException("a shadow targets a player");
        }
        // a boss body: a failed or a won attempt both start the cooldown, so the second boss body is refused
        Vec3 here = p.position();
        ShadowArmy.addBodyForTest(p, new ShadowArmy.Body(EntityType.ZOMBIE, pocket.dimension(), here, pocket.getGameTime() + 100, true, "Test Boss"));
        Mastery.setLevel(p, Mastery.GAME_PLAYER, 55); // room for a fourth now
        ShadowArmy.raise(p);
        int afterBoss = ShadowArmy.count(p.getUUID());
        ShadowArmy.addBodyForTest(p, new ShadowArmy.Body(EntityType.ZOMBIE, pocket.dimension(), here, pocket.getGameTime() + 100, true, "Test Boss"));
        ShadowArmy.raise(p);
        if (ShadowArmy.count(p.getUUID()) != afterBoss) throw new GameTestAssertException("a second boss was raised during the cooldown");
        long t0 = 0;
        h.runAtTickTime(t0 + 5, () -> power.leaveRun(p));
        h.runAtTickTime(t0 + 60, () -> {
            if (ShadowArmy.count(p.getUUID()) != 0) throw new GameTestAssertException("shadows are still there after leaving: " + ShadowArmy.count(p.getUUID()));
            GameOwner.of(p.getServer()).set(null);
            p.getServer().getPlayerList().remove(p);
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200, batch = "gateRaid")
    public static void youCannotHurtYourShadow(GameTestHelper h) {
        h.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        ServerPlayer p = player(h, "shadowhitter");
        GamePlayer.POWER.give(p);
        Zombie z = new Zombie(EntityType.ZOMBIE, h.getLevel());
        z.moveTo(p.getX() + 2, p.getY(), p.getZ(), 0f, 0f);
        h.getLevel().addFreshEntity(z);
        Minions.claim(z, p, ShadowArmy.KIND);
        Minions.settle(z);
        float before = z.getHealth();
        z.hurt(p.damageSources().playerAttack(p), 5f);
        // the event is posted by hurt(): the damage is cancelled
        if (z.getHealth() < before) throw new GameTestAssertException("owner could hurt the shadow");
        z.discard();
        GameOwner.of(p.getServer()).set(null);
        p.getServer().getPlayerList().remove(p);
        h.succeed();
    }
}
