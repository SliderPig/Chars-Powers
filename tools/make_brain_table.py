# Makes the table the clones' battle brain reads: what every key ability in the Powers profile's mods is for,
# and what has to be set up before it will fire. Re-run when a power mod updates or a new one joins.
# Run from the charpowers folder:  python -I tools/make_brain_table.py
# Writes src/main/resources/data/charpowers/clone_brain.json
import collections
import glob
import json
import os
import re
import sys
import zipfile

sys.stdout.reconfigure(encoding='utf-8')
MODS = os.path.expandvars(r'%APPDATA%\ModrinthApp\profiles\Powers\mods')
OUT = 'src/main/resources/data/charpowers/clone_brain.json'
KEYS = {'palladium:action': 'action', 'palladium:toggle': 'toggle', 'palladium:held': 'held', 'palladium:activation': 'activation'}

TYPE_ROLES = {
    'palladium:energy_beam': 'RANGED', 'palladium:projectile': 'RANGED', 'arrzenhanced:offset_projectile': 'RANGED',
    'fsang:projectile_with_offset': 'RANGED', 'palladium:intangibility': 'DEFEND', 'palladium:invisibility': 'DEFEND',
    'pantheonsent:moon_knight_blocking': 'DEFEND', 'arrzenhanced:block_damage': 'DEFEND', 'fsang:leap': 'MOVE',
    'fsang:self_x_motion': 'MOVE', 'eop:raycast_teleport': 'MOVE', 'palladium:attribute_modifier': 'BUFF',
}
# never pressed: menus, selectors, looks, and things that hand out items
SKIP_TYPES = {
    'palladium:ability_wheel', 'palladium:aim', 'palladium:show_both_arms', 'palladium:render_layer', 'palladium:particles',
    'palladium:entity_glow', 'fsang:palladium_property_modifier', 'fsang:scoreboard_modifier', 'gabeupheroes:scoreboard_mod',
    'arrzenhanced:set_property', 'eop:give_item', 'eop:tag_give_item', 'fsang:telekinesis_grab', 'charpowers:hidden_slot',
    'charpowers:clone', 'charpowers:dog_pile', 'charpowers:passive',
}
SKIP_WORDS = re.compile(r'menu|wheel|change|select|switch|configur|customi[sz]|settings|purchase|unlock|\bget\b|get[._]|dna|reload|cycle|scroll|'
                        r'flight|fly|hover|glide|skin|costume|outfit|cosmetic|sense|vision|zoom|craft|portal|mine\b|dig', re.I)
WORDS = [
    ('HEAL', re.compile(r'heal|regen|mend|recover|cure', re.I)),
    ('DEFEND', re.compile(r'shield|block|barrier|guard|forcefield|force_field|intangib|phase|dodge|invis|parry|deflect', re.I)),
    ('RANGED', re.compile(r'beam|blast|bolt|shoot|shot|laser|projectile|fireball|missile|breath|\bray\b|throw|orb|spike|lightning', re.I)),
    ('MELEE', re.compile(r'punch|slam|strike|slash|smash|claw|stomp|uppercut|kick|bite|\bcut\b|combo|swing|melee', re.I)),
    ('MOVE', re.compile(r'dash|leap|teleport|warp|blink|lunge|charge', re.I)),
    ('BUFF', re.compile(r'transform|rage|empower|boost|strength|power.?up|enhance|awaken', re.I)),
]
BREAKS = re.compile(r'\b(fill|setblock|destroy)\b|explosion|explode|create_explosion|crater|block_damage', re.I)
DANGER = re.compile(r'superpower (remove|set)|kill @s|clear @s|function [a-z_]+:power_obtain', re.I)
COMMAND_HINTS = [
    ('HEAL', re.compile(r'effect give @s [a-z_:]*(regeneration|instant_health)|\bheal\b', re.I)),
    ('DEFEND', re.compile(r'effect give @s [a-z_:]*resistance', re.I)),
    ('BUFF', re.compile(r'effect give @s [a-z_:]*(strength|speed|haste)', re.I)),
    ('RANGED', re.compile(r'\bdamage @e|run damage|summon [a-z_:]*(lightning|fireball|arrow|projectile)', re.I)),
    ('MOVE', re.compile(r'\btp @s \^|\bteleport @s \^', re.I)),
]
def text_of(a):
    parts = []
    for key in ('title', 'description'):
        v = a.get(key)
        if isinstance(v, dict):
            parts.append(str(v.get('translate') or v.get('text') or ''))
        elif v:
            parts.append(str(v))
    return ' '.join(parts)


