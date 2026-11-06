package com.example.charpowers.hidden.f;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.HiddenEvents;
import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.MobRule;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.Rank;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Shared bits for the F-rank powers, so each power is only a few lines.
public final class FHelp {
    // Registers a key power that just does one thing when used.
    public static void key(int n, String id, MobRule rule, int cooldown, Supplier<Item> icon, Consumer<PowerUser> use) {
        HiddenPowers.register(PowerDef.key(id, n, Rank.F, rule, cooldown), icon, new PowerEffect() {
            @Override public void onKey(PowerUser u) { use.accept(u); }
        });
    }

    public static void key(int n, String id, MobRule rule, Supplier<Item> icon, Consumer<PowerUser> use) {
        key(n, id, rule, PowerDef.DEFAULT_KEY_COOLDOWN, icon, use);
    }

    public static void key(int n, String id, MobRule rule, Supplier<Item> icon, PowerEffect fx) {
        key(n, id, rule, PowerDef.DEFAULT_KEY_COOLDOWN, icon, fx);
    }

    public static void key(int n, String id, MobRule rule, int cooldown, Supplier<Item> icon, PowerEffect fx) {
        HiddenPowers.register(PowerDef.key(id, n, Rank.F, rule, cooldown), icon, fx);
    }

    public static void always(int n, String id, MobRule rule, Supplier<Item> icon, PowerEffect fx) {
        HiddenPowers.register(PowerDef.always(id, n, Rank.F, rule), icon, fx);
    }

    // --- timers kept in the power's saved state ---

    public static void startFor(PowerUser u, String key, int ticks) { u.state().putLong(key, u.now() + ticks); }

    public static boolean running(PowerUser u, String key) { return u.now() < u.state().getLong(key); }

    // True on each new step the holder takes (each whole block walked).
    public static boolean stepped(PowerUser u) {
        int now = (int) u.entity().walkDist;
        int last = u.state().getInt("step");
        u.state().putInt("step", now);
        return now != last && u.entity().onGround();
    }

    // How far (sideways) the holder moved since this power last asked. Works for players too
    // (the server doesn't know a player's speed, only where they are).
    public static Vec3 moved(PowerUser u) {
        Vec3 now = u.entity().position();
        CompoundTag s = u.state();
        Vec3 d = s.contains("lx") ? new Vec3(now.x - s.getDouble("lx"), 0, now.z - s.getDouble("lz")) : Vec3.ZERO;
        s.putDouble("lx", now.x);
        s.putDouble("lz", now.z);
        return d;
    }

    public static boolean moving(PowerUser u) { return moved(u).lengthSqr() > 0.0025; }

    // Turn the holder by `degrees` (players see their own view turn).
    public static void turn(LivingEntity e, float degrees) {
        if (e instanceof net.minecraft.server.level.ServerPlayer p) {
            p.connection.teleport(0, 0, 0, degrees, 0, EnumSet.allOf(RelativeMovement.class));
        } else {
            e.setYRot(e.getYRot() + degrees);
            e.setYHeadRot(e.getYHeadRot() + degrees);
            e.yBodyRot = e.getYRot();
        }
    }

    // Spinning: start with startSpin, keep calling spinTick from tick().
    public static void startSpin(PowerUser u, int ticks, float degreesPerTick) {
        u.state().putInt("spinLeft", ticks);
        u.state().putFloat("spinStep", degreesPerTick);
    }

    public static void spinTick(PowerUser u) {
        int left = u.state().getInt("spinLeft");
        if (left <= 0) return;
        u.state().putInt("spinLeft", left - 1);
        turn(u.entity(), u.state().getFloat("spinStep"));
    }

    // A soft colour that changes once a second (never flashing).
    public static DustParticleOptions slowColour(long gameTime, float size) {
        float[][] c = {{1f, .35f, .7f}, {1f, .6f, .2f}, {1f, .95f, .3f}, {.4f, .95f, .4f}, {.35f, .7f, 1f}, {.7f, .45f, 1f}};
        float[] k = c[(int) ((gameTime / 20) % c.length)];
        return new DustParticleOptions(new Vector3f(k[0], k[1], k[2]), size);
    }

    public static DustParticleOptions randomColour(net.minecraft.util.RandomSource r) {
        return new DustParticleOptions(new Vector3f(r.nextFloat(), r.nextFloat(), r.nextFloat()), 1.0f);
    }

    public static ItemStack named(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.setHoverName(Component.literal(name));
        return s;
    }

    // --- short-lived helper entities (Ghost Kitty, confused chicken...) ---

    public static void markTemp(Entity e) { e.addTag(HiddenEvents.TEMP_TAG); }

    public static Entity find(PowerUser u, String key) {
        if (!u.state().hasUUID(key)) return null;
        UUID id = u.state().getUUID(key);
        Entity e = u.level().getEntity(id);
        return e != null && e.isAlive() ? e : null;
    }

    public static void forget(PowerUser u, String key) {
        Entity e = find(u, key);
        if (e != null) e.discard();
        u.state().remove(key);
    }

    // Floating text for `ticks` (Sticky Note, Thought Bubble), removed afterwards by textTick.
    // Made straight in code (the first version used a summon command, which broke on some text).
    public static void floatingText(PowerUser u, Vec3 at, String text, int ticks) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(u.level());
        if (d == null) return;
        d.setPos(at.x, at.y, at.z);
        CompoundTag tag = d.saveWithoutId(new CompoundTag());
        tag.putString("text", Component.Serializer.toJson(Component.literal(text)));
        tag.putString("billboard", "center");
        d.load(tag);
        markTemp(d);
        u.level().addFreshEntity(d);
        CompoundTag t = u.state().getCompound("texts");
        t.putLong(d.getUUID().toString(), u.now() + ticks);
        u.state().put("texts", t);
    }

    // Removes floating texts whose time is up (call from tick()).
    public static void textTick(PowerUser u) {
        if (!u.state().contains("texts")) return;
        CompoundTag t = u.state().getCompound("texts");
        for (String id : t.getAllKeys().toArray(new String[0])) {
            if (u.now() < t.getLong(id)) continue;
            removeText(u, id);
            t.remove(id);
        }
    }

    // Removes all of this power's floating texts now (power taken off, logging out).
    public static void clearTexts(PowerUser u) {
        if (!u.state().contains("texts")) return;
        for (String id : u.state().getCompound("texts").getAllKeys()) removeText(u, id);
        u.state().remove("texts");
    }

    private static void removeText(PowerUser u, String id) {
        try {
            Entity e = u.level().getEntity(UUID.fromString(id));
            if (e != null) e.discard();
        } catch (IllegalArgumentException ignored) {
            // an old entry from version 1.0-1.2 (those were names, not ids); it's cleaned up by the temp tag
        }
    }

    public static String holderName(PowerUser u) {
        return u.player() != null ? u.player().getGameProfile().getName() : u.entity().getType().getDescription().getString();
    }

    public static void sayTo(PowerUser u, String text) { Fx.say(u, text); }

    private FHelp() {}
}
