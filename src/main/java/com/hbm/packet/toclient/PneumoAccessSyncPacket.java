package com.hbm.packet.toclient;

import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PneumoAccessSyncPacket implements IMessage {

    public static final int MAX_DELTAS = 48;
    private int windowId;
    private byte[] types = new byte[0];
    private long[] hashes = new long[0];
    private ItemStack[] stacks = new ItemStack[0];
    private long[] amounts = new long[0];

    public PneumoAccessSyncPacket() { }

    public PneumoAccessSyncPacket(int windowId, byte[] types, long[] hashes, ItemStack[] stacks, long[] amounts) {
        if (types == null || hashes == null || stacks == null || amounts == null || types.length != hashes.length
                || types.length != stacks.length || types.length != amounts.length || types.length > MAX_DELTAS) {
            throw new IllegalArgumentException("Invalid pneumatic access delta batch");
        }
        this.windowId = windowId;
        this.types = types.clone();
        this.hashes = hashes.clone();
        this.stacks = new ItemStack[stacks.length];
        this.amounts = amounts.clone();
        for (int i = 0; i < stacks.length; i++) this.stacks[i] = stacks[i] == null ? ItemStack.EMPTY : stacks[i].copy();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        int count = buf.readUnsignedByte();
        if (count > MAX_DELTAS) throw new DecoderException("Invalid pneumatic access delta count " + count);
        types = new byte[count];
        hashes = new long[count];
        stacks = new ItemStack[count];
        amounts = new long[count];
        for (int i = 0; i < count; i++) {
            types[i] = buf.readByte();
            hashes[i] = buf.readLong();
            stacks[i] = ByteBufUtils.readItemStack(buf);
            amounts[i] = Math.max(0L, buf.readLong());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeByte(types.length);
        for (int i = 0; i < types.length; i++) {
            buf.writeByte(types[i]);
            buf.writeLong(hashes[i]);
            ByteBufUtils.writeItemStack(buf, stacks[i]);
            buf.writeLong(amounts[i]);
        }
    }

    public static class Handler implements IMessageHandler<PneumoAccessSyncPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PneumoAccessSyncPacket message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null || !(player.openContainer instanceof ContainerPneumoStorageAccess container)
                        || container.windowId != message.windowId) return;
                container.applyDeltas(message.types, message.hashes, message.stacks, message.amounts);
            });
            return null;
        }
    }
}
