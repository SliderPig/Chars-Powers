# Makes the Hidden Powers ability bar picture: Palladium's own ability_bar.png (every piece stays exactly where
# Palladium looks for it) repainted flat plum + pink, the same colours as the hidden powers menu (3.1).
# Run from the charpowers folder:  python tools/make_ability_bar.py
# Writes src/main/resources/assets/charpowers/textures/gui/ability_bars/hidden_powers.png
# and a big preview docs/previews/hidden_bar_preview.png
import glob
import io
import os
import zipfile

from PIL import Image

PLUM = (0x17, 0x11, 0x1C)
PANEL = (0x24, 0x1A, 0x2C)
BORDER = (0x3A, 0x2B, 0x45)
HIGHLIGHT = (0x2A, 0x1F, 0x33)
SOFT = (0xB9, 0xA6, 0xC8)
TEXT = (0xE9, 0xDF, 0xF0)
PINK = (0xFF, 0x88, 0xC8)
INK = (0x0E, 0x09, 0x12)        # outlines
LOCKED = (0x5A, 0x22, 0x38)     # "can't use it now"


def palladium_bar():
    jars = glob.glob(os.path.expanduser('~/.gradle/caches/forge_gradle/deobf_dependencies/local/palladium/*/palladium-*.jar'))
    jars = [j for j in jars if 'sources' not in j]
    if not jars:
        raise SystemExit('Palladium jar not found in the Gradle cache')
    with zipfile.ZipFile(sorted(jars)[-1]) as z:
        return Image.open(io.BytesIO(z.read('assets/palladium/textures/gui/ability_bar.png'))).convert('RGBA')


def repaint(px):
    r, g, b, a = px
    if a == 0:
        return px
    if (r, g, b) == (123, 144, 238):          # selected slot
        return PINK + (110,)
    if (r, g, b) == (62, 72, 118):
        return PINK + (160,)
    if (r, g, b) in ((193, 102, 102), (106, 43, 43)):   # can't be used right now
        return LOCKED + (a,)
    if r > g + 40 and b > g + 40:             # magenta bits (lock keyhole)
        return PINK + (a,)
    if r > g + 30 and r > b + 30:             # orange/red accents (cooldown line)
        return PINK + (a,)
    lum = (r + g + b) / 3
    if lum < 15:
        c = INK
    elif lum < 40:
        c = PLUM
    elif lum < 55:
        c = PANEL
    elif lum < 130:
        c = BORDER
    elif lum < 160:
        c = HIGHLIGHT
    elif lum < 200:
        c = SOFT
    else:
        c = TEXT
    return c + (a,)


def main():
    src = palladium_bar()
    out = src.copy()
    w, h = src.size
    for y in range(h):
        for x in range(w):
            if 104 <= x < 152 and y < 192:
                continue  # the coloured boxes there are colour choices for energy bars: leave them as they are
            out.putpixel((x, y), repaint(src.getpixel((x, y))))
    # one pink line along the top of the name box, for a bit of sparkle
    for x in range(2, 102):
        if out.getpixel((x, 1))[3] > 0:
            out.putpixel((x, 1), PINK + (255,))
    path = 'src/main/resources/assets/charpowers/textures/gui/ability_bars/hidden_powers.png'
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)
    preview = Image.new('RGBA', (168, 196), (40, 60, 40, 255))
    preview.alpha_composite(out.crop((0, 0, 168, 196)))
    os.makedirs('docs/previews', exist_ok=True)
    preview.resize((168 * 3, 196 * 3), Image.NEAREST).save('docs/previews/hidden_bar_preview.png')
    print('wrote', path)


if __name__ == '__main__':
    main()
