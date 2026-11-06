package com.example.charpowers.cloning;

import com.example.charpowers.cloning.model.BrainRules;
import com.example.charpowers.cloning.model.BrainRules.Option;
import com.example.charpowers.cloning.model.BrainRules.Role;
import com.example.charpowers.cloning.model.BrainRules.Situation;
import com.example.charpowers.hidden.HiddenConfig;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.condition.BuyableCondition;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.PowerManager;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.EntityPropertyHandler;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The clone's battle brain: which of its powers' abilities to use right now, and pressing them the way a player's key
// would. Most abilities need setting up first (the right ability wheel slot, the right power selected, empty hands), so
// a press is: set up, wait for Palladium to notice, press, let go.
public final class CloneBrain {
    private static final int THINK_EVERY = 10, FIGHT_MEMORY = 60, SETTLE_TICKS = 2, BETWEEN_MOVES = 30, FAILED_WAIT = 200,
            HOLD_TICKS = 40, WATCH_TICKS = 40, TOGGLES_OFF_AFTER = 200;
    private static final double SHOT_SPEED = 2.0;

    private enum Stage { SETTLE, HOLD, RELEASE }

    private record Move(String power, String ability, AbilityInstance instance, JsonObject entry, Role role) {}

    private final CloneEntity clone;
    private final Map<String, Integer> nextTry = new HashMap<>();
    private final Set<AbilityInstance> toggledOn = new HashSet<>();
    private LivingEntity target;
    private int lastFight = -1000;
    private Move current;
    private Stage stage;
    private int timer;
    // watching what an unknown ability does
    private Move watching;
    private int watchLeft;
    private float targetHealthThen, ownHealthThen;

    CloneBrain(CloneEntity clone) {
        this.clone = clone;
    }

    // The job says who it's fighting, every tick.
    void fight(LivingEntity who) {
        target = who;
        lastFight = clone.tickCount;
    }

    // true while its hands are spoken for, so it doesn't swap to its sword or swing in the middle of a move
    public boolean handsBusy() {
        return current != null;
    }

    // Watching a test: a swing would spoil what it's trying to find out.
    boolean holdSwing() {
        return handsBusy() || watching != null;
    }

    // A clone that gains a power gets its skills too: nothing else can spend skill points on it.
    static void learnSkills(CloneEntity c) {
        if (!HiddenConfig.CLONES_GET_SKILLS.get()) return;
        PowerManager.getPowerHandler(c).ifPresent(h -> {
            for (IPowerHolder holder : h.getPowerHolders().values()) {
                for (AbilityInstance a : holder.getAbilities().values()) {
                    for (Condition condition : a.getConfiguration().getUnlockingConditions()) {
                        if (condition instanceof BuyableCondition) a.setUniqueProperty(BuyableCondition.BOUGHT, true);
                    }
                }
            }
        });
    }

    void tick() {
        finishWatching();
        if (current != null) {
            carryOn();
            return;
        }
        boolean fighting = target != null && target.isAlive() && target.level() == clone.level() && clone.tickCount - lastFight <= FIGHT_MEMORY;
        if (!fighting) {
            target = null;
            if (clone.tickCount - lastFight > TOGGLES_OFF_AFTER) switchTogglesOff();
            return;
        }
        if (clone.tickCount % THINK_EVERY != 0 || watching != null) return;
        think();
    }

    private void think() {
        Map<String, Move> moves = new HashMap<>();
        List<Option> options = new ArrayList<>();
        PowerManager.getPowerHandler(clone).ifPresent(h -> {
            for (Map.Entry<ResourceLocation, IPowerHolder> e : h.getPowerHolders().entrySet()) {
                String power = e.getKey().toString();
                for (Map.Entry<String, AbilityInstance> a : e.getValue().getAbilities().entrySet()) {
                    Move move = usable(power, a.getKey(), a.getValue());
                    if (move == null) continue;
                    String key = power + "|" + a.getKey();
                    moves.put(key, move);
                    options.add(new Option(key, move.role(), move.entry().has("d") ? move.entry().get("d").getAsInt() : 32));
                }
            }
        });
        if (options.isEmpty()) return;
        double distance = Math.sqrt(target.getBoundingBox().distanceToSqr(clone.getEyePosition()));
        Situation now = new Situation(distance, clone.getHealth() / clone.getMaxHealth(), clearShot());
        Option pick = BrainRules.pick(options, now, clone.getRandom()::nextInt);
        if (pick != null) begin(moves.get(pick.key()));
    }

    @Nullable
    private Move usable(String power, String ability, AbilityInstance instance) {
        JsonObject entry = BrainTable.entry(power, ability);
        if (entry == null) return null;
        Role role = BrainTable.role(power, ability, entry);
        if (role == null || instance.isOnCooldown()) return null;
        if (nextTry.getOrDefault(power + "|" + ability, 0) > clone.tickCount) return null;
        if (entry.has("b") && !HiddenConfig.CLONES_BREAK_BLOCKS.get()) return null;
        if (toggledOn.contains(instance) || instance.isEnabled()) return null;
        boolean needsSetup = entry.has("s") || entry.has("e") || entry.has("m") || entry.has("o");
        if (!instance.isUnlocked() && !needsSetup) return null;
        return new Move(power, ability, instance, entry, role);
    }

