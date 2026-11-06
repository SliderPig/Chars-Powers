package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.model.MidnightRule;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.mastery.model.MasteryMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// How you get Cloning: at midnight, stand still in front of a glass wall with water behind it for 60 seconds.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class MidnightReflection {
    private static final Map<UUID, Watch> WATCHING = new HashMap<>();
    private static final Map<String, Integer> HINTED = new HashMap<>();

    enum Look { NONE, NO_WATER, OK }

    private static final class Watch {
        final MidnightRule rule = new MidnightRule();
        Vec3 pos;
    }

    private MidnightReflection() {}

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        if (p instanceof CloneEntity || p.level().dimension() != Level.OVERWORLD) return;
        boolean has = SuperpowerUtil.hasSuperpower(p, Mastery.CLONING);
        if (has && p.tickCount % 100 == 0) CloneData.rememberBestGear(p);
        boolean questReady = has && Mastery.level(p, Mastery.CLONING) >= MasteryMath.MAX_LEVEL && !Mastery.isAwakened(p, Mastery.CLONING);
        if (has && !questReady) {
            WATCHING.remove(p.getUUID());
            return;
        }
        Look look = lookAtReflection(p);
        boolean facing = look == Look.OK;
        long dayTime = p.level().getDayTime();
        Watch w = WATCHING.get(p.getUUID());
        if (!facing && w == null) {
            if (look == Look.NO_WATER) hint(p, "water", "Glass is good, but there is no water behind it. Your reflection needs water.");
            return;
        }
        if (w == null) {
            w = new Watch();
            WATCHING.put(p.getUUID(), w);
        }
        boolean still = w.pos != null && w.pos.distanceToSqr(p.position()) < 0.0025;
        boolean first = w.pos == null;
        w.pos = p.position();
        boolean counting = facing && (still || first);
        if (counting && !MidnightRule.isMidnight(dayTime) && !w.rule.sawMidnight()) {
            hint(p, "night", "Your reflection only wakes at midnight. Come back when the moon is high.");
        }
        if (w.rule.step(counting, dayTime)) {
            WATCHING.remove(p.getUUID());
            if (!questReady) {
                unlock(p);
            } else if (!CloneManager.clonesOf(p.getUUID()).isEmpty()) {
                p.sendSystemMessage(Component.literal("Your reflection won't come out while your clones are watching. Come back another midnight, alone.").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                ReflectionMob.stepOut(p);
            }
        } else if (!facing) {
            WATCHING.remove(p.getUUID());
        } else if (counting && (MidnightRule.isMidnight(dayTime) || w.rule.sawMidnight())) {
            p.displayClientMessage(Component.literal("Your reflection is watching... " + w.rule.seconds() + "/" + MidnightRule.SECONDS_NEEDED)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    // Quiet hints, each kind at most once every five minutes.
    private static void hint(ServerPlayer p, String kind, String text) {
        String key = p.getUUID() + kind;
        Integer last = HINTED.get(key);
        if (last != null && p.tickCount - last < 6000 && p.tickCount >= last) return;
        HINTED.put(key, p.tickCount);
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        WATCHING.remove(e.getEntity().getUUID());
        String id = e.getEntity().getUUID().toString();
        HINTED.remove(id + "water");
        HINTED.remove(id + "night");
    }

    static Look lookAtReflection(ServerPlayer p) {
        HitResult hit = p.pick(5.0, 0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return Look.NONE;
        BlockHitResult block = (BlockHitResult) hit;
        BlockState state = p.level().getBlockState(block.getBlockPos());
        if (!isGlass(state)) return Look.NONE;
        return waterBehind(p.level(), block.getBlockPos(), block.getDirection().getOpposite()) ? Look.OK : Look.NO_WATER;
    }

    // Tinted, stained and modded glass all count.
    static boolean isGlass(BlockState state) {
        if (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)) return true;
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && key.getPath().contains("glass");
    }

    // Water anywhere close behind the glass: 1-3 blocks back, 2 to each side, 5 down to 1 up.
    // (A wall on the edge of the sand has air right behind it and the water further down.)
    static boolean waterBehind(Level level, BlockPos glass, net.minecraft.core.Direction back) {
        int[] lo = new int[3];
        int[] hi = new int[3];
        int[] dir = {back.getStepX(), back.getStepY(), back.getStepZ()};
        for (int axis = 0; axis < 3; axis++) {
            if (dir[axis] != 0) {
                lo[axis] = Math.min(dir[axis], 3 * dir[axis]);
                hi[axis] = Math.max(dir[axis], 3 * dir[axis]);
            } else if (axis == 1) {
                lo[axis] = -5;
                hi[axis] = 1;
            } else {
                lo[axis] = -2;
                hi[axis] = 2;
            }
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = lo[0]; x <= hi[0]; x++)
            for (int y = lo[1]; y <= hi[1]; y++)
                for (int z = lo[2]; z <= hi[2]; z++) {
                    m.set(glass.getX() + x, glass.getY() + y, glass.getZ() + z);
                    if (level.getFluidState(m).is(FluidTags.WATER)) return true;
                }
        return false;
    }

    private static void unlock(ServerPlayer p) {
        if (!SuperpowerUtil.addSuperpower(p, Mastery.CLONING)) {
            CharPowers.LOG.error("Couldn't add the Palladium power {} to {}", Mastery.CLONING, p.getGameProfile().getName());
            return;
        }
        Mastery.sync(p);
        p.sendSystemMessage(Component.literal("Your reflection blinks. You didn't.").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.6f);
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getEyeY(), p.getZ(), 30, 0.5, 0.5, 0.5, 0.01);
    }
}
