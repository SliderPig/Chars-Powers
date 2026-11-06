package com.example.charpowers.house;

import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.cloning.CloneEntity;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.ModSounds;
import com.example.charpowers.hidden.attack.Wreck;
import com.example.charpowers.house.model.HouseRules;
import com.example.charpowers.house.model.HouseTurn;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.Setting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Place 10,000 blocks and it finds you. Every block you place after that is mastery. Five houses by mastery, five more when you've
// built a real castle. One ten-minute cooldown for the whole power. Houses cost nothing and clear what's growing on the plot, but
// never overwrite anything built or anything holding things, and every protection on the server gets asked first.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class HouseBuilder extends OwnPower {
    public static final HouseBuilder POWER = new HouseBuilder();

    private static final String PLACED = "placed", SINCE = "sinceUnlock", COOLDOWN = "cooldownEnd", TOLD = "cooldownTold";
    private static final Map<UUID, CastleScan> SCANS = new HashMap<>();
    private static final int MAX_THUMB_BYTES = 800_000; // one message can only be so big
    private static boolean asking;

    private HouseBuilder() {
        super(Mastery.HOUSE_BUILDER, Items.BRICKS);
    }

    @Override
    public String unlockMessage() { return "Ten thousand blocks placed. Your hands know houses now: open House Builder and pick one."; }

    @Override
    public String masteryHundredMessage() { return "Mastery 100. One thing left: build a real castle, 50 stacks of your own blocks, walls and towers, then look at it and use Inspect Castle."; }

    @Override
    public String awakenedMessage() { return "That's a castle. Five finer houses are yours."; }

    // ---- counting

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (asking || !(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || p instanceof CloneEntity) return;
        if (!(e.getLevel() instanceof ServerLevel level) || !Setting.enabled(POWER.id)) return;
        POWER.counted(p, level, e.getPos());
    }

    private void counted(ServerPlayer p, ServerLevel level, BlockPos pos) {
        CompoundTag data = data(p);
        long placed = data.getLong(PLACED) + 1;
        data.putLong(PLACED, placed);
        if (!owns(p)) {
            if (placed >= Setting.HOUSE_UNLOCK.l()) unlock(p);
            return;
        }
        if (HiddenRuntime.switchedOff(p)) return;
        long since = data.getLong(SINCE);
        data.putLong(SINCE, since + 1);
        Mastery.add(p, id, HouseRules.masteryGain(since));
        if (!awakened(p) && mastery(p) >= HouseRules.REMEMBER_FROM_LEVEL) HouseData.Placed.of(level).add(p.getUUID(), pos);
    }

    @Override
    public void unlock(ServerPlayer p) {
        super.unlock(p);
        if (owns(p)) p.level().playSound(null, p.blockPosition(), ModSounds.HOUSE_DONE.get(), SoundSource.PLAYERS, 0.6f, 1f);
    }

    // ---- the keys

    @Override
    public void onKey(ServerPlayer p, String ability) {
        if ("build_house".equals(ability)) sendPicker(p);
        else if ("inspect_castle".equals(ability)) inspect(p);
    }

    public void sendPicker(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("open", true);
        tag.putInt("level", mastery(p));
        tag.putBoolean("awakened", awakened(p));
        tag.putLong("cooldown", cooldownLeft(p));
        ListTag list = new ListTag();
        int sent = 0;
        for (HousePlan plan : HouseCatalog.all(server)) {
            CompoundTag h = new CompoundTag();
            h.putString("id", plan.id);
            h.putString("name", plan.name);
            h.putInt("w", plan.w);
            h.putInt("h", plan.h);
            h.putInt("d", plan.d);
            h.putInt("blocks", plan.cells.size());
            h.putBoolean("custom", plan.custom);
            h.putBoolean("awk", plan.awakened);
            String lock = HouseRules.lockReason(plan.level, plan.awakened, mastery(p), awakened(p));
            if (lock != null) h.putString("lock", lock);
            if (plan.thumb != null && sent + plan.thumb.length < MAX_THUMB_BYTES) {
                h.putByteArray("thumb", plan.thumb);
                sent += plan.thumb.length;
            }
            list.add(h);
        }
        tag.put("houses", list);
        Net.toOwner(p, id.toString(), tag);
    }

    @Override
    public void onClientAction(ServerPlayer p, String action) {
        if (!action.startsWith("build:") || !owns(p) || HiddenRuntime.switchedOff(p)) return;
        String[] parts = action.split(":");
        if (parts.length != 6) return;
        try {
            String refused = tryBuild(p, parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
            if (refused != null) tell(p, refused + ".");
        } catch (NumberFormatException ignored) {
            // anyone can send anything
        }
    }

    // Starts the house, or says why not. Nothing has happened and the cooldown hasn't started unless this returns null.
    @Nullable
    public String tryBuild(ServerPlayer p, String houseId, int cx, int around, int cz, int turns) {
        MinecraftServer server = p.getServer();
        long left = cooldownLeft(p);
        if (left > 0) return "Your hands need a rest: next house in " + HouseRules.clock(left);
        HousePlan plan = HouseCatalog.get(server, houseId);
        if (plan == null) return "I don't know that house";
        String lock = HouseRules.lockReason(plan.level, plan.awakened, mastery(p), awakened(p));
        if (lock != null) return plan.name + " needs " + lock;
        if (p.position().distanceToSqr(cx, p.getY(), cz) > HouseRules.REACH * HouseRules.REACH || Math.abs(around - p.getY()) > HouseRules.REACH) return "That's too far away";
        ServerLevel level = p.serverLevel();
        HouseSite.Result site = HouseSite.check(level, cx, cz, around, plan.w, plan.h, plan.d, turns);
        if (!site.ok()) return site.why();
        String claimed = claimCheck(level, p, plan, cx, site.y(), cz, turns);
        if (claimed != null) return claimed;
        HouseData.Jobs jobs = HouseData.Jobs.of(server);
        jobs.jobs.add(new BuildJob(plan.id, level.dimension(), cx, site.y(), cz, turns, p.getUUID(), p.getGameProfile().getName()));
        jobs.setDirty();
        data(p).putLong(COOLDOWN, now(server) + Setting.HOUSE_COOLDOWN.l());
        data(p).putBoolean(TOLD, false);
        CompoundTag built = new CompoundTag();
        built.putBoolean("built", true);
        Net.toOwner(p, id.toString(), built);
        whisper(p, plan.name + " is going up...");
        return null;
    }

    // Asks every claim and protection on the server about the whole plot.
    @Nullable
    private String claimCheck(ServerLevel level, ServerPlayer p, HousePlan plan, int cx, int y, int cz, int turns) {
        int[] rect = HouseTurn.rect(plan.w, plan.d, cx, cz, turns);
        asking = true;
        try {
            for (int x = rect[0]; x <= rect[2]; x += 2) {
                for (int z = rect[1]; z <= rect[3]; z += 2) {
                    if (!Wreck.mayTouch(level, new BlockPos(x, y, z), p)) return "That plot is protected at " + x + " " + z;
                }
                if (!Wreck.mayTouch(level, new BlockPos(x, y, rect[3]), p)) return "That plot is protected at " + x + " " + rect[3];
            }
            for (int z = rect[1]; z <= rect[3]; z += 2) {
                if (!Wreck.mayTouch(level, new BlockPos(rect[2], y, z), p)) return "That plot is protected at " + rect[2] + " " + z;
            }
            int[][] probes = {{rect[0], rect[1]}, {rect[2], rect[1]}, {rect[0], rect[3]}, {rect[2], rect[3]}, {cx, cz}};
            Wreck.BREAKING = true;
            for (int[] at : probes) {
                BlockPos pos = new BlockPos(at[0], y + 1, at[1]);
                if (MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos), level.getBlockState(pos.below()), p))) {
                    return "Something here won't let you build";
                }
            }
        } finally {
            Wreck.BREAKING = false;
            asking = false;
        }
        return null;
    }

    // ---- the castle quest

    private void inspect(ServerPlayer p) {
        if (awakened(p)) {
            whisper(p, "You've already proved it. The finer houses are yours.");
            return;
        }
        if (mastery(p) < 100) {
            whisper(p, "Inspect Castle opens at mastery 100.");
            return;
        }
        if (SCANS.containsKey(p.getUUID())) return;
        HitResult hit = p.pick(64, 1f, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            whisper(p, "Look at a block of your castle.");
            return;
        }
        whisper(p, "Walking round your castle...");
        startScan(p, block.getBlockPos());
    }

    public void startScan(ServerPlayer p, BlockPos start) {
        SCANS.put(p.getUUID(), new CastleScan(p.serverLevel(), p.getUUID(), start));
    }

    private void scanDone(ServerPlayer p, CastleScan scan) {
        List<String> missing = scan.verdict();
        if (missing.isEmpty()) {
            for (ServerLevel level : p.getServer().getAllLevels()) HouseData.Placed.of(level).forget(p.getUUID());
            tell(p, "A castle: " + scan.total() + " blocks, " + scan.yours() + " of them yours, walls, towers and a way in.");
            awaken(p);
            return;
        }
        tell(p, "That's not a castle yet (" + scan.total() + " blocks" + (scan.capped ? ", and I stopped counting" : "") + "):");
        for (String line : missing) tell(p, " - " + line);
    }

    // ---- every tick: houses going up and castles being walked round

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        if (server == null) return;
        HouseData.Jobs jobs = HouseData.Jobs.of(server);
        if (!jobs.jobs.isEmpty()) {
            for (Iterator<BuildJob> it = jobs.jobs.iterator(); it.hasNext(); ) {
                BuildJob job = it.next();
                boolean over;
                try {
                    over = job.tick(server);
                } catch (RuntimeException ex) {
                    CharPowers.LOG.error("Building {} failed", job.houseId, ex);
                    over = true;
                }
                if (over) it.remove();
            }
            jobs.setDirty();
        }
        if (!SCANS.isEmpty()) {
            for (Iterator<Map.Entry<UUID, CastleScan>> it = SCANS.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, CastleScan> entry = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
                if (p == null) {
                    it.remove();
                } else if (entry.getValue().tick()) {
                    it.remove();
                    POWER.scanDone(p, entry.getValue());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        SCANS.clear();
        HouseCatalog.reset();
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        HouseCatalog.scan(e.getServer(), true, null);
    }

    // Ops: /charpower houses reload reads the config folder again, /charpower houses list says what's there.
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("charpower").requires(s -> s.hasPermission(2))
                .then(Commands.literal("houses")
                        .then(Commands.literal("reload").executes(c -> {
                            CommandSourceStack source = c.getSource();
                            source.sendSuccess(() -> Component.literal("Reading config/charpowers/houses..."), false);
                            HouseCatalog.scan(source.getServer(), true, text -> source.sendSuccess(() -> Component.literal(text), true));
                            return 1;
                        }))
                        .then(Commands.literal("list").executes(c -> {
                            CommandSourceStack source = c.getSource();
                            for (HousePlan plan : HouseCatalog.all(source.getServer())) {
                                if (!plan.custom) continue;
                                source.sendSuccess(() -> Component.literal(plan.name + ": " + plan.cells.size() + " blocks, " + plan.w + " x " + plan.d + " wide, " + plan.h + " high, mastery " + plan.level), false);
                            }
                            source.sendSuccess(() -> Component.literal(HouseCatalog.customCount() + " own house file(s)."), false);
                            for (String trouble : HouseCatalog.problems()) source.sendSuccess(() -> Component.literal("Problem: " + trouble), false);
                            return 1;
                        }))));
    }

    // Tests only.
    public void finishJobs(MinecraftServer server) {
        HouseData.Jobs jobs = HouseData.Jobs.of(server);
        for (BuildJob job : jobs.jobs) job.finishNow(server);
        jobs.jobs.clear();
    }

    public boolean scanning(ServerPlayer p) {
        return SCANS.containsKey(p.getUUID());
    }

    // ---- the one cooldown

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    public long cooldownLeft(ServerPlayer p) {
        long end = data(p).getLong(COOLDOWN);
        return end <= 0 ? 0 : Math.max(0, end - now(p.getServer()));
    }

    public void resetCooldown(ServerPlayer p) {
        data(p).remove(COOLDOWN);
        data(p).putBoolean(TOLD, true);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        CompoundTag data = data(p);
        if (data.getLong(COOLDOWN) > 0 && !data.getBoolean(TOLD) && cooldownLeft(p) == 0) {
            data.putBoolean(TOLD, true);
            whisper(p, "Your hands are rested: you can build another house.");
        }
    }

    @Override
    public void trigger(ServerPlayer p) {
        resetCooldown(p);
        whisper(p, "House Builder's cooldown has been reset.");
    }

    @Override
    public String opInfo(MinecraftServer server) {
        return HouseData.Jobs.of(server).jobs.size() + " house(s) going up, " + HouseCatalog.customCount() + " own house file(s) from config/charpowers/houses.";
    }

    @Override
    public String opButtonLabel() { return "Reset all cooldowns"; }

    @Override
    public void opButton(ServerPlayer op) {
        for (ServerPlayer p : op.getServer().getPlayerList().getPlayers()) resetCooldown(p);
        whisper(op, "Everyone online can build a house now.");
    }

    public long placedBlocks(ServerPlayer p) {
        return data(p).getLong(PLACED);
    }

    public static boolean isRemembered(ServerLevel level, UUID who, BlockPos pos) {
        return HouseData.Placed.of(level).has(who, pos);
    }

    // Tests and things that place for the player: count one placed block as if by hand.
    public void countForTest(ServerPlayer p, Level level, BlockPos pos) {
        counted(p, (ServerLevel) level, pos);
    }
}
