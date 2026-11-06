package com.example.charpowers.armour;

import com.example.charpowers.own.Setting;
import com.example.charpowers.CharPowers;
import com.example.charpowers.armour.model.SuitRules;
import com.example.charpowers.armour.model.SuitRules.Stage;
import com.example.charpowers.hidden.HiddenRuntime;
import com.example.charpowers.hidden.fx.Fx;
import com.example.charpowers.mastery.Mastery;
import com.example.charpowers.own.OwnPower;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A suit of armour so strong you're practically invincible, with a battery, and it isn't good for you. One item, worn in the
// chest slot and covering you head to toe. The power comes from putting it on for the first time.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class InvincibleArmour extends OwnPower {
    public static final InvincibleArmour POWER = new InvincibleArmour();
    private static final UUID SPEED_ID = UUID.fromString("7a1d5c3e-62f4-49b8-a0c7-3e9f1d2b8c44");
    private static final AttributeModifier MODULE_SPEED = new AttributeModifier(SPEED_ID, "charpowers speed module", SuitRules.MODULE_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final UUID SLIM_ID = UUID.fromString("3c9e1b7d-48a2-4f6e-b5d0-91a2c7e4f655");
    private static final AttributeModifier SLIM_SLOW = new AttributeModifier(SLIM_ID, "charpowers slim suit", SuitRules.SLIM_RUN_SLOWDOWN, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final String STATION_DIM = "stationDim", STATION_POS = "stationPos";

    private static final Map<UUID, Long> SHIELD_USED = new HashMap<>();
    private static final Map<UUID, Mob> LIFTED = new HashMap<>();
    private static final Map<UUID, Long> NUKES = new HashMap<>();

    private InvincibleArmour() {
        super(Mastery.INVINCIBLE_ARMOUR, Items.NETHERITE_CHESTPLATE);
    }

    @Override
    public boolean watches() { return true; }

    @Override
    public String unlockMessage() { return "The suit closes around you. It fits. It didn't ask."; }

    @Override
    public String awakenedMessage() { return "You lived through a nuke. Crouch and right-click a charging station with the suit, and 4 Vibranium Ingots in your bag, to make it slim."; }

    private static ItemStack chest(Player p) {
        return p.getItemBySlot(EquipmentSlot.CHEST);
    }

    private boolean powered(Player p) {
        return SuitItems.fullSuit(p) && SuitItems.battery(chest(p)) > 0;
    }

    private boolean module(Player p, String name) {
        return powered(p) && SuitItems.modulesWorn(p, SuitRules.moduleSlots(Mastery.levelAnySide(p, id))).contains(name);
    }

    // How to get it: craft the suit and put it all on.
    @Override
    public void watchSecond(ServerPlayer p) {
        if (SuitItems.fullSuit(p)) unlock(p);
    }

    @Override
    public void everySecond(ServerPlayer p) {
        boolean suit = SuitItems.fullSuit(p);
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean fast = suit && module(p, "speed");
            if (fast && speed.getModifier(SPEED_ID) == null) speed.addTransientModifier(MODULE_SPEED);
            if (!fast && speed.getModifier(SPEED_ID) != null) speed.removeModifier(SPEED_ID);
            // the slim suit can slightly run: you're allowed to sprint, just not as fast
            boolean slim = suit && SuitItems.slim(chest(p));
            if (slim && speed.getModifier(SLIM_ID) == null) speed.addTransientModifier(SLIM_SLOW);
            if (!slim && speed.getModifier(SLIM_ID) != null) speed.removeModifier(SLIM_ID);
        }
        if (!suit) return;
        takeOffOtherArmour(p);
        ItemStack chest = chest(p);
        int level = mastery(p);
        if (SuitItems.battery(chest) > 0) Mastery.add(p, id, 1);
        // only written once a minute: changing an item's data every second makes everyone's PC refresh it
        if (p.level().getGameTime() / 20 % SuitRules.TICK_EVERY_SECONDS == 0) {
            SuitItems.setBattery(chest, SuitItems.battery(chest) - SuitRules.drainPerMinute(level));
            SuitItems.setWorn(chest, SuitItems.worn(chest) + SuitRules.TICK_EVERY_SECONDS);
            if (SuitItems.battery(chest) <= 0) tell(p, "The suit's battery is flat. Take it off and charge it.");
        }
        goCrazy(p, SuitRules.stage(SuitItems.worn(chest), level));
        if (questReady(p) && !NUKES.containsKey(p.getUUID()) && p.getRandom().nextInt(Setting.SUIT_NUKE_ONE_IN.i()) == 0) startNuke(p);
    }

    // It covers you head to toe, so nothing else fits over it.
    private static void takeOffOtherArmour(ServerPlayer p) {
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack other = p.getItemBySlot(slot);
            if (other.isEmpty()) continue;
            p.setItemSlot(slot, ItemStack.EMPTY);
            Fx.give(p, other);
        }
    }

    private void goCrazy(ServerPlayer p, Stage stage) {
        if (stage == Stage.FINE || HiddenRuntime.switchedOff(p)) return;
        if (p.getRandom().nextInt(120) == 0) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, false));
        if (SuitRules.atLeast(stage, Stage.WHISPERS) && p.getRandom().nextInt(300) == 0) {
            String line = SuitRules.WHISPERS.get(p.getRandom().nextInt(SuitRules.WHISPERS.size()));
            p.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (SuitRules.atLeast(stage, Stage.FAKE_MOBS) && p.getRandom().nextInt(400) == 0) fakeMob(p);
        if (SuitRules.atLeast(stage, Stage.OWN_MIND) && p.getRandom().nextInt(600) == 0) ownMind(p);
    }

    // A zombie only your PC can see, for 3 seconds. It isn't really there.
    private void fakeMob(ServerPlayer p) {
        Zombie fake = EntityType.ZOMBIE.create(p.level());
        if (fake == null) return;
        Vec3 behind = p.position().subtract(p.getLookAngle().multiply(4, 0, 4));
        fake.moveTo(behind.x, p.getY(), behind.z, p.getYRot(), 0f);
        p.connection.send(new ClientboundAddEntityPacket(fake));
        p.getServer().tell(new TickTask(p.getServer().getTickCount() + 60, () -> p.connection.send(new ClientboundRemoveEntitiesPacket(fake.getId()))));
    }

    // Later on the suit moves or hits something by itself for a moment.
    private void ownMind(ServerPlayer p) {
        LivingEntity near = null;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3), e -> e != p && e.isAlive())) near = e;
        if (near != null) {
            p.attack(near);
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        } else {
            p.setDeltaMovement(p.getRandom().nextGaussian() * 0.6, 0.2, p.getRandom().nextGaussian() * 0.6);
            p.hurtMarked = true;
        }
    }

    @Override
    public void onKey(ServerPlayer p, String ability) {
        switch (ability) {
            case "call_suit" -> callSuit(p);
            case "throw_mob" -> liftOrThrow(p);
            default -> {}
        }
    }

    void rememberStation(Player player, Level level, BlockPos pos) {
        if (!(player instanceof ServerPlayer p)) return;
        CompoundTag d = data(p);
        d.putString(STATION_DIM, level.dimension().location().toString());
        d.putLong(STATION_POS, pos.asLong());
        SuitNet.station((ServerPlayer) player, level.dimension().location().toString(), pos.asLong());
    }

    // Tells your PC where the station is (the visor points to it).
    void sendStation(ServerPlayer p) {
        CompoundTag d = data(p);
        if (d.contains(STATION_POS)) SuitNet.station(p, d.getString(STATION_DIM), d.getLong(STATION_POS));
    }

    // Ability 1: the suit comes to you and goes on: out of your bag, or off its station. Near at first, anywhere in this
    // world at 100.
    private void callSuit(ServerPlayer p) {
        if (SuitFlight.busy(p.getUUID())) return;
        if (SuitItems.fullSuit(p)) {
            if (p.isShiftKeyDown()) sendAway(p);
            else whisper(p, "You're wearing it. Crouch and press again to send it away.");
            return;
        }
        Vec3 start;
        ItemStack suit = fromBag(p);
        if (!suit.isEmpty()) {
            // out of your bag: it drops in from the sky
            double angle = p.getRandom().nextDouble() * Math.PI * 2;
            start = p.position().add(Math.cos(angle) * 12, 28, Math.sin(angle) * 12);
        } else {
            BlockPos at = knownStation(p);
            suit = fromStation(p);
            if (suit.isEmpty() || at == null) return;
            start = Vec3.atBottomCenterOf(at).add(0, 1.1, 0);
            // a very far station: it appears out of the distance instead of crossing the whole map
            Vec3 away = start.subtract(p.position());
            if (away.length() > 90) start = p.position().add(away.normalize().scale(90)).add(0, 30, 0);
        }
        SuitFlight.flyIn(p, suit, start);
    }

    // The suit has landed on you: it goes on.
    static void arrived(ServerPlayer p, ItemStack suit) {
        if (!p.isAlive()) {
            Fx.give(p, suit);
            return;
        }
        ItemStack was = chest(p);
        p.setItemSlot(EquipmentSlot.CHEST, suit);
        if (!was.isEmpty()) Fx.give(p, was);
        takeOffOtherArmour(p);
        p.level().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1f, 0.7f);
        com.example.charpowers.anim.AnimNet.play(p, "suit_stomp");
    }

    // Crouch and press while wearing it: the suit comes off and flies back to its station, or up into the sky and then your bag.
    private void sendAway(ServerPlayer p) {
        BlockPos at = knownStation(p);
        boolean usable = at != null && p.serverLevel().isLoaded(at) && p.serverLevel().getBlockEntity(at) instanceof ChargingStation.Entity st && st.chest.isEmpty();
        ItemStack suit = chest(p);
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        SuitFlight.flyOut(p, suit, usable ? at : null);
    }

    // Where the suit's station is, if it is in this dimension.
    private BlockPos knownStation(ServerPlayer p) {
        CompoundTag d = data(p);
        ResourceLocation dim = ResourceLocation.tryParse(d.getString(STATION_DIM));
        if (dim == null || !d.contains(STATION_POS) || !p.level().dimension().location().equals(dim)) return null;
        return BlockPos.of(d.getLong(STATION_POS));
    }

    private static ItemStack fromBag(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            ItemStack s = p.getInventory().items.get(i);
            if (s.is(SuitItems.SUIT.get())) {
                p.getInventory().items.set(i, ItemStack.EMPTY);
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack fromStation(ServerPlayer p) {
        CompoundTag d = data(p);
        ResourceLocation dim = ResourceLocation.tryParse(d.getString(STATION_DIM));
        if (dim == null || !d.contains(STATION_POS)) {
            whisper(p, "The suit doesn't know where it is. Put it on a station first.");
            return ItemStack.EMPTY;
        }
        BlockPos pos = BlockPos.of(d.getLong(STATION_POS));
        boolean anywhere = mastery(p) >= SuitRules.CALL_ANYWHERE_LEVEL;
        if (!p.level().dimension().location().equals(dim) || (!anywhere && pos.distSqr(p.blockPosition()) > SuitRules.CALL_RANGE * SuitRules.CALL_RANGE)) {
            whisper(p, "The suit is too far away to hear you.");
            return ItemStack.EMPTY;
        }
        ServerLevel level = p.serverLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChargingStation.Entity station) || station.chest.isEmpty()) {
            whisper(p, "The suit isn't there.");
            return ItemStack.EMPTY;
        }
        return station.takeChest(level, pos, level.getBlockState(pos));
    }

    // Strength module: pick a mob up over your head, then throw it.
    private void liftOrThrow(ServerPlayer p) {
        Mob held = LIFTED.remove(p.getUUID());
        if (held != null) {
            held.stopRiding();
            if (held.isAlive()) {
                held.setDeltaMovement(p.getLookAngle().scale(2).add(0, 0.3, 0));
                held.hurtMarked = true;
            }
            return;
        }
        if (!module(p, "strength")) {
            whisper(p, "That needs a Strength module.");
            return;
        }
        LivingEntity target = Fx.lookTarget(p, SuitRules.LIFT_REACH);
        if (target instanceof Mob mob && mob.startRiding(p, true)) LIFTED.put(p.getUUID(), mob);
    }

    // The quest's nuke: a siren, 5 seconds, then 50 hearts. It doesn't matter how you live, as long as you do.
    private void startNuke(ServerPlayer p) {
        NUKES.put(p.getUUID(), p.level().getGameTime() + SuitRules.NUKE_WARNING_TICKS);
        p.sendSystemMessage(Component.literal("Something is falling out of the sky.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.get(), SoundSource.HOSTILE, 4f, 0.6f);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || NUKES.isEmpty()) return;
        Iterator<Map.Entry<UUID, Long>> it = NUKES.entrySet().iterator();
        List<ServerPlayer> hit = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null) {
                it.remove();
            } else if (p.level().getGameTime() >= entry.getValue()) {
                it.remove();
                hit.add(p);
            }
        }
        for (ServerPlayer p : hit) nuke(p);
    }

    private static void nuke(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        level.explode(null, p.getX(), p.getY() + 1, p.getZ(), 8f, Level.ExplosionInteraction.NONE);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(SuitRules.NUKE_RADIUS), LivingEntity::isAlive)) {
            near.invulnerableTime = 0;
            near.hurt(level.damageSources().explosion(null, null), (float) Setting.SUIT_NUKE_DAMAGE.f());
        }
        if (p.isAlive() && SuitItems.fullSuit(p) && POWER.questReady(p)) POWER.awaken(p);
    }

    // Practically invincible while there's power. Things that ignore everything (/kill, the void) still ignore this.
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && POWER.powered(p) && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            e.setAmount((float) (e.getAmount() * SuitRules.DAMAGE_LEFT));
        }
        if (e.getSource().getDirectEntity() instanceof ServerPlayer p && POWER.module(p, "strength")) {
            e.setAmount((float) (e.getAmount() * SuitRules.MODULE_PUNCH));
        }
    }

    // Shield module: blocks one hit every 30 seconds.
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !POWER.module(p, "shield") || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        long now = p.level().getGameTime();
        if (now - SHIELD_USED.getOrDefault(p.getUUID(), -100000L) < SuitRules.SHIELD_TICKS) return;
        SHIELD_USED.put(p.getUUID(), now);
        e.setCanceled(true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.8f);
    }

    // You can't run in it, only walk. The slim suit can.
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !e.player.isSprinting() || !SuitItems.fullSuit(e.player)) return;
        if (!SuitItems.slim(chest(e.player))) e.player.setSprinting(false);
    }

    // The awakening's slim suit: 4 Vibranium Ingots from your bag.
    boolean tryMakeSlim(Player player, ItemStack suit) {
        if (!(player instanceof ServerPlayer p) || !owns(p) || !awakened(p) || SuitItems.slim(suit)) return false;
        int have = 0;
        List<ItemStack> ingots = new ArrayList<>();
        for (ItemStack s : p.getInventory().items) {
            var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
            if (key != null && "palladium:vibranium_ingot".equals(key.toString())) {
                have += s.getCount();
                ingots.add(s);
            }
        }
        if (have < SuitRules.SLIM_COST) {
            whisper(p, "That takes " + SuitRules.SLIM_COST + " Vibranium Ingots in your bag.");
            return true;
        }
        int left = SuitRules.SLIM_COST;
        for (ItemStack s : ingots) {
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        SuitItems.makeSlim(suit);
        whisper(p, "It fits closer now.");
        return true;
    }

    public boolean scannerOn(Player p) {
        return module(p, "scanner");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        SHIELD_USED.remove(id);
        NUKES.remove(id);
        Mob held = LIFTED.remove(id);
        if (held != null) held.stopRiding();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        SHIELD_USED.clear();
        LIFTED.clear();
        NUKES.clear();
    }
}
