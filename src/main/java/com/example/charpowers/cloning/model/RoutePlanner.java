package com.example.charpowers.cloning.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

// How a clone works out its way somewhere, built the way Baritone's pathfinder is (same moves, same costs in ticks, same
// A* with a best-so-far partial path, same trick of favouring the path it's already on). Baritone itself can't run here:
// it only works with a player on a game client, and clones live on the server. No game code here, so it can be unit tested.
public final class RoutePlanner {
    // Ticks per block: 20 / the game's speed in blocks a second.
    public static final double WALK = 20 / 4.317, SPRINT = 20 / 5.612, WATER = 20 / 2.2, SNEAK = 20 / 1.3;
    public static final double WALK_OFF = WALK * 0.8, CENTER = WALK - WALK_OFF;
    public static final double JUMP = fallTicks(1.25) - fallTicks(0.25);
    // On top of the time itself, so it would rather go round than make holes and burn through blocks for nothing.
    public static final double DIG_EXTRA = 2, PLACE_EXTRA = 20;
    public static final int MAX_FALL = 3, MAX_FALL_INTO_WATER = 20;
    private static final double FAVOR = 0.5, GREED = 2.0, MIN_PARTIAL_SQ = 25;
    private static final double[] COEFFICIENTS = {1.5, 2, 2.5, 3, 4, 5, 10};
    private static final double SQRT2 = Math.sqrt(2);

    public record Cell(int x, int y, int z) {}

    public enum Kind { WALK, DIAGONAL, UP, DOWN, FALL, DIG_DOWN, PILLAR, SWIM_UP }

    // One move: dig these (top first), put a block in `place` if there is one, then get to `to`.
    public record Step(Cell from, Cell to, Kind kind, List<Cell> dig, Cell place, double cost) {}

    public interface Terrain {
        // can you stand in this block space (air, plants, water)?
        boolean open(int x, int y, int z);

        // can you stand on top of this block?
        boolean floor(int x, int y, int z);

        boolean water(int x, int y, int z);

        // ticks to dig it: 0 if already open, negative if it mustn't or can't be dug
        double digCost(int x, int y, int z);

        // could a block be put here (air or water, never lava)?
        boolean canFill(int x, int y, int z);
    }

    public interface Goal {
        boolean done(int x, int y, int z);

        double guess(int x, int y, int z);

        // What finishing here counts as costing on top, so a cheap common ore can lose to a rarer one further off.
        default double extra(int x, int y, int z) {
            return 0;
        }
    }

    public enum Status { SEARCHING, FOUND, NO_ROUTE }

    // How many ticks it takes to fall this far, from the game's own gravity.
    public static double fallTicks(double distance) {
        if (distance <= 0) return 0;
        double left = distance;
        int tick = 0;
        while (true) {
            double fall = (Math.pow(0.98, tick) - 1) * -3.92;
            if (left <= fall) return tick + left / fall;
            left -= fall;
            tick++;
        }
    }

    private static double across(int dx, int dz) {
        int x = Math.abs(dx), z = Math.abs(dz);
        int straight = Math.abs(x - z), diagonal = Math.min(x, z);
        return (straight + diagonal * SQRT2) * SPRINT;
    }

    private static double height(int dy) {
        return dy > 0 ? dy * JUMP : -dy * (fallTicks(2) / 2);
    }

    // Close enough to a spot: within this many blocks sideways and up or down.
    public static Goal near(int tx, int ty, int tz, int sideways, int upDown) {
        return new Goal() {
            @Override
            public boolean done(int x, int y, int z) {
                return Math.abs(x - tx) + Math.abs(z - tz) <= sideways && Math.abs(y - ty) <= upDown;
            }

            @Override
            public double guess(int x, int y, int z) {
                int dx = Math.max(0, Math.abs(x - tx) - sideways), dz = Math.max(0, Math.abs(z - tz) - sideways);
                int dy = Math.abs(y - ty) <= upDown ? 0 : ty - y;
                return across(dx, dz) + height(dy);
            }
        };
    }

