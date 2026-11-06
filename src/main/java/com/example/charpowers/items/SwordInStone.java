package com.example.charpowers.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import javax.annotation.Nullable;

// The stone on top of a King's Rock with the Golden King's Long Sword stuck in it. Anyone can pull it free, once, and then only the slit is left. Until
// then nothing can break the stone, so the sword can't be lost.
public class SwordInStone extends Block implements EntityBlock {
    public static final BooleanProperty PULLED = BooleanProperty.create("pulled");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 5, 15);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.82f, 0.25f), 0.8f);

    public SwordInStone() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1f, 3600000f).noLootTable().sound(SoundType.STONE).pushReaction(PushReaction.BLOCK).noOcclusion()
                .lightLevel(s -> s.getValue(PULLED) ? 0 : 7));
        registerDefaultState(stateDefinition.any().setValue(PULLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PULLED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // once the sword's gone it's just a stone
        return state.getValue(PULLED) ? 0.06f : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(PULLED)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        pull(level, pos, state, player);
        return InteractionResult.CONSUME;
    }

    public static void pull(Level level, BlockPos pos, BlockState state, Player player) {
        level.setBlock(pos, state.setValue(PULLED, true), 3);
        ItemStack sword = new ItemStack(CharItems.LONG_SWORD.get());
        if (!player.addItem(sword)) {
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, sword);
            item.setNoPickUpDelay();
            level.addFreshEntity(item);
        }
        level.playSound(null, pos, CharItems.sound("sword_pull"), SoundSource.BLOCKS, 1.2f, 1f);
        if (level instanceof ServerLevel server) {
            Foes.particles(server, GOLD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.9, 0.4, 0.12);
            Foes.particles(server, ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 0.2, 0.6, 0.2, 0.12);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(PULLED)) return;
        if (r.nextInt(3) == 0) level.addParticle(GOLD, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.6, pos.getY() + 0.4 + r.nextDouble() * 1.9, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
        if (r.nextInt(14) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, (r.nextDouble() - 0.5) * 0.02, 0.02, (r.nextDouble() - 0.5) * 0.02);
        // a soft ringing now and then, only if you're close
        if (r.nextInt(120) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, CharItems.sound("sword_ring"), SoundSource.BLOCKS, 0.5f, 1f, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    public static final class Entity extends BlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(CharItems.SWORD_STONE_ENTITY.get(), pos, state);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(1).expandTowards(0, 3, 0);
        }
    }
}
