package com.example.charpowers.hidden.ranks;

import com.example.charpowers.hidden.HiddenPowers;
import com.example.charpowers.hidden.MobPowerTicker;
import com.example.charpowers.hidden.MobPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerEffect;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.Kind;
import com.example.charpowers.hidden.model.PowerDef;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;

import java.util.ArrayList;
import java.util.List;

import static com.example.charpowers.hidden.f.FHelp.*;
import static com.example.charpowers.hidden.model.MobRule.*;
import static com.example.charpowers.hidden.model.Rank.A;
import static com.example.charpowers.hidden.ranks.RankHelp.*;

// The A-rank hidden powers (amazing), numbers 281-295 in my list.
final class APowers {
    private static boolean busy; // a repeated hit (Echo Strike, Split Second) must not repeat itself again

    static void registerAll() {
        // #281 Afterimage: leave a frozen see-through copy of yourself. Press again within 10 s to snap back into it,
        // with the health and hunger you had when you left it. 1 minute cooldown.
        key(A, 281, "afterimage", CARRY, 20, () -> Items.GLASS_PANE, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                List<Entity> img = copies(u, "after");
                if (!img.isEmpty() && img.get(0).level() == e.level()) {
                    Vec3 at = img.get(0).position();
                    removeCopies(u, "after");
                    teleport(e, at);
                    e.setYRot(u.state().getFloat("yaw"));
                    e.setHealth(Math.min(e.getMaxHealth(), u.state().getFloat("hp")));
                    if (u.player() != null) {
                        u.player().getFoodData().setFoodLevel(u.state().getInt("food"));
                        u.player().getFoodData().setSaturation(u.state().getFloat("sat"));
                    }
                    Fx.sound(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1f);
                    startFor(u, "imgCd", 1200);
                    return;
                }
                if (running(u, "imgCd")) { tell(u, "Afterimage is resting (1 minute)."); return; }
                LivingEntity copy = copyOf(u, e.position(), "after", 200);
                if (copy == null) return;
                if (copy instanceof ArmorStand stand) stand.setInvisible(true); // only the head and armour show: see-through
                u.state().putFloat("yaw", e.getYRot());
                u.state().putFloat("hp", e.getHealth());
                if (u.player() != null) {
                    u.state().putInt("food", u.player().getFoodData().getFoodLevel());
                    u.state().putFloat("sat", u.player().getFoodData().getSaturationLevel());
                }
                u.state().putBoolean("left", true);
                Fx.sound(e, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1f, 1f);
                tell(u, "Press again within 10 s to snap back.");
            }
            @Override public void tick(PowerUser u) {
                if (u.now() % 5 != 0) return;
                copiesTick(u, false);
                if (u.state().getBoolean("left") && copies(u, "after").isEmpty()) {
                    u.state().putBoolean("left", false);
                    if (!running(u, "imgCd")) startFor(u, "imgCd", 1200);
                }
            }
            @Override public void stop(PowerUser u) { copiesTick(u, true); }
        });
        // #282 Borrowed Time: heal to full now... but 60 s later the same amount is taken back (never below half a heart). 2 minute cooldown.
        key(A, 282, "borrowed_time", ESCAPE, 2400, () -> Items.CLOCK, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                float owe = e.getMaxHealth() - e.getHealth();
                if (owe <= 0) { tell(u, "You're already at full health."); refund(u); return; }
                e.setHealth(e.getMaxHealth());
                u.state().putFloat("owe", owe);
                startFor(u, "oweAt", 1200);
                u.state().putBoolean("owing", true);
                Fx.sound(e, SoundEvents.BELL_RESONATE, 1f, 1.4f);
                Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(e), 6, 0.4);
            }
            @Override public void tick(PowerUser u) {
                if (!u.state().getBoolean("owing") || running(u, "oweAt")) return;
                u.state().putBoolean("owing", false);
                LivingEntity e = u.entity();
                if (!e.isAlive()) return;
                e.setHealth(Math.max(1.0f, e.getHealth() - u.state().getFloat("owe")));
                Fx.sound(e, SoundEvents.BELL_BLOCK, 1f, 0.5f);
                tell(u, "Time's up. The borrowed health goes back.");
            }
        });
        // #283 Gravity Hiccup: every mob within 8 blocks falls UP 4 blocks, floats for a second, then drops. 1 minute cooldown.
        key(A, 283, "gravity_hiccup", ATTACK, 1200, () -> Items.SHULKER_SHELL, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                for (LivingEntity t : foes(u, 8)) {
                    launch(t, new Vec3(0, 0.9, 0));
                    remember(u, "lift", t, u.now() + 12);
                }
                Fx.sound(u.entity(), SoundEvents.SHULKER_SHOOT, 1f, 0.6f);
            }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "lift", null, t -> {
                    if (!t.isAlive()) return;
                    if (t instanceof Player) {
                        if (t instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0, false, false));
                    } else {
                        t.setNoGravity(true);
                        t.getPersistentData().putBoolean(FLOATING, true);
                        launch(t, Vec3.ZERO);
                        remember(u, "float", t, u.now() + 20);
                    }
                });
                forEachRemembered(u, "float", t -> Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, t.position(), 1, 0.3), APowers::land);
            }
            @Override public void stop(PowerUser u) {
                forEachRemembered(u, "float", APowers::land, APowers::land);
                u.state().remove("float");
            }
        });
        // #284 Echo Strike: a shadow of every hit you land comes again 1 second later for half the damage
        always(A, 284, "echo_strike", PASSIVE, () -> Items.ECHO_SHARD, new PowerEffect() {
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (!busy && e.getAmount() > 0) later(u, "echoes", target, e.getAmount() / 2, 20);
            }
            @Override public void tick(PowerUser u) { hitsDue(u, "echoes", ParticleTypes.SCULK_SOUL); }
        });
        // #285 Return to Sender: for 5 s, everything shot or thrown at you turns around and flies back. 1 minute cooldown.
        key(A, 285, "return_to_sender", ESCAPE, 1200, () -> Items.SHIELD, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "rts", 100); Fx.sound(u.entity(), SoundEvents.ILLUSIONER_CAST_SPELL, 1f, 1.2f); }
            @Override public void tick(PowerUser u) {
                if (!running(u, "rts")) return;
                LivingEntity me = u.entity();
                for (Projectile p : u.level().getEntitiesOfClass(Projectile.class, me.getBoundingBox().inflate(5))) {
                    if (p.getOwner() == me) continue;
                    Vec3 v = p.getDeltaMovement();
                    Vec3 toMe = me.position().add(0, me.getBbHeight() / 2, 0).subtract(p.position());
                    if (v.lengthSqr() < 0.04 || v.dot(toMe) <= 0) continue; // not coming at me
                    Entity thrower = p.getOwner();
                    p.setDeltaMovement(v.scale(-1));
                    if (p instanceof AbstractHurtingProjectile f) { f.xPower = -f.xPower; f.yPower = -f.yPower; f.zPower = -f.zPower; }
                    p.setOwner(me);
                    p.hurtMarked = true;
                    if (thrower != null) p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, thrower.getEyePosition());
                    Fx.particles(u.level(), ParticleTypes.ENCHANTED_HIT, p.position(), 6, 0.2);
                }
                if (u.now() % 5 == 0) Fx.particles(u.level(), ParticleTypes.ENCHANT, me.position().add(0, 1, 0), 6, 0.8);
            }
        });
        // #286 Pause Button: freeze one mob completely for 10 s: it can't move, hit or be pushed. 1 minute cooldown.
        key(A, 286, "pause_button", ATTACK, 1200, () -> Items.BARRIER, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity t = target(u, 16);
                if (t == null) { refund(u); return; }
                CompoundTag d = t.getPersistentData();
                d.putDouble("charpowers.px", t.getX());
                d.putDouble("charpowers.py", t.getY());
                d.putDouble("charpowers.pz", t.getZ());
                if (t instanceof Mob m) {
                    if (!d.contains("charpowers.wasNoAi")) d.putBoolean("charpowers.wasNoAi", m.isNoAi());
                    m.setNoAi(true);
                } else {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 6, false, true, true));
                    t.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 250, false, false, false));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4, false, true, true));
                }
                remember(u, "paused", t, u.now() + 200);
                Fx.sound(t, SoundEvents.BEACON_DEACTIVATE, 1f, 1.5f);
            }
            @Override public void tick(PowerUser u) {
                forEachRemembered(u, "paused", t -> {
                    CompoundTag d = t.getPersistentData();
                    t.setPos(d.getDouble("charpowers.px"), d.getDouble("charpowers.py"), d.getDouble("charpowers.pz"));
                    t.setDeltaMovement(Vec3.ZERO);
                    if (u.now() % 10 == 0) Fx.particles(u.level(), ParticleTypes.END_ROD, t.position().add(0, t.getBbHeight() + 0.2, 0), 1, 0.1);
                }, APowers::unpause);
            }
            @Override public void stop(PowerUser u) {
                forEachRemembered(u, "paused", APowers::unpause, t -> {});
                u.state().remove("paused");
            }
        });
        // #287 Gravity Well: a tiny black hole where you look pulls mobs in for 4 s, then pops. 2 minute cooldown.
        key(A, 287, "gravity_well", ATTACK, 2400, () -> Items.CRYING_OBSIDIAN, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                Vec3 at;
                if (u.mob() != null && u.mob().getTarget() != null) at = u.mob().getTarget().position().add(0, 1, 0);
                else {
                    BlockHitResult hit = lookBlock(u, 16);
                    at = hit != null ? hit.getLocation().add(0, 1, 0) : u.entity().getEyePosition().add(u.entity().getViewVector(1).scale(8));
                }
                u.state().putDouble("wx", at.x);
                u.state().putDouble("wy", at.y);
                u.state().putDouble("wz", at.z);
                startFor(u, "well", 80);
                u.state().putBoolean("wellOn", true);
                u.level().playSound(null, at.x, at.y, at.z, SoundEvents.PORTAL_TRIGGER, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.6f);
            }
            @Override public void tick(PowerUser u) {
                if (!u.state().getBoolean("wellOn")) return;
                Vec3 at = new Vec3(u.state().getDouble("wx"), u.state().getDouble("wy"), u.state().getDouble("wz"));
                if (!running(u, "well")) {
                    u.state().putBoolean("wellOn", false);
                    u.level().playSound(null, at.x, at.y, at.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, net.minecraft.sounds.SoundSource.PLAYERS, 2f, 0.5f);
                    Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, at, 60, 1.0);
                    return;
                }
                Fx.particles(u.level(), ParticleTypes.SQUID_INK, at, 3, 0.15);
                Fx.particles(u.level(), ParticleTypes.REVERSE_PORTAL, at, 6, 1.2);
                for (LivingEntity t : u.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(7), t -> isFoe(u, t))) {
                    Vec3 d = at.subtract(t.position().add(0, t.getBbHeight() / 2, 0));
                    if (d.length() < 0.8) { launch(t, Vec3.ZERO); continue; }
                    Vec3 v = t.getDeltaMovement().scale(0.6).add(d.normalize().scale(0.25));
                    launch(t, v);
                    t.fallDistance = 0;
                }
            }
        });
        // #288 Second Chance Arrow: an arrow that misses turns around in the air and tries again once (Arrows)
        always(A, 288, "second_chance_arrow", PASSIVE, () -> Items.SPECTRAL_ARROW, new PowerEffect() {
            @Override public void onShoot(PowerUser u, Projectile p) { if (p instanceof AbstractArrow a) Arrows.track(a, Arrows.SECOND); }
        });
        // #289 Copycat: copy the hidden power of the mob you look at for 1 minute (if it has one). 5 minute cooldown.
        // Press again while you have a copied key power to use it.
        key(A, 289, "copycat", CARRY, 20, () -> Items.PARROT_SPAWN_EGG, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                String copied = u.state().getString("copy");
                if (!copied.isEmpty() && running(u, "copyUntil")) {
                    useLentKey(u, copied);
                    return;
                }
                if (running(u, "copyCd")) { tell(u, "Copycat is resting (5 minutes)."); return; }
                LivingEntity t = Fx.lookTarget(u.entity(), 16);
                String id = t instanceof Mob m ? MobPowers.get(m).orElse(null) : null;
                if (id == null || id.equals("copycat") || id.equals("everything")) { tell(u, "Nothing to copy there."); return; }
                u.state().putString("copy", id);
                startFor(u, "copyUntil", 1200);
                startFor(u, "copyCd", 6000);
                Fx.sound(u.entity(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1.4f);
                tell(u, "You copied " + HiddenPowers.name(id).getString() + " for 1 minute!");
            }
            @Override public List<String> lends(PowerUser u) {
                String copied = u.state().getString("copy");
                return !copied.isEmpty() && running(u, "copyUntil") ? List.of(copied) : List.of();
            }
            @Override public void tick(PowerUser u) {
                String copied = u.state().getString("copy");
                if (copied.isEmpty()) return;
                if (!running(u, "copyUntil")) {
                    endLent(u, copied);
                    u.state().putString("copy", "");
                    hud(u, "copycat", "");
                    tell(u, "Your copy wore off.");
                } else if (u.now() % 20 == 0) {
                    hud(u, "copycat", "Copycat: " + HiddenPowers.name(copied).getString());
                }
            }
            @Override public void stop(PowerUser u) {
                String copied = u.state().getString("copy");
                if (!copied.isEmpty()) endLent(u, copied);
                u.state().putString("copy", "");
                hud(u, "copycat", "");
            }
        });
        // #290 Shed Skin: once a day, a killing hit only hits your old skin: you slip out of it like a snake,
        // and the skin falls over looking just like you
        always(A, 290, "shed_skin", PASSIVE, () -> Items.SCUTE, new PowerEffect() {
            @Override public boolean preventsDeath(PowerUser u, LivingDeathEvent e) {
                if (e.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                        || e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD) || !Fx.daily(u, "skin")) return false;
                LivingEntity me = u.entity();
                LivingEntity skin = copyOf(u, me.position(), "skin", 600);
                if (skin instanceof ArmorStand stand) {
                    CompoundTag t = stand.saveWithoutId(new CompoundTag());
                    CompoundTag pose = new CompoundTag();
                    ListTag head = new ListTag();
                    head.add(net.minecraft.nbt.FloatTag.valueOf(60f));
                    head.add(net.minecraft.nbt.FloatTag.valueOf(0f));
                    head.add(net.minecraft.nbt.FloatTag.valueOf(20f));
                    pose.put("Head", head);
                    t.put("Pose", pose);
                    stand.load(t); // slumped over
                }
                me.setHealth(4.0f);
                me.clearFire();
                me.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                me.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false, true));
                Vec3 away = me.getViewVector(1).multiply(1, 0, 1).normalize().scale(0.6);
                launch(me, new Vec3(away.x, 0.2, away.z));
                Fx.sound(me, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.6f);
                tell(u, "You slipped out of your skin!");
                return true;
            }
            @Override public void tick(PowerUser u) { if (u.now() % 20 == 0) copiesTick(u, false); }
            @Override public void stop(PowerUser u) { copiesTick(u, true); }
        });
        // #291 Ghost Walk: become a ghost for 5 s: walk through mobs and walls, can't hit or be hit. 3 minute cooldown.
        key(A, 291, "ghost_walk", ESCAPE, 3600, () -> Items.PHANTOM_MEMBRANE, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                startFor(u, "ghost", 100);
                u.entity().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
                for (Mob m : u.level().getEntitiesOfClass(Mob.class, u.entity().getBoundingBox().inflate(24), m -> m.getTarget() == u.entity())) m.setTarget(null);
                Fx.sound(u.entity(), SoundEvents.PHANTOM_AMBIENT, 1f, 1.4f);
            }
            @Override public void tick(PowerUser u) {
                if (!running(u, "ghost")) return;
                LivingEntity e = u.entity();
                if (u.now() % 4 == 0) Fx.particles(u.level(), ParticleTypes.SOUL, e.position().add(0, 1, 0), 2, 0.3);
                if (!e.horizontalCollision) return;
                // pushing into a wall: come out on the other side (walls up to 3 blocks thick)
                Vec3 dir = DPowers.awayOrForward(u).multiply(1, 0, 1);
                if (dir.lengthSqr() < 1.0E-4) return;
                dir = dir.normalize();
                for (double d = 1.0; d <= 4.0; d += 0.5) {
                    Vec3 at = e.position().add(dir.scale(d));
                    if (fits(e, at)) { teleport(e, at); return; }
                }
            }
            @Override public void onAttacked(PowerUser u, LivingAttackEvent e) {
                if (running(u, "ghost") && !e.getSource().is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                        && !e.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) e.setCanceled(true);
            }
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) { if (running(u, "ghost")) e.setAmount(0); }
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) { if (running(u, "ghost")) e.setCanceled(true); }
        });
        // #292 Split Second: for 3 s every hit you land happens twice. 2 minute cooldown.
        key(A, 292, "split_second", ATTACK, 2400, () -> Items.CLOCK, new PowerEffect() {
            @Override public void onKey(PowerUser u) { startFor(u, "split", 60); Fx.sound(u.entity(), SoundEvents.BEACON_POWER_SELECT, 1f, 2f); }
            @Override public void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {
                if (!busy && running(u, "split") && e.getAmount() > 0) later(u, "splits", target, e.getAmount(), 4);
            }
            @Override public void tick(PowerUser u) { hitsDue(u, "splits", ParticleTypes.CRIT); }
        });
        // #293 Magnet Hands: yank the item out of a mob's hands into yours, and pull in every dropped item nearby. 1 minute cooldown.
        key(A, 293, "magnet_hands", CARRY, 1200, () -> Items.IRON_INGOT, u -> {
            LivingEntity me = u.entity();
            boolean got = false;
            LivingEntity t = target(u, 12);
            if (t instanceof Mob m && !m.getMainHandItem().isEmpty()) {
                give(u, m.getMainHandItem().copy());
                m.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                got = true;
            }
            for (ItemEntity it : u.level().getEntitiesOfClass(ItemEntity.class, me.getBoundingBox().inflate(10))) {
                it.setPos(me.getX(), me.getY() + 0.5, me.getZ());
                it.setNoPickUpDelay();
                got = true;
            }
            if (!got) { refund(u); return; }
            Fx.sound(me, SoundEvents.ITEM_PICKUP, 1f, 0.6f);
        });
        // #294 Mirage: 4 copies of you appear around you; mobs can't tell which is real for 10 s. 2 minute cooldown.
        key(A, 294, "mirage", ESCAPE, 2400, () -> Items.PAINTING, new PowerEffect() {
            @Override public void onKey(PowerUser u) {
                LivingEntity e = u.entity();
                int made = 0;
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 2 * i + e.getRandom().nextDouble() * 0.5;
                    Vec3 at = e.position().add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                    if (!fits(e, at)) continue;
                    if (copyOf(u, at, "mirage", 200) != null) made++;
                }
                if (made == 0) { refund(u); return; }
                startFor(u, "mirage", 200);
                Fx.particles(u.level(), ParticleTypes.POOF, e.position().add(0, 1, 0), 20, 1.0);
                Fx.sound(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 0.8f);
            }
            @Override public void tick(PowerUser u) {
                if (u.now() % 10 == 0) copiesTick(u, false);
                if (!running(u, "mirage") || u.now() % 40 != 0) return;
                List<Entity> fakes = copies(u, "mirage");
                for (Mob m : u.level().getEntitiesOfClass(Mob.class, u.entity().getBoundingBox().inflate(16), m -> !isMine(u, m))) {
                    LivingEntity t = m.getTarget();
                    boolean onUs = t == u.entity() || (t != null && fakes.contains(t));
                    if (!onUs) continue;
                    int pick = m.getRandom().nextInt(fakes.size() + 1);
                    m.setTarget(pick == fakes.size() ? u.entity() : (LivingEntity) fakes.get(pick));
                }
            }
            @Override public void onTargeted(PowerUser u, LivingChangeTargetEvent e) {
                if (!running(u, "mirage")) return;
                List<Entity> fakes = copies(u, "mirage");
                if (fakes.isEmpty() || e.getEntity().getRandom().nextInt(5) == 0) return;
                e.setNewTarget((LivingEntity) fakes.get(e.getEntity().getRandom().nextInt(fakes.size())));
            }
            @Override public void stop(PowerUser u) { copiesTick(u, true); }
        });
        // #295 Life Bank: healing past full is saved (up to 10 hearts) and given back by itself when you drop under 3 hearts
        always(A, 295, "life_bank", PASSIVE, () -> Items.GOLD_BLOCK, new PowerEffect() {
            @Override public void onHeal(PowerUser u, LivingHealEvent e) {
                LivingEntity le = u.entity();
                float extra = le.getHealth() + e.getAmount() - le.getMaxHealth();
                if (extra > 0) u.state().putFloat("bank", Math.min(20, u.state().getFloat("bank") + extra));
            }
            @Override public void tick(PowerUser u) {
                LivingEntity le = u.entity();
                float bank = u.state().getFloat("bank");
                if (bank > 0 && le.isAlive() && le.getHealth() < 6.0f) {
                    float give = Math.min(bank, le.getMaxHealth() - le.getHealth());
                    le.setHealth(le.getHealth() + give); // straight in, so it isn't counted as healing again
                    u.state().putFloat("bank", bank - give);
                    Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(le), 4, 0.4);
                }
                if (u.now() % 20 == 0) {
                    float b = u.state().getFloat("bank");
                    hud(u, "lifebank", b >= 1 ? "Life Bank: " + Math.round(b / 2) + " hearts" : "");
                }
            }
            @Override public void stop(PowerUser u) { hud(u, "lifebank", ""); }
        });
    }

    // --- building blocks ---

    static final String FLOATING = "charpowers.floating";

    static void land(Entity t) {
        t.setNoGravity(false);
        t.getPersistentData().remove(FLOATING);
    }

    static void unpause(Entity t) {
        CompoundTag d = t.getPersistentData();
        if (t instanceof Mob m && d.contains("charpowers.wasNoAi")) {
            m.setNoAi(d.getBoolean("charpowers.wasNoAi"));
            d.remove("charpowers.wasNoAi");
        }
        if (t instanceof LivingEntity le) {
            MobEffectInstance j = le.getEffect(MobEffects.JUMP);
            if (j != null && j.getAmplifier() == 250) le.removeEffect(MobEffects.JUMP);
        }
    }

    // A hit that comes again later (Echo Strike, Split Second): kept in the power's state until it's due.
    static void later(PowerUser u, String list, LivingEntity target, float amount, int ticks) {
        ListTag l = u.state().getList(list, Tag.TAG_COMPOUND);
        if (l.size() > 20) return;
        CompoundTag h = new CompoundTag();
        h.putUUID("t", target.getUUID());
        h.putFloat("a", amount);
        h.putLong("at", u.now() + ticks);
        l.add(h);
        u.state().put(list, l);
    }

    static void hitsDue(PowerUser u, String list, net.minecraft.core.particles.ParticleOptions look) {
        if (!u.state().contains(list)) return;
        ListTag l = u.state().getList(list, Tag.TAG_COMPOUND);
        ListTag keep = new ListTag();
        for (int i = 0; i < l.size(); i++) {
            CompoundTag h = l.getCompound(i);
            if (u.now() < h.getLong("at")) { keep.add(h); continue; }
            if (!(u.level().getEntity(h.getUUID("t")) instanceof LivingEntity t) || !t.isAlive()) continue;
            DamageSource src = u.player() != null ? u.entity().damageSources().playerAttack(u.player()) : u.entity().damageSources().mobAttack(u.entity());
            busy = true;
            try {
                t.invulnerableTime = 0;
                t.hurt(src, h.getFloat("a"));
            } finally {
                busy = false;
            }
            Fx.particles(u.level(), look, t.getEyePosition(), 6, 0.3);
        }
        u.state().put(list, keep);
    }

    // Uses a key power that another power is lending you right now (Copycat, Everything).
    static void useLentKey(PowerUser host, String id) {
        PowerDef def = HiddenPowers.def(id).orElse(null);
        if (def == null || def.kind() != Kind.KEY) { tell(host, HiddenPowers.name(id).getString() + " works by itself, no key needed."); return; }
        PowerUser lent = host.lent(id);
        if (MobPowerTicker.cooledDown(lent, def)) HiddenPowers.effect(id).onKey(lent);
    }

    // A lent power ends: let it tidy up, then forget its saved space.
    static void endLent(PowerUser host, String id) {
        try {
            HiddenPowers.effect(id).stop(host.lent(id));
        } finally {
            host.state().remove("lent:" + id);
        }
    }

    private APowers() {}
}
