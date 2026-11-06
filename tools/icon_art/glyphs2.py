# More symbols: faces and everything the icon tables turned out to need.
from .glyphs import *
from .glyphs import _eye


@glyph
def face(a, kind='plain', c=YELLOW):
    a.fill(a.E(16, 16, 14), c, deep=True)
    if kind == 'gasp':
        a.flat(a.E(10.5, 12, 2.4, 3.4) | a.E(21.5, 12, 2.4, 3.4), OUT)
        a.flat(a.E(16, 22, 4, 5), OUT)
        a.flat(a.L([(6, 7), (11, 5)], 1.6) | a.L([(26, 7), (21, 5)], 1.6), c[2])
    elif kind == 'laugh':
        a.flat(a.A(10.5, 13.5, 3, 200, 340, 1.8) | a.A(21.5, 13.5, 3, 200, 340, 1.8), OUT)
        a.flat(a.C((7, 17), (7, 17), (25, 17), (25, 17), (21, 26), (11, 26)), OUT)
        a.flat(a.R(9.5, 18, 22.5, 20), WHITE[0])
    elif kind == 'yawn':
        a.flat(a.L([(7, 12), (13, 13)], 1.8) | a.L([(25, 12), (19, 13)], 1.8), OUT)
        a.flat(a.E(16, 21, 6, 7), OUT)
        a.flat(a.E(16, 25, 3.6, 2.6), ROSE)
    elif kind == 'dizzy':
        a.flat(a.A(10.5, 13, 3.2, 0, 300, 1.2) | a.A(21.5, 13, 3.2, 90, 400, 1.2), OUT)
        a.flat(a.B([(10, 23), (14, 21), (18, 24), (22, 22)], 1.6), OUT)
    elif kind == 'dead':
        for x in (10.5, 21.5):
            a.flat(a.L([(x - 3, 9.5), (x + 3, 15.5)], 1.7) | a.L([(x + 3, 9.5), (x - 3, 15.5)], 1.7), OUT)
        a.flat(a.B([(10, 22), (22, 22)], 1.7), OUT)
        a.fill(a.R(16, 22, 21, 28, 2), PINK, shade=False)
    elif kind == 'sick':
        a.flat(a.L([(7, 11), (13, 13)], 1.8) | a.L([(25, 11), (19, 13)], 1.8), OUT)
        a.flat(a.B([(9, 23), (13, 20), (16, 23), (19, 20), (23, 23)], 1.7), OUT)
    elif kind == 'angry':
        a.flat(a.L([(6, 8), (13, 12)], 2.2) | a.L([(26, 8), (19, 12)], 2.2), OUT)
        a.flat(a.E(10.5, 14, 2) | a.E(21.5, 14, 2), OUT)
        a.flat(a.A(16, 26, 5.5, -160, -20, 1.8), OUT)
    elif kind == 'cool':
        glasses(a, stache=False, sticker=False)
        a.flat(a.A(16, 18, 6, 30, 150, 1.8), OUT)
    elif kind == 'sleep':
        a.flat(a.A(10.5, 11, 3, 30, 150, 1.8) | a.A(21.5, 11, 3, 30, 150, 1.8), OUT)
        a.flat(a.E(16, 22, 2.8, 2.2), OUT)
    elif kind == 'sneeze':
        a.flat(a.L([(7, 11), (13, 13)], 2) | a.L([(13, 13), (7, 15)], 2) | a.L([(25, 11), (19, 13)], 2) | a.L([(19, 13), (25, 15)], 2), OUT)
        a.flat(a.E(16, 23, 5, 3.4), OUT)
    elif kind == 'smirk':
        a.flat(a.E(10.5, 13, 2.2, 2.6) | a.E(21.5, 13, 2.2, 2.6), OUT)
        a.flat(a.B([(9, 22), (16, 23), (23, 19)], 1.8), OUT)
    else:
        a.flat(a.E(10.5, 13, 2.2, 3) | a.E(21.5, 13, 2.2, 3), OUT)
        a.flat(a.A(16, 17, 6.5, 30, 150, 1.8), OUT)


