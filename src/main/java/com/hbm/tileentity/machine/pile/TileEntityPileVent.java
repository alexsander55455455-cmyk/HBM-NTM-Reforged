package com.hbm.tileentity.machine.pile;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

@AutoRegister(name = "tileentity_pile_vent")
public class TileEntityPileVent extends TileEntityPileDeviceBase implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM compair = new FluidTankNTM(Fluids.AIR, 4_000).withPressure(1).withOwner(this);
    public boolean isActive;
    public float fan;
    public float lastFan;

    @Override
    public void update() {
        if (world == null) return;
        if (!world.isRemote) {
            EnumFacing direction = getOrientation();
            ForgeDirection forgeDirection = ForgeDirection.getOrientation(direction);
            trySubscribe(compair.getTankType(), world, pos.offset(direction),
                    ForgeDirection.getOrientation(direction));
            isActive = false;

            BlockPos channelPos = pos.offset(direction.getOpposite());
            if (world.getBlockState(channelPos).getBlock() == ModBlocks.pile_block &&
                    world.getBlockState(channelPos).getValue(BlockMeta.META) == BlockPile.META_AIR_IN) {
                TileEntity tile = world.getTileEntity(channelPos);
                if (tile instanceof TileEntityPileBaseMK2) {
                    TileEntityPileCore core = ((TileEntityPileBaseMK2) tile).getCore();
                    TileEntityPileCore.PileChannel channel =
                            core == null ? null : core.getVentilationChannel(channelPos);
                    if (channel != null) {
                        chanNum = core.getVentilationChannelNum(channel);
                        int transfer = Math.min(compair.getFill(),
                                TileEntityPileCore.PileChannel.MAX_AIR - channel.air);
                        if (transfer > 0) {
                            channel.air += transfer;
                            compair.setFill(compair.getFill() - transfer);
                            isActive = true;
                            core.markDirty();
                            markDirty();
                        }
                    }
                }
            }
            networkPackNT(35);
        } else {
            lastFan = fan;
            if (isActive) fan += 45F;
            if (fan >= 360F) {
                fan -= 360F;
                lastFan -= 360F;
            }
        }
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection direction) {
        return type == compair.getTankType() && direction == ForgeDirection.getOrientation(getOrientation());
    }

    @Override
    public @NotNull FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{compair};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{compair};
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isActive);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        isActive = buf.readBoolean();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        compair.readFromNBT(nbt, "compair");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        compair.writeToNBT(nbt, "compair");
        return nbt;
    }
}
