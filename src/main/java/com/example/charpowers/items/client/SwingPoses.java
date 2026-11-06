package com.example.charpowers.items.client;

import com.example.charpowers.items.model.WeaponStyle;

// Our own moves for the weapons, written by hand as a table so they're easy to change. Nothing here comes from another mod.
//
// A Body key says where things point when seen from outside: which way the arm goes from the shoulder and which way the blade should go, each as
// {right, up, forward}, plus how far the body is twisted to the right and leaned forward. A Hand key is the same move through your own eyes: where your fist is
// on the screen (x right, y up, z towards you) and the weapon's yaw, pitch, roll and spin in degrees.
final class SwingPoses {
    record Body(float t, float ax, float ay, float az, float bx, float by, float bz, float twist, float lean) {}

    record Hand(float t, float x, float y, float z, float yaw, float pitch, float roll, float spin) {}

    record Clip(Body[] body, Hand[] hand, float cutFrom, float cutTo) {}

    private static Body b(double t, double ax, double ay, double az, double bx, double by, double bz, double twist, double lean) {
        return new Body((float) t, (float) ax, (float) ay, (float) az, (float) bx, (float) by, (float) bz, (float) twist, (float) lean);
    }

    private static Hand h(double t, double x, double y, double z, double yaw, double pitch, double roll, double spin) {
        return new Hand((float) t, (float) x, (float) y, (float) z, (float) yaw, (float) pitch, (float) roll, (float) spin);
    }

    private static Clip clip(double cutFrom, double cutTo, Body[] body, Hand[] hand) {
        return new Clip(body, hand, (float) cutFrom, (float) cutTo);
    }

    private static Body[] bs(Body... k) {
        return k;
    }

    private static Hand[] hs(Hand... k) {
        return k;
    }

    // ---- what you hold when you aren't swinging

    static Body guardBody(WeaponStyle s) {
        return switch (s) {
            case GOLD -> b(0, 0.3, -0.72, 0.62, -0.1, 0.8, 0.6, 0.18, 0);
            case KATANA -> b(0, 0.32, -0.8, 0.5, -0.05, 0.62, 0.78, 0.25, 0);
            case HAMMER -> b(0, 0.3, -0.85, 0.42, -0.2, 0.75, 0.5, 0.2, 0);
            case STAFF -> b(0, 0.3, -0.42, 0.86, 0, 1, 0.12, 0.1, 0);
            case SPRAY -> b(0, 0.12, 0.05, 1, 0, 1, 0, 0, 0);
            default -> guardKey(s) != null ? bodyFrom(s, guardKey(s)) : b(0, 0.1, -0.9, 0.4, 0, 0.6, 0.8, 0, 0);
        };
    }

    static Hand guardHand(WeaponStyle s) {
        return switch (s) {
            case GOLD -> h(0, 0.5, -0.56, -0.86, -6, -36, 7, 0);
            case KATANA -> h(0, 0.52, -0.6, -0.82, -8, -46, 9, 0);
            case HAMMER -> h(0, 0.54, -0.72, -0.84, -10, -26, 11, 0);
            case STAFF -> h(0, 0.5, -0.68, -0.95, -4, -30, 6, 0);
            default -> guardKey(s) != null ? handFrom(s, guardKey(s)) : h(0, 0.52, -0.5, -0.72, 0, 0, 0, 0);
        };
    }

    // ---- every other move is written as a few keys of where the blade points: its direction round you (az: 0 ahead, plus to the right) and
    // up or down (el: 0 level, 90 straight up, past 90 back over your head), a turn round its own length (spin), how far it's pushed out (reach,
    // 0 to 1) and a roll in first person (for twirls). Both the arms seen from outside and your own view are worked out from the same keys,
    // so they always agree. Weight comes from the timing: a slow wind-up, a short fast strike, a little past where it hit, then back.
    private record K(float t, float az, float el, float spin, float reach, float roll) {}

    private static K k(double t, double az, double el) {
        return new K((float) t, (float) az, (float) el, 0f, 0f, 0f);
    }

    private static K k(double t, double az, double el, double spin, double reach) {
        return new K((float) t, (float) az, (float) el, (float) spin, (float) reach, 0f);
    }

    private static K k(double t, double az, double el, double spin, double reach, double roll) {
        return new K((float) t, (float) az, (float) el, (float) spin, (float) reach, (float) roll);
    }

