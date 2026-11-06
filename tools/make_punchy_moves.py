# The Player's weapons in Punchy! (first-person animations): what kind each one is, and our own two-handed moves for the long ones.
# Punchy reads both from inside this mod's jar. Without Punchy they do nothing. From the charpowers folder:  python tools/make_punchy_moves.py
import json
import math
import os

OUT = 'src/main/resources/assets/minecraft/punchy/'
FPS = 24
# Off for now. Tried for real once (2026-11-06, Punchy 3.0): the two-handed guard pose shows both hands, but our attack clips didn't move the arms,
# only the grip, so the weapons use Punchy's own one-handed sword and mace moves meanwhile. The clips have since been made slower (Punchy's own last over a second),
# not tried again. Try them live with  python tools/run_real_client.py pack lab punchy=<jar>, which reloads them as you go.
TWO_HANDED = False


def pose(right=(0, 0, 0), right_at=(0, 0, 0), left=(0, 0, 0), left_at=(0, 0, 0), grip=(0, 0, 0), grip_at=(0, 0, 0)):
    return {'right_arm': (right, right_at), 'left_arm': (left, left_at), 'itemgrip_right': (grip, grip_at)}


def shifted(p, right=(0, 0, 0), right_at=(0, 0, 0), left=(0, 0, 0), left_at=(0, 0, 0), grip=(0, 0, 0), grip_at=(0, 0, 0)):
    add = lambda a, b: tuple(x + y for x, y in zip(a, b))
    return {'right_arm': (add(p['right_arm'][0], right), add(p['right_arm'][1], right_at)), 'left_arm': (add(p['left_arm'][0], left), add(p['left_arm'][1], left_at)),
            'itemgrip_right': (add(p['itemgrip_right'][0], grip), add(p['itemgrip_right'][1], grip_at))}


def slow(keys, by=2.2):
    """Punchy eases into a clip, and a very short one barely shows: its own attacks last over a second."""
    return [(t * by, p) for t, p in keys]


def smooth(t):
    return t * t * (3 - 2 * t)


def clip(keys, loop=False, timeline='dual_handed;'):
    """keys = (time, pose). In between they ease, one frame of the file for each frame at 24 a second."""
    length = keys[-1][0]
    frames = [i / FPS for i in range(int(math.ceil(length * FPS)) + 1)]
    bones = {}
    for bone in keys[0][1]:
        for part, field in ((0, 'rotation'), (1, 'position')):
            track = {}
            for t in frames:
                t = min(t, length)
                for (t0, p0), (t1, p1) in zip(keys, keys[1:]):
                    if t0 <= t <= t1:
                        u = smooth((t - t0) / (t1 - t0)) if t1 > t0 else 1
                        track[f'{t:.4f}'.rstrip('0').rstrip('.') if t else '0.0'] = [round(a + (b - a) * u, 2) for a, b in zip(p0[bone][part], p1[bone][part])]
                        break
            bones.setdefault(bone, {})[field] = track
    out = {'animation_length': round(length, 4), 'bones': bones, 'timeline': {'0.0': timeline}}
    if loop:
        out['loop'] = True
    return out


# ---- the long weapons: right hand low on the shaft, left hand further up it
SPEAR_GUARD = pose(right=(0, -14, 0), right_at=(2, 0, 2), left=(8, 34, -4), left_at=(-7, -1, -2))
SCYTHE_GUARD = SPEAR_GUARD
# Char's two-handed swords: both hands together on a long grip, a little right of the middle. All our own moves.
HOLD = pose(right=(0, -18, 0), right_at=(3, 0, 0), left=(-6, 40, -6), left_at=(-8, -2, 1))
LOW = dict(right_at=(4, -14, 6), left_at=(-4, -14, 6))

