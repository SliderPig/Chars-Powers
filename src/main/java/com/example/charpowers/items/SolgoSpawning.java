package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.game.mob.GateBoss;
import com.example.charpowers.items.model.DropRules;
import com.example.charpowers.items.model.SolgoRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Where the drops come from, and how Solgo finds you. At night in the old dark woods, rarely, you get a quiet warning: distant whispers and, the first time, a line in chat.
// Half a minute later he's somewhere behind you. Never more than one at a time. Switch off with solgoSpawns in charpowers-items.toml.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class SolgoSpawning {
    private static final TagKey<Biome> HOME = TagKey.create(Registries.BIOME, new ResourceLocation(CharPowers.MODID, "solgo_home"));
    private static final String WARNED = "charpowers.solgoWarned", KILLED = "charpowers.solgoKilled";
    private static final Map<UUID, Long> PENDING = new HashMap<>();
    private static int counter;

    private SolgoSpawning() {}

    // Is there one about, anywhere?
    public static boolean oneAlive(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.getEntities(CharItems.SOLGO.get(), s -> s.isAlive()).isEmpty()) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !ItemConfig.SOLGO_SPAWNS.get()) return;
        MinecraftServer server = e.getServer();
        long now = server.overworld().getGameTime();
        if (!PENDING.isEmpty()) {
            var it = PENDING.entrySet().iterator();
            while (it.hasNext()) {
                var en = it.next();
                if (now < en.getValue()) continue;
                it.remove();
                ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
                if (p != null && !oneAlive(server)) arrive(p);
            }
        }
        if (++counter < SolgoRules.CHECK_TICKS) return;
        counter = 0;
        ServerLevel level = server.overworld();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL || !SolgoRules.nightTime(level.getDayTime()) || !PENDING.isEmpty() || oneAlive(server)) return;
        for (ServerPlayer p : level.players()) {
            if (p.isCreative() || p.isSpectator() || !level.getBiome(p.blockPosition()).is(HOME)) continue;
            if (level.random.nextDouble() >= SolgoRules.CHECK_CHANCE) continue;
            warn(p);
            PENDING.put(p.getUUID(), now + 500 + level.random.nextInt(300));
            break;
        }
    }

    private static void warn(ServerPlayer p) {
        Vec3 far = p.position().add(p.getViewVector(1f).scale(-14));
        p.playNotifySound(CharItems.sound("solgo_whisper"), SoundSource.AMBIENT, 0.35f, 0.8f);
        p.serverLevel().playSound(null, far.x, far.y, far.z, CharItems.sound("solgo_breath"), SoundSource.AMBIENT, 0.5f, 0.9f);
        CompoundTag saved = keptData(p);
        if (!saved.getBoolean(WARNED)) {
            saved.putBoolean(WARNED, true);
            p.sendSystemMessage(Component.translatable("entity.charpowers.solgo.warning").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static CompoundTag keptData(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    // Somewhere behind you, among the trees.
    private static void arrive(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 back = p.getViewVector(1f).multiply(1, 0, 1);
        back = back.lengthSqr() < 1e-3 ? new Vec3(0, 0, -1) : back.normalize().reverse();
        for (int i = 0; i < 8; i++) {
            double a = (level.random.nextDouble() - 0.5) * 1.5;
            double dist = 28 + level.random.nextDouble() * 14;
            double x = p.getX() + (back.x * Math.cos(a) - back.z * Math.sin(a)) * dist, z = p.getZ() + (back.x * Math.sin(a) + back.z * Math.cos(a)) * dist;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
            if (!level.isLoaded(top) || !level.getFluidState(top.below()).isEmpty() || !level.getBiome(top).is(HOME)) continue;
            Solgo s = Solgo.spawn(level, x, top.getY(), z, true);
            if (s != null) s.setTarget(p);
            return;
        }
    }

    // A rank A or S gate's boss has died, and sometimes that isn't the end of it: he's optional, the way out is already open.
    public static void afterGateBoss(ServerLevel level, GateBoss boss, int rank, int party) {
        if (boss == null || rank < 4 || level.random.nextInt(6) != 0 || oneAlive(level.getServer())) return;
        Solgo s = Solgo.spawn(level, boss.getX(), boss.getY(), boss.getZ(), true);
        if (s == null) return;
        s.scaleForParty(rank - 4, party);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), CharItems.sound("solgo_whisper"), SoundSource.HOSTILE, 2f, 0.7f);
    }

    // ---- what drops what

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        Level world = e.getEntity().level();
        if (world.isClientSide) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType());
        if (id == null) return;
        ItemStack drop = dropFor(id.toString(), e.getEntity().getLastHurtByMob() instanceof Player p ? p : e.getSource().getEntity() instanceof Player q ? q : null, e.isRecentlyHit(), world.random.nextDouble());
        if (drop.isEmpty()) return;
        ItemEntity item = new ItemEntity(world, e.getEntity().getX(), e.getEntity().getY() + 0.5, e.getEntity().getZ(), drop);
        item.setDefaultPickUpDelay();
        item.setUnlimitedLifetime();
        e.getDrops().add(item);
    }

    // Also used by the tests.
    public static ItemStack dropFor(String id, Player killer, boolean byAPlayer, double roll) {
        switch (id) {
            case "charpowers:frost_king" -> {
                if (byAPlayer && DropRules.iceStaff(roll)) return new ItemStack(CharItems.ICE_STAFF.get());
            }
            case "charpowers:dark_elf" -> {
                if (byAPlayer && DropRules.dagger(roll)) return new ItemStack(CharItems.DAGGER.get());
            }
            case "charpowers:solgo" -> {
                if (killer == null) return ItemStack.EMPTY;
                CompoundTag saved = keptData(killer);
                boolean first = !saved.getBoolean(KILLED);
                saved.putBoolean(KILLED, true);
                if (DropRules.katana(first, roll)) return new ItemStack(CharItems.KATANA.get());
            }
            default -> {}
        }
        return ItemStack.EMPTY;
    }
}
