package com.redstonejammer.registry;

import com.redstonejammer.RedstoneJammerMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, RedstoneJammerMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> REDSTONE_JAMMER_TAB =
            CREATIVE_MODE_TABS.register("redstone_jammer_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.redstonejammer"))
                    .icon(() -> new ItemStack(ModItems.RESONANCE_DISRUPTOR_WAND.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.RESONANCE_DISRUPTOR_WAND.get());
                        output.accept(ModItems.SUB_FREQUENCY_STEALTH_REMOTE.get());
                        output.accept(ModBlocks.FLUX_PROJECTOR.get());
                        output.accept(ModBlocks.CHRONO_SUPPRESSOR.get());
                    })
                    .build());
}
