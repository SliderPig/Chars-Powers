package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.client.ItemsClient;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

// Messages for the weapons: your clicks go up, what everyone should see comes down.
public final class ItemNet {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(CharPowers.MODID, "items"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ItemNet() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SwordPress.class, SwordPress::write, SwordPress::read, SwordPress::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, HammerSlam.class, HammerSlam::write, HammerSlam::read, HammerSlam::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Show.class, Show::write, Show::read, Show::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, GroundLine.class, GroundLine::write, GroundLine::read, GroundLine::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, WeaponHit.class, WeaponHit::write, WeaponHit::read, WeaponHit::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void toServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    // To everyone who can see this entity, itself included.
    public static void toWatchers(Entity e, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> e), message);
    }

    public static void toPlayersNear(ServerLevel level, double x, double y, double z, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, 64, level.dimension())), message);
    }

    // A click with the Long Sword. target = what the crosshair was on, -1 for nothing.
    public record SwordPress(int target) {
        static void write(SwordPress m, FriendlyByteBuf b) {
            b.writeVarInt(m.target + 1);
        }

        static SwordPress read(FriendlyByteBuf b) {
            return new SwordPress(b.readVarInt() - 1);
        }

        static void handle(SwordPress m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ServerPlayer p = ctx.getSender();
            ctx.enqueueWork(() -> {
                if (p != null) LongSwordMoves.press(p, m.target);
            });
            ctx.setPacketHandled(true);
        }
    }

    // A click on something with one of the weapons: it's hit when the blade gets there.
    public record WeaponHit(int target, int step) {
        static void write(WeaponHit m, FriendlyByteBuf b) {
            b.writeVarInt(m.target);
            b.writeByte(m.step);
        }

        static WeaponHit read(FriendlyByteBuf b) {
            return new WeaponHit(b.readVarInt(), b.readByte());
        }

        static void handle(WeaponHit m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ServerPlayer p = ctx.getSender();
            ctx.enqueueWork(() -> {
                if (p != null) WeaponHits.request(p, m.target, m.step);
            });
            ctx.setPacketHandled(true);
        }
    }

    // A click on the ground with the Flame Hammer, at a spot.
    public record HammerSlam(double x, double y, double z) {
        static void write(HammerSlam m, FriendlyByteBuf b) {
            b.writeDouble(m.x);
            b.writeDouble(m.y);
            b.writeDouble(m.z);
        }

        static HammerSlam read(FriendlyByteBuf b) {
            return new HammerSlam(b.readDouble(), b.readDouble(), b.readDouble());
        }

        static void handle(HammerSlam m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ServerPlayer p = ctx.getSender();
            ctx.enqueueWork(() -> {
                if (p != null) FlameBurst.requested(p, m.x, m.y, m.z);
            });
            ctx.setPacketHandled(true);
        }
    }

    // This entity is doing this move, so draw it and its trail.
    public record Show(int entity, int style, int step) {
        static void write(Show m, FriendlyByteBuf b) {
            b.writeVarInt(m.entity);
            b.writeByte(m.style);
            b.writeByte(m.step);
        }

        static Show read(FriendlyByteBuf b) {
            return new Show(b.readVarInt(), b.readByte(), b.readByte());
        }

        static void handle(Show m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            if (FMLEnvironment.dist.isClient()) ctx.enqueueWork(() -> ItemsClient.show(m.entity, WeaponStyle.values()[m.style], m.step));
            ctx.setPacketHandled(true);
        }
    }

    // A line of light rushing along the ground: kind 0 = the Long Sword's golden shockwave.
    public record GroundLine(int kind, double x, double y, double z, float dirX, float dirZ, float length) {
        static void write(GroundLine m, FriendlyByteBuf b) {
            b.writeByte(m.kind);
            b.writeDouble(m.x);
            b.writeDouble(m.y);
            b.writeDouble(m.z);
            b.writeFloat(m.dirX);
            b.writeFloat(m.dirZ);
            b.writeFloat(m.length);
        }

        static GroundLine read(FriendlyByteBuf b) {
            return new GroundLine(b.readByte(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat(), b.readFloat());
        }

        static void handle(GroundLine m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            if (FMLEnvironment.dist.isClient()) ctx.enqueueWork(() -> ItemsClient.groundLine(m.kind, m.x, m.y, m.z, m.dirX, m.dirZ, m.length));
            ctx.setPacketHandled(true);
        }
    }
}
