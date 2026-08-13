package com.redstonejammer.item;

import com.redstonejammer.RedstoneJammerEventHandler;
import com.redstonejammer.block.FluxProjectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisruptorWandItem extends Item {

    private static final Map<UUID, BlockPos> BOUND_PROJECTORS = new ConcurrentHashMap<>();

    public DisruptorWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        boolean isSneaking = player.isShiftKeyDown();
        BlockEntity be = level.getBlockEntity(pos);

        // 1. Right-clicking a Flux Inversion Projector selects/binds it
        if (be instanceof FluxProjectorBlockEntity) {
            if (!level.isClientSide()) {
                BOUND_PROJECTORS.put(player.getUUID(), pos.immutable());
                player.sendSystemMessage(Component.literal("§b[Resonance Wand] Selected Flux Inversion Projector at (" + pos.toShortString() + ")"));
                player.sendSystemMessage(Component.literal("§7Right-click a target block to link it, or shift-right-click a target block to unlink it."));
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Interacting with a target block
        if (!level.isClientSide()) {
            BlockPos boundProjectorPos = BOUND_PROJECTORS.get(player.getUUID());

            if (isSneaking) {
                // Shift-right-click on target to unlink
                boolean unlinked = false;
                if (boundProjectorPos != null) {
                    BlockEntity projBe = level.getBlockEntity(boundProjectorPos);
                    if (projBe instanceof FluxProjectorBlockEntity projector) {
                        projector.unlinkTarget(pos);
                        unlinked = true;
                    }
                }
                
                // Fallback scan in 32-block radius to unlink from any projector targeting this block
                if (!unlinked) {
                    int radius = 32;
                    for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
                        BlockEntity checkBe = level.getBlockEntity(checkPos);
                        if (checkBe instanceof FluxProjectorBlockEntity projector) {
                            if (projector.getLinkedTargets().contains(pos.immutable())) {
                                projector.unlinkTarget(pos);
                            }
                        }
                    }
                }

                RedstoneJammerEventHandler.unjamBlock(level, pos);
                player.sendSystemMessage(Component.literal("§e[Resonance Wand] Unlinked mechanism at (" + pos.toShortString() + ")"));
            } else {
                // Right-click on target to link to bound projector
                if (boundProjectorPos == null) {
                    player.sendSystemMessage(Component.literal("§e[Resonance Wand] Select a Flux Inversion Projector first by right-clicking it!"));
                    return InteractionResult.SUCCESS;
                }

                BlockEntity projBe = level.getBlockEntity(boundProjectorPos);
                if (projBe instanceof FluxProjectorBlockEntity projector) {
                    projector.linkTarget(pos);
                    player.sendSystemMessage(Component.literal("§a[Resonance Wand] Linked target (" + pos.toShortString() + ") to Flux Inversion Projector at (" + boundProjectorPos.toShortString() + ")!"));
                    if (projector.isPowered()) {
                        player.sendSystemMessage(Component.literal("§c[Flux Projector] Active redstone power detected - jamming target immediately!"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§c[Resonance Wand] Bound Projector at (" + boundProjectorPos.toShortString() + ") no longer exists!"));
                    BOUND_PROJECTORS.remove(player.getUUID());
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.literal("§b⚡ Resonance Flux Visualizer"));
        tooltipComponents.accept(Component.literal("§7Hold in mainhand/offhand to reveal invisible flux wires."));
        tooltipComponents.accept(Component.literal("§eRight-click Flux Projector to select unit."));
        tooltipComponents.accept(Component.literal("§eRight-click target block to link flux wire."));
        tooltipComponents.accept(Component.literal("§cShift + Right-click target block to unlink flux wire."));
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);
    }
}
