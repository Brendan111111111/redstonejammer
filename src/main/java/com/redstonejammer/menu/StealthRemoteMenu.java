package com.redstonejammer.menu;

import com.redstonejammer.item.StealthRemoteItem;
import com.redstonejammer.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class StealthRemoteMenu extends AbstractContainerMenu {

    public StealthRemoteMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.STEALTH_REMOTE_MENU.get(), containerId);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 1) { // Cycle Selected Suppressor
            StealthRemoteItem.cycleSelectedSuppressor(player);
            return true;
        } else if (id == 2) { // Fire Pulse on Active
            StealthRemoteItem.triggerActiveSuppressor(player);
            return true;
        } else if (id == 3) { // Mass Overload Pulse
            StealthRemoteItem.triggerAllSuppressors(player);
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
