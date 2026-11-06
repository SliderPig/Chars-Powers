# Talks to the real game started with:  python tools/run_real_client.py anim   (see src/smoke/.../AnimChecks.java)
#   python tools/run_real_client.py animsheet   does it all by itself: every picture, quits, writes the contact sheet
#   python tools/anim_lab.py send "cam front" "gun on" "wait 12" "shot g1"     does those steps, waits until they're done
#   python tools/anim_lab.py sheet                                             pictures every pose from every side, makes docs/previews/player_animations.png
#   python tools/anim_lab.py poses hand_slam:7,11 ring_thrust:6                the same pictures for just these poses, no sheet
#   python tools/anim_lab.py quit
import glob
import os
import sys
import time

from PIL import Image, ImageDraw, ImageFont

RUN = os.path.abspath('run-real')
LAB = os.path.join(RUN, 'lab')
SHOTS = os.path.join(RUN, 'screenshots')
OUT = os.path.join('docs', 'superpowers', 'previews', 'player_animations.png')

# pose: the moments worth seeing, in ticks
POSES = {
    'ring_thrust': [3, 6], 'ring_raise': [8], 'hand_slam': [7, 11], 'lantern_slap': [5, 10], 'sword_sweep_left': [3, 8], 'sword_sweep_right': [3, 8], 'sword_overhead': [6, 12],
    'hands_out': [5, 14], 'lantern_grab': [6, 18], 'lantern_throw': [4, 8], 'suit_call': [10], 'suit_stomp': [4], 'command_point': [6], 'whistle': [8], 'raise_dead': [6, 24],
    'clock_hand': [14], 'shout': [4, 12], 'ground_slam': [6, 12], 'reach_pull': [8, 16], 'temple': [12], 'reading': [20], 'hologram_swipe': [3, 9], 'split': [5, 11],
    'sweep_swing': [3, 8], 'raised_finger': [8], 'hammer_tap': [1, 4],
}
VIEWS = ['front', 'quarter', 'right', 'fp']


def send(lines, patience=240):
    os.makedirs(LAB, exist_ok=True)
    done = os.path.join(LAB, 'done')
    if os.path.exists(done):
        os.remove(done)
    tmp = os.path.join(LAB, 'todo.tmp')
    with open(tmp, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    os.replace(tmp, os.path.join(LAB, 'todo.txt'))
    start = time.time()
    while time.time() - start < patience:
        if os.path.exists(done):
            return True
        time.sleep(0.5)
    print('the game never said done')
    return False


def pose_lines(poses):
    lines = ['reload', 'setup', 'lantern', 'wait 10']
    for name, ticks in poses.items():
        for tick in ticks:
            lines.append(f'pose {name} {tick}')
            for view in VIEWS:
                lines += [f'cam {view}', 'wait 3', f'shot anim_{name}_{tick}_{view}']
    lines += ['clear', 'cam fp']
    return lines


def gun_lines():
    lines = ['reload', 'setup', 'lantern', 'cam front', 'pitch 0', 'wait 5', 'gun on', 'wait 14']
    for view in ['front', 'quarter', 'right', 'back', 'backquarter', 'left', 'fp']:
        lines += [f'cam {view}', 'wait 3', f'shot gun_hold_{view}']
    for pitch in [-45, 35]:
        lines += [f'pitch {pitch}', 'wait 3']
        for view in ['right', 'quarter', 'fp']:
            lines += [f'cam {view}', 'wait 3', f'shot gun_pitch{pitch}_{view}']
    # what the walk-through and a player see: the game's own cameras, looking a little down, just after it's called
    lines += ['pitch 10', 'gun off', 'wait 12', 'gun on', 'wait 12', 'cam tpback', 'wait 3', 'shot gun_game_back', 'cam tpfront', 'wait 3', 'shot gun_game_front', 'cam fp', 'wait 2']
    lines += ['pitch 0', 'fire on', 'wait 16']
    for view in ['quarter', 'fp']:
        lines += [f'cam {view}', 'wait 3', f'shot gun_fire_{view}']
    lines += ['fire off', 'crouch on', 'wait 6', 'cam quarter', 'wait 3', 'shot gun_crouch_quarter', 'crouch off', 'wait 3', 'gun off', 'wait 12', 'cam fp']
    return lines


def rounded(size, radius, fill):
    img = Image.new('RGBA', size, (0, 0, 0, 0))
    ImageDraw.Draw(img).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=fill)
    return img


