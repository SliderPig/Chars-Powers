package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// The Flame Hammer: slow and very heavy, sets what it hits on fire, and a charged blow on the ground throws flames round you and in a line ahead.
public class FlameHammer extends SwordItem {
    private static final int BURN_SECONDS = 6;

    public FlameHammer() {
        super(ItemTiers.FLAME, 9, -3.4f, new Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        // the normal hit still happens: this only notices that it was fully charged, before the game resets the charge
        if (!player.level().isClientSide && player instanceof ServerPlayer sp && player.getAttackStrengthScale(0.5f) >= 0.9f) FlameBurst.chargedHit(sp, target);
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setSecondsOnFire(BURN_SECONDS);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide) ItemsClient.swung(entity, WeaponStyle.HAMMER);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.charpowers.flame_hammer.about").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.charpowers.flame_hammer.about2").withStyle(ChatFormatting.RED));
    }

    // A lost point of durability isn't a new item: no bobbing in the hand.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.HAMMER));
    }
}
