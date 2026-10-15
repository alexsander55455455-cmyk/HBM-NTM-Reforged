package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.AutoRegister;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.CapabilityContextProvider;
import com.hbm.module.portmanager.ModulePortManPower;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.inventory.gui.GUIScreenNetworkSeparator;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.util.EnumUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Directional network separator, adapted from HbmMods X5846 (hbm). */
@AutoRegister
public class TileEntityNetworkSeparator extends TileEntityLoadedBase implements ITickable, IEnergyProviderMK2, IEnergyReceiverMK2, IControlReceiver, IRORValueProvider, IRORInteractive, IGUIProvider {

    /** Inter-tick tracker for transferred power */
    protected long deltaTracker = 0;
    /** Final power per tick over the past second */
    public long[] deltaTick = new long[20];
    /** Total transferred during the previous second */
    public long deltaSecond = 0;

    public static final long MAX_LIMIT = 10_000_000_000L;

    private long power;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public boolean redstone = false;
    public long configuredLimit = 1_000;
    public long actualLimit = configuredLimit;

    protected ModulePortManPower inPort;
    protected ModulePortManPower outPort;
    protected PortDef[] cachedOutPort;
    protected PortDef[] cachedInPort;
    protected ForgeDirection cachedDir;

    public TileEntityNetworkSeparator() {
        inPort = new ModulePortManPower(this).onlyInput();
        outPort = new ModulePortManPower(this).onlyOutput();
    }

    public ForgeDirection getDir(boolean opposite) {
        if(cachedDir == null) cachedDir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset).getRotation(ForgeDirection.DOWN);
        return opposite ? cachedDir.getOpposite() : cachedDir;
    }

    public PortDef[] getOutPort() { if(cachedOutPort == null) cachedOutPort = new PortDef[] { PortDef.make(pos.getX() + getDir(false).offsetX, pos.getY(), pos.getZ() + getDir(false).offsetZ, getDir(false)) }; return cachedOutPort; }
    public PortDef[] getInPort() { if(cachedInPort == null) cachedInPort = new PortDef[] { PortDef.make(pos.getX() + getDir(true).offsetX, pos.getY(), pos.getZ() + getDir(true).offsetZ, getDir(true)) }; return cachedInPort; }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        ForgeDirection out = getDir(false);
        BlockPos accessor = CapabilityContextProvider.getAccessor(pos);
        return (dir == out && accessor.equals(pos.add(out.offsetX, 0, out.offsetZ))) ||
                (dir == out.getOpposite() && accessor.equals(pos.add(-out.offsetX, 0, -out.offsetZ)));
    }

    @Override
    public void update() {

        if(!world.isRemote) {

            this.inPort.update(getInPort());
            this.outPort.update(getOutPort());

            this.actualLimit = this.configuredLimit;

            if(this.outPort.powerPorts[0].getNetwork() instanceof PowerNetMK2) {
                this.surveyOutgoingNetwork((PowerNetMK2) this.outPort.powerPorts[0].getNetwork());
            }

            this.actualLimit = Math.max(this.actualLimit, this.power);

            long total = deltaTracker;

            for(int i = 1; i < this.deltaTick.length; i++) {
                total += this.deltaTick[i];
                this.deltaTick[i - 1] = this.deltaTick[i];
            }

            this.deltaTick[19] = deltaTracker;
            this.deltaTracker = 0;
            this.deltaSecond = total;

            ForgeDirection dir = this.getDir(false);
            this.redstone = world.isBlockPowered(pos) ||
                    world.isBlockPowered(pos.add(dir.offsetX, 0, dir.offsetZ)) ||
                    world.isBlockPowered(pos.add(-dir.offsetX, 0, -dir.offsetZ));

            this.networkPackNT(15);
        }
    }

    @Override
    public void usePower(long power) {
        this.setPower(this.getPower() - power);
        deltaTracker += power;
    }

    protected void surveyOutgoingNetwork(PowerNetMK2 net) {
        long demand = 0;
        for(IEnergyReceiverMK2 rec : net.receiverEntries.keySet()) {
            if(rec == this || PowerNetMK2.isBadLink(rec)) continue;
            long required = rec instanceof TileEntityNetworkSeparator
                    ? ((TileEntityNetworkSeparator) rec).configuredLimit
                    : Math.min(Math.max(0, rec.getMaxPower() - rec.getPower()), rec.getReceiverSpeed());
            demand += Math.min(Math.max(0, required), configuredLimit - demand);
            if(demand >= configuredLimit) break;
        }
        this.actualLimit = demand;
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        inPort.destroy();
        outPort.destroy();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        inPort.destroy();
        outPort.destroy();
    }

    @Override public boolean allowDirectProvision() { return false; }
    @Override public long getReceiverSpeed() { return redstone || bufferMismatch() ? 0 : this.getMaxPower(); }
    @Override public long getProviderSpeed() { return redstone ? 0 : this.configuredLimit; }
    @Override public long getMaxPower() { return actualLimit; }
    @Override public long getPower() { return power; }
    @Override public void setPower(long power) {
        if(this.power != power) { this.power = power; if(world != null) markChanged(); }
    }
    @Override public ConnectionPriority getPriority() { return this.priority; }

    public boolean bufferMismatch() {
        return this.configuredLimit < this.getMaxPower();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.power = nbt.getLong("power");
        if(nbt.hasKey("configuredLimit")) this.configuredLimit = Math.max(0, Math.min(MAX_LIMIT, nbt.getLong("configuredLimit")));
        this.actualLimit = Math.max(this.configuredLimit, this.power);
        if(nbt.hasKey("p")) this.priority = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, nbt.getByte("p"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("power", power);
        nbt.setLong("configuredLimit", configuredLimit);
        nbt.setLong("actualLimit", actualLimit);
        nbt.setByte("p", (byte) this.priority.ordinal());
        return super.writeToNBT(nbt);
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) priority.ordinal());
        buf.writeBoolean(redstone);
        buf.writeLong(configuredLimit);
        buf.writeLong(deltaTick[19]);
        buf.writeLong(deltaSecond);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        priority = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, buf.readByte());
        redstone = buf.readBoolean();
        configuredLimit = buf.readLong();
        deltaTick[19] = buf.readLong();
        deltaSecond = buf.readLong();
    }

    @Override
    public boolean hasPermission(EntityPlayer player) {
        return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 128;
    }

    @Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) { return null; }
    @Override @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) { return new GUIScreenNetworkSeparator(this); }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if(data.hasKey("limit")) this.configuredLimit = data.getLong("limit");
        if(data.hasKey("priority")) this.priority = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, data.getByte("priority"));
        if(configuredLimit < 0) configuredLimit = 0;
        if(configuredLimit > MAX_LIMIT) configuredLimit = MAX_LIMIT;
        this.markDirty();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "deltatick",
                PREFIX_VALUE + "deltasecond",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
                PREFIX_FUNCTION + "setlimit" + NAME_SEPARATOR + "limit",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if((PREFIX_VALUE + "deltatick").equals(name))   return "" + deltaTick[19];
        if((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaSecond;
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            int priority = IRORInteractive.parseInt(params[0], 0, 2) + 1;
            ConnectionPriority p = EnumUtil.grabEnumSafely(ConnectionPriority.VALUES, priority);
            this.priority = p;
            this.markChanged();
            return null;
        }

        if((PREFIX_FUNCTION + "setlimit").equals(name) && params.length > 0) {
            this.configuredLimit = IRORInteractive.parseLong(params[0], 0, MAX_LIMIT);
            this.markChanged();
            return null;
        }

        return null;
    }

    AxisAlignedBB bb = null;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(bb == null) bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
