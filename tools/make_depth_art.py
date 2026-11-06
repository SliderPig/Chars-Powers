# The Invincible Suit sheet (128x128 flat-shaded pixels, between the plain 64 and the old smooth 256 version's own armour, lime and silver) and the Charging Station's textures and models. Replaces the flat textures make_suit.py draws. Run after it, from the charpowers folder:  python tools/make_depth_art.py
import json

from PIL import Image, ImageDraw, ImageFilter

A = 'src/main/resources/assets/charpowers/'
S = 8  # drawing pixels per texture pixel; the saved sheet is half this
WHITE, LIGHT, SILVER, MID, STEEL, DARK, INK = (246, 250, 252), (222, 228, 233), (188, 196, 203), (146, 155, 163), (92, 100, 108), (52, 58, 64), (26, 30, 34)
LIME, GLOW, DEEP = (150, 255, 30), (214, 255, 150), (80, 190, 16)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Face:
    """One face of a box, w x h texture pixels, painted in layers."""

    def __init__(self, w, h, top=DARK, bottom=INK):
        self.w, self.h = w, h
        self.im = Image.new('RGBA', (w * S, h * S))
        d = ImageDraw.Draw(self.im)
        for y in range(h * S):
            d.line((0, y, w * S, y), fill=mix(top, bottom, y / (h * S)) + (255,))

    def pts(self, points):
        return [(x * S, y * S) for x, y in points]

    def plate(self, points, top=LIGHT, bottom=MID, lift=0.14):
        """A raised plate: a soft shadow under it, a light rim up and left, a dark rim down and right, a shaded face."""
        size = self.im.size
        shadow = Image.new('RGBA', size, (0, 0, 0, 0))
        ImageDraw.Draw(shadow).polygon(self.pts([(x + lift, y + lift * 1.4) for x, y in points]), fill=(0, 0, 0, 170))
        self.im.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(S * 0.12)))
        mask = Image.new('L', size, 0)
        ImageDraw.Draw(mask).polygon(self.pts(points), fill=255)
        rim = Image.new('RGBA', size, mix(bottom, INK, 0.45) + (255,))
        ImageDraw.Draw(rim).polygon(self.pts([(x - 0.07, y - 0.07) for x, y in points]), fill=WHITE + (255,))
        fill = Image.new('RGBA', size)
        fd = ImageDraw.Draw(fill)
        ys = [y for _, y in points]
        y0, y1 = min(ys), max(ys)
        for row in range(size[1]):
            t = min(1, max(0, (row / S - y0) / max(0.01, y1 - y0)))
            fd.line((0, row, size[0], row), fill=mix(top, bottom, t) + (255,))
        inner = Image.new('L', size, 0)
        cx, cy = sum(x for x, _ in points) / len(points), sum(y for _, y in points) / len(points)
        width = max(x for x, _ in points) - min(x for x, _ in points)
        height = y1 - y0
        sx, sy = max(0.2, 1 - 0.2 / max(width, 0.4)), max(0.2, 1 - 0.2 / max(height, 0.4))
        ImageDraw.Draw(inner).polygon(self.pts([(cx + (x - cx) * sx, cy + (y - cy) * sy) for x, y in points]), fill=255)
        rim.paste(fill, (0, 0), inner)
        self.im.paste(rim, (0, 0), mask)

    def box(self, x0, y0, x1, y1, top=LIGHT, bottom=MID, cut=0.3, lift=0.14):
        self.plate([(x0 + cut, y0), (x1 - cut, y0), (x1, y0 + cut), (x1, y1 - cut), (x1 - cut, y1), (x0 + cut, y1), (x0, y1 - cut), (x0, y0 + cut)], top, bottom, lift)

    def seam(self, points, width=0.2, colour=LIME):
        """A thin glowing line."""
        size = self.im.size
        layer = Image.new('RGBA', size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).line(self.pts(points), fill=colour + (255,), width=int(width * S * 2.4))
        soft = layer.filter(ImageFilter.GaussianBlur(S * 0.22))
        self.im.alpha_composite(soft)
        self.im.alpha_composite(soft)
        ImageDraw.Draw(self.im).line(self.pts(points), fill=mix(colour, WHITE, 0.45) + (255,), width=max(1, int(width * S)))

    def groove(self, x0, y0, x1, y1):
        """A dark line cut into a plate, with a lit lip below it."""
        d = ImageDraw.Draw(self.im)
        d.line(self.pts([(x0, y0), (x1, y1)]), fill=INK + (235,), width=max(1, int(0.12 * S)))
        d.line(self.pts([(x0, y0 + 0.13), (x1, y1 + 0.13)]), fill=WHITE + (110,), width=max(1, int(0.07 * S)))

    def rivet(self, x, y, r=0.2):
        d = ImageDraw.Draw(self.im)
        d.ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=INK + (255,))
        r2 = r * 0.7
        d.ellipse([(x - r2 - 0.03) * S, (y - r2 - 0.03) * S, (x + r2 - 0.03) * S, (y + r2 - 0.03) * S], fill=LIGHT + (255,))

    def orb(self, x, y, r, colour=LIME):
        """A round light in a dark socket."""
        size = self.im.size
        layer = Image.new('RGBA', size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=colour + (255,))
        soft = layer.filter(ImageFilter.GaussianBlur(S * 0.3))
        d = ImageDraw.Draw(self.im)
        ring = r + 0.3
        d.ellipse([(x - ring) * S, (y - ring) * S, (x + ring) * S, (y + ring) * S], fill=INK + (255,), outline=WHITE + (200,), width=max(1, int(S * 0.1)))
        self.im.alpha_composite(soft)
        d.ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=colour + (255,))
        c = r * 0.5
        d.ellipse([(x - c) * S, (y - c - r * 0.2) * S, (x + c) * S, (y + c - r * 0.2) * S], fill=mix(colour, WHITE, 0.7) + (255,))

    def flip(self):
        self.im = self.im.transpose(Image.FLIP_LEFT_RIGHT)
        return self


