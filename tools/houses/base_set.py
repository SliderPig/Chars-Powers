# The five houses you earn by mastery: levels 1, 25, 50, 75, 100. Original designs.
# A few tricks done well: pale walls you can actually see, timber standing proud of them, a stone base, roofs with a real overhang.
from .lib import House, circle_disc, circle_ring

STONE = [('cobblestone', 5), ('stone', 2), ('andesite', 2), ('mossy_cobblestone', 1)]
SMOOTH = [('stone_bricks', 7), ('andesite', 2), ('cracked_stone_bricks', 1)]
GARDEN = ['poppy', 'dandelion', 'cornflower', 'azure_bluet', 'oxeye_daisy', 'allium', 'pink_tulip', 'fern', 'grass']
BRICK = [('bricks', 9), ('mud_bricks', 1)]
PLASTER = 'calcite'


def base(h, x0, x1, z0, z1, stone=STONE, ledge='cobblestone', high=1, sides='SNEW'):
    # a low stone base a block proud of the walls, sloping back to them
    faces = {'S': (h.face('S', z1), x0 - 1, x1 + 1), 'N': (h.face('N', z0), x0 - 1, x1 + 1), 'E': (h.face('E', x1), z0 - 1, z1 + 1), 'W': (h.face('W', x0), z0 - 1, z1 + 1)}
    for side in sides:
        f, a, b = faces[side]
        f.wall(a, b, 0, high, 1, stone)
        f.wall(a, b, high + 1, high + 1, 1, ledge + '_stairs', facing='in', half='bottom')


def door(f, u, y, kind, stair, double=False):
    # a door in the wall itself, nothing in front of it but a step
    f.door(u, y, kind, double=double)
    for uu in ([u, u + 1] if double else [u]):
        f.clear(uu, y, 1)
        f.clear(uu, y + 1, 1)
        f.put(uu, y - 1, 1, stair + '_stairs', facing='in')


def chimney(h, x0, x1, z0, z1, y0, y1, block, band='stone_bricks', fire='campfire'):
    h.box(x0, y0, z0, x1, y1, z1, block)
    h.box(x0, y1 - 1, z0, x1, y1 - 1, z1, band)
    h.put(x0, y1 + 1, z0, fire, lit=True, signal_fire=False, facing='south')
    slab = 'stone_brick_slab'
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) != (x0, z0):
                h.put(x, y1 + 1, z, slab, type='bottom')


