# Draws the Invincible Armour item pictures (16x16) and ability icons (32x32 = the same pictures doubled): plain flat shapes,
# an ink outline, lime and silver. Run from the charpowers folder:  python tools/make_armour_icons.py
# Then run python tools/make_own_powers.py so the power files pick the ability icons up.
import json
import os

from PIL import Image

A = 'src/main/resources/assets/charpowers/'
INK, WHITE, LIGHT, SILVER, MID, STEEL = (22, 26, 30), (246, 250, 252), (222, 228, 233), (188, 196, 203), (146, 155, 163), (84, 92, 100)
LIME, GLOW, DEEP = (150, 255, 30), (214, 255, 150), (80, 190, 16)
C = {'w': WHITE, 'l': LIGHT, 's': SILVER, 'm': MID, 'd': STEEL, 'k': INK, 'g': LIME, 'G': GLOW, 'e': DEEP}


class Pic:
    def __init__(self):
        self.im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    def px(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.im.putpixel((x, y), C[c] + (255,))

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.px(x, y, c)

    def outline(self):
        src = self.im.copy()
        for y in range(16):
            for x in range(16):
                if src.getpixel((x, y))[3]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3]:
                        self.im.putpixel((x, y), INK + (255,))
                        break
        return self.im


def suit(p, ox=0, oy=0, polished=False):
    body, arm, hi = ('l', 's', 'w') if polished else ('s', 'm', 'l')
    p.rect(6 + ox, 1 + oy, 9 + ox, 4 + oy, body)
    p.rect(7 + ox, 1 + oy, 8 + ox, 1 + oy, hi)
    p.rect(6 + ox, 3 + oy, 9 + ox, 3 + oy, 'g')
    p.rect(4 + ox, 5 + oy, 11 + ox, 9 + oy, body)
    p.rect(4 + ox, 5 + oy, 11 + ox, 5 + oy, hi)
    p.rect(7 + ox, 6 + oy, 8 + ox, 7 + oy, 'g')
    p.px(7 + ox, 6 + oy, 'G')
    p.rect(5 + ox, 9 + oy, 10 + ox, 9 + oy, 'g')
    p.rect(2 + ox, 5 + oy, 3 + ox, 10 + oy, arm)
    p.rect(12 + ox, 5 + oy, 13 + ox, 10 + oy, arm)
    p.rect(2 + ox, 4 + oy, 4 + ox, 5 + oy, hi)
    p.rect(11 + ox, 4 + oy, 13 + ox, 5 + oy, hi)
    p.rect(2 + ox, 8 + oy, 3 + ox, 8 + oy, 'g')
    p.rect(12 + ox, 8 + oy, 13 + ox, 8 + oy, 'g')
    p.rect(5 + ox, 10 + oy, 7 + ox, 13 + oy, body)
    p.rect(8 + ox, 10 + oy, 10 + ox, 13 + oy, body)
    p.rect(5 + ox, 11 + oy, 7 + ox, 11 + oy, 'd')
    p.rect(8 + ox, 11 + oy, 10 + ox, 11 + oy, 'd')
    p.rect(4 + ox, 14 + oy, 7 + ox, 14 + oy, arm)
    p.rect(8 + ox, 14 + oy, 11 + ox, 14 + oy, arm)


def head_part(p):
    p.rect(7, 0, 8, 1, 'g')
    p.rect(6, 2, 9, 2, 'l')
    p.rect(5, 3, 10, 3, 'l')
    p.rect(3, 4, 12, 9, 's')
    p.rect(3, 4, 12, 4, 'l')
    p.rect(3, 4, 3, 9, 'l')
    p.rect(12, 5, 12, 9, 'm')
    p.rect(4, 5, 11, 7, 'd')
    p.rect(5, 6, 10, 6, 'g')
    p.px(5, 6, 'G')
    p.rect(3, 10, 12, 11, 'm')
    p.rect(4, 12, 11, 12, 'm')
    p.rect(6, 13, 9, 13, 'd')
    p.rect(7, 10, 8, 12, 'd')


