package com.example.charpowers.cloning;

import dev.dubhe.curtain.features.player.patches.FakeClientConnection;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

// The clone's pretend internet connection. Curtain's sends everything into a queue nobody reads,
// which would slowly eat memory while a clone is out, so packets to a clone are simply thrown away.
final class CloneConnection extends FakeClientConnection {
    private CloneConnection() {
        super(PacketFlow.SERVERBOUND);
    }

    // Returns plain Connection on purpose: CloneManager has to load on PCs without Curtain, as it runs on every logout.
    static Connection make() {
        return new CloneConnection();
    }

    @Override
    public void send(Packet<?> packet) {}

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {}
}
