package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.ntl.IPneumaticConnector;
import com.hbm.api.ntl.ISlotMonitorProvider;
import com.hbm.api.ntl.SlotMonitor;
import com.hbm.api.ntl.StackCache;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;

public abstract class TileEntityPneumaticStorageBase extends TileEntityMachineBase implements ITickable, IPneumaticConnector,
        IFluidStandardReceiverMK2, ISlotMonitorProvider, IControlReceiver, IGUIProvider {

    public final FluidTankNTM compair;
    public final SlotMonitor[] monitors;
    protected TileEntityPneumoTube.PneumaticNode node;
    private boolean wasAvailable;
    private int previousPressure = 1;

    protected TileEntityPneumaticStorageBase(int slots) {
        super(slots);
        compair = new FluidTankNTM(Fluids.AIR, 4_000).withOwner(this).withPressure(1);
        monitors = new SlotMonitor[slots];
        for (int i = 0; i < slots; i++) monitors[i] = new SlotMonitor(i, this);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        ensureNode();
        PneumaticNetwork network = getRelevantNetwork();
        if (network != null) network.addStorage(this);

        if (world.getTotalWorldTime() % 10 == 0) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                trySubscribe(compair.getTankType(), world, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY,
                        pos.getZ() + dir.offsetZ, dir);
            }
        }

        boolean available = isAvailable();
        int pressure = compair.getPressure();
        if (available != wasAvailable || pressure != previousPressure) {
            wasAvailable = available;
            previousPressure = pressure;
            for (SlotMonitor monitor : monitors) monitor.availabilityHasChanged();
        }

        if (compair.getFill() > 0) {
            int consumption = (int) Math.ceil(compair.getFill() * 9D / compair.getMaxFill()) + 1;
            compair.setFill(Math.max(0, compair.getFill() - consumption));
        }

        updateMonitors();
        networkPackNT(15);
    }

    protected void ensureNode() {
        if (node != null && !node.expired) return;
        node = UniNodespace.getNode(world, pos, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        if (node == null || node.expired) {
            node = new TileEntityPneumoTube.PneumaticNode(pos).setConnections(
                    new DirPos(pos.getX() + 1, pos.getY(), pos.getZ(), Library.POS_X),
                    new DirPos(pos.getX() - 1, pos.getY(), pos.getZ(), Library.NEG_X),
                    new DirPos(pos.getX(), pos.getY() + 1, pos.getZ(), Library.POS_Y),
                    new DirPos(pos.getX(), pos.getY() - 1, pos.getZ(), Library.NEG_Y),
                    new DirPos(pos.getX(), pos.getY(), pos.getZ() + 1, Library.POS_Z),
                    new DirPos(pos.getX(), pos.getY(), pos.getZ() - 1, Library.NEG_Z));
            UniNodespace.createNode(world, node);
        }
    }

    public boolean isAvailable() {
        return isLoaded && !isInvalid() && compair.getFill() > 0;
    }

    @Override
    public boolean hasPermission(EntityPlayer player) {
        return isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("pressure")) {
            int pressure = compair.getPressure() + 1;
            if (pressure > 5) pressure = 1;
            compair.withPressure(pressure);
            for (SlotMonitor monitor : monitors) monitor.availabilityHasChanged();
            markDirty();
            dataChanged();
        }
    }

    @Override
    public void invalidate() {
        detachFromNetwork(true);
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        detachFromNetwork(true);
        super.onChunkUnload();
    }

    private void detachFromNetwork(boolean destroyNode) {
        if (world == null || world.isRemote) return;
        PneumaticNetwork network = getRelevantNetwork();
        if (network != null) network.removeStorage(this);
        for (SlotMonitor monitor : monitors) monitor.detachFromAllCaches();
        if (destroyNode && node != null) UniNodespace.destroyNode(world, pos, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        node = null;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        compair.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        compair.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        compair.readFromNBT(nbt, "tank");
        previousPressure = compair.getPressure();
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        compair.writeToNBT(nbt, "tank");
        return super.writeToNBT(nbt);
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { compair }; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] { compair }; }
    @Override public SlotMonitor[] getMonitors() { return monitors; }
    @Override public net.minecraft.item.ItemStack getSlotAt(int index) { return inventory.getStackInSlot(index); }

    @Override
    public PneumaticNetwork getRelevantNetwork() {
        return node != null && !node.expired && node.hasValidNet() ? node.net : null;
    }

    @Override
    public boolean isAvailableToCache(StackCache cache) {
        if (!isAvailable() || cache == null || cache.hasExpired) return false;
        int range = TileEntityPneumoTube.getRangeFromPressure(compair.getPressure());
        long dx = pos.getX() - cache.x;
        long dy = pos.getY() - cache.y;
        long dz = pos.getZ() - cache.z;
        return dx * dx + dy * dy + dz * dz <= (long) range * range;
    }
}
