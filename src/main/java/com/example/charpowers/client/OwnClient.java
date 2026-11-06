package com.example.charpowers.client;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// Where Net.OwnToClient messages go on the player's PC: each power's client code signs up here by name.
public final class OwnClient {
    private static final Map<String, Consumer<CompoundTag>> HANDLERS = new HashMap<>();

    private OwnClient() {}

    public static void on(String power, Consumer<CompoundTag> handler) {
        HANDLERS.put(power, handler);
    }

    public static void receive(String power, CompoundTag data) {
        Consumer<CompoundTag> handler = HANDLERS.get(power);
        if (handler != null) handler.accept(data);
    }
}
