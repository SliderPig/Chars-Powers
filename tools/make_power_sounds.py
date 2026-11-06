# Makes the sounds of Char's own powers from nothing but maths: noise, filters, envelopes and a little reverb. Nothing is
# copied from anywhere. Run from the charpowers folder:  python tools/make_power_sounds.py
# Writes src/main/resources/assets/charpowers/sounds/powers/<name>.ogg, then opens each one again and checks it:
# peak at -3 dB or lower, nothing clipped, sensible length, not silent.
# The list of sounds is NAMES at the bottom; each is registered in sound/PowerSounds.java and sounds.json.
import os
import sys

import numpy as np
import soundfile

SR = 44100
OUT = 'src/main/resources/assets/charpowers/sounds/powers'
PEAK_DB = -4.0  # a little below the limit, because squeezing to OGG moves peaks slightly


def rng(seed):
    return np.random.default_rng(seed)


def n_of(seconds):
    return int(seconds * SR)


def times(seconds):
    return np.arange(n_of(seconds)) / SR


def noise(seconds, seed=1):
    return rng(seed).uniform(-1, 1, n_of(seconds))


def lowpass(x, fc):
    """One-pole low-pass. fc can change over time (an array the length of x)."""
    fc = np.broadcast_to(np.asarray(fc, dtype=float), x.shape)
    a = 1 - np.exp(-2 * np.pi * fc / SR)
    out = np.zeros_like(x)
    y = 0.0
    for i in range(len(x)):
        y += a[i] * (x[i] - y)
        out[i] = y
    return out


def highpass(x, fc):
    return x - lowpass(x, fc)


def bandpass(x, fc, q=2.0):
    """A resonant band-pass (state variable filter). fc can change over time."""
    fc = np.broadcast_to(np.asarray(fc, dtype=float), x.shape)
    f = 2 * np.sin(np.pi * np.minimum(fc, SR / 6.5) / SR)
    d = 1.0 / q
    low = band = 0.0
    out = np.zeros_like(x)
    for i in range(len(x)):
        low += f[i] * band
        high = x[i] - low - d * band
        band += f[i] * high
        out[i] = band
    return out


def env(seconds, points):
    """A shape over time from (time as 0..1, level) points, straight lines between them."""
    t = np.linspace(0, 1, n_of(seconds))
    xs, ys = zip(*points)
    return np.interp(t, xs, ys)


def decay(seconds, tau, start=0.0):
    t = times(seconds)
    out = np.exp(-(t - start).clip(0) / tau)
    out[t < start] = 0
    return out


def sine_sweep(seconds, f0, f1, curve='exp', phase=0.0):
    n = n_of(seconds)
    u = np.linspace(0, 1, n)
    f = f0 * (f1 / f0) ** u if curve == 'exp' else f0 + (f1 - f0) * u
    return np.sin(phase + 2 * np.pi * np.cumsum(f) / SR)


def saw(freq, seconds, harmonics=14):
    """A soft saw: freq can change over time."""
    n = n_of(seconds)
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    ph = 2 * np.pi * np.cumsum(f) / SR
    out = np.zeros(n)
    for h in range(1, harmonics + 1):
        out += np.sin(ph * h) / h
    return out


def ring(freq, seconds, tau, start=0.0, amp=1.0):
    """A struck thing: one partial that rings and dies away."""
    t = times(seconds)
    e = np.exp(-(t - start).clip(0) / tau)
    e[t < start] = 0
    return amp * e * np.sin(2 * np.pi * freq * (t - start).clip(0))


def place(mix, part, at):
    """Adds a short sound into a longer one at a time in seconds."""
    i = int(at * SR)
    j = min(len(mix), i + len(part))
    if i < len(mix):
        mix[i:j] += part[:j - i]
    return mix


