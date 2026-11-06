package com.example.charpowers.client;

import com.example.charpowers.CharPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Hidden powers that change how YOU move. Your own PC moves your player (the server only checks it),
// so these have to happen here. Each one only nudges your speed a little, once a tick.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class ClientMovement {
    private static boolean wasJumping, wallJumped, bounced;
    private static int waterTicks, lavaTicks, wallRunTicks, frogCooldown;
    private static double groundY, lastDy;

    private static boolean has(String id) { return ClientHidden.has(id); }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof LocalPlayer p) || p != Minecraft.getInstance().player) return;
        if (p.isSpectator() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) { wasJumping = p.input.jumping; return; }
        Level l = p.level();
        // #184 Water Walker (3 s) / #264 Lava Skipper (2 s): sprint onto it and run across the top
        waterTicks = surfaceWalk(p, "water_walker", FluidTags.WATER, waterTicks, 60);
        lavaTicks = surfaceWalk(p, "lava_skipper", FluidTags.LAVA, lavaTicks, 40);
        Vec3 v = p.getDeltaMovement();
        boolean jumpNow = p.input.jumping;
        boolean jumpPressed = jumpNow && !wasJumping;
        boolean noInput = p.input.forwardImpulse == 0 && p.input.leftImpulse == 0;
        BlockState below = l.getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()));
        if (p.onGround()) { wallJumped = false; groundY = p.getY(); wallRunTicks = 0; }

        // #112 Rubber Ducky: float up to the top of the water (crouch to sink)
        if (has("rubber_ducky") && p.isInWater() && !p.input.shiftKeyDown && !p.isSwimming()) v = new Vec3(v.x, Math.max(v.y, 0.06), v.z);
        // #120 Sticky Feet: stop dead on ice when you let go of the keys
        if (has("sticky_feet") && p.onGround() && below.is(BlockTags.ICE) && noInput) v = new Vec3(v.x * 0.5, v.y, v.z * 0.5);
        // #122 Ladder Monkey: climb twice as fast
        if (has("ladder_monkey") && p.onClimbable() && v.y > 0.1) v = new Vec3(v.x, 0.4, v.z);
        // #127 Wall Slide: slide down walls slowly
        if (has("wall_slide") && !p.onGround() && p.horizontalCollision && v.y < -0.08 && !p.onClimbable()) v = new Vec3(v.x, -0.08, v.z);
        // #152 Frog Legs: a big jump out of the water
        if (frogCooldown > 0) frogCooldown--;
        if (has("frog_legs") && jumpNow && p.isInWater() && !p.isEyeInFluid(FluidTags.WATER) && frogCooldown == 0) {
            v = new Vec3(v.x, 0.6, v.z);
            frogCooldown = 10;
        }
        // #168 Clean Paws / #169 Honey Paws: undo the slow-down of soul sand, mud and honey
        boolean sticky = (has("clean_paws") && (below.is(Blocks.SOUL_SAND) || below.is(Blocks.MUD)))
                || (has("honey_paws") && below.is(Blocks.HONEY_BLOCK));
        float factor = below.getBlock().getSpeedFactor();
        if (sticky && p.onGround() && factor < 1.0f && factor > 0.1f) {
            Vec3 flat = new Vec3(v.x / factor, 0, v.z / factor);
            if (flat.length() > 0.6) flat = flat.normalize().scale(0.6);
            v = new Vec3(flat.x, v.y, flat.z);
        }
        // #176 Wall Jump: once before you land, jump off a wall
        if (has("wall_jump") && !p.onGround() && p.horizontalCollision && jumpPressed && !wallJumped) {
            Vec3 away = p.getViewVector(1).multiply(-1, 0, -1).normalize().scale(0.4);
            v = new Vec3(away.x, 0.5, away.z);
            wallJumped = true;
        }
        // #199 Umbrella: hold a banner while falling to float down gently
        boolean banner = p.getMainHandItem().getItem() instanceof BannerItem || p.getOffhandItem().getItem() instanceof BannerItem;
        if (has("umbrella") && banner && !p.onGround() && v.y < -0.15) v = new Vec3(v.x, -0.15, v.z);
        // #200 Bounce Back: slime and beds bounce you higher
        if (has("bounce_back")) {
            boolean bouncy = below.is(Blocks.SLIME_BLOCK) || below.getBlock() instanceof BedBlock;
            if (bouncy && lastDy < -0.3 && v.y > 0.1 && !bounced) { v = new Vec3(v.x, Math.min(1.5, v.y * 1.5), v.z); bounced = true; }
            if (v.y <= 0) bounced = false;
        }
        // #201 Climbing Hands: climb up to about a block of wall
        if (has("climbing_hands") && p.horizontalCollision && jumpNow && p.getY() - groundY < 1.3 && !p.onClimbable()) v = new Vec3(v.x, 0.2, v.z);
        // #233 Glide: hold jump while falling to fall slowly
        if (has("glide") && !p.onGround() && jumpNow && v.y < -0.1 && !p.isInWater()) v = new Vec3(v.x, -0.1, v.z);
        // #234 Wall Run: sprint along a wall in the air for up to a second
        if (has("wall_run") && !p.onGround() && p.isSprinting() && wallBeside(p) && wallRunTicks < 20) {
            wallRunTicks++;
            v = new Vec3(v.x, Math.max(v.y, 0), v.z);
        }

        if (!v.equals(p.getDeltaMovement())) p.setDeltaMovement(v);
        lastDy = v.y;
        wasJumping = jumpNow;
    }

    // Keeps you on top of water/lava while the power lasts. Starts when you sprint onto it from land.
    private static int surfaceWalk(LocalPlayer p, String id, net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> fluid, int ticks, int max) {
        if (!has(id)) return 0;
        Level l = p.level();
        BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        boolean over = l.getFluidState(under).is(fluid) && !l.getFluidState(under.above()).is(fluid);
        if (!over) return p.onGround() ? 0 : ticks; // on real ground again: ready for next time
        if (ticks == 0 && !p.isSprinting()) return 0;
        if (ticks >= max) return ticks; // time's up: in you go
        double top = under.getY() + 0.9;
        if (p.getY() < top + 0.15 && p.getY() > top - 0.7) {
            p.setPos(p.getX(), top, p.getZ());
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0, v.z);
            p.setOnGround(true);
            p.fallDistance = 0;
        }
        return ticks + 1;
    }

    private static boolean wallBeside(LocalPlayer p) {
        Vec3 look = p.getViewVector(1).multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        for (int s = -1; s <= 1; s += 2) {
            BlockPos at = BlockPos.containing(p.position().add(side.scale(0.8 * s)).add(0, 0.5, 0));
            if (p.level().getBlockState(at).isCollisionShapeFullBlock(p.level(), at)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!(e.getEntity() instanceof LocalPlayer p)) return;
        var in = e.getInput();
        // #141 Moonwalk: hold sprint while walking backwards to go as fast as forwards
        if (has("moonwalk") && in.forwardImpulse < 0 && Minecraft.getInstance().options.keySprint.isDown()) in.forwardImpulse *= 1.3f;
        // #196 Snack on the Run: eating doesn't slow you down while sprinting (Minecraft divides your speed by 5)
        if (has("snack_on_the_run") && p.isUsingItem() && p.getUseItem().isEdible() && p.isSprinting()) {
            in.forwardImpulse *= 5;
            in.leftImpulse *= 5;
        }
    }

    private ClientMovement() {}
}
