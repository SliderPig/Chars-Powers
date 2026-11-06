package com.example.charpowers.hidden.attack;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.HiddenConfig;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.BreakRule;
import com.example.charpowers.hidden.model.WreckQueue;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Ground wrecking for the ⛏ attack powers (craters, tunnels, torn-up ground). The damage STAYS (my choice),
// and blocks drop nothing. Mobs wreck exactly like players do (user: "no discriminatory").
// Never broken: blocks that hold things (chests...), obsidian and harder, unbreakable blocks (model/BreakRule),
// unloaded or out-of-world spots, and anything a protection says no to:
//  - Forge's BreakEvent (Forge claim mods),
//  - spawn protection (server.properties),
//  - on Arclight, Bukkit's BlockBreakEvent (claim / region plugins like GriefPrevention or WorldGuard), found by
//    itself at runtime; with no such plugin nothing says no.
// A player's wreck asks as that player; a mob's asks as a stranger called [CharPowers].
// Big wrecks are spread over ticks (WreckQueue.BREAKS_PER_TICK per level), nearest first.
public final class Wreck {
    // True while a wreck is asking "may I break this?", so the holder's own mining powers (Chain Miner, Timber,
    // Fortune Finger...) don't react to every crater block.
    public static volatile boolean BREAKING = false;

    private record Job(BlockPos pos, @Nullable ServerPlayer player) {}

    private static final Map<ResourceKey<Level>, WreckQueue<Job>> QUEUES = new HashMap<>();
    private static final GameProfile STRANGER =
            new GameProfile(UUID.nameUUIDFromBytes("[CharPowers]".getBytes(StandardCharsets.UTF_8)), "[CharPowers]");

    // Queue these spots to be wrecked (nearest to `centre` first).
    public static void blocks(PowerUser u, Collection<BlockPos> spots, BlockPos centre) {
        if (!HiddenConfig.ATTACK_BREAKS_BLOCKS.get() || spots.isEmpty()) return;
        List<Job> jobs = new ArrayList<>(spots.size());
        for (BlockPos p : spots) jobs.add(new Job(p.immutable(), u.player()));
        QUEUES.computeIfAbsent(u.level().dimension(), k -> new WreckQueue<>())
                .add(jobs, j -> j.pos().distSqr(centre));
    }

    public static void clearAll() { QUEUES.clear(); }

    public static void tick(ServerLevel l) {
        WreckQueue<Job> q = QUEUES.get(l.dimension());
        if (q == null || q.isEmpty()) return;
        int n = 0;
        for (Job j : q.take(WreckQueue.BREAKS_PER_TICK, j -> breakable(l, j.pos()))) {
            try {
                if (allowed(l, j)) {
                    BlockState was = l.getBlockState(j.pos());
                    l.setBlock(j.pos(), was.getFluidState().createLegacyBlock(), 3); // no drops; a waterlogged block leaves its water
                    if (n++ % 8 == 0) l.levelEvent(2001, j.pos(), Block.getId(was)); // some break dust + sound, not 500 a tick
                }
            } catch (RuntimeException e) {
                CharPowers.LOG.error("Wrecking a block at {} failed", j.pos(), e);
            }
        }
    }

    private static boolean breakable(ServerLevel l, BlockPos p) {
        if (!l.isInWorldBounds(p) || !l.isLoaded(p)) return false;
        BlockState s = l.getBlockState(p);
        return BreakRule.canWreck(HiddenConfig.ATTACK_BREAKS_BLOCKS.get(), s.isAir(), s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock,
                s.hasBlockEntity(), s.getDestroySpeed(l, p));
    }

    // For other powers that break blocks: the same rules and the same protections.
    public static boolean mayBreak(ServerLevel l, BlockPos pos, ServerPlayer p) {
        return breakable(l, pos) && allowed(l, new Job(pos, p));
    }

    // For things that change a spot without breaking it (a moment of fire on the ground): just the protections.
    public static boolean mayTouch(ServerLevel l, BlockPos pos, ServerPlayer p) {
        return allowed(l, new Job(pos, p));
    }

