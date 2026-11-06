# Solgo: his model, picture and animations for GeckoLib, all written here, and a preview so his looks can be judged without starting the game.
# Ten blocks tall if he ever stood up. He doesn't. From the charpowers folder:  python tools/make_solgo.py
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

R = 'src/main/resources/assets/charpowers/'
TEX_W, TEX_H = 256, 256
rng = random.Random(21)

# ---- the skeleton, drawn straight, then bent by its resting turns

H = 92  # height of his hips above the ground when his knees are bent the way they rest
bones = {}
order = []


def bone(name, parent, pivot, rotation=(0, 0, 0)):
    bones[name] = {'name': name, 'pivot': list(pivot), 'rotation': list(rotation), 'cubes': []}
    if parent:
        bones[name]['parent'] = parent
    order.append(name)


# texture regions: a shelf packer. Each region gets painted by a named painter once everything's known.
regions = []
shelf = {'x': 0, 'y': 0, 'h': 0}


def region(w, h, painter, **args):
    w, h = max(1, int(math.ceil(w))), max(1, int(math.ceil(h)))
    if shelf['x'] + w > TEX_W:
        shelf['x'] = 0
        shelf['y'] += shelf['h'] + 1
        shelf['h'] = 0
    r = {'x': shelf['x'], 'y': shelf['y'], 'w': w, 'h': h, 'painter': painter, 'args': args}
    shelf['x'] += w + 1
    shelf['h'] = max(shelf['h'], h)
    regions.append(r)
    return r


def cube(name, origin, size, painter, front=None, top=None, rotation=None, pivot=None, **args):
    """origin + size like Bedrock's. All four sides share a picture, except the front if another is given."""
    w, h, d = size
    side = region(max(w, d), h, painter, **args)
    fr = region(w, h, front, **args) if front else side
    cap = region(w, d, top or painter, cap=True, **args)
    fu = lambda r, ww, hh: {'uv': [r['x'], r['y']], 'uv_size': [ww, hh]}
    c = {'origin': [round(v, 3) for v in origin], 'size': [round(v, 3) for v in size],
         'uv': {'north': fu(fr, max(1, w), max(1, h)), 'south': fu(side, max(1, w), max(1, h)), 'east': fu(side, max(1, d), max(1, h)), 'west': fu(side, max(1, d), max(1, h)),
                'up': fu(cap, max(1, w), max(1, d)), 'down': fu(cap, max(1, w), max(1, d))}}
    if rotation:
        c['rotation'] = list(rotation)
        c['pivot'] = list(pivot)
    bones[name]['cubes'].append(c)


def hanging(name, px, py, pz, w, length, d, painter, **args):
    """A limb that hangs down from its pivot."""
    cube(name, (px - w / 2, py - length, pz - d / 2), (w, length, d), painter, **args)


def rising(name, px, py, pz, w, length, d, painter, **args):
    cube(name, (px - w / 2, py, pz - d / 2), (w, length, d), painter, **args)


bone('root', None, (0, 0, 0))
bone('hips', 'root', (0, H, 0))
cube('hips', (-5, H - 3, -3), (10, 6, 6), 'skin', shade=0.75)

# ---- legs: long thin thighs forward, shins back, feet flat with long toes
for side, sx in (('r', -3.6), ('l', 3.6)):
    bone(f'leg_{side}_up', 'hips', (sx, H, 0), (-38, 0, 6 if side == 'l' else -6))
    hanging(f'leg_{side}_up', sx, H, 0, 3.4, 54, 3.4, 'skin', shade=0.86)
    cube(f'leg_{side}_up', (sx - 2.3, H - 56, -2.8), (4.6, 5, 4.6), 'skin', shade=1.0)  # the knee, too big
    bone(f'leg_{side}_low', f'leg_{side}_up', (sx, H - 54, 0), (60, 0, 0))
    hanging(f'leg_{side}_low', sx, H - 54, 0, 2.6, 50, 2.6, 'skin', shade=0.8)
    bone(f'foot_{side}', f'leg_{side}_low', (sx, H - 104, 0), (-22, 0, 0))
    cube(f'foot_{side}', (sx - 1.8, H - 108, -5), (3.6, 4, 8), 'skin', shade=0.8)
    for toe in (-1.4, 0, 1.4):
        cube(f'foot_{side}', (sx + toe - 0.4, H - 108, -15), (0.8, 1.6, 10), 'skin', shade=0.7)