def cosy_cottage():
    h = House('cosy_cottage', 'Cosy Cottage', 1)
    frame, wood, roof = 'stripped_spruce_log', 'spruce', 'dark_oak'
    x0, x1, z0, z1 = 0, 10, 0, 7
    h.yard(-4, -3, 15, 15)
    h.box(x0, 0, z0, x1, 0, z1, STONE)
    base(h, x0, x1, z0, z1)
    h.body(x0, x1, z0, z1, PLASTER, [4], 'spruce_planks')
    S, N, E, W = h.face('S', z1), h.face('N', z0), h.face('E', x1), h.face('W', x0)
    # timber only where it does something: the corners and the plate under the roof
    for f, a, b in ((S, x0 - 1, x1 + 1), (N, x0 - 1, x1 + 1), (E, z0 - 1, z1 + 1), (W, z0 - 1, z1 + 1)):
        f.post(a, 3, 4, frame)
        f.post(b, 3, 4, frame)
        f.beam(a, b, 5, frame)
    h.box(x0, 6, z0, x1, 6, z1, 'spruce_planks')
    h.gable(x0 - 1, x1 + 1, z0 - 1, z1 + 1, 6, 'x', roof, over=1, end=1, fill=PLASTER, fill_planes=(x0, x1))
    # a little gabled porch over the front door
    dx = 3
    S.door(dx, 2, 'spruce_door')
    for y in (1, 2, 3, 4):
        S.clear(dx, y, 1)
    S.post(dx - 1, 1, 4, frame)
    S.post(dx + 1, 1, 4, frame)
    h.box(dx - 1, 1, z1 + 1, dx + 1, 1, z1 + 3, 'spruce_planks')
    h.put(dx, 1, z1 + 4, 'cobblestone_stairs', facing='north')
    for x in (dx - 1, dx + 1):
        h.box(x, 2, z1 + 3, x, 3, z1 + 3, wood + '_fence')
    h.put(dx - 1, 4, z1 + 3, wood + '_stairs', facing='west', half='top')
    h.put(dx + 1, 4, z1 + 3, wood + '_stairs', facing='east', half='top')
    h.put(dx, 4, z1 + 3, wood + '_slab', type='top')
    h.gable(dx - 1, dx + 1, z1 + 1, z1 + 3, 5, 'z', wood, over=1, end=0, fill=frame, fill_planes=(z1 + 3,), barge=False, ridge_cap=False)
    # windows: the front one gets a flower box and shutters, the others stay plain
    S.window(7, 3, 2, 2, shutter='spruce_trapdoor', box='spruce_trapdoor')
    for u in (2, 7):
        N.window(u, 3, 2, 2)
    E.window(4, 3, 2, 2, shutter='spruce_trapdoor')
    W.window(4, 3, 2, 2, shutter='spruce_trapdoor')
    for f in (E, W):
        f.put(3, 8, 0, 'glass_pane')
        f.put(4, 8, 0, 'glass_pane')
    h.dormer(8, 7, 8, roof, frame, back=3)
    # upstairs: a flight up the west end, coming out under the ridge
    h.stairs_up(1, 2, 6, 'north', 5, wood, rail='east')
    # the chimney on the east end
    h.box(12, 0, 1, 13, 0, 2, STONE)
    chimney(h, 12, 13, 1, 2, 1, 13, BRICK)
    h.box(12, 1, 1, 13, 5, 2, STONE)
    # the garden: a path, a low hedge with a gate, flowers against the house
    for z in range(z1 + 5, 16):
        h.put(dx, 0, z, 'dirt_path')
    for x in range(-3, 15):
        if abs(x - dx) > 1:
            h.put(x, 1, 14, 'oak_leaves', persistent=True)
    h.put(dx, 1, 14, wood + '_fence_gate', facing='south')
    for x in (dx - 1, dx + 1):
        h.lamp(x, 1, 14, wood + '_fence', wood + '_slab', 1)
    h.scatter([(x, 1, z) for x in range(6, 12) for z in (z1 + 3, z1 + 4)], GARDEN, 0.75)
    h.scatter([(x, 1, z1 + 2) for x in (-2, 0)], ['rose_bush', 'peony', 'lilac'], 1.0)
    return h