def reverb(x, tail=1.0, mix=0.3, seed=9, tone=5000):
    """A simple room: the sound convolved with a burst of dying noise."""
    x = x.copy()
    x[-n_of(0.04):] *= np.linspace(1, 0, n_of(0.04))  # the sound itself must not stop dead under the tail
    ir = lowpass(noise(tail, seed), tone) * decay(tail, tail / 5)
    ir /= np.sqrt((ir ** 2).sum()) or 1
    size = len(x) + len(ir)
    wet = np.fft.irfft(np.fft.rfft(np.pad(x, (0, len(ir)))) * np.fft.rfft(ir, size), size)
    dry = np.pad(x, (0, len(ir)))
    return dry * (1 - mix) + wet * mix * 3


def soft_clip(x, drive=2.0):
    return np.tanh(x * drive) / np.tanh(drive)


def finish(x, fade_in=0.002, fade_out=0.03):
    """No clicks at the ends, no DC, peak just below the limit."""
    x = x - x.mean()
    a, b = n_of(fade_in), n_of(fade_out)
    if a:
        x[:a] *= np.linspace(0, 1, a)
    if b:
        x[-b:] *= np.linspace(1, 0, b)
    return x / np.abs(x).max() * 10 ** (PEAK_DB / 20)


# ---------------------------------------------------------------- Time Control
def time_slow():
    """Slow Down: everything winds down like a tape losing speed."""
    d = 1.5
    u = np.linspace(0, 1, n_of(d))
    wobble = 1 + 0.015 * np.sin(2 * np.pi * 5 * u * d)
    drone = saw(190 * (38 / 190) ** u * wobble, d, 10)
    drone = lowpass(drone, 2400 * (120 / 2400) ** u)
    air = bandpass(noise(d, 2), 3400 * (170 / 3400) ** u, 1.4) * 0.8
    shape = env(d, [(0, 0), (0.08, 1), (0.35, 0.8), (1, 0)])
    return reverb((drone * 0.5 + air) * shape, 0.7, 0.25)


def time_tick():
    """A clockwork tick and tock."""
    d = 0.3
    mix = np.zeros(n_of(d))
    for at, f, amp in ((0.0, 1, 1.0), (0.14, 0.72, 0.7)):
        click = bandpass(noise(0.02, 3), 3100 * f, 5) * decay(0.02, 0.004)
        part = np.zeros(n_of(0.1))
        part[:len(click)] += click * 1.6
        part += ring(1850 * f, 0.1, 0.012) * 0.5 + ring(2760 * f, 0.1, 0.008) * 0.35 + ring(640 * f, 0.1, 0.018) * 0.4
        place(mix, part * amp, at)
    return reverb(mix, 0.25, 0.12, tone=7000)


def time_stop():
    """Still Moment and the Time Block: air rushes in, cuts dead, and one glassy chime hangs there."""
    d = 2.7
    swell_d = 0.42
    u = np.linspace(0, 1, n_of(swell_d))
    swell = bandpass(noise(swell_d, 4), 500 * (6000 / 500) ** u, 1.6) * u ** 2.2
    mix = np.zeros(n_of(d))
    place(mix, swell * 0.7, 0)
    # the bar-chime's partials are not in tune with each other, which is what makes it sound struck rather than played
    f = 932.0
    for ratio, amp, tau in ((1, 1, 0.75), (2.756, 0.55, 0.4), (5.404, 0.3, 0.2), (8.933, 0.14, 0.09), (0.5, 0.25, 0.9)):
        place(mix, ring(f * ratio, d - swell_d, tau, 0, amp) * 0.5, swell_d)
    return reverb(mix, 0.8, 0.35, tone=7000)


def time_rush():
    """Gotta Move switched on: a quick rising rush of air."""
    d = 0.6
    u = np.linspace(0, 1, n_of(d))
    air = bandpass(noise(d, 5), 350 * (5200 / 350) ** u, 1.3)
    tone = sine_sweep(d, 160, 980) * 0.22
    return (air + tone) * env(d, [(0, 0), (0.7, 1), (1, 0)])


