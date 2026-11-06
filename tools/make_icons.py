# Draws Char's Powers icons: one 32x32 picture for every main power, ability and hidden power, with a table saying what
# each one shows, a "contact sheet" to check them with, and a check that nothing is missing.
# Run from the charpowers folder:  python tools/make_icons.py          draws all, checks them, writes the contact sheets
#                                  python tools/make_icons.py --check  only checks what is there
# Never overwrites a picture this script didn't draw (its own carry a mark in the PNG), so hand-drawn ones are safe.
# After drawing: python tools/make_own_powers.py   makes the power files use them.
import glob
import importlib.util
import json
import os
import sys

from PIL import Image, ImageDraw, PngImagePlugin

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from icon_art.paint import OUT, fitted  # noqa: E402
from icon_art.hidden_table import H  # noqa: E402
from icon_art.main_table import EXTRA, M, SKIP_MAIN  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICONS = os.path.join(ROOT, 'src/main/resources/assets/charpowers/textures/icons')
LANG = os.path.join(ROOT, 'src/main/resources/assets/charpowers/lang/en_us.json')
POWERS = os.path.join(ROOT, 'src/main/resources/data/charpowers/palladium/powers')
SHEETS = os.path.join(ROOT, 'docs/previews')
MARK = 'charpowers-make-icons'


def draw(recipe):
    return fitted(recipe)


def is_ours(path):
    try:
        with Image.open(path) as im:
            return im.info.get('Software') == MARK
    except OSError:
        return False


def save(im, path):
    if os.path.exists(path) and not is_ours(path):
        print('kept (not drawn by this script):', os.path.relpath(path, ROOT))
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    meta = PngImagePlugin.PngInfo()
    meta.add_text('Software', MARK)
    im.save(path, pnginfo=meta, optimize=True)


def hidden_ids():
    with open(LANG, encoding='utf-8') as f:
        lang = json.load(f)
    return lang, [k[len('power.charpowers.'):-5] for k in lang if k.startswith('power.charpowers.') and k.endswith('.desc')]


def main_ids():
    """(power, [(id, name)]) for every power file, from the power files themselves."""
    with open(LANG, encoding='utf-8') as f:
        lang = json.load(f)
    out = []
    for path in sorted(glob.glob(os.path.join(POWERS, '*.json'))):
        pid = os.path.basename(path)[:-5]
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        rows = [(pid, lang.get(f'power.charpowers.{pid}', pid))]
        for key in data.get('abilities', {}):
            if pid == 'hidden_powers':  # its five slots show whichever hidden power is in them
                continue
            rows.append((key, lang.get(f'ability.charpowers.{key}', key.replace('_', ' ').title())))
        out.append((pid, rows))
    return out


def problems_of(im, name):
    out = []
    if im.size != (32, 32):
        out.append(f'{name}: is {im.size}, not 32x32')
        return out
    alpha = im.convert('RGBA').getchannel('A')
    n = sum(1 for v in alpha.tobytes() if v > 0)
    if n < 60:
        out.append(f'{name}: nearly blank ({n} pixels)')
    if all(alpha.getpixel(p) > 0 for p in ((0, 0), (31, 0), (0, 31), (31, 31))):
        out.append(f'{name}: no background left')
    rgba = im.convert('RGBA')
    edge = [(i, 0) for i in range(32)] + [(i, 31) for i in range(32)] + [(0, i) for i in range(32)] + [(31, i) for i in range(32)]
    if any(rgba.getpixel(p)[3] > 0 and rgba.getpixel(p)[:3] != OUT[:3] for p in edge):
        out.append(f'{name}: runs off the edge (its dark outline is cut off)')
    return out


def check():
    """Every id has a picture, 32x32, not blank, and no two exactly the same."""
    problems = []
    seen = {}
    lang, hidden = hidden_ids()
    names = [(i, os.path.join(ICONS, 'hidden', i + '.png')) for i in hidden]
    for _pid, rows in main_ids():
        for key, _ in rows:
            if key not in SKIP_MAIN:
                names.append((key, os.path.join(ICONS, key + '.png')))
    for name, path in names:
        if not os.path.exists(path):
            problems.append(f'{name}: no picture')
            continue
        with Image.open(path) as im:
            problems += problems_of(im, name)
            data = im.convert('RGBA').tobytes()
        if data in seen:
            problems.append(f'{name}: exactly the same picture as {seen[data]}')
        seen[data] = name
    return problems, len(names)


def contact_sheet(rows, title, path, cols=10):
    """rows: (image, name). Shown at twice size, with the real size and half size beside each, on the menu's plum."""
    cw, ch = 106, 92
    nrows = (len(rows) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cw + 8, nrows * ch + 24), (46, 30, 54, 255))
    d = ImageDraw.Draw(sheet)
    d.text((8, 6), title, fill=(255, 170, 210, 255))
    for i, (im, name) in enumerate(rows):
        x, y = 6 + (i % cols) * cw, 22 + (i // cols) * ch
        big = im.resize((64, 64), Image.NEAREST)
        sheet.alpha_composite(big, (x, y))
        small = im.resize((16, 16), Image.NEAREST)
        sheet.alpha_composite(small, (x + 68, y + 2))
        sheet.alpha_composite(im, (x + 68, y + 24))
        label = name.replace('_', ' ')
        d.text((x, y + 66), label[:15], fill=(255, 225, 240, 255))
        if len(label) > 15:
            d.text((x, y + 76), label[15:30], fill=(255, 225, 240, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.convert('RGB').save(path, optimize=True)


def main():
    if '--check' not in sys.argv:
        lang, hidden = hidden_ids()
        missing = [i for i in hidden if i not in H]
        extra = [i for i in H if i not in hidden]
        if missing or extra:
            print('hidden table is out of step. no recipe:', missing, 'unknown:', extra)
        drawn = {}
        for pid in hidden:
            if pid in H:
                im = draw(H[pid])
                drawn[pid] = im
                save(im, os.path.join(ICONS, 'hidden', pid + '.png'))
        groups = main_ids()
        main_drawn = {}
        for key, recipe in M.items():
            im = draw(recipe)
            main_drawn[key] = im
            save(im, os.path.join(ICONS, key + '.png'))
        for key, recipe in EXTRA.items():
            save(draw(recipe), os.path.join(ICONS, key + '.png'))
        # contact sheets
        for old in glob.glob(os.path.join(SHEETS, 'icons_*.png')):
            os.remove(old)
        per = 60
        for n in range(0, len(hidden), per):
            part = [(drawn[i], i) for i in hidden[n:n + per] if i in drawn]
            contact_sheet(part, f'Hidden powers {n + 1}-{n + len(part)} of {len(hidden)}', os.path.join(SHEETS, f'icons_hidden_{n // per + 1}.png'))
        flat = []
        for pid, rows in groups:
            for key, _ in rows:
                if key in main_drawn:
                    flat.append((main_drawn[key], key))
        for key, im in main_drawn.items():
            if all(key != n for _, n in flat):
                flat.append((im, key))
        for n in range(0, len(flat), per):
            contact_sheet(flat[n:n + per], f'Main powers and abilities {n + 1}-{n + len(flat[n:n + per])} of {len(flat)}', os.path.join(SHEETS, f'icons_main_{n // per + 1}.png'))
        print('drew', len(drawn), 'hidden and', len(main_drawn), 'main')
    problems, total = check()
    for p in problems:
        print('PROBLEM', p)
    print(f'checked {total} icons, {len(problems)} problems')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
