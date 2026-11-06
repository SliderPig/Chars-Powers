package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.SprayRules;
import com.example.charpowers.items.model.SprayRules.Liquid;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// A spray bottle. Fill it from water, lava or a milk bucket, hold the button to squirt whatever you're aiming at. Only lava hurts: water puts fires out,
// milk washes effects off, and it never leaves a source block anywhere.
public class SprayBottle extends Item {
    private static final String LIQUID = "liquid", SPRAYS = "sprays";
    private static final DustParticleOptions MILK_DROP = new DustParticleOptions(new Vector3f(0.98f, 0.98f, 0.94f), 0.8f);

    public SprayBottle() {
        super(new Properties().stacksTo(1));
    }

    public static Liquid liquid(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null || tag.getInt(SPRAYS) <= 0 ? Liquid.EMPTY : Liquid.of(tag.getString(LIQUID));
    }

    public static int sprays(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(SPRAYS);
    }

    public static void set(ItemStack stack, Liquid liquid, int sprays) {
        if (liquid == Liquid.EMPTY || sprays <= 0) {
            stack.removeTagKey(LIQUID);
            stack.removeTagKey(SPRAYS);
            return;
        }
        stack.getOrCreateTag().putString(LIQUID, liquid.id());
        stack.getOrCreateTag().putInt(SPRAYS, sprays);
    }