# ---------------------------------------------------------------- Cloning
def mirror_shimmer():
    """A clone steps out: many tiny glass partials waking up one after another."""
    d = 1.1
    r = rng(11)
    mix = np.zeros(n_of(d))
    for i in range(34):
        f = r.uniform(2200, 9500)
        at = r.uniform(0, 0.5) * (i / 34) ** 0.6
        tau = r.uniform(0.05, 0.22)
        place(mix, ring(f, 0.7, tau, 0, r.uniform(0.25, 0.8)), at)
    hum = sine_sweep(d, 420, 610) * env(d, [(0, 0), (0.25, 0.5), (1, 0)]) * 0.5
    return reverb(mix * 0.5 + hum * 0.5, 0.9, 0.35, tone=9000)


def glass_shatter():
    """The reflection breaks out of the glass: one crack, then shards falling and ringing."""
    d = 1.35
    r = rng(12)
    mix = np.zeros(n_of(d))
    place(mix, highpass(noise(0.03, 13), 1500) * decay(0.03, 0.007) * 1.5, 0)
    place(mix, ring(95, 0.2, 0.05, 0, 1.0), 0)
    at = 0.0
    for i in range(70):
        at += r.exponential(0.012 + i * 0.0003)
        f = r.uniform(2400, 11500)
        tau = r.uniform(0.008, 0.05)
        amp = r.uniform(0.2, 0.9) * max(0.12, 1 - at)
        tick = ring(f, 0.14, tau, 0, amp) + ring(f * r.uniform(1.4, 2.1), 0.14, tau * 0.6, 0, amp * 0.5)
        if at < d:
            place(mix, tick, at)
    return reverb(mix, 0.5, 0.2, tone=9000)


# ---------------------------------------------------------------- Death Steal
def dark_suck():
    """The black room: a long breath in, the world pulled away, a low thud as it closes."""
    d = 1.6
    pull = 1.2
    u = np.linspace(0, 1, n_of(pull))
    air = bandpass(noise(pull, 14), 260 * (3000 / 260) ** (u ** 2), 1.8) * u ** 1.8
    moan = sine_sweep(pull, 70, 210) * u * 0.35
    mix = np.zeros(n_of(d))
    place(mix, air + moan, 0)
    thud = sine_sweep(0.4, 90, 34) * decay(0.4, 0.11)
    place(mix, thud * 1.6, pull - 0.02)
    return reverb(mix, 0.8, 0.2, tone=2500)


def soul_punch():
    """Punching an icon: a body blow, and something thin and ghostly torn loose."""
    d = 0.95
    mix = np.zeros(n_of(d))
    body = sine_sweep(0.3, 150, 46) * decay(0.3, 0.07)
    place(mix, body * 1.5, 0)
    place(mix, lowpass(noise(0.05, 15), 2400) * decay(0.05, 0.012) * 1.3, 0)
    gd = 0.85
    u = np.linspace(0, 1, n_of(gd))
    wail = np.sin(2 * np.pi * np.cumsum(820 * (340 / 820) ** u * (1 + 0.012 * np.sin(2 * np.pi * 7 * u * gd))) / SR)
    breath = bandpass(noise(gd, 16), 2600 * (500 / 2600) ** u, 2.2) * 0.8
    place(mix, (wail * 0.35 + breath) * env(gd, [(0, 0), (0.1, 1), (1, 0)]) * 0.6, 0.07)
    return reverb(mix, 0.8, 0.28)


# ---------------------------------------------------------------- Necromancy
def raise_rumble():
    """The ground gives them back: a low rumble, dirt crumbling, and something moaning under it."""
    d = 1.9
    rumble = lowpass(lowpass(noise(d, 17), 150), 110) * 5
    slow = 1 + 0.5 * lowpass(rng(18).uniform(-1, 1, n_of(d)), 5) * 18
    r = rng(19)
    dirt = np.zeros(n_of(d))
    for _ in range(46):
        grain = bandpass(noise(0.05, int(r.integers(1, 9999))), r.uniform(500, 2100), 1.5) * decay(0.05, 0.012)
        place(dirt, grain * r.uniform(0.2, 0.7), r.uniform(0.25, 1.6))
    u = np.linspace(0, 1, n_of(d))
    moan = lowpass(saw(66 * (1 + 0.28 * u) * (1 + 0.012 * np.sin(2 * np.pi * 4.5 * u * d)), d, 8), 500)
    mix = rumble * slow * 0.8 + dirt * 0.7 + moan * 0.28
    return soft_clip(mix * env(d, [(0, 0), (0.25, 1), (0.7, 0.8), (1, 0)]), 1.4)