@glyph
def heart_part(a, fill=.3, c=RED):
    m = a.E(10, 11.5, 7.5) | a.E(22, 11.5, 7.5) | a.P((3.2, 14), (28.8, 14), (16, 29.5))
    a.fill(m, DARK, deep=True)
    a.fill(m & a.R(0, 4 + (1 - fill) * 26, 32, 32), c, deep=False)


@glyph
def heatwaves(a, c=ORANGE):
    for x in (7, 16, 25):
        a.flat(a.B([(x, 29), (x + 3, 23), (x - 3, 16), (x + 3, 9), (x, 3)], 3), c)


@glyph
def zzz(a, c=WHITE):
    a.fill(font_mask(a, 'Z', 1, 15, 15) | font_mask(a, 'Z', 13, 7, 11) | font_mask(a, 'Z', 23, 1, 8), c, shade=False)


@glyph
def gust(a, c=ICE):
    a.fill(a.L([(2, 11), (19, 11)], 3.2) | a.A(19, 6.5, 4.5, -270, 60, 3.2), c, shade=False)
    a.fill(a.L([(2, 18), (26, 18)], 3.2) | a.A(26, 22.5, 4.5, -90, 160, 3.2), c, shade=False)
    a.fill(a.L([(5, 25), (15, 25)], 3.2) | a.A(15, 28.5, 3.4, -90, 140, 2.8), c, shade=False)


@glyph
def glider(a, c=WHITE):
    a.fill(a.P((31, 4), (2, 15), (12, 19), (24, 9), (15, 22), (15, 29), (20, 23), (26, 26)), c)
    a.flat(a.L([(13, 19), (25, 10)], 1), c[2])


@glyph
def arc_path(a, c=WHITE, tip=True):  # a dotted jump from the bottom left to the bottom right
    for i in range(0, 7):
        t = i / 7
        x, y = 3 + 24 * t, 26 - 22 * 4 * t * (1 - t)
        a.fill(a.E(x, y, 1.7), c, shade=False)
    if tip:
        a.fill(a.P((29, 28), (21, 24), (28, 18)), c, shade=False)


@glyph
def claws(a, c=RED):
    for dx in (-8, 0, 8):
        a.fill(a.C((16 + dx + 4, 2), (16 + dx + 4, 2), (16 + dx + 1, 16), (16 + dx - 4, 30), (16 + dx - 4, 30), (16 + dx - 1, 16)), c, shade=False)


@glyph
def slash(a, c=WHITE):
    a.fill(a.C((29, 1), (29, 1), (26, 16), (12, 25), (2, 30), (2, 30), (19, 15)), c, shade=False)


@glyph
def beam(a, c=CYAN, core=WHITE, w=9):
    a.fill(a.R(8, 16 - w, 32, 16 + w), c, shade=False)
    a.flat(a.R(8, 16 - w / 2, 32, 16 + w / 2), core)
    a.fill(a.E(9, 16, w + 2.5), c, shade=False)
    a.flat(a.E(9, 16, w - .5), core)


@glyph
def iron_ball(a, c=IRON):
    a.fill(a.L([(29, 1), (20, 11)], 2.2), STEEL, shade=False)
    a.fill(a.E(14, 18, 12.5), c, deep=True)
    a.flat(a.E(10, 13, 3), c[0])


@glyph
def weight(a, c=IRON):
    a.fill(a.P((9, 12), (23, 12), (29, 30), (3, 30)), c, deep=True)
    a.fill(a.A(16, 11, 6, -180, 0, 3.4), c, shade=False)


@glyph
def gear(a, c=STEEL):
    m = a.E(16, 16, 10.5)
    for i in range(8):
        a.push(0, 0, 1, r=i * 45)
        m = m | a.R(13, 1.5, 19, 9, 1)
        a.pop()
    a.fill(m & ~a.E(16, 16, 4), c, deep=True)