    private static Body bodyFrom(WeaponStyle s, K k) {
        double az = Math.toRadians(k.az()), el = Math.toRadians(k.el());
        double bx = Math.sin(az) * Math.cos(el), by = Math.sin(el), bz = Math.cos(az) * Math.cos(el);
        double azA = az * 0.72, elA = Math.toRadians(Math.max(-88, Math.min(78, k.el() * 0.72 - 44 + k.reach() * 34)));
        double ax = Math.sin(azA) * Math.cos(elA), ay = Math.sin(elA), azz = Math.cos(azA) * Math.cos(elA);
        return b(k.t(), ax, ay, azz, bx, by, bz, Math.max(-0.75, Math.min(0.75, k.az() * 0.0068)), 0);
    }

    private static Hand handFrom(WeaponStyle s, K k) {
        double az = Math.toRadians(k.az()), e2 = Math.toRadians(k.el() * 0.42);
        double baseX = s.twoHanded ? 0.32 : 0.42;
        double lift = k.el() < 0 ? 0.24 * Math.min(1, -k.el() / 60.0) : 0;
        double x = baseX + 0.34 * Math.sin(az) * Math.cos(e2), y = -0.52 + 0.3 * Math.sin(e2) + lift, z = -0.76 - 0.22 * Math.cos(az) * Math.cos(e2) - 0.5 * k.reach();
        return h(k.t(), x, y, z, -k.az(), k.el() - 90, k.roll(), k.spin() + (s.flat ? 90 : 0));
    }

    private static Clip arc(WeaponStyle s, double cutFrom, double cutTo, K... keys) {
        Body[] body = new Body[keys.length];
        Hand[] hand = new Hand[keys.length];
        for (int i = 0; i < keys.length; i++) {
            body[i] = bodyFrom(s, keys[i]);
            hand[i] = handFrom(s, keys[i]);
        }
        return new Clip(body, hand, (float) cutFrom, (float) cutTo);
    }

    // the hold of the weapons made from keys
    private static K guardKey(WeaponStyle s) {
        return switch (s) {
            case FORK -> k(0, -6, 40);
            case DAGGER -> k(0, -10, -58);
            case KNIGHT -> k(0, -6, 50);
            case CRIMSON -> k(0, -8, 56);
            case GRAVITY -> k(0, -8, 48);
            case SPEAR -> k(0, -3, 22);
            case SCYTHE -> k(0, -10, 62);
            case COCYTUS -> k(0, -8, 52);
            default -> null;
        };
    }

    private static final Clip[] KATANA_MORE = {
            // iaido: sheathed low at the left hip, a breath, then drawn across in one flash and held there
            arc(WeaponStyle.KATANA, 0.38, 0.7, k(0, -140, -38, 0, 0), k(0.3, -150, -40, 0, 0), k(0.38, -128, -24, 0, 0.05), k(0.48, 5, 4, 0, 0.25), k(0.56, 72, 16, 0, 0.12),
                    k(0.64, 84, 20, 0, 0.08), k(0.8, 80, 18, 0, 0.05), k(1, 20, 50, 0, 0)),
            // a flourish: the blade turns once in the fingers and slides back into its sheath, then comes out again
            arc(WeaponStyle.KATANA, 2, 2, k(0, -8, 56, 38, 0), k(0.2, 25, 72, 0, 0), k(0.42, 25, 72, 360, 0), k(0.62, -138, -36, 360, 0), k(0.78, -140, -38, 360, 0), k(1, -8, 56, 398, 0)),
    };

    private static final Clip[] HAMMER_MORE = {
            // the sideways sweep: wound back to the right where you can see it, then carried round through everything
            arc(WeaponStyle.HAMMER, 0.42, 0.78, k(0, -10, 46, 90, 0), k(0.3, 72, 22, 90, 0), k(0.42, 80, 24, 90, 0), k(0.52, 30, 18, 90, 0.2), k(0.58, -10, 15, 90, 0.25),
                    k(0.68, -70, 12, 90, 0.15), k(0.8, -88, 10, 90, 0.05), k(1, -24, 40, 60, 0)),
    };

    private static final Clip[] STAFF_MORE = {
            // a spin on a crouching cast: twirled in front of you twice, then thrust out
            arc(WeaponStyle.STAFF, 2, 2, k(0, -4, 60, 0, 0, 0), k(0.12, 0, 88, 0, 0.1, 0), k(0.55, 0, 88, 0, 0.1, 720), k(0.66, -2, 22, 0, 0.75, 720), k(0.78, -2, 20, 0, 0.8, 720), k(1, -4, 58, 0, 0, 720)),
    };