# ---- body: a thin bent spine
bone('spine1', 'hips', (0, H + 2, 0), (32, 0, 0))
rising('spine1', 0, H + 2, 0, 7, 18, 4.4, 'skin', shade=0.7)
bone('spine2', 'spine1', (0, H + 20, 0), (26, 0, 0))
cube('spine2', (-6.5, H + 20, -3), (13, 22, 6), 'skin', front='ribs', shade=0.85)
for i in range(6):  # the bones of his back stick out
    cube('spine2', (-0.8, H + 21 + i * 3.4, 2.6), (1.6, 1.8, 1.8), 'skin', shade=1.05)
bone('neck1', 'spine2', (0, H + 41, 0), (18, 0, 0))
rising('neck1', 0, H + 41, 0, 3, 9, 3, 'skin', shade=0.72)
bone('neck2', 'neck1', (0, H + 50, 0), (6, 0, 0))
rising('neck2', 0, H + 50, 0, 2.6, 8, 2.6, 'skin', shade=0.72)
# small head, blank and pale, with a wide dark split where a face should be
bone('head', 'neck2', (0, H + 58, 0), (-62, 0, 0))
cube('head', (-4, H + 61.5, -3.6), (8, 7, 7.2), 'skin', front='face', top='scalp', shade=1.08)
cube('head', (-3.6, H + 60, -2), (7.2, 2, 5.4), 'void')  # dark inside the split
bone('jaw', 'head', (0, H + 62, 1), (0, 0, 0))
cube('jaw', (-3.8, H + 58, -3.4), (7.6, 3.6, 6.8), 'skin', front='chin', shade=1.0)

# ---- arms: longer than they've any right to be
bone('shoulder_r', 'spine2', (-6.5, H + 38, 0))
bone('arm_r_up', 'shoulder_r', (-7, H + 38, 0), (-93, 0, 0))
hanging('arm_r_up', -7, H + 38, 0, 3, 46, 3, 'skin', shade=0.84)
cube('arm_r_up', (-9, H + 35, -2.4), (4, 5, 4.8), 'skin', shade=1.0)
bone('arm_r_low', 'arm_r_up', (-7, H - 8, 0), (-20, 0, 0))
hanging('arm_r_low', -7, H - 8, 0, 2.4, 50, 2.4, 'wrap')
bone('hand_r', 'arm_r_low', (-7, H - 58, 0), (-5, 0, 0))
cube('hand_r', (-8.6, H - 63, -1.4), (3.2, 5, 2.8), 'skin', shade=0.9)
for i, dx in enumerate((-1.2, 0, 1.2)):  # fingers grown round the grip
    cube('hand_r', (-7 + dx - 0.45, H - 69, -1.8), (0.9, 7, 0.9), 'skin', shade=0.82)
# the katana comes straight out of his hand: no guard, the flesh just turns into blade
bone('katana', 'hand_r', (-7, H - 66, 0), (4, 0, 0))
cube('katana', (-7.6, H - 74, -0.5), (1.2, 12, 1), 'skin', shade=0.6)
cube('katana', (-7.5, H - 150, -0.42), (1, 77, 0.84), 'blade', length=77)
cube('katana', (-7.0, H - 148, -0.65), (1.4, 74, 1.3), 'blade_edge', length=74)
cube('katana', (-7.15, H - 154, -0.28), (0.7, 5, 0.56), 'blade', length=5)
bone('blade_base', 'katana', (-7, H - 76, 0))
bone('blade_tip', 'katana', (-7, H - 152, 0))

bone('shoulder_l', 'spine2', (6.5, H + 38, 0))
bone('arm_l_up', 'shoulder_l', (7, H + 38, 0), (-64, 0, -6))
hanging('arm_l_up', 7, H + 38, 0, 3, 46, 3, 'skin', shade=0.84)
cube('arm_l_up', (5, H + 35, -2.4), (4, 5, 4.8), 'skin', shade=1.0)
bone('arm_l_low', 'arm_l_up', (7, H - 8, 0), (-10, 0, 0))
hanging('arm_l_low', 7, H - 8, 0, 2.4, 50, 2.4, 'wrap')
bone('hand_l', 'arm_l_low', (7, H - 58, 0), (-8, 0, 0))
cube('hand_l', (5.3, H - 64, -1.4), (3.4, 6, 2.8), 'skin', shade=0.9)
for dx, a in ((-1.4, 8), (0, 0), (1.4, -8)):  # long fingers, nearly on the ground
    cube('hand_l', (7 + dx - 0.45, H - 80, -0.45), (0.9, 16, 0.9), 'skin', shade=0.78, rotation=(0, 0, a), pivot=(7 + dx, H - 64, 0))

