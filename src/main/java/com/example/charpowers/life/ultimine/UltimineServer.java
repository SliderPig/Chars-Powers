package com.example.charpowers.life.ultimine;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.model.OreRules;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.attack.Wreck;
import com.example.charpowers.life.LifeEnjoyer;
import com.example.charpowers.life.model.Ultimine;
import com.example.charpowers.life.model.Ultimine.P;
import com.example.charpowers.own.Setting;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

// Life Enjoyer, awakened: hold the key and the block you break takes its neighbours with it, in the shape you picked. The PC
// only says "key held, this shape"; the server works out the blocks itself and breaks each one the normal way, so claims and
// protection mods get their say for every block.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class UltimineServer {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CharPowers.MODID, "ultimine"), () -> VERSION, VERSION::equals, VERSION::equals);

    private static final Map<UUID, Integer> HELD = new HashMap<>(); // player -> shape number, only while the key is down

    private UltimineServer() {}

    public static void register() {
        CHANNEL.registerMessage(0, State.class, State::write, State::read, State::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Max.class, Max::write, Max::read, Max::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static int max() {
        return Setting.LIFE_ULTIMINE.i();
    }

    // PC -> server: key down or up, and which shape.
    public record State(boolean held, int shape) {
        void write(FriendlyByteBuf b) {
            b.writeBoolean(held);
            b.writeVarInt(shape);
        }

        static State read(FriendlyByteBuf b) {
            return new State(b.readBoolean(), b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                hold(p, held, shape);
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Max(max()));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // Server -> PC: the most blocks at once, so the outline matches what will break.
    public record Max(int max) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(max);
        }

        static Max read(FriendlyByteBuf b) {
            return new Max(b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> UltimineClient.setMax(max)));
            ctx.get().setPacketHandled(true);
        }
    }

    // The key went down or up (also used by the tests).
    public static void hold(ServerPlayer p, boolean held, int shape) {
        if (held && LifeEnjoyer.POWER.owns(p) && LifeEnjoyer.POWER.awakened(p)) HELD.put(p.getUUID(), shape);
        else HELD.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        HELD.remove(e.getEntity().getUUID());
    }

    // Called from Life Enjoyer's break event, for an awakened owner. Returns how many extra blocks went.
    public static int onBreak(ServerPlayer p, BlockPos origin, BlockState state) {
        Integer shape = HELD.get(p.getUUID());
        if (shape == null || HiddenRuntime.switchedOff(p)) return 0;
        ServerLevel level = p.serverLevel();
        HitResult hit = p.pick(p.isCreative() ? 6 : 5.5, 0f, false);
        int face = hit instanceof BlockHitResult b && b.getBlockPos().equals(origin) ? b.getDirection().get3DDataValue() : 1;
        List<P> cells = Ultimine.cells(Ultimine.Shape.of(shape), new P(origin.getX(), origin.getY(), origin.getZ()), face,
                p.getDirection().get3DDataValue(), p.blockPosition().getY(), max(), pos -> breakable(p, level, pos), pos -> same(level, state, pos));
        if (cells.size() < 2) return 0;
        int broken = 0;
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
        Wreck.BREAKING = true;
        try {
            for (int i = 1; i < cells.size(); i++) {
                ItemStack tool = p.getMainHandItem();
                if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 1) break;
                P c = cells.get(i);
                BlockPos pos = new BlockPos(c.x(), c.y(), c.z());
                if (!p.gameMode.destroyBlock(pos)) continue;
                broken++;
                x0 = Math.min(x0, c.x());
                y0 = Math.min(y0, c.y());
                z0 = Math.min(z0, c.z());
                x1 = Math.max(x1, c.x() + 1);
                y1 = Math.max(y1, c.y() + 1);
                z1 = Math.max(z1, c.z() + 1);
            }
        } finally {
            Wreck.BREAKING = false;
        }
        if (broken == 0) return 0;
        p.level().playSound(null, origin, PowerSounds.ULTIMINE_SWEEP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1f);
        // the drops land where the blocks were; a tick later (the first block's drop too) they are gathered at your feet
        AABB box = new AABB(Math.min(x0, origin.getX()), Math.min(y0, origin.getY()), Math.min(z0, origin.getZ()),
                Math.max(x1, origin.getX() + 1), Math.max(y1, origin.getY() + 1), Math.max(z1, origin.getZ() + 1)).inflate(1.0);
        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> gather(p, level, box)));
        return broken;
    }

    private static void gather(ServerPlayer p, ServerLevel level, AABB box) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.getAge() < 10)) {
            item.setPos(p.getX(), p.getY() + 0.2, p.getZ());
            item.setDeltaMovement(0, 0.1, 0);
        }
    }

    // Anything a tool can break: not air or water, not bedrock, not a chest or anything holding stuff. If the block needs the
    // right tool to give anything, your tool has to be right (no wasting blocks).
    static boolean breakable(ServerPlayer p, ServerLevel level, P c) {
        BlockPos pos = new BlockPos(c.x(), c.y(), c.z());
        if (!level.isLoaded(pos)) return false;
        BlockState s = level.getBlockState(pos);
        if (s.isAir() || s.getBlock() instanceof LiquidBlock || s.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) return false;
        return !s.requiresCorrectToolForDrops() || p.hasCorrectToolForDrops(s);
    }

    // For the free-form shape: the same block, any log with any log, or the same kind of ore (stone and deepslate versions).
    static boolean same(ServerLevel level, BlockState first, P c) {
        BlockState s = level.getBlockState(new BlockPos(c.x(), c.y(), c.z()));
        if (s.is(first.getBlock())) return true;
        if (s.is(BlockTags.LOGS) && first.is(BlockTags.LOGS)) return true;
        if (s.is(Tags.Blocks.ORES) && first.is(Tags.Blocks.ORES)) {
            var a = ForgeRegistries.BLOCKS.getKey(s.getBlock());
            var b = ForgeRegistries.BLOCKS.getKey(first.getBlock());
            return a != null && b != null && OreRules.family(a.getPath()).equals(OreRules.family(b.getPath()));
        }
        return false;
    }
}
