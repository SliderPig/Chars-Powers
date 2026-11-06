package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.cloning.jobs.CloneJob;
import com.example.charpowers.cloning.jobs.Jobs;
import com.example.charpowers.cloning.model.CloneRules;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Where the game's events reach Cloning: joining and leaving, deaths, and clicking a clone or a chest.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class CloneEvents {
    private static final long ESCAPED_DEATH_COOLDOWN = 1200;
    private static final float ESCAPED_DEATH_HEALTH = 2f;
    private static final Map<UUID, Integer> MARKING = new HashMap<>();

    private CloneEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof CloneEntity) return;
        Mastery.sync(p);
        // joining sends the whole player list with everyone listed, so take the clones back off this player's TAB
        CloneManager.hideFromTab(p.getServer(), CloneManager.all(), p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof CloneEntity) return;
        MARKING.remove(p.getUUID());
        CloneManager.hostLeft(p);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (ServerPlayer p : new ArrayList<>(e.getServer().getPlayerList().getPlayers())) {
            if (!(p instanceof CloneEntity)) CloneManager.hostLeft(p);
        }
    }

    // A mining clone leaves stone on the floor.
    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent e) {
        if (e.getEntity() instanceof CloneEntity c && c.job.refuses(e.getItem().getItem())) e.setCanceled(true);
    }

    // Escaped Death first (a totem has already had its chance by now). Void and /kill aren't saved.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeathFirst(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer host) || host instanceof CloneEntity || e.isCanceled()) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (!AbilityUtil.isEnabled(host, Cloning.POWER, "escaped_death")) return;
        long now = host.level().getGameTime();
        if (now < CloneData.escapedReadyAt(host)) return;
        CloneData.setEscapedReadyAt(host, now + ESCAPED_DEATH_COOLDOWN);
        e.setCanceled(true);
        host.setHealth(ESCAPED_DEATH_HEALTH);
        DogPile.standIn(host, e.getSource());
        host.sendSystemMessage(Component.literal("That wasn't you. (Escaped Death: 1 minute cooldown)").withStyle(ChatFormatting.LIGHT_PURPLE));
        host.level().playSound(null, host.blockPosition(), PowerSounds.MIRROR_SHIMMER.get(), SoundSource.PLAYERS, 1f, 0.8f);
    }

    // A rogue clone fights until you or it dies.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeathLast(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer host && !(host instanceof CloneEntity) && !e.isCanceled()) CloneManager.endRogues(host);
    }

    // Crouch + right-click your own clone: its menu.
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() || !(e.getTarget() instanceof CloneEntity c)) return;
        if (!c.hostId.equals(p.getUUID()) || c.rogue) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        GlobalPos chest = CloneData.chest(p, c.slot);
        String chestText = chest == null ? "none" : chest.pos().toShortString();
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new Net.OpenCloneMenu(c.slot, CloneData.label(p, c.slot), c.job.id(), c.getHealth(), c.getMaxHealth(), c.getFoodData().getFoodLevel(), chestText));
    }

    // The next chest you click after "Mark chest" becomes that clone's.
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Integer slot = MARKING.get(p.getUUID());
        if (slot == null || !(p.level().getBlockEntity(e.getPos()) instanceof ChestBlockEntity)) return;
        MARKING.remove(p.getUUID());
        markChest(p, slot, GlobalPos.of(p.level().dimension(), e.getPos()));
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    static void startMarking(ServerPlayer host, int slot) {
        MARKING.put(host.getUUID(), slot);
        host.sendSystemMessage(Component.literal("Right-click the chest for " + CloneData.label(host, slot) + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    static void markChest(ServerPlayer host, int slot, GlobalPos where) {
        CloneData.setChest(host, slot, where);
        host.sendSystemMessage(Component.literal(CloneData.label(host, slot) + " will use the chest at " + where.pos().toShortString() + ".").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // From the menu on the player's PC. Everything is checked again here.
    public static void order(ServerPlayer host, int slot, String action) {
        CloneEntity c = CloneManager.clone(host.getUUID(), slot);
        boolean realSlot = slot >= 0 && slot < CloneRules.SLOTS;
        if (realSlot && "pick".equals(action)) {
            CloneData.setPicked(host, slot, !CloneData.picked(host, slot));
            Net.sendClones(host, false);
            return;
        }
        if (realSlot && action.startsWith("name:")) {
            CloneData.setNickname(host, slot, CloneRules.cleanNickname(action.substring(5)));
            if (c != null) CloneManager.showNickname(c, host);
            Net.sendClones(host, false);
            return;
        }
        if (c == null) {
            if ("mark".equals(action) && realSlot) startMarking(host, slot);
            return;
        }
        if ("mark".equals(action)) {
            startMarking(host, slot);
        } else if ("dismiss".equals(action)) {
            if (!c.rogue) CloneManager.dismiss(c);
        } else if (action.startsWith("job:")) {
            CloneJob next = Jobs.make(action.substring(4), host.blockPosition());
            if (next != null && !c.rogue) c.setJob(next);
        }
    }
}
