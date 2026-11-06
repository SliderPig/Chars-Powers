package com.example.charpowers.hidden;

import com.example.charpowers.hidden.model.FarmRule;
import com.example.charpowers.hidden.model.MobRoll;
import com.example.charpowers.hidden.model.PowerDef;
import com.example.charpowers.hidden.model.Roller;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;
import java.util.*;

// Which mobs have a hidden power. It's saved on the mob itself, so it never changes once rolled.
// The mobs that HAVE one are kept in a small set, so the per-tick work skips the other (most) mobs for free.
public final class MobPowers {
    static final String ROLLED = "charpowers.rolled";
    static final String HIDDEN = "charpowers.hidden";
    static final String STATE = "charpowers.state";

    private static final Set<Mob> HOLDERS = Collections.newSetFromMap(new WeakHashMap<>());

    // Called when a mob joins a world (new spawn, chunk load, dimension change).
    public static void onJoin(Mob m) {
        if (!HiddenConfig.HIDDEN_POWERS_ENABLED.get()) return;
        CompoundTag data = m.getPersistentData();
        if (MobRoll.shouldRoll(data.getBoolean(ROLLED))) {
            data.putBoolean(ROLLED, true);
            Roller roller = new Roller(HiddenConfig.ANIMAL_ONE_IN.get(), HiddenConfig.OTHER_ONE_IN.get(), HiddenConfig.weights());
            String id = madeByAFarm(m) ? null : roller.roll(isAnimal(m), HiddenPowers.all(), new RandomAdapter(m.getRandom())).map(PowerDef::id).orElse(null);
            if (id != null && crowded(m) && !FarmRule.keeps(isAnimal(m) ? HiddenConfig.ANIMAL_ONE_IN.get() : HiddenConfig.OTHER_ONE_IN.get(), HiddenConfig.FARM_ONE_IN.get(), new RandomAdapter(m.getRandom()))) id = null;
            if ("everything".equals(id) && !com.example.charpowers.hidden.ranks.ZSlot.claim(m)) {
                // only one mob in the world may have the Z rank: this one gets a different power instead
                List<PowerDef> rest = HiddenPowers.all().stream().filter(d -> d.rank() != com.example.charpowers.hidden.model.Rank.Z).toList();
                id = new Roller(1, 1, HiddenConfig.weights()).roll(false, rest, new RandomAdapter(m.getRandom())).map(PowerDef::id).orElse(null);
            }
            if (id != null) data.putString(HIDDEN, id);
        }
        if (get(m).isPresent()) HOLDERS.add(m);
    }

    public static void onLeave(Mob m) { HOLDERS.remove(m); }

    // Sure to be a farm: from a spawner, bred, a golem villagers called, a slime that split. These never get a power.
    private static boolean madeByAFarm(Mob m) {
        MobSpawnType how = m.getSpawnType();
        if (how == MobSpawnType.SPAWNER || how == MobSpawnType.BREEDING || how == MobSpawnType.MOB_SUMMONED) return true;
        return how == null && (m instanceof Slime || m instanceof AgeableMob a && a.isBaby());
    }

    // Looks like a farm: born into a crowd of its own kind (dark rooms, wither skeleton pads, animal pens). It might be a big cave, so it's only much rarer.
    private static boolean crowded(Mob m) {
        return m.level().getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(16), o -> o != m && o.getType().getCategory() == m.getType().getCategory()).size() >= HiddenConfig.FARM_CROWD.get();
    }

    public static Optional<String> get(Mob m) {
        String id = m.getPersistentData().getString(HIDDEN);
        return id.isEmpty() || HiddenPowers.def(id).isEmpty() ? Optional.empty() : Optional.of(id);
    }

    // For the /hiddenpower set command: give a mob a power (or none).
    public static void set(Mob m, @Nullable String id) {
        CompoundTag data = m.getPersistentData();
        get(m).ifPresent(old -> HiddenEvents.stopFor(m, old));
        data.putBoolean(ROLLED, true);
        data.remove(STATE);
        if (id == null) {
            data.remove(HIDDEN);
            HOLDERS.remove(m);
        } else {
            data.putString(HIDDEN, id);
            HOLDERS.add(m);
        }
    }

    // Saved scratch space for this mob's power (timers etc).
    public static CompoundTag state(Mob m, String id) {
        CompoundTag data = m.getPersistentData();
        if (!data.contains(STATE)) data.put(STATE, new CompoundTag());
        CompoundTag all = data.getCompound(STATE);
        if (!all.contains(id)) all.put(id, new CompoundTag());
        return all.getCompound(id);
    }

    public static boolean isAnimal(Mob m) {
        return m instanceof Animal || m instanceof WaterAnimal || m instanceof AmbientCreature || m instanceof AbstractVillager;
    }

    // Mobs with a power that are in this world right now.
    public static List<Mob> holdersIn(ServerLevel level) {
        List<Mob> out = new ArrayList<>();
        for (Mob m : HOLDERS) if (m.level() == level && m.isAlive() && !m.isRemoved()) out.add(m);
        return out;
    }

    public static boolean holds(Mob m) { return HOLDERS.contains(m); }

    // Minecraft's RandomSource doesn't implement Java's RandomGenerator; this lets the model use it.
    record RandomAdapter(RandomSource r) implements java.util.random.RandomGenerator {
        @Override public long nextLong() { return r.nextLong(); }
        @Override public int nextInt(int bound) { return r.nextInt(bound); }
    }

    private MobPowers() {}
}
