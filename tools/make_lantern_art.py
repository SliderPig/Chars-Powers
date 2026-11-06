# Green Lantern's pictures that aren't from my Blockbench models: the suit, Sinestro for now, the bubble tile, and the
# yellow copies of every construct. From the charpowers folder:  python tools/make_lantern_art.py
import glob
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from preview_suit import draw, np, panel, text  # noqa: E402

T = 'src/main/resources/assets/charpowers/textures/entity/'
PREVIEW = 'docs/previews/lantern_suits.png'
GREEN = {'g': (46, 196, 74), 'G': (92, 255, 112), 'd': (24, 96, 44), 'k': (16, 22, 18), 'K': (9, 13, 11), 'w': (226, 255, 232)}
YELLOW = {'g': (255, 214, 40), 'G': (255, 240, 90), 'd': (190, 150, 20), 'k': (24, 18, 26), 'K': (46, 36, 50), 'w': (255, 250, 210),
          'M': (205, 45, 115), 'm': (150, 28, 85), 'b': (18, 12, 18)}


# Paints the skin layout: each part is top, then the four sides as rows of letters (right, front, left, back).
class Skin:
    def __init__(self, colours):
        self.im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.colours = colours

    def face(self, x, y, rows):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch != '.':
                    self.im.putpixel((x + i, y + j), self.colours[ch] + (255,))

    def solid(self, x, y, w, h, ch):
        self.face(x, y, [ch * w] * h)

    def limb(self, x, y, top, front, side, back, bottom='k'):
        # x, y = where its top-left corner is on the skin: 4x12 with its 4-wide top above
        self.solid(x + 4, y, 4, 4, top)
        self.solid(x + 8, y, 4, 4, bottom)
        self.face(x, y + 4, side)
        self.face(x + 4, y + 4, front)
        self.face(x + 8, y + 4, side)
        self.face(x + 12, y + 4, back)


def lantern_suit():
    s = Skin(GREEN)
    # a slim mask, the skin's own face shows through the rest
    s.face(8, 8, ['........', '........', '........', '.dddddd.', 'dwwddwwd', '.d....d.', '........', '........'])
    s.face(0, 8, ['........', '........', '........', '.....ddd', '......dd', '........', '........', '........'])
    s.face(16, 8, ['........', '........', '........', 'ddd.....', 'dd......', '........', '........', '........'])
    s.solid(20, 16, 8, 4, 'k')
    s.solid(28, 16, 8, 4, 'K')
    side = ['dggd', 'kddk', 'kkkk', 'KkkK', 'KkkK', 'KkkK', 'KkkK', 'KkkK', 'KKKK', 'KKKK', 'GGGG', 'kkkk']
    s.face(16, 20, side)
    # the lantern symbol on the chest: a ring with two bars, in the only near-white on the suit
    s.face(20, 20, ['dggggggd', 'kdgGGgdk', 'kgwwwwgk', 'kwgkkgwk', 'kwwwwwwk', 'kwgkkgwk', 'kgwwwwgk', 'kdgGGgdk', 'KkddddkK', 'KKkGGkKK', 'GGGGGGGG', 'kkkggkkk'])
    s.face(28, 20, side)
    s.face(32, 20, ['dggggggd', 'kdggggdk', 'kkdggdkk', 'KkkGGkkK', 'KkkGGkkK', 'KkkGGkkK', 'KkkGGkkK', 'KkkGGkkK', 'KKkGGkKK', 'KKkggkKK', 'GGGGGGGG', 'kkkggkkk'])
    arm = ['ggGg', 'dggd', 'kddk', 'kkkk', 'KkGk', 'KkGk', 'KkGk', 'KkGk', 'GGGG', 'kkkk', 'kkkk', 'dkkd']
    arm_side = ['gggg', 'dggd', 'kddk', 'kkkk', 'kGkk', 'kGkk', 'kGkk', 'kGkk', 'GGGG', 'kkkk', 'kkkk', 'dkkd']
    ring = arm[:10] + ['kwwk', 'dkkd']
    s.limb(40, 16, 'g', ring, arm_side, arm)
    s.limb(32, 48, 'g', arm, arm_side, arm)
    leg_front = ['kkkk', 'kkGk', 'kkGk', 'kkGk', 'kkGk', 'dggd', 'GGGG', 'kkGk', 'kkGk', 'kkGk', 'dddd', 'gggg']
    leg_side = ['KKKK', 'KGkk', 'KGkk', 'KGkk', 'KGkk', 'kddk', 'GGGG', 'KGkk', 'KGkk', 'KGkk', 'dddd', 'gggg']
    leg_back = ['KKKK', 'kkkk', 'kGkk', 'kGkk', 'kGkk', 'kddk', 'GGGG', 'kGkk', 'kGkk', 'kGkk', 'dddd', 'gggg']
    s.limb(0, 16, 'k', leg_front, leg_side, leg_back)
    s.limb(16, 48, 'k', leg_front, leg_side, leg_back)
    # the same again on the skin's second layer (jacket, sleeves, trousers, hat), so nothing on anyone's skin pokes through
    for box, to in (((0, 0, 32, 16), (32, 0)), ((16, 16, 40, 32), (16, 32)), ((40, 16, 56, 32), (40, 32)), ((0, 16, 16, 32), (0, 32)), ((32, 48, 48, 64), (48, 48)), ((16, 48, 32, 64), (0, 48))):
        s.im.paste(s.im.crop(box), to)
    return s.im


