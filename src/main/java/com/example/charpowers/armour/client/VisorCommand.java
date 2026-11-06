package com.example.charpowers.armour.client;

import com.example.charpowers.CharPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// /suitvisor opens the visor's settings; /suitvisor minimal|standard|full picks a preset without the page.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class VisorCommand {
    private VisorCommand() {}

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent e) {
        var root = Commands.literal("suitvisor").executes(c -> {
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new VisorScreen()));
            return 1;
        });
        for (String preset : new String[] {"minimal", "standard", "full"}) {
            root.then(Commands.literal(preset).executes(c -> {
                VisorSettings.preset(preset);
                c.getSource().sendSuccess(() -> Component.literal("Suit visor: " + preset + "."), false);
                return 1;
            }));
        }
        e.getDispatcher().register(root);
    }
}
