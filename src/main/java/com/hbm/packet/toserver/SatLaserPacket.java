package com.hbm.packet.toserver;

import com.hbm.items.ISatChip;
import com.hbm.items.tool.ItemSatInterface;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SatLaserPacket implements IMessage {

	//0: Add
	//1: Subtract
	//2: Set
	int x;
	int z;
	int freq;

	public SatLaserPacket()
	{
		
	}

	public SatLaserPacket(int x, int z, int freq)
	{
		this.x = x;
		this.z = z;
		this.freq = freq;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		z = buf.readInt();
		freq = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(z);
		buf.writeInt(freq);
	}

	public static class Handler implements IMessageHandler<SatLaserPacket, IMessage> {
		
		@Override
		public IMessage onMessage(SatLaserPacket m, MessageContext ctx) {
			ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
				EntityPlayer p = ctx.getServerHandler().player;
				if(p.getHeldItemMainhand().getItem() instanceof ItemSatInterface) {
					
					int freq = ISatChip.getFreqS(p.getHeldItemMainhand());
					
					if(freq == m.freq) {
						SatelliteResolver.Result resolution = SatelliteResolver.resolve(p.world,
								(int)Math.floor(p.posX), (int)Math.floor(p.posZ), p.getHeldItemMainhand(), true);
						SatelliteSavedData data = resolution.getData();
					    Satellite sat = resolution.getSatellite();
					    
					    if(sat != null && data != null && resolution.getContext() != null
								&& resolution.getContext().getSurfaceWorld() != null
								&& resolution.getContext().getSurfaceWorld().isBlockLoaded(new BlockPos(m.x, 0, m.z))) {
							sat.onClick(resolution.getContext().getSurfaceWorld(), ctx.getServerHandler().player, m.x, m.z);
							data.markSatelliteDirty();
						}
					}
				}
			});
			
			return null;
		}
	}
}