def commands_of(a):
    out = []
    for key in ('commands', 'first_tick_commands', 'last_tick_commands'):
        v = a.get(key)
        if isinstance(v, list):
            out += [str(x) for x in v]
    return '\n'.join(out)


def press_kind(a):
    for c in (a.get('conditions') or {}).get('enabling') or []:
        if isinstance(c, dict) and c.get('type') in KEYS:
            return KEYS[c['type']], bool(c.get('needs_empty_hand'))
    return None, False


def role_of(name, a, children):
    t = str(a.get('type'))
    if t in SKIP_TYPES:
        return None
    words = name + ' ' + text_of(a)
    cmds = commands_of(a)
    if DANGER.search(cmds) or SKIP_WORDS.search(name):
        return None
    if t in TYPE_ROLES:
        return TYPE_ROLES[t]
    # a key that just starts a timer: it's for whatever that timer sets off, and an attack beats a boost that comes with it
    for c in sorted(children, key=lambda x: TYPE_ROLES.get(str(x.get('type'))) != 'RANGED'):
        if str(c.get('type')) in TYPE_ROLES:
            return TYPE_ROLES[str(c.get('type'))]
    for role, rx in COMMAND_HINTS:
        if rx.search(cmds):
            return role
    for role, rx in WORDS:
        if rx.search(words):
            return role
    if t in ('palladium:dummy', 'palladium:animation_timer', 'palladium:repeating_animation_timer') and not children:
        return None
    return 'UNKNOWN'


def build():
    table = {}
    counts = collections.Counter()
    for j in sorted(glob.glob(MODS + r'\*.jar')):
        try:
            z = zipfile.ZipFile(j)
        except Exception:
            continue
        for n in z.namelist():
            m = re.match(r'data/([^/]+)/palladium/powers/(.*)\.json$', n)
            if not m:
                continue
            try:
                d = json.loads(z.read(n).decode('utf-8', 'replace'))
            except Exception:
                continue
            ab = d.get('abilities')
            if not isinstance(ab, dict):
                continue
            power = m.group(1) + ':' + m.group(2)
            for name, a in ab.items():
                if not isinstance(a, dict):
                    continue
                kind, empty_hand = press_kind(a)
                if not kind:
                    continue
                entry = make_entry(power, name, a, ab, kind, empty_hand)
                counts[entry['r'] if entry else 'SKIP'] += 1
                if entry:
                    table.setdefault(power, {})[name] = entry
    return table, counts


def make_entry(power, name, a, ab, kind, empty_hand):
    unlock = (a.get('conditions') or {}).get('unlocking') or []
    entry = {'p': kind}
    props, select, main, off = {}, None, empty_hand, False
    for c in unlock:
        if not isinstance(c, dict):
            continue
        t = c.get('type')
        # Anything else it can't arrange (tags, worn suits...) is left to the game: the clone only presses what's unlocked.
        if t == 'palladium:integer_property' and isinstance(c.get('min'), int) and c.get('property'):
            props[c['property']] = c['min']
        elif t == 'eop:power_selected':
            select = c.get('power')
        elif t == 'palladium:empty_slot':
            if c.get('slot') == 'mainhand':
                main = True
            elif c.get('slot') == 'offhand':
                off = True
    children = []
    for other, b in ab.items():
        if other != name and isinstance(b, dict):
            s = json.dumps(b.get('conditions') or {})
            if f'"ability": "{name}"' in s and 'ability_unlocked' not in s:
                children.append(b)
    role = role_of(name, a, children)
    if not role:
        return None
    entry['r'] = role
    beam = next((b for b in [a] + children if b.get('type') == 'palladium:energy_beam'), None)
    entry['d'] = int(beam.get('max_distance', 24)) if beam else 32
    if beam:
        entry['n'] = 1
    if BREAKS.search(json.dumps(a)):
        entry['b'] = 1
    if props:
        entry['s'] = props
    if select:
        entry['e'] = select
    if main:
        entry['m'] = 1
    if off:
        entry['o'] = 1
    return entry


def main():
    table, counts = build()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(table, f, separators=(',', ':'), sort_keys=True)
    print('powers', len(table), 'size', os.path.getsize(OUT))
    for k, v in counts.most_common():
        print(f'{v:6} {k}')
    for sample in ('fsang:asgardian', 'eop:bloodweaver'):
        print(sample, json.dumps(table.get(sample, {}))[:900])


if __name__ == '__main__':
    main()
