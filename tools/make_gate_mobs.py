"""Makes the look of the gate mobs and bosses: a model json + a 64x64 texture + a glow texture each.

Run: python tools/make_gate_mobs.py   (rewrites assets/charpowers/gate_models/*.json and textures/entity/gate/*.png)
The model json is read by the game itself (game/client/GateModelLoader), so the cubes here and the picture can never disagree.
Each cube: "tex" = where its faces sit in the picture, "from"/"size" in model pixels, "mat" = what it's made of.
"""
import json
import os
import random
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'charpowers')
MODELS = os.path.join(ROOT, 'gate_models')
TEXTURES = os.path.join(ROOT, 'textures', 'entity', 'gate')

# The standard humanoid picture takes these boxes (x0, y0, x1, y1); the rest is free for extras.
USED = [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32), (16, 48, 32, 64), (32, 48, 48, 64)]


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


class Sheet:
    def __init__(self, seed, size=64):
        self.size = size
        self.img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.taken = []
        self.rng = random.Random(seed)

    def free(self, w, h, d):
        W, H = 2 * (d + w), d + h
        for y in range(0, self.size - H + 1):
            for x in range(0, self.size - W + 1):
                if all(x + W <= t[0] or x >= t[2] or y + H <= t[1] or y >= t[3] for t in self.taken):
                    self.taken.append((x, y, x + W, y + H))
                    return x, y
        raise SystemExit('picture is full')


def faces(u, v, w, h, d):
    """name -> (x, y, width, height) of each face of a box in the picture"""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def paint(sheet, tex, size, base, noise=14, detail=None, light=1.0):
    """Fill the six faces of a box with a colour (or a (top, bottom) pair for a gradient), a little noise and darker edges. detail(face, x, y, w, h, put) can change pixels."""
    w, h, d = size
    rng = sheet.rng
    for name, (fx, fy, fw, fh) in faces(tex[0], tex[1], w, h, d).items():
        def put(x, y, c, glow=False):
            if 0 <= x < fw and 0 <= y < fh:
                (sheet.glow if glow else sheet.img).putpixel((fx + x, fy + y), tuple(c) + (255,))
        for y in range(fh):
            for x in range(fw):
                c = base if isinstance(base[0], int) else mix(base[0], base[1], y / max(1, fh - 1))
                k = {'top': 1.15, 'bottom': 0.7, 'right': 0.9, 'left': 0.9, 'back': 0.85}.get(name, 1.0) * light
                k *= 1 + rng.uniform(-noise, noise) / 100
                if x in (0, fw - 1) or y in (0, fh - 1):
                    k *= 0.88
                put(x, y, shade(c, k))
        if detail:
            detail(name, fx, fy, fw, fh, put)


def band(rows, color, **kw):
    """detail: horizontal stripe(s) on the sides (a belt, a trim)"""
    def f(face, fx, fy, fw, fh, put):
        if face in ('top', 'bottom'):
            return
        for y in rows:
            for x in range(fw):
                put(x, y, color)
    return f


def chain(*fs):
    def f(*a):
        for g in fs:
            if g:
                g(*a)
    return f


def face_details(skin, eye, glow_eye=True, mouth=(40, 20, 20), brow=None, tusks=False, fangs=False, scar=False, slit=False, hair=None):
    """detail for a head: eyes, brows, mouth on the front; hair on top and back"""
    def f(face, fx, fy, fw, fh, put):
        if hair and face in ('top', 'back', 'left', 'right') and True:
            for y in range(fh if face in ('top', 'back') else 3):
                for x in range(fw):
                    put(x, y, shade(hair, 0.9 + ((x * 7 + y * 3) % 5) * 0.04))
        if hair and face == 'front':
            for x in range(fw):
                put(x, 0, hair)
                if x in (0, fw - 1):
                    put(x, 1, hair)
                    put(x, 2, hair)
        if face != 'front':
            return
        ey = 3
        for ex in (1, 5):
            if slit:
                put(ex, ey, eye, glow_eye)
                put(ex + 1, ey, eye, glow_eye)
            else:
                put(ex, ey, (240, 240, 240))
                put(ex + 1, ey, eye, glow_eye)
                put(ex, ey + 1, shade(skin, 0.8))
                put(ex + 1, ey + 1, shade(skin, 0.8))
        if brow:
            for ex in (1, 5):
                put(ex, ey - 1, brow)
                put(ex + 1, ey - 1, brow)
        for x in range(2, 6):
            put(x, 6, mouth)
        if fangs:
            put(2, 7, (240, 240, 230))
            put(5, 7, (240, 240, 230))
        if tusks:
            put(1, 6, (235, 230, 210))
            put(1, 7, (235, 230, 210))
            put(6, 6, (235, 230, 210))
            put(6, 7, (235, 230, 210))
        if scar:
            put(6, 2, (150, 50, 50))
            put(6, 3, (150, 50, 50))
            put(5, 4, (150, 50, 50))
    return f