    private static final Clip[] DAGGER = {
            // held point-down: a backhand slash right to left, the wrist doing the work
            arc(WeaponStyle.DAGGER, 0.12, 0.72, k(0, 42, -48), k(0.14, 56, -42), k(0.42, -8, -32, 0, 0.25), k(0.6, -62, -40, 0, 0.12), k(0.74, -70, -44), k(1, -16, -54)),
            // and back the other way
            arc(WeaponStyle.DAGGER, 0.12, 0.72, k(0, -46, -46), k(0.14, -58, -40), k(0.42, 8, -30, 0, 0.25), k(0.6, 62, -38, 0, 0.12), k(0.74, 70, -42), k(1, 12, -54)),
            // the dash stab: turned point-forward and driven straight in
            arc(WeaponStyle.DAGGER, 0.3, 0.62, k(0, -10, -50), k(0.2, -4, -6, 0, -0.25), k(0.45, 0, 0, 0, 1.05), k(0.62, 0, -2, 0, 0.95), k(1, -10, -52)),
    };

    private static final Clip[] FORK = {
            // a quick little jab
            arc(WeaponStyle.FORK, 2, 2, k(0, -6, 30), k(0.25, -6, 12, 0, -0.18), k(0.45, 0, -4, 0, 0.75), k(0.6, 0, -4, 0, 0.62), k(1, -6, 32)),
            arc(WeaponStyle.FORK, 2, 2, k(0, -6, 30), k(0.25, 6, 10, 0, -0.18), k(0.45, 12, -8, 0, 0.7), k(0.6, 12, -6, 0, 0.58), k(1, -6, 32)),
            // twirled in the fingers when you're bored
            arc(WeaponStyle.FORK, 2, 2, k(0, -6, 40, 0, 0), k(0.15, -6, 70, 0, 0.05), k(0.75, -6, 70, 720, 0.05), k(1, -6, 40, 720, 0)),
    };

    private static final Clip[] KNIGHT = {
            arc(WeaponStyle.KNIGHT, 0.12, 0.72, k(0, 50, 40), k(0.12, 62, 44), k(0.4, 0, 10, 0, 0.15), k(0.58, -60, -10, 0, 0.1), k(0.7, -68, -14), k(1, -10, 42)),
            arc(WeaponStyle.KNIGHT, 0.12, 0.72, k(0, -55, -10), k(0.12, -64, -14), k(0.4, 0, 16, 0, 0.15), k(0.58, 60, 46, 0, 0.1), k(0.7, 68, 50), k(1, -6, 50)),
            arc(WeaponStyle.KNIGHT, 0.32, 0.62, k(0, -6, 42), k(0.22, -4, 6, 0, -0.25), k(0.45, 0, 2, 0, 1), k(0.6, 0, 2, 0, 0.9), k(1, -6, 46)),
    };

    private static final Clip[] CRIMSON = {
            arc(WeaponStyle.CRIMSON, 0.12, 0.72, k(0, 55, 70, 30, 0), k(0.12, 63, 76, 30, 0), k(0.4, 0, 10, 30, 0.15), k(0.58, -55, -40, 30, 0.1), k(0.7, -62, -47, 30, 0), k(1, -10, 46)),
            arc(WeaponStyle.CRIMSON, 0.12, 0.72, k(0, -55, -40, 30, 0), k(0.12, -62, -47, 30, 0), k(0.4, 0, 10, 30, 0.15), k(0.58, 55, 66, 30, 0.1), k(0.7, 62, 72, 30, 0), k(1, -8, 56)),
            // the heavy one: up, a beat, down hard
            arc(WeaponStyle.CRIMSON, 0.42, 0.76, k(0, -8, 56), k(0.36, -5, 126, 90, 0), k(0.48, -5, 132, 90, 0), k(0.6, -2, 22, 90, 0.2), k(0.68, 0, -40, 90, 0.3), k(0.78, 0, -47, 90, 0.3), k(1, -8, 44)),
    };

    private static final Clip[] GRAVITY = {
            // a wide, slow, heavy cut: the wind-up shows it coming
            arc(WeaponStyle.GRAVITY, 0.4, 0.8, k(0, -8, 48), k(0.32, 84, 28), k(0.44, 90, 28), k(0.55, 10, 15, 0, 0.15), k(0.64, -64, 10, 0, 0.1), k(0.76, -92, 6), k(1, -20, 40)),
            arc(WeaponStyle.GRAVITY, 0.44, 0.78, k(0, -8, 48), k(0.38, -5, 128, 90, 0), k(0.5, -5, 134, 90, 0), k(0.62, -2, 20, 90, 0.2), k(0.7, 0, -42, 90, 0.3), k(0.8, 0, -48, 90, 0.3), k(1, -8, 44)),
    };

