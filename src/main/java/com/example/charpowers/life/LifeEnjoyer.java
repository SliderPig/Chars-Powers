package com.example.charpowers.life;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.model.RoutePlanner.Cell;
import com.example.charpowers.cloning.model.Vein;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.attack.Wreck;
import com.example.charpowers.life.model.LifeRules;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

// Just a bunch of quality of life. Each small thing checks ownership only when its own moment comes.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class LifeEnjoyer extends OwnPower {
    public static final LifeEnjoyer POWER = new LifeEnjoyer();
    private static final int CROP_HEIGHT = 7;

    private static final Map<UUID, Long> LAST_USED = new HashMap<>();
    private static final Map<UUID, Long> LAST_PET = new HashMap<>();
    private static final List<TreeJob> TREES = new ArrayList<>();
    private static boolean busy;

    private record TreeJob(UUID player, Deque<BlockPos> logs) {}

    private LifeEnjoyer() {
        super(Mastery.LIFE_ENJOYER, Items.POPPY);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "The sun's gone. You watched all of it. That's the whole secret."; }

    @Override
    public String awakenedMessage() { return "A whole day well spent. Everything you enjoy just got better."; }

    public static void used(ServerPlayer p) {
        LAST_USED.put(p.getUUID(), p.level().getGameTime());
    }

    private boolean on(ServerPlayer p, String toggle) {
        return AbilityUtil.isEnabled(p, id, toggle);
    }

    @Override
    public void watchSecond(ServerPlayer p) {
        if (Seats.sitting(p)) Seats.watchSunset(p);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        growCrops(p);
        if (Seats.sitting(p)) {
            used(p);
            if (questReady(p)) Seats.watchDay(p);
        }
        if (p.level().getGameTime() - LAST_USED.getOrDefault(p.getUUID(), -1000L) < 200) Mastery.add(p, id, 1);
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        if (!"chest_sorter".equals(ability)) return;
        used(p);
        int sorted = ChestSorter.sort(p, awakened(p) ? LifeRules.SORT_RANGE_AWAKE : LifeRules.SORT_RANGE);
        whisper(p, sorted == 0 ? "No chests near you." : "Sorted " + sorted + (sorted == 1 ? " chest." : " chests."));
        if (sorted > 0) p.level().playSound(null, p.blockPosition(), SoundEvents.BARREL_CLOSE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // A few random spots around you each second. Finding a crop counts as using the power.
    private void growCrops(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int radius = LifeRules.cropRadius(mastery(p));
        int tries = LifeRules.cropTicksPerSecond(radius, CROP_HEIGHT, awakened(p));
        BlockPos at = p.blockPosition();
        for (int i = 0; i < tries; i++) {
            BlockPos pos = at.offset(p.getRandom().nextInt(2 * radius + 1) - radius, p.getRandom().nextInt(CROP_HEIGHT) - CROP_HEIGHT / 2, p.getRandom().nextInt(2 * radius + 1) - radius);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.CROPS) || state.is(Blocks.NETHER_WART)) {
                state.randomTick(level, pos, level.random);
                used(p);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (busy || Wreck.BREAKING || !(e.getPlayer() instanceof ServerPlayer p) || !POWER.owns(p) || HiddenRuntime.switchedOff(p)) return;
        BlockState state = e.getState();
        BlockPos pos = e.getPos();
        if (state.getBlock() instanceof CropBlock || state.getBlock() instanceof NetherWartBlock) {
            replant(p, state, pos);
        } else if (POWER.awakened(p)) {
            // awakened: vein mining and tree chopping are one thing now, the hold-a-key Ultimine
            if (com.example.charpowers.life.ultimine.UltimineServer.onBreak(p, pos, state) > 0) {
                used(p);
                com.example.charpowers.anim.AnimNet.play(p, "sweep_swing");
            }
        } else if (state.is(Tags.Blocks.ORES) && POWER.on(p, "vein_miner")) {
            veinMine(p, state, pos);
        } else if (state.is(BlockTags.LOGS) && POWER.on(p, "tree_logger")) {
            startTree(p, pos);
        }
    }

    private static void replant(ServerPlayer p, BlockState state, BlockPos pos) {
        used(p);
        ServerLevel level = p.serverLevel();
        Block block = state.getBlock();
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
            if (level.getBlockState(pos).isAir() && block.defaultBlockState().canSurvive(level, pos)) level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        }));
    }

    private static void veinMine(ServerPlayer p, BlockState state, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        Cell start = new Cell(pos.getX(), pos.getY(), pos.getZ());
        Predicate<Cell> same = c -> !c.equals(start) && level.getBlockState(new BlockPos(c.x(), c.y(), c.z())).is(state.getBlock());
        Vein vein = new Vein();
        vein.broke(start, same);
        int max = POWER.awakened(p) ? LifeRules.VEIN_MAX_AWAKE : Setting.LIFE_VEIN.i();
        busy = true;
        try {
            for (int count = 0; count < max; count++) {
                Cell next = vein.next(start, same);
                if (next == null) break;
                p.gameMode.destroyBlock(new BlockPos(next.x(), next.y(), next.z()));
                vein.broke(next, same);
                if (same.test(next)) break; // something said no: don't keep trying
            }
        } finally {
            busy = false;
        }
        used(p);
    }

    // A tree, not a wooden wall: joined-up logs with leaves the game grew (placed leaves don't count).
    private static void startTree(ServerPlayer p, BlockPos from) {
        ServerLevel level = p.serverLevel();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Deque<BlockPos> logs = new ArrayDeque<>();
        queue.add(from);
        seen.add(from);
        int leaves = 0;
        while (!queue.isEmpty() && logs.size() < LifeRules.TREE_MAX) {
            BlockPos at = queue.poll();
            for (int x = -1; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                BlockPos near = at.offset(x, y, z);
                if (!seen.add(near)) continue;
                BlockState s = level.getBlockState(near);
                if (s.is(BlockTags.LOGS)) {
                    logs.add(near);
                    queue.add(near);
                } else if (s.is(BlockTags.LEAVES) && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)) {
                    leaves++;
                }
            }
        }
        if (leaves < LifeRules.TREE_LEAVES_NEEDED) return;
        used(p);
        TREES.add(new TreeJob(p.getUUID(), logs));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || TREES.isEmpty()) return;
        Iterator<TreeJob> it = TREES.iterator();
        busy = true;
        try {
            while (it.hasNext()) {
                TreeJob job = it.next();
                ServerPlayer p = e.getServer().getPlayerList().getPlayer(job.player());
                if (p == null) {
                    it.remove();
                    continue;
                }
                for (int i = 0; i < LifeRules.TREE_PER_TICK && !job.logs().isEmpty(); i++) p.gameMode.destroyBlock(job.logs().poll());
                if (job.logs().isEmpty()) it.remove();
            }
        } finally {
            busy = false;
        }
    }

    // Magnet: 5 times a second, for owners with it switched on.
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 4 != 0 || !POWER.owns(p) || !POWER.on(p, "magnet")) return;
        int range = POWER.awakened(p) ? LifeRules.MAGNET_RANGE_AWAKE : Setting.LIFE_MAGNET.i();
        Vec3 to = p.position().add(0, 0.5, 0);
        for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(range), i -> i.isAlive())) {
            Entity thrower = item.getOwner();
            if (thrower == p && item.getAge() < LifeRules.OWN_DROP_WAIT) continue;
            if (thrower != null && thrower != p && item.getAge() < LifeRules.STRANGER_DROP_WAIT) continue;
            item.setDeltaMovement(to.subtract(item.position()).normalize().scale(0.35));
            item.hasImpulse = true;
        }
    }

    // Petting: an empty hand on an animal heals you both.
    @SubscribeEvent
    public static void onPet(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getTarget() instanceof Animal animal) || !p.getMainHandItem().isEmpty() || !POWER.owns(p)) return;
        long now = p.level().getGameTime();
        if (now - LAST_PET.getOrDefault(p.getUUID(), -100L) < 60) return;
        LAST_PET.put(p.getUUID(), now);
        float heal = LifeRules.petHeal(POWER.mastery(p), POWER.awakened(p));
        p.heal(heal);
        animal.heal(heal);
        used(p);
        p.serverLevel().sendParticles(ParticleTypes.HEART, animal.getX(), animal.getEyeY() + 0.3, animal.getZ(), 4, 0.3, 0.2, 0.3, 0);
    }

    // Soft landing on hay, leaves or wool. At mastery 100 on grass too.
    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !POWER.owns(p)) return;
        BlockState under = p.level().getBlockState(p.blockPosition().below());
        boolean soft = under.is(Blocks.HAY_BLOCK) || under.is(BlockTags.LEAVES) || under.is(BlockTags.WOOL)
                || (POWER.mastery(p) >= LifeRules.GRASS_LANDING_LEVEL && (under.is(Blocks.GRASS_BLOCK) || p.level().getBlockState(p.blockPosition()).is(Blocks.GRASS)));
        if (soft) {
            e.setCanceled(true);
            used(p);
        }
    }

    // A proper night's sleep. Waking up early doesn't count.
    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.wakeImmediately() || !POWER.owns(p)) return;
        p.addEffect(new MobEffectInstance(WellRested.EFFECT.get(), LifeRules.restedTicks(POWER.mastery(p), POWER.awakened(p))));
        used(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_USED.remove(e.getEntity().getUUID());
        LAST_PET.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        TREES.clear();
    }
}
