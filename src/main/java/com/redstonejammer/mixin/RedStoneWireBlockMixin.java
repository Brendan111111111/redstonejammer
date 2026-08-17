package com.redstonejammer.mixin;

import com.redstonejammer.RedstoneJammerEventHandler;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RedStoneWireBlock.class, remap = false)
public abstract class RedStoneWireBlockMixin {

    @Inject(method = "getWireSignal", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void redstonejammer$onGetWireSignal(BlockState state, CallbackInfoReturnable<Integer> cir) {
        // Redstone wire power check
    }
}
