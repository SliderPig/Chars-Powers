# Translation's voice lines

Your recordings go in a resource pack, never in the mod.

1. Make a folder with one folder per mob, then `funny`, `sad` or `useful` inside: `my voices/zombie/funny/hello.mp3`. The mob's name is its name in the game (`zombie`, `iron_golem`, `cave_spider`). Mods' mobs work the same way, without the mod's name in front.
2. Run this from the charpowers folder (it makes each one a small mono OGG):

```bash
python tools/make_voice_pack.py "C:/path/to/my voices" "~/Downloads/{C} Char's Voices Pack.zip"
```

3. Turn the pack on in Options, Resource Packs. Anyone with Translation who should hear the lines needs it.

Check in Options, Music & Sounds, Mob Voices: it lists every mob and line it found. Your "Voice/Speech" slider controls the volume.