def cube(sheet, size, base, tex=None, **kw):
    """allocate a spot, paint, and return the cube's json"""
    tex = tex or sheet.free(*size)
    paint(sheet, tex, size, base, **kw)
    return tex


# ---------------------------------------------------------------- a humanoid with options

def humanoid(sheet, c, head=(8, 8, 8), body=(8, 12, 4), arm=(4, 12, 4), leg=(4, 12, 4), skin_detail=None, head_detail=None, body_detail=None, legs_detail=None, sleeve=None):
    """c = colours: skin, cloth (body), legs, arms (default skin)"""
    parts = {}
    hw, hh, hd = head
    ht = sheet.free(*head)
    paint(sheet, ht, head, c['skin'], detail=head_detail)
    parts['head'] = {'pivot': [0, 0, 0], 'cubes': [{'tex': list(ht), 'from': [-hw / 2, -hh, -hd / 2], 'size': list(head)}], 'children': {}}
    parts['hat'] = {'pivot': [0, 0, 0], 'cubes': [], 'children': {}}
    bw, bh, bd = body
    bt = sheet.free(*body)
    paint(sheet, bt, body, c['cloth'], detail=body_detail)
    parts['body'] = {'pivot': [0, 0, 0], 'cubes': [{'tex': list(bt), 'from': [-bw / 2, 0, -bd / 2], 'size': list(body)}], 'children': {}}
    aw, ah, ad = arm
    for side, px in (('right_arm', -(bw / 2 + aw / 2)), ('left_arm', bw / 2 + aw / 2)):
        tex = sheet.free(*arm)
        paint(sheet, tex, arm, c.get('arms', c['skin']), detail=sleeve)
        parts[side] = {'pivot': [px, 2, 0], 'cubes': [{'tex': list(tex), 'from': [-aw / 2, -2, -ad / 2], 'size': list(arm)}], 'children': {}}
    lw, lh, ld = leg
    for side, px in (('right_leg', -lw / 2 - 0.1), ('left_leg', lw / 2 + 0.1)):
        tex = sheet.free(*leg)
        paint(sheet, tex, leg, c['legs'], detail=legs_detail)
        parts[side] = {'pivot': [px, 12, 0], 'cubes': [{'tex': list(tex), 'from': [-lw / 2, 0, -ld / 2], 'size': list(leg)}], 'children': {}}
    return parts


def add(sheet, parts, parent, name, pivot, size, frm, base, rot=(0, 0, 0), **kw):
    tex = cube(sheet, size, base, **kw)
    parts[parent]['children'][name] = {'pivot': list(pivot), 'rot': list(rot), 'cubes': [{'tex': list(tex), 'from': list(frm), 'size': list(size)}], 'children': {}}


def save(name, sheet, parts, scale):
    os.makedirs(MODELS, exist_ok=True)
    os.makedirs(TEXTURES, exist_ok=True)
    with open(os.path.join(MODELS, name + '.json'), 'w') as f:
        json.dump({'tex': [sheet.size, sheet.size], 'scale': scale, 'parts': parts}, f, indent=1)
    sheet.img.save(os.path.join(TEXTURES, name + '.png'))
    sheet.glow.save(os.path.join(TEXTURES, name + '_glow.png'))


# ---------------------------------------------------------------- the kinds

