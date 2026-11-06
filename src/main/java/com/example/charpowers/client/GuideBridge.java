package com.example.charpowers.client;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.client.CloneClient;
import com.example.charpowers.hidden.HiddenIcons;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.model.Kind;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.OwnPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.ability.AbilityUtil;
import net.threetag.palladium.util.icon.ItemIcon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// What Char's Power Guide reads (by name, so neither mod needs the other to build).
// Only the hidden powers THIS player has collected: the others stay secret.
public final class GuideBridge {
    // The mod's name on the guide's home card. Change it here once the name is decided.
    public static String modName() { return CharPowers.DISPLAY_NAME; }

    // Each entry: { id, rank letter, is it a key power, name, description, icon } sorted by rank, then list number.
    public static List<Object[]> hiddenOwned() {
        List<PowerDef> defs = new ArrayList<>();
        for (String id : ClientHidden.owned()) HiddenPowers.def(id).ifPresent(defs::add);
        defs.sort(Comparator.comparing(PowerDef::rank).thenComparingInt(PowerDef::number));
        List<Object[]> out = new ArrayList<>();
        for (PowerDef d : defs) {
            out.add(new Object[]{d.id(), d.rank().name(), d.kind() == Kind.KEY, HiddenPowers.name(d.id()),
                    HiddenPowers.description(d.id()), HiddenIcons.stack(d.id())});
        }
        return out;
    }

    // Char's big powers, all of them (how to get one is what a guide is for). Each entry: { name, how to get, about,
    // abilities ("Name: what it does" lines), mastery, quest, awakening, do I own it, my mastery, awakened, icon }.
    public static List<Object[]> ownPowers() {
        Player me = Minecraft.getInstance().player;
        List<Object[]> out = new ArrayList<>();
        for (OwnPower power : OwnPowers.all()) {
            String key = "power.charpowers." + power.id.getPath();
            Integer level = CloneClient.masteryLevel(power.id.toString());
            out.add(new Object[]{I18n.get(key), I18n.get(key + ".how"), I18n.get(key + ".about"), I18n.get(key + ".abilities_text"),
                    I18n.get(key + ".mastery"), I18n.get(key + ".quest"), I18n.get(key + ".awakening"),
                    me != null && AbilityUtil.hasPower(me, power.id), level == null ? 1 : level,
                    CloneClient.masteryLevel(power.id + "#awakened") != null, iconOf(power)});
        }
        return out;
    }

    // The power's own icon from its Palladium file, so a drawn one shows here too.
    private static Object iconOf(OwnPower power) {
        Minecraft mc = Minecraft.getInstance();
        Power loaded = mc.level == null ? null : PowerManager.getInstance(mc.level).getPower(power.id);
        return loaded != null ? loaded.getIcon() : new ItemIcon(power.icon);
    }

    private GuideBridge() {}
}
