package com.redstonejammer.menu;

import com.redstonejammer.block.ChronoSuppressorBlockEntity;
import com.redstonejammer.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ChronoSuppressorMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ChronoSuppressorBlockEntity blockEntity;
    private final ContainerData data;

    public ChronoSuppressorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, new SimpleContainerData(2));
    }

    public ChronoSuppressorMenu(int containerId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(ModMenus.CHRONO_SUPPRESSOR_MENU.get(), containerId);
        this.pos = pos;
        this.data = data;
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ChronoSuppressorBlockEntity suppressor) {
            this.blockEntity = suppressor;
            if (!playerInventory.player.level().isClientSide()) {
                this.data.set(0, suppressor.getDurationSeconds());
                this.data.set(1, suppressor.getPulseRadius());
            }
        } else {
            this.blockEntity = null;
        }
        this.addDataSlots(data);
    }

    public ChronoSuppressorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getDurationSeconds() {
        return this.data.get(0);
    }

    public int getPulseRadius() {
        return this.data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity != null) {
            if (id == 1) { // Increase Duration (+5s)
                int d = blockEntity.getDurationSeconds() + 5;
                if (d > 120) d = 5;
                blockEntity.setDurationSeconds(d);
                this.data.set(0, d);
            } else if (id == 2) { // Decrease Duration (-5s)
                int d = blockEntity.getDurationSeconds() - 5;
                if (d < 1) d = 120;
                blockEntity.setDurationSeconds(d);
                this.data.set(0, d);
            } else if (id == 3) { // Increase Radius (+1b)
                int r = blockEntity.getPulseRadius() + 1;
                if (r > 16) r = 1;
                blockEntity.setPulseRadius(r);
                this.data.set(1, r);
            } else if (id == 4) { // Decrease Radius (-1b)
                int r = blockEntity.getPulseRadius() - 1;
                if (r < 1) r = 16;
                blockEntity.setPulseRadius(r);
                this.data.set(1, r);
            } else if (id == 5) { // Trigger Pulse NOW
                blockEntity.triggerPulse(player);
            } else if (id == 6) { // Cycle Preset Unit Name
                blockEntity.cyclePresetName();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§a[Chrono Suppressor] Unit renamed to: '" + blockEntity.getCustomName() + "'"));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