# ---------------------------------------------------------------- Game Player
def _bleep(f0, f1, seconds, bright=0.25):
    tone = sine_sweep(seconds, f0, f1)
    tone = tone + bright * np.sign(tone) * 0.4
    return lowpass(tone, 5200) * env(seconds, [(0, 0), (0.08, 1), (0.6, 0.7), (1, 0)])


def holo_open():
    """The window opening: two rising bleeps and a soft electric hiss."""
    d = 0.26
    mix = np.zeros(n_of(d))
    place(mix, _bleep(760, 980, 0.07), 0)
    place(mix, _bleep(1180, 1560, 0.1), 0.065)
    place(mix, highpass(noise(d, 21), 3500) * env(d, [(0, 0), (0.3, 0.14), (1, 0)]), 0)
    return reverb(mix, 0.2, 0.15, tone=8000)


def holo_close():
    """The window closing: the same, falling."""
    d = 0.22
    mix = np.zeros(n_of(d))
    place(mix, _bleep(1320, 1080, 0.06), 0)
    place(mix, _bleep(820, 560, 0.09), 0.055)
    place(mix, highpass(noise(d, 22), 3500) * env(d, [(0, 0.12), (1, 0)]), 0)
    return reverb(mix, 0.18, 0.12, tone=8000)


def holo_ping():
    """A notification: two soft notes that ring out, clean as glass."""
    d = 0.75
    mix = np.zeros(n_of(d))
    for at, f in ((0.0, 1174.7), (0.09, 1760.0)):
        note = ring(f, 0.6, 0.16) + ring(f * 2.01, 0.6, 0.06, 0, 0.25) + ring(f * 3.03, 0.6, 0.03, 0, 0.1)
        place(mix, note, at)
    return reverb(mix, 0.5, 0.25, tone=8000)


def gate_open():
    """A gate tearing open: deep hum climbing, air ripping, and a thin shimmer on top."""
    d = 2.3
    u = np.linspace(0, 1, n_of(d))
    hum = lowpass(saw(48 * (1 + u ** 1.5) * (1 + 0.006 * np.sin(2 * np.pi * 6 * u * d)), d, 12), 300 + 1600 * u)
    rip = bandpass(noise(d, 23), 240 * (4800 / 240) ** u, 1.5) * (0.3 + 0.7 * np.abs(np.sin(2 * np.pi * 9 * u ** 1.4 * d))) * u
    r = rng(24)
    shine = np.zeros(n_of(d))
    for _ in range(24):
        place(shine, ring(r.uniform(3000, 8000), 0.5, r.uniform(0.05, 0.18), 0, r.uniform(0.2, 0.5)), r.uniform(0.7, 1.9))
    mix = hum * 0.5 + rip * 0.8 + shine * 0.3
    return reverb(soft_clip(mix * env(d, [(0, 0), (0.12, 0.45), (0.8, 1), (1, 0)]), 1.3), 0.9, 0.25)


def level_up():
    """Something earned: a quick climbing run of notes, the last one left ringing, a few sparks over it."""
    d = 1.15
    notes = (523.25, 659.25, 783.99, 1046.5, 1318.5)
    mix = np.zeros(n_of(d))
    for i, f in enumerate(notes):
        tau = 0.1 if i < 4 else 0.38
        length = 0.3 if i < 4 else 0.85
        tone = ring(f, length, tau) + ring(f * 2, length, tau * 0.6, 0, 0.35) + ring(f * 3, length, tau * 0.3, 0, 0.16)
        place(mix, tone, i * 0.07)
    r = rng(25)
    for _ in range(14):
        place(mix, ring(r.uniform(4000, 9000), 0.25, 0.03, 0, r.uniform(0.1, 0.25)), 0.28 + r.uniform(0, 0.5))
    return reverb(mix, 0.7, 0.25, tone=8000)


