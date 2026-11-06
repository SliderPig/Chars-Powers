# Proves the house checker catches what it's meant to: break a good house on purpose, each way, and it has to complain.
# Run from the charpowers folder:  python tools/test_house_checker.py
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from houses import base_set  # noqa: E402
from houses.check import is_door, validate  # noqa: E402


def good():
    return base_set.cosy_cottage().finish()


def front_door(h):
    # the door that opens on to the outside, south side
    return max((k for k, c in h.cells.items() if is_door(c[0]) and c[1].get('half') == 'lower'), key=lambda k: k[2])


def expect(name, h, word):
    errors = validate(h)
    hit = [e for e in errors if word in e]
    print(('ok    ' if hit else 'FAILED'), name, '->', (hit or errors or ['nothing reported'])[0][:110])
    return bool(hit)


def main():
    ok = True
    h = good()
    if validate(h):
        print('FAILED the good house has complaints:', validate(h)[:3])
        ok = False
    else:
        print('ok     the good house passes')

    # the sealed door Char found in the manor: an outer wall built over the doorway
    h = good()
    x, y, z = front_door(h)
    for yy in (y, y + 1):
        h.put(x, yy, z + 1, 'stone_bricks')
    ok &= expect('wall built in front of the door', h, 'door')

    # no way in at all
    h = good()
    for k in [k for k, c in h.cells.items() if is_door(c[0])]:
        h.put(k[0], k[1], k[2], 'stone_bricks')
    ok &= expect('doors bricked up', h, 'no door')

    # the roof inside a room
    h = good()
    for x in range(3, 8):
        h.put(x, 3, 4, 'dark_oak_stairs', facing='south', half='bottom')
        h.roof_owner[(x, 3, 4)] = 0
    ok &= expect('roof inside the house', h, 'roof')

    # a hole in a wall
    h = good()
    h.cells.pop((5, 4, 0))
    ok &= expect('hole in the back wall', h, 'outside air on both sides')

    # a hole in the roof
    h = good()
    for k in [k for k in h.roof_owner if k[2] == 2 and k[0] == 5 and k in h.cells]:
        h.cells.pop(k)
    ok &= expect('hole in the roof', h, 'outside air on both sides')

    # a room with no stairs up to it
    h = good()
    for k in list(h.flights):
        h.cells.pop(k, None)
        h.cells.pop((k[0], k[1] - 1, k[2]), None)
    for x in range(1, 2):
        for z in range(3, 6):
            h.put(x, 6, z, 'spruce_planks')
    ok &= expect('upstairs with no stairs', h, 'reached')

    # no windows on a side
    h = good()
    for k, c in list(h.cells.items()):
        if c[0].endswith('_pane') and k[2] == 0:
            h.put(k[0], k[1], k[2], 'calcite')
    ok &= expect('no back windows', h, 'no window')

    # pitch dark inside
    h = good()
    for k, c in list(h.cells.items()):
        if 'lantern' in c[0] or c[0] == 'campfire':
            del h.cells[k]
    ok &= expect('no lights', h, 'pitch dark')

    print('all good' if ok else 'THE CHECKER MISSED SOMETHING')
    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
