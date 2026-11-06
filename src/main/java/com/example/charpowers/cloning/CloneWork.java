package com.example.charpowers.cloning;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;

// What the working jobs (Mine, Chop, Cook) share: walking one step, digging with the right tool, fighting back,
// and sending what they've gathered to the marked chest.
public final class CloneWork {
    private static final int FIGHT_BACK_TICKS = 100;
    private static final int STASH_WHEN_FREE = 3;
    public static final double REACH = 4.5;

    private CloneWork() {}

    public static void say(CloneEntity c, @Nullable ServerPlayer host, String text) {
        if (host != null) host.sendSystemMessage(Component.literal(CloneData.label(host, c.slot) + " " + text).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // If something's been hitting it, it stops work and fights. True = it's busy doing that.
    public static boolean fightBack(CloneEntity c, @Nullable ServerPlayer host) {
        LivingEntity attacker = c.getLastHurtByMob();
        if (attacker == null || c.tickCount - c.getLastHurtByMobTimestamp() > FIGHT_BACK_TICKS || !CloneCombat.isFoe(c, host, attacker)) return false;
        stopDigging(c);
        CloneCombat.fight(c, attacker);
        return true;
    }

    public static void standStill(CloneEntity c) {
        CloneActions.stop(c);
    }

    // Keep digging this block with the best tool it has. Looks at it, so what it digs is what it sees.
    // False = something it mustn't break is in the way.
    public static boolean dig(CloneEntity c, BlockPos pos, BlockState state) {
        CloneActions.stop(c);
        holdBestTool(c, state);
        CloneActions.lookAt(c, Vec3.atCenterOf(pos));
        BlockPos seen = lookingAt(c);
        if (seen != null && !mayBreak(c, seen, c.level().getBlockState(seen))) {
            stopDigging(c);
            return false;
        }
        CloneActions.startDigging(c);
        return true;
    }

    public static void stopDigging(CloneEntity c) {
        CloneActions.stopDigging(c);
    }

    // What its eyes hit when it looks at that block: a nearer block in the way needs digging first.
    @Nullable
    public static BlockPos lookingAt(CloneEntity c) {
        HitResult hit = c.pick(REACH, 1f, false);
        return hit.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) hit).getBlockPos() : null;
    }