def body_part(p):
    p.rect(1, 3, 5, 6, 'l')
    p.rect(10, 3, 14, 6, 'l')
    p.rect(1, 6, 5, 6, 'm')
    p.rect(10, 6, 14, 6, 'm')
    p.rect(4, 3, 11, 13, 's')
    p.rect(4, 3, 11, 3, 'l')
    p.rect(7, 6, 8, 9, 'g')
    p.rect(6, 7, 9, 8, 'g')
    p.rect(7, 7, 8, 8, 'G')
    p.rect(4, 12, 11, 12, 'g')
    p.rect(4, 13, 11, 13, 'm')


def leg_part(p):
    for x in (3, 9):
        p.rect(x, 2, x + 4, 12, 's')
        p.rect(x, 2, x + 4, 2, 'l')
        p.rect(x, 6, x + 4, 6, 'd')
        p.rect(x + 2, 8, x + 2, 10, 'g')
        p.rect(x - 1, 12, x + 4, 13, 'm')


def boot_part(p):
    for x, flip in ((1, 1), (9, 1)):
        p.rect(x, 3, x + 4, 9, 's')
        p.rect(x, 3, x + 4, 3, 'l')
        p.rect(x, 9, x + 4, 9, 'd')
        p.rect(x, 10, x + 5, 13, 'm')
        p.rect(x + 4, 10, x + 5, 12, 'l')
        p.rect(x, 13, x + 5, 13, 'd')
        p.rect(x + 1, 5, x + 3, 5, 'g')


def chip(p):
    p.rect(1, 5, 2, 5, 's')
    p.rect(1, 8, 2, 8, 's')
    p.rect(1, 11, 2, 11, 's')
    p.rect(13, 5, 14, 5, 's')
    p.rect(13, 8, 14, 8, 's')
    p.rect(13, 11, 14, 11, 's')
    p.rect(3, 2, 12, 13, 'd')
    p.rect(3, 2, 12, 2, 'm')
    p.rect(3, 2, 3, 13, 'm')


def shield_sym(p, ox=0, oy=0):
    p.rect(5 + ox, 4 + oy, 10 + ox, 8 + oy, 'g')
    p.rect(6 + ox, 9 + oy, 9 + ox, 9 + oy, 'g')
    p.rect(7 + ox, 10 + oy, 8 + ox, 10 + oy, 'g')
    p.rect(7 + ox, 5 + oy, 8 + ox, 7 + oy, 'd')
    p.px(5 + ox, 4 + oy, 'G')


def scanner_sym(p):
    p.rect(5, 7, 10, 8, 'g')
    p.rect(6, 6, 9, 6, 'g')
    p.rect(6, 9, 9, 9, 'g')
    p.rect(7, 7, 8, 8, 'k')
    p.px(7, 7, 'G')


def strength_sym(p):
    for y, (x0, x1) in enumerate(((7, 8), (6, 9), (5, 10), (4, 11)), start=3):
        p.rect(x0, y, x1, y, 'g')
    p.rect(6, 7, 9, 12, 'g')
    p.rect(6, 7, 6, 7, 'g')
    p.px(7, 3, 'G')


def speed_sym(p):
    for y, (a, b) in enumerate(((8, 10), (7, 9), (6, 8), (6, 10), (8, 9), (7, 8), (6, 7)), start=4):
        p.rect(a, y, b, y, 'g')
    p.px(8, 4, 'G')


def battery(p):
    p.rect(7, 1, 8, 2, 'l')
    p.rect(5, 3, 10, 14, 'd')
    p.rect(5, 3, 5, 14, 'm')
    p.rect(6, 4, 9, 13, 'k')
    p.rect(6, 11, 9, 13, 'g')
    p.rect(6, 8, 9, 9, 'g')
    p.rect(6, 5, 9, 6, 'G')


def station(p):
    p.rect(2, 12, 13, 14, 'm')
    p.rect(2, 12, 13, 12, 'l')
    p.rect(3, 4, 4, 12, 'l')
    p.rect(11, 4, 12, 12, 'l')
    p.rect(5, 5, 10, 11, 'd')
    p.rect(6, 7, 9, 7, 'g')
    p.rect(6, 9, 9, 9, 'g')
    p.rect(2, 2, 13, 4, 's')
    p.rect(2, 2, 13, 2, 'l')
    p.rect(6, 1, 9, 1, 'g')


