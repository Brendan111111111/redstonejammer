package com.redstonejammer.client.gui;

import com.redstonejammer.menu.StealthRemoteMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StealthRemoteScreen extends AbstractContainerScreen<StealthRemoteMenu> {

    public StealthRemoteScreen(StealthRemoteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 120);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Button 1: Cycle Selected Suppressor
        this.addRenderableWidget(Button.builder(Component.literal("Cycle Active Suppressor"), b -> sendButton(1))
            .bounds(x + 15, y + 28, 200, 20).build());

        // Button 2: Fire Pulse on Active
        this.addRenderableWidget(Button.builder(Component.literal("Fire Active Pulse"), b -> sendButton(2))
            .bounds(x + 15, y + 54, 200, 20).build());

        // Button 3: Mass Overload Pulse
        this.addRenderableWidget(Button.builder(Component.literal("Mass Overload (All)"), b -> sendButton(3))
            .bounds(x + 15, y + 80, 200, 20).build());
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2A1B22);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 22, 0xFF170D12);
        guiGraphics.fill(x + 6, y + 24, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xFF1E1117);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, Component.literal("Stealth Remote Control Network"), 10, 7, 0xFFFF5555, false);
    }
}