# ---- the suit's faces

# ---- the suit: a 128x128 sheet drawn pixel by pixel, flat shades only (no smooth gradients), lime and silver. The model's boxes are laid out on
# a 64x64 grid, so every number in box() is doubled: one model pixel is 2x2 sheet pixels, which is room for panel lines, rivets and vents.
# Between the plain 64px sheet and the old 256px smooth one.

def _put(im, x, y, c):
    if 0 <= x < im.size[0] and 0 <= y < im.size[1]:
        im.putpixel((x, y), c + (255,))


def _rect(im, x, y, w, h, c):
    for j in range(h):
        for i in range(w):
            _put(im, x + i, y + j, c)


def plate(im, x, y, w, h, fill=SILVER, hi=LIGHT, lo=MID, edge=STEEL):
    """A panel: a dark outline, a bright top and left, a shaded bottom and right, a flat face."""
    _rect(im, x, y, w, h, edge)
    if w < 4 or h < 4:
        _rect(im, x, y, w, h, fill)
        return
    _rect(im, x + 1, y + 1, w - 2, h - 2, fill)
    _rect(im, x + 1, y + 1, w - 2, 1, hi)
    _rect(im, x + 1, y + 1, 1, h - 2, hi)
    _rect(im, x + 1, y + h - 2, w - 2, 1, lo)
    _rect(im, x + w - 2, y + 2, 1, h - 3, lo)


def groove(im, x, y, w, h):
    """A thin dark panel line with a light edge under it."""
    _rect(im, x, y, w, h, DARK)
    if w >= h:
        _rect(im, x, y + h, w, 1, LIGHT)
    else:
        _rect(im, x + w, y, 1, h, LIGHT)


def rivet(im, x, y):
    _put(im, x, y, WHITE)
    _put(im, x + 1, y, MID)
    _put(im, x, y + 1, MID)
    _put(im, x + 1, y + 1, DARK)


