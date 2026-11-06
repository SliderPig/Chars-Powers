package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.model.RoutePlanner;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Works out routes on its own thread, like Baritone does, so a long search can't slow the server's ticks. One thread for
// all clones, one search at a time, and each search has a time limit. When nobody needs a route it just sleeps.
final class PathWorker {
    private static final long MAX_MILLIS = 400;
    private static final int MAX_NODES = 60000, CHECK_EVERY = 500;

    private static final ExecutorService THREAD = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CharPowers clone pathing");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    static final class Job {
        volatile boolean done, noRoute, partial, cancelled;
        volatile List<RoutePlanner.Step> path = List.of();
    }

    private PathWorker() {}

    static Job plan(RoutePlanner.Cell start, RoutePlanner.Goal goal, RoutePlanner.Terrain terrain, boolean hasBlocks, Set<Long> favored) {
        Job job = new Job();
        THREAD.execute(() -> {
            try {
                if (job.cancelled) return;
                RoutePlanner.Search search = new RoutePlanner.Search(start, goal, terrain, MAX_NODES, hasBlocks, favored);
                long until = System.currentTimeMillis() + MAX_MILLIS;
                while (search.run(CHECK_EVERY) == RoutePlanner.Status.SEARCHING) {
                    if (job.cancelled) return;
                    if (System.currentTimeMillis() > until) search.stop();
                }
                job.noRoute = search.status() == RoutePlanner.Status.NO_ROUTE;
                job.partial = search.partial();
                job.path = job.noRoute ? List.of() : search.path();
            } catch (RuntimeException e) {
                // reading the world from another thread can very rarely trip over a change: treat it as no route and look again
                CharPowers.LOG.debug("A clone's route search failed", e);
                job.noRoute = true;
            } finally {
                job.done = true;
            }
        });
        return job;
    }
}
