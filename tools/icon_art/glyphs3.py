# The last symbols the tables asked for.
from .glyphs2 import *


@glyph
def paw(a, c=PINK, bean=None):
    bean = bean or c
    a.fill(a.E(16, 22, 9, 8), c, deep=True)
    for x, y, r in ((5.5, 15, 3.8), (11.5, 8, 3.9), (20.5, 8, 3.9), (26.5, 15, 3.8)):
        a.fill(a.E(x, y, r, r + .8), bean)


@glyph
def orb(a, c=XP):
    a.fill(a.E(16, 16, 12.5), c, deep=True)
    a.flat(a.E(16, 16, 7), c[0])
    a.flat(a.R(11, 10, 14, 13), WHITE[0])


@glyph
def orbs(a, c=XP):
    orb(a, -1, 12, .55, c=c)
    orb(a, 15, 0, .5, c=c)
    orb(a, 17, 18, .42, c=c)


@glyph
def thumb(a, c=SKIN):
    a.fill(a.R(11, 12, 29, 28, 4), c, deep=True)
    a.fill(a.L([(14, 14), (14, 4)], 6), c)
    a.fill(a.R(3, 13, 10, 29, 1), BLUE)
    for y in (17, 21, 25):
        a.flat(a.L([(20, y), (28, y)], 1), c[2])


@glyph
def webfoot(a, c=ORANGE):
    a.fill(a.R(14.5, 1, 17.5, 15), c, shade=False)
    a.fill(a.P((16, 12), (29, 26), (23, 24), (16, 30), (9, 24), (3, 26)), c, deep=True)
    a.flat(a.L([(16, 15), (16, 26)], 1) | a.L([(15, 15), (8, 22)], 1) | a.L([(17, 15), (24, 22)], 1), c[2])


@glyph
def bubblewrap(a, c=ICE):
    a.fill(a.R(1, 1, 31, 31, 3), STEEL, deep=False)
    for j in range(3):
        for i in range(3):
            x, y = 7 + i * 9, 7 + j * 9
            if (i, j) in ((1, 1), (2, 2)):  # these are popped
                a.flat(a.E(x, y, 3.8), STEEL[2])
            else:
                a.fill(a.E(x, y, 3.8), c)


@glyph
def spots(a, c=LIME, pts=((5, 7), (14, 3), (24, 7), (10, 14), (21, 15))):
    for x, y in pts:
        a.fill(a.E(x, y, 2.3), c, shade=False)


@glyph
def sand_spray(a, c=SAND):
    for x, y, r in ((6, 18, 3), (12, 13, 3.4), (12, 22, 2.8), (19, 8, 3.2), (20, 17, 3.2), (19, 26, 2.6), (27, 4, 2.4), (28, 13, 2.8), (28, 22, 2.8)):
        a.fill(a.E(x, y, r), c, shade=False)


@glyph
def slime_block(a, c=SLIME):
    a.fill(a.R(3, 3, 29, 29, 3), c, deep=True)
    a.flat(a.R(8, 10, 13, 15) | a.R(19, 10, 24, 15) | a.R(11, 20, 21, 23), c[2])


@glyph
def tear(a, c=VIOLET):
    a.fill(a.C((16, 1), (16, 1), (22, 10), (20, 18), (22, 26), (16, 31), (16, 31), (10, 26), (12, 18), (10, 10)), c, shade=False)
    a.fill(a.C((16, 7), (16, 7), (18, 12), (17, 18), (18, 23), (16, 26), (16, 26), (14, 23), (15, 18), (14, 12)), SHADOW, shade=False)


@glyph
def fading_person(a, c=PURPLE):
    """A figure made of stripes, as if it is fading out."""
    p = Art(a.size, a.ss, a.tf[-1])
    person(p, c=c, legs=c, skin=c[0:1] * 3, sticker=False)
    src = p.stack[0]
    for j in range(a.size):
        if j % 2 == 1:
            src[j, :, 3] = 0
    under = a.stack[-1]
    m = src[..., 3] > 0
    under[m] = src[m]


@glyph
def wind_slap_lines(a, c=ICE):
    gust(a, c=c, sticker=False)


@glyph
def rise_mob(a, c=GREEN):
    """A mob floating up: head, with arrows under it."""
    monster_head(a, 6, -1, .65, c=c)
    chevrons(a, 8, 16, .5, c=CYAN, n=2)


@glyph
def cone_wave(a, c=CYAN):
    for r, w in ((10, 3.4), (18, 3.6), (26, 3.8)):
        a.fill(a.A(3, 16, r, -38, 38, w), c, shade=False)


@glyph
def fist_up(a, c=SKIN):
    fist(a, c=c, sticker=False)


@glyph
def small_star_ring(a, c=YELLOW):
    for x, y, s in ((2, 2, .28), (22, 1, .3), (0, 20, .25), (23, 21, .28)):
        star(a, x, y, s, c=c)


@glyph
def seal(a, c=RED):
    """A drawn mark on the ground: a magic circle."""
    a.flat((a.E(16, 16, 15) & ~a.E(16, 16, 12.8)) | (a.E(16, 16, 7.5) & ~a.E(16, 16, 5.6)), c)
    for i in range(3):
        t = -math.pi / 2 + i * math.tau / 3
        a.flat(a.L([(16 + 12.5 * math.cos(t), 16 + 12.5 * math.sin(t)), (16 + 12.5 * math.cos(t + 2.1), 16 + 12.5 * math.sin(t + 2.1))], 1.5), c)


