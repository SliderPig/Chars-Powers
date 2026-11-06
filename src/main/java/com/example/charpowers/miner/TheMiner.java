package com.example.charpowers.miner;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.attack.Wreck;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.miner.model.MinerRules;
import com.example.charpowers.own.OreScan;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Fist as strong as netherite, night vision, efficiency, extra ores, and big digging keys. Area abilities only break
// the blocks in the charpowers:natural tag, so bricks, planks and cobble are safe.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class TheMiner extends OwnPower {
    public static final TheMiner POWER = new TheMiner();
    public static final TagKey<Block> NATURAL = BlockTags.create(new ResourceLocation(CharPowers.MODID, "natural"));

    private static final String MINED = "mined", CHUNKS = "chunks", DONE = "doneChunks";
    private static final List<DigJob> JOBS = new ArrayList<>();
    private static final Map<UUID, Long> LAST_MINED = new HashMap<>();
    private static final Map<UUID, OreScan> LODES = new HashMap<>();
    private static final List<ChunkCheck> CHECKS = new ArrayList<>();
    private static boolean areaMining;

    private TheMiner() {
        super(Mastery.THE_MINER, Items.NETHERITE_PICKAXE);
    }

    @Override
    public String unlockMessage() { return "Four whole chunks, top to bottom, by hand. The rock knows you now."; }

    @Override
    public String awakenedMessage() { return "A million blocks. Strip Mine and Motherlode are yours."; }

    @Override
    public void everySecond(ServerPlayer p) {
        if (!p.hasEffect(MobEffects.NIGHT_VISION) || p.getEffect(MobEffects.NIGHT_VISION).getDuration() < 300) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        }
        long now = p.level().getGameTime();
        if (now - LAST_MINED.getOrDefault(p.getUUID(), -1000L) < 60) Mastery.add(p, id, 1);
        if (questReady(p) && data(p).getLong(MINED) >= MinerRules.BLOCKS_FOR_QUEST) awaken(p);
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        switch (ability) {
            case "miner_stairs" -> JOBS.add(new DigJob(p, stairs(p).iterator(), MinerRules.STAIR_PER_TICK, true, true));
            case "mine_ball" -> {
                List<BlockPos> ball = Wreck.sphere(p.blockPosition(), Setting.MINER_BALL.i());
                ball.sort((a, b) -> b.getY() - a.getY());
                JOBS.add(new DigJob(p, ball.iterator(), MinerRules.BALL_PER_TICK, false, false));
                Fx.sound(p, PowerSounds.ROCK_CRUNCH, 1f, 0.8f);
            }
            case "strip_mine" -> {
                JOBS.add(new DigJob(p, wholeChunk(p).iterator(), MinerRules.STRIP_PER_TICK, true, true));
                Fx.sound(p, PowerSounds.ROCK_CRUNCH, 1f, 0.7f);
            }
            case "motherlode" -> LODES.put(p.getUUID(), new OreScan(p.serverLevel(), p.blockPosition(), Setting.MINER_MOTHERLODE.i()));
            default -> {}
        }
    }

    // Down 30 levels, 2 wide. Standing room is 2 tall, plus 1 more so you can walk down it without hitting your head.
    private static List<BlockPos> stairs(ServerPlayer p) {
        Direction ahead = p.getDirection(), side = ahead.getClockWise();
        BlockPos start = p.blockPosition();
        List<BlockPos> out = new ArrayList<>();
        for (int i = 1; i <= MinerRules.STAIR_LEVELS; i++) {
            BlockPos floor = start.relative(ahead, i).below(i);
            for (int w = 0; w < 2; w++) {
                for (int h = MinerRules.STAIR_TALL - 1; h >= 0; h--) out.add(floor.relative(side, w).above(h));
            }
        }
        return out;
    }

    private static Iterable<BlockPos> wholeChunk(ServerPlayer p) {
        ChunkPos c = p.chunkPosition();
        int top = p.level().getMaxBuildHeight() - 1, bottom = p.level().getMinBuildHeight();
        return () -> new Iterator<>() {
            private int y = top, i = 0;

            @Override
            public boolean hasNext() { return y >= bottom; }

            @Override
            public BlockPos next() {
                BlockPos pos = new BlockPos(c.getMinBlockX() + (i & 15), y, c.getMinBlockZ() + (i >> 4));
                if (++i == 256) {
                    i = 0;
                    y--;
                }
                return pos;
            }
        };
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!CHECKS.isEmpty() && CHECKS.get(0).step(e)) CHECKS.remove(0);
        if (!LODES.isEmpty()) tickLodes(e);
        if (JOBS.isEmpty()) return;
        Iterator<DigJob> it = JOBS.iterator();
        while (it.hasNext()) {
            DigJob job = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(job.player);
            if (p == null) {
                it.remove();
                continue;
            }
            if (!job.step(p)) continue;
            it.remove();
            job.handOver(p);
            if (job.counts) POWER.data(p).putLong(MINED, POWER.data(p).getLong(MINED) + job.broken);
            whisper(p, job.broken + " blocks.");
        }
    }

    // Motherlode: every ore near you comes straight to your bag, and plain rock stays where it was.
    private static void tickLodes(TickEvent.ServerTickEvent e) {
        Iterator<Map.Entry<UUID, OreScan>> it = LODES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, OreScan> entry = it.next();
            if (!entry.getValue().step(8192)) continue;
            it.remove();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null || p.level() != entry.getValue().level) continue;
            ServerLevel level = p.serverLevel();
            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            int count = 0;
            List<BlockPos> spots = entry.getValue().nearest(4096);
            Map<BlockPos, Integer> ranks = new HashMap<>();
            for (BlockPos pos : spots) {
                var key = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
                ranks.put(pos, key == null ? 0 : MinerRules.oreRank(key.getPath()));
            }
            BlockPos from = p.blockPosition();
            spots.sort((a, b) -> MinerRules.compareRarity(ranks.get(a), a.distSqr(from), ranks.get(b), b.distSqr(from)));
            List<BlockPos> opened = new ArrayList<>();
            for (BlockPos pos : spots) {
                BlockState state = level.getBlockState(pos);
                if (!OreScan.isOre(state) || !Wreck.mayBreak(level, pos, p)) continue;
                opened.add(pos);
                for (ItemStack drop : Block.getDrops(state, level, pos, null, p, tool)) Fx.give(p, drop);
                String name = state.getBlock().getDescriptionId();
                BlockState rock = name.contains("deepslate") ? Blocks.DEEPSLATE.defaultBlockState()
                        : name.contains("nether") || name.contains("quartz") ? Blocks.NETHERRACK.defaultBlockState() : Blocks.STONE.defaultBlockState();
                level.setBlock(pos, rock, Block.UPDATE_CLIENTS);
                count++;
            }
            int drained = drain(level, p, opened);
            Fx.sound(p, PowerSounds.ROCK_CRUNCH, 1f, 1f);
            whisper(p, count + " ores flew to you." + (drained > 0 ? " Water and lava cleared." : ""));
        }
    }

    // Water and lava right next to the spots Motherlode opened up are taken away, up to a limit (never a whole ocean).
    private static int drain(ServerLevel level, ServerPlayer p, List<BlockPos> opened) {
        int cleared = 0, reach = MinerRules.FLUID_REACH;
        for (BlockPos ore : opened) {
            if (cleared >= MinerRules.FLUID_CAP) break;
            for (BlockPos pos : BlockPos.betweenClosed(ore.offset(-reach, -reach, -reach), ore.offset(reach, reach, reach))) {
                if (cleared >= MinerRules.FLUID_CAP) break;
                BlockState s = level.getBlockState(pos);
                if (s.getFluidState().isEmpty() || !level.isLoaded(pos)) continue; // the ore beside it already passed the protection check
                if (s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                    level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    cleared++;
                } else if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                    level.setBlock(pos.immutable(), s.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false), Block.UPDATE_CLIENTS);
                    cleared++;
                }
            }
        }
        return cleared;
    }

    private static boolean areaOn(ServerPlayer p, String toggle) {
        return AbilityUtil.isEnabled(p, POWER.id, toggle);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (Wreck.BREAKING || !(e.getPlayer() instanceof ServerPlayer p)) return;
        boolean owner = POWER.owns(p);
        if (!owner) {
            if (e.getState().is(NATURAL)) countChunk(p, e.getPos());
            return;
        }
        CompoundTag d = POWER.data(p);
        d.putLong(MINED, d.getLong(MINED) + 1);
        if (areaMining || HiddenRuntime.switchedOff(p)) return;
        LAST_MINED.put(p.getUUID(), p.level().getGameTime());
        extraOre(p, e);
        int radius = areaOn(p, "mine_5x5") ? 2 : areaOn(p, "mine_3x3") ? 1 : 0;
        if (radius > 0) area(p, e.getPos(), radius);
    }

    // Finds more ore than normal: one more roll half the time, on top of fortune.
    private static void extraOre(ServerPlayer p, BlockEvent.BreakEvent e) {
        ItemStack tool = p.getMainHandItem();
        if (!OreScan.isOre(e.getState()) || p.getRandom().nextDouble() >= Setting.MINER_EXTRA_ORE.d()) return;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) return;
        ServerLevel level = p.serverLevel();
        for (ItemStack drop : Block.getDrops(e.getState(), level, e.getPos(), null, p, tool.isEmpty() ? new ItemStack(Items.NETHERITE_PICKAXE) : tool)) {
            Block.popResource(level, e.getPos(), drop);
        }
    }

    // 3x3 or 5x5, flat against the face you're mining, using the tool in your hand the normal way.
    private static void area(ServerPlayer p, BlockPos center, int radius) {
        HitResult hit = p.pick(p.isCreative() ? 6 : 5.5, 0f, false);
        Direction face = hit instanceof BlockHitResult b ? b.getDirection() : Direction.UP;
        Direction a = face.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        Direction c = face.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        areaMining = true;
        try {
            for (int i = -radius; i <= radius; i++) {
                for (int j = -radius; j <= radius; j++) {
                    if (i == 0 && j == 0) continue;
                    BlockPos pos = center.relative(a, i).relative(c, j);
                    if (p.level().getBlockState(pos).is(NATURAL)) p.gameMode.destroyBlock(pos);
                }
            }
        } finally {
            areaMining = false;
        }
    }

    // Fist mining and efficiency. These run on the PC too, so both sides agree how fast a block breaks.
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        if (!AbilityUtil.hasPower(p, POWER.id)) return;
        int bonus = MinerRules.efficiencySpeed(MinerRules.efficiency(Mastery.levelAnySide(p, POWER.id)));
        BlockState state = e.getState();
        if (p.getMainHandItem().isEmpty()) {
            e.setNewSpeed(Math.max(e.getNewSpeed(), MinerRules.FIST_SPEED + bonus));
        } else if (p.getMainHandItem().getDestroySpeed(state) > 1f) {
            e.setNewSpeed(e.getNewSpeed() + bonus);
        }
    }

    @SubscribeEvent
    public static void onHarvest(PlayerEvent.HarvestCheck e) {
        if (e.getEntity().getMainHandItem().isEmpty() && AbilityUtil.hasPower(e.getEntity(), POWER.id)) e.setCanHarvest(true);
    }

    // --- How to get it: 4 whole chunks in a 2x2 square, surface to bedrock, by hand.

    private static void countChunk(ServerPlayer p, BlockPos pos) {
        CompoundTag d = POWER.data(p);
        CompoundTag chunks = d.getCompound(CHUNKS);
        String key = (pos.getX() >> 4) + "," + (pos.getZ() >> 4);
        int count = chunks.getInt(key) + 1;
        chunks.putInt(key, count);
        if (chunks.size() > MinerRules.COUNTED_CHUNKS) forgetSmallest(chunks);
        d.put(CHUNKS, chunks);
        if (count >= MinerRules.CHUNK_CHECK_AFTER && count % MinerRules.CHUNK_CHECK_EVERY == 0) {
            CHECKS.add(new ChunkCheck(p, pos.getX() >> 4, pos.getZ() >> 4));
        }
    }

    private static void forgetSmallest(CompoundTag chunks) {
        String smallest = null;
        int least = Integer.MAX_VALUE;
        for (String k : chunks.getAllKeys()) {
            if (chunks.getInt(k) < least) {
                least = chunks.getInt(k);
                smallest = k;
            }
        }
        if (smallest != null) chunks.remove(smallest);
    }

    // Looks through one chunk, one slice a tick, for natural blocks still there.
    private static final class ChunkCheck {
        private final UUID player;
        private final ServerLevel level;
        private final int cx, cz;
        private int section, left;

        ChunkCheck(ServerPlayer p, int cx, int cz) {
            this.player = p.getUUID();
            this.level = p.serverLevel();
            this.cx = cx;
            this.cz = cz;
        }

        boolean step(TickEvent.ServerTickEvent e) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) return true;
            LevelChunkSection s = chunk.getSection(section);
            if (!s.hasOnlyAir() && s.maybeHas(state -> state.is(NATURAL))) {
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    if (s.getBlockState(x, y, z).is(NATURAL)) left++;
                }
            }
            if (left > MinerRules.CHUNK_LEFTOVERS) return true;
            if (++section < level.getSectionsCount()) return false;
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(player);
            if (p != null) chunkDone(p, cx, cz);
            return true;
        }
    }

    private static void chunkDone(ServerPlayer p, int cx, int cz) {
        CompoundTag d = POWER.data(p);
        long[] done = d.getLongArray(DONE);
        long me = MinerRules.chunk(cx, cz);
        Set<Long> all = new HashSet<>();
        for (long c : done) all.add(c);
        if (!all.add(me)) return;
        long[] next = new long[all.size()];
        int i = 0;
        for (long c : all) next[i++] = c;
        d.putLongArray(DONE, next);
        whisper(p, "That's a whole chunk, top to bottom. " + all.size() + " so far.");
        if (MinerRules.twoByTwo(all)) POWER.unlock(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_MINED.remove(e.getEntity().getUUID());
        LODES.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        JOBS.clear();
        LODES.clear();
        CHECKS.clear();
    }
}
