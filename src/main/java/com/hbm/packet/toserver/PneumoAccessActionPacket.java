package com.hbm.packet.toserver;

import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PneumoAccessActionPacket implements IMessage {

    public static final int LEFT_CLICK = 0;
    public static final int RIGHT_CLICK = 1;
    public static final int SHIFT_CLICK = 2;

    private int windowId;
    private int action;
    private long hash;

    public PneumoAccessActionPacket() { }

    public PneumoAccessActionPacket(int windowId, int action, long hash) {
        this.windowId = windowId;
        this.action = action;
        this.hash = hash;
    }

    @Override public void fromBytes(ByteBuf buf) { windowId = buf.readInt(); action = buf.readUnsignedByte(); hash = buf.readLong(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeInt(windowId); buf.writeByte(action); buf.writeLong(hash); }

    public static class Handler implements IMessageHandler<PneumoAccessActionPacket, IMessage> {
        @Override
        public IMessage onMessage(PneumoAccessActionPacket message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                if (message.action < LEFT_CLICK || message.action > SHIFT_CLICK
                        || !(player.openContainer instanceof ContainerPneumoStorageAccess container)
                        || container.windowId != message.windowId) return;
                container.handleAccessAction(player, message.action, message.hash);
            });
            return null;
        }
    }
}
