package com.example.charpowers.necromancy;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.necromancy.model.UndeadRules;
import com.example.charpowers.own.OwnPower;
import com.example.charpowers.sound.PowerSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AbilityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Summon the undead and command them to do your misdeeds. The summons themselves are looked after in Undead.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class Necromancy extends OwnPower {
    public static final Necromancy POWER = new Necromancy();
    private static final String ZOMBIES = "zombiesKilled";
    private static final List<EntityType<? extends Mob>> RIOT_KINDS = List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
            EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.ZOMBIFIED_PIGLIN);

    private static final Map<UUID, Body> BODIES = new HashMap<>();
    private static final List<Riot> RIOTS = new ArrayList<>();
    private static final Set<UUID> SPOILED_WARDENS = new HashSet<>();

    // A body you could raise right now.
    private record Body(EntityType<?> type, BlockPos at, long until, UUID player, String name, ItemStack[] gear) {}

    private static final class Riot {
        final UUID owner;
        final int level;
        int left = Setting.UNDEAD_RIOT.i();

        Riot(UUID owner, int level) {
            this.owner = owner;
            this.level = level;
        }
    }

    private Necromancy() {
        super(Mastery.NECROMANCY, Items.ZOMBIE_HEAD);
    }

    @Override
    public String unlockMessage() { return "A hundred zombies at night, in nothing but your skin. The hundredth one got back up, and looked at you for orders."; }

    @Override
    public String awakenedMessage() { return "You never touched it. Now even the dead players answer you."; }

    @Override
    public void everySecond(ServerPlayer p) {
        if (!Undead.of(p.getUUID()).isEmpty()) Mastery.add(p, id, 1);
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        int level = mastery(p);
        switch (ability) {
            case "summon_zombies" -> summon(p, EntityType.ZOMBIE, UndeadRules.ZOMBIES_A_SUMMON, level);
            case "summon_skeletons" -> summon(p, EntityType.SKELETON, UndeadRules.SKELETONS_A_SUMMON, level);
            case "undead_explosion" -> Undead.explodeAll(p.getUUID());
            case "chosen_revival" -> revive(p, level);
            case "undead_riot" -> {
                RIOTS.add(new Riot(p.getUUID(), level));
                Fx.sound(p, PowerSounds.RAISE_RUMBLE, 1.4f, 0.75f);
                whisper(p, "They're coming.");
            }
            case "point_undead" -> {
                LivingEntity target = Fx.lookTarget(p, 64);
                if (target == null) whisper(p, "Look at who they should go for.");
                else Undead.pointAt(p.getUUID(), target);
            }
            default -> {}
        }
    }

    private static void summon(ServerPlayer p, EntityType<? extends Mob> type, int wanted, int level) {
        int count = UndeadRules.room(Undead.ordinaryCount(p.getUUID()), wanted);
        if (count == 0) {
            whisper(p, "That's as many as you can hold together.");
            return;
        }
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            spawn(p, type, p.getX() + Math.cos(angle) * 2.5, p.getY(), p.getZ() + Math.sin(angle) * 2.5, level, 0, true);
        }
        p.level().playSound(null, p.blockPosition(), PowerSounds.RAISE_RUMBLE.get(), SoundSource.PLAYERS, 0.8f, 1f);
    }

    private static Mob spawn(ServerPlayer p, EntityType<? extends Mob> type, double x, double y, double z, int level, long until, boolean gear) {
        ServerLevel world = p.serverLevel();
        Mob m = type.create(world);
        if (m == null) return null;
        m.moveTo(x, y, z, world.random.nextFloat() * 360f, 0f);
        if (m instanceof Zombie zombie) zombie.setBaby(false);
        Undead.claim(m, p, level, until, false);
        dress(m, level, gear);
        world.addFreshEntity(m);
        return m;
    }

    private static void dress(Mob m, int level, boolean gear) {
        for (EquipmentSlot slot : EquipmentSlot.values()) m.setDropChance(slot, 0f);
        if (m.getType() == EntityType.WITHER_SKELETON) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        else if (m instanceof AbstractSkeleton) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        else if (m.getType() == EntityType.ZOMBIFIED_PIGLIN) m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        if (gear && level >= UndeadRules.GEAR_LEVEL && m instanceof Zombie) {
            m.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            m.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            m.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        }
    }

    // Chosen Revival: only if a body's waiting. Only one at a time (two at mastery 50): the oldest lies back down.
    private void revive(ServerPlayer p, int level) {
        Body body = BODIES.get(p.getUUID());
        if (body == null || p.level().getGameTime() > body.until()) {
            BODIES.remove(p.getUUID());
            whisper(p, "No body is answering.");
            return;
        }
        BODIES.remove(p.getUUID());
        ServerLevel world = p.serverLevel();
        List<Mob> kept = Undead.revived(p.getUUID());
        for (int i = 0; i <= kept.size() - UndeadRules.revivalsAtOnce(level); i++) kept.get(i).discard();
        Mob m;
        if (body.player() != null) {
            m = RevivedMob.of(world, body.player(), body.name(), body.gear(), UndeadRules.health(level) * 2);
        } else {
            m = body.type().create(world) instanceof Mob made ? made : null;
        }
        if (m == null) return;
        m.moveTo(body.at().getX() + 0.5, body.at().getY(), body.at().getZ() + 0.5, p.getYRot(), 0f);
        Undead.claim(m, p, level, 0, true);
        world.addFreshEntity(m);
        world.playSound(null, body.at(), PowerSounds.RAISE_RUMBLE.get(), SoundSource.PLAYERS, 1f, 1.1f);
        AbilityInstance key = AbilityUtil.getInstance(p, id, "chosen_revival");
        if (key != null) key.startCooldown(p, UndeadRules.REVIVE_COOLDOWN);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || RIOTS.isEmpty() || e.getServer().getTickCount() % UndeadRules.RIOT_SPAWN_EVERY != 0) return;
        Iterator<Riot> it = RIOTS.iterator();
        while (it.hasNext()) {
            Riot riot = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(riot.owner);
            if (p == null || riot.left <= 0) {
                it.remove();
                continue;
            }
            riot.left--;
            BlockPos spot = spotNear(p);
            if (spot == null) continue;
            EntityType<? extends Mob> kind = RIOT_KINDS.get(riot.left % RIOT_KINDS.size());
            long until = p.level().getGameTime() + UndeadRules.riotTicks(riot.level);
            spawn(p, kind, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, riot.level, until, false);
        }
    }

    private static BlockPos spotNear(ServerPlayer p) {
        ServerLevel world = p.serverLevel();
        for (int tries = 0; tries < 6; tries++) {
            int r = UndeadRules.RIOT_RADIUS;
            int x = p.getBlockX() + world.random.nextInt(2 * r + 1) - r, z = p.getBlockZ() + world.random.nextInt(2 * r + 1) - r;
            if (!world.hasChunk(x >> 4, z >> 4)) continue;
            for (int y = p.getBlockY() + 8; y >= p.getBlockY() - 8; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (world.getBlockState(pos.below()).isSolidRender(world, pos.below()) && world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()) return pos;
            }
        }
        return null;
    }

    // Hit someone and your undead go for them too.
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity hurt = e.getEntity();
        if (hurt instanceof Warden warden && !SPOILED_WARDENS.contains(warden.getUUID())) {
            if (!(e.getSource().getEntity() instanceof Mob m && Undead.isUndead(m))) SPOILED_WARDENS.add(warden.getUUID());
        }
        if (e.getSource().getEntity() instanceof ServerPlayer p && !Undead.of(p.getUUID()).isEmpty() && !Undead.isUndead(hurt)) {
            Undead.pointAt(p.getUUID(), hurt);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof Warden warden) wardenDied(e, warden);
        if (e.getSource().getEntity() instanceof ServerPlayer p && !Undead.isUndead(dead)) {
            if (dead instanceof Zombie) countZombie(p);
            if (dead instanceof Mob mob && POWER.owns(p) && !mob.getType().is(Tags.EntityTypes.BOSSES) && p.getRandom().nextDouble() < Setting.UNDEAD_REVIVAL.d()) {
                BODIES.put(p.getUUID(), new Body(mob.getType(), mob.blockPosition(), p.level().getGameTime() + UndeadRules.REVIVAL_WINDOW, null, null, null));
                whisper(p, "Its body twitches.");
            }
        }
        if (dead instanceof ServerPlayer victim) playerDied(victim);
    }

    // How to get it: 100 zombies, at night, wearing no armour.
    private static void countZombie(ServerPlayer p) {
        if (POWER.owns(p) || !p.level().isNight()) return;
        for (ItemStack armour : p.getArmorSlots()) if (!armour.isEmpty()) return;
        int count = POWER.data(p).getInt(ZOMBIES) + 1;
        POWER.data(p).putInt(ZOMBIES, count);
        if (count >= UndeadRules.ZOMBIES_NEEDED) POWER.unlock(p);
    }

    // The quest: a Warden, at night, and only your summons land a hit.
    private static void wardenDied(LivingDeathEvent e, Warden warden) {
        boolean spoiled = SPOILED_WARDENS.remove(warden.getUUID());
        if (spoiled || !warden.level().isNight() || !(e.getSource().getEntity() instanceof Mob m) || !Undead.isUndead(m)) return;
        UUID owner = Undead.ownerOf(m);
        ServerPlayer p = owner == null || warden.getServer() == null ? null : warden.getServer().getPlayerList().getPlayer(owner);
        if (p != null && POWER.owns(p) && POWER.questReady(p)) POWER.awaken(p);
    }

    // Awakened: a dead player's body can be raised too, by any awakened necromancer near it. Copies, so nothing is lost.
    private static void playerDied(ServerPlayer victim) {
        for (ServerPlayer p : victim.serverLevel().players()) {
            if (p == victim || !POWER.owns(p) || !POWER.awakened(p) || p.distanceToSqr(victim) > 32 * 32) continue;
            ItemStack[] gear = new ItemStack[EquipmentSlot.values().length];
            for (EquipmentSlot slot : EquipmentSlot.values()) gear[slot.ordinal()] = victim.getItemBySlot(slot).copy();
            BODIES.put(p.getUUID(), new Body(EntityType.PLAYER, victim.blockPosition(), p.level().getGameTime() + UndeadRules.REVIVAL_WINDOW, victim.getUUID(), victim.getGameProfile().getName(), gear));
            whisper(p, victim.getGameProfile().getName() + "'s body twitches.");
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        BODIES.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        BODIES.clear();
        RIOTS.clear();
        SPOILED_WARDENS.clear();
    }
}
