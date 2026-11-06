# Makes the Green Lantern construct sounds by synthesis (no recordings): an air whoosh with a slight energy hum when a construct
# forms, a heavy swoosh for a hand swing, a thump on impact and a shimmer when it dissolves.
# Run from the charpowers folder:  python tools/make_lantern_sounds.py
import os

import numpy as np
import soundfile as sf

SR = 44100
OUT = 'src/main/resources/assets/charpowers/sounds/'
rng = np.random.default_rng(7)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(n):
    return rng.standard_normal(n)


def lowpass(x, cutoff):
    # one-pole low pass; cutoff may be a per-sample array
    c = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = 1 - np.exp(-2 * np.pi * c / SR)
    y = np.empty_like(x)
    s = 0.0
    for i in range(len(x)):
        s += a[i] * (x[i] - s)
        y[i] = s
    return y


def band(x, centre, width=0.6):
    # a moving band: low pass at centre*(1+w) minus low pass at centre*(1-w)
    return lowpass(x, centre * (1 + width)) - lowpass(x, centre * (1 - width * 0.8))


def norm(x, peak=0.85):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def fade(x, ms=8):
    n = int(SR * ms / 1000)
    x = x.copy()
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    sf.write(OUT + name + '.ogg', fade(norm(x)).astype(np.float32), SR, format='OGG', subtype='VORBIS')


def construct_form():
    s = t(0.75)
    env = np.sin(np.pi * np.clip(s / 0.75, 0, 1)) ** 1.6
    sweep = 500 + 2200 * np.sin(np.pi * np.clip(s / 0.75, 0, 1) ** 0.8)
    air = band(noise(len(s)), sweep, 0.7) * env
    hum_f = 150 + 120 * s / 0.75
    hum = (np.sin(2 * np.pi * np.cumsum(hum_f) / SR) * 0.5 + np.sin(2 * np.pi * np.cumsum(hum_f * 2.01) / SR) * 0.25) * (0.75 + 0.25 * np.sin(2 * np.pi * 22 * s))
    return norm(air) * 0.85 + norm(hum * env) * 0.22


def hand_swing():
    s = t(0.5)
    env = np.exp(-((s - 0.16) / 0.11) ** 2)
    sweep = 1500 * np.exp(-s * 3.0) + 260
    air = band(noise(len(s)), sweep, 0.9) * env
    low = lowpass(noise(len(s)), 220) * env
    return norm(air) * 0.7 + norm(low) * 0.5


def impact_thump():
    s = t(0.45)
    f = 38 + 70 * np.exp(-s * 22)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-s * 9)
    click = lowpass(noise(len(s)), 900) * np.exp(-s * 38)
    return norm(body) * 0.9 + norm(click) * 0.35


def dissolve():
    s = t(0.7)
    out = np.zeros(len(s))
    for f in rng.uniform(1800, 5200, 14):
        start = rng.uniform(0, 0.25)
        tone = np.sin(2 * np.pi * f * s) * np.exp(-np.clip(s - start, 0, None) * rng.uniform(5, 9)) * (s >= start)
        out += tone * (0.6 + 0.4 * np.sin(2 * np.pi * rng.uniform(8, 18) * s))
    air = band(noise(len(s)), 3000 * np.exp(-s * 1.2) + 800, 0.8) * np.exp(-s * 5)
    return norm(out) * 0.6 + norm(air) * 0.3


if __name__ == '__main__':
    for name, fn in (('construct_form', construct_form), ('hand_swing', hand_swing), ('impact_thump', impact_thump), ('construct_dissolve', dissolve)):
        save(name, fn())
        print('made', name)
