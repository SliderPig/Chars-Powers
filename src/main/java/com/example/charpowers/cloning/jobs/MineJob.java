package com.example.charpowers.cloning.jobs;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.cloning.CloneManager;
import com.example.charpowers.cloning.CloneWork;
import com.example.charpowers.cloning.RouteWalker;
import com.example.charpowers.cloning.LevelSpace;
import com.example.charpowers.cloning.model.AirSight;
import com.example.charpowers.cloning.model.OreRules;
import com.example.charpowers.cloning.model.StripPlan;
import com.example.charpowers.cloning.model.RoutePlanner;
import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import com.example.charpowers.cloning.model.Vein;
import com.example.charpowers.own.OreScan;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Mines the way a person does. First it looks round on the surface (walking, no x-ray): ores showing in the open air and
// the mouths of caves. Caves it follows down, mining what shows. When there are none it digs a staircase to good depth
// and strip mines: a main tunnel with branches every few blocks. It only knows ores that touch air it could see.
// Torches in the dark, a glance round in new places, and a short pause now and then.
public class MineJob implements CloneJob {
    private static final int LOW_OVERWORLD = -54, LOW_NETHER = 15, STATUS_EVERY = 100, STASH_EVERY = 100, TRIPS_BEFORE_GIVE_UP = 8,
            ON_THE_WAY = 5, SERIOUS_RANK = 5, ITEM_RANGE = 6, KEEP_BLOCKS = 64, MAX_ORES = 128, MAX_TARGETS = 32, TIDY_EVERY = 5,
            WAIT_FOR_DROPS = 12, WANDER_MAX = 4, WANDER_BLOCKS = 20, SIGHT_BUDGET = 250, GLANCE_TICKS = 16;
    private static final double EXTRA_PER_RANK = 60;

    enum State { LOOK, STAIRS, STRIP }

    private final Set<BlockPos> leftAlone = new HashSet<>();
    private final Vein vein = new Vein();
    private final List<BlockPos> ores = new ArrayList<>();
    private final Set<Cell> beenCaves = new HashSet<>();
    private final Random random = new Random();
    private CloneEntity me;
    private RouteWalker walker;
    private AirSight sight;
    private LevelSpace space;
    private Cell caveTarget;
    private StripPlan strip;
    private BlockPos lastTorch;
    private Direction heading;
    private State state = State.LOOK;
    private RoutePlanner.Goal goal;
    private String goalKey;
    private int goalNumber, turns, waitUntil, nextSight, wanders, leg, skippedInARow, strips, glanceUntil, nextPause, pauseUntil, stateSince;
    private float glanceYaw;
    private boolean underground, stripFirst;

    // For tests: skip the looking and the staircase and strip mine from where it stands.
    public MineJob startInStripMode() {
        stripFirst = true;
        return this;
    }

    @Override
    public String id() {
        return "mine";
    }

    @Override
    public String describe(CloneEntity c) {
        if (!ores.isEmpty()) return "Going for ores, " + ores.size() + " in sight";
        return switch (state) {
            case LOOK -> underground ? "Following a cave, Y " + c.getBlockY() : "Looking for ores and caves";
            case STAIRS -> "Digging down, Y " + c.getBlockY();
            case STRIP -> "Strip mining at Y " + c.getBlockY();
        };
    }

    @Override
    public boolean usesTools() {
        return true;
    }

    // Stone and such stay on the floor once it has enough to bridge and pillar with: you don't want a chest full of cobble.
    @Override
    public boolean refuses(ItemStack stack) {
        return CloneWork.isThrowaway(stack) && me != null && CloneWork.countThrowaway(me) >= KEEP_BLOCKS;
    }

    @Override
    public void start(CloneEntity c) {
        me = c;
        heading = c.getDirection();
        walker = new RouteWalker(c, RouteWalker.DigRule.MINING);
        space = new LevelSpace(c.serverLevel());
        state = stripFirst || c.getBlockY() <= lowest(c) + 2 ? State.STRIP : State.LOOK;
        glanceUntil = c.tickCount + GLANCE_TICKS;
        glanceYaw = c.getYRot();
        nextPause = c.tickCount + 300 + random.nextInt(400);
        c.mover.stop();
    }

    @Override
    public void stop(CloneEntity c) {
        walker.forget();
        c.mover.stop();
    }

    private int lowest(CloneEntity c) {
        return c.level().dimension() == Level.NETHER ? LOW_NETHER : LOW_OVERWORLD;
    }

