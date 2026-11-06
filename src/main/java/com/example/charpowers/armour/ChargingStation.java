package com.example.charpowers.armour;

import com.example.charpowers.armour.model.SuitRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

// To charge the suit you have to get out of it and put it on one of these. It only ticks while it's holding the suit.
// Charging also clears your head.
public final class ChargingStation extends BaseEntityBlock {
    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");

    ChargingStation() {
        super(BlockBehaviour.Properties.of().strength(5f, 1200f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(CHARGING) ? 9 : 0));
        registerDefaultState(stateDefinition.any().setValue(CHARGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(CHARGING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(CHARGING) ? null : createTickerHelper(type, SuitItems.STATION_ENTITY.get(), Entity::tick);
    }

    // Holding the suit: puts it on the station. Empty hand: takes it back. Crouching with it, once awakened: makes it slim.
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof Entity station)) return InteractionResult.PASS;
        ItemStack held = player.getMainHandItem();
        if (player.isCrouching() && held.is(SuitItems.SUIT.get()) && InvincibleArmour.POWER.tryMakeSlim(player, held)) return InteractionResult.CONSUME;
        if (held.is(SuitItems.SUIT.get()) && station.chest.isEmpty()) {
            station.chest = held.split(1);
            station.changed(level, pos, state);
            InvincibleArmour.POWER.rememberStation(player, level, pos);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && !station.chest.isEmpty()) {
            player.setItemInHand(hand, station.chest);
            station.chest = ItemStack.EMPTY;
            station.changed(level, pos, state);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof Entity station && !station.chest.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), station.chest);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    public static final class Entity extends BlockEntity {
        ItemStack chest = ItemStack.EMPTY;
        private int ticks;

        public Entity(BlockPos pos, BlockState state) {
            super(SuitItems.STATION_ENTITY.get(), pos, state);
        }

        void changed(Level level, BlockPos pos, BlockState state) {
            setChanged();
            level.setBlock(pos, state.setValue(CHARGING, !chest.isEmpty()), Block.UPDATE_ALL);
        }

        ItemStack takeChest(Level level, BlockPos pos, BlockState state) {
            ItemStack out = chest;
            chest = ItemStack.EMPTY;
            changed(level, pos, state);
            return out;
        }

        // 1% every 3 seconds: a full charge takes 5 minutes.
        static void tick(Level level, BlockPos pos, BlockState state, Entity e) {
            if (e.chest.isEmpty() || ++e.ticks < SuitRules.CHARGE_EVERY_TICKS) return;
            e.ticks = 0;
            SuitItems.setBattery(e.chest, SuitItems.battery(e.chest) + 1);
            SuitItems.setWorn(e.chest, 0);
            e.setChanged();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!chest.isEmpty()) tag.put("chest", chest.save(new CompoundTag()));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            chest = tag.contains("chest") ? ItemStack.of(tag.getCompound("chest")) : ItemStack.EMPTY;
        }
    }
}
