package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.interfaces.AutoRegister;
import com.hbm.tileentity.TileEntityTickingBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Lightweight tile used by every assembled pile block except the simulation core.
 */
@AutoRegister(name = "tileentity_pile_block")
public class TileEntityPileBaseMK2 extends TileEntityTickingBase {

    private BlockPos corePos;
    private TileEntityPileCore cachedCore;

    public TileEntityPileBaseMK2 setCore(BlockPos corePos) {
        this.corePos = corePos.toImmutable();
        this.cachedCore = null;
        markDirty();
        return this;
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || corePos == null || !world.isBlockLoaded(corePos)) return;
        TileEntityPileCore core = getCore();
        if (core == null || core.isInvalid()) {
            if (world.getBlockState(pos).getBlock() == ModBlocks.pile_block) {
                BlockPile.restoreSinglePart(world, pos);
            }
        }
    }

    public TileEntityPileCore getCore() {
        if (cachedCore != null && !cachedCore.isInvalid() && cachedCore.getPos().equals(corePos)) {
            return cachedCore;
        }
        if (world == null || corePos == null || !world.isBlockLoaded(corePos)) return null;
        TileEntity tile = world.getTileEntity(corePos);
        if (tile instanceof TileEntityPileCore) {
            cachedCore = (TileEntityPileCore) tile;
            return cachedCore;
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("core", 10)) {
            NBTTagCompound core = nbt.getCompoundTag("core");
            corePos = new BlockPos(core.getInteger("x"), core.getInteger("y"), core.getInteger("z"));
        } else if (nbt.hasKey("cY")) {
            corePos = new BlockPos(nbt.getInteger("cX"), nbt.getInteger("cY"), nbt.getInteger("cZ"));
        }
        cachedCore = null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (corePos != null) {
            NBTTagCompound core = new NBTTagCompound();
            core.setInteger("x", corePos.getX());
            core.setInteger("y", corePos.getY());
            core.setInteger("z", corePos.getZ());
            nbt.setTag("core", core);
        }
        return nbt;
    }

    @Override
    public String getInventoryName() {
        return "container.pile";
    }
}
