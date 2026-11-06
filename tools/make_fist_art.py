# Paints the fist's texture in the same style and colours as the hand (slap.png): green fill, bright outline on every face,
# dark creases for the knuckles, finger joints and palm lines. Run from the charpowers folder:  python tools/make_fist_art.py
# (then python -c "import sys; sys.path.insert(0,'tools'); import make_lantern_art as m; m.yellow_copies()" for the yellow copy)
import json

from PIL import Image

PATH = 'src/main/resources/assets/charpowers/textures/entity/construct/'
FILL, BRIGHT, DARK = (49, 218, 69, 255), (40, 255, 0, 255), (35, 150, 49, 255)


def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'left': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'right': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def rect(im, x, y, w, h, c):
    for i in range(w):
        for j in range(h):
            im.putpixel((x + i, y + j), c)


def outline(im, x, y, w, h):
    rect(im, x, y, w, h, BRIGHT)
    rect(im, x + 1, y + 1, w - 2, h - 2, FILL)


def creases(im, x, y, w, h, rows):
    # short dark dashes across a face, like the finger joints on the hand
    for r in rows:
        yy = y + r
        if yy >= y + h - 1:
            continue
        for i in range(2, w - 2):
            if (i // 2) % 2 == 0:
                im.putpixel((x + i, yy), DARK)


def main():
    geo = json.load(open(PATH.replace('textures/entity/construct/', 'geo/construct/') + 'fist.geo.json'))['minecraft:geometry'][0]
    im = Image.new('RGBA', (128, 128), FILL)
    for bone in geo['bones']:
        for c in bone['cubes']:
            w, h, d = [int(v) for v in c['size']]
            for name, (x, y, fw, fh) in faces(c['uv'][0], c['uv'][1], w, h, d).items():
                outline(im, x, y, fw, fh)
                if name in ('left', 'right', 'back') and fh >= 8:
                    creases(im, x, y, fw, fh, (fh // 3, 2 * fh // 3))  # finger joints
                if name == 'front' and fw >= 10 and fh >= 8:  # the knuckle row across the back of the hand, and the palm lines
                    creases(im, x, y, fw, fh, (3, fh // 2, fh - 4))
                if name == 'top' and fw >= 3 and fh >= 3:
                    rect(im, x + 1, y + 1, 1, fh - 2, DARK)
    im.save(PATH + 'fist.png', optimize=True)
    print('fist painted')


if __name__ == '__main__':
    main()
