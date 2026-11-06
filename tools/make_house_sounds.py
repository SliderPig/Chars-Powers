# House Builder's two sounds, made from scratch: a soft wooden hammer tap, and a small finished flourish (three taps and a warm chord).
# Run from the charpowers folder:  python tools/make_house_sounds.py   (numpy + soundfile)
import json
import os

import numpy as np
import soundfile as sf

RATE = 44100
OUT = 'src/main/resources/assets/charpowers/sounds'
LIST = 'src/main/resources/assets/charpowers/sounds.json'


def tap(pitch=1.0, seed=1, length=0.14):
    # a knock: a short wooden resonance plus a tiny click of noise
    rng = np.random.default_rng(seed)
    t = np.arange(int(RATE * length)) / RATE
    tone = np.sin(2 * np.pi * 420 * pitch * t) * np.exp(-t * 46)
    tone += 0.5 * np.sin(2 * np.pi * 980 * pitch * t) * np.exp(-t * 70)
    tone += 0.25 * np.sin(2 * np.pi * 170 * pitch * t) * np.exp(-t * 30)
    click = rng.normal(0, 1, len(t)) * np.exp(-t * 400) * 0.35
    return tone + click


def chord(freqs, length=0.9):
    t = np.arange(int(RATE * length)) / RATE
    out = np.zeros(len(t))
    for i, f in enumerate(freqs):
        # plucked: each note starts a touch later and dies away
        start = int(RATE * 0.03 * i)
        env = np.exp(-(t[:-start or None]) * 4.2)
        note = np.sin(2 * np.pi * f * t[:-start or None]) + 0.3 * np.sin(4 * np.pi * f * t[:-start or None])
        out[start:] += note * env / len(freqs)
    return out


def mix(parts, total):
    out = np.zeros(int(RATE * total))
    for when, sound, gain in parts:
        start = int(RATE * when)
        end = min(len(out), start + len(sound))
        out[start:end] += sound[:end - start] * gain
    return out


def save(name, samples, peak):
    samples = samples / max(1e-6, np.max(np.abs(samples))) * peak
    fade = int(RATE * 0.01)
    samples[-fade:] *= np.linspace(1, 0, fade)
    sf.write(os.path.join(OUT, name + '.ogg'), samples, RATE, format='OGG', subtype='VORBIS')


def main():
    os.makedirs(OUT, exist_ok=True)
    save('house_tap', mix([(0, tap(1.0, 1), 1.0)], 0.2), 0.5)
    flourish = mix([(0.0, tap(0.9, 2), 0.7), (0.13, tap(1.0, 3), 0.7), (0.26, tap(1.15, 4), 0.8),
                    (0.42, chord([261.6, 329.6, 392.0, 523.3]), 0.9)], 1.5)
    save('house_done', flourish, 0.55)
    # added by hand to the text so the rest of the file stays exactly as it was
    with open(LIST, encoding='utf-8', newline='') as f:
        text = f.read()
    for name in ('house_tap', 'house_done'):
        if '"' + name + '"' not in text:
            entry = json.dumps({'sounds': [{'name': 'charpowers:' + name}], 'subtitle': 'sounds.charpowers.' + name}, indent=2).replace('\n', '\n  ')
            end = text.rstrip().rfind('}')
            text = text[:end].rstrip() + ',\n  "' + name + '": ' + entry + '\n}\n'
    with open(LIST, 'w', encoding='utf-8', newline='') as f:
        f.write(text)


if __name__ == '__main__':
    main()