    private static final Clip[] SPEAR = {
            // both hands: pulled back, driven out, held, drawn back
            arc(WeaponStyle.SPEAR, 0.3, 0.6, k(0, -3, 12), k(0.22, -3, 10, 0, -0.38), k(0.42, 0, 5, 0, 1), k(0.56, 0, 5, 0, 0.95), k(1, -3, 12)),
            arc(WeaponStyle.SPEAR, 0.3, 0.6, k(0, -3, 12), k(0.22, -6, 30, 0, -0.32), k(0.42, 2, 20, 0, 1), k(0.56, 2, 20, 0, 0.92), k(1, -3, 12)),
            // crouched: a low sweep at the legs
            arc(WeaponStyle.SPEAR, 0.4, 0.8, k(0, -3, 12, 90, 0), k(0.28, 76, -6, 90, 0), k(0.38, 82, -8, 90, 0), k(0.55, 0, -14, 90, 0.2), k(0.66, -70, -12, 90, 0.1), k(0.78, -86, -10, 90, 0), k(1, -3, 12, 0, 0)),
    };

    private static final Clip[] SCYTHE = {
            // a wide reap from the right, the blade's point leading round
            arc(WeaponStyle.SCYTHE, 0.36, 0.84, k(0, -10, 62, 90, 0), k(0.26, 76, 36, 90, 0), k(0.36, 86, 40, 90, 0), k(0.5, 0, 12, 90, 0.2), k(0.64, -76, 2, 90, 0.1), k(0.76, -94, -4, 90, 0), k(1, -10, 58, 40, 0)),
            arc(WeaponStyle.SCYTHE, 0.36, 0.84, k(0, -10, 62, -90, 0), k(0.26, -80, 30, -90, 0), k(0.36, -90, 34, -90, 0), k(0.5, 0, 10, -90, 0.2), k(0.64, 76, 4, -90, 0.1), k(0.76, 92, 0, -90, 0), k(1, -10, 58, -40, 0)),
            arc(WeaponStyle.SCYTHE, 0.46, 0.8, k(0, -10, 62), k(0.32, -5, 132), k(0.46, -5, 138), k(0.58, 0, 24, 0, 0.2), k(0.68, 0, -44, 0, 0.3), k(0.8, 0, -50, 0, 0.3), k(1, -10, 56)),
    };

    private static final Clip[] COCYTUS = {
            arc(WeaponStyle.COCYTUS, 0.44, 0.74, k(0, -8, 52), k(0.3, -6, 126, 0, 0), k(0.46, -6, 132, 0, 0), k(0.57, -3, 40, 0, 0.15), k(0.63, 0, -40, 0, 0.3), k(0.72, 0, -48, 0, 0.32), k(0.88, 0, -44, 0, 0.3), k(1, -8, 20, 0, 0.1)),
            arc(WeaponStyle.COCYTUS, 0.42, 0.78, k(0, -8, 52, 90, 0), k(0.3, 72, 22, 90, 0), k(0.42, 80, 24, 90, 0), k(0.52, 30, 18, 90, 0.2), k(0.58, -10, 15, 90, 0.25), k(0.68, -70, 12, 90, 0.15), k(0.8, -88, 10, 90, 0.05), k(1, -24, 42, 60, 0)),
    };

    // ---- the Long Sword's four moves, then the others

