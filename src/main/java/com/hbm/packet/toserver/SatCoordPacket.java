package com.hbm.packet.toserver;

import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SatCoordPacket implements IMessage {

	int x;
	int y;
	int z;
	int freq;

	public SatCoordPacket()
	{
		
	}

	public SatCoordPacket(int x, int y, int z, int freq)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.freq = freq;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		freq = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(freq);
	}

	public static class Handler implements IMessageHandler<SatCoordPacket, IMessage> {
		
		@Override
		public IMessage onMessage(SatCoordPacket m, MessageContext ctx) {
			ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
				EntityPlayerMP p = ctx.getServerHandler().player;
				
				if(p.getHeldItemMainhand().getItem() instanceof ISatChip) {
					
					int freq = ISatChip.getFreqS(p.getHeldItemMainhand());
					
					if(freq == m.freq) {
						SatelliteResolver.Result resolution = SatelliteResolver.resolve(p.world,
								(int)Math.floor(p.posX), (int)Math.floor(p.posZ), p.getHeldItemMainhand(), true);
						SatelliteSavedData data = resolution.getData();
					    Satellite sat = resolution.getSatellite();
					    
					    if(sat != null && data != null && resolution.getContext() != null
								&& resolution.getContext().getSurfaceWorld() != null) {
					    	sat.onCoordAction(resolution.getContext().getSurfaceWorld(), p, m.x, m.y, m.z);
							data.markSatelliteDirty();
						}
					}
				}
			});
			
			return null;
		}
	}
}
