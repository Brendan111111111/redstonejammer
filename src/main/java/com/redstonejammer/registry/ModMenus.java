package com.redstonejammer.registry;

import com.redstonejammer.RedstoneJammerMod;
import com.redstonejammer.menu.ChronoSuppressorMenu;
import com.redstonejammer.menu.StealthRemoteMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RedstoneJammerMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ChronoSuppressorMenu>> CHRONO_SUPPRESSOR_MENU =
            MENUS.register("chrono_suppressor", () -> IMenuTypeExtension.create((windowId, inv, data) -> new ChronoSuppressorMenu(windowId, inv, data.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<StealthRemoteMenu>> STEALTH_REMOTE_MENU =
            MENUS.register("stealth_remote", () -> new MenuType<>(StealthRemoteMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
}