def goblin():
    s = Sheet(1)
    skin = (88, 142, 60)
    c = {'skin': skin, 'cloth': (112, 80, 50), 'legs': (84, 62, 44)}
    p = humanoid(s, c, head=(8, 8, 8), head_detail=face_details(skin, (250, 220, 60), fangs=True, brow=shade(skin, 0.6)),
                 body_detail=band([8, 9], (60, 42, 30)))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4, -4.5, 0], (5, 2, 1), [0 if sx > 0 else -5, -1, -0.5], skin, rot=(0, 0, sx * -0.5), noise=8)
    add(s, p, 'head', 'nose', [0, -3, -4], (2, 3, 2), [-1, -1, -1.5], shade(skin, 1.1))
    save('goblin', s, p, 0.68)


def goblin_shaman():
    s = Sheet(2)
    skin = (98, 152, 84)
    robe = (96, 54, 128)
    c = {'skin': skin, 'cloth': robe, 'legs': shade(robe, 0.8), 'arms': robe}
    p = humanoid(s, c, head_detail=face_details(skin, (120, 255, 190), brow=shade(skin, 0.6)),
                 body_detail=chain(band([0, 1], (214, 180, 70)), band([11], (214, 180, 70))), sleeve=band([10, 11], (214, 180, 70)))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4, -4.5, 0], (5, 2, 1), [0 if sx > 0 else -5, -1, -0.5], skin, rot=(0, 0, sx * -0.4))
    add(s, p, 'head', 'nose', [0, -3, -4], (2, 3, 2), [-1, -1, -1.5], shade(skin, 1.1))
    add(s, p, 'head', 'hood', [0, 0, 0], (9, 4, 9), [-4.5, -9, -4.5], robe)
    add(s, p, 'body', 'skirt', [0, 12, 0], (9, 6, 5), [-4.5, 0, -2.5], shade(robe, 0.9), detail=band([5], (214, 180, 70)))
    save('goblin_shaman', s, p, 0.72)


def hobgoblin():
    s = Sheet(3)
    skin = (176, 100, 52)
    c = {'skin': skin, 'cloth': (96, 84, 76), 'legs': (70, 58, 50), 'arms': skin}
    p = humanoid(s, c, body=(10, 12, 5), arm=(5, 12, 5), leg=(5, 12, 5), head_detail=face_details(skin, (255, 70, 40), tusks=True, brow=shade(skin, 0.5), scar=True, hair=(30, 22, 20)),
                 body_detail=chain(band([0, 1, 2], (120, 120, 128)), band([9, 10], (50, 38, 30))), sleeve=band([0, 1, 2], (120, 120, 128)))
    add(s, p, 'head', 'jaw', [0, -1, -4], (6, 2, 2), [-3, 0, -1.5], shade(skin, 0.9))
    save('hobgoblin', s, p, 1.12)


def orc():
    s = Sheet(4)
    skin = (98, 118, 80)
    iron = (126, 126, 136)
    c = {'skin': skin, 'cloth': (86, 64, 48), 'legs': (66, 60, 56), 'arms': skin}
    p = humanoid(s, c, body=(10, 12, 5), arm=(5, 12, 5), leg=(4, 12, 4), head_detail=face_details(skin, (255, 190, 40), tusks=True, brow=shade(skin, 0.55), hair=(24, 24, 28)),
                 body_detail=chain(band([0, 1, 2, 3], iron), band([8], (40, 30, 24))), sleeve=band([0, 1, 2], iron))
    for sx, nm in ((-1, 'pad_r'), (1, 'pad_l')):
        add(s, p, 'body', nm, [sx * 5.5, -1, 0], (6, 3, 7), [-3, -1, -3.5], iron, detail=band([1], shade(iron, 0.7)))
    save('orc', s, p, 1.12)


def elf():
    s = Sheet(5)
    skin = (238, 216, 192)
    c = {'skin': skin, 'cloth': (58, 112, 72), 'legs': (84, 66, 46), 'arms': (58, 112, 72)}
    p = humanoid(s, c, body=(7, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=face_details(skin, (60, 160, 90), mouth=(190, 110, 110), hair=(232, 220, 150)),
                 body_detail=chain(band([0], (210, 190, 110)), band([8], (110, 80, 50))), sleeve=band([10, 11], (84, 66, 46)))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4, -4.5, 0], (6, 2, 1), [0 if sx > 0 else -6, -1, -0.5], skin, rot=(0, 0, sx * 0.35), noise=5)
    add(s, p, 'body', 'cloak', [0, 0, 2], (8, 14, 1), [-4, 0, 0], (44, 86, 56), rot=(0.12, 0, 0))
    save('elf', s, p, 1.0)


