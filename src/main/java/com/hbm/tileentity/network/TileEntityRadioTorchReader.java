package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.handler.CompatHandler;
import com.hbm.interfaces.AutoRegister;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.BufferUtil;
import com.hbm.util.Compat;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraftforge.fml.common.Optional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@AutoRegister
@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityRadioTorchReader extends TileEntityLoadedBase implements IControlReceiver, ITickable, SimpleComponent, CompatHandler.OCComponent {

    public static final int MAPPING_SIZE = 8;

    public String[] channels = new String[MAPPING_SIZE];
    public String[] names = new String[MAPPING_SIZE];
    public String[] prev = new String[MAPPING_SIZE];
    private boolean[] forceUpdate = new boolean[MAPPING_SIZE];
    public boolean polling = false;

    public TileEntityRadioTorchReader() {
        Arrays.fill(channels, "");
        Arrays.fill(names, "");
        Arrays.fill(prev, "");
        Arrays.fill(forceUpdate, true);
    }

    @Override
    public void update() {

        if (!world.isRemote) {
            EnumFacing dir = getTorchFacing().getOpposite();

            TileEntity tile = Compat.getTileStandard(world, pos.getX() + dir.getXOffset(), pos.getY() + dir.getYOffset(), pos.getZ() + dir.getZOffset());

            if (tile instanceof IRORValueProvider prov) {

                for (int i = 0; i < MAPPING_SIZE; i++) {
                    String channel = channels[i];
                    String name = names[i];
                    String previous = prev[i];

                    if (channel == null || channel.isEmpty()) continue;
                    if (name == null || name.isEmpty()) continue;

                    String value = prov.provideRORValue(IRORValueProvider.resolveValueName(prov, name));
                    if (value == null) continue;

                    if (polling || forceUpdate[i] || !value.equals(previous)) {
                        RTTYSystem.broadcast(world, channel, value);
                        this.prev[i] = value;
                        this.forceUpdate[i] = false;
                    }
                }
            }

            networkPackNT(50);
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(this.polling);
        for (String channel : channels) BufferUtil.writeString(buf, channel);
        for (String name : names) BufferUtil.writeString(buf, name);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.polling = buf.readBoolean();
        for (int i = 0; i < channels.length; i++) channels[i] = BufferUtil.readString(buf);
        for (int i = 0; i < names.length; i++) names[i] = BufferUtil.readString(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.polling = nbt.getBoolean("polling");
        for (int i = 0; i < channels.length; i++) channels[i] = nbt.getString("channels" + i);
        for (int i = 0; i < names.length; i++) names[i] = nbt.getString("names" + i);
        for (int i = 0; i < prev.length; i++) prev[i] = nbt.getString("prev" + i);
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("polling", polling);
        for (int i = 0; i < channels.length; i++) nbt.setString("channels" + i, channels[i]);
        for (int i = 0; i < names.length; i++) nbt.setString("names" + i, names[i]);
        for (int i = 0; i < prev.length; i++) nbt.setString("prev" + i, prev[i]);
        return nbt;
    }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("polling")) this.polling = data.getBoolean("polling");
        for (int i = 0; i < channels.length; i++) if (data.hasKey("channels" + i)) this.setChannel(i, data.getString("channels" + i));
        for (int i = 0; i < names.length; i++) if (data.hasKey("names" + i)) this.setName(i, data.getString("names" + i));

        this.markDirty();
    }

    @Override
    public boolean hasPermission(EntityPlayer player) {
        return player.getDistance(getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5) < 16D;
    }

    public @NotNull EnumFacing getTorchFacing() {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof RadioTorchBase) {
            return state.getValue(RadioTorchBase.FACING);
        }

        int meta = this.getBlockMetadata();
        if (meta > 5) {
            meta >>= 1;
        }
        return EnumFacing.byIndex(meta);
    }

    private void setChannel(int index, String channel) {
        if (!channel.equals(this.channels[index])) {
            this.channels[index] = channel;
            this.forceUpdate[index] = true;
            this.markDirty();
        }
    }

    private void setName(int index, String name) {
        if (!name.equals(this.names[index])) {
            this.names[index] = name;
            this.forceUpdate[index] = true;
            this.markDirty();
        }
    }

    @Override
    @Optional.Method(modid = "opencomputers")
    public String getComponentName() {
        return "radio_reader";
    }

    @Callback(direct = true, limit = 4, doc = "function(index: number, channel: string) -- Sets channel name at a index")
    @Optional.Method(modid = "opencomputers")
    public Object[] setChannel(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < channels.length) this.setChannel(index, args.checkString(1));
        return new Object[] {};
    }

    @Callback(direct = true, doc = "function(index: number):string -- Gets channel name at a index")
    @Optional.Method(modid = "opencomputers")
    public Object[] getChannel(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < channels.length) return new Object[] { channels[index] };
        else return new Object[] {};
    }

    @Callback(direct = true, limit = 4, doc = "function(index: number, channel: string) -- Sets function at a index")
    @Optional.Method(modid = "opencomputers")
    public Object[] setName(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < names.length) this.setName(index, args.checkString(1));
        return new Object[] {};
    }

    @Callback(direct = true, doc = "function(index: number):string -- Gets function at a index")
    @Optional.Method(modid = "opencomputers")
    public Object[] getName(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < names.length) return new Object[] { names[index] };
        else return new Object[] {};
    }

    @Callback(direct = true, limit = 4, doc = "function(value: boolean) -- Switches state change mode to tick-based polling")
    @Optional.Method(modid = "opencomputers")
    public Object[] setPolling(Context context, Arguments args) {
        polling = args.checkBoolean(0);
        this.markDirty();
        return new Object[] {};
    }

    @Callback(direct = true, doc = "function():boolean -- Whenever the torch is set to tick-based polling")
    @Optional.Method(modid = "opencomputers")
    public Object[] getPolling(Context context, Arguments args) {
        return new Object[] { polling };
    }

    @Callback(direct = true, doc = "function(index: number):string -- Gets last received value at a index")
    @Optional.Method(modid = "opencomputers")
    public Object[] read(Context context, Arguments args) {
        int index = args.checkInteger(0);
        if (index >= 0 && index < channels.length) return new Object[] { prev[index] };
        else return new Object[] {};
    }
}