def seam(im, x, y, w, h):
    """A glowing lime line: bright core, lime edge."""
    _rect(im, x, y, w, h, LIME)
    if w >= h:
        _rect(im, x, y + h // 2, w, max(1, h // 2), GLOW)
    else:
        _rect(im, x + w // 2, y, max(1, w // 2), h, GLOW)


def vents(im, x, y, w, n):
    for i in range(n):
        _rect(im, x, y + i * 2, w, 1, INK)
        _rect(im, x, y + i * 2 + 1, w, 1, MID)


# painters: fn(im, x, y, w, h), the face's size in sheet pixels
def silver(im, x, y, w, h):
    plate(im, x, y, w, h)


def steel(im, x, y, w, h):
    plate(im, x, y, w, h, MID, SILVER, STEEL, DARK)


def dark(im, x, y, w, h):
    plate(im, x, y, w, h, STEEL, MID, DARK, INK)


def lime(im, x, y, w, h):
    plate(im, x, y, w, h, LIME, GLOW, DEEP, DEEP)


def helmet_front(im, x, y, w, h):
    plate(im, x, y, w, h)
    plate(im, x + 2, y + 2, w - 4, 3, LIGHT, WHITE, SILVER, MID)           # brow plate
    _rect(im, x + 2, y + 6, w - 4, 6, INK)                                  # the visor: dark frame, lime glass, a glint
    _rect(im, x + 3, y + 7, w - 6, 4, LIME)
    _rect(im, x + 3, y + 7, w - 6, 1, GLOW)
    _rect(im, x + 3, y + 10, w - 6, 1, DEEP)
    _rect(im, x + 4, y + 8, 2, 1, WHITE)
    plate(im, x + 2, y + 12, 3, 3, MID, SILVER, STEEL, DARK)               # cheek guards
    plate(im, x + w - 5, y + 12, 3, 3, MID, SILVER, STEEL, DARK)
    vents(im, x + 6, y + 12, w - 12, 2)                                     # mouth grille
    rivet(im, x + 2, y + 2 + 0)
    rivet(im, x + w - 4, y + 2)


def helmet_side(front_on_right):
    def draw(im, x, y, w, h):
        plate(im, x, y, w, h)
        fx = x + (w - 10 if front_on_right else 0)
        _rect(im, fx, y + 6, 10, 6, INK)
        _rect(im, fx, y + 7, 10, 4, LIME)
        _rect(im, fx, y + 7, 10, 1, GLOW)
        groove(im, x + (2 if front_on_right else w - 4), y + 2, 2, 12)       # panel line
        plate(im, x + (3 if front_on_right else w - 9), y + 12, 6, 3, MID, SILVER, STEEL, DARK)
        rivet(im, x + (w - 4 if front_on_right else 2), y + 2)
    return draw


def helmet_back(im, x, y, w, h):
    plate(im, x, y, w, h)
    _rect(im, x + 6, y + 2, 4, 12, DARK)
    seam(im, x + 7, y + 2, 2, 12)
    groove(im, x + 2, y + 5, 4, 1)
    groove(im, x + 10, y + 5, 4, 1)
    vents(im, x + 2, y + 9, 4, 2)
    vents(im, x + 10, y + 9, 4, 2)
    rivet(im, x + 2, y + 2)
    rivet(im, x + w - 4, y + 2)


def helmet_top(im, x, y, w, h):
    plate(im, x, y, w, h, LIGHT, WHITE, SILVER, MID)
    _rect(im, x + 6, y, 4, h, DARK)
    seam(im, x + 7, y + 1, 2, h - 2)
    groove(im, x + 2, y + 4, 3, 1)
    groove(im, x + 11, y + 4, 3, 1)


def top_stripe(im, x, y, w, h):
    plate(im, x, y, w, h, LIGHT, WHITE, SILVER, MID)
    if w >= 8:
        _rect(im, x + w // 2 - 2, y, 4, h, DARK)
        seam(im, x + w // 2 - 1, y + 1, 2, h - 2)


def body_front(im, x, y, w, h):
    plate(im, x, y, w, h, MID, SILVER, STEEL, DARK)
    plate(im, x + 1, y + 1, w - 2, 5, LIGHT, WHITE, SILVER, MID)            # collar
    plate(im, x + 2, y + 8, 5, 8, SILVER, WHITE, MID, STEEL)                 # pec panels
    plate(im, x + w - 7, y + 8, 5, 8, SILVER, WHITE, MID, STEEL)
    _rect(im, x + 7, y + 7, 2, 11, DARK)                                    # centre seam
    seam(im, x + 7, y + 7, 2, 11)
    groove(im, x + 3, y + 18, w - 6, 1)
    vents(im, x + 3, y + 10, 3, 3)
    vents(im, x + w - 6, y + 10, 3, 3)
    _rect(im, x, y + 19, w, 3, INK)                                         # belt
    seam(im, x + 1, y + 19, w - 2, 2)
    plate(im, x + 6, y + 18, 4, 5, LIME, GLOW, DEEP, INK)                   # buckle
    plate(im, x + 2, y + 22, w - 4, 2, MID, SILVER, STEEL, DARK)
    rivet(im, x + 2, y + 2)
    rivet(im, x + w - 4, y + 2)


def body_side(im, x, y, w, h):
    plate(im, x, y, w, h)
    groove(im, x + 2, y + 6, w - 4, 1)
    vents(im, x + 2, y + 9, w - 4, 3)
    _rect(im, x, y + 19, w, 3, INK)
    seam(im, x + 1, y + 19, w - 2, 2)
    plate(im, x + 1, y + 22, w - 2, 2, MID, SILVER, STEEL, DARK)


def body_back(im, x, y, w, h):
    plate(im, x, y, w, h, MID, SILVER, STEEL, DARK)
    _rect(im, x + 7, y + 2, 2, 16, DARK)
    seam(im, x + 7, y + 3, 2, 14)
    for i in range(4):
        groove(im, x + 2, y + 5 + i * 3, 4, 1)
        groove(im, x + w - 6, y + 5 + i * 3, 4, 1)
    _rect(im, x, y + 19, w, 3, INK)
    seam(im, x + 1, y + 19, w - 2, 2)
    plate(im, x + 2, y + 22, w - 4, 2, STEEL, MID, DARK, INK)


def chest_plate(im, x, y, w, h):
    plate(im, x, y, w, h, SILVER, WHITE, STEEL, DARK)
    cx, cy = x + w // 2, y + h // 2
    _rect(im, cx - 4, cy - 4, 8, 8, INK)                                    # the core: a dark socket, a lime diamond, a bright heart
    for dy in range(-3, 4):
        span = 3 - abs(dy)
        _rect(im, cx - span - 1 + 0, cy + dy, span * 2 + 2, 1, LIME)
    _rect(im, cx - 1, cy - 1, 2, 2, GLOW)
    rivet(im, x + 2, y + 2)
    rivet(im, x + w - 4, y + 2)
    rivet(im, x + 2, y + h - 4)
    rivet(im, x + w - 4, y + h - 4)


def pack_back(im, x, y, w, h):
    plate(im, x, y, w, h, STEEL, MID, DARK, INK)
    vents(im, x + 2, y + 2, w - 4, 3)
    groove(im, x + 2, y + 9, w - 4, 1)
    for tx in (x + 2, x + w - 6):                                           # two thrusters
        _rect(im, tx, y + h - 7, 4, 5, INK)
        _rect(im, tx + 1, y + h - 6, 2, 3, LIME)
        _rect(im, tx + 1, y + h - 6, 2, 1, GLOW)


def arm(im, x, y, w, h):
    plate(im, x, y, w, h)
    plate(im, x, y, w, 5, LIGHT, WHITE, SILVER, MID)                       # shoulder cap
    _rect(im, x, y + 10, w, 2, DARK)                                        # elbow joint
    _rect(im, x, y + 12, w, 10, MID)
    _rect(im, x, y + 12, w, 1, SILVER)
    seam(im, x + 2, y + 14, w - 4, 2)
    vents(im, x + 2, y + 17, w - 4, 2)
    _rect(im, x, y + 22, w, 2, DARK)                                        # glove edge
    rivet(im, x + 1, y + 6)


def arm_plain(im, x, y, w, h):
    plate(im, x, y, w, h)
    plate(im, x, y, w, 5, LIGHT, WHITE, SILVER, MID)
    _rect(im, x, y + 10, w, 2, DARK)
    _rect(im, x, y + 12, w, 10, MID)
    _rect(im, x, y + 12, w, 1, SILVER)
    groove(im, x + 2, y + 16, w - 4, 1)
    _rect(im, x, y + 22, w, 2, DARK)


def leg(im, x, y, w, h):
    plate(im, x, y, w, h)
    plate(im, x, y, w, 2, LIGHT, WHITE, SILVER, MID)
    groove(im, x + 2, y + 4, w - 4, 1)
    _rect(im, x, y + 10, w, 2, DARK)                                        # knee joint
    _rect(im, x, y + 12, w, 10, MID)
    _rect(im, x, y + 12, w, 1, SILVER)
    _rect(im, x + 3, y + 13, 2, 8, DARK)
    seam(im, x + 3, y + 14, 2, 6)
    _rect(im, x, y + 22, w, 2, DARK)
    rivet(im, x + 1, y + 6)


def leg_plain(im, x, y, w, h):
    plate(im, x, y, w, h)
    groove(im, x + 2, y + 4, w - 4, 1)
    _rect(im, x, y + 10, w, 2, DARK)
    _rect(im, x, y + 12, w, 10, MID)
    _rect(im, x, y + 12, w, 1, SILVER)
    vents(im, x + 2, y + 14, w - 4, 3)
    _rect(im, x, y + 22, w, 2, DARK)


def knee_cap(im, x, y, w, h):
    plate(im, x, y, w, h, LIGHT, WHITE, MID, STEEL)
    seam(im, x + 2, y + h // 2, w - 4, 1)
    rivet(im, x + 1, y + 1) if h >= 4 else None


def boot(im, x, y, w, h):
    plate(im, x, y, w, h, MID, SILVER, STEEL, DARK)
    if h >= 4:
        _rect(im, x, y + h - 2, w, 2, INK)
        seam(im, x + 2, y + 2, max(2, w - 4), 1)


def band(im, x, y, w, h):
    plate(im, x, y, w, h, LIME, GLOW, DEEP, DARK)
    if h >= 5:
        groove(im, x + 1, y + h // 2, w - 2, 1)


def box(im, u, v, w, h, d, front, back, right, left, top, bottom):
    """One model box on the sheet, in the game's layout, doubled: top and bottom first, then right side, front, left side, back."""
    u, v, w, h, d = u * 2, v * 2, w * 2, h * 2, d * 2
    top(im, u + d, v, w, d)
    bottom(im, u + d + w, v, w, d)
    right(im, u, v + d, d, h)
    front(im, u + d, v + d, w, h)
    left(im, u + d + w, v + d, d, h)
    back(im, u + d + w + d, v + d, w, h)


def suit_sheet():
    im = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    box(im, 0, 0, 8, 8, 8, helmet_front, helmet_back, helmet_side(True), helmet_side(False), helmet_top, dark)
    box(im, 16, 16, 8, 12, 4, body_front, body_back, body_side, body_side, top_stripe, dark)
    box(im, 40, 16, 4, 12, 4, arm, arm_plain, arm, arm, silver, dark)          # the left arm is this one flipped
    box(im, 0, 16, 4, 12, 4, leg, leg_plain, leg, leg, silver, dark)           # the left leg too
    # raised parts
    box(im, 0, 32, 2, 3, 11, silver, silver, silver, silver, lime, dark)       # helmet crest
    box(im, 28, 32, 7, 7, 1, chest_plate, steel, silver, silver, silver, dark)  # chest plate
    box(im, 46, 32, 6, 8, 2, dark, pack_back, dark, dark, steel, dark)         # back pack
    box(im, 0, 46, 6, 3, 7, silver, silver, silver, silver, top_stripe, dark)  # shoulder
    box(im, 28, 46, 5, 2, 5, band, band, band, band, band, dark)               # forearm band
    box(im, 48, 46, 5, 3, 1, knee_cap, steel, silver, silver, silver, dark)    # knee guard
    box(im, 28, 54, 5, 3, 6, boot, boot, boot, boot, steel, dark)              # boot
    return im


def brighter(sheet):
    """The slim suit: the same plates in polished silver."""
    px = sheet.copy().convert('RGBA')
    data = px.load()
    for y in range(px.size[1]):
        for x in range(px.size[0]):
            r, g, b, a = data[x, y]
            if a and not (g > r + 40):
                data[x, y] = (min(255, r + 34), min(255, g + 34), min(255, b + 34), a)
    return px


# ---- the charging station: a real machine with parts, not a textured cube

def machine_texture(kind, on):
    light = LIME if on else (70, 84, 64)
    f = Face(16, 16, STEEL, DARK)
    if kind == 'base_top':
        f.box(0.3, 0.3, 15.7, 15.7, SILVER, MID, 1)
        for x, y in ((1.5, 1.5), (14.5, 1.5), (1.5, 14.5), (14.5, 14.5)):
            f.rivet(x, y, 0.45)
    elif kind == 'base_side':
        f.box(0.2, 0.2, 15.8, 15.8, SILVER, STEEL, 0.5)
        for x in (2, 6, 10, 14):
            f.groove(x, 2, x, 14)
    elif kind == 'core_side':
        f = Face(16, 16, DARK, INK)
        f.box(1, 0.5, 15, 15.5, MID, STEEL, 0.8)
        f.box(3, 2.5, 13, 13.5, INK, (12, 16, 14), 0.5, 0.05)
        for y in (4.5, 7.2, 9.9, 12.3):
            f.seam([(4.5, y), (11.5, y)], 0.45, light)
    elif kind == 'post':
        f.box(0.5, 0, 15.5, 16, WHITE, MID, 0.8)
        f.groove(8, 1, 8, 15)
        f.rivet(8, 2.5, 0.8)
        f.rivet(8, 13.5, 0.8)
    elif kind == 'cap_top':
        f.box(0.3, 0.3, 15.7, 15.7, LIGHT, SILVER, 1.4)
        f.box(3, 3, 13, 13, DARK, INK, 1, 0.05)
        f.seam([(4.6, 4.6), (11.4, 4.6), (11.4, 11.4), (4.6, 11.4), (4.6, 4.6)], 0.4, light)
    elif kind == 'cap_side':
        f.box(0.2, 0.3, 15.8, 15.7, WHITE, MID, 0.6)
        f.seam([(2, 8), (14, 8)], 0.8, light)
    else:  # the light itself
        f = Face(16, 16, light, mix(light, INK, 0.25))
        f.orb(8, 8, 4.5, light)
    return f


def save_machine():
    for kind in ('base_top', 'base_side', 'post', 'cap_top', 'cap_side', 'core_side', 'light'):
        for state in ('off', 'on'):
            on = state == 'on'
            if kind in ('base_top', 'base_side', 'post') and on:
                continue
            f = machine_texture(kind, on)
            name = f'charging_station_{kind}' + ('' if kind in ('base_top', 'base_side', 'post') else f'_{state}')
            f.im.resize((32, 32), Image.LANCZOS).convert('RGBA').save(A + f'textures/block/{name}.png', optimize=True)
    for old in ('side_off', 'side_on', 'top_off', 'top_on'):
        import os
        path = A + f'textures/block/charging_station_{old}.png'
        if os.path.exists(path):
            os.remove(path)

    def cube(a, b, tex, top=None, shade=True):
        faces = {side: {'texture': '#' + (top if top and side in ('up', 'down') else tex)} for side in ('north', 'south', 'east', 'west', 'up', 'down')}
        return {'from': a, 'to': b, 'shade': shade, 'faces': faces}

    for state in ('off', 'on'):
        model = {
            'parent': 'minecraft:block/block',
            'textures': {'particle': 'charpowers:block/charging_station_post', 'base': 'charpowers:block/charging_station_base_side', 'baseTop': 'charpowers:block/charging_station_base_top',
                         'post': 'charpowers:block/charging_station_post', 'core': f'charpowers:block/charging_station_core_side_{state}',
                         'cap': f'charpowers:block/charging_station_cap_side_{state}', 'capTop': f'charpowers:block/charging_station_cap_top_{state}',
                         'light': f'charpowers:block/charging_station_light_{state}'},
            'elements': [
                cube([0, 0, 0], [16, 3, 16], 'base', 'baseTop'),
                cube([3, 3, 3], [13, 12, 13], 'core', 'capTop'),
                cube([0.5, 3, 0.5], [3, 12, 3], 'post'),
                cube([13, 3, 0.5], [15.5, 12, 3], 'post'),
                cube([0.5, 3, 13], [3, 12, 15.5], 'post'),
                cube([13, 3, 13], [15.5, 12, 15.5], 'post'),
                cube([1, 12, 1], [15, 15, 15], 'cap', 'capTop'),
                cube([6, 15, 6], [10, 16, 10], 'light', None, state == 'off'),
            ],
        }
        with open(A + f'models/block/charging_station_{state}.json', 'w', encoding='utf-8', newline='\n') as out:
            json.dump(model, out, indent=2)
            out.write('\n')


def main():
    sheet = suit_sheet()
    sheet.save(A + 'textures/models/armor/invincible.png', optimize=True)
    brighter(sheet).save(A + 'textures/models/armor/invincible_slim.png', optimize=True)
    save_machine()
    print('depth art made')


if __name__ == '__main__':
    main()
