# Pictures of every weapon's moves from the frame shots of  python tools/run_real_client.py items only=swings  (run-real/screenshots).
# Each frame is your own view above and the two stand-ins (side on, facing you) below.
#   python tools/make_swings_sheet.py [screenshot folder] [folder for one picture per weapon]
import glob
import os
import re
import sys

from PIL import Image, ImageDraw

SHOTS = sys.argv[1] if len(sys.argv) > 1 else 'run-real/screenshots'
EACH = sys.argv[2] if len(sys.argv) > 2 else None
OUT = 'docs/previews/weapon_swings.png'
NAMES = {'golden_kings_long_sword': "Golden King's Long Sword", 'anima_katana': 'Anima Katana', 'dagger_of_darkness': 'Dagger of Darkness', 'flame_hammer': 'Flame Hammer',
         'ice_staff': 'Ice Staff', 'fork': 'Fork', 'knight_killer': 'Knight Killer', 'crimson_edge': 'Crimson Edge', 'gravity_blade': 'Gravity Blade', 'grand_spear': 'Grand Spear',
         'shadow_scythe': 'Shadow Scythe', 'cocytus_hammer': 'Cocytus Hammer'}
TP_CROP = (430, 120, 1170, 820)

frames = {}
for path in glob.glob(os.path.join(SHOTS, 'swing_*_fp.png')):
    m = re.match(r'swing_(.+)_m(\d+)_f(\d+)_fp\.png', os.path.basename(path))
    if m:
        frames.setdefault(m.group(1), {}).setdefault(int(m.group(2)), {})[int(m.group(3))] = path


def cell(fp, width):
    a = Image.open(fp).convert('RGB')
    h1 = int(a.height * width / a.width)
    tp = fp.replace('_fp.png', '_tp.png')
    b = Image.open(tp).convert('RGB').crop(TP_CROP) if os.path.exists(tp) else None
    h2 = int(width * (TP_CROP[3] - TP_CROP[1]) / (TP_CROP[2] - TP_CROP[0])) if b else 0
    out = Image.new('RGB', (width, h1 + h2), (20, 16, 26))
    out.paste(a.resize((width, h1), Image.LANCZOS), (0, 0))
    if b:
        out.paste(b.resize((width, h2), Image.LANCZOS), (0, h1))
    return out


def sheet(rows, width, title_h=18):
    """rows = [(label, [paths])]"""
    cells = [[cell(p, width) for p in paths] for _, paths in rows]
    ch = max(c.height for r in cells for c in r) if cells else 10
    cols = max(len(r) for r in cells) if cells else 1
    im = Image.new('RGB', (cols * (width + 4) + 4, len(rows) * (ch + title_h + 4) + 4), (20, 16, 26))
    d = ImageDraw.Draw(im)
    for i, ((label, _), r) in enumerate(zip(rows, cells)):
        y = 4 + i * (ch + title_h + 4)
        d.text((6, y + 2), label, fill=(240, 190, 220))
        for j, c in enumerate(r):
            im.paste(c, (4 + j * (width + 4), y + title_h))
    return im


if EACH:
    os.makedirs(EACH, exist_ok=True)
    for name, moves in frames.items():
        rows = [(f'{NAMES.get(name, name)}, move {m + 1}', [moves[m][f] for f in sorted(moves[m])]) for m in sorted(moves)]
        sheet(rows, 300).save(os.path.join(EACH, name + '.png'))
rows = []
for name in NAMES:
    for m in sorted(frames.get(name, {})):
        rows.append((f'{NAMES[name]}, move {m + 1}', [frames[name][m][f] for f in sorted(frames[name][m])]))
if rows:
    sheet(rows, 120, 14).save(OUT, optimize=True)
print(len(frames), 'weapons,', len(rows), 'moves')
