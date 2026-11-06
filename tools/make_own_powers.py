# Makes the Palladium power files and the language lines for Char's own powers from one short table each, so a new
# power is a few lines here. Run from the charpowers folder:  python tools/make_own_powers.py
# Writes src/main/resources/data/charpowers/palladium/powers/<id>.json and merges text into lang/en_us.json.
# Each power has its own file in tools/own_powers/ (one power's table), so this file stays small.
import glob
import importlib.util
import json
import os

POWERS_DIR = 'src/main/resources/data/charpowers/palladium/powers'
LANG = 'src/main/resources/assets/charpowers/lang/en_us.json'
ICONS = 'src/main/resources/assets/charpowers/textures/icons'
UNLOCKS = 'src/main/resources/assets/charpowers/mastery_unlocks.json'


def icon(name, placeholder):
    # your own drawing if it's there (textures/icons/<name>.png), otherwise the placeholder item
    if os.path.exists(os.path.join(ICONS, name + '.png')):
        return {'type': 'palladium:texture', 'texture': f'charpowers:textures/icons/{name}.png'}
    return placeholder


def ability_json(power, a):
    key = a['id']
    out = {
        'type': a.get('type', 'charpowers:key' if a['kind'] in ('key', 'held') else 'charpowers:passive'),
        'title': {'translate': f'ability.charpowers.{key}'},
        'description': {'translate': f'ability.charpowers.{key}.desc'},
        'icon': icon(key, a['icon']),
    }
    out.update(a.get('props', {}))
    if a['kind'] == 'passive':
        out['hidden_in_bar'] = True
    else:
        out['list_index'] = a['slot']
    unlocking = []
    if a.get('after'):
        unlocking.append({'type': 'palladium:ability_unlocked', 'power': 'null', 'ability': a['after']})
    if a.get('level', 1) > 1:
        unlocking.append({'type': 'charpowers:mastery_level', 'power': f'charpowers:{power}', 'level': a['level']})
    if a.get('awakened'):
        unlocking.append({'type': 'charpowers:awakened', 'power': f'charpowers:{power}'})
    unlocking.extend(a.get('unlock_extra', []))
    conditions = {}
    if unlocking:
        conditions['unlocking'] = unlocking
    if a['kind'] in ('key', 'toggle', 'held'):
        kind = {'key': 'palladium:action', 'toggle': 'palladium:toggle', 'held': 'palladium:held'}[a['kind']]
        press = {'type': kind, 'key_type': 'key_bind', 'needs_empty_hand': False,
                 'allow_scrolling_when_crouching': True}
        if a['kind'] != 'held':
            press['cooldown'] = a.get('cooldown', 0)
        conditions['enabling'] = [press]
    if conditions:
        out['conditions'] = conditions
    out['gui_position'] = list(a['pos'])
    return out


def main():
    with open(LANG, encoding='utf-8') as f:
        lang = json.load(f)
    tables = sorted(glob.glob('tools/own_powers/*.py'))
    unlocks = {}  # for the mastery toast: what each power gets at which level
    for path in tables:
        spec = importlib.util.spec_from_file_location(os.path.basename(path)[:-3], path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        p = mod.POWER
        pid = p['id']
        lang[f'power.charpowers.{pid}'] = p['name']
        lang.update(p.get('lang', {}))
        for part in ('about', 'how', 'mastery', 'quest', 'awakening', 'quest_hint', 'abilities_text'):
            if part in p:
                lang[f'power.charpowers.{pid}.{part}'] = p[part]
        if 'abilities' not in p:  # the power file is written by hand (Cloning): only its guide text and icons come from here
            path = os.path.join(POWERS_DIR, pid + '.json')
            with open(path, encoding='utf-8') as f:
                power = json.load(f)
            placeholders = p.get('placeholders', {})
            power['icon'] = icon(pid, placeholders.get(pid, power['icon']))
            for key, a in power['abilities'].items():
                a['icon'] = icon(key, placeholders.get(key, a.get('icon')))
            with open(path, 'w', encoding='utf-8', newline='\n') as f:
                json.dump(power, f, indent=2)
                f.write('\n')
            continue
        lines = []
        abilities = {}
        for a in p['abilities']:
            abilities[a['id']] = ability_json(pid, a)
            lang[f"ability.charpowers.{a['id']}"] = a['name']
            lang[f"ability.charpowers.{a['id']}.desc"] = a['desc']
            lines.append(f"{a['name']}: {a['desc']}")
        lang[f'power.charpowers.{pid}.abilities_text'] = '\n'.join(lines)
        unlocks[pid] = [{'level': a['level'], 'name': a['name']} for a in p['abilities'] if a.get('level', 1) > 1]
        power = {
            'name': {'translate': f'power.charpowers.{pid}'},
            'icon': icon(pid, p['icon']),
            'gui_display_type': 'tree',
            'persistent_data': True,
            'abilities': abilities,
        }
        if 'energy_bars' in p:
            power['energy_bars'] = p['energy_bars']
        with open(os.path.join(POWERS_DIR, pid + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(power, f, indent=2)
            f.write('\n')
    with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write('\n')
    with open(UNLOCKS, 'w', encoding='utf-8', newline=chr(10)) as f:
        json.dump({k: v for k, v in sorted(unlocks.items()) if v}, f, indent=1)
        f.write(chr(10))
    print('made', len(tables), 'powers')


if __name__ == '__main__':
    main()