@glyph
def boomerang(a, c=WOOD):
    a.fill(a.C((4, 8), (4, 8), (17, 6), (29, 18), (29, 18), (22, 24), (22, 24), (16, 15), (8, 16)), c, deep=True)


@glyph
def pocket(a, c=NAVY):
    a.fill(a.C((3, 5), (3, 5), (29, 5), (29, 5), (28, 22), (16, 30), (4, 22)), c, deep=True)
    a.flat(a.R(3, 5, 29, 9), c[2])
    a.flat(a.L([(6, 12), (6, 21), (16, 27), (26, 21), (26, 12)], .8), GOLD)


@glyph
def fluff(a, c=STEEL):
    a.fill(a.E(10, 19, 7) | a.E(18, 14, 8) | a.E(23, 21, 6) | a.E(15, 23, 6), c, deep=True)
    a.flat(a.L([(4, 9), (9, 13)], 1.2) | a.L([(27, 7), (23, 11)], 1.2), c[1])


@glyph
def med_cross(a, c=RED):
    a.fill(a.E(16, 16, 14), WHITE, deep=True)
    a.fill(a.R(12.5, 6, 19.5, 26) | a.R(6, 12.5, 26, 19.5), c, shade=False)


@glyph
def fork_knife(a, c=STEEL):
    a.fill(a.R(7, 13, 10, 31) | a.R(4, 2, 5.6, 12) | a.R(7.7, 2, 9.3, 12) | a.R(11.4, 2, 13, 12) | a.R(4, 10, 13, 14), c, shade=False)
    a.fill(a.C((20, 2), (20, 2), (27, 8), (27, 19), (23, 19)) | a.R(20, 2, 23.5, 31), c, shade=False)


@glyph
def ear(a, c=SKIN):
    a.fill(a.C((9, 12), (13, 3), (23, 3), (27, 11), (22, 19), (19, 26), (13, 29), (10, 25), (13, 21), (11, 17)), c, deep=True)
    a.flat(a.A(18, 11, 5, -170, 80, 1.8), c[2])


@glyph
def domino(a, c=CAT):
    a.fill(a.C((1, 10), (1, 10), (9, 8), (16, 12), (23, 8), (31, 10), (31, 10), (29, 20), (21, 23), (16, 18), (11, 23), (3, 20)), c, deep=True)
    a.flat(a.E(10, 15, 3.5, 2.8) | a.E(22, 15, 3.5, 2.8), WHITE[0])


@glyph
def bobber(a):
    a.fill(a.L([(16, 1), (16, 8)], 1.4), WHITE, shade=False)
    m = a.E(16, 18, 10.5, 11.5)
    a.fill(m, WHITE, deep=True)
    a.fill(m & a.R(0, 0, 32, 18), RED, deep=False)


@glyph
def bars(a, c=GREEN):
    a.fill(a.R(3, 20, 10, 30, 1), RED)
    a.fill(a.R(12.5, 12, 19.5, 30, 1), YELLOW)
    a.fill(a.R(22, 3, 29, 30, 1), c)


@glyph
def pumpkin(a):
    a.flat(a.R(14, 1, 18, 7), GREEN)
    a.fill(a.C((3, 16), (6, 7), (16, 5), (26, 7), (29, 16), (26, 26), (16, 28), (6, 26)), ORANGE, deep=True)
    a.flat(a.P((8, 14), (13, 11), (13, 16)) | a.P((24, 14), (19, 11), (19, 16)) | a.P((16, 15), (18, 18), (14, 18)), OUT)
    a.flat(a.P((7, 20), (11, 19), (13, 22), (16, 20), (19, 22), (21, 19), (25, 20), (21, 25), (11, 25)), OUT)


