package com.hbm.packet.toclient;

import com.hbm.packet.PacketDispatcher;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbmspace.tileentity.TESpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * Event-driven rendering snapshot. Operational satellite data stays server-side;
 * clients receive only stable type IDs and validated orbit settings.
 */
public class SatelliteSnapshotPacket implements IMessage {

    private NBTTagCompound snapshot;

    public SatelliteSnapshotPacket() { }

    public SatelliteSnapshotPacket(SatelliteSavedData data) {
        snapshot = data.createClientSnapshot();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        new PacketBuffer(buf).writeCompoundTag(snapshot);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            snapshot = new PacketBuffer(buf).readCompoundTag();
        } catch(IOException ignored) {
            snapshot = new NBTTagCompound();
        }
    }

    public static void send(EntityPlayerMP player) {
        if(player == null) return;
        SatelliteSavedData data = TESpaceUtil.getData(
                player.world, (int) Math.floor(player.posX), (int) Math.floor(player.posZ));
        PacketDispatcher.sendTo(new SatelliteSnapshotPacket(data), player);
    }

    public static void broadcastForBody(SatelliteSavedData data, EntityPlayerMP source) {
        if(data == null || source == null || source.getServer() == null) return;
        for(EntityPlayerMP player : source.getServer().getPlayerList().getPlayers()) {
            SatelliteSavedData playerData = TESpaceUtil.getData(
                    player.world, (int) Math.floor(player.posX), (int) Math.floor(player.posZ));
            if(data.getBodyKey().equals(playerData.getBodyKey())) {
                PacketDispatcher.sendTo(new SatelliteSnapshotPacket(data), player);
            }
        }
    }

    public static void broadcastForBody(SatelliteSavedData data, net.minecraft.world.World world) {
        if(data == null || world == null || world.getMinecraftServer() == null) return;
        for(EntityPlayerMP player : world.getMinecraftServer().getPlayerList().getPlayers()) {
            SatelliteSavedData playerData = TESpaceUtil.getData(
                    player.world, (int) Math.floor(player.posX), (int) Math.floor(player.posZ));
            if(data.getBodyKey().equals(playerData.getBodyKey())) {
                PacketDispatcher.sendTo(new SatelliteSnapshotPacket(data), player);
            }
        }
    }

    public static class Handler implements IMessageHandler<SatelliteSnapshotPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SatelliteSnapshotPacket message, MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                    SatelliteSavedData.applyClientSnapshot(message.snapshot));
            return null;
        }
    }
}
