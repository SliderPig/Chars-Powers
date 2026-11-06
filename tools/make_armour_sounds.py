# Makes the Invincible Armour sounds from nothing (no recordings): heavy metal footsteps and landings, a soft servo whirr,
# a thruster roar for the flight and a clank for each piece. Run from the charpowers folder:  python tools/make_armour_sounds.py
import json
import os
import re

import numpy as np
import soundfile as sf

RATE = 44100
OUT = 'src/main/resources/assets/charpowers/'
rng = np.random.default_rng(7)


def t_of(sec):
    return np.arange(int(sec * RATE)) / RATE


def band(x, lo, hi):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / RATE)
    gain = np.clip((f - lo) / max(1, lo * 0.3), 0, 1) * np.clip((hi - f) / max(1, hi * 0.3), 0, 1)
    return np.fft.irfft(spec * gain, len(x))


def noise(n):
    return rng.standard_normal(int(n))


def ring(t, freqs, decays, amps):
    out = np.zeros_like(t)
    for f, d, a in zip(freqs, decays, amps):
        out += a * np.sin(2 * np.pi * f * t) * np.exp(-t * d)
    return out


def finish(x, peak=0.8):
    x = np.tanh(x * 1.2)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    n = int(0.004 * RATE)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x.astype(np.float32)


def clunk(heavy=1.0, seed=0.0):
    t = t_of(0.55 if heavy > 1 else 0.38)
    base = 1 + 0.12 * seed
    thud = np.sin(2 * np.pi * (62 * base + 40 * np.exp(-t * 30)) * t) * np.exp(-t * (16 / heavy))
    hit = band(noise(len(t)), 300, 3500) * np.exp(-t * 90) * 1.4
    metal = ring(t, [380 * base, 910 * base, 1520 * base, 2330 * base], [26, 34, 48, 70], [0.5, 0.4, 0.3, 0.18])
    x = thud * 1.3 * heavy + hit + metal * heavy
    # a second, smaller clack: the toe coming down
    gap = int((0.05 + 0.01 * seed) * RATE)
    second = np.zeros_like(x)
    second[gap:] = (band(noise(len(t) - gap), 400, 3000) * np.exp(-t[:len(t) - gap] * 110) * 0.9 + ring(t[:len(t) - gap], [520 * base, 1240 * base], [40, 60], [0.35, 0.2]))
    return finish(x + second * 0.8)


def land(seed=0.0):
    x = clunk(1.7, seed)
    t = t_of(len(x) / RATE)
    rattle = band(noise(len(t)), 1500, 6000) * np.exp(-t * 14) * (np.sin(2 * np.pi * 38 * t) > 0) * 0.35
    return finish(x + rattle)


def servo(seed=0.0):
    t = t_of(0.4)
    sweep = 260 + 380 * np.sin(np.clip(t / 0.4, 0, 1) * np.pi / 2) + 30 * seed
    phase = 2 * np.pi * np.cumsum(sweep) / RATE
    tone = np.sin(phase) + 0.4 * np.sin(2 * phase) + 0.2 * np.sin(3 * phase)
    grit = band(noise(len(t)), 900, 3200) * 0.25
    env = np.sin(np.clip(t / 0.4, 0, 1) * np.pi) ** 1.5
    return finish((tone * 0.5 + grit) * env, 0.45)


def thruster():
    sec = 1.25
    t = t_of(sec)
    roar = band(noise(len(t)), 120, 2200) * 1.0
    hiss = band(noise(len(t)), 2500, 9000) * 0.3
    rumble = np.sin(2 * np.pi * 55 * t) * 0.5 + np.sin(2 * np.pi * 83 * t) * 0.3
    wobble = 1 + 0.12 * np.sin(2 * np.pi * 9 * t)
    env = np.minimum(np.minimum(t / 0.15, (sec - t) / 0.2), 1.0)
    return finish((roar + hiss + rumble) * wobble * env, 0.7)


def clank(seed=0.0):
    t = t_of(0.6)
    base = 1 + 0.1 * seed
    metal = ring(t, [310 * base, 740 * base, 1180 * base, 1910 * base, 2750 * base], [10, 14, 22, 30, 45], [0.6, 0.5, 0.4, 0.3, 0.2])
    hit = band(noise(len(t)), 400, 5000) * np.exp(-t * 70) * 1.2
    thump = np.sin(2 * np.pi * 90 * t) * np.exp(-t * 25) * 0.8
    return finish(metal + hit + thump)


FILES = {}
for i, s in enumerate((-1.0, 0.0, 1.0), 1):
    FILES[f'suit_step_{i}'] = clunk(1.0, s)
for i, s in enumerate((-0.5, 0.6), 1):
    FILES[f'suit_land_{i}'] = land(s)
    FILES[f'suit_servo_{i}'] = servo(s)
    FILES[f'suit_clank_{i}'] = clank(s)
FILES['suit_thruster'] = thruster()

os.makedirs(OUT + 'sounds', exist_ok=True)
for name, data in FILES.items():
    sf.write(OUT + f'sounds/{name}.ogg', data, RATE, format='OGG', subtype='VORBIS')

EVENTS = {'suit_step': ('suit_step_1', 'suit_step_2', 'suit_step_3'), 'suit_land': ('suit_land_1', 'suit_land_2'),
          'suit_servo': ('suit_servo_1', 'suit_servo_2'), 'suit_clank': ('suit_clank_1', 'suit_clank_2'), 'suit_thruster': ('suit_thruster',)}
SUBTITLES = {'suit_step': 'Armour clunks', 'suit_land': 'Armour crashes down', 'suit_servo': 'Servos whirr', 'suit_clank': 'Armour clanks', 'suit_thruster': 'Thrusters roar'}


def add_entries(path, entries):
    """Adds lines before the last } and replaces ones this script wrote before, keeping the rest of the file as it was."""
    with open(path, encoding='utf-8', newline='') as f:
        text = f.read()
    eol = chr(13) + chr(10) if chr(13) in text else chr(10)
    text = text.replace(chr(13) + chr(10), chr(10))
    for key in entries:
        text = re.sub(r',\s*"%s"\s*:\s*(\{[^{}]*(\{[^{}]*\}[^{}]*)*\}|"[^"]*")' % re.escape(key), '', text)
    body = text.rstrip()
    assert body.endswith('}')
    body = body[:-1].rstrip()
    nl = chr(10)
    add = ''.join(',' + nl + '  ' + json.dumps(k) + ': ' + (json.dumps(v, indent=2).replace(nl, nl + '  ') if isinstance(v, dict) else json.dumps(v)) for k, v in entries.items())
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write((body + add + nl + '}' + nl).replace(nl, eol))


add_entries(OUT + 'sounds.json', {k: {'sounds': [{'name': 'charpowers:' + v} for v in vs], 'subtitle': 'sounds.charpowers.' + k} for k, vs in EVENTS.items()})
add_entries(OUT + 'lang/en_us.json', {'sounds.charpowers.' + k: v for k, v in SUBTITLES.items()})
print('armour sounds made')
