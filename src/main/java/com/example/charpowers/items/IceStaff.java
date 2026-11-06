package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// The Ice Staff: dropped by the Frost King. Right-click throws a shard of ice that hurts and freezes. Punchy treats it as a spear.
public class IceStaff extends Item {
    public static final int WAIT = 14, MAX_USES = 480, SPIN_TICKS = 15;

    public IceStaff() {
        super(new Properties().durability(MAX_USES).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean spin = player.isShiftKeyDown();
        player.getCooldowns().addCooldown(this, spin ? WAIT * 2 : WAIT);
        player.swing(hand);
        if (!level.isClientSide && spin && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            WeaponHits.later(sp, SPIN_TICKS, () -> {
                if (sp.getMainHandItem().getItem() == this || sp.getOffhandItem().getItem() == this) cast(level, sp, sp.getItemInHand(hand), hand, 2);
            });
        } else if (!level.isClientSide) {
            cast(level, player, stack, hand, 0);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void cast(Level level, Player player, ItemStack stack, InteractionHand hand, int extra) {
        {
            IceShard shard = new IceShard(level, player);
            shard.setPower(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack) + extra);
            shard.setPos(player.getX(), player.getEyeY() - 0.2, player.getZ());
            shard.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 2.1f, 0.6f);
            level.addFreshEntity(shard);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), CharItems.sound("ice_cast"), SoundSource.PLAYERS, 0.8f, 0.95f + level.random.nextFloat() * 0.15f);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide) ItemsClient.swung(entity, WeaponStyle.STAFF);
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.POWER_ARROWS || enchantment.category == EnchantmentCategory.BREAKABLE;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.BLUE_ICE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.charpowers.ice_staff.about").withStyle(ChatFormatting.AQUA));
    }

    // A lost point of durability isn't a new item: no bobbing in the hand.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.STAFF));
    }
}
