package com.hbm.uninos.networkproviders;

import com.hbm.lib.ForgeDirection;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.network.TileEntityPneumoTube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PneumaticNetworkRoutingTest {

    static {
        ForgeTestBootstrap.ensureServerSide();
        Bootstrap.register();
    }

    @Test
    void skipsFullFirstReceiverAndUsesTheNextLoadedReceiver() {
        TestWorld world = new TestWorld();
        HandlerTile source = tile(world, new BlockPos(0, 0, 0), 64);
        TileEntityPneumoTube tube = tube(world, new BlockPos(1, 0, 0));
        HandlerTile full = tile(world, new BlockPos(2, 0, 0), 64);
        HandlerTile fallback = tile(world, new BlockPos(3, 0, 0), 64);
        source.setInventorySlotContents(0, new ItemStack(Items.STICK, 10));
        full.setInventorySlotContents(0, new ItemStack(Items.STICK, 64));

        PneumaticNetwork network = networkWithReceivers(tube, full, fallback);

        assertTrue(network.send(source, tube, ForgeDirection.EAST, PneumaticNetwork.SEND_FIRST,
                PneumaticNetwork.RECEIVE_ROBIN, 10, 0));
        assertEquals(0, source.getStackInSlot(0).getCount());
        assertEquals(64, full.getStackInSlot(0).getCount());
        assertEquals(10, fallback.getStackInSlot(0).getCount());
    }

    @Test
    void partialAcceptanceDoesNotDuplicateOrLoseItems() {
        TestWorld world = new TestWorld();
        HandlerTile source = tile(world, new BlockPos(0, 0, 0), 64);
        TileEntityPneumoTube tube = tube(world, new BlockPos(1, 0, 0));
        HandlerTile limited = tile(world, new BlockPos(2, 0, 0), 3);
        source.setInventorySlotContents(0, new ItemStack(Items.STICK, 10));

        PneumaticNetwork network = networkWithReceivers(tube, limited);

        assertTrue(network.send(source, tube, ForgeDirection.EAST, PneumaticNetwork.SEND_FIRST,
                PneumaticNetwork.RECEIVE_ROBIN, 10, 0));
        assertEquals(7, source.getStackInSlot(0).getCount());
        assertEquals(3, limited.getStackInSlot(0).getCount());
        assertEquals(10, source.getStackInSlot(0).getCount() + limited.getStackInSlot(0).getCount());
    }

    @Test
    void roundRobinOffsetSelectsTheRequestedReceiverFirst() {
        TestWorld world = new TestWorld();
        HandlerTile source = tile(world, new BlockPos(0, 0, 0), 64);
        TileEntityPneumoTube tube = tube(world, new BlockPos(1, 0, 0));
        HandlerTile first = tile(world, new BlockPos(2, 0, 0), 64);
        HandlerTile second = tile(world, new BlockPos(3, 0, 0), 64);
        source.setInventorySlotContents(0, new ItemStack(Items.STICK, 4));

        PneumaticNetwork network = networkWithReceivers(tube, first, second);

        assertTrue(network.send(source, tube, ForgeDirection.EAST, PneumaticNetwork.SEND_FIRST,
                PneumaticNetwork.RECEIVE_ROBIN, 10, 1));
        assertEquals(0, first.getStackInSlot(0).getCount());
        assertEquals(4, second.getStackInSlot(0).getCount());
    }

    private static PneumaticNetwork networkWithReceivers(TileEntityPneumoTube tube, HandlerTile... receivers) {
        PneumaticNetwork network = new PneumaticNetwork();
        for (HandlerTile receiver : receivers) {
            network.addReceiver(new PneumaticNetwork.ReceiverTarget(receiver.getPos(), ForgeDirection.WEST, tube));
        }
        return network;
    }

    private static HandlerTile tile(TestWorld world, BlockPos pos, int slotLimit) {
        HandlerTile tile = new HandlerTile(slotLimit);
        world.addTile(pos, tile);
        return tile;
    }

    private static TileEntityPneumoTube tube(TestWorld world, BlockPos pos) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            TileEntityPneumoTube tube = (TileEntityPneumoTube) unsafe.allocateInstance(TileEntityPneumoTube.class);
            tube.inventory = new ItemStackHandler(15);
            tube.pattern = new ModulePatternMatcher(15);
            world.addTile(pos, tube);
            return tube;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create constructor-free tube test fixture", e);
        }
    }

    private static final class HandlerTile extends TileEntity implements IInventory {
        private ItemStack stack = ItemStack.EMPTY;
        private final int slotLimit;

        private HandlerTile(int slotLimit) {
            this.slotLimit = slotLimit;
        }

        @Override public int getSizeInventory() { return 1; }
        @Override public boolean isEmpty() { return stack.isEmpty(); }
        @Override public ItemStack getStackInSlot(int index) { return index == 0 ? stack : ItemStack.EMPTY; }
        @Override public ItemStack decrStackSize(int index, int count) {
            if (index != 0 || stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
            ItemStack removed = stack.splitStack(count);
            if (stack.getCount() <= 0) stack = ItemStack.EMPTY;
            return removed;
        }
        @Override public ItemStack removeStackFromSlot(int index) {
            if (index != 0) return ItemStack.EMPTY;
            ItemStack removed = stack;
            stack = ItemStack.EMPTY;
            return removed;
        }
        @Override public void setInventorySlotContents(int index, ItemStack value) {
            if (index != 0) return;
            stack = value.isEmpty() ? ItemStack.EMPTY : value;
            if (stack.getCount() > slotLimit) stack.setCount(slotLimit);
        }
        @Override public int getInventoryStackLimit() { return slotLimit; }
        @Override public void markDirty() { }
        @Override public boolean isUsableByPlayer(EntityPlayer player) { return true; }
        @Override public void openInventory(EntityPlayer player) { }
        @Override public void closeInventory(EntityPlayer player) { }
        @Override public boolean isItemValidForSlot(int index, ItemStack value) { return index == 0; }
        @Override public int getField(int id) { return 0; }
        @Override public void setField(int id, int value) { }
        @Override public int getFieldCount() { return 0; }
        @Override public void clear() { stack = ItemStack.EMPTY; }
        @Override public String getName() { return "routing-test"; }
        @Override public boolean hasCustomName() { return false; }
        @Override public ITextComponent getDisplayName() { return new TextComponentString(getName()); }
    }

    private static final class TestWorld extends World {
        private final Map<BlockPos, TileEntity> tiles = new HashMap<>();

        private TestWorld() {
            super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false);
        }

        private void addTile(BlockPos pos, TileEntity tile) {
            tile.setWorld(this);
            tile.setPos(pos);
            tiles.put(pos.toImmutable(), tile);
        }

        @Override
        @Nullable
        public TileEntity getTileEntity(BlockPos pos) {
            return tiles.get(pos);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return true;
        }

        @Override
        public Entity getEntityByID(int id) {
            return null;
        }
    }
}
