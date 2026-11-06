package com.example.charpowers.game;

import com.example.charpowers.CharPowers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

// Which creatures are shadows right now, so every PC in that world can tint them. Its own small channel.
public final class ShadowNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CharPowers.MODID, "shadows"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ShadowNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, Ids.class, Ids::write, Ids::read, Ids::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    static void send(ServerLevel level, int[] ids) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new Ids(ids));
    }

    public record Ids(int[] ids) {
        void write(FriendlyByteBuf b) {
            b.writeVarIntArray(ids);
        }

        static Ids read(FriendlyByteBuf b) { return new Ids(b.readVarIntArray(64)); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.game.client.ShadowClient.set(ids)));
            ctx.get().setPacketHandled(true);
        }
    }
}
