# The symbols the icons are made from. Each one fills the 32x32 design box and can be placed smaller, turned or mirrored:
# flame(a) is a big flame, flame(a, 18, 18, .4) a little one in the bottom right corner.
from .paint import *


# ------------------------------------------------------------------ simple symbols
@glyph
def heart(a, c=RED, gleam=True):
    a.fill(a.E(10, 11.5, 7.5) | a.E(22, 11.5, 7.5) | a.P((3.2, 14), (28.8, 14), (16, 29.5)), c, deep=True)
    if gleam:
        a.flat(a.R(7, 7, 11, 9.5, 1), WHITE[0])


@glyph
def drop(a, c=BLUE):
    a.fill(a.E(16, 20, 9.5) | a.P((16, 1.5), (7, 16.5), (25, 16.5)), c, deep=True)
    a.flat(a.B([(11, 18), (11, 23), (14, 26)], 1.8), c[0])


@glyph
def flame(a, c=ORANGE, core=YELLOW):
    a.fill(a.C((17, 1), (17, 1), (20, 8), (26, 14), (27.5, 22), (22, 29.5), (11, 30), (5, 25), (4.5, 17), (9, 9), (9, 9), (11, 14), (14, 9)), c, deep=True)
    a.fill(a.C((16, 12), (16, 12), (20.5, 18), (22, 23.5), (18, 28.5), (12.5, 28.5), (10, 24), (11, 19), (11, 19), (13.5, 20)), core, shade=False)
    a.flat(a.C((16, 20), (16, 20), (18.5, 24), (16.5, 28), (13.5, 27), (13.5, 24)), WHITE[0])


@glyph
def bolt(a, c=YELLOW):
    a.fill(a.P((19, 1), (6, 18), (14, 18), (10, 31), (26, 12), (17.5, 12), (23, 1)), c)


@glyph
def star(a, c=YELLOW, n=5, inner=.45):
    pts = []
    for i in range(n * 2):
        r = 15 if i % 2 == 0 else 15 * inner
        t = -math.pi / 2 + i * math.pi / n
        pts.append((16 + r * math.cos(t), 16.8 + r * math.sin(t)))
    a.fill(a.P(*pts), c, deep=True)


@glyph
def sparkle(a, c=YELLOW):
    a.fill(a.P((16, 0.5), (19, 13), (31.5, 16), (19, 19), (16, 31.5), (13, 19), (0.5, 16), (13, 13)), c, shade=False)
    a.flat(a.E(16, 16, 3), WHITE[0])


@glyph
def plus(a, c=GREEN):
    a.fill(a.R(11, 2, 21, 30, 1) | a.R(2, 11, 30, 21, 1), c)


@glyph
def minus(a, c=RED):
    a.fill(a.R(2, 11, 30, 21, 1), c)


@glyph
def cross(a, c=RED):
    a.fill(a.L([(6, 6), (26, 26)], 7) | a.L([(26, 6), (6, 26)], 7), c)


@glyph
def check(a, c=GREEN):
    a.fill(a.L([(5, 17), (13, 25), (27, 7)], 7), c)


@glyph
def no_sign(a, c=RED):
    a.fill((a.E(16, 16, 15) & ~a.E(16, 16, 10)) | (a.L([(7, 7), (25, 25)], 5) & a.E(16, 16, 13)), c)


@glyph
def arrow(a, c=GREEN):  # points up; use r= to turn it
    a.fill(a.P((16, 1), (29, 15), (21.5, 15), (21.5, 31), (10.5, 31), (10.5, 15), (3, 15)), c)


@glyph
def chevrons(a, c=CYAN, n=2):  # points up
    for i in range(n):
        y = 5 + i * 10
        a.fill(a.L([(5, y + 9), (16, y), (27, y + 9)], 5.5), c)


@glyph
def uturn(a, c=GREEN):  # starts bottom right, goes up and over, comes down on the left into an arrow head
    a.fill(a.A(17, 15, 9.5, -180, 60, 6), c)
    a.fill(a.P((0.5, 14), (15, 14), (7.7, 26)), c)


@glyph
def spin_arrows(a, c=CYAN):
    a.fill(a.A(16, 16, 11, -160, 10, 5.5), c)
    a.fill(a.P((21, 15), (32, 15), (26.5, 25)), c)
    a.fill(a.A(16, 16, 11, 20, 170, 5.5) & a.R(0, 17, 32, 32) & ~a.E(27, 18, 6), c)
    a.fill(a.P((10.5, 17), (0, 17), (5.2, 7)), c)


@glyph
def swap_arrows(a, c=PURPLE, c2=None):
    c2 = c2 or c
    a.fill(a.R(3, 7, 22, 13) | a.P((20, 2), (30, 10), (20, 18)), c)
    a.fill(a.R(10, 19, 29, 25) | a.P((12, 14), (2, 22), (12, 30)), c2)


@glyph
def waves(a, c=WHITE, n=3):  # sound or signal going out to the right
    for i in range(n):
        a.fill(a.A(4, 16, 8 + i * 8.5, -48, 48, 3.8), c, shade=False)


@glyph
def speed_lines(a, c=WHITE):
    a.flat(a.L([(4, 8), (26, 8)], 3) | a.L([(10, 16), (30, 16)], 3) | a.L([(2, 24), (22, 24)], 3), c)


@glyph
def burst(a, c=YELLOW, core=WHITE, n=8):
    pts = []
    for i in range(n * 2):
        r = 15.5 if i % 2 == 0 else 8
        t = -math.pi / 2 + i * math.pi / n
        pts.append((16 + r * math.cos(t), 16 + r * math.sin(t)))
    a.fill(a.P(*pts), c, shade=False)
    if core:
        a.flat(a.E(16, 16, 5.5), core)


@glyph
def boom(a, c=ORANGE, core=YELLOW):
    pts = []
    for i, r in enumerate((15.5, 9, 14, 8.5, 15.5, 9.5, 13, 8.5, 15, 9, 14, 8.5, 15.5, 9, 13.5, 9.5, 15, 8.5, 14, 9)):
        t = -math.pi / 2 + i * math.tau / 20
        pts.append((16 + r * math.cos(t), 16 + r * math.sin(t)))
    a.fill(a.P(*pts), c, deep=True)
    a.fill(a.C((16, 7), (22, 11), (24, 17), (19, 23), (12, 23), (8, 17), (10, 11)), core, shade=False)
    a.flat(a.E(16, 16, 3.5), WHITE[0])


@glyph
def rings(a, c=CYAN):  # a shockwave along the ground
    a.flat((a.E(16, 22, 15, 8) & ~a.E(16, 22, 12, 5.8)) | (a.E(16, 22, 8, 4) & ~a.E(16, 22, 5.2, 2)), c)


@glyph
def swirl(a, c=PURPLE, w=3.4):
    pts = []
    for i in range(40):
        t = i * 0.42
        r = 1 + i * 0.36
        pts.append((16 + r * math.cos(t), 16 + r * math.sin(t)))
    a.fill(a.L(pts, w), c, shade=False)


@glyph
def target(a, c=RED):
    a.flat((a.E(16, 16, 12) & ~a.E(16, 16, 8.5)) | a.E(16, 16, 3.5) | a.R(14.5, 1, 17.5, 8) | a.R(14.5, 24, 17.5, 31) | a.R(1, 14.5, 8, 17.5) | a.R(24, 14.5, 31, 17.5), c)


@glyph
def qmark(a, c=YELLOW):
    a.fill(a.A(16, 11, 7.5, -190, 60, 6) | a.L([(20, 17), (16, 20), (16, 22)], 6), c)
    a.fill(a.E(16, 28, 3.4), c)


@glyph
def bang(a, c=RED):
    a.fill(a.P((11, 2), (21, 2), (19, 21), (13, 21)), c)
    a.fill(a.E(16, 27, 4), c)


@glyph
def dots(a, c=WHITE, n=3):
    for i in range(n):
        a.fill(a.E(6 + i * 10, 16, 3.6), c)


@glyph
def note(a, c=PURPLE):
    a.fill(a.E(10, 25, 6, 4.8) | a.R(13, 5, 16.5, 25) | a.P((13, 3), (26, 8), (26, 15), (16, 11)), c)


@glyph
def notes(a, c=PINK):
    a.fill(a.E(8, 25, 5, 4) | a.E(23, 22, 5, 4) | a.R(10.5, 6, 13, 25) | a.R(25.5, 3, 28, 22) | a.P((10.5, 5), (28, 2), (28, 9), (13, 12)), c)


@glyph
def letter(a, ch='U', c=WHITE):
    a.fill(font_mask(a, ch, 0, 0, 32), c)


# A small chunky font, for the few icons that want a letter or number. Each 5x7.
_FONT = {
    '0': '01110 11011 11011 11011 11011 11011 01110', '1': '00110 01110 00110 00110 00110 00110 01111',
    '2': '01110 11011 00011 00110 01100 11000 11111', '3': '11110 00011 00011 01110 00011 00011 11110',
    '4': '00110 01110 11010 11111 00010 00010 00010', '5': '11111 11000 11110 00011 00011 11011 01110',
    '6': '01110 11000 11110 11011 11011 11011 01110', '7': '11111 00011 00110 00110 01100 01100 01100',
    '8': '01110 11011 11011 01110 11011 11011 01110', '9': '01110 11011 11011 01111 00011 00011 01110',
    'A': '01110 11011 11011 11111 11011 11011 11011', 'B': '11110 11011 11011 11110 11011 11011 11110',
    'C': '01110 11011 11000 11000 11000 11011 01110', 'D': '11110 11011 11011 11011 11011 11011 11110',
    'E': '11111 11000 11000 11110 11000 11000 11111', 'F': '11111 11000 11000 11110 11000 11000 11000',
    'H': '11011 11011 11011 11111 11011 11011 11011', 'I': '11111 00100 00100 00100 00100 00100 11111',
    'K': '11011 11011 11110 11100 11110 11011 11011', 'L': '11000 11000 11000 11000 11000 11000 11111',
    'M': '10001 11011 11111 10101 10001 10001 10001', 'N': '10011 11011 11111 11111 11011 11001 11001',
    'O': '01110 11011 11011 11011 11011 11011 01110', 'P': '11110 11011 11011 11110 11000 11000 11000',
    'R': '11110 11011 11011 11110 11100 11110 11011', 'S': '01111 11000 11000 01110 00011 00011 11110',
    'T': '11111 00100 00100 00100 00100 00100 00100', 'U': '11011 11011 11011 11011 11011 11011 01110',
    'V': '11011 11011 11011 11011 01110 01110 00100', 'W': '10001 10001 10001 10101 11111 11011 10001',
    'X': '11011 11011 01110 00100 01110 11011 11011', 'Y': '11011 11011 11011 01110 00100 00100 00100',
    'Z': '11111 00011 00110 01100 11000 11000 11111', '?': '01110 11011 00011 00110 00100 00000 00100',
    '!': '00100 01110 01110 00100 00100 00000 00100', 'x': '00000 00000 11011 01110 00100 01110 11011',
}


def font_mask(a, ch, x, y, h):
    """A letter as a shape, h design units tall."""
    u = h / 7
    w = 5 * u
    m = np.zeros((a.size, a.size), bool)
    x += (32 - w) / 2 if h == 32 else 0
    rows = _FONT[ch].split()
    for j, row in enumerate(rows):
        for i, b in enumerate(row):
            if b == '1':
                m |= a.R(x + i * u - .02, y + j * u - .02, x + (i + 1) * u + .02, y + (j + 1) * u + .02)
    return m


@glyph
def word(a, text='NO', h=14, c=WHITE, x0=None, y0=9):
    u = h / 7
    total = sum(6 * u for _ in text) - u
    x = (32 - total) / 2 if x0 is None else x0
    m = np.zeros((a.size, a.size), bool)
    for ch in text:
        if ch != ' ':
            m |= font_mask(a, ch, x, y0, h)
        x += 6 * u
    a.fill(m, c, shade=False)


# ------------------------------------------------------------------ weather, sky, ground
@glyph
def cloud(a, c=WHITE):
    a.fill(a.E(9.5, 19, 7.5, 6.5) | a.E(17, 13, 8.5, 8.5) | a.E(24, 19, 6.5, 6) | a.R(9, 18, 24, 25.5), c, deep=True)


@glyph
def rain_cloud(a, c=STEEL, d=BLUE):
    for x, y in ((9, 25), (16, 26), (23, 25)):
        a.fill(a.L([(x + 1, y), (x, y + 4)], 2.6), d, shade=False)
    a.fill(a.E(9.5, 14, 7.5, 6.5) | a.E(17, 9, 8.5, 7.5) | a.E(24, 14, 6.5, 6) | a.R(9, 13, 24, 20.5), c, deep=True)


@glyph
def storm_cloud(a, c=DARK):
    bolt(a, 11, 13, .58)
    a.fill(a.E(9.5, 13, 7.5, 6.5) | a.E(17, 8, 8.5, 7) | a.E(24, 13, 6.5, 6) | a.R(9, 12, 24, 19.5), c, deep=True)


@glyph
def sun(a, c=YELLOW):
    for i in range(8):
        t = i * math.tau / 8
        a.fill(a.L([(16 + 12 * math.cos(t), 16 + 12 * math.sin(t)), (16 + 14.5 * math.cos(t), 16 + 14.5 * math.sin(t))], 3.4), ORANGE, shade=False)
    a.fill(a.E(16, 16, 9), c, deep=True)


@glyph
def moon(a, c=YELLOW):
    a.fill(a.E(16, 16, 14) & ~a.E(23, 12, 12), c, deep=True)


@glyph
def full_moon(a, c=BONE):
    a.fill(a.E(16, 16, 14), c, deep=True)
    a.flat(a.E(11, 11, 3) | a.E(21, 19, 3.6) | a.E(12, 22, 2), c[2])


@glyph
def rainbow(a):
    for i, c in enumerate(RAINBOW[:5]):
        r = 14 - i * 2.4
        a.flat(a.A(16, 24, r, -180, 0, 2.8), c)


@glyph
def snowflake(a, c=ICE):
    for i in range(3):
        t = i * math.pi / 3
        dx, dy = 14 * math.cos(t), 14 * math.sin(t)
        a.fill(a.L([(16 - dx, 16 - dy), (16 + dx, 16 + dy)], 3), c, shade=False)
    for i in range(6):
        t = i * math.pi / 3
        bx, by = 16 + 9 * math.cos(t), 16 + 9 * math.sin(t)
        for sgn in (-1, 1):
            t2 = t + sgn * 1.0
            a.fill(a.L([(bx, by), (bx + 5 * math.cos(t2), by + 5 * math.sin(t2))], 2.4), c, shade=False)


