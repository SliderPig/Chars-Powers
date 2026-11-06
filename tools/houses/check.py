# Every house has to pass this before it gets written. It walks in like a player would, and looks for what a builder would be
# embarrassed by: a door you can't reach, a room nobody can get into, a roof inside a room, a gap in a wall, a black interior.
from collections import deque

DIRS4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
DIRS6 = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))
LIGHTS = ('lantern', 'soul_lantern', 'campfire', 'soul_campfire', 'torch', 'wall_torch', 'glowstone', 'sea_lantern', 'candle')
SOFT = ('poppy', 'dandelion', 'azure_bluet', 'oxeye_daisy', 'cornflower', 'allium', 'blue_orchid', '_tulip', 'lilac', 'rose_bush', 'peony', 'pink_petals',
        'fern', 'grass', 'lily_of_the_valley', 'moss_carpet', '_carpet', 'torch', 'lantern', 'chain', 'flower_pot', 'potted_', 'red_mushroom', 'brown_mushroom',
        'sapling', 'lightning_rod', 'end_rod', 'amethyst_cluster', 'bamboo', 'sugar_cane', 'lily_pad', 'campfire', 'candle', 'ladder', 'water')


def soft(b):
    return any(k in b for k in SOFT) and b != 'grass_block'


def is_door(b):
    return b is not None and b.endswith('_door')


def is_gate(b):
    return b is not None and b.endswith('_fence_gate')


# ---- air: what stops it. A door counts as shut here, so "enclosed" means enclosed.
def stops_air(b):
    if b is None or soft(b) or b.endswith('_fence') or is_gate(b):
        return False
    return True


def regions(h):
    # returns (outside air set, list of enclosed air regions)
    xs = [k[0] for k in h.cells]
    ys = [k[1] for k in h.cells]
    zs = [k[2] for k in h.cells]
    x0, x1, y1, z0, z1 = min(xs) - 1, max(xs) + 1, max(ys) + 1, min(zs) - 1, max(zs) + 1
    outside = set()
    todo = deque()
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            todo.append((x, y1, z))
            outside.add((x, y1, z))
    while todo:
        x, y, z = todo.popleft()
        for dx, dy, dz in DIRS6:
            n = (x + dx, y + dy, z + dz)
            if n in outside or not (x0 <= n[0] <= x1 and 0 <= n[1] <= y1 and z0 <= n[2] <= z1):
                continue
            if stops_air(h.get(*n)):
                continue
            outside.add(n)
            todo.append(n)
    rooms = []
    seen = set()
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            for y in range(0, y1 + 1):
                c = (x, y, z)
                if c in outside or c in seen or stops_air(h.get(*c)):
                    continue
                room = {c}
                seen.add(c)
                q = deque([c])
                while q:
                    a = q.popleft()
                    for dx, dy, dz in DIRS6:
                        n = (a[0] + dx, a[1] + dy, a[2] + dz)
                        if n in seen or n in outside or stops_air(h.get(*n)) or not (x0 <= n[0] <= x1 and 0 <= n[1] <= y1 and z0 <= n[2] <= z1):
                            continue
                        seen.add(n)
                        room.add(n)
                        q.append(n)
                rooms.append(room)
    return outside, rooms


# ---- walking: the player's body against the real shapes, in half blocks
def extents(cell):
    if cell is None:
        return ()
    b, p = cell
    if soft(b) or is_door(b) or is_gate(b):
        return ()
    if b.endswith('_slab'):
        t = p.get('type', 'bottom')
        return ((0, 1),) if t == 'double' else ((0, .5),) if t == 'bottom' else ((.5, 1),)
    if b.endswith('_stairs'):
        return ((0, .5),) if p.get('half', 'bottom') == 'bottom' else ((.5, 1),)
    if b.endswith('_trapdoor'):
        if p.get('open') == 'true':
            return ((0, 1),)
        return () if p.get('half') == 'bottom' else ((.8, 1),)
    if b.endswith('_fence') or b.endswith('_wall'):
        return ((0, 1.5),)
    return ((0, 1),)


def surfaces(cell, y):
    # the heights (doubled) you can stand at on this cell
    if cell is None:
        return ()
    b, p = cell
    if soft(b) and b != 'ladder':
        return ()
    if b == 'ladder':
        return (2 * y,)
    if is_door(b) or is_gate(b) or b.endswith('_fence') or b.endswith('_wall') or b.endswith('_pane') or b == 'iron_bars':
        return ()
    if b.endswith('_slab'):
        t = p.get('type', 'bottom')
        return (2 * y + 1,) if t == 'bottom' else (2 * y + 2,)
    if b.endswith('_stairs') and p.get('half', 'bottom') == 'bottom':
        return (2 * y + 1, 2 * y + 2)
    if b.endswith('_trapdoor'):
        return ()
    return (2 * y + 2,)


