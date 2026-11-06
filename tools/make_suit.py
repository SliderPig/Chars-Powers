# Makes everything the Invincible Armour suit needs but code: item icons, recipes and text. Then run make_depth_art.py: it replaces the suit sheets and station models this draws.
# (Older note) textures, block and item models, recipes and text.
# Run from the charpowers folder:  python tools/make_suit.py
# The pictures are drawn right here as rows of letters, so they're easy to change. Or draw over the PNGs it writes,
# and don't run this again.
import json
import os

from PIL import Image, ImageDraw

R = 'src/main/resources/'
A = R + 'assets/charpowers/'
D = R + 'data/charpowers/'
# Sleek lime and silver: the suit is for a friend whose favourite colour is lime green.
WHITE, LIGHT, SILVER, MID, STEEL, INK = (244, 248, 250), (222, 228, 233), (188, 196, 203), (146, 155, 163), (92, 100, 108), (34, 38, 43)
LIME, DEEP, GLOW = (150, 255, 30), (90, 205, 20), (220, 255, 150)
COLOURS = {'k': INK, 'p': MID, 'b': STEEL, 's': LIGHT, 'l': WHITE, 'i': LIME, 'h': DEEP,
           'c': LIME, 'r': LIME, 'y': LIME, 'u': DEEP, 'd': LIME}
SHEET = {'W': WHITE, 'L': LIGHT, 'S': SILVER, 'M': MID, 'D': STEEL, 'K': INK, 'i': LIME, 'h': DEEP, 'g': GLOW}


def picture(rows):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows[:16]):
        for x, ch in enumerate(row.ljust(16, '.')[:16]):
            if ch in COLOURS:
                im.putpixel((x, y), COLOURS[ch] + (255,))
    return im


ICONS = {
    'invincible_suit': ['....kkkkkkkk....', '....ksssssskk...', '....kiiiiiiik...', '....kppppppkk...', '..kkkkkkkkkkkk..', '.ksspppsspppssk.', '.ksspppiipppssk.',
                        '.kkkkppiippkkkk.', '.kss.kppppk.ssk.', '.kss.kppppk.ssk.', '.kkk.kpkkpk.kkk.', '.....kpkkpk.....', '.....kskksk.....', '.....kpkkpk.....', '....kkskkskk....', '....kkkkkkkk....'],
    'suit_head_part': ['................', '................', '....kkkkkkkk....', '...kssssssssk...', '..ksppppppppsk..', '..ksppppppppsk..', '..kiiiiiiiiiik..', '..kiiiiiiiiiik..',
                       '..kspppsspppsk..', '..kspppsspppsk..', '..kkppppppppkk..', '...kkkkkkkkkk...', '................', '................', '................', '................'],
    'suit_body_part': ['................', '..kkkk....kkkk..', '.kssskkkkkkssssk', '.ksspppsspppssk.', '.ksspppsspppssk.', '.kkkpppiipppkkk.', '...kppiiiippk...', '...kpppiipppk...',
                       '...kppppppppk...', '...kspppppppk...', '...kkssssskk....', '....kppppk......', '....kkkkkk......', '................', '................', '................'],
    'suit_leg_part': ['................', '...kkkkkkkkkk...', '..kspppsspppsk..', '..kspppsspppsk..', '..kpppkkkkpppk..', '..kpppk..kpppk..', '..kiipk..kpiik..', '..kpppk..kpppk..',
                      '..kpppk..kpppk..', '..kspsk..kspsk..', '..kpppk..kpppk..', '..kpppk..kpppk..', '..kkkkk..kkkkk..', '................', '................', '................'],
    'suit_boot_part': ['................', '................', '................', '................', '.kkkkk....kkkkk.', '.kpppk....kpppk.', '.kiipk....kpiik.', '.kpppk....kpppk.',
                       '.kpppk....kpppk.', '.kpppkkk.kkpppk.', '.kpppssk.kssppk.', '.ksssssk.ksssssk', '.kkkkkkk.kkkkkkk', '................', '................', '................'],
    'shield_module': ['................', '..kkkkkkkkkkkk..', '..kppppppppppk..', '..kpkkkkkkkkpk..', '..kpkuuddddkpk..', '..kpkuuddddkpk..', '..kpkuuuuddkpk..', '..kpkuuuuddkpk..',
                      '..kpkkuuuukkpk..', '..kppkkuukkppk..', '..kpppkkkkpppk..', '..kppppppppppk..', '..kkkkkkkkkkkk..', '................', '................', '................'],
    'scanner_module': ['................', '..kkkkkkkkkkkk..', '..kppppppppppk..', '..kppppppppppk..', '..kpppkkkkpppk..', '..kpkkcccckkpk..', '..kkcccsscccck..', '..kkcccsscccck..',
                       '..kpkkcccckkpk..', '..kpppkkkkpppk..', '..kppppppppppk..', '..kppppppppppk..', '..kkkkkkkkkkkk..', '................', '................', '................'],
    'strength_module': ['................', '..kkkkkkkkkkkk..', '..kppppppppppk..', '..kppkkkkkkppk..', '..kpkrrrrrrkpk..', '..kpkrrkrkrkpk..', '..kpkrrrrrrkpk..', '..kpkrrrrrrkpk..',
                        '..kppkrrrrkppk..', '..kpppkrrkpppk..', '..kppppkkppppk..', '..kppppppppppk..', '..kkkkkkkkkkkk..', '................', '................', '................'],
    'speed_module': ['................', '..kkkkkkkkkkkk..', '..kppppppppppk..', '..kpppppkkpppk..', '..kppppkyykppk..', '..kpppkyykpppk..', '..kppkyyyyykpk..', '..kpkyyyyykppk..',
                     '..kppppkyykppk..', '..kpppkyykpppk..', '..kpppkkppppk...', '..kppppppppppk..', '..kkkkkkkkkkkk..', '................', '................', '................'],
}


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)


