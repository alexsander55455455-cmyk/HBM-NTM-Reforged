package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.AutoRegister;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIScreenFlowControlPump;
import com.hbm.lib.CapabilityContextProvider;
import com.hbm.lib.ForgeDirection;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.EnumUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Three-block flow-control pump, adapted from HbmMods X5846 (hbm). */
@AutoRegister
public class TileEntityFlowControlPump extends TileEntityLoadedBase implements ITickable, IFluidStandardTransceiverMK2, IControlReceiver, IGUIProvider {
    public int bufferSize = 100;
    public final FluidTankNTM[] tank = { new FluidTankNTM(Fluids.NONE, bufferSize).withOwner(this) };
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone;
    public boolean topRail, bottomRail = true, isWorking;
    public float rotor, prevRotor, rotorSpeed;
    private final ModulePortManFluidAdaptive input = new ModulePortManFluidAdaptive(this).setInputTanks(tank);
    private final ModulePortManFluidAdaptive output = new ModulePortManFluidAdaptive(this).setOutputTanks(tank);
    private PortDef[] inPorts, outPorts;
    private ForgeDirection direction;

    public ForgeDirection getDir(boolean opposite) {
        if (direction == null) direction = ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset).getRotation(ForgeDirection.DOWN);
        return opposite ? direction.getOpposite() : direction;
    }
    public PortDef[] getInPorts() {
        if (inPorts == null) {
            ForgeDirection dir = getDir(true);
            inPorts = new PortDef[] { PortDef.make(pos.getX() + dir.offsetX, pos.getY(), pos.getZ() + dir.offsetZ, dir) };
        }
        return inPorts;
    }
    public PortDef[] getOutPorts() {
        if (outPorts == null) {
            ForgeDirection dir = getDir(false);
            outPorts = new PortDef[] { PortDef.make(pos.getX() + dir.offsetX, pos.getY(), pos.getZ() + dir.offsetZ, dir) };
        }
        return outPorts;
    }
    @Override public boolean canConnect(FluidType type, ForgeDirection dir) {
        ForgeDirection out = getDir(false);
        BlockPos accessor = CapabilityContextProvider.getAccessor(pos);
        return type == tank[0].getTankType() && type != Fluids.NONE &&
                ((dir == out && accessor.equals(getOutPorts()[0].portPositions[0])) ||
                (dir == out.getOpposite() && accessor.equals(getInPorts()[0].portPositions[0])));
    }
    private boolean canReceive() {
        BlockPos accessor = CapabilityContextProvider.getAccessor(pos);
        return !redstone && !bufferMismatch() && (accessor.equals(pos) || accessor.equals(getInPorts()[0].portPositions[0]));
    }
    @Override public void update() {
        if (!world.isRemote) {
            // Shrinking the limit must not discard fluid already buffered.
            tank[0].changeTankSize(Math.max(tank[0].getFill(), bufferSize));
            ForgeDirection dir = getDir(false);
            redstone = world.isBlockPowered(pos) || world.isBlockPowered(pos.add(dir.offsetX, 0, dir.offsetZ)) ||
                    world.isBlockPowered(pos.add(-dir.offsetX, 0, -dir.offsetZ));
            input.update(getInPorts());
            output.update(getOutPorts());
            networkPackNT(15);
            isWorking = false;
        } else {
            if (world.getTotalWorldTime() % 20 == 0) {
                topRail = !world.isAirBlock(pos.up());
                bottomRail = !world.isAirBlock(pos.down());
            }
            rotorSpeed = MathHelper.clamp(rotorSpeed + (isWorking ? 1F : -1F), 0F, 30F);
            prevRotor = rotor;
            rotor += rotorSpeed;
            if (rotor >= 360F) { rotor -= 360F; prevRotor -= 360F; }
        }
    }
    @Override public long getDemand(FluidType type, int pressure) {
        return canReceive() ? IFluidStandardTransceiverMK2.super.getDemand(type, pressure) : 0;
    }
    @Override public long transferFluid(FluidType type, int pressure, long amount) {
        if (!canReceive()) return amount;
        long remainder = IFluidStandardTransceiverMK2.super.transferFluid(type, pressure, amount);
        if (remainder != amount) markChanged();
        return remainder;
    }
    @Override public void useUpFluid(FluidType type, int pressure, long amount) {
        int before = tank[0].getFill();
        IFluidStandardTransceiverMK2.super.useUpFluid(type, pressure, amount);
        if (tank[0].getFill() != before) { isWorking = true; markChanged(); }
    }
    public boolean bufferMismatch() { return bufferSize != tank[0].getMaxFill(); }
    @Override public long getReceiverSpeed(FluidType type, int pressure) { return canReceive() ? 1_000_000_000 : 0; }
    @Override public long getProviderSpeed(FluidType type, int pressure) { return redstone ? 0 : 1_000_000_000; }
    @Override public ConnectionPriority getFluidPriority() { return priority; }
    @Override public FluidTankNTM[] getSendingTanks() { return tank; }
    @Override public FluidTankNTM[] getReceivingTanks() { return tank; }
    @Override public FluidTankNTM[] getAllTanks() { return tank; }
    @Override public void invalidate() { super.invalidate(); input.destroy(); output.destroy(); }
    @Override public void onChunkUnload() { super.onChunkUnload(); input.destroy(); output.destroy(); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        tank[0].writeToNBT(nbt, "t");
        nbt.setByte("p", (byte) priority.ordinal());
        nbt.setInteger("buffer", bufferSize);
        return super.writeToNBT(nbt);
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tank[0].readFromNBT(nbt, "t");
        if (nbt.hasKey("p")) priority = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, nbt.getByte("p"));
        if (nbt.hasKey("buffer")) bufferSize = MathHelper.clamp(nbt.getInteger("buffer"), 0, 10_000);
    }
    @Override public void serialize(ByteBuf buf) {
        super.serialize(buf);
        tank[0].serialize(buf);
        buf.writeByte(priority.ordinal());
        buf.writeInt(bufferSize);
        buf.writeBoolean(redstone);
        buf.writeBoolean(isWorking);
    }
    @Override public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        tank[0].deserialize(buf);
        priority = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, buf.readByte());
        bufferSize = buf.readInt();
        redstone = buf.readBoolean();
        isWorking = buf.readBoolean();
    }
    @Override public boolean hasPermission(EntityPlayer player) { return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 128; }
    @Override public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("capacity")) bufferSize = MathHelper.clamp(data.getInteger("capacity"), 0, 10_000);
        if (data.hasKey("pressure")) tank[0].withPressure(MathHelper.clamp(data.getByte("pressure"), 0, 5));
        if (data.hasKey("priority")) priority = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, data.getByte("priority"));
        markDirty();
    }
    @Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return null; }
    @Override @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIScreenFlowControlPump(this); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 1, pos.getZ() + 2); }
    @Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 65536; }
}
