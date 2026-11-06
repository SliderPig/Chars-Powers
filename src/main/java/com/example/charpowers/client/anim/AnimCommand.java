package com.example.charpowers.client.anim;

import com.example.charpowers.CharPowers;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// /charanim <name> tries a pose on your own model, on your own screen. Nobody else sees it and nothing else happens.
@Mod.EventBusSubscriber(modid = CharPowers.MODID, value = Dist.CLIENT)
public final class AnimCommand {
    private AnimCommand() {}

    @SubscribeEvent
    public static void onCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("charanim")
                .then(Commands.literal("on").executes(c -> toggle(c, true)))
                .then(Commands.literal("off").executes(c -> toggle(c, false)))
                .then(Commands.literal("stop").executes(AnimCommand::stop))
                .then(Commands.literal("list").executes(AnimCommand::list))
                .then(Commands.literal("reload").executes(AnimCommand::reload))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(PlayerAnims.names(), b))
                        .executes(c -> play(c, -1))
                        .then(Commands.argument("tick", DoubleArgumentType.doubleArg(0))
                                .executes(c -> play(c, DoubleArgumentType.getDouble(c, "tick"))))));
    }

    private static void say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private static int toggle(CommandContext<CommandSourceStack> c, boolean on) {
        AnimSettings.set(on);
        if (!on) PlayerAnims.clearAll();
        say(c, "Player animations " + (on ? "on." : "off."));
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> c) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) PlayerAnims.clear(p.getId());
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        say(c, String.join(", ", PlayerAnims.names()));
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        String problem = PlayerAnims.reload();
        say(c, problem == null ? "Poses reloaded: " + PlayerAnims.names().size() + "." : "Couldn't read the poses: " + problem);
        return 1;
    }

    private static int play(CommandContext<CommandSourceStack> c, double tick) {
        String name = StringArgumentType.getString(c, "name");
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        boolean ok = tick >= 0 ? PlayerAnims.freeze(p.getId(), name, tick) : PlayerAnims.play(p.getId(), name);
        if (!ok) c.getSource().sendFailure(Component.literal(AnimSettings.on() ? "No pose called " + name + ". /charanim list" : "Player animations are off. /charanim on"));
        return ok ? 1 : 0;
    }
}