@glyph
def snowball(a, c=WHITE):
    a.fill(a.E(16, 16, 12), c, deep=True)
    a.flat(a.E(20, 21, 2) | a.E(13, 18, 1.3) | a.E(21, 13, 1.3), ICE[2])


@glyph
def ice_cube(a):
    block(a, top=WHITE, left=ICE, right=CYAN, sticker=False)
    a.flat(a.L([(9, 15), (12, 21)], 1.2) | a.L([(20, 20), (24, 15)], 1.2), WHITE[0])


@glyph
def tornado(a, c=STEEL):
    for i, (w, y) in enumerate(((13.5, 5), (11, 10), (8.5, 15), (6, 20), (3.8, 25))):
        o = (-1) ** i * 1.2
        a.fill(a.R(16 - w + o, y - 2.6, 16 + w + o, y + 2.2, 2.2), c)
    a.fill(a.R(14, 27, 18, 30.5, 1.5), c)


@glyph
def wave(a, c=BLUE, foam=WHITE):
    m = a.C((1, 30), (1, 30), (2, 19), (9, 8), (20, 3), (30, 8), (30, 8), (23, 9), (18, 14), (22, 19), (30, 18), (30, 18), (30.5, 30), (30.5, 30))
    a.fill(m, c, deep=True)
    a.fill(a.B([(10, 9), (20, 4), (29, 7.5)], 2.8) | a.E(28.5, 8.5, 2.2), foam, shade=False)


@glyph
def water(a, c=BLUE):
    a.fill(a.R(1, 20, 31, 31), c)
    pts = [(1 + i * 2.5, 20 + 1.6 * math.sin(i * 1.05)) for i in range(13)]
    a.flat(a.L(pts, 1.8), c[0])


@glyph
def lava_pool(a):
    a.fill(a.R(1, 15, 31, 31, 3), LAVA, deep=True)
    a.flat(a.E(9, 22, 3.5, 2) | a.E(22, 26, 4, 1.8) | a.E(23, 19, 2.4, 1.4), YELLOW[0])


@glyph
def splash(a, c=BLUE):
    a.fill(a.P((3, 30), (5, 20), (2, 11), (10, 18), (12, 6), (16, 17), (21, 5), (22, 18), (30, 11), (27, 21), (29, 30)), c, deep=True)
    for x, y in ((6, 6), (26, 5), (16, 3)):
        a.fill(a.E(x, y, 1.8), c, shade=False)


@glyph
def bubble_ball(a, c=CYAN):
    a.fill(a.E(16, 16, 13) & ~a.E(16, 16, 10.8), c, shade=False)
    a.flat(a.E(16, 16, 11), (c[0][0], c[0][1], c[0][2], 255) if False else c[1])
    a.fill(a.E(16, 16, 13) & ~a.E(16, 16, 11), c[2:] + c[2:] + c[2:], shade=False)
    a.flat(a.B([(9, 14), (11, 9), (15, 7.5)], 2.4), WHITE[0])


@glyph
def bubbles(a, c=CYAN):
    bubble_ball(a, 1, 14, .55, c=c)
    bubble_ball(a, 15, 3, .42, c=c)
    bubble_ball(a, 20, 20, .32, c=c)


@glyph
def rock(a, c=STONE):
    a.fill(a.C((5, 26), (3, 18), (8, 10), (15, 6), (24, 8), (29, 17), (28, 26)), c, deep=True)
    a.flat(a.L([(12, 13), (16, 18)], 1.3) | a.L([(21, 17), (24, 21)], 1.3), c[2])


@glyph
def pebbles(a, c=STONE):
    rock(a, 0, 8, .62, c=c)
    rock(a, 13, 13, .55, c=STONE)
    rock(a, 7, 2, .42, c=IRON)


@glyph
def spikes(a, c=STONE):
    a.fill(a.P((1, 30), (7, 8), (11, 22), (17, 2), (22, 21), (26, 12), (31, 30)), c, deep=True)
    a.flat(a.L([(7, 11), (6, 20)], 1.4) | a.L([(17, 6), (15, 18)], 1.4), c[0])


@glyph
def crack_ground(a, c=DIRT, g=GRASS):
    a.fill(a.R(1, 9, 31, 31, 2), c, deep=True)
    a.fill(a.R(1, 9, 31, 14, 2), g, shade=False)
    a.flat(a.L([(15, 9), (12, 15), (18, 20), (13, 25), (17, 31)], 2.8) | a.L([(18, 20), (26, 23)], 1.6) | a.L([(12, 15), (5, 19)], 1.6), OUT)


@glyph
def crater(a, c=DIRT):
    a.fill(a.E(16, 19, 15.5, 11), c, deep=True)
    a.fill(a.E(16, 18, 10.5, 6.5), SHADOW, shade=False)
    a.flat(a.A(16, 18, 11, 200, 340, 1.8), GRASS[1])


@glyph
def meteor(a, c=STONE):
    a.fill(a.P((28, 3), (15, 8), (7, 14), (18, 24), (24, 15)), ORANGE, shade=False)
    a.fill(a.P((26, 7), (15, 11), (10, 15), (17, 21), (22, 15)), YELLOW, shade=False)
    a.fill(a.C((3, 20), (7, 14), (13, 14), (17, 20), (15, 27), (8, 29)), c, deep=True)
    a.flat(a.E(9, 20, 2) | a.E(12, 25, 1.4), c[2])


@glyph
def planet(a, c=BLUE, land=GREEN):
    m = a.E(16, 16, 14)
    a.fill(m, c, deep=True)
    a.flat(m & (a.E(9, 10, 5, 4) | a.E(22, 21, 6, 4) | a.E(25, 9, 3)), land[1])


@glyph
def black_hole(a, c=VIOLET):
    for r0, c0 in ((0, c), (120, PINK), (240, c)):
        pts = []
        for i in range(20):
            t = math.radians(r0 + i * 13)
            r = 15.5 - i * 0.42
            pts.append((16 + r * math.cos(t), 16 + r * math.sin(t)))
        a.fill(a.L(pts, 2.4), c0, shade=False)
    a.fill(a.E(16, 16, 7.5), OUT, shade=False)
    a.flat(a.E(16, 16, 7.5) & ~a.E(16, 16, 6.3), VIOLET[0])


@glyph
def rift(a, c=VIOLET):
    a.fill(a.P((19, 0), (8, 8), (13, 14), (3, 23), (10, 32), (22, 23), (18, 18), (28, 9)), c, shade=False)
    a.fill(a.P((18, 5), (13, 9.5), (17.5, 14), (9, 23), (11.5, 27), (18, 22), (14.5, 18), (23, 9)), SHADOW, shade=False)
    a.flat(a.R(16, 12, 17, 13) | a.R(13, 21, 14, 22), WHITE[0])


@glyph
def portal(a, c=PURPLE):
    a.fill(a.E(16, 16, 11.5, 15.5), DARK, deep=True)
    a.fill(a.E(16, 16, 8, 12), c, shade=False)
    a.flat(a.A(16, 16, 5, -90, 120, 2) | a.E(16, 16, 1.5), VIOLET[0])


# ------------------------------------------------------------------ plants
@glyph
def leaf(a, c=GREEN):
    a.fill(a.C((5, 28), (5, 28), (3, 15), (11, 6), (28, 3), (28, 3), (27, 18), (18, 27)), c, deep=True)
    a.flat(a.L([(6, 27), (21, 10)], 1.6) | a.L([(13, 20), (13, 12)], 1.2) | a.L([(13, 20), (20, 21)], 1.2), c[2])


@glyph
def clover(a, c=GREEN):
    for x, y in ((9.5, 9), (22.5, 9), (9.5, 20), (22.5, 20)):
        a.fill(a.E(x, y, 7), c)
    a.flat(a.E(16, 14.5, 2.5), c[2])
    a.fill(a.B([(16, 15), (17, 24), (21, 30)], 2.6), c, shade=False)


@glyph
def flower(a, c=RED, mid=YELLOW):
    a.fill(a.L([(16, 16), (16, 31)], 3), GREEN, shade=False)
    a.fill(a.C((20, 25), (27, 22), (27, 22), (25, 28)), GREEN, shade=False)
    for i in range(5):
        t = -math.pi / 2 + i * math.tau / 5
        a.fill(a.E(16 + 7 * math.cos(t), 11 + 7 * math.sin(t), 5), c)
    a.fill(a.E(16, 11, 3.6), mid, shade=False)


@glyph
def rose(a, c=RED):
    a.fill(a.B([(16, 15), (15, 24), (17, 31)], 3), GREEN, shade=False)
    a.fill(a.C((16, 25), (9, 21), (9, 21), (11, 27)), GREEN, shade=False)
    a.fill(a.C((16, 1.5), (24, 4), (27, 11), (22, 18), (10, 18), (5, 11), (8, 4)), c, deep=True)
    a.flat(a.A(16, 9.5, 4, -150, 150, 1.8) | a.A(16, 9.5, 7.5, 40, 200, 1.5), c[2])


@glyph
def tree(a, c=GREEN, w=BROWN):
    a.fill(a.R(13, 18, 19, 31), w)
    a.fill(a.E(16, 12, 13, 11), c, deep=True)
    a.flat(a.E(11, 8, 2.5) | a.E(21, 13, 2.4), c[0])


@glyph
def pine(a, c=FOREST, w=BROWN):
    a.fill(a.R(13, 24, 19, 31), w)
    a.fill(a.P((16, 1), (24, 11), (8, 11)) | a.P((16, 6), (27, 18), (5, 18)) | a.P((16, 12), (29.5, 26), (2.5, 26)), c, deep=True)


@glyph
def sapling(a, c=GREEN):
    a.fill(a.L([(16, 15), (16, 26)], 3), BROWN, shade=False)
    a.fill(a.C((16, 16), (16, 16), (5, 14), (3, 4), (3, 4), (13, 6)), c)
    a.fill(a.C((16, 14), (16, 14), (26, 12), (29, 2), (29, 2), (19, 4)), c)
    a.fill(a.E(16, 28, 11, 3.4), DIRT, shade=False)


@glyph
def cactus(a, c=GREEN):
    a.fill(a.R(11.5, 2, 20.5, 31, 3.5) | a.R(3, 9, 8.5, 20, 2.5) | a.R(3, 16, 13, 21, 1) | a.R(23.5, 6, 29, 17, 2.5) | a.R(19, 13, 29, 18, 1), c, deep=True)
    for x, y in ((14, 6), (18, 12), (14, 18), (18, 25), (5.5, 13), (26, 10)):
        a.dot(x, y, BONE[0])


@glyph
def web(a, c=WHITE):
    for i in range(8):
        t = i * math.tau / 8
        a.flat(a.L([(16, 16), (16 + 15 * math.cos(t), 16 + 15 * math.sin(t))], 1.4), c)
    for r in (5, 10, 14.5):
        pts = []
        for i in range(9):
            t = i * math.tau / 8
            pts.append((16 + r * math.cos(t), 16 + r * math.sin(t)))
        a.flat(a.L(pts, 1.4), c)


@glyph
def grass_blade(a, c=GRASS):
    a.fill(a.C((13, 31), (13, 31), (12, 18), (17, 3), (17, 3), (19, 18), (19, 31), (19, 31)), c)


@glyph
def seeds(a, c=TAN):
    for x, y, r in ((9, 10, -20), (21, 8, 25), (15, 19, 0), (7, 24, 30), (24, 22, -25)):
        a.push(x - 16, y - 16, 1, r=r)
        a.fill(a.C((16, 11), (16, 11), (20, 17), (16, 21), (12, 17)), c)
        a.pop()


@glyph
def vine(a, c=GREEN):
    a.fill(a.B([(3, 29), (9, 19), (6, 11), (15, 7), (22, 14), (29, 4)], 3.4), c, shade=False)
    for x, y, r in ((10, 13, 30), (18, 12, -40), (22, 7, 20)):
        a.fill(a.E(x, y, 2.6, 1.8), FOREST, shade=False)
    a.flat(a.L([(4, 26), (1, 24)], 1.4) | a.L([(9, 18), (12, 20)], 1.4) | a.L([(26, 8), (28, 10)], 1.4), BONE[0])


@glyph
def nettle(a, c=FOREST):
    a.fill(a.P((16, 1), (19, 7), (24, 5), (23, 11), (29, 12), (25, 17), (29, 21), (22, 22), (17, 29), (16, 31), (15, 29), (10, 22), (3, 21), (7, 17), (3, 12), (9, 11), (8, 5), (13, 7)), c, deep=True)
    a.flat(a.L([(16, 6), (16, 28)], 1.5), LIME[1])


@glyph
def mushroom(a, c=RED):
    a.fill(a.R(12, 16, 20, 30, 2.5), BONE, deep=True)
    a.fill(a.C((2, 18), (2, 18), (4, 8), (16, 2), (28, 8), (30, 18), (30, 18), (16, 21)), c, deep=True)
    a.flat(a.E(10, 11, 2.6) | a.E(20, 8, 2.2) | a.E(23, 14, 2), WHITE[0])


@glyph
def berries(a, c=RED):
    for x, y in ((10, 19), (22, 19), (16, 25), (16, 14)):
        a.fill(a.E(x, y, 6), c)
        a.dot(x - 2, y - 2, WHITE[0])
    a.fill(a.C((16, 9), (16, 9), (9, 8), (6, 2), (6, 2), (14, 3)) | a.C((16, 9), (16, 9), (23, 8), (26, 2), (26, 2), (18, 3)), GREEN)


# ------------------------------------------------------------------ food
@glyph
def apple(a, c=RED):
    a.fill(a.E(10.5, 18, 8.5, 11) | a.E(21.5, 18, 8.5, 11), c, deep=True)
    a.flat(a.R(8, 11, 11, 15, 1), WHITE[0])
    a.fill(a.L([(16, 8), (17, 3)], 2.4), BROWN, shade=False)
    a.fill(a.C((18, 6), (18, 6), (24, 1), (29, 2), (29, 2), (26, 7)), GREEN)


@glyph
def bread(a, c=TAN):
    a.fill(a.C((3, 17), (4, 9), (10, 7), (16, 8), (22, 7), (28, 9), (29, 17), (27, 26), (5, 26)), c, deep=True)
    a.flat(a.L([(9, 11), (11, 19)], 1.8) | a.L([(15, 11), (17, 19)], 1.8) | a.L([(21, 11), (23, 19)], 1.8), c[2])


@glyph
def cookie(a, c=TAN, bite=True):
    m = a.E(16, 16, 13.5)
    if bite:
        m &= ~(a.E(29, 7, 5.5) | a.E(25, 4, 4))
    a.fill(m, c, deep=True)
    for x, y in ((10, 11), (17, 8), (21, 16), (12, 20), (18, 24), (8, 17)):
        a.fill(a.R(x - 1.5, y - 1.5, x + 1.5, y + 1.5, .5), BROWN, shade=False)


