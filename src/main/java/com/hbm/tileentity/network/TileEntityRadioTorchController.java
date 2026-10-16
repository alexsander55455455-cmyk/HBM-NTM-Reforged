package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.AutoRegister;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.BufferUtil;
import com.hbm.util.Compat;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;

@AutoRegister
public class TileEntityRadioTorchController extends TileEntityLoadedBase implements ITickable, IControlReceiver {

    public String channel = "";
    public String prev;
    public boolean polling = true;
    private boolean receiving;
    private boolean firstTick = true;

    @Override
    public void update() {
        if (!world.isRemote) {
            boolean receiving = false;

            if (channel != null && !channel.isEmpty()) {
                EnumFacing dir = this.getTorchFacing().getOpposite();

                TileEntity tile = Compat.getTileStandard(world, getPos().getX() + dir.getXOffset(), getPos().getY() + dir.getYOffset(), getPos().getZ() + dir.getZOffset());

                if (tile instanceof IRORInteractive ror) {

                    RTTYSystem.RTTYChannel chan = RTTYSystem.listen(world, channel);
                    if (chan != null) {
                        String rec = "" + chan.signal;
                        if ("selfdestruct".equals(rec)) {
                            world.destroyBlock(getPos(), false);
                            ExplosionVNT vnt = new ExplosionVNT(world, getPos(), 5);
                            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
                            vnt.setPlayerProcessor(new PlayerProcessorStandard());
                            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                            vnt.explode();
                            return;
                        }
                        if (chan.timeStamp >= world.getTotalWorldTime() - 1 && chan.timeStamp != -1) {
                            receiving = true;
                            boolean changed = !rec.equals(prev);
                            if (this.polling || !this.receiving || changed) {
                                try {
                                    if (!rec.isEmpty())
                                        ror.runRORFunction(IRORInteractive.resolveFunctionName(ror, rec), IRORInteractive.getParams(rec));
                                } catch (RORFunctionException _) {
                                }
                            }
                            if (changed) {
                                prev = rec;
                                this.markDirty();
                            }
                        }
                    }
                }
            }

            // The broadcast queue is empty for the first tick after loading.
            if (!this.firstTick || receiving) {
                if (this.receiving != receiving) {
                    this.receiving = receiving;
                    this.markDirty();
                }
            }
            this.firstTick = false;
            networkPackNT(50);
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(this.polling);
        BufferUtil.writeString(buf, channel);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.polling = buf.readBoolean();
        channel = BufferUtil.readString(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.polling = nbt.getBoolean("polling");
        channel = nbt.getString("channel");
        this.prev = nbt.getString("prev");
        this.receiving = nbt.hasKey("receiving") ? nbt.getBoolean("receiving") : !this.prev.isEmpty();
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("polling", polling);
        nbt.setString("channel", channel);
        if (prev != null) nbt.setString("prev", prev);
        nbt.setBoolean("receiving", receiving);
        return nbt;
    }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("polling")) this.polling = data.getBoolean("polling");
        if (data.hasKey("channel")) {
            String channel = data.getString("channel");
            if (!channel.equals(this.channel)) {
                this.channel = channel;
                this.prev = null;
                this.receiving = false;
            }
        }

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
}
