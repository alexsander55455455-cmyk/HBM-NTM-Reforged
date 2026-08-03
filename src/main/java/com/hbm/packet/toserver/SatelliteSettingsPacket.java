package com.hbm.packet.toserver;

import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.OrbitSettings;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

/**
 * Applies orbit settings to the held payload. Every numeric field is clamped
 * server-side and ownership is always derived from the authenticated sender.
 */
public class SatelliteSettingsPacket implements IMessage {

    int handOrdinal;
    int frequency;
    boolean claimOwner;
    NBTTagCompound settings;

    public SatelliteSettingsPacket() { }

    public SatelliteSettingsPacket(EnumHand hand, int frequency, boolean claimOwner, NBTTagCompound settings) {
        this.handOrdinal = hand.ordinal();
        this.frequency = frequency;
        this.claimOwner = claimOwner;
        this.settings = settings == null ? new NBTTagCompound() : settings;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(handOrdinal);
        buf.writeInt(frequency);
        buf.writeBoolean(claimOwner);
        new PacketBuffer(buf).writeCompoundTag(settings);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        handOrdinal = buf.readUnsignedByte();
        frequency = buf.readInt();
        claimOwner = buf.readBoolean();
        try {
            settings = new PacketBuffer(buf).readCompoundTag();
        } catch(IOException ignored) {
            settings = new NBTTagCompound();
        }
    }

    public static class Handler implements IMessageHandler<SatelliteSettingsPacket, IMessage> {
        @Override
        public IMessage onMessage(SatelliteSettingsPacket message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(message, player));
            return null;
        }

        private static void apply(SatelliteSettingsPacket message, EntityPlayerMP player) {
            if(!isValidHandOrdinal(message.handOrdinal)) return;
            EnumHand hand = EnumHand.values()[message.handOrdinal];
            ItemStack stack = player.getHeldItem(hand);
            SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
            if(descriptor == null || !(stack.getItem() instanceof ISatChip)) return;

            Satellite sample = descriptor.create();
            OrbitSettings existing = OrbitSettings.readFromStack(stack, sample);
            OrbitSettings settings = OrbitSettings.readFromNBT(message.settings, sample);
            if(message.claimOwner) {
                settings.setOwner(player.getUniqueID(), player.getName());
            } else {
                settings.setOwner(existing.getOwnerUuid(), existing.getOwnerName());
            }
            settings.validate();
            settings.writeToStack(stack);
            ISatChip.setFreqS(stack, clampFrequency(message.frequency));
            player.inventoryContainer.detectAndSendChanges();
        }
    }

    static boolean isValidHandOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < EnumHand.values().length;
    }

    static int clampFrequency(int frequency) {
        return Math.max(0, Math.min(100_000, frequency));
    }
}
