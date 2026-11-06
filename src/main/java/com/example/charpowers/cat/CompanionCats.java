package com.example.charpowers.cat;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cat.model.CatRules;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.OwnPowers;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Keeps each player's cat: makes it when the player wants one, takes it away when not. Off until chosen, and only for people who
// own one of Char's powers (they get one quiet hint, once). It costs nothing for anyone who does not use it.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class CompanionCats {
    private static final String ON = "charpowers.cat.on", HINTED = "charpowers.cat.hinted";
    private static final Map<UUID, CompanionCat> CATS = new HashMap<>();

    private CompanionCats() {}

    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static boolean switchedOn(Player p) {
        return data(p).getBoolean(ON);
    }

    public static void setOn(Player p, boolean on) {
        data(p).putBoolean(ON, on);
    }

    public static boolean ownsAPower(Player p) {
        for (OwnPower o : OwnPowers.all()) if (o.owns(p)) return true;
        return false;
    }

    public static CompanionCat catOf(UUID owner) {
        CompanionCat c = CATS.get(owner);
        return c != null && !c.isRemoved() ? c : null;
    }

    // Level-up: a meow and two hops.
    public static void cheer(ServerPlayer p) {
        CompanionCat c = catOf(p.getUUID());
        if (c != null) c.cheer();
    }

    // Looks at one player: makes or removes their cat, and gives the one-time hint.
    public static void look(ServerPlayer p) {
        boolean owns = ownsAPower(p);
        if (owns && !data(p).getBoolean(HINTED)) {
            data(p).putBoolean(HINTED, true);
            OwnPower.tell(p, "A little black cat could keep you company. Type /charcat if you would like one.");
        }
        boolean want = CatRules.wanted(switchedOn(p), owns, p.isAlive(), p.isSpectator());
        CompanionCat cat = catOf(p.getUUID());
        if (cat != null && cat.level() != p.level()) {
            cat.discard();
            cat = null;
        }
        if (!want) {
            if (cat != null) cat.discard();
            CATS.remove(p.getUUID());
            return;
        }
        if (cat == null && p.level() instanceof ServerLevel level) {
            CompanionCat made = new CompanionCat(CatEntities.CAT.get(), level);
            made.bindTo(p.getUUID());
            made.moveTo(p.getX() + 1, p.getY(), p.getZ(), p.getYRot(), 0f);
            level.addFreshEntity(made);
            CATS.put(p.getUUID(), made);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 40 != 9) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) look(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        CompanionCat c = CATS.remove(e.getEntity().getUUID());
        if (c != null) c.discard();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        CATS.clear();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("charcat")
                .executes(c -> toggle(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("state", StringArgumentType.word()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[] {"on", "off"}, b))
                        .executes(c -> toggle(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "state")))));
    }

    private static int toggle(ServerPlayer p, String state) {
        if (!ownsAPower(p)) {
            OwnPower.tell(p, "The cat only comes to people who have one of Char's powers.");
            return 0;
        }
        boolean on = state == null ? !switchedOn(p) : state.equalsIgnoreCase("on");
        setOn(p, on);
        look(p);
        OwnPower.tell(p, on ? "Your cat is here." : "Your cat curls up somewhere and goes away.");
        return 1;
    }
}