@glyph
def fortune(a):
    a.fill(a.C((2, 22), (6, 9), (16, 4), (26, 9), (30, 22), (30, 22), (21, 21), (16, 14), (11, 21), (2, 22)), GOLD, deep=True)
    a.fill(a.R(13, 17, 30, 25), WHITE)
    a.flat(a.L([(17, 21), (26, 21)], 1.2), RED)


@glyph
def drumstick(a, c=BROWN):
    a.fill(a.L([(19, 21), (27, 29)], 4), BONE)
    a.fill(a.E(26, 26, 2.8) | a.E(29, 29, 2.8), BONE)
    a.fill(a.C((3, 12), (8, 3), (18, 3), (25, 10), (22, 20), (14, 25), (5, 20)), c, deep=True)
    a.flat(a.B([(8, 9), (13, 6.5)], 2), c[0])


@glyph
def raw_meat(a):
    a.fill(a.C((3, 15), (7, 6), (17, 4), (27, 9), (29, 18), (22, 27), (10, 26)), ROSE, deep=True)
    a.flat(a.B([(7, 13), (13, 9), (22, 11), (25, 18), (20, 23)], 2.2), PINK[0])
    a.fill(a.E(14, 17, 3.4), WHITE, shade=False)


@glyph
def potato(a, c=TAN):
    a.fill(a.C((4, 15), (9, 6), (20, 4), (28, 11), (28, 21), (19, 28), (8, 25)), c, deep=True)
    for x, y in ((11, 12), (19, 11), (21, 19), (12, 20)):
        a.flat(a.E(x, y, 1.3), c[2])


@glyph
def egg(a, c=BONE):
    a.fill(a.C((16, 2), (23, 9), (26, 19), (21, 29), (11, 29), (6, 19), (9, 9)), c, deep=True)
    a.flat(a.E(11, 13, 1.8, 3), WHITE[0])


@glyph
def fried_egg(a):
    a.fill(a.C((4, 12), (11, 4), (21, 5), (29, 11), (28, 22), (19, 28), (9, 25), (3, 19)), WHITE, deep=True)
    a.fill(a.E(16, 15, 6.5), ORANGE, deep=True)
    a.dot(13, 12, WHITE[0])
    a.dot(14, 12, WHITE[0])


@glyph
def soup(a, c=ORANGE):
    a.fill(a.C((2, 15), (2, 15), (5, 25), (11, 30), (21, 30), (27, 25), (30, 15), (30, 15)), BROWN, deep=True)
    a.fill(a.E(16, 15, 14, 4), c, shade=False)
    a.fill(a.E(11, 15, 2) | a.E(20, 14, 2), GREEN, shade=False)
    a.fill(a.B([(12, 9), (10, 6), (12, 3), (11, 1)], 2) | a.B([(20, 9), (18, 6), (20, 3), (19, 1)], 2), WHITE, shade=False)


@glyph
def bucket(a, c=IRON, milk=True):
    a.fill(a.A(16, 10, 10, -180, 0, 2), STEEL, shade=False)
    a.fill(a.P((5, 10), (27, 10), (24, 30), (8, 30)), c, deep=True)
    if milk:
        a.fill(a.E(16, 11, 11, 3.5), WHITE, shade=False)


@glyph
def bone(a, c=BONE):
    a.fill(a.L([(9, 23), (23, 9)], 6.5), c, deep=True)
    a.fill(a.E(5.5, 21.5, 4.6) | a.E(10.5, 26.5, 4.6) | a.E(21.5, 5.5, 4.6) | a.E(26.5, 10.5, 4.6), c, deep=True)


@glyph
def ribs(a, c=BONE):
    a.fill(a.R(14, 2, 18, 30, 1), c)
    for y in (6, 12, 18, 24):
        a.fill(a.B([(16, y), (24, y + 1), (28, y + 5)], 3) | a.B([(16, y), (8, y + 1), (4, y + 5)], 3), c, shade=False)


# ------------------------------------------------------------------ animals
def _eye(a, x, y, c=OUT):
    a.flat(a.R(x - 1, y - 1.5, x + 1, y + 1.5), c)


@glyph
def cat(a, c=CAT, mood='plain', eye='#b8f06a'):
    a.fill(a.P((4, 17), (4.5, 3), (15, 10)) | a.P((28, 17), (27.5, 3), (17, 10)) | a.E(16, 19, 13, 11), c, deep=True)
    a.flat(a.P((6.5, 11), (7, 6.5), (11, 9.5)) | a.P((25.5, 11), (25, 6.5), (21, 9.5)), PINK)
    e = rgb(eye)
    if mood == 'happy':
        a.flat(a.A(10.5, 19.5, 3, 200, 340, 1.6) | a.A(21.5, 19.5, 3, 200, 340, 1.6), e)
    elif mood == 'dead':
        a.flat(a.L([(8, 15), (12.5, 20)], 1.3) | a.L([(12.5, 15), (8, 20)], 1.3) | a.L([(19.5, 15), (24, 20)], 1.3) | a.L([(24, 15), (19.5, 20)], 1.3), e)
    else:
        a.flat(a.E(10.5, 18, 3, 3.2) | a.E(21.5, 18, 3, 3.2), e)
        a.flat(a.R(10, 15.5, 11, 20.5) | a.R(21, 15.5, 22, 20.5), OUT)
    if mood == 'angry':
        a.flat(a.L([(6, 12.5), (14, 16)], 2.2) | a.L([(26, 12.5), (18, 16)], 2.2), RED)
    a.flat(a.P((14.5, 22), (17.5, 22), (16, 24)), PINK)
    if mood == 'open':
        a.flat(a.E(16, 26.5, 4, 2.8), OUT)
        a.flat(a.E(16, 27.2, 2.4, 1.5), PINK)
    else:
        a.flat(a.L([(13, 26), (16, 24.5), (19, 26)], 1.1), WHITE[2])


@glyph
def dog(a, c=TAN, wet=False):
    a.fill(a.E(16, 15, 12, 12), c, deep=True)
    a.fill(a.C((6, 5), (1, 8), (1.5, 18), (6, 19)) | a.C((26, 5), (31, 8), (30.5, 18), (26, 19)), BROWN, deep=True)
    a.fill(a.E(16, 22, 7, 6), BONE, shade=False)
    a.flat(a.E(16, 18.5, 3, 2.2), OUT)
    a.flat(a.L([(16, 20), (16, 23)], 1.2), OUT)
    _eye(a, 10.5, 12)
    _eye(a, 21.5, 12)
    a.fill(a.R(14, 24, 18, 30, 2), PINK, shade=False)


@glyph
def wolf(a, c=WHITE, eye=OUT):
    a.fill(a.P((5, 14), (5, 2), (13, 8)) | a.P((27, 14), (27, 2), (19, 8)) | a.C((4, 12), (10, 6), (22, 6), (28, 12), (26, 21), (20, 30), (12, 30), (6, 21)), c, deep=True)
    a.fill(a.C((10, 19), (22, 19), (20, 29), (12, 29)), c[2:] + c[2:] + c[2:] if False else WHITE, shade=False)
    a.flat(a.P((13.5, 21), (18.5, 21), (16, 24)), OUT)
    a.flat(a.P((7, 12.5), (13, 15), (11, 17)) | a.P((25, 12.5), (19, 15), (21, 17)), eye)


@glyph
def cow(a):
    a.fill(a.P((5, 9), (1, 3), (8, 5)) | a.P((27, 9), (31, 3), (24, 5)), BONE)
    a.fill(a.R(5, 5, 27, 30, 5), WHITE, deep=True)
    a.flat(a.E(10, 10, 5.5, 5) | a.E(25, 14, 3.5, 5), CAT[1])
    a.fill(a.R(7, 20, 25, 30, 4), PINK, deep=True)
    a.flat(a.E(11.5, 25, 1.6) | a.E(20.5, 25, 1.6), PINK[2])
    a.flat(a.R(10.5, 14, 13, 17) | a.R(19, 14, 21.5, 17), OUT)


@glyph
def pig(a):
    a.fill(a.P((6, 11), (4, 3), (12, 7)) | a.P((26, 11), (28, 3), (20, 7)), PINK)
    a.fill(a.R(4, 6, 28, 29, 6), PINK, deep=True)
    a.fill(a.R(10, 15, 22, 24, 3), ROSE[0:1] + ROSE[0:1] + (ROSE[1],), deep=False)
    a.flat(a.R(12.5, 18, 14.5, 21.5) | a.R(17.5, 18, 19.5, 21.5), ROSE[2])
    a.flat(a.R(8, 10, 10.5, 13) | a.R(21.5, 10, 24, 13), OUT)


@glyph
def sheep(a):
    m = None
    for x, y, r in ((7, 9, 6), (16, 6, 6.5), (25, 9, 6), (5, 18, 5.5), (27, 18, 5.5), (9, 25, 5.5), (23, 25, 5.5), (16, 27, 4.5), (16, 15, 9)):
        e = a.E(x, y, r)
        m = e if m is None else (m | e)
    a.fill(m, WHITE, deep=True)
    a.fill(a.R(10, 11, 22, 26, 4), TAN, deep=True)
    a.flat(a.R(12, 15, 14.5, 18) | a.R(17.5, 15, 20, 18), OUT)
    a.flat(a.P((14, 21), (18, 21), (16, 23)), ROSE)


@glyph
def chicken(a, c=WHITE):
    a.fill(a.E(11, 4, 3) | a.E(16, 3, 3.2) | a.E(21, 4, 3) | a.R(10, 3, 22, 8), RED)
    a.fill(a.R(7, 6, 25, 30, 7), c, deep=True)
    a.fill(a.P((2, 15), (12, 12), (12, 19)), ORANGE)
    a.fill(a.E(10, 22, 2.6, 4), RED, shade=False)
    a.flat(a.R(10, 10, 13, 13) | a.R(19, 10, 22, 13), OUT)


@glyph
def chick(a):
    a.fill(a.E(16, 20, 12, 10.5), YELLOW, deep=True)
    a.fill(a.E(10, 22, 4, 3), GOLD, shade=False)
    a.fill(a.P((21, 15), (31, 18), (21, 21)), ORANGE)
    a.flat(a.R(16, 12, 19, 15), OUT)
    a.flat(a.L([(14, 10), (12, 5)], 1.6) | a.L([(16, 10), (17, 5)], 1.6), YELLOW)


@glyph
def duck(a, c=YELLOW):
    a.fill(a.E(14, 22, 12.5, 8), c, deep=True)
    a.fill(a.P((1, 19), (4, 13), (8, 19)), c)
    a.fill(a.E(19.5, 10, 7.5), c, deep=True)
    a.fill(a.C((25, 9), (31, 11), (31, 11), (26, 14)), ORANGE)
    a.flat(a.R(20.5, 7.5, 23, 10), OUT)
    a.flat(a.A(13, 21, 6, 20, 160, 1.6), c[2])


@glyph
def parrot(a):
    a.fill(a.C((6, 31), (6, 31), (6, 18), (9, 7), (17, 3), (24, 8), (24, 15), (18, 21), (16, 31), (16, 31)), RED, deep=True)
    a.fill(a.C((22, 8), (29, 9), (30, 16), (26, 18), (26, 14), (22, 14)), GOLD)
    a.fill(a.E(16, 10, 3.6), WHITE, shade=False)
    a.flat(a.R(15.5, 8.5, 18, 11), OUT)
    a.fill(a.C((6, 19), (11, 18), (12, 26), (8, 31), (5, 26)), BLUE)


@glyph
def horse(a, c=BROWN):
    a.fill(a.P((9, 31), (8, 16), (11, 9), (13, 2), (16, 7), (23, 9), (30, 18), (29, 24), (24, 25), (19, 21), (19, 31)), c, deep=True)
    a.fill(a.P((9, 31), (8, 16), (11, 9), (13, 2), (13, 13), (11, 31)), DARK, shade=False)
    a.flat(a.R(19, 13, 21.5, 15.5), OUT)
    a.flat(a.R(26.5, 20, 28, 22), OUT)


@glyph
def rabbit(a, c=WHITE):
    a.fill(a.E(10.5, 9, 3.6, 9) | a.E(21.5, 9, 3.6, 9), c)
    a.flat(a.E(10.5, 9, 1.6, 6) | a.E(21.5, 9, 1.6, 6), PINK)
    a.fill(a.E(16, 21.5, 11, 9.5), c, deep=True)
    a.flat(a.R(9.5, 18, 12, 21) | a.R(20, 18, 22.5, 21), OUT)
    a.flat(a.P((14, 23), (18, 23), (16, 25)), ROSE)


@glyph
def frog(a, c=GREEN):
    a.fill(a.E(9, 9, 5.5) | a.E(23, 9, 5.5) | a.E(16, 19, 14.5, 10.5), c, deep=True)
    a.flat(a.E(9, 9, 3.2) | a.E(23, 9, 3.2), WHITE[0])
    a.flat(a.R(8, 8, 10.5, 11) | a.R(22, 8, 24.5, 11), OUT)
    a.flat(a.B([(6, 20), (16, 25), (26, 20)], 1.5), OUT)


@glyph
def bat(a, c=DARK):
    a.fill(a.P((16, 11), (20, 8), (31, 5), (29, 20), (25, 16), (21, 23), (18, 19), (16, 25), (14, 19), (11, 23), (7, 16), (3, 20), (1, 5), (12, 8)), c, deep=True)
    a.fill(a.P((11.5, 9), (12, 3), (15, 7), (17, 7), (20, 3), (20.5, 9), (20, 17), (12, 17)), c, shade=False)
    a.flat(a.R(13, 9.5, 15, 11.5) | a.R(17, 9.5, 19, 11.5), RED[0])


@glyph
def bee(a):
    a.fill(a.E(11, 8, 6.5, 5) | a.E(21, 7, 6.5, 5), WHITE, shade=False)
    a.fill(a.E(16, 19, 13.5, 9), YELLOW, deep=True)
    a.flat(a.E(16, 19, 13.5, 9) & (a.R(8, 8, 12, 30) | a.R(16, 8, 20, 30)), CAT[1])
    a.flat(a.R(23.5, 15, 26, 18), OUT)


@glyph
def fly(a):
    a.fill(a.E(9, 11, 6.5, 4.5) | a.E(23, 11, 6.5, 4.5), ICE, shade=False)
    a.fill(a.E(16, 19, 7, 9), CAT, deep=True)
    a.flat(a.E(12, 13, 2.4) | a.E(20, 13, 2.4), RED)


