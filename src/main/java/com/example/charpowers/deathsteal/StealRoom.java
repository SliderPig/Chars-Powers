package com.example.charpowers.deathsteal;

import com.example.charpowers.deathsteal.model.StealRules;
import com.example.charpowers.own.Pocket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// The all-black room with the white floor: a podium for each power, an icon floating over each, spinning so it never
// faces you, and something to punch. It only exists while someone is in it.
final class StealRoom {
    private static final double PUNCH_REACH = 5;

    record Option(String kind, String id, ItemStack icon, Component name) {}

    private final ServerPlayer player;
    private final ServerLevel level;
    final List<Option> options;
    private final List<Display.ItemDisplay> icons = new ArrayList<>();
    private final List<Interaction> targets = new ArrayList<>();
    private final List<Entity> names = new ArrayList<>();
    private final List<BlockPos> podiums = new ArrayList<>();
    private final BlockPos middle;
    private boolean wasSwinging;
    final long opened;

    StealRoom(ServerPlayer player, ServerLevel level, List<Option> options, int mastery) {
        this.player = player;
        this.level = level;
        this.options = options;
        this.middle = Pocket.roomOf(player.getUUID()).offset(0, 0, 100);
        this.opened = level.getGameTime();
        Pocket.buildRoom(level, middle, 16, 8, Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.WHITE_CONCRETE.defaultBlockState());
        Pocket.enter(player, level, middle);
        for (int i = 0; i < options.size(); i++) {
            int[] spot = StealRules.spot(i, options.size());
            BlockPos podium = middle.offset(spot[0], 0, -spot[1]);
            level.setBlock(podium, Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
            podiums.add(podium);
            icons.add(icon(podium, options.get(i).icon()));
            targets.add(target(podium));
            if (mastery >= StealRules.ALL_ICONS_LEVEL) names.add(name(podium, options.get(i).name()));
        }
    }

    private Display.ItemDisplay icon(BlockPos podium, ItemStack stack) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(level);
        d.setPos(podium.getX() + 0.5, podium.getY() + 2.0, podium.getZ() + 0.5);
        CompoundTag tag = d.saveWithoutId(new CompoundTag());
        tag.put("item", stack.save(new CompoundTag()));
        tag.putString("item_display", "fixed");
        d.load(tag);
        level.addFreshEntity(d);
        return d;
    }

    private Interaction target(BlockPos podium) {
        Interaction box = EntityType.INTERACTION.create(level);
        box.setPos(podium.getX() + 0.5, podium.getY() + 1.2, podium.getZ() + 0.5);
        CompoundTag tag = box.saveWithoutId(new CompoundTag());
        tag.putFloat("width", 1.4f);
        tag.putFloat("height", 1.6f);
        box.load(tag);
        level.addFreshEntity(box);
        return box;
    }

    private Entity name(BlockPos podium, Component text) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(level);
        d.setPos(podium.getX() + 0.5, podium.getY() + 2.9, podium.getZ() + 0.5);
        CompoundTag tag = d.saveWithoutId(new CompoundTag());
        tag.putString("text", Component.Serializer.toJson(text));
        tag.putString("billboard", "center");
        d.load(tag);
        level.addFreshEntity(d);
        return d;
    }

    // Every other tick: each icon turns its edge to you, unless your mastery lets it show itself.
    void spin(int mastery) {
        long now = level.getGameTime() - opened;
        for (int i = 0; i < icons.size(); i++) {
            Display.ItemDisplay d = icons.get(i);
            float toYou = (float) Math.toDegrees(Math.atan2(-(player.getX() - d.getX()), player.getZ() - d.getZ()));
            d.setYRot(StealRules.faces(mastery, i, icons.size(), now) ? toYou : toYou + 90f);
        }
    }

    // The moment you swing: which icon you're looking at, or -1. It's worked out here rather than trusting your PC to report a hit, so
    // whatever else is going on over there, a punch at an icon counts.
    int swungAt(ServerPlayer p) {
        boolean started = p.swinging && !wasSwinging;
        wasSwinging = p.swinging;
        if (!started) return -1;
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getViewVector(1f).scale(PUNCH_REACH));
        int best = -1;
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < targets.size(); i++) {
            var hit = targets.get(i).getBoundingBox().clip(eye, end);
            if (hit.isPresent() && hit.get().distanceToSqr(eye) < nearest) {
                nearest = hit.get().distanceToSqr(eye);
                best = i;
            }
        }
        return best;
    }

    int indexOf(Entity hit) {
        return targets.indexOf(hit);
    }

    void close() {
        icons.forEach(Entity::discard);
        targets.forEach(Entity::discard);
        names.forEach(Entity::discard);
        for (BlockPos podium : podiums) level.setBlock(podium, Blocks.AIR.defaultBlockState(), 2);
    }
}
