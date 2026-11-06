package com.example.charpowers.orders;

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

// The order wheel's own channel: the server says what you have, the PC says which order you picked.
public final class OrderNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CharPowers.MODID, "orders"), () -> VERSION, VERSION::equals, VERSION::equals);

    private OrderNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, Give.class, Give::write, Give::read, Give::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Have.class, Have::write, Have::read, Have::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    static void sendHave(ServerPlayer p, int clones, int pets, int minions) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Have(clones, pets, minions));
    }

    // PC -> server
    public record Give(String group, String action) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(group, 16);
            b.writeUtf(action, 16);
        }

        static Give read(FriendlyByteBuf b) { return new Give(b.readUtf(16), b.readUtf(16)); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sender != null) OrderServer.give(sender, group, action);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // server -> PC: how many of each kind you have out right now
    public record Have(int clones, int pets, int minions) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(clones);
            b.writeVarInt(pets);
            b.writeVarInt(minions);
        }

        static Have read(FriendlyByteBuf b) { return new Have(b.readVarInt(), b.readVarInt(), b.readVarInt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.orders.client.OrderWheel.have(clones, pets, minions)));
            ctx.get().setPacketHandled(true);
        }
    }
}