def oak_timber_house():
    h = House('oak_timber_house', 'Oak Timber House', 25)
    frame, wood, roof = 'stripped_dark_oak_log', 'dark_oak', 'spruce'
    h.yard(-5, -4, 17, 20)
    # ground floor in stone, the upper floor sails out a block over it on brackets
    x0, x1, z0, z1 = 1, 11, 1, 10
    h.box(x0 - 1, 0, z0 - 1, x1 + 1, 0, z1 + 1, STONE)
    base(h, x0, x1, z0, z1)
    h.body(x0, x1, z0, z1, SMOOTH, [4], 'oak_planks')
    GS, GN, GE, GW = h.face('S', z1), h.face('N', z0), h.face('E', x1), h.face('W', x0)
    for f, a, b in ((GS, x0, x1), (GN, x0, x1), (GE, z0, z1), (GW, z0, z1)):
        for u in range(a + 1, b, 3):
            f.put(u, 5, 1, wood + '_stairs', facing='in', half='top')
    ux0, ux1, uz0, uz1 = 0, 12, 0, 11
    h.box(ux0, 6, uz0, ux1, 6, uz1, 'oak_planks')
    h.ring(ux0, 7, uz0, ux1, 10, uz1, PLASTER)
    US, UN, UE, UW = h.face('S', uz1), h.face('N', uz0), h.face('E', ux1), h.face('W', ux0)
    for f, a, b, posts in ((US, ux0 - 1, ux1 + 1, (-1, 3, 9, 13)), (UN, ux0 - 1, ux1 + 1, (-1, 3, 9, 13)), (UE, uz0 - 1, uz1 + 1, (-1, 5, 12)), (UW, uz0 - 1, uz1 + 1, (-1, 5, 12))):
        f.beam(a, b, 6, frame)
        f.beam(a, b, 10, frame)
        for u in posts:
            f.post(u, 7, 9, frame)
    h.box(x0, 11, z0, x1, 11, z1, 'oak_planks')
    h.gable(ux0 - 1, ux1 + 1, uz0 - 1, uz1 + 1, 11, 'z', roof, over=1, end=1, fill=PLASTER, fill_planes=(uz0, uz1))
    # the gables: a window under a tie beam and a king post
    for z, plane in ((uz1 + 1, uz1), (uz0 - 1, uz0)):
        h.box(3, 14, z, 9, 14, z, frame, axis='x')
        h.box(6, 15, z, 6, 17, z, frame, axis='y')
        h.box(4, 11, z, 4, 13, z, frame, axis='y')
        h.box(8, 11, z, 8, 13, z, frame, axis='y')
        h.box(5, 12, plane, 7, 13, plane, 'glass_pane')
    # front door in a stone surround under the jetty
    door(GS, 6, 2, 'spruce_door', 'stone_brick')
    h.put(6, 1, z1 + 2, 'stone_brick_slab', type='bottom')
    GS.post(5, 2, 4, 'stone_bricks')
    GS.post(7, 2, 4, 'stone_bricks')
    GS.put(6, 4, 1, 'stone_brick_stairs', facing='in', half='top')
    GS.window(2, 3, 2, 2, shutter='spruce_trapdoor', box='spruce_trapdoor')
    GS.window(9, 3, 2, 2, shutter='spruce_trapdoor', box='spruce_trapdoor')
    # upper windows sit between the posts
    for u, w in ((1, 2), (5, 3), (10, 2)):
        US.window(u, 8, w, 2)
        UN.window(u, 8, w, 2)
    for f in (UE, UW):
        for u, w in ((1, 3), (7, 3)):
            f.window(u, 8, w, 2)
    GE.window(3, 3, 2, 2)
    GE.window(7, 3, 2, 2)
    GW.window(7, 3, 2, 2)
    GN.window(3, 3, 2, 2)
    door(GN, 8, 2, 'spruce_door', 'cobblestone')
    # a slim brick chimney on the west side, tied to the wall
    h.box(-2, 0, 2, -1, 0, 3, STONE)
    chimney(h, -2, -1, 2, 3, 1, 19, BRICK)
    h.box(0, 2, 2, 0, 5, 3, BRICK)
    # inside: two real flights
    h.stairs_up(2, 2, 9, 'north', 5, wood, rail='east')
    h.stairs_up(8, 7, 8, 'north', 5, wood, rail='west')
    # garden: a straight path, a low stone wall, one good tree
    for z in range(z1 + 3, 21):
        h.put(6, 0, z, 'dirt_path')
    for x in list(range(-3, 5)) + list(range(8, 16)):
        h.put(x, 1, 18, 'cobblestone_wall')
    for x in (5, 7):
        h.box(x, 1, 18, x, 2, 18, 'cobblestone_wall')
        h.put(x, 3, 18, 'lantern', hanging=False)
    h.scatter([(x, 1, z) for x in list(range(1, 5)) + list(range(8, 12)) for z in (14, 15)], GARDEN, 0.7)
    h.small_tree(14, 1, 15, 'oak_log', 'oak_leaves', 4, 3)
    return h


