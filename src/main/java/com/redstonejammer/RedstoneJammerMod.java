package com.redstonejammer;

import com.mojang.logging.LogUtils;
import com.redstonejammer.registry.ModBlockEntities;
import com.redstonejammer.registry.ModBlocks;
import com.redstonejammer.registry.ModCreativeTab;
import com.redstonejammer.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RedstoneJammerMod.MOD_ID)
public class RedstoneJammerMod {
    public static final String MOD_ID = "redstonejammer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RedstoneJammerMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Redstone Jammer Mod for Minecraft 26.1.2 & NeoForge 26.1.2.94 by Mantoku!");

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        com.redstonejammer.registry.ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerPayloads);
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            modEventBus.addListener(this::onRegisterMenuScreens);
        }
        
        NeoForge.EVENT_BUS.register(new RedstoneJammerEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Redstone Jammer common setup complete.");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Registering Redstone Jammer client visual renderers.");
    }

    private void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playToServer(
            com.redstonejammer.network.SetSuppressorNamePayload.TYPE,
            com.redstonejammer.network.SetSuppressorNamePayload.STREAM_CODEC,
            (payload, context) -> {
                context.enqueueWork(() -> {
                    if (context.player().level().getBlockEntity(payload.pos()) instanceof com.redstonejammer.block.ChronoSuppressorBlockEntity suppressor) {
                        suppressor.setCustomName(payload.name());
                        context.player().sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Chrono Suppressor] Unit renamed to: '" + payload.name() + "'"));
                    }
                });
            }
        );
    }

    private void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.redstonejammer.registry.ModMenus.CHRONO_SUPPRESSOR_MENU.get(), com.redstonejammer.client.gui.ChronoSuppressorScreen::new);
        event.register(com.redstonejammer.registry.ModMenus.STEALTH_REMOTE_MENU.get(), com.redstonejammer.client.gui.StealthRemoteScreen::new);
    }
}
