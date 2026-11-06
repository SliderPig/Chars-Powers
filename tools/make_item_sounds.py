# Sounds for Char's new weapons and for Solgo. All made here from nothing (noise and sine waves), no samples. From the charpowers folder:  python tools/make_item_sounds.py
import json
import os

import numpy as np
import soundfile as sf

SR = 44100
OUT = 'src/main/resources/assets/charpowers/sounds/items/'
rng = np.random.default_rng(1234)


def t(seconds):
    return np.arange(int(seconds * SR)) / SR


def lowpass(x, cutoff):
    """Simple one-pole; cutoff can be a number or an array the same length as x."""
    c = np.broadcast_to(cutoff, x.shape)
    a = 1 - np.exp(-2 * np.pi * c / SR)
    y = np.zeros_like(x)
    v = 0.0
    for i in range(len(x)):
        v += a[i] * (x[i] - v)
        y[i] = v
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, lo, hi):
    return highpass(lowpass(x, hi), lo)


def noise(seconds):
    return rng.standard_normal(int(seconds * SR))


def env(n, attack, decay_power=1.0):
    u = np.linspace(0, 1, n)
    a = np.clip(u / max(attack, 1e-4), 0, 1)
    return a * (1 - u) ** decay_power


def tone(freq, seconds, phase=0.0):
    f = np.broadcast_to(freq, (int(seconds * SR),))
    return np.sin(2 * np.pi * np.cumsum(f) / SR + phase)


def norm(x, peak=0.85):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def write(name, x, peak=0.85):
    fade = np.minimum(1, (len(x) - np.arange(len(x))) / (0.01 * SR))
    sf.write(OUT + name + '.ogg', norm(x * fade, peak).astype(np.float32), SR, format='OGG', subtype='VORBIS')


def whoosh(seconds, lo, hi, attack=0.25, power=1.5):
    n = int(seconds * SR)
    cut = lo + (hi - lo) * np.sin(np.pi * np.linspace(0, 1, n)) ** 2
    return lowpass(noise(seconds), cut) * env(n, attack, power)


def ring(freqs, seconds, decay=4.0):
    out = np.zeros(int(seconds * SR))
    for i, f in enumerate(freqs):
        out += tone(f, seconds) * np.exp(-t(seconds) * decay * (1 + i * 0.35)) / (1 + i * 0.6)
    return out


def spray(hiss_lo, hiss_hi, seconds=0.16):
    n = int(seconds * SR)
    x = bandpass(noise(seconds), hiss_lo, hiss_hi) * env(n, 0.08, 1.2)
    click = np.zeros(n)
    c = int(0.008 * SR)
    click[:c] = noise(0.008)[:c] * np.linspace(1, 0, c) * 0.6
    return x + click


