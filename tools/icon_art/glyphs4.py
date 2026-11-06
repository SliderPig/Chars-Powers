# Symbols for the main powers.
from .glyphs3 import *


@glyph
def crystal_ball(a, c=PURPLE):
    a.fill(a.P((7, 31), (10, 23), (22, 23), (25, 31)), WOOD)
    a.fill(a.E(16, 14, 13), c, deep=True)
    a.flat(a.A(16, 14, 9, 190, 260, 2), c[0])
    a.flat(a.E(19, 17, 2.4), WHITE[0])
    a.dot(13, 18, WHITE[0])
    a.dot(21, 10, WHITE[0])


@glyph
def gamepad(a, c=CYAN):
    a.fill(a.C((3, 12), (9, 8), (23, 8), (29, 12), (31, 24), (26, 27), (21, 20), (11, 20), (6, 27), (1, 24)), c, deep=True)
    a.flat(a.R(7.5, 11.5, 9.5, 17.5) | a.R(5.5, 13.5, 11.5, 15.5), NAVY[1])
    a.flat(a.E(21, 16, 1.6), PINK)
    a.flat(a.E(25, 12.5, 1.6), YELLOW)


@glyph
def holo_grid(a, c=CYAN):
    a.fill(a.R(1, 1, 31, 31, 3), NAVY, shade=False)
    a.flat(a.R(1, 1, 31, 31, 3) & ~a.R(2.5, 2.5, 29.5, 29.5, 2), c[0])
    for j in range(3):
        for i in range(3):
            a.flat(a.R(5 + i * 8, 5 + j * 8, 11 + i * 8, 11 + j * 8), c[1] if (i + j) % 2 else c[2])


@glyph
def checklist(a, c=WHITE, tick=GREEN):
    a.fill(a.R(5, 2, 27, 30, 2), c, deep=True)
    for y in (6, 13, 20):
        a.flat(a.R(8, y, 13, y + 5), STEEL)
        a.flat(a.R(15, y + 1, 24, y + 3), STEEL)
        a.flat(a.L([(9, y + 2), (10.5, y + 4), (14, y - 1)], 1.3), tick)


@glyph
def shop_stall(a, c=PINK):
    a.fill(a.R(4, 14, 28, 31), WOOD, deep=True)
    a.fill(a.R(8, 18, 24, 26), SHADOW, shade=False)
    a.fill(a.P((5, 3), (27, 3), (31, 14), (1, 14)), c, deep=True)
    for x in (5, 13, 21):
        a.flat(a.P((x + 2, 3), (x + 5, 3), (x + 6.5, 14), (x + 1, 14)), WHITE)
    a.fill(a.E(16, 22, 2.8), GOLD, shade=False)


@glyph
def stop_sign(a, c=RED):
    a.fill(a.P((10, 1), (22, 1), (31, 10), (31, 22), (22, 31), (10, 31), (1, 22), (1, 10)), c, deep=True)
    a.flat(a.P((11, 4), (21, 4), (28, 11), (28, 21), (21, 28), (11, 28), (4, 21), (4, 11)) & ~a.P((12, 6), (20, 6), (26, 12), (26, 20), (20, 26), (12, 26), (6, 20), (6, 12)), WHITE[0])
    a.fill(a.R(11, 11, 21, 23, 1.5) | a.R(11, 8, 13.3, 14) | a.R(14, 7, 16.3, 14) | a.R(17, 7, 19.3, 14) | a.R(19.2, 9, 21, 14), WHITE, shade=False)


@glyph
def chalkboard(a, c=FOREST):
    a.fill(a.R(1, 3, 31, 25, 1), WOOD, shade=False)
    a.fill(a.R(3, 5, 29, 23), c, shade=False)
    a.fill(a.R(8, 25, 11, 31) | a.R(21, 25, 24, 31), WOOD, shade=False)
    a.flat(a.L([(6, 18), (9, 9), (12, 18)], 1.4) | a.L([(7, 15), (11, 15)], 1.2) | a.R(15, 13, 20, 14.2) | a.R(17, 11, 18.2, 16.2), WHITE[0])
    a.flat(a.L([(22, 18), (26, 17)], 1.2), PINK)


@glyph
def miner_helmet(a, c=YELLOW):
    a.fill(a.C((3, 24), (4, 13), (10, 6), (16, 5), (22, 6), (28, 13), (29, 24)), c, deep=True)
    a.fill(a.R(1, 22, 31, 27, 2), c)
    a.fill(a.R(11, 7, 21, 16, 2), STEEL)
    a.flat(a.E(16, 11.5, 3), WHITE[0])
    a.flat(a.P((13, 3), (19, 3), (16, 7)), YELLOW[0])


@glyph
def grid_blocks(a, n=3, c=STONE, gap=1):
    cell = (28 - gap * (n - 1)) / n
    for j in range(n):
        for i in range(n):
            x, y = 2 + i * (cell + gap), 2 + j * (cell + gap)
            a.fill(a.R(x, y, x + cell, y + cell), c if (i + j) % 2 else (IRON if n == 3 else STONE))


