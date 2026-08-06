package com.hbm.packet.toclient;

import com.hbm.capability.BackpackCapability;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BackpackSlotSyncPacket implements IMessage {
    private ItemStack backpack = ItemStack.EMPTY;

    private static ItemStack pendingBackpack = ItemStack.EMPTY;
    private static boolean hasPendingBackpack;

    public BackpackSlotSyncPacket() {
    }

    public BackpackSlotSyncPacket(ItemStack backpack) {
        this.backpack = backpack.isEmpty() ? ItemStack.EMPTY : backpack.copy();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        backpack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, backpack);
    }

    public static class Handler implements IMessageHandler<BackpackSlotSyncPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(BackpackSlotSyncPacket message, MessageContext context) {
            ItemStack syncedBackpack = message.backpack.isEmpty() ? ItemStack.EMPTY : message.backpack.copy();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    BackpackCapability.getData(player).setEquippedBackpack(syncedBackpack);
                } else {
                    pendingBackpack = syncedBackpack;
                    hasPendingBackpack = true;
                }
            });
            return null;
        }
    }

    /** Applies a login/dimension sync that arrived just before the client player existed. */
    @SideOnly(Side.CLIENT)
    public static void applyPending() {
        if (!hasPendingBackpack) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        BackpackCapability.getData(player).setEquippedBackpack(pendingBackpack);
        pendingBackpack = ItemStack.EMPTY;
        hasPendingBackpack = false;
    }

    @SideOnly(Side.CLIENT)
    public static void clearPending() {
        pendingBackpack = ItemStack.EMPTY;
        hasPendingBackpack = false;
    }
}
