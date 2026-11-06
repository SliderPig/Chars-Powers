package com.example.charpowers.hidden;

import com.example.charpowers.CharPowers;
import com.example.charpowers.hidden.model.CooldownSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.icon.IIcon;
import net.threetag.palladium.util.icon.ItemIcon;
import net.threetag.palladium.util.icon.TexturedIcon;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;
import net.threetag.palladiumcore.registry.DeferredRegister;
import net.threetag.palladiumcore.registry.RegistrySupplier;

// One of the 5 hidden-power keys on Palladium's ability bar. Pressing it uses whatever power is in that slot.
// Its icon on the bar changes to the equipped power's icon.
public class HiddenSlotAbility extends Ability {
    public static final DeferredRegister<Ability> ABILITIES = DeferredRegister.create(CharPowers.MODID, Ability.REGISTRY);
    public static final RegistrySupplier<Ability> HIDDEN_SLOT = ABILITIES.register("hidden_slot", HiddenSlotAbility::new);

    private static final IIcon EMPTY = new TexturedIcon(HiddenIcons.EMPTY);

    public static final PalladiumProperty<Integer> SLOT = new IntegerProperty("slot").configurable("Which hidden power key this is (0-4)");

    public HiddenSlotAbility() {
        this.withProperty(SLOT, 0);
        this.withProperty(ICON, EMPTY);
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        // each player's copy gets its own icon, so the bar can show what they've equipped
        manager.register(ICON, EMPTY);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled || !(entity instanceof ServerPlayer p)) return;
        // show the power's own cooldown on the bar (Palladium drains the slot and syncs it to the player's PC)
        int left = HiddenRuntime.useKey(p, entry.getProperty(SLOT));
        if (left > 0) entry.startCooldown(p, left);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer p)) return;
        // Palladium's bar cooldown belongs to the slot (and blocks the key), the power's cooldown to the power:
        // keep the slot's matching whatever is in it now (a swap, Ground Slam throwing itself, a refund...)
        if (p.tickCount % 5 == 0) {
            int left = HiddenRuntime.cooldownLeft(p, entry.getProperty(SLOT));
            if (CooldownSync.needsSync(entry.cooldown, left)) entry.startCooldown(p, left);
        }
        if (p.tickCount % 20 != 0) return;
        String id = PlayerHiddenData.get(p).key(entry.getProperty(SLOT));
        IIcon want = id == null ? EMPTY : HiddenIcons.icon(id);
        if (!same(want, entry.getProperty(ICON))) {
            entry.setUniqueProperty(ICON, want);
            entry.syncProperty(ICON, p, SyncType.EVERYONE);
        }
    }

    // Two item stacks are never equal() even when they show the same item, and this is asked every second.
    private static boolean same(IIcon a, IIcon b) {
        if (a instanceof ItemIcon x && b instanceof ItemIcon y) return x.stack().getItem() == y.stack().getItem();
        return a.equals(b);
    }
}
