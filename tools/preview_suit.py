# A test picture of the Invincible Suit: the real armour sheet on a body, the items, the station and the battery on screen.
# Same boxes, texture layout and drawing order as the game, so overlaps come out the same. From the charpowers folder:
#   python tools/preview_suit.py
import math
import os

import numpy as np
from PIL import Image, ImageDraw

A = 'src/main/resources/assets/charpowers/textures/'
OUT = 'docs/previews/suit_lime_silver.png'
FONT = os.path.join(os.path.expanduser('~'), 'Downloads', 'powerguide', 'gui', 'mcfont', 'ascii.png')


def cut(sheet, u, v, w, h):
    return np.array(sheet.crop((u, v, u + w, v + h)))


# A face: its picture, one corner, the two edges from that corner, how much light it gets, which way it points.
def box_faces(sheet, lo, size, inflate, uv, mirror=False):
    (x0, y0, z0), (w, h, d) = lo, size
    x1, y1, z1 = x0 + w + inflate, y0 + h + inflate, z0 + d + inflate
    x0, y0, z0 = x0 - inflate, y0 - inflate, z0 - inflate
    u, v = uv
    W, H, D = x1 - x0, y1 - y0, z1 - z0
    front, back = cut(sheet, u + d, v + d, w, h), cut(sheet, u + d + w + d, v + d, w, h)
    right, left = cut(sheet, u, v + d, d, h), cut(sheet, u + d + w, v + d, d, h)
    top = cut(sheet, u + d, v, w, d)
    down = (0, -H, 0)
    if mirror:
        return [(front, (x1, y1, z1), (-W, 0, 0), down, 0.82, (0, 0, 1)), (back, (x0, y1, z0), (W, 0, 0), down, 0.82, (0, 0, -1)),
                (right, (x1, y1, z0), (0, 0, D), down, 0.62, (1, 0, 0)), (left, (x0, y1, z1), (0, 0, -D), down, 0.62, (-1, 0, 0)),
                (top, (x1, y1, z0), (-W, 0, 0), (0, 0, D), 1.0, (0, 1, 0))]
    return [(front, (x0, y1, z1), (W, 0, 0), down, 0.82, (0, 0, 1)), (back, (x1, y1, z0), (-W, 0, 0), down, 0.82, (0, 0, -1)),
            (right, (x0, y1, z0), (0, 0, D), down, 0.62, (-1, 0, 0)), (left, (x1, y1, z1), (0, 0, -D), down, 0.62, (1, 0, 0)),
            (top, (x0, y1, z0), (W, 0, 0), (0, 0, D), 1.0, (0, 1, 0))]


def body_faces(sheet):
    out = []
    out += box_faces(sheet, (-4, 24, -4), (8, 8, 8), 1, (0, 0))
    out += box_faces(sheet, (-4, 12, -2), (8, 12, 4), 1, (16, 16))
    out += box_faces(sheet, (-8, 12, -2), (4, 12, 4), 1, (40, 16))
    out += box_faces(sheet, (4, 12, -2), (4, 12, 4), 1, (40, 16), True)
    out += box_faces(sheet, (-3.9, 0, -2), (4, 12, 4), 1, (0, 16))
    out += box_faces(sheet, (-0.1, 0, -2), (4, 12, 4), 1, (0, 16), True)
    return out


def block_faces(top, side):
    t, s = np.array(top), np.array(side)
    down = (0, -16, 0)
    return [(s, (-8, 16, 8), (16, 0, 0), down, 0.82, (0, 0, 1)), (s, (8, 16, 8), (0, 0, -16), down, 0.62, (1, 0, 0)),
            (s, (-8, 16, -8), (0, 0, 16), down, 0.62, (-1, 0, 0)), (t, (-8, 16, -8), (16, 0, 0), (0, 0, 16), 1.0, (0, 1, 0))]


