package com.redstonejammer.mixin;

import com.redstonejammer.RedstoneJammerEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RedstoneTorchBlock.class, remap = false)
public abstract class RedstoneTorchBlockMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onTorchTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (RedstoneJammerEventHandler.isJammed(pos)) {
            if (state.hasProperty(RedstoneTorchBlock.LIT) && state.getValue(RedstoneTorchBlock.LIT)) {
                level.setBlock(pos, state.setValue(RedstoneTorchBlock.LIT, false), Block.UPDATE_CLIENTS);
            }
            ci.cancel();
        }
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onTorchNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean isMoving, CallbackInfo ci) {
        if (RedstoneJammerEventHandler.isJammed(pos)) {
            if (state.hasProperty(RedstoneTorchBlock.LIT) && state.getValue(RedstoneTorchBlock.LIT)) {
                level.setBlock(pos, state.setValue(RedstoneTorchBlock.LIT, false), Block.UPDATE_CLIENTS);
            }
            ci.cancel();
        }
    }
}
