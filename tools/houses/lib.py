# The little building kit behind tools/make_houses.py: a House is a dictionary of block cells plus helpers that know
# what a good builder does (recessed walls, shutters that hug the wall, roofs with overhangs and proper corners).
# Coordinates: the front of a house faces +z (south). Looking at the front, +x is to your right. y=0 is the ground.
import gzip
import random
import struct

DIRS = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}
CCW = {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}
OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}

# Anything here doesn't count as a full cube when fences, panes and walls look for something to join.
PARTIAL = ('_stairs', '_slab', '_fence', '_fence_gate', '_wall', '_pane', 'iron_bars', '_door', '_trapdoor', 'lantern', 'chain',
           '_carpet', 'flower_pot', 'potted_', 'campfire', 'torch', 'end_rod', 'lightning_rod', 'poppy', 'dandelion', 'azure_bluet',
           'oxeye_daisy', 'cornflower', 'allium', 'blue_orchid', '_tulip', 'lilac', 'rose_bush', 'peony', 'pink_petals', 'fern', 'grass',
           'water', 'cauldron', 'composter', 'candle', 'vine', 'ladder', 'bell', 'lily_of_the_valley', 'azalea', 'lily_pad', 'dirt_path',
           'sea_pickle', 'bamboo', 'sapling', 'amethyst_cluster', 'hopper', 'anvil', 'scaffolding', 'sugar_cane', 'grindstone')


def is_full(block):
    if block is None:
        return False
    if block.endswith('_leaves') or block in ('grass_block', 'moss_block'):
        return True
    return not any(k in block for k in PARTIAL)


def wood_fence(b):
    return b is not None and b.endswith('_fence') and b != 'nether_brick_fence'