@glyph
def stone_ball(a, c=STONE):
    m = a.E(16, 16, 14.5)
    a.fill(m, c, deep=True)
    for x, y, col in ((10, 10, DIAMOND), (20, 8, GOLD), (23, 18, EMERALD), (12, 21, RED), (17, 15, DIAMOND)):
        a.flat(m & a.R(x - 2, y - 2, x + 2, y + 2), col)


@glyph
def pit(a, c=STONE):
    a.fill(a.R(1, 1, 31, 31, 2), c, deep=True)
    a.fill(a.R(1, 1, 31, 6, 2), GRASS, shade=False)
    a.fill(a.P((5, 6), (27, 6), (27, 12), (23, 12), (23, 18), (19, 18), (19, 26), (13, 26), (13, 18), (9, 18), (9, 12), (5, 12)), SHADOW, shade=False)


@glyph
def wheat(a, c=GOLD):
    a.fill(a.B([(14, 31), (15, 18), (17, 5)], 2.4), GRASS, shade=False)
    for y in (6, 11, 16, 21):
        a.fill(a.E(12, y + 1, 3.6, 2.4) | a.E(21, y, 3.6, 2.4), c)
    a.fill(a.E(17, 3, 2.6, 3.4), c)


@glyph
def letter_block(a, ch='A', c=STONE, ink=DARK):
    a.fill(a.R(2, 2, 30, 30, 3), c, deep=True)
    a.fill(font_mask(a, ch, 9, 6, 21), ink, shade=False)


@glyph
def puppet(a, c=GREEN):
    a.fill(a.R(2, 1, 30, 5.5, 1.5), WOOD)
    zombie(a, 6, 12, .62, c=c)
    a.flat(a.R(8, 5.5, 9, 17) | a.R(23, 5.5, 24, 17) | a.R(15.5, 5.5, 16.5, 15), WHITE[0])


@glyph
def path_tile(a):
    a.fill(a.R(1, 1, 31, 31, 3), GRASS, deep=True)
    m = a.P((11, 1), (22, 1), (25, 31), (8, 31))
    a.flat(a.R(1, 1, 31, 31, 3) & m, SAND)
    for x, y in ((14, 6), (18, 10), (13, 15), (19, 19), (12, 24), (18, 26)):
        a.flat(a.E(x, y, 1.5), STONE)
    for x, y in ((5, 8), (27, 13), (5, 21), (28, 25)):
        a.dot(x, y, GRASS[0])


@glyph
def gill_head(a, c=TEAL):
    a.fill(a.R(4, 4, 25, 28, 7), c, deep=True)
    a.flat(a.R(16, 11, 19, 15), WHITE[0])
    a.flat(a.R(17, 12, 19, 15), OUT)
    for x in (7, 10.5, 14):
        a.flat(a.A(x - 8, 19, 8, -28, 28, 1.5), c[2])
    a.flat(a.L([(17, 22), (22, 21)], 1.4), c[2])


@glyph
def thorn_ring(a, c=FOREST):
    for i in range(10):
        t = i * math.tau / 10
        a.fill(a.P((16 + 12 * math.cos(t - .28), 16 + 12 * math.sin(t - .28)), (16 + 15.8 * math.cos(t), 16 + 15.8 * math.sin(t)), (16 + 12 * math.cos(t + .28), 16 + 12 * math.sin(t + .28))), BONE, shade=False)
    a.fill(a.E(16, 16, 13) & ~a.E(16, 16, 8.5), c, shade=False)
    a.flat(a.A(16, 16, 11, 170, 300, 1.4), c[0])


@glyph
def ring_fists(a, c=LIME):
    for i in range(6):
        t = i * math.tau / 6 - math.pi / 2
        fist(a, 16 + 11 * math.cos(t) - 7, 16 + 11 * math.sin(t) - 7, .44, c=c, wrist=False)


@glyph
def hand_rise(a, c=FOREST):
    a.fill(a.E(16, 27, 15, 4.5), DIRT, deep=True)
    hand(a, 2, -2, .84, c=c)


@glyph
def eye_half(a, c=VIOLET):
    eye(a, c=c, sticker=False)
    a.flat(a.C((1, 15), (1, 15), (9, 6), (16, 5), (23, 6), (31, 15), (31, 15), (16, 13)), DARK)


@glyph
def book_page(a, n='2', c=PURPLE):
    page(a, lines=False, sticker=False)
    a.fill(font_mask(a, n, 10, 9, 15), c, shade=False)


@glyph
def empty_slot(a, c=STEEL):
    m = a.E(16, 16, 11) & ~a.E(16, 16, 8)
    a.flat(m & ~(a.R(13, 0, 19, 32) | a.R(0, 13, 32, 19)), c)