def dark_elf():
    s = Sheet(6)
    skin = (86, 84, 112)
    leather = (38, 36, 52)
    c = {'skin': skin, 'cloth': leather, 'legs': (30, 28, 42), 'arms': leather}
    p = humanoid(s, c, body=(7, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=face_details(skin, (255, 60, 220), slit=True, mouth=(60, 40, 70), hair=(236, 236, 246)),
                 body_detail=chain(band([0, 1], (120, 70, 170)), band([8], (120, 70, 170))), sleeve=band([10, 11], (120, 70, 170)))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4, -4.5, 0], (6, 2, 1), [0 if sx > 0 else -6, -1, -0.5], skin, rot=(0, 0, sx * 0.3), noise=5)
    add(s, p, 'head', 'hood', [0, 0, 0], (9, 3, 9), [-4.5, -9, -4.5], leather, detail=band([2], (120, 70, 170)))
    add(s, p, 'body', 'cloak', [0, 0, 2], (8, 15, 1), [-4, 0, 0], (24, 22, 34), rot=(0.1, 0, 0))
    save('dark_elf', s, p, 1.0)


def ant_soldier():
    s = Sheet(7)
    ch = (64, 26, 24)
    hi = (116, 44, 38)
    c = {'skin': ch, 'cloth': (52, 22, 22), 'legs': ch, 'arms': ch}
    p = humanoid(s, c, body=(6, 10, 4), arm=(2, 12, 2), leg=(3, 12, 3), head_detail=face_details(ch, (255, 70, 40), slit=True, mouth=ch),
                 body_detail=chain(band([2, 5, 8], hi)), sleeve=band([4, 8], hi))
    add(s, p, 'head', 'mandible_r', [-1.5, -1, -4], (1, 1, 3), [-1, -0.5, -3], hi, rot=(0, 0.3, 0))
    add(s, p, 'head', 'mandible_l', [1.5, -1, -4], (1, 1, 3), [0, -0.5, -3], hi, rot=(0, -0.3, 0))
    add(s, p, 'head', 'antenna_r', [-2, -8, -2], (1, 5, 1), [-0.5, -5, -0.5], hi, rot=(-0.5, 0, -0.3))
    add(s, p, 'head', 'antenna_l', [2, -8, -2], (1, 5, 1), [-0.5, -5, -0.5], hi, rot=(-0.5, 0, 0.3))
    add(s, p, 'body', 'arm2_r', [-3, 6, 0], (1, 8, 1), [-0.5, 0, -0.5], ch, rot=(-0.3, 0, 0.5))
    add(s, p, 'body', 'arm2_l', [3, 6, 0], (1, 8, 1), [-0.5, 0, -0.5], ch, rot=(-0.3, 0, -0.5))
    add(s, p, 'body', 'abdomen', [0, 9, 3], (6, 6, 8), [-3, -2, 0], shade(ch, 0.9), detail=band([2, 4], hi), rot=(0.4, 0, 0))
    save('ant_soldier', s, p, 1.0)


