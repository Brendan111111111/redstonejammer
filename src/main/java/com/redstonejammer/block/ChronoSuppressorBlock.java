package com.redstonejammer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ChronoSuppressorBlock extends Block implements EntityBlock {

    public ChronoSuppressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && stack.has(DataComponents.CUSTOM_NAME)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChronoSuppressorBlockEntity suppressor) {
                suppressor.setCustomName(stack.getHoverName().getString());
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty()) {
            if (stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME)) {
                if (!level.isClientSide()) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ChronoSuppressorBlockEntity suppressor) {
                        String name = stack.getHoverName().getString();
                        suppressor.setCustomName(name);
                        player.sendSystemMessage(Component.literal("§a[Chrono Suppressor] Named unit: '" + name + "'!"));
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Allow StealthRemoteItem and ResonanceWandItem to execute item useOn!
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChronoSuppressorBlockEntity suppressor) {
                player.openMenu(suppressor, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : (lvl, p, st, be) -> {
            if (be instanceof ChronoSuppressorBlockEntity suppressor) {
                suppressor.tick(lvl, p, st);
            }
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChronoSuppressorBlockEntity(pos, state);
    }
}
