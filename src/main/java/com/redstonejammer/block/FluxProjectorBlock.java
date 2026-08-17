package com.redstonejammer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

public class FluxProjectorBlock extends Block implements EntityBlock {

    public FluxProjectorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluxProjectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (lvl, p, st, be) -> {
            if (be instanceof FluxProjectorBlockEntity projector) {
                if (lvl.isClientSide()) {
                    FluxProjectorBlockEntity.clientTick(lvl, p, st, projector);
                } else {
                    FluxProjectorBlockEntity.serverTick(lvl, p, st, projector);
                }
            }
        };
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluxProjectorBlockEntity projector) {
                projector.setPowered(level.hasNeighborSignal(pos));
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, isMoving);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FluxProjectorBlockEntity projector) {
                boolean hasPower = level.hasNeighborSignal(pos);
                projector.setPowered(hasPower);
            }
        }
    }
}