# ---- rags: strips hanging off his shoulders and hips. Each one's a thin sheet that can sway
RAGS = [('rag_a', 'spine2', -5, H + 40, 3.6, 7, 58, -58), ('rag_b', 'spine2', 1, H + 41, 3.6, 8, 72, -58), ('rag_c', 'spine2', 5.5, H + 39, 3.2, 6, 50, -58),
        ('rag_d', 'hips', -3.6, H + 3, -3.4, 5, 30, 0), ('rag_e', 'hips', 3, H + 3, -3.4, 4, 26, 0), ('rag_f', 'arm_r_up', -8.6, H + 34, 0, 3, 30, 0), ('rag_g', 'arm_l_low', 8.5, H - 10, 0, 3, 26, 0)]
for name, parent, x, y, z, w, length, turn in RAGS:
    bone(name, parent, (x, y, z), (turn, 0, 0))
    cube(name, (x - w / 2, y - length, z - 0.1), (w, length, 0.2), 'rag', width=w, length=length)
bone('mantle', 'spine2', (0, H + 41, 0), (-26, 0, 0))
cube('mantle', (-9, H + 28, -4.2), (18, 15, 9), 'rag_solid')

# ---- painting

im = Image.new('RGBA', (TEX_W, TEX_H), (0, 0, 0, 0))
glow = Image.new('RGBA', (TEX_W, TEX_H), (0, 0, 0, 0))


def jitter(c, amount=6):
    k = rng.randint(-amount, amount)
    return tuple(max(0, min(255, v + k)) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def paint(r):
    x0, y0, w, h = r['x'], r['y'], r['w'], r['h']
    p = r['painter']
    a = r['args']
    shade = a.get('shade', 1)
    for j in range(h):
        for i in range(w):
            u = (i + 0.5) / w
            v = (j + 0.5) / h
            c = None
            if p == 'skin':
                roll = 0.62 + 0.38 * math.sin(math.pi * u)
                base = 204 * shade * roll
                c = jitter((int(base), int(base * 0.965), int(base * 0.9)), 5)
                if rng.random() < 0.03:
                    c = (int(base * 0.55), int(base * 0.5), int(base * 0.58), 255)
            elif p == 'scalp':
                base = 176
                c = jitter((base, int(base * 0.96), int(base * 0.9)), 8)
            elif p == 'ribs':
                base = 178 * (0.7 + 0.3 * math.sin(math.pi * u))
                rib = (j % 3 == 0) and 0.12 < u < 0.88 and v < 0.78
                c = jitter((int(base * (0.55 if rib else 1)), int(base * (0.52 if rib else 0.965)), int(base * (0.56 if rib else 0.9))), 5)
                if abs(u - 0.5) < 0.05:
                    c = (64, 54, 62, 255)
            elif p == 'face':
                # blank and pale; a dark wide split crooked across where a mouth would go
                base = 226 - abs(u - 0.5) * 50
                c = jitter((int(base), int(base * 0.975), int(base * 0.93)), 3)
                dents = (abs(u - 0.26) < 0.09 or abs(u - 0.74) < 0.09) and 0.2 < v < 0.34
                if dents:
                    c = (int(base * 0.72), int(base * 0.68), int(base * 0.72), 255)
                if j == h - 1 or j == h - 2 and (i < 2 or i > w - 3):
                    c = (10, 6, 10, 255)
            elif p == 'chin':
                base = 214 - abs(u - 0.5) * 50
                c = jitter((int(base), int(base * 0.975), int(base * 0.93)), 3)
                if j == 0:
                    c = (10, 6, 10, 255)
            elif p == 'void':
                c = (8, 3, 8, 255)
                if rng.random() < 0.08:
                    c = (64, 8, 30, 255)
            elif p == 'wrap':
                # dirty bandage round the forearm, skin showing through
                band = (j + i // 2) % 5 < 3
                c = jitter((46, 40, 52), 8) if band else jitter((158, 150, 142), 8)
            elif p == 'rag':
                wid = a.get('width', 4)
                length = a.get('length', 30)
                edge = 1 - v
                tears = 0.5 + 0.5 * math.sin(i * 2.1 + j * 0.15) * math.sin(i * 0.7 + 1.3)
                gone = v > 0.72 + 0.28 * tears or (rng.random() < 0.05 and v > 0.3)
                if gone:
                    c = (0, 0, 0, 0)
                else:
                    c = jitter((26, 20, 34), 5)
                    if rng.random() < 0.12:
                        c = jitter((48, 34, 58), 5)
            elif p == 'rag_solid':
                gone = a.get('cap') is None and v > 0.55 + 0.45 * abs(math.sin(i * 1.7))
                c = (0, 0, 0, 0) if gone else jitter((24, 18, 32), 5)
            elif p == 'blade':
                # pale where it leaves his hand, darkening to violet-black along the length
                k = (j + 0.5) / h  # 0 at the end by his hand, 1 at the point
                if k < 0.14:
                    m = k / 0.14
                    c = jitter((int(176 - 66 * m), int(170 - 66 * m), int(160 - 38 * m)), 4)
                else:
                    m = (k - 0.14) / 0.86
                    c = jitter((int(110 - 58 * m), int(104 - 66 * m), int(122 - 42 * m)), 4)
            elif p == 'blade_edge':
                c = (238, 226, 255, 255) if i == 0 else (120, 90, 160, 255)
            if c is None:
                c = (255, 0, 255, 255)
            im.putpixel((x0 + i, y0 + j), c)
            if p == 'blade_edge' and i == 0:
                glow.putpixel((x0 + i, y0 + j), (170, 120, 255, 255))
            if p == 'void':
                glow.putpixel((x0 + i, y0 + j), (60, 8, 30, 255))
            if p == 'face' and (j == h - 1 or j == h - 2 and (i < 2 or i > w - 3)):
                glow.putpixel((x0 + i, y0 + j), (80, 10, 40, 255))


for r in regions:
    paint(r)

os.makedirs(R + 'geo', exist_ok=True)
os.makedirs(R + 'textures/entity', exist_ok=True)
os.makedirs(R + 'animations', exist_ok=True)
im.save(R + 'textures/entity/solgo.png', optimize=True)
glow.save(R + 'textures/entity/solgo_glowmask.png', optimize=True)

geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
    'description': {'identifier': 'geometry.solgo', 'texture_width': TEX_W, 'texture_height': TEX_H, 'visible_bounds_width': 12, 'visible_bounds_height': 12, 'visible_bounds_offset': [0, 5, 0]},
    'bones': [{k: v for k, v in bones[n].items() if not (k == 'cubes' and not v)} for n in order]}]}