    private static final Clip[] GOLD = {
            // horizontal slash, right to left
            clip(0.14, 0.72, bs(b(0, 0.65, -0.68, 0.34, 0.88, 0.2, 0.36, 0.7, 0), b(0.16, 0.65, -0.68, 0.34, 0.88, 0.2, 0.36, 0.7, 0), b(0.4, 0, -0.76, 0.64, 0, 0.18, 1, 0, 0.08),
                    b(0.62, -0.58, -0.7, 0.42, -0.92, 0.12, 0.3, -0.7, 0.1), b(1, -0.5, -0.72, 0.48, -0.85, 0.2, 0.45, -0.55, 0.04)),
                    hs(h(0, 0.82, -0.36, -0.92, -78, -90, 0, 0), h(0.16, 0.82, -0.36, -0.92, -78, -90, 0, 0), h(0.4, 0.1, -0.44, -1.18, 0, -92, 0, 0), h(0.62, -0.66, -0.5, -0.92, 80, -94, 0, 0), h(1, -0.5, -0.56, -0.9, 62, -90, 0, 0))),
            // reverse slash, rising from low on the left to high on the right
            clip(0.14, 0.72, bs(b(0, -0.6, -0.72, 0.34, -0.9, -0.12, 0.36, -0.7, 0.05), b(0.14, -0.6, -0.72, 0.34, -0.9, -0.12, 0.36, -0.7, 0.05), b(0.4, 0.05, -0.66, 0.74, 0, 0.32, 1, 0.05, 0.04),
                    b(0.64, 0.62, -0.46, 0.62, 0.86, 0.46, 0.22, 0.75, -0.04), b(1, 0.55, -0.5, 0.66, 0.8, 0.5, 0.35, 0.6, 0)),
                    hs(h(0, -0.7, -0.68, -0.9, 76, -106, 0, 0), h(0.14, -0.7, -0.68, -0.9, 76, -106, 0, 0), h(0.4, 0.06, -0.42, -1.18, 2, -88, 0, 0), h(0.64, 0.78, -0.1, -0.92, -80, -70, 0, 0), h(1, 0.66, -0.22, -0.9, -60, -68, 0, 0))),
            // overhead slam: up, a beat, then all the way down
            clip(0.4, 0.74, bs(b(0, 0.12, 0.96, 0.22, 0, 0.14, -1, 0.15, -0.14), b(0.38, 0.12, 0.96, 0.26, 0, 0.14, -1, 0.15, -0.16), b(0.54, 0.05, 0.2, 1, 0, 1, -0.1, 0.05, 0.12),
                    b(0.68, 0.02, -0.94, 0.14, 0, 0.12, 1, 0, 0.5), b(0.86, 0.02, -0.94, 0.14, 0, 0.12, 1, 0, 0.5), b(1, 0.1, -0.9, 0.3, 0, 0.3, 1, 0.05, 0.3)),
                    hs(h(0, 0.46, 0.18, -1.1, -8, 16, 0, 90), h(0.38, 0.44, 0.22, -1.08, -8, 20, 0, 90), h(0.54, 0.3, -0.2, -1.2, -6, -80, 0, 90), h(0.68, 0.26, -0.72, -1.1, -5, -118, 0, 90), h(0.86, 0.26, -0.74, -1.08, -5, -120, 0, 90), h(1, 0.36, -0.68, -1.0, -5, -96, 0, 60))),
            // finisher: pull right back, a low driving thrust with the whole body behind it
            clip(0.26, 0.6, bs(b(0, 0.45, -0.86, 0.1, 0.02, 0.3, 1, 0.55, -0.1), b(0.26, 0.45, -0.86, 0.1, 0.02, 0.3, 1, 0.55, -0.1), b(0.44, 0.06, -0.8, 0.6, 0, 0.5, 0.87, -0.2, 0.55),
                    b(0.7, 0.06, -0.8, 0.6, 0, 0.5, 0.87, -0.2, 0.55), b(1, 0.2, -0.8, 0.56, 0, 0.6, 0.8, 0.05, 0.1)),
                    hs(h(0, 0.54, -0.5, -0.22, -5, -92, 0, 0), h(0.26, 0.54, -0.5, -0.22, -5, -92, 0, 0), h(0.44, 0.1, -0.26, -1.72, 0, -90, 0, 0), h(0.7, 0.1, -0.26, -1.72, 0, -90, 0, 0), h(1, 0.4, -0.5, -1.0, -3, -60, 0, 0))),
    };

