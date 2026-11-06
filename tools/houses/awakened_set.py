# The five better houses the castle quest unlocks: bigger, more detailed. Original designs.
import math

from .base_set import PLASTER, base, chimney, door
from .lib import House, circle_disc, circle_ring

GRANITE = [('stone_bricks', 7), ('andesite', 2), ('cracked_stone_bricks', 1)]
TOWER = [('stone_bricks', 8), ('andesite', 2), ('cracked_stone_bricks', 1), ('mossy_stone_bricks', 1)]
BRICK = [('bricks', 9), ('mud_bricks', 1)]


def toward_centre(dx, dz):
    if abs(dx) > abs(dz):
        return 'west' if dx > 0 else 'east'
    return 'north' if dz > 0 else 'south'


def paper_walls(h, x0, x1, z0, z1, y0, y1, frame, gap=3):
    # post and beam walls with paper screens: a dark rail low down, white panels above, the timber a block proud
    for f, a, b in ((h.face('S', z1), x0, x1), (h.face('N', z0), x0, x1), (h.face('E', x1), z0, z1), (h.face('W', x0), z0, z1)):
        for u in range(a, b + 1):
            post = (u - a) % gap == 0 or u == b
            for y in range(y0, y1 + 1):
                if y == y0:
                    b_ = 'dark_oak_planks'
                elif y <= y0 + 3:
                    b_ = 'white_stained_glass_pane' if not post else PLASTER
                else:
                    b_ = PLASTER
                f.put(u, y, 0, b_)
        for u in range(a - 1, b + 2):
            f.put(u, y0 + 4 if y0 + 4 <= y1 else y1, 1, frame, axis='u')
        for u in range(a, b + 1):
            if (u - a) % gap == 0 or u == b:
                f.post(u, y0, min(y0 + 3, y1), frame)