def draw(im, faces, cx, cy, yaw, pitch, unit):
    pixels = np.array(im)
    height, width = pixels.shape[:2]
    depth = np.full((height, width), -1e9)
    cyaw, syaw, cpit, spit = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)

    def turn(p):
        rx = p[0] * cyaw + p[2] * syaw
        rz = -p[0] * syaw + p[2] * cyaw
        return rx, p[1] * cpit - rz * spit, rz * cpit + p[1] * spit

    for tex, corner, across, down, shade, normal in faces:
        if turn(normal)[2] <= 1e-6:
            continue
        p, a, b = turn(corner), turn(across), turn(down)
        ox, oy = cx + p[0] * unit, cy - p[1] * unit
        ax, ay, bx, by = a[0] * unit, -a[1] * unit, b[0] * unit, -b[1] * unit
        det = ax * by - ay * bx
        xs_all = [ox, ox + ax, ox + bx, ox + ax + bx]
        ys_all = [oy, oy + ay, oy + by, oy + ay + by]
        x0, x1 = max(0, int(min(xs_all))), min(width, int(max(xs_all)) + 1)
        y0, y1 = max(0, int(min(ys_all))), min(height, int(max(ys_all)) + 1)
        xs, ys = np.meshgrid(np.arange(x0, x1), np.arange(y0, y1))
        dx, dy = xs + 0.5 - ox, ys + 0.5 - oy
        fu, fv = (dx * by - dy * bx) / det, (ax * dy - ay * dx) / det
        inside = (fu >= 0) & (fu < 1) & (fv >= 0) & (fv < 1)
        rows, cols = tex.shape[:2]
        tu = np.clip((fu * cols).astype(int), 0, cols - 1)
        tv = np.clip((fv * rows).astype(int), 0, rows - 1)
        colour = tex[tv, tu]
        d = p[2] + a[2] * fu + b[2] * fv
        ok = inside & (colour[..., 3] > 0) & (d >= depth[ys, xs] - 1e-6)
        pixels[ys[ok], xs[ok]] = (colour[ok][:, :3] * shade).astype(np.uint8)
        depth[ys[ok], xs[ok]] = d[ok]
    im.paste(Image.fromarray(pixels))


