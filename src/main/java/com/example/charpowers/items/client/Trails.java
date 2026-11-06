package com.example.charpowers.items.client;

import com.example.charpowers.items.model.TrailMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// The swing trails. A ribbon remembers where both ends of a blade were over the last moments, in the world, and is drawn as a smooth strip that fades towards its old end.
// It's cheap: a ribbon has a few dozen points and exists only for the fraction of a second after a swing, and nothing runs when nobody is swinging.
public final class Trails {
    private static final int MAX_POINTS = 96, SUB = 3, MAX_RIBBONS = 48;
    private static final double MIN_MOVE_SQ = 0.0009;
    private static final List<Ribbon> RIBBONS = new ArrayList<>();
    private static final Map<Long, Ribbon> LIVE = new HashMap<>();

    private Trails() {}

    private static final class Ribbon {
        final TrailStyle style;
        final double[] ax = new double[MAX_POINTS], ay = new double[MAX_POINTS], az = new double[MAX_POINTS];
        final double[] bx = new double[MAX_POINTS], by = new double[MAX_POINTS], bz = new double[MAX_POINTS];
        final float[] t = new float[MAX_POINTS];
        int n;

        Ribbon(TrailStyle style) {
            this.style = style;
        }

        void add(double[] base, double[] tip, float time) {
            if (n > 0 && dist2(ax[n - 1], ay[n - 1], az[n - 1], base) + dist2(bx[n - 1], by[n - 1], bz[n - 1], tip) < MIN_MOVE_SQ) {
                t[n - 1] = time;
                return;
            }
            if (n == MAX_POINTS) {
                shift(ax);
                shift(ay);
                shift(az);
                shift(bx);
                shift(by);
                shift(bz);
                System.arraycopy(t, 1, t, 0, MAX_POINTS - 1);
                n--;
            }
            ax[n] = base[0];
            ay[n] = base[1];
            az[n] = base[2];
            bx[n] = tip[0];
            by[n] = tip[1];
            bz[n] = tip[2];
            t[n] = time;
            n++;
        }

        private static void shift(double[] a) {
            System.arraycopy(a, 1, a, 0, MAX_POINTS - 1);
        }

        private static double dist2(double x, double y, double z, double[] p) {
            double dx = x - p[0], dy = y - p[1], dz = z - p[2];
            return dx * dx + dy * dy + dz * dz;
        }

        boolean dead(float now) {
            return n == 0 || now - t[n - 1] > style.life;
        }
    }

    private static long key(int id, TrailStyle s) {
        return ((long) id << 8) | s.ordinal();
    }

    // Another point of what this thing's blade is doing. Starts a new ribbon if there wasn't one going.
    static void sample(int id, TrailStyle style, double[] base, double[] tip, float now) {
        Ribbon r = LIVE.computeIfAbsent(key(id, style), k -> {
            Ribbon fresh = new Ribbon(style);
            RIBBONS.add(fresh);
            if (RIBBONS.size() > MAX_RIBBONS) RIBBONS.remove(0);
            return fresh;
        });
        r.add(base, tip, now);
    }

    // This thing's swing is over: its next one is a new ribbon, not joined to this.
    static void cut(int id) {
        for (TrailStyle s : TrailStyle.values()) LIVE.remove(key(id, s));
    }

    // A line of light rushing along the ground. The points are dated in the future and light up as their moment comes.
    static void groundLine(TrailStyle style, List<double[]> left, List<double[]> right, float start, float ticksPerPoint) {
        Ribbon r = new Ribbon(style);
        for (int i = 0; i < left.size() && i < MAX_POINTS; i++) r.add(left.get(i), right.get(i), start + i * ticksPerPoint);
        RIBBONS.add(r);
    }

    static void render(PoseStack pose, Camera camera, float partial) {
        if (RIBBONS.isEmpty()) return;
        float now = SwingTrack.now(partial);
        Iterator<Ribbon> it = RIBBONS.iterator();
        while (it.hasNext()) {
            Ribbon r = it.next();
            if (r.dead(now)) {
                it.remove();
                LIVE.values().remove(r);
            }
        }
        if (RIBBONS.isEmpty()) return;
        Vec3 cam = camera.getPosition();
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        for (Ribbon r : RIBBONS) {
            if (r.n < 2) continue;
            for (TrailStyle.Layer layer : r.style.layers) draw(r, layer, buffers.getBuffer(layer.type()), m, cam, now);
            for (TrailStyle.Layer layer : r.style.layers) buffers.endBatch(layer.type());
        }
    }

