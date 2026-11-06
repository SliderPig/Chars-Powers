package com.example.charpowers.items;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

// How tough each weapon is and what repairs it. The fork is the weakest thing here, the sword and the katana sit near netherite.
public enum ItemTiers implements Tier {
    FORK(90, 0f, 0, 12, () -> Ingredient.of(Items.IRON_NUGGET)),
    DAGGER(1100, 2f, 3, 18, () -> Ingredient.of(Items.AMETHYST_SHARD)),
    GOLDEN(2100, 4f, 4, 22, () -> Ingredient.of(Items.GOLD_INGOT)),
    SOUL(1800, 4f, 4, 15, () -> Ingredient.of(Items.ECHO_SHARD)),
    FLAME(2000, 4f, 4, 12, () -> {
        Item vibranium = ForgeRegistries.ITEMS.getValue(new ResourceLocation("palladium", "vibranium_ingot"));
        return vibranium == null || vibranium == Items.AIR ? Ingredient.of(Items.NETHERITE_SCRAP) : Ingredient.of(vibranium);
    });

    private final int uses, level, enchantability;
    private final float damage;
    private final Supplier<Ingredient> repair;

    ItemTiers(int uses, float damage, int level, int enchantability, Supplier<Ingredient> repair) {
        this.uses = uses;
        this.damage = damage;
        this.level = level;
        this.enchantability = enchantability;
        this.repair = repair;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return 2f;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }
}
