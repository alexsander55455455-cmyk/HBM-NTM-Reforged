package com.hbm.packet.toserver;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Reserved legacy discriminator. The old handler performed a second,
 * out-of-band Creative transaction and could race the real slot update.
 */
public class BackpackCreativeMovePacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<BackpackCreativeMovePacket, IMessage> {

        @Override
        public IMessage onMessage(BackpackCreativeMovePacket message, MessageContext context) {
            return null;
        }
    }
}
