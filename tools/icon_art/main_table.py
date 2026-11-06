# What each main power's and ability's icon shows, picked after reading its description in tools/own_powers/.
from .glyphs4 import *

# Invincible Armour's and the House Builder's icons are drawn separately.
SKIP_MAIN = {'invincible_armour', 'call_suit', 'throw_mob', 'module_shield', 'module_scanner', 'module_strength',
             'module_speed', 'going_crazy', 'slim_suit', 'house_builder'}

HOLO = CYAN
HOLOBODY = dict(c=CYAN, legs=BLUE, skin=ICE)
RING = dict(c=LIME)

M = {
    # ---------------------------------------------------------------- Book Keeping
    'book_keeping': lambda a: (book(a, -2, 0, .92), clock(a, 15, 14, .54)),
    'open_book': lambda a: (open_book(a, 0, 0, 1, c=PURPLE),),
    'book_holds_deaths': lambda a: (book(a, -2, 0, .92, c=CAT), skull(a, 11, 11, .66)),
    'no_dupes': lambda a: (gem(a, -3, -1, .7), gem(a, 12, 8, .7), no_sign(a, 11, 7, .72)),
    'page_2': lambda a: (book_page(a, 0, 0, 1, n='2'),),
    'page_3': lambda a: (book_page(a, 0, 0, 1, n='3'),),
    'page_4': lambda a: (book_page(a, 0, 0, 1, n='4'),),
    'page_5': lambda a: (book_page(a, 0, 0, 1, n='5'),),

    # ---------------------------------------------------------------- Cloning
    'cloning': lambda a: (person(a, 9, 1, .72, c=STEEL, legs=IRON), person(a, -9, 1, .72, c=STEEL, legs=IRON), person(a, 0, 3, .88)),
    'clone': lambda a: (person(a, -7, 1, .94), person(a, 9, 1, .94), sparkle(a, 12, -2, .28)),
    'escaped_death': lambda a: (grave(a, -3, 9, .72), person(a, 7, 0, .92, pose='run')),
    'dog_pile': lambda a: (person(a, -5, 15, .5), person(a, 5, 15, .5, c=ROSE), person(a, 17, 15, .5, c=GREEN), person(a, 0, 5, .5, c=ORANGE), person(a, 12, 5, .5, c=PURPLE), person(a, 6, -5, .5)),
    'mastery_share': lambda a: (person(a, -5, 1, .92), orbs(a, 13, -1, .62), chevrons(a, 15, 17, .42, c=WHITE, r=-90)),
    'clone_level_up': lambda a: (person(a, -5, 1, .94), word(a, text='2', h=21, c=YELLOW, x0=14, y0=5)),
    'power_clone': lambda a: (person(a, -5, 3, .86), star(a, 12, -3, .66, c=VIOLET)),

    # ---------------------------------------------------------------- Death Steal
    'death_steal': lambda a: (hand_grab(a, 1, -4, .8, c=DARK), skull(a, 6, 11, .64, eye=VIOLET[1])),
    'steal_power': lambda a: (star(a, 7, 13, .6, c=VIOLET), hand_grab(a, 1, -5, .8, c=BONE)),
    'peek_one': lambda a: (eye_half(a, 0, 3, 1),),
    'peek_two': lambda a: (eye_half(a, -2, -3, .66), eye_half(a, 11, 12, .66),),
    'steal_twice': lambda a: (hand_grab(a, -2, -4, .78, c=BONE), word(a, text='x2', h=14, c=VIOLET, x0=8, y0=18)),
    'see_all': lambda a: (eye(a, 0, 3, 1, c=GOLD), sparkle(a, -1, -2, .3, c=VIOLET), sparkle(a, 23, -2, .3, c=VIOLET), sparkle(a, 12, 22, .3, c=VIOLET)),
    'keep_theirs': lambda a: (star(a, 10, -2, .68, c=STEEL), star(a, -3, 8, .74, c=VIOLET)),

    # ---------------------------------------------------------------- Future Sight
    'future_sight': lambda a: (crystal_ball(a, 0, 0, 1),),
    'hush': lambda a: (mouth(a, 0, 6, .95, kind='lips'), a.fill(a.R(13.5, 1, 18.5, 30, 2), SKIN)),
    'ore_hints': lambda a: (speech(a, 0, 0, 1), gem(a, 8, 1, .5)),
    'fight_tips': lambda a: (speech(a, 0, 0, 1, c=ROSE), creeper(a, 8, 2, .46)),
    'sense_powers': lambda a: (ear(a, -3, 1, .94), sparkle(a, 14, -2, .6, c=VIOLET)),
    'rank_sense': lambda a: (gem(a, 0, 0, 1, c=PURPLE), word(a, text='A', h=14, c=WHITE, x0=11, y0=7)),
    'ore_glow': lambda a: (burst(a, 0, 0, 1, c=YELLOW, core=None), ore(a, 5, 5, .68, c=GOLD)),
    'ore_count': lambda a: (ore(a, -3, 5, .8), word(a, text='9', h=14, c=YELLOW, x0=20, y0=0)),
    'see_through': lambda a: (zombie(a, 13, 13, .56), eye(a, -3, -3, .78, c=SOUL)),

    # ---------------------------------------------------------------- Game Player
    'game_player': lambda a: (gamepad(a, 0, 3, 1),),
    'status_window': lambda a: (window(a, 0, 0, 1),),
    'gp_dash': lambda a: (boot(a, 7, 1, .84, c=CYAN, sole=NAVY), speed_lines(a, -3, 8, .5, c=CYAN)),
    'own_inventory': lambda a: (holo_grid(a, 0, 0, 1),),
    'gp_step': lambda a: (person(a, 9, 1, .88, **HOLOBODY), sparkle(a, -3, 5, .58, c=CYAN), sparkle(a, 6, 19, .34, c=CYAN)),
    'gp_fear': lambda a: (eye(a, 0, 3, 1, c=RED, lid=SHADOW),),
    'gp_shadow': lambda a: (person(a, 2, 1, 1, c=SHADOW, legs=SHADOW, skin=PURPLE), sparkle(a, 15, 3, .48, c=PURPLE), sparkle(a, -2, 15, .34, c=PURPLE)),
    'gates': lambda a: (gate(a, 0, 0, 1),),
    'daily_quests': lambda a: (checklist(a, 0, 0, 1),),
    'story_chain': lambda a: (open_book(a, -1, -3, .88, c=NAVY), chain(a, 15, 14, .56)),
    'more_dailies_4': lambda a: (checklist(a, -3, 0, .94), word(a, text='4', h=14, c=YELLOW, x0=20, y0=17)),
    'more_dailies_5': lambda a: (checklist(a, -3, 0, .94), word(a, text='5', h=14, c=YELLOW, x0=20, y0=17)),
    'second_story': lambda a: (open_book(a, -1, -3, .88, c=NAVY), word(a, text='2', h=14, c=YELLOW, x0=20, y0=17)),
    'extra_slot': lambda a: (a.fill(a.R(2, 2, 26, 26, 4), NAVY, shade=False), a.flat(a.R(2, 2, 26, 26, 4) & ~a.R(4, 4, 24, 24, 3), CYAN[0]), plus(a, 15, 15, .52, c=GREEN)),
    'player_shop': lambda a: (shop_stall(a, 0, 0, 1),),

    # ---------------------------------------------------------------- Green Lantern
    'green_lantern': lambda a: (lantern_ring(a, 0, 0, 1),),
    'rising_fist': lambda a: (rings(a, 0, 9, 1, c=LIME), fist(a, 4, -2, .76, **RING)),
    'backhand_slap': lambda a: (hand(a, 7, 3, .84, fx=True, **RING), burst(a, -3, 5, .5, c=YELLOW, core=None), chevrons(a, 0, 19, .36, c=LIME, r=-90)),
    'hand_slam': lambda a: (rings(a, 0, 9, 1, c=LIME), hand(a, 4, -3, .76, fy=True, **RING)),
    'hand_throw': lambda a: (hand_grab(a, -3, -3, .72, **RING), arc_path(a, 5, 4, .84, c=LIME)),
    'construct_sword': lambda a: (sword(a, 0, 0, 1, r=45, c=LIME, hilt=FOREST),),
    'launch_swords': lambda a: (sword(a, -2, -8, .72, r=90, c=LIME, hilt=FOREST), sword(a, 8, 4, .72, r=90, c=LIME, hilt=FOREST), sword(a, -3, 14, .72, r=90, c=LIME, hilt=FOREST)),
    'minigun': lambda a: (minigun(a, 0, 1, 1),),
    'turret': lambda a: (turret(a, 0, 0, 1),),
    'bubble': lambda a: (dome(a, 0, 0, 1),),
    'nuke': lambda a: (mushroom_cloud(a, 0, 0, 1, c=LIME),),
    'fist_field': lambda a: (ring_fists(a, 0, 0, 1),),
    'sword_rain': lambda a: (sword(a, -4, -3, .62, r=180, c=LIME, hilt=FOREST), sword(a, 10, -8, .62, r=180, c=LIME, hilt=FOREST), sword(a, 20, 2, .6, r=180, c=LIME, hilt=FOREST), sword(a, 6, 11, .62, r=180, c=LIME, hilt=FOREST)),
    'gl_flight': lambda a: (person(a, 2, -2, .8, pose='jump', c=LIME, legs=FOREST), chevrons(a, 8, 18, .46, c=LIME)),
    'gl_tools': lambda a: (axe(a, 2, 1, .94, c=LIME, fx=True), pickaxe(a, -1, 1, .94, c=LIME, w=FOREST)),
    'suit_off': lambda a: (chest_plate(a, 0, 0, .92, c=LIME), cross(a, 16, 16, .5)),

    # ---------------------------------------------------------------- Hidden Powers
    'hidden_powers': lambda a: (monster_head(a, 0, 0, 1, c=DARK), qmark(a, 8, 8, .5, c=PINK)),

    # ---------------------------------------------------------------- Life Enjoyer
    'life_enjoyer': lambda a: (flower(a, -2, 0, .96, c=PINK), heart(a, 17, 16, .46, c=RED)),
    'vein_miner': lambda a: (pickaxe(a, -3, 3, .88), a.flat(a.L([(23, 4), (28, 13), (22, 22)], 1.4), GOLD), gem(a, 16, -3, .44, c=GOLD), gem(a, 20, 8, .42, c=GOLD), gem(a, 14, 16, .44, c=GOLD)),
    'tree_logger': lambda a: (tree(a, 7, -1, .86), axe(a, -4, 6, .78)),
    'magnet': lambda a: (magnet(a, -2, -2, .76, c=BLUE), apple(a, 14, 12, .42), sword(a, 4, 16, .44, r=45), gem(a, 20, 2, .3)),
    'auto_tool': lambda a: (gear(a, -3, -3, .72), pickaxe(a, 7, 6, .8)),
    'chest_sorter': lambda a: (chest(a, -2, 7, .8), bars(a, 15, -3, .54)),
    'ultimine': lambda a: (axe(a, 2, 1, .94, fx=True), pickaxe(a, -1, 1, .94), sparkle(a, 8, 6, .5, c=PINK)),
    'farming': lambda a: (wheat(a, 0, 0, 1),),
    'comfy': lambda a: (pillow(a, -1, 5, .88), zzz(a, 14, -3, .54, c=PINK)),
    'animal_friend': lambda a: (paw(a, -2, 3, .86, c=PINK), heart(a, 17, -2, .46)),
    'soft_landing': lambda a: (hay(a, -1, 9, .84), person(a, 8, -4, .52, pose='fall')),
    'easy_walking': lambda a: (shoe(a, -2, 9, .8), drumstick(a, 13, -3, .56)),

    # ---------------------------------------------------------------- Mob Control
    'mob_control': lambda a: (puppet(a, 0, 0, 1),),
    'command_mob': lambda a: (pointing(a, -3, 1, .66), zombie(a, 12, 11, .64)),
    'ride_mob': lambda a: (creeper(a, -1, 8, .78), saddle(a, 2, -5, .88)),
    'pet_follower': lambda a: (zombie(a, -2, 6, .82), heart(a, 16, -2, .5, c=PINK)),
    'swap_minds': lambda a: (zombie(a, 13, -2, .6), face(a, -3, 13, .6, c=SKIN), swap_arrows(a, 8, 8, .5, c=VIOLET, r=-45)),
    'mob_peace': lambda a: (zombie(a, -3, 7, .78), flag(a, 15, -3, .62, c=WHITE)),
    'boss_tamer': lambda a: (dragon(a, -2, 5, .86), crown(a, 12, -4, .6)),

    # ---------------------------------------------------------------- Necromancy
    'necromancy': lambda a: (flame(a, 1, -5, .94, c=GREEN, core=LIME), skull(a, 3, 8, .8)),
    'summon_zombies': lambda a: (zombie(a, -3, -1, .62), zombie(a, 13, 1, .6), zombie(a, 5, 13, .64)),
    'summon_skeletons': lambda a: (skull(a, -3, -2, .74), bow(a, 13, 8, .72)),
    'undead_explosion': lambda a: (boom(a, 0, 0, 1, c=LIME, core=YELLOW), skull(a, 8, 8, .52)),
    'chosen_revival': lambda a: (grave(a, -3, 5, .84), hand(a, 11, -3, .64, c=FOREST)),
    'undead_riot': lambda a: (hand(a, -6, 5, .6, c=FOREST), hand(a, 13, 6, .58, c=BONE), hand(a, 3, 8, .62, c=FOREST), a.fill(a.E(16, 28, 15, 3.5), DIRT, deep=True)),
    'point_undead': lambda a: (pointing(a, -3, 8, .78, c=BONE), target(a, 15, -2, .54)),
    'undead_gear': lambda a: (zombie(a, -3, 7, .78), a.fill(a.R(0, 3, 22, 10, 2), IRON, deep=True), sword(a, 10, -1, .72, r=45)),
    'fast_arrows': lambda a: (dart(a, 1, -9, .72, r=90), dart(a, 4, 5, .72, r=90), speed_lines(a, -3, 19, .36), skull(a, -2, -1, .34)),
    'sun_proof': lambda a: (sun(a, 12, -3, .66), umbrella(a, -3, 3, .72, c=DARK), skull(a, 10, 14, .54)),
    'raise_players': lambda a: (grave(a, -3, 8, .72), person(a, 8, -1, .86, pose='cheer', c=SOUL, legs=TEAL)),

    # ---------------------------------------------------------------- Superior Authority
    'superior_authority': lambda a: (crown(a, 0, 1, 1),),
    'stop': lambda a: (stop_sign(a, 0, 0, 1),),
    'no_jump': lambda a: (person(a, 5, 3, .7, pose='jump'), no_sign(a, 0, 0, 1)),
    'freeze': lambda a: (person(a, -3, 0, 1, c=CYAN, legs=BLUE, skin=ICE), snowflake(a, 15, 14, .56)),
    'kneel': lambda a: (person(a, -2, 4, .9, pose='kneel'), crown(a, 16, -3, .5)),
    'look_at_me': lambda a: (eye(a, -3, -5, .62), eye(a, 13, -5, .62), person(a, 6, 11, .62, c=ROSE, legs=NAVY)),
    'attack': lambda a: (target(a, 11, -2, .68), sword(a, -3, 5, .84, r=45, c=RED, hilt=DARK)),
    'die': lambda a: (skull(a, 1, 6, .84, eye=RED[1]), crown(a, 4, -5, .74)),

    # ---------------------------------------------------------------- Teacher
    'teacher': lambda a: (chalkboard(a, 0, 0, 1),),
    'teach': lambda a: (open_book(a, -2, 9, .78), lightbulb(a, 12, -2, .6)),
    'no_other_powers': lambda a: (star(a, 2, 2, .82, c=VIOLET), no_sign(a, 0, 0, 1)),

    # ---------------------------------------------------------------- The Miner
    'the_miner': lambda a: (miner_helmet(a, -2, -2, .84), pickaxe(a, 12, 11, .62)),
    'miner_stairs': lambda a: (stairs(a, 0, 0, 1, c=STONE, fx=True), arrow(a, -1, -1, .5, r=135, c=YELLOW)),
    'mine_3x3': lambda a: (grid_blocks(a, 0, 0, 1, n=3), pickaxe(a, 13, 13, .6)),
    'mine_5x5': lambda a: (grid_blocks(a, 0, 0, 1, n=5), pickaxe(a, 13, 13, .6, c=GOLD)),
    'mine_ball': lambda a: (stone_ball(a, 0, 0, 1),),
    'strip_mine': lambda a: (pit(a, 0, 0, 1),),
    'motherlode': lambda a: (gem(a, 8, -1, .72), gem(a, -3, 11, .56, c=EMERALD), gem(a, 18, 15, .56, c=GOLD), sparkle(a, -1, -1, .34), sparkle(a, 22, 0, .24, c=WHITE)),
    'fist_mining': lambda a: (ore(a, 13, -3, .64, c=DIAMOND), fist(a, -2, 8, .76)),
    'miner_eyes': lambda a: (eye(a, -1, -1, .9, c=YELLOW, lid=STEEL), torch(a, 15, 12, .6)),
    'miner_efficiency': lambda a: (pickaxe(a, -3, 2, .92, c=CYAN), chevrons(a, 15, 15, .52, c=YELLOW)),
    'more_ores': lambda a: (ore(a, -2, 2, .88, c=EMERALD), plus(a, 17, -1, .46)),

    # ---------------------------------------------------------------- Time Control
    'time_control': lambda a: (clock(a, 0, 0, 1, c=VIOLET),),
    'slow_down': lambda a: (clock(a, -3, -3, .74), snail(a, 9, 11, .7)),
    'gotta_move': lambda a: (shoe(a, -1, 9, .84, c=RED, sole=WHITE), bolt(a, 15, -2, .56), speed_lines(a, -3, -1, .4)),
    'hurry_up': lambda a: (gear(a, -3, -2, .84), chevrons(a, 14, 13, .56, c=YELLOW, r=90)),
    'let_time_flow': lambda a: (block(a, -3, 5, .84), hourglass(a, 14, -3, .58)),
    'time_block': lambda a: (block(a, 0, 0, 1, top=PURPLE, left=VIOLET, right=PURPLE), clock(a, 12, 11, .56)),
    'barrage': lambda a: (burst(a, -3, -3, .6, c=YELLOW, core=None), burst(a, 15, -3, .5, c=ORANGE, core=None), burst(a, 16, 13, .56, c=YELLOW, core=None), fist_side(a, -1, 11, .66)),
    'still_moment': lambda a: (stopwatch(a, -1, -1, .96, c=ICE), pause(a, 14, 14, .54, c=VIOLET)),

    # ---------------------------------------------------------------- Translation
    'translation': lambda a: (speech(a, 9, -2, .72, c=PINK, tail='right'), speech(a, -2, 5, .78), paw(a, 2, 5, .42, c=CAT)),
    'understand_mobs': lambda a: (zombie(a, -2, 9, .72), speech(a, 11, -3, .68)),
    'sad_lines': lambda a: (speech(a, 0, 0, 1, c=ICE), drop(a, 9, 1, .42, c=BLUE)),
    'useful_lines': lambda a: (speech(a, 0, 0, 1), lightbulb(a, 9, 1, .44)),
    'choose_voices': lambda a: (mic(a, -3, 2, .86), check(a, 16, 15, .5)),

    # ---------------------------------------------------------------- Vocal Expression
    'vocal_expression': lambda a: (mouth(a, -3, 9, .58, kind='shout'), letter_block(a, 12, -2, .667, ch='A'), letter_block(a, 12, 14, .667, ch='B', c=WOOD)),
    'speak': lambda a: (speech(a, 0, 0, 1, c=STONE), word(a, text='ABC', h=7, c=OUT if False else WHITE, x0=7, y0=9)),
    'hard_words': lambda a: (letter_block(a, -1, 1, 1, ch='H', c=IRON), burst(a, 17, -2, .5, c=ORANGE, core=None)),
    'two_words': lambda a: (letter_block(a, -2, -2, .667, ch='A', c=WOOD), letter_block(a, 11, 12, .667, ch='B', c=STONE)),
    'true_words': lambda a: (letter_block(a, -1, 1, 1, ch='B', c=GOLD, ink=RED), flame(a, 16, -3, .52)),
}

# Drawn but not named after a power or ability: what an empty hidden-power key shows.
EXTRA = {
    'hidden/empty_slot': lambda a: (empty_slot(a, 0, 0, 1),),
}
