package com.example.charpowers.cloning.jobs;

import com.example.charpowers.cloning.CloneCombat;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import net.minecraft.server.level.ServerPlayer;

// A rogue clone: you are its only target, and it comes after you if you change dimension (after 10 s).
public class RogueJob implements CloneJob {
    private static final int DIMENSION_DELAY = 200;
    private int awayFor;

    @Override
    public String id() {
        return "rogue";
    }

    @Override
    public String describe(CloneEntity c) {
        return "Hunting you";
    }

    @Override
    public boolean lockedIn() {
        return true;
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (host == null) return;
        if (host.level() != c.level()) {
            c.mover.stop();
            if (++awayFor >= DIMENSION_DELAY) {
                awayFor = 0;
                CloneCombat.teleportToHost(c, host);
            }
            return;
        }
        awayFor = 0;
        if (c.mover.longStuck()) {
            c.mover.forgetStuck();
            CloneCombat.teleportToHost(c, host);
        }
        CloneCombat.fight(c, host);
    }
}
