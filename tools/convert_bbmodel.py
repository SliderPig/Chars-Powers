# Turns my Blockbench files into what GeckoLib and the game read. Their animations come straight across unchanged.
# Run from the charpowers folder:  python tools/convert_bbmodel.py
# After changing a model in Blockbench, save the .bbmodel in Downloads and run this again.
import base64
import json
import os

SRC = os.path.join(os.path.expanduser('~'), 'Downloads') + '/'  # where the .bbmodel files are
OUT = 'src/main/resources/assets/charpowers/'
CONSTRUCTS = {'fist': 'Fist Emerge', 'slap': 'Hand Slap3', 'slam': 'Hand Slam', 'throw': 'Hand Throw', 'greatsword': 'Greatsword Big',
              'dagger': 'Throwig Dagger Big', 'minigun': 'Minigun', 'turret': 'Turrent'}
LANTERN = 'Green Lantern'


def number(v):
    if isinstance(v, (int, float)):
        return round(v, 4)
    try:
        return round(float(v), 4)
    except (TypeError, ValueError):
        return str(v).replace('\n', '')


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, separators=(',', ':'))


def save_texture(d, path):
    source = d['textures'][0]['source']
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(base64.b64decode(source.split(',', 1)[1]))


def cube(e, tex_w, tex_h):
    origin_from, to = e['from'], e['to']
    size = [round(to[i] - origin_from[i], 4) for i in range(3)]
    out = {'origin': [round(-(origin_from[0] + size[0]), 4), round(origin_from[1], 4), round(origin_from[2], 4)], 'size': size}
    if e.get('inflate'):
        out['inflate'] = e['inflate']
    if e.get('rotation') and any(e['rotation']):
        r, o = e['rotation'], e.get('origin', [0, 0, 0])
        out['rotation'] = [-r[0], -r[1], r[2]]
        out['pivot'] = [-o[0], o[1], o[2]]
    if e.get('box_uv'):
        out['uv'] = e.get('uv_offset', [0, 0])
        if e.get('mirror_uv'):
            out['mirror'] = True
    else:
        uv = {}
        for face, data in e.get('faces', {}).items():
            if data.get('texture') is None:
                continue
            u0, v0, u1, v1 = data['uv']
            w, h = u1 - u0, v1 - v0
            if face in ('up', 'down'):  # the way Bedrock models hold these two faces
                uv[face] = {'uv': [round(u0 + w, 4), round(v0 + h, 4)], 'uv_size': [round(-w, 4), round(-h, 4)]}
            else:
                uv[face] = {'uv': [round(u0, 4), round(v0, 4)], 'uv_size': [round(w, 4), round(h, 4)]}
        out['uv'] = uv
    return out


def geo(d):
    groups = {g['uuid']: g for g in d.get('groups', [])}
    elements = {e['uuid']: e for e in d.get('elements', [])}
    tex_w, tex_h = d['resolution']['width'], d['resolution']['height']
    bones = []

    def walk(node, parent):
        g = groups.get(node.get('uuid'), node)
        o = g.get('origin', [0, 0, 0])
        bone = {'name': g['name'], 'pivot': [-o[0], o[1], o[2]]}
        if parent:
            bone['parent'] = parent
        r = g.get('rotation', [0, 0, 0])
        if any(r):
            bone['rotation'] = [-r[0], -r[1], r[2]]
        cubes = []
        kids = []
        for child in node.get('children', []):
            if isinstance(child, str):
                if child in elements and elements[child].get('visibility', True) is not False:
                    cubes.append(cube(elements[child], tex_w, tex_h))
            else:
                kids.append(child)
        if cubes:
            bone['cubes'] = cubes
        bones.append(bone)
        for kid in kids:
            walk(kid, g['name'])

    loose = []
    for top in d.get('outliner', []):
        if isinstance(top, str):
            if top in elements:
                loose.append(cube(elements[top], tex_w, tex_h))
        else:
            walk(top, None)
    if loose:
        bones.insert(0, {'name': 'loose', 'pivot': [0, 0, 0], 'cubes': loose})
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.unknown', 'texture_width': tex_w, 'texture_height': tex_h,
                        'visible_bounds_width': 8, 'visible_bounds_height': 8, 'visible_bounds_offset': [0, 2, 0]},
        'bones': bones}]}


def animations(d):
    out = {}
    for a in d.get('animations', []):
        bones = {}
        for animator in a.get('animators', {}).values():
            if animator.get('type', 'bone') != 'bone' or not animator.get('keyframes'):
                continue
            channels = {}
            for kf in sorted(animator['keyframes'], key=lambda k: k['time']):
                channel = kf.get('channel')
                if channel not in ('rotation', 'position', 'scale') or not kf.get('data_points'):
                    continue
                p = kf['data_points'][0]
                frame = {'vector': [number(p.get('x', 0)), number(p.get('y', 0)), number(p.get('z', 0))]}
                if kf.get('easing'):
                    frame['easing'] = kf['easing']
                    if kf.get('easingArgs'):
                        frame['easingArgs'] = kf['easingArgs']
                elif kf.get('interpolation') == 'step':
                    frame['easing'] = 'step'
                elif kf.get('interpolation') == 'catmullrom':
                    frame['easing'] = 'catmullrom'
                channels.setdefault(channel, {})[str(round(kf['time'], 4))] = frame
            if channels:
                bones[animator['name']] = channels
        entry = {'animation_length': round(a.get('length', 0), 4), 'bones': bones}
        if a.get('loop') == 'loop':
            entry['loop'] = True
        elif a.get('loop') == 'hold':
            entry['loop'] = 'hold_on_last_frame'
        out[a['name']] = entry
    return {'format_version': '1.8.0', 'animations': out, 'geckolib_format_version': 2}


for name, file in CONSTRUCTS.items():
    with open(SRC + file + '.bbmodel', encoding='utf-8') as f:
        d = json.load(f)
    write_json(OUT + f'geo/construct/{name}.geo.json', geo(d))
    write_json(OUT + f'animations/construct/{name}.animation.json', animations(d))
    if name != 'fist':  # the fist's texture is painted by tools/make_fist_art.py in the hand's style
        save_texture(d, OUT + f'textures/entity/construct/{name}.png')
    print(name, 'bones', len(geo(d)['minecraft:geometry'][0]['bones']), 'animations', list(animations(d)['animations']))

# the lantern is a plain item model
with open(SRC + LANTERN + '.bbmodel', encoding='utf-8') as f:
    d = json.load(f)
save_texture(d, OUT + 'textures/item/power_battery.png')
res = d['resolution']
elements = []
for e in d['elements']:
    faces = {}
    for face, data in e.get('faces', {}).items():
        if data.get('texture') is None:
            continue
        u0, v0, u1, v1 = data['uv']
        k_u, k_v = 16 / res['width'], 16 / res['height']
        faces[face] = {'uv': [round(u0 * k_u, 3), round(v0 * k_v, 3), round(u1 * k_u, 3), round(v1 * k_v, 3)], 'texture': '#0'}
    item = {'from': e['from'], 'to': e['to'], 'faces': faces}
    r = e.get('rotation')
    if r and any(r):
        axis = 'xyz'[[i for i, v in enumerate(r) if v][0]]
        item['rotation'] = {'angle': r['xyz'.index(axis)], 'axis': axis, 'origin': e.get('origin', [8, 8, 8])}
    elements.append(item)
write_json(OUT + 'models/item/power_battery.json', {'textures': {'0': 'charpowers:item/power_battery', 'particle': 'charpowers:item/power_battery'},
                                                   'elements': elements, 'display': d.get('display', {}) or {}})
print('lantern elements', len(elements))