    private static void holdBestTool(CloneEntity c, BlockState state) {
        Inventory inv = c.getInventory();
        int best = -1;
        float bestSpeed = 1f;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty() || !s.isDamageableItem()) continue;
            float speed = s.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        if (best < 0) return;
        if (Inventory.isHotbarSlot(best)) {
            inv.selected = best;
        } else {
            ItemStack here = inv.getItem(inv.selected);
            inv.setItem(inv.selected, inv.getItem(best));
            inv.setItem(best, here);
        }
    }

    // Blocks it can spend on bridges and pillars, the way Baritone uses its throwaway items.
    public static boolean isThrowaway(ItemStack s) {
        return s.is(Tags.Items.COBBLESTONE) || s.is(Tags.Items.STONE) || s.is(Tags.Items.NETHERRACK) || s.is(ItemTags.DIRT)
                || s.is(Items.COBBLED_DEEPSLATE) || s.is(Items.TUFF) || s.is(Items.CALCITE) || s.is(Items.ANDESITE) || s.is(Items.DIORITE) || s.is(Items.GRANITE);
    }

    public static boolean hasThrowaway(CloneEntity c) {
        return countThrowaway(c) > 0;
    }

    public static int countThrowaway(CloneEntity c) {
        int n = 0;
        for (ItemStack s : c.getInventory().items) if (isThrowaway(s)) n += s.getCount();
        return n;
    }

    // Puts one of those blocks against this side of that block. False = it had none, or it wouldn't go.
    public static boolean placeThrowaway(CloneEntity c, BlockPos against, Direction face) {
        Inventory inv = c.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (!isThrowaway(inv.items.get(i))) continue;
            if (Inventory.isHotbarSlot(i)) {
                inv.selected = i;
            } else {
                ItemStack here = inv.getItem(inv.selected);
                inv.setItem(inv.selected, inv.getItem(i));
                inv.setItem(i, here);
            }
            return CloneActions.placeAgainst(c, against, face);
        }
        return false;
    }

    // Could it dig this block from where it is, with nothing else in the way?
    public static boolean canSee(CloneEntity c, BlockPos pos) {
        Vec3 eye = c.getEyePosition();
        BlockHitResult hit = c.level().clip(new ClipContext(eye, Vec3.atCenterOf(pos), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    public static boolean hasToolFor(CloneEntity c, BlockState state) {
        for (ItemStack s : c.getInventory().items) if (!s.isEmpty() && s.isCorrectToolForDrops(state)) return true;
        return false;
    }

    // Never chests, furnaces or anything else with a storage inside, and nothing unbreakable.
    public static boolean mayBreak(CloneEntity c, BlockPos pos, BlockState state) {
        return !state.hasBlockEntity() && state.getDestroySpeed(c.level(), pos) >= 0 && !state.is(BlockTags.BEDS);
    }

    @Nullable
    public static Container chest(CloneEntity c, @Nullable ServerPlayer host) {
        return host == null ? null : CloneGear.chest(c, host, true);
    }

    // Once its pockets are nearly full, what it's gathered goes to the marked chest. It keeps its tools, weapons and food.
    // False = it needed to and couldn't (no chest, or the chest is full).
    public static boolean stashIfFull(CloneEntity c, @Nullable ServerPlayer host) {
        Inventory inv = c.getInventory();
        int free = 0;
        for (ItemStack s : inv.items) if (s.isEmpty()) free++;
        if (free >= STASH_WHEN_FREE) return true;
        Container chest = chest(c, host);
        return chest != null && CloneGear.stashLoot(c, chest);
    }

    // A torch on the floor where it stands, if it has one. Quiet false = none left or it wouldn't go.
    public static boolean placeTorch(CloneEntity c) {
        Inventory inv = c.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (!inv.items.get(i).is(Items.TORCH)) continue;
            if (Inventory.isHotbarSlot(i)) {
                inv.selected = i;
            } else {
                ItemStack here = inv.getItem(inv.selected);
                inv.setItem(inv.selected, inv.getItem(i));
                inv.setItem(i, here);
            }
            return CloneActions.placeAgainst(c, c.blockPosition().below(), Direction.UP);
        }
        return false;
    }

    // Puts one of this seed into the soil, from above, like a player's right-click. False = it has none or it wouldn't go.
    public static boolean plantOn(CloneEntity c, BlockPos soil, net.minecraft.world.item.Item seed) {
        Inventory inv = c.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (!inv.items.get(i).is(seed)) continue;
            if (Inventory.isHotbarSlot(i)) {
                inv.selected = i;
            } else {
                ItemStack here = inv.getItem(inv.selected);
                inv.setItem(inv.selected, inv.getItem(i));
                inv.setItem(i, here);
            }
            return CloneActions.placeAgainst(c, soil, Direction.UP);
        }
        return false;
    }

    public static boolean hasTorch(CloneEntity c) {
        for (ItemStack s : c.getInventory().items) if (s.is(Items.TORCH)) return true;
        return false;
    }

    // Turn the head to look this way (yaw in degrees, as the game counts them), a little up or down.
    public static void glance(CloneEntity c, float yawDegrees, float pitch) {
        double yaw = Math.toRadians(yawDegrees), p = Math.toRadians(pitch);
        Vec3 dir = new Vec3(-Math.sin(yaw) * Math.cos(p), -Math.sin(p), Math.cos(yaw) * Math.cos(p));
        CloneActions.lookAt(c, c.getEyePosition().add(dir.scale(4)));
    }
}
