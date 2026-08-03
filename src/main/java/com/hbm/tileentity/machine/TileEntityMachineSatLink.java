package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.AutoRegister;
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
public class TileEntityMachineSatLink extends TileEntityTickingBase implements ITickable, IRORValueProvider, IRORInteractive {

    public static final float SPEED = 0.25F;
    public static final float ACTIVE_ROT = -15F;
    public static final float ACTIVE_LIFT = -45F;
    public static final float INACTIVE_ROT = 0F;
    public static final float INACTIVE_LIFT = -85F;

    public boolean connected;
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
            connected = world.getHeight(pos.getX(), pos.getZ()) <= pos.getY()
                    && getSatelliteData().isFreqTaken(frequency);
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
        this.frequency = Math.max(0, Math.min(100_000, frequency));
        markDirty();
    }

    public void setLink(ItemStack chip) {
        setFrequency(ISatChip.getFreqS(chip));
        OrbitKey explicit = ISatChip.getOrbitKeyS(chip);
        orbitKey = explicit == null ? OrbitKey.fromWorld(world, pos.getX(), pos.getZ()) : explicit;
        markDirty();
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(connected);
        buf.writeInt(frequency);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        connected = buf.readBoolean();
        frequency = buf.readInt();
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
                PREFIX_VALUE + "connected", PREFIX_VALUE + "freq", PREFIX_VALUE + "rx",
                PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
                PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if((PREFIX_VALUE + "connected").equals(name)) return connected ? "TRUE" : "FALSE";
        if((PREFIX_VALUE + "freq").equals(name)) return Integer.toString(frequency);
        if((PREFIX_VALUE + "rx").equals(name)) {
            Satellite satellite = getSatelliteData().getSatFromFreq(frequency);
            return satellite == null ? "" : satellite.getTransmission();
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if((PREFIX_FUNCTION + "setfreq").equals(name) && params.length == 1) {
            setFrequency(IRORInteractive.parseInt(params[0], 0, 100_000));
            orbitKey = null;
        } else if((PREFIX_FUNCTION + "tx").equals(name)) {
            SatelliteSavedData data = getSatelliteData();
            Satellite satellite = data.getSatFromFreq(frequency);
            String payload = String.join(PARAM_SEPARATOR, params).trim();
            if(satellite != null && !payload.isEmpty()) {
                satellite.onCommand(world, payload.split("\\s+"));
                data.markSatelliteDirty();
            }
            SatelliteRayScan.reportEvent(world, pos.getX(), pos.getY(), pos.getZ(),
                    SatelliteRayScan.RayEvent.INFO_RADIO, 300);
            markDirty();
        }
        return null;
    }

    @Override
    public String getInventoryName() { return "container.machineSatLink"; }

    private SatelliteSavedData getSatelliteData() {
        OrbitKey local = OrbitKey.fromWorld(world, pos.getX(), pos.getZ());
        return orbitKey == null || orbitKey.equals(local)
                ? TESpaceUtil.getData(world, pos.getX(), pos.getZ())
                : SatelliteSavedData.getDataForOrbit(world, orbitKey);
    }
}