class House:
    def __init__(self, id, name, level=1, awakened=False):
        self.id, self.name, self.level, self.awakened = id, name, level, awakened
        self.cells = {}
        self.rng = random.Random(id)
        self.roofs = []  # (wall rectangle, top height per column)
        self.roof_owner = {}  # cell -> which roof it belongs to
        self.roof_specs = []  # (overhang, end, span, name) for the checker
        self.flights = []
        self.rails = []
        self.fixtures = set()
        self.must_be_clear = []
        self.flush_ok = False
        self._new = {}

    # ---- plain blocks
    def put(self, x, y, z, block, **props):
        if y < 0:
            raise ValueError(f'{self.id}: y below 0 at {x},{y},{z}')
        if isinstance(block, (list, tuple)):
            block = self.pick(block)
        self.cells[(x, y, z)] = (block, {k: str(v).lower() for k, v in props.items()})
        self.roof_owner.pop((x, y, z), None)

    def pick(self, choices):
        # a list of names, or (name, weight) pairs
        names = [c[0] if isinstance(c, tuple) else c for c in choices]
        weights = [c[1] if isinstance(c, tuple) else 1 for c in choices]
        return self.rng.choices(names, weights)[0]

    def get(self, x, y, z):
        c = self.cells.get((x, y, z))
        return c[0] if c else None

    def clear(self, x0, y0, z0, x1=None, y1=None, z1=None):
        x1, y1, z1 = x0 if x1 is None else x1, y0 if y1 is None else y1, z0 if z1 is None else z1
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.cells.pop((x, y, z), None)

    def box(self, x0, y0, z0, x1, y1, z1, block, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.put(x, y, z, block, **props)

    def ring(self, x0, y0, z0, x1, y1, z1, block, **props):
        # the four walls of a box, no floor or ceiling
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, z0, block, **props)
                self.put(x, y, z1, block, **props)
            for z in range(z0 + 1, z1):
                self.put(x0, y, z, block, **props)
                self.put(x1, y, z, block, **props)

    # ---- faces: write a wall once and it works on any side
    def face(self, side, plane):
        return Face(self, side, plane)

    # ---- roofs
    def _roof(self, x, y, z, block, **props):
        # walls and beams already there win: a roof never cuts a hole in them
        if (x, y, z) in self.cells and (x, y, z) not in self.roof_owner:
            return
        self.put(x, y, z, block, **props)
        self.roof_owner[(x, y, z)] = len(self.roofs)
        self._new[(x, z)] = max(self._new.get((x, z), -999), y)

    def _roof_done(self, x0, x1, z0, z1, over=1, end=1, trims=True, inset=0):
        # trims=False: a tier sitting on a lower roof, not crossing it. inset: the walls are this far inside the roof's rectangle
        self.roofs.append(((x0 + inset, x1 - inset, z0 + inset, z1 - inset), self._new if trims else {}))
        self.roof_specs.append((over, end, min(x1 - x0, z1 - z0) + 1, f'{x0},{z0}'))
        self._new = {}

    def clear_room(self, x0, y0, z0, x1, y1, z1):
        # say that a room must stay empty: anything left in it (a stray roof, say) is removed at the end and the checker looks too
        self.must_be_clear.append((x0, y0, z0, x1, y1, z1))

    def gable(self, x0, x1, z0, z1, y, axis, mat, over=1, end=1, flare=False, ridge=None, fill=None, fill_planes=None,
              barge=True, soffit=True, ridge_cap=True, steep=False, inset=0):
        # (x0..x1, z0..z1) = the walls under it. y = height of the row of stairs right above the wall plane.
        # axis = which way the ridge runs. fill = what closes the triangular gable ends.
        # steep = each row climbs 2 (a full block under each stair), for tall pointed roofs.
        stairs, slab = mat + '_stairs', mat + '_slab'
        body = ridge if steep and ridge else slab
        s0, s1 = (z0, z1) if axis == 'x' else (x0, x1)
        a0, a1 = (x0 - end, x1 + end) if axis == 'x' else (z0 - end, z1 + end)
        up0, up1 = ('south', 'north') if axis == 'x' else ('east', 'west')
        at = (lambda a, s: (a, s)) if axis == 'x' else (lambda a, s: (s, a))
        span = s1 - s0 + 1
        half = (span + 1) // 2
        rise = 2 if steep else 1
        heights = {}
        for i in range(-over, half):
            yy = y + i * rise if i >= 0 else y + i
            heights[i] = yy
            for a in range(a0, a1 + 1):
                centre = span % 2 == 1 and i == half - 1
                sa, sb = s0 + i, s1 - i
                if centre:
                    cx, cz = at(a, sa)
                    if ridge:
                        self._roof(cx, yy, cz, ridge)
                    else:
                        self._roof(cx, yy, cz, slab, type='double')
                    if ridge_cap:
                        self._roof(cx, yy + 1, cz, slab, type='bottom')
                else:
                    cx, cz = at(a, sa)
                    self._roof(cx, yy, cz, stairs, facing=up0, half='bottom')
                    cx, cz = at(a, sb)
                    self._roof(cx, yy, cz, stairs, facing=up1, half='bottom')
                    if span % 2 == 0 and i == half - 1 and ridge_cap:
                        cx, cz = at(a, sa)
                        self._roof(cx, yy + 1, cz, slab, type='bottom')
                        cx, cz = at(a, sb)
                        self._roof(cx, yy + 1, cz, slab, type='bottom')
                if steep and i > 0:
                    cx, cz = at(a, sa)
                    self._roof(cx, yy - 1, cz, slab, type='double')
                    if not centre:
                        cx, cz = at(a, sb)
                        self._roof(cx, yy - 1, cz, slab, type='double')
                if soffit and -over < i < 0:
                    cx, cz = at(a, sa)
                    self._roof(cx, yy - 1, cz, stairs, facing=up0, half='top')
                    cx, cz = at(a, sb)
                    self._roof(cx, yy - 1, cz, stairs, facing=up1, half='top')
            if flare and i == -over:
                for a in range(a0, a1 + 1):
                    cx, cz = at(a, s0 + i - 1)
                    self._roof(cx, yy, cz, slab, type='bottom')
                    cx, cz = at(a, s1 - i + 1)
                    self._roof(cx, yy, cz, slab, type='bottom')
            # the stepped board under each overhanging end
            if barge and end > 0 and i >= 0:
                towards = ('east', 'west') if axis == 'x' else ('south', 'north')
                for a, way in ((a0, towards[0]), (a1, towards[1])):
                    for s in {s0 + i, s1 - i}:
                        cx, cz = at(a, s)
                        drop = yy - (2 if steep and i > 0 else 1)
                        self._roof(cx, drop, cz, stairs, facing=way, half='top')
        if fill:
            planes = fill_planes if fill_planes else (x0, x1) if axis == 'x' else (z0, z1)
            for a in planes:
                for i in range(1, half):
                    for yy in range(y, heights[i]):
                        for s in {s0 + i, s1 - i}:
                            cx, cz = at(a, s)
                            if not self.get(cx, yy, cz):
                                self.put(cx, yy, cz, fill)
        self._roof_done(x0, x1, z0, z1, over, end, True, inset)
        return heights[half - 1]

    def profile(self, x0, x1, z0, z1, rows, axis, mat, over=1, end=1, body=None, fill=None, planes=None, cap=True, ridge=None):
        # a roof with any outline: rows = heights from the outermost row inward (gambrel barns, bell-cast eaves...).
        stairs, slab = mat + '_stairs', mat + '_slab'
        s0, s1 = (z0, z1) if axis == 'x' else (x0, x1)
        a0, a1 = (x0 - end, x1 + end) if axis == 'x' else (z0 - end, z1 + end)
        up0, up1 = ('south', 'north') if axis == 'x' else ('east', 'west')
        at = (lambda a, s: (a, s)) if axis == 'x' else (lambda a, s: (s, a))
        span = s1 - s0 + 1 + 2 * over
        half = (span + 1) // 2
        rows = list(rows)[:half] + [rows[-1]] * max(0, half - len(rows))
        for i, yy in enumerate(rows):
            sa, sb = s0 - over + i, s1 + over - i
            gap = yy - rows[i - 1] if i else 0
            for a in range(a0, a1 + 1):
                for s, up in ((sa, up0), (sb, up1)):
                    cx, cz = at(a, s)
                    if s == sa == sb or (span % 2 == 1 and i == half - 1):
                        self._roof(cx, yy, cz, ridge or slab, **({} if ridge else {'type': 'double'}))
                        if cap:
                            self._roof(cx, yy + 1, cz, slab, type='bottom')
                    else:
                        self._roof(cx, yy, cz, stairs, facing=up, half='bottom')
                        if span % 2 == 0 and i == half - 1 and cap:
                            self._roof(cx, yy + 1, cz, slab, type='bottom')
                    for k in range(1, gap):
                        if body:
                            self._roof(cx, yy - k, cz, body)
                        else:
                            self._roof(cx, yy - k, cz, slab, type='double')
            if fill:
                for a in planes or ((x0, x1) if axis == 'x' else (z0, z1)):
                    for s in {sa, sb}:
                        if s0 <= s <= s1:
                            for y in range(rows[over], yy):
                                cx, cz = at(a, s)
                                if not self.get(cx, y, cz):
                                    self.put(cx, y, cz, fill)
        self._roof_done(x0, x1, z0, z1, over, end)
        return rows[-1]

    def hip(self, x0, x1, z0, z1, y, mat, over=1, flare=False, top=None, soffit=True, corner_up=False, stop=None, trims=True, inset=0):
        # a roof that slopes on all four sides. stop = leave the middle open after this many rows inward (for a next tier).
        stairs, slab = mat + '_stairs', mat + '_slab'
        i = -over
        yy = y
        while True:
            a0, a1, b0, b1 = x0 + i, x1 - i, z0 + i, z1 - i
            yy = y + i
            if a0 > a1 or b0 > b1 or (stop is not None and i >= stop):
                break
            if a0 == a1 or b0 == b1:
                for x in range(a0, a1 + 1):
                    for z in range(b0, b1 + 1):
                        self._roof(x, yy, z, top or slab, **({} if top else {'type': 'double'}))
                        if not top:
                            self._roof(x, yy + 1, z, slab, type='bottom')
                break
            for x in range(a0, a1 + 1):
                self._roof(x, yy, b0, stairs, facing='south', half='bottom')
                self._roof(x, yy, b1, stairs, facing='north', half='bottom')
            for z in range(b0 + 1, b1):
                self._roof(a0, yy, z, stairs, facing='east', half='bottom')
                self._roof(a1, yy, z, stairs, facing='west', half='bottom')
            if soffit and -over < i < 0:
                for x in range(a0, a1 + 1):
                    self._roof(x, yy - 1, b0, stairs, facing='south', half='top')
                    self._roof(x, yy - 1, b1, stairs, facing='north', half='top')
                for z in range(b0 + 1, b1):
                    self._roof(a0, yy - 1, z, stairs, facing='east', half='top')
                    self._roof(a1, yy - 1, z, stairs, facing='west', half='top')
            if flare and i == -over:
                for x in range(a0 - 1, a1 + 2):
                    self._roof(x, yy, b0 - 1, slab, type='bottom')
                    self._roof(x, yy, b1 + 1, slab, type='bottom')
                for z in range(b0, b1 + 1):
                    self._roof(a0 - 1, yy, z, slab, type='bottom')
                    self._roof(a1 + 1, yy, z, slab, type='bottom')
                if corner_up:  # eaves that curl up at the corners
                    for cx, cz in ((a0 - 1, b0 - 1), (a1 + 1, b0 - 1), (a0 - 1, b1 + 1), (a1 + 1, b1 + 1)):
                        self.put(cx, yy, cz, slab, type='double')
                        self.put(cx, yy + 1, cz, slab, type='bottom')
            i += 1
        self._roof_done(x0, x1, z0, z1, over, over, trims, inset)
        return yy

    def cone(self, cx, cz, r, y, mat, over=1, step=2, body=None, tip=None):
        # a round roof. Each ring is a row of stairs; step=2 puts a full course under each for a steep witch's-hat look.
        stairs, slab = mat + '_stairs', mat + '_slab'
        rings = r + over
        last = y
        for k in range(rings + 1):
            rad = rings - k
            yy = y + (k - over) * step if k >= over else y + (k - over)
            last = yy
            if rad == 0:
                if body:
                    self.put(cx, yy, cz, body)
                else:
                    self.put(cx, yy, cz, slab, type='double')
                if step > 1:
                    if body:
                        self.put(cx, yy - 1, cz, body)
                    else:
                        self.put(cx, yy - 1, cz, slab, type='double')
                self._new[(cx, cz)] = yy
                self.roof_owner[(cx, yy, cz)] = len(self.roofs)
                break
            for dx, dz in circle_ring(rad):
                x, z = cx + dx, cz + dz
                if abs(dx) > abs(dz):
                    f = 'west' if dx > 0 else 'east'
                else:
                    f = 'north' if dz > 0 else 'south'
                # a wall already there wins, and only the part over the tower itself hides other roofs
                if not ((x, yy, z) in self.cells and (x, yy, z) not in self.roof_owner):
                    self.put(x, yy, z, stairs, facing=f, half='bottom')
                    self.roof_owner[(x, yy, z)] = len(self.roofs)
                if rad <= r:
                    self._new[(x, z)] = max(self._new.get((x, z), -999), yy)
                if step > 1 and k > over:
                    for s in range(1, step):
                        if (x, yy - s, z) in self.cells and (x, yy - s, z) not in self.roof_owner:
                            continue
                        if body:
                            self.put(x, yy - s, z, body)
                        else:
                            self.put(x, yy - s, z, slab, type='double')
        if tip:
            for n, (b, props) in enumerate(tip):
                self.put(cx, last + 1 + n, cz, b, **props)
        self._roof_done(cx - r, cx + r, cz - r, cz + r, over, over)
        return last

    # ---- lived-in bits
    def chimney(self, x0, z0, x1, z1, y0, taper, top, block, trim='stone_brick', fire=True, x2=None, z2=None, w=2):
        # wide at the bottom, narrow from `taper` up, a shoulder between, a band and a slab cap, and real smoke
        x2 = x0 if x2 is None else x2
        z2 = z0 if z2 is None else z2
        for y in range(y0, taper):
            self.box(x0, y, z0, x1, y, z1, block)
        influe = lambda x, z: x2 <= x < x2 + w and z2 <= z < z2 + w
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if not influe(x, z):
                    # the shoulder steps in towards the flue
                    f = 'east' if x < x2 else 'west' if x >= x2 + w else 'south' if z < z2 else 'north'
                    self.put(x, taper, z, trim + '_stairs', facing=f, half='bottom')
        self.box(x2, taper, z2, x2 + w - 1, top, z2 + w - 1, block)
        self.box(x2, top - 1, z2, x2 + w - 1, top - 1, z2 + w - 1, trim + 's' if trim.endswith('brick') else trim)
        if fire:
            self.put(x2, top + 1, z2, 'campfire', lit=True, signal_fire=False, facing='south')
            if w > 1:
                self.put(x2 + 1, top + 1, z2 + 1, trim + '_slab', type='bottom')
                self.put(x2 + 1, top + 1, z2, trim + '_slab', type='bottom')
                self.put(x2, top + 1, z2 + 1, 'flower_pot')

    def body(self, x0, x1, z0, z1, walls, storeys, floor='spruce_planks', y0=1, base=None):
        # A closed box of walls on (x0..x1, z0..z1). storeys = wall height of each floor, bottom up; a floor goes between them.
        # walls = one block or a list, one per storey. Returns the height of each floor slab plus the wall top.
        if base:
            self.box(x0, 0, z0, x1, 0, z1, base)
        self.box(x0, y0, z0, x1, y0, z1, floor)
        y = y0
        out = []
        for i, high in enumerate(storeys):
            out.append(y)
            w = walls[i] if isinstance(walls, list) and isinstance(walls[0], (str, list)) else walls
            self.ring(x0, y + 1, z0, x1, y + high, z1, w)
            y += high
            if i + 1 < len(storeys):
                y += 1
                self.box(x0, y, z0, x1, y, z1, floor)
        out.append(y)
        return out

    def dormer(self, cx, zf, y, roof, wall, pane='glass_pane', tall=1, back=3):
        # A dormer on a south-facing slope: a window at (cx, y, zf), cheeks going back into the roof, a little gable over it, low enough to
        # stay under the main ridge. Roof trimming opens the slope under it into the attic.
        for z in range(zf - back + 1, zf + 1):
            for yy in range(y - 1, y + tall):
                self.put(cx - 1, yy, z, wall)
                self.put(cx + 1, yy, z, wall)
        self.put(cx, y - 1, zf, wall)
        for t in range(tall):
            self.put(cx, y + t, zf, pane)
        for x in (cx - 1, cx, cx + 1):
            yy = y - 2
            while yy > y - 6 and not self.get(x, yy, zf):
                self.put(x, yy, zf, wall)
                yy -= 1
        self.gable(cx - 1, cx + 1, zf - back + 1, zf, y + tall, 'z', roof, over=0, end=1, fill=wall, fill_planes=(zf,), barge=False, ridge_cap=False)

    def lamp(self, x, y, z, post='spruce_fence', top='spruce_slab', height=2):
        for i in range(height):
            self.put(x, y + i, z, post)
        self.put(x, y + height, z, 'lantern', hanging=False)
        self.put(x, y + height + 1, z, top, type='bottom')

    def stone_lantern(self, x, y, z, stone='cobblestone'):
        self.put(x, y, z, stone + '_wall')
        self.put(x, y + 1, z, 'lantern', hanging=False)
        self.put(x, y + 2, z, stone + '_slab', type='bottom')

    def stairs_up(self, x, y, z, way, steps, mat, wide=1, solid=None, rail=None):
        # A real flight going `way` from (x,y,z). The top step is level with the floor it reaches, so steps = the floor's height - the
        # floor you start on. One block under each step and open space below. rail = a side ('north'...) to put a fence on, up where the hole is.
        dx, dz = DIRS[way]
        sx, sz = (0, 1) if dx else (1, 0)
        top = y + steps - 1
        for i in range(steps):
            for w in range(wide):
                cx, cz = x + dx * i + sx * w, z + dz * i + sz * w
                self.put(cx, y + i, cz, mat + '_stairs', facing=way, half='bottom')
                self.fixtures.add((cx, y + i, cz))
                self.flights.append((cx, y + i, cz))
                if i > 0:
                    self.put(cx, y + i - 1, cz, mat + '_slab', type='top')
                    self.fixtures.add((cx, y + i - 1, cz))
            if rail and i < steps - 1:
                rx, rz = DIRS[rail]
                far = wide if rx + rz > 0 else -1
                cx, cz = x + dx * i + (sx * far if sx else rx), z + dz * i + (sz * far if sz else rz)
                self.rails.append((cx, top + 1, cz, mat + '_fence'))

    def yard(self, x0, z0, x1, z1, y=0, nibble=2, block='grass_block'):
        # a plot of ground that isn't a perfect rectangle: its corners and some edge cells are bitten off
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = min(x - x0, x1 - x) + min(z - z0, z1 - z)
                if edge < nibble:
                    continue
                if (x in (x0, x1) or z in (z0, z1)) and self.rng.random() < 0.18:
                    continue
                self.put(x, y, z, block)

    def scatter(self, cells, blocks, chance=1.0):
        for (x, y, z) in cells:
            if self.rng.random() < chance:
                b = self.pick(blocks)
                if b in ('rose_bush', 'lilac', 'peony'):
                    self.put(x, y, z, b, half='lower')
                    self.put(x, y + 1, z, b, half='upper')
                else:
                    self.put(x, y, z, b)

    def small_tree(self, x, y, z, log, leaves, height=4, radius=2, petals=False):
        for i in range(height):
            self.put(x, y + i, z, log, axis='y')
        top = y + height
        for dy in range(-2, 2):
            rad = radius if dy < 0 else radius - 1 if dy == 0 else max(radius - 2, 1)
            for dx in range(-rad, rad + 1):
                for dz in range(-rad, rad + 1):
                    if dx * dx + dz * dz > rad * rad + 1:
                        continue
                    if abs(dx) == rad and abs(dz) == rad and self.rng.random() < 0.6:
                        continue
                    if not (dx == 0 and dz == 0 and dy < 0) and not self.get(x + dx, top + dy, z + dz):
                        self.put(x + dx, top + dy, z + dz, leaves, persistent=True)
        if petals:
            for _ in range(radius * 6):
                dx, dz = self.rng.randint(-radius - 1, radius + 1), self.rng.randint(-radius - 1, radius + 1)
                if (dx or dz) and not self.get(x + dx, y, z + dz) and self.get(x + dx, y - 1, z + dz) == 'grass_block':
                    self.put(x + dx, y, z + dz, 'pink_petals', flower_amount=self.rng.randint(1, 4), facing=self.rng.choice(list(DIRS)))

    # ---- finishing: what the game would work out when a player places these blocks by hand
    def finish(self):
        self._trim_roofs()
        self._clear_flights()
        for x0, y0, z0, x1, y1, z1 in self.must_be_clear:
            for x in range(x0, x1 + 1):
                for y in range(y0, y1 + 1):
                    for z in range(z0, z1 + 1):
                        c = (x, y, z)
                        if c in self.cells and c not in self.fixtures and c in self.roof_owner:
                            del self.cells[c]
        from .check import light_up
        light_up(self)
        self._connect()
        self._stairs()
        return self

    def _trim_roofs(self):
        # where two roofs cross, each piece hidden under the other's surface goes: valleys come out clean and no roof ends up inside
        for cell, owner in list(self.roof_owner.items()):
            x, y, z = cell
            for i, (rect, top) in enumerate(self.roofs):
                if i == owner:
                    continue
                if rect[0] <= x <= rect[1] and rect[2] <= z <= rect[3] and (x, z) in top and y < top[(x, z)]:
                    self.cells.pop(cell, None)
                    del self.roof_owner[cell]
                    break

    def _clear_flights(self):
        # every flight of stairs has head room, whatever floor was put across it
        steps = set(self.flights)
        for x, y, z in self.flights:
            for up in (1, 2, 3):
                c = (x, y + up, z)
                if c not in steps and c not in self.roof_owner and not (c in self.fixtures and self.cells.get(c, ('', {}))[0].endswith('_slab')):
                    self.cells.pop(c, None)
        for x, y, z, b in self.rails:
            if (x, y, z) not in self.cells and (x, y - 1, z) in self.cells:
                self.put(x, y, z, b)
                self.fixtures.add((x, y, z))

    def _connect(self):
        for (x, y, z), (b, p) in list(self.cells.items()):
            if b.endswith('_pane') or b == 'iron_bars':
                for d, (dx, dz) in DIRS.items():
                    n = self.get(x + dx, y, z + dz)
                    p[d] = 'true' if n and (is_full(n) or n.endswith('_pane') or n == 'iron_bars' or n.endswith('_wall')) else 'false'
            elif wood_fence(b):
                for d, (dx, dz) in DIRS.items():
                    n = self.get(x + dx, y, z + dz)
                    gate = n and n.endswith('_fence_gate') and self.cells[(x + dx, y, z + dz)][1].get('facing') in (CCW[d], OPP[CCW[d]])
                    p[d] = 'true' if n and (is_full(n) or wood_fence(n) or gate) else 'false'
            elif b.endswith('_wall'):
                above = self.get(x, y + 1, z)
                on = {}
                for d, (dx, dz) in DIRS.items():
                    n = self.get(x + dx, y, z + dz)
                    gate = n and n.endswith('_fence_gate') and self.cells[(x + dx, y, z + dz)][1].get('facing') in (CCW[d], OPP[CCW[d]])
                    joins = bool(n and (is_full(n) or n.endswith('_wall') or n.endswith('_pane') or n == 'iron_bars' or gate))
                    on[d] = joins
                    tall = above and (is_full(above) or above.endswith('_wall'))
                    p[d] = ('tall' if tall else 'low') if joins else 'none'
                straight = (on['north'] and on['south'] and not on['east'] and not on['west']) or (on['east'] and on['west'] and not on['north'] and not on['south'])
                p['up'] = 'false' if straight and not (above and (above.endswith('_wall') or 'lantern' in above or 'torch' in above or above.endswith('_slab'))) else 'true'

    def _stairs(self):
        for (x, y, z), (b, p) in list(self.cells.items()):
            if b.endswith('_stairs'):
                p['shape'] = self._stair_shape(x, y, z, p)

    def _stair_at(self, x, y, z):
        c = self.cells.get((x, y, z))
        return c[1] if c and c[0].endswith('_stairs') else None

    def _take(self, x, y, z, p, face):
        dx, dz = DIRS[face]
        o = self._stair_at(x + dx, y, z + dz)
        return not o or o.get('facing') != p.get('facing') or o.get('half') != p.get('half')

    def _stair_shape(self, x, y, z, p):
        d = p['facing']
        half = p.get('half', 'bottom')
        dx, dz = DIRS[d]
        behind = self._stair_at(x + dx, y, z + dz)
        if behind and behind.get('half', 'bottom') == half:
            d1 = behind['facing']
            if d1 not in (d, OPP[d]) and self._take(x, y, z, p, OPP[d1]):
                return 'outer_left' if d1 == CCW[d] else 'outer_right'
        front = self._stair_at(x - dx, y, z - dz)
        if front and front.get('half', 'bottom') == half:
            d2 = front['facing']
            if d2 not in (d, OPP[d]) and self._take(x, y, z, p, d2):
                return 'inner_left' if d2 == CCW[d] else 'inner_right'
        return 'straight'


