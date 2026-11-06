# Makes a Modrinth modpack with Char's Powers and everything it needs. It takes the exact jars from the Powers profile and asks Modrinth
# for each one by its hash: if Modrinth has it the pack just points there, if not the jar goes inside the pack. Your own mods always go inside.
# From the charpowers folder:  python tools/make_mrpack.py <version>
import hashlib
import json
import os
import sys
import urllib.error
import urllib.request
import zipfile

PROFILE_MODS = os.path.join(os.environ['APPDATA'], 'ModrinthApp', 'profiles', 'Powers', 'mods')
# Char's Powers needs palladium (architectury, and palladiumcore comes inside it) and geckolib. Cloning needs curtain.
# The suit's recipes need gravecore, and gravecore needs curios, lodestone and packetfixer.
MODS = ['palladium-', 'architectury-', 'geckolib-forge', 'curtain-', 'gravecore-', 'curios-forge', 'lodestone-', 'packetfixer-']
FORGE, MINECRAFT = '47.4.28', '1.20.1'
EXTRA_OWN = ['{C} charmobmaker-1.0.jar', '{C} charteams-1.0.jar', '{C} charmap-1.0.jar', '{C} charlod-1.0.jar', '{C} charmenu-1.0.jar', '{C} clogger-1.0.jar']


def hashes(path):
    with open(path, 'rb') as f:
        data = f.read()
    return hashlib.sha1(data).hexdigest(), hashlib.sha512(data).hexdigest(), len(data)


def on_modrinth(sha512):
    request = urllib.request.Request(f'https://api.modrinth.com/v2/version_file/{sha512}?algorithm=sha512', headers={'User-Agent': 'charpowers-pack-tool/1.0'})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            version = json.load(response)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise
    for file in version['files']:
        if file['hashes'].get('sha512') == sha512:
            return file['url']
    return None


def main(version):
    names = os.listdir(PROFILE_MODS)
    files, inside = [], []
    for prefix in MODS:
        found = [n for n in names if n.startswith(prefix) and n.endswith('.jar')]
        if len(found) != 1:
            raise SystemExit(f'Expected one jar for {prefix}, found {found}')
        name = found[0]
        path = os.path.join(PROFILE_MODS, name)
        sha1, sha512, size = hashes(path)
        url = on_modrinth(sha512)
        if url:
            print('from Modrinth:', name)
            files.append({'path': 'mods/' + name, 'hashes': {'sha1': sha1, 'sha512': sha512}, 'env': {'client': 'required', 'server': 'required'}, 'downloads': [url], 'fileSize': size})
        else:
            print('inside the pack (not on Modrinth):', name)
            inside.append((path, name))
    own = '{C} charpowers-' + version + '.jar'
    downloads = os.path.join(os.environ['USERPROFILE'], 'Downloads')
    # the jar can be switched off in the profile (.disabled); the copy in Downloads is the same file
    own_path = next((p for p in [os.path.join(PROFILE_MODS, own), os.path.join(PROFILE_MODS, own + '.disabled'), os.path.join(downloads, own)] if os.path.exists(p)), None)
    if own_path is None:
        raise SystemExit('No ' + own + ' in the profile or in Downloads')
    print('inside the pack (yours):', own)
    inside.append((own_path, own))
    # your other mods that a friend joining your world needs too (they add blocks, mobs or menus)
    for extra in EXTRA_OWN:
        path = os.path.join(downloads, extra)
        if os.path.exists(path):
            print('inside the pack (yours):', extra)
            inside.append((path, extra))
    index = {'formatVersion': 1, 'game': 'minecraft', 'versionId': version, 'name': "Char's Powers", 'summary': "Char's Powers and the mods it needs.",
             'files': files, 'dependencies': {'minecraft': MINECRAFT, 'forge': FORGE}}
    out = os.path.join(os.environ['USERPROFILE'], 'Downloads', "{C} Char's Powers " + version + '.mrpack')
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('modrinth.index.json', json.dumps(index, indent=2))
        for path, name in inside:
            z.write(path, 'overrides/mods/' + name)
    print('wrote', out, os.path.getsize(out), 'bytes,', len(files), 'downloaded on import,', len(inside), 'inside')


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '1.1')
