package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.mastery.Mastery;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

// For testing only (ops): clones themselves are run from the Clone key and the menus.
// /charclones unlock|reflection <player>, /charclones mastery set <player> <level>
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class CloneCommand {
    private CloneCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("charclones").requires(s -> s.hasPermission(2))
                .then(Commands.literal("mastery")
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100)).executes(CloneCommand::setMastery)))))
                .then(Commands.literal("reflection")
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                            ReflectionMob.stepOut(EntityArgument.getPlayer(c, "player"));
                            return 1;
                        })))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("player", EntityArgument.player()).executes(CloneCommand::unlock))));
    }

    private static int setMastery(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        int level = IntegerArgumentType.getInteger(c, "level");
        Mastery.setLevel(p, Mastery.CLONING, level);
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " is now Cloning mastery " + level), true);
        return 1;
    }

    private static int unlock(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        SuperpowerUtil.addSuperpower(p, Cloning.POWER);
        Mastery.sync(p);
        c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " now has Cloning"), true);
        return 1;
    }
}
