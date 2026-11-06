package com.example.charpowers.cloning;

import dev.dubhe.curtain.features.player.fakes.IServerPlayer;
import dev.dubhe.curtain.features.player.helpers.EntityPlayerActionPack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// The clone's hands and feet, from Curtain. Every use of Curtain's action pack is in this one file,
// so if Curtain ever has to go (it hasn't been tried on Arclight yet) only this needs replacing.
final class CloneActions {
    private CloneActions() {}

    private static EntityPlayerActionPack pack(CloneEntity c) {
        return ((IServerPlayer) c).getActionPack();
    }

    static void walk(CloneEntity c, float forward, boolean sprint) {
        pack(c).setForward(forward).setSprinting(sprint);
    }

    static void stop(CloneEntity c) {
        pack(c).setForward(0f).setStrafing(0f).setSprinting(false);
    }

    static void lookAt(CloneEntity c, Vec3 where) {
        pack(c).lookAt(where);
    }

    static void jump(CloneEntity c) {
        pack(c).start(EntityPlayerActionPack.ActionType.JUMP, EntityPlayerActionPack.Action.once());
    }

    // Hold left click on whatever it's looking at. Curtain works out the mining time, tool wear and drops the way the game does.
    static void startDigging(CloneEntity c) {
        if (!c.digging) {
            c.digging = true;
            pack(c).start(EntityPlayerActionPack.ActionType.ATTACK, EntityPlayerActionPack.Action.continuous());
        }
    }

    static void stopDigging(CloneEntity c) {
        if (c.digging) {
            c.digging = false;
            pack(c).start(EntityPlayerActionPack.ActionType.ATTACK, null);
        }
    }

    // Right-click the side of a block with what's in its hand, the way a player places one.
    static boolean placeAgainst(CloneEntity c, BlockPos against, Direction face) {
        Vec3 spot = Vec3.atCenterOf(against).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        lookAt(c, spot);
        BlockHitResult hit = new BlockHitResult(spot, face, against, false);
        return c.gameMode.useItemOn(c, c.level(), c.getMainHandItem(), InteractionHand.MAIN_HAND, hit).consumesAction();
    }

    // Opens a door or gate, the way a right-click with an empty hand would.
    static void clickBlock(CloneEntity c, BlockPos pos) {
        lookAt(c, Vec3.atCenterOf(pos));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        c.level().getBlockState(pos).use(c.level(), c, InteractionHand.MAIN_HAND, hit);
    }

    // Right-click once with what's in that hand, looking up so it can't use a block by accident. It isn't Curtain's USE
    // action, as that lets go at once: this way an item that has to be held (like eating) carries on until it's done.
    static void useItem(CloneEntity c, InteractionHand hand) {
        pack(c).look(c.getYRot(), -90f);
        c.gameMode.useItem(c, c.level(), c.getItemInHand(hand), hand);
    }
}