@glyph
def squid(a, c=NAVY):
    for x in (6, 11, 16, 21, 26):
        a.fill(a.B([(x, 18), (x + (x - 16) * .15, 24), (x, 31)], 3), c, shade=False)
    a.fill(a.C((16, 1), (25, 7), (27, 19), (5, 19), (7, 7)), c, deep=True)
    a.flat(a.E(11, 13, 3) | a.E(21, 13, 3), WHITE[0])
    a.flat(a.R(11, 12, 13, 15) | a.R(21, 12, 23, 15), OUT)


@glyph
def crab(a):
    for sx in (1, -1):
        for i in range(3):
            a.fill(a.L([(16 + sx * 10, 19 + i * 3), (16 + sx * 15, 24 + i * 3)], 1.8), RED, shade=False)
    a.fill(a.C((4, 2), (1, 6), (2, 11), (6, 13), (7, 8), (9, 7), (7, 3)) | a.C((28, 2), (31, 6), (30, 11), (26, 13), (25, 8), (23, 7), (25, 3)), ORANGE, deep=True)
    a.fill(a.L([(6, 12), (10, 18)], 2.5) | a.L([(26, 12), (22, 18)], 2.5), RED, shade=False)
    a.fill(a.E(16, 20, 11, 8), RED, deep=True)
    a.fill(a.L([(12, 13), (12, 8)], 1.5) | a.L([(20, 13), (20, 8)], 1.5), RED, shade=False)
    a.flat(a.E(12, 7.5, 2.4) | a.E(20, 7.5, 2.4), WHITE[0])
    a.dot(12, 8, OUT)
    a.dot(20, 8, OUT)


@glyph
def snake(a, c=GREEN):
    a.fill(a.B([(4, 28), (14, 29), (24, 26), (23, 20), (9, 18), (8, 12), (16, 10)], 5), c, shade=False)
    a.fill(a.C((13, 7), (21, 4), (27, 8), (25, 13), (15, 13)), c, deep=True)
    a.flat(a.R(20, 7, 22, 9), OUT)
    a.flat(a.L([(27, 10), (30, 11)], 1.2) | a.L([(30, 11), (31, 9)], 1) | a.L([(30, 11), (31, 13)], 1), RED)


@glyph
def fish(a, c=TAN, belly=None):
    a.fill(a.P((23, 16), (31, 6), (30, 16), (31, 26)), c, deep=True)
    a.fill(a.C((1, 16), (9, 8), (19, 8), (26, 16), (19, 24), (9, 24)), c, deep=True)
    a.flat(a.R(7, 12, 10, 15), OUT)
    a.flat(a.A(9, 16, 7, -45, 45, 1.4), c[2])
    if belly:
        a.flat(a.E(12, 21, 6, 2), belly)


@glyph
def dolphin(a, c=BLUE):
    a.fill(a.C((2, 19), (2, 19), (7, 9), (18, 5), (28, 11), (31, 21), (31, 21), (26, 19), (17, 14), (9, 15)), c, deep=True)
    a.fill(a.P((16, 7), (15, 1), (22, 7)), c)
    a.fill(a.P((14, 14), (11, 23), (21, 16)), c)
    a.fill(a.P((31, 21), (23, 28), (31, 30)), c)
    a.flat(a.R(8, 11, 10, 13), OUT)


@glyph
def pufferfish(a):
    for i in range(12):
        t = i * math.tau / 12
        a.fill(a.P((16 + 10 * math.cos(t - .25), 16 + 10 * math.sin(t - .25)), (16 + 15.5 * math.cos(t), 16 + 15.5 * math.sin(t)), (16 + 10 * math.cos(t + .25), 16 + 10 * math.sin(t + .25))), ORANGE, shade=False)
    a.fill(a.E(16, 16, 11.5), YELLOW, deep=True)
    a.flat(a.E(16, 21, 7, 4.5), WHITE[0])
    a.flat(a.E(11, 13, 2.8) | a.E(21, 13, 2.8), OUT)
    a.dot(10, 12, WHITE[0])
    a.dot(20, 12, WHITE[0])
    a.flat(a.E(16, 19, 2), ROSE)


@glyph
def snail(a, c=TAN, shell=ROSE):
    a.fill(a.C((1, 28), (5, 24), (24, 24), (31, 27), (31, 30), (3, 30)), c, shade=False)
    a.fill(a.L([(5, 25), (4, 16)], 2.4) | a.L([(8, 25), (9, 17)], 2.4), c, shade=False)
    a.fill(a.E(19, 15, 11.5), shell, deep=True)
    a.flat(a.A(19, 15, 7, 0, 300, 1.8) | a.A(19, 15, 3, 60, 360, 1.8), shell[2])


@glyph
def worm(a, c=PINK):
    a.fill(a.B([(3, 25), (9, 14), (16, 21), (23, 11), (29, 13)], 6), c, shade=False)
    a.flat(a.L([(7, 16), (11, 20)], 1.1) | a.L([(15, 19), (19, 21)], 1.1) | a.L([(20, 12), (24, 16)], 1.1), c[2])
    a.dot(29, 12, OUT)


@glyph
def mole(a):
    a.fill(a.E(16, 15, 12.5, 12), BROWN, deep=True)
    a.fill(a.E(16, 19, 4.5, 4), PINK)
    a.flat(a.R(8.5, 10, 12, 12) | a.R(20, 10, 23.5, 12), OUT)
    a.flat(a.R(14, 23, 18, 26), WHITE[0])
    a.flat(a.L([(15.9, 23), (15.9, 26)], .6), OUT)
    a.fill(a.E(5, 26, 4, 3.6) | a.E(27, 26, 4, 3.6), PINK)


@glyph
def chipmunk(a):
    a.fill(a.E(8, 7, 3.5) | a.E(24, 7, 3.5), BROWN)
    a.fill(a.E(16, 15, 11.5, 11), TAN, deep=True)
    a.fill(a.E(7, 21, 6.5, 6) | a.E(25, 21, 6.5, 6), BONE, deep=True)
    a.fill(a.R(14, 7, 18, 13), BROWN, shade=False)
    a.flat(a.R(9.5, 12, 12, 15) | a.R(20, 12, 22.5, 15), OUT)
    a.flat(a.P((14.5, 18), (17.5, 18), (16, 20)), ROSE)
    a.fill(a.R(13.5, 22, 18.5, 28), WHITE, shade=False)
    a.flat(a.L([(15.9, 22), (15.9, 28)], .6), OUT)


@glyph
def cricket(a, c=FOREST):
    a.fill(a.L([(14, 17), (5, 8), (1, 18)], 2.2) | a.L([(20, 18), (26, 10), (31, 18)], 2.2), c, shade=False)
    a.fill(a.E(16, 19, 13, 6), c, deep=True)
    a.fill(a.E(25, 16, 5), LIME, deep=True)
    a.flat(a.R(26, 14, 28, 16), OUT)
    a.flat(a.L([(29, 13), (31, 5)], 1) | a.L([(26, 12), (24, 4)], 1), c)
    a.flat(a.L([(9, 24), (7, 29)], 1.5) | a.L([(15, 24), (14, 29)], 1.5) | a.L([(21, 24), (22, 29)], 1.5), c)


@glyph
def gull(a):
    a.fill(a.P((1, 12), (9, 7), (15, 14), (17, 14), (23, 7), (31, 12), (24, 13), (19, 20), (13, 20), (8, 13)), WHITE, deep=True)
    a.fill(a.E(16, 18, 4.5, 7), WHITE, deep=True)
    a.flat(a.P((1, 12), (5, 9.5), (6, 11.5)) | a.P((31, 12), (27, 9.5), (26, 11.5)), DARK)
    a.fill(a.P((14.5, 11), (17.5, 11), (16, 7)), GOLD, shade=False)


@glyph
def monkey(a):
    a.fill(a.E(4.5, 15, 4) | a.E(27.5, 15, 4), TAN)
    a.fill(a.E(16, 15, 12), BROWN, deep=True)
    a.fill(a.E(11.5, 12, 5) | a.E(20.5, 12, 5) | a.E(16, 20, 8, 6.5), TAN, shade=False)
    a.flat(a.R(10.5, 10, 13, 13) | a.R(19, 10, 21.5, 13), OUT)
    a.flat(a.B([(12, 21), (16, 23), (20, 21)], 1.2), OUT)


# ------------------------------------------------------------------ mobs
@glyph
def creeper(a, c=GREEN):
    a.fill(a.R(4, 4, 28, 28), c, deep=True)
    for x, y in ((7, 8), (14, 6), (21, 9), (6, 20), (23, 22), (19, 14)):
        a.flat(a.R(x, y, x + 3, y + 3), c[2] if (x + y) % 2 else c[0])
    a.flat(a.R(8, 10, 14, 16) | a.R(18, 10, 24, 16) | a.R(13, 16, 19, 22) | a.R(10, 19, 13, 26) | a.R(19, 19, 22, 26), OUT)


@glyph
def zombie(a, c=FOREST, flat_top=True):
    a.fill(a.R(4, 4, 28, 28), c, deep=True)
    a.fill(a.R(4, 4, 28, 10), DIRT, shade=False)
    a.flat(a.R(8, 13, 14, 17) | a.R(18, 13, 24, 17), OUT)
    a.flat(a.R(13, 18, 19, 20) | a.R(11, 22, 21, 24), c[2])


@glyph
def skull(a, c=BONE, eye=OUT):
    a.fill(a.E(16, 13, 12.5, 11.5) | a.R(9, 19, 23, 30, 2), c, deep=True)
    a.flat(a.E(10.5, 14, 3.6, 4) | a.E(21.5, 14, 3.6, 4), eye)
    a.flat(a.P((16, 19), (13.8, 23), (18.2, 23)), OUT)
    a.flat(a.R(12.5, 26, 13.8, 30) | a.R(15.4, 26, 16.6, 30) | a.R(18.2, 26, 19.5, 30), c[2])


@glyph
def villager(a):
    a.fill(a.R(6, 3, 26, 27, 2), SKIN, deep=True)
    a.fill(a.R(13, 14, 19, 30, 1.5), SKIN[0:1] + (SKIN[2],) + (SKIN[2],), shade=False)
    a.flat(a.R(13, 14, 19, 16) | a.R(18, 16, 19, 30), SKIN[2])
    a.flat(a.R(7, 11, 25, 13.5), BROWN[2])
    a.flat(a.R(9, 14, 12.5, 16.5) | a.R(19.5, 14, 23, 16.5), EMERALD)
    a.flat(a.R(9, 15, 11, 16.5) | a.R(21, 15, 23, 16.5), OUT)


@glyph
def phantom(a, c=NAVY):
    a.fill(a.P((16, 8), (24, 11), (31, 8), (27, 20), (21, 18), (18, 28), (16, 24), (14, 28), (11, 18), (5, 20), (1, 8), (8, 11)), c, deep=True)
    a.flat(a.R(11, 11, 14, 14) | a.R(18, 11, 21, 14), LIME[0])


@glyph
def ghost(a, c=WHITE, face=True, mouth=False):
    a.fill(a.E(16, 13, 11.5, 11.5) | a.R(4.5, 13, 27.5, 26) | a.P((4.5, 25), (4.5, 31), (10, 26)) | a.P((10, 25), (16, 31), (22, 25)) | a.P((22, 26), (27.5, 31), (27.5, 25)), c, deep=True)
    if face:
        a.flat(a.E(11.5, 13, 2.4, 3.2) | a.E(20.5, 13, 2.4, 3.2), OUT)
    if mouth:
        a.flat(a.E(16, 20, 3, 3.2), OUT)


@glyph
def soul(a, c=SOUL):
    a.fill(a.C((16, 1), (16, 1), (22, 9), (26, 18), (21, 28), (11, 28), (6, 18), (10, 9)), c, deep=True)
    a.flat(a.E(12, 17, 2, 2.6) | a.E(20, 17, 2, 2.6), OUT)
    a.flat(a.E(16, 22.5, 2, 1.6), OUT)


# ------------------------------------------------------------------ people
def _head(a, x, y, skin, s=4.6, eyes=OUT):
    a.fill(a.R(x - s, y - s, x + s, y + s, 1.6), skin)
    a.flat(a.R(x + 0.5, y - 1, x + 2.2, y + 1) | a.R(x - 3.6, y - 1, x - 1.9, y + 1), eyes)


@glyph
def person(a, pose='stand', c=BLUE, legs=NAVY, skin=SKIN, eyes=OUT):
    head = _head

    def _head_(a, x, y, skin):
        head(a, x, y, skin, eyes=eyes)
    _draw_person(a, pose, c, legs, skin, _head_)


def _draw_person(a, pose, c, legs, skin, _head):
    if pose == 'stand':
        a.fill(a.R(11, 21, 15.5, 31) | a.R(16.5, 21, 21, 31), legs)
        a.fill(a.L([(8.5, 13), (8.5, 22)], 3.4) | a.L([(23.5, 13), (23.5, 22)], 3.4), c)
        a.fill(a.R(11, 11, 21, 22, 1), c)
        _head(a, 16, 6, skin)
    elif pose == 'cheer':
        a.fill(a.R(11, 21, 15.5, 31) | a.R(16.5, 21, 21, 31), legs)
        a.fill(a.L([(10, 13), (4, 4)], 3.4) | a.L([(22, 13), (28, 4)], 3.4), c)
        a.fill(a.R(11, 11, 21, 22, 1), c)
        _head(a, 16, 6, skin)
    elif pose == 'run':
        a.fill(a.L([(14, 21), (7, 24), (3, 30)], 4) | a.L([(16, 21), (24, 24), (24, 31)], 4), legs)
        a.fill(a.L([(13, 13), (6, 16), (5, 21)], 3.4) | a.L([(20, 13), (27, 13), (29, 8)], 3.4), c)
        a.fill(a.P((12, 11), (21, 11), (19, 22), (11, 22)), c)
        _head(a, 18, 6, skin)
    elif pose == 'jump':
        a.fill(a.L([(13, 20), (7, 22), (9, 28)], 4) | a.L([(18, 20), (25, 22), (23, 28)], 4), legs)
        a.fill(a.L([(11, 12), (5, 6)], 3.4) | a.L([(21, 12), (27, 6)], 3.4), c)
        a.fill(a.R(11, 10, 21, 21, 1), c)
        _head(a, 16, 5, skin)
    elif pose == 'crouch':
        a.fill(a.L([(12, 22), (7, 26), (11, 30)], 4) | a.L([(20, 22), (25, 26), (21, 30)], 4), legs)
        a.fill(a.L([(10, 15), (6, 22)], 3.2) | a.L([(22, 15), (26, 22)], 3.2), c)
        a.fill(a.R(11, 13, 21, 24, 2), c)
        _head(a, 16, 8, skin)
    elif pose == 'kneel':
        a.fill(a.L([(13, 21), (8, 28), (3, 29)], 4) | a.L([(19, 21), (23, 21), (24, 30)], 4), legs)
        a.fill(a.L([(10, 12), (9, 20)], 3.4) | a.L([(22, 12), (24, 19)], 3.4), c)
        a.fill(a.R(11, 11, 21, 22, 1), c)
        _head(a, 16, 6, skin)
    elif pose == 'lie':
        a.fill(a.R(19, 19, 31, 23) | a.R(19, 23.5, 31, 27.5), legs)
        a.fill(a.R(8.5, 18.5, 20, 27.5, 1), c)
        a.fill(a.L([(10, 15.5), (19, 15.5)], 3), c)
        a.fill(a.R(1, 18, 9, 27, 1.6), skin)
        a.flat(a.R(3, 19.5, 5, 21.5) | a.R(3, 23.5, 5, 25.5), OUT)
    elif pose == 'fall':
        a.fill(a.L([(14, 12), (10, 4)], 4) | a.L([(18, 12), (23, 5)], 4), legs)
        a.fill(a.L([(11, 19), (4, 16)], 3.2) | a.L([(21, 19), (28, 16)], 3.2), c)
        a.fill(a.R(11, 11, 21, 22, 1), c)
        _head(a, 16, 26, skin)
    elif pose == 'punch':
        a.fill(a.R(9, 21, 13.5, 31) | a.L([(17, 22), (20, 26), (19, 31)], 4), legs)
        a.fill(a.L([(10, 13), (6, 22)], 3.4) | a.L([(19, 14), (30, 13)], 3.6), c)
        a.fill(a.R(9, 11, 19, 22, 1), c)
        a.fill(a.E(29, 13, 3), skin)
        _head(a, 14, 6, skin)


