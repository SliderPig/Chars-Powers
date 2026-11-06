package com.example.charpowers.house.model;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// A little isometric picture of a pile of coloured blocks, for the picker. Pure maths: only the blocks you could see are drawn.
public final class IsoThumb {
    public static final int W = 180, H = 130;
    private static final double COS = 0.866, SIN = 0.5;

    private IsoThumb() {}

    // cells: x, y, z, 0xRRGGBB, four ints per block. Returns W x H pixels of 0xAARRGGBB, transparent round the house.
    public static int[] render(int w, int h, int d, int[] cells) {
        int[] out = new int[W * H];
        if (cells.length == 0) return out;
        // nobody sees a block with something on all the sides we look from
        BitSet solid = new BitSet(w * h * d);
        for (int i = 0; i < cells.length; i += 4) solid.set(index(cells[i], cells[i + 1], cells[i + 2], w, d));
        // back to front, bottom to top: later ones paint over earlier ones
        long[] order = new long[cells.length / 4];
        int n = 0;
        for (int i = 0; i < cells.length; i += 4) {
            int x = cells[i], y = cells[i + 1], z = cells[i + 2];
            boolean top = y + 1 >= h || !solid.get(index(x, y + 1, z, w, d));
            boolean front = z + 1 >= d || !solid.get(index(x, y, z + 1, w, d));
            boolean side = x + 1 >= w || !solid.get(index(x + 1, y, z, w, d));
            if (!top && !front && !side) continue;
            order[n++] = ((long) (x + y + z) << 32) | (i & 0xFFFFFFFFL);
        }
        java.util.Arrays.sort(order, 0, n);
        double widthUnits = (w + d) * COS, heightUnits = (w + d) * SIN + h;
        double scale = Math.min((W - 2) / widthUnits, (H - 2) / heightUnits);
        double ox = (W - widthUnits * scale) / 2 + d * COS * scale;
        double oy = (H - heightUnits * scale) / 2 + h * scale;
        for (int k = 0; k < n; k++) {
            int i = (int) order[k];
            int x = cells[i], y = cells[i + 1], z = cells[i + 2], rgb = cells[i + 3];
            double[] a = project(x, y + 1, z, scale, ox, oy), b = project(x + 1, y + 1, z, scale, ox, oy), c = project(x + 1, y + 1, z + 1, scale, ox, oy),
                    e = project(x, y + 1, z + 1, scale, ox, oy), f = project(x, y, z + 1, scale, ox, oy), g = project(x + 1, y, z + 1, scale, ox, oy), j = project(x + 1, y, z, scale, ox, oy);
            quad(out, a, b, c, e, shade(rgb, 1.0));
            quad(out, e, c, g, f, shade(rgb, 0.8));
            quad(out, c, b, j, g, shade(rgb, 0.62));
        }
        return out;
    }

    private static int index(int x, int y, int z, int w, int d) {
        return x + z * w + y * w * d;
    }

    private static double[] project(int x, int y, int z, double scale, double ox, double oy) {
        return new double[] {ox + (x - z) * COS * scale, oy + (x + z) * SIN * scale - y * scale};
    }

    private static int shade(int rgb, double k) {
        int r = (int) (((rgb >> 16) & 255) * k), g = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    // A filled four-sided shape.
    private static void quad(int[] out, double[] a, double[] b, double[] c, double[] d, int colour) {
        double[][] pts = {a, b, c, d};
        int y0 = H, y1 = -1;
        for (double[] p : pts) {
            y0 = Math.min(y0, (int) Math.floor(p[1]));
            y1 = Math.max(y1, (int) Math.ceil(p[1]));
        }
        y0 = Math.max(0, y0);
        y1 = Math.min(H - 1, y1);
        for (int y = y0; y <= y1; y++) {
            double mid = y + 0.5, lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                double[] p = pts[i], q = pts[(i + 1) % 4];
                if ((p[1] <= mid && q[1] > mid) || (q[1] <= mid && p[1] > mid)) {
                    double at = p[0] + (mid - p[1]) / (q[1] - p[1]) * (q[0] - p[0]);
                    lo = Math.min(lo, at);
                    hi = Math.max(hi, at);
                }
            }
            if (hi < lo) continue;
            int x0 = Math.max(0, (int) Math.floor(lo)), x1 = Math.min(W - 1, (int) Math.ceil(hi) - 1);
            for (int x = x0; x <= x1; x++) out[x + y * W] = colour;
        }
    }

    // Squeezed small enough to send to the PC.
    public static byte[] pack(int[] pixels) {
        byte[] raw = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            raw[i * 4] = (byte) (pixels[i] >>> 24);
            raw[i * 4 + 1] = (byte) (pixels[i] >>> 16);
            raw[i * 4 + 2] = (byte) (pixels[i] >>> 8);
            raw[i * 4 + 3] = (byte) pixels[i];
        }
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer));
        deflater.end();
        return out.toByteArray();
    }

    // Null if what arrived isn't exactly one picture.
    public static int[] unpack(byte[] packed) {
        byte[] raw = new byte[W * H * 4];
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(packed);
            int n = 0;
            while (n < raw.length && !inflater.finished()) {
                int got = inflater.inflate(raw, n, raw.length - n);
                if (got == 0 && (inflater.needsInput() || inflater.needsDictionary())) break;
                n += got;
            }
            if (n != raw.length) return null;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
        int[] pixels = new int[W * H];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (raw[i * 4] & 255) << 24 | (raw[i * 4 + 1] & 255) << 16 | (raw[i * 4 + 2] & 255) << 8 | (raw[i * 4 + 3] & 255);
        }
        return pixels;
    }
}
