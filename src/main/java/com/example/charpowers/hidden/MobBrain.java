package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.hidden.fx.PowerUser;
import com.example.charpowers.hidden.model.MobMoves;
import com.example.charpowers.hidden.ranks.RankEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;

// What a mob does with a power that was written for players: the same move a smart mob would make with it, at the moment it helps.
// Only runs for the few mobs that hold such a power, and only looks around when it could act.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class MobBrain {
    private static final String WAIT = "brainWait";
    private static final int HEAL_GAP = 300, LEAP_GAP = 100, THROW_GAP = 120, DISARM_GAP = 900, RALLY_GAP = 400, BLINK_GAP = 400, FROST_GAP = 300, SENSE_GAP = 40;
    private static final Map<String, Item> DROPS = Map.ofEntries(Map.entry("chain_miner", Items.RAW_IRON), Map.entry("lucky_miner", Items.COAL), Map.entry("fortune_finger", Items.LAPIS_LAZULI),
            Map.entry("glass_hands", Items.GLASS), Map.entry("timber", Items.OAK_LOG), Map.entry("seed_finder", Items.WHEAT_SEEDS), Map.entry("apple_luck", Items.APPLE),
            Map.entry("sapling_luck", Items.OAK_SAPLING), Map.entry("forager", Items.SWEET_BERRIES), Map.entry("lucky_clover", Items.GREEN_DYE), Map.entry("treasure_fisher", Items.NAUTILUS_SHELL),
            Map.entry("fish_whisperer", Items.COD), Map.entry("bare_hand_fisher", Items.COD), Map.entry("quick_shears", Items.WHITE_WOOL), Map.entry("wool_grower", Items.WHITE_WOOL),
            Map.entry("silkworm", Items.STRING), Map.entry("copper_polish", Items.COPPER_INGOT), Map.entry("pocket_table", Items.OAK_PLANKS));
    private static final Map<String, String> LINES = Map.of("echo", "...echo ...echo ...echo", "snail_mail", "This message was sent 5 minutes ago.", "sticky_note", "Note to self: get them.",
            "rumbly_tummy", "*stomach growls*", "paper_cut", "Ow.", "microwave_ding", "Ding!");

    // A few times a second is plenty for a mob to change its mind.
    static void tick(Mob m, PowerUser u, String id) {
        if (HiddenConfig.MOBS_USE_CARRIED.get() && m.tickCount % 5 == 0) act(m, u, id);
    }

    public static void act(Mob m, PowerUser u, String id) {
        LivingEntity foe = foe(m);
        double far = foe == null ? Double.MAX_VALUE : foe.distanceTo(m);
        switch (MobMoves.of(id)) {
            case LEAP -> {
                if (foe != null && m.onGround() && far >= 3 && far <= 10 && m.hasLineOfSight(foe) && ready(u, LEAP_GAP)) leap(m, foe, far);
            }
            case CLIMB -> {
                if (foe != null && m.horizontalCollision && foe.getY() > m.getY() + 0.5) {
                    Vec3 v = m.getDeltaMovement();
                    m.setDeltaMovement(v.x, 0.42, v.z);
                    m.fallDistance = 0;
                }
            }
            case SOFT_FALL -> {
                if (!m.onGround() && m.getDeltaMovement().y < -0.2) m.addEffect(quiet(MobEffects.SLOW_FALLING));
            }
            case FLOAT -> {
                if (m.isInWater() || m.isInLava()) {
                    Vec3 v = m.getDeltaMovement();
                    m.setDeltaMovement(v.x * 1.1, Math.max(v.y, 0.25), v.z * 1.1);
                    if (m.isInLava()) m.addEffect(quiet(MobEffects.FIRE_RESISTANCE));
                }
            }
            case RUN -> {
                if (foe != null) m.addEffect(quiet(MobEffects.MOVEMENT_SPEED));
            }
            case SENSE -> sense(m, u, foe);
            case STALK -> {
                if (foe != null && far > 5) m.addEffect(quiet(MobEffects.INVISIBILITY));
                else m.removeEffect(MobEffects.INVISIBILITY);
            }
            case HEAL -> {
                if (m.getHealth() < m.getMaxHealth() * 0.7f && ready(u, HEAL_GAP)) {
                    m.heal(4f);
                    Fx.sound(m, SoundEvents.GENERIC_EAT, 1f, 1f);
                    Fx.particles(u.level(), ParticleTypes.HEART, Fx.head(m), 4, 0.3);
                }
            }
            case THROW -> {
                if (foe != null && far >= 3 && far <= 12 && m.hasLineOfSight(foe) && ready(u, THROW_GAP)) fling(m, u, foe, id, far);
            }
            case DISARM -> {
                if (foe instanceof ServerPlayer p && far <= 3 && !p.getAbilities().instabuild && !p.getMainHandItem().isEmpty() && ready(u, DISARM_GAP)) knockOut(m, p);
            }
            case RALLY -> {
                if (foe != null && ready(u, RALLY_GAP)) rally(m, u, foe);
            }
            case BLINK -> {
                if (m.getHealth() < m.getMaxHealth() * 0.5f && ready(u, BLINK_GAP)) blink(m, u);
            }
            case FROST -> {
                if (foe != null && far <= 6 && m.hasLineOfSight(foe) && ready(u, FROST_GAP)) {
                    foe.setTicksFrozen(Math.max(foe.getTicksFrozen(), 160));
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    Fx.sound(m, SoundEvents.POWDER_SNOW_STEP, 1f, 0.7f);
                    Fx.particles(u.level(), ParticleTypes.SNOWFLAKE, foe.getEyePosition(), 16, 0.4);
                }
            }
            case GATHER -> {
                if (m instanceof Enemy && !m.canPickUpLoot()) m.setCanPickUpLoot(true);
            }
            case SHOUT -> MobPowerTicker.randomUse(m, u, () -> shout(m, id));
        }
    }

    // Who it's fighting: what it has chosen, or whoever just hit it. So even a cow can answer back.
    @Nullable
    private static LivingEntity foe(Mob m) {
        LivingEntity t = m.getTarget();
        if (t == null) t = m.getLastHurtByMob();
        return t != null && t.isAlive() && t.level() == m.level() ? t : null;
    }

    private static boolean ready(PowerUser u, int gap) {
        if (u.now() < u.state().getLong(WAIT)) return false;
        u.state().putLong(WAIT, u.now() + gap);
        return true;
    }

    private static MobEffectInstance quiet(net.minecraft.world.effect.MobEffect effect) {
        return new MobEffectInstance(effect, 40, 0, false, false);
    }

    private static void leap(Mob m, LivingEntity foe, double far) {
        Vec3 flat = foe.position().subtract(m.position()).multiply(1, 0, 1).normalize().scale(Math.min(1.2, far * 0.16));
        m.setDeltaMovement(flat.x, 0.5, flat.z);
        m.fallDistance = 0;
        Fx.sound(m, SoundEvents.SLIME_JUMP, 1f, 1.2f);
    }

    // It knows where you are through walls, and you hear it notice.
    private static void sense(Mob m, PowerUser u, @Nullable LivingEntity foe) {
        if (foe != null || !(m instanceof Enemy) || !ready(u, SENSE_GAP)) return;
        Player p = Fx.nearestPlayer(m, 24);
        if (p == null || p.getAbilities().instabuild || p.isSpectator()) return;
        m.setTarget(p);
        Fx.sound(m, SoundEvents.SCULK_CLICKING, 1f, 1.4f);
    }

    private static void fling(Mob m, PowerUser u, LivingEntity foe, String id, double far) {
        Snowball shot = new Snowball(u.level(), m);
        shot.setItem(new ItemStack(HiddenPowers.icon(id)));
        shot.addTag(RankEvents.SLING);
        Vec3 to = foe.getEyePosition().subtract(m.getEyePosition());
        shot.shoot(to.x, to.y + far * 0.1, to.z, 1.3f, 2f);
        u.level().addFreshEntity(shot);
        Fx.sound(m, SoundEvents.WITCH_THROW, 1f, 1.2f);
    }

    // Knocked out of your hand onto the ground, never stolen: you can pick it straight back up.
    private static void knockOut(Mob m, ServerPlayer p) {
        ItemStack held = p.getMainHandItem().copy();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.drop(held, true);
        p.displayClientMessage(Component.literal("").append(m.getType().getDescription()).append(" knocked it out of your hand!"), true);
        Fx.sound(p, SoundEvents.ITEM_BREAK, 0.6f, 1.6f);
    }

    private static void rally(Mob m, PowerUser u, LivingEntity foe) {
        int called = 0;
        for (Mob other : u.level().getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(12), o -> o != m && o != foe && o.getTarget() == null && (o instanceof Enemy || o.getClass() == m.getClass()))) {
            other.setTarget(foe);
            called++;
        }
        if (called > 0) Fx.sound(m, SoundEvents.VINDICATOR_CELEBRATE, 1f, 1.2f);
    }

    private static void blink(Mob m, PowerUser u) {
        Vec3 from = m.position();
        for (int tries = 0; tries < 8; tries++) {
            double x = from.x + (m.getRandom().nextDouble() - 0.5) * 24, z = from.z + (m.getRandom().nextDouble() - 0.5) * 24;
            if (m.randomTeleport(x, from.y, z, true)) {
                m.heal(2f);
                Fx.particles(u.level(), ParticleTypes.PORTAL, from.add(0, 1, 0), 24, 0.4);
                return;
            }
        }
    }

    private static void shout(Mob m, String id) {
        if ("microwave_ding".equals(id)) Fx.sound(m, SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 2f);
        if ("rumbly_tummy".equals(id)) Fx.sound(m, SoundEvents.GENERIC_EAT, 1f, 0.5f);
        String line = "narrator".equals(id) ? com.example.charpowers.hidden.fx.TextLists.random("narrator", com.example.charpowers.hidden.fx.TextLists.NARRATOR, m.getRandom()) : LINES.get(id);
        if (line != null) Fx.mobSay(m, line);
    }

    // The ones that gather things: it was carrying some when you got it.
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !(e.getSource().getEntity() instanceof Player) || !HiddenConfig.MOBS_USE_CARRIED.get()) return;
        Item item = MobPowers.get(m).map(DROPS::get).orElse(null);
        if (item != null) e.getDrops().add(new ItemEntity(m.level(), m.getX(), m.getY(), m.getZ(), new ItemStack(item)));
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent e) {
        if (!(e.getEntity() instanceof Mob m) || !HiddenConfig.MOBS_USE_CARRIED.get()) return;
        String id = MobPowers.get(m).orElse("");
        if ("double_xp".equals(id) || "xp_magnet".equals(id)) e.setDroppedExperience(e.getDroppedExperience() * 2);
    }

    private MobBrain() {}
}
