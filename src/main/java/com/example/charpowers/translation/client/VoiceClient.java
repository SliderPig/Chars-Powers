package com.example.charpowers.translation.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.cloning.client.CloneClient;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.translation.model.VoiceRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

// Plays the voice lines. Only you hear them, because this only runs on your PC, and only if you own the power.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class VoiceClient {
    private static final Random RANDOM = new Random();
    private static final Path HEARD_FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/voices_heard.txt");
    private static final Path OFF_FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/voices_off.txt");

    private static List<VoiceRules.Line> lines;
    private static Set<String> heard, off;
    private static final Map<String, Long> LAST_FROM_MOB = new HashMap<>();
    private static long lastLine;

    private VoiceClient() {}

    private static boolean owned(LocalPlayer me) {
        return AbilityUtil.hasPower(me, Mastery.TRANSLATION);
    }

    private static int level() {
        Integer level = CloneClient.masteryLevel(Mastery.TRANSLATION.toString());
        return level == null ? 1 : level;
    }

    public static boolean awakened() {
        return CloneClient.masteryLevel(Mastery.TRANSLATION + "#awakened") != null;
    }

    // Every line in the resource packs that are on. Read again each time you join and each time the list opens.
    public static List<VoiceRules.Line> lines() {
        if (lines == null) reload();
        return lines;
    }

    public static void reload() {
        List<VoiceRules.Line> found = new ArrayList<>();
        for (ResourceLocation id : Minecraft.getInstance().getSoundManager().getAvailableSounds()) {
            if (!CharPowers.MODID.equals(id.getNamespace())) continue;
            VoiceRules.Line line = VoiceRules.parse(id.getPath());
            if (line != null) found.add(line);
        }
        found.sort(Comparator.comparing(VoiceRules.Line::id));
        lines = found;
        if (heard == null) heard = read(HEARD_FILE);
        if (off == null) off = read(OFF_FILE);
    }

    public static Set<String> heard() {
        lines();
        return heard;
    }

    public static boolean isOff(String mob) {
        lines();
        return off.contains(mob);
    }

    public static void setOff(String mob, boolean mute) {
        lines();
        if (mute) off.add(mob);
        else off.remove(mob);
        write(OFF_FILE, off);
    }

    private static Set<String> read(Path file) {
        try {
            return Files.exists(file) ? new TreeSet<>(Files.readAllLines(file)) : new TreeSet<>();
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't read {}", file, e);
            return new TreeSet<>();
        }
    }

    private static void write(Path file, Set<String> values) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, values);
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't save {}", file, e);
        }
    }

    @SubscribeEvent
    public static void onJoin(ClientPlayerNetworkEvent.LoggingIn e) {
        lines = null;
    }

    // Every 2 seconds at most, and only with the power and at least one line to play.
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.player == null || mc.level == null || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        if (now % 1200 == 0 && level() >= 100 && !awakened() && owned(mc.player) && VoiceRules.heardAll(lines(), heard)) {
            Net.CHANNEL.sendToServer(new Net.OwnToServer(Mastery.TRANSLATION.toString(), "heard_all"));
        }
        if (now % 40 != 0 || now - lastLine < VoiceRules.LINE_GAP_TICKS || !owned(mc.player) || lines().isEmpty()) return;
        int level = level();
        int range = VoiceRules.range(level);
        List<Mob> mobs = mc.level.getEntitiesOfClass(Mob.class, mc.player.getBoundingBox().inflate(range), m -> true);
        mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(mc.player)));
        for (Mob mob : mobs) {
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
            if (type == null || isOff(type.getPath())) continue;
            if (now - LAST_FROM_MOB.getOrDefault(type.getPath(), -VoiceRules.SAME_MOB_GAP_TICKS) < VoiceRules.SAME_MOB_GAP_TICKS) continue;
            VoiceRules.Line line = VoiceRules.pick(lines, type.getPath(), level, heard, RANDOM);
            if (line == null) continue;
            play(mc, line, mob);
            lastLine = now;
            LAST_FROM_MOB.put(type.getPath(), now);
            return;
        }
    }

    private static void play(Minecraft mc, VoiceRules.Line line, Mob mob) {
        mc.getSoundManager().play(new SimpleSoundInstance(new ResourceLocation(CharPowers.MODID, line.id()), SoundSource.VOICE,
                1f, 1f, RandomSource.create(), false, 0, SoundInstance.Attenuation.LINEAR, mob.getX(), mob.getY(), mob.getZ(), false));
        if (heard.add(line.id())) write(HEARD_FILE, heard);
    }

    // The list lives in the sounds menu, as the powers list asks.
    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (!(e.getScreen() instanceof SoundOptionsScreen screen) || mc.player == null || !owned(mc.player)) return;
        e.addListener(Button.builder(Component.literal("Mob Voices"), b -> mc.setScreen(new VoicesScreen(screen)))
                .bounds(screen.width / 2 + 105, screen.height - 27, 80, 20).build());
    }
}
