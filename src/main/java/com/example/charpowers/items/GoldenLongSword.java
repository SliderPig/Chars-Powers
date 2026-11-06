package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// The Golden King's Long Sword. Pulled out of the stone on top of a King's Rock. Clicks with it don't make normal hits: they run the combo in LongSwordMoves.
public class GoldenLongSword extends SwordItem {
    public GoldenLongSword() {
        super(ItemTiers.GOLDEN, 4, -2.6f, new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.charpowers.golden_kings_long_sword.about").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.charpowers.golden_kings_long_sword.about2").withStyle(ChatFormatting.YELLOW));
    }

    // A lost point of durability isn't a new item: no bobbing in the hand.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.GOLD));
    }
}
