package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.AutoRegister;
import com.hbm.handler.CompatHandler;
import com.hbm.saveddata.satellites.SatelliteOperationContext;
import com.hbm.saveddata.satellites.SatelliteHandle;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.BufferUtil;
import net.minecraft.util.text.ITextComponent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraftforge.fml.common.Optional;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.OrbitKey;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.tileentity.TileEntityTickingBase;
import com.hbmspace.tileentity.TESpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@AutoRegister(name = "tileentity_machine_satlink")
@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityMachineSatLink extends TileEntityTickingBase implements ITickable, IRORValueProvider, IRORInteractive, SimpleComponent, CompatHandler.OCComponent {

    public static final float SPEED = 0.25F;
    public static final float ACTIVE_ROT = -15F;
    public static final float ACTIVE_LIFT = -45F;
    public static final float INACTIVE_ROT = 0F;
    public static final float INACTIVE_LIFT = -85F;

    public boolean connected;
    public volatile ITextComponent[] info = new ITextComponent[0];
    private int frequency;
    private OrbitKey orbitKey;
    public float rot = INACTIVE_ROT;
    public float prevRot = INACTIVE_ROT;
    public float lift = INACTIVE_LIFT;
    public float prevLift = INACTIVE_LIFT;

    @Override
    public void update() {
        if(world == null) return;
        if(!world.isRemote) {
            // Match the original station check. The antenna's own dummy blocks
            // occupy the column above the controller, so canSeeSky(pos.up())
            // incorrectly treats every completed station as obstructed.
            SatelliteSavedData data = getSatelliteData();
            connected = world.getHeight(pos.getX(), pos.getZ()) <= pos.getY()
                    && data.isFreqTaken(frequency);
            Satellite satellite = connected ? data.getSatFromFreq(frequency) : null;
            info = satellite == null ? new ITextComponent[0]
                    : satellite.getInfo(SatelliteOperationContext.create(world, new SatelliteHandle(data.getOrbitKey(), frequency)));
            networkPackNT(150);
            return;
        }
        prevRot = rot;
        prevLift = lift;
        rot = approach(rot, connected ? ACTIVE_ROT : INACTIVE_ROT);
        lift = approach(lift, connected ? ACTIVE_LIFT : INACTIVE_LIFT);
    }

    private float approach(float value, float target) {
        if(Math.abs(value - target) <= SPEED) return target;
        return value < target ? value + SPEED : value - SPEED;
    }

    public int getFrequency() { return frequency; }

    public void setFrequency(int frequency) {
        int clamped = Math.max(0, Math.min(100_000, frequency));
        if(this.frequency != clamped) {
            this.frequency = clamped;
            connected = false;
            info = new ITextComponent[0];
        }
        markDirty();
    }

    public void setLink(ItemStack chip) {
        setFrequency(ISatChip.getFreqS(chip));
        OrbitKey explicit = ISatChip.getOrbitKeyS(chip);
        orbitKey = explicit == null ? OrbitKey.fromWorld(world, pos.getX(), pos.getZ()) : explicit;
        connected = false;
        info = new ITextComponent[0];
        markDirty();
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(connected);
        buf.writeInt(frequency);
        buf.writeInt(info.length);
        for(ITextComponent line : info) BufferUtil.writeString(buf, ITextComponent.Serializer.componentToJson(line));
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        connected = buf.readBoolean();
        frequency = buf.readInt();
        int length = buf.readInt();
        if(length < 0 || length > 16) throw new IllegalArgumentException("Invalid satellite info count: " + length);
        ITextComponent[] received = new ITextComponent[length];
        for(int i = 0; i < length; i++) received[i] = ITextComponent.Serializer.jsonToComponent(BufferUtil.readString(buf));
        info = received;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        setFrequency(nbt.getInteger("freq"));
        orbitKey = OrbitKey.parse(nbt.getString("orbitKey"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("freq", frequency);
        if(orbitKey != null) nbt.setString("orbitKey", orbitKey.asString());
        return nbt;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-2, 0, -2), pos.add(3, 10, 3));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() { return 65536D; }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "connected", PREFIX_VALUE + "freq", PREFIX_VALUE + "rx", PREFIX_VALUE + "type",
                PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
                PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload",
                PREFIX_FUNCTION + "txrx" + NAME_SEPARATOR + "return freq" + PARAM_SEPARATOR + "payload"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if((PREFIX_VALUE + "connected").equals(name)) return connected ? "TRUE" : "FALSE";
        if((PREFIX_VALUE + "freq").equals(name)) return Integer.toString(frequency);
        if((PREFIX_VALUE + "rx").equals(name) || (PREFIX_VALUE + "type").equals(name)) {
            Satellite satellite = world == null ? null : getSatelliteData().getSatFromFreq(frequency);
            return satellite == null ? "" : (PREFIX_VALUE + "type").equals(name) ? satellite.getType() : satellite.getTransmission();
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if((PREFIX_FUNCTION + "setfreq").equals(name) && params.length == 1) {
            setFrequency(IRORInteractive.parseInt(params[0], 0, 100_000));
            orbitKey = null;
        } else if((PREFIX_FUNCTION + "tx").equals(name) && params.length > 0) {
            sendCommand(String.join(PARAM_SEPARATOR, params));
        } else if((PREFIX_FUNCTION + "txrx").equals(name) && params.length > 1) {
            String payload = String.join(PARAM_SEPARATOR, java.util.Arrays.copyOfRange(params, 1, params.length));
            if(sendCommand(payload)) RTTYSystem.broadcast(world, params[0], provideRORValue(PREFIX_VALUE + "rx"));
        }

        return null;
    }

    public boolean sendCommand(String payload) {
        if(world == null || world.isRemote || payload == null || payload.trim().isEmpty()) return false;
        SatelliteSavedData data = getSatelliteData();
        Satellite satellite = data.getSatFromFreq(frequency);
        SatelliteOperationContext context = SatelliteOperationContext.create(world, new SatelliteHandle(data.getOrbitKey(), frequency));
        boolean accepted = satellite != null && satellite.sendCommand(context, payload.trim().split("\\s+"));
        if(accepted) data.markSatelliteDirty();
        SatelliteRayScan.reportEvent(world, pos.getX(), pos.getY(), pos.getZ(), SatelliteRayScan.RayEvent.INFO_RADIO, 300);
        markDirty();
        return accepted;
    }

    @Override
    @Optional.Method(modid = "opencomputers")
    public String getComponentName() { return "ntm_satlink"; }

    @Callback(direct = true, doc = "function():boolean -- Returns connection state")
    @Optional.Method(modid = "opencomputers")
    public Object[] isConnected(Context context, Arguments args) { return new Object[] { connected }; }

    @Callback(limit = 4, doc = "function(freq:number) -- Sets frequency in the current orbit")
    @Optional.Method(modid = "opencomputers")
    public Object[] setFreq(Context context, Arguments args) { setFrequency(args.checkInteger(0)); return new Object[0]; }

    @Callback(direct = true, doc = "function():number -- Gets satellite frequency")
    @Optional.Method(modid = "opencomputers")
    public Object[] getFreq(Context context, Arguments args) { return new Object[] { frequency }; }

    @Callback(doc = "function():string -- Gets satellite type")
    @Optional.Method(modid = "opencomputers")
    public Object[] getType(Context context, Arguments args) { return new Object[] { provideRORValue(PREFIX_VALUE + "type") }; }

    @Callback(limit = 4, doc = "function(command:string) -- Transmits a satellite command")
    @Optional.Method(modid = "opencomputers")
    public Object[] send(Context context, Arguments args) {
        if(!sendCommand(args.checkString(0))) throw new IllegalStateException("Satellite unavailable or target surface not loaded");
        return new Object[0];
    }

    @Callback(doc = "function():string -- Gets the last satellite response")
    @Optional.Method(modid = "opencomputers")
    public Object[] read(Context context, Arguments args) { return new Object[] { provideRORValue(PREFIX_VALUE + "rx") }; }

    @Override
    @Optional.Method(modid = "opencomputers")
    public String[] methods() { return new String[] { "isConnected", "setFreq", "getFreq", "getType", "send", "read" }; }

    @Override
    @Optional.Method(modid = "opencomputers")
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        switch(method) {
            case "isConnected": return isConnected(context, args);
            case "setFreq": return setFreq(context, args);
            case "getFreq": return getFreq(context, args);
            case "getType": return getType(context, args);
            case "send": return send(context, args);
            case "read": return read(context, args);
            default: throw new NoSuchMethodException(method);
        }
    }

    @Override
    public String getInventoryName() { return "container.machineSatLink"; }

    public SatelliteSavedData getSatelliteData() {
        OrbitKey local = OrbitKey.fromWorld(world, pos.getX(), pos.getZ());
        return orbitKey == null || orbitKey.equals(local)
                ? TESpaceUtil.getData(world, pos.getX(), pos.getZ())
                : SatelliteSavedData.getDataForOrbit(world, orbitKey);
    }
}
