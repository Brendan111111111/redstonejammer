package com.redstonejammer.registry;

import com.redstonejammer.RedstoneJammerMod;
import com.redstonejammer.item.DisruptorWandItem;
import com.redstonejammer.item.StealthRemoteItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneJammerMod.MOD_ID);

    public static final DeferredItem<Item> RESONANCE_DISRUPTOR_WAND = ITEMS.register("resonance_disruptor_wand",
            () -> new DisruptorWandItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RedstoneJammerMod.MOD_ID, "resonance_disruptor_wand")))
                    .stacksTo(1)
                    .durability(500)));

    public static final DeferredItem<Item> SUB_FREQUENCY_STEALTH_REMOTE = ITEMS.register("sub_frequency_stealth_remote",
            () -> new StealthRemoteItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(RedstoneJammerMod.MOD_ID, "sub_frequency_stealth_remote")))
                    .stacksTo(1)));
}