def mountain_lodge():
    h = House('mountain_lodge', 'Mountain Lodge', 50)
    log, frame, wood, roof = 'spruce_log', 'stripped_spruce_log', 'spruce', 'deepslate_tile'
    x0, x1, z0, z1 = 0, 18, 0, 12
    h.yard(-6, -4, 23, 25)
    h.box(x0, 0, z0, x1, 0, z1, STONE)
    base(h, x0, x1, z0, z1, STONE)
    # stone below, dark logs laid along the walls above
    h.body(x0, x1, z0, z1, [SMOOTH, log], [4, 4], 'spruce_planks')
    # the great gable: a wing on the front, glass up to the peak
    ax0, ax1, zf = 5, 13, z1 + 4
    h.box(ax0 - 1, 0, z1 + 1, ax1 + 1, 0, zf + 1, STONE)
    base(h, ax0, ax1, z1 + 1, zf, STONE, sides='SEW')
    h.body(ax0, ax1, z1, zf, [SMOOTH, log], [4, 4], 'spruce_planks')
    for f, a, b, along in ((h.face('S', z1), x0, x1, 'x'), (h.face('N', z0), x0, x1, 'x'), (h.face('E', x1), z0, z1, 'z'), (h.face('W', x0), z0, z1, 'z'),
                           (h.face('S', zf), ax0, ax1, 'x'), (h.face('E', ax1), z1, zf, 'z'), (h.face('W', ax0), z1, zf, 'z')):
        for u in range(a, b + 1):
            for y in range(7, 11):
                f.put(u, y, 0, log, axis=along)
        f.post(a, 7, 10, frame, d=0)
        f.post(b, 7, 10, frame, d=0)
    # one space with the main house behind it
    h.clear(ax0 + 1, 2, z1, ax1 - 1, 5, zf - 1)
    h.clear(ax0 + 1, 7, z1, ax1 - 1, 10, zf - 1)
    h.gable(x0, x1, z0, z1, 11, 'x', roof, over=2, end=2, fill=log, fill_planes=(x0, x1))
    h.gable(ax0, ax1, z1 - 4, zf, 11, 'z', roof, over=2, end=2, fill=frame, fill_planes=(zf,))
    F = h.face('S', zf)
    # glass wall: tall lights between timbers
    for x in range(ax0 + 1, ax1):
        for y in range(7, 15):
            if y >= 11 and y - 11 >= min(x - ax0, ax1 - x):
                continue
            if x in (ax0 + 2, 9, ax1 - 2):
                h.put(x, y, zf, frame, axis='y')
            elif y == 11:
                h.put(x, y, zf, frame, axis='x')
            else:
                h.put(x, y, zf, 'glass_pane')
    # the front door in a stone frame, the balcony over it for a porch
    door(F, 9, 2, wood + '_door', 'stone_brick')
    h.put(9, 1, zf + 2, 'stone_brick_slab', type='bottom')
    F.post(8, 1, 4, 'stone_bricks')
    F.post(10, 1, 4, 'stone_bricks')
    F.put(9, 4, 1, 'stone_brick_stairs', facing='in', half='top')
    F.window(ax0 + 1, 3, 2, 2)
    F.window(ax1 - 2, 3, 2, 2)
    # balcony across the front on brackets, its own door from upstairs
    for x in range(ax0, ax1 + 1):
        F.put(x, 6, 1, wood + '_slab', type='top')
        F.put(x, 6, 2, wood + '_slab', type='top')
        F.put(x, 7, 2, wood + '_fence')
    for d in (1, 2):
        F.put(ax0, 7, d, wood + '_fence')
        F.put(ax1, 7, d, wood + '_fence')
    for x in (ax0, ax0 + 2, ax1 - 2, ax1):
        F.put(x, 5, 1, wood + '_stairs', facing='in', half='top')
    F.door(9, 7, wood + '_door')
    F.put(9, 9, 0, 'glass_pane')
    # windows
    S2, N2 = h.face('S', z1), h.face('N', z0)
    for u in (2, 15):
        S2.window(u, 3, 2, 2, head='stone_brick')
        S2.window(u, 8, 2, 2, shutter=wood + '_trapdoor')
    for u in (3, 8, 14):
        N2.window(u, 3, 2, 2, head='stone_brick')
    for u in (8, 14):
        N2.window(u, 8, 2, 2, shutter=wood + '_trapdoor')
    for f in (h.face('E', x1), h.face('W', x0)):
        for u in (3, 8):
            f.window(u, 3, 2, 2, head='stone_brick')
            f.window(u, 8, 2, 2, shutter=wood + '_trapdoor')
        f.window(5, 12, 3, 2)
    # a stone chimney up the west end
    h.box(-3, 0, 8, -1, 0, 10, STONE)
    h.box(-3, 1, 8, -1, 11, 10, SMOOTH)
    chimney(h, -3, -2, 8, 9, 12, 20, SMOOTH)
    # inside
    h.stairs_up(2, 2, 2, 'east', 5, wood, wide=2, rail='south')
    # outside: a path, two firs, lamps
    for z in range(zf + 3, 26):
        h.put(9, 0, z, 'dirt_path')
    for x, z in ((2, 21), (16, 22)):
        h.small_tree(x, 1, z, 'spruce_log', 'spruce_leaves', 5, 2)
    for x, z in ((6, 21), (12, 21)):
        h.stone_lantern(x, 1, z)
    h.scatter([(x, 1, z) for x in range(-3, 21) for z in range(19, 24) if abs(x - 9) > 1 and not h.get(x, 1, z) and h.get(x, 0, z) == 'grass_block'], ['fern', 'grass', 'fern', 'cornflower'], 0.25)
    return h


