# Palladium's own window, tabs and widgets sheets repainted dark plum. The menu itself is drawn in code now (client/power/PowerMenu):
# these only remain for anything of Palladium's that still reaches for the sheets directly.
# Run from the charpowers folder:  python tools/make_power_menu.py
# Writes src/main/resources/assets/charpowers/textures/gui/powers/{window,tabs,widgets,background}.png
import glob
import io
import os
import random
import zipfile

from PIL import Image, ImageDraw

INK = (0x0E, 0x09, 0x12)
PLUM = (0x17, 0x11, 0x1C)
PANEL = (0x24, 0x1A, 0x2C)
BORDER = (0x3A, 0x2B, 0x45)
HOVER = (0x4A, 0x30, 0x5C)
SILVER = (0xC9, 0xBB, 0xD6)
LIGHT = (0xF0, 0xE8, 0xF6)
SOFT = (0xB9, 0xA6, 0xC8)
TEXT = (0xE9, 0xDF, 0xF0)
PINK = (0xFF, 0x88, 0xC8)
OUT = 'src/main/resources/assets/charpowers/textures/gui/powers'


def palladium(name):
    jars = glob.glob(os.path.expanduser('~/.gradle/caches/forge_gradle/deobf_dependencies/local/palladium/*/pa*.jar'))
    jars = [j for j in jars if 'sources' not in j]
    if not jars:
        raise SystemExit('Palladium jar not found in the Gradle cache')
    with zipfile.ZipFile(sorted(jars)[-1]) as z:
        return Image.open(io.BytesIO(z.read('assets/palladium/textures/gui/powers/' + name))).convert('RGBA')


def rect(d, box, fill, edge=None):
    d.rectangle((box[0], box[1], box[2] - 1, box[3] - 1), fill=fill + (255,), outline=(edge or fill) + (255,))


def make_window():
    im = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # the main window: frame around the 234x169 inside at (9, 18). Palladium draws the inside over the middle.
    rect(d, (0, 0, 252, 196), PANEL, INK)
    rect(d, (1, 1, 251, 195), PANEL, SILVER)
    rect(d, (2, 2, 250, 194), PANEL, BORDER)
    rect(d, (8, 17, 244, 188), INK, SILVER)
    im.paste((0, 0, 0, 0), (9, 18, 243, 187))  # see-through: Palladium draws the skills underneath the window
    # the silver plaque the dark "Powers" title sits on
    rect(d, (6, 3, 56, 16), SILVER, INK)
    # the panel Palladium uses for its "buy this ability" box: dark text goes on it, so it stays light
    rect(d, (0, 196, 202, 256), SILVER, INK)
    rect(d, (1, 197, 201, 255), SILVER, LIGHT)
    return im


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def tab_colour(v):
    stops = [(0, INK), (85, BORDER), (139, (0x4A, 0x38, 0x58)), (198, SILVER), (255, LIGHT)]
    for (v0, c0), (v1, c1) in zip(stops, stops[1:]):
        if v <= v1:
            return mix(c0, c1, (v - v0) / (v1 - v0))
    return LIGHT


def make_tabs(src):
    im = src.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = tab_colour((r + g + b) // 3) + (a,)
    return im


# In widgets.png the long rows at the bottom (130-208) are the list tabs' ability rows: normal, locked, hovered.
def make_widgets(src):
    im = src.copy()
    px = im.load()
    rows = [(130, PANEL, BORDER), (156, PLUM, BORDER), (182, HOVER, PINK)]
    for top, fill, edge in rows:
        solid = [(x, y) for y in range(top, top + 26) for x in range(im.width) if px[x, y][3]]
        if not solid:
            continue
        x0, x1 = min(p[0] for p in solid), max(p[0] for p in solid)
        y0, y1 = min(p[1] for p in solid), max(p[1] for p in solid)
        for x, y in solid:
            on_edge = x in (x0, x1) or y in (y0, y1)
            px[x, y] = (edge if on_edge else fill) + (px[x, y][3],)
    return im


def make_background():
    random.seed(7)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), random.choice([(0x2F, 0x26, 0x38), (0x33, 0x29, 0x3D), (0x2B, 0x22, 0x33), (0x37, 0x2D, 0x42)]) + (255,))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    window = make_window()
    tabs = make_tabs(palladium('tabs.png'))
    widgets = make_widgets(palladium('widgets.png'))
    background = make_background()
    window.save(OUT + '/window.png')
    tabs.save(OUT + '/tabs.png')
    widgets.save(OUT + '/widgets.png')
    background.save(OUT + '/background.png')
    print('done')


if __name__ == '__main__':
    main()
