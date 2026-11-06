"""Second wave of gate creatures: models + pictures for the new themes (lycans, demons, statues, undead, beasts, ice, sci-fi, surprises).
Run: python tools/make_gate_mobs2.py   (needs tools/make_gate_mobs.py next to it; writes into the same asset folders)
Few cubes each, so they are cheap to draw."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_gate_mobs as g  # noqa: E402
from make_gate_mobs import add, band, chain, cube, face_details, humanoid, paint, save, shade  # noqa: E402


def Sheet(seed, size=64):
    return g.Sheet(seed, 128)  # room for every little extra


def empty(sheet):
    parts = {}
    for n, piv in (('head', [0, 0, 0]), ('hat', [0, 0, 0]), ('body', [0, 0, 0]), ('right_arm', [-5, 2, 0]), ('left_arm', [5, 2, 0]), ('right_leg', [-2, 12, 0]), ('left_leg', [2, 12, 0])):
        parts[n] = {'pivot': piv, 'cubes': [], 'children': {}}
    return parts


def put(sheet, parts, part, size, frm, base, **kw):
    tex = cube(sheet, size, base, **kw)
    parts[part]['cubes'].append({'tex': list(tex), 'from': list(frm), 'size': list(size)})


def eyes(color, y=2, xs=(1,), w=2):
    """glowing eye pixels on the front face of a cube"""
    def f(face, fx, fy, fw, fh, p):
        if face == 'front':
            for x in xs:
                for i in range(w):
                    p(x + i, y, color, True)
                    p(fw - 1 - (x + i), y, color, True)
    return f


def nose_of(color):
    """a dark nose on the lower middle of the front face, so a beast reads as an animal and not a sheep"""
    def f(face, fx, fy, fw, fh, put_):
        if face == 'front':
            for x in (fw // 2 - 1, fw // 2):
                for y in (fh - 3, fh - 2):
                    put_(x, y, color)
    return f


def beast(sheet, parts, body_c, head_c, eye, legs=4, body=(10, 8, 16), y=10, headsize=(7, 6, 6), mouth=False):
    """four-legged body: a long body, a head in front, legs that are real arms/legs (front pair swings) + static back legs"""
    bw, bh, bd = body
    put(sheet, parts, 'body', body, [-bw / 2, y, -bd / 2], body_c, detail=band([bh - 2], shade(body_c, 0.75)))
    parts['head']['pivot'] = [0, y + 2, -bd / 2]
    put(sheet, parts, 'head', headsize, [-headsize[0] / 2, -3, -headsize[2]], head_c, detail=chain(eyes(eye, 2), nose_of(shade(head_c, 0.3))))
    for side, nm, px in ((-1, 'right_arm', -bw / 2 + 2), (1, 'left_arm', bw / 2 - 2)):
        parts[nm]['pivot'] = [px, y + bh - 2, -bd / 2 + 3]
        put(sheet, parts, nm, (4, 24 - (y + bh - 2), 4), [-2, 0, -2], shade(body_c, 0.85))
    for side, nm, px in ((-1, 'right_leg', -bw / 2 + 2), (1, 'left_leg', bw / 2 - 2)):
        parts[nm]['pivot'] = [px, y + bh - 2, bd / 2 - 3]
        put(sheet, parts, nm, (4, 24 - (y + bh - 2), 4), [-2, 0, -2], shade(body_c, 0.85))


# ---------------------------------------------------------------- wolves

def wolf(alpha=False, king=False):
    s = Sheet(21 + (1 if alpha else 0) + (2 if king else 0), 128 if king else 64)
    fur = (92, 88, 96) if not alpha else (156, 160, 172)
    if king:
        fur = (70, 68, 80)
    p = empty(s)
    beast(s, p, fur, shade(fur, 1.1), (255, 200, 40) if not king else (255, 70, 40), body=(8, 8, 14) if not king else (12, 11, 18), y=8 if not king else 5, headsize=(6, 6, 5) if not king else (8, 8, 7))
    add(s, p, 'head', 'snout', [0, 0, -5], (3, 3, 4), [-1.5, -1.5, -4] if not king else [-1.5, -1.5, -6], shade(fur, 1.2))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 2.2, -3, -1], (2, 3, 1), [-1, -3, -0.5], shade(fur, 0.7))
    add(s, p, 'body', 'tail', [0, 11 if not king else 8, 7 if not king else 9], (3, 3, 9), [-1.5, -1.5, 0], shade(fur, 0.7), rot=(0.6, 0, 0))
    if alpha or king:
        add(s, p, 'body', 'mane', [0, 8 if not king else 5, -4], (10 if not king else 14, 5, 6), [-5 if not king else -7, -2, -3], (214, 218, 228) if not king else (110, 100, 130))
    if king:
        add(s, p, 'head', 'crown', [0, -3, -2], (7, 2, 5), [-3.5, -2, -2.5], (232, 196, 80))
    save('wolf_king' if king else 'alpha_wolf' if alpha else 'dire_wolf', s, p, 2.0 if king else 1.5 if alpha else 1.2)


# ---------------------------------------------------------------- demons

def demon():
    s = Sheet(24)
    skin = (170, 40, 44)
    c = {'skin': skin, 'cloth': (60, 28, 34), 'legs': (50, 24, 28), 'arms': skin}
    p = humanoid(s, c, head_detail=eyes((255, 230, 60), 3, (1,)), body_detail=chain(band([0, 1], (30, 20, 24)), band([8], (200, 170, 70))), sleeve=band([0, 1], (30, 20, 24)))
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 3, -8, 0], (2, 5, 2), [-1, -5, -1], (40, 30, 30), rot=(0, 0, sx * 0.45))
    add(s, p, 'body', 'tail', [0, 11, 2], (1, 1, 9), [-0.5, -0.5, 0], shade(skin, 0.8), rot=(0.9, 0, 0))
    save('demon', s, p, 1.05)


def imp():
    s = Sheet(25)
    skin = (200, 70, 40)
    c = {'skin': skin, 'cloth': shade(skin, 0.8), 'legs': shade(skin, 0.7), 'arms': skin}
    p = humanoid(s, c, body=(6, 8, 4), arm=(2, 9, 2), leg=(2, 8, 2), head_detail=eyes((255, 240, 80), 3, (1,)), )
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 3, -8, 0], (1, 3, 1), [-0.5, -3, -0.5], (50, 30, 30), rot=(0, 0, sx * 0.4))
    for sx, nm in ((-1, 'wing_r'), (1, 'wing_l')):
        add(s, p, 'body', nm, [sx * 2, 1, 2.5], (1, 10, 8), [-0.5, 0, 0], (120, 30, 40), rot=(0, sx * -0.5, sx * 0.4), noise=4)
    add(s, p, 'body', 'tail', [0, 8, 2], (1, 1, 8), [-0.5, -0.5, 0], shade(skin, 0.8), rot=(0.8, 0, 0))
    save('imp', s, p, 0.8)


def demon_knight():
    s = Sheet(26)
    plate = (36, 34, 44)
    c = {'skin': (150, 40, 44), 'cloth': plate, 'legs': (30, 28, 36), 'arms': plate}
    p = humanoid(s, c, body=(10, 12, 5), arm=(5, 12, 5), leg=(4, 12, 4), head_detail=eyes((255, 70, 40), 3, (1,)), body_detail=chain(band([0, 1, 2], (150, 40, 44)), band([8], (200, 170, 70))), sleeve=band([0, 1], (150, 40, 44)))
    add(s, p, 'head', 'helm', [0, -4, 0], (9, 5, 9), [-4.5, -5, -4.5], plate, detail=band([3], (150, 40, 44)))
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 4.5, -7, 0], (2, 6, 2), [-1, -6, -1], (200, 190, 180), rot=(0, 0, sx * 0.7))
    for sx, nm in ((-1, 'pad_r'), (1, 'pad_l')):
        add(s, p, 'body', nm, [sx * 6.5, -1, 0], (7, 3, 7), [-3.5, -2, -3.5], (60, 56, 70))
    save('demon_knight', s, p, 1.2)


def demon_lord():
    s = Sheet(27, 128)
    plate = (30, 24, 36)
    red = (170, 30, 36)
    c = {'skin': red, 'cloth': plate, 'legs': (26, 20, 30), 'arms': plate}
    p = humanoid(s, c, body=(12, 13, 6), arm=(6, 14, 6), leg=(5, 12, 5), head_detail=eyes((255, 220, 60), 3, (1,)), body_detail=chain(band([0, 1, 2], red), band([9], (210, 170, 60))), sleeve=band([0, 1], red))
    add(s, p, 'head', 'crown', [0, -8, 0], (9, 3, 9), [-4.5, -3, -4.5], (210, 170, 60))
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 4.5, -7, 0], (2, 9, 2), [-1, -9, -1], (220, 210, 200), rot=(0, 0, sx * 0.6))
    for sx, nm in ((-1, 'wing_r'), (1, 'wing_l')):
        add(s, p, 'body', nm, [sx * 3, 1, 3], (1, 18, 12), [-0.5, 0, 0], (80, 14, 24), rot=(0, sx * -0.5, sx * 0.5), noise=4)
        add(s, p, 'body', 'pad' + nm[4:], [sx * 7.5, -1, 0], (8, 4, 8), [-4, -2, -4], (60, 50, 70))
    add(s, p, 'body', 'cape', [0, 0, 3.5], (12, 20, 1), [-6, 0, 0], (110, 14, 24), rot=(0.08, 0, 0))
    save('demon_lord', s, p, 1.55)


# ---------------------------------------------------------------- gargoyle (stone until it wakes)

def gargoyle():
    s = Sheet(30)
    stone = (128, 128, 132)

    def cracks(face, fx, fy, fw, fh, put_):
        for i in range(min(fw, fh) // 2):
            put_(fw // 2 + i % 2, 1 + i, shade(stone, 0.7))
    c = {'skin': stone, 'cloth': stone, 'legs': shade(stone, 0.95), 'arms': stone}
    p = humanoid(s, c, body=(9, 12, 5), arm=(4, 13, 4), leg=(4, 11, 4), head_detail=eyes((255, 120, 60), 3, (1,)), body_detail=chain(cracks, band([8], shade(stone, 0.8))))
    for sx, nm in ((-1, 'wing_r'), (1, 'wing_l')):
        add(s, p, 'body', nm, [sx * 2, 0, 2.5], (1, 12, 9), [-0.5, 0, 0], shade(stone, 0.85), rot=(0, sx * -0.5, sx * 0.5), noise=6)
        add(s, p, 'head', 'horn' + nm[4:], [sx * 3, -8, 0], (2, 4, 2), [-1, -4, -1], shade(stone, 0.7), rot=(0, 0, sx * 0.4))
    save('gargoyle', s, p, 1.1)


# ---------------------------------------------------------------- undead crypt

def skeleton_knight():
    s = Sheet(33)
    bone = (214, 210, 190)
    steel = (110, 112, 124)
    c = {'skin': bone, 'cloth': steel, 'legs': bone, 'arms': bone}
    p = humanoid(s, c, body=(8, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=eyes((80, 255, 200), 3, (1,)), body_detail=chain(band([0, 1, 2], (150, 150, 164)), band([8], (60, 50, 50))), legs_detail=band([6, 7], steel))
    add(s, p, 'head', 'helm', [0, -4, 0], (9, 5, 9), [-4.5, -5, -4.5], steel, detail=band([3], (70, 70, 80)))
    add(s, p, 'head', 'plume', [0, -9, 1], (2, 5, 6), [-1, -4, -3], (150, 30, 40), rot=(0.2, 0, 0))
    save('skeleton_knight', s, p, 1.05)


def wraith():
    s = Sheet(34)
    cloth = (40, 36, 70)
    p = empty(s)
    put(s, p, 'head', (8, 8, 8), [-4, -8, -4], (70, 66, 100), detail=eyes((120, 255, 255), 3, (1,)))
    p['head']['pivot'] = [0, 1, 0]
    put(s, p, 'body', (8, 12, 4), [-4, 0, -2], cloth, detail=band([10, 11], (24, 20, 44)))
    for sx, nm in (('-', 'right_arm'), ('+', 'left_arm')):
        p[nm]['pivot'] = [-5 if sx == '-' else 5, 2, 0]
        put(s, p, nm, (3, 12, 3), [-1.5, -2, -1.5], (60, 56, 90))
    add(s, p, 'body', 'wisp1', [0, 12, 0], (7, 6, 4), [-3.5, 0, -2], (30, 26, 58))
    add(s, p, 'body', 'wisp2', [0, 18, 0], (5, 6, 3), [-2.5, 0, -1.5], (22, 18, 44))
    add(s, p, 'head', 'hood', [0, 1, 0], (9, 4, 9), [-4.5, -9, -4.5], cloth)
    save('wraith', s, p, 1.1)


def ghoul():
    s = Sheet(35)
    skin = (116, 130, 106)
    c = {'skin': skin, 'cloth': (86, 92, 80), 'legs': (70, 74, 66), 'arms': skin}
    p = humanoid(s, c, body=(8, 11, 4), arm=(3, 15, 3), leg=(3, 12, 3), head_detail=eyes((240, 220, 80), 3, (1,)), body_detail=band([3, 6], shade(skin, 0.6)))
    add(s, p, 'head', 'jaw', [0, -1, -4], (6, 3, 3), [-3, 0, -2], shade(skin, 0.75))
    save('ghoul', s, p, 0.95)


def lich(boss=False):
    s = Sheet(36 if not boss else 37, 128 if boss else 64)
    bone = (206, 206, 196)
    robe = (50, 30, 80) if not boss else (30, 40, 70)
    trim = (110, 220, 255) if boss else (150, 90, 210)
    c = {'skin': bone, 'cloth': robe, 'legs': shade(robe, 0.8), 'arms': robe}
    p = humanoid(s, c, body=(8, 12, 4) if not boss else (10, 13, 5), arm=(3, 12, 3) if not boss else (4, 13, 4), leg=(3, 12, 3) if not boss else (4, 12, 4),
                 head_detail=eyes(trim, 3, (1,)), body_detail=chain(band([0, 1], trim), band([9], trim)), sleeve=band([10, 11], trim))
    add(s, p, 'body', 'skirt', [0, 12, 0], (10, 8, 6), [-5, 0, -3], shade(robe, 0.9), detail=band([7], trim))
    if boss:
        add(s, p, 'head', 'crown', [0, -8, 0], (9, 4, 9), [-4.5, -4, -4.5], (80, 100, 140))
        for i, x in enumerate((-4, -1, 2)):
            add(s, p, 'head', 'spike%d' % i, [x + 0.5, -12, 0], (1, 4, 1), [-0.5, -3, -0.5], (200, 240, 255))
        add(s, p, 'body', 'cape', [0, 0, 3], (10, 20, 1), [-5, 0, 0], (20, 26, 50), rot=(0.1, 0, 0))
    else:
        add(s, p, 'head', 'horn_r', [-3, -8, 0], (2, 5, 2), [-1, -5, -1], (80, 70, 90), rot=(0, 0, -0.35))
        add(s, p, 'head', 'horn_l', [3, -8, 0], (2, 5, 2), [-1, -5, -1], (80, 70, 90), rot=(0, 0, 0.35))
    save('lich_lord' if boss else 'lich', s, p, 1.5 if boss else 1.1)


# ---------------------------------------------------------------- beasts

def lizardman():
    s = Sheet(40)
    skin = (70, 130, 72)
    c = {'skin': skin, 'cloth': (86, 70, 50), 'legs': shade(skin, 0.8), 'arms': skin}
    p = humanoid(s, c, body=(7, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=eyes((255, 220, 40), 3, (1,)), body_detail=band([8], (60, 44, 30)))
    add(s, p, 'head', 'snout', [0, -3, -4], (5, 3, 4), [-2.5, -1, -4], shade(skin, 1.1))
    add(s, p, 'head', 'crest', [0, -8, 1], (1, 3, 5), [-0.5, -3, -2], (200, 120, 50))
    add(s, p, 'body', 'tail', [0, 11, 2], (3, 3, 10), [-1.5, -1.5, 0], shade(skin, 0.85), rot=(0.7, 0, 0))
    save('lizardman', s, p, 1.0)


def snake(boss=False):
    s = Sheet(41 if not boss else 42, 128 if boss else 64)
    skin = (80, 150, 120) if not boss else (100, 120, 200)
    belly = (200, 200, 150)
    p = empty(s)
    put(s, p, 'head', (8, 8, 8), [-4, -8, -4], skin, detail=eyes((255, 240, 60), 3, (1,)))
    p['head']['pivot'] = [0, 0, 0]
    put(s, p, 'body', (8, 12, 4), [-4, 0, -2], (skin if not boss else (60, 80, 150)), detail=band([0, 1], belly))
    for nm, px in (('right_arm', -5.5), ('left_arm', 5.5)):
        p[nm]['pivot'] = [px, 2, 0]
        put(s, p, nm, (3, 12, 3), [-1.5, -2, -1.5], skin)
    # the coiled tail: four stacked rings getting smaller
    for i, (w, h) in enumerate(((10, 6), (8, 6), (6, 5), (4, 5))):
        add(s, p, 'body', 'coil%d' % i, [0, 12 + i * 3, 0], (w, h, w), [-w / 2, 0, -w / 2], shade(skin, 0.9 - 0.05 * i), detail=band([2], belly))
    if boss:
        add(s, p, 'head', 'crown', [0, -8, 0], (9, 3, 9), [-4.5, -3, -4.5], (232, 196, 80))
        for sx, nm in ((-1, 'fin_r'), (1, 'fin_l')):
            add(s, p, 'head', nm, [sx * 4, -4, 0], (1, 7, 5), [-0.5, -3, -2], (60, 200, 220), rot=(0, 0, sx * 0.4))
        add(s, p, 'body', 'hood', [0, -1, 0], (14, 3, 6), [-7, -1, -3], (60, 200, 220))
    save('snake_queen' if boss else 'snake_warrior', s, p, 1.5 if boss else 1.05)


def spider():
    s = Sheet(43)
    fur = (60, 44, 36)
    p = empty(s)
    put(s, p, 'body', (10, 7, 12), [-5, 12, -4], fur, detail=band([3], (120, 40, 30)))
    p['head']['pivot'] = [0, 15, -5]
    put(s, p, 'head', (6, 5, 5), [-3, -2.5, -5], shade(fur, 1.2), detail=eyes((255, 50, 40), 1, (0, 3), 1))
    for z, yaw in ((-3, 0.5), (-0.5, 0.15), (2, -0.15), (4.5, -0.5)):
        add(s, p, 'body', 'legr%s' % z, [-4, 16, z], (9, 2, 2), [-9, -1, -1], fur, rot=(0, -yaw, -0.8))
        add(s, p, 'body', 'legl%s' % z, [4, 16, z], (9, 2, 2), [0, -1, -1], fur, rot=(0, yaw, 0.8))
    add(s, p, 'head', 'fang_r', [-1.5, 2, -5], (1, 3, 1), [-0.5, 0, -0.5], (230, 230, 220))
    add(s, p, 'head', 'fang_l', [1.5, 2, -5], (1, 3, 1), [-0.5, 0, -0.5], (230, 230, 220))
    save('giant_spider', s, p, 1.0)


# ---------------------------------------------------------------- ice

def ice_bear(boss=False):
    s = Sheet(44 if not boss else 45, 128 if boss else 64)
    p = empty(s)
    if not boss:
        beast(s, p, (196, 212, 232), (214, 226, 242), (40, 90, 170), body=(12, 10, 16), y=6, headsize=(8, 7, 7))
        add(s, p, 'head', 'ear_r', [-3, -3, -2], (2, 2, 1), [-1, -1, -0.5], (200, 210, 224))
        add(s, p, 'head', 'ear_l', [3, -3, -2], (2, 2, 1), [-1, -1, -0.5], (200, 210, 224))
        save('ice_bear', s, p, 1.1)


def frost_elf():
    s = Sheet(46)
    skin = (214, 230, 245)
    c = {'skin': skin, 'cloth': (70, 110, 170), 'legs': (50, 80, 130), 'arms': (70, 110, 170)}
    p = humanoid(s, c, body=(7, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=eyes((120, 220, 255), 3, (1,), 1) if False else face_details(skin, (80, 190, 255), mouth=(150, 170, 200), hair=(240, 250, 255)),
                 body_detail=chain(band([0], (230, 240, 250)), band([8], (100, 130, 180))), sleeve=band([10, 11], (230, 240, 250)))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4, -4.5, 0], (6, 2, 1), [0 if sx > 0 else -6, -1, -0.5], skin, rot=(0, 0, sx * 0.35), noise=5)
    add(s, p, 'body', 'cloak', [0, 0, 2], (8, 14, 1), [-4, 0, 0], (200, 225, 245), rot=(0.12, 0, 0))
    save('ice_archer', s, p, 1.0)


def frost_king():
    s = Sheet(47, 128)
    ice = (160, 210, 250)
    deep = (50, 90, 170)
    c = {'skin': (220, 238, 250), 'cloth': deep, 'legs': shade(deep, 0.8), 'arms': deep}
    p = humanoid(s, c, body=(11, 13, 5), arm=(5, 14, 5), leg=(5, 12, 5), head_detail=eyes((120, 240, 255), 3, (1,)), body_detail=chain(band([0, 1, 2], ice), band([9], ice)), sleeve=band([0, 1], ice))
    for i, x in enumerate((-5, -2.5, 0, 2.5, 5)):
        add(s, p, 'head', 'ice%d' % i, [x, -8, 0], (2, 5 + (i % 2) * 3, 2), [-1, -8, -1], (190, 230, 255), rot=(0, 0, x * 0.06))
    for sx, nm in ((-1, 'pad_r'), (1, 'pad_l')):
        add(s, p, 'body', nm, [sx * 7, -1, 0], (8, 5, 8), [-4, -3, -4], ice)
        add(s, p, 'body', 'spike' + nm[3:], [sx * 7, -3, 0], (3, 6, 3), [-1.5, -6, -1.5], (220, 245, 255))
    add(s, p, 'body', 'cape', [0, 0, 3], (11, 20, 1), [-5.5, 0, 0], (200, 230, 250), rot=(0.1, 0, 0))
    save('frost_king', s, p, 1.55)


# ---------------------------------------------------------------- sci-fi

def sentry_drone():
    s = Sheet(50)
    hull = (150, 160, 176)
    p = empty(s)
    put(s, p, 'body', (8, 6, 8), [-4, 4, -4], hull, detail=band([2], (60, 70, 90)))
    p['head']['pivot'] = [0, 7, -4]
    put(s, p, 'head', (4, 4, 3), [-2, -2, -3], (70, 80, 100), detail=eyes((255, 60, 60), 1, (1,), 2))
    for sx, nm in (('-', 'right_arm'), ('+', 'left_arm')):
        p[nm]['pivot'] = [-6 if sx == '-' else 6, 6, 0]
        put(s, p, nm, (2, 2, 6), [-1, -1, -5], (80, 90, 110))
    add(s, p, 'body', 'rotor_r', [-5, 3, 2], (5, 1, 5), [-2.5, 0, -2.5], (210, 214, 224))
    add(s, p, 'body', 'rotor_l', [5, 3, 2], (5, 1, 5), [-2.5, 0, -2.5], (210, 214, 224))
    add(s, p, 'body', 'light', [0, 3, 0], (2, 2, 2), [-1, -1, -1], (80, 200, 255))
    save('sentry_drone', s, p, 0.9)


def cyber_soldier():
    s = Sheet(51)
    suit = (52, 62, 84)
    c = {'skin': (60, 70, 92), 'cloth': suit, 'legs': (40, 48, 66), 'arms': suit}
    p = humanoid(s, c, body=(8, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=chain(lambda f, fx, fy, fw, fh, put_: [put_(x, 3, (80, 220, 255), True) for x in range(1, 7)] + [put_(x, 4, (80, 220, 255), True) for x in range(2, 6)] if f == 'front' else None),
                 body_detail=chain(band([0, 1], (120, 130, 150)), band([4], (80, 220, 255))), sleeve=band([0, 1], (120, 130, 150)))
    add(s, p, 'head', 'visor', [0, -4, 0], (9, 5, 9), [-4.5, -5, -4.5], (70, 80, 100), detail=lambda f, fx, fy, fw, fh, put_: [put_(x, 2, (80, 220, 255), True) for x in range(1, 8)] if f == 'front' else None)
    add(s, p, 'body', 'pack', [0, 1, 2], (6, 8, 3), [-3, 0, 0], (90, 100, 120), detail=band([2], (80, 220, 255)))
    save('cyber_soldier', s, p, 1.0)


def bulwark_mech(boss=False):
    s = Sheet(52 if not boss else 53, 128 if boss else 64)
    hull = (120, 128, 144) if not boss else (140, 90, 90)
    c = {'skin': (70, 76, 90), 'cloth': hull, 'legs': shade(hull, 0.7), 'arms': shade(hull, 0.85)}
    p = humanoid(s, c, body=(14, 13, 8), arm=(5, 14, 5), leg=(6, 12, 6), head=(7, 5, 6), head_detail=lambda f, fx, fy, fw, fh, put_: [put_(x, 2, (255, 70, 60), True) for x in range(1, fw - 1)] if f == 'front' else None,
                 body_detail=chain(band([0, 1, 2], (60, 64, 76)), band([9], (255, 150, 40))), sleeve=band([0, 1, 2], (60, 64, 76)))
    add(s, p, 'body', 'shield', [0, 6, -5], (14, 14, 2), [-7, -7, -1], shade(hull, 1.15), detail=band([1, 12], (255, 150, 40)))
    add(s, p, 'body', 'vent_r', [-4, 0, 4], (3, 3, 3), [-1.5, -3, -1.5], (50, 54, 64))
    add(s, p, 'body', 'vent_l', [4, 0, 4], (3, 3, 3), [-1.5, -3, -1.5], (50, 54, 64))
    if boss:
        add(s, p, 'body', 'core', [0, 5, 4.5], (6, 6, 2), [-3, -3, 0], (255, 120, 60), detail=eyes((255, 200, 120), 2, (1,), 3))
        for sx, nm in ((-1, 'gun_r'), (1, 'gun_l')):
            add(s, p, 'body', nm, [sx * 9, -4, 0], (3, 3, 8), [-1.5, -1.5, -7], (60, 64, 76))
    save('war_mech' if boss else 'bulwark_mech', s, p, 1.8 if boss else 1.35)


def xeno_crawler():
    s = Sheet(54)
    ch = (30, 34, 40)
    sheen = (60, 160, 120)
    p = empty(s)
    put(s, p, 'body', (6, 6, 14), [-3, 10, -6], ch, detail=band([2, 5], sheen))
    p['head']['pivot'] = [0, 13, -7]
    put(s, p, 'head', (5, 5, 9), [-2.5, -2.5, -9], shade(ch, 1.1), detail=eyes((180, 255, 120), 1, (0,), 1))
    for z in (-3, 0, 3):
        add(s, p, 'body', 'lr%s' % z, [-3, 13, z], (8, 2, 2), [-8, -1, -1], ch, rot=(0, 0, -0.9))
        add(s, p, 'body', 'll%s' % z, [3, 13, z], (8, 2, 2), [0, -1, -1], ch, rot=(0, 0, 0.9))
    add(s, p, 'body', 'tail', [0, 12, 8], (2, 2, 10), [-1, -1, 0], ch, rot=(-0.5, 0, 0))
    add(s, p, 'head', 'tooth', [0, 1, -9], (3, 2, 1), [-1.5, 0, 0], (220, 230, 220))
    save('alien_crawler', s, p, 1.0)


def rift_alien():
    s = Sheet(55)
    skin = (60, 40, 90)
    glow = (200, 120, 255)
    c = {'skin': skin, 'cloth': (40, 26, 64), 'legs': (36, 24, 56), 'arms': skin}
    p = humanoid(s, c, head=(9, 10, 9), body=(6, 12, 3), arm=(2, 14, 2), leg=(3, 13, 3), head_detail=lambda f, fx, fy, fw, fh, put_: [put_(x, y, glow, True) for x in (1, 2, 6, 7) for y in (4, 5)] if f == 'front' else None,
                 body_detail=band([3, 6, 9], glow))
    p['head']['cubes'][0]['from'] = [-4.5, -10, -4.5]
    add(s, p, 'head', 'ridge', [0, -10, 1], (3, 3, 8), [-1.5, -3, -4], shade(skin, 1.2))
    save('alien', s, p, 1.05)


# ---------------------------------------------------------------- surprises

def mimic():
    s = Sheet(60)
    wood = (140, 96, 50)
    p = empty(s)
    put(s, p, 'body', (14, 8, 12), [-7, 16, -6], wood, detail=band([2], (210, 170, 60)))
    p['head']['pivot'] = [0, 16, -6]
    put(s, p, 'head', (14, 5, 12), [-7, -5, 0], shade(wood, 1.1), detail=band([2], (210, 170, 60)))
    add(s, p, 'head', 'lock', [0, -2, 0], (2, 3, 1), [-1, 0, 0], (230, 220, 160))
    for i in range(4):
        add(s, p, 'body', 'tooth%d' % i, [-5 + i * 3.3, 16, -6], (2, 3, 1), [-1, -3, 0], (240, 240, 230))
    add(s, p, 'body', 'tongue', [0, 18, -3], (4, 1, 6), [-2, 0, -5], (200, 60, 90))
    p['right_leg']['pivot'] = [-4, 24, 0]
    p['left_leg']['pivot'] = [4, 24, 0]
    put(s, p, 'right_leg', (3, 3, 3), [-1.5, -3, -1.5], (100, 70, 40))
    put(s, p, 'left_leg', (3, 3, 3), [-1.5, -3, -1.5], (100, 70, 40))
    g_ = g
    save('mimic', s, p, 1.0)


def slime(king=False):
    s = Sheet(61 if not king else 62, 64 if not king else 128)
    jelly = (90, 200, 90)
    p = empty(s)
    put(s, p, 'body', (12, 9, 12), [-6, 15, -6], jelly, noise=20)
    p['head']['pivot'] = [0, 20, -5]
    put(s, p, 'head', (8, 4, 2), [-4, -2, -2], shade(jelly, 1.1), detail=eyes((20, 60, 20), 1, (1,), 1))
    add(s, p, 'body', 'core', [0, 20, 0], (5, 5, 5), [-2.5, -2.5, -2.5], (60, 150, 60))
    if king:
        add(s, p, 'body', 'crown', [0, 15, 0], (10, 3, 10), [-5, -3, -5], (232, 196, 80))
        for i, x in enumerate((-4, 0, 4)):
            add(s, p, 'body', 'cs%d' % i, [x, 12, 0], (2, 3, 2), [-1, -3, -1], (232, 196, 80))
    else:
        add(s, p, 'body', 'helm', [0, 15, 0], (10, 4, 10), [-5, -4, -5], (150, 156, 170), detail=band([1], (80, 84, 96)))
    save('slime_king' if king else 'slime_knight', s, p, 1.5 if king else 1.0)


def mushroom_man():
    s = Sheet(63)
    stem = (226, 216, 190)
    cap = (200, 50, 50)

    def spots(face, fx, fy, fw, fh, put_):
        for (x, y) in ((1, 1), (4, 2), (6, 1), (2, 5), (5, 5)):
            put_(x, y, (250, 240, 230))
    c = {'skin': stem, 'cloth': stem, 'legs': shade(stem, 0.85), 'arms': shade(stem, 0.9)}
    p = humanoid(s, c, body=(7, 11, 5), arm=(3, 10, 3), leg=(3, 10, 3), head=(7, 6, 7), head_detail=eyes((40, 40, 40), 2, (1,), 1))
    add(s, p, 'head', 'cap', [0, -6, 0], (13, 4, 13), [-6.5, -4, -6.5], cap, detail=spots)
    add(s, p, 'head', 'cap2', [0, -10, 0], (9, 3, 9), [-4.5, -3, -4.5], shade(cap, 0.9), detail=spots)
    save('mushroom_man', s, p, 1.0)


def minotaur():
    s = Sheet(64)
    fur = (96, 62, 44)
    c = {'skin': fur, 'cloth': (120, 84, 60), 'legs': (70, 48, 36), 'arms': fur}
    p = humanoid(s, c, body=(12, 13, 6), arm=(6, 14, 6), leg=(5, 12, 5), head=(8, 9, 9), head_detail=eyes((255, 90, 40), 3, (1,)), body_detail=chain(band([0, 1], (60, 50, 50)), band([9], (60, 40, 30))), sleeve=band([0, 1], (60, 50, 50)))
    add(s, p, 'head', 'snout', [0, -2, -4], (5, 4, 4), [-2.5, -1, -4], shade(fur, 1.2), detail=lambda f, fx, fy, fw, fh, put_: [put_(1, 1, (20, 10, 10)), put_(3, 1, (20, 10, 10))] if f == 'front' else None)
    add(s, p, 'head', 'ring', [0, 0, -7], (2, 2, 1), [-1, 1, 0], (230, 200, 90))
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 4, -8, 0], (6, 2, 2), [0 if sx > 0 else -6, -1, -1], (230, 224, 200), rot=(0, 0, sx * -0.5))
    save('minotaur', s, p, 1.35)


# ---------------------------------------------------------------- desert tomb

def strips_of(wrap):
    return lambda f, fx, fy, fw, fh, put_: [put_(x, y, shade(wrap, 0.7)) for y in range(1, fh, 3) for x in range(fw)] if f not in ('top', 'bottom') else None


def mummy():
    s = Sheet(70)
    wrap = (206, 192, 150)
    c = {'skin': wrap, 'cloth': wrap, 'legs': shade(wrap, 0.9), 'arms': wrap}
    strips = strips_of(wrap)
    p = humanoid(s, c, head_detail=chain(strips, eyes((255, 90, 60), 3, (1,))), body_detail=strips, sleeve=strips, legs_detail=strips)
    add(s, p, 'right_arm', 'rag', [0, 10, 0], (1, 6, 3), [-0.5, 0, -1.5], shade(wrap, 0.8))
    save('mummy', s, p, 1.0)


def scarab():
    s = Sheet(71)
    shell = (40, 70, 110)
    p = empty(s)
    put(s, p, 'body', (8, 4, 10), [-4, 18, -5], shell, detail=band([1], (90, 190, 220)))
    p['head']['pivot'] = [0, 20, -5]
    put(s, p, 'head', (4, 3, 3), [-2, -1.5, -3], shade(shell, 1.3), detail=eyes((255, 220, 60), 1, (0,), 1))
    for z in (-3, 0, 3):
        add(s, p, 'body', 'lr%s' % z, [-4, 21, z], (4, 1, 1), [-4, -0.5, -0.5], (20, 30, 50), rot=(0, 0, -0.6))
        add(s, p, 'body', 'll%s' % z, [4, 21, z], (4, 1, 1), [0, -0.5, -0.5], (20, 30, 50), rot=(0, 0, 0.6))
    add(s, p, 'head', 'jaw_r', [-1, 1, -3], (1, 1, 2), [-0.5, 0, -2], (200, 200, 190))
    add(s, p, 'head', 'jaw_l', [1, 1, -3], (1, 1, 2), [-0.5, 0, -2], (200, 200, 190))
    save('scarab', s, p, 1.0)


def sand_golem():
    s = Sheet(72)
    sand = (206, 178, 112)
    c = {'skin': sand, 'cloth': sand, 'legs': shade(sand, 0.9), 'arms': sand}
    p = humanoid(s, c, body=(12, 12, 6), arm=(6, 14, 6), leg=(5, 11, 5), head_detail=eyes((255, 120, 40), 3, (1,)), body_detail=band([4, 8], shade(sand, 0.75)), sleeve=band([5], shade(sand, 0.75)))
    add(s, p, 'body', 'rubble', [0, 10, 0], (12, 3, 6), [-6, 0, -3], shade(sand, 0.8))
    add(s, p, 'head', 'brow', [0, -5, -4], (8, 2, 1), [-4, 0, -0.5], shade(sand, 0.7))
    save('sand_golem', s, p, 1.3)


def tomb_king():
    s = Sheet(73, 128)
    wrap = (210, 190, 140)
    gold = (232, 196, 80)
    c = {'skin': wrap, 'cloth': wrap, 'legs': shade(wrap, 0.85), 'arms': wrap}
    strips = strips_of(wrap)
    p = humanoid(s, c, body=(11, 13, 6), arm=(5, 14, 5), leg=(5, 12, 5), head_detail=chain(strips, eyes((80, 255, 200), 3, (1,))), body_detail=chain(strips, band([0, 1], gold), band([9], gold)), sleeve=chain(strips, band([0], gold)))
    add(s, p, 'head', 'mask', [0, -5, -4], (8, 8, 1), [-4, -3, -0.5], gold, detail=eyes((20, 60, 120), 3, (1,)))
    add(s, p, 'head', 'crown', [0, -8, 0], (9, 3, 9), [-4.5, -3, -4.5], (60, 100, 200))
    add(s, p, 'head', 'cobra', [0, -11, -3], (3, 5, 2), [-1.5, -5, 0], gold)
    add(s, p, 'body', 'collar', [0, -1, 0], (13, 3, 8), [-6.5, -1, -4], gold)
    add(s, p, 'body', 'cape', [0, 0, 3.5], (11, 20, 1), [-5.5, 0, 0], (40, 60, 140), rot=(0.08, 0, 0))
    save('tomb_king', s, p, 1.55)


# ---------------------------------------------------------------- dragon

def young_dragon():
    s = Sheet(74, 128)
    green = (60, 120, 70)
    belly = (210, 190, 120)
    p = empty(s)
    beast(s, p, green, shade(green, 1.15), (255, 200, 40), body=(12, 11, 18), y=5, headsize=(8, 7, 8))
    add(s, p, 'head', 'snout', [0, 1, -8], (5, 4, 5), [-2.5, -2, -5], shade(green, 1.25))
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 3, -3, 0], (2, 2, 7), [-1, -1, 0], (230, 224, 200), rot=(0.5, 0, 0))
    for sx, nm in ((-1, 'wing_r'), (1, 'wing_l')):
        add(s, p, 'body', nm, [sx * 6, 6, -2], (14, 1, 12), [0 if sx > 0 else -14, 0, 0], (80, 150, 90), rot=(0, 0, sx * -0.35), noise=5)
        add(s, p, 'body', 'bone' + nm[4:], [sx * 6, 6, -2], (14, 2, 2), [0 if sx > 0 else -14, -1, -1], (40, 80, 50), rot=(0, 0, sx * -0.35))
    for i, (w, z) in enumerate(((6, 8), (5, 14), (3, 19))):
        add(s, p, 'body', 'tail%d' % i, [0, 9, z], (w, w, 8), [-w / 2, -w / 2, 0], shade(green, 0.9 - 0.08 * i), detail=band([w // 2], belly))
    add(s, p, 'body', 'belly', [0, 9, 0], (10, 2, 16), [-5, 0, -8], belly)
    save('young_dragon', s, p, 2.3)


ALL = [wolf, lambda: wolf(alpha=True), lambda: wolf(king=True), demon, imp, demon_knight, demon_lord, gargoyle,
       skeleton_knight, wraith, ghoul, lich, lambda: lich(True), lizardman, snake, lambda: snake(True), spider,
       ice_bear, frost_elf, frost_king, sentry_drone, cyber_soldier, bulwark_mech, lambda: bulwark_mech(True), xeno_crawler, rift_alien,
       mimic, slime, lambda: slime(True), mushroom_man, minotaur, mummy, scarab, sand_golem, tomb_king, young_dragon]

if __name__ == '__main__':
    for fn in ALL:
        fn()
    print('made', len(ALL))