    // Asks every protection whether this block may break.
    private static boolean allowed(ServerLevel l, Job j) {
        ServerPlayer p = j.player() != null && !j.player().isRemoved() && j.player().level() == l ? j.player() : null;
        ServerPlayer asker = p != null ? p : FakePlayerFactory.get(l, STRANGER);
        if (p != null && l.getServer().isUnderSpawnProtection(l, j.pos(), p)) return false;
        if (p == null && l.getServer().isUnderSpawnProtection(l, j.pos(), asker)) return false;
        BREAKING = true;
        try {
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l, j.pos(), l.getBlockState(j.pos()), asker))) return false;
            return Bukkit.allows(l, j.pos(), asker);
        } finally {
            BREAKING = false;
        }
    }

    // Arclight (Forge + Bukkit plugins): fire Bukkit's BlockBreakEvent by reflection so claim plugins can say no.
    // On a plain Forge server (or singleplayer) there is no Bukkit, and this always says yes.
    private static final class Bukkit {
        private static boolean looked, there, warned, broken;
        private static Method callEvent, craftBlockAt, bukkitEntity;
        private static Object pluginManager;
        private static java.lang.reflect.Constructor<?> breakEvent;
        private static Method isCancelled;

        static boolean allows(ServerLevel l, BlockPos pos, ServerPlayer asker) {
            if (!find()) return true;
            if (broken) return false;
            try {
                Object block = craftBlockAt.invoke(null, l, pos);
                Object player = bukkitEntity.invoke(asker);
                Object ev = breakEvent.newInstance(block, player);
                callEvent.invoke(pluginManager, ev);
                return !(boolean) isCancelled.invoke(ev);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Bukkit is here but asking failed: say no (claims stay safe), and say so in the log once
                if (!warned) CharPowers.LOG.warn("Couldn't ask Bukkit plugins about wrecking a block: attack powers won't break blocks", e);
                warned = true;
                return false;
            }
        }

        private static boolean find() {
            if (looked) return there;
            looked = true;
            Class<?> bukkit;
            try {
                bukkit = Class.forName("org.bukkit.Bukkit");
            } catch (ClassNotFoundException | LinkageError e) {
                return false; // no Bukkit here: plain Forge (or singleplayer), nothing to ask
            }
            try {
                Object server = bukkit.getMethod("getServer").invoke(null);
                if (server == null) return false;
                pluginManager = bukkit.getMethod("getPluginManager").invoke(null);
                Class<?> event = Class.forName("org.bukkit.event.Event");
                callEvent = pluginManager.getClass().getMethod("callEvent", event);
                String craft = server.getClass().getPackage().getName(); // org.bukkit.craftbukkit.v1_20_R1
                Class<?> craftBlock = Class.forName(craft + ".block.CraftBlock");
                for (Method m : craftBlock.getMethods()) {
                    if (m.getName().equals("at") && m.getParameterCount() == 2) craftBlockAt = m;
                }
                bukkitEntity = net.minecraft.world.entity.Entity.class.getMethod("getBukkitEntity");
                Class<?> blockClass = Class.forName("org.bukkit.block.Block");
                Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
                Class<?> bbe = Class.forName("org.bukkit.event.block.BlockBreakEvent");
                breakEvent = bbe.getConstructor(blockClass, playerClass);
                isCancelled = bbe.getMethod("isCancelled");
                if (craftBlockAt == null) throw new NoSuchMethodException("CraftBlock.at");
                CharPowers.LOG.info("Attack powers: Bukkit found, wrecking asks claim plugins first");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                // Bukkit is here but its pieces couldn't be found: never wreck rather than ignore claims
                CharPowers.LOG.warn("Bukkit found but its block-break check couldn't be set up: attack powers won't break blocks", e);
                broken = true;
            }
            there = true;
            return true;
        }
    }

    // --- shapes ---

    public static List<BlockPos> sphere(BlockPos c, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            if (x * x + y * y + z * z <= r * r + r) out.add(c.offset(x, y, z));
        }
        return out;
    }

    // A crater: the lower half of a ball under `c`, plus everything above the ground inside the rim
    // (so loose stuff sitting on the crater goes too, up to r/2 high).
    public static List<BlockPos> bowl(BlockPos c, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            int flat = x * x + z * z;
            if (flat > r * r + r) continue;
            int depth = (int) Math.round(Math.sqrt(Math.max(0, r * r - flat)) * 0.6);
            for (int y = -depth; y <= r / 2; y++) out.add(c.offset(x, y, z));
        }
        return out;
    }

    public static List<BlockPos> box(BlockPos a, BlockPos b) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }

    // A straight tunnel `width` blocks across, `length` long.
    public static List<BlockPos> tunnel(Vec3 from, Vec3 dir, int length, int width) {
        Set<BlockPos> out = new LinkedHashSet<>();
        Vec3 d = dir.normalize();
        double half = (width - 1) / 2.0;
        for (double s = 0; s <= length; s += 0.5) {
            Vec3 at = from.add(d.scale(s));
            for (double x = -half; x <= half; x++) for (double y = -half; y <= half; y++) for (double z = -half; z <= half; z++) {
                out.add(BlockPos.containing(at.x + x, at.y + y, at.z + z));
            }
        }
        return new ArrayList<>(out);
    }

    private Wreck() {}
}