def stone_golem():
    s = Sheet(8)
    stone = (124, 124, 128)

    def moss(face, fx, fy, fw, fh, put):
        for (x, y) in ((1, 1), (2, 1), (1, 2), (fw - 2, fh - 2), (fw - 3, fh - 2), (fw - 2, fh - 3)):
            put(x, y, (74, 112, 58))

    def cracks(face, fx, fy, fw, fh, put):
        for i in range(min(fw, fh) // 2):
            put(fw // 2 + i % 2, 2 + i, shade(stone, 0.55))
    c = {'skin': stone, 'cloth': stone, 'legs': shade(stone, 0.92), 'arms': stone}
    p = humanoid(s, c, head=(8, 8, 8), body=(12, 12, 6), arm=(6, 14, 6), leg=(5, 12, 5), head_detail=chain(face_details(stone, (255, 170, 50), slit=True, mouth=shade(stone, 0.5)), moss),
                 body_detail=chain(cracks, moss), sleeve=moss, legs_detail=moss)
    add(s, p, 'head', 'brow', [0, -5, -4], (8, 2, 1), [-4, 0, -0.5], shade(stone, 0.8))
    save('stone_golem', s, p, 1.35)


def goblin_king():
    s = Sheet(9, 128)
    skin = (80, 150, 56)
    gold = (232, 190, 60)
    c = {'skin': skin, 'cloth': (150, 40, 44), 'legs': (80, 40, 40), 'arms': skin}
    p = humanoid(s, c, body=(12, 12, 6), arm=(5, 12, 5), leg=(5, 12, 5), head=(9, 8, 8), head_detail=face_details(skin, (255, 230, 60), fangs=True, brow=shade(skin, 0.55)),
                 body_detail=chain(band([0, 1], gold), band([9], gold)), sleeve=band([0, 1], gold))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4.5, -4.5, 0], (6, 2, 1), [0 if sx > 0 else -6, -1, -0.5], skin, rot=(0, 0, sx * -0.35))
    add(s, p, 'head', 'nose', [0, -3, -4], (2, 3, 2), [-1, -1, -1.5], shade(skin, 1.1))
    add(s, p, 'head', 'crown', [0, -8, 0], (9, 3, 9), [-4.5, -3, -4.5], gold, detail=band([1], shade(gold, 0.75)))
    for i, x in enumerate((-4, -1, 2)):
        add(s, p, 'head', 'spike%d' % i, [x + 0.5, -11, 0], (2, 3, 2), [-1, -1, -1], gold)
    add(s, p, 'body', 'cape', [0, 0, 3], (12, 18, 1), [-6, 0, 0], (140, 28, 36), rot=(0.1, 0, 0), detail=band([0, 1], (240, 240, 235)))
    add(s, p, 'body', 'belly', [0, 5, -3], (10, 6, 2), [-5, 0, -1], shade(skin, 0.95))
    save('goblin_king', s, p, 1.45)


def hobgoblin_warlord():
    s = Sheet(10, 128)
    skin = (170, 92, 46)
    plate = (88, 82, 84)
    c = {'skin': skin, 'cloth': plate, 'legs': (62, 54, 52), 'arms': plate}
    p = humanoid(s, c, body=(12, 12, 6), arm=(6, 13, 6), leg=(5, 12, 5), head_detail=face_details(skin, (255, 60, 30), tusks=True, brow=shade(skin, 0.5), scar=True),
                 body_detail=chain(band([0, 1, 2], (130, 124, 128)), band([5], (160, 40, 30)), band([9, 10], (40, 30, 26))), sleeve=band([0, 1, 2, 3], (130, 124, 128)))
    add(s, p, 'head', 'helm', [0, -4, 0], (9, 5, 9), [-4.5, -5, -4.5], plate, detail=band([4], (130, 124, 128)))
    for sx, nm in ((-1, 'horn_r'), (1, 'horn_l')):
        add(s, p, 'head', nm, [sx * 4.5, -7, 0], (2, 6, 2), [-1, -6, -1], (230, 224, 200), rot=(0, 0, sx * 0.7))
    for sx, nm in ((-1, 'pad_r'), (1, 'pad_l')):
        add(s, p, 'body', nm, [sx * 7, -1, 0], (8, 4, 8), [-4, -2, -4], (110, 106, 110), detail=band([1], (50, 46, 50)))
        add(s, p, nm.replace('pad', 'spike'), [0, 0, 0], (2, 3, 2), [-1, -5, -1], (200, 196, 190)) if False else None
    add(s, p, 'body', 'spike_r', [-7, -3, 0], (2, 4, 2), [-1, -4, -1], (200, 196, 190))
    add(s, p, 'body', 'spike_l', [7, -3, 0], (2, 4, 2), [-1, -4, -1], (200, 196, 190))
    add(s, p, 'body', 'cape', [0, 0, 3], (12, 17, 1), [-6, 0, 0], (110, 24, 24), rot=(0.1, 0, 0))
    save('hobgoblin_warlord', s, p, 1.55)


