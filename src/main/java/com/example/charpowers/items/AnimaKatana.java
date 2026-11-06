package com.example.charpowers.items;

import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

// Solgo's katana. Anima means soul: it cuts your soul, not your body, so armour, shields, Protection and Resistance do nothing against it.
// That's a lot, so it's a slow swing, 10 damage at full charge, and Sharpness and the like only count for half.
public class AnimaKatana extends SwordItem {
    public AnimaKatana() {
        super(ItemTiers.SOUL, 5, -2.8f, new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    // Instead of the normal hit, which armour would soften. Returning true stops the normal one on both sides.
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!(target instanceof LivingEntity victim) || !target.isAttackable()) return false;
        if (!player.level().isClientSide) strike(player, victim, stack);
        return true;
    }

    private static boolean strike(Player player, LivingEntity victim, ItemStack stack) {
        float charge = player.getAttackStrengthScale(0.5f);
        player.resetAttackStrengthTicker();
        return strike(player, victim, stack, charge);
    }

    // charge = how far the swing has recovered, 0 to 1
    public static boolean strike(Player player, LivingEntity victim, ItemStack stack, float charge) {
        if (!Foes.canHit(player, victim)) return false;
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float extra = EnchantmentHelper.getDamageBonus(stack, victim.getMobType()) * 0.5f;
        float damage = base * (0.2f + charge * charge * 0.8f) + extra * charge;
        if (damage <= 0 || !victim.hurt(Foes.soul(player), damage)) return false;
        player.setLastHurtMob(victim);
        int fire = EnchantmentHelper.getFireAspect(player);
        if (fire > 0) victim.setSecondsOnFire(fire * 4);
        int push = EnchantmentHelper.getKnockbackBonus(player);
        if (push > 0) victim.knockback(push * 0.5f, Math.sin(Math.toRadians(player.getYRot())), -Math.cos(Math.toRadians(player.getYRot())));
        EnchantmentHelper.doPostHurtEffects(victim, player);
        EnchantmentHelper.doPostDamageEffects(player, victim);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        player.awardStat(Stats.DAMAGE_DEALT, Math.round(damage * 10f));
        player.causeFoodExhaustion(0.1f);
        if (player.level() instanceof ServerLevel level) {
            Vec3 at = victim.position().add(0, victim.getBbHeight() * 0.6, 0);
            Foes.particles(level, ParticleTypes.SOUL, at, 8, 0.3, 0.05);
            Foes.particles(level, ParticleTypes.WITCH, at, 12, 0.35, 0.08);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), CharItems.sound("soul_cut"), SoundSource.PLAYERS, 1f, 0.9f + level.random.nextFloat() * 0.2f);
        }
        return true;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide) ItemsClient.swung(entity, WeaponStyle.KATANA);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.charpowers.anima_katana.about").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("item.charpowers.anima_katana.about2").withStyle(ChatFormatting.DARK_PURPLE));
    }

    // A lost point of durability isn't a new item: no bobbing in the hand.
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ItemsClient.hands(WeaponStyle.KATANA));
    }
}
