# Char's Powers

My own powers mod for Minecraft Forge 1.20.1, built on Palladium.

## What's in it

- **Main powers**, each with mastery (1-100) and an awakening: Cloning, Teacher, Translation, Future Sight, Superior Authority, The Miner, Life Enjoyer, Vocal Expression, Time Control, Necromancy, Mob Control, Death Steal, Invincible Armour, Green Lantern, Book Keeping, Game Player and House Builder.
- **Hidden powers**: 381 small powers, ranks F to Z, that mobs can spawn with and players can collect.
- **Game Player raids**: gates with themed rooms, 36 creatures, 12 bosses, a shop, skills and raids with friends.
- **Weapons**: Ice Staff, Golden King's Long Sword, Flame Hammer, Fork, Spray Bottle, Dagger of Darkness, Anima Katana, and the Game Player's system weapons, each with its own swings.
- Player animations for every power, original sounds, an icon for every power, and rounded menus.

## Needs

- Minecraft 1.20.1 with Forge 47.x
- Palladium 4.5.9 (with Architectury)
- GeckoLib 4.8.4
- Curtain 1.3.2 for Cloning's clones (optional)

## Building

Put the Palladium, Palladium Core, GeckoLib and Curtain jars in `libs/`, then run `gradlew build`. The jar ends up in `build/libs/`.

- `gradlew test` runs the unit tests.
- `gradlew runGameTestServer -Psmoke` runs the headless game tests.

## Tools

The `tools/` folder has the Python scripts that make the art, icons, sounds, houses and power files.

## Credits

- The sounds listed in `src/main/resources/assets/charpowers/sounds/CREDITS.md` are CC0 sounds from Freesound; all other sounds were made for this mod.
