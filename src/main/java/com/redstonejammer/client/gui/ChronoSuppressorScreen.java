package com.redstonejammer.client.gui;

import com.redstonejammer.block.ChronoSuppressorBlockEntity;
import com.redstonejammer.menu.ChronoSuppressorMenu;
import com.redstonejammer.network.SetSuppressorNamePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ChronoSuppressorScreen extends AbstractContainerScreen<ChronoSuppressorMenu> {

    private EditBox nameInput;

    public ChronoSuppressorScreen(ChronoSuppressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 140);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // Button 1: Duration -5s
        this.addRenderableWidget(Button.builder(Component.literal("-5s"), b -> sendButton(2))
            .bounds(x + 130, y + 24, 38, 18).build());

        // Button 2: Duration +5s
        this.addRenderableWidget(Button.builder(Component.literal("+5s"), b -> sendButton(1))
            .bounds(x + 174, y + 24, 38, 18).build());

        // Button 3: Radius -1b
        this.addRenderableWidget(Button.builder(Component.literal("-1b"), b -> sendButton(4))
            .bounds(x + 130, y + 46, 38, 18).build());

        // Button 4: Radius +1b
        this.addRenderableWidget(Button.builder(Component.literal("+1b"), b -> sendButton(3))
            .bounds(x + 174, y + 46, 38, 18).build());

        // EditBox: Custom Unit Name Input
        this.nameInput = new EditBox(this.font, x + 14, y + 68, 118, 18, Component.literal("Unit Name"));
        this.nameInput.setMaxLength(28);
        ChronoSuppressorBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            this.nameInput.setValue(be.getCustomName());
        }
        this.addRenderableWidget(this.nameInput);

        // Button: Set Name
        this.addRenderableWidget(Button.builder(Component.literal("Set Name"), b -> saveCustomName())
            .bounds(x + 136, y + 68, 76, 18).build());

        // Button 5: TRIGGER PULSE
        this.addRenderableWidget(Button.builder(Component.literal("TRIGGER PULSE"), b -> sendButton(5))
            .bounds(x + 15, y + 98, 200, 24).build());
    }

    private void saveCustomName() {
        if (this.nameInput != null) {
            String typed = this.nameInput.getValue().trim();
            if (!typed.isEmpty()) {
                ClientPacketDistributor.sendToServer(new SetSuppressorNamePayload(this.menu.getPos(), typed));
                ChronoSuppressorBlockEntity be = this.menu.getBlockEntity();
                if (be != null) {
                    be.setCustomName(typed);
                }
            }
        }
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public void onClose() {
        saveCustomName();
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E222B);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 22, 0xFF0F1218);
        guiGraphics.fill(x + 6, y + 24, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xFF14171E);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, Component.literal("Chrono-Pulse Suppressor"), 10, 7, 0xFF55FFFF, false);

        int dur = this.menu.getDurationSeconds();
        int rad = this.menu.getPulseRadius();

        guiGraphics.text(this.font, Component.literal("Duration: " + dur + "s (" + (dur * 20) + "t)"), 14, 28, 0xFFFFAA00, false);
        guiGraphics.text(this.font, Component.literal("Pulse Radius: " + rad + " Blocks"), 14, 48, 0xFF55FFFF, false);
    }
}
