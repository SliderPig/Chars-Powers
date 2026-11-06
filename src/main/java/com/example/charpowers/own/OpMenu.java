package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.hidden.HiddenConfig;
import com.example.charpowers.mastery.Mastery;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.threetag.palladium.power.SuperpowerUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The op menu (/charpower menu): every power's on/off, mastery speed and numbers, the mod's general settings, and a player to give
// powers to. The PC only ever asks: everything checks here that whoever's asking really is an op.
public final class OpMenu {
    public static final String CHANNEL = "charpowers:op";
    private static final Map<UUID, UUID> PICKED = new HashMap<>();
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> GENERAL = new LinkedHashMap<>();
    private static final Map<String, String> LABELS = new HashMap<>();
    private static final Map<String, Double> STEPS = new HashMap<>();

    static {
        general("hidden", "Mobs have hidden powers", HiddenConfig.HIDDEN_POWERS_ENABLED, 1);
        general("hidden_mobs", "Hidden power: 1 in (a mob)", HiddenConfig.OTHER_ONE_IN, 5);
        general("hidden_animals", "Hidden power: 1 in (an animal)", HiddenConfig.ANIMAL_ONE_IN, 10);
        general("hidden_farm", "Hidden power: 1 in (a farm mob, 0 = never)", HiddenConfig.FARM_ONE_IN, 50);
        general("hidden_use", "Mobs use powers made for players", HiddenConfig.MOBS_USE_CARRIED, 1);
        general("hears", "Mob power messages a minute", HiddenConfig.PLAYER_HEARS_PER_MINUTE, 1);
        general("attack_scale", "Attack powers damage", HiddenConfig.ATTACK_DAMAGE_SCALE, 0.1);
        general("attack_break", "Attack powers break blocks", HiddenConfig.ATTACK_BREAKS_BLOCKS, 1);
        general("max_clones", "Most clones on the server", HiddenConfig.MAX_CLONES_ON_SERVER, 1);
        general("clone_skills", "Clones get skills", HiddenConfig.CLONES_GET_SKILLS, 1);
        general("clone_break", "Clones' powers break blocks", HiddenConfig.CLONES_BREAK_BLOCKS, 1);
    }

    private OpMenu() {}

    private static void general(String key, String label, ForgeConfigSpec.ConfigValue<?> value, double step) {
        GENERAL.put(key, value);
        LABELS.put(key, label);
        STEPS.put(key, step);
    }

    public static void open(ServerPlayer op) {
        send(op, true);
    }

    @Nullable
    private static ServerPlayer picked(ServerPlayer op) {
        ServerPlayer who = op.getServer().getPlayerList().getPlayer(PICKED.getOrDefault(op.getUUID(), op.getUUID()));
        return who != null ? who : op;
    }

    @Nullable
    private static OwnPower power(String path) {
        return OwnPowers.get(new ResourceLocation(CharPowers.MODID, path));
    }

    public static void action(ServerPlayer op, String action) {
        if (!op.hasPermissions(2)) return;
        String[] a = action.split(":");
        try {
            switch (a[0]) {
                case "flip" -> Setting.setEnabled(a[1], !Setting.enabled(new ResourceLocation(CharPowers.MODID, a[1])));
                case "rate" -> Setting.setMasteryRate(a[1], Setting.masteryRate(a[1]) + Double.parseDouble(a[2]));
                case "add" -> {
                    for (Setting s : Setting.of(a[1])) {
                        if (s.key.equals(a[2])) s.set(s.d() + Double.parseDouble(a[3]));
                    }
                }
                case "reset" -> Setting.reset(a[1]);
                case "general" -> general(a[1], a.length > 2 ? Double.parseDouble(a[2]) : 0);
                case "pick" -> {
                    List<ServerPlayer> all = new ArrayList<>(op.getServer().getPlayerList().getPlayers());
                    all.removeIf(p -> p.getTags().contains("charpowers.clone"));
                    int at = all.indexOf(picked(op));
                    PICKED.put(op.getUUID(), all.get(Math.floorMod(at + Integer.parseInt(a[1]), all.size())).getUUID());
                }
                default -> onPlayer(op, a);
            }
        } catch (RuntimeException e) {
            CharPowers.LOG.warn("Op menu couldn't do {}", action, e);
        }
        send(op, false);
    }

    private static void onPlayer(ServerPlayer op, String[] a) {
        OwnPower power = a.length > 1 ? power(a[1]) : null;
        ServerPlayer who = picked(op);
        if (power == null || who == null) return;
        switch (a[0]) {
            case "give" -> power.give(who);
            case "take" -> SuperpowerUtil.removeSuperpower(who, power.id);
            case "level" -> Mastery.setLevel(who, power.id, Math.max(1, Math.min(100, power.mastery(who) + Integer.parseInt(a[2]))));
            case "awaken" -> power.awaken(who);
            case "trigger" -> power.trigger(who);
            case "free" -> power.opButton(op);
            default -> {}
        }
    }

    @SuppressWarnings("unchecked")
    private static void general(String key, double delta) {
        ForgeConfigSpec.ConfigValue<?> value = GENERAL.get(key);
        if (value instanceof ForgeConfigSpec.BooleanValue b) b.set(!b.get());
        else if (value instanceof ForgeConfigSpec.IntValue n) n.set(Math.max(0, (int) Math.round(n.get() + delta)));
        else if (value instanceof ForgeConfigSpec.DoubleValue n) n.set(Math.max(0, Math.round((n.get() + delta) * 100) / 100.0));
        if (value != null) value.save();
    }

    private static void send(ServerPlayer op, boolean open) {
        ServerPlayer who = picked(op);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("open", open);
        tag.putString("player", who == null ? "" : who.getGameProfile().getName());
        tag.putInt("online", op.getServer().getPlayerList().getPlayerCount());
        ListTag general = new ListTag();
        for (Map.Entry<String, ForgeConfigSpec.ConfigValue<?>> e : GENERAL.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putString("key", e.getKey());
            row.putString("label", LABELS.get(e.getKey()));
            row.putDouble("step", STEPS.get(e.getKey()));
            Object v = e.getValue().get();
            if (v instanceof Boolean b) row.putBoolean("flag", b);
            else row.putDouble("value", ((Number) v).doubleValue());
            row.putBoolean("isFlag", v instanceof Boolean);
            general.add(row);
        }
        tag.put("general", general);
        ListTag powers = new ListTag();
        for (OwnPower power : OwnPowers.all()) {
            String path = power.id.getPath();
            CompoundTag row = new CompoundTag();
            row.putString("path", path);
            row.putBoolean("on", Setting.enabled(power.id));
            row.putDouble("rate", Setting.masteryRate(path));
            String info = power.opInfo(op.getServer());
            if (info != null) row.putString("info", info);
            String button = power.opButtonLabel();
            if (button != null) row.putString("button", button);
            if (who != null) {
                row.putBoolean("has", SuperpowerUtil.hasSuperpower(who, power.id));
                row.putInt("level", power.mastery(who));
                row.putBoolean("awakened", power.awakened(who));
            }
            ListTag numbers = new ListTag();
            for (Setting s : Setting.of(path)) {
                CompoundTag n = new CompoundTag();
                n.putString("key", s.key);
                n.putString("label", s.label);
                n.putDouble("value", s.d());
                n.putDouble("step", s.step);
                numbers.add(n);
            }
            row.put("numbers", numbers);
            powers.add(row);
        }
        tag.put("powers", powers);
        Net.toOwner(op, CHANNEL, tag);
    }
}
