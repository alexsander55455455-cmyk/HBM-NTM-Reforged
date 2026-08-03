package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.interfaces.AutoRegister;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.tileentity.TileEntityTickingBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simulation controller for a second-generation Chicago Pile.
 */
@AutoRegister(name = "tileentity_pile_core")
public class TileEntityPileCore extends TileEntityTickingBase {

    private static final int DATA_VERSION = 2;
    public static final int MAX_HEAT = 800;
    public static final int MAX_CHANNELS = 169;

    public PileOrientation orientation = PileOrientation.NEITHER;
    public EnumFacing assemblyFacing = EnumFacing.NORTH;

    public int height;
    public int width;
    public int depth;
    public int left;
    public int right;
    public int up;

    private BlockPos boundsMin;
    private BlockPos boundsMax;

    public final List<PileChannel> fuelChannels = new ArrayList<>();
    public final List<PileChannel> ventilationChannels = new ArrayList<>();
    public final List<PileChannel> controlChannels = new ArrayList<>();
    public PileSegment[] segments = new PileSegment[0];

    public double highestHeat;
    private boolean disassembling;
    private boolean meltingDown;
    private boolean partLinksValidated;

    public TileEntityPileCore setupSize(int up, int down, int left, int right, int depth,
                                        EnumFacing assemblyFacing, BlockPos min, BlockPos max) {
        this.height = up + 1 + down;
        this.width = left + 1 + right;
        this.depth = depth;
        this.left = left;
        this.right = right;
        this.up = up;
        this.assemblyFacing = assemblyFacing;
        this.orientation = PileOrientation.getOrientation(assemblyFacing);
        this.boundsMin = min.toImmutable();
        this.boundsMax = max.toImmutable();
        this.segments = new PileSegment[Math.max(0, width)];
        this.partLinksValidated = true;
        markDirty();
        return this;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote || disassembling) return;
        restoreMissingPartLinks();
        runSimulation();
        handleVentilation();
        handleMeltdown();
        if (!fuelChannels.isEmpty() || !ventilationChannels.isEmpty() || !controlChannels.isEmpty()) {
            markDirty();
        }
        networkPackNT(25);
    }

    @Override
    public void invalidate() {
        if (!disassembling && !meltingDown && world != null && !world.isRemote) {
            ejectAllFuel();
        }
        super.invalidate();
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(highestHeat);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        highestHeat = buf.readDouble();
    }

    public PileChannel getFuelChannel(BlockPos entry) {
        return getChannel(entry, fuelChannels);
    }

    public PileChannel getVentilationChannel(BlockPos entry) {
        return getChannel(entry, ventilationChannels);
    }

    public PileChannel getControlChannel(BlockPos entry) {
        return getChannel(entry, controlChannels);
    }

    private PileChannel getChannel(BlockPos entry, List<PileChannel> channels) {
        for (PileChannel channel : channels) {
            if (channel.entry.equals(entry)) return channel;
        }
        return null;
    }

    public int getFuelChannelNum(PileChannel channel) {
        return fuelChannels.indexOf(channel);
    }

    public int getVentilationChannelNum(PileChannel channel) {
        return ventilationChannels.indexOf(channel);
    }

    public int getControlChannelNum(PileChannel channel) {
        return controlChannels.indexOf(channel);
    }

    private List<PileChannel> getChannelList(PileChannelType type) {
        if (type == PileChannelType.FUEL) return fuelChannels;
        if (type == PileChannelType.VENTILATION) return ventilationChannels;
        return controlChannels;
    }

    /**
     * Validates the complete line before changing any metadata, so a failed drill
     * cannot leave a half-created channel.
     */
    public boolean drillChannel(BlockPos start, EnumFacing direction, EntityPlayer player) {
        if (world == null || world.isRemote) return false;
        int startMeta = getPileMeta(start);
        PileChannelType type = PileChannelType.getChannelType(direction, orientation);
        int size = type == PileChannelType.CONTROL ? height :
                type == PileChannelType.FUEL ? depth : width;
        if (size <= 0) return false;

        List<PileChannel> list = getChannelList(type);
        if (isChannelEntrance(startMeta)) {
            for (int index = 0; index < list.size(); index++) {
                PileChannel existing = list.get(index);
                if (existing.entry.equals(start) && existing.direction == direction) {
                    if (existing.type == PileChannelType.FUEL) existing.ejectAll();
                    list.remove(index);
                    for (int i = 0; i < size; i++) {
                        setPileMeta(start.offset(direction, i), BlockPile.META_DUMMY);
                    }
                    world.playSound(null, start, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 1F, 0.75F);
                    recalculateSegments();
                    markDirty();
                    return true;
                }
            }
        }

        boolean invalid = false;
        for (int i = 0; i < size; i++) {
            BlockPos cursor = start.offset(direction, i);
            if (world.getBlockState(cursor).getBlock() != ModBlocks.pile_block) {
                sendError(player, cursor, "Foreign block in reactor");
                invalid = true;
                continue;
            }
            int meta = getPileMeta(cursor);
            if (meta == BlockPile.META_EDGE) {
                sendError(player, cursor, "Cannot drill along edge");
                invalid = true;
            } else if (meta == BlockPile.META_CORE) {
                sendError(player, cursor, "Cannot intersect core");
                invalid = true;
            } else if (meta == BlockPile.META_CHANNEL) {
                sendError(player, cursor, "Cannot intersect channel");
                invalid = true;
            } else if (meta != BlockPile.META_DUMMY) {
                sendError(player, cursor, "Cannot intersect channel IO");
                invalid = true;
            }
        }
        if (invalid) return false;

        for (int i = 0; i < size; i++) {
            int meta;
            if (i == 0) {
                meta = type == PileChannelType.FUEL ? BlockPile.META_FUEL_IN :
                        type == PileChannelType.VENTILATION ? BlockPile.META_AIR_IN :
                                BlockPile.META_CONTROL;
            } else if (i == size - 1) {
                meta = type == PileChannelType.FUEL ? BlockPile.META_FUEL_OUT :
                        type == PileChannelType.VENTILATION ? BlockPile.META_AIR_OUT :
                                BlockPile.META_CONTROL;
            } else {
                meta = BlockPile.META_CHANNEL;
            }
            setPileMeta(start.offset(direction, i), meta);
        }

        list.add(new PileChannel(start, direction, size, type));
        world.playSound(null, start, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 1F, 1.25F);
        recalculateSegments();
        markDirty();
        return true;
    }

    private boolean isChannelEntrance(int meta) {
        return meta == BlockPile.META_FUEL_IN || meta == BlockPile.META_AIR_IN || meta == BlockPile.META_CONTROL;
    }

    private int getPileMeta(BlockPos target) {
        if (world.getBlockState(target).getBlock() != ModBlocks.pile_block) return -1;
        return world.getBlockState(target).getValue(BlockMeta.META);
    }

    private void setPileMeta(BlockPos target, int meta) {
        IBlockState newState = ModBlocks.pile_block.getDefaultState().withProperty(BlockMeta.META, meta);
        if (!world.setBlockState(target, newState, 3)) return;

        // Same-block metadata changes do not reliably invalidate the cached
        // connected-texture model. Force every changed channel block to sync
        // and rebuild immediately instead of waiting for another interaction.
        if (world instanceof WorldServer) {
            ((WorldServer) world).getPlayerChunkMap().markBlockForUpdate(target);
        }
        world.markBlockRangeForRenderUpdate(target, target);
    }

    private void runSimulation() {
        for (PileChannel channel : fuelChannels) {
            if (channel.length <= 0) continue;
            double producedNeutrons = 0D;
            for (int i = 0; i < channel.rods.length; i++) {
                ItemStack stack = channel.rods[i];
                if (!stack.isEmpty() && stack.getItem() instanceof ItemPileRodMK2) {
                    double neutrons = ItemPileRodMK2.getReactivity(stack, channel.incomingNeutrons / channel.length);
                    producedNeutrons += neutrons;
                    channel.heat += neutrons * ItemPileRodMK2.getHeatPerNeutron(stack);
                    channel.rods[i] = ItemPileRodMK2.react(stack, neutrons);
                }
            }
            channel.outgoingNeutrons = producedNeutrons;
            channel.incomingNeutrons = 0D;
        }

        for (PileSegment segment : segments) {
            if (segment == null || segment.type != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;
            for (PileChannel channel : segment.channels) channel.incomingNeutrons += outgoing;
        }

        for (int i = 1; i < segments.length - 1; i++) {
            PileSegment segment = segments[i];
            if (segment == null || segment.type != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;

            double multiplier = 1D;
            for (int j = i - 1; j >= 1; j--) {
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                multiplier *= neighbor.getNeutronMultiplier();
                if (neighbor.type == PileChannelType.FUEL) {
                    for (PileChannel channel : neighbor.channels) {
                        channel.incomingNeutrons += outgoing * multiplier;
                    }
                }
            }

            multiplier = 1D;
            for (int j = i + 1; j < segments.length - 1; j++) {
                PileSegment neighbor = segments[j];
                if (neighbor == null) continue;
                multiplier *= neighbor.getNeutronMultiplier();
                if (neighbor.type == PileChannelType.FUEL) {
                    for (PileChannel channel : neighbor.channels) {
                        channel.incomingNeutrons += outgoing * multiplier;
                    }
                }
            }
        }
    }

    private void handleVentilation() {
        for (PileChannel ventilation : ventilationChannels) {
            if (ventilation.air <= 0) continue;
            double fillFraction = (double) ventilation.air / PileChannel.MAX_AIR;
            for (PileChannel fuel : fuelChannels) {
                if (Math.abs(fuel.entry.getY() - ventilation.entry.getY()) == 1) {
                    fuel.heat *= 1D - fillFraction * 0.05D;
                }
            }
            ventilation.air = Math.max(0, ventilation.air - (int) Math.ceil(fillFraction * 5D));

            if (world.getTotalWorldTime() % 3L == 0L) {
                double x = ventilation.entry.getX() + 0.5D
                        + ventilation.direction.getXOffset() * (width - 0.375D);
                double y = ventilation.entry.getY() + 0.5D;
                double z = ventilation.entry.getZ() + 0.5D
                        + ventilation.direction.getZOffset() * (width - 0.375D);
                NBTTagCompound data = new NBTTagCompound();
                data.setString("type", "tower");
                data.setFloat("lift", 1F);
                data.setFloat("base", (0.125F + world.rand.nextFloat() * 0.125F) * (float) fillFraction);
                data.setFloat("max", (float) fillFraction);
                data.setFloat("strafe", 0.0025F);
                data.setBoolean("noWind", true);
                data.setInteger("life", 20 + world.rand.nextInt(30));
                data.setInteger("color", 0xa0a0a0);
                PacketThreading.createAllAroundThreadedPacket(
                        new AuxParticlePacketNT(data, x, y, z),
                        new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 150));
            }
        }

        for (PileChannel fuel : fuelChannels) {
            fuel.heat *= 0.999D;
            if (fuel.heat < 20D) fuel.heat = 20D;
        }
    }

    private void handleMeltdown() {
        highestHeat = 0D;
        for (PileChannel channel : fuelChannels) {
            highestHeat = Math.max(highestHeat, channel.heat);
        }
        if (highestHeat <= MAX_HEAT || fuelChannels.isEmpty()) return;

        double centerX = 0D;
        double centerZ = 0D;
        for (PileChannel channel : fuelChannels) {
            centerX += channel.entry.getX() + 0.5D + channel.direction.getXOffset() * (channel.length - 1) / 2D;
            centerZ += channel.entry.getZ() + 0.5D + channel.direction.getZOffset() * (channel.length - 1) / 2D;
        }
        centerX /= fuelChannels.size();
        centerZ /= fuelChannels.size();

        meltingDown = true;
        try {
            disassemble(null, false);
            world.newExplosion(null, centerX, pos.getY() + up, centerZ, 15F, true, true);
            for (int i = 0; i < 15; i++) {
                double motionY = world.rand.nextDouble() * 0.5D + 1D;
                EntityBulletBaseMK4 fragment = new EntityBulletBaseMK4(world, null,
                        XFactoryFlamer.pile_debris, 100F, 0.35F,
                        centerX, pos.getY() + up + 1D, centerZ, 0D, motionY, 0D);
                world.spawnEntity(fragment);
            }
        } finally {
            meltingDown = false;
        }
    }

    public void disassemble(BlockPos brokenPart, boolean ejectFuel) {
        if (world == null || world.isRemote || disassembling) return;
        disassembling = true;
        try {
            if (ejectFuel) ejectAllFuel();

            BlockPos min = boundsMin;
            BlockPos max = boundsMax;
            boolean hasExactBounds = min != null && max != null;
            if (!hasExactBounds) {
                min = pos.add(-BlockPile.MAX_H_SIZE, -BlockPile.MAX_V_SIZE, -BlockPile.MAX_H_SIZE);
                max = pos.add(BlockPile.MAX_H_SIZE, BlockPile.MAX_V_SIZE, BlockPile.MAX_H_SIZE);
            }

            for (BlockPos.MutableBlockPos cursor : BlockPos.getAllInBoxMutable(min, max)) {
                BlockPos target = cursor.toImmutable();
                if (brokenPart != null && target.equals(brokenPart)) {
                    BlockPile.restoreSinglePart(world, target);
                    continue;
                }
                if (world.getBlockState(target).getBlock() != ModBlocks.pile_block) continue;
                TileEntity tile = world.getTileEntity(target);
                boolean belongs = hasExactBounds || target.equals(pos);
                if (!hasExactBounds && tile instanceof TileEntityPileBaseMK2) {
                    BlockPos linkedCore = ((TileEntityPileBaseMK2) tile).getCorePos();
                    belongs = pos.equals(linkedCore);
                }
                if (belongs) {
                    BlockPile.restoreSinglePart(world, target);
                }
            }
            fuelChannels.clear();
            ventilationChannels.clear();
            controlChannels.clear();
            segments = new PileSegment[0];
        } finally {
            disassembling = false;
        }
    }

    private void restoreMissingPartLinks() {
        if (partLinksValidated || boundsMin == null || boundsMax == null ||
                !world.isAreaLoaded(boundsMin, boundsMax)) return;
        for (BlockPos.MutableBlockPos cursor : BlockPos.getAllInBoxMutable(boundsMin, boundsMax)) {
            BlockPos target = cursor.toImmutable();
            if (target.equals(pos) || world.getBlockState(target).getBlock() != ModBlocks.pile_block) continue;
            TileEntity tile = world.getTileEntity(target);
            if (tile instanceof TileEntityPileBaseMK2 &&
                    !pos.equals(((TileEntityPileBaseMK2) tile).getCorePos())) {
                ((TileEntityPileBaseMK2) tile).setCore(pos);
            }
        }
        partLinksValidated = true;
    }

    private void ejectAllFuel() {
        for (PileChannel channel : fuelChannels) channel.ejectAll();
    }

    private void recalculateSegments() {
        segments = new PileSegment[Math.max(0, width)];
        for (PileChannel channel : fuelChannels) addChannelToSegment(channel);
        for (PileChannel channel : controlChannels) addChannelToSegment(channel);
    }

    private void addChannelToSegment(PileChannel channel) {
        int index = getChannelVerticalIndex(channel);
        if (index < 0 || index >= segments.length) return;
        if (segments[index] == null) {
            segments[index] = new PileSegment(channel.type);
        }
        if (segments[index].type == channel.type) {
            segments[index].channels.add(channel);
        }
    }

    private int getChannelVerticalIndex(PileChannel channel) {
        EnumFacing segmentFacing = channel.direction.getAxis().isHorizontal() ? channel.direction : assemblyFacing;
        if (!segmentFacing.getAxis().isHorizontal()) segmentFacing = orientation.defaultFacing();
        EnumFacing channelRight = segmentFacing.rotateY();
        int deltaX = (channel.entry.getX() - pos.getX()) * channelRight.getXOffset();
        int deltaZ = (channel.entry.getZ() - pos.getZ()) * channelRight.getZOffset();
        return (deltaX == 0 ? deltaZ : deltaX) + left;
    }

    private void sendError(EntityPlayer player, BlockPos at, String message) {
        if (player == null) return;
        player.sendMessage(new TextComponentString("[Chicago Pile " + at.getX() + ", " + at.getY() + ", " +
                at.getZ() + "] " + message).setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        height = MathHelper.clamp(nbt.getInteger("height"), 0, BlockPile.MAX_V_SIZE);
        width = MathHelper.clamp(nbt.getInteger("width"), 0, BlockPile.MAX_H_SIZE);
        depth = MathHelper.clamp(nbt.getInteger("depth"), 0, BlockPile.MAX_H_SIZE);
        left = MathHelper.clamp(nbt.getInteger("left"), 0, BlockPile.MAX_H_SIZE - 1);
        right = MathHelper.clamp(nbt.getInteger("right"), 0, BlockPile.MAX_H_SIZE - 1);
        up = MathHelper.clamp(nbt.getInteger("up"), 0, BlockPile.MAX_V_SIZE - 1);
        orientation = PileOrientation.byOrdinal(nbt.getInteger("orientation"));
        assemblyFacing = EnumFacing.byIndex(nbt.hasKey("assemblyFacing") ?
                nbt.getInteger("assemblyFacing") : orientation.defaultFacing().getIndex());
        if (nbt.hasKey("bounds", 10)) {
            NBTTagCompound bounds = nbt.getCompoundTag("bounds");
            boundsMin = new BlockPos(bounds.getInteger("minX"), bounds.getInteger("minY"), bounds.getInteger("minZ"));
            boundsMax = new BlockPos(bounds.getInteger("maxX"), bounds.getInteger("maxY"), bounds.getInteger("maxZ"));
        }
        partLinksValidated = false;

        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();
        if (nbt.getInteger("pileDataVersion") >= DATA_VERSION) {
            readChannelList(nbt.getTagList("fuelChannels", 10), PileChannelType.FUEL, fuelChannels);
            readChannelList(nbt.getTagList("ventilationChannels", 10), PileChannelType.VENTILATION, ventilationChannels);
            readChannelList(nbt.getTagList("controlChannels", 10), PileChannelType.CONTROL, controlChannels);
        } else {
            readLegacyChannels(nbt, "f", readLegacyCount(nbt, "fc"), fuelChannels);
            readLegacyChannels(nbt, "v", readLegacyCount(nbt, "vc"), ventilationChannels);
            readLegacyChannels(nbt, "c", readLegacyCount(nbt, "cc"), controlChannels);
        }
        recalculateSegments();
    }

    private static int readLegacyCount(NBTTagCompound nbt, String key) {
        if (nbt.hasKey(key, 1)) return nbt.getByte(key) & 0xFF;
        return nbt.getInteger(key);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("pileDataVersion", DATA_VERSION);
        nbt.setInteger("height", height);
        nbt.setInteger("width", width);
        nbt.setInteger("depth", depth);
        nbt.setInteger("left", left);
        nbt.setInteger("right", right);
        nbt.setInteger("up", up);
        nbt.setInteger("orientation", orientation.ordinal());
        nbt.setInteger("assemblyFacing", assemblyFacing.getIndex());
        if (boundsMin != null && boundsMax != null) {
            NBTTagCompound bounds = new NBTTagCompound();
            bounds.setInteger("minX", boundsMin.getX());
            bounds.setInteger("minY", boundsMin.getY());
            bounds.setInteger("minZ", boundsMin.getZ());
            bounds.setInteger("maxX", boundsMax.getX());
            bounds.setInteger("maxY", boundsMax.getY());
            bounds.setInteger("maxZ", boundsMax.getZ());
            nbt.setTag("bounds", bounds);
        }
        nbt.setTag("fuelChannels", writeChannelList(fuelChannels));
        nbt.setTag("ventilationChannels", writeChannelList(ventilationChannels));
        nbt.setTag("controlChannels", writeChannelList(controlChannels));
        return nbt;
    }

    private NBTTagList writeChannelList(List<PileChannel> channels) {
        NBTTagList list = new NBTTagList();
        for (PileChannel channel : channels) list.appendTag(channel.writeToNBT());
        return list;
    }

    private void readChannelList(NBTTagList tags, PileChannelType type, List<PileChannel> target) {
        for (int i = 0; i < Math.min(tags.tagCount(), MAX_CHANNELS); i++) {
            PileChannel channel = readChannelFromNBT(tags.getCompoundTagAt(i), type);
            if (channel != null) target.add(channel);
        }
    }

    private void readLegacyChannels(NBTTagCompound nbt, String prefix, int count, List<PileChannel> target) {
        for (int i = 0; i < Math.min(Math.max(0, count), MAX_CHANNELS); i++) {
            PileChannel channel = readLegacyChannel(nbt, prefix + i);
            if (channel != null) target.add(channel);
        }
    }

    private PileChannel readChannelFromNBT(NBTTagCompound nbt, PileChannelType type) {
        EnumFacing direction = EnumFacing.byIndex(nbt.getInteger("direction"));
        BlockPos entry = new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
        int expectedLength = type == PileChannelType.CONTROL ? height :
                type == PileChannelType.FUEL ? depth : width;
        int storedLength = nbt.hasKey("length") ? nbt.getInteger("length") : expectedLength;
        int length = MathHelper.clamp(storedLength, 0,
                type == PileChannelType.CONTROL ? BlockPile.MAX_V_SIZE : BlockPile.MAX_H_SIZE);
        PileChannel channel = new PileChannel(entry, direction, length, type);
        channel.heat = nbt.hasKey("heat") ? nbt.getDouble("heat") : 20D;
        channel.incomingNeutrons = nbt.getDouble("incomingNeutrons");
        channel.air = MathHelper.clamp(nbt.getInteger("air"), 0, PileChannel.MAX_AIR);
        channel.control = MathHelper.clamp(nbt.getDouble("control"), 0D, 1D);
        channel.readItems(nbt.getTagList("items", 10));
        return channel;
    }

    private PileChannel readLegacyChannel(NBTTagCompound owner, String name) {
        BlockPos entry = new BlockPos(owner.getInteger(name + "_x"), owner.getInteger(name + "_y"),
                owner.getInteger(name + "_z"));
        EnumFacing direction = EnumFacing.byIndex(owner.getByte(name + "_d"));
        PileChannelType type = PileChannelType.getChannelType(direction, orientation);
        int length = type == PileChannelType.CONTROL ? height :
                type == PileChannelType.FUEL ? depth : width;
        PileChannel channel = new PileChannel(entry, direction, length, type);
        channel.heat = owner.hasKey(name + "heat") ? owner.getDouble(name + "heat") : 20D;
        channel.incomingNeutrons = owner.getDouble(name + "neutrons");
        channel.air = MathHelper.clamp(owner.getInteger(name + "air"), 0, PileChannel.MAX_AIR);
        channel.control = owner.hasKey(name + "control") ?
                MathHelper.clamp(owner.getDouble(name + "control"), 0D, 1D) : 1D;
        channel.readItems(owner.getTagList(name + "items", 10));
        return channel;
    }

    @Override
    public String getInventoryName() {
        return "container.pile_core";
    }

    public enum PileOrientation {
        NORTH_SOUTH,
        EAST_WEST,
        NEITHER;

        public static PileOrientation getOrientation(EnumFacing direction) {
            if (direction == EnumFacing.NORTH || direction == EnumFacing.SOUTH) return NORTH_SOUTH;
            if (direction == EnumFacing.EAST || direction == EnumFacing.WEST) return EAST_WEST;
            return NEITHER;
        }

        static PileOrientation byOrdinal(int ordinal) {
            PileOrientation[] values = values();
            return values[Math.max(0, Math.min(ordinal, values.length - 1))];
        }

        EnumFacing defaultFacing() {
            return this == EAST_WEST ? EnumFacing.EAST : EnumFacing.NORTH;
        }
    }

    public enum PileChannelType {
        FUEL,
        VENTILATION,
        CONTROL;

        static PileChannelType getChannelType(EnumFacing channelDirection, PileOrientation pileOrientation) {
            if (channelDirection.getAxis() == EnumFacing.Axis.Y) return CONTROL;
            return PileOrientation.getOrientation(channelDirection) == pileOrientation ? FUEL : VENTILATION;
        }
    }

    public class PileChannel {
        public static final int MAX_AIR = 1_000;

        public final BlockPos entry;
        public final EnumFacing direction;
        public final int length;
        public final PileChannelType type;
        public final ItemStack[] rods;

        public double heat = 20D;
        public double outgoingNeutrons;
        public double incomingNeutrons;
        public int air;
        public double control = 1D;

        PileChannel(BlockPos entry, EnumFacing direction, int length, PileChannelType type) {
            this.entry = entry.toImmutable();
            this.direction = direction;
            this.length = Math.max(0, length);
            this.type = type;
            this.rods = new ItemStack[this.length];
            Arrays.fill(this.rods, ItemStack.EMPTY);
        }

        public void loadItem(ItemStack input) {
            if (input == null || input.isEmpty()) return;
            ItemStack moving = input.copy();
            moving.setCount(1);
            if (rods.length == 0) {
                dropItem(moving, -1);
                return;
            }
            for (int i = 0; i < rods.length; i++) {
                if (rods[i].isEmpty()) {
                    rods[i] = moving;
                    markDirty();
                    return;
                }
                ItemStack previous = rods[i];
                rods[i] = moving;
                moving = previous;
            }
            dropItem(moving, length);
            markDirty();
        }

        public void ejectAll() {
            for (int i = 0; i < rods.length; i++) {
                dropItem(rods[i], length);
                rods[i] = ItemStack.EMPTY;
            }
            markDirty();
        }

        private void dropItem(ItemStack stack, int channelDepth) {
            if (stack == null || stack.isEmpty() || world == null || world.isRemote) return;
            BlockPos dropPos = entry.offset(direction, channelDepth);
            world.spawnEntity(new EntityItem(world, dropPos.getX() + 0.5D, dropPos.getY() + 0.5D,
                    dropPos.getZ() + 0.5D, stack.copy()));
        }

        NBTTagCompound writeToNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger("x", entry.getX());
            nbt.setInteger("y", entry.getY());
            nbt.setInteger("z", entry.getZ());
            nbt.setInteger("direction", direction.getIndex());
            nbt.setInteger("length", length);
            nbt.setDouble("heat", heat);
            nbt.setDouble("incomingNeutrons", incomingNeutrons);
            nbt.setInteger("air", air);
            nbt.setDouble("control", control);
            NBTTagList items = new NBTTagList();
            for (int slot = 0; slot < rods.length; slot++) {
                if (rods[slot].isEmpty()) continue;
                NBTTagCompound item = new NBTTagCompound();
                item.setInteger("slot", slot);
                rods[slot].writeToNBT(item);
                items.appendTag(item);
            }
            nbt.setTag("items", items);
            return nbt;
        }

        private void readItems(NBTTagList items) {
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound itemTag = items.getCompoundTagAt(i);
                int slot = itemTag.getInteger("slot");
                if (slot >= 0 && slot < rods.length) rods[slot] = new ItemStack(itemTag);
            }
        }
    }

    public class PileSegment {
        public final List<PileChannel> channels = new ArrayList<>();
        public final PileChannelType type;

        PileSegment(PileChannelType type) {
            this.type = type;
        }

        double getNeutronMultiplier() {
            if (type != PileChannelType.CONTROL) return 1D;
            int size = depth - 1;
            if (size < 3) return 0D;
            double total = 0D;
            for (PileChannel channel : channels) total += channel.control;
            return MathHelper.clamp(total / size, 0D, 0.5D);
        }
    }
}
