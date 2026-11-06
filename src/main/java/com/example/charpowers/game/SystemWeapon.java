package com.example.charpowers.game;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

// Weapons from the System, for the Player alone: sold in their shop and left behind in gates. They never wear out, and in anyone else's
// hands they do half a heart (GamePlayer's events). Nobody else can even pick one up.
public class SystemWeapon extends SwordItem {
    public enum Kind {
        KNIGHT_KILLER("knight_killer", "Knight Killer", 2, -1.2f, "Poisons what it cuts."),
        CRIMSON_EDGE("crimson_edge", "Crimson Edge", 5, -2.2f, "Every hit heals you."),
        GRAVITY_BLADE("gravity_blade", "Gravity Blade", 6, -2.4f, "Drags nearby enemies in."),
        GRAND_SPEAR("grand_spear", "Grand Spear", 5, -2.5f, "Reaches 3 blocks further."),
        SHADOW_SCYTHE("shadow_scythe", "Shadow Scythe", 7, -2.8f, "Cuts everything round what you hit."),
        COCYTUS_HAMMER("cocytus_hammer", "Cocytus Hammer", 10, -3.2f, "Freezes everything near the blow.");

        public final String id, title, about;
        final int damage;
        final float speed;

        Kind(String id, String title, int damage, float speed, String about) {
            this.id = id;
            this.title = title;
            this.damage = damage;
            this.speed = speed;
            this.about = about;
        }
    }

    private static final UUID REACH = UUID.fromString("7b0b6d0e-3c1b-4b7e-9a3f-5f2a1d6c8e01");
    private static boolean striking;
    public final Kind kind;

    public SystemWeapon(Kind kind, Properties properties) {
        super(Tiers.NETHERITE, kind.damage, kind.speed, properties);
        this.kind = kind;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (kind != Kind.GRAND_SPEAR || slot != EquipmentSlot.MAINHAND) return base;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> out = ImmutableMultimap.builder();
        out.putAll(base);
        out.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH, "Grand Spear", 3, AttributeModifier.Operation.ADDITION));
        return out.build();
    }

    // What makes each one its own. Only called when the Player lands a blow.
    void strike(Player player, LivingEntity target, float damage) {
        if (striking) return; // the Scythe's sweep is a hit too, and that mustn't set it off again
        striking = true;
        try {
            effect(player, target, damage);
        } finally {
            striking = false;
        }
    }

    private void effect(Player player, LivingEntity target, float damage) {
        Level level = target.level();
        switch (kind) {
            case KNIGHT_KILLER -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            case CRIMSON_EDGE -> player.heal(3f);
            case GRAVITY_BLADE -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(6), e -> e != player && e != target && e.isAlive())) {
                    Vec3 pull = target.position().subtract(e.position()).normalize().scale(0.7);
                    e.setDeltaMovement(pull.x, 0.2, pull.z);
                    e.hurtMarked = true;
                }
            }
            case SHADOW_SCYTHE -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), e -> e != player && e != target && e.isAlive() && !player.isAlliedTo(e))) {
                    e.hurt(player.damageSources().playerAttack(player), damage * 0.5f);
                }
            }
            case COCYTUS_HAMMER -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), e -> e != player && e.isAlive() && !player.isAlliedTo(e))) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), 120));
                }
            }
            default -> {}
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(kind.about).withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Only the Player can wield this.").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    // Their swings: Char's own moves, see items/client/SwingPoses.
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide) com.example.charpowers.items.client.ItemsClient.swung(entity, com.example.charpowers.items.WeaponStyles.of(kind));
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.example.charpowers.items.client.ItemsClient.hands(() -> kind == null ? null : com.example.charpowers.items.WeaponStyles.of(kind)));
    }
}
