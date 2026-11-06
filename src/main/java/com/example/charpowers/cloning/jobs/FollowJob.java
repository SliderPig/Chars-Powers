package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneCombat;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import net.minecraft.server.level.ServerPlayer;

// The default: stay near the player. Too far away, in another dimension or stuck for 30 s = teleports over.
public class FollowJob implements CloneJob {
    private static final double TELEPORT_SQR = 48 * 48;
    private static final double CLOSE_SQR = 4 * 4;

    @Override
    public String id() {
        return "follow";
    }

    @Override
    public String describe(CloneEntity c) {
        return "Following you";
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (host == null) return;
        if (host.level() != c.level() || c.distanceToSqr(host) > TELEPORT_SQR || c.mover.longStuck()) {
            CloneCombat.teleportToHost(c, host);
        } else if (c.distanceToSqr(host) > CLOSE_SQR) {
            c.mover.walkTo(host.blockPosition());
        } else {
            c.mover.stop();
        }
    }
}
