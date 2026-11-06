package com.example.charpowers.cloning;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// One clone as the clones menu shows it. items = its whole inventory: 0-35 pockets, 36-39 armour, 40 off hand.
public record CloneInfo(int slot, String name, boolean picked, boolean out, String state, String task, float health, float maxHealth, int food,
                        String where, String chest, List<Component> powers, List<ItemStack> items) {
    // Its nickname, or its number if it hasn't got one.
    public String label() {
        return name.isEmpty() ? "Clone " + (slot + 1) : name;
    }

    public void write(FriendlyByteBuf b) {
        b.writeVarInt(slot);
        b.writeUtf(name, 32);
        b.writeBoolean(picked);
        b.writeBoolean(out);
        b.writeUtf(state, 64);
        b.writeUtf(task, 64);
        b.writeFloat(health);
        b.writeFloat(maxHealth);
        b.writeVarInt(food);
        b.writeUtf(where, 96);
        b.writeUtf(chest, 64);
        b.writeCollection(powers, FriendlyByteBuf::writeComponent);
        b.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static CloneInfo read(FriendlyByteBuf b) {
        return new CloneInfo(b.readVarInt(), b.readUtf(32), b.readBoolean(), b.readBoolean(), b.readUtf(64), b.readUtf(64), b.readFloat(), b.readFloat(), b.readVarInt(),
                b.readUtf(96), b.readUtf(64), b.readList(FriendlyByteBuf::readComponent), b.readList(FriendlyByteBuf::readItem));
    }
}
