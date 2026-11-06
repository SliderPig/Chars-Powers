package com.example.charpowers.smoke;

import com.example.charpowers.CharPowers;
import com.example.charpowers.anim.AnimNet;
import com.example.charpowers.anim.AnimTriggers;
import com.example.charpowers.lantern.ConstructEntity;
import com.example.charpowers.lantern.GreenLantern;
import com.example.charpowers.mastery.Mastery;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

// On a real headless server: telling everyone about a pose does no harm, and the minigun knows whose hands it's in.
@GameTestHolder(CharPowers.MODID)
@PrefixGameTestTemplate(false)
public class AnimSmokeTests {
    private static List<ConstructEntity> guns(ServerLevel level, ServerPlayer p) {
        return level.getEntitiesOfClass(ConstructEntity.class, p.getBoundingBox().inflate(6), c -> "minigun".equals(c.kind()));
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "anim")
    public static void minigunKnowsItsHolderAndPosesSend(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(2, 230, 1100));
        // keep the chunks round it loaded, and give them time to start ticking before anything is made in them
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, true);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "gunner"));
        level.getServer().getPlayerList().placeNewPlayer(new dev.dubhe.curtain.features.player.patches.FakeClientConnection(PacketFlow.SERVERBOUND), p);
        p.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        p.setNoGravity(true);
        var power = GreenLantern.POWER;
        h.runAfterDelay(150, () -> {
            power.give(p);
            // every kind of send, with and without the ring paying for it
            AnimNet.play(p, "ring_thrust");
            AnimNet.stop(p);
            AnimTriggers.beforeKey();
            AnimTriggers.afterKey(p, Mastery.GREEN_LANTERN, "hand_slam");
            AnimTriggers.ringSpent();
            AnimTriggers.afterKey(p, Mastery.GREEN_LANTERN, "hand_slam");
            AnimTriggers.afterKey(p, Mastery.GREEN_LANTERN, "no_such_key");
            AnimTriggers.afterRelease(p, Mastery.GREEN_LANTERN, "launch_swords");
            power.onKey(p, "minigun");
        });
        h.runAfterDelay(160, () -> {
            List<ConstructEntity> guns = guns(level, p);
            if (guns.size() != 1) throw new GameTestAssertException("expected one gun, found " + guns.size());
            if (guns.get(0).holder() != p.getId()) throw new GameTestAssertException("the gun doesn't know who's holding it");
            power.onClientAction(p, "fire:1");
        });
        h.runAfterDelay(190, () -> {
            List<ConstructEntity> guns = guns(level, p);
            if (guns.isEmpty() || !"Shooting".equals(guns.get(0).animation())) throw new GameTestAssertException("the gun should be shooting, it's " + (guns.isEmpty() ? "gone" : guns.get(0).animation()));
            power.onClientAction(p, "fire:0");
            power.onKey(p, "minigun");
        });
        h.runAfterDelay(215, () -> {
            boolean gone = guns(level, p).isEmpty();
            p.discard();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setChunkForced((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz, false);
            if (!gone) throw new GameTestAssertException("the gun stayed out");
            h.succeed();
        });
    }
}
