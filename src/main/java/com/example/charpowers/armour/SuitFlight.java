package com.example.charpowers.armour;

import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.model.SuitMotion;
import com.example.charpowers.hidden.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// The suit flying to you (IN) or away from you (OUT), as one closed bundle that rises up behind you, opens like a shell, closes round you
// from behind and last drops its helmet on. It carries the real suit item the whole time, so nothing can be lost: if it is killed or its
// owner is gone, the item drops or goes in the bag. Nothing here breaks or moves anything. The timeline is SuitMotion's.
public class SuitFlight extends Entity {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CharPowers.MODID);
    public static final RegistryObject<EntityType<SuitFlight>> TYPE = TYPES.register("suit_flight",
            () -> EntityType.Builder.<SuitFlight>of(SuitFlight::new, MobCategory.MISC).sized(0.6f, 1.8f).fireImmune().noSummon().clientTrackingRange(12).updateInterval(1).build("charpowers:suit_flight"));
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(SuitFlight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(SuitFlight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WHO = SynchedEntityData.defineId(SuitFlight.class, EntityDataSerializers.INT);
    public static final int IN = 0, OUT = 1;
    private static final Set<UUID> BUSY = new HashSet<>();

    private ItemStack stack = ItemStack.EMPTY;
    private UUID owner;
    private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private BlockPos station;
    private int age;
    private boolean done;

    public SuitFlight(EntityType<? extends SuitFlight> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    public static boolean busy(UUID player) {
        return BUSY.contains(player);
    }

    // The suit comes to you: from `start`, carrying `suit`.
    static void flyIn(ServerPlayer p, ItemStack suit, Vec3 start) {
        SuitFlight f = new SuitFlight(TYPE.get(), p.level());
        f.entityData.set(MODE, IN);
        f.entityData.set(WHO, p.getId());
        f.stack = suit;
        f.owner = p.getUUID();
        f.from = start;
        f.setPos(start);
        BUSY.add(p.getUUID());
        p.level().addFreshEntity(f);
        com.example.charpowers.anim.AnimNet.play(p, "suit_equip");
    }

    // The suit comes off you and flies to a station block, or into the sky when there is none.
    static void flyOut(ServerPlayer p, ItemStack suit, BlockPos target) {
        SuitFlight f = new SuitFlight(TYPE.get(), p.level());
        f.entityData.set(MODE, OUT);
        f.entityData.set(WHO, p.getId());
        f.entityData.set(AGE, 0);
        f.stack = suit;
        f.owner = p.getUUID();
        f.station = target;
        f.from = p.position();
        f.to = target != null ? Vec3.atBottomCenterOf(target).add(0, 1.1, 0) : p.position().add(7, 70, 5);
        f.setPos(p.position());
        BUSY.add(p.getUUID());
        p.level().addFreshEntity(f);
        com.example.charpowers.anim.AnimNet.play(p, "suit_unequip");
    }

    public int mode() {
        return entityData.get(MODE);
    }

    // Ticks since it started, as the server counted them (sent every tick): the picture works out everything else from this.
    public int age() {
        return entityData.get(AGE);
    }

    // The entity id of the player it belongs to.
    public int who() {
        return entityData.get(WHO);
    }

    public boolean leaving() {
        return mode() == OUT;
    }

    // Where it is on the putting-on timeline, smoothed between ticks.
    public double timeline(float partial) {
        double ticks = Math.min(SuitMotion.TOTAL, age() + partial);
        return SuitMotion.timeline((int) Math.floor(ticks), leaving()) + (leaving() ? -1 : 1) * (ticks - Math.floor(ticks));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MODE, IN);
        entityData.define(AGE, 0);
        entityData.define(WHO, -1);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel level = (ServerLevel) level();
        ServerPlayer p = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (p == null) return; // the owner is offline: the suit waits where it is
        BUSY.add(owner);
        boolean in = mode() == IN;
        age++;
        if (p.level() != level || p.isRemoved() || !p.isAlive()) {
            finishLost(p);
            return;
        }
        entityData.set(AGE, age);
        double t = SuitMotion.timeline(age, !in), before = SuitMotion.timeline(age - 1, !in);
        // where it hovers: just behind you, a little off the ground
        double yaw = Math.toRadians(p.getYRot());
        Vec3 behind = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(1.6);
        Vec3 hover = p.position().add(behind).add(0, 0.45 + 0.07 * Math.sin(age * 0.35), 0);
        Vec3 pos;
        float face = p.getYRot();
        if (t < SuitMotion.FLY) {
            Vec3 src = in ? from : to;
            double[] a = {src.x, src.y, src.z}, b = {hover.x, hover.y, hover.z};
            double[] at = SuitMotion.curve(a, SuitMotion.bow(a, b), b, SuitMotion.ease(SuitMotion.flight(t)));
            pos = new Vec3(at[0], at[1], at[2]);
            Vec3 d = pos.subtract(position());
            if (d.horizontalDistanceSqr() > 1e-4) {
                float motion = (float) Math.toDegrees(Math.atan2(-d.x, d.z)) + (in ? 0 : 180);
                face = Mth.rotLerp((float) SuitMotion.ease((SuitMotion.flight(t) - 0.6) / 0.4), motion, p.getYRot());
            } else {
                face = getYRot();
            }
            flames(level, d, true);
        } else {
            pos = hover.lerp(p.position(), SuitMotion.glide(t));
            if (t < SuitMotion.LAND) flames(level, Vec3.ZERO, false);
        }
        setPos(pos);
        setYRot(face);
        setXRot(0);
        if (age % 20 == 1 && t < SuitMotion.CLOSE_END) level.playSound(null, getX(), getY(), getZ(), SuitSounds.THRUSTER.get(), SoundSource.PLAYERS, t < SuitMotion.FLY ? 1.2f : 0.6f, 1f);
        for (SuitMotion.Beat beat : SuitMotion.beats(before, t)) beat(level, p, beat, in);
        if (age >= SuitMotion.TOTAL) {
            if (in) {
                done = true;
                InvincibleArmour.arrived(p, stack);
                stack = ItemStack.EMPTY;
                discard();
            } else {
                arriveOut(level, p);
            }
        }
    }

    private void beat(ServerLevel level, ServerPlayer p, SuitMotion.Beat beat, boolean in) {
        switch (beat) {
            case OPEN_SERVO -> level.playSound(null, getX(), getY(), getZ(), SuitSounds.SERVO.get(), SoundSource.PLAYERS, 0.9f, 0.8f);
            case CLANK_LEGS -> plate(level, 1.0f, 0.5);
            case CLANK_CHEST -> plate(level, 0.92f, 1.2);
            case CLANK_ARMS -> plate(level, 0.85f, 1.4);
            case HELMET -> {
                clank(level, 0.65f);
                level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.9, getZ(), 10, 0.25, 0.1, 0.25, 0.2);
                if (in) {
                    SuitNet.shake(p);
                    // the chest core glows
                    level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 12, 0.3, 0.3, 0.3, 0.04);
                    level.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1.2, getZ(), 8, 0.35, 0.35, 0.35, 0.02);
                }
            }
        }
    }

    private void plate(ServerLevel level, float pitch, double height) {
        clank(level, pitch);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY() + height, getZ(), 5, 0.3, 0.1, 0.3, 0.12);
    }

    // The flame: out of the back while it flies, out of the boots while it hovers.
    private void flames(ServerLevel level, Vec3 d, boolean flying) {
        Vec3 back = flying && d.lengthSqr() > 1e-4 ? d.normalize().scale(-1.0) : new Vec3(0, -0.4, 0);
        double y = flying ? 0.6 : 0.05;
        level.sendParticles(ParticleTypes.FLAME, getX() + back.x, getY() + y + back.y, getZ() + back.z, flying ? 4 : 3, 0.12, 0.08, 0.12, 0.03);
        if (age % 2 == 0) level.sendParticles(ParticleTypes.SMOKE, getX() + back.x * 1.5, getY() + y + back.y * 1.5, getZ() + back.z * 1.5, 2, 0.15, 0.1, 0.15, 0.01);
        if (age % 3 == 0) level.sendParticles(ParticleTypes.FIREWORK, getX() + back.x, getY() + y + back.y, getZ() + back.z, 1, 0.2, 0.1, 0.2, 0.05);
    }

    private void clank(ServerLevel level, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), SuitSounds.CLANK.get(), SoundSource.PLAYERS, 1f, pitch);
    }

    // Arrived at the station (or high in the sky): the suit rests on the station if it is free, otherwise it goes in the bag.
    private void arriveOut(ServerLevel level, ServerPlayer p) {
        done = true;
        boolean stored = false;
        if (station != null && level.isLoaded(station) && level.getBlockEntity(station) instanceof ChargingStation.Entity st && st.chest.isEmpty()) {
            BlockState state = level.getBlockState(station);
            st.chest = stack;
            st.changed(level, station, state);
            stored = true;
            InvincibleArmour.POWER.rememberStation(p, level, station);
            clank(level, 0.8f);
        }
        if (!stored) {
            Fx.give(p, stack);
            InvincibleArmour.POWER.whisper(p, station == null ? "The suit is waiting in your bag." : "The station was taken. The suit is back in your bag.");
        }
        stack = ItemStack.EMPTY;
        discard();
    }

    // The player is gone or dead: the suit is put in their bag, or dropped.
    private void finishLost(ServerPlayer p) {
        done = true;
        if (p != null && !p.isRemoved()) Fx.give(p, stack);
        else if (!stack.isEmpty()) spawnAtLocation(stack);
        stack = ItemStack.EMPTY;
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && owner != null) BUSY.remove(owner);
        if (!level().isClientSide && !done && !stack.isEmpty() && reason.shouldDestroy()) {
            spawnAtLocation(stack);
            stack = ItemStack.EMPTY;
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        stack = tag.contains("suit") ? ItemStack.of(tag.getCompound("suit")) : ItemStack.EMPTY;
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        entityData.set(MODE, tag.getInt("mode"));
        age = tag.getInt("age");
        from = new Vec3(tag.getDouble("fx"), tag.getDouble("fy"), tag.getDouble("fz"));
        to = new Vec3(tag.getDouble("tx"), tag.getDouble("ty"), tag.getDouble("tz"));
        station = tag.contains("station") ? BlockPos.of(tag.getLong("station")) : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!stack.isEmpty()) tag.put("suit", stack.save(new CompoundTag()));
        if (owner != null) tag.putUUID("owner", owner);
        tag.putInt("mode", mode());
        tag.putInt("age", age);
        tag.putDouble("fx", from.x);
        tag.putDouble("fy", from.y);
        tag.putDouble("fz", from.z);
        tag.putDouble("tx", to.x);
        tag.putDouble("ty", to.y);
        tag.putDouble("tz", to.z);
        if (station != null) tag.putLong("station", station.asLong());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
