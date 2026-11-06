# Draws each house from tools/make_houses.py as an isometric picture, from flat colours only (no game textures in it).
#   python tools/preview_houses.py                 thumbnails for the picker (assets/charpowers/textures/gui/houses/<id>.png)
#   python tools/preview_houses.py out=<folder> [only=<id>,<id>] [scale=10] [back]     bigger pictures to look at
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import make_houses  # noqa: E402
from houses.lib import is_full  # noqa: E402

THUMBS = 'src/main/resources/assets/charpowers/textures/gui/houses'
COS, SIN = 0.866, 0.5

COLOURS = {
    'grass_block': (112, 160, 72), 'dirt': (130, 95, 66), 'coarse_dirt': (118, 86, 60), 'dirt_path': (150, 122, 72), 'gravel': (135, 130, 126),
    'podzol': (92, 66, 34), 'moss_block': (90, 120, 50), 'moss_carpet': (90, 120, 50), 'sand': (219, 207, 163),
    'cobblestone': (126, 126, 126), 'mossy_cobblestone': (108, 124, 92), 'stone': (125, 125, 125), 'andesite': (136, 136, 137), 'polished_andesite': (132, 135, 134),
    'stone_bricks': (122, 121, 122), 'mossy_stone_bricks': (112, 120, 96), 'cracked_stone_bricks': (118, 117, 118), 'chiseled_stone_bricks': (120, 119, 120),
    'smooth_stone': (158, 158, 158), 'tuff': (108, 109, 102), 'deepslate_bricks': (70, 70, 72), 'deepslate_tiles': (54, 54, 56), 'polished_deepslate': (72, 72, 74),
    'cobbled_deepslate': (77, 77, 80), 'chiseled_deepslate': (54, 54, 56), 'bricks': (150, 97, 83), 'mud_bricks': (137, 104, 79), 'packed_mud': (142, 107, 80),
    'white_terracotta': (210, 178, 161), 'calcite': (223, 224, 220), 'smooth_quartz': (236, 230, 223), 'quartz_block': (236, 230, 223), 'quartz_pillar': (236, 230, 223), 'chiseled_quartz_block': (231, 226, 218),
    'white_concrete': (207, 213, 214), 'pink_terracotta': (162, 78, 79), 'red_terracotta': (143, 61, 47), 'terracotta': (152, 94, 67), 'light_gray_terracotta': (135, 107, 98),
    'mushroom_stem': (203, 196, 185), 'birch_planks': (192, 175, 121), 'oak_planks': (162, 131, 79), 'spruce_planks': (115, 85, 49), 'dark_oak_planks': (67, 43, 20),
    'mangrove_planks': (117, 54, 48), 'cherry_planks': (226, 178, 172), 'acacia_planks': (168, 90, 50), 'crimson_planks': (101, 48, 70),
    'oak_log': (109, 85, 51), 'spruce_log': (59, 37, 17), 'birch_log': (216, 215, 210), 'dark_oak_log': (60, 46, 26), 'cherry_log': (54, 33, 44), 'mangrove_log': (84, 66, 41),
    'stripped_oak_log': (177, 144, 86), 'stripped_spruce_log': (115, 89, 52), 'stripped_birch_log': (197, 176, 118), 'stripped_dark_oak_log': (72, 56, 36), 'stripped_cherry_log': (214, 145, 148), 'stripped_mangrove_log': (119, 54, 47),
    'oak_leaves': (72, 110, 50), 'spruce_leaves': (56, 84, 56), 'azalea_leaves': (90, 120, 50), 'flowering_azalea_leaves': (112, 118, 74), 'cherry_leaves': (232, 170, 200), 'birch_leaves': (110, 140, 70),
    'glass_pane': (190, 220, 235), 'glass': (190, 220, 235), 'tinted_glass': (60, 60, 70), 'white_stained_glass_pane': (240, 240, 240), 'light_blue_stained_glass_pane': (120, 170, 220), 'purple_stained_glass_pane': (130, 70, 180), 'pink_stained_glass_pane': (240, 150, 190), 'yellow_stained_glass_pane': (230, 210, 90),
    'iron_bars': (120, 125, 125), 'hay_block': (180, 160, 60), 'composter': (100, 70, 40), 'cauldron': (70, 70, 72), 'water_cauldron': (70, 70, 72), 'water': (60, 110, 200), 'campfire': (200, 120, 50), 'soul_campfire': (80, 200, 220),
    'lantern': (240, 190, 100), 'soul_lantern': (120, 220, 230), 'chain': (70, 75, 80), 'flower_pot': (120, 70, 50), 'end_rod': (235, 230, 220), 'lightning_rod': (190, 120, 90), 'bell': (220, 190, 80),
    'amethyst_block': (130, 90, 180), 'sea_lantern': (170, 210, 200), 'glowstone': (240, 200, 120), 'ochre_froglight': (240, 220, 160), 'pearlescent_froglight': (240, 210, 225), 'gold_block': (240, 205, 70), 'copper_block': (190, 110, 80),
    'waxed_oxidized_copper': (84, 160, 130), 'waxed_oxidized_cut_copper': (84, 160, 130), 'waxed_weathered_cut_copper': (110, 150, 110), 'waxed_exposed_cut_copper': (160, 125, 100), 'waxed_cut_copper': (190, 110, 80),
    'honeycomb_block': (230, 160, 40), 'wall_torch': (240, 190, 100), 'amethyst_cluster': (170, 120, 210), 'red_mushroom': (200, 50, 40), 'brown_mushroom': (150, 110, 80),
    'bookshelf': (150, 110, 60), 'dripstone_block': (134, 107, 92), 'prismarine_bricks': (90, 160, 140), 'dark_prismarine': (55, 95, 80), 'purpur_block': (170, 125, 170), 'blackstone': (42, 36, 42), 'polished_blackstone_bricks': (48, 42, 50), 'gilded_blackstone': (70, 50, 40), 'black_terracotta': (37, 22, 16), 'green_terracotta': (76, 83, 42), 'blue_terracotta': (74, 60, 91), 'cyan_terracotta': (87, 91, 91), 'yellow_terracotta': (186, 133, 35),
}
FLOWER = {'poppy': (220, 50, 40), 'dandelion': (240, 210, 60), 'azure_bluet': (225, 230, 225), 'oxeye_daisy': (235, 235, 215), 'cornflower': (70, 100, 210), 'allium': (180, 110, 220),
          'blue_orchid': (60, 170, 220), 'pink_tulip': (240, 150, 190), 'red_tulip': (220, 60, 50), 'white_tulip': (235, 235, 235), 'orange_tulip': (240, 140, 60), 'lily_of_the_valley': (235, 240, 230),
          'lilac': (190, 140, 200), 'rose_bush': (190, 40, 50), 'peony': (235, 170, 215), 'pink_petals': (240, 170, 210), 'fern': (80, 130, 60), 'grass': (100, 150, 70), 'lily_pad': (60, 120, 50), 'azalea': (90, 120, 50), 'flowering_azalea': (130, 120, 90),
          'tall_grass': (100, 150, 70), 'large_fern': (80, 130, 60), 'cherry_sapling': (230, 170, 200), 'bamboo': (100, 150, 60), 'sugar_cane': (130, 180, 90)}