def circle_ring(r):
    # the cells of a round ring of radius r, closed (no diagonal holes)
    cells = []
    for dx in range(-r - 1, r + 2):
        for dz in range(-r - 1, r + 2):
            d = (dx * dx + dz * dz) ** 0.5
            if r - 0.5 <= d < r + 0.5:
                cells.append((dx, dz))
    return cells


def circle_disc(r):
    return [(dx, dz) for dx in range(-r - 1, r + 2) for dz in range(-r - 1, r + 2) if (dx * dx + dz * dz) ** 0.5 < r + 0.5]


class Face:
    # One side of the house. u runs along the wall, d is how far out from `plane` (d=0 the wall itself, d=1 in front of it).
    # Facings in props can be 'out', 'in', 'l', 'r' and get turned into real ones.
    SIDES = {
        'S': ('south', 'north', 'west', 'east'),
        'N': ('north', 'south', 'west', 'east'),
        'E': ('east', 'west', 'north', 'south'),
        'W': ('west', 'east', 'north', 'south'),
    }

    def __init__(self, house, side, plane):
        self.h, self.side, self.plane = house, side, plane
        self.out, self.inn, self.l, self.r = Face.SIDES[side]
        self.along_x = side in 'SN'

    def at(self, u, d):
        if self.side == 'S':
            return u, self.plane + d
        if self.side == 'N':
            return u, self.plane - d
        if self.side == 'E':
            return self.plane + d, u
        return self.plane - d, u

    def _props(self, props):
        out = dict(props)
        if 'facing' in out and out['facing'] in ('out', 'in', 'l', 'r'):
            out['facing'] = {'out': self.out, 'in': self.inn, 'l': self.l, 'r': self.r}[out['facing']]
        if out.get('axis') == 'u':
            out['axis'] = 'x' if self.along_x else 'z'
        elif out.get('axis') == 'd':
            out['axis'] = 'z' if self.along_x else 'x'
        return out

    def put(self, u, y, d, block, **props):
        x, z = self.at(u, d)
        self.h.put(x, y, z, block, **self._props(props))

    def get(self, u, y, d):
        x, z = self.at(u, d)
        return self.h.get(x, y, z)

    def clear(self, u, y, d):
        x, z = self.at(u, d)
        self.h.clear(x, y, z)

    def wall(self, u0, u1, y0, y1, d, block, **props):
        for u in range(u0, u1 + 1):
            for y in range(y0, y1 + 1):
                self.put(u, y, d, block, **props)

    def post(self, u, y0, y1, block, d=1):
        for y in range(y0, y1 + 1):
            if block.endswith(('_log', '_stem')) or 'pillar' in block:
                self.put(u, y, d, block, axis='y')
            else:
                self.put(u, y, d, block)

    def beam(self, u0, u1, y, block, d=1):
        for u in range(u0, u1 + 1):
            if block.endswith(('_log', '_stem')) or 'pillar' in block:
                self.put(u, y, d, block, axis='u')
            else:
                self.put(u, y, d, block)

    def window(self, u, y, w=1, h=2, pane='glass_pane', shutter=None, head=None, sill=None, box=None, flowers=None, d=0):
        # glass set back in the wall; shutters hug the wall either side; a little head above and a sill or flower box below
        for uu in range(u, u + w):
            for yy in range(y, y + h):
                self.put(uu, yy, d, pane)
        if shutter:
            for yy in range(y, y + h):
                self.put(u - 1, yy, d + 1, shutter, facing='out', open=True, half='bottom')
                self.put(u + w, yy, d + 1, shutter, facing='out', open=True, half='bottom')
        if head:
            for uu in range(u, u + w):
                self.put(uu, y + h, d + 1, head + '_stairs', facing='in', half='top')
        if sill:
            for uu in range(u, u + w):
                self.put(uu, y - 1, d + 1, sill + '_stairs', facing='in', half='bottom')
        if box:
            for uu in range(u, u + w):
                self.put(uu, y - 1, d + 1, 'moss_block')
                self.put(uu, y - 1, d + 2, box, facing='out', open=True, half='bottom')
                self.put(uu, y, d + 1, self.h.pick(flowers or ['poppy', 'cornflower', 'oxeye_daisy', 'azure_bluet', 'allium', 'pink_tulip']))
            if not self.get(u - 1, y - 1, d + 1):
                self.put(u - 1, y - 1, d + 1, box, facing='l', open=True, half='bottom')
            if not self.get(u + w, y - 1, d + 1):
                self.put(u + w, y - 1, d + 1, box, facing='r', open=True, half='bottom')

    def door(self, u, y, kind, double=False, d=0):
        hinge_lo = {'S': 'left', 'N': 'right', 'E': 'right', 'W': 'left'}[self.side]
        hinge_hi = 'right' if hinge_lo == 'left' else 'left'
        cells = [(u, hinge_lo)] if not double else [(u, hinge_lo), (u + 1, hinge_hi)]
        for uu, hinge in cells:
            self.put(uu, y, d, kind, facing='out', half='lower', hinge=hinge, open=False)
            self.put(uu, y + 1, d, kind, facing='out', half='upper', hinge=hinge, open=False)

    def gusset(self, u, y, way, mat, d=1):
        # the little angled brace in the corner under a beam: `way` = 'l' or 'r', the side its post is on
        self.put(u, y, d, mat + '_stairs', facing=way, half='top')

    def hang_lantern(self, u, y, arm, d=1, soul=False):
        # an arm out from the wall with a lantern hanging from it
        self.put(u, y, d, arm)
        self.put(u, y - 1, d, 'soul_lantern' if soul else 'lantern', hanging=True)

    def corbels(self, u0, u1, y, mat, d=1):
        for u in range(u0, u1 + 1):
            self.put(u, y, d, mat + '_stairs', facing='in', half='top')

    def balcony(self, u0, u1, y, depth, floor, rail, corbel=None):
        # a slab floor sticking out of the wall on upside-down stairs, with a railing round it
        for u in range(u0, u1 + 1):
            for d in range(1, depth + 1):
                self.put(u, y, d, floor + '_slab', type='top')
            if corbel:
                self.put(u, y - 1, 1, corbel + '_stairs', facing='in', half='top')
            self.put(u, y + 1, depth, rail)
        for d in range(1, depth + 1):
            self.put(u0, y + 1, d, rail)
            self.put(u1, y + 1, d, rail)