def crop(path, view):
    img = Image.open(path).convert('RGB')
    w, h = img.size
    if view == 'fp':
        box = (0, 0, w, h)
    elif view == 'game':
        box = (w // 2 - 330, 150, w // 2 + 330, 810)
    elif os.path.basename(path).startswith('gun_'):
        side = int(h * 0.92)
        box = ((w - side) // 2, (h - side) // 2, (w + side) // 2, (h + side) // 2)
    else:
        box = (w // 2 - 240, 230, w // 2 + 240, 710)
    return img.crop(box)


def sheet(poses, gun=True):
    try:
        title = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 26)
        small = ImageFont.truetype(r'C:\Windows\Fonts\segoeui.ttf', 15)
        big = ImageFont.truetype(r'C:\Windows\Fonts\segoeuib.ttf', 40)
    except OSError:
        title = small = big = ImageFont.load_default()
    cell, fp_w, gap = 210, 373, 10
    rows = []
    if gun:
        rows.append(('minigun: held', [('gun_hold_front', 'front'), ('gun_hold_quarter', 'quarter'), ('gun_hold_right', 'right'), ('gun_hold_fp', 'fp')]))
        rows.append(('minigun: from behind and the left', [('gun_hold_back', 'back'), ('gun_hold_backquarter', 'backquarter'), ('gun_hold_left', 'left')]))
        rows.append(('minigun: the game cameras', [('gun_game_back', 'game'), ('gun_game_front', 'game')]))
        rows.append(('minigun: looking up', [('gun_pitch-45_quarter', 'quarter'), ('gun_pitch-45_right', 'right'), ('gun_pitch-45_fp', 'fp')]))
        rows.append(('minigun: looking down', [('gun_pitch35_quarter', 'quarter'), ('gun_pitch35_right', 'right'), ('gun_pitch35_fp', 'fp')]))
        rows.append(('minigun: firing, crouching', [('gun_fire_quarter', 'quarter'), ('gun_crouch_quarter', 'quarter'), ('gun_fire_fp', 'fp')]))
    for name, ticks in poses.items():
        for tick in ticks:
            rows.append((f'{name}  at tick {tick}', [(f'anim_{name}_{tick}_{v}', v) for v in VIEWS]))
    row_h = cell + 44
    per_col = 16
    cols = (len(rows) + per_col - 1) // per_col
    col_w = 3 * (cell + gap) + fp_w + 44
    W = cols * col_w + 30
    H = 92 + min(per_col, len(rows)) * row_h + 20
    out = Image.new('RGB', (W, H), (30, 18, 36))
    d = ImageDraw.Draw(out)
    d.text((30, 20), 'Player animations', font=big, fill=(255, 170, 214))
    for i, (label, shots) in enumerate(rows):
        x = 20 + (i // per_col) * col_w
        y = 92 + (i % per_col) * row_h
        card = rounded((col_w - 14, row_h - 8), 18, (56, 34, 64, 255))
        out.paste(card, (x, y), card)
        d.text((x + 16, y + 4), label, font=title if len(label) < 34 else small, fill=(250, 226, 240))
        cx = x + 14
        for shot, view in shots:
            path = os.path.join(SHOTS, shot + '.png')
            w = fp_w if view == 'fp' else cell
            if os.path.exists(path):
                pic = crop(path, view).resize((w, cell), Image.LANCZOS)
                mask = Image.new('L', (w, cell), 0)
                ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, cell - 1), 12, fill=255)
                out.paste(pic, (cx, y + 36 - 2), mask)
            cx += w + gap
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    out.save(OUT)
    print('wrote', OUT, out.size)


def main():
    what = sys.argv[1]
    if what == 'send':
        ok = send(sys.argv[2:])
        print('done' if ok else 'not done')
    elif what == 'gun':
        print(send(gun_lines()))
    elif what == 'poses':
        poses = {}
        for a in sys.argv[2:]:
            name, _, ticks = a.partition(':')
            poses[name] = [int(t) for t in ticks.split(',')] if ticks else POSES[name]
        print(send(pose_lines(poses)))
    elif what == 'sheet':
        if '--only' not in sys.argv:
            send(gun_lines())
            send(pose_lines(POSES))
        sheet(POSES)
    elif what == 'quit':
        send(['quit'], patience=5)


if __name__ == '__main__':
    main()
