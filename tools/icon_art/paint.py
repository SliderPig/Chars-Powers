# A very small pixel-art painter for Char's Powers icons.
# You draw with simple shapes in a 32x32 "design box", and each shape comes out as hard pixels with the same look:
# light from the top left, shade to the bottom right, and a dark outline round every symbol. Symbols can be placed
# smaller, turned or mirrored, so the same flame works as a big picture or as a little badge in a corner.
import functools
import math

import numpy as np
from PIL import Image, ImageDraw

SIZE = 32
SS = 6  # shapes are drawn this many times larger, then each pixel is on if enough of it is covered

OUT = (30, 18, 38, 255)  # the dark outline, a very dark plum


def rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def ramp(hi, mid, lo):
    return (rgb(hi), rgb(mid), rgb(lo))


RED = ramp('#ff7a7a', '#e5323f', '#a01a2e')
BLOOD = ramp('#d94050', '#a81c30', '#6a0f22')
ORANGE = ramp('#ffc060', '#f6861f', '#bc5414')
YELLOW = ramp('#fff58c', '#ffd83f', '#dc9c1c')
GOLD = ramp('#ffe88a', '#f2b82c', '#b6761a')
LIME = ramp('#d2f870', '#93d83a', '#58961f')
GREEN = ramp('#82e690', '#36b65c', '#1b7a44')
FOREST = ramp('#5cb868', '#2c8748', '#175a35')
TEAL = ramp('#7cecd6', '#26bca4', '#147c78')
CYAN = ramp('#b4f4ff', '#58ccf2', '#2c88ca')
ICE = ramp('#eafcff', '#aeeaf8', '#68b4dc')
BLUE = ramp('#8ab6ff', '#4280f2', '#2452b2')
NAVY = ramp('#6478c8', '#34448e', '#1e2864')
PURPLE = ramp('#cda4ff', '#925ee2', '#5e34aa')
VIOLET = ramp('#e690ff', '#b440e4', '#7c20aa')
PINK = ramp('#ffcce2', '#ff80b4', '#d44e8e')
ROSE = ramp('#ff9cc0', '#ec4c8c', '#a82664')
WHITE = ramp('#ffffff', '#ecedf4', '#b8bacc')
STEEL = ramp('#dee4f0', '#a0aabe', '#667088')
IRON = ramp('#c8ccd6', '#8c92a2', '#585e70')
STONE = ramp('#bcbcbc', '#8c8c8c', '#5c5c5c')
DARK = ramp('#685c78', '#40364e', '#272032')
SHADOW = ramp('#52466a', '#2e2640', '#191426')
CAT = ramp('#564a68', '#2e2838', '#191520')  # Char's black cat
BROWN = ramp('#b8804e', '#8e5832', '#60381c')
WOOD = ramp('#d8a868', '#ae7c42', '#7a5228')
TAN = ramp('#f0d29a', '#d0a266', '#9c6e3e')
BONE = ramp('#fffcec', '#eae2c4', '#bcac8c')
SKIN = ramp('#ffe0c4', '#f4b890', '#c6845c')
COPPER = ramp('#f4b088', '#d87c4c', '#964e2c')
SLIME = ramp('#c8f8a0', '#84dc68', '#48a048')
LAVA = ramp('#ffe070', '#ff8a22', '#d03a14')
SOUL = ramp('#c8fff4', '#62e0d8', '#2a9ca8')
ENDER = ramp('#62d8b8', '#157a68', '#0c3c44')
XP = ramp('#e4ff8a', '#a4e83c', '#5ca81c')
SAND = ramp('#fff0b4', '#ecd488', '#bc9e58')
GRASS = ramp('#a4e860', '#62b838', '#3c8426')
DIRT = ramp('#a87c54', '#7c5636', '#543822')
EMERALD = ramp('#8cf8b4', '#20cc70', '#0c8848')
DIAMOND = ramp('#d0fff8', '#54e4dc', '#209ca8')

RAINBOW = [RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE]


def solid(c):
    """A flat colour: one tone of a ramp, or a '#hex' string."""
    return rgb(c) if isinstance(c, str) else c


def shift(m, dx, dy):
    out = np.zeros_like(m)
    h, w = m.shape
    ys0, ys1 = max(0, dy), min(h, h + dy)
    xs0, xs1 = max(0, dx), min(w, w + dx)
    out[ys0:ys1, xs0:xs1] = m[ys0 - dy:ys1 - dy, xs0 - dx:xs1 - dx]
    return out


def dilate4(m):
    return m | shift(m, 1, 0) | shift(m, -1, 0) | shift(m, 0, 1) | shift(m, 0, -1)


