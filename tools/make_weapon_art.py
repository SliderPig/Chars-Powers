# The Player's own weapons: hand-drawn icons, models and names. Drawn big and shrunk to 32x32. From the charpowers folder:  python tools/make_weapon_art.py
import json
import math
import os

from PIL import Image, ImageDraw

A = 'src/main/resources/assets/charpowers/'
BIG = 512
INK = (18, 20, 30, 255)


def crisp(icon):
    """Every pixel solid or empty. In the hand an item is a 3D shape cut out of its icon, so see-through pixels become ghostly fins."""
    r, g, b, a = icon.split()
    a = a.point(lambda v: 255 if v >= 96 else 0)
    solid = Image.merge('RGBA', (r, g, b, a))
    empty = Image.new('RGBA', solid.size, (0, 0, 0, 0))
    empty.paste(solid, mask=a)
    return empty


def blade(d, top, bottom, half, face, edge):
    """A straight blade pointing up, from top to bottom along the middle."""
    cx = BIG // 2
    d.polygon([(cx, top), (cx + half, top + half * 2), (cx + half, bottom), (cx - half, bottom), (cx - half, top + half * 2)], fill=INK)
    i = half - 11
    d.polygon([(cx, top + 16), (cx + i, top + half * 2 + 6), (cx + i, bottom - 10), (cx, bottom - 10)], fill=face)
    d.polygon([(cx, top + 16), (cx - i, top + half * 2 + 6), (cx - i, bottom - 10), (cx, bottom - 10)], fill=edge)


def handle(d, top, length, guard, metal, wrap):
    cx = BIG // 2
    d.rounded_rectangle((cx - 18, top, cx + 18, top + length), 8, fill=INK)
    d.rectangle((cx - 9, top + 8, cx + 9, top + length - 8), fill=wrap)
    for y in range(top + 16, top + length - 12, 22):
        d.line((cx - 9, y, cx + 9, y + 8), fill=INK, width=5)
    d.rounded_rectangle((cx - guard, top - 16, cx + guard, top + 16), 12, fill=INK)
    d.rounded_rectangle((cx - guard + 9, top - 8, cx + guard - 9, top + 8), 6, fill=metal)
    d.ellipse((cx - 26, top + length - 6, cx + 26, top + length + 44), fill=INK)
    d.ellipse((cx - 15, top + length + 5, cx + 15, top + length + 33), fill=metal)


def knight_killer(im, d):
    cx = BIG // 2
    blade(d, 60, 310, 42, (178, 170, 240, 255), (92, 76, 170, 255))
    d.line((cx, 90, cx, 296), fill=(240, 236, 255, 255), width=5)
    handle(d, 320, 110, 66, (150, 110, 255, 255), (50, 40, 86, 255))


def crimson_edge(im, d):
    cx = BIG // 2
    blade(d, 24, 330, 54, (236, 66, 70, 255), (128, 18, 34, 255))
    d.line((cx - 8, 70, cx - 8, 310), fill=(255, 190, 180, 255), width=7)
    handle(d, 340, 104, 92, (214, 170, 80, 255), (88, 18, 26, 255))


def gravity_blade(im, d):
    cx = BIG // 2
    blade(d, 18, 330, 60, (42, 62, 120, 255), (18, 26, 66, 255))
    for y in (100, 170, 240, 300):
        d.arc((cx - 40, y - 22, cx + 40, y + 22), 200, 340, fill=(140, 226, 255, 255), width=8)
    handle(d, 340, 104, 84, (120, 200, 255, 255), (28, 36, 80, 255))


def grand_spear(im, d):
    cx = BIG // 2
    d.rounded_rectangle((cx - 13, 140, cx + 13, 506), 6, fill=INK)
    d.rectangle((cx - 5, 150, cx + 5, 496), fill=(150, 112, 70, 255))
    for y in range(190, 480, 46):
        d.line((cx - 5, y, cx + 5, y + 10), fill=INK, width=5)
    d.polygon([(cx, 4), (cx + 48, 120), (cx + 18, 158), (cx - 18, 158), (cx - 48, 120)], fill=INK)
    d.polygon([(cx, 26), (cx + 32, 118), (cx + 12, 146), (cx, 146)], fill=(226, 255, 236, 255))
    d.polygon([(cx, 26), (cx - 32, 118), (cx - 12, 146), (cx, 146)], fill=(100, 190, 140, 255))
    d.rounded_rectangle((cx - 42, 152, cx + 42, 180), 10, fill=INK)
    d.rounded_rectangle((cx - 32, 158, cx + 32, 174), 5, fill=(214, 180, 90, 255))


def shadow_scythe(im, d):
    cx = BIG // 2
    d.rounded_rectangle((cx - 13, 60, cx + 13, 506), 6, fill=INK)
    d.rectangle((cx - 5, 70, cx + 5, 496), fill=(70, 62, 96, 255))
    d.polygon([(cx + 18, 56), (cx - 90, 28), (cx - 190, 76), (cx - 236, 196), (cx - 170, 118), (cx - 86, 92), (cx + 18, 108)], fill=INK)
    d.polygon([(cx + 4, 68), (cx - 88, 42), (cx - 172, 86), (cx - 206, 160), (cx - 156, 106), (cx - 84, 80), (cx + 4, 94)], fill=(146, 92, 220, 255))
    d.line([(cx - 88, 50), (cx - 168, 90), (cx - 196, 148)], fill=(232, 208, 255, 255), width=7)
    d.ellipse((cx - 24, 40, cx + 24, 88), fill=INK)
    d.ellipse((cx - 13, 51, cx + 13, 77), fill=(200, 140, 255, 255))


