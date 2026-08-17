package com.redstonejammer.mixin;

import com.redstonejammer.RedstoneJammerEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBehaviour.BlockStateBase.class, remap = false)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onGetSignal(BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (RedstoneJammerEventHandler.isJammed(pos)) {
            cir.setReturnValue(0);
            return;
        }
        if (direction != null && RedstoneJammerEventHandler.isJammed(pos.relative(direction.getOpposite()))) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onGetDirectSignal(BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (RedstoneJammerEventHandler.isJammed(pos)) {
            cir.setReturnValue(0);
            return;
        }
        if (direction != null && RedstoneJammerEventHandler.isJammed(pos.relative(direction.getOpposite()))) {
            cir.setReturnValue(0);
        }
    }
}
