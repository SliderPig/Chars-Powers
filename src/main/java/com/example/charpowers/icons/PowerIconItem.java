package com.example.charpowers.icons;

import com.example.charpowers.CharPowers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.threetag.palladium.util.icon.IIcon;
import net.threetag.palladium.util.icon.ItemIcon;
import net.threetag.palladium.util.icon.TexturedIcon;

import java.util.function.Consumer;

// An item nobody can get, only there to show any power's picture wherever the game wants an ItemStack: Death Steal's
// floating icons and the Power Guide. Which picture is kept in the stack's NBT, and PowerIconRenderer draws it.
public final class PowerIconItem extends Item {
    private static final String TEXTURE = "texture";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CharPowers.MODID);
    public static final RegistryObject<Item> ICON = ITEMS.register("power_icon", PowerIconItem::new);

    private PowerIconItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static ItemStack of(ResourceLocation texture) {
        ItemStack stack = new ItemStack(ICON.get());
        stack.getOrCreateTag().putString(TEXTURE, texture.toString());
        return stack;
    }

    // A Palladium power's icon as a stack. Pictures made on the fly can't be shown this way: those get a star.
    public static ItemStack of(IIcon icon) {
        if (icon instanceof ItemIcon item) return item.stack();
        if (icon instanceof TexturedIcon tex) {
            ResourceLocation path = ResourceLocation.tryParse(tex.texture().toString());
            if (path != null) return of(path);
        }
        return new ItemStack(Items.NETHER_STAR);
    }

    static ResourceLocation textureOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? null : ResourceLocation.tryParse(tag.getString(TEXTURE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return PowerIconRenderer.instance();
            }
        });
    }
}
