package com.example.charpowers.hidden;

import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Supplier;

// The list of every hidden power: its facts (PowerDef), its icon and what it does (PowerEffect).
// Each rank's file (e.g. f/FPowers) adds its powers here once at start-up.
public final class HiddenPowers {
    private record Entry(PowerDef def, Supplier<Item> icon, PowerEffect effect) {}

    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();
    private static final PowerEffect NOTHING = new PowerEffect() {};

    public static void register(PowerDef def, Supplier<Item> icon, PowerEffect effect) {
        if (BY_ID.containsKey(def.id())) throw new IllegalStateException("Hidden power registered twice: " + def.id());
        BY_ID.put(def.id(), new Entry(def, icon, effect));
    }

    public static Optional<PowerDef> def(String id) {
        Entry e = id == null ? null : BY_ID.get(id);
        return e == null ? Optional.empty() : Optional.of(e.def());
    }

    public static PowerEffect effect(String id) {
        Entry e = BY_ID.get(id);
        return e == null ? NOTHING : e.effect();
    }

    public static Item icon(String id) {
        Entry e = BY_ID.get(id);
        return e == null ? Items.BARRIER : e.icon().get();
    }

    public static List<PowerDef> all() {
        List<PowerDef> out = new ArrayList<>();
        BY_ID.values().forEach(e -> out.add(e.def()));
        return out;
    }

    public static List<PowerDef> ofRank(Rank r) {
        return all().stream().filter(d -> d.rank() == r).toList();
    }

    public static Set<String> ids() { return Collections.unmodifiableSet(BY_ID.keySet()); }

    public static Component name(String id) { return Component.translatable("power.charpowers." + id); }

    public static Component description(String id) { return Component.translatable("power.charpowers." + id + ".desc"); }

    private HiddenPowers() {}
}