@glyph
def clapper(a):
    a.push(0, 0, 1, r=-8)
    a.fill(a.P((2, 12), (29, 5), (30, 10), (3, 17)), WHITE, shade=False)
    for x, y in ((6, 10), (13, 8), (20, 6)):
        a.flat(a.P((x, y + 1), (x + 3, y), (x + 5, y + 5), (x + 2, y + 6)), OUT)
    a.pop()
    a.fill(a.R(3, 15, 29, 30, 1), CAT, deep=True)
    a.flat(a.R(6, 20, 20, 21.5) | a.R(6, 24, 15, 25.5), WHITE[1])


@glyph
def hive(a, c=GOLD):
    for y, w in ((6, 8), (12, 12), (18, 13), (24, 10)):
        a.fill(a.R(16 - w, y - 3, 16 + w, y + 3, 2.8), c)
    a.flat(a.E(16, 20, 3, 3.4), OUT)


@glyph
def comb(a, c=GOLD):
    def hexa(x, y, r=6.3):
        return a.P(*[(x + r * math.cos(i * math.pi / 3), y + r * math.sin(i * math.pi / 3)) for i in range(6)])
    for x, y in ((9, 9), (21, 9), (15, 19.5), (27, 19.5), (3.5, 19.5)):
        a.fill(hexa(x, y), c)
        a.flat(hexa(x, y, 3.2), YELLOW[0])


@glyph
def dragon(a, c=PURPLE):  # head from the side, snout to the right, jaws open
    a.fill(a.P((9, 8), (1, 1), (15, 5)), BONE)
    a.fill(a.P((1, 31), (1, 16), (4, 9), (13, 4), (21, 7), (31, 11), (31, 16), (20, 16), (16, 21), (22, 22), (28, 22), (24, 28), (11, 28), (9, 31)), c, deep=True)
    a.fill(a.P((1, 18), (1, 12), (-1, 15)) | a.P((3, 24), (1, 21), (0, 26)), c, shade=False)
    a.fill(a.P((21, 16), (24, 16), (22.5, 20)) | a.P((26, 16), (29, 16), (27.5, 19.5)), WHITE, shade=False)
    a.fill(a.P((19, 23), (22, 22), (20.5, 18.5)), WHITE, shade=False)
    a.flat(a.P((12, 9), (18, 9), (17, 13), (13, 12)), YELLOW)
    a.flat(a.R(15, 10, 16.5, 13), OUT)
    a.flat(a.R(26, 12, 28, 13.5), c[2])


@glyph
def donkey(a, c=STONE):
    a.fill(a.E(10, 6, 3.4, 7) | a.E(22, 6, 3.4, 7), c)
    a.fill(a.R(7, 8, 25, 31, 6), c, deep=True)
    a.fill(a.R(9, 20, 23, 31, 4), BONE, shade=False)
    a.flat(a.R(12, 25, 14, 27) | a.R(18, 25, 20, 27), OUT)
    a.flat(a.L([(8, 12), (14, 15)], 2) | a.L([(24, 12), (18, 15)], 2), OUT)


@glyph
def piggy_bank(a, c=PINK):
    a.fill(a.R(8, 23, 12, 30) | a.R(20, 23, 24, 30), c)
    a.fill(a.E(15, 17, 13, 10), c, deep=True)
    a.fill(a.R(24, 14, 31, 22, 2), c)
    a.flat(a.R(26, 16.5, 27.5, 19.5) | a.R(28.5, 16.5, 30, 19.5), c[2])
    a.flat(a.R(10, 8, 20, 10), OUT)
    a.flat(a.R(21, 12, 23, 14), OUT)


@glyph
def bramble(a, c=FOREST):
    a.fill(a.B([(2, 25), (10, 15), (20, 19), (30, 7)], 4.5), c, shade=False)
    for x, y, tx, ty in ((7, 18, 2, 12), (11, 17, 13, 9), (15, 18, 13, 26), (20, 18, 23, 26), (24, 14, 19, 8), (27, 10, 31, 16)):
        a.fill(a.P((x - 2, y), (x + 2, y), (tx, ty)), BONE, shade=False)


