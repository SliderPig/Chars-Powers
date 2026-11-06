# One picture of all the new weapons and Solgo, from the real-game shots in run-real/screenshots. From the charpowers folder, after python tools/run_real_client.py items:  python tools/make_items_sheet.py
import os
import sys

from PIL import Image, ImageDraw

FOLDERS = sys.argv[1:] or ['run-real/screenshots']  # later folders fill in shots the first ones don't have
OUT = 'docs/previews/new_items.png'
PICKS = ['sword_fp_idle', 'sword_fp_slash1', 'sword_fp_slam_line', 'sword_fp_thrust', 'sword_tp_move0', 'sword_tp_move2', 'sword_front_idle', 'rock_far', 'rock_sword_close', 'rock_sword_night',
         'katana_fp_idle', 'katana_fp_cut0', 'katana_tp_cut0', 'katana_tp_after1', 'dagger_fp_cut', 'dagger_tp_cut', 'hammer_fp_idle', 'hammer_tp_down', 'flame_ring', 'flame_ring_late',
         'staff_fp_idle', 'ice_flying_tp', 'ice_frozen', 'fork_front', 'spray_water', 'spray_lava', 'spray_milk', 'solgo_day_16', 'solgo_scale', 'solgo_night_13',
         'solgo_move_walk', 'solgo_move_windup', 'solgo_move_slash', 'solgo_move_lunge', 'solgo_move_scream', 'solgo_move_run', 'solgo_dying_late', 'solgo_day_40', 'solgo_day_6', 'solgo_night_35']
W, H, COLS = 320, 180, 5


def where(name):
    for folder in FOLDERS:
        if os.path.exists(os.path.join(folder, name + '.png')):
            return os.path.join(folder, name + '.png')
    return None


found = [p for p in PICKS if where(p)]
rows = (len(found) + COLS - 1) // COLS
sheet = Image.new('RGB', (COLS * W + (COLS + 1) * 6, rows * (H + 20) + 6), (24, 18, 30))
d = ImageDraw.Draw(sheet)
for i, name in enumerate(found):
    x, y = 6 + (i % COLS) * (W + 6), 6 + (i // COLS) * (H + 20)
    sheet.paste(Image.open(where(name)).convert('RGB').resize((W, H), Image.LANCZOS), (x, y))
    d.text((x + 4, y + H + 3), name.replace('_', ' '), fill=(240, 190, 220))
sheet.save(OUT, optimize=True)
print('sheet of', len(found), 'of', len(PICKS), 'shots')