    @Override
    public void tick(CloneEntity c) {
        ServerPlayer host = CloneManager.host(c);
        if (CloneWork.fightBack(c, host)) {
            walker.forget();
            goalKey = null;
            return;
        }
        if (c.isUsingItem()) return;
        if (!hasPickaxe(c)) {
            CloneWork.say(c, host, "has no pickaxe left. Put one in its chest.");
            c.setJob(new FollowJob());
            return;
        }
        if (c.tickCount % STASH_EVERY == 0 && !CloneWork.stashIfFull(c, host)) {
            CloneWork.say(c, host, "is full and has nowhere to put things. Mark it an emptier chest.");
            c.setJob(new FollowJob());
            return;
        }
        if (c.tickCount % STATUS_EVERY == 0) {
            BlockPos seen = CloneWork.lookingAt(c);
            CharPowers.LOG.info("[Cloning] {} mine: state={} at {} ground={} ores={} first={} vein={} goal={} searching={} stepsLeft={} heading={} digging={} seeing={} {} turns={} leg={}",
                    c.getGameProfile().getName(), state, c.blockPosition().toShortString(), c.onGround(), ores.size(), ores.isEmpty() ? "-" : ores.get(0).toShortString() + " " + c.level().getBlockState(ores.get(0)).getBlock(),
                    vein.size(), goalKey, walker.searching(), walker.stepsLeft(), heading, c.digging, seen == null ? "-" : seen.toShortString(), seen == null ? "" : c.level().getBlockState(seen).getBlock(), turns, leg);
        }
        if (c.tickCount % TIDY_EVERY == 0) tidyOres(c);
        look(c);
        if (digInReach(c)) return;
        // what it just broke needs a moment before it can be picked up, or it would walk off and leave it
        if (c.tickCount < waitUntil) {
            walker.standStill();
            return;
        }
        // a glance round when it gets somewhere new, a short stop now and then
        if (c.tickCount < glanceUntil) {
            walker.standStill();
            int left = glanceUntil - c.tickCount;
            CloneWork.glance(c, glanceYaw + (left > GLANCE_TICKS * 2 / 3 ? 55 : left > GLANCE_TICKS / 3 ? -55 : 0), left > GLANCE_TICKS / 2 ? -10 : 5);
            return;
        }
        if (c.tickCount >= nextPause) {
            pauseUntil = c.tickCount + 10 + random.nextInt(20);
            nextPause = pauseUntil + 300 + random.nextInt(500);
            glanceYaw = c.getYRot() + random.nextInt(90) - 45;
        }
        if (c.tickCount < pauseUntil) {
            walker.standStill();
            CloneWork.glance(c, glanceYaw, 0);
            return;
        }
        if (torchIfDark(c)) return;
        if (!ores.isEmpty()) goForOres(c, host);
        else if (state == State.LOOK) lookAround(c, host);
        else if (state == State.STAIRS) stairs(c, host);
        else strip(c, host);
    }

    private void newPlace(CloneEntity c) {
        glanceUntil = c.tickCount + GLANCE_TICKS;
        glanceYaw = c.getYRot();
    }

    private void go(State next, CloneEntity c) {
        state = next;
        stateSince = c.tickCount;
        goalKey = null;
        walker.forget();
        turns = 0;
        newPlace(c);
    }

    private static boolean hasPickaxe(CloneEntity c) {
        for (ItemStack s : c.getInventory().items) if (s.getItem() instanceof PickaxeItem) return true;
        return false;
    }

    private boolean wanted(CloneEntity c, BlockPos pos) {
        BlockState state = c.level().getBlockState(pos);
        return OreScan.isOre(state) && !leftAlone.contains(pos) && CloneWork.hasToolFor(c, state);
    }

    private static Cell cell(BlockPos pos) {
        return new Cell(pos.getX(), pos.getY(), pos.getZ());
    }

    private static int rank(CloneEntity c, BlockPos pos) {
        return OreRules.rank(BuiltInRegistries.BLOCK.getKey(c.level().getBlockState(pos).getBlock()).toString());
    }

    // Ores that have gone are crossed off, and what was touching them becomes its vein to finish.
    private void tidyOres(CloneEntity c) {
        boolean gone = false;
        for (BlockPos pos : new ArrayList<>(ores)) {
            if (OreScan.isOre(c.level().getBlockState(pos)) && !leftAlone.contains(pos)) continue;
            ores.remove(pos);
            gone = true;
            vein.broke(cell(pos), part -> wanted(c, new BlockPos(part.x(), part.y(), part.z())));
        }
        if (!gone) return;
        Cell at = cell(c.blockPosition());
        Cell part;
        while ((part = vein.next(at, found -> wanted(c, new BlockPos(found.x(), found.y(), found.z())))) != null) {
            BlockPos pos = new BlockPos(part.x(), part.y(), part.z());
            if (ores.contains(pos)) break;
            ores.add(pos);
        }
        goalKey = null;
        waitUntil = c.tickCount + WAIT_FOR_DROPS;
    }

