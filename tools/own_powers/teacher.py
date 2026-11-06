POWER = {
    'id': 'teacher',
    'name': 'Teacher',
    'icon': 'minecraft:lectern',
    'about': "A joke power. You can teach anyone any power except Teacher. But you can't have any other powers, so you have nothing to teach. It does nothing at all.",
    'how': "Trade 50 villagers all the way up to Master, then crouch and right-click a lectern twice to sign. Every other power you have is put away until you quit (crouch and right-click a lectern twice again).",
    'mastery': 'The bar goes up like the others but does nothing. It goes up while you play, and at 100 you get a message.',
    'quest': 'At mastery 100, stand at a lectern for 3 whole Minecraft days (one hour).',
    'awakening': 'Nothing.',
    'quest_hint': 'Mastery 100: stand at a lectern for 3 whole days. You get nothing.',
    'abilities': [
        {'id': 'teach', 'name': 'Teach', 'desc': 'Teach anyone any power. You have nothing to teach.',
         'icon': 'minecraft:book', 'kind': 'key', 'slot': 0, 'cooldown': 100, 'pos': (-0.5, 0)},
        {'id': 'no_other_powers', 'name': 'No Other Powers', 'desc': "You can't gain any other abilities. That's the rule. Quit at a lectern to get your old ones back.",
         'icon': 'minecraft:barrier', 'kind': 'passive', 'after': 'teach', 'pos': (0.5, 0)},
    ],
}