def call(p):
    suit(p, 2, 1)
    for y in (4, 7, 10):
        p.rect(0, y, 2, y, 'g')
    p.rect(0, 13, 1, 13, 'G')


def throw(p):
    p.rect(3, 1, 8, 6, 's')
    p.rect(3, 1, 8, 1, 'l')
    for x in (3, 5, 7):
        p.rect(x, 7, x + 1, 7, 's')
    p.rect(4, 7, 4, 7, 'd')
    p.rect(6, 7, 6, 7, 'd')
    p.rect(3, 0, 8, 0, 'g')
    p.rect(10, 9, 14, 13, 'e')
    p.rect(10, 9, 14, 9, 'g')
    p.px(11, 10, 'k')
    p.px(13, 10, 'k')
    p.rect(11, 12, 13, 12, 'k')
    for x, y in ((10, 3), (11, 2), (12, 2), (13, 3), (13, 4), (13, 5)):
        p.px(x, y, 'g')
    p.px(13, 6, 'G')


def crazy(p):
    p.rect(3, 2, 12, 13, 's')
    p.rect(4, 1, 11, 1, 's')
    p.rect(4, 14, 11, 14, 'm')
    p.rect(3, 2, 12, 2, 'l')
    for ex in (4, 9):
        for dx, dy in ((0, 0), (2, 0), (1, 1), (0, 2), (2, 2)):
            p.px(ex + dx, 5 + dy, 'g')
    for i, x in enumerate(range(4, 12)):
        p.px(x, 10 + (i % 2), 'e')


def slim(p):
    suit_slim(p)
    p.rect(13, 1, 13, 5, 'g')
    p.rect(12, 3, 14, 3, 'g')
    p.px(13, 3, 'G')


def suit_slim(p):
    p.rect(7, 1, 8, 3, 'w')
    p.rect(7, 2, 8, 2, 'g')
    p.rect(6, 4, 9, 9, 'l')
    p.rect(7, 5, 8, 6, 'g')
    p.px(7, 5, 'G')
    p.rect(5, 4, 5, 9, 's')
    p.rect(10, 4, 10, 9, 's')
    p.rect(6, 10, 7, 14, 'l')
    p.rect(8, 10, 9, 14, 'l')
    p.rect(6, 14, 7, 14, 'm')
    p.rect(8, 14, 9, 14, 'm')


def make(fn, *, chip_on=False):
    p = Pic()
    if chip_on:
        chip(p)
    fn(p)
    return p.outline()


def save(im, path, scale=1):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    im.save(path, optimize=True)


def main():
    items = {
        'invincible_suit': make(lambda p: suit(p)),
        'suit_head_part': make(head_part),
        'suit_body_part': make(body_part),
        'suit_leg_part': make(leg_part),
        'suit_boot_part': make(boot_part),
        'shield_module': make(shield_sym, chip_on=True),
        'scanner_module': make(scanner_sym, chip_on=True),
        'strength_module': make(strength_sym, chip_on=True),
        'speed_module': make(speed_sym, chip_on=True),
        'charging_station': make(station),
    }
    for name, im in items.items():
        save(im, A + f'textures/item/{name}.png')
    with open(A + 'models/item/charging_station.json', 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'charpowers:item/charging_station'}}, f, indent=2)
        f.write('\n')
    icons = {
        'invincible_armour': make(head_part),
        'call_suit': make(call),
        'throw_mob': make(throw),
        'module_shield': make(shield_sym, chip_on=True),
        'module_scanner': make(scanner_sym, chip_on=True),
        'module_strength': make(strength_sym, chip_on=True),
        'module_speed': make(speed_sym, chip_on=True),
        'going_crazy': make(crazy),
        'slim_suit': make(slim),
    }
    for name, im in icons.items():
        save(im, A + f'textures/icons/{name}.png', 2)
    print('armour icons made')


if __name__ == '__main__':
    main()