def farmhouse():
    h = House('farmhouse', 'Farmhouse with Barn', 75)
    white, trim, wood, roof = 'smooth_quartz', 'stripped_spruce_log', 'spruce', 'spruce'
    h.yard(-5, -6, 40, 27)
    # the house: white walls, dark corners, a band at the floor line
    x0, x1, z0, z1 = 0, 12, 0, 9
    h.box(x0 - 1, 0, z0 - 1, x1, 0, z1 + 1, STONE)
    base(h, x0, x1, z0, z1, sides='NSW')
    h.body(x0, x1, z0, z1, white, [4, 4], 'oak_planks')
    S, N, W = h.face('S', z1), h.face('N', z0), h.face('W', x0)
    for f, a, b in ((S, x0, x1), (N, x0, x1), (W, z0, z1)):
        f.post(a, 2, 10, trim, d=0)
        f.post(b, 2, 10, trim, d=0)
        f.beam(a, b, 6, trim, d=0)
    h.gable(x0 - 1, x1, z0 - 1, z1 + 1, 11, 'x', roof, over=1, end=1, fill=white, fill_planes=(x0, x1))
    # a verandah across the front, its roof meeting the wall under the upstairs windows
    for x in range(x0 - 1, x1 + 1):
        for z in range(z1 + 1, z1 + 4):
            h.put(x, 0, z, STONE)
            h.put(x, 1, z, 'oak_planks')
    h.clear(x0 - 1, 2, z1 + 1, x1, 2, z1 + 1)
    for x in range(x0 - 2, x1 + 1):
        for y, z in ((4, z1 + 4), (5, z1 + 3), (6, z1 + 2), (7, z1 + 1)):
            h.put(x, y, z, wood + '_stairs', facing='north')
    for x in range(x0 - 1, x1 + 1):
        h.put(x, 4, z1 + 3, trim, axis='x')
        if x not in (5, 6):
            h.put(x, 2, z1 + 3, 'spruce_fence')
    for x in (x0 - 1, 4, 7, x1):
        h.box(x, 2, z1 + 3, x, 3, z1 + 3, 'spruce_fence')
    for z in (z1 + 1, z1 + 2):
        h.put(x0 - 1, 2, z, 'spruce_fence')
    for y, z in ((5, z1 + 2), (5, z1 + 1), (6, z1 + 1)):
        h.put(x0 - 1, y, z, 'spruce_planks')
    for x in (5, 6):
        h.put(x, 1, z1 + 4, 'oak_stairs', facing='north')
    S.door(5, 2, 'spruce_door', double=True)
    S.window(1, 3, 2, 2, shutter='spruce_trapdoor')
    S.window(9, 3, 2, 2, shutter='spruce_trapdoor')
    for u in (2, 6, 10):
        S.window(u, 8, 2, 2)
    for u in (8, 10):
        N.window(u, 3, 1, 2)
    for u in (2, 6, 10):
        N.window(u, 8, 2, 2, shutter='spruce_trapdoor')
    W.window(3, 3, 3, 2, shutter='spruce_trapdoor')
    W.window(3, 8, 3, 2, shutter='spruce_trapdoor')
    for cx in (3, 9):
        h.dormer(cx, 9, 13, roof, white, back=3)
    chimney(h, 6, 7, 2, 3, 14, 20, BRICK)
    h.stairs_up(2, 2, 1, 'east', 5, wood, rail='south')
    # the barn: red boards with white trim, a tall barn roof
    bx0, bx1, bz0, bz1 = 13, 29, -3, 13
    h.box(bx0, 0, bz0, bx1, 0, bz1, STONE)
    h.box(bx0, 1, bz0, bx1, 1, bz1, 'spruce_planks')
    h.ring(bx0, 1, bz0, bx1, 2, bz1, STONE)
    h.ring(bx0, 3, bz0, bx1, 9, bz1, 'mangrove_planks')
    BS, BN, BE, BW = h.face('S', bz1), h.face('N', bz0), h.face('E', bx1), h.face('W', bx0)
    for f, a, b in ((BS, bx0, bx1), (BN, bx0, bx1), (BE, bz0, bz1), (BW, bz0, bz1)):
        for u in range(a + 1, b, 2):
            for y in range(3, 10):
                f.put(u, y, 0, 'stripped_mangrove_log', axis='y')
        f.post(a, 3, 9, white, d=0)
        f.post(b, 3, 9, white, d=0)
        f.beam(a, b, 9, white, d=0)
    rows = [9, 10, 12, 14, 16, 17, 18, 19, 20, 21]
    h.profile(bx0, bx1, bz0, bz1, rows, 'z', 'dark_oak', over=1, end=1, fill='mangrove_planks', planes=(bz0, bz1), body='dark_oak_planks')
    # a cupola on the ridge
    cx, cz = 21, 5
    h.box(cx - 1, 22, cz - 1, cx + 1, 23, cz + 1, white)
    for x, z in ((cx, cz - 1), (cx, cz + 1), (cx - 1, cz), (cx + 1, cz)):
        h.put(x, 23, z, 'glass_pane')
    h.put(cx, 23, cz, 'lantern', hanging=False)
    h.hip(cx - 1, cx + 1, cz - 1, cz + 1, 24, 'dark_oak', over=1, top='dark_oak_slab', soffit=False, trims=False)
    h.put(cx, 26, cz, 'lightning_rod', facing='up')
    # big doors in a white frame, a hay window above
    BS.wall(20, 23, 2, 7, 0, 'spruce_planks')
    BS.post(19, 2, 8, white)
    BS.post(24, 2, 8, white)
    BS.beam(19, 24, 8, white)
    door(BS, 21, 2, 'spruce_door', 'stone_brick', double=True)
    h.box(21, 1, bz1 + 2, 22, 1, bz1 + 2, 'stone_brick_slab', type='bottom')
    BS.window(21, 11, 2, 2)
    BS.post(20, 10, 13, white)
    BS.post(23, 10, 13, white)
    BS.beam(20, 23, 14, white)
    BS.beam(20, 23, 10, white)
    for u in (0, 7):
        BE.window(u, 5, 2, 2)
    BN.window(21, 5, 2, 2)
    BW.window(11, 6, 2, 2)
    BW.door(5, 2, 'spruce_door')
    h.clear(12, 2, 5, 12, 3, 5)
    # the silo, round stone with a copper cap, a way through from the barn
    scx, scz, r = 33, 3, 3
    for dx, dz in circle_disc(r):
        h.put(scx + dx, 0, scz + dz, STONE)
        h.put(scx + dx, 1, scz + dz, 'spruce_planks')
    for y in range(1, 18):
        for dx, dz in circle_ring(r):
            h.put(scx + dx, y, scz + dz, 'stone_bricks' if y % 5 == 0 else SMOOTH)
    h.cone(scx, scz, r, 18, 'waxed_cut_copper', over=1, step=1, tip=[('lightning_rod', {'facing': 'up'})])
    h.put(bx1, 2, 3, 'spruce_door', facing='east', half='lower', hinge='left', open=False)
    h.put(bx1, 3, 3, 'spruce_door', facing='east', half='upper', hinge='left', open=False)
    h.clear(scx - r, 2, 3, scx - r, 3, 3)
    h.put(scx + r, 8, 3, 'glass_pane')
    h.put(scx, 8, scz + r, 'glass_pane')
    # the yard
    for x in range(14, 34):
        if x not in (21, 22):
            h.put(x, 1, 24, 'spruce_fence')
    for z in range(15, 25):
        h.put(33, 1, z, 'spruce_fence')
    for x in (21, 22):
        for z in range(16, 28):
            h.put(x, 0, z, 'dirt_path')
    for x in (5, 6):
        for z in range(z1 + 5, 28):
            h.put(x, 0, z, 'dirt_path')
    h.box(16, 1, 16, 17, 1, 16, 'hay_block', axis='x')
    h.put(17, 2, 16, 'hay_block', axis='z')
    h.put(26, 1, 16, 'water_cauldron', level=3)
    h.put(27, 1, 16, 'water_cauldron', level=3)
    h.scatter([(x, 1, z) for x in list(range(-3, 4)) + list(range(8, 12)) for z in (15, 16)], GARDEN, 0.7)
    h.small_tree(-3, 1, 22, 'oak_log', 'oak_leaves', 4, 3)
    return h