def base_colour(b):
    if b in COLOURS:
        return COLOURS[b]
    if b in FLOWER:
        return FLOWER[b]
    for suffix in ('_stairs', '_slab', '_fence_gate', '_fence', '_wall', '_trapdoor', '_door', '_carpet'):
        if b.endswith(suffix):
            root = b[:-len(suffix)]
            for tail in ('', '_planks', 's', '_block', '_bricks'):
                if root + tail in COLOURS:
                    return COLOURS[root + tail]
            if root == 'brick':
                return COLOURS['bricks']
            if root == 'cobbled_deepslate':
                return COLOURS['cobbled_deepslate']
            if root == 'smooth_stone':
                return COLOURS['smooth_stone']
            if root == 'cut_copper':
                return COLOURS['waxed_cut_copper']
    if b.startswith('potted_'):
        return FLOWER.get(b[7:], (120, 70, 50))
    if b.endswith('candle'):
        return (235, 170, 200)
    MISSING.add(b)
    return (255, 0, 255)


MISSING = set()


def stair_boxes(p):
    half = p.get('half', 'bottom')
    f = p.get('facing', 'north')
    shape = p.get('shape', 'straight')
    low, high = (0, .5) if half == 'bottom' else (.5, 1)
    alow, ahigh = (.5, 1) if half == 'bottom' else (0, .5)
    out = [(0, low, 0, 1, high, 1)]
    q = {'north': (0, 0, 1, .5), 'south': (0, .5, 1, 1), 'west': (0, 0, .5, 1), 'east': (.5, 0, 1, 1)}
    x0, z0, x1, z1 = q[f]
    # a corner piece takes a quarter (outer) or adds a quarter (inner)
    left = {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}[f]
    right = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}[f]
    if shape.startswith('outer'):
        side = left if shape == 'outer_left' else right
        a = q[side]
        x0, z0, x1, z1 = max(x0, a[0]), max(z0, a[1]), min(x1, a[2]), min(z1, a[3])
        out.append((x0, alow, z0, x1, ahigh, z1))
    else:
        out.append((x0, alow, z0, x1, ahigh, z1))
        if shape.startswith('inner'):
            side = left if shape == 'inner_left' else right
            a = q[side]
            out.append((a[0], alow, a[1], a[2], ahigh, a[3]))
    return out