    // Down (or up) to this height, wherever is quickest.
    public static Goal height(int ty, int slack) {
        return new Goal() {
            @Override
            public boolean done(int x, int y, int z) {
                return Math.abs(y - ty) <= slack;
            }

            @Override
            public double guess(int x, int y, int z) {
                return Math.abs(y - ty) <= slack ? 0 : height(ty - y);
            }
        };
    }

    // One thing it wants. `extra` makes it less attractive: a coal block is worth a bigger walk less than a diamond.
    public record Target(int x, int y, int z, double extra) {}

    // Baritone's mining goal: feet in the block, or head in it. Getting there means digging it out, and walking into the
    // spot picks up what dropped. Whichever target is cheapest overall wins.
    public static Goal anyOf(List<Target> targets) {
        Target[] all = targets.toArray(new Target[0]);
        return new Goal() {
            @Override
            public boolean done(int x, int y, int z) {
                for (Target t : all) {
                    if (t.x() == x && t.z() == z && (t.y() == y || t.y() == y + 1)) return true;
                }
                return false;
            }

            @Override
            public double guess(int x, int y, int z) {
                double best = Double.MAX_VALUE;
                for (Target t : all) {
                    best = Math.min(best, across(t.x() - x, t.z() - z) + height(t.y() - y) + t.extra());
                }
                return best;
            }

            @Override
            public double extra(int x, int y, int z) {
                double best = Double.MAX_VALUE;
                for (Target t : all) {
                    if (t.x() == x && t.z() == z && (t.y() == y || t.y() == y + 1)) best = Math.min(best, t.extra());
                }
                return best == Double.MAX_VALUE ? 0 : best;
            }
        };
    }

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static final class Search {
        private static final class Node {
            final int x, y, z;
            final double cost, guess;
            final Node parent;
            final Step step;
            boolean paid;

            Node(int x, int y, int z, double cost, double guess, Node parent, Step step) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.cost = cost;
                this.guess = guess;
                this.parent = parent;
                this.step = step;
            }

            double total() {
                return cost + GREED * guess;
            }
        }

