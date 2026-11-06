package com.example.charpowers.mobcontrol;

import com.example.charpowers.CharPowers;
import com.example.charpowers.mobcontrol.model.PetCareRules;
import com.example.charpowers.own.Minions;
import com.example.charpowers.own.OwnPower;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

// Looking after a pet: feed it its favourite food to heal it, and ride the big or saddled ones with an empty hand.
// Both only work on your own pets. Name tags, saddles and everything else go through the game as usual.
@Mod.EventBusSubscriber(modid = CharPowers.MODID)
public final class PetCare {
    private PetCare() {}

    private static String path(Mob m) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(m.getType());
        return key == null ? "" : key.getPath();
    }

    public static boolean accepts(Mob pet, ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(pet);
        boolean breeding = pet instanceof Animal a && a.isFood(stack);
        return PetCareRules.accepts(path(pet), pet instanceof Animal, breeding, food != null, food != null && food.isMeat(), stack.is(ItemTags.FISHES));
    }

    // Feeds the pet. Returns true if the food was taken. A pet that is not hurt turns it down.
    public static boolean feed(ServerPlayer p, Mob pet, ItemStack stack) {
        if (!accepts(pet, stack)) return false;
        if (pet.getHealth() >= pet.getMaxHealth()) {
            OwnPower.whisper(p, pet.getName().getString() + " isn't hungry.");
            return true;
        }
        FoodProperties food = stack.getFoodProperties(pet);
        pet.heal(PetCareRules.heal(food == null ? 4 : food.getNutrition()));
        if (!p.getAbilities().instabuild) stack.shrink(1);
        if (pet.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() + 0.2, pet.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
            level.playSound(null, pet.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.7f, 1f);
        }
        return true;
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || !(e.getTarget() instanceof Mob pet)) return;
        if (!Minions.isKind(pet, "pet") || !p.getUUID().equals(Minions.ownerOf(pet))) return;
        ItemStack held = p.getMainHandItem();
        if (!held.isEmpty()) {
            if (feed(p, pet, held)) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (p.isShiftKeyDown() || p.isPassenger()) return;
        boolean saddleable = pet instanceof Saddleable;
        if (!PetCareRules.ridable(path(pet), saddleable, saddleable && ((Saddleable) pet).isSaddled())) return;
        if (pet instanceof AbstractHorse horse && !horse.isTamed()) horse.setTamed(true);
        if (MobControl.rideAsPet(p, pet)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
