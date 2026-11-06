package com.example.charpowers.deathsteal;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.deathsteal.model.StealRules;
import com.example.charpowers.hidden.HiddenGrant;
import com.example.charpowers.hidden.HiddenIcons;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.icons.PowerIconItem;
import com.example.charpowers.hidden.MobPowers;
import com.example.charpowers.hidden.PlayerHiddenData;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.Pocket;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.power.ability.AbilityInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// When a person or mob kills you, you take one of their powers. You wake up in the black room, and punch an icon you can't
// see. Nothing runs unless someone is in that room.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class DeathSteal extends OwnPower {
    public static final DeathSteal POWER = new DeathSteal();
    private static final String LAST_DAY = "lastDay", TAKEN = "takenFrom", STOLEN = "stolen", PENDING = "pending";
    private static final String KIND_POWER = "power", KIND_HIDDEN = "hidden", KIND_QUEST = "quest", KIND_DECOY = "decoy";

    private static final Map<UUID, StealRoom> ROOMS = new HashMap<>();

    private DeathSteal() {
        super(Mastery.DEATH_STEAL, Items.WITHER_ROSE);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "Something in you woke up. It's waiting for you to die."; }

    @Override
    public String awakenedMessage() { return "You took death from yourself. From now on, whoever killed you keeps what you take."; }

    // It awakens by itself: 1 in 1000 each Minecraft day.
    @Override
    public void watchSecond(ServerPlayer p) {
        long day = p.level().getDayTime() / 24000;
        if (data(p).getLong(LAST_DAY) == day) return;
        data(p).putLong(LAST_DAY, day);
        if (p.getRandom().nextInt(Setting.STEAL_DAILY_ONE_IN.i()) == 0) unlock(p);
    }

    // Using a power you stole counts as using this one.
    @Override
    public void everySecond(ServerPlayer p) {
        ListTag stolen = data(p).getList(STOLEN, Tag.TAG_STRING);
        if (stolen.isEmpty()) return;
        var handler = PowerManager.getPowerHandler(p).orElse(null);
        if (handler == null) return;
        for (Tag t : stolen) {
            ResourceLocation power = ResourceLocation.tryParse(t.getAsString());
            IPowerHolder holder = power == null ? null : handler.getPowerHolders().get(power);
            if (holder == null) continue;
            for (AbilityInstance a : holder.getAbilities().values()) {
                if (a.isEnabled()) {
                    Mastery.add(p, id, 1);
                    return;
                }
            }
        }
    }

    private static List<StealRoom.Option> optionsOf(ServerPlayer p, LivingEntity killer) {
        List<StealRoom.Option> out = new ArrayList<>();
        if (killer instanceof Mob m) {
            MobPowers.get(m).ifPresent(hid -> out.add(hidden(hid)));
        } else if (killer instanceof Player k) {
            for (Power power : SuperpowerUtil.getSuperpowers(k)) {
                ResourceLocation pid = power.getId();
                if (!StealRules.stealable(pid.toString()) || SuperpowerUtil.hasSuperpower(p, pid)) continue;
                ItemStack icon = PowerIconItem.of(power.getIcon());
                out.add(new StealRoom.Option(KIND_POWER, pid.toString(), icon, power.getName()));
            }
            for (String hid : PlayerHiddenData.get(k).owned()) {
                if (!PlayerHiddenData.get(p).owned().contains(hid)) out.add(hidden(hid));
            }
        }
        Collections.shuffle(out);
        return out.size() > StealRules.MAX_OPTIONS ? new ArrayList<>(out.subList(0, StealRules.MAX_OPTIONS)) : out;
    }

    private static StealRoom.Option hidden(String hid) {
        return new StealRoom.Option(KIND_HIDDEN, hid, HiddenIcons.stack(hid), HiddenPowers.name(hid));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !POWER.owns(p) || !(e.getSource().getEntity() instanceof LivingEntity killer)) return;
        CompoundTag d = POWER.data(p);
        if (killer == p) {
            if (POWER.questReady(p)) saveQuest(p);
            return;
        }
        if (!StealRules.canStealFrom(d.getCompound(TAKEN).getInt(killer.getUUID().toString()), POWER.mastery(p))) return;
        List<StealRoom.Option> options = optionsOf(p, killer);
        if (!options.isEmpty()) save(p, killer.getUUID(), options);
    }

    // What's waiting is saved on the player, so logging off in the room doesn't lose it.
    private static void save(ServerPlayer p, UUID killer, List<StealRoom.Option> options) {
        CompoundTag pending = new CompoundTag();
        pending.putUUID("killer", killer);
        ListTag list = new ListTag();
        for (StealRoom.Option o : options) {
            CompoundTag one = new CompoundTag();
            one.putString("kind", o.kind());
            one.putString("id", o.id());
            one.put("icon", o.icon().save(new CompoundTag()));
            one.putString("name", Component.Serializer.toJson(o.name()));
            list.add(one);
        }
        pending.put("options", list);
        POWER.data(p).put(PENDING, pending);
    }

    // The quest: die by your own hand, then punch the one icon that is Death Steal. The icons never show themselves, and
    // every power you've stolen is one more to choose from.
    private static void saveQuest(ServerPlayer p) {
        int stolen = POWER.data(p).getList(STOLEN, Tag.TAG_STRING).size();
        List<StealRoom.Option> options = new ArrayList<>();
        Component name = Component.literal("?");
        options.add(new StealRoom.Option(KIND_QUEST, "", questIcon(), name));
        for (int i = 0; i < Math.min(stolen, StealRules.MAX_OPTIONS - 1); i++) options.add(new StealRoom.Option(KIND_DECOY, "", questIcon(), name));
        Collections.shuffle(options);
        save(p, p.getUUID(), options);
    }

    private static ItemStack questIcon() {
        return PowerIconItem.of(new ResourceLocation(POWER.id.getNamespace(), "textures/icons/" + POWER.id.getPath() + ".png"));
    }

    private static List<StealRoom.Option> load(CompoundTag pending) {
        List<StealRoom.Option> out = new ArrayList<>();
        for (Tag t : pending.getList("options", Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) t;
            Component name = Component.Serializer.fromJson(one.getString("name"));
            out.add(new StealRoom.Option(one.getString("kind"), one.getString("id"), ItemStack.of(one.getCompound("icon")), name == null ? Component.literal("?") : name));
        }
        return out;
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !e.isEndConquered()) openIfWaiting(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.isAlive()) openIfWaiting(p);
    }

    private static void openIfWaiting(ServerPlayer p) {
        CompoundTag d = POWER.data(p);
        if (!d.contains(PENDING)) return;
        ServerLevel level = Pocket.level(p);
        if (level == null) return;
        StealRoom old = ROOMS.remove(p.getUUID());
        if (old != null) old.close();
        int mastery = POWER.mastery(p);
        boolean quest = d.getCompound(PENDING).getUUID("killer").equals(p.getUUID());
        ROOMS.put(p.getUUID(), new StealRoom(p, level, load(d.getCompound(PENDING)), quest ? 1 : mastery));
        PowerSounds.toPlayer(p, PowerSounds.DARK_SUCK, 1f, 1f);
        whisper(p, quest ? "One of these is you." : "Punch one.");
        CharPowers.LOG.info("[DeathSteal] {} is in the black room with {} to choose from", p.getGameProfile().getName(), ROOMS.get(p.getUUID()).options.size());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ROOMS.isEmpty() || e.getServer().getTickCount() % 2 != 0) return;
        Iterator<Map.Entry<UUID, StealRoom>> it = ROOMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, StealRoom> entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            StealRoom room = entry.getValue();
            if (p == null) {
                room.close();
                it.remove();
                continue;
            }
            // left some other way, by a command say: the room goes, and what was waiting opens next time
            if (!Pocket.in(p)) {
                room.close();
                it.remove();
                continue;
            }
            boolean quest = POWER.data(p).getCompound(PENDING).getUUID("killer").equals(p.getUUID());
            room.spin(quest ? 1 : POWER.mastery(p));
            int swung = room.swungAt(p);
            if (swung >= 0) {
                it.remove();
                choose(p, room, swung);
                continue;
            }
            // nobody stays for ever: if you won't choose, the room chooses
            if (p.level().getGameTime() - room.opened > Setting.STEAL_TIME_LIMIT.i()) {
                it.remove();
                choose(p, room, p.getRandom().nextInt(room.options.size()));
            }
        }
    }

    // First, and even if something else called the hit off (a Stop order, say): in here a punch is how you get out.
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onPunch(AttackEntityEvent e) {
        if (ROOMS.isEmpty() || !(e.getEntity() instanceof ServerPlayer p)) return;
        StealRoom room = ROOMS.get(p.getUUID());
        if (room == null) return;
        int index = room.indexOf(e.getTarget());
        if (index < 0) return;
        e.setCanceled(true);
        ROOMS.remove(p.getUUID());
        choose(p, room, index);
    }

    private static void choose(ServerPlayer p, StealRoom room, int index) {
        CompoundTag d = POWER.data(p);
        CompoundTag pending = d.getCompound(PENDING);
        UUID killer = pending.getUUID("killer");
        d.remove(PENDING);
        StealRoom.Option chosen = room.options.get(index);
        CharPowers.LOG.info("[DeathSteal] {} punched {}", p.getGameProfile().getName(), chosen.name().getString());
        room.close();
        PowerSounds.toPlayer(p, PowerSounds.SOUL_PUNCH, 1f, 1f);
        Pocket.leave(p);
        switch (chosen.kind()) {
            case KIND_QUEST -> POWER.awaken(p);
            case KIND_DECOY -> tell(p, "That wasn't you. Try dying again.");
            default -> {
                com.example.charpowers.anim.AnimNet.play(p, "reach_pull");
                steal(p, killer, chosen);
            }
        }
    }

    private static void steal(ServerPlayer p, UUID killerId, StealRoom.Option chosen) {
        boolean copy = POWER.awakened(p);
        Entity killer = find(p, killerId);
        if (KIND_HIDDEN.equals(chosen.kind())) {
            if (!copy && killer instanceof Mob m) MobPowers.set(m, null);
            if (!copy && killer instanceof ServerPlayer k) HiddenGrant.take(k, chosen.id());
            HiddenGrant.give(p, chosen.id());
        } else {
            ResourceLocation power = ResourceLocation.tryParse(chosen.id());
            if (power == null) return;
            if (!copy && killer instanceof ServerPlayer k) SuperpowerUtil.removeSuperpower(k, power);
            SuperpowerUtil.addSuperpower(p, power);
            ListTag stolen = POWER.data(p).getList(STOLEN, Tag.TAG_STRING);
            stolen.add(StringTag.valueOf(chosen.id()));
            POWER.data(p).put(STOLEN, stolen);
        }
        CompoundTag taken = POWER.data(p).getCompound(TAKEN);
        taken.putInt(killerId.toString(), taken.getInt(killerId.toString()) + 1);
        POWER.data(p).put(TAKEN, taken);
        Mastery.add(p, POWER.id, StealRules.POINTS_A_STEAL);
        tell(p, "You took " + chosen.name().getString() + ".");
        if (killer instanceof ServerPlayer k && !copy) tell(k, p.getGameProfile().getName() + " came back from the dead and took " + chosen.name().getString() + " from you.");
    }

    private static Entity find(ServerPlayer p, UUID id) {
        for (ServerLevel level : p.getServer().getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        StealRoom room = ROOMS.remove(e.getEntity().getUUID());
        if (room != null) room.close();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ROOMS.values().forEach(StealRoom::close);
        ROOMS.clear();
    }
}
