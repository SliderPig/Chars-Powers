package com.example.charpowers.items;

import com.example.charpowers.CharPowers;
import com.example.charpowers.items.model.ComboRules;
import com.example.charpowers.items.model.WeaponStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// What the Golden King's Long Sword does on the server: decides each move of the combo from your clicks, lands its hits at the right moment,
// and sends the golden shockwave along the ground. The hits follow the same rules as a normal sword (teams, pets, protections through hurt()).
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class LongSwordMoves {
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1f, 0.78f, 0.18f), 1.1f);
    private static final double REACH = 4.4, SHOCK_LENGTH = 9, THRUST_LENGTH = 6.5;
    private static final Map<UUID, Run> RUNS = new HashMap<>();

    private static final class Run {
        final ComboRules.State state = new ComboRules.State();
        int hitTick = -1, clicked = -1;
        // the shockwave spreading along the ground after a slam
        Vec3 shockFrom, shockDir;
        double shockAt;
        final Set<Integer> shockHit = new HashSet<>();
        boolean lunging;
    }

    private LongSwordMoves() {}

    // A click reached the server.
    public static void press(ServerPlayer p, int clicked) {
        if (!(p.getMainHandItem().getItem() instanceof GoldenLongSword) || p.isSpectator()) return;
        Run run = RUNS.computeIfAbsent(p.getUUID(), u -> new Run());
        if (clicked >= 0) run.clicked = clicked;
        ComboRules.Outcome outcome = ComboRules.press(run.state, p.serverLevel().getGameTime());
        if (outcome == ComboRules.Outcome.STARTED) begin(p, run);
    }

    // Just for the tests: where a combo stands.
    public static int stepOf(ServerPlayer p) {
        Run run = RUNS.get(p.getUUID());
        return run == null ? -1 : run.state.step;
    }

    private static void begin(ServerPlayer p, Run run) {
        int step = run.state.step;
        ServerLevel level = p.serverLevel();
        run.hitTick = ComboRules.hitAt(step);
        run.lunging = false;
        ItemNet.toWatchers(p, new ItemNet.Show(p.getId(), WeaponStyle.GOLD.ordinal(), step));
        float pitch = switch (step) {
            case ComboRules.HORIZONTAL -> 1.0f;
            case ComboRules.REVERSE -> 1.15f;
            case ComboRules.OVERHEAD -> 0.8f;
            default -> 0.7f;
        };
        level.playSound(null, p.getX(), p.getY(), p.getZ(), CharItems.sound("gold_swing"), SoundSource.PLAYERS, 0.9f, pitch);
        if (step == ComboRules.FINISHER) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), CharItems.sound("gold_thrust"), SoundSource.PLAYERS, 1f, 1f);
            // a short leap forward with the thrust, and the wait shows on the item
            Vec3 look = p.getLookAngle().multiply(1, 0, 1);
            look = look.lengthSqr() < 1e-4 ? Vec3.ZERO : look.normalize();
            p.setDeltaMovement(look.x * 1.05, 0.28, look.z * 1.05);
            p.hurtMarked = true;
            run.lunging = true;
            p.getCooldowns().addCooldown(p.getMainHandItem().getItem(), ComboRules.length(ComboRules.FINISHER) + ComboRules.FINISHER_WAIT);
        } else if (step == ComboRules.OVERHEAD) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), CharItems.sound("gold_beat"), SoundSource.PLAYERS, 0.7f, 1f);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || RUNS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Run>> it = RUNS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Run> en = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            tick(p, en.getValue());
        }
    }

    private static void tick(ServerPlayer p, Run run) {
        if (!(p.getMainHandItem().getItem() instanceof GoldenLongSword)) {
            run.hitTick = -1;
            run.shockFrom = null;
            run.state.queued = false;
            return;
        }
        long now = p.serverLevel().getGameTime();
        if (ComboRules.tick(run.state, now) >= 0) begin(p, run);
        if (run.hitTick >= 0 && now - run.state.start >= run.hitTick) {
            int step = run.state.step;
            run.hitTick = -1;
            land(p, run, step);
        }
        if (run.shockFrom != null) spreadShock(p, run);
    }

    private static void land(ServerPlayer p, Run run, int step) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        List<LivingEntity> hits = new ArrayList<>();
        if (step == ComboRules.FINISHER) {
            // straight through everything along the line in front of you
            Vec3 from = p.getEyePosition(), to = from.add(look.scale(THRUST_LENGTH));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.3), e -> Foes.sweepHits(p, e))) {
                if (e.getBoundingBox().inflate(0.9).clip(from, to).isPresent() && p.hasLineOfSight(e)) hits.add(e);
            }
        } else {
            double half = step == ComboRules.OVERHEAD ? 32 : 78;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(REACH + 1), e -> Foes.sweepHits(p, e))) {
                Vec3 d = e.position().subtract(p.position());
                if (Math.abs(d.y) < 3.5 && ComboRules.inArc(d.x, d.z, look.x, look.z, REACH + e.getBbWidth() * 0.5, half) && p.hasLineOfSight(e)) hits.add(e);
            }
        }
        Entity clicked = run.clicked >= 0 ? level.getEntity(run.clicked) : null;
        run.clicked = -1;
        if (clicked instanceof LivingEntity c && !hits.contains(c) && Foes.canHit(p, c) && p.distanceToSqr(c) < (REACH + 2) * (REACH + 2)) hits.add(c);
        ItemStack sword = p.getMainHandItem();
        for (LivingEntity e : hits) hit(p, sword, e, ComboRules.damage(step), step == ComboRules.FINISHER ? 0.9f : 0.35f);
        if (step == ComboRules.OVERHEAD) slam(p, run, level, look);
        if (step == ComboRules.FINISHER) {
            Foes.particles(level, GOLD_DUST, p.getEyePosition().add(look.scale(2.5)), 20, 0.5, 0.05);
            if (!hits.isEmpty()) Foes.particles(level, ParticleTypes.FLASH, hits.get(0).getEyePosition(), 1, 0, 0);
        }
    }

    private static void hit(ServerPlayer p, ItemStack sword, LivingEntity e, float scale, float push) {
        float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) + EnchantmentHelper.getDamageBonus(sword, e.getMobType());
        float damage = base * scale;
        if (!e.hurt(p.damageSources().playerAttack(p), damage)) return;
        p.setLastHurtMob(e);
        int fire = EnchantmentHelper.getFireAspect(p);
        if (fire > 0) e.setSecondsOnFire(fire * 4);
        float pushBy = push + EnchantmentHelper.getKnockbackBonus(p) * 0.5f;
        e.knockback(pushBy, Math.sin(Math.toRadians(p.getYRot())), -Math.cos(Math.toRadians(p.getYRot())));
        EnchantmentHelper.doPostHurtEffects(e, p);
        EnchantmentHelper.doPostDamageEffects(p, e);
        if (!sword.isEmpty()) sword.hurtAndBreak(1, p, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        p.awardStat(Stats.DAMAGE_DEALT, Math.round(damage * 10f));
        p.causeFoodExhaustion(0.1f);
        Foes.particles(p.serverLevel(), GOLD_DUST, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
    }

    private static void slam(ServerPlayer p, Run run, ServerLevel level, Vec3 look) {
        Vec3 flat = look.multiply(1, 0, 1);
        flat = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 at = p.position().add(flat.scale(1.8));
        BlockPos ground = Foes.groundNear(level, BlockPos.containing(at.x, p.getY(), at.z), 1, 4);
        if (ground == null) return; // swung over a drop: no ground, no shockwave
        run.shockFrom = new Vec3(at.x, ground.getY(), at.z);
        run.shockDir = flat;
        run.shockAt = 0;
        run.shockHit.clear();
        level.playSound(null, at.x, ground.getY(), at.z, CharItems.sound("gold_slam"), SoundSource.PLAYERS, 1.1f, 1f);
        ItemNet.toPlayersNear(level, at.x, ground.getY(), at.z, new ItemNet.GroundLine(0, at.x, ground.getY(), at.z, (float) flat.x, (float) flat.z, (float) SHOCK_LENGTH));
    }

    // Half a block ahead of the slam a tick is too slow to look like a shockwave: it covers a block and a half.
    private static void spreadShock(ServerPlayer p, Run run) {
        ServerLevel level = p.serverLevel();
        run.shockAt += 1.5;
        if (run.shockAt > SHOCK_LENGTH) {
            run.shockFrom = null;
            return;
        }
        Vec3 at = run.shockFrom.add(run.shockDir.scale(run.shockAt));
        BlockPos ground = Foes.groundNear(level, BlockPos.containing(at.x, run.shockFrom.y, at.z), 2, 3);
        if (ground == null) {
            run.shockFrom = null;
            return;
        }
        double y = ground.getY();
        ParticleOptions glow = ParticleTypes.END_ROD;
        Foes.particles(level, glow, at.x, y + 0.1, at.z, 3, 0.35, 0.02, 0.35, 0.02);
        Foes.particles(level, GOLD_DUST, at.x, y + 0.2, at.z, 8, 0.5, 0.15, 0.5, 0.02);
        ItemStack sword = p.getMainHandItem();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at.x - 1.3, y - 0.5, at.z - 1.3, at.x + 1.3, y + 2.5, at.z + 1.3), e -> Foes.sweepHits(p, e))) {
            if (run.shockHit.add(e.getId())) hit(p, sword, e, 0.7f, 0.3f);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        RUNS.remove(e.getEntity().getUUID());
    }
}