def boxes_for(b, p):
    if b.endswith('_stairs'):
        return stair_boxes(p)
    if b.endswith('_slab'):
        t = p.get('type', 'bottom')
        return [(0, 0, 0, 1, 1, 1)] if t == 'double' else [(0, 0, 0, 1, .5, 1)] if t == 'bottom' else [(0, .5, 0, 1, 1, 1)]
    if b.endswith('_trapdoor'):
        if p.get('open') == 'true':
            f = p.get('facing', 'north')
            return [{'north': (0, 0, .81, 1, 1, 1), 'south': (0, 0, 0, 1, 1, .19), 'west': (.81, 0, 0, 1, 1, 1), 'east': (0, 0, 0, .19, 1, 1)}[f]]
        return [(0, 0, 0, 1, .19, 1)] if p.get('half') == 'bottom' else [(0, .81, 0, 1, 1, 1)]
    if b.endswith('_door'):
        f = p.get('facing', 'north')
        return [{'north': (0, 0, .81, 1, 1, 1), 'south': (0, 0, 0, 1, 1, .19), 'west': (.81, 0, 0, 1, 1, 1), 'east': (0, 0, 0, .19, 1, 1)}[f]]
    if b.endswith('_fence') or b.endswith('_wall') or b.endswith('_pane') or b == 'iron_bars':
        t = {'_fence': .13, '_wall': .25, '_pane': .06}.get(next((s for s in ('_fence', '_wall', '_pane') if b.endswith(s)), ''), .06)
        c0, c1 = .5 - t, .5 + t
        out = [(c0, 0, c0, c1, 1, c1)] if not b.endswith('_pane') and b != 'iron_bars' or not any(p.get(d) in ('true',) for d in ('north', 'south', 'east', 'west')) else []
        if b.endswith('_wall') and p.get('up') == 'false':
            out = []
        for d, box in (('north', (c0, 0, 0, c1, 1, .5)), ('south', (c0, 0, .5, c1, 1, 1)), ('west', (0, 0, c0, .5, 1, c1)), ('east', (.5, 0, c0, 1, 1, c1))):
            v = p.get(d)
            if v in ('true', 'low', 'tall'):
                x0, y0, z0, x1, y1, z1 = box
                if b.endswith('_fence'):
                    out.append((x0, .4, z0, x1, .85, z1))
                elif b.endswith('_wall'):
                    out.append((x0, 0, z0, x1, .85 if v == 'low' else 1, z1))
                else:
                    out.append(box)
        return out or [(c0, 0, c0, c1, 1, c1)]
    if b.endswith('_fence_gate'):
        f = p.get('facing', 'north')
        return [(0, .3, .4, 1, .9, .6)] if f in ('north', 'south') else [(.4, .3, 0, .6, .9, 1)]
    if b.endswith('_carpet'):
        return [(0, 0, 0, 1, .06, 1)]
    if b == 'dirt_path':
        return [(0, 0, 0, 1, .94, 1)]
    if b in ('lantern', 'soul_lantern'):
        return [(.3, .0, .3, .7, .5, .7)] if p.get('hanging') != 'true' else [(.3, .1, .3, .7, .6, .7)]
    if b == 'chain' or b == 'end_rod' or b == 'lightning_rod':
        return [(.44, 0, .44, .56, 1, .56)]
    if b in FLOWER or b.startswith('potted_') or b == 'flower_pot':
        if b == 'pink_petals':
            return [(0, 0, 0, 1, .1, 1)]
        if b == 'lily_pad':
            return [(0, 0, 0, 1, .06, 1)]
        return [(.3, 0, .3, .7, .7, .7)]
    if b in ('amethyst_cluster', 'red_mushroom', 'brown_mushroom'):
        return [(.3, 0, .3, .7, .6, .7)]
    if b.endswith('torch') or b.endswith('candle'):
        return [(.42, 0, .42, .58, .6, .58)]
    if b in ('campfire', 'soul_campfire'):
        return [(.05, 0, .05, .95, .45, .95)]
    return [(0, 0, 0, 1, 1, 1)]


def shade(c, f, jitter=0):
    return tuple(max(0, min(255, int(v * f + jitter))) for v in c)