# ---------------------------------------------------------------- Mob Control
def _chirp(f0, f1, seconds, warble=38, depth=0.05):
    u = np.linspace(0, 1, n_of(seconds))
    f = f0 * (f1 / f0) ** u * (1 + depth * np.sin(2 * np.pi * warble * u * seconds))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return (np.sin(ph) + 0.25 * np.sin(2 * ph + 0.6)) * env(seconds, [(0, 0), (0.12, 1), (0.7, 0.8), (1, 0)])


def claim_chirp():
    """A mob taking to you: a little questioning bird-call, up and up again."""
    d = 0.42
    mix = np.zeros(n_of(d))
    place(mix, _chirp(1000, 1500, 0.12), 0)
    place(mix, _chirp(1300, 2300, 0.2), 0.13)
    return reverb(mix, 0.25, 0.14, tone=7000)


def order_chirp():
    """An order given: two sharp downward chirps."""
    d = 0.26
    mix = np.zeros(n_of(d))
    place(mix, _chirp(1900, 1250, 0.08, 60, 0.03), 0)
    place(mix, _chirp(1600, 1000, 0.09, 60, 0.03), 0.1)
    return reverb(mix, 0.18, 0.12, tone=7000)


# ---------------------------------------------------------------- Superior Authority
def command_boom():
    """A word nobody can disobey: a huge low stab that shakes the room."""
    d = 1.9
    sub = sine_sweep(d, 78, 33) * decay(d, 0.42)
    brass = lowpass(saw(55 * np.ones(n_of(d)), d, 24) + 0.6 * saw(82.5 * np.ones(n_of(d)), d, 16), 200 + 2800 * decay(d, 0.16)) * decay(d, 0.3)
    hit = lowpass(noise(d, 26), 1500) * decay(d, 0.035)
    return reverb(soft_clip(sub * 1.4 + brass * 0.5 + hit * 1.1, 2.0), 1.2, 0.32, tone=2800)


# ---------------------------------------------------------------- Future Sight
def whisper_shimmer():
    """Being told something: breath shaped like speech you can't quite catch, with a thin glitter over it."""
    d = 1.3
    base = noise(d, 27)
    u = np.linspace(0, 1, n_of(d))
    r = rng(28)
    syll = np.interp(u, np.linspace(0, 1, 9), r.uniform(0.05, 1.0, 9)) * (0.6 + 0.4 * np.sin(2 * np.pi * 7.3 * u * d))
    f1 = np.interp(u, np.linspace(0, 1, 7), r.uniform(450, 950, 7))
    f2 = np.interp(u, np.linspace(0, 1, 7), r.uniform(1300, 2500, 7))
    voice = bandpass(base, f1, 5) + bandpass(base, f2, 6) * 0.9 + bandpass(base, 3100, 7) * 0.4
    shine = np.zeros(n_of(d))
    for _ in range(16):
        place(shine, ring(r.uniform(4200, 9000), 0.5, r.uniform(0.06, 0.2), 0, r.uniform(0.1, 0.3)), r.uniform(0, 0.85))
    mix = voice * syll * env(d, [(0, 0), (0.1, 1), (0.75, 0.8), (1, 0)]) + shine * 0.5
    return reverb(mix, 0.8, 0.35, tone=7000)


# ---------------------------------------------------------------- Vocal Expression
def word_thunk():
    """A word landing: a heavy hollow block set down."""
    d = 0.34
    body = sine_sweep(d, 210, 92) * decay(d, 0.05)
    box = ring(310, d, 0.035, 0, 0.5) + ring(470, d, 0.022, 0, 0.3)
    hit = lowpass(noise(d, 29), 2800) * decay(d, 0.008)
    grit = bandpass(noise(d, 30), 1200, 1.2) * decay(d, 0.025) * 0.4
    return reverb(body * 1.2 + box + hit + grit, 0.25, 0.14, tone=3000)