# The whole of him: magenta skin, black hair, yellow and black. Only until I draw his real one.
def sinestro():
    s = Skin(YELLOW)
    s.solid(8, 0, 8, 8, 'b')
    s.solid(16, 0, 8, 8, 'M')
    s.face(8, 8, ['bbbbbbbb', 'bbMbbMbb', 'MMMbbMMM', 'MMMMMMMM', 'MgbMMbgM', 'MMMMMMMM', 'MMmmmmMM', 'MMMMMMMM'])
    s.face(0, 8, ['bbbbbbbb', 'bbbbbbbb', 'bbbbMMMM', 'bbbMMMMM', 'bbbMMMMM', 'bbMMMMMM', 'bMMMMMMM', 'MMMMMMMM'])
    s.face(16, 8, ['bbbbbbbb', 'bbbbbbbb', 'MMMMbbbb', 'MMMMMbbb', 'MMMMMbbb', 'MMMMMMbb', 'MMMMMMMb', 'MMMMMMMM'])
    s.face(24, 8, ['bbbbbbbb'] * 7 + ['MMMMMMMM'])
    s.solid(20, 16, 8, 4, 'k')
    s.solid(28, 16, 8, 4, 'k')
    side = ['kkkk'] * 10 + ['gggg', 'kkkk']
    s.face(16, 20, side)
    s.face(20, 20, ['kkkkkkkk', 'kkgggGkk', 'kgGkkgGk', 'kgkkkkgk', 'kgGkkgGk', 'kkgggGkk', 'kkkggkkk', 'kkkggkkk', 'kkkkkkkk', 'kkkkkkkk', 'gggggggg', 'kkkkkkkk'])
    s.face(28, 20, side)
    s.face(32, 20, ['kkkkkkkk'] * 10 + ['gggggggg', 'kkkkkkkk'])
    arm = ['gggg', 'gggg', 'gggg', 'kkkk', 'kkkk', 'kkkk', 'kkkk', 'kkkk', 'dddd', 'gggg', 'gggg', 'gggg']
    ring = arm[:9] + ['gggg', 'gwwg', 'gggg']
    s.limb(40, 16, 'g', ring, arm, arm)
    s.limb(32, 48, 'g', arm, arm, arm)
    leg = ['kkkk', 'kkkk', 'kkkk', 'kkkk', 'kkkk', 'kkkk', 'kkkk', 'kkkk', 'dddd', 'gggg', 'gggg', 'gggg']
    s.limb(0, 16, 'k', leg, leg, leg)
    s.limb(16, 48, 'k', leg, leg, leg)
    return s.im


