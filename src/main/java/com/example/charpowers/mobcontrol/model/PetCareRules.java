package com.example.charpowers.mobcontrol.model;

import java.util.Set;

// What a pet will eat to get better, and which pets can be ridden. No game code, so it can be tested on its own.
public final class PetCareRules {
    // Animals that live on meat or fish; every other kind of animal eats plants.
    private static final Set<String> MEAT_ANIMALS = Set.of("wolf", "cat", "ocelot", "fox", "polar_bear", "axolotl");
    // Big things you can ride with an empty hand and no saddle.
    private static final Set<String> BIG_RIDES = Set.of("ravager", "hoglin", "zoglin");

    private PetCareRules() {}

    public static boolean eatsMeat(String entityPath, boolean isAnimal) {
        return !isAnimal || MEAT_ANIMALS.contains(entityPath);
    }

    // The kind's own favourite (what you breed it with) is always welcome. Otherwise meat for meat eaters and
    // plants for the plant eaters.
    public static boolean accepts(String entityPath, boolean isAnimal, boolean breedingFood, boolean edible, boolean meat, boolean fish) {
        if (breedingFood) return true;
        if (!edible) return false;
        if (eatsMeat(entityPath, isAnimal)) return meat || (fish && !isAnimal) || (fish && MEAT_ANIMALS.contains(entityPath));
        return !meat;
    }

    // How much health it gets back: what the food would give you, at least 2 (a heart).
    public static float heal(int nutrition) {
        return Math.max(2f, nutrition);
    }

    public static boolean ridable(String entityPath, boolean saddleable, boolean saddled) {
        return BIG_RIDES.contains(entityPath) || (saddleable && saddled);
    }
}
