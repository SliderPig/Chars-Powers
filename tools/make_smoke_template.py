# Writes the empty structure the headless tests stand in. Run from the charpowers folder.
import gzip
import os
import struct


def s(text):
    b = text.encode()
    return struct.pack('>H', len(b)) + b


def tag(kind, name, payload):
    return bytes([kind]) + s(name) + payload


def ints(*values):
    return bytes([3]) + struct.pack('>i', len(values)) + b''.join(struct.pack('>i', v) for v in values)


air = tag(8, 'Name', s('minecraft:air')) + b'\0'
root = (tag(3, 'DataVersion', struct.pack('>i', 3465))
        + tag(9, 'size', ints(5, 5, 5))
        + tag(9, 'palette', bytes([10]) + struct.pack('>i', 1) + air)
        + tag(9, 'blocks', bytes([0]) + struct.pack('>i', 0))
        + tag(9, 'entities', bytes([0]) + struct.pack('>i', 0)))
data = bytes([10]) + s('') + root + b'\0'
out = 'src/smoke/resources/data/charpowers/structures/empty.nbt'
os.makedirs(os.path.dirname(out), exist_ok=True)
with gzip.open(out, 'wb') as f:
    f.write(data)
print('wrote', out)
