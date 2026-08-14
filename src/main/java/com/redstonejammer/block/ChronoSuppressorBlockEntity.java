package com.redstonejammer.block;

import com.redstonejammer.RedstoneJammerEventHandler;
import com.redstonejammer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChronoSuppressorBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {

    private static final String[] PRESET_NAMES = {
        "Alpha Unit", "Bravo Unit", "Charlie Unit", "Vault Guard", 
        "Perimeter Jammer", "Redstone Core", "Main Gate", "Secret Safehouse", "Sub-Level Jammer"
    };
    private int presetNameIndex = 0;
    private String customName = "Alpha Unit";

    @Override
    public Component getDisplayName() {
        return Component.literal(getCustomName() + " Control Panel");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        return new com.redstonejammer.menu.ChronoSuppressorMenu(containerId, playerInventory, this.worldPosition);
    }

    private static final int[] PRESET_DURATIONS_SECONDS = {1, 5, 10, 15, 30, 60, 120};
    private int durationIndex = 2; // Default 10 seconds
    private int customDurationSeconds = 10;

    private int pulseRadius = 3; // Default 3 blocks radius

    // Optimization 2: Pre-computed sphere offset tables (0 runtime geometry iterations)
    private static final BlockPos[][] PRECOMPUTED_SPHERE_OFFSETS = new BlockPos[17][];

    static {
        for (int r = 1; r <= 16; r++) {
            List<BlockPos> offsets = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= r) {
                            offsets.add(new BlockPos(dx, dy, dz));
                        }
                    }
                }
            }
            PRECOMPUTED_SPHERE_OFFSETS[r] = offsets.toArray(new BlockPos[0]);
        }
    }

    private final List<BlockPos> linkedTargets = new ArrayList<>();
    private final Set<BlockPos> activePulseTargets = new HashSet<>();
    private int activeJamTicks = 0;

    public ChronoSuppressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHRONO_SUPPRESSOR_BE.get(), pos, state);
    }

    public String getCustomName() {
        return (customName != null && !customName.isEmpty()) ? customName : "Suppressor (" + worldPosition.toShortString() + ")";
    }

    public void setCustomName(String name) {
        this.customName = name;
        setChanged();
    }

    public void cyclePresetName() {
        presetNameIndex = (presetNameIndex + 1) % PRESET_NAMES.length;
        this.customName = PRESET_NAMES[presetNameIndex];
        setChanged();
    }

    public int getDurationSeconds() {
        return customDurationSeconds;
    }

    public int getDurationTicks() {
        return getDurationSeconds() * 20;
    }

    public void setDurationSeconds(int seconds) {
        this.customDurationSeconds = Math.max(1, Math.min(120, seconds));
        setChanged();
    }

    public int getPulseRadius() {
        return pulseRadius;
    }

    public void setPulseRadius(int radius) {
        this.pulseRadius = Math.max(1, Math.min(16, radius));
        setChanged();
    }

    public void cycleDuration(Player player) {
        durationIndex = (durationIndex + 1) % PRESET_DURATIONS_SECONDS.length;
        this.customDurationSeconds = PRESET_DURATIONS_SECONDS[durationIndex];
        setChanged();
        if (player != null) {
            player.sendSystemMessage(Component.literal("§d[" + getCustomName() + "] Duration set to " 
                + getDurationSeconds() + " seconds (" + getDurationTicks() + " ticks) | Pulse Radius: " + pulseRadius + " Blocks"));
        }
    }

    public void cycleRadius(Player player) {
        this.pulseRadius = (this.pulseRadius % 16) + 1;
        setChanged();
        if (player != null) {
            player.sendSystemMessage(Component.literal("§b[" + getCustomName() + "] Pulse Radius set to " 
                + pulseRadius + " Blocks | Duration: " + getDurationSeconds() + " seconds"));
        }
    }

    public void linkTarget(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (!linkedTargets.contains(immutable)) {
            linkedTargets.add(immutable);
            setChanged();
        }
    }

    public void unlinkTarget(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (linkedTargets.remove(immutable)) {
            if (level != null && !level.isClientSide()) {
                RedstoneJammerEventHandler.unjamBlock(level, immutable);
            }
            setChanged();
        }
    }

    public List<BlockPos> getLinkedTargets() {
        return linkedTargets;
    }

    public boolean isActive() {
        return activeJamTicks > 0;
    }

    public int getActiveJamTicks() {
        return activeJamTicks;
    }

    public void triggerPulse(Player player) {
        if (level == null || level.isClientSide()) return;

        // Clear previous active pulse targets if re-triggered early
        if (!activePulseTargets.isEmpty()) {
            for (BlockPos target : activePulseTargets) {
                RedstoneJammerEventHandler.unjamBlock(level, target);
            }
            activePulseTargets.clear();
        }

        int durationTicks = getDurationTicks();
        activeJamTicks = durationTicks;
        setChanged();

        // 1. Jam explicitly linked targets
        for (BlockPos target : linkedTargets) {
            if (level.isLoaded(target)) {
                BlockState tState = level.getBlockState(target);
                if (RedstoneJammerEventHandler.isJammableReceiver(tState) ||
                    (!tState.isAir() && !tState.is(RedstoneJammerEventHandler.IMMUNE_JAM_TAG) && !RedstoneJammerEventHandler.isPureSource(tState))) {
                    RedstoneJammerEventHandler.jamBlock(level, target);
                    activePulseTargets.add(target.immutable());
                }
            }
        }

        // 2. Jam area targets within radius using pre-computed geometry tables (Instant linear lookup)
        BlockPos center = getBlockPos();
        int safeRadius = Math.max(1, Math.min(16, pulseRadius));
        BlockPos[] offsets = PRECOMPUTED_SPHERE_OFFSETS[safeRadius];
        if (offsets != null) {
            for (BlockPos offset : offsets) {
                BlockPos targetPos = center.offset(offset).immutable();
                if (level.isLoaded(targetPos)) {
                    BlockState targetState = level.getBlockState(targetPos);
                    if (RedstoneJammerEventHandler.isJammableReceiver(targetState) ||
                        (!targetState.isAir() && !targetState.is(RedstoneJammerEventHandler.IMMUNE_JAM_TAG) && !RedstoneJammerEventHandler.isPureSource(targetState) && targetState.isRedstoneConductor(level, targetPos))) {
                        RedstoneJammerEventHandler.jamBlock(level, targetPos);
                        activePulseTargets.add(targetPos);
                    }
                }
            }
        }

        if (player != null) {
            player.sendSystemMessage(Component.literal("§c[Chrono Suppressor Engaged] Timed Pulse Fired on '" + getCustomName() + "'! (Radius: " 
                + pulseRadius + "b, Duration: " + getDurationSeconds() + "s / " + durationTicks + "t)"));
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (activeJamTicks > 0) {
            activeJamTicks--;

            if (activeJamTicks <= 0) {
                // Pulse expired - cleanly unjam all targets
                for (BlockPos target : activePulseTargets) {
                    RedstoneJammerEventHandler.unjamBlock(level, target);
                }
                activePulseTargets.clear();
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("CustomName", this.customName != null ? this.customName : "");
        output.putInt("DurationSeconds", this.customDurationSeconds);
        output.putInt("PulseRadius", this.pulseRadius);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.customName = input.getStringOr("CustomName", "Alpha Unit");
        this.customDurationSeconds = input.getIntOr("DurationSeconds", 10);
        this.pulseRadius = input.getIntOr("PulseRadius", 3);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            activeJamTicks = 0;
            for (BlockPos target : activePulseTargets) {
                RedstoneJammerEventHandler.unjamBlock(level, target);
            }
            activePulseTargets.clear();
            for (BlockPos target : linkedTargets) {
                RedstoneJammerEventHandler.unjamBlock(level, target);
            }
        }
        super.setRemoved();
    }
}
