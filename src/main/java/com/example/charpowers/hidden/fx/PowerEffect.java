package com.example.charpowers.hidden.fx;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.List;

// What one hidden power DOES. Each power fills in only the parts it needs.
// The same code runs for a player and for a mob; PowerUser says which.
public interface PowerEffect {
    // the key was pressed (player) or the mob decided to use it
    default void onKey(PowerUser u) {}

    // every server tick while it's equipped (player) or the mob has it. Effects slow themselves down.
    default void tick(PowerUser u) {}

    // the holder got hurt
    default void onHurt(PowerUser u, LivingHurtEvent e) {}

    // the holder hit something
    default void onAttack(PowerUser u, LivingEntity target) {}

    // the holder died
    default void onDeath(PowerUser u) {}

    // the holder killed something
    default void onKill(PowerUser u, LivingEntity victim) {}

    // the holder jumped
    default void onJump(PowerUser u) {}

    // the holder landed after falling this far (blocks)
    default void onFall(PowerUser u, float distance) {}

    // a sound is about to play right where the holder is (change its pitch/volume, or cancel it)
    default void onSound(PowerUser u, PlayLevelSoundEvent e) {}

    // Chat arrives on its own thread, so it's split in two:
    // swallowsChat: decide right away whether this message is hidden from everyone (only READ state here)
    default boolean swallowsChat(PowerUser u) { return false; }

    // onChat: the holder (a player) sent this message; runs safely on the server's main thread.
    // `swallowed` = some power hid it from chat.
    default void onChat(PowerUser u, String text, boolean swallowed) {}

    // the holder (a player) broke a block
    default void onBreak(PowerUser u, BlockEvent.BreakEvent e) {}

    // the holder (a player) right-clicked with an item, or on a block (block may be null)
    default void onRightClick(PowerUser u, ItemStack held, @Nullable BlockState block) {}

    // the power was taken off / the holder unloaded: clean up anything it spawned
    default void stop(PowerUser u) {}

    // --- added with ranks E to Z ---

    // the holder is about to be hurt. Cancelling the event = no damage at all (no red flash, no knockback)
    default void onAttacked(PowerUser u, LivingAttackEvent e) {}

    // the holder's hit on `target` is landing: change e's amount to hit harder or softer
    default void onHit(PowerUser u, LivingEntity target, LivingHurtEvent e) {}

    // the holder is landing: change the damage multiplier or distance (softer landings)
    default void onFallEvent(PowerUser u, LivingFallEvent e) {}

    // the holder is being knocked back (set the strength to 0 to stand firm)
    default void onKnockback(PowerUser u, LivingKnockBackEvent e) {}

    // the holder finished eating or drinking this (a copy of the item as it was before)
    default void onAte(PowerUser u, ItemStack food) {}

    // the holder is healing
    default void onHeal(PowerUser u, LivingHealEvent e) {}

    // the holder (a player) woke up in a bed
    default void onWake(PowerUser u) {}

    // the holder (a player) right-clicked a mob / a block
    default void onUseEntity(PowerUser u, PlayerInteractEvent.EntityInteract e) {}
    default void onUseBlock(PowerUser u, PlayerInteractEvent.RightClickBlock e) {}

    // a mob picked the holder as its new target (cancel = it doesn't)
    default void onTargeted(PowerUser u, LivingChangeTargetEvent e) {}

    // the holder shot or threw something (arrow, snowball, trident...)
    default void onShoot(PowerUser u, Projectile p) {}

    // the holder killed something, and these are its drops (add more to e.getDrops())
    default void onDrops(PowerUser u, LivingDropsEvent e) {}

    // the holder is about to die. Return true to stop the death (heal them first!)
    default boolean preventsDeath(PowerUser u, LivingDeathEvent e) { return false; }

    // a potion effect is being put on the holder
    default void onEffectAdded(PowerUser u, MobEffectEvent.Added e) {}

    // Powers that hand out OTHER powers for a while (Copycat, Everything): their ids. Those work as if they were
    // equipped, each with its own saved space (PowerUser.lent). Lent powers never lend anything themselves.
    default List<String> lends(PowerUser u) { return List.of(); }

    // --- added with the attack powers ---

    // the holder's hit (a normal hit or a key power's hit) is landing on `target`: add boosts to it
    default void boostHit(PowerUser u, LivingEntity target, Boost b) {}

    // how close a mob's target must be for the mob to use this power (ATTACK powers)
    default double mobRange() { return 4.0; }
}