# ---------------------------------------------------------------- The Miner
def rock_crunch():
    """A vein's worth of rock giving way: crunching gravel over a deep thump."""
    d = 1.25
    r = rng(31)
    mix = np.zeros(n_of(d))
    at = 0.0
    while at < d - 0.3:
        at += r.exponential(0.026)
        dur = r.uniform(0.02, 0.09)
        grain = bandpass(noise(dur, int(r.integers(1, 99999))), r.uniform(240, 1900), r.uniform(0.9, 2.2)) * decay(dur, dur / 3)
        place(mix, grain * r.uniform(0.3, 1.0) * max(0.25, 1 - at), at)
    place(mix, sine_sweep(0.5, 95, 38) * decay(0.5, 0.13) * 1.6, 0)
    return soft_clip(reverb(mix, 0.4, 0.18, tone=2600), 1.5)


# ---------------------------------------------------------------- Life Enjoyer
def ultimine_sweep():
    """Ultimine going through: a fast sweep of air with a pickaxe's quick clinks along it."""
    d = 0.62
    u = np.linspace(0, 1, n_of(d))
    air = bandpass(noise(d, 32), 520 * (4800 / 520) ** u, 1.5) * env(d, [(0, 0), (0.5, 1), (1, 0)])
    mix = air * 0.55
    at = 0.0
    gap = 0.11
    for i in range(7):
        tick = ring(3000 + i * 300, 0.1, 0.012, 0, 0.55) + ring(4500 + i * 420, 0.1, 0.008, 0, 0.3)
        place(mix, tick, at)
        at += gap
        gap *= 0.82
    return reverb(mix, 0.3, 0.15, tone=7000)


# ---------------------------------------------------------------- Book Keeping
def _flutter(seconds, seed, rate=26.0, f0=2600.0, f1=5200.0):
    u = np.linspace(0, 1, n_of(seconds))
    paper = bandpass(noise(seconds, seed), f0 * (f1 / f0) ** u, 0.9)
    return paper * (0.3 + 0.7 * np.abs(np.sin(2 * np.pi * rate * u ** 0.8 * seconds)))


def page_flip():
    """One page turned: a dry paper flap."""
    d = 0.32
    flap = _flutter(d, 33) * env(d, [(0, 0), (0.15, 1), (0.5, 0.6), (1, 0)])
    thud = lowpass(noise(d, 34), 700) * decay(d, 0.02, 0.2) * 0.8
    return flap + thud


def ink_scratch():
    """A page being written: three scratches of a nib, then a tap."""
    d = 0.7
    mix = np.zeros(n_of(d))
    for at, dur, f in ((0.0, 0.16, 3500), (0.2, 0.11, 4400), (0.34, 0.2, 3900)):
        stroke = bandpass(noise(dur, int(f)), f, 3.5)
        stroke = stroke * (0.45 + 0.55 * np.sin(2 * np.pi * 62 * times(dur)) ** 2) * env(dur, [(0, 0), (0.15, 1), (0.8, 0.8), (1, 0)])
        place(mix, stroke, at)
    place(mix, ring(1900, 0.06, 0.006, 0, 0.8) + lowpass(noise(0.06, 35), 3000) * decay(0.06, 0.004), 0.59)
    return reverb(mix, 0.15, 0.08, tone=6000)


def pages_rewind():
    """Time pulled back to a page: pages flying over faster and faster, stopped by the cover."""
    d = 1.3
    mix = np.zeros(n_of(d))
    at = 0.0
    gap = 0.16
    i = 0
    while at < 1.0:
        flip = _flutter(0.14, 40 + i, 34) * env(0.14, [(0, 0), (0.2, 1), (1, 0)])
        place(mix, flip * (0.4 + 0.6 * at), at)
        at += gap
        gap = max(0.028, gap * 0.87)
        i += 1
    place(mix, lowpass(noise(0.1, 90), 900) * decay(0.1, 0.022) * 1.4 + ring(180, 0.1, 0.03, 0, 0.8), 1.06)
    return reverb(mix, 0.5, 0.2, tone=5000)