def _mul(a, b):
    return (a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1],
            a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3],
            a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5])


def _smooth(pts, closed, steps=6):
    """Catmull-Rom through the points. Repeat a point to make a sharp corner there."""
    n = len(pts)
    out = []
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        if closed:
            p0, p1, p2, p3 = pts[(i - 1) % n], pts[i], pts[(i + 1) % n], pts[(i + 2) % n]
        else:
            p0, p1, p2, p3 = pts[max(i - 1, 0)], pts[i], pts[i + 1], pts[min(i + 2, n - 1)]
        for k in range(steps):
            t = k / steps
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * (2 * p1[d] + (p2[d] - p0[d]) * t + (2 * p0[d] - 5 * p1[d] + 4 * p2[d] - p3[d]) * t2
                                    + (3 * p1[d] - p0[d] - 3 * p2[d] + p3[d]) * t3) for d in (0, 1)))
    if not closed:
        out.append(pts[-1])
    return out


class Art:
    def __init__(self, size=SIZE, ss=SS, tf=(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)):
        self.size, self.ss = size, ss
        self.stack = [np.zeros((size, size, 4), np.uint8)]
        self.tf = [tf]

    # ---- placing -------------------------------------------------------------------------------
    def push(self, x=0, y=0, s=1.0, fx=False, fy=False, r=0):
        c, sn = math.cos(math.radians(r)), math.sin(math.radians(r))
        m = (1, 0, 0, 1, x, y)
        m = _mul(m, (s, 0, 0, s, 0, 0))
        m = _mul(m, (1, 0, 0, 1, 16, 16))
        m = _mul(m, (c, sn, -sn, c, 0, 0))
        m = _mul(m, (-1 if fx else 1, 0, 0, -1 if fy else 1, 0, 0))
        m = _mul(m, (1, 0, 0, 1, -16, -16))
        self.tf.append(_mul(self.tf[-1], m))

    def pop(self):
        self.tf.pop()

    def scale(self):
        m = self.tf[-1]
        return math.hypot(m[0], m[1])

    def _xy(self, p):
        m = self.tf[-1]
        return (m[0] * p[0] + m[2] * p[1] + m[4], m[1] * p[0] + m[3] * p[1] + m[5])

    # ---- shapes: each gives a True/False grid of the 32x32 pixels ------------------------------
    def _raster(self, draw, cover=0.5):
        size, ss = self.size, self.ss
        im = Image.new('L', (size * ss, size * ss), 0)
        draw(ImageDraw.Draw(im))
        a = np.asarray(im, dtype=np.float32) / 255.0
        a = a.reshape(size, ss, size, ss).mean(axis=(1, 3))
        return a >= cover

    def P(self, *pts):
        """Polygon."""
        q = [tuple(v * self.ss for v in self._xy(p)) for p in pts]
        return self._raster(lambda d: d.polygon(q, fill=255))

    def E(self, cx, cy, rx, ry=None):
        """Ellipse (circle when ry is left out)."""
        ry = rx if ry is None else ry
        return self.P(*[(cx + rx * math.cos(i * math.tau / 48), cy + ry * math.sin(i * math.tau / 48)) for i in range(48)])

    def R(self, x0, y0, x1, y1, r=0):
        """Rectangle, r rounds its corners."""
        if r <= 0:
            return self.P((x0, y0), (x1, y0), (x1, y1), (x0, y1))
        pts = []
        for cx, cy, st in ((x1 - r, y0 + r, -90), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90), (x0 + r, y0 + r, 180)):
            for k in range(7):
                t = math.radians(st + k * 15)
                pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
        return self.P(*pts)

    def C(self, *pts):
        """Smooth closed blob through the points. Repeat a point for a sharp corner."""
        return self.P(*_smooth(list(pts), True))

    def L(self, pts, w=2.0):
        """Thick line with round ends through the points."""
        ss = self.ss
        q = [tuple(v * ss for v in self._xy(p)) for p in pts]
        hw = max(w * self.scale() * ss / 2, ss * 0.52)
        thin = hw < ss * 0.8

        def draw(d):
            for i in range(len(q) - 1):
                (ax, ay), (bx, by) = q[i], q[i + 1]
                ln = math.hypot(bx - ax, by - ay) or 1
                nx, ny = -(by - ay) / ln * hw, (bx - ax) / ln * hw
                d.polygon([(ax + nx, ay + ny), (bx + nx, by + ny), (bx - nx, by - ny), (ax - nx, ay - ny)], fill=255)
            for (x, y) in q:
                d.ellipse((x - hw, y - hw, x + hw, y + hw), fill=255)
        return self._raster(draw, 0.36 if thin else 0.5)

    def B(self, pts, w=2.0):
        """Thick smooth curve through the points."""
        return self.L(_smooth(list(pts), False), w)

    def A(self, cx, cy, r, a0, a1, w=2.0):
        """Thick arc. Angles in degrees: 0 right, 90 down."""
        n = max(4, int(abs(a1 - a0) / 8))
        return self.L([(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)),
                        cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)], w)

    # ---- painting ------------------------------------------------------------------------------
    def fill(self, m, c, shade=True, deep=False):
        """Fill with a colour ramp (hi, mid, lo): light on the top left edge, shade on the bottom right edge."""
        px = self.stack[-1]
        if isinstance(c[0], int):  # one flat colour
            px[m] = c
            return
        px[m] = c[1]
        if shade:
            up_left = shift(m, 1, 1)
            down_right = shift(m, -1, -1)
            hi = m & ~up_left
            lo = m & ~down_right
            if deep:
                lo = lo | (m & ~shift(m, -2, -2))
            both = hi & lo
            px[hi & ~both] = c[0]
            px[lo & ~hi] = c[2]

    def flat(self, m, c):
        self.fill(m, c[1] if not isinstance(c[0], int) else c, shade=False)

    def dot(self, x, y, c):
        """One pixel, wherever this design spot lands."""
        c = c[1] if not isinstance(c[0], int) else c
        px, py = self._xy((x, y))
        i, j = int(py), int(px)
        if 0 <= i < self.size and 0 <= j < self.size:
            self.stack[-1][i, j] = c

    def ring(self, m):
        """Dark outline just outside a shape, over whatever is under it."""
        self.stack[-1][dilate4(m) & ~m] = OUT

    # ---- layers: a symbol is painted by itself, then stuck on with a dark outline all round -----
    def begin(self):
        self.stack.append(np.zeros((self.size, self.size, 4), np.uint8))

    def end(self, sticker=True):
        top = self.stack.pop()
        under = self.stack[-1]
        sil = top[..., 3] > 0
        if sticker:
            under[dilate4(sil) & ~sil] = OUT
        under[sil] = top[sil]

    def finish(self):
        px = self.stack[0]
        sil = px[..., 3] > 0
        px[dilate4(sil) & ~sil] = OUT
        return Image.fromarray(px, 'RGBA')


