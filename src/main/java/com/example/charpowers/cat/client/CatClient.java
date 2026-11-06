package com.example.charpowers.cat.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cat.CompanionCat;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// Your PC's side of the cat: other people's cats are cute, but you can hide them with /charcathide (your own always shows).
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class CatClient {
    private static boolean hideOthers = load();

    private CatClient() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("charpowers").resolve("cat.properties");
    }

    private static boolean load() {
        try (InputStream in = Files.newInputStream(file())) {
            Properties p = new Properties();
            p.load(in);
            return Boolean.parseBoolean(p.getProperty("hideOthers", "false").trim());
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Properties p = new Properties();
            p.setProperty("hideOthers", Boolean.toString(hideOthers));
            try (OutputStream out = Files.newOutputStream(file())) {
                p.store(out, "Char's Powers: cats");
            }
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't save the cat settings", e);
        }
    }

    public static boolean hidingOthers() {
        return hideOthers;
    }

    @SubscribeEvent
    public static void render(RenderLivingEvent.Pre<?, ?> e) {
        if (!hideOthers || !(e.getEntity() instanceof CompanionCat cat)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(cat.getOwnerUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("charcathide")
                .executes(c -> set(!hideOthers))
                .then(Commands.argument("state", StringArgumentType.word()).executes(c -> set("on".equalsIgnoreCase(StringArgumentType.getString(c, "state"))))));
    }

    private static int set(boolean hide) {
        hideOthers = hide;
        save();
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(hide ? "Other people's cats are hidden." : "You can see other people's cats."));
        return 1;
    }
}
