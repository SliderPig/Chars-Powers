package com.example.charpowers.life;

import com.example.charpowers.CharPowers;
import com.example.charpowers.life.model.LifeRules;
import com.example.charpowers.own.OwnPower;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Sitting on stairs and slabs, and the two things you do sitting down: the sunset that earns the power, and the whole
// day that awakens it. A seat is a hidden marker that goes away when you get up, so nobody sitting = nothing running.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Seats {
    private static final String TAG = "charpowers.seat";
    private static final Map<UUID, Seat> SEATS = new HashMap<>();
    private static final double SEAT_HEIGHT = 0.75;

    private static final class Seat {
        final ArmorStand stand;
        final BlockPos at;
        boolean sunset, wholeDay;

        Seat(ArmorStand stand, BlockPos at) {
            this.stand = stand;
            this.at = at;
        }
    }

    private Seats() {}

    static boolean sitting(ServerPlayer p) {
        return SEATS.containsKey(p.getUUID());
    }

    private static boolean seatable(BlockState state, boolean owner) {
        if (state.is(BlockTags.STAIRS)) return state.getValue(StairBlock.HALF) == Half.BOTTOM;
        return owner && state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    private static long time(Level level) {
        return level.getDayTime() % 24000;
    }

    // Empty hand, right-click. Owners sit on any stair or slab. Before you have the power, you can only sit on a stair,
    // and only when the sun's about to go down and you're facing it.
    @SubscribeEvent
    public static void onClick(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCrouching() || !p.getMainHandItem().isEmpty() || p.isPassenger()) return;
        ServerLevel level = p.serverLevel();
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);
        boolean owner = LifeEnjoyer.POWER.owns(p);
        if (!seatable(state, owner) || !level.getBlockState(pos.above()).isAir()) return;
        long now = time(level);
        boolean sunset = level.dimension() == Level.OVERWORLD && LifeRules.sunsetCanStart(now) && LifeRules.facing(p.getYRot(), LifeRules.WEST_YAW);
        if (!owner && !sunset) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY() + SEAT_HEIGHT, pos.getZ() + 0.5);
        CompoundTag look = new CompoundTag();
        look.putBoolean("Marker", true);
        look.putBoolean("Invisible", true);
        stand.readAdditionalSaveData(look);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.addTag(TAG);
        Seat seat = new Seat(stand, pos);
        seat.sunset = !owner;
        seat.wholeDay = owner && LifeEnjoyer.POWER.questReady(p) && LifeRules.isSunrise(now);
        SEATS.put(p.getUUID(), seat);
        level.addFreshEntity(stand);
        p.startRiding(stand, true);
        if (seat.sunset) OwnPower.whisper(p, "You sit down to watch the sun go.");
        if (seat.wholeDay) OwnPower.whisper(p, "A whole day. Face the sun, keep your pet close and your belly full.");
    }

    // Once a second, only for someone sitting and not yet a Life Enjoyer.
    static void watchSunset(ServerPlayer p) {
        Seat seat = SEATS.get(p.getUUID());
        if (seat == null || !seat.sunset) return;
        long now = time(p.level());
        if (!LifeRules.facing(p.getYRot(), LifeRules.WEST_YAW)) {
            seat.sunset = false;
            OwnPower.whisper(p, "You looked away. There's always tomorrow.");
        } else if (LifeRules.sunIsGone(now)) {
            seat.sunset = false;
            LifeEnjoyer.POWER.unlock(p);
        }
    }

    // Once a second for an owner on the quest: stay sat from sunrise to sunset, facing the sun.
    static void watchDay(ServerPlayer p) {
        Seat seat = SEATS.get(p.getUUID());
        if (seat == null || !seat.wholeDay) return;
        long now = time(p.level());
        String wrong = null;
        if (!LifeRules.facing(p.getYRot(), LifeRules.questFacing(now))) wrong = "You stopped facing the sun.";
        else if (p.getFoodData().getFoodLevel() < 20) wrong = "You got hungry.";
        else if (p.level().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(6), a -> a.isOwnedBy(p)).isEmpty()) wrong = "Your pet wandered off.";
        if (wrong != null) {
            seat.wholeDay = false;
            OwnPower.tell(p, wrong + " Try again tomorrow.");
        } else if (now >= LifeRules.QUEST_SUNSET && now < LifeRules.SUNRISE_FROM) {
            seat.wholeDay = false;
            LifeEnjoyer.POWER.awaken(p);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (SEATS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        Seat seat = SEATS.get(p.getUUID());
        if (seat != null && seat.wholeDay) {
            seat.wholeDay = false;
            OwnPower.tell(p, "You got hurt. Try again tomorrow.");
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || SEATS.isEmpty() || e.getServer().getTickCount() % 10 != 0) return;
        Iterator<Map.Entry<UUID, Seat>> it = SEATS.entrySet().iterator();
        while (it.hasNext()) {
            Seat seat = it.next().getValue();
            if (seat.stand.getPassengers().isEmpty() || seat.stand.level().getBlockState(seat.at).isAir()) {
                seat.stand.discard();
                it.remove();
            }
        }
    }

    // A seat left behind by a crash or a server stop goes away when its chunk loads.
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof ArmorStand a && a.getTags().contains(TAG) && !e.getLevel().isClientSide()) {
            for (Seat seat : SEATS.values()) {
                if (seat.stand == a) return;
            }
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Seat seat = SEATS.remove(e.getEntity().getUUID());
        if (seat != null) seat.stand.discard();
    }

    @SubscribeEvent
    public static void onStopping(net.minecraftforge.event.server.ServerStoppingEvent e) {
        SEATS.values().forEach(seat -> seat.stand.discard());
        SEATS.clear();
    }
}