def dump(obj, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


# The whole suit on one 64x32 sheet, the game's layout for armour: head at the top left, legs below, body in the middle, arms right.
# Silver plates shaded from the top down, thin lime lines running along them. Each face is rows of letters, one letter a pixel.
HEAD_FRONT = ['LLLLLLLL', 'SLLLLLLS', 'SSLLLLSS', 'MiiiiiiM', 'MSiiiiSM', 'DSSiiSSD', 'DMSSSSMD', 'KDMMMMDK']
HEAD_SIDE = ['SSSSLLLL', 'MSSSSSLL', 'MMSSSSSS', 'DMMMMMii', 'DDMMMMMM', 'DDDMMMSS', 'KDDDMMMM', 'KKDDDDDD']
HEAD_BACK = ['LLLLLLLL', 'SSSSSSSS', 'MMMiiMMM', 'MMMiiMMM', 'DMMhhMMD', 'DDMMMMDD', 'DDDMMDDD', 'KDDDDDDK']
HEAD_TOP = ['SSSLLSSS', 'SLLiiLLS', 'SLLiiLLS', 'SLLiiLLS', 'SLLiiLLS', 'SLLiiLLS', 'SLLiiLLS', 'SSSLLSSS']
# The arms cover the body's outer columns, the helmet its top two rows and the legs its bottom two, so its lines sit in what's left.
BODY_FRONT = ['LLLWWLLL', 'LLSLLSLL', 'MiSLLSiM', 'MSiLLiSM', 'MSSiiSSM', 'MSLggLSM', 'DMSiiSMD', 'DMMhhMMD', 'DDMSSMDD', 'iiiiiiii', 'DMMMMMMD', 'KDDDDDDK']
BODY_BACK = ['LLLLLLLL', 'SSSSSSSS', 'MSSiiSSM', 'MSSiiSSM', 'DMMiiMMD', 'DMMhhMMD', 'DMMhhMMD', 'DDMhhMDD', 'DDMMMMDD', 'iiiiiiii', 'DMMMMMMD', 'KDDDDDDK']
BODY_SIDE = ['LLLL', 'SSSS', 'MSSM', 'MMMM', 'DMMD', 'DMMD', 'DMMD', 'DDDD', 'DDDD', 'iiii', 'DDMM', 'KDDK']
ARM_FRONT = ['WLLL', 'LLLS', 'SSSM', 'DDDD', 'MSSM', 'MSSM', 'MSSM', 'DMMD', 'iiii', 'LLLL', 'SSSS', 'MMMM']
ARM_SIDE = ['LLLL', 'SSSS', 'SMMS', 'DDDD', 'MSSM', 'MiiM', 'MiiM', 'DiiD', 'iiii', 'LLLL', 'SSSS', 'MMMM']
LEG_FRONT = ['SLLS', 'MSSM', 'MSSM', 'DMMD', 'LLLL', 'SiiS', 'DMMD', 'MSSM', 'MSSM', 'iiii', 'LLLL', 'SSSS']
LEG_SIDE = ['SSSS', 'MMMM', 'MMMM', 'DDDD', 'MMMM', 'MiiM', 'MiiM', 'MiiM', 'MMMM', 'iiii', 'LLLL', 'SSSS']
LEG_BACK = ['MMMM', 'MSSM', 'DMMD', 'DDDD', 'MMMM', 'DMMD', 'DMMD', 'DMMD', 'DDDD', 'iiii', 'SSSS', 'MMMM']


def suit_sheet(brighter=False):
    im = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    # the slim suit is the same lines in brighter, polished silver
    swap = {'M': 'S', 'D': 'M', 'K': 'D', 'S': 'L'} if brighter else {}

    def face(x, y, rows):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                im.putpixel((x + i, y + j), SHEET[swap.get(ch, ch)] + (255,))

    def solid(x, y, w, h, ch):
        face(x, y, [ch * w] * h)

    def flip(rows):
        return [row[::-1] for row in rows]
    face(8, 0, HEAD_TOP)
    solid(16, 0, 8, 8, 'D')
    face(0, 8, HEAD_SIDE)
    face(8, 8, HEAD_FRONT)
    face(16, 8, flip(HEAD_SIDE))
    face(24, 8, HEAD_BACK)
    solid(20, 16, 8, 4, 'L')
    solid(28, 16, 8, 4, 'D')
    face(16, 20, BODY_SIDE)
    face(20, 20, BODY_FRONT)
    face(28, 20, BODY_SIDE)
    face(32, 20, BODY_BACK)
    solid(44, 16, 4, 4, 'W')
    solid(48, 16, 4, 4, 'D')
    face(40, 20, ARM_SIDE)
    face(44, 20, ARM_FRONT)
    face(48, 20, ARM_FRONT)
    face(52, 20, ARM_FRONT)
    solid(4, 16, 4, 4, 'S')
    solid(8, 16, 4, 4, 'K')
    face(0, 20, LEG_SIDE)
    face(4, 20, LEG_FRONT)
    face(8, 20, LEG_BACK)
    face(12, 20, LEG_BACK)
    return im


save(suit_sheet(), A + 'textures/models/armor/invincible.png')
save(suit_sheet(True), A + 'textures/models/armor/invincible_slim.png')
for name, rows in ICONS.items():
    save(picture(rows), A + f'textures/item/{name}.png')
    dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'charpowers:item/{name}'}}, A + f'models/item/{name}.json')


