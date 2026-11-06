# Writes Char's ten house designs as vanilla structure files: data/charpowers/structures/houses/<id>.nbt
# Run from the charpowers folder:  python tools/make_houses.py      (then tools/preview_houses.py for the picker's pictures)
# The designs are original, written block by block in tools/houses/. The front of every house faces +z (south).
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from houses import awakened_set, base_set  # noqa: E402
from houses.check import validate  # noqa: E402
from houses.check import validate  # noqa: E402
from houses.lib import write_structure  # noqa: E402

OUT = 'src/main/resources/data/charpowers/structures/houses'


def all_houses():
    return [make().finish() for make in base_set.ALL + awakened_set.ALL]


def main():
    os.makedirs(OUT, exist_ok=True)
    listing = []
    failed = 0
    for h in all_houses():
        errors = validate(h)
        for e in errors[:12]:
            print(f'  {h.id}: {e}')
        if len(errors) > 12:
            print(f'  {h.id}: ...and {len(errors) - 12} more')
        failed += len(errors)
        size, blocks, states = write_structure(h, os.path.join(OUT, h.id + '.nbt'))
        listing.append({'id': h.id, 'name': h.name, 'level': h.level, 'awakened': h.awakened})
        print(f'{h.id:20} {size[0]:3} x {size[2]:3} x {size[1]:3} high  {blocks:6} blocks  {states:4} states')
    with open(os.path.join(OUT, 'catalogue.json'), 'w', encoding='utf-8') as f:
        json.dump(listing, f, indent=1)
    if failed:
        print(f'{failed} problems: these houses are not good enough yet')
        sys.exit(1)
    if failed:
        print(f'{failed} problems: these houses are not good enough yet')
        sys.exit(1)


if __name__ == '__main__':
    main()
