package com.redstonejammer.item;

import com.redstonejammer.block.ChronoSuppressorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StealthRemoteItem extends Item {

    // Multi-suppressor network mapping per player
    private static final Map<UUID, List<BlockPos>> PLAYER_SUPPRESSORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ACTIVE_INDEX = new ConcurrentHashMap<>();

    public StealthRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        boolean isSneaking = player.isShiftKeyDown();
        BlockEntity be = level.getBlockEntity(pos);
        UUID uuid = player.getUUID();

        // 1. Interacting with a Chrono-Pulse Suppressor
        if (be instanceof ChronoSuppressorBlockEntity suppressor) {
            if (!level.isClientSide()) {
                List<BlockPos> list = PLAYER_SUPPRESSORS.computeIfAbsent(uuid, k -> new ArrayList<>());
                BlockPos immutablePos = pos.immutable();

                if (!list.contains(immutablePos)) {
                    list.add(immutablePos);
                }
                int idx = list.indexOf(immutablePos);
                ACTIVE_INDEX.put(uuid, idx);

                String name = suppressor.getCustomName();
                if (isSneaking) {
                    // Shift-click opens timer/radius adjustment
                    suppressor.cycleDuration(player);
                } else {
                    player.sendSystemMessage(Component.literal("§a[Stealth Remote Network] Paired with Unit: '" + name + "' (#" 
                        + (idx + 1) + ") at (" + pos.toShortString() + ") [Network Total: " + list.size() + "]"));
                    player.sendSystemMessage(Component.literal("§7Duration: " + suppressor.getDurationSeconds() 
                        + "s | Pulse Radius: " + suppressor.getPulseRadius() + " Blocks"));
                    player.sendSystemMessage(Component.literal("§eRight-click Stealth Remote to open Remote Control panel."));
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Return PASS so normal right-clicking on blocks or air opens the Remote GUI
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (containerId, playerInventory, p) -> new com.redstonejammer.menu.StealthRemoteMenu(containerId, playerInventory),
                Component.literal("Sub-Frequency Stealth Remote Network")
            ));
        }
        return InteractionResult.SUCCESS;
    }

    public static void cycleSelectedSuppressor(Player player) {
        UUID uuid = player.getUUID();
        List<BlockPos> list = PLAYER_SUPPRESSORS.get(uuid);
        if (list == null || list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[Stealth Remote] No Chrono-Pulse Suppressor linked! Right-click a Suppressor block to pair it."));
            return;
        }
        int currentIdx = ACTIVE_INDEX.getOrDefault(uuid, 0);
        int nextIdx = (currentIdx + 1) % list.size();
        ACTIVE_INDEX.put(uuid, nextIdx);
        BlockPos pos = list.get(nextIdx);
        BlockEntity suppBe = player.level().getBlockEntity(pos);
        String name = (suppBe instanceof ChronoSuppressorBlockEntity suppressor) ? suppressor.getCustomName() : ("Unit #" + (nextIdx + 1));
        player.sendSystemMessage(Component.literal("§b[Stealth Remote Network] Selected Active Unit: '" + name + "' (#" + (nextIdx + 1) + "/" + list.size() + ") at (" + pos.toShortString() + ")"));
    }

    public static void triggerActiveSuppressor(Player player) {
        UUID uuid = player.getUUID();
        List<BlockPos> list = PLAYER_SUPPRESSORS.get(uuid);
        if (list == null || list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[Stealth Remote] No Chrono-Pulse Suppressor linked!"));
            return;
        }
        int currentIdx = ACTIVE_INDEX.getOrDefault(uuid, 0);
        if (currentIdx >= list.size()) currentIdx = 0;
        BlockPos targetPos = list.get(currentIdx);
        BlockEntity suppBe = player.level().getBlockEntity(targetPos);
        if (suppBe instanceof ChronoSuppressorBlockEntity suppressor) {
            player.sendSystemMessage(Component.literal("§c⚡ [Stealth Remote GUI] Fired pulse on Active Unit: '" + suppressor.getCustomName() + "'!"));
            suppressor.triggerPulse(player);
        } else {
            player.sendSystemMessage(Component.literal("§c[Stealth Remote GUI] Active Suppressor at (" + targetPos.toShortString() + ") no longer exists. Removing from network."));
            list.remove(currentIdx);
            ACTIVE_INDEX.put(uuid, 0);
        }
    }

    public static void triggerAllSuppressors(Player player) {
        UUID uuid = player.getUUID();
        List<BlockPos> list = PLAYER_SUPPRESSORS.get(uuid);
        if (list == null || list.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[Stealth Remote] No Chrono-Pulse Suppressor linked!"));
            return;
        }
        int count = 0;
        for (BlockPos pos : new ArrayList<>(list)) {
            BlockEntity suppBe = player.level().getBlockEntity(pos);
            if (suppBe instanceof ChronoSuppressorBlockEntity suppressor) {
                suppressor.triggerPulse(player);
                count++;
            }
        }
        player.sendSystemMessage(Component.literal("§d💥 [Stealth Remote Network] MASS OVERLOAD PULSE triggered across " + count + " active Suppressors!"));
    }
}
