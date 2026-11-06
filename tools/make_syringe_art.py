# The Power Syringe's icons: empty, and filled with glowing pink. Drawn big and shrunk to 32x32 so they're smooth.
# Also its models and recipe. From the charpowers folder:  python tools/make_syringe_art.py
import json

from PIL import Image, ImageDraw, ImageFilter

A = 'src/main/resources/assets/charpowers/'
D = 'src/main/resources/data/charpowers/'
BIG = 512
STEEL, LIGHT, DARK, GLASS = (150, 158, 168, 255), (232, 238, 244, 255), (54, 58, 66, 255), (205, 228, 245, 90)


def syringe(filled):
    im = Image.new('RGBA', (BIG, BIG), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx = BIG // 2
    half = 74  # the barrel, wide enough to read at item size
    # needle
    d.polygon([(cx - 11, 380), (cx + 11, 380), (cx + 2, 500), (cx - 2, 500)], fill=LIGHT)
    d.line((cx - 5, 384, cx - 1, 490), fill=DARK, width=4)
    d.rounded_rectangle((cx - 34, 340, cx + 34, 386), 10, fill=STEEL, outline=DARK, width=9)
    # plunger, behind the glass
    d.rounded_rectangle((cx - 17, 30, cx + 17, 130), 6, fill=STEEL, outline=DARK, width=9)
    d.rounded_rectangle((cx - 72, 6, cx + 72, 50), 16, fill=LIGHT, outline=DARK, width=10)
    # what's in it
    if filled:
        glow = Image.new('RGBA', (BIG, BIG), (0, 0, 0, 0))
        ImageDraw.Draw(glow).rounded_rectangle((cx - half - 10, 110, cx + half + 10, 356), 20, fill=(255, 70, 180, 255))
        im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(24)))
    glass = Image.new('RGBA', (BIG, BIG), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glass)
    gd.rounded_rectangle((cx - half, 120, cx + half, 346), 18, fill=(205, 228, 245, 110))
    if filled:
        for y in range(136, 340):
            t = (y - 136) / 204
            gd.line((cx - half + 8, y, cx + half - 8, y), fill=(255, int(150 - 70 * t), int(220 - 40 * t), 255))
        for x, y, r in ((cx - 26, 200, 13), (cx + 28, 262, 10), (cx - 8, 306, 8), (cx + 22, 176, 7)):
            gd.ellipse((x - r, y - r, x + r, y + r), fill=(255, 238, 250, 255))
    im.alpha_composite(glass)
    # the barrel's edge, its shine and marks, on top
    top = Image.new('RGBA', (BIG, BIG), (0, 0, 0, 0))
    td = ImageDraw.Draw(top)
    td.rounded_rectangle((cx - half, 120, cx + half, 346), 18, outline=DARK, width=11)
    td.rounded_rectangle((cx - half + 14, 136, cx - half + 34, 330), 8, fill=(255, 255, 255, 150))
    for y in (180, 226, 272, 316):
        td.line((cx + half - 40, y, cx + half - 12, y), fill=DARK, width=8)
    td.rounded_rectangle((cx - half - 22, 104, cx + half + 22, 140), 14, fill=LIGHT, outline=DARK, width=10)
    td.line((cx - half - 6, 114, cx + half, 114), fill=(255, 255, 255, 255), width=6)
    im.alpha_composite(top)
    return im.rotate(-45, resample=Image.BICUBIC, center=(cx, cx))


for name, filled in (('power_syringe', False), ('power_syringe_filled', True)):
    syringe(filled).resize((32, 32), Image.LANCZOS).save(A + f'textures/item/{name}.png', optimize=True)

model = {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'charpowers:item/power_syringe'},
         'overrides': [{'predicate': {'charpowers:filled': 1}, 'model': 'charpowers:item/power_syringe_filled'}]}
with open(A + 'models/item/power_syringe.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump(model, f, indent=2)
with open(A + 'models/item/power_syringe_filled.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'charpowers:item/power_syringe_filled'}}, f, indent=2)
recipe = {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': [' A ', 'IPI', ' G '],
          'key': {'A': {'item': 'minecraft:amethyst_shard'}, 'I': {'item': 'minecraft:iron_ingot'}, 'P': {'item': 'minecraft:ender_pearl'}, 'G': {'item': 'minecraft:glass_bottle'}},
          'result': {'item': 'charpowers:power_syringe'}}
with open(D + 'recipes/power_syringe.json', 'w', encoding='utf-8', newline='\n') as f:
    json.dump(recipe, f, indent=2)

# a big preview to check it by
sheet = Image.new('RGB', (560, 280), (60, 60, 66))
for i, filled in enumerate((False, True)):
    icon = syringe(filled).resize((32, 32), Image.LANCZOS).resize((256, 256), Image.NEAREST)
    sheet.paste(icon, (12 + i * 272, 12), icon)
sheet.save('docs/previews/power_syringe.png')
print('syringe made')
