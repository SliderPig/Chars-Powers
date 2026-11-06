package com.example.charpowers.own;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.threetag.palladium.power.SuperpowerUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Draws one of Char's own powers out of you and keeps it in the syringe. Using it again puts the power back. Your mastery stays on you.
// It holds one at a time and it isn't used up. Whoever's holding a filled one can take what's in it.
public class PowerSyringe extends Item {
    private static final String STORED = "stored", PICK = "pick";

    public PowerSyringe(Properties properties) {
        super(properties);
    }

    @Nullable
    private static ResourceLocation stored(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(STORED) ? ResourceLocation.tryParse(stack.getTag().getString(STORED)) : null;
    }

    public static boolean filled(ItemStack stack) {
        return stored(stack) != null;
    }

    private static List<OwnPower> powersOf(ServerPlayer p) {
        List<OwnPower> mine = new ArrayList<>();
        for (OwnPower power : OwnPowers.all()) {
            if (power.owns(p)) mine.add(power);
        }
        return mine;
    }

    private static Component nameOf(ResourceLocation power) {
        return Component.translatable("power." + power.getNamespace() + "." + power.getPath());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer p) {
            if (filled(stack)) putBack(p, stack);
            else if (p.isShiftKeyDown()) choose(p, stack);
            else draw(p, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void choose(ServerPlayer p, ItemStack stack) {
        List<OwnPower> mine = powersOf(p);
        if (mine.isEmpty()) {
            OwnPower.whisper(p, "You have no power this can draw.");
            return;
        }
        int next = Math.floorMod(stack.getOrCreateTag().getInt(PICK) + 1, mine.size());
        stack.getOrCreateTag().putInt(PICK, next);
        p.displayClientMessage(Component.literal("Draws: ").append(nameOf(mine.get(next).id)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void draw(ServerPlayer p, ItemStack stack) {
        List<OwnPower> mine = powersOf(p);
        if (mine.isEmpty()) {
            OwnPower.whisper(p, "You have no power this can draw.");
            return;
        }
        OwnPower power = mine.get(Math.floorMod(stack.getOrCreateTag().getInt(PICK), mine.size()));
        SuperpowerUtil.removeSuperpower(p, power.id);
        stack.getOrCreateTag().putString(STORED, power.id.toString());
        stack.getOrCreateTag().remove(PICK);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1f, 0.8f);
        p.serverLevel().sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
        p.displayClientMessage(Component.literal("Drawn out: ").append(nameOf(power.id)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void putBack(ServerPlayer p, ItemStack stack) {
        ResourceLocation id = stored(stack);
        OwnPower power = id == null ? null : OwnPowers.get(id);
        if (power == null) {
            OwnPower.whisper(p, "Whatever's in this no longer exists.");
            return;
        }
        if (power.owns(p)) {
            OwnPower.whisper(p, "You already have that power.");
            return;
        }
        power.give(p);
        if (!SuperpowerUtil.hasSuperpower(p, id)) return;
        stack.getOrCreateTag().remove(STORED);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.4);
        p.displayClientMessage(Component.literal("It's yours again: ").append(nameOf(id)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return filled(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        ResourceLocation id = stored(stack);
        if (id != null) {
            lines.add(Component.literal("Holds: ").append(nameOf(id)).withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.literal("Use it to take this power.").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.literal("Use it to draw a power out and keep it in here.").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("Crouch and use it to choose which.").withStyle(ChatFormatting.GRAY));
        }
    }
}
