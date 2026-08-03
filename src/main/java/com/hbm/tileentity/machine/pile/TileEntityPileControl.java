package com.hbm.tileentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.interfaces.AutoRegister;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@AutoRegister(name = "tileentity_pile_control")
public class TileEntityPileControl extends TileEntityPileDeviceBase implements IRORInteractive {

    public static final double SPEED = 1D / 60D;

    public double syncLevel;
    public double level;
    public double lastLevel;
    public int turnProgress;
    public double targetLevel;
    public boolean wasRedstone;

    @Override
    public void update() {
        if (world == null) return;
        if (!world.isRemote) {
            boolean canMove = false;
            BlockPos channelPos = pos.down();
            if (world.getBlockState(channelPos).getBlock() == ModBlocks.pile_block &&
                    world.getBlockState(channelPos).getValue(BlockMeta.META) == BlockPile.META_CONTROL) {
                TileEntity tile = world.getTileEntity(channelPos);
                if (tile instanceof TileEntityPileBaseMK2) {
                    TileEntityPileCore core = ((TileEntityPileBaseMK2) tile).getCore();
                    TileEntityPileCore.PileChannel channel =
                            core == null ? null : core.getControlChannel(channelPos);
                    if (channel != null) {
                        canMove = true;
                        chanNum = core.getControlChannelNum(channel);
                        if (channel.control != level) {
                            channel.control = level;
                            core.markDirty();
                        }
                    }
                }
            }

            if (canMove && level != targetLevel) {
                double previous = level;
                if (Math.abs(level - targetLevel) <= SPEED) level = targetLevel;
                else if (level < targetLevel) level += SPEED;
                else level -= SPEED;
                if (level != previous) markDirty();
            }

            EnumFacing direction = getOrientation();
            boolean redstone = world.getRedstonePower(pos.offset(direction), direction.getOpposite()) > 0;
            if (redstone && !wasRedstone) setTarget(1D);
            if (!redstone && wasRedstone) setTarget(0D);
            if (redstone != wasRedstone) markDirty();
            wasRedstone = redstone;
            networkPackNT(100);
        } else {
            lastLevel = level;
            if (turnProgress > 0) {
                level += (syncLevel - level) / turnProgress;
                turnProgress--;
            } else {
                level = syncLevel;
            }
        }
    }

    public void setTarget(double target) {
        double clamped = MathHelper.clamp(target, 0D, 1D);
        if (targetLevel != clamped) {
            targetLevel = clamped;
            markDirty();
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(level);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        double previous = syncLevel;
        syncLevel = buf.readDouble();
        if (syncLevel != previous) turnProgress = 2;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        level = nbt.getDouble("level");
        targetLevel = nbt.getDouble("targetLevel");
        wasRedstone = nbt.hasKey("wasRedstone") ? nbt.getBoolean("wasRedstone") : nbt.getBoolean("redstone");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("level", level);
        nbt.setDouble("targetLevel", targetLevel);
        nbt.setBoolean("wasRedstone", wasRedstone);
        return nbt;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "extendrods" + NAME_SEPARATOR + "percent"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            setTarget(IRORInteractive.parseInt(params[0], 0, 100) / 100D);
            return null;
        }
        if ((PREFIX_FUNCTION + "extendrods").equals(name) && params.length > 0) {
            setTarget(targetLevel + IRORInteractive.parseInt(params[0], -100, 100) / 100D);
            return null;
        }
        return null;
    }
}