def elf_mage():
    s = Sheet(11, 128)
    skin = (244, 226, 206)
    robe = (230, 230, 246)
    blue = (60, 110, 210)
    gold = (232, 196, 90)
    c = {'skin': skin, 'cloth': robe, 'legs': robe, 'arms': robe}
    p = humanoid(s, c, body=(7, 12, 4), arm=(3, 12, 3), leg=(3, 12, 3), head_detail=face_details(skin, (90, 190, 255), mouth=(190, 120, 120), hair=(250, 250, 255)),
                 body_detail=chain(band([0, 1], gold), band([5], blue), band([9], gold)), sleeve=chain(band([9, 10, 11], blue), band([8], gold)), legs_detail=band([10, 11], blue))
    for sx, nm in ((-1, 'ear_r'), (1, 'ear_l')):
        add(s, p, 'head', nm, [sx * 4, -4.5, 0], (7, 2, 1), [0 if sx > 0 else -7, -1, -0.5], skin, rot=(0, 0, sx * 0.4), noise=5)
    add(s, p, 'head', 'circlet', [0, -6, 0], (9, 1, 9), [-4.5, -1, -4.5], gold)
    add(s, p, 'head', 'gem', [0, -6.5, -4.6], (2, 2, 1), [-1, -1, -0.5], blue)
    s.glow.putpixel((0, 0), (0, 0, 0, 0))
    add(s, p, 'body', 'robe', [0, 12, 0], (9, 12, 5), [-4.5, 0, -2.5], robe, detail=chain(band([0], gold), band([8, 9], blue), band([11], gold)))
    add(s, p, 'body', 'mantle', [0, -1, 0], (11, 3, 6), [-5.5, -1, -3], blue, detail=band([2], gold))
    add(s, p, 'body', 'train', [0, 0, 2.5], (8, 20, 1), [-4, 0, 0], (240, 240, 252), rot=(0.08, 0, 0), detail=band([19], gold))
    save('elf_mage', s, p, 1.25)


def ant_queen():
    s = Sheet(12, 128)
    ch = (72, 20, 22)
    hi = (150, 54, 40)
    c = {'skin': ch, 'cloth': (60, 18, 20), 'legs': ch, 'arms': ch}
    p = humanoid(s, c, body=(8, 10, 5), arm=(3, 13, 3), leg=(4, 12, 4), head=(8, 8, 8), head_detail=face_details(ch, (255, 200, 40), slit=True, mouth=ch),
                 body_detail=band([2, 5, 8], hi), sleeve=band([4, 8], hi))
    add(s, p, 'head', 'mandible_r', [-2, -1, -4], (1, 1, 4), [-1, -0.5, -4], hi, rot=(0, 0.4, 0))
    add(s, p, 'head', 'mandible_l', [2, -1, -4], (1, 1, 4), [0, -0.5, -4], hi, rot=(0, -0.4, 0))
    add(s, p, 'head', 'antenna_r', [-2, -8, -2], (1, 7, 1), [-0.5, -7, -0.5], hi, rot=(-0.6, 0, -0.35))
    add(s, p, 'head', 'antenna_l', [2, -8, -2], (1, 7, 1), [-0.5, -7, -0.5], hi, rot=(-0.6, 0, 0.35))
    add(s, p, 'head', 'crest', [0, -8, 0], (6, 2, 6), [-3, -1, -3], (232, 186, 60))
    add(s, p, 'body', 'abdomen', [0, 8, 4], (12, 12, 16), [-6, -4, 0], shade(ch, 0.9), detail=chain(band([2, 5, 8, 11], hi)), rot=(0.5, 0, 0))
    for i, y in enumerate((2, 5)):
        add(s, p, 'body', 'legpair_r%d' % i, [-4, y, 0], (1, 11, 1), [-0.5, 0, -0.5], ch, rot=(0, 0, 0.7 + i * 0.2))
        add(s, p, 'body', 'legpair_l%d' % i, [4, y, 0], (1, 11, 1), [-0.5, 0, -0.5], ch, rot=(0, 0, -0.7 - i * 0.2))
    add(s, p, 'body', 'wing_r', [-2, 0, 2.5], (1, 12, 7), [-0.5, 0, 0], (200, 190, 150), rot=(0, 0.4, 0.5), noise=4)
    add(s, p, 'body', 'wing_l', [2, 0, 2.5], (1, 12, 7), [-0.5, 0, 0], (200, 190, 150), rot=(0, -0.4, -0.5), noise=4)
    save('ant_queen', s, p, 1.6)


ALL = [goblin, goblin_shaman, hobgoblin, orc, elf, dark_elf, ant_soldier, stone_golem, goblin_king, hobgoblin_warlord, elf_mage, ant_queen]

if __name__ == '__main__':
    for fn in ALL:
        fn()
    print('made', len(ALL))
