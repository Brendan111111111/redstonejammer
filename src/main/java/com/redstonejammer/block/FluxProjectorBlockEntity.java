package com.redstonejammer.block;

import com.redstonejammer.RedstoneJammerEventHandler;
import com.redstonejammer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class FluxProjectorBlockEntity extends BlockEntity {

    private final List<BlockPos> linkedTargets = new ArrayList<>();
    private boolean isPowered = false;

    public FluxProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_PROJECTOR_BE.get(), pos, state);
    }

    public void linkTarget(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (!linkedTargets.contains(immutable)) {
            linkedTargets.add(immutable);
            setChanged();
            if (level != null && !level.isClientSide()) {
                if (isPowered) {
                    RedstoneJammerEventHandler.jamBlock(level, immutable);
                }
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public void unlinkTarget(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (linkedTargets.remove(immutable)) {
            if (level != null && !level.isClientSide()) {
                RedstoneJammerEventHandler.unjamBlock(level, immutable);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
            setChanged();
        }
    }

    public List<BlockPos> getLinkedTargets() {
        return linkedTargets;
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean powered) {
        if (this.isPowered != powered) {
            this.isPowered = powered;
            setChanged();
            if (level != null && !level.isClientSide()) {
                updateJamming();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            setPowered(false);
        }
        super.setRemoved();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluxProjectorBlockEntity be) {
        if (level == null || level.isClientSide()) return;

        boolean currentlyPowered = level.hasNeighborSignal(pos);
        if (be.isPowered != currentlyPowered) {
            be.isPowered = currentlyPowered;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

            if (be.isPowered) {
                for (BlockPos target : be.linkedTargets) {
                    RedstoneJammerEventHandler.jamBlock(level, target);
                }
            } else {
                for (BlockPos target : be.linkedTargets) {
                    RedstoneJammerEventHandler.unjamBlock(level, target);
                }
            }
        }
    }

    private void updateJamming() {
        if (level == null || level.isClientSide()) return;

        for (BlockPos target : linkedTargets) {
            if (isPowered) {
                RedstoneJammerEventHandler.jamBlock(level, target);
            } else {
                RedstoneJammerEventHandler.unjamBlock(level, target);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IsPowered", isPowered);
        tag.putInt("TargetCount", linkedTargets.size());
        for (int i = 0; i < linkedTargets.size(); i++) {
            tag.putLong("Target_" + i, linkedTargets.get(i).asLong());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        this.isPowered = input.getBooleanOr("IsPowered", false);
        linkedTargets.clear();
        int count = input.getIntOr("TargetCount", 0);
        for (int i = 0; i < count; i++) {
            long l = input.getLongOr("Target_" + i, 0L);
            if (l != 0L) {
                linkedTargets.add(BlockPos.of(l));
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("IsPowered", isPowered);
        output.putInt("TargetCount", linkedTargets.size());
        for (int i = 0; i < linkedTargets.size(); i++) {
            output.putLong("Target_" + i, linkedTargets.get(i).asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.isPowered = input.getBooleanOr("IsPowered", false);
        linkedTargets.clear();
        int count = input.getIntOr("TargetCount", 0);
        for (int i = 0; i < count; i++) {
            long l = input.getLongOr("Target_" + i, 0L);
            if (l != 0L) {
                linkedTargets.add(BlockPos.of(l));
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FluxProjectorBlockEntity be) {
        if (!level.isClientSide()) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;

        // 3. Client Optimization: Distance Culling (Only render particles if player is within 48 blocks)
        double distSq = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distSq > 48.0 * 48.0) return;

        boolean holdingWand = mc.player.getMainHandItem().is(com.redstonejammer.registry.ModItems.RESONANCE_DISRUPTOR_WAND.get())
                           || mc.player.getOffhandItem().is(com.redstonejammer.registry.ModItems.RESONANCE_DISRUPTOR_WAND.get());

        if (holdingWand && !be.linkedTargets.isEmpty()) {
            for (BlockPos target : be.linkedTargets) {
                spawnFluxLaserParticles(level, pos, target, be.isPowered);
            }
        }
    }

    private static void spawnFluxLaserParticles(Level level, BlockPos startPos, BlockPos targetPos, boolean isPowered) {
        double startX = startPos.getX() + 0.5;
        double startY = startPos.getY() + 0.5;
        double startZ = startPos.getZ() + 0.5;

        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY() + 0.5;
        double targetZ = targetPos.getZ() + 0.5;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double dz = targetZ - startZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1) return;

        // High particle density for a solid laser beam appearance
        int steps = Math.max(6, (int) (distance * 8));
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        // BLUE laser when powered & jamming (0x00A2FF), YELLOW laser when unpowered (0xFFDD00)
        int laserColor = isPowered ? 0x00A2FF : 0xFFDD00;
        net.minecraft.core.particles.DustParticleOptions dustParticle =
            new net.minecraft.core.particles.DustParticleOptions(laserColor, 0.85f);

        for (int i = 0; i <= steps; i++) {
            double px = startX + stepX * i;
            double py = startY + stepY * i;
            double pz = startZ + stepZ * i;

            // Dense straight beam ray
            level.addParticle(dustParticle, px, py, pz, 0, 0, 0);

            // Glowing focal core running through the center of the laser beam
            if (i % 3 == 0) {
                if (isPowered) {
                    level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        px, py, pz, 0, 0, 0
                    );
                } else {
                    level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        px, py, pz, 0, 0, 0
                    );
                }
            }
        }
    }
}
