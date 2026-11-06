package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.icons.PowerIconItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.threetag.palladium.util.icon.IIcon;
import net.threetag.palladium.util.icon.ItemIcon;
import net.threetag.palladium.util.icon.TexturedIcon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Each hidden power's own picture: assets/charpowers/textures/icons/hidden/<id>.png (tools/make_icons.py draws them).
// If a new power has no picture yet, everything falls back to the item it registered with.
public final class HiddenIcons {
    private static final Map<String, Boolean> DRAWN = new ConcurrentHashMap<>();
    public static final ResourceLocation EMPTY = new ResourceLocation(CharPowers.MODID, "textures/icons/hidden/empty_slot.png");

    public static ResourceLocation texture(String id) {
        return new ResourceLocation(CharPowers.MODID, "textures/icons/hidden/" + id + ".png");
    }

    // Looked at once per power. The server has the pictures in the same jar, so this works on both sides.
    public static boolean drawn(String id) {
        return DRAWN.computeIfAbsent(id, i -> HiddenIcons.class.getResource("/assets/" + CharPowers.MODID + "/textures/icons/hidden/" + i + ".png") != null);
    }

    // For Palladium's key bar.
    public static IIcon icon(String id) {
        return drawn(id) ? new TexturedIcon(texture(id)) : new ItemIcon(HiddenPowers.icon(id));
    }

    // For anything that shows an ItemStack: the Power Guide, Death Steal's room.
    public static ItemStack stack(String id) {
        return drawn(id) ? PowerIconItem.of(texture(id)) : new ItemStack(HiddenPowers.icon(id));
    }

    private HiddenIcons() {}
}