        private final Terrain terrain;
        private final Goal goal;
        private final int maxNodes;
        private final boolean hasBlocks;
        private final Set<Long> favored;
        private final PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.total(), b.total()));
        private final Map<Long, Double> best = new HashMap<>();
        private final Node first;
        private final Node[] bestSoFar = new Node[COEFFICIENTS.length];
        private final double[] bestScore = new double[COEFFICIENTS.length];
        private final List<Cell> digging = new ArrayList<>(4);
        private Node found;
        private boolean partial;
        private int nodes;
        private Status status = Status.SEARCHING;

        public Search(Cell start, Goal goal, Terrain terrain, int maxNodes, boolean hasBlocks, Set<Long> favored) {
            this.goal = goal;
            this.terrain = terrain;
            this.maxNodes = maxNodes;
            this.hasBlocks = hasBlocks;
            this.favored = favored;
            first = new Node(start.x(), start.y(), start.z(), 0, goal.guess(start.x(), start.y(), start.z()), null, null);
            open.add(first);
            best.put(key(start.x(), start.y(), start.z()), 0.0);
            for (int i = 0; i < COEFFICIENTS.length; i++) {
                bestSoFar[i] = first;
                bestScore[i] = first.guess;
            }
        }

        public Status status() {
            return status;
        }

        // Only part of the way: it ran out of search before reaching the goal, so it should look again from there.
        public boolean partial() {
            return partial;
        }

        public int nodes() {
            return nodes;
        }

        // Look at up to this many more spots.
        public Status run(int budget) {
            for (int i = 0; i < budget && status == Status.SEARCHING; i++) {
                Node node = open.poll();
                if (node == null || nodes >= maxNodes) {
                    stop();
                    break;
                }
                if (!node.paid && node.cost > best.getOrDefault(key(node.x, node.y, node.z), Double.MAX_VALUE)) continue;
                nodes++;
                if (goal.done(node.x, node.y, node.z)) {
                    double extra = goal.extra(node.x, node.y, node.z);
                    if (extra <= 0 || node.paid) {
                        found = node;
                        status = Status.FOUND;
                        break;
                    }
                    // not finished until nothing else could beat this with its extra counted
                    Node paid = new Node(node.x, node.y, node.z, node.cost + extra, 0, node.parent, node.step);
                    paid.paid = true;
                    open.add(paid);
                }
                for (int c = 0; c < COEFFICIENTS.length; c++) {
                    double score = node.guess + node.cost / COEFFICIENTS[c];
                    if (score < bestScore[c]) {
                        bestScore[c] = score;
                        bestSoFar[c] = node;
                    }
                }
                expand(node);
            }
            return status;
        }

        // Time's up (or nowhere left to go): the best partial path, the way Baritone picks it.
        public void stop() {
            if (status != Status.SEARCHING) return;
            for (Node node : bestSoFar) {
                double dx = node.x - first.x, dy = node.y - first.y, dz = node.z - first.z;
                if (dx * dx + dy * dy + dz * dz >= MIN_PARTIAL_SQ) {
                    found = node;
                    partial = true;
                    status = Status.FOUND;
                    return;
                }
            }
            Node last = bestSoFar[bestSoFar.length - 1];
            if (last == first) {
                status = Status.NO_ROUTE;
            } else {
                found = last;
                partial = true;
                status = Status.FOUND;
            }
        }

        public List<Step> path() {
            List<Step> steps = new ArrayList<>();
            for (Node n = found; n != null && n.step != null; n = n.parent) steps.add(n.step);
            Collections.reverse(steps);
            return steps;
        }

        private void expand(Node n) {
            int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] side : sides) {
                int nx = n.x + side[0], nz = n.z + side[1];
                traverse(n, nx, nz);
                ascend(n, nx, nz);
                descend(n, nx, nz);
            }
            int[][] corners = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] corner : corners) diagonal(n, corner[0], corner[1]);
            digDown(n);
            pillar(n);
            swimUp(n);
        }

        // Standing on something it could put a block against: real ground, or the block the last move placed.
        private boolean canPlaceFrom(Node n) {
            if (terrain.floor(n.x, n.y - 1, n.z)) return true;
            Step s = n.step;
            return s != null && s.place() != null && s.place().x() == n.x && s.place().y() == n.y - 1 && s.place().z() == n.z;
        }

        private void traverse(Node n, int nx, int nz) {
            double dig = clear(nx, n.y + 1, nz, nx, n.y, nz);
            if (dig < 0) return;
            boolean wet = terrain.water(nx, n.y, nz) || terrain.water(n.x, n.y, n.z);
            if (terrain.floor(nx, n.y - 1, nz) || terrain.water(nx, n.y, nz)) {
                double walk = wet ? WATER : dig == 0 ? SPRINT : WALK;
                add(n, nx, n.y, nz, Kind.WALK, walk + dig, null);
            } else if (hasBlocks && !wet && terrain.canFill(nx, n.y - 1, nz) && canPlaceFrom(n)) {
                add(n, nx, n.y, nz, Kind.WALK, SNEAK + PLACE_EXTRA + dig, new Cell(nx, n.y - 1, nz));
            }
        }

        private void ascend(Node n, int nx, int nz) {
            if (!terrain.floor(nx, n.y, nz)) return;
            double dig = clear(n.x, n.y + 2, n.z, nx, n.y + 2, nz, nx, n.y + 1, nz);
            if (dig < 0) return;
            add(n, nx, n.y + 1, nz, Kind.UP, WALK + JUMP + dig, null);
        }

        private void descend(Node n, int nx, int nz) {
            double dig = clear(nx, n.y + 1, nz, nx, n.y, nz);
            if (dig < 0) return;
            List<Cell> top = List.copyOf(digging);
            double below = terrain.digCost(nx, n.y - 1, nz);
            if (below < 0) return;
            if (below > 0) {
                if (!terrain.floor(nx, n.y - 2, nz)) return;
                digging.clear();
                digging.addAll(top);
                digging.add(new Cell(nx, n.y - 1, nz));
                add(n, nx, n.y - 1, nz, Kind.DOWN, WALK_OFF + fallTicks(1) + CENTER + dig + below + DIG_EXTRA, null);
                return;
            }
            if (terrain.floor(nx, n.y - 1, nz)) return; // that's just walking across
            for (int fall = 1; fall <= MAX_FALL_INTO_WATER; fall++) {
                int y = n.y - fall;
                if (!terrain.open(nx, y, nz)) return;
                boolean water = terrain.water(nx, y, nz);
                if (fall > MAX_FALL && !water && !terrain.water(nx, y - 1, nz)) continue;
                if (water || terrain.floor(nx, y - 1, nz)) {
                    if (fall > MAX_FALL && !water) return;
                    digging.clear();
                    digging.addAll(top);
                    add(n, nx, y, nz, Kind.FALL, WALK_OFF + fallTicks(fall) + CENTER + dig, null);
                    return;
                }
            }
        }

        private void diagonal(Node n, int dx, int dz) {
            int nx = n.x + dx, nz = n.z + dz;
            if (!terrain.open(nx, n.y, nz) || !terrain.open(nx, n.y + 1, nz) || !terrain.floor(nx, n.y - 1, nz)) return;
            if (terrain.water(nx, n.y, nz) || terrain.water(n.x, n.y, n.z)) return;
            boolean a = terrain.open(nx, n.y, n.z) && terrain.open(nx, n.y + 1, n.z);
            boolean b = terrain.open(n.x, n.y, nz) && terrain.open(n.x, n.y + 1, nz);
            if (!a && !b) return;
            digging.clear();
            add(n, nx, n.y, nz, Kind.DIAGONAL, (a && b ? SPRINT : WALK) * SQRT2, null);
        }

        private void digDown(Node n) {
            double below = terrain.digCost(n.x, n.y - 1, n.z);
            if (below <= 0 || !terrain.floor(n.x, n.y - 2, n.z)) return;
            digging.clear();
            digging.add(new Cell(n.x, n.y - 1, n.z));
            add(n, n.x, n.y - 1, n.z, Kind.DIG_DOWN, fallTicks(1) + below + DIG_EXTRA, null);
        }

        private void pillar(Node n) {
            if (!hasBlocks || terrain.water(n.x, n.y, n.z) || !canPlaceFrom(n)) return;
            double dig = clear(n.x, n.y + 2, n.z);
            if (dig < 0) return;
            add(n, n.x, n.y + 1, n.z, Kind.PILLAR, JUMP + PLACE_EXTRA + dig, new Cell(n.x, n.y, n.z));
        }

        private void swimUp(Node n) {
            if (!terrain.water(n.x, n.y, n.z) || !terrain.open(n.x, n.y + 1, n.z) || !terrain.open(n.x, n.y + 2, n.z)) return;
            digging.clear();
            add(n, n.x, n.y + 1, n.z, Kind.SWIM_UP, WATER, null);
        }

        // What has to be dug to clear these spots (x, y, z in threes, top first). Negative = it can't be done.
        private double clear(int... cells) {
            digging.clear();
            double total = 0;
            for (int i = 0; i < cells.length; i += 3) {
                double cost = terrain.digCost(cells[i], cells[i + 1], cells[i + 2]);
                if (cost < 0) return -1;
                if (cost > 0) {
                    digging.add(new Cell(cells[i], cells[i + 1], cells[i + 2]));
                    total += cost + DIG_EXTRA;
                }
            }
            return total;
        }

        private void add(Node from, int x, int y, int z, Kind kind, double moveCost, Cell place) {
            long k = key(x, y, z);
            double cost = from.cost + (favored.contains(k) ? moveCost * FAVOR : moveCost);
            if (cost >= best.getOrDefault(k, Double.MAX_VALUE)) return;
            best.put(k, cost);
            Step step = new Step(new Cell(from.x, from.y, from.z), new Cell(x, y, z), kind, List.copyOf(digging), place, moveCost);
            open.add(new Node(x, y, z, cost, goal.guess(x, y, z), from, step));
        }
    }
}
