package com.hbm.tileentity;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.sound.AudioWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TileEntityLoadedBase extends TileEntity implements ILoadedTile, IBufPacketReceiver {
    private static final ByteBuf UPDATE_TAG_SCRATCH = Unpooled.buffer(64);

    public boolean isLoaded = true;
    public boolean muffled = false;
    public boolean tilted = false;
    public int tiltBlocksChecked = 0;
    public int tiltBlocksValid = 0;

    protected boolean hasDataChanged = true;
    private long lastPackedBufHash = 0L;

    /**
     * @return if the tileEntity is loaded. Note that even if it's loaded, it may be invalid!
     */
    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        isLoaded = true;
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        isLoaded = false;
    }

    /**
     * The "chunks is modified, pls don't forget to save me" effect of markDirty, minus the block updates
     */
    public void markChanged() {
        world.markChunkDirty(pos, this);
    }

    public AudioWrapper createAudioLoop() {
        return null;
    } //Vidarin: Remember to override this if you use rebootAudio!!

    public AudioWrapper rebootAudio(AudioWrapper wrapper) {
        wrapper.stopSound();
        AudioWrapper audio = createAudioLoop();
        audio.startSound();
        return audio;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        muffled = nbt.getBoolean("muffled");
        tilted = nbt.getBoolean("tilted");
        hasDataChanged = true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("muffled", muffled);
        nbt.setBoolean("tilted", tilted);
        return super.writeToNBT(nbt);
    }

    public float getVolume(float baseVolume) {
        return muffled ? baseVolume * 0.1F : baseVolume;
    }

    public void setMuffled(boolean muffled) {
        this.muffled = muffled;
        dataChanged();
    }

    public void dataChanged() {
        hasDataChanged = true;
    }

    @Override
    public final NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        UPDATE_TAG_SCRATCH.clear();
        serializeInitial(UPDATE_TAG_SCRATCH);
        byte[] bytes = new byte[UPDATE_TAG_SCRATCH.readableBytes()];
        UPDATE_TAG_SCRATCH.readBytes(bytes);
        tag.setByteArray("hbmSync", bytes);
        return tag;
    }

    @Override
    public final void handleUpdateTag(@NotNull NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (tag.hasKey("hbmSync")) {
            ByteBuf buf = Unpooled.wrappedBuffer(tag.getByteArray("hbmSync"));
            deserializeInitial(buf);
        }
    }

    @Nullable
    @Override
    public final SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    /**
     * {@inheritDoc}
     * only call super.serialize() on noisy machines. It has no effect on others.<br>
     * The final ByteBuf is compared with previous packets sent in order to avoid unnecessary traffic.<br>
     * A side effect of this is that compilation effectively runs on server thread, instead of PacketThreading IO thread;
     * Override {@link #networkPackNT(int)} if this behavior is undesirable.
     */
    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(muffled);
        buf.writeBoolean(tilted);
    }

    /**
     * {@inheritDoc}
     * only call super.deserialize() on noisy machines. It has no effect on others.<br>
     * This happens on the <strong>Netty Client IO thread</strong>!
     * Direct List modification is guaranteed to produce a CME.<br>
     */
    @Override
    public void deserialize(ByteBuf buf) {
        muffled = buf.readBoolean();
        tilted = buf.readBoolean();
    }

    /**
     * Payload emitted once per chunk-load sync via {@link #getUpdateTag()}. Defaults to the
     * per-tick {@link #serialize(ByteBuf)} payload so TEs that sync everything per-tick need no
     * extra work.
     */
    public void serializeInitial(ByteBuf buf) {
        serialize(buf);
    }

    /**
     * Symmetric counterpart to {@link #serializeInitial(ByteBuf)}. Invoked from
     * {@link #handleUpdateTag(NBTTagCompound)} on the main client thread during chunk data
     * resolution, after the standard NBT path has zeroed subclass fields, so it must not depend
     * on pre-existing field values.
     */
    public void deserializeInitial(ByteBuf buf) {
        deserialize(buf);
    }

    /**
     * Sends a sync packet that uses ByteBuf for efficient information-cramming
     */
    public void networkPackNT(int range) {
        if (world.isRemote) return;

        BufPacket packet = new BufPacket(pos.getX(), pos.getY(), pos.getZ(), this);
        ByteBuf preBuf = packet.getCompiledBuffer();

        long preHash = Library.fnv1a64(preBuf);
        if (preHash == lastPackedBufHash) {
            packet.releaseBuffer();
            return;
        }

        lastPackedBufHash = preHash;
        PacketThreading.createAllAroundThreadedPacket(packet,
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(),
                        range));
    }

    /**
     * Sends a sync packet, skipping compilation entirely when data has not changed.
     * <p>
     * TEs using this must call {@link #dataChanged()} whenever any synced field changes.
     * Failing to do so will cause clients to never receive the update.
     */
    public void networkPackMK2(int range) {
        if (world.isRemote) return;

        if (!hasDataChanged) return;

        BufPacket packet = new BufPacket(pos.getX(), pos.getY(), pos.getZ(), this);
        PacketThreading.createAllAroundThreadedPacket(packet,
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(),
                        range));
        hasDataChanged = false;
    }

    public enum TiltType {
        UNAVOIDABLE,
        CONFIG
    }

    public void checkTilt(TiltType type, boolean extraHeavy) {
        boolean doesTilt = type == TiltType.UNAVOIDABLE ||
                type == TiltType.CONFIG && (GeneralConfig.enableMachineGravity || GeneralConfig.enable528MachineGravity);

        if (!doesTilt || getFloorCount() <= 0) {
            tilted = false;
            return;
        }

        long identity = (pos.getY() + pos.getZ() * 27_644_437L) * 27_644_437L + pos.getX();
        if ((world.getTotalWorldTime() + identity) % 20 != 0) return;

        if (tiltBlocksChecked >= getFloorCount()) {
            boolean wasTilted = tilted;
            tilted = tiltBlocksValid < tiltBlocksChecked * 0.95D;
            if (tilted && !wasTilted) {
                world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 3F, 1F);
            }
            markChanged();
            tiltBlocksChecked = 0;
            tiltBlocksValid = 0;
        }

        BlockPos floorPos = getFloorPosFromIndex(tiltBlocksChecked);
        if (floorPos == null) return;

        IBlockState state = world.getBlockState(floorPos);
        tiltBlocksChecked++;

        if (extraHeavy) {
            Material material = state.getMaterial();
            if (!material.isSolid() || !state.isFullCube()) return;
            if (material == Material.SAND || material == Material.CLOTH || material == Material.GROUND) return;
            if (state.getBlock().getExplosionResistance(null) < Blocks.STONE.getExplosionResistance(null)) return;
            tiltBlocksValid++;
            return;
        }

        if (!state.isSideSolid(world, floorPos, EnumFacing.UP) || state.getMaterial() == Material.SAND) return;
        Block block = state.getBlock();
        if (block == ModBlocks.dirt_dead || block == ModBlocks.dirt_oily || block == ModBlocks.stone_cracked) return;
        tiltBlocksValid++;
    }

    public int getFloorCount() {
        return 0;
    }

    public BlockPos getFloorPosFromIndex(int index) {
        return null;
    }

    public BlockPos standardFloor3x3(int index) {
        return new BlockPos(pos.getX() - 1 + index / 2 * 2, pos.getY() - 1, pos.getZ() - 1 + index % 2 * 2);
    }

    public BlockPos standardFloor5x5(int index) {
        return new BlockPos(pos.getX() - 2 + index / 3 * 2, pos.getY() - 1, pos.getZ() - 2 + index % 3 * 2);
    }

    public BlockPos standardFloor7x7(int index) {
        return new BlockPos(pos.getX() - 3 + index / 4 * 2, pos.getY() - 1, pos.getZ() - 3 + index % 4 * 2);
    }

}
