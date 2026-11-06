package com.example.charpowers.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

// Rounded shapes with soft edges. Meant for screens drawn in real screen pixels (scale the pose by 1 / GUI scale first),
// so corners are smooth at any GUI scale. There are no straight-line box buttons in this mod's newer screens: I hate them.
public final class Soft {
    private Soft() {}

    private static int faded(int colour, float amount) {
        return ((int) (((colour >>> 24) & 0xFF) * amount) << 24) | (colour & 0xFFFFFF);
    }

    // How far in from the side a corner of radius r starts, on the row this far from the top.
    private static float inset(int row, int r) {
        float d = r - row - 0.5f;
        return r - (float) Math.sqrt(Math.max(0f, r * r - d * d));
    }

    private static void span(GuiGraphics g, int x0, int x1, int y, float left, int colour) {
        int whole = (int) Math.ceil(left);
        float part = whole - left;
        if (x0 + whole < x1 - whole) g.fill(x0 + whole, y, x1 - whole, y + 1, colour);
        if (part > 0.02f && whole > 0) {
            int edge = faded(colour, part);
            g.fill(x0 + whole - 1, y, x0 + whole, y + 1, edge);
            g.fill(x1 - whole, y, x1 - whole + 1, y + 1, edge);
        }
    }

    public static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int colour) {
        int h = y1 - y0;
        r = Math.min(r, Math.min(x1 - x0, h) / 2);
        for (int i = 0; i < r; i++) {
            float in = inset(i, r);
            span(g, x0, x1, y0 + i, in, colour);
            span(g, x0, x1, y1 - 1 - i, in, colour);
        }
        if (h > 2 * r) g.fill(x0, y0 + r, x1, y1 - r, colour);
    }

    private static int shift(int colour, int by) {
        int r = Math.max(0, Math.min(255, ((colour >> 16) & 255) + by)), gr = Math.max(0, Math.min(255, ((colour >> 8) & 255) + by)), b = Math.max(0, Math.min(255, (colour & 255) + by));
        return (colour & 0xFF000000) | (r << 16) | (gr << 8) | b;
    }

    // A raised plate: a soft shadow under it, a lit top edge, a dark bottom edge, then its face.
    public static void plate(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int colour, int lift) {
        fill(g, x0 + 1, y0 + lift, x1 + 1, y1 + lift, r, 0x50000000);
        fill(g, x0, y0, x1, y1, r, shift(colour, -40) | 0xFF000000);
        fill(g, x0, y0, x1, y1 - lift, r, shift(colour, 60) | 0xFF000000);
        fill(g, x0 + 1, y0 + 1, x1 - 1, y1 - lift, Math.max(0, r - 1), colour | 0xFF000000);
    }

    // A sunken well: dark at the top where the edge shades it, a thin lit lip along the bottom.
    public static void well(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int colour, int depth) {
        fill(g, x0, y0, x1, y1, r, 0x66B8E8FF);
        fill(g, x0, y0, x1, y1 - 1, r, 0xF0030A14);
        fill(g, x0 + 1, y0 + depth, x1 - 1, y1 - 1, Math.max(0, r - 1), colour);
    }

    // A thin line round the same shape.
    public static void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int colour) {
        int h = y1 - y0;
        r = Math.min(r, Math.min(x1 - x0, h) / 2);
        for (int i = 0; i < r; i++) {
            float out = inset(i, r);
            float d = r - i - 0.5f;
            float in = d >= r - 1 ? (x1 - x0) / 2f : r - (float) Math.sqrt((r - 1) * (r - 1) - d * d);
            for (int y : new int[] {y0 + i, y1 - 1 - i}) {
                int from = (int) Math.ceil(out), to = Math.max(from + 1, (int) Math.floor(in));
                g.fill(x0 + from, y, x0 + to, y + 1, colour);
                g.fill(x1 - to, y, x1 - from, y + 1, colour);
                float part = from - out;
                if (part > 0.02f && from > 0) {
                    int edge = faded(colour, part);
                    g.fill(x0 + from - 1, y, x0 + from, y + 1, edge);
                    g.fill(x1 - from, y, x1 - from + 1, y + 1, edge);
                }
            }
        }
        if (h > 2 * r) {
            g.fill(x0, y0 + r, x0 + 1, y1 - r, colour);
            g.fill(x1 - 1, y0 + r, x1, y1 - r, colour);
        }
    }

    // ---- Smooth shapes made of real triangles: they work in any units (GUI units are fine), any pose, and their edges fade across
    // one real screen pixel, so nothing looks stepped at any GUI scale. They only add to the batch: draw them inside g.drawManaged,
    // or call g.flush() afterwards. Blits don't go through the batch, so flush before one.

    private static final int MOST_POINTS = 512;
    private static final float[] XS = new float[MOST_POINTS], YS = new float[MOST_POINTS], XS2 = new float[MOST_POINTS], YS2 = new float[MOST_POINTS];

    // How big one real screen pixel is in the units being drawn in right now.
    private static float pixel(GuiGraphics g) {
        float units = Math.abs(g.pose().last().pose().m00()) * (float) Minecraft.getInstance().getWindow().getGuiScale();
        return units > 0.0001f ? 1f / units : 1f;
    }

    private static int segments(float r, float pixel) {
        return Mth.clamp((int) Math.ceil(Math.sqrt(r / pixel) * 1.6f), 1, 24);
    }

    public static int alpha(int colour, float amount) {
        return (Mth.clamp((int) (((colour >>> 24) & 0xFF) * amount), 0, 255) << 24) | (colour & 0xFFFFFF);
    }

    // Blends two colours, alpha too.
    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        return ((int) (aa + ((b >>> 24) - aa) * t) << 24) | ((int) (ar + (((b >> 16) & 255) - ar) * t) << 16) | ((int) (ag + (((b >> 8) & 255) - ag) * t) << 8) | (int) (ab + ((b & 255) - ab) * t);
    }

    // A slow start and a gentle stop, for t from 0 to 1.
    public static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    // A quick start and a long soft stop, for things sliding into place.
    public static float easeOut(float t) {
        t = 1f - Mth.clamp(t, 0f, 1f);
        return 1f - t * t * t;
    }

    // The outline of a rounded box grown by off (shrunk if negative), clockwise on screen from the top-left corner's left end. Returns the number of points.
    private static int contour(float[] xs, float[] ys, float x0, float y0, float x1, float y1, float rtl, float rtr, float rbr, float rbl, float off, int n) {
        float l = x0 - off, t = y0 - off, e = x1 + off, b = y1 + off;
        float most = Math.max(0f, Math.min(e - l, b - t) / 2f);
        float a = Math.min(Math.max(0f, rtl + off), most), c = Math.min(Math.max(0f, rtr + off), most), d = Math.min(Math.max(0f, rbr + off), most), f = Math.min(Math.max(0f, rbl + off), most);
        int p = 0;
        p = corner(xs, ys, p, l + a, t + a, a, Mth.PI, n);
        p = corner(xs, ys, p, e - c, t + c, c, Mth.PI * 1.5f, n);
        p = corner(xs, ys, p, e - d, b - d, d, 0f, n);
        p = corner(xs, ys, p, l + f, b - f, f, Mth.PI * 0.5f, n);
        return p;
    }

    private static int corner(float[] xs, float[] ys, int p, float cx, float cy, float r, float from, int n) {
        for (int k = 0; k <= n; k++) {
            float angle = from + k * Mth.HALF_PI / n;
            xs[p] = cx + Mth.cos(angle) * r;
            ys[p] = cy + Mth.sin(angle) * r;
            p++;
        }
        return p;
    }

    private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, int colour) {
        v.vertex(m, x, y, 0f).color((colour >> 16) & 255, (colour >> 8) & 255, colour & 255, colour >>> 24).endVertex();
    }

    // The colour of a top-to-bottom fade at height y.
    private static int shade(float y, float y0, float y1, int top, int bottom, float amount) {
        int c = top == bottom ? top : mix(top, bottom, (y - y0) / Math.max(0.001f, y1 - y0));
        return amount >= 1f ? c : alpha(c, amount);
    }

    // A band between two outlines.
    private static void band(VertexConsumer v, Matrix4f m, float[] ox, float[] oy, float[] ix, float[] iy, int count, float y0, float y1, int top, int bottom, float outer, float inner) {
        for (int i = 0; i < count; i++) {
            int j = i + 1 == count ? 0 : i + 1;
            vertex(v, m, ox[i], oy[i], shade(oy[i], y0, y1, top, bottom, outer));
            vertex(v, m, ix[i], iy[i], shade(iy[i], y0, y1, top, bottom, inner));
            vertex(v, m, ix[j], iy[j], shade(iy[j], y0, y1, top, bottom, inner));
            vertex(v, m, ox[j], oy[j], shade(oy[j], y0, y1, top, bottom, outer));
        }
    }

    private static void fan(VertexConsumer v, Matrix4f m, float[] xs, float[] ys, int count, float y0, float y1, int top, int bottom) {
        float cx = 0, cy = 0;
        for (int i = 0; i < count; i++) {
            cx += xs[i];
            cy += ys[i];
        }
        cx /= count;
        cy /= count;
        int mid = shade(cy, y0, y1, top, bottom, 1f);
        for (int i = 0; i < count; i++) {
            int j = i + 1 == count ? 0 : i + 1;
            int ci = shade(ys[i], y0, y1, top, bottom, 1f);
            vertex(v, m, cx, cy, mid);
            vertex(v, m, xs[j], ys[j], shade(ys[j], y0, y1, top, bottom, 1f));
            vertex(v, m, xs[i], ys[i], ci);
            vertex(v, m, xs[i], ys[i], ci);
        }
    }

    // A rounded box, each corner its own radius, fading from top to bottom.
    public static void shape(GuiGraphics g, float x0, float y0, float x1, float y1, float rtl, float rtr, float rbr, float rbl, int top, int bottom) {
        if (x1 - x0 <= 0 || y1 - y0 <= 0) return;
        float pixel = pixel(g), half = pixel / 2f;
        int n = segments(Math.max(Math.max(rtl, rtr), Math.max(rbr, rbl)), pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int count = contour(XS, YS, x0, y0, x1, y1, rtl, rtr, rbr, rbl, -half, n);
        contour(XS2, YS2, x0, y0, x1, y1, rtl, rtr, rbr, rbl, half, n);
        fan(v, m, XS, YS, count, y0, y1, top, bottom);
        band(v, m, XS2, YS2, XS, YS, count, y0, y1, top, bottom, 0f, 1f);
    }

    public static void round(GuiGraphics g, float x0, float y0, float x1, float y1, float r, int top, int bottom) {
        shape(g, x0, y0, x1, y1, r, r, r, r, top, bottom);
    }

    public static void round(GuiGraphics g, float x0, float y0, float x1, float y1, float r, int colour) {
        shape(g, x0, y0, x1, y1, r, r, r, r, colour, colour);
    }

    public static void disc(GuiGraphics g, float cx, float cy, float r, int colour) {
        round(g, cx - r, cy - r, cx + r, cy + r, r, colour);
    }

    // A thin line round a rounded box, just inside its edge. Can fade from top to bottom too.
    public static void ring(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float thick, int top, int bottom) {
        if (x1 - x0 <= 0 || y1 - y0 <= 0) return;
        float pixel = pixel(g), half = pixel / 2f;
        thick = Math.max(thick, pixel);
        int n = segments(r, pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int count = contour(XS, YS, x0, y0, x1, y1, r, r, r, r, half, n);
        contour(XS2, YS2, x0, y0, x1, y1, r, r, r, r, -half, n);
        band(v, m, XS, YS, XS2, YS2, count, y0, y1, top, bottom, 0f, 1f);
        if (thick > pixel) {
            contour(XS, YS, x0, y0, x1, y1, r, r, r, r, half - thick, n);
            band(v, m, XS2, YS2, XS, YS, count, y0, y1, top, bottom, 1f, 1f);
            System.arraycopy(XS, 0, XS2, 0, count);
            System.arraycopy(YS, 0, YS2, 0, count);
        }
        contour(XS, YS, x0, y0, x1, y1, r, r, r, r, -thick - half, n);
        band(v, m, XS2, YS2, XS, YS, count, y0, y1, top, bottom, 1f, 0f);
    }

    public static void ring(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float thick, int colour) {
        ring(g, x0, y0, x1, y1, r, thick, colour, colour);
    }

    // A soft shadow or glow round a box: strongest just inside the edge, gone blur further out. inside = also fill under the box.
    public static void shadow(GuiGraphics g, float x0, float y0, float x1, float y1, float r, float blur, int colour, boolean inside) {
        if (x1 - x0 <= 0 || y1 - y0 <= 0 || (colour >>> 24) == 0) return;
        float pixel = pixel(g);
        int n = segments(r + blur, pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float start = -Math.min(blur * 0.35f, Math.min(x1 - x0, y1 - y0) / 2f);
        int count = contour(XS, YS, x0, y0, x1, y1, r, r, r, r, start, n);
        if (inside) fan(v, m, XS, YS, count, y0, y1, colour, colour);
        int steps = Mth.clamp((int) (blur / pixel / 2f), 3, 7);
        float before = 1f;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps, off = start + (blur - start) * t, fade = (1f - t) * (1f - t);
            contour(XS2, YS2, x0, y0, x1, y1, r, r, r, r, off, n);
            band(v, m, XS2, YS2, XS, YS, count, y0, y1, colour, colour, fade, before);
            System.arraycopy(XS2, 0, XS, 0, count);
            System.arraycopy(YS2, 0, YS, 0, count);
            before = fade;
        }
    }

    // Any convex shape, points clockwise on screen, edges smoothed the same way.
    public static void polygon(GuiGraphics g, float[] xs, float[] ys, int count, int colour) {
        float half = pixel(g) / 2f;
        for (int i = 0; i < count; i++) {
            // which way this corner points out, from the two edges either side of it (y runs down, so clockwise = outside on the left of travel)
            int a = i == 0 ? count - 1 : i - 1, b = i + 1 == count ? 0 : i + 1;
            float ax = ys[i] - ys[a], ay = xs[a] - xs[i], bx = ys[b] - ys[i], by = xs[i] - xs[b];
            float al = Math.max(0.0001f, Mth.sqrt(ax * ax + ay * ay)), bl = Math.max(0.0001f, Mth.sqrt(bx * bx + by * by));
            ax /= al;
            ay /= al;
            bx /= bl;
            by /= bl;
            float nx = ax + bx, ny = ay + by, nl = Math.max(0.0001f, Mth.sqrt(nx * nx + ny * ny));
            nx /= nl;
            ny /= nl;
            float stretch = half / Math.max(0.35f, ax * nx + ay * ny);
            XS[i] = xs[i] - nx * stretch;
            YS[i] = ys[i] - ny * stretch;
            XS2[i] = xs[i] + nx * stretch;
            YS2[i] = ys[i] + ny * stretch;
        }
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        fan(v, m, XS, YS, count, 0f, 1f, colour, colour);
        band(v, m, XS2, YS2, XS, YS, count, 0f, 1f, colour, colour, 0f, 1f);
    }

    // A straight line with round ends.
    public static void line(GuiGraphics g, float ax, float ay, float bx, float by, float width, int colour) {
        float dx = bx - ax, dy = by - ay, length = Mth.sqrt(dx * dx + dy * dy), r = width / 2f;
        if (length < 0.001f) {
            disc(g, ax, ay, r, colour);
            return;
        }
        int n = Math.min(12, segments(r, pixel(g)) + 1);
        float[] xs = new float[2 * n + 2], ys = new float[2 * n + 2];
        float along = (float) Mth.atan2(dy, dx) - Mth.HALF_PI;
        for (int i = 0; i <= n; i++) {
            float angle = along + Mth.PI * i / n;
            xs[i] = bx + Mth.cos(angle) * r;
            ys[i] = by + Mth.sin(angle) * r;
            xs[n + 1 + i] = ax + Mth.cos(angle + Mth.PI) * r;
            ys[n + 1 + i] = ay + Mth.sin(angle + Mth.PI) * r;
        }
        polygon(g, xs, ys, 2 * n + 2, colour);
    }

    // Part of a circle, clockwise from the top: from and to go 0 to 1 round it. Round ends.
    public static void arc(GuiGraphics g, float cx, float cy, float r, float thick, float from, float to, int colour) {
        if (to <= from) return;
        float pixel = pixel(g), half = pixel / 2f, ro = r, ri = r - thick;
        int n = Mth.clamp((int) ((to - from) * Mth.TWO_PI * r / pixel / 3f), 3, 120);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int faint = alpha(colour, 0f);
        float[] radii = {ro + half, ro - half, ri + half, ri - half};
        int[] colours = {faint, colour, colour, faint};
        for (int i = 0; i < n; i++) {
            float a = (from + (to - from) * i / n) * Mth.TWO_PI - Mth.HALF_PI, b = (from + (to - from) * (i + 1) / n) * Mth.TWO_PI - Mth.HALF_PI;
            float ca = Mth.cos(a), sa = Mth.sin(a), cb = Mth.cos(b), sb = Mth.sin(b);
            for (int k = 0; k < 3; k++) {
                vertex(v, m, cx + ca * radii[k], cy + sa * radii[k], colours[k]);
                vertex(v, m, cx + ca * radii[k + 1], cy + sa * radii[k + 1], colours[k + 1]);
                vertex(v, m, cx + cb * radii[k + 1], cy + sb * radii[k + 1], colours[k + 1]);
                vertex(v, m, cx + cb * radii[k], cy + sb * radii[k], colours[k]);
            }
        }
        float mid = r - thick / 2f, aEnd = from * Mth.TWO_PI - Mth.HALF_PI, bEnd = to * Mth.TWO_PI - Mth.HALF_PI;
        disc(g, cx + Mth.cos(aEnd) * mid, cy + Mth.sin(aEnd) * mid, thick / 2f, colour);
        disc(g, cx + Mth.cos(bEnd) * mid, cy + Mth.sin(bEnd) * mid, thick / 2f, colour);
    }

    // A small arrow head: dx, dy say which way it points.
    public static void chevron(GuiGraphics g, float cx, float cy, float size, float dx, float dy, float width, int colour) {
        float px = -dy, py = dx;
        line(g, cx - dx * size / 2f + px * size, cy - dy * size / 2f + py * size, cx + dx * size / 2f, cy + dy * size / 2f, width, colour);
        line(g, cx - dx * size / 2f - px * size, cy - dy * size / 2f - py * size, cx + dx * size / 2f, cy + dy * size / 2f, width, colour);
    }

    // Paints the outside of a rounded corner: the bit between the square corner at (x, y) and the curve of radius r. Rounds off
    // anything that was drawn square, and makes the soft flare where a tab meets its card. sx, sy (+1 or -1) say which way the inside lies.
    public static void cornerOutside(GuiGraphics g, float x, float y, float r, int sx, int sy, int colour) {
        if (r <= 0) return;
        float pixel = pixel(g), half = pixel / 2f;
        int n = segments(r, pixel);
        VertexConsumer v = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float cx = x + sx * r, cy = y + sy * r;
        int faint = alpha(colour, 0f);
        boolean flip = sx * sy < 0; // keeps the faces pointing at the screen
        for (int i = 0; i < n; i++) {
            float a = Mth.HALF_PI * i / n, b = Mth.HALF_PI * (i + 1) / n;
            float ax = -sx * Mth.cos(a), ay = -sy * Mth.sin(a), bx = -sx * Mth.cos(b), by = -sy * Mth.sin(b);
            float ox = cx + ax * (r + half), oy = cy + ay * (r + half), px = cx + bx * (r + half), py = cy + by * (r + half);
            // from the square corner to just outside the curve: solid
            quad(v, m, flip, x, y, colour, ox, oy, colour, px, py, colour, px, py, colour);
            // across the curve: solid to clear
            quad(v, m, flip, ox, oy, colour, cx + ax * (r - half), cy + ay * (r - half), faint, cx + bx * (r - half), cy + by * (r - half), faint, px, py, colour);
        }
    }

    private static void quad(VertexConsumer v, Matrix4f m, boolean flip, float ax, float ay, int ac, float bx, float by, int bc, float cx, float cy, int cc, float dx, float dy, int dc) {
        if (flip) {
            vertex(v, m, dx, dy, dc);
            vertex(v, m, cx, cy, cc);
            vertex(v, m, bx, by, bc);
            vertex(v, m, ax, ay, ac);
        } else {
            vertex(v, m, ax, ay, ac);
            vertex(v, m, bx, by, bc);
            vertex(v, m, cx, cy, cc);
            vertex(v, m, dx, dy, dc);
        }
    }
}
