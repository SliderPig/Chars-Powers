package com.example.charpowers.house;

import com.example.charpowers.CharPowers;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Turning block names from a file into real blocks. Blocks from an older game are brought up to date the way the game itself does it, modded
// names work when that mod is here, unknown ones are left out. Some blocks are never placed from a file, whatever it says.
public final class HouseBlocks {
    private static final Set<Block> NEVER = Set.of(Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK, Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW,
            Blocks.SPAWNER, Blocks.BARRIER, Blocks.BEDROCK, Blocks.END_PORTAL, Blocks.END_GATEWAY, Blocks.NETHER_PORTAL, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.LAVA, Blocks.TNT,
            Blocks.STRUCTURE_VOID, Blocks.LIGHT, Blocks.REINFORCED_DEEPSLATE);

    private HouseBlocks() {}

    // "minecraft:oak_stairs[facing=east]" to a block state, or null to leave it out. The kinds it couldn't place go in `skipped`.
    public static BlockState resolve(String text, int dataVersion, Set<String> skipped) {
        String name = text;
        Map<String, String> props = new LinkedHashMap<>();
        int open = text.indexOf('[');
        if (open >= 0 && text.endsWith("]")) {
            name = text.substring(0, open);
            for (String pair : text.substring(open + 1, text.length() - 1).split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) props.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        int now = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        if (dataVersion > 0 && dataVersion < now && (name.startsWith("minecraft:") || name.indexOf(':') < 0)) {
            try {
                CompoundTag tag = new CompoundTag();
                tag.putString("Name", name);
                if (!props.isEmpty()) {
                    CompoundTag p = new CompoundTag();
                    props.forEach(p::putString);
                    tag.put("Properties", p);
                }
                Tag fixed = DataFixers.getDataFixer().update(References.BLOCK_STATE, new Dynamic<Tag>(NbtOps.INSTANCE, tag), dataVersion, now).getValue();
                if (fixed instanceof CompoundTag c) {
                    name = c.getString("Name");
                    props.clear();
                    CompoundTag p = c.getCompound("Properties");
                    for (String key : p.getAllKeys()) props.put(key, p.getString(key));
                }
            } catch (RuntimeException e) {
                CharPowers.LOG.debug("Couldn't update the old block {}", text, e);
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            skipped.add(name);
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (NEVER.contains(block)) {
            skipped.add(name);
            return null;
        }
        if (block.defaultBlockState().isAir()) return null;
        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> e : props.entrySet()) state = withProperty(state, e.getKey(), e.getValue());
        return state;
    }

    private static BlockState withProperty(BlockState state, String key, String value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
        return property == null ? state : apply(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState apply(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.isPresent() ? state.setValue(property, parsed.get()) : state;
    }

    // An old schematic's number and data to a block string, or null if the game never had it.
    public static String legacy(int idAndData) {
        try {
            Dynamic<?> tag = BlockStateData.getTag(idAndData);
            String name = tag.get("Name").asString("");
            if (name.isEmpty() || name.equals("minecraft:air")) return null;
            Map<String, String> props = tag.get("Properties").asMap(k -> k.asString(""), v -> v.asString(""));
            if (props.isEmpty()) return name;
            StringBuilder out = new StringBuilder(name).append('[');
            boolean first = true;
            for (Map.Entry<String, String> e : props.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(e.getKey()).append('=').append(e.getValue());
            }
            return out.append(']').toString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
