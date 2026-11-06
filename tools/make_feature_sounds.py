# Makes the sounds for Raise Shadow from nothing (no recordings): a deep rising rumble with a hollow whisper, and a dull
# collapse for when a shadow fails. Run from the charpowers folder:  python tools/make_feature_sounds.py
import json
import os
import re

import numpy as np
import soundfile as sf

RATE = 44100
OUT = 'src/main/resources/assets/charpowers/'
rng = np.random.default_rng(11)


def t_of(sec):
    return np.arange(int(sec * RATE)) / RATE


def band(x, lo, hi):
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / RATE)
    gain = np.clip((f - lo) / max(1, lo * 0.3), 0, 1) * np.clip((hi - f) / max(1, hi * 0.3), 0, 1)
    return np.fft.irfft(spec * gain, len(x))


def finish(x, peak=0.8):
    x = np.tanh(x * 1.1)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    n = int(0.01 * RATE)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x.astype(np.float32)


def shadow_rise():
    t = t_of(2.2)
    swell = np.sin(np.pi * np.clip(t / 2.2, 0, 1)) ** 1.5
    # a low note that climbs a fifth, with a slightly flat twin so it beats slowly
    f = 52 * (1 + 0.5 * (t / 2.2) ** 1.4)
    ph = 2 * np.pi * np.cumsum(f) / RATE
    tone = np.sin(ph) + 0.6 * np.sin(ph * 1.005 + 0.4) + 0.3 * np.sin(2 * ph)
    # breathy whisper rising in pitch
    whisper = band(rng.standard_normal(len(t)), 600, 3000) * (0.05 + 0.25 * (t / 2.2) ** 2)
    # a heartbeat thud at the start, like something heavy getting up
    thud = np.sin(2 * np.pi * 46 * t) * np.exp(-t * 7) * 1.2
    thud2 = np.sin(2 * np.pi * 40 * np.clip(t - 0.35, 0, None)) * np.exp(-np.clip(t - 0.35, 0, None) * 8) * (t > 0.35) * 0.9
    return finish(tone * swell * 0.7 + whisper * swell + thud + thud2)


def shadow_fail():
    t = t_of(1.1)
    f = 90 * np.exp(-t * 1.6)
    ph = 2 * np.pi * np.cumsum(f) / RATE
    body = np.sin(ph) * np.exp(-t * 3.5)
    dust = band(rng.standard_normal(len(t)), 200, 1800) * np.exp(-t * 5) * 0.4
    return finish(body + dust, 0.7)


FILES = {'shadow_rise': shadow_rise(), 'shadow_fail': shadow_fail()}
os.makedirs(OUT + 'sounds', exist_ok=True)
for name, data in FILES.items():
    sf.write(OUT + f'sounds/{name}.ogg', data, RATE, format='OGG', subtype='VORBIS')

EVENTS = {'shadow_rise': ('shadow_rise',), 'shadow_fail': ('shadow_fail',)}
SUBTITLES = {'shadow_rise': 'A shadow stands up', 'shadow_fail': 'A shadow crumbles'}


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
print('feature sounds made')