def station(top, core):
    im = Image.new('RGBA', (16, 16), STEEL + (255,))
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 15, 15), outline=LIGHT + (255,))
    d.line((0, 15, 15, 15), fill=MID + (255,))
    d.line((15, 0, 15, 15), fill=MID + (255,))
    d.rectangle((1, 1, 14, 14), outline=INK + (255,))
    if top:
        d.rectangle((4, 4, 11, 11), fill=INK + (255,), outline=SILVER + (255,))
        d.rectangle((6, 6, 9, 9), fill=core + (255,))
    else:
        for y in (4, 7, 10):
            d.line((3, y, 12, y), fill=core + (255,))
            d.line((3, y + 1, 12, y + 1), fill=INK + (255,))
    return im


for state, core in (('off', MID), ('on', LIME)):
    save(station(False, core), A + f'textures/block/charging_station_side_{state}.png')
    save(station(True, core), A + f'textures/block/charging_station_top_{state}.png')
    dump({'parent': 'minecraft:block/cube_column', 'textures': {'side': f'charpowers:block/charging_station_side_{state}', 'end': f'charpowers:block/charging_station_top_{state}'}},
         A + f'models/block/charging_station_{state}.json')
dump({'variants': {'charging=false': {'model': 'charpowers:block/charging_station_off'}, 'charging=true': {'model': 'charpowers:block/charging_station_on'}}}, A + 'blockstates/charging_station.json')
dump({'parent': 'charpowers:block/charging_station_off'}, A + 'models/item/charging_station.json')
dump({'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'charpowers:charging_station'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}, D + 'loot_tables/blocks/charging_station.json')
dump({'replace': False, 'values': ['charpowers:charging_station']}, R + 'data/minecraft/tags/blocks/mineable/pickaxe.json')
dump({'replace': False, 'values': ['charpowers:charging_station']}, R + 'data/minecraft/tags/blocks/needs_diamond_tool.json')

NEEDS = [{'type': 'forge:mod_loaded', 'modid': 'palladium'}, {'type': 'forge:mod_loaded', 'modid': 'gravecore'}]
V, B, T = 'palladium:vibranium_ingot', 'palladium:vibranium_block', 'palladium:titanium_block'
CIRCUIT, FLUX, WEAVE = 'palladium:vibranium_circuit', 'palladium:vibranium_flux_capacitor', 'palladium:vibranium_weave_boots'
BOARD, SCREEN, BATTERY, CPU = 'grave.core:advanced_motherboard', 'grave.core:screen', 'grave.core:netherite_battery', 'grave.core:cpu'


def shaped(name, rows, keys):
    dump({'type': 'minecraft:crafting_shaped', 'conditions': NEEDS, 'category': 'equipment', 'pattern': rows,
          'key': {k: {'item': v} for k, v in keys.items()}, 'result': {'item': f'charpowers:{name}'}}, D + f'recipes/{name}.json')


shaped('suit_head_part', ['VVV', 'VMV', ' S '], {'V': V, 'M': BOARD, 'S': SCREEN})
shaped('suit_body_part', ['BNB', 'BCB', 'BFB'], {'B': B, 'N': BATTERY, 'C': CPU, 'F': FLUX})
shaped('suit_leg_part', ['VCV', 'T T', 'V V'], {'V': V, 'C': CIRCUIT, 'T': T})
shaped('suit_boot_part', ['NCN', 'NWN'], {'N': 'minecraft:netherite_ingot', 'C': CIRCUIT, 'W': WEAVE})
dump({'type': 'minecraft:crafting_shapeless', 'category': 'equipment', 'conditions': NEEDS,
      'ingredients': [{'item': f'charpowers:suit_{p}_part'} for p in ('head', 'body', 'leg', 'boot')], 'result': {'item': 'charpowers:invincible_suit'}}, D + 'recipes/invincible_suit.json')
shaped('charging_station', ['BGB', ' S '], {'B': BATTERY, 'G': 'palladium:redstone_flux_crystal_geode', 'S': 'palladium:suit_stand'})
for name, core in {'shield': 'minecraft:shield', 'scanner': 'minecraft:spyglass', 'strength': 'minecraft:netherite_ingot', 'speed': 'minecraft:rabbit_foot'}.items():
    shaped(f'{name}_module', [' C ', 'CXC', ' C '], {'C': CIRCUIT, 'X': core})
dump({'type': 'charpowers:suit_module'}, D + 'recipes/suit_module.json')

# things from before it became one item
for old in ('invincible_helmet', 'invincible_chestplate', 'invincible_leggings', 'invincible_boots'):
    for path in (D + f'recipes/{old}.json', A + f'models/item/{old}.json', A + f'textures/item/{old}.png'):
        if os.path.exists(path):
            os.remove(path)
for n in (1, 2):
    for sheet in ('invincible', 'invincible_slim'):
        path = A + f'textures/models/armor/{sheet}_layer_{n}.png'
        if os.path.exists(path):
            os.remove(path)

LANG = A + 'lang/en_us.json'
with open(LANG, encoding='utf-8') as f:
    lang = json.load(f)
for key in ('item.charpowers.invincible_helmet', 'item.charpowers.invincible_chestplate', 'item.charpowers.invincible_leggings', 'item.charpowers.invincible_boots'):
    lang.pop(key, None)
for key, text in {'item.charpowers.invincible_suit': 'Invincible Suit', 'item.charpowers.suit_head_part': 'Suit Head Part', 'item.charpowers.suit_body_part': 'Suit Body Part',
                  'item.charpowers.suit_leg_part': 'Suit Leg Part', 'item.charpowers.suit_boot_part': 'Suit Boot Part',
                  'item.charpowers.shield_module': 'Shield Module', 'item.charpowers.scanner_module': 'Scanner Module',
                  'item.charpowers.strength_module': 'Strength Module', 'item.charpowers.speed_module': 'Speed Module',
                  'block.charpowers.charging_station': 'Charging Station', 'itemGroup.charpowers': "Char's Powers"}.items():
    lang[key] = text
with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(lang, f, indent=2, ensure_ascii=False)
    f.write('\n')
print('suit made')