def bubble():
    im = Image.new('RGBA', (16, 16), (49, 218, 69, 70))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 15, 15), outline=(40, 255, 0, 200))
    d.line((8, 0, 8, 15), fill=(35, 150, 49, 120))
    d.line((0, 8, 15, 8), fill=(35, 150, 49, 120))
    return im


# Green hard light to yellow: red takes the green channel's brightness.
def yellow_copies():
    for path in glob.glob(T + 'construct/*.png'):
        if path.endswith('_yellow.png'):
            continue
        im = Image.open(path).convert('RGBA')
        px = im.load()
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                r, g, b, a = px[x, y]
                px[x, y] = (min(255, g + 20), int(g * 0.84), int(b * 0.35), a)
        im.save(path[:-4] + '_yellow.png', optimize=True)


def skin_faces(sheet):
    def part(lo, size, uv):
        (x0, y0, z0), (w, h, d) = lo, size
        x1, y1, z1 = x0 + w, y0 + h, z0 + d
        u, v = uv
        a = np.array(sheet)

        def cut(cu, cv, cw, ch):
            return a[cv:cv + ch, cu:cu + cw]
        down = (0, -h, 0)
        return [(cut(u + d, v + d, w, h), (x0, y1, z1), (w, 0, 0), down, 0.82, (0, 0, 1)), (cut(u + d + w + d, v + d, w, h), (x1, y1, z0), (-w, 0, 0), down, 0.82, (0, 0, -1)),
                (cut(u, v + d, d, h), (x0, y1, z0), (0, 0, d), down, 0.62, (-1, 0, 0)), (cut(u + d + w, v + d, d, h), (x1, y1, z1), (0, 0, -d), down, 0.62, (1, 0, 0)),
                (cut(u + d, v, w, d), (x0, y1, z0), (w, 0, 0), (0, 0, d), 1.0, (0, 1, 0))]
    return (part((-4, 24, -4), (8, 8, 8), (0, 0)) + part((-4, 12, -2), (8, 12, 4), (16, 16)) + part((-8, 12, -2), (4, 12, 4), (40, 16))
            + part((4, 12, -2), (4, 12, 4), (32, 48)) + part((-4, 0, -2), (4, 12, 4), (0, 16)) + part((0, 0, -2), (4, 12, 4), (16, 48)))


def main():
    suit, him = lantern_suit(), sinestro()
    os.makedirs(T + 'construct', exist_ok=True)
    suit.save(T + 'lantern_suit.png', optimize=True)
    him.save(T + 'sinestro.png', optimize=True)
    bubble().save(T + 'bubble.png', optimize=True)
    yellow_copies()
    # a test picture: the suit over a plain skin, and Sinestro
    plain = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    ImageDraw.Draw(plain).rectangle((0, 0, 63, 63), fill=(214, 170, 130, 255))
    worn = Image.alpha_composite(plain, suit)
    im = Image.new('RGB', (900, 480), (24, 26, 30))
    panel(im, (0, 0, 900, 480), (129, 177, 255), (205, 225, 255), (104, 160, 72))
    for x, sheet, yaw in ((130, worn, -0.5), (360, worn, 3.14159 - 0.5), (590, him, -0.5), (780, him, 3.14159 - 0.5)):
        draw(im, skin_faces(sheet), x, 460, yaw, 0.18, 12)
    text(im, 'Your suit', 90, 14, (30, 40, 60))
    text(im, 'Sinestro', 640, 14, (30, 40, 60))
    im.save(PREVIEW)
    print('lantern art made')


if __name__ == '__main__':
    main()