def make_weapons():
    write('spray_water', spray(2500, 9000), 0.6)
    lava = spray(800, 3500, 0.22)
    crackle = np.zeros_like(lava)
    for _ in range(9):
        at = rng.integers(0, len(lava) - 500)
        crackle[at:at + 400] += noise(0.01)[:400] * np.linspace(1, 0, 400)
    write('spray_lava', lava + crackle * 0.8, 0.6)
    write('spray_milk', lowpass(spray(1500, 6000), 4500), 0.55)
    # a slosh and a glug
    n = t(0.35)
    glug = tone(180 + 240 * n, 0.35) * np.exp(-n * 8) * (0.5 + 0.5 * np.sign(np.sin(n * 60)))
    write('spray_fill', lowpass(glug + bandpass(noise(0.35), 400, 2200) * env(len(n), 0.1, 2) * 0.5, 3000))

    n = t(0.35)
    shimmer = sum(tone(f * (1 + 0.15 * n), 0.35) / (i + 1) for i, f in enumerate((1760, 2640, 3520)))
    write('ice_cast', shimmer * np.exp(-n * 9) * 0.5 + whoosh(0.35, 1200, 6000, 0.08, 2) * 0.8)
    # glass-like breaking
    x = np.zeros(int(0.45 * SR))
    for _ in range(26):
        at = int(rng.integers(0, int(0.2 * SR)))
        f = rng.uniform(2200, 7000)
        d = int(rng.uniform(0.02, 0.12) * SR)
        x[at:at + d] += tone(f, (d + 0.5) / SR) * np.exp(-np.linspace(0, 6, d)) * rng.uniform(0.2, 1)
    write('ice_shatter', x + highpass(noise(0.45), 3500) * np.exp(-t(0.45) * 14) * 0.5)

    # a big dull blow, and rock thrown up
    n = t(0.9)
    thump = tone(95 * np.exp(-n * 3.5) + 28, 0.9) * np.exp(-n * 5)
    grit = lowpass(noise(0.9), 1800) * np.exp(-n * 7)
    clang = ring((210, 487, 803), 0.9, 9) * 0.25
    write('hammer_slam', thump * 1.3 + grit * 0.7 + clang, 0.95)
    # flames rushing outward
    n = t(0.95)
    roar = lowpass(noise(0.95), 500 + 2600 * np.exp(-n * 2)) * env(len(n), 0.05, 1.2)
    fizz = np.zeros(len(n))
    for _ in range(50):
        at = rng.integers(0, len(n) - 400)
        fizz[at:at + 300] += noise(0.01)[:300] * np.linspace(1, 0, 300) * rng.uniform(0.2, 1)
    write('flame_burst', roar + fizz * 0.5)

    # stone letting go and a bright ring
    n = t(1.8)
    grind = lowpass(noise(1.8), 1200) * np.clip(1 - n / 0.5, 0, 1) ** 1.2
    write('sword_pull', grind * 0.8 + ring((1046.5, 1568, 2637, 3136), 1.8, 2.2) * np.clip((n - 0.3) * 18, 0, 1) * 0.7)
    n = t(2.2)
    write('sword_ring', ring((1318.5, 1976, 2637), 2.2, 2.0) * np.clip(n * 12, 0, 1), 0.45)

    # the long sword: a clean cut of air with a bit of metal in it
    write('gold_swing', whoosh(0.32, 700, 5200, 0.3, 1.2) + ring((2093, 3136), 0.32, 12) * 0.08)
    n = t(1.1)
    boom = tone(70 * np.exp(-n * 3) + 34, 1.1) * np.exp(-n * 4.5)
    chime = ring((783.99, 1174.66, 1567.98, 2349.32), 1.1, 3) * 0.3
    write('gold_slam', boom * 1.3 + chime + lowpass(noise(1.1), 2500) * np.exp(-n * 9) * 0.5, 0.95)
    n = t(0.5)
    write('gold_thrust', whoosh(0.5, 900, 8000, 0.45, 1.3) + ring((1567.98, 2349.32), 0.5, 7) * 0.2 * np.clip((n - 0.18) * 30, 0, 1))
    write('gold_beat', ring((1567.98, 2093), 0.5, 7), 0.5)

    # the katana: thin, high and quick, with a hollow note under it
    n = t(0.38)
    write('katana_swing', whoosh(0.38, 1500, 9000, 0.25, 1.4) + tone(260 + 160 * n, 0.38) * env(len(n), 0.3, 2) * 0.2)
    # the soul cut: a strange backwards-swelling hollow sound, then a thin glassy ping
    n = t(0.75)
    swell = tone(220 * (1 + 0.5 * np.sin(n * 18)), 0.75) * np.clip(n / 0.08, 0, 1) * np.exp(-n * 6)
    high = ring((1480, 2217, 2960), 0.75, 7) * 0.35
    write('soul_cut', swell + high + bandpass(noise(0.75), 400, 2000) * np.exp(-n * 11) * 0.4)
    write('dagger_swing', whoosh(0.2, 1800, 7000, 0.3, 1.2), 0.6)
    write('fork_ting', ring((3136, 4186, 5274), 0.4, 14), 0.5)


def tremolo(n, rate, depth=0.5):
    return 1 - depth + depth * np.sin(n * rate * 2 * np.pi)


def wrong_voice(seconds, base, seed):
    """Whisper-like: filtered noise with drifting vowel bands. Pitch-wrong on purpose, no words."""
    r = np.random.default_rng(seed)
    n = t(seconds)
    src = noise(seconds)
    out = np.zeros_like(src)
    for f0, gain in ((base, 1.0), (base * 2.3, 0.7), (base * 3.9, 0.35)):
        wob = 1 + 0.18 * np.sin(n * r.uniform(1.3, 2.7) * 2 * np.pi + r.uniform(0, 6)) + 0.12 * np.sin(n * r.uniform(5, 9) * 2 * np.pi)
        out += bandpass(src, f0 * wob * 0.8, f0 * wob * 1.3) * gain
    syll = np.clip(np.sin(n * r.uniform(2.1, 3.3) * 2 * np.pi + r.uniform(0, 6)) + 0.35 * np.sin(n * 7.1 * 2 * np.pi), 0, 1)
    return out * syll * env(len(n), 0.12, 0.5)


