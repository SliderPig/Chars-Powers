# Writes docs/icons-to-make.md: every icon the own powers use, and which ones you've drawn so far.
# Run from the charpowers folder:  python tools/list_icons.py
# To add one: save <name>.png (16x16, or any square size) in src/main/resources/assets/charpowers/textures/icons/,
# then run python tools/make_own_powers.py. Until a PNG is there, the power uses the placeholder item listed here.
import glob
import importlib.util
import json
import os

ICONS = 'src/main/resources/assets/charpowers/textures/icons'
POWERS = 'src/main/resources/data/charpowers/palladium/powers'

# Powers on the list that aren't built yet. Names can still change when they are.
NOT_BUILT = {
}


def has(name):
    return os.path.exists(os.path.join(ICONS, name + '.png'))


def main():
    built = []
    for path in sorted(glob.glob('tools/own_powers/*.py')):
        spec = importlib.util.spec_from_file_location(os.path.basename(path)[:-3], path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        p = mod.POWER
        if 'abilities' in p:
            rows = [(a['id'], a['name'], a['icon']) for a in p['abilities']]
            built.append((p['id'], p['name'], p['icon'], rows))
        else:  # written by hand: read its file
            with open(os.path.join(POWERS, p['id'] + '.json'), encoding='utf-8') as f:
                data = json.load(f)
            ph = p.get('placeholders', {})
            rows = [(k, k.replace('_', ' ').title(), ph.get(k, '')) for k in data['abilities']]
            built.append((p['id'], p['name'], ph.get(p['id'], ''), rows))
    out = ['# Icons to make', '',
           'Save each one as `<file name>.png` (16x16, or any square size) in `src/main/resources/assets/charpowers/textures/icons/`, then run `python tools/make_own_powers.py`.', '']
    total = done = 0
    for pid, name, icon, rows in built:
        out += [f'## {name}', '', '| File name | What it is | Placeholder for now | Drawn? |', '|---|---|---|---|']
        out.append(f'| `{pid}` | The power itself | {icon} | {"yes" if has(pid) else ""} |')
        total += 1
        done += has(pid)
        for aid, aname, aicon in rows:
            out.append(f'| `{aid}` | {aname} | {aicon} | {"yes" if has(aid) else ""} |')
            total += 1
            done += has(aid)
        out.append('')
    out += ['## Not built yet (names may change)', '']
    later = 0
    for pid, names in NOT_BUILT.items():
        out.append(f'- **{pid.replace("_", " ").title()}:** `{pid}`, ' + ', '.join(f'`{n}`' for n in names))
        later += 1 + len(names)
    out += ['', f'{done} of {total} drawn for the built powers, plus about {later} for the powers not built yet.', '']
    os.makedirs('docs', exist_ok=True)
    with open('docs/icons-to-make.md', 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out))
    print('\n'.join(out))


if __name__ == '__main__':
    main()
