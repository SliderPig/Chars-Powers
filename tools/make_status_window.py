# Game Player's Solo Leveling look: a see-through hologram panel and the small notification one, drawn big and shrunk down so
# everything stays smooth. The screen scales them smoothly too. From the charpowers folder:  python tools/make_status_window.py
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = 'src/main/resources/assets/charpowers/textures/gui/'
LINE, GLOW, TRACE = (205, 242, 255), (40, 170, 255), (30, 130, 235)


def chamfered(x0, y0, x1, y1, cut):
    return [(x0 + cut, y0), (x1 - cut, y0), (x1, y0 + cut), (x1, y1 - cut), (x1 - cut, y1), (x0 + cut, y1), (x0, y1 - cut), (x0, y0 + cut)]


def glow_line(base, draw_it, blur, strength):
    layer = Image.new('RGBA', base.size, (0, 0, 0, 0))
    draw_it(ImageDraw.Draw(layer))
    soft = layer.filter(ImageFilter.GaussianBlur(blur))
    for _ in range(strength):
        base.alpha_composite(soft)


def traces(d, rng, box, count, alpha):
    x0, y0, x1, y1 = box
    for _ in range(count):
        x, y = rng.randint(x0, x1), rng.randint(y0, y1)
        points = [(x, y)]
        for _ in range(rng.randint(2, 5)):
            if rng.random() < 0.5:
                x += rng.choice((-1, 1)) * rng.randint(20, 90)
            else:
                y += rng.choice((-1, 1)) * rng.randint(20, 90)
            x, y = max(x0, min(x1, x)), max(y0, min(y1, y))
            points.append((x, y))
        d.line(points, fill=TRACE + (alpha,), width=2)
        d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=TRACE + (alpha + 30,))


def window(W, H, out_w, out_h, name, seed, busy, banner=False):
    rng = random.Random(seed)
    margin = 34
    x0, y0, x1, y1 = margin, margin, W - margin, H - margin
    shape = chamfered(x0, y0, x1, y1, 26)
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    # the see-through panel, darker towards the bottom
    mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(mask).polygon(shape, fill=255)
    fill = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fill)
    for y in range(H):
        t = y / H
        fd.line((0, y, W, y), fill=(int(6 + 4 * (1 - t)), int(18 + 14 * (1 - t)), int(40 + 22 * (1 - t)), int(150 + 45 * t)))
    im.paste(fill, (0, 0), mask)
    # faint circuit lines and motes, more of them at the edges
    detail = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    dd = ImageDraw.Draw(detail)
    edge = int(W * 0.22)
    traces(dd, rng, (x0 + 8, y0 + 8, x0 + edge, y1 - 8), busy, 46)
    traces(dd, rng, (x1 - edge, y0 + 8, x1 - 8, y1 - 8), busy, 46)
    traces(dd, rng, (x0 + 8, y0 + 8, x1 - 8, y1 - 8), busy // 3, 16)
    for _ in range(busy * 3):
        x, y = rng.randint(x0 + 8, x1 - 8), rng.randint(y0 + 8, y1 - 8)
        dd.ellipse((x, y, x + 2, y + 2), fill=(170, 225, 255, rng.randint(40, 130)))
    im.alpha_composite(Image.composite(detail, Image.new('RGBA', (W, H), (0, 0, 0, 0)), mask))
    # the frame: soft glow, then a thin bright line
    closed = shape + [shape[0]]
    glow_line(im, lambda d: d.line(closed, fill=GLOW + (255,), width=12), 20, 2)
    glow_line(im, lambda d: d.line(closed, fill=GLOW + (255,), width=6), 7, 1)
    d = ImageDraw.Draw(im)
    d.line(closed, fill=LINE + (235,), width=3)
    inner = 16
    d.rectangle((x0 + inner, y0 + inner, x1 - inner, y1 - inner), outline=(150, 215, 255, 70), width=2)

    # layers: a raised band round the edge, and the middle sunk into it with a shadow along its top
    band = int(W * 0.022)
    sunk = chamfered(x0 + band, y0 + band, x1 - band, y1 - band, 18)
    ring_mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(ring_mask).polygon(shape, fill=255)
    ImageDraw.Draw(ring_mask).polygon(sunk, fill=0)
    ring = Image.new('RGBA', (W, H), (34, 92, 150, 120))
    rd = ImageDraw.Draw(ring)
    for y in range(H):
        rd.line((0, y, W, y), fill=(int(70 - 40 * y / H), int(150 - 70 * y / H), int(220 - 90 * y / H), 125))
    im.paste(ring, (0, 0), ring_mask)
    d = ImageDraw.Draw(im)
    shade = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shade).line(sunk[:2] + [sunk[2]], fill=(0, 0, 0, 210), width=16)
    ImageDraw.Draw(shade).line([sunk[7], sunk[0]], fill=(0, 0, 0, 170), width=12)
    im.alpha_composite(shade.filter(ImageFilter.GaussianBlur(9)))
    d.line(sunk + [sunk[0]], fill=(120, 200, 255, 120), width=2)
    for corner in sunk[1::2]:
        d.ellipse((corner[0] - 7, corner[1] - 7, corner[0] + 7, corner[1] + 7), fill=(200, 240, 255, 230))

    # bright bars top and bottom, and notched strips down the sides, like the show's frame
    def bars(dr):
        cx = W // 2
        span = int(W * 0.26)
        dr.line((cx - span, y0, cx + span, y0), fill=(235, 250, 255, 255), width=8)
        dr.line((cx - span // 2, y1, cx + span // 2, y1), fill=(235, 250, 255, 255), width=8)
        step = int(H * 0.12)
        for sign, x in ((1, x0), (-1, x1)):
            for top in (y0 + step, y1 - step * 2):
                dr.line([(x, top), (x + sign * 10, top + 12), (x + sign * 10, top + step - 12), (x, top + step)], fill=(225, 246, 255, 255), width=5)
    glow_line(im, bars, 9, 2)
    bars(ImageDraw.Draw(im))
    if banner:
        # a title banner sitting over the top edge, with little tails
        bw, bh = int(W * 0.17), int(H * 0.052)
        cx, cy = W // 2, y0 + int(H * 0.056)
        tail = int(bw * 0.2)
        ribbon = [(cx - bw, cy - bh), (cx + bw, cy - bh), (cx + bw + tail, cy), (cx + bw, cy + bh), (cx - bw, cy + bh), (cx - bw - tail, cy)]
        glow_line(im, lambda dr: dr.polygon(ribbon, fill=(0, 0, 0, 180)), 12, 1)
        glow_line(im, lambda dr: dr.line(ribbon + [ribbon[0]], fill=GLOW + (255,), width=8), 9, 1)
        rd = ImageDraw.Draw(im)
        rd.polygon(ribbon, fill=(14, 52, 92, 245))
        rd.polygon([(cx - bw + 4, cy - bh + 5), (cx + bw - 4, cy - bh + 5), (cx + bw + tail - 22, cy - 8), (cx - bw - tail + 22, cy - 8)], fill=(34, 96, 156, 170))
        rd.line(ribbon + [ribbon[0]], fill=LINE + (250,), width=3)
    small = im.resize((out_w, out_h), Image.LANCZOS)
    small.save(OUT + name, optimize=True)


window(1680, 1290, 560, 430, 'status_window.png', 11, 26, True)
window(1200, 420, 400, 140, 'status_note.png', 12, 6)
print('status window made')