def pagoda_house():
    h = House('pagoda_house', 'Pagoda House', 1, True)
    frame, roof, wood = 'stripped_dark_oak_log', 'deepslate_tile', 'dark_oak'
    h.yard(-11, -6, 31, 37, nibble=3)
    # a stone terrace, a dark timber verandah on it
    x0, x1, z0, z1 = 0, 20, 0, 16
    h.box(x0, 0, z0, x1, 0, z1, GRANITE)
    h.ring(x0, 1, z0, x1, 1, z1, GRANITE)
    h.ring(x0 + 1, 2, z0 + 1, x1 - 1, 2, z1 - 1, GRANITE)
    h.box(x0 + 2, 1, z0 + 2, x1 - 2, 2, z1 - 2, GRANITE)
    for f, a, b in ((h.face('S', z1), x0, x1), (h.face('N', z0), x0, x1), (h.face('E', x1), z0, z1), (h.face('W', x0), z0, z1)):
        f.wall(a, b, 2, 2, 0, 'stone_brick_stairs', facing='in', half='bottom')
    h.box(x0 + 1, 3, z0 + 1, x1 - 1, 3, z1 - 1, wood + '_slab', type='bottom')
    h.ring(x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, wood + '_fence')
    for x in (9, 10, 11):
        h.clear(x, 4, z1 - 1)
    # first storey
    h.box(4, 3, 4, 16, 3, 12, 'dark_oak_planks')
    paper_walls(h, 4, 16, 4, 12, 4, 10, frame)
    # slim posts holding up the deep eaves
    for x, z in [(x, z) for x in (2, 6, 14, 18) for z in (2, 14)] + [(10, 2), (2, 8), (18, 8)]:
        h.box(x, 4, z, x, 8, z, frame, axis='y')
    for f, a, b in ((h.face('S', 14), 2, 18), (h.face('N', 2), 2, 18), (h.face('E', 18), 2, 14), (h.face('W', 2), 2, 14)):
        f.beam(a, b, 9, frame, d=0)
    # the door in the middle of the south side, nothing in front of it
    F = h.face('S', 12)
    F.door(10, 4, wood + '_door')
    F.clear(10, 4, 1)
    F.clear(10, 5, 1)
    F.put(9, 6, 1, wood + '_stairs', facing='l', half='top')
    F.put(11, 6, 1, wood + '_stairs', facing='r', half='top')
    F.put(10, 6, 1, wood + '_slab', type='top')
    for x in (9, 10, 11):
        h.put(x, 1, 17, 'stone_brick_stairs', facing='north')
        h.put(x, 2, 16, 'stone_brick_stairs', facing='north')
        h.put(x, 3, 15, 'stone_brick_stairs', facing='north')
    # three tiers, each smaller than the last, enough wall showing between them, corners curling up
    h.hip(3, 17, 3, 13, 10, roof, over=3, flare=True, corner_up=True, stop=2)
    h.box(5, 11, 5, 15, 11, 11, 'dark_oak_planks')
    paper_walls(h, 5, 15, 5, 11, 12, 16, frame)
    h.hip(4, 16, 4, 12, 17, roof, over=2, flare=True, corner_up=True, stop=2, trims=False)
    h.box(6, 18, 6, 14, 18, 10, 'dark_oak_planks')
    paper_walls(h, 6, 14, 6, 10, 19, 22, frame)
    top = h.hip(5, 15, 5, 11, 23, roof, over=1, flare=True, corner_up=True, trims=False)
    for y, b, p in ((top + 2, 'dark_oak_fence', {}), (top + 3, 'honeycomb_block', {}), (top + 4, 'chain', {'axis': 'y'}), (top + 5, 'lightning_rod', {'facing': 'up'})):
        h.put(10, y, 8, b, **p)
    # lanterns on the corners of the lowest eaves
    for x, z in ((0, 0), (20, 0), (0, 16), (20, 16)):
        h.put(x, 6, z, 'chain', axis='y')
        h.put(x, 5, z, 'lantern', hanging=True)
    # inside: stairs between the floors
    h.stairs_up(6, 4, 6, 'east', 8, wood, rail='south')
    h.stairs_up(7, 12, 7, 'east', 7, wood, rail='south')
    # the garden: a red gate, a stone walk between two ponds, bamboo and blossom
    for x in (6, 14):
        h.box(x, 1, 33, x, 6, 33, 'stripped_mangrove_log', axis='y')
        h.put(x, 0, 33, 'stone_bricks')
    for x in range(4, 17):
        h.put(x, 7, 33, 'mangrove_planks')
        h.put(x, 8, 33, 'dark_oak_slab', type='bottom')
    for x in range(7, 14):
        h.put(x, 5, 33, 'stripped_mangrove_log', axis='x')
    h.put(10, 6, 33, 'stripped_mangrove_log', axis='y')
    for x, f in ((4, 'west'), (16, 'east')):
        h.put(x, 8, 33, 'dark_oak_stairs', facing=f)
    for x in range(-7, 28):
        for z in range(19, 30):
            if ((x - 3) / 7.0) ** 2 + ((z - 24) / 4.5) ** 2 < 1 or ((x - 17) / 7.0) ** 2 + ((z - 24) / 4.5) ** 2 < 1:
                h.put(x, 0, z, 'water')
                if h.rng.random() < 0.08:
                    h.put(x, 1, z, 'lily_pad')
    for x in range(-8, 29):
        for z in range(18, 31):
            if h.get(x, 0, z) == 'grass_block' and any(h.get(x + a, 0, z + b) == 'water' for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                h.put(x, 0, z, h.pick(['cobblestone', 'mossy_cobblestone', 'andesite', 'gravel']))
    for z in range(18, 37):
        h.put(10, 0, z, 'smooth_stone')
        h.put(9, 0, z, 'gravel')
        h.put(11, 0, z, 'gravel')
    for x, z in ((-8, 26), (-9, 24), (-8, 22), (28, 24), (27, 26)):
        for y in range(1, 7):
            h.put(x, y, z, 'bamboo', age=0, leaves='small' if y < 4 else 'large', stage=0)
    h.small_tree(-6, 1, 8, 'cherry_log', 'cherry_leaves', 5, 3, petals=True)
    h.small_tree(26, 1, 12, 'cherry_log', 'cherry_leaves', 4, 3, petals=True)
    for x, z in ((5, 18), (15, 18), (8, 31), (12, 31)):
        h.stone_lantern(x, 1, z, 'stone_brick')
    return h


def _spiral(h, cx, cz, r, y0, y1, mat):
    # stairs round the inside of a tower, climbing one block per step
    ring = sorted(circle_ring(r), key=lambda c: math.atan2(c[1], c[0]))
    # every step touches the next by a whole side: a diagonal gets a corner step on its inner side
    cells = []
    for i, c in enumerate(ring):
        cells.append(c)
        n = ring[(i + 1) % len(ring)]
        if c[0] != n[0] and c[1] != n[1]:
            cells.append(min(((c[0], n[1]), (n[0], c[1])), key=lambda q: q[0] * q[0] + q[1] * q[1]))
    y = y0
    i = 0
    while y <= y1:
        dx, dz = cells[i % len(cells)]
        nx, nz = cells[(i + 1) % len(cells)]
        sx, sz = nx - dx, nz - dz
        f = ('east' if sx > 0 else 'west') if abs(sx) >= abs(sz) else ('south' if sz > 0 else 'north')
        c = (cx + dx, y, cz + dz)
        h.put(c[0], c[1], c[2], mat + '_stairs', facing=f, half='bottom')
        h.fixtures.add(c)
        h.flights.append(c)
        if y > y0:
            h.put(c[0], y - 1, c[2], mat + '_slab', type='top')
            h.fixtures.add((c[0], y - 1, c[2]))
        i += 1
        y += 1


def wizard_tower():
    h = House('wizard_tower', 'Wizard Tower', 1, True)
    roof, frame = 'purpur', 'stripped_dark_oak_log'
    h.yard(-11, -10, 26, 21, nibble=3)
    cx, cz, r = 0, 0, 6
    for dx, dz in circle_disc(r + 1):
        h.put(cx + dx, 0, cz + dz, TOWER)
    wall_top = 27
    for y in range(1, wall_top + 1):
        for dx, dz in circle_ring(r):
            h.put(cx + dx, y, cz + dz, 'polished_deepslate' if y in (9, 18) else TOWER)
    for dx, dz in circle_disc(r - 1):
        h.put(cx + dx, 1, cz + dz, 'dark_oak_planks')
    # a sloped foot and a corbel at each band
    for dx, dz in circle_ring(r + 1):
        f = toward_centre(dx, dz)
        h.put(cx + dx, 1, cz + dz, TOWER)
        h.put(cx + dx, 2, cz + dz, 'stone_brick_stairs', facing=f, half='bottom')
        for y in (8, 17):
            h.put(cx + dx, y, cz + dz, 'deepslate_brick_stairs', facing=f, half='top')
    # floors, the spiral running round the wall outside them
    for y in (9, 18, 25):
        for dx, dz in circle_disc(r - 2):
            h.put(cx + dx, y, cz + dz, 'dark_oak_planks')
    _spiral(h, cx, cz, r - 1, 2, 25, 'dark_oak')
    # slit windows in the four directions
    for y in (5, 13, 22):
        for (dx, dz) in ((r, 0), (-r, 0), (0, r), (0, -r)):
            x, z = cx + dx, cz + dz
            h.box(x, y, z, x, y + 1, z, 'purple_stained_glass_pane')
            f = toward_centre(dx, dz)
            ox, oz = x + (dx > 0) - (dx < 0), z + (dz > 0) - (dz < 0)
            h.put(ox, y + 2, oz, 'stone_brick_stairs', facing=f, half='top')
            h.put(ox, y - 1, oz, 'stone_brick_stairs', facing=f, half='bottom')
    # the gallery near the top: corbels, a slab walk, an iron rail
    for dx, dz in circle_ring(r + 1) + circle_ring(r + 2):
        f = toward_centre(dx, dz)
        h.put(cx + dx, 19, cz + dz, 'deepslate_brick_stairs', facing=f, half='top')
        h.put(cx + dx, 20, cz + dz, 'deepslate_brick_slab', type='bottom')
    for dx, dz in circle_ring(r + 2):
        h.put(cx + dx, 21, cz + dz, 'iron_bars')
    for dx, dz in ((8, 0), (-8, 0), (0, 8), (0, -8)):
        h.put(cx + dx, 22, cz + dz, 'soul_lantern', hanging=False)
    # a tall witch's hat
    h.cone(cx, cz, r, wall_top + 1, roof, over=2, step=2, body='purpur_block', tip=[('amethyst_block', {}), ('end_rod', {'facing': 'up'})])
    # the front door: a stone arch on the south, steps up to it
    h.clear(0, 2, 7, 0, 3, 7)
    h.put(0, 2, 6, 'dark_oak_door', facing='south', half='lower', hinge='left', open=False)
    h.put(0, 3, 6, 'dark_oak_door', facing='south', half='upper', hinge='left', open=False)
    for x in (-1, 1):
        h.box(x, 1, 7, x, 3, 7, 'stone_bricks')
    h.put(-1, 4, 7, 'stone_brick_stairs', facing='west', half='top')
    h.put(1, 4, 7, 'stone_brick_stairs', facing='east', half='top')
    h.put(0, 4, 7, 'chiseled_stone_bricks')
    h.put(0, 1, 7, 'stone_bricks')
    h.put(0, 1, 8, 'stone_brick_stairs', facing='north')
    # a little solid turret hanging off the west side, for the outline
    tx, tz, tr = -7, -3, 2
    for k, rad in enumerate((1, 2)):
        for dx, dz in circle_ring(rad):
            h.put(tx + dx, 10 + k, tz + dz, 'stone_brick_stairs', facing=toward_centre(dx, dz), half='top')
    for y in range(12, 19):
        for dx, dz in circle_disc(tr):
            h.put(tx + dx, y, tz + dz, TOWER)
    for (dx, dz) in ((-2, 0), (0, 2), (0, -2)):
        h.box(tx + dx, 14, tz + dz, tx + dx, 15, tz + dz, 'purple_stained_glass_pane')
    h.cone(tx, tz, tr, 19, roof, over=1, step=2, body='purpur_block', tip=[('end_rod', {'facing': 'up'})])
    # the cottage leaning on the east side
    x0, x1, z0, z1 = 7, 18, -3, 4
    h.box(x0, 0, z0 - 1, x1 + 1, 0, z1 + 1, TOWER)
    base(h, x0, x1, z0, z1, sides='SNE')
    h.body(x0, x1, z0, z1, PLASTER, [5], 'dark_oak_planks')
    S, N, E = h.face('S', z1), h.face('N', z0), h.face('E', x1)
    for f, a, b, posts in ((S, x0, x1 + 1, (x1 + 1,)), (N, x0, x1 + 1, (x1 + 1,)), (E, z0 - 1, z1 + 1, (z0 - 1, z1 + 1))):
        for u in posts:
            f.post(u, 3, 5, frame)
        f.beam(a, b, 6, frame)
    h.gable(x0, x1 + 1, z0 - 1, z1 + 1, 7, 'x', 'dark_oak', over=1, end=1, fill=PLASTER, fill_planes=(x0, x1))
    door(S, 12, 2, 'dark_oak_door', 'cobblestone')
    S.post(11, 2, 4, frame)
    S.post(13, 2, 4, frame)
    S.put(12, 4, 1, 'dark_oak_stairs', facing='in', half='top')
    S.window(8, 3, 2, 2, pane='yellow_stained_glass_pane', shutter='dark_oak_trapdoor', box='dark_oak_trapdoor', flowers=['allium', 'blue_orchid', 'azure_bluet'])
    S.window(15, 3, 2, 2, pane='yellow_stained_glass_pane', shutter='dark_oak_trapdoor', box='dark_oak_trapdoor', flowers=['allium', 'blue_orchid', 'azure_bluet'])
    E.window(0, 3, 2, 2, pane='yellow_stained_glass_pane', shutter='dark_oak_trapdoor')
    N.window(13, 3, 2, 2, pane='yellow_stained_glass_pane', shutter='dark_oak_trapdoor')
    h.box(x1, 9, 0, x1, 10, 1, 'purple_stained_glass_pane')
    # moss creeping over the old roof
    for (x, y, z), (b, p) in list(h.cells.items()):
        if b == 'dark_oak_stairs' and x > 6 and (x, y, z) in h.roof_owner and h.rng.random() < 0.06 and not h.get(x, y + 1, z):
            h.put(x, y + 1, z, 'moss_carpet')
    h.dormer(12, 4, 9, 'dark_oak', frame, pane='yellow_stained_glass_pane', back=3)
    chimney(h, 16, 17, -2, -1, 10, 15, BRICK, fire='soul_campfire')
    # through to the tower
    h.clear(6, 2, 0, 6, 3, 0)
    h.put(7, 2, 0, 'dark_oak_door', facing='east', half='lower', hinge='left', open=False)
    h.put(7, 3, 0, 'dark_oak_door', facing='east', half='upper', hinge='left', open=False)
    # the garden: a winding path, crystals, a pair of lamps
    for x, z in ((0, 9), (1, 11), (1, 10), (2, 12), (3, 13), (3, 14), (2, 15), (1, 16), (0, 17), (0, 18), (0, 19), (0, 20)):
        h.put(x, 0, z, 'dirt_path')
    for x, z in ((12, 6), (12, 7), (12, 8), (11, 9), (10, 10), (9, 10), (8, 11), (7, 11), (6, 12), (5, 12), (4, 13)):
        h.put(x, 0, z, 'dirt_path')
    h.scatter([(x, 1, z) for x in range(-8, 22) for z in range(9, 19) if not h.get(x, 1, z) and h.get(x, 0, z) == 'grass_block'], ['fern', 'grass', 'allium', 'azure_bluet', 'blue_orchid', 'red_mushroom', 'brown_mushroom'], 0.2)
    for x, z in ((-7, 13), (8, 16), (17, 12)):
        h.put(x, 1, z, 'amethyst_block')
        h.put(x, 2, z, 'amethyst_cluster', facing='up')
    for x in (-3, 3):
        h.box(x, 1, 19, x, 3, 19, 'mossy_stone_bricks')
        h.put(x, 4, 19, 'soul_lantern', hanging=False)
    h.small_tree(-9, 1, 6, 'dark_oak_log', 'azalea_leaves', 4, 2)
    h.small_tree(21, 1, 14, 'dark_oak_log', 'flowering_azalea_leaves', 4, 3)
    return h


def _sash(f, u, y, w=2, h_=3, shutter=True):
    # a tall merchant's window: a white head set in the brick, a thin white sill, dark shutters
    f.window(u, y, w, h_)
    for uu in range(u, u + w):
        f.put(uu, y + h_, 0, 'smooth_quartz')
        f.put(uu, y - 1, 1, 'smooth_quartz_slab', type='top')
    if shutter:
        for yy in range(y, y + h_):
            f.put(u - 1, yy, 1, 'dark_oak_trapdoor', facing='out', open=True, half='bottom')
            f.put(u + w, yy, 1, 'dark_oak_trapdoor', facing='out', open=True, half='bottom')


def canal_house():
    h = House('canal_house', 'Canal Merchant House', 1, True)
    h.flush_ok = True  # a stepped gable stands above the roof on purpose
    roof, trim = 'deepslate_tile', 'smooth_quartz'
    x0, x1, z0, z1 = 0, 12, 0, 15
    h.yard(-6, -5, 30, 18, nibble=2)
    # a solid stone basement, tall brick walls on it, a white band at each floor
    h.box(x0, 0, z0, x1, 2, z1, 'stone_bricks')
    tops = h.body(x0, x1, z0, z1, BRICK, [4, 4, 5], 'spruce_planks', y0=3)
    S, N, E, W = h.face('S', z1), h.face('N', z0), h.face('E', x1), h.face('W', x0)
    for f, a, b in ((S, x0, x1), (N, x0, x1), (E, z0, z1), (W, z0, z1)):
        for y in (3, 8, 13):
            f.wall(a, b, y, y, 0, trim)
        f.wall(a, b, 3, 3, 1, 'stone_brick_slab', type='top')
    wall_top = tops[-1]
    ry = wall_top + 1
    # a steep roof, ridge running back from the street
    h.gable(x0, x1, z0, z1, ry, 'z', roof, over=1, end=0, flare=True, steep=True, barge=False)
    # the stepped gable at both ends, stone coping on every step
    for zz in (z0, z1):
        for x in range(x0, x1 + 1):
            k = min(x - x0, x1 - x)
            top = ry + (k // 2 + 1) * 4 - 1
            for y in range(ry, top + 1):
                h.put(x, y, zz, BRICK)
            h.put(x, top + 1, zz, 'stone_brick_slab', type='bottom')
    # front: a straight stone stoop up to the door
    S.door(6, 4, 'dark_oak_door')
    for x in (5, 6, 7):
        h.put(x, 3, z1 + 1, 'stone_bricks')
        h.box(x, 1, z1 + 1, x, 2, z1 + 1, 'stone_bricks')
        h.put(x, 2, z1 + 2, 'stone_brick_stairs', facing='north')
        h.put(x, 1, z1 + 2, 'stone_bricks')
        h.put(x, 1, z1 + 3, 'stone_brick_stairs', facing='north')
    for x in (4, 8):
        h.box(x, 1, z1 + 1, x, 3, z1 + 1, 'stone_bricks')
        h.box(x, 1, z1 + 2, x, 2, z1 + 2, 'stone_bricks')
        h.put(x, 1, z1 + 3, 'stone_bricks')
        h.put(x, 4, z1 + 1, 'iron_bars')
        h.put(x, 5, z1 + 1, 'lantern', hanging=False)
        h.put(x, 3, z1 + 2, 'iron_bars')
        h.put(x, 2, z1 + 3, 'iron_bars')
    # a white door case
    for x in (5, 7):
        S.post(x, 4, 6, 'quartz_pillar', d=0)
    S.wall(5, 7, 7, 7, 0, trim)
    S.put(6, 6, 0, 'yellow_stained_glass_pane')
    S.wall(4, 8, 7, 7, 1, 'smooth_quartz_slab', type='top')
    # windows
    for u in (2, 9):
        _sash(S, u, 5, 2, 2)
        _sash(S, u, 10, 2, 2)
        _sash(S, u, 15, 2, 3)
    _sash(S, 6, 15, 1, 3, shutter=False)
    # a door on to a little iron balcony in the middle of the first floor
    S.door(6, 9, 'spruce_door')
    S.put(6, 11, 0, 'glass_pane')
    for x in range(4, 9):
        for d in (1, 2):
            S.put(x, 8, d, 'smooth_stone_slab', type='top')
        S.put(x, 7, 1, 'stone_brick_stairs', facing='in', half='top') if x not in (4, 8) else None
        S.put(x, 9, 2, 'iron_bars')
    for d in (1, 2):
        S.put(4, 9, d, 'iron_bars')
        S.put(8, 9, d, 'iron_bars')
    # gable windows and the hoist beam, with its chain and lantern
    S.window(3, ry + 2, 2, 2)
    S.window(8, ry + 2, 2, 2)
    S.window(6, ry + 6, 1, 2)
    for d in range(0, 3):
        S.put(6, ry + 12, d, 'stripped_dark_oak_log', axis='d')
    for y in range(ry + 9, ry + 12):
        S.put(6, y, 2, 'chain', axis='y')
    S.put(6, ry + 8, 2, 'lantern', hanging=True)
    # plain sides and back
    for u in (2, 6, 10):
        for y in (5, 10, 15):
            _sash(N, u, y, 1, 2, shutter=False)
    for f in (W,):
        for u in (2, 7, 12):
            for y in (5, 10, 15):
                _sash(f, u, y, 1, 2, shutter=False)
    for y in (10, 15):
        for u in (4, 8, 12):
            _sash(E, u, y + 4 if False else y, 1, 2, shutter=False) if y > 12 else None
    N.window(5, ry + 2, 3, 2)
    # a chimney on the roof
    chimney(h, 10, 11, 3, 4, ry + 2, ry + 15, BRICK)
    # inside: flights up the back, each coming out level with its floor
    h.stairs_up(2, 4, 1, 'east', 5, 'spruce', rail='south')
    h.stairs_up(10, 9, 1, 'west', 5, 'spruce', rail='south')
    # a warehouse wing to the east, lower, with loading doors
    wx0, wx1, wz0, wz1 = 13, 25, 2, 15
    h.box(wx0, 0, wz0 - 1, wx1 + 1, 0, wz1 + 1, 'stone_bricks')
    base(h, wx0, wx1, wz0, wz1, 'stone_bricks', ledge='stone_brick', sides='SNE')
    h.body(wx0, wx1, wz0, wz1, BRICK, [4, 4], 'spruce_planks')
    WS, WE, WN = h.face('S', wz1), h.face('E', wx1), h.face('N', wz0)
    for f, a, b in ((WS, wx0, wx1), (WE, wz0, wz1), (WN, wx0, wx1)):
        f.wall(a, b, 6, 6, 0, trim)
    h.gable(wx0, wx1, wz0, wz1, 11, 'x', roof, over=1, end=1, flare=True, fill='bricks', fill_planes=(wx0, wx1))
    for cx in (15, 21):
        door(WS, cx, 2, 'spruce_door', 'stone_brick', double=True)
        WS.wall(cx - 1, cx + 2, 4, 4, 0, trim)
        _sash(WS, cx, 8, 2, 2)
    WS.door(19, 7, 'spruce_door') if False else None
    for d in range(0, 3):
        WS.put(18, 10, d, 'stripped_dark_oak_log', axis='d')
    WS.put(18, 9, 2, 'chain', axis='y')
    WS.put(18, 8, 2, 'chain', axis='y')
    WS.put(18, 7, 2, 'hay_block', axis='y')
    WE.window(6, 3, 2, 2)
    WE.window(11, 3, 2, 2)
    _sash(WE, 8, 8, 2, 2)
    WN.window(18, 3, 2, 2)
    h.stairs_up(15, 2, 3, 'east', 5, 'spruce', rail='south')
    # the quay and canal in front: paving, mooring posts, a stone kerb, water
    for x in range(-6, 31):
        for z in range(z1 + 1, z1 + 6):
            if not h.get(x, 0, z) or h.get(x, 0, z) == 'grass_block':
                h.put(x, 0, z, h.pick([('stone_bricks', 5), ('andesite', 2), ('cracked_stone_bricks', 1)]))
        h.put(x, 0, z1 + 6, 'smooth_stone')
        for z in range(z1 + 7, z1 + 12):
            h.put(x, 0, z, 'water')
        h.put(x, 0, z1 + 12, 'stone_bricks')
    for x in (-6, 30):
        for z in range(z1 + 7, z1 + 12):
            h.put(x, 0, z, 'stone_bricks')
    for x in range(-4, 30, 8):
        h.box(x, 1, z1 + 6, x, 2, z1 + 6, 'spruce_fence')
        h.put(x, 3, z1 + 6, 'lantern', hanging=False)
    h.small_tree(-4, 1, 6, 'oak_log', 'oak_leaves', 5, 3)
    h.small_tree(28, 1, 5, 'oak_log', 'oak_leaves', 4, 2)
    return h


def cherry_villa():
    h = House('cherry_villa', 'Cherry Blossom Villa', 1, True)
    frame, wood, roof = 'stripped_cherry_log', 'cherry', 'cherry'
    pane = 'pink_stained_glass_pane'
    h.yard(-8, -6, 35, 37, nibble=3)
    # a two-storey hall at the back, two low wings reaching forward, a garden wall with a moon gate between them
    x0, x1, z0, z1 = 0, 27, 0, 12
    h.box(x0 - 1, 0, z0 - 1, x1 + 1, 0, z1 + 1, 'stone_bricks')
    base(h, x0, x1, z0, z1, 'stone_bricks', ledge='stone_brick')
    h.body(x0, x1, z0, z1, PLASTER, [4, 4], 'cherry_planks')
    S, N, E, W = h.face('S', z1), h.face('N', z0), h.face('E', x1), h.face('W', x0)
    long_posts = (-1, 5, 10, 17, 22, 28)
    for f, a, b, posts in ((S, x0 - 1, x1 + 1, long_posts), (N, x0 - 1, x1 + 1, long_posts), (E, z0 - 1, z1 + 1, (-1, 6, 13)), (W, z0 - 1, z1 + 1, (-1, 6, 13))):
        f.beam(a, b, 6, frame)
        f.beam(a, b, 10, frame)
        for u in posts:
            f.post(u, 3, 5, frame)
            f.post(u, 7, 9, frame)
    wings = ((0, 8), (19, 27))
    wz = 24
    for a, b in wings:
        h.box(a - 1, 0, z1 + 1, b + 1, 0, wz + 1, 'stone_bricks')
        base(h, a, b, z1 + 1, wz, 'stone_bricks', ledge='stone_brick', sides='SEW')
        h.body(a, b, z1, wz, PLASTER, [4], 'cherry_planks')
        WS, WE, WW = h.face('S', wz), h.face('E', b), h.face('W', a)
        for f, l, r in ((WS, a - 1, b + 1), (WE, z1 + 1, wz + 1), (WW, z1 + 1, wz + 1)):
            f.beam(l, r, 5, frame)
            f.post(r, 3, 4, frame)
        for f in (WE, WW):
            f.post(z1 + 6, 3, 4, frame)
        h.face('S', wz).post(a - 1, 3, 4, frame)
        # the wing opens into the hall
        h.clear(a + 1, 2, z1, b - 1, 5, wz - 1)
    # roofs: the hall's slopes on all four sides and curls at the corners, the wings run forward to gables
    h.hip(x0 - 1, x1 + 1, z0 - 1, z1 + 1, 11, roof, over=1, flare=True, corner_up=True, inset=1)
    for a, b in wings:
        h.gable(a - 1, b + 1, z1, wz + 1, 6, 'z', roof, over=1, end=1, flare=True, fill=PLASTER, fill_planes=(wz,))
    # the hall's front door under a verandah, facing the courtyard
    door_u = 13
    S.door(door_u, 2, wood + '_door', double=True)
    for x in (door_u, door_u + 1):
        S.clear(x, 2, 1)
        S.clear(x, 3, 1)
    S.post(door_u - 1, 3, 4, frame)
    S.post(door_u + 2, 3, 4, frame)
    S.beam(door_u - 1, door_u + 2, 5, frame)
    S.window(10, 3, 2, 2, pane=pane)
    S.window(16, 3, 2, 2, pane=pane)
    S.window(12, 8, 4, 2, pane=pane)
    for x in range(9, 19):
        for z in (z1 + 1, z1 + 2, z1 + 3):
            h.put(x, 0, z, 'stone_bricks')
            h.put(x, 1, z, wood + '_slab', type='top')
        for z, y in ((z1 + 4, 4), (z1 + 3, 5), (z1 + 2, 6), (z1 + 1, 7)):
            h.put(x, y, z, roof + '_stairs', facing='north')
        h.put(x, 4, z1 + 3, frame, axis='x')
        h.clear(x, 2, z1 + 1)
    for x in (9, 12, 15, 18):
        h.box(x, 2, z1 + 3, x, 3, z1 + 3, wood + '_fence')
    for x in (door_u, door_u + 1):
        h.put(x, 1, z1 + 4, 'stone_brick_stairs', facing='north')
    # windows: pink glass on every wall, kept between the timbers
    for u, w in ((1, 3), (7, 2), (12, 4), (19, 2), (24, 3)):
        N.window(u, 3, w, 2, pane=pane)
        N.window(u, 8, w, 2, pane=pane)
    for f in (E, W):
        for u, w in ((1, 3), (8, 3)):
            f.window(u, 3, w, 2, pane=pane)
            f.window(u, 8, w, 2, pane=pane)
    for a, b in wings:
        WS, WE, WW = h.face('S', wz), h.face('E', b), h.face('W', a)
        WS.window(a + 3, 3, 3, 2, pane=pane)
        h.box(a + 3, 8, wz, a + 5, 8, wz, pane)
        outer, inner = (WW, WE) if a == 0 else (WE, WW)
        outer.window(z1 + 2, 3, 3, 2, pane=pane)
        outer.window(z1 + 7, 3, 3, 2, pane=pane)
        inner.window(z1 + 8, 3, 2, 2, pane=pane)
        inner.door(z1 + 6, 2, wood + '_door')
        inner.clear(z1 + 6, 2, 1)
        inner.clear(z1 + 6, 3, 1)
        inner.put(z1 + 6, 1, 1, 'stone_brick_stairs', facing='in')
    # the moon gate: a wall across the front of the courtyard with a round hole, logs round the edge
    gx, gy, gr = 13.5, 3.8, 3.3
    for x in range(9, 19):
        h.put(x, 0, wz, 'stone_bricks')
        for y in range(1, 7):
            d = math.hypot(x - gx, y - gy)
            if d < gr:
                continue
            h.put(x, y, wz, frame if d < gr + 1 else PLASTER, axis='z') if d < gr + 1 else h.put(x, y, wz, PLASTER)
        h.put(x, 7, wz, 'dark_oak_slab', type='bottom')
        h.put(x, 6, wz + 1, roof + '_stairs', facing='north')
        h.put(x, 6, wz - 1, roof + '_stairs', facing='south')
    # courtyard: a stone walk, ponds either side, two small trees
    for z in range(z1 + 5, 38):
        h.put(13, 0, z, 'smooth_stone')
        h.put(14, 0, z, 'smooth_stone')
    for x in range(9, 19):
        for z in range(z1 + 5, wz):
            if x in (13, 14):
                continue
            if ((x - 13.5) / 5.2) ** 2 + ((z - 20) / 3.2) ** 2 < 1:
                h.put(x, 0, z, 'water')
                if h.rng.random() < 0.15:
                    h.put(x, 1, z, 'lily_pad')
    h.stone_lantern(10, 1, z1 + 5, 'stone_brick')
    h.stone_lantern(17, 1, z1 + 5, 'stone_brick')
    # outside: the walk up to the gate, two big blossom trees, petals on the grass
    h.small_tree(5, 1, 31, 'cherry_log', 'cherry_leaves', 6, 4, petals=True)
    h.small_tree(23, 1, 31, 'cherry_log', 'cherry_leaves', 6, 4, petals=True)
    h.small_tree(32, 1, 6, 'cherry_log', 'cherry_leaves', 5, 3, petals=True)
    h.stone_lantern(11, 1, 28, 'stone_brick')
    h.stone_lantern(16, 1, 28, 'stone_brick')
    # inside
    h.stairs_up(10, 2, 1, 'east', 5, wood, rail='south')
    return h


def _round_tower(h, cx, cz, r, y1, keep_out):
    # a round tower. Where it overlaps the hall, the hall's own walls inside it go; its foot stays out of the hall's rooms.
    ax0, ax1, az0, az1 = keep_out
    for dx, dz in circle_disc(r):
        h.put(cx + dx, 0, cz + dz, TOWER)
        h.put(cx + dx, 1, cz + dz, 'dark_oak_planks')
    for dx, dz in circle_disc(r - 1):
        h.clear(cx + dx, 2, cz + dz, cx + dx, y1 + 2, cz + dz)
    for y in range(1, y1 + 1):
        for dx, dz in circle_ring(r):
            h.put(cx + dx, y, cz + dz, 'smooth_stone' if y == 7 else TOWER)
    for dx, dz in circle_ring(r + 1):
        x, z = cx + dx, cz + dz
        f = toward_centre(dx, dz)
        if not (ax0 <= x <= ax1 and az0 <= z <= az1):
            h.put(x, 0, z, TOWER)
            h.put(x, 1, z, TOWER)
            h.put(x, 2, z, 'stone_brick_stairs', facing=f, half='bottom')
        if not (ax0 - 1 <= x <= ax1 + 1 and az0 - 1 <= z <= az1 + 1):
            h.put(x, y1 - 1, z, 'stone_brick_stairs', facing=f, half='top')
    for y in (4, 10, 15):
        for (dx, dz) in ((r, 0), (-r, 0), (0, r), (0, -r)):
            x, z = cx + dx, cz + dz
            if y < y1 - 2 and not (ax0 <= x <= ax1 and az0 <= z <= az1):
                h.box(x, y, z, x, y + 1, z, 'light_blue_stained_glass_pane')
    h.cone(cx, cz, r, y1 + 1, 'deepslate_tile', over=2, step=2, body='deepslate_tiles', tip=[('dark_oak_fence', {}), ('lightning_rod', {'facing': 'up'})])


def towered_manor():
    h = House('towered_manor', 'Manor with Towers', 1, True)
    roof, stone = 'deepslate_tile', 'stone_brick'
    x0, x1, z0, z1 = 0, 31, 0, 14
    h.yard(-10, -8, 41, 35, nibble=3)
    h.box(x0, 0, z0, x1, 0, z1, GRANITE)
    base(h, x0, x1, z0, z1, GRANITE, ledge=stone)
    h.body(x0, x1, z0, z1, GRANITE, [5, 5], 'dark_oak_planks')
    S, N, E, W = h.face('S', z1), h.face('N', z0), h.face('E', x1), h.face('W', x0)

    def lancet(f, u, y, h_=3, d=0):
        # two tall lights with a pointed head and a stone sill
        for yy in range(y, y + h_):
            f.put(u, yy, d, 'light_blue_stained_glass_pane')
            f.put(u + 1, yy, d, 'light_blue_stained_glass_pane')
        f.put(u, y + h_, d, stone + '_stairs', facing='l', half='top')
        f.put(u + 1, y + h_, d, stone + '_stairs', facing='r', half='top')
        f.put(u, y - 1, d + 1, stone + '_stairs', facing='in', half='bottom')
        f.put(u + 1, y - 1, d + 1, stone + '_stairs', facing='in', half='bottom')

    def buttress(f, u):
        f.post(u, 2, 5, 'stone_bricks')
        f.put(u, 6, 1, stone + '_stairs', facing='in')
        f.post(u, 8, 10, 'stone_bricks')
        f.put(u, 11, 1, stone + '_stairs', facing='in')

    for f, a, b in ((S, x0, x1), (N, x0, x1), (E, z0, z1), (W, z0, z1)):
        f.wall(a, b, 7, 7, 0, 'smooth_stone')
        f.wall(a, b, 12, 12, 1, stone + '_stairs', facing='in', half='top')
    for u in (6, 25):
        buttress(S, u)
    for u in (6, 11, 20, 25):
        buttress(N, u)
    for f in (E, W):
        buttress(f, 7)
    for u in (8, 22):
        lancet(S, u, 3)
        lancet(S, u, 8)
    for u in (8, 13, 17, 22):
        lancet(N, u, 3)
        lancet(N, u, 8)
    for f in (E, W):
        lancet(f, 4, 3)
        lancet(f, 4, 8)
        lancet(f, 9, 3)
        lancet(f, 9, 8)
    h.gable(x0, x1, z0, z1, 13, 'x', roof, over=1, end=1, flare=True, fill='stone_bricks', fill_planes=(x0, x1))
    for cx in (6, 25):
        h.dormer(cx, z1 - 1, 15, roof, 'stone_bricks', pane='light_blue_stained_glass_pane', tall=2, back=3)
    # the gatehouse in the middle: its doors right on the front, under a pointed stone arch
    g0, g1, gz = 12, 19, z1 + 4
    h.box(g0 - 1, 0, z1 + 1, g1 + 1, 0, gz + 1, GRANITE)
    base(h, g0, g1, z1 + 1, gz, GRANITE, ledge=stone, sides='SEW')
    h.body(g0, g1, z1, gz, GRANITE, [5, 5], 'dark_oak_planks')
    h.clear(g0 + 1, 2, z1, g1 - 1, 6, gz - 1)
    h.clear(g0 + 1, 8, z1, g1 - 1, 12, gz - 1)
    G = h.face('S', gz)
    G.wall(g0, g1, 7, 7, 0, 'smooth_stone')
    G.wall(g0 - 1, g1 + 1, 12, 12, 1, stone + '_stairs', facing='in', half='top')
    h.gable(g0, g1, 9, gz, 13, 'z', roof, over=1, end=1, flare=True, fill='stone_bricks', fill_planes=(gz,))
    G.door(15, 2, 'dark_oak_door', double=True)
    for x in (15, 16):
        for y in (2, 3):
            G.clear(x, y, 1)
        G.put(x, 1, 1, stone + '_stairs', facing='in')
        h.put(x, 1, gz + 2, stone + '_slab', type='bottom')
    G.post(14, 1, 4, 'smooth_stone')
    G.post(17, 1, 4, 'smooth_stone')
    G.put(15, 4, 1, stone + '_stairs', facing='l', half='top')
    G.put(16, 4, 1, stone + '_stairs', facing='r', half='top')
    G.put(14, 5, 1, stone + '_stairs', facing='l', half='top')
    G.put(17, 5, 1, stone + '_stairs', facing='r', half='top')
    G.put(15, 5, 1, 'smooth_stone')
    G.put(16, 5, 1, 'smooth_stone')
    G.wall(15, 16, 6, 6, 1, stone + '_slab', type='bottom')
    lancet(G, 13, 8)
    lancet(G, 17, 8)
    G.wall(15, 16, 9, 11, 0, 'light_blue_stained_glass_pane')
    G.wall(15, 16, 14, 15, 0, 'light_blue_stained_glass_pane')
    for x in (15, 16):
        h.put(x, 18, gz + 1, 'dark_oak_fence')
    h.put(15, 19, gz + 1, 'lightning_rod', facing='up')
    # four round towers, the front pair bigger
    inner = (x0 + 1, x1 - 1, z0 + 1, z1 - 1)
    _round_tower(h, x0 - 1, z1 + 1, 4, 19, inner)
    _round_tower(h, x1 + 1, z1 + 1, 4, 19, inner)
    _round_tower(h, x0 - 1, z0 - 1, 3, 15, inner)
    _round_tower(h, x1 + 1, z0 - 1, 3, 15, inner)
    # a way through from the hall into each tower
    for x, z in ((2, 13), (3, 13), (29, 13), (28, 13), (1, 1), (30, 1)):
        h.clear(x, 2, z, x, 3, z)
    # chimneys
    for cx in (10, 20):
        chimney(h, cx, cx + 1, 6, 8, 19, 25, GRANITE)
    # inside: two dividing walls with doorways, a wide stair
    for wx in (11, 20):
        for y in list(range(2, 7)) + list(range(8, 13)):
            h.box(wx, y, z0 + 1, wx, y, z1 - 1, 'dark_oak_planks')
        h.clear(wx, 2, 7, wx, 3, 8)
        h.clear(wx, 8, 7, wx, 9, 8)
    h.stairs_up(13, 2, 2, 'east', 6, 'dark_oak', wide=2, rail='south')
    # a paved terrace with a low wall, a gravel walk through a hedge
    for x in range(5, 27):
        for z in range(z1 + 2, 27):
            if not h.get(x, 0, z) or h.get(x, 0, z) == 'grass_block':
                h.put(x, 0, z, 'stone_bricks' if (x + z) % 2 else 'polished_andesite')
    for x in range(5, 27):
        if not 13 < x < 18:
            h.put(x, 1, 27, 'stone_brick_wall')
    for z in range(z1 + 6, 28):
        for x in (4, 27):
            h.put(x, 1, z, 'stone_brick_wall')
    for x in (13, 18):
        h.box(x, 1, 27, x, 2, 27, 'stone_bricks')
        h.put(x, 3, 27, 'lantern', hanging=False)
    for z in range(27, 36):
        for x in (15, 16):
            h.put(x, 0, z, 'gravel')
    for x in list(range(-6, 13)) + list(range(19, 38)):
        h.put(x, 1, 31, 'oak_leaves', persistent=True)
    h.small_tree(-7, 1, 26, 'dark_oak_log', 'oak_leaves', 5, 3)
    h.small_tree(38, 1, 26, 'dark_oak_log', 'oak_leaves', 5, 3)
    return h


ALL = [pagoda_house, wizard_tower, canal_house, cherry_villa, towered_manor]