def fitted(recipe):
    """Paints a recipe so that it, with its outline, sits fully inside the 32x32 picture. A symbol placed a little too
    far out is just moved in; only if the whole thing is too big is it drawn slightly smaller."""
    margin = 16
    big = Art(SIZE + 2 * margin, 3, (1.0, 0.0, 0.0, 1.0, float(margin), float(margin)))
    recipe(big)
    ys, xs = np.nonzero(np.asarray(big.finish())[..., 3] > 0)
    x0, x1, y0, y1 = xs.min() - margin, xs.max() - margin, ys.min() - margin, ys.max() - margin
    s = min(1.0, SIZE / max(x1 - x0 + 1, y1 - y0 + 1))
    if s == 1.0:
        dx = -x0 if x0 < 0 else SIZE - 1 - x1 if x1 > SIZE - 1 else 0
        dy = -y0 if y0 < 0 else SIZE - 1 - y1 if y1 > SIZE - 1 else 0
        a = Art(tf=(1.0, 0.0, 0.0, 1.0, float(dx), float(dy)))
        recipe(a)
        return a.finish()
    for _ in range(4):  # rounding can still push a pixel out, so check
        cx, cy = (x0 + x1 + 1) / 2, (y0 + y1 + 1) / 2
        a = Art(tf=(s, 0.0, 0.0, s, SIZE / 2 - cx * s, SIZE / 2 - cy * s))
        recipe(a)
        im = a.finish()
        if not _clipped(a):
            return im
        s *= 0.97
    return im


def _clipped(a):
    """True if a coloured pixel is on the very edge, where its outline would be lost."""
    px = a.stack[0]
    colour = (px[..., 3] > 0) & ~((px[..., 0] == OUT[0]) & (px[..., 1] == OUT[1]) & (px[..., 2] == OUT[2]))
    return colour[0].any() or colour[-1].any() or colour[:, 0].any() or colour[:, -1].any()


def glyph(fn):
    """Makes a drawing function placeable: name(a, x, y, s, fx=, fy=, r=, ...its own options...)."""
    @functools.wraps(fn)
    def placed(a, x=0, y=0, s=1.0, fx=False, fy=False, r=0, sticker=True, **kw):
        a.push(x, y, s, fx, fy, r)
        a.begin()
        fn(a, **kw)
        a.end(sticker)
        a.pop()
    return placed