MOVES = {
    'charpowers_spear_guard': clip([(0, SPEAR_GUARD), (2, shifted(SPEAR_GUARD, right_at=(0, -0.4, 0), left_at=(0, -0.4, 0))), (4, SPEAR_GUARD)], loop=True),
    'charpowers_spear_in': clip([(0, shifted(SPEAR_GUARD, **LOW)), (0.4, SPEAR_GUARD)]),
    'charpowers_spear_thrust': clip(slow([(0, SPEAR_GUARD), (0.12, shifted(SPEAR_GUARD, right_at=(0, 0, 4), left_at=(0, 0, 3))), (0.28, shifted(SPEAR_GUARD, right_at=(0, 1, -16), left_at=(0, 1, -14))),
                                     (0.4, shifted(SPEAR_GUARD, right_at=(0, 1, -16), left_at=(0, 1, -14))), (0.8, SPEAR_GUARD)], 2.0)),
    'charpowers_scythe_guard': clip([(0, SCYTHE_GUARD), (2, shifted(SCYTHE_GUARD, right_at=(0, -0.4, 0), left_at=(0, -0.4, 0))), (4, SCYTHE_GUARD)], loop=True),
    'charpowers_scythe_in': clip([(0, shifted(SCYTHE_GUARD, **LOW)), (0.4, SCYTHE_GUARD)]),
    'charpowers_scythe_reap': clip(slow([(0, SCYTHE_GUARD), (0.12, shifted(SCYTHE_GUARD, right=(6, 36, 0), left=(6, 36, 0), grip=(0, 0, 60))), (0.34, shifted(SCYTHE_GUARD, right=(-4, -60, 0), left=(-4, -60, 0), grip=(0, 0, 60))), (0.8, SCYTHE_GUARD)], 2.0)),
    'charpowers_two_guard': clip([(0, HOLD), (2, shifted(HOLD, right_at=(0, -0.3, 0), left_at=(0, -0.3, 0))), (4, HOLD)], loop=True),
    'charpowers_two_in': clip([(0, shifted(HOLD, **LOW)), (0.4, HOLD)]),
    # right to left, blade laid flat
    'charpowers_two_slash': clip(slow([(0, HOLD), (0.07, shifted(HOLD, right=(6, 44, 0), left=(6, 44, 0), grip=(0, 0, -72))), (0.2, shifted(HOLD, right=(-2, -6, 0), left=(-2, -6, 0), right_at=(0, 0, -4), left_at=(0, 0, -4), grip=(10, 0, -72))),
                                  (0.32, shifted(HOLD, right=(-4, -58, 0), left=(-4, -58, 0), grip=(10, 0, -72))), (0.62, HOLD)], 2.2)),
    # left to right, rising
    'charpowers_two_reverse': clip(slow([(0, HOLD), (0.07, shifted(HOLD, right=(-14, -50, 0), left=(-14, -50, 0), grip=(0, 0, 72))), (0.2, shifted(HOLD, right=(0, 0, 0), left=(0, 0, 0), right_at=(0, 0, -4), left_at=(0, 0, -4), grip=(10, 0, 72))),
                                    (0.32, shifted(HOLD, right=(16, 48, 0), left=(16, 48, 0), grip=(10, 0, 72))), (0.62, HOLD)], 2.2)),
    # up, a beat, and down hard
    'charpowers_two_overhead': clip(slow([(0, HOLD), (0.16, shifted(HOLD, right=(48, 0, 0), left=(48, 0, 0), right_at=(0, 3, 2), left_at=(0, 3, 2), grip=(-30, 0, 0))), (0.26, shifted(HOLD, right=(52, 0, 0), left=(52, 0, 0), right_at=(0, 3, 2), left_at=(0, 3, 2), grip=(-30, 0, 0))),
                                     (0.38, shifted(HOLD, right=(-46, 0, 0), left=(-46, 0, 0), right_at=(0, -3, -4), left_at=(0, -3, -4), grip=(70, 0, 0))), (0.5, shifted(HOLD, right=(-46, 0, 0), left=(-46, 0, 0), right_at=(0, -3, -4), left_at=(0, -3, -4), grip=(70, 0, 0))), (0.8, HOLD)], 2.0)),
    # the hammer: heavy, the head comes up and over, slowly
    'charpowers_hammer_slam': clip(slow([(0, HOLD), (0.34, shifted(HOLD, right=(58, 0, 0), left=(58, 0, 0), right_at=(0, 4, 3), left_at=(0, 4, 3), grip=(-20, 0, 0))), (0.5, shifted(HOLD, right=(62, 0, 0), left=(62, 0, 0), right_at=(0, 4, 3), left_at=(0, 4, 3), grip=(-20, 0, 0))),
                                    (0.64, shifted(HOLD, right=(-40, 0, 0), left=(-40, 0, 0), right_at=(0, -4, -5), left_at=(0, -4, -5), grip=(60, 0, 0))), (0.9, shifted(HOLD, right=(-40, 0, 0), left=(-40, 0, 0), right_at=(0, -4, -5), left_at=(0, -4, -5), grip=(60, 0, 0))), (1.3, HOLD)], 1.5)),
}


def long_weapon(kind, guard, hand_in, attacks):
    return {'kind': kind, 'customAnimation': [{'type': 'idle', 'name': guard}, {'type': 'hand_in', 'var_1': hand_in},
                                              {'type': 'attack', **{f'var_{i + 1}': a for i, a in enumerate(attacks)}, 'critical_hit': attacks[0]}]}


COMPAT = {'itemSpecific': {
    'charpowers:knight_killer': {'kind': 'SWORD'},
    'charpowers:crimson_edge': {'kind': 'SWORD'},
    'charpowers:gravity_blade': {'kind': 'SWORD'},
    'charpowers:shadow_scythe': long_weapon('SWORD', 'charpowers_scythe_guard', 'charpowers_scythe_in', ['charpowers_scythe_reap']) if TWO_HANDED else {'kind': 'SWORD'},
    'charpowers:grand_spear': long_weapon('SPEAR', 'charpowers_spear_guard', 'charpowers_spear_in', ['charpowers_spear_thrust']) if TWO_HANDED else {'kind': 'SPEAR'},
    'charpowers:cocytus_hammer': {'kind': 'MACE'},
    'charpowers:ice_staff': {'kind': 'SPEAR'},
    'charpowers:fork': {'kind': 'SWORD'},
    'charpowers:dagger_of_darkness': {'kind': 'SWORD'},
    'charpowers:golden_kings_long_sword': long_weapon('SWORD', 'charpowers_two_guard', 'charpowers_two_in', ['charpowers_two_slash', 'charpowers_two_reverse', 'charpowers_two_overhead']) if TWO_HANDED else {'kind': 'SWORD'},
    'charpowers:anima_katana': long_weapon('SWORD', 'charpowers_two_guard', 'charpowers_two_in', ['charpowers_two_slash', 'charpowers_two_reverse']) if TWO_HANDED else {'kind': 'SWORD'},
    'charpowers:flame_hammer': long_weapon('MACE', 'charpowers_two_guard', 'charpowers_two_in', ['charpowers_hammer_slam']) if TWO_HANDED else {'kind': 'MACE'},
}}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, separators=(',', ':'))
        f.write('\n')


write(OUT + 'compat/charpowers.json', COMPAT)
if TWO_HANDED:
    write(OUT + 'animations/charpowers.animation.json', {'format_version': '1.8.0', 'animations': MOVES})
elif os.path.exists(OUT + 'animations/charpowers.animation.json'):
    os.remove(OUT + 'animations/charpowers.animation.json')
print('punchy moves made')
