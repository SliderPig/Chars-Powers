package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.SuitItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

// Char's weapons and everything they need: the items, the sword in the stone, the ice shard, Solgo, Kings Rock and the sounds. Nothing to do with powers.
public final class CharItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CharPowers.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CharPowers.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CharPowers.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CharPowers.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, CharPowers.MODID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, CharPowers.MODID);

    public static final RegistryObject<Item> ICE_STAFF = ITEMS.register("ice_staff", IceStaff::new);
    public static final RegistryObject<Item> LONG_SWORD = ITEMS.register("golden_kings_long_sword", GoldenLongSword::new);
    public static final RegistryObject<Item> FLAME_HAMMER = ITEMS.register("flame_hammer", FlameHammer::new);
    public static final RegistryObject<Item> FORK = ITEMS.register("fork", ForkItem::new);
    public static final RegistryObject<Item> SPRAY_BOTTLE = ITEMS.register("spray_bottle", SprayBottle::new);
    public static final RegistryObject<Item> DAGGER = ITEMS.register("dagger_of_darkness", DaggerOfDarkness::new);
    public static final RegistryObject<Item> KATANA = ITEMS.register("anima_katana", AnimaKatana::new);

    public static final RegistryObject<Block> SWORD_STONE = BLOCKS.register("sword_in_stone", SwordInStone::new);
    public static final RegistryObject<Item> SWORD_STONE_ITEM = ITEMS.register("sword_in_stone", () -> new BlockItem(SWORD_STONE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<SwordInStone.Entity>> SWORD_STONE_ENTITY = BLOCK_ENTITIES.register("sword_in_stone",
            () -> BlockEntityType.Builder.of(SwordInStone.Entity::new, SWORD_STONE.get()).build(null));

    public static final RegistryObject<EntityType<IceShard>> ICE_SHARD = ENTITIES.register("ice_shard",
            () -> EntityType.Builder.<IceShard>of(IceShard::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(2).build("charpowers:ice_shard"));
    public static final RegistryObject<EntityType<Solgo>> SOLGO = ENTITIES.register("solgo",
            () -> EntityType.Builder.<Solgo>of(Solgo::new, MobCategory.MONSTER).sized(1.5f, 7.5f).clientTrackingRange(16).build("charpowers:solgo"));
    private static final RegistryObject<Item> SOLGO_EGG = ITEMS.register("solgo_spawn_egg", () -> new ForgeSpawnEggItem(SOLGO, 0x17131f, 0xc9c6bd, new Item.Properties()));

    public static final RegistryObject<StructureType<KingsRock>> KINGS_ROCK = STRUCTURES.register("kings_rock", () -> () -> KingsRock.CODEC);
    public static final RegistryObject<StructurePieceType> KINGS_ROCK_PIECE = PIECES.register("kings_rock", () -> (StructurePieceType.ContextlessType) KingsRock.Piece::new);

    private static final Map<String, RegistryObject<SoundEvent>> SOUND_LIST = new HashMap<>();

    static {
        for (String name : new String[]{"spray_water", "spray_lava", "spray_milk", "spray_fill", "ice_cast", "ice_shatter", "hammer_slam", "flame_burst", "sword_pull", "sword_ring", "gold_swing", "gold_slam", "gold_thrust", "gold_beat",
                "katana_swing", "soul_cut", "dagger_swing", "fork_ting", "solgo_breath", "solgo_whisper", "solgo_windup", "solgo_slash", "solgo_lunge", "solgo_scream", "solgo_flicker", "solgo_hurt", "solgo_death"}) {
            SOUND_LIST.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CharPowers.MODID, name))));
        }
    }

    private CharItems() {}

    public static SoundEvent sound(String name) {
        return SOUND_LIST.get(name).get();
    }

    public static void register(IEventBus modBus) {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ItemConfig.SPEC, "charpowers-items.toml");
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURES.register(modBus);
        PIECES.register(modBus);
        modBus.addListener(CharItems::attributes);
        modBus.addListener(CharItems::tab);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(ItemNet::register));
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(SOLGO.get(), Solgo.attributes().build());
    }

    private static void tab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == SuitItems.TAB.getKey()) {
            for (RegistryObject<Item> item : ITEMS.getEntries()) e.accept(item);
        } else if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(SOLGO_EGG);
        }
    }
}
