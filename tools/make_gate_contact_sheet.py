"""Builds docs/previews/gate_mobs.png from the real-game lineup screenshots (L*_mobs, L*_bosses) in run-real/screenshots.
Run after: python tools/run_real_client.py pack lineup"""
import glob
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
shots = sorted(glob.glob(os.path.join(ROOT, 'run-real', 'screenshots', 'L0*_mobs.png'))) + sorted(glob.glob(os.path.join(ROOT, 'run-real', 'screenshots', 'L1*_bosses.png')))
W, H, COLS = 800, 300, 2
rows = (len(shots) + COLS - 1) // COLS
sheet = Image.new('RGB', (W * COLS, H * rows), (30, 24, 40))
d = ImageDraw.Draw(sheet)
for i, f in enumerate(shots):
    im = Image.open(f).convert('RGB').crop((0, 150, 1600, 750)).resize((W, H), Image.LANCZOS)  # no sky or hand
    x, y = (i % COLS) * W, (i // COLS) * H
    sheet.paste(im, (x, y))
    d.text((x + 6, y + 4), os.path.basename(f).replace('.png', ''), fill=(255, 190, 230))
out = os.path.join(ROOT, 'docs', 'superpowers', 'previews', 'gate_mobs.png')
os.makedirs(os.path.dirname(out), exist_ok=True)
sheet.save(out)
print('saved', out, len(shots), 'pictures')