class Walker:
    def __init__(self, h, doors=True):
        self.h, self.doors = h, doors
        xs = [k[0] for k in h.cells]
        zs = [k[2] for k in h.cells]
        self.x0, self.x1, self.z0, self.z1 = min(xs) - 2, max(xs) + 2, min(zs) - 2, max(zs) + 2
        self.top = max(k[1] for k in h.cells) + 2
        self.cols = {}

    def cell(self, x, y, z):
        return self.h.cells.get((x, y, z))

    def column(self, x, z):
        c = self.cols.get((x, z))
        if c is None:
            c = set()
            for y in range(0, self.top):
                c.update(surfaces(self.cell(x, y, z), y))
            # past the edge of the plot, and in bitten-off corners, there's plain ground
            if self.cell(x, 0, z) is None:
                c.add(2)
            self.cols[(x, z)] = c
        return c

    def clear(self, x, z, f2):
        f = f2 / 2
        for y in range(int(f), int(f + 1.79) + 1):
            cell = self.cell(x, y, z)
            if cell is None:
                continue
            if not self.doors and is_door(cell[0]):
                return False
            for a, b in extents(cell):
                if y + a < f + 1.8 and y + b > f:
                    return False
        return True

    def door_ok(self, x, z, f2, dx, dz):
        # you go through a door straight on, never sideways
        for y in (f2 // 2, f2 // 2 + 1):
            cell = self.cell(x, y, z)
            if cell and (is_door(cell[0]) or is_gate(cell[0])):
                facing = cell[1].get('facing', 'north')
                along_z = facing in ('north', 'south')
                if (along_z and dz == 0) or (not along_z and dx == 0):
                    return False
        return True

    def walk(self):
        start = (self.x0, self.z0, 2)
        seen = {start}
        q = deque([start])
        while q:
            x, z, f = q.popleft()
            for dx, dz in DIRS4:
                nx, nz = x + dx, z + dz
                if not (self.x0 <= nx <= self.x1 and self.z0 <= nz <= self.z1):
                    continue
                if not self.door_ok(x, z, f, dx, dz):
                    continue
                for s in self.column(nx, nz):
                    rise = s - f
                    if rise > 2 or rise < -8 or (nx, nz, s) in seen:
                        continue
                    if not self.clear(nx, nz, s) or not self.door_ok(nx, nz, s, dx, dz):
                        continue
                    if rise > 1 and not self.clear(x, z, f + 2):
                        continue
                    seen.add((nx, nz, s))
                    q.append((nx, nz, s))
            # ladders go straight up and down
            for up in (2, -2):
                n = f + up
                here = self.cell(x, f // 2, z)
                if here and here[0] == 'ladder' and (x, z, n) not in seen and self.clear(x, z, n):
                    seen.add((x, z, n))
                    q.append((x, z, n))
        return seen


def floors_in(room, walker):
    # the places in this room you could stand, if you could get there
    out = set()
    columns = {(x, z) for x, y, z in room}
    for x, z in columns:
        for s in walker.column(x, z):
            if (x, s // 2, z) in room and walker.clear(x, z, s):
                out.add((x, z, s))
    return out


def validate(h):
    errors = []
    outside, rooms = regions(h)
    walker = Walker(h)
    walked = walker.walk()
    real = []
    for room in rooms:
        floors = floors_in(room, walker)
        if len(floors) >= 4:
            real.append((room, floors))
    if not real:
        errors.append('no enclosed room at all: a gap lets the outside in, or there is no inside')
    # 1. doors: every door you can walk to, and at least one at the ground that leads inside
    doors = [(k, c) for k, c in h.cells.items() if is_door(c[0]) and c[1].get('half') == 'lower']
    if not doors:
        errors.append('no doors')
    ground_ok = False
    for (x, y, z), c in doors:
        f = c[1].get('facing', 'north')
        ax, az = (0, 1) if f in ('north', 'south') else (1, 0)
        a, b = (x + ax, y, z + az), (x - ax, y, z - az)
        reached = any((x, z, s) in walked for s in (2 * y, 2 * y + 1, 2 * y - 1))
        if not reached:
            errors.append(f'door at {x},{y},{z} cannot be walked to from outside: sealed or behind a wall')
            continue
        a_out, b_out = a in outside, b in outside
        if a_out and b_out:
            path = leak_from(h, outside, (x, y + 2, z), a, b) if (x, y, z) in h.cells else ''
            errors.append(f'door at {x},{y},{z} has outside air on both sides: {path}')
        if (a_out or b_out) and y <= 6:
            ground_ok = True
            # the front door has to be something you can see: no tunnel to it
            away = a if a_out else b
            dx, dz = away[0] - x, away[2] - z
            depth = 0
            while h.get(x + dx * (depth + 1), y, z + dz * (depth + 1)) is None and h.get(x + dx * (depth + 1), y + 1, z + dz * (depth + 1)) is None and depth < 12:
                depth += 1
            roofed = sum(1 for d in range(1, min(depth, 8) + 1) if h.get(x + dx * d, y + 2, z + dz * d) is not None)
            if roofed > 3:
                errors.append(f'door at {x},{y},{z} is at the end of a {roofed} block tunnel: nobody will see it')
    if not ground_ok:
        errors.append('no door at ground level leading from outside to inside')
    # 2. every room can be walked into, by the doors and stairs alone
    for room, floors in real:
        if not floors & walked:
            x, z, s = sorted(floors)[len(floors) // 2]
            errors.append(f'sealed room around {x},{s // 2},{z} ({len(floors)} floor blocks): no way in')
        else:
            stuck = floors - walked
            if len(stuck) > max(3, len(floors) // 4):
                x, z, s = sorted(stuck)[len(stuck) // 2]
                errors.append(f'part of the room around {x},{s // 2},{z} can\'t be reached ({len(stuck)} of {len(floors)} floor blocks)')
    # 3. no roof inside a room: roof pieces have sky over them, or another roof, never room air
    enclosed = set().union(*[r for r, _ in real]) if real else set()
    bad = [k for k in h.roof_owner if k in h.cells and (k[0], k[1] + 1, k[2]) in enclosed and h.get(*k) and h.get(*k).endswith(('_stairs', '_slab'))
           and h.cells[k][1].get('half') != 'top' and h.cells[k][1].get('type') != 'top']
    if bad:
        errors.append(f'{len(bad)} roof pieces with room air on top of them, first at {sorted(bad)[0]}: the roof is inside the house')
    for box in h.must_be_clear:
        x0, y0, z0, x1, y1, z1 = box
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    c = h.get(x, y, z)
                    if c and not soft(c) and (x, y, z) not in h.fixtures and not is_door(c):
                        errors.append(f'{c} at {x},{y},{z} is inside a room that should be empty')
    # 4. head height: 2 clear above every floor, except where the roof itself comes down at the eaves or stairs pass over
    for room, floors in real:
        for x, z, s in floors:
            pass
        columns = {}
        for x, y, z in room:
            columns.setdefault((x, z), []).append(y)
        for (x, z), ys in columns.items():
            ys.sort()
            run = 1
            for i in range(len(ys)):
                nxt = ys[i + 1] if i + 1 < len(ys) else None
                if nxt == ys[i] + 1:
                    run += 1
                    continue
                top = ys[i]
                above = h.get(x, top + 1, z)
                below = h.get(x, top - run, z)
                if run == 1 and below and not soft(below) and above and (x, top + 1, z) not in h.roof_owner and (x, top + 1, z) not in h.fixtures \
                        and (x, top - 1, z) not in h.fixtures and not above.endswith(('_stairs', '_slab')) and not below.endswith(('_stairs', '_slab')):
                    errors.append(f'only 1 block of head height at {x},{top},{z}')
                run = 1
    # 5. windows on every side
    for name, (dx, dz) in (('front', (0, 1)), ('back', (0, -1)), ('east', (1, 0)), ('west', (-1, 0))):
        if not sees_window(h, dx, dz):
            errors.append(f'no window to be seen on the {name}')
    # 6. the ground floor stands on the foundation
    for room, floors in real:
        for x, z, s in floors:
            if s // 2 <= 3 and h.get(x, 0, z) is None:
                errors.append(f'floor at {x},{z} has nothing under it')
                break
    # 7. roofs overhang
    for overhang, end, span, what in h.roof_specs:
        if span > 5 and (overhang < 1 or end < 1) and not h.flush_ok:
            errors.append(f'{what} roof (span {span}) has no overhang (over {overhang}, end {end})')
    # 8. not pitch dark
    lights = [k for k, c in h.cells.items() if any(c[0] == l or c[0].endswith(l) for l in LIGHTS)]
    for room, floors in real:
        for x, z, s in floors:
            y = s // 2
            if not any(abs(l[0] - x) + abs(l[1] - y) + abs(l[2] - z) <= 12 for l in lights):
                errors.append(f'floor at {x},{y},{z} is pitch dark')
                break
    return errors


def leak_from(h, outside, near, a, b):
    # walk the air from the inside of the door to open sky, and say where it got out
    f = h.cells[(near[0], near[1] - 2, near[2])][1].get('facing', 'north')
    dx, dz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[f]
    start = (near[0] - dx, near[1] - 2, near[2] - dz)
    ytop = max(k[1] for k in h.cells)
    open_sky = lambda c: not any(stops_air(h.get(c[0], y, c[2])) for y in range(c[1] + 1, ytop + 1))
    parent = {start: None}
    q = deque([start])
    while q:
        c = q.popleft()
        if open_sky(c):
            path = []
            while c is not None:
                path.append(c)
                c = parent[c]
            path.reverse()
            return 'air gets out: ' + ' '.join(f'{p[0]},{p[1]},{p[2]}' for p in path[-12:])
        for d in DIRS6:
            n = (c[0] + d[0], c[1] + d[1], c[2] + d[2])
            if n not in parent and not stops_air(h.get(*n)) and n[1] >= 0 and n[1] <= ytop + 1 and len(parent) < 200000:
                parent[n] = c
                q.append(n)
    return 'no path found'


def sees_window(h, dx, dz):
    xs = [k[0] for k in h.cells]
    zs = [k[2] for k in h.cells]
    ys = [k[1] for k in h.cells]
    x0, x1, z0, z1, y1 = min(xs), max(xs), min(zs), max(zs), max(ys)
    for y in range(2, y1 + 1):
        lines = range(z0, z1 + 1) if dx else range(x0, x1 + 1)
        for u in lines:
            x, z = (x1 if dx > 0 else x0, u) if dx else (u, z1 if dz > 0 else z0)
            while x0 <= x <= x1 and z0 <= z <= z1:
                b = h.get(x, y, z)
                if b and not soft(b) and not b.endswith('_trapdoor'):
                    if b.endswith('_pane') or b == 'iron_bars':
                        return True
                    break
                x -= dx
                z -= dz
    return False


# ---- finishing touches that need to know where the rooms are
def light_up(h):
    # a lantern every few blocks under each ceiling, and one by each outside door, so nothing is black inside even unfurnished
    outside, rooms = regions(h)
    walker = Walker(h)
    for room in rooms:
        floors = floors_in(room, walker)
        if len(floors) < 4:
            continue
        spots = []
        for x, y, z in room:
            above = h.get(x, y + 1, z)
            if not above or soft(above) or above.endswith(('_pane', '_fence', '_door')) or above == 'iron_bars' or above.endswith('_trapdoor'):
                continue
            if h.get(x, y, z) is not None:
                continue
            if h.cells.get((x, y + 1, z), (None, {}))[1].get('half') == 'top' and above.endswith('_stairs'):
                continue
            # three clear below it to the floor, so nobody knocks their head
            if any((x, y - i, z) not in room or h.get(x, y - i, z) is not None for i in (1, 2)):
                continue
            spots.append((x, y, z))
        spots.sort(key=lambda c: (c[0], c[2], c[1]))
        placed = [k for k, c in h.cells.items() if c[0] in ('lantern', 'soul_lantern') and k in room]
        lit = lambda f: any(abs(l[0] - f[0]) + abs(l[2] - f[1]) + abs(l[1] - f[2] // 2) <= 8 for l in placed)
        kind = 'soul_lantern' if any('soul' in c[0] for c in h.cells.values()) and 'wizard' in h.id else 'lantern'
        for f in sorted(floors, key=lambda c: (c[2], c[0], c[1])):
            if lit(f):
                continue
            dist = lambda s: abs(s[0] - f[0]) + abs(s[2] - f[1]) + abs(s[1] - f[2] // 2)
            best = min(spots, key=dist, default=None)
            if best is None or dist(best) > 10:
                # a tower with its ceiling far away: a lantern standing on the floor instead
                c = (f[0], f[2] // 2, f[1])
                if c in room and h.get(*c) is None:
                    h.put(c[0], c[1], c[2], kind, hanging=False)
                    placed.append(c)
                continue
            h.put(best[0], best[1], best[2], kind, hanging=True)
            placed.append(best)
    # outside each outer door
    for (x, y, z), c in list(h.cells.items()):
        if not is_door(c[0]) or c[1].get('half') != 'lower':
            continue
        f = c[1].get('facing', 'north')
        dx, dz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[f]
        if (x + dx, y, z + dz) not in outside:
            continue
        sx, sz = dz, dx
        done = False
        for u in (0, 1, -1, 2, -2):
            px, pz = x + dx + sx * u, z + dz + sz * u
            if h.get(px, y + 2, pz) is None and h.get(px, y + 3, pz) and not soft(h.get(px, y + 3, pz)) and not h.get(px, y + 3, pz).endswith('_fence'):
                h.put(px, y + 2, pz, 'lantern', hanging=True)
                done = True
                break
        if not done:
            for u in (1, -1):
                px, pz = x + dx + sx * u, z + dz + sz * u
                wall = h.get(x + sx * u, y + 1, z + sz * u)
                if h.get(px, y + 1, pz) is None and wall and not soft(wall) and not wall.endswith(('_pane', '_door')):
                    h.put(px, y + 1, pz, 'wall_torch', facing=f)
                    break
