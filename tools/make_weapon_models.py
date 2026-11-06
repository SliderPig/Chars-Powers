# Char's seven new weapons: icons (32x32), real 3D models for the long ones, the sword-in-stone block, the trail textures, and what Punchy should treat each one as.
# Original designs, all drawn here. From the charpowers folder:  python tools/make_weapon_models.py
import json
import math
import os
import random

from PIL import Image, ImageDraw

A = 'src/main/resources/assets/charpowers/'
BIG = 512
INK = (16, 14, 22, 255)


def crisp(icon):
    r, g, b, a = icon.split()
    a = a.point(lambda v: 255 if v >= 96 else 0)
    solid = Image.merge('RGBA', (r, g, b, a))
    empty = Image.new('RGBA', solid.size, (0, 0, 0, 0))
    empty.paste(solid, mask=a)
    return empty


def save_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=1)
        f.write('\n')


def make_icon(draw, turn=-45):
    im = Image.new('RGBA', (BIG, BIG), (0, 0, 0, 0))
    draw(ImageDraw.Draw(im))
    if turn:
        im = im.rotate(turn, resample=Image.BICUBIC, center=(BIG // 2, BIG // 2))
    return crisp(im.resize((32, 32), Image.LANCZOS))


CX = BIG // 2


# ---- icons

def icon_sword(d):
    d.polygon([(CX, -30), (CX + 40, 40), (CX + 36, 362), (CX - 36, 362), (CX - 40, 40)], fill=INK)
    d.polygon([(CX, -10), (CX + 25, 46), (CX + 24, 350), (CX, 350)], fill=(255, 226, 120, 255))
    d.polygon([(CX, -10), (CX - 25, 46), (CX - 24, 350), (CX, 350)], fill=(214, 150, 40, 255))
    d.line((CX, 30, CX, 340), fill=(120, 74, 20, 255), width=8)
    d.polygon([(CX - 110, 340), (CX - 92, 372), (CX - 60, 366), (CX, 392), (CX + 60, 366), (CX + 92, 372), (CX + 110, 340), (CX + 86, 402), (CX - 86, 402)], fill=INK)
    d.polygon([(CX - 86, 358), (CX - 74, 382), (CX, 382), (CX + 74, 382), (CX + 86, 358), (CX + 66, 392), (CX - 66, 392)], fill=(255, 208, 70, 255))
    d.ellipse((CX - 17, 362, CX + 17, 396), fill=(214, 28, 50, 255))
    d.rounded_rectangle((CX - 19, 400, CX + 19, 494), 6, fill=INK)
    d.rectangle((CX - 10, 406, CX + 10, 488), fill=(120, 24, 34, 255))
    for y in range(414, 484, 18):
        d.line((CX - 10, y, CX + 10, y + 7), fill=(240, 196, 70, 255), width=5)
    d.polygon([(CX - 30, 490), (CX - 14, 508), (CX, 492), (CX + 14, 508), (CX + 30, 490), (CX + 26, 530), (CX - 26, 530)], fill=INK)
    d.polygon([(CX - 18, 502), (CX + 18, 502), (CX + 16, 522), (CX - 16, 522)], fill=(255, 208, 70, 255))


def icon_katana(d):
    pts_back, pts_edge = [], []
    for i in range(0, 21):
        t = i / 20
        y = -30 + t * 400
        bend = (1 - t) ** 2 * 70
        pts_back.append((CX + bend - 18, y + (1 - t) * 26))
        pts_edge.append((CX + bend + 20 * (0.25 + 0.75 * min(1, t * 4)), y))
    d.polygon(pts_back + pts_edge[::-1], fill=INK)
    inner_back = [(x + 9, y) for x, y in pts_back]
    inner_edge = [(x - 8, y + 6) for x, y in pts_edge]
    mid = [((a[0] + b[0]) / 2, a[1]) for a, b in zip(inner_back, inner_edge)]
    d.polygon(inner_back + mid[::-1], fill=(58, 40, 96, 255))
    d.polygon(mid + inner_edge[::-1], fill=(150, 90, 240, 255))
    d.line([(x, y) for x, y in inner_edge], fill=(238, 214, 255, 255), width=7)
    d.ellipse((CX - 44, 352, CX + 44, 396), fill=INK)
    d.ellipse((CX - 30, 362, CX + 30, 386), fill=(112, 60, 190, 255))
    d.rounded_rectangle((CX - 17, 388, CX + 17, 508), 6, fill=INK)
    d.rectangle((CX - 8, 396, CX + 8, 500), fill=(44, 28, 74, 255))
    for y in range(404, 496, 22):
        d.polygon([(CX - 8, y), (CX, y + 10), (CX + 8, y), (CX, y - 10)], fill=(176, 126, 255, 255))


def icon_hammer(d):
    d.rounded_rectangle((CX - 16, 130, CX + 16, 510), 6, fill=INK)
    d.rectangle((CX - 7, 140, CX + 7, 500), fill=(116, 66, 42, 255))
    for y in (220, 330, 440):
        d.rectangle((CX - 16, y, CX + 16, y + 20), fill=INK)
        d.rectangle((CX - 10, y + 5, CX + 10, y + 15), fill=(160, 170, 200, 255))
    d.polygon([(CX - 150, 8), (CX + 150, 8), (CX + 170, 40), (CX + 170, 160), (CX + 150, 196), (CX - 150, 196), (CX - 170, 160), (CX - 170, 40)], fill=INK)
    d.polygon([(CX - 138, 22), (CX + 138, 22), (CX + 154, 46), (CX + 154, 96), (CX - 154, 96), (CX - 154, 46)], fill=(120, 134, 176, 255))
    d.polygon([(CX - 154, 96), (CX + 154, 96), (CX + 154, 152), (CX + 138, 182), (CX - 138, 182), (CX - 154, 152)], fill=(54, 58, 92, 255))
    # flames breaking through the metal
    d.polygon([(CX - 90, 182), (CX - 62, 92), (CX - 38, 140), (CX - 12, 60), (CX + 22, 138), (CX + 48, 88), (CX + 88, 182)], fill=(255, 120, 26, 255))
    d.polygon([(CX - 50, 182), (CX - 28, 124), (CX - 6, 150), (CX + 16, 112), (CX + 48, 182)], fill=(255, 214, 90, 255))


def icon_staff(d):
    d.rounded_rectangle((CX - 13, 170, CX + 13, 510), 6, fill=INK)
    d.rectangle((CX - 5, 180, CX + 5, 500), fill=(190, 206, 226, 255))
    for y in (250, 360, 450):
        d.rectangle((CX - 13, y, CX + 13, y + 16), fill=(80, 130, 186, 255))
    # the crystal crown
    d.polygon([(CX, -12), (CX + 56, 90), (CX + 30, 210), (CX - 30, 210), (CX - 56, 90)], fill=INK)
    d.polygon([(CX, 12), (CX + 40, 92), (CX + 18, 196), (CX, 196)], fill=(214, 248, 255, 255))
    d.polygon([(CX, 12), (CX - 40, 92), (CX - 18, 196), (CX, 196)], fill=(112, 196, 240, 255))
    for side in (-1, 1):
        d.polygon([(CX + side * 96, 74), (CX + side * 110, 150), (CX + side * 50, 214), (CX + side * 34, 170)], fill=INK)
        d.polygon([(CX + side * 88, 98), (CX + side * 96, 148), (CX + side * 54, 194), (CX + side * 46, 166)], fill=(160, 226, 250, 255))
    d.polygon([(CX - 56, 196), (CX + 56, 196), (CX + 30, 244), (CX - 30, 244)], fill=INK)
    d.polygon([(CX - 36, 204), (CX + 36, 204), (CX + 20, 234), (CX - 20, 234)], fill=(214, 226, 240, 255))


def icon_dagger(d):
    d.polygon([(CX + 26, 18), (CX + 46, 94), (CX + 30, 150), (CX + 44, 214), (CX + 26, 300), (CX - 34, 300), (CX - 24, 214), (CX - 40, 150), (CX - 22, 86)], fill=INK)
    d.polygon([(CX + 22, 50), (CX + 30, 96), (CX + 16, 150), (CX + 28, 214), (CX + 14, 288), (CX, 288), (CX, 60)], fill=(82, 62, 124, 255))
    d.polygon([(CX + 22, 50), (CX, 60), (CX, 288), (CX - 22, 288), (CX - 12, 214), (CX - 26, 150), (CX - 10, 92)], fill=(26, 18, 40, 255))
    d.polygon([(CX - 96, 296), (CX + 96, 296), (CX + 58, 346), (CX - 58, 346)], fill=INK)
    d.polygon([(CX - 66, 306), (CX + 66, 306), (CX + 44, 334), (CX - 44, 334)], fill=(52, 40, 76, 255))
    d.ellipse((CX - 17, 304, CX + 17, 338), fill=(170, 80, 255, 255))
    d.rounded_rectangle((CX - 19, 344, CX + 19, 468), 6, fill=INK)
    d.rectangle((CX - 9, 352, CX + 9, 460), fill=(40, 30, 58, 255))
    for y in range(362, 456, 20):
        d.line((CX - 9, y, CX + 9, y + 8), fill=(110, 84, 160, 255), width=5)
    d.polygon([(CX - 30, 466), (CX + 30, 466), (CX, 512)], fill=INK)
    d.polygon([(CX - 14, 474), (CX + 14, 474), (CX, 496)], fill=(170, 80, 255, 255))


def icon_fork(d):
    for x in (-44, 0, 44):
        d.rounded_rectangle((CX + x - 13, 22, CX + x + 13, 190), 8, fill=INK)
    d.polygon([(CX - 57, 160), (CX + 57, 160), (CX + 57, 200), (CX + 30, 262), (CX - 30, 262), (CX - 57, 200)], fill=INK)
    for x in (-44, 0, 44):
        d.rounded_rectangle((CX + x - 5, 34, CX + x + 5, 196), 3, fill=(226, 232, 240, 255))
    d.polygon([(CX - 47, 176), (CX + 47, 176), (CX + 47, 198), (CX + 22, 250), (CX - 22, 250), (CX - 47, 198)], fill=(190, 198, 212, 255))
    d.rounded_rectangle((CX - 20, 250, CX + 20, 490), 14, fill=INK)
    d.rounded_rectangle((CX - 10, 258, CX + 10, 480), 8, fill=(170, 178, 194, 255))
    d.line((CX - 4, 268, CX - 4, 470), fill=(232, 236, 244, 255), width=6)


def bottle_icon(liquid):
    fills = {'empty': None, 'water': ((64, 150, 240, 255), (156, 208, 255, 255)), 'lava': ((232, 86, 20, 255), (255, 186, 56, 255)), 'milk': ((232, 232, 224, 255), (255, 255, 250, 255))}
    fill = fills[liquid]

    def draw(d):
        # bottle
        d.rounded_rectangle((CX - 96, 196, CX + 96, 496), 36, fill=INK)
        d.rounded_rectangle((CX - 80, 212, CX + 80, 480), 26, fill=(196, 226, 240, 120))
        if fill:
            d.rounded_rectangle((CX - 80, 280, CX + 80, 480), 26, fill=fill[0])
            d.rectangle((CX - 80, 280, CX + 80, 296), fill=fill[1])
            for x, y, r in ((-30, 340, 11), (26, 392, 8), (-8, 430, 6)):
                d.ellipse((CX + x - r, y - r, CX + x + r, y + r), fill=fill[1])
        d.rectangle((CX - 58, 230, CX - 44, 450), fill=(255, 255, 255, 170))
        # neck
        d.rectangle((CX - 42, 134, CX + 42, 210), fill=INK)
        d.rectangle((CX - 28, 146, CX + 28, 206), fill=(196, 226, 240, 150))
        # the trigger head, in pink because it's Char's
        d.polygon([(CX - 66, 74), (CX + 118, 66), (CX + 118, 114), (CX + 40, 114), (CX + 52, 148), (CX - 66, 148)], fill=INK)
        d.polygon([(CX - 54, 86), (CX + 106, 78), (CX + 106, 102), (CX + 28, 102), (CX + 40, 136), (CX - 54, 136)], fill=(236, 118, 170, 255))
        d.polygon([(CX - 12, 136), (CX + 40, 136), (CX + 30, 212), (CX + 2, 170)], fill=INK)
        d.polygon([(CX + 2, 144), (CX + 28, 144), (CX + 24, 188), (CX + 8, 166)], fill=(190, 80, 136, 255))
        d.rectangle((CX + 104, 76, CX + 126, 104), fill=INK)
    return make_icon(draw, 0)


# ---- 3D models: boxes on a small sheet of colours

class Sheet:
    SIZE = 64
    CELL = 8

    def __init__(self):
        self.im = Image.new('RGBA', (self.SIZE, self.SIZE), (0, 0, 0, 0))
        self.names = {}
        self.rng = random.Random(7)

    def cell(self, name, colour, shade=0.06, wrap=None):
        """A square of colour with a little grain, so faces aren't dead flat. wrap = a second colour in diagonal bands."""
        n = len(self.names)
        x0, y0 = (n % 8) * self.CELL, (n // 8) * self.CELL
        for y in range(self.CELL):
            for x in range(self.CELL):
                c = colour
                if wrap and (x + y) % 4 < 2:
                    c = wrap
                k = 1 + (self.rng.random() - 0.5) * shade * 2
                self.im.putpixel((x0 + x, y0 + y), tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,))
        self.names[name] = (x0 * 16 / self.SIZE + 0.1, y0 * 16 / self.SIZE + 0.1, (x0 + self.CELL) * 16 / self.SIZE - 0.1, (y0 + self.CELL) * 16 / self.SIZE - 0.1)

    def uv(self, name):
        return list(self.names[name])


class Model:
    def __init__(self, sheet):
        self.sheet = sheet
        self.elements = []

    def box(self, frm, to, cell, sides=None, rotation=None, glow=False):
        """sides = cell name for particular faces, e.g. {'up': 'x'}"""
        faces = {}
        for face in ('north', 'south', 'east', 'west', 'up', 'down'):
            name = (sides or {}).get(face, cell)
            faces[face] = {'uv': self.sheet.uv(name), 'texture': '#0'}
        el = {'from': [round(v, 3) for v in frm], 'to': [round(v, 3) for v in to], 'faces': faces}
        if rotation:
            el['rotation'] = rotation
        if glow:
            el['forge_data'] = {'block_light': 15, 'sky_light': 15, 'ambient_occlusion': False}
            el['shade'] = False
        self.elements.append(el)

    def centred(self, y0, y1, half_x, half_z, cell, ox=0.0, oz=0.0, **kw):
        self.box((8 + ox - half_x, y0, 8 + oz - half_z), (8 + ox + half_x, y1, 8 + oz + half_z), cell, **kw)


TILT = 30


def display(grip, third_scale, first_scale, ground=0.42, tilt=TILT):
    """Grip height in the model: the hand holds it there. Third person leans the blade 30 degrees along the arm; first person keeps the grip at the spot SwingPoses asks for."""
    t = math.radians(tilt)
    off = grip - 8
    third = [0, round(-2 - third_scale * off * math.cos(t), 3), round(-0.5 + third_scale * off * math.sin(t), 3)]
    first = [0, round(-first_scale * off, 3), 0]
    return {
        'thirdperson_righthand': {'rotation': [-tilt, -90, 0], 'translation': third, 'scale': [third_scale] * 3},
        'firstperson_righthand': {'rotation': [0, -30, 0], 'translation': first, 'scale': [first_scale] * 3},
        'ground': {'rotation': [0, 0, 45], 'translation': [0, 2, 0], 'scale': [ground] * 3},
        'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
    }


def write_3d(name, model, sheet, disp):
    sheet.im.save(A + f'textures/item/{name}_3d.png', optimize=True)
    save_json(A + f'models/item/{name}_3d.json', {'gui_light': 'front', 'textures': {'0': f'charpowers:item/{name}_3d', 'particle': f'charpowers:item/{name}_3d'}, 'elements': model.elements, 'display': disp})
    flat = {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'charpowers:item/{name}'}}
    save_json(A + f'models/item/{name}.json', {'loader': 'forge:separate_transforms', 'gui_light': 'front', 'base': {'parent': f'charpowers:item/{name}_3d'}, 'perspectives': {'gui': flat, 'fixed': flat}})


def long_sword():
    s = Sheet()
    for name, c in {'edge': (255, 240, 176), 'flat': (246, 202, 96), 'flat2': (224, 170, 66), 'fuller': (136, 84, 24), 'gold': (255, 206, 72), 'gold2': (214, 150, 38), 'gold3': (156, 98, 24),
                    'ruby': (226, 30, 56), 'wire': (240, 196, 70), 'pearl': (255, 246, 214)}.items():
        s.cell(name, c)
    s.cell('grip', (112, 22, 32), wrap=(236, 186, 64))
    m = Model(s)
    # blade: a broad centre with bright edges, tapering in steps to a point at y 32
    y = -1
    for top, half in ((22, 2.75), (26, 2.25), (29, 1.5), (31, 0.85), (32, 0.4)):
        edge = 0.7 if half > 1 else 0.3
        m.centred(y, top, half - edge, 0.4, 'flat' if top < 26 else 'edge')
        m.centred(y, top, 0.45, 0.55, 'fuller' if top <= 26 else 'edge')
        if half > 0.5:
            m.box((8 - half, y, 7.7), (8 - half + edge, top, 8.3), 'edge')
            m.box((8 + half - edge, y, 7.7), (8 + half, top, 8.3), 'edge')
        y = top
    m.centred(-1, 1, 3.1, 0.9, 'gold')
    # crown-shaped guard
    m.centred(-3, -1, 6.5, 1.25, 'gold', sides={'down': 'gold3'})
    for ox, tall in ((-6, 2.6), (-3.2, 1.2), (3.2, 1.2), (6, 2.6)):
        m.centred(-1, -1 + tall, 0.8, 0.9, 'gold2', ox=ox)
    m.centred(-3.5, -0.5, 1.2, 1.5, 'ruby', glow=True)
    # long two-handed grip and a crown for a pommel
    m.centred(-13, -3, 1.1, 1.1, 'grip')
    m.centred(-15, -13, 1.9, 1.9, 'gold', sides={'down': 'gold3'})
    for ox, oz in ((-1.2, -1.2), (1.2, -1.2), (-1.2, 1.2), (1.2, 1.2)):
        m.centred(-16, -15, 0.5, 0.5, 'gold2', ox=ox, oz=oz)
    write_3d('golden_kings_long_sword', m, s, display(-8, 0.74, 0.74))


def katana():
    s = Sheet()
    for name, c in {'edge': (246, 228, 255), 'face': (160, 92, 244), 'back': (54, 34, 92), 'dark': (26, 18, 44), 'tsuba': (84, 48, 148), 'tsuba2': (40, 26, 72), 'cap': (176, 126, 255)}.items():
        s.cell(name, c)
    s.cell('wrap', (40, 26, 72), wrap=(186, 136, 255))
    m = Model(s)
    # a long thin blade with a gentle curve: each piece is set a little further over
    y = -5
    step = 2.0
    while y < 32:
        top = min(32, y + step)
        t = (y + 5) / 37
        over = -t * t * 3.2
        width = 1.25 if top < 28 else (0.85 if top < 31 else 0.35)
        m.box((8 + over - width, y, 7.72), (8 + over, top, 8.28), 'back')
        m.box((8 + over, y, 7.64), (8 + over + width * 0.7, top, 8.36), 'face')
        m.box((8 + over + width * 0.7, y, 7.8), (8 + over + width, top, 8.2), 'edge', glow=True)
        y = top
    # round guard (three squares make it look round) and a wrapped grip
    m.centred(-6, -5, 2.3, 1.4, 'tsuba', sides={'up': 'tsuba2'})
    m.centred(-6, -5, 1.6, 2.1, 'tsuba', sides={'up': 'tsuba2'})
    m.centred(-16, -6, 0.95, 0.75, 'wrap')
    m.centred(-16.0, -15.0, 1.15, 0.95, 'cap')
    write_3d('anima_katana', m, s, display(-11, 0.74, 0.74))


def flame_hammer():
    s = Sheet()
    for name, c in {'metal': (118, 132, 174), 'metal2': (72, 78, 118), 'dark': (42, 44, 72), 'silver': (196, 206, 226), 'fire': (255, 122, 26), 'fire2': (255, 214, 90), 'wood': (112, 62, 40)}.items():
        s.cell(name, c)
    s.cell('wrap', (64, 36, 26), wrap=(150, 96, 56))
    m = Model(s)
    m.centred(-16, 14, 1.15, 1.15, 'wood')
    m.centred(-12, -2, 1.35, 1.35, 'wrap')
    for y in (-13, -2, 6):
        m.centred(y, y + 1.4, 1.55, 1.55, 'silver')
    m.centred(-16, -14.4, 1.9, 1.9, 'metal2')
    # the head: heavy, wider at the striking faces
    m.box((0.6, 15, 3.8), (15.4, 27, 12.2), 'metal2', sides={'up': 'metal', 'down': 'dark'})
    for x0, x1 in ((-1.4, 1.4), (14.6, 17.4)):
        m.box((x0, 14, 3), (x1, 28, 13), 'metal', sides={'west': 'silver', 'east': 'silver', 'up': 'silver', 'down': 'dark'})
    m.box((3.2, 27, 5), (12.8, 28.4, 11), 'metal')
    m.box((6, 13.2, 6), (10, 15.2, 10), 'silver')
    # flames in the metal: cracks that glow on both faces
    for z, sign in ((3.75, -1), (12.25, 1)):
        for x0, x1, y0, y1 in ((3, 4.2, 16, 21), (4.2, 5.4, 18.5, 25), (6.8, 8, 16, 26), (8, 9.2, 19, 23), (10.8, 12, 16.5, 22.5), (12, 13, 19, 24)):
            m.box((x0, y0, z - 0.1), (x1, y1, z + 0.1), 'fire', glow=True)
        m.box((7.1, 17, z - 0.14), (7.7, 22.5, z + 0.14), 'fire2', glow=True)
    write_3d('flame_hammer', m, s, display(-11, 0.72, 0.72))


def ice_staff():
    s = Sheet()
    for name, c in {'ice': (196, 240, 255), 'ice2': (118, 200, 244), 'ice3': (70, 150, 220), 'white': (236, 252, 255), 'shaft': (192, 206, 226), 'band': (70, 122, 180), 'silver': (222, 232, 244)}.items():
        s.cell(name, c)
    m = Model(s)
    m.centred(-16, 18, 0.8, 0.8, 'shaft')
    for y in (-14, -6, 2, 10):
        m.centred(y, y + 1.2, 1.05, 1.05, 'band')
    m.centred(-16, -15, 1.15, 1.15, 'band')
    # a silver claw holding the crystal
    m.centred(17, 19, 2.6, 2.6, 'silver')
    for ox, oz in ((-2.2, 0), (2.2, 0), (0, -2.2), (0, 2.2)):
        m.centred(19, 22, 0.6, 0.6, 'silver', ox=ox, oz=oz)
    # the crystal: a tall turned prism with a point, two smaller ones leaning out
    rot = {'origin': [8, 24, 8], 'axis': 'y', 'angle': 45}
    m.centred(19, 28, 2.6, 2.6, 'ice', sides={'north': 'white', 'east': 'ice2', 'west': 'ice2', 'south': 'ice3'}, rotation=rot)
    m.centred(28, 30.5, 1.7, 1.7, 'ice', sides={'north': 'white', 'south': 'ice2'}, rotation=rot)
    m.centred(30.5, 32, 0.8, 0.8, 'white', rotation=rot, glow=True)
    m.box((3.6, 20, 7), (5.6, 27, 9), 'ice2', sides={'east': 'white'}, rotation={'origin': [4.6, 20, 8], 'axis': 'z', 'angle': 22.5})
    m.box((10.4, 20, 7), (12.4, 25.5, 9), 'ice', sides={'west': 'white'}, rotation={'origin': [11.4, 20, 8], 'axis': 'z', 'angle': -22.5})
    write_3d('ice_staff', m, s, display(-2, 0.82, 0.62, tilt=0))


def dagger():
    s = Sheet()
    for name, c in {'edge': (146, 110, 206), 'face': (58, 42, 92), 'dark': (22, 16, 34), 'gem': (190, 90, 255), 'guard': (44, 34, 64), 'grip': (30, 22, 44)}.items():
        s.cell(name, c)
    m = Model(s)
    # a wavy little blade
    y = 7
    for top, over, half in ((10, 0.0, 1.6), (13, 0.5, 1.45), (16, -0.1, 1.2), (18.5, 0.4, 0.8), (20, 0.2, 0.3)):
        m.box((8 + over - half, y, 7.72), (8 + over, top, 8.28), 'dark')
        m.box((8 + over, y, 7.72), (8 + over + half * 0.7, top, 8.28), 'face')
        m.box((8 + over + half * 0.7, y, 7.82), (8 + over + half, top, 8.18), 'edge', glow=True)
        y = top
    m.centred(6, 7, 3.2, 0.9, 'guard')
    m.box((3.8, 7, 7.5), (5, 8.4, 8.5), 'guard')
    m.box((11, 7, 7.5), (12.2, 8.4, 8.5), 'guard')
    m.centred(5.8, 7.2, 0.8, 1.05, 'gem', glow=True)
    m.centred(1, 6, 0.8, 0.8, 'grip')
    m.centred(0, 1, 1.1, 1.1, 'guard')
    write_3d('dagger_of_darkness', m, s, display(3.5, 0.9, 0.9, ground=0.6))


# ---- sword in stone

def stone_block():
    rng = random.Random(3)
    top = Image.new('RGBA', (16, 16))
    side = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            g = rng.randint(96, 132)
            moss = rng.random() < 0.22
            top.putpixel((x, y), (g - 26, g + 8, g - 40, 255) if moss else (g, g, g, 255))
            g2 = rng.randint(84, 122) - (2 * y if y > 8 else 0)
            moss2 = y < 3 and rng.random() < 0.5
            side.putpixel((x, y), (g2 - 26, g2 + 6, g2 - 40, 255) if moss2 else (g2, g2, g2, 255))
    # the slit the sword sits in
    for x in range(3, 13):
        top.putpixel((x, 7), (14, 12, 12, 255))
        top.putpixel((x, 8), (22, 18, 16, 255))
    for x, y in ((3, 6), (12, 9), (2, 8), (13, 7), (5, 9), (10, 6)):
        top.putpixel((x, y), (214, 170, 66, 255))
    top.save(A + 'textures/block/sword_stone_top.png', optimize=True)
    side.save(A + 'textures/block/sword_stone_side.png', optimize=True)
    model = {'textures': {'top': 'charpowers:block/sword_stone_top', 'side': 'charpowers:block/sword_stone_side', 'particle': 'charpowers:block/sword_stone_side'},
             'elements': [
                 {'from': [1, 0, 1], 'to': [15, 3, 15], 'faces': {f: {'uv': [0, 13, 16, 16] if f not in ('up', 'down') else [0, 0, 16, 16], 'texture': '#top' if f == 'up' else '#side', **({'cullface': 'down'} if f == 'down' else {})} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
                 {'from': [3, 3, 2], 'to': [13, 5, 14], 'faces': {f: {'uv': [0, 11, 16, 13] if f not in ('up', 'down') else [0, 0, 16, 16], 'texture': '#top' if f == 'up' else '#side'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
             ]}
    save_json(A + 'models/block/sword_in_stone.json', model)
    save_json(A + 'blockstates/sword_in_stone.json', {'variants': {'pulled=false': {'model': 'charpowers:block/sword_in_stone'}, 'pulled=true': {'model': 'charpowers:block/sword_in_stone', 'y': 90}}})
    save_json(A + 'models/item/sword_in_stone.json', {'parent': 'charpowers:block/sword_in_stone'})


# ---- soft shapes for the trails: white, with the shape in the see-through part

def noise_row(rng, n, smooth):
    vals = [rng.random() for _ in range(n)]
    for _ in range(smooth):
        vals = [(vals[i - 1] + vals[i] + vals[(i + 1) % n]) / 3 for i in range(n)]
    lo, hi = min(vals), max(vals)
    return [(v - lo) / (hi - lo + 1e-6) for v in vals]


def trail_textures():
    w, h = 128, 64
    rng = random.Random(11)
    soft = Image.new('RGBA', (w, h))
    streak = Image.new('RGBA', (w, h))
    wisp = Image.new('RGBA', (w, h))
    rows = [noise_row(rng, w, 3) for _ in range(h)]
    blobs = [noise_row(rng, w, 7) for _ in range(5)]
    for y in range(h):
        v = y / (h - 1)
        hump = math.sin(math.pi * v) ** 0.85
        ramp = v ** 1.4 * (1 - max(0, v - 0.93) / 0.07 * 0.7)
        for x in range(w):
            soft.putpixel((x, y), (255, 255, 255, int(255 * hump)))
            streak.putpixel((x, y), (255, 255, 255, int(255 * ramp * (0.45 + 0.55 * rows[y][x]))))
            band = blobs[int(v * 4.99)][x]
            wob = blobs[(int(v * 4.99) + 2) % 5][(x * 2) % w]
            wisp.putpixel((x, y), (255, 255, 255, int(255 * min(1, max(0, hump * (0.25 + 1.25 * band * wob + 0.35 * rows[y][x]))))))
    glow = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 31.5, y - 31.5) / 31.5
            glow.putpixel((x, y), (255, 255, 255, int(255 * max(0, 1 - d) ** 2.2)))
    os.makedirs(A + 'textures/misc', exist_ok=True)
    soft.save(A + 'textures/misc/ribbon_soft.png', optimize=True)
    streak.save(A + 'textures/misc/ribbon_streak.png', optimize=True)
    wisp.save(A + 'textures/misc/ribbon_wisp.png', optimize=True)
    glow.save(A + 'textures/misc/ribbon_glow.png', optimize=True)
    # the ice shard
    shard = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.65 + 0.35 * (x / 15) + rng.random() * 0.1
            shard.putpixel((x, y), (int(170 * k), int(232 * k), 255, 255))
    os.makedirs(A + 'textures/entity', exist_ok=True)
    shard.save(A + 'textures/entity/ice_shard.png', optimize=True)


# ---- the lot

os.makedirs(A + 'textures/item', exist_ok=True)
os.makedirs(A + 'textures/block', exist_ok=True)
icons = {'golden_kings_long_sword': icon_sword, 'anima_katana': icon_katana, 'flame_hammer': icon_hammer, 'ice_staff': icon_staff, 'dagger_of_darkness': icon_dagger, 'fork': icon_fork}
sheet = Image.new('RGB', (4 * 200 + 16, 2 * 200 + 16), (46, 50, 60))
for i, (name, draw) in enumerate(icons.items()):
    icon = make_icon(draw)
    icon.save(A + f'textures/item/{name}.png', optimize=True)
    big = icon.resize((192, 192), Image.NEAREST)
    sheet.paste(big, (12 + i % 4 * 200, 12 + i // 4 * 200), big)
for i, liquid in enumerate(('empty', 'water', 'lava', 'milk')):
    icon = bottle_icon(liquid)
    icon.save(A + ('textures/item/spray_bottle.png' if liquid == 'empty' else f'textures/item/spray_bottle_{liquid}.png'), optimize=True)
    if liquid in ('empty', 'lava'):
        big = icon.resize((192, 192), Image.NEAREST)
        p = 6 if liquid == 'empty' else 7
        sheet.paste(big, (12 + p % 4 * 200, 12 + p // 4 * 200), big)
sheet.save('docs/previews/new_item_icons.png')

long_sword()
katana()
flame_hammer()
ice_staff()
dagger()
stone_block()
trail_textures()

save_json(A + 'models/item/fork.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'charpowers:item/fork'}})
save_json(A + 'models/item/spray_bottle.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'charpowers:item/spray_bottle'},
                                                'overrides': [{'predicate': {'charpowers:liquid': n + 1}, 'model': f'charpowers:item/spray_bottle_{l}'} for n, l in enumerate(('water', 'lava', 'milk'))]})
for l in ('water', 'lava', 'milk'):
    save_json(A + f'models/item/spray_bottle_{l}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'charpowers:item/spray_bottle_{l}'}})
save_json(A + 'models/item/solgo_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
print('weapon models made')
