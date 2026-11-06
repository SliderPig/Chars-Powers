package com.example.charpowers;

import com.example.charpowers.hidden.HiddenConfig;
import com.example.charpowers.hidden.HiddenSlotAbility;
import com.example.charpowers.hidden.f.FPowers;
import com.example.charpowers.hidden.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// Char's Powers: my own power pack. This file just switches every part on.
@Mod(CharPowers.MODID)
public class CharPowers {
    public static final String MODID = "charpowers";
    // the final name isn't picked yet; change it here (and in mods.toml) when it is
    public static final String DISPLAY_NAME = "Char's Powers";
    public static final Logger LOG = LogUtils.getLogger();

    public CharPowers() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HiddenConfig.SPEC, "charpowers-common.toml");
        ModSounds.register(modBus);
        com.example.charpowers.icons.PowerIconItem.register(modBus);
        com.example.charpowers.sound.PowerSounds.register(modBus);
        com.example.charpowers.life.WellRested.register(modBus);
        com.example.charpowers.vocal.Words.register(modBus);
        com.example.charpowers.armour.SuitItems.register(modBus);
        com.example.charpowers.lantern.LanternEntities.register(modBus);
        com.example.charpowers.game.GameEntities.register(modBus);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> com.example.charpowers.client.anim.PoseAnimation::register);
        com.example.charpowers.game.mob.GateMobs.register(modBus);
        com.example.charpowers.items.CharItems.register(modBus);
        com.example.charpowers.game.ShadowArmy.register(modBus);
        com.example.charpowers.cat.CatEntities.register(modBus);
        // Palladium's registry helper needs to know our mod's event bus first, or the game crashes at start-up
        net.threetag.palladiumcore.forge.PalladiumCoreForge.registerModEventBus(MODID, modBus);
        com.example.charpowers.cloning.Cloning.init();
        com.example.charpowers.own.OwnPowers.init();
        HiddenSlotAbility.ABILITIES.register();
        com.example.charpowers.mastery.MasteryCondition.CONDITIONS.register();
        com.example.charpowers.cloning.CloneEntities.register(modBus);
        FPowers.registerAll();
        com.example.charpowers.hidden.ranks.RankPowers.registerAll();
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Net::register));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(com.example.charpowers.anim.AnimNet::register));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(com.example.charpowers.life.ultimine.UltimineServer::register));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(com.example.charpowers.mobcontrol.PetNet::register));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(com.example.charpowers.orders.OrderNet::register));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(com.example.charpowers.game.ShadowNet::register));
    }
}