def text(target, s, x, y, colour, scale=2):
    font = Image.open(FONT).convert('RGBA')
    for ch in s:
        n = ord(ch)
        cell = font.crop((n % 16 * 8, n // 16 * 8, n % 16 * 8 + 8, n // 16 * 8 + 8))
        width = 8
        while width > 0 and all(cell.getpixel((width - 1, yy))[3] == 0 for yy in range(8)):
            width -= 1
        for yy in range(8):
            for xx in range(8):
                if cell.getpixel((xx, yy))[3] > 0:
                    target.paste(colour, (x + xx * scale, y + yy * scale, x + (xx + 1) * scale, y + (yy + 1) * scale))
        x += (width + 1 if ch != ' ' else 4) * scale


def panel(target, box, top, bottom, ground):
    d = ImageDraw.Draw(target)
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        t = (y - y0) / (y1 - y0)
        d.line((x0, y, x1, y), fill=tuple(int(top[k] + (bottom[k] - top[k]) * t) for k in range(3)))
    d.rectangle((x0, y1 - 44, x1, y1), fill=ground)


# At night the plates fall into shadow, but the lime lines are bright on their own.
def in_the_dark(sheet):
    dark = sheet.copy()
    px = dark.load()
    for y in range(dark.size[1]):
        for x in range(dark.size[0]):
            c = px[x, y]
            if c[3] and not (c[1] > 190 and c[1] > c[0] + 40):
                px[x, y] = (int(c[0] * 0.42), int(c[1] * 0.42), int(c[2] * 0.46), c[3])
    return dark


def main():
    suit = Image.open(A + 'models/armor/invincible.png').convert('RGBA')
    slim = Image.open(A + 'models/armor/invincible_slim.png').convert('RGBA')
    im = Image.new('RGB', (1500, 900), (24, 26, 30))
    panel(im, (16, 16, 1040, 500), (129, 177, 255), (205, 225, 255), (104, 160, 72))
    panel(im, (1056, 16, 1484, 500), (16, 18, 26), (30, 32, 42), (46, 46, 50))
    feet = 486
    text(im, 'Daylight', 28, 26, (30, 40, 60))
    text(im, 'Caves / night', 1068, 26, (200, 205, 215))
    for x, sheet, yaw in ((180, suit, -0.5), (500, suit, math.pi - 0.5), (820, slim, -0.5)):
        draw(im, body_faces(sheet), x, feet, yaw, 0.18, 11.5)
    text(im, 'Suit', 300, 60, (30, 40, 60))
    text(im, 'Back', 620, 60, (30, 40, 60))
    text(im, 'Slim suit', 906, 60, (30, 40, 60))
    draw(im, body_faces(in_the_dark(suit)), 1270, feet, -0.5, 0.18, 11.5)

    # the items, big and at the size they really are at GUI scale 3
    d = ImageDraw.Draw(im)
    names = ['invincible_suit', 'suit_head_part', 'suit_body_part', 'suit_leg_part', 'suit_boot_part', 'shield_module', 'scanner_module', 'strength_module', 'speed_module']
    d.rectangle((16, 516, 830, 884), fill=(198, 198, 198))
    text(im, 'Items in the bag', 28, 526, (64, 64, 64))
    for i, n in enumerate(names):
        tex = Image.open(A + f'item/{n}.png').convert('RGBA')
        big = tex.resize((80, 80), Image.NEAREST)
        x, y = 30 + i % 5 * 158, 562 + i // 5 * 110
        d.rectangle((x - 4, y - 4, x + 84, y + 84), fill=(139, 139, 139))
        im.paste(big, (x, y), big)
        small = tex.resize((48, 48), Image.NEAREST)
        d.rectangle((x + 92, y + 16, x + 144, y + 68), fill=(139, 139, 139))
        im.paste(small, (x + 94, y + 18), small)

    # the station, off and charging
    d.rectangle((846, 516, 1200, 884), fill=(40, 44, 50))
    text(im, 'Charging station', 858, 526, (222, 228, 233))
    for x, state in ((940, 'off'), (1106, 'on')):
        top = Image.open(A + f'block/charging_station_top_{state}.png').convert('RGBA')
        side = Image.open(A + f'block/charging_station_side_{state}.png').convert('RGBA')
        draw(im, block_faces(top, side), x, 790, -0.78, 0.5, 5.6)
        text(im, state, x - 18, 830, (150, 255, 30) if state == 'on' else (146, 155, 163))

    # the battery on screen, at GUI scale 2, over a game-ish background
    d = ImageDraw.Draw(im)
    d.rectangle((1216, 516, 1484, 884), fill=(86, 118, 70))
    text(im, 'On screen', 1228, 526, (240, 240, 240))
    for y, level in ((566, 82), (666, 12)):
        x = 1228
        d.rectangle((x, y, x + 140 - 1, y + 44 - 1), fill=(34, 38, 43))
        d.rectangle((x + 2, y + 2, x + 140 - 3, y + 44 - 3), fill=(92, 100, 108))
        text(im, f'Suit {level}%', x + 8, y + 6, (244, 248, 250))
        d.rectangle((x + 8, y + 26, x + 131, y + 35), fill=(34, 38, 43))
        d.rectangle((x + 10, y + 28, x + 10 + int(120 * level / 100), y + 33), fill=(255, 90, 90) if level <= 15 else (150, 255, 30))
        mx = x + 144
        for m in 'SP':
            d.rectangle((mx, y, mx + 21, y + 21), fill=(34, 38, 43))
            d.rectangle((mx + 2, y + 2, mx + 19, y + 19), fill=(92, 100, 108))
            text(im, m, mx + 6, y + 4, (222, 228, 233))
            mx += 24
    im.save(OUT)
    print('wrote', OUT)


if __name__ == '__main__':
    main()