    // What it can see from here: the open air joined to where it stands, and ore touching that air. A bit each tick.
    private void look(CloneEntity c) {
        if (sight == null) {
            if (c.tickCount < nextSight || (!ores.isEmpty() && c.tickCount % 100 != 0)) return;
            Cell at = cell(c.blockPosition());
            sight = state == State.LOOK ? new AirSight(space, at, 24, 24, 2, 8000) : new AirSight(space, at, 9, 6, 3, 1500);
        }
        if (!sight.step(SIGHT_BUDGET)) return;
        boolean deep = c.getBlockY() <= lowest(c) + 16;
        BlockPos at = c.blockPosition();
        Set<BlockPos> have = new HashSet<>(ores);
        boolean added = false;
        for (Cell found : sight.ores()) {
            BlockPos pos = new BlockPos(found.x(), found.y(), found.z());
            if (have.contains(pos) || !wanted(c, pos)) continue;
            // on the way down it doesn't stop for coal and copper, unless they're right there
            if (state == State.STAIRS && !deep && rank(c, pos) < SERIOUS_RANK && pos.distManhattan(at) > ON_THE_WAY) continue;
            ores.add(pos);
            added = true;
        }
        if (state == State.LOOK && caveTarget == null && c.getBlockY() > lowest(c) + 4) {
            caveTarget = sight.deepestCave(3, beenCaves, 4);
            if (caveTarget != null && !underground && Math.abs(caveTarget.y() - c.getBlockY()) < 2) {
                // a hollow at the same level, not a cave worth the walk
                beenCaves.add(caveTarget);
                caveTarget = null;
            }
        }
        sight = null;
        nextSight = c.tickCount + 40;
        if (ores.size() > MAX_ORES) {
            ores.sort(Comparator.comparingDouble(pos -> extraFor(c, pos) + 4 * Math.sqrt(pos.distSqr(at))));
            ores.subList(MAX_ORES, ores.size()).clear();
        }
        if (added) goalKey = null;
    }

    // Lights a torch where it's dark, if it carries any.
    private boolean torchIfDark(CloneEntity c) {
        if (c.tickCount % 20 != 0 || !c.onGround() || !CloneWork.hasTorch(c)) return false;
        BlockPos at = c.blockPosition();
        if (c.level().canSeeSky(at) || c.level().getBrightness(LightLayer.BLOCK, at) > 3) return false;
        if (lastTorch != null && lastTorch.distManhattan(at) < 6 && c.level().getBlockState(lastTorch).is(Blocks.TORCH)) return false;
        if (!c.level().getBlockState(at).isAir()) return false;
        if (CloneWork.placeTorch(c)) {
            lastTorch = at;
            waitUntil = c.tickCount + 6;
            return true;
        }
        return false;
    }

    // Anything it can already reach and see, it just digs, the way you would.
    private boolean digInReach(CloneEntity c) {
        if (!c.onGround() || ores.isEmpty()) return false;
        BlockPos best = null;
        double nearest = (CloneWork.REACH - 0.5) * (CloneWork.REACH - 0.5);
        for (BlockPos pos : ores) {
            double far = pos.distToCenterSqr(c.getEyePosition());
            if (far < nearest && CloneWork.canSee(c, pos)) {
                nearest = far;
                best = pos;
            }
        }
        if (best == null) return false;
        if (!CloneWork.dig(c, best, c.level().getBlockState(best))) {
            leftAlone.add(best);
            return false;
        }
        return true;
    }

    private double extraFor(CloneEntity c, BlockPos pos) {
        if (vein.contains(cell(pos))) return 0;
        return (OreRules.TOP_RANK - rank(c, pos)) * EXTRA_PER_RANK;
    }

