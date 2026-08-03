package com.hbm.inventory.container;

import com.hbm.api.ntl.StackCache;
import com.hbm.api.ntl.StackCache.CacheSlot;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PneumoAccessSyncPacket;
import com.hbm.packet.toserver.PneumoAccessActionPacket;
import com.hbm.tileentity.network.TileEntityPneumoStorageAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContainerPneumoStorageAccess extends Container {

    public static final int GRID_SIZE = 6 * 8;
    private static final int DELTAS_PER_PACKET = PneumoAccessSyncPacket.MAX_DELTAS;

    private final TileEntityPneumoStorageAccess access;
    private final InventoryPneumoStorageAccess displayInventory = new InventoryPneumoStorageAccess();
    private final EntityPlayer player;
    private final LinkedHashMap<Long, CacheSlotDummy> cachedEntries = new LinkedHashMap<>();
    private final LinkedList<Delta> syncQueue = new LinkedList<>();
    private String searchString = "";

    public int listingStart;
    public int listingSize;
    public boolean detailedSearch;
    private Comparator<CacheSlotDummy> sorter = SORT_BY_STACK_SIZE;

    public ContainerPneumoStorageAccess(InventoryPlayer inventory, TileEntityPneumoStorageAccess access) {
        this.access = access;
        this.player = inventory.player;
        int horizontalOffset = 34;
        for (int row = 0; row < 6; row++) for (int column = 0; column < 8; column++) {
            addSlotToContainer(new SlotPneumo(displayInventory, column + row * 8,
                    8 + column * 18 + horizontalOffset, 17 + row * 18));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column + row * 9 + 9,
                    8 + column * 18 + horizontalOffset, 169 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column, 8 + column * 18 + horizontalOffset, 227));
        }
    }

    public int getStackCount() { return listingSize; }

    public void setSorter(Comparator<CacheSlotDummy> sorter) {
        listingStart = 0;
        this.sorter = sorter == null ? SORT_BY_STACK_SIZE : sorter;
        rebuildClientIndex();
    }

    public void setSearchString(String search) {
        listingStart = 0;
        searchString = search == null ? "" : search.toLowerCase(Locale.US);
        rebuildClientIndex();
    }

    public void setListingStart(int row) {
        listingStart = Math.max(0, row);
        rebuildClientIndex();
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < GRID_SIZE) {
            if (!player.world.isRemote) return ItemStack.EMPTY;
            SlotPneumo slot = (SlotPneumo) getSlot(slotId);
            long hash = slot.getHasStack() ? StackCache.getStackIdentity(slot.getStack()) : StackCache.getNullIdentity();
            int action = clickType == ClickType.QUICK_MOVE ? PneumoAccessActionPacket.SHIFT_CLICK
                    : dragType == 1 ? PneumoAccessActionPacket.RIGHT_CLICK : PneumoAccessActionPacket.LEFT_CLICK;
            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                PacketDispatcher.wrapper.sendToServer(new PneumoAccessActionPacket(windowId, action, hash));
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (player.world.isRemote || index < GRID_SIZE || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || access.cache == null || access.cache.hasExpired) return ItemStack.EMPTY;
        ItemStack original = slot.getStack().copy();
        long remainder = access.cache.addItemsAndReturnQuantity(original, original.getCount());
        int accepted = original.getCount() - (int) Math.min(original.getCount(), Math.max(0L, remainder));
        if (accepted <= 0) return ItemStack.EMPTY;
        slot.decrStackSize(accepted);
        slot.onSlotChanged();
        access.markDirty();
        return original;
    }

    public void handleAccessAction(EntityPlayerMP player, int action, long hash) {
        if (player != this.player || !canInteractWith(player) || access.cache == null || access.cache.hasExpired) return;
        ItemStack held = player.inventory.getItemStack();
        long heldHash = StackCache.getStackIdentity(held);

        if ((action == PneumoAccessActionPacket.LEFT_CLICK || action == PneumoAccessActionPacket.RIGHT_CLICK)
                && !held.isEmpty() && hash != heldHash) {
            int requested = action == PneumoAccessActionPacket.LEFT_CLICK ? held.getCount() : 1;
            long remainder = access.cache.addItemsAndReturnQuantity(held, requested);
            int accepted = requested - (int) Math.min(requested, Math.max(0L, remainder));
            if (accepted > 0) held.shrink(accepted);
            if (held.isEmpty()) player.inventory.setItemStack(ItemStack.EMPTY);
            finishAction(player);
            return;
        }

        CacheSlot cacheSlot = access.cache.cacheSlots.get(hash);
        if (cacheSlot == null || cacheSlot.displayStack == null || cacheSlot.stacksize <= 0) {
            finishAction(player);
            return;
        }

        if (action == PneumoAccessActionPacket.LEFT_CLICK || action == PneumoAccessActionPacket.RIGHT_CLICK) {
            if (!held.isEmpty() && heldHash != hash) return;
            ItemStack result = cacheSlot.displayStack.copy();
            int alreadyHeld = held.isEmpty() ? 0 : held.getCount();
            int capacity = Math.max(0, result.getMaxStackSize() - alreadyHeld);
            if (action == PneumoAccessActionPacket.RIGHT_CLICK) capacity = Math.min(1, capacity);
            long grabbed = access.cache.consumeItemsAndReturnQuantity(result, Math.min((long) capacity, cacheSlot.stacksize));
            if (grabbed > 0) {
                result.setCount(alreadyHeld + (int) grabbed);
                player.inventory.setItemStack(result);
            }
            finishAction(player);
            return;
        }

        if (action == PneumoAccessActionPacket.SHIFT_CLICK) {
            ItemStack result = cacheSlot.displayStack.copy();
            result.setCount((int) Math.min(Math.min((long) result.getMaxStackSize(), cacheSlot.stacksize), Integer.MAX_VALUE));
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(new InvWrapper(player.inventory), result, false);
            int inserted = result.getCount() - (remainder.isEmpty() ? 0 : remainder.getCount());
            if (inserted > 0) access.cache.consumeItemsAndReturnQuantity(result, inserted);
            finishAction(player);
        }
    }

    private void finishAction(EntityPlayerMP player) {
        access.markDirty();
        checkAndSyncCache();
        super.detectAndSendChanges();
        player.updateHeldItem();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!player.world.isRemote) checkAndSyncCache();
        else rebuildClientIndex();
    }

    private void checkAndSyncCache() {
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        if (access.cache != null && !access.cache.hasExpired) {
            for (Map.Entry<Long, CacheSlot> entry : access.cache.cacheSlots.entrySet()) {
                long hash = entry.getKey();
                CacheSlot proper = entry.getValue();
                if (hash == StackCache.getNullIdentity() || proper.displayStack == null || proper.stacksize <= 0) continue;
                seen.add(hash);
                CacheSlotDummy previous = cachedEntries.get(hash);
                if (previous == null) {
                    previous = new CacheSlotDummy(proper.displayStack, proper.stacksize);
                    cachedEntries.put(hash, previous);
                    syncQueue.add(new Delta(DeltaType.NEW_TYPE, hash, previous.displayStack, previous.stacksize));
                } else if (previous.stacksize != proper.stacksize) {
                    previous.stacksize = proper.stacksize;
                    syncQueue.add(new Delta(DeltaType.COUNT_CHANGE, hash, ItemStack.EMPTY, proper.stacksize));
                }
            }
        }

        Iterator<Map.Entry<Long, CacheSlotDummy>> iterator = cachedEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, CacheSlotDummy> entry = iterator.next();
            if (seen.contains(entry.getKey())) continue;
            syncQueue.add(new Delta(DeltaType.REMOVE_TYPE, entry.getKey(), ItemStack.EMPTY, 0));
            iterator.remove();
        }
        if (player instanceof EntityPlayerMP serverPlayer) flushDeltas(serverPlayer);
    }

    private void flushDeltas(EntityPlayerMP serverPlayer) {
        while (!syncQueue.isEmpty()) {
            int count = Math.min(DELTAS_PER_PACKET, syncQueue.size());
            byte[] types = new byte[count];
            long[] hashes = new long[count];
            ItemStack[] stacks = new ItemStack[count];
            long[] amounts = new long[count];
            for (int i = 0; i < count; i++) {
                Delta delta = syncQueue.removeFirst();
                types[i] = (byte) delta.type.ordinal();
                hashes[i] = delta.hash;
                stacks[i] = delta.stack;
                amounts[i] = delta.amount;
            }
            PacketDispatcher.sendTo(new PneumoAccessSyncPacket(windowId, types, hashes, stacks, amounts), serverPlayer);
        }
    }

    public void applyDeltas(byte[] types, long[] hashes, ItemStack[] stacks, long[] amounts) {
        if (types == null || hashes == null || stacks == null || amounts == null || types.length != hashes.length
                || types.length != stacks.length || types.length != amounts.length) return;
        for (int i = 0; i < types.length; i++) {
            DeltaType type = types[i] >= 0 && types[i] < DeltaType.values().length ? DeltaType.values()[types[i]] : null;
            if (type == DeltaType.NEW_TYPE && stacks[i] != null && !stacks[i].isEmpty()) {
                cachedEntries.put(hashes[i], new CacheSlotDummy(stacks[i], amounts[i]));
            } else if (type == DeltaType.COUNT_CHANGE) {
                CacheSlotDummy existing = cachedEntries.get(hashes[i]);
                if (existing != null) existing.stacksize = Math.max(0L, amounts[i]);
            } else if (type == DeltaType.REMOVE_TYPE) {
                cachedEntries.remove(hashes[i]);
            }
        }
        rebuildClientIndex();
    }

    public void rebuildClientIndex() {
        List<CacheSlotDummy> entries = new ArrayList<>(cachedEntries.values());
        entries.removeIf(entry -> entry.stacksize <= 0);
        if (!searchString.isEmpty()) {
            entries.removeIf(entry -> {
                String display = entry.displayStack.getDisplayName().toLowerCase(Locale.US);
                if (display.contains(searchString)) return false;
                return !detailedSearch || !entry.displayStack.getTranslationKey().toLowerCase(Locale.US).contains(searchString);
            });
        }
        listingSize = entries.size();
        Collections.sort(entries, sorter);
        int maximumStart = Math.max(0, (int) Math.ceil(listingSize / 8D) - 6);
        listingStart = Math.min(listingStart, maximumStart);
        int offset = listingStart * 8;
        for (int i = 0; i < GRID_SIZE; i++) {
            SlotPneumo slot = (SlotPneumo) inventorySlots.get(i);
            int index = offset + i;
            if (index < entries.size()) {
                CacheSlotDummy entry = entries.get(index);
                displayInventory.setInventorySlotContents(i, entry.displayStack.copy());
                slot.amount = entry.stacksize;
            } else {
                displayInventory.setInventorySlotContents(i, ItemStack.EMPTY);
                slot.amount = 0;
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !access.isInvalid() && player.getDistanceSq(access.getPos()) <= 15D * 15D;
    }

    public enum DeltaType { NEW_TYPE, COUNT_CHANGE, REMOVE_TYPE }

    private static final class Delta {
        final DeltaType type; final long hash; final ItemStack stack; final long amount;
        Delta(DeltaType type, long hash, ItemStack stack, long amount) {
            this.type = type; this.hash = hash; this.stack = stack == null ? ItemStack.EMPTY : stack; this.amount = amount;
        }
    }

    public static final class CacheSlotDummy {
        public final ItemStack displayStack;
        public long stacksize;
        public final int itemId;
        public final int meta;
        @Nullable public final NBTTagCompound nbt;

        CacheSlotDummy(ItemStack stack, long stacksize) {
            displayStack = stack.copy();
            displayStack.setCount(1);
            this.stacksize = Math.max(0L, stacksize);
            itemId = Item.getIdFromItem(displayStack.getItem());
            meta = displayStack.getMetadata();
            nbt = displayStack.hasTagCompound() ? displayStack.getTagCompound().copy() : null;
        }
    }

    public static final Comparator<CacheSlotDummy> SORT_BY_STACK_SIZE = Comparator
            .comparingLong((CacheSlotDummy value) -> value.stacksize).reversed()
            .thenComparingInt(value -> value.itemId).thenComparingInt(value -> value.meta);
    public static final Comparator<CacheSlotDummy> SORT_BY_ID = Comparator
            .comparingInt((CacheSlotDummy value) -> value.itemId).thenComparingInt(value -> value.meta)
            .thenComparing(Comparator.comparingLong((CacheSlotDummy value) -> value.stacksize).reversed());
    public static final Comparator<CacheSlotDummy> SORT_BY_INTERNAL = Comparator
            .comparing((CacheSlotDummy value) -> value.displayStack.getTranslationKey(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(SORT_BY_ID);
    public static final Comparator<CacheSlotDummy> SORT_BY_LOCALIZED = Comparator
            .comparing((CacheSlotDummy value) -> value.displayStack.getDisplayName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(SORT_BY_ID);

    public static class SlotPneumo extends Slot {
        public long amount;
        SlotPneumo(IInventory inventory, int index, int x, int y) { super(inventory, index, x, y); }
        @Override public boolean isItemValid(ItemStack stack) { return false; }
        @Override public boolean canTakeStack(EntityPlayer player) { return true; }
    }

    private static class InventoryPneumoStorageAccess implements IInventory {
        private final NonNullList<ItemStack> stacks = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        @Override public int getSizeInventory() { return stacks.size(); }
        @Override public boolean isEmpty() { for (ItemStack stack : stacks) if (!stack.isEmpty()) return false; return true; }
        @Override public ItemStack getStackInSlot(int index) { return index >= 0 && index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY; }
        @Override public ItemStack decrStackSize(int index, int count) { return ItemStack.EMPTY; }
        @Override public ItemStack removeStackFromSlot(int index) { ItemStack old = getStackInSlot(index); setInventorySlotContents(index, ItemStack.EMPTY); return old; }
        @Override public void setInventorySlotContents(int index, ItemStack stack) { if (index >= 0 && index < stacks.size()) stacks.set(index, stack == null ? ItemStack.EMPTY : stack); }
        @Override public int getInventoryStackLimit() { return 1; }
        @Override public void markDirty() { }
        @Override public boolean isUsableByPlayer(EntityPlayer player) { return true; }
        @Override public void openInventory(EntityPlayer player) { }
        @Override public void closeInventory(EntityPlayer player) { }
        @Override public boolean isItemValidForSlot(int index, ItemStack stack) { return false; }
        @Override public int getField(int id) { return 0; }
        @Override public void setField(int id, int value) { }
        @Override public int getFieldCount() { return 0; }
        @Override public void clear() { for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY); }
        @Override public String getName() { return "container.pneumoStorageAccess"; }
        @Override public boolean hasCustomName() { return false; }
        @Override public ITextComponent getDisplayName() { return new TextComponentString(getName()); }
    }
}
