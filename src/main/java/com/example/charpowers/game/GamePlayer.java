package com.example.charpowers.game;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.Net;
import com.example.charpowers.game.model.GameRules;
import com.example.charpowers.game.model.GatePlan;
import com.example.charpowers.game.model.GateRules;
import com.example.charpowers.game.model.QuestBook;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.own.Pocket;
import com.example.charpowers.sound.PowerSounds;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A screen only you can see: quests give points, points buy stats and skills. Once awakened, special quests pay coins for a shop.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class GamePlayer extends OwnPower {
    public static final GamePlayer POWER = new GamePlayer();
    private static final Path SHOP_FILE = FMLPaths.CONFIGDIR.get().resolve("charpowers/game_shop.txt");
    private static final String POINTS = "points", COINS = "coins", STATS = "stats", OWNED = "owned", ON = "on", LOOK = "normalLook", DAY = "day", DAILY = "daily", DAILY_DONE = "dailyDone",
            STORY = "story", STORY_COUNT = "storyCount", STORY2 = "story2", STORY2_COUNT = "story2Count", SPECIAL = "special", SPECIAL_DONE = "specialDone", ALTAR = "altar", GATE = "gate", GATE_DIM = "gateDim", GATE_FINAL = "gateFinal", LEAVE_IN = "leaveIn", GATES = "gates", IN_GATE = "inGate", NEXT_GATE = "nextGate", OPEN_WAIT = "openWait", TRACK = "track", STORAGE = "storage", HOLO_STORAGE = "container.charpowers.storage";

    private record Skill(String id, String name, String about, int cost) {}

    private static final List<Skill> SKILLS = List.of(
            new Skill("dash", "Dash", "A key: burst forward", 30),
            new Skill("double_jump", "Double Jump", "Press jump again in the air", 40),
            new Skill("shadow_step", "Shadow Step", "A key: blink to where you look", 90),
            new Skill("bloodlust", "Bloodlust", "A key: everything near you freezes in fear", 110),
            new Skill("auto_pickup", "Auto Pickup", "Nearby items come to you", 20),
            new Skill("night_eyes", "Night Eyes", "See in the dark", 30),
            new Skill("soft_landing", "Soft Landing", "Falls hurt half as much", 30),
            new Skill("second_wind", "Second Wind", "Heal a bit when nearly dead", 50),
            new Skill("quick_hands", "Quick Hands", "Mine and swing faster", 45),
            new Skill("deep_breath", "Deep Breath", "Breathe underwater", 40),
            new Skill("fire_walker", "Fire Walker", "Fire and lava don't hurt", 70),
            new Skill("iron_skin", "Iron Skin", "Take 15% less damage", 80),
            new Skill("life_steal", "Life Steal", "Heal 10% of the damage you deal", 90),
            new Skill("lucky_strike", "Lucky Strike", "1 in 5 hits does half again as much", 80),
            new Skill("stealth", "Stealth", "Invisible while you crouch", 60));

    private static final Attribute[] ATTRIBUTES = {Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.LUCK};
    private static final AttributeModifier.Operation[] OPERATIONS = {AttributeModifier.Operation.ADDITION, AttributeModifier.Operation.MULTIPLY_BASE, AttributeModifier.Operation.ADDITION, AttributeModifier.Operation.ADDITION, AttributeModifier.Operation.ADDITION};
    private static final UUID[] STAT_IDS = {UUID.fromString("5a7c1d10-90b1-4c1e-8a11-000000000001"), UUID.fromString("5a7c1d10-90b1-4c1e-8a11-000000000002"), UUID.fromString("5a7c1d10-90b1-4c1e-8a11-000000000003"),
            UUID.fromString("5a7c1d10-90b1-4c1e-8a11-000000000004"), UUID.fromString("5a7c1d10-90b1-4c1e-8a11-000000000005")};
    private static final int SECOND_WIND_GAP = 60, ROOM_HALF = 8, STORAGE_SLOTS = 54, MAX_GATES = 3, ALTAR_Z = 125, FINAL_Z = 150;
    private static final DustParticleOptions GATE_LIGHT = new DustParticleOptions(new Vector3f(0.25f, 0.75f, 1f), 0.8f);

    private final Map<UUID, BlockPos> inDungeon = new HashMap<>();
    private final Map<UUID, Vec3> lastSpot = new HashMap<>();
    private final Map<UUID, GameBossMob> bosses = new HashMap<>();
    private final Map<UUID, Integer> windWait = new HashMap<>();
    private final Map<UUID, GateRun> runs = new HashMap<>(); // by the host who opened the gate
    private final Map<UUID, GateRun> memberOf = new HashMap<>(); // everyone inside a run, friends too
    private final Map<UUID, double[]> deathReturn = new HashMap<>();
    private final Map<UUID, String> deathReturnDim = new HashMap<>();
    private int runClock;

    private GamePlayer() {
        super(Mastery.GAME_PLAYER, Items.EXPERIENCE_BOTTLE);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "Welcome, Player. A screen opens only you can see. Press H."; }

    @Override
    public String awakenedMessage() { return "Final boss cleared. The player shop is open: special quests now pay coins."; }

    @Override
    protected void announce(ServerPlayer p, String text) {
        note(p, text);
    }

    // A small hologram at the top of your screen, only you see it.
    public void note(ServerPlayer p, String text) {
        CompoundTag tag = new CompoundTag();
        tag.putString("note", text);
        Net.toOwner(p, id.toString(), tag);
    }

    // ---- how you get it: walk into a dungeon, 1 in 1000 times a gate opens that only you can see. Everything behind it is yours alone.

    private static BlockPos nearSpawner(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition();
        for (int cx = (at.getX() - 8) >> 4; cx <= (at.getX() + 8) >> 4; cx++) {
            for (int cz = (at.getZ() - 8) >> 4; cz <= (at.getZ() + 8) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (e.getValue() instanceof SpawnerBlockEntity && e.getKey().distSqr(at) <= 64) return e.getKey();
                }
            }
        }
        return null;
    }

    @Override
    public void watchSecond(ServerPlayer p) {
        CompoundTag d = data(p);
        if (d.contains(STATS)) clearStats(p);
        GameOwner world = GameOwner.of(p.getServer());
        if (world.owner != null) {
            if (!world.owner.equals(p.getUUID())) return; // someone else is the Player, so this costs nobody anything
            world.set(null); // it was them, and they've lost it
        }
        if (Pocket.in(p) && d.contains(ALTAR)) {
            sitOnAltar(p, d);
            tickExit(p);
            return;
        }
        if (d.contains(GATE)) {
            tickGate(p, d);
            return;
        }
        BlockPos spawner = nearSpawner(p);
        if (spawner == null) {
            inDungeon.remove(p.getUUID());
            return;
        }
        if (spawner.equals(inDungeon.put(p.getUUID(), spawner))) return;
        if (Setting.enabled(id) && p.getRandom().nextInt(Setting.GAME_ROOM_CHANCE.i()) == 0) openGate(p, spawner.above(), false);
    }

    // On the altar, crouching: it takes your life a heart at a time until half a heart is left, then you're the Player.
    private void sitOnAltar(ServerPlayer p, CompoundTag d) {
        BlockPos altar = BlockPos.of(d.getLong(ALTAR));
        if (!p.isShiftKeyDown() || !p.blockPosition().below().equals(altar)) return;
        if (p.getHealth() > 1.01f) {
            p.setHealth(Math.max(1f, p.getHealth() - 2f));
            p.playNotifySound(SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS, 0.5f, 0.6f);
            return;
        }
        d.remove(ALTAR);
        d.remove(GATE);
        unlock(p);
        p.setHealth(Math.max(p.getHealth(), 6f));
        Pocket.leave(p);
    }

    private void openGate(ServerPlayer p, BlockPos at, boolean finalRoom) {
        CompoundTag d = data(p);
        d.putLong(GATE, at.asLong());
        d.putString(GATE_DIM, p.level().dimension().location().toString());
        d.putBoolean(GATE_FINAL, finalRoom);
        p.playNotifySound(PowerSounds.GATE_OPEN.get(), SoundSource.PLAYERS, 0.8f, 1f);
        note(p, finalRoom ? "A secret gate has opened. Final boss ahead. Only you can see it." : "A gate has opened nearby. Only you can see it.");
    }

    // A ring in the air, drawn on your screen alone. Walk into it.
    private void tickGate(ServerPlayer p, CompoundTag d) {
        BlockPos gate = BlockPos.of(d.getLong(GATE));
        if (Pocket.in(p) || !p.level().dimension().location().toString().equals(d.getString(GATE_DIM))) return;
        if (gate.distSqr(p.blockPosition()) < 48 * 48) drawRing(p, Vec3.atBottomCenterOf(gate), 1.4f, 32);
        if (p.position().distanceToSqr(gate.getX() + 0.5, gate.getY() + 0.5, gate.getZ() + 0.5) <= 4) enterRoom(p, d);
    }

    // It always faces you, so it's a ring from wherever you stand, and only your game is told about it.
    void drawRing(ServerPlayer p, Vec3 at, float size, int points) {
        ServerLevel level = p.serverLevel();
        Vec3 toYou = p.position().subtract(at).multiply(1, 0, 1);
        Vec3 side = toYou.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : new Vec3(-toYou.z, 0, toYou.x).normalize();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            Vec3 spot = at.add(side.scale(Math.cos(angle) * size)).add(0, size - 0.2 + Math.sin(angle) * size, 0);
            level.sendParticles(p, GATE_LIGHT, true, spot.x, spot.y, spot.z, 3, 0.04, 0.04, 0.04, 0);
        }
    }

    // ---- gates: they open near you as you explore, and you can tear one open yourself

    // At most a few at once: the oldest closes. The one you're tracking stays the one you're tracking.
    private static ListTag roomForAGate(CompoundTag d) {
        ListTag gates = d.getList(GATES, Tag.TAG_COMPOUND);
        if (gates.size() < MAX_GATES) return gates;
        gates.remove(0);
        if (d.contains(TRACK)) {
            if (d.getInt(TRACK) == 0) d.remove(TRACK);
            else d.putInt(TRACK, d.getInt(TRACK) - 1);
        }
        return gates;
    }

    private void growGates(ServerPlayer p, CompoundTag d) {
        long now = p.level().getGameTime();
        if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD || peaceful(p) || (d.contains(NEXT_GATE) && now < d.getLong(NEXT_GATE))) return;
        d.putLong(NEXT_GATE, now + Setting.GAME_GATE_GAP.l());
        ListTag gates = roomForAGate(d);
        ServerLevel level = p.serverLevel();
        for (int tries = 0; tries < 6; tries++) {
            double angle = p.getRandom().nextDouble() * Math.PI * 2, far = 24 + p.getRandom().nextInt(72);
            int x = (int) (p.getX() + Math.cos(angle) * far), z = (int) (p.getZ() + Math.sin(angle) * far);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            CompoundTag gate = new CompoundTag();
            gate.putLong("pos", new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z).asLong());
            gate.putInt("rank", GateRules.clampRank(p.getRandom().nextInt(GateRules.topRank(mastery(p)) + 2)));
            boolean sealed = gate.getInt("rank") >= 2 && p.getRandom().nextInt(8) == 0; // a sealed gate: optional, harder, pays double, no way out
            if (sealed) gate.putBoolean("sealed", true);
            gate.putString("theme", GatePlan.themeFor(gate.getInt("rank"), new java.util.Random(p.getRandom().nextLong()), sealed).name());
            gates.add(gate);
            d.put(GATES, gates);
            note(p, "A " + (sealed ? "sealed " : "") + gateLabel(gate) + " gate has opened nearby. " + (sealed ? "A sealed gate cannot be left until its boss is dead. " : "") + "See GATES on your Status Window.");
            return;
        }
    }

    // Draws the ones near you, points you at the one you're tracking, and takes you in when you walk into one.
    private void tickGates(ServerPlayer p, CompoundTag d) {
        ListTag gates = d.getList(GATES, Tag.TAG_COMPOUND);
        if (gates.isEmpty() || Pocket.in(p) || p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        int tracked = d.contains(TRACK) ? d.getInt(TRACK) : -1;
        for (int i = 0; i < gates.size(); i++) {
            CompoundTag gate = gates.getCompound(i);
            BlockPos at = BlockPos.of(gate.getLong("pos"));
            double far = Math.sqrt(at.distSqr(p.blockPosition()));
            if (far < 64) drawRing(p, Vec3.atBottomCenterOf(at), 1.6f, 32);
            if (i == tracked) {
                p.displayClientMessage(Component.literal("Rank " + GateRules.RANKS[gate.getInt("rank")] + " gate: " + (int) far + " blocks " + GateRules.compass(at.getX() - p.getX(), at.getZ() - p.getZ())).withStyle(ChatFormatting.AQUA), true);
                for (int y = 0; y < 24; y += 2) p.serverLevel().sendParticles(p, GATE_LIGHT, true, at.getX() + 0.5, at.getY() + y, at.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0);
            }
            if (far <= 2.5) {
                gates.remove(i);
                d.put(GATES, gates);
                if (i == tracked) d.remove(TRACK);
                else if (i < tracked) d.putInt(TRACK, tracked - 1);
                enterGate(p, gate.getInt("rank"), gate.getBoolean("sealed"), themeOf(gate));
                return;
            }
        }
    }

    // Standing on the ground there, so you can walk into it even if you opened it in mid-air. With a wall in the way, it's where you are.
    private static BlockPos groundNear(ServerPlayer p, double x, double z) {
        ServerLevel level = p.serverLevel();
        BlockPos at = BlockPos.containing(x, p.getY(), z);
        for (int up = 0; up < 4 && !level.getBlockState(at).getCollisionShape(level, at).isEmpty(); up++) at = at.above();
        if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) return p.blockPosition();
        for (int down = 0; down < 64 && level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty(); down++) at = at.below();
        return at;
    }

    // On command: a gate right in front of you, of any rank your mastery allows. It takes a while to be able to do it again.
    private void openGateNow(ServerPlayer p, CompoundTag d, int rank) {
        long now = p.level().getGameTime();
        if (rank > GateRules.topRank(mastery(p))) return;
        if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD || Pocket.in(p)) {
            note(p, "Gates only open in the Overworld.");
            return;
        }
        if (peaceful(p)) {
            note(p, "Nothing lives in gates on Peaceful.");
            return;
        }
        if (d.contains(OPEN_WAIT) && now < d.getLong(OPEN_WAIT)) {
            note(p, "You can open another gate in " + (d.getLong(OPEN_WAIT) - now) / 20 + " seconds.");
            return;
        }
        d.putLong(OPEN_WAIT, now + Setting.GAME_OPEN_GAP.l());
        ListTag gates = roomForAGate(d);
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        CompoundTag gate = new CompoundTag();
        gate.putLong("pos", groundNear(p, p.getX() + look.x * 5, p.getZ() + look.z * 5).asLong());
        gate.putInt("rank", rank);
        gate.putString("theme", GatePlan.themeFor(rank, new java.util.Random(p.getRandom().nextLong()), false).name());
        gates.add(gate);
        d.put(GATES, gates);
        d.putInt(TRACK, gates.size() - 1);
        p.playNotifySound(PowerSounds.GATE_OPEN.get(), SoundSource.PLAYERS, 0.8f, 1.1f);
        note(p, "A Rank " + GateRules.RANKS[rank] + " gate tears open in front of you.");
    }

    private static boolean peaceful(ServerPlayer p) {
        return p.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL;
    }

    // "[Rank B: Demon Castle]"
    static String gateLabel(CompoundTag gate) {
        GatePlan.Theme t = themeOf(gate);
        return "[Rank " + GateRules.RANKS[GateRules.clampRank(gate.getInt("rank"))] + (t == null ? "" : ": " + GatePlan.title(t)) + "]";
    }

    static GatePlan.Theme themeOf(CompoundTag gate) {
        try {
            return gate.contains("theme") ? GatePlan.Theme.valueOf(gate.getString("theme")) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    void enterGate(ServerPlayer p, int rank, boolean sealed) {
        enterGate(p, rank, sealed, null);
    }

    void enterGate(ServerPlayer p, int rank, boolean sealed, GatePlan.Theme theme) {
        ServerLevel level = Pocket.level(p);
        if (level == null || peaceful(p)) return;
        GateRun run = new GateRun(p, level, rank, sealed, theme);
        runs.put(p.getUUID(), run);
        memberOf.put(p.getUUID(), run);
        data(p).putBoolean(IN_GATE, true);
        Vec3 in = run.entrance();
        p.setYRot(-90f);
        Pocket.enter(p, level, BlockPos.containing(in));
        p.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.6f);
        int friends = 0;
        for (ServerPlayer f : GateParty.takeAccepted(p)) {
            joinRun(run, f);
            friends++;
        }
        note(p, (sealed ? "SEALED gate. " : "") + "Rank " + GateRules.RANKS[rank] + " " + run.themeName() + ". " + run.rooms.size() + " rooms"
                + (friends > 0 ? ", " + friends + " friend" + (friends > 1 ? "s" : "") + " with you" : "")
                + ". Clear each room to open the next door. " + (sealed ? "There is no way out until the boss is dead." : "Crouch in the ring behind you to run."));
    }

    // For tests: one second of gate time, and a jump to the start of a room.
    public void advanceGates() {
        for (int i = 0; i < 20; i++) tickRuns();
    }

    public void gateJumpToRoom(ServerPlayer p, int room) {
        GateRun run = memberOf.get(p.getUUID());
        if (run == null) return;
        run.buildAll();
        Vec3 at = run.startOf(room);
        p.teleportTo(run.level, at.x, at.y, at.z, -90f, 0f);
    }

    GateRun runOf(UUID id) {
        return memberOf.get(id);
    }

    // A friend goes in (together with the host, or into the raid already running).
    void joinRun(GateRun run, ServerPlayer p) {
        memberOf.put(p.getUUID(), run);
        run.join(p);
        if (owns(p)) data(p).putBoolean(IN_GATE, true);
        noteAll(p, "You are in the gate. Rank " + GateRules.RANKS[run.rank] + ". " + (run.sealed ? "No way out until the boss is dead." : ""));
    }

    // Walks out of the gate you are in, if you are allowed to.
    void leaveRun(ServerPlayer p) {
        GateRun run = memberOf.get(p.getUUID());
        if (run == null) return;
        if (run.sealed) {
            noteAll(p, "A sealed gate cannot be left until the boss is dead.");
            return;
        }
        memberOf.remove(p.getUUID());
        run.leave(p.getUUID(), true);
    }

    // The hologram note if you are the Player, a short line over the hotbar if you are a friend.
    void noteAll(ServerPlayer p, String text) {
        if (owns(p)) note(p, text);
        else p.displayClientMessage(Component.literal(text), true);
    }

    // Once a tick for building and traps, once a second for goals. A run ends when its last person is gone.
    private void tickRuns() {
        if (runs.isEmpty()) return;
        boolean second = ++runClock % 20 == 0;
        for (java.util.Iterator<GateRun> it = runs.values().iterator(); it.hasNext(); ) {
            GateRun run = it.next();
            run.tick();
            if (!second) continue;
            boolean over = run.second();
            memberOf.entrySet().removeIf(e -> e.getValue() == run && (over || !run.has(e.getKey())));
            if (over) it.remove();
        }
    }

    void gateCleared(ServerPlayer p) {
        GateRun run = memberOf.get(p.getUUID());
        if (run != null) gateCleared(run);
    }

    // Everyone who is still in gets a share of the loot; only a Player earns points and coins (double in a sealed gate).
    void gateCleared(GateRun run) {
        int times = run.sealed ? 2 : 1;
        for (ServerPlayer p : run.players()) {
            StringBuilder got = new StringBuilder();
            for (com.example.charpowers.game.model.GatePlan.Item l : com.example.charpowers.game.model.GatePlan.chestLoot(run.rank + (run.sealed ? 1 : 0), new java.util.Random(p.getRandom().nextLong()), 3 + times)) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(l.id()));
                if (item == null) continue;
                Fx.give(p, new ItemStack(item, l.count()));
                got.append(got.length() == 0 ? "" : ", ").append(l.count()).append(' ').append(new ItemStack(item).getHoverName().getString());
            }
            p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1f);
            if (!owns(p)) {
                p.sendSystemMessage(Component.literal("Gate cleared! Loot: " + got).withStyle(ChatFormatting.LIGHT_PURPLE));
                continue;
            }
            CompoundTag d = data(p);
            int points = (int) Math.round(GateRules.points(run.rank) * times * Setting.GAME_POINTS.d() / 100), coins = GateRules.coins(run.rank) * times;
            d.putInt(POINTS, d.getInt(POINTS) + points);
            d.putInt(COINS, d.getInt(COINS) + coins);
            Mastery.add(p, id, GameRules.QUEST_MASTERY * (run.rank + 1L) * times);
            String extra = "";
            if (p.getRandom().nextInt(100) < GateRules.weaponChance(run.rank) + (run.sealed ? 15 : 0)) {
                SystemWeapon.Kind[] all = SystemWeapon.Kind.values();
                SystemWeapon.Kind kind = all[p.getRandom().nextInt(Math.min(all.length, run.rank + 2))];
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(CharPowers.MODID, kind.id));
                if (item != null) {
                    Fx.give(p, new ItemStack(item));
                    extra = " You found the " + kind.title + "!";
                }
            }
            note(p, "Gate cleared. +" + points + " points, +" + coins + " coins." + extra);
            p.sendSystemMessage(Component.literal("Gate loot: " + got).withStyle(ChatFormatting.LIGHT_PURPLE));
            send(p, false);
        }
    }

    // Rooms are in the pocket dimension, each player's far from anyone's, so nobody else can ever walk in or hear a thing.
    private void enterRoom(ServerPlayer p, CompoundTag d) {
        ServerLevel level = Pocket.level(p);
        if (level == null) return;
        boolean finalRoom = d.getBoolean(GATE_FINAL);
        BlockPos middle = Pocket.roomOf(p.getUUID()).offset(0, 0, finalRoom ? FINAL_Z : ALTAR_Z);
        Pocket.buildRoom(level, middle, ROOM_HALF, 6, (finalRoom ? Blocks.OBSIDIAN : Blocks.DEEPSLATE_TILES).defaultBlockState(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) level.setBlock(middle.offset(sx * 4, 6, sz * 4), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        }
        p.setYRot(180f); // facing into the room
        Pocket.enter(p, level, middle.offset(0, 0, ROOM_HALF - 2));
        if (finalRoom) {
            bosses.put(p.getUUID(), GameBossMob.spawn(p, level, Vec3.atBottomCenterOf(middle.offset(0, 0, -3))));
            note(p, "FINAL BOSS.");
        } else {
            level.setBlock(middle, Blocks.LAPIS_BLOCK.defaultBlockState(), 2);
            d.putLong(ALTAR, middle.asLong());
            note(p, "Crouch on the altar and stay there. It takes you down to half a heart.");
        }
    }

    // Behind where you came in, a way back out if you're not ready.
    private void tickExit(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos middle = Pocket.roomOf(p.getUUID());
        BlockPos room = middle.offset(0, 0, data(p).getBoolean(GATE_FINAL) ? FINAL_Z : ALTAR_Z);
        BlockPos out = room.offset(0, 0, ROOM_HALF - 1);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12;
            level.sendParticles(p, GATE_LIGHT, true, out.getX() + 0.5 + Math.cos(angle) * 0.9, out.getY() + 1 + Math.sin(angle) * 0.9, out.getZ() + 0.5, 1, 0.02, 0.02, 0.02, 0);
        }
        if (p.position().distanceToSqr(out.getX() + 0.5, out.getY() + 0.5, out.getZ() + 0.5) <= 1.2) Pocket.leave(p);
    }

    // For testing: /charpower trigger game_player <player>. No power yet = the gate opens now. With it = 100 points and fresh quests, or at mastery 100 the boss's gate.
    @Override
    public void trigger(ServerPlayer p) {
        CompoundTag d = data(p);
        BlockPos ahead = p.blockPosition().offset((int) Math.round(p.getLookAngle().x * 4), 0, (int) Math.round(p.getLookAngle().z * 4));
        if (!owns(p)) {
            openGate(p, ahead, false);
        } else if (questReady(p)) {
            d.putInt(STORY, QuestBook.STORY.size());
            openGate(p, ahead, true);
        } else {
            d.putInt(POINTS, d.getInt(POINTS) + 100);
            d.remove(DAY);
            rollDay(p, d);
            note(p, "100 points and new daily quests.");
        }
    }

    @Override
    public String opInfo(MinecraftServer server) {
        var owner = GameOwner.of(server).owner;
        if (owner == null) return "There's no Player in this world yet.";
        return "The Player: " + server.getProfileCache().get(owner).map(GameProfile::getName).orElse(owner.toString());
    }

    @Override
    public String opButtonLabel() {
        return "FREE THE PLAYER SLOT";
    }

    @Override
    public void opButton(ServerPlayer op) {
        GameOwner.of(op.getServer()).set(null);
    }

    public GameBossMob bossOf(Player p) {
        return bosses.get(p.getUUID());
    }

    void bossBeaten(ServerPlayer p) {
        bosses.remove(p.getUUID());
        CompoundTag d = data(p);
        d.remove(GATE);
        d.putInt(LEAVE_IN, 3);
        awaken(p);
        send(p, false);
    }

    // ---- every second, for owners

    @Override
    public void everySecond(ServerPlayer p) {
        CompoundTag d = data(p);
        GameOwner world = GameOwner.of(p.getServer());
        if (world.owner == null) world.set(p.getUUID());
        rollDay(p, d);
        Mastery.add(p, id, 1);
        applyStats(p, d);
        Vec3 now = p.position();
        Vec3 was = lastSpot.put(p.getUUID(), now);
        if (was == null) send(p, false);
        else {
            double moved = Math.hypot(now.x - was.x, now.z - was.z);
            if (moved >= 1 && moved < 20) count(p, "walk", "", (int) moved);
        }
        if (on(d, "auto_pickup")) {
            for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(5))) {
                item.setNoPickUpDelay();
                item.playerTouch(p);
            }
        }
        if (on(d, "night_eyes")) {
            MobEffectInstance vision = p.getEffect(MobEffects.NIGHT_VISION);
            if (vision == null || vision.getDuration() < 220) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
        }
        if (on(d, "second_wind")) {
            int wait = windWait.getOrDefault(p.getUUID(), 0);
            if (wait > 0) windWait.put(p.getUUID(), wait - 1);
            else if (p.getHealth() <= 6f) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false, true));
                windWait.put(p.getUUID(), SECOND_WIND_GAP);
                note(p, "Second Wind.");
            }
        }
        if (!memberOf.containsKey(p.getUUID())) { // while in a gate, GateRun is driven from the server tick, for everyone in it
            // you logged off or the server restarted while you were in one: it's gone, so you can't be left inside
            if (d.getBoolean(IN_GATE)) {
                d.remove(IN_GATE);
                if (Pocket.in(p)) {
                    note(p, "The gate closed while you were away.");
                    Pocket.leave(p);
                }
            }
            growGates(p, d);
            tickGates(p, d);
        }
        passiveSkills(p, d);
        // after the boss, a moment to take it in, then back to the world
        if (d.contains(LEAVE_IN)) {
            int left = d.getInt(LEAVE_IN) - 1;
            if (left > 0) d.putInt(LEAVE_IN, left);
            else {
                d.remove(LEAVE_IN);
                if (Pocket.in(p)) Pocket.leave(p);
            }
        } else if (Pocket.in(p) && bosses.containsKey(p.getUUID())) {
            tickExit(p);
        }
        // the last quest: story finished, mastery 100, the next dungeon you walk into has a gate
        if (questReady(p) && d.getInt(STORY) >= QuestBook.STORY.size()) {
            if (d.contains(GATE)) tickGate(p, d);
            else {
                BlockPos spawner = nearSpawner(p);
                if (spawner != null) openGate(p, spawner.above(), true);
            }
        }
    }

    private static void keep(ServerPlayer p, net.minecraft.world.effect.MobEffect effect, int amplifier) {
        MobEffectInstance now = p.getEffect(effect);
        if (now == null || now.getDuration() < 60) p.addEffect(new MobEffectInstance(effect, 100, amplifier, true, false, false));
    }

    private void passiveSkills(ServerPlayer p, CompoundTag d) {
        if (on(d, "quick_hands")) keep(p, MobEffects.DIG_SPEED, 0);
        if (on(d, "deep_breath")) keep(p, MobEffects.WATER_BREATHING, 0);
        if (on(d, "fire_walker")) keep(p, MobEffects.FIRE_RESISTANCE, 0);
        if (on(d, "stealth") && p.isShiftKeyDown()) keep(p, MobEffects.INVISIBILITY, 0);
    }

    // ---- stats

    private static int[] stats(CompoundTag d) {
        int[] saved = d.getIntArray(STATS);
        return saved.length == GameRules.STATS.length ? saved : new int[GameRules.STATS.length];
    }

    private static void applyStats(ServerPlayer p, CompoundTag d) {
        int[] points = stats(d);
        for (int i = 0; i < points.length; i++) {
            AttributeInstance a = p.getAttribute(ATTRIBUTES[i]);
            if (a == null) continue;
            double want = GameRules.statEffect(i, points[i]);
            AttributeModifier have = a.getModifier(STAT_IDS[i]);
            if (have == null && want == 0) continue;
            if (have != null && have.getAmount() == want) continue;
            if (have != null) a.removeModifier(STAT_IDS[i]);
            if (want != 0) a.addTransientModifier(new AttributeModifier(STAT_IDS[i], "charpowers game player", want, OPERATIONS[i]));
        }
    }

    private static void clearStats(ServerPlayer p) {
        for (int i = 0; i < ATTRIBUTES.length; i++) {
            AttributeInstance a = p.getAttribute(ATTRIBUTES[i]);
            if (a != null && a.getModifier(STAT_IDS[i]) != null) a.removeModifier(STAT_IDS[i]);
        }
    }

    // ---- quests

    private void rollDay(ServerPlayer p, CompoundTag d) {
        long day = p.level().getDayTime() / 24000;
        if (d.contains(DAY) && d.getLong(DAY) == day) return;
        boolean first = !d.contains(DAY);
        d.putLong(DAY, day);
        d.putIntArray(DAILY, new int[GameRules.dailies(100)]);
        d.putIntArray(DAILY_DONE, new int[GameRules.dailies(100)]);
        d.putInt(SPECIAL, 0);
        d.putBoolean(SPECIAL_DONE, false);
        if (!first) note(p, "New daily quests.");
    }

    private List<QuestBook.Quest> today(ServerPlayer p, CompoundTag d) {
        return QuestBook.dailies(seed(p, d), GameRules.dailies(mastery(p)));
    }

    private static long seed(ServerPlayer p, CompoundTag d) {
        return p.getUUID().getLeastSignificantBits() * 31 + d.getLong(DAY);
    }

    void count(ServerPlayer p, String kind, String what, int n) {
        CompoundTag d = data(p);
        rollDay(p, d);
        boolean changed = false;
        List<QuestBook.Quest> quests = today(p, d);
        int[] counts = d.getIntArray(DAILY), done = d.getIntArray(DAILY_DONE);
        for (int i = 0; i < quests.size() && i < counts.length; i++) {
            QuestBook.Quest q = quests.get(i);
            if (done[i] != 0 || !q.matches(kind, what)) continue;
            counts[i] += n;
            changed = true;
            if (counts[i] >= q.count()) {
                done[i] = 1;
                pay(p, d, q);
            }
        }
        if (changed) {
            d.putIntArray(DAILY, counts);
            d.putIntArray(DAILY_DONE, done);
        }
        changed |= chain(p, d, QuestBook.STORY, STORY, STORY_COUNT, kind, what, n);
        if (GameRules.secondChain(mastery(p))) changed |= chain(p, d, QuestBook.SECOND_STORY, STORY2, STORY2_COUNT, kind, what, n);
        if (awakened(p) && !d.getBoolean(SPECIAL_DONE)) {
            QuestBook.Quest q = QuestBook.special(seed(p, d));
            if (q.matches(kind, what)) {
                changed = true;
                d.putInt(SPECIAL, d.getInt(SPECIAL) + n);
                if (d.getInt(SPECIAL) >= q.count()) {
                    d.putBoolean(SPECIAL_DONE, true);
                    d.putInt(COINS, d.getInt(COINS) + q.points());
                    note(p, "" + q.text() + ": done. +" + q.points() + " coins.");
                    p.playNotifySound(PowerSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 0.6f, 1f);
                }
            }
        }
        if (changed && kind.equals("walk")) return;
        if (changed) send(p, false);
    }

    private boolean chain(ServerPlayer p, CompoundTag d, List<QuestBook.Quest> chain, String at, String have, String kind, String what, int n) {
        int index = d.getInt(at);
        if (index >= chain.size() || !chain.get(index).matches(kind, what)) return false;
        d.putInt(have, d.getInt(have) + n);
        if (d.getInt(have) >= chain.get(index).count()) {
            pay(p, d, chain.get(index));
            d.putInt(at, index + 1);
            d.putInt(have, 0);
            if (chain == QuestBook.STORY && index + 1 == chain.size()) {
                note(p, mastery(p) >= 100 ? "Story complete. The next dungeon you walk into holds a secret room." : "Story complete. At mastery 100, a secret room waits in a dungeon.");
            }
        }
        return true;
    }

    private void pay(ServerPlayer p, CompoundTag d, QuestBook.Quest q) {
        int points = (int) Math.round(GameRules.questPoints(q.points(), mastery(p)) * Setting.GAME_POINTS.d() / 100);
        d.putInt(POINTS, d.getInt(POINTS) + points);
        Mastery.add(p, id, GameRules.QUEST_MASTERY);
        note(p, "" + q.text() + ": done. +" + points + " points.");
        p.playNotifySound(PowerSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 0.6f, 1f);
    }

    // ---- keys and the screen

    @Override
    public void onKey(ServerPlayer p, String ability) {
        CompoundTag keyData = data(p);
        if ("gp_step".equals(ability)) {
            if (!on(keyData, "shadow_step")) {
                note(p, "Shadow Step isn't one of your skills. Buy it on your Status Window.");
                return;
            }
            var hit = p.pick(12, 0f, false);
            Vec3 to = hit.getLocation().subtract(p.getLookAngle().scale(0.6));
            p.connection.teleport(to.x, to.y, to.z, p.getYRot(), p.getXRot());
            p.fallDistance = 0;
            p.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.4f);
        } else if ("gp_fear".equals(ability)) {
            if (!on(keyData, "bloodlust")) {
                note(p, "Bloodlust isn't one of your skills. Buy it on your Status Window.");
                return;
            }
            for (net.minecraft.world.entity.LivingEntity e : p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, p.getBoundingBox().inflate(12), e -> e != p && e.isAlive() && !p.isAlliedTo(e))) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
            }
            p.playNotifySound(SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.5f, 1.3f);
        } else if ("gp_dash".equals(ability)) {
            if (!on(data(p), "dash")) {
                note(p, "Dash isn't one of your skills. Buy it on your Status Window.");
                return;
            }
            Vec3 look = p.getLookAngle().multiply(1, 0.2, 1).normalize();
            p.setDeltaMovement(p.getDeltaMovement().add(look.scale(1.6)));
            p.hurtMarked = true;
            p.fallDistance = 0;
            p.playNotifySound(SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8f, 1.6f);
        } else if ("gp_shadow".equals(ability)) {
            ShadowArmy.raise(p);
        }
    }

    private static boolean on(CompoundTag d, String skill) {
        return d.getCompound(ON).getBoolean(skill);
    }

    private static boolean owned(CompoundTag d, String skill) {
        return d.getCompound(OWNED).getBoolean(skill);
    }

    private static int countOn(CompoundTag d) {
        int n = 0;
        for (Skill s : SKILLS) {
            if (on(d, s.id())) n++;
        }
        return n;
    }

    private static UUID parseId(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return new UUID(0, 0);
        }
    }

    @Override
    public void onClientAction(ServerPlayer p, String action) {
        if (!owns(p)) return;
        CompoundTag d = data(p);
        String[] parts = action.split(":");
        switch (parts[0]) {
            case "open" -> {
                send(p, true);
                com.example.charpowers.anim.AnimNet.play(p, "hologram_swipe");
                return;
            }
            case "storage" -> {
                openStorage(p);
                return;
            }
            case "refresh" -> {}
            case "gate" -> {
                int rank = number(parts, GateRules.RANKS.length);
                if (rank >= 0) openGateNow(p, d, rank);
            }
            case "invite" -> {
                if (parts.length > 1) GateParty.invite(p, parseId(parts[1]));
                send(p, false);
                return;
            }
            case "uninvite" -> {
                if (parts.length > 1) GateParty.uninvite(p, parseId(parts[1]));
                send(p, false);
                return;
            }
            case "track" -> {
                int i = number(parts, MAX_GATES);
                if (i >= 0) {
                    if (d.contains(TRACK) && d.getInt(TRACK) == i) d.remove(TRACK);
                    else d.putInt(TRACK, i);
                }
            }
            case "look" -> d.putBoolean(LOOK, !d.getBoolean(LOOK));
            case "stat" -> {
                int stat = number(parts, GameRules.STATS.length);
                if (stat < 0) return;
                int[] points = stats(d);
                int cost = GameRules.statCost(points[stat]);
                if (d.getInt(POINTS) < cost) return;
                d.putInt(POINTS, d.getInt(POINTS) - cost);
                points[stat]++;
                d.putIntArray(STATS, points);
                applyStats(p, d);
            }
            case "skill" -> {
                int i = number(parts, SKILLS.size());
                if (i < 0) return;
                Skill s = SKILLS.get(i);
                if (!owned(d, s.id())) {
                    if (d.getInt(POINTS) < s.cost()) return;
                    d.putInt(POINTS, d.getInt(POINTS) - s.cost());
                    subTag(d, OWNED).putBoolean(s.id(), true);
                }
                boolean now = on(d, s.id());
                if (now || countOn(d) < GameRules.slots(mastery(p))) subTag(d, ON).putBoolean(s.id(), !now);
                else note(p, "All your skill slots are full. Switch one off first.");
            }
            case "shop" -> {
                if (!awakened(p)) return;
                List<ShopItem> shop = shop();
                int i = number(parts, shop.size());
                if (i < 0 || d.getInt(COINS) < shop.get(i).price()) return;
                d.putInt(COINS, d.getInt(COINS) - shop.get(i).price());
                Fx.give(p, new ItemStack(shop.get(i).item(), shop.get(i).count()));
            }
            case "double_jump" -> {
                if (on(d, "double_jump")) p.fallDistance = 0;
                return;
            }
            default -> {
                return;
            }
        }
        send(p, false);
    }

    // A private stash: no block, no world, nobody else can ever reach it. It's saved on you and survives death.
    public void openStorage(ServerPlayer p) {
        CompoundTag d = data(p);
        SimpleContainer box = new SimpleContainer(STORAGE_SLOTS) {
            @Override
            public void stopOpen(Player who) {
                ListTag saved = new ListTag();
                for (int i = 0; i < getContainerSize(); i++) {
                    if (getItem(i).isEmpty()) continue;
                    CompoundTag item = getItem(i).save(new CompoundTag());
                    item.putByte("Slot", (byte) i);
                    saved.add(item);
                }
                d.put(STORAGE, saved);
            }
        };
        ListTag saved = d.getList(STORAGE, Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag item = saved.getCompound(i);
            box.setItem(item.getByte("Slot") & 255, ItemStack.of(item));
        }
        p.openMenu(new SimpleMenuProvider((id, inventory, who) -> ChestMenu.sixRows(id, inventory, box), Component.translatable(HOLO_STORAGE)));
    }

    private static int number(String[] parts, int limit) {
        try {
            int n = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
            return n < limit ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static CompoundTag subTag(CompoundTag d, String key) {
        if (!d.contains(key)) d.put(key, new CompoundTag());
        return d.getCompound(key);
    }

    private static CompoundTag row(String text, int have, int need, String reward) {
        CompoundTag t = new CompoundTag();
        t.putString("text", text);
        t.putInt("have", have);
        t.putInt("need", need);
        t.putString("reward", reward);
        return t;
    }

    public void send(ServerPlayer p, boolean open) {
        CompoundTag d = data(p), tag = new CompoundTag();
        rollDay(p, d);
        int level = mastery(p);
        tag.putBoolean("open", open);
        tag.putBoolean("normal", d.getBoolean(LOOK));
        tag.putBoolean("doubleJump", on(d, "double_jump"));
        tag.putBoolean("awakened", awakened(p));
        tag.putInt("points", d.getInt(POINTS));
        tag.putInt("coins", d.getInt(COINS));
        tag.putInt("level", level);
        tag.putInt("slots", GameRules.slots(level));
        tag.putInt("slotsUsed", countOn(d));
        ListTag stats = new ListTag();
        int[] points = stats(d);
        for (int i = 0; i < points.length; i++) {
            CompoundTag t = new CompoundTag();
            t.putString("name", GameRules.STATS[i]);
            t.putInt("points", points[i]);
            t.putInt("cost", GameRules.statCost(points[i]));
            t.putString("words", GameRules.statWords(i, points[i]));
            stats.add(t);
        }
        tag.put("stats", stats);
        ListTag skills = new ListTag();
        for (Skill s : SKILLS) {
            CompoundTag t = new CompoundTag();
            t.putString("name", s.name());
            t.putString("about", s.about());
            t.putInt("cost", s.cost());
            t.putBoolean("owned", owned(d, s.id()));
            t.putBoolean("on", on(d, s.id()));
            skills.add(t);
        }
        tag.put("skills", skills);
        ListTag quests = new ListTag();
        List<QuestBook.Quest> daily = today(p, d);
        int[] counts = d.getIntArray(DAILY), done = d.getIntArray(DAILY_DONE);
        for (int i = 0; i < daily.size() && i < counts.length; i++) {
            QuestBook.Quest q = daily.get(i);
            quests.add(row(q.text(), done[i] != 0 ? q.count() : Math.min(counts[i], q.count()), q.count(), "+" + GameRules.questPoints(q.points(), level)));
        }
        addChain(quests, QuestBook.STORY, d.getInt(STORY), d.getInt(STORY_COUNT), level, "The story is finished.");
        if (GameRules.secondChain(level)) addChain(quests, QuestBook.SECOND_STORY, d.getInt(STORY2), d.getInt(STORY2_COUNT), level, "The side story is finished.");
        if (awakened(p)) {
            QuestBook.Quest q = QuestBook.special(seed(p, d));
            quests.add(row(q.text(), d.getBoolean(SPECIAL_DONE) ? q.count() : Math.min(d.getInt(SPECIAL), q.count()), q.count(), "+" + q.points() + " coins"));
        }
        tag.put("quests", quests);
        tag.putInt("topRank", GateRules.topRank(level));
        long wait = d.contains(OPEN_WAIT) ? d.getLong(OPEN_WAIT) - p.level().getGameTime() : 0;
        tag.putInt("openWait", (int) Math.max(0, wait / 20));
        ListTag gateRows = new ListTag();
        ListTag gates = d.getList(GATES, Tag.TAG_COMPOUND);
        for (int i = 0; i < gates.size(); i++) {
            CompoundTag gate = gates.getCompound(i), t = new CompoundTag();
            BlockPos at = BlockPos.of(gate.getLong("pos"));
            t.putString("rank", GateRules.RANKS[gate.getInt("rank")]);
            t.putBoolean("sealed", gate.getBoolean("sealed"));
            t.putString("label", gateLabel(gate));
            GatePlan.Theme th = themeOf(gate);
            t.putString("warn", th == null ? "" : GatePlan.warning(th));
            t.putInt("far", (int) Math.sqrt(at.distSqr(p.blockPosition())));
            t.putString("way", GateRules.compass(at.getX() - p.getX(), at.getZ() - p.getZ()));
            t.putBoolean("tracked", d.contains(TRACK) && d.getInt(TRACK) == i);
            gateRows.add(t);
        }
        tag.put("gates", gateRows);
        tag.put("mates", GateParty.rows(p));
        if (awakened(p)) {
            ListTag shop = new ListTag();
            for (ShopItem s : shop()) {
                CompoundTag t = new CompoundTag();
                t.putString("item", BuiltInRegistries.ITEM.getKey(s.item()).toString());
                t.putInt("count", s.count());
                t.putInt("price", s.price());
                shop.add(t);
            }
            tag.put("shop", shop);
        }
        Net.toOwner(p, id.toString(), tag);
    }

    private static void addChain(ListTag quests, List<QuestBook.Quest> chain, int at, int have, int level, String finished) {
        if (at >= chain.size()) {
            quests.add(row(finished, 1, 1, ""));
            return;
        }
        QuestBook.Quest q = chain.get(at);
        quests.add(row(q.text(), Math.min(have, q.count()), q.count(), "+" + GameRules.questPoints(q.points(), level)));
    }

    // ---- the shop: a plain text file, so anything from any mod can go in

    private record ShopItem(Item item, int count, int price) {}

    private static void addShop(List<ShopItem> out, String line) {
        String[] parts = line.trim().split("\\s+");
        if (line.startsWith("#") || parts.length != 3 || !ResourceLocation.isValidResourceLocation(parts[0])) return;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0]));
        if (item == null || item == Items.AIR) return;
        for (ShopItem have : out) {
            if (have.item() == item) return;
        }
        try {
            out.add(new ShopItem(item, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        } catch (NumberFormatException e) {
            CharPowers.LOG.warn("Bad shop line: {}", line);
        }
    }

    // What's for sale: everything built in, then whatever's in the file, which can be anything from any mod.
    private static List<ShopItem> shop() {
        List<ShopItem> out = new ArrayList<>();
        for (String line : com.example.charpowers.game.model.ShopPrices.BUILT_IN) addShop(out, line);
        try {
            if (!Files.exists(SHOP_FILE)) {
                Files.createDirectories(SHOP_FILE.getParent());
                Files.writeString(SHOP_FILE, "# Adds to the Player's shop: item, how many, price in coins. Anything from any mod works.\n");
            }
            for (String line : Files.readAllLines(SHOP_FILE)) addShop(out, line);
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't read {}", SHOP_FILE, e);
        }
        return out;
    }

    // ---- what counts towards quests. Everything checks you have the power first, so nobody else pays for this.

    private static ServerPlayer owner(Player p) {
        return p instanceof ServerPlayer sp && POWER.owns(sp) ? sp : null;
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && POWER.owns(p)) POWER.count(p, "kill", BuiltInRegistries.ENTITY_TYPE.getKey(e.getEntity().getType()).toString(), 1);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        ServerPlayer p = owner(e.getPlayer());
        if (p != null) POWER.count(p, "mine", BuiltInRegistries.BLOCK.getKey(e.getState().getBlock()).toString(), 1);
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent e) {
        ServerPlayer p = owner(e.getEntity());
        if (p != null) POWER.count(p, "craft", "", Math.max(1, e.getCrafting().getCount()));
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent e) {
        ServerPlayer p = owner(e.getEntity());
        if (p != null) POWER.count(p, "smelt", "", Math.max(1, e.getSmelting().getCount()));
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getItem().isEdible() && POWER.owns(p)) POWER.count(p, "eat", "", 1);
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent e) {
        ServerPlayer p = owner(e.getEntity());
        if (p != null) POWER.count(p, "fish", "", 1);
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent e) {
        if (e.getCausedByPlayer() != null) {
            ServerPlayer p = owner(e.getCausedByPlayer());
            if (p != null) POWER.count(p, "breed", "", 1);
        }
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent e) {
        ServerPlayer p = owner(e.getEntity());
        if (p != null) POWER.count(p, "trade", "", 1);
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        ServerPlayer p = owner(e.getEntity());
        if (p != null && !e.wakeImmediately()) POWER.count(p, "sleep", "", 1);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer hurt && POWER.owns(hurt) && on(POWER.data(hurt), "iron_skin")) e.setAmount(e.getAmount() * 0.85f);
        if (!(e.getSource().getDirectEntity() instanceof ServerPlayer p)) return;
        boolean holdingOne = p.getMainHandItem().getItem() instanceof SystemWeapon;
        if (!POWER.owns(p)) {
            if (holdingOne) e.setAmount(0.5f);
            return;
        }
        CompoundTag d = POWER.data(p);
        if (on(d, "lucky_strike") && p.getRandom().nextInt(5) == 0) e.setAmount(e.getAmount() * 1.5f);
        if (on(d, "life_steal")) p.heal(e.getAmount() * 0.1f);
        if (holdingOne) ((SystemWeapon) p.getMainHandItem().getItem()).strike(p, e.getEntity(), e.getAmount());
    }

    // Nobody else can even pick one up.
    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent e) {
        if (e.getItem().getItem().getItem() instanceof SystemWeapon && !POWER.owns(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDied(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            GateRun run = POWER.memberOf.remove(p.getUUID());
            if (run != null) {
                // after respawning you go back to where you came in from, not to your bed
                CompoundTag back = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("charpowers.pocket.back");
                if (!back.isEmpty()) {
                    POWER.deathReturn.put(p.getUUID(), new double[]{back.getDouble("x"), back.getDouble("y"), back.getDouble("z"), back.getFloat("yaw")});
                    POWER.deathReturnDim.put(p.getUUID(), back.getString("dim"));
                }
                run.leave(p.getUUID(), false);
                POWER.noteAll(p, "You fell in the gate.");
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        double[] at = POWER.deathReturn.remove(p.getUUID());
        String dim = POWER.deathReturnDim.remove(p.getUUID());
        if (at == null || dim == null) return;
        p.getServer().execute(() -> {
            ResourceLocation id = ResourceLocation.tryParse(dim);
            ServerLevel to = id == null ? null : p.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
            if (to != null) p.teleportTo(to, at[0], at[1], at[2], (float) at[3], 0f);
        });
    }

    // Friends logged off inside a gate that is gone now: bring them back out.
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || POWER.owns(p) || !Pocket.in(p) || POWER.memberOf.containsKey(p.getUUID())) return;
        p.getServer().execute(() -> {
            if (Pocket.in(p)) {
                POWER.noteAll(p, "The gate closed while you were away.");
                Pocket.leave(p);
            }
        });
    }

    // Hitting a soul jar in a crypt room: it takes the hit instead of the block.
    @SubscribeEvent
    public static void onLeftClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAction() != net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) return;
        GateRun run = POWER.memberOf.get(p.getUUID());
        if (run != null && run.jarHit(e.getPos(), p)) e.setCanceled(true);
    }

    // Friends in the same gate never hurt each other (swings, arrows, powers).
    @SubscribeEvent
    public static void onFriendlyFire(net.minecraftforge.event.entity.living.LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim) || !(e.getSource().getEntity() instanceof ServerPlayer attacker) || victim == attacker) return;
        GateRun run = POWER.memberOf.get(victim.getUUID());
        if (run != null && run == POWER.memberOf.get(attacker.getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if (e.phase == net.minecraftforge.event.TickEvent.Phase.END) POWER.tickRuns();
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && POWER.owns(p) && on(POWER.data(p), "soft_landing")) e.setDamageMultiplier(e.getDamageMultiplier() * 0.5f);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        POWER.inDungeon.remove(id);
        POWER.lastSpot.remove(id);
        POWER.windWait.remove(id);
        GateRun run = POWER.memberOf.remove(id);
        if (run != null) run.leave(id, false); // the run goes on for the others, and ends by itself when nobody is left
        GateParty.forget(id);
        GameBossMob boss = POWER.bosses.remove(id);
        if (boss != null) boss.discard();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        POWER.inDungeon.clear();
        POWER.lastSpot.clear();
        POWER.windWait.clear();
        POWER.bosses.clear();
        POWER.runs.values().forEach(GateRun::end);
        POWER.runs.clear();
        POWER.memberOf.clear();
    }
}
