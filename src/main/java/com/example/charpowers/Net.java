package com.example.charpowers;

import com.example.charpowers.client.ClientHidden;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.model.Loadout;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// Messages between the server and the game on the player's PC.
public final class Net {
    private static final String VERSION = "7"; // bump when messages change, so old and new versions don't mix
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CharPowers.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, SyncHidden.class, SyncHidden::write, SyncHidden::read, SyncHidden::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Equip.class, Equip::write, Equip::read, Equip::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, LieDown.class, LieDown::write, LieDown::read, LieDown::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, Off.class, Off::write, Off::read, Off::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, Hud.class, Hud::write, Hud::read, Hud::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, MasterySync.class, MasterySync::write, MasterySync::read, MasterySync::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, OpenCloneMenu.class, OpenCloneMenu::write, OpenCloneMenu::read, OpenCloneMenu::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, CloneOrder.class, CloneOrder::write, CloneOrder::read, CloneOrder::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8, RequestClones.class, RequestClones::write, RequestClones::read, RequestClones::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(9, CloneList.class, CloneList::write, CloneList::read, CloneList::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(10, OwnToServer.class, OwnToServer::write, OwnToServer::read, OwnToServer::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(11, OwnToClient.class, OwnToClient::write, OwnToClient::read, OwnToClient::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    // Player -> server: something for one of Char's own powers. The power decides whether to believe it.
    public record OwnToServer(String power, String action) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(power);
            b.writeUtf(action);
        }

        static OwnToServer read(FriendlyByteBuf b) { return new OwnToServer(b.readUtf(), b.readUtf()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sender != null && com.example.charpowers.own.OpMenu.CHANNEL.equals(power)) {
                    com.example.charpowers.own.OpMenu.action(sender, action);
                    return;
                }
                com.example.charpowers.own.OwnPower own = com.example.charpowers.own.OwnPowers.get(ResourceLocation.tryParse(power));
                if (sender != null && own != null) own.onClientAction(sender, action);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static void toOwner(ServerPlayer p, String power, net.minecraft.nbt.CompoundTag data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OwnToClient(power, data));
    }

    // Server -> player: something for one of Char's own powers to show (see client/OwnClient).
    public record OwnToClient(String power, net.minecraft.nbt.CompoundTag data) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(power);
            b.writeNbt(data);
        }

        static OwnToClient read(FriendlyByteBuf b) { return new OwnToClient(b.readUtf(), b.readNbt()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.client.OwnClient.receive(power, data)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Player -> server: the clones menu is open, send me the latest.
    public record RequestClones() {
        void write(FriendlyByteBuf b) {}

        static RequestClones read(FriendlyByteBuf b) { return new RequestClones(); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sender != null) sendClones(sender, false);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static void sendClones(ServerPlayer host, boolean open) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> host), new CloneList(open, com.example.charpowers.cloning.CloneManager.describe(host)));
    }

    // Server -> player: your clones, for the open clones menu. open = also open it.
    public record CloneList(boolean open, java.util.List<com.example.charpowers.cloning.CloneInfo> clones) {
        void write(FriendlyByteBuf b) {
            b.writeBoolean(open);
            b.writeCollection(clones, (buf, c) -> c.write(buf));
        }

        static CloneList read(FriendlyByteBuf b) {
            return new CloneList(b.readBoolean(), b.readList(com.example.charpowers.cloning.CloneInfo::read));
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.cloning.client.CloneClient.cloneList(open, clones)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Server -> player: your mastery levels, for the bar on the power screen.
    public record MasterySync(java.util.Map<String, Integer> levels) {
        void write(FriendlyByteBuf b) { b.writeMap(levels, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt); }

        static MasterySync read(FriendlyByteBuf b) { return new MasterySync(b.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.cloning.client.CloneClient.setMastery(levels)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Server -> player: you crouch + right-clicked your clone, here's its menu.
    public record OpenCloneMenu(int slot, String name, String job, float health, float maxHealth, int food, String chest) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(slot);
            b.writeUtf(name, 32);
            b.writeUtf(job, 32);
            b.writeFloat(health);
            b.writeFloat(maxHealth);
            b.writeVarInt(food);
            b.writeUtf(chest, 64);
        }

        static OpenCloneMenu read(FriendlyByteBuf b) {
            return new OpenCloneMenu(b.readVarInt(), b.readUtf(32), b.readUtf(32), b.readFloat(), b.readFloat(), b.readVarInt(), b.readUtf(64));
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.charpowers.cloning.client.CloneClient.openMenu(slot, name, job, health, maxHealth, food, chest)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Player -> server: a button in the clone menu ("job:attack", "mark", "dismiss", "pick", "name:SliderM").
    public record CloneOrder(int slot, String action) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(slot);
            b.writeUtf(action, 32);
        }

        static CloneOrder read(FriendlyByteBuf b) { return new CloneOrder(b.readVarInt(), b.readUtf(32)); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sender = ctx.get().getSender();
            ctx.get().enqueueWork(() -> {
                if (sender != null) com.example.charpowers.cloning.CloneEvents.order(sender, slot, action);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // Server -> player: your hidden powers are switched off (dampened) or back on. The parts that run on your PC
    // (movement powers, things on your screen) listen to this too.
    public record Off(boolean off) {
        void write(FriendlyByteBuf b) { b.writeBoolean(off); }

        static Off read(FriendlyByteBuf b) { return new Off(b.readBoolean()); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHidden.setOff(off)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Server -> player: a short line on your screen from a power (Nine Lives' lives, Everything's power right now...).
    // An empty text takes the line away.
    public record Hud(String key, String text) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(key, 64);
            b.writeUtf(text, 256);
        }

        static Hud read(FriendlyByteBuf b) { return new Hud(b.readUtf(64), b.readUtf(256)); }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHidden.hud(key, text)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Server -> players near someone: that player lies flat for a while (Play Dead, Flop).
    public record LieDown(int entityId, int ticks) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(entityId);
            b.writeVarInt(ticks);
        }

        static LieDown read(FriendlyByteBuf b) {
            return new LieDown(b.readVarInt(), b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHidden.lieDown(entityId, ticks)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static void sendTo(ServerPlayer p, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    // Server -> player: your collection and slots (so the menu tab can show them).
    public record SyncHidden(List<String> owned, List<String> keys, List<String> always) {
        @SuppressWarnings("unchecked")
        public static SyncHidden of(Loadout l) {
            var m = l.toMap();
            return new SyncHidden((List<String>) m.get("owned"), (List<String>) m.get("keys"), (List<String>) m.get("always"));
        }

        void write(FriendlyByteBuf b) {
            b.writeCollection(owned, FriendlyByteBuf::writeUtf);
            b.writeCollection(keys, FriendlyByteBuf::writeUtf);
            b.writeCollection(always, FriendlyByteBuf::writeUtf);
        }

        static SyncHidden read(FriendlyByteBuf b) {
            return new SyncHidden(b.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                    b.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                    b.readCollection(ArrayList::new, FriendlyByteBuf::readUtf));
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHidden.set(this)));
            ctx.get().setPacketHandled(true);
        }
    }

    // Player -> server: put a power in a slot (id) or take it off (empty id). The server checks everything again.
    public record Equip(boolean keySlot, int slot, String id) {
        void write(FriendlyByteBuf b) {
            b.writeBoolean(keySlot);
            b.writeVarInt(slot);
            b.writeUtf(id, 128);
        }

        static Equip read(FriendlyByteBuf b) {
            return new Equip(b.readBoolean(), b.readVarInt(), b.readUtf(128));
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            ctx.get().enqueueWork(() -> { if (p != null) HiddenRuntime.equip(p, keySlot, slot, id); });
            ctx.get().setPacketHandled(true);
        }
    }

    private Net() {}
}
