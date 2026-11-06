package com.example.charpowers.items.client;

import com.example.charpowers.CharPowers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

// Punchy (first-person animations, optional) leaves alone any item on its own blacklist and draws it the normal way, which is where our
// moves come in. So all of Char's weapons go on that list, in memory only: Punchy's own config file isn't touched. Checked now and then,
// because Punchy can reload its settings. Without Punchy this does nothing.
final class PunchyBridge {
    private static final boolean THERE = ModList.get().isLoaded("punchy");
    private static int wait;
    private static boolean broken, handedOver;
    private static List<String> ours;

    private PunchyBridge() {}

    static void tick() {
        if (!THERE || broken || --wait > 0) return;
        wait = 100;
        try {
            Class<?> config = Class.forName("punchy.config.PunchyConfig");
            Field dataField = config.getDeclaredField("DATA");
            dataField.setAccessible(true);
            Object data = dataField.get(null);
            if (data == null) return;
            Field listField = data.getClass().getDeclaredField("itemBlacklist");
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) listField.get(data);
            List<String> want = ourItems();
            if (list != null && list.containsAll(want)) {
                handedOver = true;
                return;
            }
            List<String> fresh = new ArrayList<>(list == null ? List.of() : list);
            for (String id : want) {
                if (!fresh.contains(id)) fresh.add(id);
            }
            listField.set(data, fresh);
            handedOver = true;
        } catch (Throwable t) {
            broken = true;
            CharPowers.LOG.warn("Couldn't hand Char's weapons over from Punchy, it keeps its own moves for them: {}", t.toString());
        }
    }

    // True once Punchy has really been told: then it draws them the plain way and our own arms go on.
    static boolean handedOver() {
        return handedOver;
    }

    private static List<String> ourItems() {
        if (ours != null) return ours;
        List<String> out = new ArrayList<>();
        for (var item : ForgeRegistries.ITEMS) {
            if (com.example.charpowers.items.WeaponStyles.of(item) != null) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null) out.add(id.toString());
            }
        }
        ours = out;
        return out;
    }
}
