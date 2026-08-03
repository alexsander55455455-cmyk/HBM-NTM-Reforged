package com.hbm.packet.toclient;

import com.hbm.inventory.container.ContainerBackpack;
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

import java.util.UUID;

/**
 * Atomic visible-page synchronization for the dynamically paged black-hole
 * backpack. A full page implicitly clears every omitted display slot; a delta
 * contains only cells whose prototype or true count changed.
 */
public class BlackHoleBackpackPagePacket implements IMessage {
    private int windowId;
    private UUID storageId;
    private int sequence;
    private int scrollRow;
    private int capacity;
    private int filled;
    private boolean full;
    private boolean scrollAcknowledgement;
    private int[] indices = new int[0];
    private ItemStack[] prototypes = new ItemStack[0];
    private long[] counts = new long[0];

    public BlackHoleBackpackPagePacket() {
    }

    public BlackHoleBackpackPagePacket(int windowId, UUID storageId, int sequence, int scrollRow,
                                       int capacity, int filled, boolean full, boolean scrollAcknowledgement,
                                       int[] indices,
                                       ItemStack[] prototypes, long[] counts) {
        if (storageId == null || indices == null || prototypes == null || counts == null
                || indices.length != prototypes.length || indices.length != counts.length
                || indices.length > ContainerBackpack.VISIBLE_SLOTS) {
            throw new IllegalArgumentException("Invalid black-hole backpack page");
        }
        this.windowId = windowId;
        this.storageId = storageId;
        this.sequence = sequence;
        this.scrollRow = Math.max(0, scrollRow);
        this.capacity = Math.max(0, capacity);
        this.filled = Math.max(0, filled);
        this.full = full;
        this.scrollAcknowledgement = scrollAcknowledgement;
        this.indices = indices.clone();
        this.prototypes = new ItemStack[prototypes.length];
        this.counts = counts.clone();
        for (int index = 0; index < prototypes.length; index++) {
            ItemStack prototype = prototypes[index];
            if (prototype == null || prototype.isEmpty() || this.counts[index] <= 0L) {
                this.prototypes[index] = ItemStack.EMPTY;
                this.counts[index] = 0L;
            } else {
                this.prototypes[index] = prototype.copy();
                this.prototypes[index].setCount(1);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        storageId = new UUID(buf.readLong(), buf.readLong());
        sequence = buf.readInt();
        scrollRow = buf.readInt();
        capacity = buf.readInt();
        filled = buf.readInt();
        full = buf.readBoolean();
        scrollAcknowledgement = buf.readBoolean();
        int entryCount = buf.readUnsignedByte();
        if (entryCount > ContainerBackpack.VISIBLE_SLOTS) {
            throw new DecoderException("Invalid black-hole backpack page size " + entryCount);
        }

        indices = new int[entryCount];
        prototypes = new ItemStack[entryCount];
        counts = new long[entryCount];
        for (int index = 0; index < entryCount; index++) {
            indices[index] = buf.readUnsignedByte();
            prototypes[index] = ByteBufUtils.readItemStack(buf);
            counts[index] = buf.readLong();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeLong(storageId.getMostSignificantBits());
        buf.writeLong(storageId.getLeastSignificantBits());
        buf.writeInt(sequence);
        buf.writeInt(scrollRow);
        buf.writeInt(capacity);
        buf.writeInt(filled);
        buf.writeBoolean(full);
        buf.writeBoolean(scrollAcknowledgement);
        buf.writeByte(indices.length);
        for (int index = 0; index < indices.length; index++) {
            buf.writeByte(indices[index]);
            ByteBufUtils.writeItemStack(buf, prototypes[index]);
            buf.writeLong(counts[index]);
        }
    }

    public static class Handler implements IMessageHandler<BlackHoleBackpackPagePacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(BlackHoleBackpackPagePacket message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null || !(player.openContainer instanceof ContainerBackpack container)
                        || container.windowId != message.windowId) {
                    return;
                }
                container.applyBlackHolePage(message.storageId, message.sequence, message.scrollRow,
                        message.capacity, message.filled, message.full, message.scrollAcknowledgement,
                        message.indices, message.prototypes, message.counts);
            });
            return null;
        }
    }
}
