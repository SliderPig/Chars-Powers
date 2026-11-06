package com.example.charpowers.mastery;

import com.example.charpowers.Net;
import com.example.charpowers.mastery.model.MasteryMath;
import com.example.charpowers.own.OwnPowers;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Every one of Char's own powers has its own mastery, 100 levels (see "My powers list").
// Points are saved on the player and survive death. Add a power to TRACKED to give it a bar on the power screen.
public final class Mastery {
    public static final ResourceLocation CLONING = new ResourceLocation("charpowers", "cloning");
    public static final ResourceLocation TEACHER = new ResourceLocation("charpowers", "teacher");
    public static final ResourceLocation TRANSLATION = new ResourceLocation("charpowers", "translation");
    public static final ResourceLocation FUTURE_SIGHT = new ResourceLocation("charpowers", "future_sight");
    public static final ResourceLocation SUPERIOR_AUTHORITY = new ResourceLocation("charpowers", "superior_authority");
    public static final ResourceLocation THE_MINER = new ResourceLocation("charpowers", "the_miner");
    public static final ResourceLocation LIFE_ENJOYER = new ResourceLocation("charpowers", "life_enjoyer");
    public static final ResourceLocation VOCAL_EXPRESSION = new ResourceLocation("charpowers", "vocal_expression");
    public static final ResourceLocation TIME_CONTROL = new ResourceLocation("charpowers", "time_control");
    public static final ResourceLocation NECROMANCY = new ResourceLocation("charpowers", "necromancy");
    public static final ResourceLocation MOB_CONTROL = new ResourceLocation("charpowers", "mob_control");
    public static final ResourceLocation DEATH_STEAL = new ResourceLocation("charpowers", "death_steal");
    public static final ResourceLocation INVINCIBLE_ARMOUR = new ResourceLocation("charpowers", "invincible_armour");
    public static final ResourceLocation GREEN_LANTERN = new ResourceLocation("charpowers", "green_lantern");
    public static final ResourceLocation BOOK_KEEPING = new ResourceLocation("charpowers", "book_keeping");
    public static final ResourceLocation GAME_PLAYER = new ResourceLocation("charpowers", "game_player");
    public static final ResourceLocation HOUSE_BUILDER = new ResourceLocation("charpowers", "house_builder");
    public static final List<ResourceLocation> TRACKED = List.of(CLONING, TEACHER, TRANSLATION, FUTURE_SIGHT, SUPERIOR_AUTHORITY, THE_MINER, LIFE_ENJOYER, VOCAL_EXPRESSION, TIME_CONTROL, NECROMANCY, MOB_CONTROL, DEATH_STEAL, INVINCIBLE_ARMOUR, GREEN_LANTERN, BOOK_KEEPING, GAME_PLAYER, HOUSE_BUILDER);
    private static final String KEY = "charpowers.mastery";
    // Only used on the player's PC: what the last MasterySync said (the PC has no saved points to read).
    private static volatile Map<String, Integer> clientLevels = Map.of();

    private Mastery() {}

    private static CompoundTag tag(Player p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static long points(Player p, ResourceLocation power) {
        return tag(p).getLong(power.toString());
    }

    public static int level(Player p, ResourceLocation power) {
        return MasteryMath.levelOf(points(p, power));
    }

    // For things that run on both sides and have to agree, like how fast a block breaks.
    public static int levelAnySide(Player p, ResourceLocation power) {
        return p.level().isClientSide ? clientLevels.getOrDefault(power.toString(), 1) : level(p, power);
    }

    // The PC's toast signs up here: told when a level goes up (not on the first sync after joining).
    public static volatile java.util.function.BiConsumer<String, Integer> clientLevelUp;

    public static void setClientLevels(Map<String, Integer> levels) {
        Map<String, Integer> was = clientLevels;
        clientLevels = Map.copyOf(levels);
        java.util.function.BiConsumer<String, Integer> hook = clientLevelUp;
        if (hook == null || was.isEmpty()) return;
        for (Map.Entry<String, Integer> e : levels.entrySet()) {
            Integer before = was.get(e.getKey());
            if (!e.getKey().contains("#") && before != null && e.getValue() > before) hook.accept(e.getKey(), e.getValue());
        }
    }

    public static void add(ServerPlayer p, ResourceLocation power, long base) {
        // ops can change how fast each power's mastery goes. A part point is a chance of a point.
        double scaled = base * com.example.charpowers.own.Setting.masteryRate(power.getPath());
        long points = (long) scaled;
        if (p.getRandom().nextDouble() < scaled - points) points++;
        if (points <= 0) return;
        int before = level(p, power);
        tag(p).putLong(power.toString(), Math.min(MasteryMath.POINTS_TO_MAX, points(p, power) + points));
        int after = level(p, power);
        if (after != before) {
            p.sendSystemMessage(Component.literal("Mastery " + after + "/" + MasteryMath.MAX_LEVEL).withStyle(ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
            if (after == MasteryMath.MAX_LEVEL) OwnPowers.reachedHundred(p, power);
            sync(p);
            if (after > before) com.example.charpowers.cat.CompanionCats.cheer(p);
        }
    }

    public static void setLevel(ServerPlayer p, ResourceLocation power, int level) {
        int before = level(p, power);
        tag(p).putLong(power.toString(), MasteryMath.pointsFor(level));
        if (level == MasteryMath.MAX_LEVEL && before != level) OwnPowers.reachedHundred(p, power);
        sync(p);
    }

    public static boolean isAwakened(Player p, ResourceLocation power) {
        return tag(p).getBoolean(power + "#awakened");
    }

    public static void setAwakened(ServerPlayer p, ResourceLocation power) {
        tag(p).putBoolean(power + "#awakened", true);
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        Map<String, Integer> levels = new HashMap<>();
        for (ResourceLocation id : TRACKED) {
            levels.put(id.toString(), level(p, id));
            if (isAwakened(p, id)) levels.put(id + "#awakened", 1);
        }
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Net.MasterySync(levels));
    }
}
