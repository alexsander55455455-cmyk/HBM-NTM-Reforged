package com.hbmspace.packet.toserver;

import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteResolver;
import com.hbm.items.ISatChip;
import net.minecraft.item.ItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SatActivatePacket implements IMessage {

    //0: Add
    //1: Subtract
    //2: Set

    int freq;

    public SatActivatePacket()
    {

    }

    public SatActivatePacket(int freq)
    {

        this.freq = freq;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        freq = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(freq);
    }

    public static class Handler implements IMessageHandler<SatActivatePacket, IMessage> {

        @Override
        public IMessage onMessage(SatActivatePacket m, MessageContext ctx) {
            ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
                EntityPlayer p = ctx.getServerHandler().player;
                if(m.freq < 0 || m.freq > 100_000) return;
                ItemStack held = p.getHeldItemMainhand();
                com.hbm.saveddata.satellites.OrbitKey explicit = held.getItem() instanceof ISatChip
                        && ISatChip.getFreqS(held) == m.freq ? ISatChip.getOrbitKeyS(held) : null;
                SatelliteResolver.Result resolution = SatelliteResolver.resolve(p.world,
                        (int)Math.floor(p.posX), (int)Math.floor(p.posZ), m.freq, explicit, true);
                SatelliteSavedData data = resolution.getData();
                Satellite sat = resolution.getSatellite();
                if(sat != null && data != null && resolution.getContext() != null
                        && resolution.getContext().getSurfaceWorld() != null) {
                    sat.onClick(resolution.getContext().getSurfaceWorld(), ctx.getServerHandler().player, 0, 0);
                    data.markSatelliteDirty();
                }
            });

            return null;
        }
    }
}
