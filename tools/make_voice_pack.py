# Turns your voice recordings into a resource pack for the Translation power. Recordings never go in the mod's jar.
#   python tools/make_voice_pack.py "C:/path/to/my voices" "~/Downloads/{C} Char's Voices Pack.zip"
# Folders: my voices/<mob>/<funny|sad|useful>/anything.mp3 (or .ogg/.wav), e.g. my voices/zombie/funny/hello.mp3
# <mob> is the mob's name in the game: zombie, iron_golem, cave_spider. Every file becomes one short mono OGG.
import json
import os
import re
import sys
import tempfile
import zipfile

import soundfile


def clean(name):
    return re.sub(r'[^a-z0-9_]', '_', name.lower())


def main(src, out):
    sounds = {}
    with tempfile.TemporaryDirectory() as tmp, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        for mob in sorted(os.listdir(src)):
            for kind in ('funny', 'sad', 'useful'):
                folder = os.path.join(src, mob, kind)
                if not os.path.isdir(folder):
                    continue
                for file in sorted(os.listdir(folder)):
                    if not file.lower().endswith(('.mp3', '.ogg', '.wav', '.flac')):
                        continue
                    name = clean(os.path.splitext(file)[0])
                    data, rate = soundfile.read(os.path.join(folder, file), always_2d=True)
                    made = os.path.join(tmp, name + '.ogg')
                    soundfile.write(made, data.mean(axis=1), rate, format='OGG', subtype='VORBIS')
                    z.write(made, f'assets/charpowers/sounds/voice/{clean(mob)}/{kind}/{name}.ogg')
                    sounds[f'voice.{clean(mob)}.{kind}.{name}'] = {
                        'sounds': [{'name': f'charpowers:voice/{clean(mob)}/{kind}/{name}', 'stream': False}]}
        z.writestr('assets/charpowers/sounds.json', json.dumps(sounds, indent=1))
        z.writestr('pack.mcmeta', json.dumps({'pack': {'pack_format': 15, 'description': "Char's voice lines for Translation"}}))
    print(f'{len(sounds)} lines -> {out}')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
