package com.redstonejammer.registry;

import com.redstonejammer.RedstoneJammerMod;
import com.redstonejammer.block.ChronoSuppressorBlock;
import com.redstonejammer.block.FluxProjectorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RedstoneJammerMod.MOD_ID);

    public static final DeferredBlock<Block> FLUX_PROJECTOR = registerBlock("flux_inversion_projector",
            () -> new FluxProjectorBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneJammerMod.MOD_ID, "flux_inversion_projector")))
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)));

    public static final DeferredBlock<Block> CHRONO_SUPPRESSOR = registerBlock("chrono_pulse_suppressor",
            () -> new ChronoSuppressorBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(RedstoneJammerMod.MOD_ID, "chrono_pulse_suppressor")))
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 4.0F)
                    .noOcclusion()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RedstoneJammerMod.MOD_ID, name)))));
        return block;
    }
}
