package com.example.charpowers.anim;

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

// "This player is doing this pose": server to everyone who can see them, and to them. Only ever a pose on a model.
public final class AnimNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(CharPowers.MODID, "anim"), () -> VERSION, VERSION::equals, VERSION::equals);

    private AnimNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, Play.class, Play::write, Play::read, Play::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    // Plays a pose from player_poses.json on this player, for them and for everyone near. An empty name stops whatever's playing.
    public static void play(ServerPlayer p, String pose) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new Play(p.getId(), pose));
    }

    public static void stop(ServerPlayer p) {
        play(p, "");
    }

    public record Play(int entity, String pose) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(entity);
            b.writeUtf(pose, 64);
        }

        static Play read(FriendlyByteBuf b) {
            return new Play(b.readVarInt(), b.readUtf(64));
        }

        void handle(Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context c = context.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.client.anim.PlayerAnims.onMessage(entity, pose)));
            c.setPacketHandled(true);
        }
    }
}