    private static boolean fill(Player player, ItemStack stack, Liquid adding, int to) {
        Liquid in = liquid(stack);
        if (!SprayRules.canFill(in, sprays(stack), adding)) return false;
        set(stack, adding, to);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), CharItems.sound("spray_fill"), SoundSource.PLAYERS, 0.8f, adding == Liquid.LAVA ? 0.7f : 1f);
        return true;
    }

    // Right-clicking a filled cauldron takes what's in it.
    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level level = c.getLevel();
        BlockPos pos = c.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = c.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = c.getItemInHand();
        Liquid in = liquid(stack);
        if (state.is(Blocks.WATER_CAULDRON) && SprayRules.canFill(in, sprays(stack), Liquid.WATER)) {
            if (!level.isClientSide) {
                fill(player, stack, Liquid.WATER, SprayRules.afterFill(in, sprays(stack), Liquid.WATER));
                LayeredCauldronBlock.lowerFillLevel(state, level, pos);
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.is(Blocks.LAVA_CAULDRON) && SprayRules.canFill(in, sprays(stack), Liquid.LAVA)) {
            if (!level.isClientSide) {
                fill(player, stack, Liquid.LAVA, SprayRules.afterFill(in, sprays(stack), Liquid.LAVA));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // a milk bucket in the other hand
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        Liquid in = liquid(stack);
        if (other.is(Items.MILK_BUCKET) && SprayRules.canFill(in, sprays(stack), Liquid.MILK)) {
            if (!level.isClientSide) {
                fill(player, stack, Liquid.MILK, SprayRules.afterFill(in, sprays(stack), Liquid.MILK));
                if (!player.getAbilities().instabuild) player.setItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        // the liquid you're looking at, if the bottle can take it
        BlockHitResult aim = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (aim.getType() == HitResult.Type.BLOCK && level.mayInteract(player, aim.getBlockPos())) {
            BlockPos pos = aim.getBlockPos();
            var fluid = level.getFluidState(pos);
            if (fluid.is(FluidTags.WATER) && SprayRules.canFill(in, sprays(stack), Liquid.WATER)) {
                if (!level.isClientSide) fill(player, stack, Liquid.WATER, Liquid.WATER.full);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (fluid.is(FluidTags.LAVA) && fluid.isSource() && level.getBlockState(pos).is(Blocks.LAVA) && SprayRules.canFill(in, sprays(stack), Liquid.LAVA)) {
                if (!level.isClientSide) {
                    // a source of lava is used up by filling: it's the bucket rule, so this can't make endless fire
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                    fill(player, stack, Liquid.LAVA, Liquid.LAVA.full);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        if (in == Liquid.EMPTY) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    // Milk it straight from a cow.
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Cow cow && !cow.isBaby() && liquid(stack) == Liquid.EMPTY) {
            ItemStack held = player.getItemInHand(hand);
            if (!player.level().isClientSide) {
                set(held, Liquid.MILK, Liquid.MILK.perFill);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.COW_MILK, SoundSource.PLAYERS, 1f, 1.2f);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    // Click a milk bucket onto the bottle in your inventory, like a bundle.
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (action != ClickAction.SECONDARY || !other.is(Items.MILK_BUCKET) || !slot.allowModification(player)) return false;
        Liquid in = liquid(stack);
        if (!SprayRules.canFill(in, sprays(stack), Liquid.MILK)) return false;
        set(stack, Liquid.MILK, SprayRules.afterFill(in, sprays(stack), Liquid.MILK));
        player.playSound(CharItems.sound("spray_fill"), 0.8f, 1.1f);
        if (!player.getAbilities().instabuild) carried.set(new ItemStack(Items.BUCKET));
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int left) {
        if (!(user instanceof Player player)) return;
        if ((getUseDuration(stack) - left) % SprayRules.TICKS_PER_SPRAY != 0) return;
        Liquid liquid = liquid(stack);
        if (liquid == Liquid.EMPTY) {
            player.stopUsingItem();
            return;
        }
        if (level instanceof ServerLevel server) {
            sprayOnce(server, player, liquid);
            if (!player.getAbilities().instabuild) set(stack, liquid, sprays(stack) - 1);
            if (sprays(stack) <= 0) player.stopUsingItem();
        }
    }

    // One squirt: particles along the aim, then whatever's in the cone gets it.
    public static void sprayOnce(ServerLevel level, Player player, Liquid liquid) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        // a crouching spray goes in your own face: the milk joke
        boolean self = player.isShiftKeyDown();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 from = eye.add(look.scale(0.55)).add(side.scale(0.22)).add(0, -0.18, 0);
        ParticleOptions[] looks = switch (liquid) {
            case WATER -> new ParticleOptions[]{ParticleTypes.SPLASH, ParticleTypes.FALLING_WATER};
            case LAVA -> new ParticleOptions[]{ParticleTypes.FLAME, ParticleTypes.FALLING_LAVA, ParticleTypes.SMOKE};
            default -> new ParticleOptions[]{MILK_DROP, ParticleTypes.WHITE_ASH};
        };
        if (self) {
            Foes.particles(level, looks[0], eye.add(look.scale(0.4)), 10, 0.2, 0.02);
        } else {
            for (int i = 0; i < 12; i++) {
                ParticleOptions p = looks[i % looks.length];
                double spread = Math.tan(Math.toRadians(SprayRules.CONE_DEGREES)) * 0.65;
                Vec3 dir = look.add((level.random.nextDouble() - 0.5) * spread, (level.random.nextDouble() - 0.5) * spread, (level.random.nextDouble() - 0.5) * spread).normalize();
                double speed = 0.35 + level.random.nextDouble() * 0.5;
                Vec3 at = from.add(dir.scale(level.random.nextDouble() * 1.8));
                // count 0 means the numbers are its direction and speed, so the drops fly along the aim
                Foes.particles(level, p, at.x, at.y, at.z, 0, dir.x, dir.y, dir.z, speed);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), CharItems.sound("spray_" + liquid.id()), SoundSource.PLAYERS, 0.45f, 0.95f + level.random.nextFloat() * 0.15f);
        if (self) {
            soak(level, player, player, liquid);
            return;
        }
        AABB box = player.getBoundingBox().expandTowards(look.scale(SprayRules.RANGE)).inflate(1.5);
        double cos = Math.cos(Math.toRadians(SprayRules.CONE_DEGREES));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != player && x.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double dist = to.length();
            if (dist > SprayRules.RANGE + e.getBbWidth() || to.normalize().dot(look) < cos * (dist < 1.5 ? 0.3 : 1) || !player.hasLineOfSight(e)) continue;
            soak(level, player, e, liquid);
        }
        if (liquid == Liquid.WATER) doused(level, player, eye, look);
    }

    private static void soak(ServerLevel level, Player player, LivingEntity e, Liquid liquid) {
        switch (liquid) {
            case WATER -> {
                if (e.isOnFire()) {
                    e.clearFire();
                    level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.6f);
                }
                // what normally hates water flinches, but even that isn't hurt by a squirt
                if (e instanceof EnderMan || e instanceof Blaze || e.isSensitiveToWater()) {
                    level.broadcastEntityEvent(e, (byte) 2);
                    Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1e-3) e.knockback(0.25f, -away.normalize().x, -away.normalize().z);
                }
                if (e instanceof Cat) e.playSound(SoundEvents.CAT_HISS, 0.8f, 1f);
            }
            case MILK -> {
                if (e.curePotionEffects(new ItemStack(Items.MILK_BUCKET)) && player instanceof ServerPlayer sp && e == player) {
                    sp.displayClientMessage(Component.translatable("item.charpowers.spray_bottle.self"), true);
                }
            }
            case LAVA -> {
                if (!Foes.canHit(player, e)) return;
                DamageSource lava = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LAVA), player);
                e.hurt(lava, SprayRules.LAVA_DAMAGE);
                e.setSecondsOnFire(SprayRules.LAVA_BURN_SECONDS);
            }
            default -> {}
        }
    }

    // Puts out fire along the aim. Not a griefing tool: it only removes fire and dowses campfires, like a splash water bottle.
    private static void doused(ServerLevel level, Player player, Vec3 eye, Vec3 look) {
        for (double d = 1; d <= SprayRules.RANGE; d += 1) {
            BlockPos c = BlockPos.containing(eye.add(look.scale(d)));
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof BaseFireBlock && level.mayInteract(player, p)) {
                    level.removeBlock(p, false);
                    level.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
                } else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT) && level.mayInteract(player, p)) {
                    level.levelEvent(null, 1009, p, 0);
                    CampfireBlock.dowse(player, level, p, s);
                    level.setBlock(p, s.setValue(CampfireBlock.LIT, false), 11);
                }
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return liquid(stack) != Liquid.EMPTY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return SprayRules.barWidth(sprays(stack), liquid(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return liquid(stack).barColour;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Liquid l = liquid(stack);
        if (l == Liquid.EMPTY) {
            lines.add(Component.translatable("item.charpowers.spray_bottle.empty").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("item.charpowers.spray_bottle.holds", Component.translatable("item.charpowers.spray_bottle." + l.id()), sprays(stack), l.full).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("item.charpowers.spray_bottle.how").withStyle(ChatFormatting.GRAY));
        }
    }

    // A lost point of durability isn't a new item: no bobbing in the hand.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.SPRAY));
    }
}