@glyph
def tiny_people(a, c=BLUE):
    person(a, -4, 3, .62, c=c)
    person(a, 14, 3, .62, c=c)


@glyph
def fist(a, c=SKIN, wrist=True):
    if wrist:
        a.fill(a.R(9, 22, 23, 31, 1), c)
        a.flat(a.R(9, 29, 23, 31), c[2])
    a.fill(a.R(4, 5, 28, 25, 5), c, deep=True)
    for x in (10.5, 16, 21.5):
        a.flat(a.L([(x, 6), (x, 13)], 1.2), c[2])
    a.fill(a.R(4, 13, 19, 22, 3.5), c)
    a.ring(a.R(4, 13, 19, 22, 3.5) & ~a.R(0, 23, 32, 32))
    a.flat(a.L([(6, 12.4), (19.6, 12.4)], .7) | a.L([(19.6, 13), (19.6, 22)], .7), c[2])


@glyph
def hand(a, c=SKIN):
    a.fill(a.R(9, 15, 25, 30, 4), c, deep=True)
    for x in (9, 13.2, 17.4, 21.6):
        a.fill(a.R(x, 3 + abs(x - 15) * .4, x + 3.6, 18, 1.7), c)
    a.fill(a.L([(10, 23), (3, 14)], 4), c)
    a.flat(a.L([(12, 22), (21, 22)], 1) | a.L([(13, 26), (19, 26)], 1), c[2])


@glyph
def pointing(a, c=SKIN):  # a finger pointing to the right
    a.fill(a.R(14, 11.5, 31, 17.5, 3), c)
    a.fill(a.R(3, 11, 22, 27, 4.5), c, deep=True)
    a.fill(a.L([(8, 12), (16, 6)], 4), c)
    a.flat(a.L([(9, 17), (20, 17)], 1) | a.L([(9, 22), (20, 22)], 1), c[2])


@glyph
def foot(a, c=SKIN):
    a.fill(a.C((9, 13), (17, 11), (23, 15), (22, 24), (17, 31), (11, 29), (10, 22)), c, deep=True)
    for x, y, r in ((7, 8, 3.8), (12.5, 5.5, 2.8), (17, 5, 2.6), (21, 6.5, 2.4), (24.5, 9.5, 2.1)):
        a.fill(a.E(x, y, r), c)


@glyph
def boot(a, c=BROWN, sole=None):
    sole = sole or DARK
    a.fill(a.R(4, 25, 30, 30, 1), sole)
    a.fill(a.C((6, 3), (6, 3), (19, 3), (19, 3), (19.5, 14), (29, 18), (30, 26), (30, 26), (5, 26), (5, 26)), c, deep=True)
    a.flat(a.R(5.5, 7, 19, 9), c[2])
    a.flat(a.L([(20, 15), (17, 19)], 1.2) | a.L([(24, 17), (21, 21)], 1.2), c[0])


@glyph
def shoe(a, c=WHITE, sole=PINK):
    a.fill(a.R(3, 24, 30, 29, 2), sole)
    a.fill(a.C((4, 12), (4, 12), (14, 12), (14, 12), (19, 18), (29, 20), (30, 25), (30, 25), (3, 25), (3, 25)), c, deep=True)
    a.flat(a.L([(14, 15), (11, 19)], 1.2) | a.L([(18, 18), (15, 22)], 1.2), c[2])


@glyph
def footprints(a, c=SKIN):
    foot(a, -1, 11, .62, c=c, sticker=False)
    foot(a, 13, 0, .62, c=c, sticker=False)


@glyph
def leg_kick(a, c=NAVY):  # a leg kicking out to the right
    a.fill(a.L([(4, 8), (13, 16), (24, 16)], 7), c)
    boot(a, 12, 2, .72, c=BROWN)


@glyph
def arm(a, c=SKIN, sleeve=BLUE):  # a bent arm with a big elbow to the left
    a.fill(a.L([(26, 7), (9, 13), (22, 25)], 7), c)
    a.fill(a.R(21, 3, 31, 12, 1), sleeve)


@glyph
def long_arm(a, c=SKIN):
    a.fill(a.B([(3, 29), (10, 24), (17, 14), (24, 8)], 5), c, shade=False)
    a.flat(a.L([(5, 27), (9, 24)], 1), c[2])
    a.fill(a.E(25, 7, 5.5), c)
    a.fill(a.L([(22, 3), (20, 1)], 2.2) | a.L([(26, 2), (26, 0)], 2.2) | a.L([(30, 6), (32, 5)], 2.2), c, shade=False)


@glyph
def mouth(a, kind='open', lip=ROSE):
    if kind == 'open':
        a.fill(a.E(16, 16, 14.5, 12), lip, deep=True)
        a.fill(a.E(16, 16, 11, 8.5), DARK, shade=False)
        a.flat(a.R(8, 8, 24, 11), WHITE[0])
        a.flat(a.E(16, 22, 6, 3.4), PINK)
    elif kind == 'shout':
        a.fill(a.E(16, 16, 12.5, 14.5), lip, deep=True)
        a.fill(a.E(16, 16, 9, 11), DARK, shade=False)
        a.flat(a.R(9, 6, 23, 9) | a.R(10, 24, 22, 26.5), WHITE[0])
        a.flat(a.E(16, 22, 5, 2.4), PINK)
    elif kind == 'smile':
        a.fill(a.C((2, 10), (2, 10), (16, 13), (30, 10), (30, 10), (27, 22), (16, 28), (5, 22)), lip, deep=True)
        a.fill(a.C((6, 12.5), (6, 12.5), (16, 15.5), (26, 12.5), (26, 12.5), (24, 20), (16, 24), (8, 20)), WHITE, shade=False)
        a.flat(a.L([(10.5, 15), (11, 22)], .8) | a.L([(16, 16), (16, 24)], .8) | a.L([(21.5, 15), (21, 22)], .8), WHITE[2])
    elif kind == 'lips':
        a.fill(a.C((2, 15), (9, 8), (16, 11), (23, 8), (30, 15), (30, 15), (23, 25), (9, 25), (2, 15)), lip, deep=True)
        a.flat(a.B([(4, 15), (16, 18), (28, 15)], 1.4), lip[2])


@glyph
def teeth(a):
    a.fill(a.C((2, 5), (16, 2), (30, 5), (29, 13), (3, 13)) | a.C((3, 19), (29, 19), (30, 27), (16, 30), (2, 27)), ROSE, deep=True)
    a.fill(a.P((5, 8), (9.5, 8), (7.2, 17)) | a.P((11, 8), (15, 8), (13, 15)) | a.P((17, 8), (21, 8), (19, 15)) | a.P((22.5, 8), (27, 8), (24.8, 17)), WHITE, shade=False)
    a.fill(a.P((9, 24), (14, 24), (11.5, 16)) | a.P((18, 24), (23, 24), (20.5, 16)), WHITE, shade=False)


@glyph
def nose(a, c=SKIN):
    a.fill(a.C((13, 2), (19, 4), (22, 15), (28, 21), (25, 28), (16, 29), (9, 28), (8, 22), (13, 17)), c, deep=True)
    a.flat(a.E(13, 25.5, 2, 1.6), c[2])
    a.flat(a.E(21, 25.5, 2, 1.6), c[2])


@glyph
def tummy(a, c=PINK):
    a.fill(a.C((11, 2), (17, 3), (17, 9), (26, 9), (29, 18), (22, 28), (11, 26), (15, 20), (10, 13)), c, deep=True)
    a.flat(a.B([(19, 12), (23, 17), (19, 23)], 1.5), c[2])


@glyph
def eye(a, c=PURPLE, lid=WHITE):
    a.fill(a.C((1, 16), (1, 16), (9, 7), (16, 5.5), (23, 7), (31, 16), (31, 16), (23, 25), (16, 26.5), (9, 25)), lid, deep=True)
    a.fill(a.E(16, 16, 7.5), c, deep=False)
    a.flat(a.E(16, 16, 3.6), OUT)
    a.flat(a.R(12.5, 11.5, 15, 14), WHITE[0])


@glyph
def closed_eye(a, c=SKIN):
    a.fill(a.C((1, 14), (1, 14), (16, 5), (31, 14), (31, 14), (16, 21)), c, deep=True)
    a.flat(a.B([(2, 15), (16, 21), (30, 15)], 2), OUT)
    for x, y, dx in ((6, 19, -2), (12, 22, -1), (20, 22, 1), (26, 19, 2)):
        a.flat(a.L([(x, y), (x + dx, y + 4)], 1.6), OUT)


@glyph
def glasses(a, c=DARK, stache=True):
    a.fill(a.E(9.5, 11, 7.5) | a.E(22.5, 11, 7.5) | a.R(14, 9, 18, 11.5), c)
    a.fill(a.E(9.5, 11, 5) | a.E(22.5, 11, 5), CYAN, shade=False)
    a.flat(a.R(6.5, 8, 9, 10) | a.R(19.5, 8, 22, 10), WHITE[0])
    if stache:
        a.fill(a.C((16, 19), (21, 18), (29, 22), (29, 22), (21, 26), (16, 23), (11, 26), (3, 22), (3, 22), (11, 18)), BROWN, deep=True)


@glyph
def moustache(a, c=WHITE):
    a.fill(a.C((16, 11), (22, 9), (31, 15), (31, 15), (22, 23), (16, 18), (10, 23), (1, 15), (1, 15), (10, 9)), c, deep=True)


# ------------------------------------------------------------------ gear: weapons and tools
@glyph
def sword(a, c=STEEL, hilt=GOLD):  # tip up: r=45 turns it to the top right
    a.fill(a.P((16, 0.5), (20, 5), (20, 21), (12, 21), (12, 5)), c)
    a.flat(a.P((16, 0.5), (20, 5), (20, 21), (16, 21)), c[2])
    a.flat(a.R(15.4, 3, 16.6, 20), c[0])
    a.fill(a.R(7, 21, 25, 25, 1.5), hilt)
    a.fill(a.R(13.8, 25, 18.2, 30), BROWN)
    a.fill(a.E(16, 30, 2.5), hilt, shade=False)


@glyph
def dagger(a, c=STEEL, hilt=DARK):
    a.fill(a.P((16, 2), (20, 8), (20, 19), (12, 19), (12, 8)), c)
    a.flat(a.P((16, 2), (20, 8), (20, 19), (16, 19)), c[2])
    a.fill(a.R(9, 19, 23, 22.5, 1), hilt)
    a.fill(a.R(14, 22, 18, 29, 1), hilt)


@glyph
def spear(a, c=ICE, shaft=WOOD):  # tip up
    a.fill(a.R(14.6, 10, 17.4, 31.5), shaft, shade=False)
    a.fill(a.P((16, 0.5), (22, 11), (16, 15), (10, 11)), c)


@glyph
def scythe(a, c=STEEL):
    a.fill(a.L([(8, 31), (20, 3)], 3), WOOD, shade=False)
    a.fill(a.C((20, 2), (30, 5), (31, 18), (31, 18), (26, 9), (18, 7)), c)


@glyph
def axe(a, c=STEEL):
    a.fill(a.L([(7, 30), (22, 5)], 3.4), WOOD, shade=False)
    a.fill(a.C((13, 5), (13, 5), (19, 2), (26, 3), (31, 8), (29, 17), (29, 17), (21, 11), (15, 10)), c, deep=True)


@glyph
def pickaxe(a, c=STEEL, w=WOOD):
    a.fill(a.L([(6, 30), (21, 10)], 3.4), w, shade=False)
    a.fill(a.C((6, 7), (6, 7), (16, 2), (25, 4), (30, 13), (30, 25), (30, 25), (25, 13), (19, 8), (12, 6)), c, deep=True)


@glyph
def shovel(a, c=STEEL):
    a.fill(a.L([(7, 29), (21, 11)], 3.2), WOOD, shade=False)
    a.fill(a.C((14, 12), (18, 3), (24, 1), (31, 8), (29, 14), (20, 18)), c, deep=True)


@glyph
def hammer(a, c=IRON):
    a.fill(a.L([(5, 30), (17, 14)], 4), WOOD, shade=False)
    a.fill(a.P((9, 8), (19, 1), (31, 15), (21, 22)), c, deep=True)
    a.flat(a.P((9, 8), (11, 6.5), (23, 20.5), (21, 22)), c[0])


@glyph
def wrench(a, c=STEEL):
    a.fill(a.L([(6, 26), (20, 12)], 5.5), c, deep=True)
    a.fill((a.E(23, 9, 7.5) | a.E(5.5, 26.5, 4)) & ~a.P((22, 8), (29, -1), (34, 5)), c, deep=True)


@glyph
def shears(a, c=STEEL):
    a.fill(a.P((14, 17), (24, 1), (19, 18)) | a.P((18, 17), (8, 1), (13, 18)), c)
    a.fill(a.E(9, 24, 5.5) & ~a.E(9, 24, 2.6) | a.E(23, 24, 5.5) & ~a.E(23, 24, 2.6), RED)
    a.fill(a.L([(12, 20), (15, 16)], 2.4) | a.L([(20, 20), (17, 16)], 2.4), RED, shade=False)