@glyph
def spiky_hair(a, c=YELLOW, skin=SKIN):
    a.fill(a.R(8, 13, 24, 30, 3), skin, deep=True)
    a.flat(a.R(11, 19, 13.5, 22) | a.R(18.5, 19, 21, 22), OUT)
    a.fill(a.P((6, 17), (3, 8), (9, 11), (9, 2), (14, 9), (16, 1), (19, 9), (24, 2), (23, 11), (29, 8), (26, 17)), c, deep=True)


@glyph
def grave(a, c=STONE):
    a.fill(a.C((7, 28), (7, 10), (11, 4), (16, 3), (21, 4), (25, 10), (25, 28)), c, deep=True)
    a.fill(a.R(1, 27, 31, 31, 1), DIRT)
    a.flat(a.R(14.5, 8, 17.5, 22) | a.R(11, 11, 21, 14), c[2])


@glyph
def hat(a, c=CAT, band=PINK):
    a.fill(a.R(1, 22, 31, 27, 2), c)
    a.fill(a.R(8, 5, 24, 24, 2), c, deep=True)
    a.flat(a.R(8, 17, 24, 21), band)


@glyph
def mime_face(a):
    a.fill(a.E(16, 19, 12, 12.5), WHITE, deep=True)
    a.fill(a.C((3, 11), (9, 3), (22, 3), (29, 9), (25, 11), (8, 12)), CAT, deep=True)
    a.flat(a.L([(10, 15), (10, 20)], 1.8) | a.L([(22, 15), (22, 20)], 1.8), OUT)
    a.flat(a.L([(10, 26), (22, 26)], 2.2), OUT)
    a.flat(a.L([(14, 24), (18, 28)], 1.3) | a.L([(18, 24), (14, 28)], 1.3), RED)


@glyph
def sweat(a, c=CYAN):
    drop(a, 1, 8, .55, c=c, sticker=False)
    drop(a, 17, 2, .42, c=c, sticker=False)
    drop(a, 19, 19, .38, c=c, sticker=False)


@glyph
def angry_cloud(a, c=STEEL):
    a.fill(a.E(8.5, 19, 7.5, 7.5) | a.E(17, 12, 9, 9) | a.E(25, 18, 6.5, 7) | a.R(8, 18, 25, 26.5), c, deep=True)
    a.flat(a.L([(8, 13), (14, 17)], 2.2) | a.L([(26, 13), (20, 17)], 2.2), OUT)
    a.flat(a.E(11, 19, 1.8) | a.E(23, 19, 1.8), OUT)
    a.flat(a.A(17, 26, 4, -160, -20, 1.6), OUT)


@glyph
def jelly_leg(a, c=PINK):
    a.fill(a.B([(10, 2), (6, 8), (12, 14), (6, 20), (10, 26)], 5.5) | a.B([(22, 2), (18, 8), (24, 14), (18, 20), (22, 26)], 5.5), c, shade=False)
    a.fill(a.R(4, 25, 13, 30, 1.5) | a.R(16, 25, 25, 30, 1.5), c)


@glyph
def tail(a, c=CAT):
    pts = [(4, 28), (14, 28), (24, 22), (25, 11), (17, 7), (11, 12), (14, 18), (19, 15)]
    a.fill(a.B(pts, 5.4), (c[0], c[0], c[0]), shade=False)
    a.fill(a.B(pts, 3.2), c, shade=False)
    a.fill(a.E(19, 15, 3.2), PINK, shade=False)


@glyph
def lint_ball(a):
    fluff(a, 2, 4, .95, sticker=False)


@glyph
def stinger(a, c=DARK):
    a.fill(a.P((3, 3), (29, 16), (3, 29), (10, 16)), c)


