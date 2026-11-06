package com.example.charpowers.translation;

import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.translation.model.VoiceRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;

// Hear what mobs are saying, in voice lines from my own resource pack. Nearly all of it runs on the player's
// PC (client/VoiceClient). The server only counts the parrot time and the mastery.
public final class Translation extends OwnPower {
    public static final Translation POWER = new Translation();
    private static final String PARROT = "parrotSeconds";
    private static final String PARROT_ID = "minecraft:parrot";

    private Translation() {
        super(Mastery.TRANSLATION, Items.PARROT_SPAWN_EGG);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "Your parrot said something. You understood it."; }

    @Override
    public String awakenedMessage() { return "You've heard them all. Now you can choose who gets to speak: Options, Music & Sounds, Mob Voices."; }

    // How to get it: a parrot on your shoulder for 20 minutes in total.
    @Override
    public void watchSecond(ServerPlayer p) {
        if (!isParrot(p.getShoulderEntityLeft()) && !isParrot(p.getShoulderEntityRight())) return;
        CompoundTag d = data(p);
        int seconds = d.getInt(PARROT) + 1;
        d.putInt(PARROT, seconds);
        if (seconds >= VoiceRules.PARROT_SECONDS) unlock(p);
    }

    private static boolean isParrot(CompoundTag shoulder) {
        return !shoulder.isEmpty() && PARROT_ID.equals(shoulder.getString("id"));
    }

    // Mastery: a second near any mob counts as listening.
    @Override
    public void everySecond(ServerPlayer p) {
        int range = VoiceRules.range(mastery(p));
        if (!p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(range), m -> true).isEmpty()) Mastery.add(p, id, 1);
    }

    // The player's PC says every line in their folders has been heard.
    @Override
    public void onClientAction(ServerPlayer p, String action) {
        if ("heard_all".equals(action) && owns(p) && questReady(p)) awaken(p);
    }
}