@glyph
def hook(a, c=STEEL):
    a.fill(a.A(14, 20, 8, -20, 200, 3.4) | a.L([(21.5, 17), (21.5, 3)], 3.4), c, shade=False)
    a.fill(a.P((4, 22), (10, 14), (11, 22)), c)
    a.fill(a.E(21.5, 3, 2.4), GOLD, shade=False)


@glyph
def grapple(a, c=STEEL):
    a.fill(a.R(14, 11, 18, 31), BROWN, shade=False)
    a.fill(a.A(9, 9, 7, 0, 150, 3.4) | a.A(23, 9, 7, 30, 180, 3.4) | a.L([(16, 16), (16, 1)], 3.4), c, shade=False)
    a.fill(a.P((1, 11), (4, 6), (6, 11)) | a.P((31, 11), (28, 6), (26, 11)) | a.P((16, 0), (19, 5), (13, 5)), c)


@glyph
def bow(a, c=WOOD):
    a.fill(a.A(5, 16, 15, -82, 82, 3.8), c, shade=False)
    a.flat(a.L([(7, 1.5), (7, 30.5)], 1.2), WHITE[0])


@glyph
def dart(a, c=STEEL, fl=WHITE, shaft=WOOD):  # a bow arrow with its tip up
    a.fill(a.R(14.7, 8, 17.3, 28), shaft, shade=False)
    a.fill(a.P((16, 0.5), (21, 10), (11, 10)), c)
    a.fill(a.P((14.7, 22), (9.5, 31), (14.7, 31)) | a.P((17.3, 22), (22.5, 31), (17.3, 31)), fl, shade=False)


@glyph
def shield(a, c=BLUE, band=None):
    m = a.C((3, 4), (3, 4), (16, 2), (29, 4), (29, 4), (28.5, 17), (16, 31), (16, 31), (3.5, 17))
    a.fill(m, c, deep=True)
    if band:
        a.flat(m & (a.R(13.5, 0, 18.5, 32) | a.R(0, 11, 32, 16)), band)


@glyph
def syringe(a, c=RED):
    a.push(0, 0, 1, r=45)
    a.fill(a.R(11, 9, 21, 24, 1), WHITE)
    a.fill(a.R(12.5, 14, 19.5, 23), c, shade=False)
    a.fill(a.R(8, 22.5, 24, 25, .5), STEEL)
    a.fill(a.R(14.5, 24, 17.5, 30), STEEL)
    a.fill(a.R(11, 29, 21, 31.5), STEEL)
    a.fill(a.R(15.2, 1, 16.8, 9), STEEL, shade=False)
    a.pop()


@glyph
def magnet(a, c=RED):
    a.fill(a.A(16, 13, 10, -180, 0, 8.5) | a.R(1.8, 13, 10.2, 22) | a.R(21.8, 13, 30.2, 22), c, deep=True)
    a.fill(a.R(1.8, 22, 10.2, 30, 1) | a.R(21.8, 22, 30.2, 30, 1), STEEL, deep=True)


@glyph
def magnifier(a, c=CYAN):
    a.fill(a.L([(19, 19), (29, 29)], 6), WOOD)
    a.fill(a.E(13, 13, 12), STEEL, deep=True)
    a.fill(a.E(13, 13, 8.5), c, shade=False)
    a.flat(a.A(13, 13, 5.5, 190, 270, 1.8), WHITE[0])


@glyph
def key(a, c=GOLD):
    a.fill(a.L([(14, 17), (28, 28)], 4), c, shade=False)
    a.fill(a.L([(23, 24), (20, 28)], 3) | a.L([(27, 27), (24, 31)], 3), c, shade=False)
    a.fill(a.E(10, 11, 9) & ~a.E(10, 11, 4), c, deep=True)


@glyph
def lantern(a, c=YELLOW):
    a.fill(a.A(16, 6, 4.5, -180, 0, 2), DARK, shade=False)
    a.fill(a.R(9, 6, 23, 10, 1) | a.R(9, 26, 23, 30, 1), DARK)
    a.fill(a.R(10.5, 9, 21.5, 27), c)
    a.flat(a.R(10.5, 9, 12.5, 27) | a.R(19.5, 9, 21.5, 27), DARK[1])
    a.flat(a.E(16, 19, 3, 4.5), WHITE[0])


@glyph
def torch(a):
    a.fill(a.R(13.5, 13, 18.5, 31), WOOD, shade=False)
    flame(a, 7, 0, .56, sticker=False)


@glyph
def flashlight(a, c=STEEL):
    a.fill(a.P((14, 12), (31, 1), (31, 23)), YELLOW, shade=False)
    a.fill(a.P((14, 12), (31, 7), (31, 17)), WHITE, shade=False)
    a.fill(a.R(1, 8, 12, 16, 1) | a.R(11, 6, 15, 18, 1), c, deep=True)


@glyph
def compass(a, wrong=False):
    a.fill(a.E(16, 16, 14), GOLD, deep=True)
    a.fill(a.E(16, 16, 10.5), WHITE, shade=False)
    n, s = (RED, IRON)
    a.fill(a.P((16, 16), (19.5, 19.5), (24, 24) if False else (16, 27), (12.5, 19.5)) if wrong else a.P((16, 5), (19.5, 16), (12.5, 16)), n, shade=False)
    a.fill(a.P((16, 5), (19.5, 16), (12.5, 16)) if wrong else a.P((16, 27), (19.5, 16), (12.5, 16)), s, shade=False)


@glyph
def clock(a, c=GOLD, h=(16, 8), m=(23, 16)):
    a.fill(a.E(16, 16, 14.5), c, deep=True)
    a.fill(a.E(16, 16, 10.5), WHITE, shade=False)
    a.flat(a.L([(16, 16), h], 2) | a.L([(16, 16), m], 2), OUT)
    a.flat(a.E(16, 16, 1.6), RED)


@glyph
def stopwatch(a, c=STEEL):
    a.fill(a.R(13, 0.5, 19, 5, 1), c)
    a.fill(a.L([(25, 7), (27, 5)], 3), c, shade=False)
    a.fill(a.E(16, 18, 13), c, deep=True)
    a.fill(a.E(16, 18, 9.5), WHITE, shade=False)
    a.flat(a.L([(16, 18), (21, 11)], 2), RED)
    a.flat(a.E(16, 18, 1.6), OUT)


@glyph
def hourglass(a, c=GOLD, sand=SAND):
    a.fill(a.P((7, 4), (25, 4), (25, 7), (18, 16), (25, 25), (25, 28), (7, 28), (7, 25), (14, 16), (7, 7)), ICE, shade=False)
    a.fill(a.P((10, 9), (22, 9), (17, 15), (15, 15)) | a.P((16, 17), (23, 27), (9, 27)), sand, shade=False)
    a.fill(a.R(4, 1, 28, 5, 1) | a.R(4, 27, 28, 31, 1), c)


@glyph
def pause(a, c=CYAN):
    a.fill(a.R(5, 3, 13.5, 29, 1.5) | a.R(18.5, 3, 27, 29, 1.5), c, deep=True)


@glyph
def bell(a, c=GOLD):
    a.fill(a.C((16, 3), (23, 7), (25, 18), (30, 25), (30, 25), (2, 25), (2, 25), (7, 18), (9, 7)), c, deep=True)
    a.fill(a.E(16, 2.5, 2.2), c, shade=False)
    a.fill(a.E(16, 28, 3.6), BROWN, shade=False)
    a.flat(a.B([(11, 9), (10, 15), (9, 19)], 1.8), c[0])


@glyph
def anchor(a, c=IRON):
    a.fill(a.R(14, 6, 18, 28), c, shade=False)
    a.fill(a.E(16, 5, 4.2) & ~a.E(16, 5, 1.6), c, shade=False)
    a.fill(a.R(9, 10.5, 23, 14), c, shade=False)
    a.fill(a.A(16, 16, 12.5, 15, 165, 4), c, shade=False)
    a.fill(a.P((0.5, 21), (8, 18), (6, 26)) | a.P((31.5, 21), (24, 18), (26, 26)), c)


@glyph
def umbrella(a, c=PINK):
    a.fill(a.L([(16, 14), (16, 27)], 2.4) | a.A(12.5, 27, 3.5, 0, 180, 2.4), WOOD, shade=False)
    a.fill(a.C((1, 18), (1, 18), (5, 7), (16, 2), (27, 7), (31, 18), (31, 18), (26, 15), (21, 18), (16, 15), (11, 18), (6, 15)), c, deep=True)


@glyph
def bomb(a, c=DARK):
    a.fill(a.B([(21, 8), (25, 3), (29, 5)], 2.2), WOOD, shade=False)
    a.fill(a.E(14, 18, 12.5), c, deep=True)
    a.fill(a.R(16, 5, 22, 10, 1), STEEL)
    a.flat(a.E(10, 13, 2.5), c[0])
    sparkle(a, 22, -1, .38, c=YELLOW, sticker=False)


@glyph
def tnt(a):
    a.fill(a.R(4, 5, 28, 29, 1), RED, deep=True)
    a.fill(a.R(4, 12, 28, 21), WHITE, shade=False)
    a.fill(a.R(9, 12.5, 11, 20.5) | a.R(21, 12.5, 23, 20.5) | a.R(7.5, 13, 12.5, 15) | a.R(19.5, 13, 24.5, 15), OUT)
    a.fill(a.R(14, 14, 16, 20.5) | a.R(17, 14, 18, 20.5), OUT)
    a.flat(a.L([(16, 5), (17, 2)], 1.6), WHITE[2])


@glyph
def rocket(a, c=RED):
    a.push(0, 0, 1, r=30)
    a.fill(a.P((11, 24), (5, 31), (11, 28)) | a.P((21, 24), (27, 31), (21, 28)), BLUE)
    a.fill(a.R(10, 10, 22, 28, 1), WHITE, deep=True)
    a.fill(a.P((10, 11), (16, 1), (22, 11)), c, deep=True)
    a.flat(a.R(10, 16, 22, 19), c)
    a.fill(a.P((13, 28), (16, 32), (19, 28)), ORANGE, shade=False)
    a.pop()


@glyph
def popper(a):
    a.fill(a.P((3, 30), (9, 13), (20, 24)), PINK, deep=True)
    a.flat(a.L([(6, 25), (11, 26)], 1.5) | a.L([(8, 19), (15, 23)], 1.5), WHITE[0])
    for x, y, c in ((16, 6, YELLOW), (23, 11, CYAN), (27, 4, RED), (28, 19, GREEN), (20, 15, PURPLE), (10, 4, GREEN)):
        a.fill(a.R(x - 2, y - 2, x + 2, y + 2), c, shade=False)


@glyph
def confetti(a):
    for x, y, c, t in ((6, 7, RED, 20), (15, 4, YELLOW, -30), (25, 7, CYAN, 40), (10, 16, GREEN, -15), (21, 16, PURPLE, 25), (6, 25, CYAN, 30), (15, 26, PINK, -30), (26, 25, ORANGE, 15)):
        a.push(x - 16, y - 16, 1, r=t)
        a.fill(a.R(13, 14, 19, 18), c, shade=False)
        a.pop()


@glyph
def mask(a, kind='sad', c=WHITE):
    a.fill(a.C((4, 4), (4, 4), (16, 2), (28, 4), (28, 4), (28, 18), (22, 28), (16, 30), (10, 28), (4, 18)), c, deep=True)
    if kind == 'sad':
        a.flat(a.L([(8, 11), (13, 13)], 2) | a.L([(24, 11), (19, 13)], 2), OUT)
        a.flat(a.E(10.5, 15, 2.6, 2) | a.E(21.5, 15, 2.6, 2), OUT)
        a.flat(a.A(16, 26, 6, -160, -20, 2.4), OUT)
        a.fill(a.E(9, 21, 1.6, 2.6), BLUE, shade=False)
    else:
        a.flat(a.E(10.5, 13, 3.2, 2.4) | a.E(21.5, 13, 3.2, 2.4), OUT)
        a.flat(a.R(14.8, 12, 17.2, 19), c[2])
        a.flat(a.A(16, 19, 5, 30, 150, 1.8), OUT)


@glyph
def crown(a, c=GOLD, gem=RED):
    a.fill(a.P((3, 26), (1.5, 7), (9.5, 14), (16, 3), (22.5, 14), (30.5, 7), (29, 26)), c, deep=True)
    a.fill(a.R(3, 22, 29, 28), c)
    for x, col in ((9, BLUE), (16, gem), (23, BLUE)):
        a.fill(a.E(x, 24.5, 2.1), col, shade=False)
    for x, y in ((1.5, 6), (16, 2), (30.5, 6)):
        a.fill(a.E(x if x != 1.5 else 2.2, y, 2) if False else a.E(min(max(x, 2.6), 29.4), y, 2.1), gem, shade=False)


@glyph
def gem(a, c=DIAMOND):
    a.fill(a.P((9, 4), (23, 4), (30, 12), (16, 30), (2, 12)), c, deep=True)
    a.flat(a.L([(2, 12), (30, 12)], 1.2) | a.L([(9, 4), (11, 12), (16, 28)], 1.2) | a.L([(23, 4), (21, 12), (16, 28)], 1.2), c[2])
    a.flat(a.P((9, 5), (15, 5), (11, 11), (5, 11)), c[0])


@glyph
def coin(a, c=GOLD, mark=True):
    a.fill(a.E(16, 16, 14), c, deep=True)
    a.flat(a.E(16, 16, 10.5) & ~a.E(16, 16, 9.2), c[2])
    if mark:
        a.flat(a.R(14, 9.5, 18, 22.5) | a.R(12, 11, 20, 13.5) | a.R(12, 19, 20, 21.5), c[2])


@glyph
def nugget(a, c=GOLD):
    a.fill(a.C((3, 22), (6, 12), (13, 7), (21, 6), (28, 13), (29, 23), (20, 27), (9, 27)), c, deep=True)
    a.flat(a.E(11, 14, 2.6, 1.8) | a.E(20, 13, 1.6), WHITE[0])
    a.flat(a.E(20, 21, 2.6, 1.6), c[2])


@glyph
def ingot(a, c=COPPER):
    a.fill(a.P((8, 8), (29, 8), (24, 16), (3, 16)), c)
    a.fill(a.P((3, 16), (24, 16), (24, 25), (3, 25)), c, shade=False)
    a.fill(a.P((24, 16), (29, 8), (29, 17), (24, 25)), c[2:] * 3, shade=False)
    a.flat(a.L([(9, 10), (22, 10)], 1.4), WHITE[0])


@glyph
def bag(a, c=WOOD, mark=None):
    a.fill(a.C((10, 11), (22, 11), (29, 19), (28, 28), (22, 31), (10, 31), (4, 28), (3, 19)), c, deep=True)
    a.fill(a.P((10, 2), (22, 2), (18, 10), (14, 10)), c)
    a.fill(a.R(11, 9, 21, 12, 1), RED, shade=False)


