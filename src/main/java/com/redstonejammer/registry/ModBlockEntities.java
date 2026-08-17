package com.redstonejammer.registry;

import com.redstonejammer.RedstoneJammerMod;
import com.redstonejammer.block.ChronoSuppressorBlockEntity;
import com.redstonejammer.block.FluxProjectorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RedstoneJammerMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxProjectorBlockEntity>> FLUX_PROJECTOR_BE =
            BLOCK_ENTITIES.register("flux_inversion_projector_be",
                    () -> new BlockEntityType<>(FluxProjectorBlockEntity::new, Set.of(ModBlocks.FLUX_PROJECTOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChronoSuppressorBlockEntity>> CHRONO_SUPPRESSOR_BE =
            BLOCK_ENTITIES.register("chrono_pulse_suppressor_be",
                    () -> new BlockEntityType<>(ChronoSuppressorBlockEntity::new, Set.of(ModBlocks.CHRONO_SUPPRESSOR.get())));
}
