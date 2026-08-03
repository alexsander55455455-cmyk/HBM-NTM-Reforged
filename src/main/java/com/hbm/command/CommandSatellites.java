package com.hbm.command;

import com.hbm.items.ISatChip;
import com.hbm.packet.toclient.SatelliteSnapshotPacket;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteLaunchResult;
import com.hbm.saveddata.satellites.OrbitKey;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import com.hbmspace.dim.CelestialBody;
import com.hbmspace.tileentity.TESpaceUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Administrative and testing command with explicit celestial-body routing. */
public class CommandSatellites extends CommandBase {

    @Override public @NotNull String getName() { return "ntmsatellites"; }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "/ntmsatellites orbit [body] | list [body] | descend <frequency> [body]";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        String @NotNull [] args) throws CommandException {
        if(args.length == 0) throw new WrongUsageException(getUsage(sender));
        switch(args[0]) {
            case "orbit": orbit(server, sender, args); break;
            case "list": list(server, sender, args); break;
            case "descend": descend(server, sender, args); break;
            default: throw new WrongUsageException(getUsage(sender));
        }
    }

    private void orbit(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack held = player.getHeldItemMainhand();
        if(SatelliteTypeRegistry.byItem(held) == null) throw new CommandException("Held item is not a registered satellite payload");

        OrbitKey target = resolveOrbitKey(player.getServerWorld(), player.getPosition(), args.length >= 2 ? args[1] : null);
        int frequency = ISatChip.getFreqS(held);
        SatelliteLaunchResult result = SatelliteTypeRegistry.orbit(
                player.getServerWorld(), target, held.copy(), frequency, player.posX, player.posY, player.posZ, null);
        if(!result.isSuccess()) throw new CommandException("Satellite launch failed: " + result.name());
        if(!player.capabilities.isCreativeMode) held.shrink(1);
        success(sender, "Satellite " + frequency + " orbited " + target.asString());
    }

    private void list(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        WorldServer fallback = sender.getEntityWorld() instanceof WorldServer
                ? (WorldServer) sender.getEntityWorld() : server.getWorld(0);
        String bodyName = args.length >= 2 ? args[1] : null;
        SatelliteSavedData data = resolveData(sender, fallback, bodyName);
        if(data.sats.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "No satellites in " + data.getBodyKey()));
            return;
        }
        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "Satellites in " + data.getBodyKey() + ":"));
        for(Int2ObjectMap.Entry<Satellite> entry : data.sats.int2ObjectEntrySet()) {
            SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.bySatellite(entry.getValue());
            String type = descriptor == null ? entry.getValue().getClass().getSimpleName() : descriptor.getKey();
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + Integer.toString(entry.getIntKey())
                    + TextFormatting.GRAY + " - " + type));
        }
    }

    private void descend(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length < 2) throw new WrongUsageException(getUsage(sender));
        int frequency = parseInt(args[1], 0, 100_000);
        WorldServer fallback = sender.getEntityWorld() instanceof WorldServer
                ? (WorldServer) sender.getEntityWorld() : server.getWorld(0);
        String bodyName = args.length >= 3 ? args[2] : null;
        SatelliteSavedData data = resolveData(sender, fallback, bodyName);
        if(data.removeSatellite(frequency) == null) throw new CommandException("No satellite " + frequency + " in " + data.getBodyKey());
        SatelliteSnapshotPacket.broadcastForBody(data, fallback);
        success(sender, "Satellite " + frequency + " removed from " + data.getBodyKey());
    }

    private static OrbitKey resolveOrbitKey(WorldServer fallback, BlockPos pos, @Nullable String name)
            throws CommandException {
        if(name == null || name.trim().isEmpty()) {
            return OrbitKey.fromWorld(fallback, pos.getX(), pos.getZ());
        }
        for(CelestialBody body : CelestialBody.getAllBodies()) {
            if(body.name.equalsIgnoreCase(name)) return OrbitKey.body(body);
        }
        throw new CommandException("Unknown celestial body: " + name);
    }

    private static SatelliteSavedData resolveData(ICommandSender sender, WorldServer fallback,
                                                  @Nullable String name) throws CommandException {
        if(name == null || name.trim().isEmpty()) {
            BlockPos pos = sender.getPosition();
            return TESpaceUtil.getData(fallback, pos.getX(), pos.getZ());
        }
        for(CelestialBody body : CelestialBody.getAllBodies()) {
            if(body.name.equalsIgnoreCase(name)) return SatelliteSavedData.getDataForBody(fallback, body);
        }
        throw new CommandException("Unknown celestial body: " + name);
    }

    private static void success(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + text));
    }

    @Override
    public @NotNull List<String> getTabCompletions(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                                                   String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1) return getListOfStringsMatchingLastWord(args, "orbit", "list", "descend");
        if("orbit".equals(args[0]) && args.length == 2) return bodyNames(args);
        if("list".equals(args[0]) && args.length == 2) return bodyNames(args);
        if("descend".equals(args[0]) && args.length == 2) {
            BlockPos pos = sender.getPosition();
            SatelliteSavedData data = TESpaceUtil.getData(sender.getEntityWorld(), pos.getX(), pos.getZ());
            List<String> frequencies = new ArrayList<>();
            for(int frequency : data.sats.keySet()) frequencies.add(Integer.toString(frequency));
            return getListOfStringsMatchingLastWord(args, frequencies);
        }
        if("descend".equals(args[0]) && args.length == 3) return bodyNames(args);
        return Collections.emptyList();
    }

    private static List<String> bodyNames(String[] args) {
        List<String> names = new ArrayList<>();
        for(CelestialBody body : CelestialBody.getAllBodies()) names.add(body.name);
        return getListOfStringsMatchingLastWord(args, names);
    }

}