def make_solgo():
    # a long slow breath in and out, too low
    n = t(3.2)
    breath = lowpass(noise(3.2), 240 + 260 * np.sin(np.pi * n / 3.2)) * (np.sin(np.pi * n / 3.2) ** 2)
    rattle = lowpass(noise(3.2), 120) * (0.5 + 0.5 * np.sign(np.sin(n * 2 * np.pi * 11))) * np.clip(np.sin(np.pi * (n - 1.6) / 1.6), 0, 1) ** 2
    write('solgo_breath', breath + rattle * 0.8, 0.6)

    write('solgo_whisper', wrong_voice(2.4, 620, 5) + wrong_voice(2.4, 1040, 9) * 0.7, 0.55)

    # the blade being drawn back: a rising metal creak
    n = t(0.9)
    sweep = tone(220 + 1100 * n ** 2, 0.9) * (0.4 + 0.6 * np.sign(np.sin(n * 2 * np.pi * (24 + 60 * n))))
    write('solgo_windup', lowpass(sweep, 2400) * np.clip(n / 0.3, 0, 1) * (1 - np.clip((n - 0.75) / 0.15, 0, 1)) + whoosh(0.9, 200, 900, 0.8, 0.5) * 0.7, 0.6)

    # the slash itself: huge and hard
    n = t(0.62)
    cut = bandpass(noise(0.62), 900, 11000) * np.clip(n / 0.03, 0, 1) * np.exp(-n * 7)
    body = tone(130 * np.exp(-n * 5) + 44, 0.62) * np.exp(-n * 6)
    write('solgo_slash', cut + body + ring((1760, 2489), 0.62, 10) * 0.15, 0.9)

    write('solgo_lunge', whoosh(0.6, 300, 5000, 0.2, 1) + tone(80, 0.6) * env(int(0.6 * SR), 0.1, 2) * 0.5, 0.8)

    # a scream made wrong: several voices sliding against each other
    n = t(1.5)
    voices = np.zeros(len(n))
    for i, f in enumerate((310, 433, 587, 911)):
        vib = 1 + 0.04 * np.sin(n * (6 + i * 1.7) * 2 * np.pi)
        slide = 1 + 0.5 * n ** 2 * (1 if i % 2 else -0.6)
        voices += np.tanh(tone(f * vib * slide, 1.5) * 2.2) / (1 + i * 0.4)
    write('solgo_scream', lowpass(voices, 5200) * np.clip(n / 0.1, 0, 1) * (1 - n / 1.5) ** 0.8 + highpass(noise(1.5), 3000) * (1 - n / 1.5) ** 2 * 0.25, 0.8)

    # a flicker: stuttering air
    n = t(0.45)
    write('solgo_flicker', bandpass(noise(0.45), 600, 4000) * (0.5 + 0.5 * np.sign(np.sin(n * 2 * np.pi * 34))) * env(len(n), 0.1, 1) + tone(1900 - 1500 * n, 0.45) * env(len(n), 0.05, 2) * 0.15, 0.6)

    n = t(0.5)
    write('solgo_hurt', wrong_voice(0.5, 480, 21) * 1.6 + lowpass(noise(0.5), 500) * np.exp(-n * 9), 0.6)

    # coming apart: the voices sink and thin out
    n = t(3.2)
    sink = np.zeros(len(n))
    for f in (210, 297, 340):
        sink += np.tanh(tone(f * np.exp(-n * 0.9), 3.2) * 1.8)
    write('solgo_death', lowpass(sink, 1800) * (1 - n / 3.2) ** 1.3 + wrong_voice(3.2, 700, 33) * 0.8, 0.85)


os.makedirs(OUT, exist_ok=True)
make_weapons()
make_solgo()

NAMES = {'spray_water': 'Bottle sprays', 'spray_lava': 'Bottle spits lava', 'spray_milk': 'Bottle sprays milk', 'spray_fill': 'Bottle fills', 'ice_cast': 'Ice shard flies', 'ice_shatter': 'Ice shatters',
         'hammer_slam': 'Hammer slams the ground', 'flame_burst': 'Flames burst out', 'sword_pull': 'Sword comes free', 'sword_ring': 'Sword hums', 'gold_swing': 'Long Sword swings', 'gold_slam': 'Long Sword shakes the ground',
         'gold_thrust': 'Long Sword thrusts', 'gold_beat': 'Long Sword rings', 'katana_swing': 'Katana cuts the air', 'soul_cut': 'A soul is cut', 'dagger_swing': 'Dagger whispers past', 'fork_ting': 'Fork goes ting',
         'solgo_breath': 'Something breathes', 'solgo_whisper': 'Whispers', 'solgo_windup': 'A long blade is drawn back', 'solgo_slash': 'Solgo slashes', 'solgo_lunge': 'Solgo lunges', 'solgo_scream': 'Solgo screams', 'solgo_flicker': 'Solgo flickers',
         'solgo_hurt': 'Solgo hurts', 'solgo_death': 'Solgo comes apart'}
path = 'src/main/resources/assets/charpowers/sounds.json'
data = json.load(open(path, encoding='utf-8'))
for name in NAMES:
    data[name] = {'sounds': [{'name': f'charpowers:items/{name}'}], 'subtitle': f'sounds.charpowers.{name}'}
with open(path, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
    f.write('\n')
lang_path = 'src/main/resources/assets/charpowers/lang/en_us.json'
lang = json.load(open(lang_path, encoding='utf-8'))
for name, title in NAMES.items():
    lang[f'sounds.charpowers.{name}'] = title
with open(lang_path, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(lang, f, indent=2, ensure_ascii=False)
    f.write('\n')
print('item sounds made')
