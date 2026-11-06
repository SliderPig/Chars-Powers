package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// Dropped by Dark Elves. Fast, leaves dark wisps behind it, and blinds what it cuts. Mobs don't care about being blind, so they get slowed instead.
public class DaggerOfDarkness extends SwordItem {
    private static final int BLIND_TICKS = 50, SLOW_TICKS = 40;

    public DaggerOfDarkness() {
        super(ItemTiers.DAGGER, 2, -1.4f, new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0), attacker);
        if (!(target instanceof Player)) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1), attacker);
        if (target.level() instanceof ServerLevel level) {
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
            Foes.particles(level, ParticleTypes.SQUID_INK, at, 4, 0.25, 0.03);
            Foes.particles(level, ParticleTypes.LARGE_SMOKE, at, 4, 0.2, 0.01);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide) ItemsClient.swung(entity, WeaponStyle.DAGGER);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.charpowers.dagger_of_darkness.about").withStyle(ChatFormatting.DARK_PURPLE));
    }

    // A lost point of durability isn't a new item: no bobbing in the hand.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.DAGGER));
    }
}
