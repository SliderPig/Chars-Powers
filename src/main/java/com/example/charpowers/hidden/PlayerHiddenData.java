package com.example.charpowers.hidden;

import com.example.charpowers.Net;
import com.example.charpowers.hidden.model.Loadout;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Saves a player's hidden powers. It lives in Forge's "PlayerPersisted" data, which survives dying.
public final class PlayerHiddenData {
    private static final String KEY = "charpowers.hidden";
    private static final String STATE = "charpowers.hidden.state";

    private static CompoundTag root(Player p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static Loadout get(Player p) {
        CompoundTag tag = root(p).getCompound(KEY);
        Map<String, Object> m = new HashMap<>();
        m.put("owned", strings(tag.getList("owned", Tag.TAG_STRING)));
        m.put("keys", strings(tag.getList("keys", Tag.TAG_STRING)));
        m.put("always", strings(tag.getList("always", Tag.TAG_STRING)));
        return Loadout.fromMap(m);
    }

    @SuppressWarnings("unchecked")
    public static void save(ServerPlayer p, Loadout l) {
        CompoundTag tag = new CompoundTag();
        Map<String, Object> m = l.toMap();
        for (String k : List.of("owned", "keys", "always")) {
            ListTag list = new ListTag();
            for (String s : (List<String>) m.get(k)) list.add(StringTag.valueOf(s));
            tag.put(k, list);
        }
        root(p).put(KEY, tag);
        sync(p);
    }

    // Saved scratch space for one power on this player (timers etc).
    public static CompoundTag state(Player p, String id) {
        CompoundTag r = root(p);
        if (!r.contains(STATE)) r.put(STATE, new CompoundTag());
        CompoundTag all = r.getCompound(STATE);
        if (!all.contains(id)) all.put(id, new CompoundTag());
        return all.getCompound(id);
    }

    public static void clearState(Player p, String id) {
        root(p).getCompound(STATE).remove(id);
    }

    // Safety net for respawning: copy our data from the old player if Forge didn't already.
    public static void copy(Player from, Player to) {
        CompoundTag old = root(from);
        CompoundTag now = root(to);
        if (old.contains(KEY) && !now.contains(KEY)) now.put(KEY, old.getCompound(KEY).copy());
        if (old.contains(STATE) && !now.contains(STATE)) now.put(STATE, old.getCompound(STATE).copy());
    }

    public static void sync(ServerPlayer p) {
        Net.sendTo(p, Net.SyncHidden.of(get(p)));
    }

    private static List<String> strings(ListTag list) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    private PlayerHiddenData() {}
}
