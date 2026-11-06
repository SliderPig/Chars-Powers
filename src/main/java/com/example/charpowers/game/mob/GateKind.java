package com.example.charpowers.game.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// Every creature that lives in a gate. The look of each one is in tools/make_gate_mobs.py (model + picture), the rest is here.
public enum GateKind {
    GOBLIN("goblin", "Goblin", false, 0.45f, 1.3f, 0x58a03c, 0x704f30, 14, 3, 0.34, 0, 0, SoundEvents.VINDICATOR_AMBIENT, SoundEvents.VINDICATOR_HURT, SoundEvents.VINDICATOR_DEATH),
    GOBLIN_SHAMAN("goblin_shaman", "Goblin Shaman", false, 0.5f, 1.4f, 0x62984f, 0x60367f, 18, 2, 0.28, 0, 0, SoundEvents.WITCH_AMBIENT, SoundEvents.WITCH_HURT, SoundEvents.WITCH_DEATH),
    HOBGOBLIN("hobgoblin", "Hobgoblin", false, 0.75f, 2.2f, 0xb06434, 0x58524c, 45, 7, 0.24, 6, 0.5, SoundEvents.PIGLIN_BRUTE_AMBIENT, SoundEvents.PIGLIN_BRUTE_HURT, SoundEvents.PIGLIN_BRUTE_DEATH),
    ORC("orc", "Orc", false, 0.7f, 2.1f, 0x627650, 0x7e7e88, 36, 6, 0.26, 4, 0.3, SoundEvents.PIGLIN_BRUTE_ANGRY, SoundEvents.PIGLIN_BRUTE_HURT, SoundEvents.PIGLIN_BRUTE_DEATH),
    ELF("elf", "Elf", false, 0.6f, 1.95f, 0xeed8c0, 0x3a7048, 20, 3, 0.30, 0, 0, SoundEvents.PILLAGER_AMBIENT, SoundEvents.PILLAGER_HURT, SoundEvents.PILLAGER_DEATH),
    DARK_ELF("dark_elf", "Dark Elf", false, 0.6f, 1.95f, 0x56546f, 0x7846aa, 22, 5, 0.36, 0, 0, SoundEvents.EVOKER_AMBIENT, SoundEvents.EVOKER_HURT, SoundEvents.EVOKER_DEATH),
    ANT_SOLDIER("ant_soldier", "Ant Soldier", false, 0.6f, 1.9f, 0x401a18, 0x742c26, 16, 4, 0.34, 2, 0, SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH),
    STONE_GOLEM("stone_golem", "Stone Golem", false, 1.0f, 2.8f, 0x7c7c80, 0x4a7038, 90, 10, 0.20, 8, 1.0, SoundEvents.IRON_GOLEM_STEP, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH),
    GOBLIN_KING("goblin_king", "Goblin King", true, 0.9f, 2.8f, 0x50963a, 0xe8be3c, 220, 9, 0.28, 4, 0.8, SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH),
    HOBGOBLIN_WARLORD("hobgoblin_warlord", "Hobgoblin Warlord", true, 1.0f, 3.2f, 0xaa5c2e, 0x6e1818, 320, 12, 0.26, 8, 0.9, SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH),
    ELF_MAGE("elf_mage", "Elf Mage", true, 0.65f, 2.6f, 0xe6e6f6, 0x3c6ed2, 260, 8, 0.28, 2, 0.5, SoundEvents.EVOKER_AMBIENT, SoundEvents.EVOKER_HURT, SoundEvents.EVOKER_DEATH),
    ANT_QUEEN("ant_queen", "Ant Queen", true, 1.6f, 3.0f, 0x48141a, 0x96362a, 300, 10, 0.26, 6, 0.9, SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH),
    DIRE_WOLF("dire_wolf", "Dire Wolf", false, 0.7f, 0.85f, 0x606068, 0xffc828, 16, 4, 0.38, 0, 0, SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH),
    ALPHA_WOLF("alpha_wolf", "Alpha Wolf", false, 1.1f, 1.3f, 0xc8ccd4, 0xffc828, 34, 6, 0.34, 2, 0.3, SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH),
    DEMON("demon", "Demon", false, 0.6f, 2.0f, 0xaa282c, 0x1e1418, 28, 6, 0.28, 2, 0, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, SoundEvents.ZOMBIFIED_PIGLIN_HURT, SoundEvents.ZOMBIFIED_PIGLIN_DEATH),
    IMP("imp", "Imp", false, 0.5f, 0.9f, 0xc84628, 0x783028, 12, 3, 0.3, 0, 0, SoundEvents.BLAZE_AMBIENT, SoundEvents.BLAZE_HURT, SoundEvents.BLAZE_DEATH),
    DEMON_KNIGHT("demon_knight", "Demon Knight", false, 0.8f, 2.2f, 0x24222c, 0x962c2c, 50, 8, 0.24, 8, 0.5, SoundEvents.PIGLIN_BRUTE_ANGRY, SoundEvents.PIGLIN_BRUTE_HURT, SoundEvents.PIGLIN_BRUTE_DEATH),
    GARGOYLE("gargoyle", "Gargoyle", false, 0.7f, 1.9f, 0x808084, 0xff783c, 40, 7, 0.3, 4, 0.3, SoundEvents.VEX_AMBIENT, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH),
    SKELETON_KNIGHT("skeleton_knight", "Skeleton Knight", false, 0.6f, 2.0f, 0xd6d2be, 0x6e707c, 30, 6, 0.26, 6, 0, SoundEvents.SKELETON_AMBIENT, SoundEvents.SKELETON_HURT, SoundEvents.SKELETON_DEATH),
    WRAITH("wraith", "Wraith", false, 0.6f, 1.9f, 0x28244a, 0x78ffff, 22, 5, 0.3, 0, 0, SoundEvents.VEX_AMBIENT, SoundEvents.VEX_HURT, SoundEvents.VEX_DEATH),
    GHOUL("ghoul", "Ghoul", false, 0.6f, 1.8f, 0x748266, 0xf0dc50, 24, 5, 0.38, 0, 0, SoundEvents.HUSK_AMBIENT, SoundEvents.HUSK_HURT, SoundEvents.HUSK_DEATH),
    LICH("lich", "Lich", false, 0.6f, 2.0f, 0x32204e, 0x965ad2, 40, 4, 0.24, 2, 0, SoundEvents.ILLUSIONER_AMBIENT, SoundEvents.ILLUSIONER_HURT, SoundEvents.ILLUSIONER_DEATH),
    LIZARDMAN("lizardman", "Lizardman", false, 0.6f, 2.0f, 0x468248, 0x56462e, 28, 6, 0.3, 0, 0, SoundEvents.ZOMBIE_VILLAGER_AMBIENT, SoundEvents.ZOMBIE_VILLAGER_HURT, SoundEvents.ZOMBIE_VILLAGER_DEATH),
    SNAKE_WARRIOR("snake_warrior", "Snake Warrior", false, 0.8f, 2.0f, 0x509678, 0xc8c896, 30, 5, 0.26, 0, 0, SoundEvents.ENDERMITE_AMBIENT, SoundEvents.ENDERMITE_HURT, SoundEvents.ENDERMITE_DEATH),
    GIANT_SPIDER("giant_spider", "Giant Spider", false, 1.4f, 0.9f, 0x3c2c24, 0xc82820, 30, 6, 0.32, 0, 0, SoundEvents.SPIDER_AMBIENT, SoundEvents.SPIDER_HURT, SoundEvents.SPIDER_DEATH),
    ICE_BEAR("ice_bear", "Ice Bear", false, 1.5f, 1.5f, 0xe2e8f0, 0x3c78c8, 70, 9, 0.26, 4, 0.6, SoundEvents.POLAR_BEAR_AMBIENT, SoundEvents.POLAR_BEAR_HURT, SoundEvents.POLAR_BEAR_DEATH),
    ICE_ARCHER("ice_archer", "Ice Archer", false, 0.6f, 1.95f, 0xd6e6f5, 0x466eaa, 20, 3, 0.3, 0, 0, SoundEvents.STRAY_AMBIENT, SoundEvents.STRAY_HURT, SoundEvents.STRAY_DEATH),
    SENTRY_DRONE("sentry_drone", "Sentry Drone", false, 0.7f, 0.7f, 0x96a0b0, 0x50c8ff, 14, 3, 0.3, 0, 0, SoundEvents.BEE_LOOP, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH),
    CYBER_SOLDIER("cyber_soldier", "Cyber Soldier", false, 0.6f, 1.95f, 0x343e54, 0x50dcff, 26, 5, 0.28, 4, 0, SoundEvents.PILLAGER_AMBIENT, SoundEvents.PILLAGER_HURT, SoundEvents.PILLAGER_DEATH),
    BULWARK_MECH("bulwark_mech", "Bulwark Mech", false, 1.0f, 2.6f, 0x78809a, 0xff9628, 80, 9, 0.2, 10, 1.0, SoundEvents.IRON_GOLEM_STEP, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH),
    ALIEN_CRAWLER("alien_crawler", "Alien Crawler", false, 0.9f, 0.7f, 0x1e2228, 0x3ca078, 22, 5, 0.36, 0, 0, SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH),
    ALIEN("alien", "Alien", false, 0.6f, 2.1f, 0x3c285a, 0xc878ff, 24, 5, 0.3, 0, 0, SoundEvents.ENDERMAN_AMBIENT, SoundEvents.ENDERMAN_HURT, SoundEvents.ENDERMAN_DEATH),
    MIMIC("mimic", "Mimic", false, 1.0f, 0.9f, 0x8c6032, 0xd2aa3c, 40, 8, 0.3, 2, 0, SoundEvents.SHULKER_AMBIENT, SoundEvents.SHULKER_HURT, SoundEvents.SHULKER_DEATH),
    SLIME_KNIGHT("slime_knight", "Slime Knight", false, 0.9f, 0.9f, 0x5ac85a, 0x969caa, 26, 5, 0.26, 3, 0, SoundEvents.SLIME_JUMP, SoundEvents.SLIME_HURT, SoundEvents.SLIME_DEATH),
    SLIME_KING("slime_king", "Slime King", false, 1.4f, 1.4f, 0x5ac85a, 0xe8c450, 90, 8, 0.22, 2, 0.5, SoundEvents.SLIME_JUMP, SoundEvents.SLIME_HURT, SoundEvents.SLIME_DEATH),
    MUSHROOM_MAN("mushroom_man", "Mushroom Man", false, 0.6f, 1.6f, 0xc83232, 0xe2d8be, 20, 3, 0.24, 0, 0, SoundEvents.HUSK_AMBIENT, SoundEvents.HUSK_HURT, SoundEvents.HUSK_DEATH),
    MINOTAUR("minotaur", "Minotaur", false, 0.9f, 2.4f, 0x603e2c, 0xe6e0c8, 80, 10, 0.26, 4, 0.8, SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH),
    MUMMY("mummy", "Mummy", false, 0.6f, 1.95f, 0xcec096, 0xff5a3c, 30, 5, 0.22, 0, 0, SoundEvents.HUSK_AMBIENT, SoundEvents.HUSK_HURT, SoundEvents.HUSK_DEATH),
    SCARAB("scarab", "Scarab", false, 0.4f, 0.3f, 0x28466e, 0x5abedc, 6, 2, 0.4, 0, 0, SoundEvents.SILVERFISH_AMBIENT, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH),
    SAND_GOLEM("sand_golem", "Sand Golem", false, 1.0f, 2.4f, 0xceb270, 0xff7828, 70, 8, 0.2, 4, 0.9, SoundEvents.IRON_GOLEM_STEP, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH),
    WOLF_KING("wolf_king", "Wolf King", true, 1.4f, 1.6f, 0x464450, 0xe8c450, 260, 10, 0.34, 4, 0.8, SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH),
    DEMON_LORD("demon_lord", "Demon Lord", true, 1.0f, 3.2f, 0x1e1824, 0xaa1e24, 340, 13, 0.26, 8, 0.9, SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH),
    LICH_LORD("lich_lord", "Lich Lord", true, 0.7f, 2.9f, 0x1e2846, 0x6edcff, 260, 9, 0.26, 4, 0.6, SoundEvents.WITHER_SKELETON_AMBIENT, SoundEvents.WITHER_SKELETON_HURT, SoundEvents.WITHER_SKELETON_DEATH),
    SNAKE_QUEEN("snake_queen", "Snake Queen", true, 1.3f, 2.8f, 0x6478c8, 0xe8c450, 280, 10, 0.26, 4, 0.8, SoundEvents.ENDERMITE_AMBIENT, SoundEvents.ENDERMITE_HURT, SoundEvents.ENDERMITE_DEATH),
    FROST_KING("frost_king", "Frost King", true, 0.9f, 3.0f, 0x3250aa, 0xc8e6ff, 320, 11, 0.26, 6, 0.9, SoundEvents.STRAY_AMBIENT, SoundEvents.STRAY_HURT, SoundEvents.STRAY_DEATH),
    WAR_MECH("war_mech", "War Mech", true, 1.4f, 3.4f, 0x8c5a5a, 0xff7828, 380, 12, 0.22, 12, 1.0, SoundEvents.IRON_GOLEM_STEP, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH),
    TOMB_KING("tomb_king", "Tomb King", true, 0.9f, 2.9f, 0xd2be8c, 0x3c64c8, 290, 10, 0.26, 4, 0.8, SoundEvents.HUSK_AMBIENT, SoundEvents.HUSK_HURT, SoundEvents.HUSK_DEATH),
    YOUNG_DRAGON("young_dragon", "Young Dragon", true, 2.2f, 2.6f, 0x3c7846, 0xe8c450, 340, 12, 0.28, 6, 0.9, SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH);

    public final String id, title;
    public final boolean boss;
    public final float width, height;
    public final int eggA, eggB;
    public final double health, damage, speed, armor, knockback;
    public final SoundEvent ambient, hurt, death;

    GateKind(String id, String title, boolean boss, float width, float height, int eggA, int eggB, double health, double damage, double speed, double armor, double knockback,
             SoundEvent ambient, SoundEvent hurt, SoundEvent death) {
        this.id = id;
        this.title = title;
        this.boss = boss;
        this.width = width;
        this.height = height;
        this.eggA = eggA;
        this.eggB = eggB;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.knockback = knockback;
        this.ambient = ambient;
        this.hurt = hurt;
        this.death = death;
    }
}