# ---- writing the vanilla structure file
def _nbt_str(s):
    b = s.encode('utf-8')
    return struct.pack('>H', len(b)) + b


def _named(tag, name, payload):
    return bytes([tag]) + _nbt_str(name) + payload


def _ints(values):
    return bytes([3]) + struct.pack('>i', len(values)) + b''.join(struct.pack('>i', v) for v in values)


def write_structure(house, path):
    xs = [k[0] for k in house.cells]
    ys = [k[1] for k in house.cells]
    zs = [k[2] for k in house.cells]
    x0, z0 = min(xs), min(zs)
    size = (max(xs) - x0 + 1, max(ys) + 1, max(zs) - z0 + 1)
    palette, index = [], {}
    entries = []
    for (x, y, z), (b, p) in sorted(house.cells.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
        key = (b, tuple(sorted(p.items())))
        if key not in index:
            index[key] = len(palette)
            palette.append(key)
        entries.append((x - x0, y, z - z0, index[key]))
    body = bytearray()
    body += _named(3, 'DataVersion', struct.pack('>i', 3465))
    body += _named(9, 'size', _ints(list(size)))
    pal = bytearray([10]) + struct.pack('>i', len(palette))
    for b, props in palette:
        pal += _named(8, 'Name', _nbt_str('minecraft:' + b))
        if props:
            inner = bytearray()
            for k, v in props:
                inner += _named(8, k, _nbt_str(v))
            pal += _named(10, 'Properties', bytes(inner) + b'\0')
        pal += b'\0'
    body += _named(9, 'palette', bytes(pal))
    blocks = bytearray([10]) + struct.pack('>i', len(entries))
    for x, y, z, s in entries:
        blocks += _named(9, 'pos', _ints([x, y, z]))
        blocks += _named(3, 'state', struct.pack('>i', s))
        blocks += b'\0'
    body += _named(9, 'blocks', bytes(blocks))
    body += _named(9, 'entities', bytes([0]) + struct.pack('>i', 0))
    meta = bytearray()
    meta += _named(8, 'name', _nbt_str(house.name))
    meta += _named(3, 'level', struct.pack('>i', house.level))
    meta += _named(1, 'awakened', bytes([1 if house.awakened else 0]))
    body += _named(10, 'charpowers', bytes(meta) + b'\0')
    with gzip.GzipFile(path, 'wb', mtime=0) as f:
        f.write(_named(10, '', bytes(body) + b'\0'))
    return size, len(entries), len(palette)
