package com.example.charpowers.cloning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

// What's saved on the host (survives death and restarts): each slot's death lock, marked chest, nickname and whether
// the Clone key brings it, and Escaped Death's cooldown. Times are world game time.
final class CloneData {
    private static final String KEY = "charpowers.clones";

    private CloneData() {}

    private static CompoundTag tag(Player host) {
        CompoundTag data = host.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    static long lockedUntil(Player host, int slot) {
        return tag(host).getLong("lock" + slot);
    }

    static void lock(Player host, int slot, long until) {
        tag(host).putLong("lock" + slot, until);
    }

    // Whether the Clone key brings this one out. All of them do until you untick one.
    static boolean picked(Player host, int slot) {
        return !tag(host).getBoolean("unpicked" + slot);
    }

    static void setPicked(Player host, int slot, boolean picked) {
        tag(host).putBoolean("unpicked" + slot, !picked);
    }

    static String nickname(Player host, int slot) {
        return tag(host).getString("nick" + slot);
    }

    static void setNickname(Player host, int slot, String name) {
        tag(host).putString("nick" + slot, name);
    }

    // What to call it in messages.
    static String label(Player host, int slot) {
        String name = nickname(host, slot);
        return name.isEmpty() ? "Clone " + (slot + 1) : name;
    }

    static long escapedReadyAt(Player host) {
        return tag(host).getLong("escapedReady");
    }

    static void setEscapedReadyAt(Player host, long time) {
        tag(host).putLong("escapedReady", time);
    }

    // The best gear this player has ever worn or held, for the reflection to use if they strip off to fight it.
    static ItemStack bestGear(Player host, EquipmentSlot slot) {
        CompoundTag t = tag(host);
        return t.contains("best" + slot.getName()) ? ItemStack.of(t.getCompound("best" + slot.getName())) : ItemStack.EMPTY;
    }

    static void rememberBestGear(Player host) {
        CompoundTag t = tag(host);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack now = host.getItemBySlot(slot);
            if (now.isEmpty()) continue;
            ItemStack best = bestGear(host, slot);
            if (best.isEmpty() || CloneGear.gearScore(now) > CloneGear.gearScore(best)) t.put("best" + slot.getName(), now.save(new CompoundTag()));
        }
    }

    static void setChest(Player host, int slot, GlobalPos where) {
        CompoundTag t = tag(host);
        t.putString("chestDim" + slot, where.dimension().location().toString());
        t.putLong("chestPos" + slot, where.pos().asLong());
    }

    @Nullable
    static GlobalPos chest(Player host, int slot) {
        CompoundTag t = tag(host);
        if (!t.contains("chestPos" + slot)) return null;
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("chestDim" + slot))),
                BlockPos.of(t.getLong("chestPos" + slot)));
    }
}
