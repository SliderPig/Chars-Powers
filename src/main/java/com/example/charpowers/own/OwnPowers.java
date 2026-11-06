package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.HiddenSlotAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.SuperpowerUtil;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladiumcore.registry.RegistrySupplier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The list of Char's own powers, and the one timer they share: once a second per player, it finds which of them that
// player has and only runs those, so a power nobody owns costs nothing.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class OwnPowers {
    private static final Map<ResourceLocation, OwnPower> BY_ID = new LinkedHashMap<>();
    private static final List<OwnPower> WATCHERS = new ArrayList<>();

    // "charpowers:key" in a power file: a key ability that tells its OwnPower it was pressed.
    public static final RegistrySupplier<Ability> KEY = HiddenSlotAbility.ABILITIES.register("key", KeyAbility::new);

    private OwnPowers() {}

    public static void init() {
        add(new com.example.charpowers.cloning.CloningPower());
        add(com.example.charpowers.teacher.Teacher.POWER);
        add(com.example.charpowers.translation.Translation.POWER);
        add(com.example.charpowers.futuresight.FutureSight.POWER);
        add(com.example.charpowers.authority.SuperiorAuthority.POWER);
        add(com.example.charpowers.miner.TheMiner.POWER);
        add(com.example.charpowers.life.LifeEnjoyer.POWER);
        add(com.example.charpowers.vocal.VocalExpression.POWER);
        add(com.example.charpowers.timecontrol.TimeControl.POWER);
        add(com.example.charpowers.necromancy.Necromancy.POWER);
        add(com.example.charpowers.mobcontrol.MobControl.POWER);
        add(com.example.charpowers.deathsteal.DeathSteal.POWER);
        add(com.example.charpowers.armour.InvincibleArmour.POWER);
        add(com.example.charpowers.lantern.GreenLantern.POWER);
        add(com.example.charpowers.book.BookKeeping.POWER);
        add(com.example.charpowers.game.GamePlayer.POWER);
        add(com.example.charpowers.house.HouseBuilder.POWER);
    }

    private static void add(OwnPower power) {
        BY_ID.put(power.id, power);
        if (power.watches()) WATCHERS.add(power);
    }

    public static OwnPower get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Iterable<OwnPower> all() {
        return BY_ID.values();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 0 || p instanceof CloneEntity) return;
        if (!HiddenRuntime.switchedOff(p)) {
            for (ResourceLocation id : new ArrayList<>(SuperpowerUtil.getSuperpowerIds(p))) {
                OwnPower power = BY_ID.get(id);
                if (power != null) power.everySecond(p);
            }
        }
        for (OwnPower power : WATCHERS) {
            if (!power.owns(p)) power.watchSecond(p);
        }
    }

    // The power's own line when its mastery reaches 100.
    public static void reachedHundred(ServerPlayer p, ResourceLocation id) {
        OwnPower power = BY_ID.get(id);
        String text = power == null ? null : power.masteryHundredMessage();
        if (text != null) power.announce(p, text);
    }

    public static class KeyAbility extends Ability {
        @Override
        public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
            if (!enabled || !(entity instanceof ServerPlayer p) || p instanceof CloneEntity || HiddenRuntime.switchedOff(p)) return;
            OwnPower power = BY_ID.get(holder.getPower().getId());
            if (power == null) return;
            com.example.charpowers.anim.AnimTriggers.beforeKey();
            power.onKey(p, entry.getConfiguration().getId());
            com.example.charpowers.anim.AnimTriggers.afterKey(p, power.id, entry.getConfiguration().getId());
        }

        // Only matters for keys you hold down.
        @Override
        public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
            if (!(entity instanceof ServerPlayer p) || p instanceof CloneEntity) return;
            OwnPower power = BY_ID.get(holder.getPower().getId());
            if (power == null) return;
            com.example.charpowers.anim.AnimTriggers.beforeKey();
            power.onKeyUp(p, entry.getConfiguration().getId());
            com.example.charpowers.anim.AnimTriggers.afterRelease(p, power.id, entry.getConfiguration().getId());
        }
    }
}
