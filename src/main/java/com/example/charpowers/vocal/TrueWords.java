package com.example.charpowers.vocal;

import com.example.charpowers.hidden.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// What an awakened word does where it hits (a creature, or the place it crashes). Small and fun, nothing that wrecks a base
// except BOOM, which was already there.
public final class TrueWords {
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.ALLIUM};

    private TrueWords() {}

    public static void run(ServerPlayer p, String word, ServerLevel world, LivingEntity target, Vec3 at) {
        BlockPos spot = BlockPos.containing(at);
        switch (word) {
            case "BURN" -> {
                if (target != null) target.setSecondsOnFire(8);
                else for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-2, -1, -2), spot.offset(2, 1, 2))) {
                    if (world.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(world, pos, Direction.UP)) world.setBlock(pos, BaseFireBlock.getState(world, pos), Block.UPDATE_ALL);
                }
            }
            case "ICE" -> {
                if (target != null) {
                    target.setTicksFrozen(400);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                } else freezeWater(world, spot, 3);
            }
            case "FREEZE" -> {
                for (LivingEntity e : near(world, p, at, 5)) {
                    e.setTicksFrozen(600);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 5));
                }
                freezeWater(world, spot, 5);
                Fx.particles(world, ParticleTypes.SNOWFLAKE, at, 40, 2.5);
            }
            case "BOOM" -> world.explode(p, at.x, at.y, at.z, 4f, Level.ExplosionInteraction.TNT);
            case "GROW" -> {
                int used = 0;
                for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-3, -1, -3), spot.offset(3, 1, 3))) {
                    if (used < 12 && BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), world, pos.immutable())) used++;
                }
                for (LivingEntity e : near(world, p, at, 4)) if (e instanceof Animal a && a.isBaby()) a.ageUp(600);
                Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, at, 20, 2);
            }
            case "BLOOM" -> {
                int placed = 0;
                for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-3, -2, -3), spot.offset(3, 2, 3))) {
                    if (placed >= 14 || world.random.nextInt(3) != 0) continue;
                    if (world.getBlockState(pos).isAir() && world.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                        world.setBlock(pos, FLOWERS[world.random.nextInt(FLOWERS.length)].defaultBlockState(), Block.UPDATE_ALL);
                        placed++;
                    }
                }
                Fx.particles(world, ParticleTypes.CHERRY_LEAVES, at.add(0, 1, 0), 30, 2);
            }
            case "HEAL" -> {
                for (LivingEntity e : near(world, null, at, 4)) {
                    if (e instanceof ServerPlayer) {
                        e.heal(8f);
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                    }
                }
                if (p.distanceToSqr(at) < 36) p.heal(8f);
                Fx.particles(world, ParticleTypes.HEART, at.add(0, 1, 0), 10, 1);
            }
            case "PUSH" -> {
                for (LivingEntity e : near(world, p, at, 5)) Fx.push(e, at, 2.4);
                Fx.particles(world, ParticleTypes.CLOUD, at, 25, 1.5);
            }
            case "PULL" -> {
                for (LivingEntity e : near(world, p, at, 7)) {
                    e.setDeltaMovement(at.subtract(e.position()).normalize().scale(1.3).add(0, 0.2, 0));
                    e.hurtMarked = true;
                }
                Fx.particles(world, ParticleTypes.PORTAL, at, 30, 1.5);
            }
            case "LIGHT" -> {
                for (LivingEntity e : near(world, p, at, 7)) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
                Fx.particles(world, ParticleTypes.END_ROD, at.add(0, 1, 0), 40, 2);
            }
            case "STORM" -> {
                world.setWeatherParameters(0, 1200, true, false);
                for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-5, -2, -5), spot.offset(5, 3, 5))) {
                    if (world.getBlockState(pos).getBlock() instanceof FireBlock) world.removeBlock(pos.immutable(), false);
                }
                for (LivingEntity e : near(world, p, at, 5)) {
                    e.clearFire();
                    Fx.push(e, at, 0.8);
                }
                Fx.particles(world, ParticleTypes.RAIN, at.add(0, 2, 0), 60, 3);
            }
            case "SLEEP" -> {
                for (LivingEntity e : near(world, p, at, 4)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 6));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 2));
                }
                Fx.particles(world, ParticleTypes.NOTE, at.add(0, 1.5, 0), 12, 1);
            }
            case "JUMP" -> {
                for (LivingEntity e : near(world, p, at, 4)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 1.1, 0));
                    e.hurtMarked = true;
                }
                Fx.particles(world, ParticleTypes.CLOUD, at, 15, 1);
            }
            default -> {}
        }
    }

    // Creatures close to the spot; the speaker is left out unless `not` is null.
    private static List<LivingEntity> near(ServerLevel world, ServerPlayer not, Vec3 at, double range) {
        AABB box = new AABB(at, at).inflate(range);
        return world.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != not && e.position().distanceToSqr(at) <= range * range);
    }

    private static void freezeWater(ServerLevel world, BlockPos spot, int r) {
        for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-r, -2, -r), spot.offset(r, 2, r))) {
            if (world.getFluidState(pos).is(Fluids.WATER) && world.getFluidState(pos).isSource()) world.setBlock(pos.immutable(), Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
