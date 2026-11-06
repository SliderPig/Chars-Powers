package com.example.charpowers.hidden.attack;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.PowerUser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.*;

// Runs bits of an attack power a little later (Fast Fist's next punch, a meteor landing after its warning...).
// Nothing here is saved: if the holder dies, logs out, changes dimension or (a mob) unloads, everything still
// waiting for them is dropped and their clean-ups run (floating blocks shatter, cages vanish). The cooldown stays used.
public final class Later {
    private record Job(long due, UUID owner, LivingEntity holder, Runnable run) {}

    private static final Map<ResourceKey<Level>, List<Job>> JOBS = new HashMap<>();
    // owner -> (name -> clean-up)
    private static final Map<UUID, Map<String, Runnable>> CLEANUPS = new HashMap<>();

    public static void after(PowerUser u, int ticks, Runnable r) {
        LivingEntity e = u.entity();
        JOBS.computeIfAbsent(u.level().dimension(), k -> new ArrayList<>())
                .add(new Job(u.now() + Math.max(1, ticks), e.getUUID(), e, r));
    }

    // Something to tidy up if the holder's plans get cancelled. Call done() with the same name once it's finished
    // normally, so it doesn't run later.
    public static void cleanup(PowerUser u, String name, Runnable r) {
        CLEANUPS.computeIfAbsent(u.entity().getUUID(), k -> new HashMap<>()).put(name, r);
    }

    public static void done(PowerUser u, String name) {
        Map<String, Runnable> m = CLEANUPS.get(u.entity().getUUID());
        if (m != null) m.remove(name);
    }

    // The holder died / logged out / changed dimension / unloaded: drop everything waiting and tidy up.
    public static void cancel(LivingEntity owner) {
        UUID id = owner.getUUID();
        for (List<Job> list : JOBS.values()) list.removeIf(j -> j.owner().equals(id));
        Map<String, Runnable> m = CLEANUPS.remove(id);
        if (m == null) return;
        for (Runnable r : m.values()) safely(r);
    }

    public static void clearAll() {
        JOBS.clear();
        CLEANUPS.clear();
    }

    public static void tick(ServerLevel l) {
        List<Job> list = JOBS.get(l.dimension());
        if (list == null || list.isEmpty()) return;
        long now = l.getGameTime();
        List<Job> due = new ArrayList<>();
        List<LivingEntity> gone = new ArrayList<>();
        for (Iterator<Job> it = list.iterator(); it.hasNext(); ) {
            Job j = it.next();
            if (j.holder().isRemoved() || !j.holder().isAlive() || j.holder().level() != l) {
                gone.add(j.holder());
                it.remove();
            } else if (now >= j.due()) {
                due.add(j);
                it.remove();
            }
        }
        for (LivingEntity e : gone) cancel(e);
        for (Job j : due) safely(j.run()); // a job may add new jobs: they go in the list for a later tick
    }

    private static void safely(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            // one broken power must never crash the server
            CharPowers.LOG.error("An attack power's later step failed", e);
        }
    }

    private Later() {}
}