    private static void draw(Ribbon r, TrailStyle.Layer layer, VertexConsumer v, Matrix4f m, Vec3 cam, float now) {
        float width = r.style.width;
        boolean add = layer.additive();
        double[] p0 = null, p1 = null;
        float a0 = 0, u0 = 0;
        for (int i = 0; i < r.n - 1; i++) {
            int a = Math.max(0, i - 1), b = i, c = i + 1, d = Math.min(r.n - 1, i + 2);
            for (int s = (i == 0 ? 0 : 1); s <= SUB; s++) {
                double u = s / (double) SUB;
                double tx = TrailMath.catmullRom(r.bx[a], r.bx[b], r.bx[c], r.bx[d], u), ty = TrailMath.catmullRom(r.by[a], r.by[b], r.by[c], r.by[d], u), tz = TrailMath.catmullRom(r.bz[a], r.bz[b], r.bz[c], r.bz[d], u);
                double bx = TrailMath.catmullRom(r.ax[a], r.ax[b], r.ax[c], r.ax[d], u), by = TrailMath.catmullRom(r.ay[a], r.ay[b], r.ay[c], r.ay[d], u), bz = TrailMath.catmullRom(r.az[a], r.az[b], r.az[c], r.az[d], u);
                if (width > 0) {
                    // a thing with no blade: the strip faces the camera
                    double dx = r.bx[c] - r.bx[b], dy = r.by[c] - r.by[b], dz = r.bz[c] - r.bz[b];
                    double vx = tx - cam.x, vy = ty - cam.y, vz = tz - cam.z;
                    double sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
                    double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
                    if (len < 1e-6) continue;
                    sx *= width / len;
                    sy *= width / len;
                    sz *= width / len;
                    bx = tx - sx;
                    by = ty - sy;
                    bz = tz - sz;
                    tx += sx;
                    ty += sy;
                    tz += sz;
                }
                float time = (float) (r.t[b] + (r.t[c] - r.t[b]) * u);
                float age = now - time;
                // future points of a ground line aren't lit yet
                float life = age < 0 ? 0 : (float) TrailMath.fade(age, r.style.life);
                float alpha = layer.a() * life;
                // the newest edge comes up quickly rather than starting as a hard wall
                float head = i >= r.n - 2 ? (float) (1 - u * 0.65) : 1;
                alpha *= head;
                double[] q0 = {bx + (tx - bx) * layer.from() - cam.x, by + (ty - by) * layer.from() - cam.y, bz + (tz - bz) * layer.from() - cam.z};
                double[] q1 = {bx + (tx - bx) * layer.to() - cam.x, by + (ty - by) * layer.to() - cam.y, bz + (tz - bz) * layer.to() - cam.z};
                float uv = time * 0.18f;
                if (p0 != null) {
                    quad(v, m, p0, p1, q1, q0, layer, a0, alpha, u0, uv, add);
                }
                p0 = q0;
                p1 = q1;
                a0 = alpha;
                u0 = uv;
            }
        }
    }

    private static void quad(VertexConsumer v, Matrix4f m, double[] p0, double[] p1, double[] q1, double[] q0, TrailStyle.Layer l, float a0, float a1, float u0, float u1, boolean add) {
        vertex(v, m, p0, l, a0, u0, 0f, add);
        vertex(v, m, p1, l, a0, u0, 1f, add);
        vertex(v, m, q1, l, a1, u1, 1f, add);
        vertex(v, m, q0, l, a1, u1, 0f, add);
    }

    private static void vertex(VertexConsumer v, Matrix4f m, double[] p, TrailStyle.Layer l, float a, float u, float tv, boolean add) {
        // light that adds up fades by getting dimmer; a smear has to fade by getting clearer
        if (add) v.vertex(m, (float) p[0], (float) p[1], (float) p[2]).color(l.r() * a, l.g() * a, l.b() * a, 1f).uv(u, tv).endVertex();
        else v.vertex(m, (float) p[0], (float) p[1], (float) p[2]).color(l.r(), l.g(), l.b(), a).uv(u, tv).endVertex();
    }
}
