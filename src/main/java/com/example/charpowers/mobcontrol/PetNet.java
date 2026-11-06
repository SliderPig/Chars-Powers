package com.example.charpowers.mobcontrol;

import com.example.charpowers.CharPowers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// Once a second the server tells an owner which pets they have and how they are doing. Its own small channel.
public final class PetNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CharPowers.MODID, "mobcontrol"), () -> VERSION, VERSION::equals, VERSION::equals);

    // state: 0 following, 1 fighting, 2 staying, 3 guarding
    public record Entry(int id, String name, String type, float health, float maxHealth, byte state) {}

    private PetNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, Pets.class, Pets::write, Pets::read, Pets::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    static void send(ServerPlayer p, List<Entry> list) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Pets(list));
    }

    public record Pets(List<Entry> list) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(list.size());
            for (Entry e : list) {
                b.writeVarInt(e.id());
                b.writeUtf(e.name(), 64);
                b.writeUtf(e.type(), 100);
                b.writeFloat(e.health());
                b.writeFloat(e.maxHealth());
                b.writeByte(e.state());
            }
        }

        static Pets read(FriendlyByteBuf b) {
            int n = Math.min(b.readVarInt(), 64);
            List<Entry> out = new ArrayList<>();
            for (int i = 0; i < n; i++) out.add(new Entry(b.readVarInt(), b.readUtf(64), b.readUtf(100), b.readFloat(), b.readFloat(), b.readByte()));
            return new Pets(out);
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.example.charpowers.mobcontrol.client.PetHud.receive(list)));
            ctx.get().setPacketHandled(true);
        }
    }
}
