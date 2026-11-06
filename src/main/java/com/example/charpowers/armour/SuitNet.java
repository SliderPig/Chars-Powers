package com.example.charpowers.armour;

import com.example.charpowers.CharPowers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

// Two tiny messages to your own PC: shake the view when the helmet lands, and where your charging station is (for the visor's arrow).
public final class SuitNet {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(CharPowers.MODID, "armour"), () -> VERSION, VERSION::equals, VERSION::equals);

    private SuitNet() {}

    static void register() {
        CHANNEL.registerMessage(0, Shake.class, (m, b) -> {}, b -> new Shake(), SuitNet::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Station.class, Station::write, Station::read, SuitNet::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    static void shake(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Shake());
    }

    // `dimension` empty = no station known.
    static void station(ServerPlayer p, String dimension, long pos) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Station(dimension, pos));
    }

    private static void handle(Shake m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.example.charpowers.armour.client.SuitShake::start));
        ctx.get().setPacketHandled(true);
    }

    private static void handle(Station m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.armour.client.SuitVisor.station(m.dimension(), m.pos())));
        ctx.get().setPacketHandled(true);
    }

    public record Shake() {}

    public record Station(String dimension, long pos) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(dimension);
            b.writeLong(pos);
        }

        static Station read(FriendlyByteBuf b) {
            return new Station(b.readUtf(), b.readLong());
        }
    }
}