@glyph
def gift(a, c=PINK, ribbon=YELLOW):
    a.fill(a.R(4, 13, 28, 30, 1), c, deep=True)
    a.fill(a.R(2, 9, 30, 15, 1), c)
    a.fill(a.R(13.5, 9, 18.5, 30), ribbon, shade=False)
    a.fill(a.C((16, 9), (8, 8), (6, 2), (12, 2)) | a.C((16, 9), (24, 8), (26, 2), (20, 2)), ribbon)


@glyph
def dice(a, c=WHITE):
    a.push(0, 0, 1, r=-12)
    a.fill(a.R(4, 4, 28, 28, 4), c, deep=True)
    for x, y in ((10, 10), (22, 10), (16, 16), (10, 22), (22, 22)):
        a.flat(a.E(x, y, 2.2), RED)
    a.pop()


@glyph
def gauge(a, c=RED, needle=0.85):
    a.fill(a.E(16, 19, 15, 15) & a.R(0, 0, 32, 23), DARK, deep=True)
    a.flat(a.A(16, 19, 11, -180, -130, 3.4), GREEN)
    a.flat(a.A(16, 19, 11, -125, -65, 3.4), YELLOW)
    a.flat(a.A(16, 19, 11, -60, -2, 3.4), c)
    t = math.radians(-180 + 180 * needle)
    a.flat(a.L([(16, 20), (16 + 9 * math.cos(t), 19 + 9 * math.sin(t))], 2), WHITE[0])
    a.flat(a.E(16, 20, 2.4), WHITE[0])


@glyph
def flag(a, c=RED):
    a.fill(a.R(6, 2, 9.5, 31), WOOD, shade=False)
    a.fill(a.C((9, 3), (9, 3), (17, 5), (28, 3), (28, 3), (27, 17), (27, 17), (17, 19), (9, 17), (9, 17)), c, deep=True)


@glyph
def chain(a, c=STEEL):
    a.push(0, 0, 1, r=-45)
    for y in (2, 12, 22):
        a.fill(a.R(10, y - 2, 22, y + 9, 5) & ~a.R(13.5, y + 1, 18.5, y + 6, 2), c)
    a.pop()


@glyph
def cage(a, c=BONE):
    a.fill(a.A(16, 16, 11, -180, 0, 2.8) | a.R(3.5, 16, 6.3, 30) | a.R(25.7, 16, 28.5, 30) | a.R(10.2, 8, 13, 30) | a.R(19, 8, 21.8, 30) | a.R(2, 27, 30, 31, 1) | a.R(14.6, 4, 17.4, 30), c, shade=False)


@glyph
def jar(a, c=SOUL):
    a.fill(a.R(9, 2, 23, 7, 1), WOOD)
    a.fill(a.R(5, 7, 27, 30, 5), ICE, deep=True)
    a.fill(a.R(8, 12, 24, 27, 3), SHADOW, shade=False)


@glyph
def potion(a, c=RED):
    a.fill(a.R(13, 2, 19, 11, 1), ICE)
    a.fill(a.R(12, 1, 20, 4, 1), WOOD, shade=False)
    a.fill(a.E(16, 20, 11.5), ICE, deep=True)
    m = a.E(16, 20, 8.8) & a.R(0, 15, 32, 32)
    a.fill(m, c, shade=False)
    a.flat(a.R(10, 13, 12.5, 16), WHITE[0])


@glyph
def bottle(a, c=GREEN):
    a.fill(a.R(13, 2, 19, 12, 1), c)
    a.fill(a.R(9, 11, 23, 30, 3), c, deep=True)
    a.flat(a.R(11, 14, 13, 24), c[0])


@glyph
def book(a, c=PURPLE, page=BONE):
    a.fill(a.R(5, 3, 28, 30, 2), c, deep=True)
    a.fill(a.R(9, 26, 28, 30), page, shade=False)
    a.fill(a.R(5, 3, 9, 30, 1), c[2:] * 3, shade=False)
    a.flat(a.R(12, 8, 25, 10) | a.R(12, 13, 21, 15), page[1])


@glyph
def open_book(a, c=BROWN, page=BONE, lines=True):
    a.fill(a.P((1, 8), (16, 11), (31, 8), (31, 27), (16, 30), (1, 27)), c, shade=False)
    a.fill(a.P((3, 5), (16, 8), (16, 27), (3, 24)), page, shade=False)
    a.fill(a.P((29, 5), (16, 8), (16, 27), (29, 24)), page[1:] + (page[2],), shade=False)
    a.flat(a.L([(16, 8), (16, 27)], 1.2), page[2])
    if lines:
        a.flat(a.L([(6, 10), (13, 12)], 1.2) | a.L([(6, 15), (13, 17)], 1.2) | a.L([(26, 10), (19, 12)], 1.2) | a.L([(26, 15), (19, 17)], 1.2), page[2])


@glyph
def page(a, c=WHITE, lines=True, fold=True):
    a.fill(a.P((6, 2), (21, 2), (27, 8), (27, 30), (6, 30)), c, deep=True)
    if fold:
        a.fill(a.P((21, 2), (21, 8), (27, 8)), c[2:] * 3, shade=False)
    if lines:
        a.flat(a.R(10, 12, 23, 14) | a.R(10, 17, 23, 19) | a.R(10, 22, 19, 24), STEEL)


@glyph
def quill(a, c=WHITE):
    a.fill(a.C((30, 2), (30, 2), (29, 11), (22, 19), (13, 22), (13, 22), (15, 13), (22, 5)), c, deep=True)
    a.flat(a.L([(27, 5), (10, 25)], 1.4), c[2])
    a.fill(a.L([(14, 21), (5, 30)], 2), OUT, shade=False)


@glyph
def feather(a, c=WHITE):
    a.fill(a.C((28, 3), (28, 3), (26, 12), (19, 22), (10, 24), (10, 24), (9, 15), (16, 6)), c, deep=True)
    a.flat(a.L([(25, 6), (4, 29)], 1.6), c[2])
    a.flat(a.L([(20, 14), (24, 13)], 1) | a.L([(14, 19), (17, 20)], 1) | a.L([(16, 13), (14, 9)], 1), c[2])


@glyph
def wing(a, c=WHITE):
    a.fill(a.C((2, 5), (2, 5), (12, 3), (24, 6), (30, 13), (24, 15), (28, 19), (21, 20), (23, 25), (16, 24), (16, 29), (8, 23), (3, 14)), c, deep=True)
    a.flat(a.B([(7, 9), (17, 10), (25, 14)], 1.3) | a.B([(7, 13), (14, 16), (21, 19)], 1.3), c[2])


@glyph
def envelope(a, c=WHITE):
    a.fill(a.R(2, 7, 30, 26, 1), c, deep=True)
    a.flat(a.L([(3, 8), (16, 18), (29, 8)], 1.6), c[2])
    a.fill(a.E(16, 18, 2.8), RED, shade=False)


@glyph
def sticky(a, c=YELLOW):
    a.fill(a.P((4, 4), (28, 4), (28, 21), (21, 28), (4, 28)), c, deep=True)
    a.fill(a.P((28, 21), (21, 21), (21, 28)), c[2:] * 3, shade=False)
    a.flat(a.R(8, 10, 23, 12) | a.R(8, 15, 20, 17), c[2])


@glyph
def speech(a, c=WHITE, tail='left'):
    a.fill(a.R(2, 3, 30, 22, 6), c, deep=True)
    a.fill(a.P((8, 19), (7, 30), (17, 21)) if tail == 'left' else a.P((24, 19), (25, 30), (15, 21)), c, shade=False)


@glyph
def thought(a, c=WHITE):
    a.fill(a.E(9, 12, 7) | a.E(16, 8, 7.5) | a.E(24, 12, 6.5) | a.E(17, 15, 8), c, deep=True)
    a.fill(a.E(9, 24, 3), c)
    a.fill(a.E(4.5, 29, 1.9), c, shade=False)


@glyph
def megaphone(a, c=ROSE):
    a.fill(a.P((5, 12), (25, 2), (25, 28), (5, 20)), c, deep=True)
    a.fill(a.R(1, 11, 7, 21, 1), DARK)
    a.fill(a.R(9, 19, 14, 28, 1), DARK)


@glyph
def mic(a, c=STEEL):
    a.fill(a.L([(8, 26), (17, 15)], 5), DARK)
    a.fill(a.E(20, 11, 9.5), c, deep=True)
    a.flat(a.L([(14, 8), (24, 17)], 1) | a.L([(19, 3), (28, 12)], 1) | a.L([(13, 12), (20, 19)], 1), c[2])


@glyph
def speaker(a, c=STEEL):
    a.fill(a.P((2, 11), (9, 11), (19, 3), (19, 29), (9, 21), (2, 21)), c, deep=True)


@glyph
def kazoo(a, c=YELLOW):
    a.fill(a.P((1, 20), (24, 13), (31, 13), (31, 24), (24, 24), (2, 25)), c, deep=True)
    a.fill(a.E(19, 12, 6.5, 4), RED, deep=True)
    a.flat(a.E(19, 11, 3.5, 1.5), RED[2])


@glyph
def trombone(a, c=GOLD):
    a.flat(a.R(3, 11, 25, 13.5) | a.R(3, 21, 22, 23.5), c)
    a.flat(a.A(25, 17, 5, -90, 90, 2.8), c)
    a.fill(a.L([(3, 11), (3, 23)], 2.4), c, shade=False)
    a.fill(a.P((11, 11), (11, 13.5), (22, 7), (22, 2)) if False else a.P((21, 22), (31, 14), (31, 31)), c, deep=True)


@glyph
def drum(a, c=RED):
    a.fill(a.L([(5, 12), (17, 1)], 2.4) | a.L([(27, 12), (15, 1)], 2.4), WOOD, shade=False)
    a.fill(a.R(4, 15, 28, 28, 1) | a.E(16, 28, 12, 3), c, deep=True)
    a.flat(a.L([(7, 19), (13, 29)], 1.2) | a.L([(13, 29), (19, 19)], 1.2) | a.L([(19, 19), (25, 28)], 1.2), GOLD)
    a.fill(a.E(16, 15, 12, 4.5), WHITE, shade=False)


@glyph
def spring(a, c=STEEL):
    pts = []
    for i in range(26):
        pts.append((16 + 10 * math.sin(i * .95), 4 + i * .96))
    a.fill(a.L(pts, 3.4), c, shade=False)


@glyph
def disco_ball(a):
    a.flat(a.R(15, 0, 17, 5), WHITE[2])
    m = a.E(16, 17, 13)
    a.fill(m, WHITE, deep=True)
    for i, c in enumerate((CYAN, PINK, YELLOW, PURPLE, GREEN)):
        for j in range(5):
            if (i * 2 + j * 3) % 4 < 2:
                a.flat(m & a.R(4 + j * 5, 5 + i * 5, 8.8 + j * 5, 8.8 + i * 5), c)
    a.flat(m & (a.R(8.8, 0, 9.2, 32) | a.R(0, 8.8, 32, 9.2)), WHITE[2])


# ------------------------------------------------------------------ blocks and places
@glyph
def block(a, top=GRASS, left=DIRT, right=None, marks=None):
    right = right or left
    a.fill(a.P((16, 2), (29, 9), (16, 16), (3, 9)), top, shade=False)
    a.fill(a.P((3, 9), (16, 16), (16, 30), (3, 23)), (left[1], left[1], left[1]), shade=False)
    a.fill(a.P((29, 9), (16, 16), (16, 30), (29, 23)), (right[2], right[2], right[2]), shade=False)
    a.flat(a.L([(4, 9), (16, 2.5), (28, 9)], .8), top[0])


@glyph
def ore(a, c=DIAMOND, stone=STONE):
    a.fill(a.P((16, 2), (29, 9), (16, 16), (3, 9)), stone, shade=False)
    a.fill(a.P((3, 9), (16, 16), (16, 30), (3, 23)), (stone[1], stone[1], stone[1]), shade=False)
    a.fill(a.P((29, 9), (16, 16), (16, 30), (29, 23)), (stone[2], stone[2], stone[2]), shade=False)
    for x, y in ((15, 6), (21, 9), (10, 9), (7, 15), (12, 21), (20, 19), (25, 15), (22, 24)):
        a.fill(a.R(x - 1.6, y - 1.4, x + 1.6, y + 1.4), c, shade=False)


@glyph
def table(a):
    a.fill(a.P((16, 2), (29, 9), (16, 16), (3, 9)), WOOD, shade=False)
    for x, y in ((12, 7), (16, 9), (20, 11), (12, 11), (20, 7)):
        a.flat(a.L([(x - 3, y), (x + 3, y)], .9), BROWN[2])
    a.flat(a.L([(9, 8), (19, 13)], .9) | a.L([(13, 6), (23, 11)], .9), BROWN[2])
    a.fill(a.P((3, 9), (16, 16), (16, 30), (3, 23)), (BROWN[1],) * 3, shade=False)
    a.fill(a.P((29, 9), (16, 16), (16, 30), (29, 23)), (BROWN[2],) * 3, shade=False)
    a.flat(a.P((6, 14), (13, 18), (13, 25), (6, 21)), WOOD)
    a.flat(a.P((26, 14), (19, 18), (19, 25), (26, 21)), BROWN)


@glyph
def chest(a, c=WOOD, band=GOLD):
    a.fill(a.R(3, 6, 29, 28, 3), c, deep=True)
    a.flat(a.R(3, 15, 29, 17.5), c[2])
    a.fill(a.R(13, 12.5, 19, 20.5, 1), band)


@glyph
def furnace(a, on=True):
    a.fill(a.R(4, 3, 28, 29, 1), STONE, deep=True)
    a.fill(a.R(8, 8, 24, 12), STONE[2:] * 3, shade=False)
    a.fill(a.R(9, 16, 23, 26), OUT, shade=False)
    if on:
        a.fill(a.P((11, 26), (13, 20), (16, 23), (19, 18), (21, 26)), ORANGE, shade=False)


@glyph
def microwave(a):
    a.fill(a.R(1, 6, 31, 28, 2), STEEL, deep=True)
    a.fill(a.R(4, 9, 22, 25, 1), SHADOW, shade=False)
    a.fill(a.E(13, 21, 6, 2.2), YELLOW, shade=False)
    a.fill(a.R(24, 9, 28.5, 14), CYAN, shade=False)
    for y in (17, 20, 23):
        a.flat(a.R(24.5, y, 26, y + 1) | a.R(27, y, 28.5, y + 1), DARK)


