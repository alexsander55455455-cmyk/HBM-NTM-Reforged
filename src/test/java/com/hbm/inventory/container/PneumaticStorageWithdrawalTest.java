package com.hbm.inventory.container;

import com.hbm.api.ntl.ISlotMonitorProvider;
import com.hbm.api.ntl.SlotMonitor;
import com.hbm.api.ntl.StackCache;
import com.hbm.packet.toserver.PneumoAccessActionPacket;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.tileentity.network.TileEntityPneumoStorageAccess;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PneumaticStorageWithdrawalTest {
    static {
        ForgeTestBootstrap.ensureServerSide();
        Bootstrap.register();
    }

    @Test
    void repeatedShiftClicksCannotWithdrawTheSameCachedItemsTwice() throws Exception {
        Fixture fixture = new Fixture();
        fixture.withdraw();
        fixture.withdraw();
        assertEquals(10, fixture.playerSticks());
        assertEquals(0, fixture.storage.amount);
    }

    @Test
    void staleListingOnlyGrantsItemsThatStillExistInStorage() throws Exception {
        Fixture fixture = new Fixture();
        fixture.storage.amount = 3;
        fixture.withdraw();
        assertEquals(3, fixture.playerSticks());
        assertEquals(0, fixture.storage.amount);
    }

    @Test
    void fullInventoryDoesNotConsumeStorageItems() throws Exception {
        Fixture fixture = new Fixture();
        for (int i = 0; i < fixture.player.inventory.getSizeInventory(); i++) {
            fixture.player.inventory.setInventorySlotContents(i, new ItemStack(Items.DIAMOND, 64));
        }
        fixture.withdraw();
        assertEquals(0, fixture.playerSticks());
        assertEquals(10, fixture.storage.amount);
    }

    @Test
    void partialInventorySpaceOnlyConsumesTheQuantityThatFits() throws Exception {
        Fixture fixture = new Fixture();
        for (int i = 0; i < fixture.player.inventory.getSizeInventory(); i++) {
            fixture.player.inventory.setInventorySlotContents(i, new ItemStack(Items.DIAMOND, 64));
        }
        fixture.player.inventory.setInventorySlotContents(0, new ItemStack(Items.STICK, 61));
        fixture.withdraw();
        assertEquals(64, fixture.playerSticks());
        assertEquals(7, fixture.storage.amount);
    }

    private static final class Fixture {
        final TestPlayer player;
        final Storage storage = new Storage();
        final ContainerPneumoStorageAccess container;
        final long hash = StackCache.getStackIdentity(new ItemStack(Items.STICK));

        Fixture() throws Exception {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            player = (TestPlayer) ((Unsafe) unsafeField.get(null)).allocateInstance(TestPlayer.class);
            player.world = new TestWorld();
            player.inventory = new InventoryPlayer(player);
            TileEntityPneumoStorageAccess access = new TileEntityPneumoStorageAccess() {
                @Override public void markDirty() { }
            };
            access.setWorld(player.world);
            access.setPos(BlockPos.ORIGIN);
            access.cache = new StackCache(0, 0, 0);
            storage.network.addStorage(storage);
            storage.network.addStackCache(access.cache);
            storage.updateMonitors();
            container = new ContainerPneumoStorageAccess(player.inventory, access);
            // Prime the listing as after the first sync. The following actions run before another monitor tick.
            container.applyDeltas(new byte[] {0}, new long[] {hash}, new ItemStack[] {new ItemStack(Items.STICK)}, new long[] {10});
        }

        void withdraw() { container.handleAccessAction(player, PneumoAccessActionPacket.SHIFT_CLICK, hash); }

        int playerSticks() {
            int count = 0;
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (stack.getItem() == Items.STICK) count += stack.getCount();
            }
            return count;
        }
    }

    private static final class Storage implements ISlotMonitorProvider {
        final PneumaticNetwork network = new PneumaticNetwork();
        final SlotMonitor[] monitors = {new SlotMonitor(0, this)};
        long amount = 10;
        @Override public SlotMonitor[] getMonitors() { return monitors; }
        @Override public ItemStack getSlotAt(int index) { return new ItemStack(Items.STICK); }
        @Override public long getAmountAt(int index) { return amount; }
        @Override public long useUpItem(int index, long requested) {
            long removed = Math.min(amount, requested);
            amount -= removed;
            return requested - removed;
        }
        @Override public long addItem(int index, long requested) { return requested; }
        @Override public long setupType(int index, ItemStack stack, long requested) { return requested; }
        @Override public boolean allowTypeSetting() { return false; }
        @Override public boolean isAvailableToCache(StackCache cache) { return true; }
        @Override public PneumaticNetwork getRelevantNetwork() { return network; }
    }

    private static final class TestPlayer extends EntityPlayerMP {
        private TestPlayer() { super(null, null, new GameProfile(UUID.randomUUID(), "storage-test"), null); }
        @Override public void updateHeldItem() { }
    }

    private static final class TestWorld extends World {
        TestWorld() { super(new SaveHandlerMP(), new WorldInfo(new NBTTagCompound()), new WorldProviderSurface(), new Profiler(), false); }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
    }
}