    private void begin(Move move) {
        current = move;
        stage = Stage.SETTLE;
        timer = SETTLE_TICKS;
        JsonObject e = move.entry();
        BrainTable.props(e).forEach(this::setProperty);
        if (e.has("e")) clone.getPersistentData().putString("selectedPower", e.get("e").getAsString());
        if (e.has("m")) emptyMainHand();
        if (e.has("o")) emptyOffHand();
    }

    @SuppressWarnings("unchecked")
    private void setProperty(String name, int value) {
        EntityPropertyHandler.getHandler(clone).ifPresent(h -> {
            PalladiumProperty<?> property = ((PropertyManager) h).getPropertyByName(name);
            if (property != null) h.set((PalladiumProperty<Integer>) property, value);
        });
    }

    private void emptyMainHand() {
        Inventory inv = clone.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.selected = i;
                return;
            }
        }
        ItemStack held = inv.getItem(inv.selected);
        int free = inv.getFreeSlot();
        if (free >= 0) {
            inv.setItem(free, held);
            inv.setItem(inv.selected, ItemStack.EMPTY);
        }
    }

    private void emptyOffHand() {
        ItemStack off = clone.getOffhandItem();
        if (!off.isEmpty() && clone.getInventory().getFreeSlot() >= 0) {
            clone.getInventory().add(off.copy());
            clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private void carryOn() {
        Move move = current;
        aim(move);
        if (--timer > 0) return;
        if (stage == Stage.SETTLE) {
            if (!move.instance().isUnlocked() || target == null) {
                finish(FAILED_WAIT);
                return;
            }
            startWatching(move);
            move.instance().keyPressed(clone, true);
            String kind = move.entry().get("p").getAsString();
            if (kind.equals("toggle") || kind.equals("activation")) toggledOn.add(move.instance());
            stage = kind.equals("held") ? Stage.HOLD : Stage.RELEASE;
            timer = kind.equals("held") ? HOLD_TICKS : 1;
        } else {
            move.instance().keyPressed(clone, false);
            finish(BETWEEN_MOVES);
        }
    }

    private void finish(int wait) {
        nextTry.put(current.power() + "|" + current.ability(), clone.tickCount + wait);
        current = null;
        stage = null;
    }

    // Look where the move should go: a shot leads a moving target, and anything else looks it in the eye.
    private void aim(Move move) {
        if (target == null) return;
        Vec3 at = target.getEyePosition();
        if (move.role() == Role.RANGED && !move.entry().has("n")) { // n = a beam, which hits where it's pointed
            double distance = clone.getEyePosition().distanceTo(at);
            at = at.add(target.getDeltaMovement().scale(BrainRules.leadTicks(distance, SHOT_SPEED)));
        }
        CloneActions.lookAt(clone, at);
    }

    // Would a shot reach the target without going through a friend?
    private boolean clearShot() {
        if (target == null || !clone.hasLineOfSight(target)) return false;
        Vec3 from = clone.getEyePosition(), to = target.getEyePosition();
        AABB between = new AABB(from, to).inflate(1.0);
        ServerPlayer host = CloneManager.host(clone);
        for (LivingEntity e : clone.level().getEntitiesOfClass(LivingEntity.class, between, e -> e != clone && e != target)) {
            boolean friend = e == host || e instanceof CloneEntity || e instanceof DogPileMob || e instanceof net.minecraft.world.entity.player.Player
                    || e.isAlliedTo(clone) || (host != null && e.isAlliedTo(host));
            if (friend && e.getBoundingBox().inflate(0.6).clip(from, to).isPresent()) return false;
        }
        return true;
    }

    private void startWatching(Move move) {
        if (move.role() != Role.UNKNOWN || target == null) return;
        watching = move;
        watchLeft = WATCH_TICKS;
        targetHealthThen = target.getHealth();
        ownHealthThen = clone.getHealth();
    }

    private void finishWatching() {
        if (watching == null || --watchLeft > 0) return;
        double lost = target == null ? 0 : Math.max(0, targetHealthThen - target.getHealth());
        BrainTable.record(watching.power(), watching.ability(), BrainRules.judge(lost, clone.getHealth() - ownHealthThen));
        watching = null;
    }

    // The fight's over: anything it switched on goes off again.
    private void switchTogglesOff() {
        if (toggledOn.isEmpty()) return;
        for (AbilityInstance a : toggledOn) {
            if (a.isEnabled()) {
                a.keyPressed(clone, true);
                a.keyPressed(clone, false);
            }
        }
        toggledOn.clear();
    }
}
