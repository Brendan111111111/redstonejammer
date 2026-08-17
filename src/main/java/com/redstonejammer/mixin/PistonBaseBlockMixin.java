package com.redstonejammer.mixin;

import com.redstonejammer.RedstoneJammerEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PistonBaseBlock.class, remap = false)
public abstract class PistonBaseBlockMixin {

    @Inject(method = "checkIfExtend", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onCheckIfExtend(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (RedstoneJammerEventHandler.isJammed(pos)) {
            if (state.hasProperty(PistonBaseBlock.EXTENDED) && state.getValue(PistonBaseBlock.EXTENDED)) {
                Direction facing = state.getValue(PistonBaseBlock.FACING);
                level.blockEvent(pos, (PistonBaseBlock)(Object)this, 1, facing.get3DDataValue());
            }
            ci.cancel();
        }
    }

    @Inject(method = "getNeighborSignal", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onGetNeighborSignal(SignalGetter level, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (RedstoneJammerEventHandler.isJammed(pos)) {
            cir.setReturnValue(false);
        }
    }
}