@glyph
def hay(a):
    a.fill(a.R(3, 6, 29, 28, 2), YELLOW, deep=True)
    for x in (6.5, 11.5, 16.5, 21.5, 26):
        a.flat(a.L([(x, 7), (x + 1, 27)], 1.1), GOLD)
    a.fill(a.R(3, 12, 29, 15) | a.R(3, 20, 29, 23), BROWN, shade=False)


@glyph
def wool(a, c=WHITE):
    a.fill(a.R(4, 4, 28, 28, 3), c, deep=True)
    a.flat(a.A(13, 12, 4, 180, 400, 1.4) | a.A(21, 20, 4, 180, 400, 1.4) | a.A(10, 22, 2.6, 200, 400, 1.2), c[2])


@glyph
def pillow(a, c=WHITE):
    a.fill(a.C((3, 5), (3, 5), (16, 8), (29, 5), (29, 5), (27, 16), (29, 27), (29, 27), (16, 24), (3, 27), (3, 27), (5, 16)), c, deep=True)
    a.flat(a.B([(7, 9), (10, 14), (8, 19)], 1.2), c[2])


@glyph
def wall(a, c=STONE):
    a.fill(a.R(2, 2, 30, 30), c, deep=True)
    for y in (9, 16, 23):
        a.flat(a.R(2, y, 30, y + 1), c[2])
    for x, y0 in ((10, 2), (22, 2), (16, 9), (6, 9), (26, 9), (10, 16), (22, 16), (16, 23), (6, 23), (26, 23)):
        a.flat(a.R(x, y0, x + 1, y0 + 7), c[2])


@glyph
def glass_pane(a, c=ICE):
    a.fill(a.R(4, 2, 28, 30), c, deep=True)
    a.fill(a.R(4, 2, 28, 4) | a.R(4, 28, 28, 30) | a.R(4, 2, 6, 30) | a.R(26, 2, 28, 30), STEEL, shade=False)
    a.flat(a.L([(10, 12), (17, 6)], 1.4) | a.L([(11, 19), (22, 8)], 1.4), WHITE[0])


@glyph
def mirror(a, c=GOLD):
    a.fill(a.E(16, 15, 11.5, 14), c, deep=True)
    a.fill(a.E(16, 15, 8.5, 11), ICE, shade=False)
    a.flat(a.L([(11, 15), (16, 9)], 1.8) | a.L([(13, 20), (20, 12)], 1.5), WHITE[0])
    a.fill(a.R(13, 28, 19, 31, 1), c)


@glyph
def stairs(a, c=WOOD):
    a.fill(a.P((2, 30), (2, 22), (9, 22), (9, 15), (16, 15), (16, 8), (23, 8), (23, 2), (30, 2), (30, 30)), c, deep=True)


@glyph
def ladder(a, c=WOOD):
    a.fill(a.R(7, 1, 11, 31) | a.R(21, 1, 25, 31) | a.R(8, 5, 24, 8) | a.R(8, 12, 24, 15) | a.R(8, 19, 24, 22) | a.R(8, 26, 24, 29), c, shade=False)


@glyph
def bed(a, c=RED):
    a.fill(a.R(2, 23, 5, 31) | a.R(27, 23, 30, 31), BROWN, shade=False)
    a.fill(a.R(2, 12, 7, 27, 1), WOOD)
    a.fill(a.R(5, 20, 30, 27, 1), WHITE)
    a.fill(a.R(14, 17, 30, 26, 1.5), c, deep=True)
    a.fill(a.R(6, 15, 14, 21, 2), WHITE, deep=True)


@glyph
def house(a, c=WOOD, roof=RED):
    a.fill(a.R(6, 14, 26, 30), c, deep=True)
    a.fill(a.P((1, 16), (16, 2), (31, 16)), roof, deep=True)
    a.fill(a.R(13, 20, 19, 30, 1), BROWN, shade=False)


@glyph
def campfire(a):
    a.fill(a.L([(5, 28), (27, 22)], 4.4) | a.L([(5, 22), (27, 28)], 4.4), BROWN, shade=False)
    flame(a, 7, 2, .56, sticker=False)


@glyph
def snowman(a):
    a.fill(a.E(16, 22, 9.5), WHITE, deep=True)
    a.fill(a.E(16, 10, 7.5), WHITE, deep=True)
    a.flat(a.R(12.5, 8, 14.5, 10) | a.R(17.5, 8, 19.5, 10), OUT)
    a.fill(a.P((15, 11), (17, 11), (24, 13), (15, 14)) if False else a.P((15, 11), (23, 13), (15, 14)), ORANGE, shade=False)
    a.flat(a.E(16, 20, 1.2) | a.E(16, 25, 1.2), OUT)
    a.fill(a.L([(8, 19), (1, 14)], 1.6) | a.L([(24, 19), (31, 14)], 1.6), BROWN, shade=False)


@glyph
def dummy(a, c=TAN):
    a.fill(a.R(14, 10, 18, 31), WOOD, shade=False)
    a.fill(a.R(3, 13, 29, 17), WOOD, shade=False)
    a.fill(a.E(16, 7.5, 6.5), c, deep=True)
    a.fill(a.R(9, 12, 23, 24, 1), c, deep=True)
    target(a, 10, 12, .38, c=RED, sticker=False)


@glyph
def trap_plate(a, c=STONE):
    a.fill(a.P((2, 20), (22, 20), (30, 12), (10, 12)), c, shade=False)
    a.fill(a.P((2, 20), (22, 20), (22, 25), (2, 25)), (c[2],) * 3, shade=False)
    a.fill(a.P((22, 20), (30, 12), (30, 17), (22, 25)), (DARK[1],) * 3, shade=False)


@glyph
def saddle(a, c=BROWN):
    a.fill(a.C((3, 12), (3, 12), (11, 14), (22, 13), (29, 7), (30, 16), (22, 21), (10, 21)), c, deep=True)
    a.fill(a.R(13, 19, 18, 28), DARK, shade=False)
    a.fill(a.R(11, 26, 20, 30, 1), STEEL)


@glyph
def skate(a, c=WHITE):
    a.fill(a.L([(3, 29), (29, 29)], 2.2), STEEL, shade=False)
    a.fill(a.R(9, 24, 11.5, 29) | a.R(21, 24, 23.5, 29), STEEL, shade=False)
    a.fill(a.C((6, 3), (6, 3), (17, 3), (17, 3), (18, 12), (27, 17), (28, 25), (28, 25), (5, 25), (5, 25)), c, deep=True)
    a.flat(a.L([(9, 8), (16, 8)], 1.2) | a.L([(9, 12), (16, 12)], 1.2) | a.L([(9, 16), (19, 16)], 1.2), RED)


@glyph
def sock(a, c=WHITE, band=RED):
    a.fill(a.C((9, 2), (9, 2), (21, 2), (21, 2), (20, 16), (28, 21), (28, 28), (20, 30), (10, 25), (8, 17)), c, deep=True)
    a.flat(a.R(9, 5, 21, 7.5) | a.R(9, 10, 21, 12.5), band)


@glyph
def spoon(a, c=STEEL):
    a.fill(a.L([(6, 27), (19, 14)], 3), c, shade=False)
    a.fill(a.E(21.5, 10.5, 7, 8.5), c, deep=True)
    a.flat(a.E(21, 11, 3.8, 5), c[2])


@glyph
def yarn(a, c=STEEL):
    a.fill(a.E(15, 14, 12), c, deep=True)
    a.flat(a.A(9, 11, 10, -30, 60, 1.4) | a.A(21, 12, 10, 120, 220, 1.4) | a.A(15, 25, 11, -130, -50, 1.4), c[2])
    a.fill(a.B([(15, 26), (21, 30), (30, 27)], 2), c, shade=False)


@glyph
def chef_hat(a, c=WHITE):
    a.fill(a.E(9, 12, 7) | a.E(16, 9, 8) | a.E(23, 12, 7) | a.R(8, 12, 24, 27, 1), c, deep=True)
    a.flat(a.R(8, 22, 24, 24), c[2])


@glyph
def horned_helm(a, c=IRON):
    a.fill(a.C((6, 12), (6, 12), (2, 9), (1, 2), (1, 2), (8, 6), (11, 9)) | a.C((26, 12), (26, 12), (30, 9), (31, 2), (31, 2), (24, 6), (21, 9)), BONE, deep=True)
    a.fill(a.C((5, 26), (5, 13), (16, 5), (27, 13), (27, 26), (27, 26), (21, 26), (19, 19), (13, 19), (11, 26), (5, 26)), c, deep=True)
    a.fill(a.R(14.5, 7, 17.5, 30, 1), GOLD, shade=False)
    a.flat(a.R(9, 16, 13, 19) | a.R(19, 16, 23, 19), RED)


@glyph
def house_key(a):
    key(a)


@glyph
def window(a, c=CYAN):  # a floating see-through status window
    a.fill(a.R(1, 4, 31, 28, 3), NAVY, shade=False)
    a.fill(a.A(0, 0, 0, 0, 0, 0) & a.R(0, 0, 0, 0), c)
    m = a.R(1, 4, 31, 28, 3) & ~a.R(2.5, 5.5, 29.5, 26.5, 2)
    a.flat(m, c[0])
    a.flat(a.R(5, 9, 15, 11) | a.R(5, 14, 27, 16) | a.R(5, 19, 22, 21), c[1])
    a.flat(a.E(25, 10, 2), PINK)


@glyph
def scroll(a, c=BONE):
    a.fill(a.R(7, 5, 25, 27), c, deep=True)
    a.fill(a.R(4, 2, 28, 7, 2.4) | a.R(4, 25, 28, 30, 2.4), TAN, deep=False)
    a.flat(a.R(10, 11, 22, 13) | a.R(10, 15, 22, 17) | a.R(10, 19, 19, 21), c[2])


@glyph
def tag_price(a, c=YELLOW):
    a.fill(a.P((2, 4), (16, 4), (30, 18), (18, 30), (4, 16)), c, deep=True)
    a.flat(a.E(9, 10, 2.4), OUT)


@glyph
def handshake(a):
    a.fill(a.L([(1, 13), (11, 15), (20, 22)], 6), SKIN, shade=False)
    a.fill(a.L([(31, 13), (21, 15), (12, 22)], 6), TAN, shade=False)
    a.flat(a.L([(14, 15), (20, 21)], 1) | a.L([(18, 13), (24, 19)], 1), SKIN[2])
    a.fill(a.R(0, 8, 4, 19), BLUE, shade=False)
    a.fill(a.R(28, 8, 32, 19), RED, shade=False)


@glyph
def lectern(a):
    a.fill(a.P((3, 11), (29, 4), (29, 10), (3, 16)), WOOD)
    a.fill(a.R(13, 12, 19, 27), BROWN)
    a.fill(a.R(7, 26, 25, 31, 1), WOOD)
    a.fill(a.P((6, 8), (15, 6), (15, 3), (6, 5)), BONE, shade=False)


@glyph
def lightbulb(a, c=YELLOW):
    a.fill(a.E(16, 12, 11), c, deep=True)
    a.fill(a.R(11.5, 20, 20.5, 25), c)
    a.fill(a.R(11.5, 24, 20.5, 30, 1), STEEL)
    a.flat(a.L([(10, 9), (13, 6)], 1.8), WHITE[0])


@glyph
def turret(a, c=LIME):
    a.fill(a.P((6, 31), (10, 20), (22, 20), (26, 31)), STEEL, deep=True)
    a.fill(a.R(8, 9, 22, 21, 3), c, deep=True)
    a.fill(a.R(20, 12, 31, 16), c)
    a.fill(a.R(21, 16, 29, 18.5), c, shade=False)


@glyph
def minigun(a, c=LIME):
    a.fill(a.R(1, 6, 26, 10) | a.R(1, 11.5, 26, 15.5) | a.R(1, 17, 26, 21), c)
    a.fill(a.R(3, 4, 7, 23, 1) | a.R(17, 4, 21, 23, 1), c[2:] * 3, shade=False)
    a.fill(a.R(19, 4, 31, 23, 2), STEEL, deep=True)
    a.fill(a.R(22, 22, 27, 31, 1), DARK)


@glyph
def nuke(a, c=LIME):
    a.fill(a.R(11, 2, 21, 5), c, shade=False)
    a.fill(a.E(16, 18, 9.5, 13), c, deep=True)
    a.fill(a.P((7, 24), (2, 31), (13, 29)) | a.P((25, 24), (30, 31), (19, 29)), STEEL)
    a.flat(a.E(16, 16, 2) | a.P((16, 15), (10, 8), (14, 7)) | a.P((16, 15), (22, 8), (18, 7)) | a.P((16, 17), (11, 23), (21, 23)), OUT)


@glyph
def mushroom_cloud(a, c=ORANGE):
    a.fill(a.R(13, 15, 19, 27) | a.P((9, 30), (23, 30), (19, 24), (13, 24)), STEEL, deep=True)
    a.fill(a.E(9, 12, 7) | a.E(16, 8, 8) | a.E(23, 12, 7) | a.E(16, 15, 7), c, deep=True)
    a.flat(a.E(16, 12, 4), YELLOW[1])


@glyph
def dome(a, c=LIME):
    a.fill((a.E(16, 22, 15.5, 19) & ~a.E(16, 22, 12.5, 16)) & a.R(0, 0, 32, 26), c, shade=False)
    a.fill(a.R(1, 25, 31, 30, 2), DARK)
    a.flat(a.A(16, 22, 10, 200, 260, 1.8), WHITE[0])


@glyph
def lantern_ring(a, c=LIME):
    a.fill(a.E(16, 22, 9.5) & ~a.E(16, 22, 6), STEEL, shade=False)
    a.fill(a.R(8, 3, 24, 15, 3), c, deep=True)
    a.flat(a.A(16, 9, 3.6, -40, 310, 1.4) | a.R(11, 11.5, 21, 13), c[2])


@glyph
def gate(a, c=CYAN):
    a.fill(a.E(16, 16, 14.5, 15.5), SHADOW, shade=False)
    m = a.E(16, 16, 14.5, 15.5) & ~a.E(16, 16, 11.5, 12.5)
    a.flat(m, c[1])
    a.flat(a.A(16, 16, 15, 190, 280, 1.5), c[0])
    a.flat(a.E(16, 16, 6, 7), VIOLET[1])
    a.flat(a.E(16, 16, 2.6, 3.4), WHITE[0])


@glyph
def lightning_chain(a, c=YELLOW):
    a.fill(a.E(4, 25, 3.6) | a.E(16, 7, 3.6) | a.E(28, 24, 3.6), c, shade=False)
    a.fill(a.L([(4, 25), (9, 14), (11, 19), (16, 7), (19, 18), (23, 12), (28, 24)], 2.2), c, shade=False)


@glyph
def ecg(a, c=RED):
    a.flat(a.L([(1, 17), (9, 17), (12, 9), (17, 26), (21, 13), (23, 17), (31, 17)], 2.2), c)
