# The icons for House Builder and the picker's little black cat: 32x32 pixel art, plum and pink. Run from the charpowers folder.
#   python tools/make_house_art.py
import os

from PIL import Image

ICONS = 'src/main/resources/assets/charpowers/textures/icons'
GUI = 'src/main/resources/assets/charpowers/textures/gui/houses'
PINK, ROSE, PLUM, DARK, CREAM, WOOD, GLASS, STONE, STONE2, BLACK, GREEN = (
    (255, 168, 216, 255), (214, 92, 150, 255), (88, 48, 104, 255), (45, 26, 52, 255), (244, 232, 214, 255),
    (122, 84, 52, 255), (170, 220, 240, 255), (150, 148, 156, 255), (112, 110, 120, 255), (24, 20, 28, 255), (110, 170, 90, 255))


def rect(img, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            img.putpixel((x, y), c)


def house(img, roof=PINK, shade=ROSE):
    # a little gabled house, with its roof stepping up like real stairs
    rect(img, 8, 15, 23, 27, CREAM)
    rect(img, 8, 15, 8, 27, WOOD)
    rect(img, 23, 15, 23, 27, WOOD)
    rect(img, 8, 27, 23, 27, WOOD)
    rect(img, 8, 20, 23, 20, WOOD)
    rect(img, 14, 21, 17, 27, WOOD)
    rect(img, 16, 24, 16, 24, GLASS)
    rect(img, 10, 22, 12, 24, GLASS)
    rect(img, 19, 22, 21, 24, GLASS)
    for i in range(10):
        rect(img, 4 + i, 15 - i, 5 + i, 16 - i, roof)
        rect(img, 26 - i, 15 - i, 27 - i, 16 - i, shade)
    for i in range(9):
        rect(img, 6 + i, 15 - i, 25 - i, 15 - i, CREAM)
    rect(img, 15, 9, 16, 11, GLASS)
    rect(img, 21, 4, 23, 9, PLUM)


def power_icon():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    house(img)
    rect(img, 2, 28, 29, 29, GREEN)
    img.save(os.path.join(ICONS, 'house_builder.png'))


def build_icon():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    house(img, PINK, ROSE)
    # a ghost corner and a sparkle: it's going up
    for x, y in ((2, 4), (3, 3), (3, 5), (4, 4), (27, 21), (28, 20), (28, 22), (29, 21)):
        img.putpixel((x, y), PINK)
    rect(img, 2, 28, 29, 29, GREEN)
    img.save(os.path.join(ICONS, 'build_house.png'))


def castle_icon():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    rect(img, 4, 16, 27, 28, STONE)
    rect(img, 4, 8, 10, 28, STONE2)
    rect(img, 21, 8, 27, 28, STONE2)
    for x in (4, 7, 10, 21, 24, 27):
        rect(img, x, 5, x, 7, STONE2)
    for x in (12, 15, 18):
        rect(img, x, 13, x + 1, 15, STONE)
    rect(img, 13, 21, 18, 28, DARK)
    rect(img, 14, 20, 17, 20, DARK)
    rect(img, 6, 12, 8, 15, PINK)
    rect(img, 23, 12, 25, 15, PINK)
    # the magnifying glass that's judging it
    for x, y in ((2, 22), (3, 21), (4, 20), (5, 19), (6, 19), (7, 20), (8, 21), (8, 22), (8, 23), (7, 24), (6, 25), (5, 25), (4, 25), (3, 24), (2, 23)):
        img.putpixel((x, y), CREAM)
    rect(img, 8, 25, 9, 26, WOOD)
    rect(img, 10, 27, 11, 28, WOOD)
    img.save(os.path.join(ICONS, 'inspect_castle.png'))


def brick_icon():
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    bricks = (178, 92, 76, 255)
    mortar = (226, 208, 196, 255)
    rect(img, 5, 7, 26, 25, mortar)
    for row, y in enumerate(range(8, 25, 4)):
        off = 0 if row % 2 else 4
        for x in range(5 - off, 27, 8):
            rect(img, max(5, x), y, min(26, x + 6), y + 2, bricks)
    rect(img, 12, 14, 18, 16, PINK)
    img.save(os.path.join(ICONS, 'bricklayer.png'))


def cat():
    # a black cat sitting on the corner of the picker
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    rect(img, 11, 13, 22, 29, BLACK)
    rect(img, 9, 19, 24, 29, BLACK)
    rect(img, 10, 4, 22, 13, BLACK)
    for x, y in ((10, 3), (11, 2), (10, 2), (22, 3), (21, 2), (22, 2), (10, 1), (22, 1)):
        img.putpixel((x, y), BLACK)
    rect(img, 12, 7, 14, 8, PINK)
    rect(img, 18, 7, 20, 8, PINK)
    img.putpixel((13, 7), BLACK)
    img.putpixel((19, 7), BLACK)
    rect(img, 16, 10, 16, 10, ROSE)
    for x, y in ((24, 28), (25, 27), (26, 26), (27, 25), (27, 24), (28, 23), (28, 22), (27, 21)):
        rect(img, x, y, x + 1, y + 1, BLACK)
    img.save(os.path.join(GUI, 'cat.png'))


def main():
    os.makedirs(ICONS, exist_ok=True)
    os.makedirs(GUI, exist_ok=True)
    power_icon()
    build_icon()
    castle_icon()
    brick_icon()
    cat()


if __name__ == '__main__':
    main()
