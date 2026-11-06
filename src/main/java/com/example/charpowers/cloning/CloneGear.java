package com.example.charpowers.cloning;

import com.example.charpowers.CharPowers;
import com.example.charpowers.cloning.model.GearRank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.fml.loading.FMLPaths;
import net.threetag.palladium.power.ItemPowerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

// The marked chest: a direct link, so the clone never has to walk to it. It wears the best armour it finds,
// keeps the best sword, axe and pickaxe, and eats when hungry. Clones never craft.
public final class CloneGear {
    private static final int HUNGRY_AT = 14;
    private static Set<String> powerItems;

    private CloneGear() {}

    // null when no chest is marked, it's gone, or it's in another dimension.
    // force = load its chunk if needed; only worth it when the clone really wants something.
    @Nullable
    static Container chest(CloneEntity c, ServerPlayer host, boolean force) {
        GlobalPos where = CloneData.chest(host, c.slot);
        if (where == null || where.dimension() != c.level().dimension()) return null;
        ServerLevel level = c.serverLevel();
        BlockPos pos = where.pos();
        if (!force && !level.isLoaded(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock block) return ChestBlock.getContainer(block, state, level, pos, true);
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container container ? container : null;
    }

    static void refresh(CloneEntity c, ServerPlayer host, boolean force) {
        boolean missing = force || missingSomething(c);
        Container chest = chest(c, host, missing);
        if (chest != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.ARMOR) wearBest(c, chest, slot);
            }
            keepBest(c, chest, CloneGear::weaponScore, s -> s.getItem() instanceof SwordItem);
            keepBest(c, chest, CloneGear::weaponScore, s -> s.getItem() instanceof AxeItem);
            keepBest(c, chest, CloneGear::toolScore, s -> s.getItem() instanceof PickaxeItem);
        }
        if (!c.job.usesTools()) holdWeapon(c);
    }

    private static boolean missingSomething(CloneEntity c) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR && c.getItemBySlot(slot).isEmpty()) return true;
        }
        return bestSlot(c.getInventory(), s -> s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem, CloneGear::weaponScore) < 0;
    }

    private static int enchants(ItemStack s) {
        return EnchantmentHelper.getEnchantments(s).values().stream().mapToInt(Integer::intValue).sum();
    }

    static int gearScore(ItemStack s) {
        if (s.getItem() instanceof ArmorItem) return armorScore(s);
        return weaponScore(s) + toolScore(s);
    }

    private static int armorScore(ItemStack s) {
        ArmorItem a = (ArmorItem) s.getItem();
        return GearRank.armor(a.getDefense(), a.getToughness(), enchants(s));
    }

    private static int weaponScore(ItemStack s) {
        float damage = s.getItem() instanceof SwordItem sword ? sword.getDamage() : s.getItem() instanceof DiggerItem digger ? digger.getAttackDamage() : 0;
        return GearRank.weapon(damage, enchants(s));
    }

    private static int toolScore(ItemStack s) {
        return GearRank.tool(s.getItem() instanceof TieredItem t ? t.getTier().getLevel() : 0, enchants(s));
    }

    private static void wearBest(CloneEntity c, Container chest, EquipmentSlot slot) {
        int best = -1;
        int bestScore = c.getItemBySlot(slot).getItem() instanceof ArmorItem ? armorScore(c.getItemBySlot(slot)) : -1;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == slot && armorScore(s) > bestScore) {
                best = i;
                bestScore = armorScore(s);
            }
        }
        if (best < 0) return;
        ItemStack old = c.getItemBySlot(slot);
        c.setItemSlot(slot, chest.removeItem(best, 1));
        chest.setChanged();
        if (!old.isEmpty()) storeOrDrop(c, chest, old);
    }

    // Swaps in the chest's best item of a kind when it beats the clone's own.
    private static void keepBest(CloneEntity c, Container chest, ToIntFunction<ItemStack> score, Predicate<ItemStack> kind) {
        Inventory inv = c.getInventory();
        int own = bestSlot(inv, kind, score);
        int ownScore = own < 0 ? -1 : score.applyAsInt(inv.getItem(own));
        int best = -1;
        int bestScore = ownScore;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (kind.test(s) && score.applyAsInt(s) > bestScore) {
                best = i;
                bestScore = score.applyAsInt(s);
            }
        }
        if (best < 0) return;
        ItemStack taken = chest.removeItem(best, 1);
        chest.setChanged();
        if (own >= 0) {
            storeOrDrop(c, chest, inv.getItem(own));
            inv.setItem(own, taken);
        } else if (!inv.add(taken)) {
            storeOrDrop(c, chest, taken);
        }
    }

    private static int bestSlot(Inventory inv, Predicate<ItemStack> kind, ToIntFunction<ItemStack> score) {
        int best = -1;
        int bestScore = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (kind.test(s) && score.applyAsInt(s) > bestScore) {
                best = i;
                bestScore = score.applyAsInt(s);
            }
        }
        return best;
    }

    // The best sword or axe goes in the hand. Never while it's in the middle of eating.
    static void holdWeapon(CloneEntity c) {
        if (c.isUsingItem() || c.tryingItemFor > 0 || c.brain.handsBusy()) return;
        Inventory inv = c.getInventory();
        int weapon = bestSlot(inv, s -> s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem, CloneGear::weaponScore);
        if (weapon < 0) return;
        inv.selected = toHotbar(inv, weapon);
    }

    private static int toHotbar(Inventory inv, int slot) {
        if (Inventory.isHotbarSlot(slot)) return slot;
        ItemStack here = inv.getItem(0);
        inv.setItem(0, inv.getItem(slot));
        inv.setItem(slot, here);
        return 0;
    }

    private static boolean goodFood(ItemStack s, CloneEntity c) {
        if (!s.isEdible()) return false;
        FoodProperties food = s.getFoodProperties(c);
        if (food == null) return false;
        return food.getEffects().stream().noneMatch(e -> e.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL);
    }

    static void eatIfHungry(CloneEntity c, ServerPlayer host) {
        if (c.isUsingItem() || c.tryingItemFor > 0 || c.getFoodData().getFoodLevel() > HUNGRY_AT) return;
        Inventory inv = c.getInventory();
        int food = -1;
        for (int i = 0; i < inv.items.size() && food < 0; i++) if (goodFood(inv.items.get(i), c)) food = i;
        if (food < 0) {
            Container chest = chest(c, host, true);
            if (chest == null) return;
            for (int i = 0; i < chest.getContainerSize() && food < 0; i++) {
                if (!goodFood(chest.getItem(i), c)) continue;
                ItemStack taken = chest.removeItem(i, chest.getItem(i).getCount());
                chest.setChanged();
                inv.add(taken);
                if (!taken.isEmpty()) storeOrDrop(c, chest, taken);
                for (int j = 0; j < inv.items.size() && food < 0; j++) if (goodFood(inv.items.get(j), c)) food = j;
            }
            if (food < 0) return;
        }
        inv.selected = toHotbar(inv, food);
        c.startUsingItem(InteractionHand.MAIN_HAND);
    }

    // Working clones: what they've gathered goes to the chest, keeping tools, weapons, armour and food. False = no room.
    static boolean stashLoot(CloneEntity c, Container chest) {
        Inventory inv = c.getInventory();
        boolean room = false;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty() || s.isDamageableItem() || goodFood(s, c)) continue;
            ItemStack left = HopperBlockEntity.addItem(null, chest, s, null);
            inv.items.set(i, left);
            if (left.isEmpty()) room = true;
        }
        chest.setChanged();
        return room;
    }

    public static void storeIn(CloneEntity c, Container chest, ItemStack stack) {
        storeOrDrop(c, chest, stack);
        chest.setChanged();
    }

    @Nullable
    public static ItemStack findIn(CloneEntity c, Predicate<ItemStack> wanted) {
        for (ItemStack s : c.getInventory().items) if (!s.isEmpty() && wanted.test(s)) return s;
        return null;
    }

    // Dismissed: everything goes back in the chest. What doesn't fit (or with no chest) lands at the clone's feet.
    static void giveBack(CloneEntity c, @Nullable ServerPlayer host) {
        Container chest = host == null ? null : chest(c, host, true);
        Inventory inv = c.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.removeItemNoUpdate(i);
            if (!s.isEmpty()) storeOrDrop(c, chest, s);
        }
    }

    // Done trying an item: whatever is in that hand now goes back to the chest (nothing, if it got used up).
    static void returnTried(CloneEntity c, ServerPlayer host) {
        if (c.tryingHand == null) return;
        ItemStack held = c.getItemInHand(c.tryingHand);
        if (held.isEmpty()) return;
        c.setItemInHand(c.tryingHand, ItemStack.EMPTY);
        storeOrDrop(c, chest(c, host, true), held);
    }

    static void returnOffhand(CloneEntity c, ServerPlayer host) {
        ItemStack s = c.getOffhandItem();
        if (s.isEmpty()) return;
        c.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        storeOrDrop(c, chest(c, host, true), s);
    }

    private static void storeOrDrop(CloneEntity c, @Nullable Container chest, ItemStack stack) {
        ItemStack left = chest == null ? stack : HopperBlockEntity.addItem(null, chest, stack, null);
        if (!left.isEmpty()) c.drop(left, false, false);
    }

    // Power Clone: a real body can use an item the normal way, which also works for mods that give powers on
    // right-click. It takes one known power item, holds it in the off hand and uses it (CloneManager checks after).
    static void tryPowerItem(CloneEntity c, ServerPlayer host) {
        Container chest = chest(c, host, false);
        if (chest == null) return;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (s.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if (c.triedItems.contains(id) || !isPowerItem(s, id)) continue;
            c.triedItems.add(id);
            ItemStack one = chest.removeItem(i, 1);
            chest.setChanged();
            c.powersBefore = CloneManager.powersOf(c);
            c.tryHeld = 0;
            returnOffhand(c, host); // some items refuse to work unless the off hand is empty (FSang's filled syringes)
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (givesPowerWhenHeld(one)) {
                hand = InteractionHand.OFF_HAND;
                c.setItemSlot(EquipmentSlot.OFFHAND, one);
            } else {
                // in the main hand like a person would: many mods' scripts only listen to that hand
                Inventory inv = c.getInventory();
                ItemStack held = inv.getItem(inv.selected);
                inv.setItem(inv.selected, one);
                if (!held.isEmpty() && !inv.add(held)) storeOrDrop(c, chest, held);
            }
            c.tryingHand = hand;
            CloneActions.useItem(c, hand);
            c.tryingItemFor = 40;
            return;
        }
    }

    private static boolean givesPowerWhenHeld(ItemStack s) {
        var worn = ItemPowerManager.getInstance().getPowerForItem("offhand", s.getItem());
        return worn != null && !worn.isEmpty();
    }

    private static boolean isPowerItem(ItemStack s, String id) {
        return givesPowerWhenHeld(s) || powerItems().contains(id);
    }

    // The list inside the mod, plus any you add to config/charpowers/clone_power_items.txt (one item id a line).
    private static Set<String> powerItems() {
        if (powerItems != null) return powerItems;
        Set<String> set = new HashSet<>();
        Path file = FMLPaths.CONFIGDIR.get().resolve("charpowers").resolve("clone_power_items.txt");
        try {
            try (InputStream in = CloneGear.class.getResourceAsStream("/data/charpowers/clone_power_items.txt")) {
                if (in != null) {
                    for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                        String t = line.trim();
                        if (!t.isEmpty() && !t.startsWith("#")) set.add(t);
                    }
                }
            }
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "# Extra items a clone should try to use for Power Clone, one id a line. The mod has its own list too.\n");
            }
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("#")) set.add(t);
                }
            }
        } catch (IOException e) {
            CharPowers.LOG.warn("Couldn't read {}", file, e);
        }
        powerItems = set;
        return set;
    }
}