@glyph
def cracked_planet(a, c=STONE):
    m = a.E(16, 16, 14.5)
    a.fill(m, c, deep=True)
    a.flat(m & a.L([(19, 1), (14, 9), (20, 15), (12, 21), (16, 31)], 3), OUT)
    a.flat(m & (a.L([(20, 15), (29, 18)], 1.5) | a.L([(12, 21), (3, 19)], 1.5)), OUT)
    a.flat(m & (a.E(9, 9, 2.6) | a.E(24, 24, 2.6)), c[2])


@glyph
def giant_blade(a, c=STEEL):
    a.fill(a.P((10, 0), (22, 0), (22, 21), (16, 30.5), (10, 21)), c)
    a.flat(a.P((16, 0), (22, 0), (22, 21), (16, 30.5)), c[2])
    a.flat(a.R(15.4, 0, 16.6, 27), c[0])


@glyph
def shuriken_cross(a, c=STEEL):
    a.fill(a.P((16, 1), (19, 13), (31, 16), (19, 19), (16, 31), (13, 19), (1, 16), (13, 13)), c, deep=True)
    a.flat(a.E(16, 16, 2.4), OUT)


@glyph
def hearts_chain(a, c=RED):
    heart(a, -2, 5, .55, c=c, gleam=False)
    heart(a, 15, 5, .55, c=PINK, gleam=False)
    a.fill(a.L([(12, 13), (20, 13)], 2), STEEL, shade=False)


@glyph
def rod_line(a):
    hook(a, sticker=False)


@glyph
def vial_green(a):
    bottle(a, c=GREEN, sticker=False)


@glyph
def kettle(a, c=DARK):
    a.fill(a.E(16, 19, 12), c, deep=True)
    a.fill(a.A(16, 7, 6, -180, 0, 2.6), STEEL, shade=False)


@glyph
def crack_lines(a, c=WHITE):
    for x, y, tx, ty in ((16, 11, 16, 2), (9, 13, 5, 5), (23, 13, 27, 5)):
        a.fill(a.L([(x, y), (tx, ty)], 2.6), c, shade=False)


@glyph
def impact(a, c=YELLOW):
    burst(a, c=c, core=WHITE, sticker=False)


@glyph
def bite_mark(a, c=WHITE):
    teeth(a, sticker=False)


@glyph
def pinch_fingers(a, c=SKIN):
    a.fill(a.E(27, 16, 6, 12), c, deep=True)
    a.fill(a.L([(27, 7), (15, 6), (9, 13.2)], 5.5), c)
    a.fill(a.L([(27, 25), (15, 26), (9, 18.8)], 5.5), c)
    a.flat(a.R(7, 11, 10.5, 13.4) | a.R(7, 18.6, 10.5, 21), WHITE[1])
    a.flat(a.L([(24, 12), (24, 20)], 1), c[2])


@glyph
def wave_palm(a, c=SKIN):
    hand(a, c=c, sticker=False)


@glyph
def shoulder(a, c=RED):
    """A shoulder charge: a body leaning right."""
    a.fill(a.L([(8, 26), (17, 15)], 8.5), c)
    a.fill(a.E(21, 14, 9), c, deep=True)
    a.fill(a.R(5, 2, 15, 12, 2), SKIN)
    a.flat(a.R(10, 5, 12, 8), OUT)


@glyph
def leg_low(a, c=NAVY):
    a.fill(a.L([(3, 5), (10, 19), (24, 24)], 6.5), c)
    a.fill(a.P((20, 19), (31, 22), (31, 29), (20, 28)), BROWN)


@glyph
def scale_tag(a, c=GOLD):
    tag_price(a, c=c, sticker=False)


@glyph
def tiny_mob(a, c=GREEN):
    monster_head(a, c=c, sticker=False)


@glyph
def mob_row(a, c=GREEN):
    monster_head(a, -2, 7, .58, c=c)
    monster_head(a, 15, 7, .58, c=ROSE)


@glyph
def line_ground(a, c=WHITE):
    a.flat(a.R(1, 28, 31, 30.5), c)


@glyph
def chicken_small(a):
    chick(a, sticker=False)


@glyph
def lock(a, c=GOLD):
    a.fill(a.A(16, 12, 7.5, -180, 0, 3.6) | a.R(6.7, 12, 10.3, 16) | a.R(21.7, 12, 25.3, 16), STEEL, shade=False)
    a.fill(a.R(4, 14, 28, 31, 2), c, deep=True)
    a.flat(a.E(16, 21, 2.6) | a.R(14.8, 21, 17.2, 27), OUT)


@glyph
def arrow_rain(a, c=STEEL):
    for x, y in ((2, 0), (12, 5), (21, 0), (6, 13), (17, 14)):
        dart(a, x - 8, y - 7, .55, r=180, c=c)


@glyph
def tall_person(a, c=BLUE):
    a.fill(a.R(11, 21, 15.5, 31) | a.R(16.5, 21, 21, 31), NAVY)


@glyph
def shadow_person(a, pose='stand', eye=RED[0]):
    person(a, pose=pose, c=SHADOW, legs=SHADOW, skin=SHADOW, sticker=False)
