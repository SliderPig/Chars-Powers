package com.example.charpowers.cloning;

import com.example.charpowers.cloning.jobs.CloneJob;
import com.example.charpowers.cloning.jobs.FollowJob;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// One of your 1-3 clones: a real player body with a pretend connection, so moving, hunger, tools wearing out,
// XP and picking things up all work the normal way. Curtain supplies the hands and feet (CloneActions).
public class CloneEntity extends ServerPlayer {
    public final UUID hostId;
    public final int slot;
    public final CloneMover mover = new CloneMover(this);
    public final CloneBrain brain = new CloneBrain(this);
    public CloneJob job = new FollowJob();
    public long earnedMastery;
    public boolean rogue;
    public boolean rogueChecked;
    // the only Palladium powers this clone may hold: first-join rolls from other mods get taken off again
    final Set<ResourceLocation> allowedPowers = new HashSet<>();
    // what other mods have tried to give it so far; they keep trying, and that must never count as a chest item working
    final Set<ResourceLocation> rolledPowers = new HashSet<>();
    final Set<String> triedItems = new HashSet<>();
    Set<ResourceLocation> powersBefore = new HashSet<>();
    int tryingItemFor;
    int tryHeld;
    public boolean digging;
    InteractionHand tryingHand;
    boolean gone;

    CloneEntity(MinecraftServer server, ServerLevel level, GameProfile profile, UUID hostId, int slot) {
        super(server, level, profile);
        this.hostId = hostId;
        this.slot = slot;
        this.addTag(Cloning.TAG);
    }

    @Override
    public void tick() {
        // what a real player's connection would do for them
        if (getServer().getTickCount() % 10 == 0) {
            connection.resetPosition();
            serverLevel().getChunkSource().move(this);
        }
        try {
            super.tick();
            doTick();
        } catch (NullPointerException ignored) {
            // a body with no real connection can hit this in vanilla's movement checks; the same as Curtain's own
        }
        if (!gone && isAlive()) CloneManager.tickClone(this);
    }

    // Everyone's PC needs telling where a new body really is, and to show its whole skin.
    void fixBody() {
        ServerLevel level = serverLevel();
        getServer().getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(this, (byte) (yHeadRot * 256 / 360)), level.dimension());
        getServer().getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(this), level.dimension());
        entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
    }

    public void setJob(CloneJob next) {
        if (rogue && !next.lockedIn()) return;
        job.stop(this);
        job = next;
        job.start(this);
    }

    @Override
    public void die(DamageSource source) {
        if (gone) return;
        // the server has keepInventory on, but a clone's gear has to stay where it fell
        getInventory().dropAll();
        super.die(source);
        setHealth(20f);
        foodData = new FoodData();
        CloneManager.onDied(this);
    }

    // Takes the body off the server (next tick, as this is often called while the world is ticking entities).
    void leave(Component reason) {
        if (gone) return;
        gone = true;
        ejectPassengers();
        stopRiding();
        MinecraftServer server = getServer();
        server.tell(new TickTask(server.getTickCount(), () -> connection.onDisconnect(reason)));
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        doCheckFallDamage(0.0, y, 0.0, onGround);
    }
}
