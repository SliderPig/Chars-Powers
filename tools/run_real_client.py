# The closest test to really playing: starts the real game the way the Modrinth app does, with the same Forge and the Powers
# profile's own copies of the mods we need, in a throwaway folder (run-real/), never touching the profile itself. The jar
# must be built with -Psmoke so ClientChecks is in it: it walks through everything, saves screenshots to run-real/screenshots and quits.
#   gradlew build -Psmoke -x test   then   python tools/run_real_client.py
import glob
import json
import os
import shutil
import subprocess
import sys

M = os.path.join(os.environ['APPDATA'], 'ModrinthApp')
META = os.path.join(M, 'meta')
VERSION = '1.20.1-47.4.28'
JAVA = r'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe'
PROFILE_MODS = os.path.join(M, 'profiles', 'Powers', 'mods')
NEEDED = ['palladium-', 'geckolib-forge', 'curtain-', 'embeddium-', 'architectury-', 'packetfixer-']
GUEST = 'guest' in sys.argv  # a second game that joins one started with 'lanhost', like a friend joining your world
RUN = os.path.abspath('run-real-guest' if GUEST else 'run-real')
TIMEOUT = 560


def allowed(rules):
    ok = not rules
    for r in rules or []:
        name = (r.get('os') or {}).get('name')
        if name in (None, 'windows') and 'features' not in r:
            ok = r['action'] == 'allow'
    return ok


def library_path(lib):
    art = (lib.get('downloads') or {}).get('artifact')
    if art and art.get('path'):
        return art['path']
    group, name, ver = lib['name'].split(':')[:3]
    return f"{group.replace('.', '/')}/{name}/{ver}/{name}-{ver}.jar"


