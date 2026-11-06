package com.example.charpowers.timecontrol;

import com.example.charpowers.CharPowers;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

// /char_chunks: the chunks you hold with Let Time Flow (the list names this command). /char_chunks remove <number>.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class TimeCommand {
    private TimeCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("char_chunks")
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    List<String> chunks = TimeControl.POWER.chunks(p);
                    if (chunks.isEmpty()) c.getSource().sendSuccess(() -> Component.literal("You aren't holding any chunks."), false);
                    for (int i = 0; i < chunks.size(); i++) {
                        String[] parts = chunks.get(i).split("\\|");
                        String line = (i + 1) + ": " + parts[0] + " at x " + (Integer.parseInt(parts[1]) * 16) + ", z " + (Integer.parseInt(parts[2]) * 16);
                        c.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return chunks.size();
                })
                .then(Commands.literal("remove").then(Commands.argument("number", IntegerArgumentType.integer(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean done = TimeControl.POWER.removeChunk(p, IntegerArgumentType.getInteger(c, "number") - 1);
                    c.getSource().sendSuccess(() -> Component.literal(done ? "That chunk can rest now." : "No such number. See /char_chunks."), false);
                    return done ? 1 : 0;
                }))));
    }
}
