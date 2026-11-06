package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.threetag.palladium.power.SuperpowerUtil;

import javax.annotation.Nullable;

// One of Char's own big powers (the list on the Desktop). The power itself is a Palladium power file, made by
// tools/make_own_powers.py. Everything here only runs for players it concerns: see OwnPowers.
public abstract class OwnPower {
    public final ResourceLocation id;
    public final Item icon;
    private final String key;

    protected OwnPower(ResourceLocation id, Item icon) {
        this.id = id;
        this.icon = icon;
        this.key = "charpowers.own." + id.getPath();
    }

    // Once a second, only for players who own this power and aren't switched off.
    public void everySecond(ServerPlayer p) {}

    // Once a second for players who DON'T own it. Only for how-to-get steps nothing else can notice.
    public void watchSecond(ServerPlayer p) {}

    public boolean watches() { return false; }

    // A key ability was pressed. Cooldowns come from the power file.
    public void onKey(ServerPlayer p, String ability) {}

    // A line for this power's page in the op menu, and an extra button there.
    public String opInfo(net.minecraft.server.MinecraftServer server) {
        return null;
    }

    public String opButtonLabel() {
        return null;
    }

    public void opButton(ServerPlayer op) {}

    // For testing: /charpower trigger <power> <player> makes whatever is rare happen now.
    public void trigger(ServerPlayer p) {
        whisper(p, "That power has nothing to trigger.");
    }

    // A held key was let go.
    public void onKeyUp(ServerPlayer p, String ability) {}

    // The player's PC sent something (Net.OwnToServer). Check it: anyone can send anything.
    public void onClientAction(ServerPlayer p, String action) {}

    // What the player sees the moment they earn it.
    public abstract String unlockMessage();

    // Shown once at mastery 100. Null = nothing extra.
    @Nullable
    public String masteryHundredMessage() { return null; }

    // Shown when the quest is done.
    public abstract String awakenedMessage();

    public final boolean owns(Player p) {
        return Setting.enabled(id) && SuperpowerUtil.hasSuperpower(p, id);
    }

    public final int mastery(Player p) {
        return Mastery.level(p, id);
    }

    public final boolean awakened(Player p) {
        return Mastery.isAwakened(p, id);
    }

    public final boolean questReady(Player p) {
        return mastery(p) >= 100 && !awakened(p);
    }

    // Gives the power, nothing else. Powers that change other things when you get them override this.
    public void give(ServerPlayer p) {
        if (!SuperpowerUtil.addSuperpower(p, id)) {
            CharPowers.LOG.error("Couldn't add the Palladium power {} to {}", id, p.getGameProfile().getName());
            return;
        }
        Mastery.sync(p);
    }

    // How-to-get is finished. /charpower unlock jumps straight here.
    public void unlock(ServerPlayer p) {
        give(p);
        if (!owns(p)) return;
        announce(p, unlockMessage());
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1f);
    }

    public void awaken(ServerPlayer p) {
        Mastery.setAwakened(p, id);
        announce(p, awakenedMessage());
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1f);
    }

    // This power's own saved data on the player. It survives death, like mastery.
    public final CompoundTag data(Player p) {
        CompoundTag all = p.getPersistentData();
        if (!all.contains(Player.PERSISTED_NBT_TAG)) all.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = all.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(key)) root.put(key, new CompoundTag());
        return root.getCompound(key);
    }

    // How this power says its big moments. Chat, unless it has its own way.
    protected void announce(ServerPlayer p, String text) {
        tell(p, text);
    }

    public static void tell(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // The little line above the hotbar, for things that would flood the chat.
    public static void whisper(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
    }
}
