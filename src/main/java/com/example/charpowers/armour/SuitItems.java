package com.example.charpowers.armour;

import com.example.charpowers.CharPowers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// Everything of the suit's that has to be registered: the one suit item (worn in the chest slot, it covers you head to toe),
// the 4 parts it's built from, the 4 modules, the charging station, the recipe that crafts a module into the suit, and the
// mod's creative tab. Battery, wear and modules are saved on the suit, so they travel with it.
public final class SuitItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CharPowers.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CharPowers.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CharPowers.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CharPowers.MODID);

    public static final RegistryObject<Item> SUIT = ITEMS.register("invincible_suit", SuitPiece::new);
    public static final RegistryObject<Item> HEAD_PART = ITEMS.register("suit_head_part", () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> BODY_PART = ITEMS.register("suit_body_part", () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> LEG_PART = ITEMS.register("suit_leg_part", () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> BOOT_PART = ITEMS.register("suit_boot_part", () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> SHIELD = ITEMS.register("shield_module", () -> new ModuleItem("shield"));
    public static final RegistryObject<Item> SCANNER = ITEMS.register("scanner_module", () -> new ModuleItem("scanner"));
    public static final RegistryObject<Item> STRENGTH = ITEMS.register("strength_module", () -> new ModuleItem("strength"));
    public static final RegistryObject<Item> SPEED = ITEMS.register("speed_module", () -> new ModuleItem("speed"));

    // Green Lantern's Power Battery. It's here so all the mod's items share one list and one creative tab.
    public static final RegistryObject<Item> POWER_BATTERY = ITEMS.register("power_battery", () -> new com.example.charpowers.lantern.LanternItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static final RegistryObject<Item> THE_BOOK = ITEMS.register("the_book", () -> new com.example.charpowers.book.BookItem(new Item.Properties().stacksTo(1).fireResistant()));

    public static final RegistryObject<Item> POWER_SYRINGE = ITEMS.register("power_syringe", () -> new com.example.charpowers.own.PowerSyringe(new Item.Properties().stacksTo(1)));

    static {
        for (com.example.charpowers.game.SystemWeapon.Kind kind : com.example.charpowers.game.SystemWeapon.Kind.values()) {
            ITEMS.register(kind.id, () -> new com.example.charpowers.game.SystemWeapon(kind, new Item.Properties().stacksTo(1).fireResistant()));
        }
    }

    public static final RegistryObject<Block> STATION = BLOCKS.register("charging_station", ChargingStation::new);
    public static final RegistryObject<Item> STATION_ITEM = ITEMS.register("charging_station", () -> new BlockItem(STATION.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<ChargingStation.Entity>> STATION_ENTITY = BLOCK_ENTITIES.register("charging_station",
            () -> BlockEntityType.Builder.of(ChargingStation.Entity::new, STATION.get()).build(null));
    public static final RegistryObject<RecipeSerializer<ModuleRecipe>> MODULE_RECIPE = RECIPES.register("suit_module",
            () -> new SimpleCraftingRecipeSerializer<>(ModuleRecipe::new));

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CharPowers.MODID);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.charpowers")).icon(() -> new ItemStack(SUIT.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> item : ITEMS.getEntries()) out.accept(item.get());
            }).build());

    private static final String BATTERY = "battery", WORN = "worn", MODULES = "modules", SLIM = "slim";

    private SuitItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        SuitSounds.register(modBus);
        SuitFlight.register(modBus);
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(SuitNet::register));
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPES.register(modBus);
        TABS.register(modBus);
    }

    public static boolean fullSuit(Player p) {
        return p.getItemBySlot(EquipmentSlot.CHEST).is(SUIT.get());
    }

    public static double battery(ItemStack chest) {
        CompoundTag tag = chest.getTag();
        return tag != null && tag.contains(BATTERY) ? tag.getDouble(BATTERY) : 100;
    }

    public static void setBattery(ItemStack chest, double value) {
        chest.getOrCreateTag().putDouble(BATTERY, Math.max(0, Math.min(100, value)));
    }

    public static long worn(ItemStack chest) {
        CompoundTag tag = chest.getTag();
        return tag == null ? 0 : tag.getLong(WORN);
    }

    public static void setWorn(ItemStack chest, long seconds) {
        chest.getOrCreateTag().putLong(WORN, seconds);
    }

    public static boolean slim(ItemStack piece) {
        CompoundTag tag = piece.getTag();
        return tag != null && tag.getBoolean(SLIM);
    }

    public static void makeSlim(ItemStack piece) {
        piece.getOrCreateTag().putBoolean(SLIM, true);
    }

    public static List<String> modulesOn(ItemStack piece) {
        List<String> out = new ArrayList<>();
        CompoundTag tag = piece.getTag();
        if (tag != null) {
            for (Tag t : tag.getList(MODULES, Tag.TAG_STRING)) out.add(t.getAsString());
        }
        return out;
    }

    // The modules in the suit, as far as your mastery has slots for.
    public static List<String> modulesWorn(Player p, int slots) {
        List<String> all = modulesOn(p.getItemBySlot(EquipmentSlot.CHEST));
        return all.size() <= slots ? all : new ArrayList<>(all.subList(0, slots));
    }

    public static final class SuitPiece extends ArmorItem {
        SuitPiece() {
            super(MATERIAL, Type.CHESTPLATE, new Item.Properties().fireResistant().stacksTo(1));
        }

        @Override
        @Nullable
        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return CharPowers.MODID + ":textures/models/armor/" + (slim(stack) ? "invincible_slim" : "invincible") + ".png";
        }

        // One item in the chest slot, drawn on the whole body.
        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
                @Override
                public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                    // Forge copies which parts are showing from the normal model onto this one, so that has to show them all too
                    original.setAllVisible(true);
                    original.hat.visible = false;
                    var model = com.example.charpowers.armour.client.SuitModel.get();
                    original.copyPropertiesTo((net.minecraft.client.model.HumanoidModel) model);
                    model.setAllVisible(true);
                    model.hat.visible = false;
                    return model;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.literal("Battery " + Math.round(battery(stack)) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (String m : modulesOn(stack)) lines.add(Component.literal(m.substring(0, 1).toUpperCase() + m.substring(1) + " module").withStyle(ChatFormatting.GRAY));
            if (slim(stack)) lines.add(Component.literal("Slim").withStyle(ChatFormatting.GRAY));
        }
    }

    public static final class ModuleItem extends Item {
        public final String name;

        ModuleItem(String name) {
            super(new Item.Properties().stacksTo(16));
            this.name = name;
        }
    }

    // Any suit piece + a module it doesn't have yet = that piece with the module in it.
    public static final class ModuleRecipe extends CustomRecipe {
        public ModuleRecipe(ResourceLocation id, CraftingBookCategory category) {
            super(id, category);
        }

        private static ItemStack[] parts(CraftingContainer grid) {
            ItemStack piece = ItemStack.EMPTY, module = ItemStack.EMPTY;
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack s = grid.getItem(i);
                if (s.isEmpty()) continue;
                if (s.getItem() instanceof SuitPiece && piece.isEmpty()) piece = s;
                else if (s.getItem() instanceof ModuleItem && module.isEmpty()) module = s;
                else return null;
            }
            return piece.isEmpty() || module.isEmpty() ? null : new ItemStack[] {piece, module};
        }

        @Override
        public boolean matches(CraftingContainer grid, Level level) {
            ItemStack[] parts = parts(grid);
            return parts != null && !modulesOn(parts[0]).contains(((ModuleItem) parts[1].getItem()).name);
        }

        @Override
        public ItemStack assemble(CraftingContainer grid, net.minecraft.core.RegistryAccess access) {
            ItemStack[] parts = parts(grid);
            if (parts == null) return ItemStack.EMPTY;
            ItemStack out = parts[0].copy();
            ListTag list = out.getOrCreateTag().getList(MODULES, Tag.TAG_STRING);
            list.add(StringTag.valueOf(((ModuleItem) parts[1].getItem()).name));
            out.getOrCreateTag().put(MODULES, list);
            return out;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return MODULE_RECIPE.get();
        }
    }

    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) { return 0; } // it doesn't wear out: it runs out of battery instead

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return 20; // only ever one piece: it's the whole suit
        }

        @Override
        public int getEnchantmentValue() { return 0; }

        @Override
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }

        @Override
        public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }

        @Override
        public String getName() { return CharPowers.MODID + ":invincible"; }

        @Override
        public float getToughness() { return 12f; }

        @Override
        public float getKnockbackResistance() { return 0.5f; }
    };
}