def main():
    global NEEDED
    if 'pack' in sys.argv:  # exactly what the modpack holds, to prove it starts with nothing else
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        from make_mrpack import MODS
        NEEDED = MODS
    d = json.load(open(os.path.join(META, 'versions', VERSION, VERSION + '.json'), encoding='utf-8'))
    cp, seen = [], set()
    for lib in d['libraries']:
        if lib.get('include_in_classpath') is False or not allowed(lib.get('rules')) or 'natives' in lib['name'].split(':')[-1] and 'windows' not in lib['name']:
            continue
        path = os.path.join(META, 'libraries', library_path(lib))
        if os.path.exists(path) and path not in seen:
            seen.add(path)
            cp.append(path)
    cp.append(os.path.join(META, 'versions', VERSION, VERSION + '.jar'))

    os.makedirs(os.path.join(RUN, 'mods'), exist_ok=True)
    shutil.rmtree(os.path.join(RUN, 'saves', 'smoke'), ignore_errors=True)
    shutil.rmtree(os.path.join(RUN, 'screenshots'), ignore_errors=True)
    # a brand new game folder opens the first-launch Narrator screen and waits for a click for ever
    options_file = os.path.join(RUN, 'options.txt')
    old_options = []
    if os.path.exists(options_file):
        with open(options_file, encoding='utf-8') as f:
            old_options = [line for line in f.read().splitlines() if not line.startswith(('onboardAccessibility:', 'pauseOnLostFocus:'))]
    with open(options_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(old_options + ['onboardAccessibility:false', 'pauseOnLostFocus:false']) + '\n')
    for old in glob.glob(os.path.join(RUN, 'mods', '*.jar')):
        os.remove(old)
    for name in os.listdir(PROFILE_MODS):
        if name.endswith('.jar') and any(name.startswith(n) for n in NEEDED):
            shutil.copy(os.path.join(PROFILE_MODS, name), os.path.join(RUN, 'mods', name))
    # punchy=<jar>: with Punchy in the game too, and only the short check of the weapons in first person
    punchy = [a[len('punchy='):] for a in sys.argv if a.startswith('punchy=')]
    for jar in punchy:
        shutil.copy(jar, os.path.join(RUN, 'mods', os.path.basename(jar)))
    # anim: stays open and does what run-real/lab/todo.txt says (see AnimChecks), to look at player poses without a restart
    anim = 'anim' in sys.argv or 'animsheet' in sys.argv
    if anim:
        os.makedirs(os.path.join(RUN, 'lab'), exist_ok=True)
    if 'animsheet' in sys.argv:  # not a lab: every pose and the minigun from every side, then it quits by itself
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import anim_lab
        if os.path.exists(os.path.join(RUN, 'config', 'charpowers', 'player_poses.json')):
            os.remove(os.path.join(RUN, 'config', 'charpowers', 'player_poses.json'))
        with open(os.path.join(RUN, 'lab', 'todo.txt'), 'w', encoding='utf-8') as f:
            f.write('\n'.join(['wait 60'] + anim_lab.gun_lines() + anim_lab.pose_lines(anim_lab.POSES) + ['quit']) + '\n')
    # lab: stays open and reloads run-real/resourcepacks/lab each time run-real/lab/go appears, to try poses without a restart
    lab = 'lab' in sys.argv
    if lab:
        os.makedirs(os.path.join(RUN, 'lab'), exist_ok=True)
        pack = os.path.join(RUN, 'resourcepacks', 'lab')
        os.makedirs(pack, exist_ok=True)
        with open(os.path.join(pack, 'pack.mcmeta'), 'w', encoding='utf-8') as f:
            json.dump({'pack': {'pack_format': 15, 'description': 'lab'}}, f)
        options = os.path.join(RUN, 'options.txt')
        with open(options, encoding='utf-8') as f:
            lines = f.read().splitlines()
        with open(options, 'w', encoding='utf-8') as f:
            f.write('\n'.join('resourcePacks:["vanilla","mod_resources","file/lab"]' if line.startswith('resourcePacks:') else line for line in lines) + '\n')
    for jar in [a[len('extra='):] for a in sys.argv if a.startswith('extra=')]:  # another mod to try alongside
        shutil.copy(jar, os.path.join(RUN, 'mods', os.path.basename(jar)))
    built = sorted(glob.glob('build/libs/charpowers-*.jar'), key=os.path.getmtime)[-1]
    shutil.copy(built, os.path.join(RUN, 'mods', 'charpowers-smoke.jar'))
    print('mods:', sorted(os.listdir(os.path.join(RUN, 'mods'))))

    natives = os.path.join(META, 'natives', VERSION)
    values = {'natives_directory': natives, 'launcher_name': 'charpowers-test', 'launcher_version': '1', 'classpath': os.pathsep.join(cp), 'classpath_separator': os.pathsep,
              'library_directory': os.path.join(META, 'libraries'), 'version_name': VERSION, 'auth_player_name': 'Dev2' if GUEST else 'Dev', 'game_directory': RUN, 'assets_root': os.path.join(META, 'assets'),
              'assets_index_name': d['assets'], 'auth_uuid': '00000000000000000000000000000abd' if GUEST else '00000000000000000000000000000abc', 'auth_access_token': '0', 'clientid': '0', 'auth_xuid': '0', 'user_type': 'legacy', 'version_type': 'release'}

    def fill(text):
        for key, value in values.items():
            text = text.replace('${' + key + '}', value)
        return text

    def flat(args):
        out = []
        for a in args:
            if isinstance(a, str):
                out.append(fill(a))
            elif allowed(a.get('rules')):
                v = a['value']
                out.extend(fill(x) for x in ([v] if isinstance(v, str) else v))
        return out

    flags = {'house': 'housecheck', 'steal': 'stealcheck', 'small': 'smallcheck', 'pets': 'petcheck', 'gui': 'guicheck', 'guimods': 'guimods', 'lanternlab': 'lanternlab', 'features': 'featurecheck', 'icons': 'iconcheck', 'lanhost': 'lanhost', 'items': 'itemcheck', 'anim': 'animcheck', 'animsheet': 'animcheck', 'lineup': 'lineupcheck'}
    cmd = [JAVA, '-Xmx2500M', '-Dcharpowers.smokeclient=true'] + (['-Dcharpowers.punchycheck=true'] if punchy else []) + (['-Dcharpowers.punchylab=true'] if lab else []) + (['-Dcharpowers.languest=true'] if GUEST else []) + [f'-Dcharpowers.{v}=true' for k, v in flags.items() if k in sys.argv] + ['-Dcharpowers.itemonly=' + a[len('only='):] for a in sys.argv if a.startswith('only=')] + flat(d['arguments']['jvm']) + [d['mainClass']] + flat(d['arguments']['game']) + ['--width', '1600', '--height', '900']
    with open(os.path.join(RUN, 'launch.log'), 'w', encoding='utf-8', errors='replace') as log:
        try:
            result = subprocess.run(cmd, cwd=RUN, stdout=log, stderr=subprocess.STDOUT, timeout=3000 if (lab or 'lanternlab' in sys.argv) else int(os.environ.get('ANIM_SECONDS', 560)) if anim else TIMEOUT)
            print('exit', result.returncode)
        except subprocess.TimeoutExpired:
            print('timed out after', TIMEOUT, 's')
    print('screenshots:', len(glob.glob(os.path.join(RUN, 'screenshots', '*.png'))))
    if 'animsheet' in sys.argv:
        import anim_lab
        anim_lab.sheet(anim_lab.POSES)
    return 0


if __name__ == '__main__':
    sys.exit(main())
