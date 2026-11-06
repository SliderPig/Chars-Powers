package com.example.charpowers.hidden.model;

import java.util.*;
import java.util.random.RandomGenerator;

// A player's hidden powers: everything they own (the collection) + what's in the 5 key and 10 always-on slots.
// All the "is this allowed?" rules live here so the server can check anything the client asks for.
public final class Loadout {
    public static final int KEY_SLOTS = 5;
    public static final int ALWAYS_SLOTS = 10;

    private final LinkedHashSet<String> owned = new LinkedHashSet<>();
    private final String[] keys = new String[KEY_SLOTS];
    private final String[] always = new String[ALWAYS_SLOTS];

    public Set<String> owned() { return Collections.unmodifiableSet(owned); }
    public String key(int i) { return i >= 0 && i < KEY_SLOTS ? keys[i] : null; }
    public String always(int i) { return i >= 0 && i < ALWAYS_SLOTS ? always[i] : null; }

    // Adds a power to the collection. Getting one you already have does nothing.
    public boolean add(String id) { return owned.add(id); }

    public boolean equip(PowerDef p, boolean keySlot, int slot) {
        if (!owned.contains(p.id())) return false;
        if (keySlot != (p.kind() == Kind.KEY)) return false;
        String[] arr = keySlot ? keys : always;
        if (slot < 0 || slot >= arr.length) return false;
        clearSlotsHolding(p.id()); // moving, not copying
        arr[slot] = p.id();
        return true;
    }

    public void unequip(boolean keySlot, int slot) {
        String[] arr = keySlot ? keys : always;
        if (slot >= 0 && slot < arr.length) arr[slot] = null;
    }

    public List<String> equipped() {
        List<String> out = new ArrayList<>();
        for (String s : keys) if (s != null) out.add(s);
        for (String s : always) if (s != null) out.add(s);
        return out;
    }

    // Takes one power away completely (collection and slot).
    public boolean remove(String id) {
        clearSlotsHolding(id);
        return owned.remove(id);
    }

    public void clearAll() {
        owned.clear();
        Arrays.fill(keys, null);
        Arrays.fill(always, null);
    }

    public Optional<String> removeRandomEquipped(RandomGenerator r) {
        List<String> eq = equipped();
        if (eq.isEmpty()) return Optional.empty();
        String id = eq.get(r.nextInt(eq.size()));
        remove(id);
        return Optional.of(id);
    }

    public List<String> removeEquipped() {
        List<String> eq = equipped();
        eq.forEach(this::remove);
        return eq;
    }

    private void clearSlotsHolding(String id) {
        for (int i = 0; i < keys.length; i++) if (id.equals(keys[i])) keys[i] = null;
        for (int i = 0; i < always.length; i++) if (id.equals(always[i])) always[i] = null;
    }

    // Plain lists of strings so the Minecraft side can turn it into NBT (empty slot = "").
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("owned", new ArrayList<>(owned));
        m.put("keys", slotList(keys));
        m.put("always", slotList(always));
        return m;
    }

    public static Loadout fromMap(Map<String, Object> m) {
        Loadout l = new Loadout();
        if (m.get("owned") instanceof List<?> list) {
            for (Object x : list) if (x instanceof String s && !s.isEmpty()) l.owned.add(s);
        }
        fill(l, l.keys, m.get("keys"));
        fill(l, l.always, m.get("always"));
        return l;
    }

    private static List<String> slotList(String[] arr) {
        List<String> out = new ArrayList<>();
        for (String s : arr) out.add(s == null ? "" : s);
        return out;
    }

    private static void fill(Loadout l, String[] arr, Object o) {
        if (!(o instanceof List<?> list)) return;
        for (int i = 0; i < arr.length && i < list.size(); i++) {
            // never equip something that isn't owned (broken or hand-edited saves)
            if (list.get(i) instanceof String s && !s.isEmpty() && l.owned.contains(s)) arr[i] = s;
        }
    }
}
