package com.example.charpowers.lantern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// The dome. Nothing gets in or out: whoever was inside when it formed stays in, everyone else stays out. It's only ever checked
// while it exists, and only for what's near its wall.
public class BubbleEntity extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(BubbleEntity.class, EntityDataSerializers.FLOAT);
    public static final List<BubbleEntity> ALL = new ArrayList<>();

    private final Set<UUID> inside = new HashSet<>();
    private boolean started;

    public BubbleEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static BubbleEntity make(Level level, Vec3 at, float radius) {
        BubbleEntity b = new BubbleEntity(LanternEntities.BUBBLE.get(), level);
        b.entityData.set(RADIUS, radius);
        b.moveTo(at.x, at.y, at.z, 0f, 0f);
        level.addFreshEntity(b);
        ALL.add(b);
        return b;
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    public boolean holds(Vec3 pos) {
        return pos.y >= getY() - 1 && pos.distanceToSqr(position()) < radius() * radius();
    }

    // Is this point and that point on different sides of the wall?
    public static boolean crossing(Level level, Vec3 from, Vec3 to) {
        for (BubbleEntity b : ALL) {
            if (b.level() == level && b.holds(from) != b.holds(to)) return true;
        }
        return false;
    }

    public static boolean inAny(Entity e) {
        for (BubbleEntity b : ALL) {
            if (b.level() == e.level() && b.holds(e.position())) return true;
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RADIUS, 10f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        double r = radius();
        AABB near = new AABB(getX() - r - 2, getY() - 2, getZ() - r - 2, getX() + r + 2, getY() + r + 2, getZ() + r + 2);
        if (!started) {
            started = true;
            for (Entity e : level().getEntitiesOfClass(LivingEntity.class, near)) {
                if (holds(e.position())) inside.add(e.getUUID());
            }
        }
        for (Entity e : level().getEntities(this, near, x -> x instanceof LivingEntity || x instanceof Projectile)) {
            boolean in = holds(e.position());
            // a shot only counts when it crosses the wall: you can still shoot things on your own side
            if (e instanceof Projectile) {
                if (in != holds(new Vec3(e.xo, e.yo, e.zo))) e.discard();
                continue;
            }
            boolean belongs = inside.contains(e.getUUID());
            if (in == belongs) continue;
            if (in && e.tickCount < 5) { // born in here
                inside.add(e.getUUID());
                continue;
            }
            Vec3 out = e.position().subtract(position());
            Vec3 way = out.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : out.normalize();
            Vec3 to = position().add(way.scale(belongs ? r - 0.6 : r + 0.6));
            double y = Math.max(to.y, getY());
            if (e instanceof ServerPlayer p) p.connection.teleport(to.x, y, to.z, p.getYRot(), p.getXRot());
            else e.setPos(to.x, y, to.z);
            e.setDeltaMovement(way.scale(belongs ? -0.2 : 0.2));
            e.hurtMarked = true;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        ALL.remove(this);
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
