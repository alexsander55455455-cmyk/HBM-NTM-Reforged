package com.hbm.packet.toserver;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Reserved legacy discriminator. Creative backpack clicks are handled by the
 * vanilla Creative slot-update path; accepting this packet must not mutate
 * inventory state.
 */
public class BackpackCreativeTrashPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<BackpackCreativeTrashPacket, IMessage> {

        @Override
        public IMessage onMessage(BackpackCreativeTrashPacket message, MessageContext context) {
            return null;
        }
    }
}
