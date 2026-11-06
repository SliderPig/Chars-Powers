package com.example.charpowers.hidden.f;

// All 100 F-rank hidden powers (useless and funny). Called once when the mod starts.
public final class FPowers {
    public static void registerAll() {
        FKeyPowers.registerAll();
        FBodyPowers.registerAll();
        FEventPowers.registerAll();
    }

    private FPowers() {}
}