@glyph
def hoe_path(a, c=SAND):
    a.fill(a.P((10, 3), (22, 3), (31, 30), (1, 30)), c, deep=True)
    for x, y in ((14, 8), (18, 13), (11, 17), (20, 22), (9, 25), (16, 26)):
        a.flat(a.E(x, y, 1.5), STONE)


@glyph
def farmland(a):
    a.fill(a.R(1, 19, 31, 31, 1), DIRT, deep=True)
    for x in (4, 13, 22):
        a.flat(a.R(x, 19, x + 5, 22), DIRT[2])


@glyph
def ladder_up(a):
    ladder(a, sticker=False)


@glyph
def tripwire(a):
    a.fill(a.R(1, 7, 7, 25, 1), WOOD)
    a.fill(a.R(25, 7, 31, 25, 1), WOOD)
    a.flat(a.R(2, 15, 30, 16.5), WHITE[0])
    a.fill(a.E(4, 15.6, 2.3) | a.E(28, 15.6, 2.3), STEEL, shade=False)


@glyph
def hand_grab(a, c=SKIN):  # a claw of a hand, reaching down
    a.fill(a.R(10, 1, 22, 9), c)
    a.fill(a.R(7, 7, 25, 18, 3.5), c, deep=True)
    for x, tx in ((7, 4), (13, 12), (19, 20), (25, 28)):
        a.fill(a.L([(x + (0 if x < 16 else -.5), 16), (tx, 28)], 3.8), c)


@glyph
def ender_chest(a):
    chest(a, c=ENDER, band=SOUL, sticker=False)
    a.flat(a.E(16, 16.5, 2.2), OUT)


@glyph
def hat_chef(a):
    chef_hat(a, sticker=False)


@glyph
def lure_meat(a):
    hook(a, sticker=False)


@glyph
def stone_person(a):
    person(a, c=STONE, legs=STONE, skin=IRON, sticker=False)


@glyph
def wind_arrow(a, c=ICE):
    gust(a, c=c, sticker=False)


@glyph
def trident_fork(a, c=STEEL):
    fork_knife(a, c=c, sticker=False)


@glyph
def cross_swords(a):
    sword(a, 0, 0, .95, r=45, sticker=True)
    sword(a, 0, 0, .95, r=-45, sticker=True)


@glyph
def sword_ring(a, c=STEEL, n=6):
    for i in range(n):
        t = i * 360 / n
        cx = 16 + 10.5 * math.sin(math.radians(t))
        cy = 16 - 10.5 * math.cos(math.radians(t))
        sword(a, cx - 8, cy - 8, .5, r=t + 90, c=c)


@glyph
def small_fists(a, c=SKIN):
    for x, y, s in ((-1, -1, .55), (15, 0, .5), (7, 8, .6), (-2, 17, .47), (17, 17, .47)):
        fist(a, x, y, s, c=c, wrist=False)


@glyph
def fist_side(a, c=SKIN):  # a punch going to the right
    a.fill(a.R(1, 11, 13, 22), BLUE if c is SKIN else c)
    a.fill(a.R(10, 6, 30, 27, 5), c, deep=True)
    for y in (11.5, 16.5, 21.5):
        a.flat(a.L([(23, y), (30, y)], 1.2), c[2])
    a.fill(a.R(11, 15, 22, 24, 3.5), c)
    a.ring(a.R(11, 15, 22, 24, 3.5) & ~a.R(0, 25, 32, 32))


@glyph
def rib_cage_shirt(a):
    a.fill(a.R(5, 3, 27, 30, 3), DARK, deep=True)
    ribs(a, 3, 3, .82, sticker=False)


@glyph
def chest_plate(a, c=BONE):
    a.fill(a.P((8, 2), (13, 5), (19, 5), (24, 2), (31, 7), (28, 14), (25, 12), (25, 30), (7, 30), (7, 12), (4, 14), (1, 7)), c, deep=True)
    a.flat(a.R(15, 7, 17, 28) | a.R(9, 12, 23, 13.5) | a.R(9, 17, 23, 18.5) | a.R(9, 22, 23, 23.5), c[2])


