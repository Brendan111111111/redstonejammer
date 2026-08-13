package com.redstonejammer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneJammerEventHandler {
    private static final Set<BlockPos> JAMMED_POSITIONS = new HashSet<>();

    public static Set<BlockPos> getConnectedStructurePositions(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        result.add(startPos.immutable());
        BlockState startState = level.getBlockState(startPos);

        // Scan vertical stack for multiblocks/doors (e.g., Malisis Doors, 2-3 block tall doors)
        for (int dy = -3; dy <= 3; dy++) {
            if (dy == 0) continue;
            BlockPos p = startPos.above(dy);
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && (state.is(startState.getBlock()) || isDoorOrRedstone(state))) {
                result.add(p.immutable());
            }
        }

        // Scan horizontal neighbors (double doors, garage doors, wide multiblock doors)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos p = startPos.relative(dir);
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && (state.is(startState.getBlock()) || isDoorOrRedstone(state))) {
                result.add(p.immutable());
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pVert = p.above(dy);
                    BlockState vertState = level.getBlockState(pVert);
                    if (!vertState.isAir() && (vertState.is(startState.getBlock()) || isDoorOrRedstone(vertState))) {
                        result.add(pVert.immutable());
                    }
                }
            }
        }

        return result;
    }

    private static boolean isDoorOrRedstone(BlockState state) {
        String blockName = state.getBlock().getClass().getName().toLowerCase();
        if (blockName.contains("door") || blockName.contains("gate") || blockName.contains("redstone") || blockName.contains("wire")) {
            return true;
        }
        for (Property<?> prop : state.getProperties()) {
            String name = prop.getName().toLowerCase();
            if (name.contains("open") || name.contains("power") || name.contains("lit") || name.contains("powered")) {
                return true;
            }
        }
        return false;
    }

    private static final Map<BlockPos, Set<BlockPos>> JAMMED_TARGET_STRUCTURES = new ConcurrentHashMap<>();

    public static void jamBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        Set<BlockPos> targets = getConnectedStructurePositions(level, pos);
        JAMMED_TARGET_STRUCTURES.put(pos.immutable(), targets);
        for (BlockPos p : targets) {
            JAMMED_POSITIONS.add(p.immutable());
            applyJamToBlockState(level, p);
        }
    }

    public static void unjamBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        Set<BlockPos> targets = JAMMED_TARGET_STRUCTURES.remove(pos.immutable());
        if (targets == null) {
            targets = getConnectedStructurePositions(level, pos);
        }
        for (BlockPos p : targets) {
            JAMMED_POSITIONS.remove(p.immutable());
        }
    }

    public static boolean isJammed(BlockPos pos) {
        if (pos == null) return false;
        return JAMMED_POSITIONS.contains(pos.immutable());
    }

    public static void applyJamToBlockState(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        BlockState newState = state;
        boolean changed = false;

        // Dynamic Property Reflection: works on vanilla + ALL mods (Malisis Doors, Create, etc.)
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty boolProp) {
                String name = boolProp.getName().toLowerCase();
                if (name.contains("open") || name.contains("powered") || name.contains("lit") || name.contains("active") || name.contains("enabled")) {
                    if (state.getValue(boolProp)) {
                        newState = newState.setValue(boolProp, false);
                        changed = true;
                    }
                }
            } else if (property instanceof IntegerProperty intProp) {
                String name = intProp.getName().toLowerCase();
                if (name.contains("power") || name.contains("signal") || name.contains("level")) {
                    if (state.getValue(intProp) > 0) {
                        newState = newState.setValue(intProp, 0);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            level.setBlock(pos, newState, 3);
            level.updateNeighborsAt(pos, newState.getBlock());
        }
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level == null || level.isClientSide()) return;

        if (!JAMMED_POSITIONS.isEmpty()) {
            for (BlockPos pos : new HashSet<>(JAMMED_POSITIONS)) {
                applyJamToBlockState(level, pos);
            }
        }
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        BlockPos pos = event.getPos();
        if (isJammed(pos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        if (isJammed(pos)) {
            // Allow wand interaction
            if (player != null && player.getMainHandItem().is(com.redstonejammer.registry.ModItems.RESONANCE_DISRUPTOR_WAND.get())) {
                return;
            }

            event.setCanceled(true);
            if (!event.getLevel().isClientSide() && player != null) {
                player.sendSystemMessage(Component.literal("§c[Redstone Jammer] Mechanism is jammed by electromagnetic interference!"));
            }
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getPos() != null && isJammed(event.getPos())) {
            unjamBlock((Level) event.getLevel(), event.getPos());
        }
    }
}