def tudor_manor():
    h = House('tudor_manor', 'Tudor Manor', 100)
    frame, wood, roof, stone = 'stripped_dark_oak_log', 'dark_oak', 'deepslate_tile', 'stone_brick'
    x0, x1, z0, z1 = 0, 31, 0, 12
    h.yard(-5, -4, 36, 30)
    h.box(x0 - 1, 0, z0 - 1, x1 + 1, 0, z1 + 1, 'stone_bricks')
    base(h, x0, x1, z0, z1, 'stone_bricks', ledge=stone)
    # brick below, plaster and dark timber above, a stone string course between
    h.body(x0, x1, z0, z1, [BRICK, PLASTER], [5, 4], 'dark_oak_planks')

    def timber(f, a, b, posts):
        f.corbels(a, b, 6, stone)
        f.beam(a - 1, b + 1, 7, frame)
        f.beam(a - 1, b + 1, 11, frame)
        for u in posts:
            f.post(u, 8, 10, frame)

    long_posts = (-1, 4, 9, 14, 17, 22, 27, 32)
    timber(h.face('S', z1), x0, x1, long_posts)
    timber(h.face('N', z0), x0, x1, long_posts)
    timber(h.face('E', x1), z0, z1, (-1, 6, 13))
    timber(h.face('W', x0), z0, z1, (-1, 6, 13))
    # three gabled bays on the front, the middle one bigger, with the door
    bays = ((2, 8, 3), (11, 20, 4), (23, 29, 3))
    for a, b, deep in bays:
        zf = z1 + deep
        h.box(a - 1, 0, z1 + 1, b + 1, 0, zf + 1, 'stone_bricks')
        base(h, a, b, z1 + 1, zf, 'stone_bricks', ledge=stone, sides='SEW')
        h.body(a, b, z1, zf, [BRICK, PLASTER], [5, 4], 'dark_oak_planks')
        P, PE, PW = h.face('S', zf), h.face('E', b), h.face('W', a)
        posts = (a - 1, a + 1, b - 1, b + 1) if b - a == 6 else (a - 1, a + 2, b - 2, b + 1)
        timber(P, a, b, posts)
        timber(PE, z1 + 1, zf, (zf + 1,))
        timber(PW, z1 + 1, zf, (zf + 1,))
        # open into the main house
        h.clear(a + 1, 2, z1, b - 1, 6, zf - 1)
        h.clear(a + 1, 8, z1, b - 1, 11, zf - 1)
    h.gable(x0 - 1, x1 + 1, z0 - 1, z1 + 1, 12, 'x', roof, over=1, end=1, fill=PLASTER, fill_planes=(x0, x1))
    for a, b, deep in bays:
        zf = z1 + deep
        h.gable(a - 1, b + 1, z1 - 4, zf + 1, 12, 'z', roof, over=1, end=1, fill=PLASTER, fill_planes=(zf,))
        mid = (a + b) // 2
        wide = 1 if (b - a) % 2 else 0
        h.box(mid - 1, 13, zf, mid + 1 + wide, 14, zf, 'glass_pane')
        h.box(a, 12, zf + 1, b, 12, zf + 1, frame, axis='x')
        h.box(mid, 15, zf + 1, mid + wide, 15, zf + 1, frame, axis='y')

    # the gaps between the bays are filled in, up under the roof
    for a, b in ((9, 10), (21, 22)):
        h.box(a, 8, z1 + 1, b, 11, z1 + 3, PLASTER)

    def stone_window(f, u, y, w=3, h_=3):
        # glass in a stone frame, a stone upright between the lights when it's wide
        f.window(u, y, w, h_)
        for uu in range(u, u + w):
            f.put(uu, y + h_, 1, stone + '_stairs', facing='in', half='top')
            f.put(uu, y - 1, 1, stone + '_stairs', facing='in', half='bottom')
        if w >= 3:
            for uu in range(u + 1, u + w, 2):
                for yy in range(y, y + h_):
                    f.put(uu, yy, 0, 'stone_brick_wall')

    for a, b, deep in bays:
        P = h.face('S', z1 + deep)
        mid = (a + b) // 2
        if b - a == 6:
            stone_window(P, mid - 1, 3, 3, 3)
            P.window(mid - 1, 8, 3, 2)
        else:
            stone_window(P, a + 1, 3, 2, 3)
            stone_window(P, b - 2, 3, 2, 3)
            P.window(a + 3, 8, 4, 2)
    # the door in the middle bay: a stone arch standing proud, oak doors, steps up to it
    P = h.face('S', z1 + 4)
    door(P, 15, 2, wood + '_door', stone, double=True)
    P.post(14, 1, 4, 'stone_bricks')
    P.post(17, 1, 4, 'stone_bricks')
    P.put(15, 4, 1, stone + '_stairs', facing='l', half='top')
    P.put(16, 4, 1, stone + '_stairs', facing='r', half='top')
    P.wall(14, 17, 5, 5, 1, stone + '_slab', type='top')
    h.box(15, 1, z1 + 6, 16, 1, z1 + 6, stone + '_slab', type='bottom')
    # plain walls: windows in a steady row between the timbers
    N, E, W = h.face('N', z0), h.face('E', x1), h.face('W', x0)
    for u in (1, 6, 11, 19, 24, 29):
        stone_window(N, u, 3, 2, 3)
        N.window(u, 8, 2, 2)
    for f in (E, W):
        for u in (2, 9):
            stone_window(f, u - 1, 3, 3, 3)
            f.window(u - 1, 8, 3, 2)
        f.window(5, 13, 3, 2)
    # two brick stacks on the ridge
    for cx in (8, 22):
        chimney(h, cx, cx + 1, 5, 7, 18, 23, BRICK)
    # inside: two dividing walls with doorways, a wide stair
    for wx in (10, 21):
        for y in list(range(2, 7)) + list(range(8, 12)):
            h.box(wx, y, z0 + 1, wx, y, z1 - 1, 'dark_oak_planks')
        h.clear(wx, 2, 6, wx, 3, 7)
        h.clear(wx, 8, 6, wx, 9, 7)
    h.stairs_up(12, 2, 2, 'east', 6, wood, wide=2, rail='south')
    # the garden: one straight gravel walk, low hedges, flowers, lamps
    for z in range(z1 + 7, 31):
        for x in (15, 16):
            h.put(x, 0, z, 'gravel')
    for z in range(z1 + 7, 28):
        for x in (6, 25):
            h.put(x, 1, z, 'oak_leaves', persistent=True)
    for x in list(range(6, 14)) + list(range(18, 26)):
        h.put(x, 1, 28, 'oak_leaves', persistent=True)
    for x, z in ((13, 20), (18, 20), (13, 27), (18, 27)):
        h.put(x, 1, z, 'stone_bricks')
        h.put(x, 2, z, 'spruce_fence')
        h.put(x, 3, z, 'lantern', hanging=False)
    h.scatter([(x, 1, z) for x in list(range(7, 13)) + list(range(19, 25)) for z in (22, 23, 24)], ['poppy', 'cornflower', 'pink_tulip', 'white_tulip', 'allium'], 0.8)
    return h


ALL = [cosy_cottage, oak_timber_house, mountain_lodge, farmhouse, tudor_manor]