def render(house, scale=10, view='front', pad=6, ss=2):
    S = scale * ss
    # a view is a turn of the whole house: draw the +z and +x faces of what's turned towards us
    cells = house.cells
    xs = [k[0] for k in cells]
    zs = [k[2] for k in cells]
    ys = [k[1] for k in cells]
    x0, x1, z0, z1 = min(xs), max(xs) + 1, min(zs), max(zs) + 1
    h = max(ys) + 1
    w, d = x1 - x0, z1 - z0
    mirror = view == 'left'
    back = view == 'back'

    def turned(x, y, z):
        # x, z measured in the view's own frame
        rx, rz = x - x0, z - z0
        if mirror:
            rx = w - rx
        if back:
            rx, rz = w - rx, d - rz
        return rx, y, rz

    def proj(x, y, z):
        return ((x - z) * COS * S, (x + z) * SIN * S - y * S)

    px = [proj(a, 0, b) for a in (0, w) for b in (0, d)] + [proj(a, h, b) for a in (0, w) for b in (0, d)]
    minx, maxx = min(p[0] for p in px), max(p[0] for p in px)
    miny, maxy = min(p[1] for p in px), max(p[1] for p in px)
    W, H = int(maxx - minx + pad * 2 * ss), int(maxy - miny + pad * 2 * ss)
    ox, oy = pad * ss - minx, pad * ss - miny
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)

    def poly(pts, col):
        dr.polygon([(p[0] + ox, p[1] + oy) for p in pts], fill=col + (255,))

    items = []
    for (x, y, z), (b, p) in cells.items():
        full = is_full(b)
        rx, ry, rz = turned(x, y, z)
        for bx in boxes_for(b, p):
            ax0, ay0, az0, ax1, ay1, az1 = bx
            if mirror:
                ax0, ax1 = 1 - ax1, 1 - ax0
                rrx = rx - 1
            else:
                rrx = rx
            rrz = rz
            if back:
                ax0, ax1 = 1 - ax1, 1 - ax0
                az0, az1 = 1 - az1, 1 - az0
                rrx, rrz = rx - 1, rz - 1
            items.append((rrx + ax0, y + ay0, rrz + az0, rrx + ax1, y + ay1, rrz + az1, b, full, (x, y, z)))
    items.sort(key=lambda t: (t[0] + t[3]) / 2 + (t[2] + t[5]) / 2 + (t[1] + t[4]) / 2)
    for ax0, ay0, az0, ax1, ay1, az1, b, full, (x, y, z) in items:
        c = base_colour(b)
        j = ((x * 73856093) ^ (y * 19349663) ^ (z * 83492791)) % 13 - 6
        if b.endswith('_log') or b.endswith('_planks'):
            j = j // 2
        alpha_glass = b.endswith('_pane') or b == 'glass'
        top = shade(c, 1.0, j)
        left = shade(c, 0.80, j)
        right = shade(c, 0.62, j)
        if alpha_glass:
            top, left, right = shade(c, .95), shade(c, .8), shade(c, .55)
        poly([proj(ax0, ay1, az0), proj(ax1, ay1, az0), proj(ax1, ay1, az1), proj(ax0, ay1, az1)], top)
        poly([proj(ax0, ay1, az1), proj(ax1, ay1, az1), proj(ax1, ay0, az1), proj(ax0, ay0, az1)], left)
        poly([proj(ax1, ay1, az0), proj(ax1, ay1, az1), proj(ax1, ay0, az1), proj(ax1, ay0, az0)], right)
    return img.resize((W // ss, H // ss), Image.LANCZOS)


def thumbnail(img, tw=180, th=130):
    # fit into a card, soft shadow free, keep the house's own transparent background
    bbox = img.getbbox()
    img = img.crop(bbox)
    f = min(tw / img.width, th / img.height)
    img = img.resize((max(1, int(img.width * f)), max(1, int(img.height * f))), Image.LANCZOS)
    out = Image.new('RGBA', (tw, th), (0, 0, 0, 0))
    out.paste(img, ((tw - img.width) // 2, (th - img.height) // 2))
    return out


def main():
    args = dict(a.split('=', 1) if '=' in a else (a, '1') for a in sys.argv[1:])
    houses = make_houses.all_houses()
    only = args.get('only')
    if only:
        houses = [h for h in houses if h.id in only.split(',')]
    outdir = args.get('out')
    if outdir:
        os.makedirs(outdir, exist_ok=True)
        scale = int(args.get('scale', 10))
        for h in houses:
            views = ['front', 'left', 'back'] if 'all' in args else ['back'] if 'back' in args else ['front']
            for v in views:
                render(h, scale, v).save(os.path.join(outdir, f'{h.id}_{v}.png'))
    else:
        os.makedirs(THUMBS, exist_ok=True)
        for h in houses:
            thumbnail(render(h, 8)).save(os.path.join(THUMBS, h.id + '.png'))
    if MISSING:
        print('no colour for', sorted(MISSING))


if __name__ == '__main__':
    main()
