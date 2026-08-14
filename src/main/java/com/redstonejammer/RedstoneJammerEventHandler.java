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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneJammerEventHandler {
    // 1. Tag-based Datapack Hooks for universal modpack compatibility
    public static final TagKey<Block> IMMUNE_JAM_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("redstonejammer", "immune_blocks"));
    public static final TagKey<Block> POWER_SOURCE_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("redstonejammer", "power_sources"));
    public static final TagKey<Block> JAMMABLE_RECEIVER_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("redstonejammer", "jammable_receivers"));

    // 2. High-Performance Fastutil Spatial Chunk Partitioning (Zero GC allocations on large multiplayer worlds)
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

    public static boolean isPureSource(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(IMMUNE_JAM_TAG)) return true;
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
            b instanceof net.minecraft.world.level.block.LightningRodBlock) {
            return true;
        }

        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        return name.contains("lever") || name.contains("button") || name.contains("torch") ||
               name.contains("daylight") || name.contains("pressure_plate") ||
               regName.contains("lever") || regName.contains("button") || regName.contains("torch") ||
               regName.contains("daylight") || regName.contains("pressure_plate");
    }

    public static boolean isLaserEmitter(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        return (regName.contains("laser") || name.contains("laser") ||
                regName.contains("projector") || name.contains("projector") ||
                regName.contains("forcefield") || name.contains("forcefield") ||
                regName.contains("emitter") || name.contains("emitter") ||
                regName.contains("mffs") || name.contains("mffs")) &&
                !regName.contains("laser_bridge") && !regName.contains("laser_beam") && !regName.contains("laser_door") &&
                !name.contains("laserbridge") && !name.contains("laserbeam") && !name.contains("laserdoor");
    }

    public static boolean isLaserBeamBlock(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        return regName.contains("laser_bridge") || name.contains("laserbridge") ||
               regName.contains("laser_beam") || name.contains("laserbeam") ||
               regName.contains("laser_door") || name.contains("laserdoor") ||
               regName.contains("laser_source") || name.contains("lasersource") ||
               regName.contains("laser_field") || name.contains("laserfield") ||
               regName.contains("forcefield_block") || name.contains("forcefieldblock") ||
               regName.contains("beam_block") || name.contains("beamblock");
    }

    public static boolean isPowerSourceOrConductor(BlockState state) {
        return isPureSource(state);
    }

    public static boolean isPistonBase(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.piston.PistonBaseBlock) return true;
        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        return (name.contains("piston") || regName.contains("piston")) &&
               !name.contains("head") && !name.contains("moving") &&
               !regName.contains("head") && !regName.contains("moving");
    }

    public static boolean isPistonHead(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (b instanceof net.minecraft.world.level.block.piston.PistonHeadBlock ||
            b instanceof net.minecraft.world.level.block.piston.MovingPistonBlock) return true;
        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        return (name.contains("piston") && name.contains("head")) ||
               (name.contains("piston") && name.contains("moving")) ||
               (regName.contains("piston") && regName.contains("head")) ||
               (regName.contains("piston") && regName.contains("moving"));
    }

    public static boolean isStickyPiston(BlockState state) {
        if (state == null || state.isAir()) return false;
        net.minecraft.world.level.block.Block b = state.getBlock();
        if (state.is(net.minecraft.world.level.block.Blocks.STICKY_PISTON)) return true;
        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        return name.contains("sticky") || regName.contains("sticky");
    }

    public static Direction getPistonFacing(BlockState state) {
        if (state == null) return Direction.NORTH;
        if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING)) {
            return state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING);
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase("facing") && prop.getValueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                Property<Direction> dirProp = (Property<Direction>) prop;
                return state.getValue(dirProp);
            }
        }
        return Direction.NORTH;
    }

    public static boolean isPistonExtended(BlockState state) {
        if (state == null) return false;
        if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)) {
            return state.getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED);
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase("extended") && prop instanceof BooleanProperty boolProp) {
                return state.getValue(boolProp);
            }
        }
        return false;
    }

    public static BlockState setPistonRetracted(BlockState state) {
        if (state == null) return state;
        if (state.hasProperty(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)) {
            return state.setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED, false);
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equalsIgnoreCase("extended") && prop instanceof BooleanProperty boolProp) {
                return state.setValue(boolProp, false);
            }
        }
        return state;
    }

    public static boolean isJammableReceiver(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(IMMUNE_JAM_TAG)) return false;
        if (isPureSource(state)) return false;
        if (state.is(JAMMABLE_RECEIVER_TAG)) return true;
        if (isPistonBase(state) || isPistonHead(state) || isLaserEmitter(state) || isLaserBeamBlock(state)) return true;

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

        String name = b.getClass().getName().toLowerCase();
        String regName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath().toLowerCase();
        if (regName.contains("laser") || name.contains("laser") ||
            regName.contains("bridge") || name.contains("bridge") ||
            regName.contains("door") || name.contains("door") ||
            regName.contains("elevator") || name.contains("elevator") ||
            regName.contains("modem") || name.contains("modem") ||
            regName.contains("computer") || name.contains("computer") ||
            regName.contains("gate") || name.contains("gate") ||
            regName.contains("latch") || name.contains("latch") ||
            regName.contains("router") || name.contains("router") ||
            regName.contains("wireless") || name.contains("wireless") ||
            regName.contains("forcefield") || name.contains("forcefield") ||
            regName.contains("projector") || name.contains("projector") ||
            regName.contains("mffs") || name.contains("mffs") ||
            regName.contains("framed") || name.contains("framed") ||
            regName.contains("refinedstorage") || name.contains("refinedstorage") ||
            regName.contains("securitycraft") || name.contains("securitycraft") ||
            regName.contains("keypad") || name.contains("keypad") ||
            regName.contains("scanner") || name.contains("scanner") ||
            regName.contains("alarm") || name.contains("alarm") ||
            regName.contains("deriver") || name.contains("deriver") ||
            regName.contains("matrix") || name.contains("matrix")) {
            return true;
        }

        for (Property<?> property : state.getProperties()) {
            String propName = property.getName().toLowerCase();
            if (propName.contains("open") || propName.contains("powered") || propName.contains("lit") ||
                propName.contains("active") || propName.contains("enabled") || propName.contains("extended") ||
                propName.contains("triggered") || propName.contains("power") || propName.contains("signal") ||
                propName.contains("level") || propName.contains("delay") || propName.contains("crafting") ||
                propName.contains("on") || propName.contains("working") || propName.contains("running")) {
                return true;
            }
        }
        return false;
    }

    public static Set<BlockPos> getConnectedStructurePositions(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        result.add(startPos.immutable());
        BlockState startState = level.getBlockState(startPos);
        if (startState.isAir() || isPureSource(startState)) return result;

        // 1. Collect all non-source, non-air blocks within 2-block radius (sphere dx^2 + dy^2 + dz^2 <= 6)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= 6) {
                        BlockPos rPos = startPos.offset(dx, dy, dz);
                        if (level.isLoaded(rPos)) {
                            BlockState rState = level.getBlockState(rPos);
                            if (!rState.isAir() && !isPureSource(rState)) {
                                result.add(rPos.immutable());
                            }
                        }
                    }
                }
            }
        }

        // 2. Doors: vertical halves & adjacent double doors for any door in the radius
        Set<BlockPos> doorPositions = new HashSet<>();
        for (BlockPos p : result) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock ||
                state.getBlock().getClass().getName().toLowerCase().contains("door")) {
                doorPositions.add(p);
            }
        }
        for (BlockPos doorPos : doorPositions) {
            BlockState dState = level.getBlockState(doorPos);
            for (int dy = -2; dy <= 2; dy++) {
                if (dy == 0) continue;
                BlockPos p = doorPos.above(dy);
                if (level.isLoaded(p) && level.getBlockState(p).is(dState.getBlock())) {
                    result.add(p.immutable());
                }
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos p = doorPos.relative(dir);
                if (level.isLoaded(p) && level.getBlockState(p).is(dState.getBlock())) {
                    result.add(p.immutable());
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos pVert = p.above(dy);
                        if (level.isLoaded(pVert) && level.getBlockState(pVert).is(dState.getBlock())) {
                            result.add(pVert.immutable());
                        }
                    }
                }
            }
        }

        // 3. Pistons: base, extended head, and quasi-connectivity position for any piston in the radius
        Set<BlockPos> currentPosSnapshot = new HashSet<>(result);
        for (BlockPos p : currentPosSnapshot) {
            BlockState pState = level.getBlockState(p);
            if (isPistonBase(pState)) {
                result.add(p.above().immutable());
                Direction facing = getPistonFacing(pState);
                BlockPos headPos = p.relative(facing);
                result.add(headPos.immutable());
                if (isStickyPiston(pState) && isPistonExtended(pState)) {
                    result.add(p.relative(facing, 2).immutable());
                }
            } else if (isPistonHead(pState)) {
                Direction facing = getPistonFacing(pState);
                BlockPos basePos = p.relative(facing.getOpposite());
                result.add(basePos.immutable());
                result.add(basePos.above().immutable());
            }
        }

        // 4. Pulse Path & Wire Trace (Follow connected redstone dust lines up to 64 blocks, including slopes)
        Set<BlockPos> wireQueue = new HashSet<>();
        for (BlockPos p : result) {
            BlockState pState = level.getBlockState(p);
            if (pState.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                wireQueue.add(p);
            }
        }

        // Trace wire chains (flat + stepped up/down)
        Set<BlockPos> visitedWires = new HashSet<>();
        while (!wireQueue.isEmpty() && visitedWires.size() < 64) {
            BlockPos currentWire = wireQueue.iterator().next();
            wireQueue.remove(currentWire);
            if (visitedWires.add(currentWire)) {
                result.add(currentWire);
                // Also jam supporting block under wire so signal cannot conduct through it
                result.add(currentWire.below().immutable());

                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    // Flat neighbor
                    BlockPos flatPos = currentWire.relative(dir);
                    if (!visitedWires.contains(flatPos) && level.isLoaded(flatPos)) {
                        BlockState flatState = level.getBlockState(flatPos);
                        if (flatState.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                            wireQueue.add(flatPos.immutable());
                        }
                    }
                    // Stepped UP neighbor
                    BlockPos upPos = currentWire.above().relative(dir);
                    if (!visitedWires.contains(upPos) && level.isLoaded(upPos)) {
                        BlockState upState = level.getBlockState(upPos);
                        if (upState.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                            wireQueue.add(upPos.immutable());
                        }
                    }
                    // Stepped DOWN neighbor
                    BlockPos downPos = currentWire.below().relative(dir);
                    if (!visitedWires.contains(downPos) && level.isLoaded(downPos)) {
                        BlockState downState = level.getBlockState(downPos);
                        if (downState.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock) {
                            wireQueue.add(downPos.immutable());
                        }
                    }
                }
            }
        }

        // 5. Laser Bridges, Beams, & Forcefield Projectors (Follow beam paths up to 64 blocks)
        Set<BlockPos> laserEmitters = new HashSet<>();
        for (BlockPos p : result) {
            BlockState pState = level.getBlockState(p);
            if (isLaserEmitter(pState) || isLaserBeamBlock(pState)) {
                laserEmitters.add(p);
            }
        }
        for (BlockPos emitterPos : laserEmitters) {
            BlockState eState = level.getBlockState(emitterPos);
            Direction facing = getPistonFacing(eState);
            Set<Direction> dirs = new HashSet<>();
            if (facing != null) dirs.add(facing);
            for (Direction d : Direction.values()) {
                dirs.add(d);
            }
            for (Direction dir : dirs) {
                for (int dist = 1; dist <= 64; dist++) {
                    BlockPos bPos = emitterPos.relative(dir, dist);
                    if (!level.isLoaded(bPos)) break;
                    BlockState bState = level.getBlockState(bPos);
                    if (bState.isAir()) continue;
                    if (isLaserBeamBlock(bState) || isLaserEmitter(bState) ||
                        bState.getBlock().getClass().getName().toLowerCase().contains("laser") ||
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bState.getBlock()).getPath().toLowerCase().contains("laser")) {
                        result.add(bPos.immutable());
                    } else if (!bState.canBeReplaced()) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    public static synchronized void jamBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        BlockState state = level.getBlockState(pos);
        if (isPureSource(state)) return;

        Set<BlockPos> targets = getConnectedStructurePositions(level, pos);
        JAMMED_TARGET_STRUCTURES.put(pos.immutable(), targets);
        for (BlockPos p : targets) {
            BlockState targetState = level.getBlockState(p);
            if (!isPureSource(targetState)) {
                long chunkKey = packChunkPos(p.getX() >> 4, p.getZ() >> 4);
                CHUNK_JAMMED_POSITIONS.computeIfAbsent(chunkKey, k -> new LongOpenHashSet()).add(p.asLong());
                applyJamToBlockState(level, p);
            }
        }
    }

    public static synchronized void unjamBlock(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        Set<BlockPos> targets = JAMMED_TARGET_STRUCTURES.remove(pos.immutable());
        if (targets == null) {
            targets = getConnectedStructurePositions(level, pos);
        }
        for (BlockPos p : targets) {
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
                level.updateNeighborsAt(p, state.getBlock());
                for (Direction dir : Direction.values()) {
                    BlockPos nPos = p.relative(dir);
                    if (level.isLoaded(nPos)) {
                        BlockState nState = level.getBlockState(nPos);
                        level.updateNeighborsAt(nPos, nState.getBlock());
                    }
                }
            }
        }
    }

    public static synchronized boolean isJammed(BlockPos pos) {
        if (pos == null) return false;
        long chunkKey = packChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        LongSet set = CHUNK_JAMMED_POSITIONS.get(chunkKey);
        return set != null && set.contains(pos.asLong());
    }

    public static void applyJamToBlockState(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || isPureSource(state)) {
            long chunkKey = packChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
            LongSet set = CHUNK_JAMMED_POSITIONS.get(chunkKey);
            if (set != null) {
                set.remove(pos.asLong());
            }
            return;
        }

        // Special handling for Laser Bridge / Beam blocks: instantly dissolve to air so no lingering laser lines exist
        if (isLaserBeamBlock(state)) {
            level.removeBlockEntity(pos);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            return;
        }

        // Special handling for Laser Emitters & Projectors: sweep along line-of-sight and dissolve active bridge/beam blocks
        if (isLaserEmitter(state)) {
            for (Direction dir : Direction.values()) {
                for (int dist = 1; dist <= 64; dist++) {
                    BlockPos beamPos = pos.relative(dir, dist);
                    if (!level.isLoaded(beamPos)) break;
                    BlockState bState = level.getBlockState(beamPos);
                    if (bState.isAir()) continue;
                    if (isLaserBeamBlock(bState) ||
                        bState.getBlock().getClass().getName().toLowerCase().contains("laserbridge") ||
                        bState.getBlock().getClass().getName().toLowerCase().contains("laserbeam") ||
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bState.getBlock()).getPath().toLowerCase().contains("laser_bridge")) {
                        level.removeBlockEntity(beamPos);
                        level.setBlock(beamPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    } else if (!bState.canBeReplaced()) {
                        break;
                    }
                }
            }
        }

        // Special handling for Piston Head: delegate to its base
        if (isPistonHead(state)) {
            Direction facing = getPistonFacing(state);
            BlockPos basePos = pos.relative(facing.getOpposite());
            if (level.isLoaded(basePos)) {
                applyJamToBlockState(level, basePos);
            } else {
                level.removeBlockEntity(pos);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
            return;
        }

        // Special handling for Pistons: cleanly retract sticky / regular pistons (Vanilla & SecurityCraft) and pull attached block
        if (isPistonBase(state)) {
            if (isPistonExtended(state)) {
                Direction facing = getPistonFacing(state);
                BlockPos headPos = pos.relative(facing);
                BlockPos pulledPos = pos.relative(facing, 2);
                boolean isSticky = isStickyPiston(state);

                // 1. FIRST: set base to EXTENDED = false silently so vanilla/modded PistonHead removal never drops a piston item
                BlockState retractedState = setPistonRetracted(state);
                level.setBlock(pos, retractedState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);

                // 2. SECOND: handle pulling attached block if sticky piston (supports normal blocks, modded blocks, SecurityCraft reinforced blocks)
                if (isSticky && level.isLoaded(pulledPos)) {
                    BlockState pulledState = level.getBlockState(pulledPos);
                    boolean isMovable = !pulledState.isAir() && 
                                        !isPistonHead(pulledState) &&
                                        !pulledState.is(net.minecraft.world.level.block.Blocks.BEDROCK) &&
                                        !pulledState.is(net.minecraft.world.level.block.Blocks.BARRIER) &&
                                        !pulledState.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME);

                    if (isMovable) {
                        if (pulledState.getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.DESTROY) {
                            level.destroyBlock(pulledPos, true);
                            level.removeBlockEntity(headPos);
                            level.setBlock(headPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                        } else {
                            // Extract TileEntity / BlockEntity data if present
                            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pulledPos);
                            net.minecraft.nbt.CompoundTag tag = null;
                            if (be != null) {
                                try {
                                    tag = be.saveWithFullMetadata(level.registryAccess());
                                } catch (Exception ignored) {}
                                level.removeBlockEntity(pulledPos);
                            }

                            // Clear pulled position
                            level.setBlock(pulledPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);

                            // Clear old head position & place the pulled block (including SecurityCraft reinforced blocks)
                            level.removeBlockEntity(headPos);
                            level.setBlock(headPos, pulledState, net.minecraft.world.level.block.Block.UPDATE_ALL);

                            // Restore TileEntity / BlockEntity if present
                            if (tag != null) {
                                try {
                                    net.minecraft.world.level.block.entity.BlockEntity newBe = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(headPos, pulledState, tag, level.registryAccess());
                                    if (newBe != null) {
                                        level.setBlockEntity(newBe);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    } else {
                        level.removeBlockEntity(headPos);
                        level.setBlock(headPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    }
                } else {
                    level.removeBlockEntity(headPos);
                    level.setBlock(headPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                }

                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PISTON_CONTRACT, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
                return;
            }
        }

        // Universal Deep TileEntity / BlockEntity Jamming for Modded Receivers & Machines (Laser Bridges, CC:Tweaked, MFFS, More Red, Moving Elevators, SecurityCraft, Powah, BBL, etc.)
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            try {
                net.minecraft.nbt.CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
                boolean tagChanged = false;
                String[] boolKeys = new String[]{
                    "active", "powered", "enabled", "is_active", "laser_active", "running", "online", "open",
                    "activated", "beeping", "transmitting", "receiving", "locked", "blinking", "on", "crafting",
                    "working", "charging", "emitting"
                };
                for (String key : boolKeys) {
                    if (tag.contains(key)) {
                        tag.putBoolean(key, false);
                        tagChanged = true;
                    }
                }
                String[] intKeys = new String[]{
                    "power", "signal", "redstone", "redstone_power", "rs_power", "signal_level", "output_signal", "burn_time", "progress", "state"
                };
                for (String key : intKeys) {
                    if (tag.contains(key)) {
                        tag.putInt(key, 0);
                        tagChanged = true;
                    }
                }
                if (tagChanged) {
                    try {
                        net.minecraft.world.level.block.entity.BlockEntity reloadedBe = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
                        if (reloadedBe != null) {
                            level.setBlockEntity(reloadedBe);
                        }
                    } catch (Exception ignored) {}
                    be.setChanged();
                }
            } catch (Exception ignored) {}
        }

        // Optimization: Dirty-check fast exit (If block is already fully jammed/unpowered, don't trigger level.setBlock)
        boolean needsUpdate = false;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty boolProp) {
                String name = boolProp.getName().toLowerCase();
                if (name.contains("open") || name.contains("powered") || name.contains("lit") || 
                    name.contains("active") || name.contains("enabled") || name.contains("extended") || 
                    name.contains("triggered") || name.contains("on") || name.contains("working") || 
                    name.contains("running") || name.contains("charging") || name.contains("crafting") ||
                    name.contains("blinking") || name.contains("transmitting") || name.contains("receiving")) {
                    if (state.getValue(boolProp)) {
                        needsUpdate = true;
                        break;
                    }
                }
            } else if (property instanceof IntegerProperty intProp) {
                String name = intProp.getName().toLowerCase();
                if (name.contains("power") || name.contains("signal") || name.contains("level") || 
                    name.contains("delay") || name.contains("charge") || name.contains("stage") ||
                    name.contains("output")) {
                    if (state.getValue(intProp) > 0) {
                        needsUpdate = true;
                        break;
                    }
                }
            }
        }

        if (!needsUpdate) {
            return; // Already in dormant jammed state, 0 CPU cycles or network packets used!
        }

        BlockState newState = state;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty boolProp) {
                String name = boolProp.getName().toLowerCase();
                if (name.contains("open") || name.contains("powered") || name.contains("lit") || 
                    name.contains("active") || name.contains("enabled") || name.contains("extended") || 
                    name.contains("triggered") || name.contains("on") || name.contains("working") || 
                    name.contains("running") || name.contains("charging") || name.contains("crafting") ||
                    name.contains("blinking") || name.contains("transmitting") || name.contains("receiving")) {
                    if (state.getValue(boolProp)) {
                        newState = newState.setValue(boolProp, false);
                    }
                }
            } else if (property instanceof IntegerProperty intProp) {
                String name = intProp.getName().toLowerCase();
                if (name.contains("power") || name.contains("signal") || name.contains("level") || 
                    name.contains("delay") || name.contains("charge") || name.contains("stage") ||
                    name.contains("output")) {
                    if (state.getValue(intProp) > 0) {
                        newState = newState.setValue(intProp, 0);
                    }
                }
            }
        }

        // Flag 18 = UPDATE_CLIENTS (2) | UPDATE_KNOWN_SHAPE (16) prevents neighbor recalculation loops that pull signal from torches
        level.setBlock(pos, newState, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level == null || level.isClientSide()) return;

        synchronized (RedstoneJammerEventHandler.class) {
            if (!CHUNK_JAMMED_POSITIONS.isEmpty()) {
                var chunkIterator = CHUNK_JAMMED_POSITIONS.long2ObjectEntrySet().iterator();
                while (chunkIterator.hasNext()) {
                    var entry = chunkIterator.next();
                    long chunkKey = entry.getLongKey();
                    int chunkX = getChunkX(chunkKey);
                    int chunkZ = getChunkZ(chunkKey);

                    // Skip unloaded chunks to guarantee 0 tick lag
                    if (!level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }

                    LongSet posSet = entry.getValue();
                    long[] positions = posSet.toLongArray();
                    for (long posLong : positions) {
                        BlockPos pos = BlockPos.of(posLong);
                        if (level.isLoaded(pos)) {
                            BlockState state = level.getBlockState(pos);
                            if (state.isAir() || isPureSource(state)) {
                                posSet.remove(posLong);
                                JAMMED_TARGET_STRUCTURES.remove(pos);
                            } else {
                                applyJamToBlockState(level, pos);
                            }
                        }
                    }

                    if (posSet.isEmpty()) {
                        chunkIterator.remove();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        // Spatial chunks manage their state automatically without memory leak
    }

    @SubscribeEvent
    public void onPistonPre(PistonEvent.Pre event) {
        BlockPos pos = event.getPos();
        if (isJammed(pos) || (event.getDirection() != null && isJammed(pos.relative(event.getDirection())))) {
            // Only cancel EXTEND events! Retract events MUST run so sticky pistons can pull their blocks back and close!
            if (event.getPistonMoveType() == PistonEvent.PistonMoveType.EXTEND) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            BlockPos sourcePos = event.getPos();

            // 1. If source block itself or its vertical column is jammed, cancel neighbor notifications entirely
            if (isJammed(sourcePos) || isJammed(sourcePos.above()) || isJammed(sourcePos.below())) {
                event.setCanceled(true);
                return;
            }

            // 2. Safely strip directions pointing to jammed neighbor blocks, wire on top, solid blocks underneath, or adjacent jammed conductors
            EnumSet<Direction> notifiedSides = event.getNotifiedSides();
            if (notifiedSides != null && !notifiedSides.isEmpty()) {
                notifiedSides.removeIf(dir -> {
                    BlockPos targetPos = sourcePos.relative(dir);
                    if (isJammed(targetPos)) return true;
                    if (isJammed(targetPos.above())) return true;
                    if (isJammed(targetPos.below())) return true;
                    for (Direction d2 : Direction.values()) {
                        if (isJammed(targetPos.relative(d2))) {
                            return true;
                        }
                    }
                    return false;
                });
                if (notifiedSides.isEmpty()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        if (isJammed(pos)) {
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
    }
}
