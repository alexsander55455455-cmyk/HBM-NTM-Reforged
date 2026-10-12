package com.hbm.command;

import com.hbm.lib.HbmWorld;
import com.hbm.Tags;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.SpawnCondition;
import com.hbm.world.gen.NTMWorldGenerator;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public class CommandLocate extends CommandBase {

    private static final int MAX_DISTANCE = 256;
    private static final Map<UUID, Search> searches = new HashMap<>();

    @Override
    public String getName() {
        return "ntmlocate";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return String.format(Locale.US,
                "%s/%s structure <name> %s- Locates the nearest structure with a given name.",
                TextFormatting.GREEN, getName(), TextFormatting.LIGHT_PURPLE
        );
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(!(sender instanceof EntityPlayer))
            throw new PlayerNotFoundException("");

        if(args.length != 2)
            throw new WrongUsageException(getUsage(sender));

        if(args[0].equals("structure")) {
            EntityPlayer player = (EntityPlayer) sender;

            SpawnCondition structure = NBTStructure.getStructure(args[1]);

            if(structure == null) {
                ITextComponent message = new TextComponentTranslation("commands.locate.no_match");
                message.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(message);
                return;
            }

            Search previous = searches.remove(player.getUniqueID());
            if (previous != null) previous.stop();
            Search search = new Search(player, structure, HbmWorld.worldGenerator);
            searches.put(player.getUniqueID(), search);
            sender.sendMessage(new TextComponentTranslation("commands.locate.searching", structure.name));
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (Search search : new ArrayList<>(searches.values())) search.onTick(event);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        for (Search search : new ArrayList<>(searches.values())) {
            if (event.getWorld() == search.world) search.stop();
        }
    }

    class Search {
        final EntityPlayer player;
        final World world;
        final SpawnCondition spawn;
        final NTMWorldGenerator generator;
        final int chunkX;
        final int chunkZ;
        final ArrayDeque<ChunkPos> chunks = new ArrayDeque<>();
        final List<Chunk> loaded = new ArrayList<>();
        int radius;
        int edge;
        ChunkPos candidate;
        ChunkPos found;
        boolean expanded;

        Search(EntityPlayer player, SpawnCondition spawn, NTMWorldGenerator generator) {
            this.player = player;
            this.world = player.world;
            this.spawn = spawn;
            this.generator = generator;
            chunkX = MathHelper.floor(player.posX) >> 4;
            chunkZ = MathHelper.floor(player.posZ) >> 4;
        }

        // Only terrain near matching candidates is generated, one chunk per server tick.
        boolean step() {
            if (candidate != null) {
                if (!chunks.isEmpty()) {
                    ChunkPos pos = chunks.removeFirst();
                    loaded.add(world.getChunk(pos.x, pos.z));
                    return false;
                }
                if (!expanded) {
                    StructureStart start = generator.getStructureStartAt(world, candidate.x, candidate.z);
                    if (!NBTStructure.GenStructure.matchesStructure(start, spawn)) {
                        releaseChunks();
                        candidate = null;
                        return false;
                    }
                    StructureBoundingBox box = start.getBoundingBox();
                    // Population writes from chunk origin + 8 and needs its eastern/southern neighbors.
                    for (int x = (box.minX - 8) >> 4; x <= ((box.maxX - 8) >> 4) + 1; x++) {
                        for (int z = (box.minZ - 8) >> 4; z <= ((box.maxZ - 8) >> 4) + 1; z++) {
                            chunks.add(new ChunkPos(x, z));
                        }
                    }
                    expanded = true;
                    return false;
                }
                if (generator.getVerifiedStructureAt(world, spawn, candidate.x, candidate.z) != null) {
                    found = candidate;
                    return true;
                }
                releaseChunks();
                candidate = null;
                return false;
            }

            ChunkPos pos = nextPosition();
            if (pos == null) return true;
            StructureStart saved = generator.getStructureStartAt(world, pos.x, pos.z);
            if (saved != null ? NBTStructure.GenStructure.matchesStructure(saved, spawn)
                    : generator.getStructureAt(world, pos.x, pos.z) == spawn) {
                candidate = pos;
                expanded = false;
                for (int x = pos.x; x <= pos.x + 1; x++) {
                    for (int z = pos.z; z <= pos.z + 1; z++) chunks.add(new ChunkPos(x, z));
                }
            }
            return false;
        }

        private ChunkPos nextPosition() {
            if (radius == 0) {
                radius = 1;
                return new ChunkPos(chunkX, chunkZ);
            }
            if (radius >= MAX_DISTANCE) return null;
            int side = edge / (radius * 2);
            int offset = edge % (radius * 2);
            ChunkPos pos;
            switch (side) {
                case 0: pos = new ChunkPos(chunkX - radius + offset, chunkZ - radius); break;
                case 1: pos = new ChunkPos(chunkX + radius, chunkZ - radius + offset); break;
                case 2: pos = new ChunkPos(chunkX + radius - offset, chunkZ + radius); break;
                default: pos = new ChunkPos(chunkX - radius, chunkZ + radius - offset); break;
            }
            if (++edge == radius * 8) {
                edge = 0;
                radius++;
            }
            return pos;
        }

        void onTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (player.isDead || player.world != world || !world.playerEntities.contains(player)) {
                stop();
                return;
            }
            long deadline = System.nanoTime() + 20_000_000L;
            for (int i = 0; i < 1024; i++) {
                boolean loadingChunk = !chunks.isEmpty();
                if (step()) {
                    ITextComponent message = found == null
                            ? new TextComponentTranslation("commands.locate.none_found")
                            : new TextComponentTranslation("commands.locate.success.coordinates", spawn.name, found.x * 16, found.z * 16);
                    message.getStyle().setColor(found == null ? TextFormatting.RED : TextFormatting.GREEN);
                    player.sendMessage(message);
                    stop();
                    return;
                }
                if (loadingChunk || System.nanoTime() >= deadline) return;
            }
        }

        private void stop() {
            releaseChunks();
            searches.remove(player.getUniqueID(), this);
        }

        private void releaseChunks() {
            if (world instanceof WorldServer) {
                WorldServer serverWorld = (WorldServer) world;
                ChunkProviderServer provider = serverWorld.getChunkProvider();
                for (Chunk chunk : loaded) {
                    if (!serverWorld.getPlayerChunkMap().contains(chunk.x, chunk.z)
                            && !world.getPersistentChunks().containsKey(chunk.getPos())) provider.queueUnload(chunk);
                }
            }
            loaded.clear();
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if(args.length < 1)
            return Collections.emptyList();

        if(args.length == 1)
            return getListOfStringsMatchingLastWord(args, "structure");

        if(args.length == 2) {
            List<String> structures = NBTStructure.listStructures();
            return getListOfStringsMatchingLastWord(args, structures.toArray(new String[structures.size()]));
        }

        return Collections.emptyList();
    }

}