with open(R + 'geo/solgo.geo.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump(geo, f, separators=(',', ':'))
    f.write('\n')


# ---- animations: keys are (time, {bone: (x, y, z) turn added to rest}), positions under a bone name with + at the end

def smooth(u):
    return u * u * (3 - 2 * u)


def sharp(u):
    return 1 - (1 - u) ** 3


def track_names(keys):
    names = set()
    for _, pose in keys:
        names.update(pose)
    return names


def build(keys, loop=False, fps=20, ease=smooth):
    length = keys[-1][0]
    frames = int(round(length * fps))
    out = {}
    for name in track_names(keys):
        real = name.rstrip('+')
        kind = 'position' if name.endswith('+') else 'rotation'
        vals = {}
        for fi in range(frames + 1):
            tt = fi / fps
            for (t0, p0), (t1, p1) in zip(keys, keys[1:]):
                if t0 - 1e-6 <= tt <= t1 + 1e-6:
                    u = ease((tt - t0) / (t1 - t0)) if t1 > t0 else 1
                    a = p0.get(name, (0, 0, 0))
                    b = p1.get(name, (0, 0, 0))
                    vals[f'{tt:.2f}'] = [round(a[i] + (b[i] - a[i]) * u, 2) for i in range(3)]
                    break
        out.setdefault(real, {})[kind] = vals
    anim = {'animation_length': round(length, 3), 'bones': out}
    anim['loop'] = True if loop else 'hold_on_last_frame'
    return anim


def add(*poses):
    out = {}
    for pose in poses:
        for k, v in pose.items():
            old = out.get(k, (0, 0, 0))
            out[k] = (old[0] + v[0], old[1] + v[1], old[2] + v[2])
    return out


def cloth(t, speed=1.0, size=1.0):
    """Rags sway on their own time, a beat behind him."""
    out = {}
    for i, (name, *_rest) in enumerate(RAGS):
        out[name] = (math.sin(t * 2.3 * speed + i * 1.4) * 7 * size, 0, math.sin(t * 1.7 * speed + i * 2.2) * 5 * size)
    return out


def idle_pose(t):
    s = math.sin(t * math.pi * 2 / 4.8)
    c = math.cos(t * math.pi * 2 / 4.8)
    pose = {'hips': (0, 0, s * 3), 'spine1': (s * 1.5, 0, -s * 2), 'spine2': (c * 2.2, c * 2, 0), 'neck1': (-c * 2, 0, s * 3),
            'arm_l_up': (s * 4, 0, c * 2), 'arm_l_low': (c * 3, 0, 0), 'arm_r_up': (c * 1.5, 0, 0), 'hand_l': (s * 5, 0, c * 3),
            'hips+': (0, s * 0.8, 0)}
    return add(pose, cloth(t, 0.7))


IDLE = build([(i * 0.4, idle_pose(i * 0.4)) for i in range(13)], loop=True)


def walk_pose(t):
    # one foot hurries and the other drags: the phase runs unevenly through the cycle
    cycle = 2.4
    raw = (t % cycle) / cycle
    phase = raw + 0.09 * math.sin(raw * 2 * math.pi)
    a = phase * 2 * math.pi
    s, c = math.sin(a), math.cos(a)
    lift_r = max(0, -c)  # the leg that's coming through bends and lifts
    lift_l = max(0, c)
    sway = math.sin(a + 0.6)
    pose = {'leg_r_up': (-s * 21 - lift_r * 18, 0, 0), 'leg_r_low': (lift_r * 34, 0, 0), 'foot_r': (s * 8 - lift_r * 12, 0, 0),
            'leg_l_up': (s * 21 - lift_l * 18, 0, 0), 'leg_l_low': (lift_l * 34, 0, 0), 'foot_l': (-s * 8 - lift_l * 12, 0, 0),
            'hips': (0, s * 7, sway * 8), 'hips+': (sway * 2.5, abs(c) * 3 - 3, 0), 'spine1': (c * 2, -s * 5, -sway * 7), 'spine2': (-c * 2.5, -s * 7, -sway * 3),
            'neck1': (s * 3, s * 9, sway * 7), 'neck2': (0, 0, -sway * 5),
            'arm_l_up': (math.sin(a - 1.1) * 15, 0, 0), 'arm_l_low': (max(0, math.sin(a - 0.5)) * -14, 0, 0), 'hand_l': (math.sin(a - 0.2) * 12, 0, 0),
            'arm_r_up': (math.sin(a + 2.0) * 5, s * 4, 0), 'arm_r_low': (math.sin(a + 1.6) * 4, 0, 0)}
    return add(pose, cloth(t, 1.3, 1.3))


WALK = build([(i * 0.2, walk_pose(i * 0.2)) for i in range(13)], loop=True, ease=lambda u: u)


def run_pose(t):
    # phase two: down on all fours, knees high, arms reaching
    cycle = 0.9
    a = (t % cycle) / cycle * 2 * math.pi
    s, c = math.sin(a), math.cos(a)
    base = {'hips+': (0, -30 + abs(s) * 5, 0), 'spine1': (34, 0, 0), 'spine2': (6, 0, 0), 'neck1': (-14, 0, 0), 'neck2': (-14, 0, 0),
            'leg_r_up': (-40, 0, -6), 'leg_l_up': (-40, 0, 6), 'leg_r_low': (34, 0, 0), 'leg_l_low': (34, 0, 0),
            'arm_l_up': (-22, 0, -8), 'arm_l_low': (6, 0, 0), 'arm_r_up': (14, 0, -6), 'arm_r_low': (10, 0, 0)}
    move = {'leg_r_up': (-s * 34, 0, 0), 'leg_l_up': (-s * 30, 0, 0), 'leg_r_low': (max(0, c) * 30, 0, 0), 'leg_l_low': (max(0, c) * 26, 0, 0), 'foot_r': (s * 14, 0, 0), 'foot_l': (s * 14, 0, 0),
            'arm_l_up': (s * 38, 0, 0), 'arm_l_low': (max(0, -c) * -18, 0, 0), 'arm_r_up': (s * 24, 0, 0), 'hand_l': (s * 10, 0, 0),
            'spine1': (c * 7, 0, 0), 'spine2': (-c * 5, 0, 0), 'hips': (s * 5, 0, c * 4), 'neck2': (c * 6, 0, 0)}
    return add(base, move, cloth(t, 3, 1.8))


RUN = build([(i * 0.075, run_pose(i * 0.075)) for i in range(13)], loop=True, ease=lambda u: u)

UP_R = {'spine1': (0, 18, 0), 'spine2': (-8, 22, 0), 'shoulder_r': (0, 0, 0), 'arm_r_up': (-40, 62, 22), 'arm_r_low': (-22, 0, 0), 'hand_r': (-18, 0, 0), 'katana': (-30, 0, 0),
        'arm_l_up': (14, 0, -14), 'hips': (0, 10, 0), 'hips+': (0, -6, 0), 'neck1': (-4, -18, 0), 'neck2': (0, -14, 0)}
WINDUP = build([(0, {}), (0.7, UP_R), (0.9, add(UP_R, {'arm_r_up': (-5, 4, 0), 'spine2': (-2, 3, 0)}))])

CUT = {'spine1': (6, -22, 0), 'spine2': (12, -28, 0), 'arm_r_up': (-6, -82, -6), 'arm_r_low': (-12, 0, 0), 'hand_r': (-8, 0, 0), 'katana': (-12, 0, 0), 'hips': (0, -14, 0), 'hips+': (0, -10, 0),
       'arm_l_up': (-10, 0, 24), 'neck1': (0, 20, 0), 'neck2': (0, 14, 0), 'leg_l_up': (-10, 0, 0), 'leg_r_up': (8, 0, 0)}
SLASH = build([(0, UP_R), (0.18, CUT), (0.4, CUT), (0.8, add(CUT, {'arm_r_up': (4, 14, 2)}))], ease=sharp)

LOW = {'hips+': (0, -30, 0), 'leg_r_up': (-26, 0, -4), 'leg_l_up': (-26, 0, 4), 'leg_r_low': (34, 0, 0), 'leg_l_low': (34, 0, 0), 'foot_r': (-8, 0, 0), 'foot_l': (-8, 0, 0),
       'spine1': (14, 0, 0), 'spine2': (10, 0, 0), 'neck1': (-14, 0, 0), 'neck2': (-8, 0, 0), 'arm_r_up': (-6, 22, 0), 'arm_r_low': (-16, 0, 0), 'katana': (-10, 0, 0), 'arm_l_up': (-16, 0, -4), 'hand_l': (-10, 0, 0)}
CROUCH = build([(0, {}), (0.7, LOW), (0.95, add(LOW, {'hips+': (0, -2, 0), 'spine2': (2, 0, 0)}))])

STAB = {'hips+': (0, -14, 0), 'spine1': (22, 0, 0), 'spine2': (12, 0, 0), 'leg_r_up': (24, 0, 0), 'leg_r_low': (14, 0, 0), 'leg_l_up': (-44, 0, 0), 'leg_l_low': (28, 0, 0), 'neck1': (-18, 0, 0), 'neck2': (-14, 0, 0),
        'arm_r_up': (-22, -6, 0), 'arm_r_low': (10, 0, 0), 'hand_r': (6, 0, 0), 'katana': (-2, 0, 0), 'arm_l_up': (34, 0, -10)}
LUNGE = build([(0, LOW), (0.2, STAB), (0.65, STAB), (0.95, add(STAB, {'hips+': (0, 6, 0)}))], ease=sharp)

ARRIVE = build([(0, add(LOW, {'hips+': (0, -10, 0)})), (0.35, LOW), (0.8, {})])

SCREAM_POSE = {'neck1': (-26, 0, 0), 'neck2': (-22, 0, 8), 'jaw': (42, 0, 0), 'spine2': (-14, 0, 0), 'spine1': (-8, 0, 0), 'arm_l_up': (-10, 0, -44), 'arm_l_low': (-12, 0, 0), 'hand_l': (-14, 0, 0), 'arm_r_up': (6, 0, 26), 'hips+': (0, 4, 0)}
SCREAM = build([(0, {}), (0.55, {'neck1': (8, 0, 0), 'spine2': (6, 0, 0)}), (0.75, SCREAM_POSE), (1.45, add(SCREAM_POSE, {'neck2': (-2, 6, -8), 'jaw': (6, 0, 0)})), (1.8, {})])

STARED = {'neck1': (-5, 0, 0), 'spine2': (-3, 0, 0), 'arm_l_low': (-4, 0, 0), 'hand_l': (-10, 0, 0)}
STARED_ANIM = build([(0, {}), (0.1, STARED)] + [(0.15 + i * 0.1, add(STARED, {'neck2': (0, 0, (1 if i % 2 else -1) * 2.5), 'spine2': (0, (1 if i % 2 else -1) * 0.8, 0), 'hand_l': (0, 0, (1 if i % 2 else -1) * 4)})) for i in range(10)], ease=lambda u: u)

FOLD = {'hips+': (0, -46, 0), 'leg_r_up': (-44, 0, -18), 'leg_l_up': (-34, 0, 18), 'leg_r_low': (52, 0, 0), 'leg_l_low': (48, 0, 0), 'spine1': (24, 0, 10), 'spine2': (24, 0, -8), 'neck1': (24, 0, 0), 'neck2': (22, 0, 12),
        'arm_r_up': (44, 0, 18), 'arm_r_low': (18, 0, 0), 'katana': (46, 0, 0), 'arm_l_up': (20, 0, -14), 'hand_l': (20, 0, 0), 'jaw': (24, 0, 0)}
DOWN = add(FOLD, {'hips+': (0, -38, 0), 'root': (0, 0, 0), 'spine1': (26, 0, 0), 'spine2': (6, 0, 0), 'leg_r_up': (-24, 0, 0), 'leg_l_up': (-22, 0, 0), 'leg_r_low': (20, 0, 0), 'leg_l_low': (20, 0, 0)})
DEATH = build([(0, {}), (0.25, {'neck1': (-30, 0, 0), 'jaw': (36, 0, 0), 'spine2': (-14, 0, 0)}), (0.7, add({'neck1': (-20, 0, 0), 'jaw': (30, 0, 0)}, {'hips+': (0, -8, 0)})), (1.6, FOLD), (2.4, DOWN), (3.5, DOWN)])

anims = {'idle': IDLE, 'walk': WALK, 'run': RUN, 'windup': WINDUP, 'slash': SLASH, 'crouch': CROUCH, 'lunge': LUNGE, 'arrive': ARRIVE, 'scream': SCREAM, 'stared': STARED_ANIM, 'death': DEATH}
with open(R + 'animations/solgo.animation.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump({'format_version': '1.8.0', 'animations': anims}, f, separators=(',', ':'))
    f.write('\n')


# ---- preview: draws him as GeckoLib would, so poses can be judged here

def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def trans(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def apply(m, v):
    return tuple(m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2] + m[i][3] for i in range(3))


def pose_at(anim, tt):
    """A bone's added turn and shift at a time."""
    out = {}
    if not anim:
        return out
    for name, tracks in anim['bones'].items():
        rot = (0, 0, 0)
        pos = (0, 0, 0)
        for kind in ('rotation', 'position'):
            vals = tracks.get(kind)
            if not vals:
                continue
            ks = sorted((float(k), v) for k, v in vals.items())
            val = ks[-1][1]
            for k0, k1 in zip(ks, ks[1:]):
                if k0[0] <= tt <= k1[0]:
                    u = (tt - k0[0]) / max(1e-6, k1[0] - k0[0])
                    val = [k0[1][i] + (k1[1][i] - k0[1][i]) * u for i in range(3)]
                    break
            if kind == 'rotation':
                rot = tuple(val)
            else:
                pos = tuple(val)
        out[name] = (rot, pos)
    return out


def world_matrices(pose):
    mats = {}
    for name in order:
        b = bones[name]
        px, py, pz = b['pivot']
        rot = b['rotation']
        add_rot, add_pos = pose.get(name, ((0, 0, 0), (0, 0, 0)))
        ax, ay, az = math.radians(-(rot[0] + add_rot[0])), math.radians(-(rot[1] + add_rot[1])), math.radians(rot[2] + add_rot[2])
        m = trans(-add_pos[0], add_pos[1], add_pos[2])
        m = mat_mul(m, trans(-px, py, pz))
        m = mat_mul(m, mat_mul(rz(az), mat_mul(ry(ay), rx(ax))))
        m = mat_mul(m, trans(px, -py, -pz))
        parent = b.get('parent')
        mats[name] = mat_mul(mats[parent], m) if parent else m
    return mats


def faces_of(c):
    x, y, z = c['origin']
    w, h, d = c['size']
    x0, x1 = -(x + w), -x
    y0, y1 = y, y + h
    z0, z1 = z, z + d
    v = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    return v, [((0, 1, 2, 3), 'north'), ((5, 4, 7, 6), 'south'), ((4, 0, 3, 7), 'east'), ((1, 5, 6, 2), 'west'), ((3, 2, 6, 7), 'up'), ((4, 5, 1, 0), 'down')]


def face_colour(c, face):
    uv = c['uv'][face]
    x, y = uv['uv']
    w, h = uv['uv_size']
    px = im.crop((int(x), int(y), int(x + max(1, w)), int(y + max(1, h)))).resize((1, 1), Image.BOX).getpixel((0, 0))
    return px


def draw_pose(d, ox, oy, scale, yaw, pose, look=0.0):
    mats = world_matrices(pose)
    tris = []
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    for name in order:
        m = mats[name]
        for c in bones[name]['cubes']:
            verts, faces = faces_of(c)
            cm = m
            if 'rotation' in c:
                rot = c['rotation']
                px, py, pz = c['pivot']
                local = mat_mul(trans(-px, py, pz), mat_mul(rz(math.radians(rot[2])), mat_mul(ry(math.radians(-rot[1])), mat_mul(rx(math.radians(-rot[0])), trans(px, -py, -pz)))))
                cm = mat_mul(m, local)
            pts = [apply(cm, v) for v in verts]
            for idx, face in faces:
                col = face_colour(c, face)
                if col[3] < 40:
                    continue
                q = [pts[i] for i in idx]
                # turn round the vertical; the viewer stands at -z looking towards +z
                vp = []
                for (x, y, z) in q:
                    rx_, rz_ = x * cy + z * sy, -x * sy + z * cy
                    vp.append((ox - rx_ * scale, oy - y * scale, rz_))
                depth = sum(p[2] for p in vp) / 4
                ax, ay = vp[1][0] - vp[0][0], vp[1][1] - vp[0][1]
                bx, by = vp[2][0] - vp[0][0], vp[2][1] - vp[0][1]
                light = {'up': 1.0, 'north': 0.92, 'south': 0.72, 'east': 0.8, 'west': 0.8, 'down': 0.55}[face]
                tris.append((depth, [(p[0], p[1]) for p in vp], tuple(int(v * light) for v in col[:3])))
    tris.sort(key=lambda t: -t[0])
    for _, pts, col in tris:
        d.polygon(pts, fill=col)


def preview():
    fog = (92, 104, 112)
    W, H_img = 1500, 900
    out = Image.new('RGB', (W, H_img), fog)
    d = ImageDraw.Draw(out)
    s = 3.1
    ground = 520
    rows = [('rest, from the front', None, 0, 0), ('rest, 3/4', None, 0, 40), ('rest, side', None, 0, 90), ('walk', 'walk', 0.45, 55), ('walk later', 'walk', 1.5, 55)]
    x = 150
    for title, name, tt, yaw in rows:
        d.line((x - 130, ground, x + 130, ground), fill=(40, 46, 50), width=2)
        draw_pose(d, x, ground, s, yaw, pose_at(anims.get(name), tt))
        d.rectangle((x + 92, ground - 29 * s, x + 100, ground), fill=(60, 120, 200))
        d.text((x - 110, ground + 8), title, fill=(255, 255, 255))
        x += 295
    s2 = 1.7
    ground2 = 860
    row = [('windup', 'windup', 0.85), ('slash', 'slash', 0.16), ('crouch', 'crouch', 0.9), ('lunge', 'lunge', 0.3), ('scream', 'scream', 1.0), ('stared', 'stared', 0.5), ('run', 'run', 0.2), ('run', 'run', 0.6), ('death', 'death', 1.6), ('death end', 'death', 3.4)]
    x = 85
    for title, name, tt in row:
        draw_pose(d, x, ground2, s2, 62, pose_at(anims[name], tt))
        d.text((x - 30, ground2 + 6), title, fill=(255, 255, 255))
        x += 146
    os.makedirs('docs/previews', exist_ok=True)
    out.save('docs/previews/solgo_preview.png')
    # how tall is he?
    mats = world_matrices({})
    top = max(apply(mats[n], (0, b, 0))[1] for n in order for b in [bones[n]['pivot'][1]])
    print('head pivot height (blocks):', round(apply(mats['head'], (0, bones['head']['pivot'][1], 0))[1] / 16, 2))
    print('foot height (px):', round(min(apply(mats[f'foot_{sd}'], (0, H - 108, -5))[1] for sd in 'rl'), 1))
    print('blade tip height (px):', round(apply(mats['blade_tip'], (7, H - 152, 0))[1], 1), 'hand height', round(apply(mats['hand_r'], (7, H - 58, 0))[1], 1), 'left fingers', round(apply(mats['hand_l'], (-7, H - 80, 0))[1], 1))


preview()
print('solgo made; texture rows used up to', shelf['y'] + shelf['h'], 'of', TEX_H)