    private static final Clip[] KATANA = {
            // a long diagonal cut, high right to low left
            clip(0.12, 0.72, bs(b(0, 0.55, 0.05, 0.84, 0.72, 0.68, -0.15, 0.65, -0.05), b(0.14, 0.55, 0.05, 0.84, 0.72, 0.68, -0.15, 0.65, -0.05), b(0.42, 0.05, -0.62, 0.78, 0, 0.14, 1, 0, 0.14),
                    b(0.66, -0.52, -0.82, 0.24, -0.8, -0.4, 0.42, -0.75, 0.2), b(1, -0.45, -0.82, 0.34, -0.72, -0.2, 0.62, -0.5, 0.1)),
                    hs(h(0, 0.7, 0.02, -0.86, -58, -34, 0, 38), h(0.14, 0.7, 0.02, -0.86, -58, -34, 0, 38), h(0.42, 0.08, -0.42, -1.14, 2, -92, 0, 38), h(0.66, -0.56, -0.86, -0.9, 62, -142, 0, 38), h(1, -0.46, -0.72, -0.9, 48, -118, 0, 30))),
            // the answer: rising back the other way
            clip(0.12, 0.72, bs(b(0, -0.5, -0.84, 0.22, -0.78, -0.3, 0.46, -0.7, 0.15), b(0.14, -0.5, -0.84, 0.22, -0.78, -0.3, 0.46, -0.7, 0.15), b(0.42, 0.05, -0.55, 0.82, 0, 0.3, 1, 0, 0.05),
                    b(0.66, 0.58, -0.08, 0.8, 0.74, 0.65, -0.1, 0.7, -0.05), b(1, 0.5, -0.2, 0.82, 0.68, 0.62, 0.2, 0.55, 0)),
                    hs(h(0, -0.56, -0.84, -0.9, 62, -138, 0, -38), h(0.14, -0.56, -0.84, -0.9, 62, -138, 0, -38), h(0.42, 0.08, -0.36, -1.14, 0, -86, 0, -38), h(0.66, 0.74, 0.06, -0.86, -60, -36, 0, -38), h(1, 0.62, -0.08, -0.86, -46, -44, 0, -30))),
            KATANA_MORE[0], KATANA_MORE[1],
    };

    private static final Clip[] HAMMER = {
            // heave it up, hold, bring it down
            clip(0.42, 0.72, bs(b(0, 0.25, -0.2, 0.9, -0.1, 0.9, 0.2, 0.1, 0), b(0.26, 0.15, 0.97, -0.05, 0, 0.1, -1, 0.25, -0.2), b(0.46, 0.15, 0.97, 0, 0, 0.1, -1, 0.25, -0.22),
                    b(0.6, 0.06, 0.05, 1, 0, 1, -0.05, 0.05, 0.14), b(0.72, 0.03, -0.95, 0.1, 0, 0.1, 1, 0, 0.55), b(0.9, 0.03, -0.95, 0.1, 0, 0.1, 1, 0, 0.55), b(1, 0.12, -0.9, 0.3, 0, 0.35, 0.95, 0.05, 0.3)),
                    hs(h(0, 0.5, -0.66, -0.86, -8, -24, 8, 0), h(0.26, 0.46, 0.16, -1.15, -8, 14, 4, 0), h(0.46, 0.44, 0.2, -1.12, -8, 18, 4, 0), h(0.6, 0.3, -0.24, -1.2, -6, -74, 0, 0), h(0.72, 0.28, -0.74, -1.08, -5, -112, 0, 0), h(0.9, 0.28, -0.76, -1.06, -5, -114, 0, 0), h(1, 0.36, -0.72, -0.96, -5, -84, 2, 0))),
            HAMMER_MORE[0],
    };

    private static final Clip[] STAFF = {
            // pushes it forward to cast or to poke
            clip(2, 2, bs(b(0, 0.3, -0.42, 0.86, 0, 1, 0.12, 0.1, 0), b(0.35, 0.08, 0.3, 0.95, 0, 0.8, 0.6, -0.1, 0.12), b(1, 0.2, -0.3, 0.92, 0, 1, 0.3, 0.05, 0.02)),
                    hs(h(0, 0.5, -0.68, -0.95, -4, -30, 6, 0), h(0.35, 0.34, -0.44, -1.4, -2, -68, 2, 0), h(1, 0.46, -0.62, -1.05, -3, -40, 4, 0))),
            STAFF_MORE[0],
    };

    static Clip clip(WeaponStyle s, int step) {
        Clip[] set = set(s);
        if (set == null) return null;
        return set[Math.floorMod(step, set.length)];
    }

    private static Clip[] set(WeaponStyle s) {
        return switch (s) {
            case GOLD -> GOLD;
            case KATANA -> KATANA;
            case DAGGER -> DAGGER;
            case HAMMER -> HAMMER;
            case STAFF -> STAFF;
            case FORK -> FORK;
            case KNIGHT -> KNIGHT;
            case CRIMSON -> CRIMSON;
            case GRAVITY -> GRAVITY;
            case SPEAR -> SPEAR;
            case SCYTHE -> SCYTHE;
            case COCYTUS -> COCYTUS;
            default -> null;
        };
    }

    private SwingPoses() {}
}
