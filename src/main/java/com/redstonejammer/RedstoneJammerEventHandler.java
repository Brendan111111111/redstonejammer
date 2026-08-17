package com.redstonejammer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneJammerEventHandler {
    // 1. Tag-based Datapack Hooks for universal modpack compatibility
    public static final TagKey<Block> IMMUNE_JAM_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("redstonejammer", "immune_blocks"));
    public static final TagKey<Block> POWER_SOURCE_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("redstonejammer", "power_sources"));
    public static final TagKey<Block> JAMMABLE_RECEIVER_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("redstonejammer", "jammable_receivers"));

    // 2. High-Performance Fastutil Spatial Chunk Partitioning (Zero GC allocations)
    private static final Long2ObjectOpenHashMap<LongSet> CHUNK_JAMMED_POSITIONS = new Long2ObjectOpenHashMap<>();
    private static final Map<BlockPos, Set<BlockPos>> JAMMED_TARGET_STRUCTURES = new ConcurrentHashMap<>();

    public static long packChunkPos(int chunkX, int chunkZ) {
        return (((long) chunkX) & 0xFFFFFFFFL) | ((((long) chunkZ) & 0xFFFFFFFFL) << 32);
    }

    public static int getChunkX(long chunkKey) {
        return (int) (chunkKey & 0xFFFFFFFFL);
    }

    public static int getChunkZ(long chunkKey) {
        return (int) ((chunkKey >>> 32) & 0xFFFFFFFFL);
    }

    public static String getBlockId(net.minecraft.world.level.block.Block b) {
        if (b == null) return "";
        try {
            var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b);
            return key != null ? key.toString().toLowerCase() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean isImmune(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(IMMUNE_JAM_TAG)) return true;

        // Mod's own functional devices must NEVER be jammed by their own jamming fields
        String regName = getBlockId(state.getBlock());
        return regName.startsWith("redstonejammer:");
    }

    public static boolean isPureSource(BlockState state) {
        return isImmune(state) || isPowerSource(state);
    }

    public static boolean isPowerSourceOrConductor(BlockState state) {
        return isImmune(state) || isPowerSource(state);
    }

    public static boolean isPowerSource(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(POWER_SOURCE_TAG)) return true;

        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.RedstoneTorchBlock ||
            b instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock ||
            b instanceof net.minecraft.world.level.block.LeverBlock ||
            b instanceof net.minecraft.world.level.block.ButtonBlock ||
            b instanceof net.minecraft.world.level.block.BasePressurePlateBlock ||
            b instanceof net.minecraft.world.level.block.DaylightDetectorBlock ||
            b instanceof net.minecraft.world.level.block.TargetBlock ||
            b instanceof net.minecraft.world.level.block.TrappedChestBlock ||
            b instanceof net.minecraft.world.level.block.SculkSensorBlock ||
            b instanceof net.minecraft.world.level.block.TripWireHookBlock ||
            b instanceof net.minecraft.world.level.block.LightningRodBlock ||
            b instanceof net.minecraft.world.level.block.PoweredBlock) {
            return true;
        }

        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return name.contains("lever") || name.contains("button") || name.contains("torch") ||
               name.contains("daylight") || name.contains("pressure_plate") ||
               fullId.contains("lever") || fullId.contains("button") || fullId.contains("torch") ||
               fullId.contains("daylight") || fullId.contains("pressure_plate");
    }

    public static boolean isLaserEmitter(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (isLaserBeamOrBridge(state)) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return fullId.contains("laser") || name.contains("laser") ||
               fullId.contains("projector") || name.contains("projector") ||
               fullId.contains("forcefield") || name.contains("forcefield") ||
               fullId.contains("emitter") || name.contains("emitter") ||
               fullId.contains("laserbridgesanddoors") ||
               fullId.contains("securitycraft");
    }

    public static boolean isLaserRelated(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return fullId.contains("laser") || name.contains("laser") ||
               fullId.contains("projector") || name.contains("projector") ||
               fullId.contains("forcefield") || name.contains("forcefield") ||
               fullId.contains("emitter") || name.contains("emitter") ||
               fullId.contains("bridge") || name.contains("bridge") ||
               fullId.contains("beam") || name.contains("beam") ||
               fullId.contains("laserbridgesanddoors") ||
               fullId.contains("securitycraft");
    }

    public static boolean isLaserBeamOrBridge(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();

        // The emitter/source/projector/controller block itself is not the beam
        if (fullId.contains("source") || fullId.contains("emitter") || fullId.contains("projector") ||
            fullId.contains("controller") || fullId.contains("core") || fullId.contains("block") ||
            fullId.contains("switch") || fullId.contains("keypad") || fullId.contains("terminal")) {
            if (!fullId.contains("laser_bridge") && !fullId.contains("laser_door") &&
                !fullId.contains("laser_fence") && !fullId.contains("laser_beam") &&
                !fullId.contains("laser_platform") && !fullId.contains("forcefield_block")) {
                return false;
            }
        }

        return fullId.contains("laser_bridge") || fullId.contains("laser_door") || fullId.contains("laser_fence") ||
               fullId.contains("laser_beam") || fullId.contains("laser_platform") || fullId.contains("forcefield_block") ||
               fullId.equals("laserbridgesanddoors:laser_bridge") ||
               fullId.equals("laserbridgesanddoors:laser_door") ||
               fullId.equals("laserbridgesanddoors:laser_fence") ||
               fullId.equals("laserbridgesanddoors:laser_platform") ||
               fullId.equals("laserbridgesanddoors:laser") ||
               fullId.equals("laserbridgesanddoors:laser_beam") ||
               (fullId.contains("laser") && (fullId.contains("bridge") || fullId.contains("door") || fullId.contains("fence") || fullId.contains("beam") || fullId.contains("platform"))) ||
               (name.contains("laser") && (name.contains("bridge") || name.contains("door") || name.contains("fence") || name.contains("beam") || name.contains("platform")));
    }

    public static boolean isPistonBase(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.piston.PistonBaseBlock) return true;
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return (name.contains("piston") || fullId.contains("piston")) &&
               !name.contains("head") && !name.contains("moving") &&
               !fullId.contains("head") && !fullId.contains("moving");
    }

    public static boolean isPistonHead(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.piston.PistonHeadBlock ||
            b instanceof net.minecraft.world.level.block.piston.MovingPistonBlock) return true;
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return (name.contains("piston") && name.contains("head")) ||
               (name.contains("piston") && name.contains("moving")) ||
               (fullId.contains("piston") && fullId.contains("head")) ||
               (fullId.contains("piston") && fullId.contains("moving"));
    }

    public static Direction getPistonFacing(BlockState state) {
        if (state == null) return Direction.NORTH;
        if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING)) {
            return state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING);
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof net.minecraft.world.level.block.state.properties.EnumProperty<?> enumProp) {
                if (prop.getName().equalsIgnoreCase("facing")) {
                    Object val = state.getValue(prop);
                    if (val instanceof Direction d) return d;
                }
            }
        }
        return Direction.NORTH;
    }

    public static boolean isPistonExtended(BlockState state) {
        if (state == null) return false;
        if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)) {
            return state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED);
        }
        return false;
    }

    public static BlockState setPistonRetracted(BlockState state) {
        if (state == null) return state;
        if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)) {
            return state.setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED, false);
        }
        return state;
    }

    public static boolean isModdedWire(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.RedStoneWireBlock) return true;
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return fullId.contains("morered") || fullId.contains("moorered") ||
               fullId.contains("wire") || fullId.contains("alloy") || fullId.contains("cable") ||
               name.contains("wire") || name.contains("morered") || name.contains("moorered") ||
               name.contains("cable") || name.contains("projectred") || fullId.contains("projectred") ||
               fullId.contains("bundled") || fullId.contains("insulated");
    }

    public static boolean isRedstoneSource(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.RedStoneWireBlock ||
            b instanceof net.minecraft.world.level.block.RedstoneTorchBlock ||
            b instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock ||
            b instanceof net.minecraft.world.level.block.LeverBlock ||
            b instanceof net.minecraft.world.level.block.ButtonBlock ||
            b instanceof net.minecraft.world.level.block.PressurePlateBlock ||
            b instanceof net.minecraft.world.level.block.RepeaterBlock ||
            b instanceof net.minecraft.world.level.block.ComparatorBlock ||
            b instanceof net.minecraft.world.level.block.DaylightDetectorBlock ||
            b instanceof net.minecraft.world.level.block.ObserverBlock ||
            b instanceof net.minecraft.world.level.block.TargetBlock ||
            b instanceof net.minecraft.world.level.block.SculkSensorBlock ||
            b instanceof net.minecraft.world.level.block.PoweredBlock ||
            isModdedWire(state)) {
            return true;
        }
        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        return fullId.contains("torch") || name.contains("torch") ||
               fullId.contains("lever") || name.contains("lever") ||
               fullId.contains("button") || name.contains("button") ||
               fullId.contains("repeater") || name.contains("repeater") ||
               fullId.contains("comparator") || name.contains("comparator") ||
               fullId.contains("wire") || name.contains("wire") ||
               fullId.contains("morered") || name.contains("morered") ||
               fullId.contains("moorered") || name.contains("moorered") ||
               fullId.contains("projectred") || name.contains("projectred") ||
               fullId.contains("redstone") || name.contains("redstone");
    }

    public static boolean isJammableReceiver(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (isImmune(state)) return false;
        if (state.is(JAMMABLE_RECEIVER_TAG)) return true;
        if (isPistonBase(state) || isPistonHead(state) || isLaserRelated(state) || isModdedWire(state)) return true;

        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.DoorBlock ||
            b instanceof net.minecraft.world.level.block.TrapDoorBlock ||
            b instanceof net.minecraft.world.level.block.FenceGateBlock ||
            b instanceof net.minecraft.world.level.block.RedstoneLampBlock ||
            b instanceof net.minecraft.world.level.block.piston.PistonBaseBlock ||
            b instanceof net.minecraft.world.level.block.piston.PistonHeadBlock ||
            b instanceof net.minecraft.world.level.block.piston.MovingPistonBlock ||
            b instanceof net.minecraft.world.level.block.DispenserBlock ||
            b instanceof net.minecraft.world.level.block.DropperBlock ||
            b instanceof net.minecraft.world.level.block.HopperBlock ||
            b instanceof net.minecraft.world.level.block.CrafterBlock ||
            b instanceof net.minecraft.world.level.block.BellBlock ||
            b instanceof net.minecraft.world.level.block.NoteBlock ||
            b instanceof net.minecraft.world.level.block.PoweredRailBlock ||
            b instanceof net.minecraft.world.level.block.RedStoneWireBlock ||
            b instanceof net.minecraft.world.level.block.RepeaterBlock ||
            b instanceof net.minecraft.world.level.block.ComparatorBlock ||
            b instanceof net.minecraft.world.level.block.ObserverBlock) {
            return true;
        }

        String fullId = getBlockId(b);
        String name = b.getClass().getName().toLowerCase();
        if (fullId.contains("laser") || name.contains("laser") ||
            fullId.contains("bridge") || name.contains("bridge") ||
            fullId.contains("door") || name.contains("door") ||
            fullId.contains("elevator") || name.contains("elevator") ||
            fullId.contains("modem") || name.contains("modem") ||
            fullId.contains("computer") || name.contains("computer") ||
            fullId.contains("gate") || name.contains("gate") ||
            fullId.contains("latch") || name.contains("latch") ||
            fullId.contains("router") || name.contains("router") ||
            fullId.contains("wireless") || name.contains("wireless") ||
            fullId.contains("forcefield") || name.contains("forcefield") ||
            fullId.contains("projector") || name.contains("projector") ||
            fullId.contains("securitycraft") || name.contains("securitycraft") ||
            fullId.contains("keypad") || name.contains("keypad") ||
            fullId.contains("scanner") || name.contains("scanner") ||
            fullId.contains("alarm") || name.contains("alarm") ||
            fullId.contains("wire") || name.contains("wire") ||
            fullId.contains("morered") || name.contains("morered") ||
            fullId.contains("moorered") || name.contains("moorered") ||
            fullId.contains("projectred") || name.contains("projectred")) {
            return true;
        }

        for (Property<?> property : state.getProperties()) {
            String propName = property.getName().toLowerCase();
            if (propName.contains("open") || propName.contains("powered") || propName.contains("lit") ||
                propName.contains("active") || propName.contains("enabled") || propName.contains("extended") ||
                propName.contains("triggered") || propName.contains("power") || propName.contains("signal") ||
                propName.contains("level") || propName.contains("delay") || propName.contains("crafting") ||
                propName.contains("on") || propName.contains("working") || propName.contains("running") ||
                propName.contains("emitting") || propName.contains("laser")) {
                return true;
            }
        }
        return false;
    }

    public static Set<BlockPos> getConnectedStructurePositions(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        if (level == null || startPos == null || !level.isLoaded(startPos)) return result;

        try {
            result.add(startPos.immutable());
            BlockState startState = level.getBlockState(startPos);
            if (startState.isAir() || isImmune(startState)) return result;

            // 1. Doors: Connect top/bottom halves and double doors
            if (startState.getBlock() instanceof net.minecraft.world.level.block.DoorBlock ||
                startState.getBlock().getClass().getName().toLowerCase().contains("door")) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dy == 0) continue;
                    BlockPos vertPos = startPos.above(dy);
                    if (level.isLoaded(vertPos)) {
                        BlockState vState = level.getBlockState(vertPos);
                        if (vState.is(startState.getBlock())) {
                            result.add(vertPos.immutable());
                        }
                    }
                }
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos adjPos = startPos.relative(dir);
                    if (level.isLoaded(adjPos)) {
                        BlockState aState = level.getBlockState(adjPos);
                        if (aState.is(startState.getBlock())) {
                            result.add(adjPos.immutable());
                            for (int dy = -1; dy <= 1; dy++) {
                                BlockPos adjVert = adjPos.above(dy);
                                if (level.isLoaded(adjVert) && level.getBlockState(adjVert).is(startState.getBlock())) {
                                    result.add(adjVert.immutable());
                                }
                            }
                        }
                    }
                }
            }

            // 2. Pistons: Connect piston base and piston head
            if (isPistonBase(startState)) {
                Direction facing = getPistonFacing(startState);
                BlockPos headPos = startPos.relative(facing);
                result.add(headPos.immutable());
            } else if (isPistonHead(startState)) {
                Direction facing = getPistonFacing(startState);
                BlockPos basePos = startPos.relative(facing.getOpposite());
                result.add(basePos.immutable());
            }

            // 3. Modded Laser Bridges & Doors: Follow straight beam axis (up to 64 blocks)
            if (isLaserRelated(startState) || isLaserBeamOrBridge(startState) || isLaserEmitter(startState)) {
                for (Direction dir : Direction.values()) {
                    for (int dist = 1; dist <= 64; dist++) {
                        BlockPos bPos = startPos.relative(dir, dist);
                        if (!level.isLoaded(bPos)) break;
                        BlockState bState = level.getBlockState(bPos);
                        if (bState.isAir()) continue;
                        if (isLaserBeamOrBridge(bState) || isLaserRelated(bState)) {
                            result.add(bPos.immutable());
                        } else if (!bState.canBeReplaced() && !isLaserRelated(bState)) {
                            break;
                        }
                    }
                }
            }

            // 4. Redstone & Modded Wire Lines (Traverse wire network up to 32 blocks)
            if (isModdedWire(startState) || startState.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                Set<BlockPos> wireQueue = new HashSet<>();
                wireQueue.add(startPos);
                Set<BlockPos> visitedWires = new HashSet<>();
                while (!wireQueue.isEmpty() && visitedWires.size() < 32) {
                    BlockPos currentWire = wireQueue.iterator().next();
                    wireQueue.remove(currentWire);
                    if (visitedWires.add(currentWire)) {
                        result.add(currentWire);
                        for (Direction dir : Direction.values()) {
                            BlockPos flatPos = currentWire.relative(dir);
                            if (!visitedWires.contains(flatPos) && level.isLoaded(flatPos)) {
                                BlockState adjacentState = level.getBlockState(flatPos);
                                if (isModdedWire(adjacentState) || adjacentState.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                                    wireQueue.add(flatPos.immutable());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return result;
    }

    public static synchronized void jamBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        try {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || isImmune(state)) return;

            Set<BlockPos> targets = getConnectedStructurePositions(level, pos);
            JAMMED_TARGET_STRUCTURES.put(pos.immutable(), targets);
            for (BlockPos p : targets) {
                if (level.isLoaded(p)) {
                    BlockState targetState = level.getBlockState(p);
                    if (!targetState.isAir() && !isImmune(targetState)) {
                        long chunkKey = packChunkPos(p.getX() >> 4, p.getZ() >> 4);
                        CHUNK_JAMMED_POSITIONS.computeIfAbsent(chunkKey, k -> new LongOpenHashSet()).add(p.asLong());
                        applyJamToBlockState(level, p);
                        level.updateNeighborsAt(p, targetState.getBlock());
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static synchronized void unjamBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        try {
            Set<BlockPos> targets = JAMMED_TARGET_STRUCTURES.remove(pos.immutable());
            if (targets == null) {
                targets = getConnectedStructurePositions(level, pos);
            }
            for (BlockPos p : targets) {
                if (p == null) continue;
                long chunkKey = packChunkPos(p.getX() >> 4, p.getZ() >> 4);
                LongSet set = CHUNK_JAMMED_POSITIONS.get(chunkKey);
                if (set != null) {
                    set.remove(p.asLong());
                    if (set.isEmpty()) {
                        CHUNK_JAMMED_POSITIONS.remove(chunkKey);
                    }
                }
                if (!level.isClientSide() && level.isLoaded(p)) {
                    BlockState state = level.getBlockState(p);
                    Block block = state.getBlock();
                    if (block instanceof net.minecraft.world.level.block.RedstoneTorchBlock ||
                        block instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock) {
                        if (state.hasProperty(net.minecraft.world.level.block.RedstoneTorchBlock.LIT) && !state.getValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT)) {
                            level.setBlock(p, state.setValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
                        }
                    } else if (block instanceof net.minecraft.world.level.block.RepeaterBlock) {
                        if (state.hasProperty(net.minecraft.world.level.block.RepeaterBlock.LOCKED) && state.getValue(net.minecraft.world.level.block.RepeaterBlock.LOCKED)) {
                            level.setBlock(p, state.setValue(net.minecraft.world.level.block.RepeaterBlock.LOCKED, false), net.minecraft.world.level.block.Block.UPDATE_ALL);
                        }
                    } else {
                        level.updateNeighborsAt(p, block);
                    }
                    for (Direction dir : Direction.values()) {
                        BlockPos nPos = p.relative(dir);
                        if (level.isLoaded(nPos)) {
                            BlockState nState = level.getBlockState(nPos);
                            level.updateNeighborsAt(nPos, nState.getBlock());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static synchronized boolean isJammed(BlockPos pos) {
        if (pos == null) return false;
        long chunkKey = packChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        LongSet set = CHUNK_JAMMED_POSITIONS.get(chunkKey);
        return set != null && set.contains(pos.asLong());
    }

    public static void applyJamToBlockState(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || pos == null || !level.isLoaded(pos)) return;

        try {
            BlockState state = level.getBlockState(pos);
            if (isImmune(state) || state.isAir()) {
                return;
            }

            Block block = state.getBlock();

            // 1. Redstone Torches: Extinguish in-place (LIT = false) with UPDATE_CLIENTS (no recursive neighbor cascade!)
            if (block instanceof net.minecraft.world.level.block.RedstoneTorchBlock ||
                block instanceof net.minecraft.world.level.block.RedstoneWallTorchBlock) {
                if (state.hasProperty(net.minecraft.world.level.block.RedstoneTorchBlock.LIT) && state.getValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT)) {
                    level.setBlock(pos, state.setValue(net.minecraft.world.level.block.RedstoneTorchBlock.LIT, false), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 2. Redstone & Modded Wires (More Red, ProjectRed, etc.): Zero out power & signals
            if (isModdedWire(state) || block instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                BlockState modState = state;
                if (modState.hasProperty(net.minecraft.world.level.block.RedStoneWireBlock.POWER) && modState.getValue(net.minecraft.world.level.block.RedStoneWireBlock.POWER) > 0) {
                    modState = modState.setValue(net.minecraft.world.level.block.RedStoneWireBlock.POWER, 0);
                }
                for (Property<?> property : state.getProperties()) {
                    String propName = property.getName().toLowerCase();
                    if (property instanceof IntegerProperty ip && (propName.contains("power") || propName.contains("signal") || propName.contains("level"))) {
                        int min = ip.getPossibleValues().iterator().next();
                        if (modState.getValue(ip) > min) {
                            modState = modState.setValue(ip, min);
                        }
                    } else if (property instanceof BooleanProperty bp && (propName.contains("power") || propName.contains("active") || propName.contains("on") || propName.contains("lit"))) {
                        if (modState.getValue(bp)) {
                            modState = modState.setValue(bp, false);
                        }
                    }
                }
                if (modState != state) {
                    level.setBlock(pos, modState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 3. Repeaters: Set unpowered & locked
            if (block instanceof net.minecraft.world.level.block.RepeaterBlock) {
                if (state.hasProperty(net.minecraft.world.level.block.RepeaterBlock.POWERED) && state.hasProperty(net.minecraft.world.level.block.RepeaterBlock.LOCKED)) {
                    if (state.getValue(net.minecraft.world.level.block.RepeaterBlock.POWERED) || !state.getValue(net.minecraft.world.level.block.RepeaterBlock.LOCKED)) {
                        level.setBlock(pos, state.setValue(net.minecraft.world.level.block.RepeaterBlock.POWERED, false).setValue(net.minecraft.world.level.block.RepeaterBlock.LOCKED, true), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    }
                }
                return;
            }

            // 4. Comparators: Set unpowered
            if (block instanceof net.minecraft.world.level.block.ComparatorBlock) {
                if (state.hasProperty(net.minecraft.world.level.block.ComparatorBlock.POWERED) && state.getValue(net.minecraft.world.level.block.ComparatorBlock.POWERED)) {
                    level.setBlock(pos, state.setValue(net.minecraft.world.level.block.ComparatorBlock.POWERED, false), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 5. Levers: Toggle off
            if (block instanceof net.minecraft.world.level.block.LeverBlock) {
                if (state.hasProperty(net.minecraft.world.level.block.LeverBlock.POWERED) && state.getValue(net.minecraft.world.level.block.LeverBlock.POWERED)) {
                    level.setBlock(pos, state.setValue(net.minecraft.world.level.block.LeverBlock.POWERED, false), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 6. Daylight Detectors: Zero out power
            if (block instanceof net.minecraft.world.level.block.DaylightDetectorBlock) {
                if (state.hasProperty(net.minecraft.world.level.block.DaylightDetectorBlock.POWER) && state.getValue(net.minecraft.world.level.block.DaylightDetectorBlock.POWER) > 0) {
                    level.setBlock(pos, state.setValue(net.minecraft.world.level.block.DaylightDetectorBlock.POWER, 0), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 7. Doors, Trapdoors & Fence Gates: Force closed and unpowered
            if (block instanceof net.minecraft.world.level.block.DoorBlock ||
                block instanceof net.minecraft.world.level.block.TrapDoorBlock ||
                block instanceof net.minecraft.world.level.block.FenceGateBlock) {
                BlockState modState = state;
                if (modState.hasProperty(net.minecraft.world.level.block.DoorBlock.OPEN) && modState.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)) {
                    modState = modState.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, false);
                }
                if (modState.hasProperty(net.minecraft.world.level.block.DoorBlock.POWERED) && modState.getValue(net.minecraft.world.level.block.DoorBlock.POWERED)) {
                    modState = modState.setValue(net.minecraft.world.level.block.DoorBlock.POWERED, false);
                }
                if (modState != state) {
                    level.setBlock(pos, modState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 8. Redstone Lamps & Bulbs: Extinguish (LIT = false)
            if (block instanceof net.minecraft.world.level.block.RedstoneLampBlock) {
                if (state.hasProperty(net.minecraft.world.level.block.RedstoneLampBlock.LIT) && state.getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT)) {
                    level.setBlock(pos, state.setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, false), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 9. Pistons: Trigger standard retraction and force EXTENDED = false
            if (isPistonBase(state)) {
                Direction facing = getPistonFacing(state);
                if (isPistonExtended(state)) {
                    level.blockEvent(pos, state.getBlock(), 1, facing.get3DDataValue());
                }
                if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED) && state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)) {
                    level.setBlock(pos, state.setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED, false), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 10. Piston Heads: Allow vanilla retraction to manage moving blocks without interference
            if (isPistonHead(state)) {
                return;
            }

            // 11. Modded Laser Beams / Bridges: Clear beam blocks to AIR without triggering neighbor updates on emitters
            if (isLaserBeamOrBridge(state)) {
                if (!state.isAir()) {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                return;
            }

            // 12. Modded Laser Emitter / Source / Projector: Deactivate properties and suppress emitter state
            if (isLaserRelated(state)) {
                BlockState modState = state;
                for (Property<?> property : state.getProperties()) {
                    String propName = property.getName().toLowerCase();
                    if (property instanceof BooleanProperty bp) {
                        if (propName.contains("powered") || propName.contains("lit") || propName.contains("active") ||
                            propName.contains("enabled") || propName.contains("emitting") || propName.contains("on") ||
                            propName.contains("open") || propName.contains("laser") || propName.contains("running") ||
                            propName.contains("working")) {
                            if (modState.getValue(bp)) {
                                modState = modState.setValue(bp, false);
                            }
                        }
                    } else if (property instanceof IntegerProperty ip) {
                        if (propName.contains("power") || propName.contains("signal") || propName.contains("output") ||
                            propName.contains("level") || propName.contains("beam") || propName.contains("length")) {
                            int min = ip.getPossibleValues().iterator().next();
                            if (modState.getValue(ip) > min) {
                                modState = modState.setValue(ip, min);
                            }
                        }
                    }
                }
                if (modState != state) {
                    level.setBlock(pos, modState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
                // Suppress BlockEntity if present
                try {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        java.lang.reflect.Method[] methods = be.getClass().getMethods();
                        for (java.lang.reflect.Method m : methods) {
                            String mName = m.getName().toLowerCase();
                            if ((mName.startsWith("set") || mName.startsWith("disable")) &&
                                (mName.contains("active") || mName.contains("power") || mName.contains("enable") || mName.contains("emit")) &&
                                m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                                m.invoke(be, false);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                return;
            }

            // 13. Universal Property Zeroing for any other modded or custom jammable receiver
            BlockState modState = state;
            for (Property<?> property : state.getProperties()) {
                String propName = property.getName().toLowerCase();
                if (property instanceof BooleanProperty bp) {
                    if (propName.contains("open") || propName.contains("powered") || propName.contains("lit") ||
                        propName.contains("active") || propName.contains("enabled") || propName.contains("extended") ||
                        propName.contains("triggered") || propName.contains("on") || propName.contains("working") ||
                        propName.contains("running") || propName.contains("crafting") || propName.contains("emitting") ||
                        propName.contains("laser")) {
                        if (modState.getValue(bp)) {
                            modState = modState.setValue(bp, false);
                        }
                    }
                } else if (property instanceof IntegerProperty ip) {
                    if (propName.contains("power") || propName.contains("signal") || propName.contains("output") ||
                        propName.contains("level")) {
                        int min = ip.getPossibleValues().iterator().next();
                        if (modState.getValue(ip) > min) {
                            modState = modState.setValue(ip, min);
                        }
                    }
                }
            }
            if (modState != state) {
                level.setBlock(pos, modState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        processJammedBlocks(event.getLevel());
    }

    private void processJammedBlocks(Level level) {
        if (level == null || level.isClientSide()) return;

        try {
            synchronized (RedstoneJammerEventHandler.class) {
                if (!CHUNK_JAMMED_POSITIONS.isEmpty()) {
                    var chunkIterator = CHUNK_JAMMED_POSITIONS.long2ObjectEntrySet().iterator();
                    while (chunkIterator.hasNext()) {
                        var entry = chunkIterator.next();
                        long chunkKey = entry.getLongKey();
                        int chunkX = getChunkX(chunkKey);
                        int chunkZ = getChunkZ(chunkKey);

                        if (!level.hasChunk(chunkX, chunkZ)) {
                            continue;
                        }

                        LongSet posSet = entry.getValue();
                        long[] positions = posSet.toLongArray();
                        for (long posLong : positions) {
                            try {
                                BlockPos pos = BlockPos.of(posLong);
                                if (level.isLoaded(pos)) {
                                    BlockState state = level.getBlockState(pos);
                                    if (isImmune(state)) {
                                        posSet.remove(posLong);
                                        JAMMED_TARGET_STRUCTURES.remove(pos);
                                    } else if (!state.isAir()) {
                                        applyJamToBlockState(level, pos);
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }

                        if (posSet.isEmpty()) {
                            chunkIterator.remove();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        // Spatial chunks manage their state automatically without memory leak
    }

    @SubscribeEvent
    public void onPistonPre(PistonEvent.Pre event) {
        try {
            BlockPos pos = event.getPos();
            if (pos != null) {
                if (isJammed(pos) || (event.getDirection() != null && isJammed(pos.relative(event.getDirection())))) {
                    if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        try {
            BlockPos pos = event.getPos();
            if (pos != null && isJammed(pos)) {
                event.setCanceled(true);
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        try {
            if (event.getLevel() instanceof Level level && !level.isClientSide()) {
                BlockPos sourcePos = event.getPos();
                if (sourcePos == null) return;

                // 1. If source block itself is jammed, cancel its neighbor notifications entirely
                if (isJammed(sourcePos)) {
                    event.setCanceled(true);
                    return;
                }

                // 2. Filter out notified sides that lead to jammed blocks without breaking unjammed sides
                var notifiedSides = event.getNotifiedSides();
                if (notifiedSides != null) {
                    notifiedSides.removeIf(dir -> isJammed(sourcePos.relative(dir)));
                    if (notifiedSides.isEmpty()) {
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        try {
            BlockPos pos = event.getPos();
            Player player = event.getEntity();

            if (pos != null && isJammed(pos)) {
                if (player != null) {
                    net.minecraft.world.item.ItemStack main = player.getMainHandItem();
                    net.minecraft.world.item.ItemStack off = player.getOffhandItem();
                    if (main.is(com.redstonejammer.registry.ModItems.RESONANCE_DISRUPTOR_WAND.get()) ||
                        main.is(com.redstonejammer.registry.ModItems.SUB_FREQUENCY_STEALTH_REMOTE.get()) ||
                        off.is(com.redstonejammer.registry.ModItems.RESONANCE_DISRUPTOR_WAND.get()) ||
                        off.is(com.redstonejammer.registry.ModItems.SUB_FREQUENCY_STEALTH_REMOTE.get())) {
                        return;
                    }
                }

                event.setCanceled(true);
                if (!event.getLevel().isClientSide() && player != null) {
                    player.sendSystemMessage(Component.literal("§c[Redstone Jammer] Mechanism is jammed by electromagnetic interference!"));
                }
            }
        } catch (Throwable ignored) {}
    }
}