def cocytus_hammer(im, d):
    cx = BIG // 2
    d.rounded_rectangle((cx - 15, 130, cx + 15, 506), 6, fill=INK)
    d.rectangle((cx - 6, 140, cx + 6, 496), fill=(112, 150, 176, 255))
    for y in range(200, 480, 50):
        d.line((cx - 6, y, cx + 6, y + 10), fill=INK, width=5)
    d.polygon([(cx - 126, 34), (cx + 126, 34), (cx + 146, 70), (cx + 146, 158), (cx + 126, 190), (cx - 126, 190), (cx - 146, 158), (cx - 146, 70)], fill=INK)
    d.polygon([(cx - 116, 48), (cx + 116, 48), (cx + 130, 76), (cx + 130, 112), (cx - 130, 112), (cx - 130, 76)], fill=(226, 248, 255, 255))
    d.polygon([(cx - 130, 112), (cx + 130, 112), (cx + 130, 150), (cx + 116, 176), (cx - 116, 176), (cx - 130, 150)], fill=(112, 186, 226, 255))
    d.polygon([(cx - 30, 70), (cx + 8, 112), (cx - 12, 150), (cx - 54, 112)], fill=(255, 255, 255, 220))


WEAPONS = {'knight_killer': ('Knight Killer', knight_killer), 'crimson_edge': ('Crimson Edge', crimson_edge), 'gravity_blade': ('Gravity Blade', gravity_blade),
           'grand_spear': ('Grand Spear', grand_spear), 'shadow_scythe': ('Shadow Scythe', shadow_scythe), 'cocytus_hammer': ('Cocytus Hammer', cocytus_hammer)}

# The spear and scythe are long: the same art, only bigger in the hand. These are handheld's own numbers with the size multiplied, and the
# rest of the shape moved so your hand still holds it low on the shaft instead of at its middle. Slots, the ground and frames stay as they were.
LONG = {'grand_spear': 2.2, 'shadow_scythe': 2.0}
HELD = {'thirdperson_righthand': ((0, -90, 55), (0, 4.0, 0.5), 0.85), 'thirdperson_lefthand': ((0, 90, -55), (0, 4.0, 0.5), 0.85),
        'firstperson_righthand': ((0, -90, 25), (1.13, 3.2, 1.13), 0.68), 'firstperson_lefthand': ((0, 90, -25), (1.13, 3.2, 1.13), 0.68)}
HOLD_AT_NOW, HOLD_AT_LONG = -5.0, -2.6  # how far down the picture's diagonal the hand holds it, from the middle: a normal sword, then these


def long_display(size):
    out = {}
    for name, (rotation, at, scale) in HELD.items():
        # the same spot of the picture that was at the hand before has to end up there again, after turning and scaling
        ry, rz = math.radians(-abs(rotation[1])), math.radians(abs(rotation[2]))
        off = scale * (HOLD_AT_NOW - size * HOLD_AT_LONG)
        x, y = off * (math.cos(rz) - math.sin(rz)), off * (math.sin(rz) + math.cos(rz))
        move = (x * math.cos(ry), y, -x * math.sin(ry))
        out[name] = {'rotation': list(rotation), 'translation': [round(a + b, 2) for a, b in zip(at, move)], 'scale': [round(scale * size, 3)] * 3}
    return out


sheet = Image.new('RGB', (3 * 200 + 16, 2 * 200 + 16), (46, 50, 60))
lang_path = A + 'lang/en_us.json'
lang = json.load(open(lang_path, encoding='utf-8'))
for i, (key, (title, draw)) in enumerate(WEAPONS.items()):
    im = Image.new('RGBA', (BIG, BIG), (0, 0, 0, 0))
    draw(im, ImageDraw.Draw(im))
    icon = crisp(im.rotate(-45, resample=Image.BICUBIC, center=(BIG // 2, BIG // 2)).resize((32, 32), Image.LANCZOS))
    icon.save(A + f'textures/item/{key}.png', optimize=True)
    with open(A + f'models/item/{key}.json', 'w', encoding='utf-8', newline='\n') as f:
        model = {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'charpowers:item/{key}'}}
        if key in LONG:
            model['display'] = long_display(LONG[key])
        json.dump(model, f, indent=2)
    lang[f'item.charpowers.{key}'] = title
    big = icon.resize((192, 192), Image.NEAREST)
    sheet.paste(big, (12 + i % 3 * 200, 12 + i // 3 * 200), big)
sheet.save('docs/previews/player_weapons.png')
with open(lang_path, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(lang, f, indent=2, ensure_ascii=False)
    f.write('\n')

# Punchy! (first-person animations) works out a weapon's kind from the game's tool tags. tools/make_punchy_moves.py sets it exactly, these are in case that
# file is ever gone. 1.20.1 has no mace tag, so the hammer goes with the axes, the nearest heavy swing.
TAGS = {'swords': ['knight_killer', 'crimson_edge', 'gravity_blade', 'shadow_scythe', 'golden_kings_long_sword', 'anima_katana', 'dagger_of_darkness', 'fork'], 'spears': ['grand_spear', 'ice_staff'], 'axes': ['cocytus_hammer', 'flame_hammer']}
for tag, keys in TAGS.items():
    path = A.replace('assets/charpowers/', f'data/minecraft/tags/items/{tag}.json')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'replace': False, 'values': [f'charpowers:{key}' for key in keys]}, f, indent=2)
        f.write('\n')
print('weapons made')