@glyph
def snake_skin(a):
    a.fill(a.B([(4, 26), (13, 28), (24, 24), (23, 18), (10, 16), (9, 10), (18, 8)], 5), TAN, shade=False)
    a.flat(a.L([(6, 25), (8, 28)], 1) | a.L([(12, 26), (13, 30)], 1) | a.L([(19, 24), (20, 28)], 1) | a.L([(14, 15), (15, 19)], 1) | a.L([(9, 10), (6, 13)], 1), TAN[2])


@glyph
def footprint_glow(a):
    foot(a, -1, 11, .62, c=XP, sticker=False)
    foot(a, 13, 0, .62, c=XP, sticker=False)


@glyph
def fuse_bomb(a):
    bomb(a, sticker=False)


@glyph
def lips(a):
    mouth(a, kind='lips', sticker=False)


@glyph
def question_box(a, c=GOLD):
    a.fill(a.R(3, 3, 29, 29, 3), c, deep=True)
    a.fill(font_mask(a, '?', 8.5, 6, 21), WHITE[0], shade=False)


@glyph
def tooth_spark(a):
    mouth(a, kind='smile', sticker=False)


@glyph
def monster_head(a, c=GREEN):  # a plain mob, used when a power is about mobs in general
    a.fill(a.R(5, 5, 27, 28, 2), c, deep=True)
    a.flat(a.R(9, 12, 14, 17) | a.R(18, 12, 23, 17), OUT)
    a.flat(a.R(11, 21, 21, 23.5), OUT)


@glyph
def hole_ground(a):
    a.fill(a.E(16, 19, 15, 10), DIRT, deep=True)
    a.fill(a.E(16, 18.5, 10, 6), SHADOW, shade=False)


@glyph
def blood_drops(a):
    drop(a, 0, 6, .62, c=BLOOD)
    drop(a, 17, 0, .45, c=RED)
    drop(a, 18, 17, .42, c=BLOOD)


@glyph
def x_mark_ground(a, c=RED):
    a.flat(a.L([(6, 12), (26, 26)], 3.4) | a.L([(26, 12), (6, 26)], 3.4), c)


@glyph
def trombone_slide(a, c=GOLD):
    a.flat(a.R(1, 9, 22, 11.5) | a.R(1, 16, 18, 18.5), c)
    a.fill(a.A(22, 13.2, 4.8, -90, 90, 2.6), c, shade=False)
    a.fill(a.A(3, 13.7, 3.6, 90, 270, 2.4), c, shade=False)
    a.fill(a.P((15, 18), (31, 11), (31, 31), (15, 21)), c, deep=True)


@glyph
def coil(a, c=STEEL):
    for i in range(5):
        y = 6 + i * 5
        a.fill(a.L([(7, y + 2.2), (25, y - 2.2)], 3.2), c)
    a.fill(a.R(5, 1, 27, 5, 1) | a.R(5, 27, 27, 31, 1), c)


@glyph
def yarn_ball(a, c=PINK):
    a.fill(a.E(15, 14, 12), c, deep=True)
    a.flat(a.A(5, 8, 13, -10, 70, 1.3) | a.A(3, 16, 13, -30, 40, 1.3) | a.A(27, 8, 13, 110, 190, 1.3) | a.A(29, 17, 13, 150, 215, 1.3), c[2])
    a.fill(a.B([(17, 26), (22, 30), (31, 28)], 2), c, shade=False)


@glyph
def wrecking(a, c=IRON):
    a.fill(a.L([(31, 1), (20, 13)], 1.8), STEEL, shade=False)
    a.fill(a.E(13, 18, 12.5), c, deep=True)
    a.flat(a.E(9, 13, 3), c[0])


@glyph
def chain_links(a, c=STEEL):
    chain(a, c=c, sticker=False)
