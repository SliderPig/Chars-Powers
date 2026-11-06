package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.Stream;

// /hiddenpower give|take|giveall|takeall|list|check|set|summon : for testing (ops only), until Death Steal exists.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class HiddenCommand {
    private static final SuggestionProvider<CommandSourceStack> POWERS =
            (c, b) -> SharedSuggestionProvider.suggest(HiddenPowers.ids(), b);
    private static final SuggestionProvider<CommandSourceStack> POWERS_OR_RANDOM =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.concat(Stream.of("random"), HiddenPowers.ids().stream()), b);
    private static final SuggestionProvider<CommandSourceStack> POWERS_OR_NONE =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.concat(Stream.of("none"), HiddenPowers.ids().stream()), b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("hiddenpower").requires(s -> s.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("power", StringArgumentType.word()).suggests(POWERS).executes(HiddenCommand::give))))
                .then(Commands.literal("take").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("power", StringArgumentType.word()).suggests(POWERS).executes(HiddenCommand::take))))
                .then(Commands.literal("giveall").then(Commands.argument("player", EntityArgument.player()).executes(HiddenCommand::giveAll)))
                .then(Commands.literal("takeall").then(Commands.argument("player", EntityArgument.player()).executes(HiddenCommand::takeAll)))
                .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player()).executes(HiddenCommand::list)))
                .then(Commands.literal("check").executes(HiddenCommand::check))
                .then(Commands.literal("set").then(Commands.argument("power", StringArgumentType.word()).suggests(POWERS_OR_NONE)
                        .executes(HiddenCommand::set)))
                // /hiddenpower summon <mob> <power|random>: a new mob right in front of you, with that power
                .then(Commands.literal("summon").then(Commands.argument("mob", ResourceLocationArgument.id())
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(Commands.argument("power", StringArgumentType.word()).suggests(POWERS_OR_RANDOM)
                                .executes(HiddenCommand::summon)))));
    }

    private static String name(String id) { return HiddenPowers.name(id).getString(); }

    private static int summon(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ResourceLocation mobId = ResourceLocationArgument.getId(c, "mob");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
        if (type == null) return fail(c, "There's no mob called " + mobId);
        String id = StringArgumentType.getString(c, "power");
        if (id.equals("random")) {
            var all = HiddenPowers.all();
            id = all.get(p.getRandom().nextInt(all.size())).id();
        }
        if (HiddenPowers.def(id).isEmpty()) return fail(c, "There's no hidden power called " + id);
        // 3 blocks in front of you, on the ground you're standing on
        Vec3 front = p.position().add(p.getViewVector(1).multiply(1, 0, 1).normalize().scale(3));
        Entity made = type.spawn(p.serverLevel(), BlockPos.containing(front.x, p.getY(), front.z), MobSpawnType.COMMAND);
        if (!(made instanceof Mob m)) {
            if (made != null) made.discard();
            return fail(c, mobId + " isn't a mob that can have a power");
        }
        MobPowers.set(m, id);
        String power = id;
        c.getSource().sendSuccess(() -> Component.literal("Summoned a " + m.getType().getDescription().getString() + " with " + name(power)), true);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        String id = StringArgumentType.getString(c, "power");
        if (HiddenPowers.def(id).isEmpty()) return fail(c, "There's no hidden power called " + id);
        boolean added = HiddenGrant.give(p, id);
        c.getSource().sendSuccess(() -> Component.literal(added ? "Gave " + p.getGameProfile().getName() + " " + name(id)
                : p.getGameProfile().getName() + " already has " + name(id)), true);
        return added ? 1 : 0;
    }

    // Every hidden power there is, at once (for testing).
    private static int giveAll(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        int added = 0;
        for (String id : HiddenPowers.ids()) if (HiddenGrant.give(p, id)) added++;
        int n = added;
        c.getSource().sendSuccess(() -> Component.literal("Gave " + p.getGameProfile().getName() + " " + n + " hidden power(s) ("
                + HiddenPowers.ids().size() + " exist)"), true);
        return added;
    }

    // Clears every hidden power the player has.
    private static int takeAll(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        int had = PlayerHiddenData.get(p).owned().size();
        HiddenGrant.wipe(p);
        c.getSource().sendSuccess(() -> Component.literal("Took all " + had + " hidden power(s) from " + p.getGameProfile().getName()), true);
        return had;
    }

    private static int take(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        String id = StringArgumentType.getString(c, "power");
        boolean taken = HiddenGrant.take(p, id);
        c.getSource().sendSuccess(() -> Component.literal(taken ? "Took " + name(id) + " from " + p.getGameProfile().getName()
                : p.getGameProfile().getName() + " doesn't have " + id), true);
        return taken ? 1 : 0;
    }

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        var l = PlayerHiddenData.get(p);
        StringBuilder sb = new StringBuilder(p.getGameProfile().getName() + " has " + l.owned().size() + " hidden power(s): ");
        for (String id : l.owned()) sb.append(name(id)).append(l.equipped().contains(id) ? " [on]" : "").append(", ");
        String text = l.owned().isEmpty() ? p.getGameProfile().getName() + " has no hidden powers" : sb.substring(0, sb.length() - 2);
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return l.owned().size();
    }

    private static Mob lookedAtMob(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        LivingEntity t = Fx.lookTarget(p, 32);
        return t instanceof Mob m ? m : null;
    }

    private static int check(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Mob m = lookedAtMob(c);
        if (m == null) return fail(c, "Look at a mob first (up to 32 blocks)");
        String text = MobPowers.get(m)
                .map(id -> m.getType().getDescription().getString() + " has " + name(id) + " ("
                        + HiddenPowers.def(id).map(d -> d.rank().name()).orElse("?") + ")")
                .orElse(m.getType().getDescription().getString() + " has nothing");
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Mob m = lookedAtMob(c);
        if (m == null) return fail(c, "Look at a mob first (up to 32 blocks)");
        String id = StringArgumentType.getString(c, "power");
        if (id.equals("none")) {
            MobPowers.set(m, null);
            c.getSource().sendSuccess(() -> Component.literal("That " + m.getType().getDescription().getString() + " has no hidden power now"), true);
            return 1;
        }
        if (HiddenPowers.def(id).isEmpty()) return fail(c, "There's no hidden power called " + id);
        MobPowers.set(m, id);
        c.getSource().sendSuccess(() -> Component.literal("That " + m.getType().getDescription().getString() + " now has " + name(id)), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String why) {
        c.getSource().sendFailure(Component.literal(why));
        return 0;
    }

    private HiddenCommand() {}
}
