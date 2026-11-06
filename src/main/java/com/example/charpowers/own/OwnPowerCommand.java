package com.example.charpowers.own;

import com.example.charpowers.CharPowers;
import com.example.charpowers.mastery.Mastery;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

// For testing only (ops), so no power needs the grind:
// /charpower give|take|unlock|quest|trigger <power> <player>, /charpower mastery <power> <player> <level>. /charpower menu opens all of it as a screen.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class OwnPowerCommand {
    private OwnPowerCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("charpower").requires(s -> s.hasPermission(2))
                .then(Commands.literal("menu").executes(c -> {
                    OpMenu.open(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("give").then(action("Gave", OwnPower::give)))
                .then(Commands.literal("take").then(action("Took", (power, p) -> SuperpowerUtil.removeSuperpower(p, power.id))))
                .then(Commands.literal("unlock").then(action("Finished the unlock of", OwnPower::unlock)))
                .then(Commands.literal("quest").then(action("Finished the quest of", OwnPower::awaken)))
                .then(Commands.literal("trigger").then(action("Triggered", OwnPower::trigger)))
                .then(Commands.literal("mastery").then(powerArg().then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(OwnPowerCommand::mastery))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> powerArg() {
        return Commands.argument("power", StringArgumentType.word()).suggests((c, b) -> {
            List<String> names = new ArrayList<>();
            for (OwnPower power : OwnPowers.all()) names.add(power.id.getPath());
            return SharedSuggestionProvider.suggest(names, b);
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> action(String did, BiConsumer<OwnPower, ServerPlayer> what) {
        return powerArg().then(Commands.argument("player", EntityArgument.player()).executes(c -> {
            OwnPower power = power(c);
            if (power == null) return 0;
            ServerPlayer p = EntityArgument.getPlayer(c, "player");
            what.accept(power, p);
            c.getSource().sendSuccess(() -> Component.literal(did + " " + power.id.getPath() + ": " + p.getGameProfile().getName()), true);
            return 1;
        }));
    }

    private static OwnPower power(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "power");
        OwnPower power = OwnPowers.get(new ResourceLocation(CharPowers.MODID, name));
        if (power == null) c.getSource().sendFailure(Component.literal("No such power: " + name));
        return power;
    }

    private static int mastery(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        OwnPower power = power(c);
        if (power == null) return 0;
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        int level = IntegerArgumentType.getInteger(c, "level");
        Mastery.setLevel(p, power.id, level);
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " is now " + power.id.getPath() + " mastery " + level), true);
        return 1;
    }
}