# ---------------------------------------------------------------- Teacher
def chalk_tap():
    """Chalk on a board: tap tap, and one short stroke (not a squeal, nobody wants that)."""
    d = 0.55
    mix = np.zeros(n_of(d))
    for at in (0.0, 0.13):
        place(mix, ring(1500, 0.05, 0.006, 0, 0.8) + ring(620, 0.05, 0.01, 0, 0.5) + lowpass(noise(0.05, 37), 2600) * decay(0.05, 0.004), at)
    dur = 0.22
    stroke = bandpass(noise(dur, 38), 2300, 1.2) * (0.5 + 0.5 * np.sin(2 * np.pi * 95 * times(dur)) ** 2) * env(dur, [(0, 0), (0.1, 1), (0.8, 0.7), (1, 0)])
    place(mix, stroke * 0.55, 0.28)
    return reverb(mix, 0.3, 0.2, tone=4500)


# name: (maker, shortest and longest allowed in seconds)
NAMES = {
    'time_slow': time_slow, 'time_tick': time_tick, 'time_stop': time_stop, 'time_rush': time_rush,
    'mirror_shimmer': mirror_shimmer, 'glass_shatter': glass_shatter,
    'dark_suck': dark_suck, 'soul_punch': soul_punch,
    'raise_rumble': raise_rumble,
    'holo_open': holo_open, 'holo_close': holo_close, 'holo_ping': holo_ping, 'gate_open': gate_open, 'level_up': level_up,
    'claim_chirp': claim_chirp, 'order_chirp': order_chirp,
    'command_boom': command_boom,
    'whisper_shimmer': whisper_shimmer,
    'word_thunk': word_thunk,
    'rock_crunch': rock_crunch,
    'ultimine_sweep': ultimine_sweep,
    'page_flip': page_flip, 'ink_scratch': ink_scratch, 'pages_rewind': pages_rewind,
    'chalk_tap': chalk_tap,
}


def trim(x):
    """Cuts off the silent end the reverb leaves."""
    level = np.abs(x)
    limit = level.max() * 10 ** (-58 / 20)
    last = np.nonzero(level > limit)[0][-1]
    return x[:min(len(x), last + n_of(0.03))]


def check(path):
    data, sr = soundfile.read(path)
    problems = []
    secs = len(data) / sr
    peak = np.abs(data).max()
    peak_db = 20 * np.log10(peak) if peak else -999
    rms = np.sqrt((data ** 2).mean())
    rms_db = 20 * np.log10(rms) if rms else -999
    if data.ndim != 1:
        problems.append('not mono')
    if sr != SR:
        problems.append(f'rate {sr}')
    if peak_db > -3.0:
        problems.append(f'peak {peak_db:.1f} dB')
    if (np.abs(data) > 0.999).any():
        problems.append('clipped')
    if rms_db < -45:
        problems.append('nearly silent')
    if secs < 0.1 or secs > 3.5:
        problems.append(f'length {secs:.2f} s')
    if abs(data.mean()) > 0.02:
        problems.append('off centre')
    return secs, peak_db, rms_db, problems


def main():
    os.makedirs(OUT, exist_ok=True)
    only = set(sys.argv[1:])
    bad = 0
    for name, maker in NAMES.items():
        path = os.path.join(OUT, name + '.ogg')
        if not only or name in only:
            x = trim(maker())
            soundfile.write(path, finish(x), SR, format='OGG', subtype='VORBIS')
        secs, peak, rms, problems = check(path)
        bad += bool(problems)
        print(f'{name:18} {secs:5.2f} s  peak {peak:6.1f} dB  loudness {rms:6.1f} dB  {os.path.getsize(path) // 1024:3} KB  {"; ".join(problems)}')
    print('problems:', bad)
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
