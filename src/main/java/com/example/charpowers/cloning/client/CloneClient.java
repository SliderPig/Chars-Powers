package com.example.charpowers.cloning.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneEntities;
import com.example.charpowers.cloning.DogPileMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.example.charpowers.cloning.CloneInfo;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// The parts of Cloning that run on the player's PC: clones show their owner's name and skin, their join, leave and
// death lines stay out of chat. Also remembers your mastery levels for the power menu's side panel.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class CloneClient {
    private static final Pattern HIDDEN_NAME = Pattern.compile("\\b[A-Za-z0-9_]{1,13}\\.c[1-5]\\b");
    private static final String MARK = ".c";
    private static final Map<String, Integer> MASTERY = new HashMap<>();

    private CloneClient() {}

    public static void setMastery(Map<String, Integer> levels) {
        com.example.charpowers.mastery.Mastery.setClientLevels(levels);
        MASTERY.clear();
        MASTERY.putAll(levels);
    }

    @Nullable
    public static Integer masteryLevel(String power) {
        return MASTERY.get(power);
    }

    public static void cloneList(boolean open, List<CloneInfo> clones) {
        Minecraft mc = Minecraft.getInstance();
        if (open && !(mc.screen instanceof CloneListScreen)) mc.setScreen(new CloneListScreen(null));
        if (mc.screen instanceof CloneListScreen screen) screen.update(clones);
    }

    public static void openMenu(int slot, String name, String job, float health, float maxHealth, int food, String chest) {
        Minecraft.getInstance().setScreen(new CloneScreen(slot, name, job, health, maxHealth, food, chest));
    }

    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat e) {
        Player p = e.getEntity();
        if (!p.level().isClientSide) return;
        String name = p.getGameProfile().getName();
        int mark = name.indexOf(MARK);
        if (mark < 0) return;
        e.setDisplayname(Component.literal(realName(name.substring(0, mark))));
    }

    // A clone with a nickname wears that over its head. Checked every time, so a new nickname shows straight away.
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent e) {
        if (e.getEntity() instanceof Player p && p.hasCustomName() && p.getGameProfile().getName().contains(MARK)) e.setContent(p.getCustomName());
    }

    // The owner's entry in this PC's player list, when this name is a clone's. The server doesn't always know a skin
    // (singleplayer doesn't, and neither does an offline-mode server), but every PC already knows its owner's.
    @Nullable
    public static PlayerInfo ownerOf(String name) {
        int mark = name.indexOf(MARK);
        ClientPacketListener net = Minecraft.getInstance().getConnection();
        if (mark < 0 || net == null) return null;
        String start = name.substring(0, mark);
        for (PlayerInfo info : net.getOnlinePlayers()) {
            String other = info.getProfile().getName();
            if (other.startsWith(start) && !other.contains(MARK)) return info;
        }
        return null;
    }

    // Long names are cut short in the clone's hidden name, so look for the real one among who's online.
    private static String realName(String start) {
        ClientPacketListener net = Minecraft.getInstance().getConnection();
        if (net != null) {
            for (PlayerInfo info : net.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name.startsWith(start) && !name.contains(MARK)) return name;
            }
        }
        return start;
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent.System e) {
        if (HIDDEN_NAME.matcher(e.getMessage().getString()).find()) e.setCanceled(true);
    }

    @Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(CloneEntities.DOG_PILE.get(), DogPileRenderer::new);
            @SuppressWarnings("unchecked")
            EntityType<DogPileMob> reflection = (EntityType<DogPileMob>) (EntityType<?>) CloneEntities.REFLECTION.get();
            e.registerEntityRenderer(reflection, DogPileRenderer::new);
            @SuppressWarnings("unchecked")
            EntityType<DogPileMob> revived = (EntityType<DogPileMob>) (EntityType<?>) CloneEntities.REVIVED.get();
            e.registerEntityRenderer(revived, DogPileRenderer::new);
        }
    }
}
