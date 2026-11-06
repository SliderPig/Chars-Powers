package com.example.charpowers.items;

import com.example.charpowers.game.SystemWeapon;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;

// Which of Char's weapons swings which way. Used on both sides: the PC draws the move, the server lands its hit at the same moment.
public final class WeaponStyles {
    private WeaponStyles() {}

    @Nullable
    public static WeaponStyle of(Item item) {
        if (item instanceof GoldenLongSword) return WeaponStyle.GOLD;
        if (item instanceof AnimaKatana) return WeaponStyle.KATANA;
        if (item instanceof DaggerOfDarkness) return WeaponStyle.DAGGER;
        if (item instanceof FlameHammer) return WeaponStyle.HAMMER;
        if (item instanceof IceStaff) return WeaponStyle.STAFF;
        if (item instanceof ForkItem) return WeaponStyle.FORK;
        if (item instanceof SprayBottle) return WeaponStyle.SPRAY;
        if (item instanceof SystemWeapon w) return of(w.kind);
        return null;
    }

    public static WeaponStyle of(SystemWeapon.Kind kind) {
        return switch (kind) {
            case KNIGHT_KILLER -> WeaponStyle.KNIGHT;
            case CRIMSON_EDGE -> WeaponStyle.CRIMSON;
            case GRAVITY_BLADE -> WeaponStyle.GRAVITY;
            case GRAND_SPEAR -> WeaponStyle.SPEAR;
            case SHADOW_SCYTHE -> WeaponStyle.SCYTHE;
            case COCYTUS_HAMMER -> WeaponStyle.COCYTUS;
        };
    }

    // Swings that wait for the blade to arrive before they hit. The Long Sword runs its own combo, the bottle and staff don't hit.
    public static boolean timedHits(WeaponStyle s) {
        return s != null && s != WeaponStyle.GOLD && s != WeaponStyle.SPRAY && s != WeaponStyle.STAFF;
    }
}
