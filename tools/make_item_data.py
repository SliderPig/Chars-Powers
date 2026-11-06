# Everything the game reads from data files for Char's new weapons: recipes, tags, the soul damage type, Kings Rock's world generation, advancements, names.
# From the charpowers folder:  python tools/make_item_data.py
import json
import os

D = 'src/main/resources/data/'
LANG = 'src/main/resources/assets/charpowers/lang/en_us.json'


def save(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# ---- recipes (the craftable three)
save(D + 'charpowers/recipes/fork.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['N N', ' I '], 'key': {'N': {'item': 'minecraft:iron_nugget'}, 'I': {'item': 'minecraft:iron_ingot'}}, 'result': {'item': 'charpowers:fork'}})
save(D + 'charpowers/recipes/spray_bottle.json', {'type': 'minecraft:crafting_shapeless', 'category': 'equipment', 'ingredients': [{'item': 'minecraft:glass_bottle'}, {'item': 'minecraft:iron_ingot'}], 'result': {'item': 'charpowers:spray_bottle'}})
save(D + 'charpowers/recipes/flame_hammer.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['VVV', 'VR ', ' R '], 'key': {'V': {'item': 'palladium:vibranium_block'}, 'R': {'item': 'minecraft:blaze_rod'}}, 'result': {'item': 'charpowers:flame_hammer'}})

# ---- the soul cut goes straight through everything
save(D + 'charpowers/damage_type/soul.json', {'message_id': 'charpowers.soul', 'exhaustion': 0.1, 'scaling': 'never'})
for tag in ('bypasses_armor', 'bypasses_shield', 'bypasses_effects', 'bypasses_resistance', 'bypasses_enchantments'):
    save(D + f'minecraft/tags/damage_type/{tag}.json', {'replace': False, 'values': ['charpowers:soul']})

# ---- swords and such, in case another mod (or Punchy without its file) goes by tags
TAGS = {'swords': ['golden_kings_long_sword', 'anima_katana', 'dagger_of_darkness', 'fork'], 'axes': ['flame_hammer'], 'spears': ['ice_staff']}
for tag, items in TAGS.items():
    path = D + f'minecraft/tags/items/{tag}.json'
    old = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {'replace': False, 'values': []}
    for key in items:
        if f'charpowers:{key}' not in old['values']:
            old['values'].append(f'charpowers:{key}')
    save(path, old)

# ---- nothing big smashes the stone while the sword's in it
for tag in ('wither_immune', 'dragon_immune'):
    save(D + f'minecraft/tags/blocks/{tag}.json', {'replace': False, 'values': ['charpowers:sword_in_stone']})

# ---- Kings Rock: in a river biome, rare but findable (one chance every 48 chunks or so)
save(D + 'charpowers/tags/worldgen/biome/has_structure/kings_rock.json', {'replace': False, 'values': ['#minecraft:is_river']})
save(D + 'charpowers/worldgen/structure/kings_rock.json', {'type': 'charpowers:kings_rock', 'biomes': '#charpowers:has_structure/kings_rock', 'step': 'surface_structures', 'spawn_overrides': {}, 'terrain_adaptation': 'none'})
save(D + 'charpowers/worldgen/structure_set/kings_rock.json', {'structures': [{'structure': 'charpowers:kings_rock', 'weight': 1}], 'placement': {'type': 'minecraft:random_spread', 'spacing': 48, 'separation': 18, 'salt': 735120984}})

# ---- Solgo's woods
save(D + 'charpowers/tags/worldgen/biome/solgo_home.json', {'replace': False, 'values': ['minecraft:dark_forest', 'minecraft:old_growth_birch_forest', 'minecraft:old_growth_pine_taiga', 'minecraft:old_growth_spruce_taiga']})


# ---- loot: Solgo's katana goes through the drops event, this is what he always leaves behind
save(D + 'charpowers/loot_tables/entities/solgo.json', {'type': 'minecraft:entity', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:echo_shard', 'functions': [{'function': 'minecraft:set_count', 'count': {'min': 2, 'max': 4}}]}]},
                                                                                                 {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:phantom_membrane', 'functions': [{'function': 'minecraft:set_count', 'count': {'min': 3, 'max': 6}}]}]}]})


# ---- advancements: found them all
def holds(*items):
    return {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': list(items)}]}}


def advancement(name, parent, icon, frame, criterion, hidden=False):
    data = {'display': {'icon': {'item': icon}, 'title': {'translate': f'advancements.charpowers.{name}.title'}, 'description': {'translate': f'advancements.charpowers.{name}.description'}, 'frame': frame, 'show_toast': True, 'announce_to_chat': True, 'hidden': hidden}, 'criteria': {'done': criterion}}
    if parent:
        data['parent'] = f'charpowers:weapons/{parent}'
    else:
        data['display']['background'] = 'minecraft:textures/block/stone.png'
    save(D + f'charpowers/advancements/weapons/{name}.json', data)


advancement('root', None, 'charpowers:fork', 'task', holds('charpowers:fork', 'charpowers:spray_bottle', 'charpowers:flame_hammer', 'charpowers:ice_staff', 'charpowers:golden_kings_long_sword', 'charpowers:dagger_of_darkness', 'charpowers:anima_katana'))
advancement('sword', 'root', 'charpowers:golden_kings_long_sword', 'challenge', holds('charpowers:golden_kings_long_sword'))
advancement('staff', 'root', 'charpowers:ice_staff', 'goal', holds('charpowers:ice_staff'))
advancement('dagger', 'root', 'charpowers:dagger_of_darkness', 'goal', holds('charpowers:dagger_of_darkness'))
advancement('hammer', 'root', 'charpowers:flame_hammer', 'goal', holds('charpowers:flame_hammer'))
advancement('solgo', 'root', 'charpowers:solgo_spawn_egg', 'challenge', {'trigger': 'minecraft:player_killed_entity', 'conditions': {'entity': [{'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'type': 'charpowers:solgo'}}]}})
advancement('katana', 'solgo', 'charpowers:anima_katana', 'challenge', holds('charpowers:anima_katana'))

# ---- names and tooltips
TEXT = {
    'item.charpowers.ice_staff': 'Ice Staff',
    'item.charpowers.ice_staff.about': 'Right-click: throws a shard of ice that freezes what it hits.',
    'item.charpowers.golden_kings_long_sword': "Golden King's Long Sword",
    'item.charpowers.golden_kings_long_sword.about': 'Slash, reverse, slam: keep the beat for a finisher.',
    'item.charpowers.golden_kings_long_sword.about2': 'Mash it and you only get the first three.',
    'item.charpowers.flame_hammer': 'The Flame Hammer',
    'item.charpowers.flame_hammer.about': 'Very slow. Sets what it hits on fire.',
    'item.charpowers.flame_hammer.about2': 'A charged blow on the ground (or crouch and hit a block) throws flames out.',
    'item.charpowers.fork': 'Fork',
    'item.charpowers.fork.about': "It's a fork.",
    'item.charpowers.spray_bottle': 'Spray Bottle',
    'item.charpowers.spray_bottle.empty': 'Empty. Right-click water or lava, or hold a milk bucket in the other hand.',
    'item.charpowers.spray_bottle.holds': '%s: %s of %s sprays',
    'item.charpowers.spray_bottle.how': 'Hold right-click to spray. Crouch to spray yourself.',
    'item.charpowers.spray_bottle.water': 'Water',
    'item.charpowers.spray_bottle.lava': 'Lava',
    'item.charpowers.spray_bottle.milk': 'Milk',
    'item.charpowers.spray_bottle.self': 'You sprayed milk in your own face. It helped.',
    'item.charpowers.dagger_of_darkness': 'Dagger of Darkness',
    'item.charpowers.dagger_of_darkness.about': 'Blinds what it cuts.',
    'item.charpowers.anima_katana': 'Anima Katana',
    'item.charpowers.anima_katana.about': 'Cuts the soul, not the body.',
    'item.charpowers.anima_katana.about2': 'Armour, shields and Resistance do nothing against it.',
    'item.charpowers.sword_in_stone': 'Sword in the Stone',
    'item.charpowers.solgo_spawn_egg': 'Solgo Spawn Egg',
    'entity.charpowers.solgo': 'Solgo',
    'entity.charpowers.solgo.warning': 'Something very tall is standing in the trees.',
    'entity.charpowers.ice_shard': 'Ice Shard',
    'death.attack.charpowers.soul': "%1$s's soul was cut by %2$s",
    'death.attack.charpowers.soul.item': "%1$s's soul was cut by %2$s using %3$s",
    'death.attack.charpowers.soul.player': "%1$s's soul was cut while fighting %2$s",
    'advancements.charpowers.root.title': 'Armed, Technically',
    'advancements.charpowers.root.description': "Get hold of one of Char's weapons. A fork counts.",
    'advancements.charpowers.sword.title': 'Rightful King',
    'advancements.charpowers.sword.description': 'Pull the sword from a King\'s Rock in a river',
    'advancements.charpowers.staff.title': 'Cold Hands',
    'advancements.charpowers.staff.description': "Take the Frost King's staff",
    'advancements.charpowers.dagger.title': 'Lights Out',
    'advancements.charpowers.dagger.description': 'Take a Dark Elf\'s dagger',
    'advancements.charpowers.hammer.title': 'Hot Stuff',
    'advancements.charpowers.hammer.description': 'Make the Flame Hammer',
    'advancements.charpowers.solgo.title': 'Stared Him Down',
    'advancements.charpowers.solgo.description': 'Kill Solgo',
    'advancements.charpowers.katana.title': 'Soul Taker',
    'advancements.charpowers.katana.description': 'Take the Anima Katana from Solgo',
}
lang = json.load(open(LANG, encoding='utf-8'))
lang.update(TEXT)
with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
    json.dump(lang, f, indent=2, ensure_ascii=False)
    f.write('\n')
print('item data made')
