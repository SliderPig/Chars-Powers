package com.example.charpowers.client;

import com.example.charpowers.Net;

import java.util.List;

// The player's own hidden powers, as the server last told us (for the menu tab).
public final class ClientHidden {
    private static List<String> owned = List.of();
    private static List<String> keys = List.of();
    private static List<String> always = List.of();

    public static void set(Net.SyncHidden m) {
        owned = List.copyOf(m.owned());
        keys = List.copyOf(m.keys());
        always = List.copyOf(m.always());
    }

    public static List<String> owned() { return owned; }

    private static boolean off;
    // key -> { text, when it last came (ms) }. The server repeats lines every second; old ones fade out by themselves.
    private static final java.util.Map<String, Object[]> HUD = new java.util.LinkedHashMap<>();

    public static void setOff(boolean value) { off = value; }

    // Is this power in one of my slots and working (not dampened)?
    public static boolean has(String id) {
        if (off) return false;
        return keys.contains(id) || always.contains(id);
    }

    public static void hud(String key, String text) {
        if (text.isEmpty()) HUD.remove(key);
        else HUD.put(key, new Object[]{text, System.currentTimeMillis()});
    }

    public static java.util.List<String> hudLines() {
        long now = System.currentTimeMillis();
        HUD.values().removeIf(v -> now - (long) v[1] > 3000);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Object[] v : HUD.values()) out.add((String) v[0]);
        return out;
    }

    // The server says this player lies flat for a while (Play Dead, Flop).
    public static void lieDown(int entityId, int ticks) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof net.minecraft.world.entity.player.Player p) {
            com.example.charpowers.hidden.Lying.startOnClient(p, ticks);
        }
    }

    // "" = empty slot
    public static String key(int i) { return i < keys.size() ? keys.get(i) : ""; }

    public static String always(int i) { return i < always.size() ? always.get(i) : ""; }

    private ClientHidden() {}
}
