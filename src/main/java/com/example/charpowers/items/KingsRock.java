package com.example.charpowers.items;

import com.example.charpowers.items.model.RockShape;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;

// The King's Rock: a giant boulder sitting in a river, with the Golden King's Long Sword stuck in its top. Which biomes and how rare it is live in
// data/charpowers/worldgen, so /locate structure charpowers:kings_rock finds it. Rivers are thin, so each place it's allowed looks a little way around itself for one.
public final class KingsRock extends Structure {
    public static final Codec<KingsRock> CODEC = simpleCodec(KingsRock::new);
    private static final int LOOK_CHUNKS = 5;

    private KingsRock(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return CharItems.KINGS_ROCK.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
        ChunkPos chunk = c.chunkPos();
        int sea = c.chunkGenerator().getSeaLevel();
        for (int ring = 0; ring <= LOOK_CHUNKS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = chunk.getBlockX(8) + dx * 16, z = chunk.getBlockZ(8) + dz * 16;
                    if (!c.validBiome().test(c.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(sea), QuartPos.fromBlock(z), c.randomState().sampler()))) continue;
                    int floor = c.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState());
                    if (floor < sea - 16 || floor > sea + 12) continue;
                    long seed = c.seed() ^ (x * 341873128712L + z * 132897987541L);
                    return Optional.of(new GenerationStub(new BlockPos(x, sea, z), b -> b.addPiece(new Piece(x, floor, z, seed))));
                }
            }
        }
        return Optional.empty();
    }

    public static final class Piece extends StructurePiece {
        private static final int BELOW = 6;
        private final int x, y, z;
        private final long seed;

        Piece(int x, int y, int z, long seed) {
            super(CharItems.KINGS_ROCK_PIECE.get(), 0, box(x, y, z, seed));
            this.x = x;
            this.y = y;
            this.z = z;
            this.seed = seed;
        }

        public Piece(CompoundTag tag) {
            super(CharItems.KINGS_ROCK_PIECE.get(), tag);
            x = tag.getInt("cx");
            y = tag.getInt("cy");
            z = tag.getInt("cz");
            seed = tag.getLong("seed");
        }

        private static BoundingBox box(int x, int y, int z, long seed) {
            RockShape rock = new RockShape(seed);
            int r = rock.reach() + 1;
            return new BoundingBox(x - r, y - BELOW, z - r, x + r, y + rock.top() + 4, z + r);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext c, CompoundTag tag) {
            tag.putInt("cx", x);
            tag.putInt("cy", y);
            tag.putInt("cz", z);
            tag.putLong("seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot) {
            place(level, box, x, y, z, seed);
        }

        // Puts down whatever part of the rock is inside this box.
        public static void place(WorldGenLevel level, BoundingBox box, int x, int y, int z, long seed) {
            RockShape rock = new RockShape(seed);
            int r = rock.reach();
            int[] peak = rock.peak();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int wx = x + dx, wz = z + dz;
                    if (wx < box.minX() || wx > box.maxX() || wz < box.minZ() || wz > box.maxZ()) continue;
                    for (int dy = -BELOW; dy <= rock.top(); dy++) {
                        RockShape.Kind kind = rock.kind(dx, dy, dz);
                        if (kind == RockShape.Kind.AIR) continue;
                        p.set(wx, y + dy, wz);
                        if (p.getY() < level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight() || !box.isInside(p)) continue;
                        level.setBlock(p, state(kind), 2);
                        if (kind == RockShape.Kind.MOSS && dy > 0 && !rock.solid(dx, dy + 1, dz)) {
                            int bits = (dx * 31 + dz * 17 + dy * 7) & 15;
                            boolean swordSpot = dx == peak[0] && dz == peak[2];
                            if (bits < 3 && !swordSpot) {
                                p.move(0, 1, 0);
                                if (box.isInside(p) && level.getBlockState(p).isAir()) level.setBlock(p, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                                p.move(0, -1, 0);
                            }
                        }
                    }
                }
            }
            p.set(x + peak[0], y + peak[1] + 1, z + peak[2]);
            if (box.isInside(p)) level.setBlock(p, CharItems.SWORD_STONE.get().defaultBlockState(), 2);
        }

        public static BlockPos swordSpot(int x, int y, int z, long seed) {
            int[] peak = new RockShape(seed).peak();
            return new BlockPos(x + peak[0], y + peak[1] + 1, z + peak[2]);
        }

        private static BlockState state(RockShape.Kind kind) {
            return switch (kind) {
                case ANDESITE -> Blocks.ANDESITE.defaultBlockState();
                case TUFF -> Blocks.TUFF.defaultBlockState();
                case COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
                case MOSSY -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                case MOSS -> Blocks.MOSS_BLOCK.defaultBlockState();
                case GOLD -> Blocks.GOLD_ORE.defaultBlockState();
                case RAW_GOLD -> Blocks.RAW_GOLD_BLOCK.defaultBlockState();
                default -> Blocks.STONE.defaultBlockState();
            };
        }
    }
}
