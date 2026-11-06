package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// It's just a fork.
public class ForkItem extends SwordItem {
    public ForkItem() {
        super(ItemTiers.FORK, 2, -2.0f, new Properties().stacksTo(1));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
        target.level().playSound(null, target.getX(), target.getEyeY(), target.getZ(), CharItems.sound("fork_ting"), net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1f);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.charpowers.fork.about").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        if (entity.level().isClientSide) ItemsClient.swung(entity, WeaponStyle.FORK);
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.FORK));
    }
}