    private void goForOres(CloneEntity c, ServerPlayer host) {
        if (goalKey == null) {
            BlockPos at = c.blockPosition();
            List<RoutePlanner.Target> targets = new ArrayList<>();
            for (BlockPos pos : ores) targets.add(new RoutePlanner.Target(pos.getX(), pos.getY(), pos.getZ(), extraFor(c, pos)));
            for (ItemEntity item : c.level().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(ITEM_RANGE), e -> e.isAlive() && !CloneWork.isThrowaway(e.getItem()) && !e.hasPickUpDelay())) {
                BlockPos pos = item.blockPosition();
                targets.add(new RoutePlanner.Target(pos.getX(), pos.getY(), pos.getZ(), 0));
            }
            targets.sort(Comparator.comparingDouble(t -> t.extra() + 4 * (Math.abs(t.x() - at.getX()) + Math.abs(t.y() - at.getY()) + Math.abs(t.z() - at.getZ()))));
            if (targets.size() > MAX_TARGETS) targets = new ArrayList<>(targets.subList(0, MAX_TARGETS));
            goal = RoutePlanner.anyOf(targets);
            goalKey = "ores " + (++goalNumber);
        }
        RouteWalker.Result result = walker.go(goalKey, goal);
        if (result == RouteWalker.Result.ARRIVED) {
            goalKey = null;
        } else if (result == RouteWalker.Result.NO_ROUTE) {
            // none of these can be reached from here: stop trying them, the nearest first
            ores.sort(Comparator.comparingDouble(pos -> pos.distSqr(c.blockPosition())));
            for (BlockPos pos : new ArrayList<>(ores.subList(0, Math.min(8, ores.size())))) {
                leftAlone.add(pos);
                ores.remove(pos);
            }
            walker.forget();
            goalKey = null;
        }
    }

    // Surface and caves: walk to the deepest cave in sight, else look elsewhere, else give up and dig down.
    private void lookAround(CloneEntity c, ServerPlayer host) {
        if (caveTarget != null) {
            if (goalKey == null) {
                goal = RoutePlanner.near(caveTarget.x(), caveTarget.y(), caveTarget.z(), 1, 1);
                goalKey = "cave " + (++goalNumber);
            }
            RouteWalker.Result result = walker.go(goalKey, goal);
            if (result != RouteWalker.Result.WORKING) {
                if (result == RouteWalker.Result.ARRIVED) {
                    underground = !c.level().canSeeSky(c.blockPosition());
                    newPlace(c);
                }
                beenCaves.add(caveTarget);
                caveTarget = null;
                goalKey = null;
                nextSight = 0;
                walker.forget();
            }
            return;
        }
        if (sight != null || c.tickCount - stateSince < 30) {
            walker.standStill(); // let the look finish before choosing
            return;
        }
        // nothing here. Underground that means the cave has run out; above ground it looks a few places, then digs.
        if (underground || wanders >= WANDER_MAX) {
            go(State.STAIRS, c);
            return;
        }
        if (goalKey == null) {
            BlockPos at = c.blockPosition();
            goal = RoutePlanner.near(at.getX() + heading.getStepX() * WANDER_BLOCKS, at.getY(), at.getZ() + heading.getStepZ() * WANDER_BLOCKS, 3, 40);
            goalKey = "wander " + (++goalNumber);
        }
        RouteWalker.Result result = walker.go(goalKey, goal);
        if (result != RouteWalker.Result.WORKING) {
            wanders++;
            heading = heading.getClockWise();
            goalKey = null;
            walker.forget();
            nextSight = 0;
            newPlace(c);
        }
    }

    // A staircase down to good depth, mining what shows on the way.
    private void stairs(CloneEntity c, ServerPlayer host) {
        if (c.getBlockY() <= lowest(c) + 1) {
            go(State.STRIP, c);
            return;
        }
        if (goalKey == null) {
            goal = RoutePlanner.height(lowest(c), 0);
            goalKey = "stairs " + (++goalNumber);
        }
        RouteWalker.Result result = walker.go(goalKey, goal);
        if (result == RouteWalker.Result.ARRIVED) {
            go(State.STRIP, c);
        } else if (result == RouteWalker.Result.NO_ROUTE) {
            walker.forget();
            goalKey = null;
            heading = heading.getClockWise();
            if (++turns >= TRIPS_BEFORE_GIVE_UP) giveUp(c, host);
        }
    }

    // A main tunnel with a branch each side every few blocks, then the next strip alongside.
    private void strip(CloneEntity c, ServerPlayer host) {
        if (strip == null) {
            strip = new StripPlan(cell(c.blockPosition()), heading.getStepX(), heading.getStepZ());
            leg = 0;
            strips++;
        }
        if (leg >= strip.size()) {
            strip = new StripPlan(strip.nextOrigin(), heading.getStepX(), heading.getStepZ());
            leg = 0;
            strips++;
            goalKey = null;
        }
        StripPlan.Leg now = strip.leg(leg);
        if (goalKey == null) {
            goal = RoutePlanner.near(now.to().x(), now.to().y(), now.to().z(), 0, 1);
            goalKey = "strip " + strips + ":" + leg;
        }
        RouteWalker.Result result = walker.go(goalKey, goal);
        if (result == RouteWalker.Result.WORKING) return;
        goalKey = null;
        if (result == RouteWalker.Result.ARRIVED) {
            skippedInARow = 0;
            leg++;
            if (now.branch() || random.nextInt(4) == 0) newPlace(c);
        } else {
            // lava, water or a drop in the way: it turns back and does the next one
            walker.forget();
            leg++;
            if (++skippedInARow >= TRIPS_BEFORE_GIVE_UP) {
                strip = null;
                heading = heading.getClockWise();
                skippedInARow = 0;
                if (++turns >= TRIPS_BEFORE_GIVE_UP) giveUp(c, host);
            }
        }
    }

    private void giveUp(CloneEntity c, ServerPlayer host) {
        CloneWork.say(c, host, "is stuck in the mine and has come back.");
        c.setJob(new FollowJob());
    }
}
