package com.example.charpowers.cloning;

import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

// Cloning's place in the list of own powers. Its real work lives in this package and has its own timers.
public final class CloningPower extends OwnPower {
    public CloningPower() {
        super(Mastery.CLONING, Items.ARMOR_STAND);
    }

    @Override
    public boolean watches() { return true; }

    // Lost the power (a DNA Normalizer, Teacher, an op...): clones can't outlive it.
    @Override
    public void watchSecond(ServerPlayer p) {
        if (!CloneManager.clonesOf(p.getUUID()).isEmpty()) CloneManager.dismissAll(p);
    }

    @Override
    public String unlockMessage() { return "Your reflection blinks. You didn't."; }

    @Override
    public String awakenedMessage() { return "You beat your reflection. It's part of you now: 5 clones, and stronger."; }
}
