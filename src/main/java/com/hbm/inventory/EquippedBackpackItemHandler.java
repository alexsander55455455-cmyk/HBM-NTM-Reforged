package com.hbm.inventory;

import com.hbm.capability.BackpackCapability;
import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class EquippedBackpackItemHandler implements IBackpackInventory {
    private final EntityPlayer player;
    private ItemStack cachedStack = ItemStack.EMPTY;
    private IBackpackInventory cachedInventory;

    public EquippedBackpackItemHandler(EntityPlayer player) {
        this.player = player;
    }

    private IBackpackInventory getInventory() {
        ItemStack backpack = BackpackCapability.getData(player).getEquippedBackpack();
        int expectedCapacity = backpack.getItem() instanceof ItemBackpack item ? item.getStorageSlots(backpack) : 0;
        if (backpack != cachedStack || cachedInventory != null && !cachedInventory.isInfiniteStorage()
                && cachedInventory.getCapacity() != expectedCapacity) {
            if (cachedInventory == null || !cachedInventory.tryRebindClientBackpack(backpack)) {
                cachedInventory = backpack.getItem() instanceof ItemBackpack item
                        ? item.createInventory(backpack, player.world) : null;
            }
            cachedStack = backpack;
        }
        return cachedInventory;
    }

    private boolean isAvailable(int slot, IBackpackInventory inventory) {
        return inventory != null && slot >= 0 && (slot < inventory.getSlots() || inventory.isInfiniteStorage());
    }

    public int getCapacity() {
        IBackpackInventory inventory = getInventory();
        return inventory == null ? 0 : inventory.getCapacity();
    }

    @Override
    public int getFilledSlotCount() {
        IBackpackInventory inventory = getInventory();
        return inventory == null ? 0 : inventory.getFilledSlotCount();
    }

    @Override
    public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
        if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
        IBackpackInventory inventory = getInventory();
        return inventory == null ? stack : inventory.insertItemAnywhere(stack, simulate);
    }

    @Override
    public boolean supportsAutoPickup() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.supportsAutoPickup();
    }

    @Override
    public boolean isAutoPickupEnabled() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.isAutoPickupEnabled();
    }

    @Override
    public void setAutoPickupEnabled(boolean enabled) {
        IBackpackInventory inventory = getInventory();
        if (inventory != null) inventory.setAutoPickupEnabled(enabled);
    }

    @Override
    public boolean supportsAutoSorting() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.supportsAutoSorting();
    }

    @Override
    public boolean isAutoSortEnabled() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.isAutoSortEnabled();
    }

    @Override
    public void setAutoSortEnabled(boolean enabled) {
        IBackpackInventory inventory = getInventory();
        if (inventory != null) inventory.setAutoSortEnabled(enabled);
    }

    @Override
    public boolean supportsManualSorting() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.supportsManualSorting();
    }

    @Override
    public void sortContents() {
        IBackpackInventory inventory = getInventory();
        if (inventory != null) inventory.sortContents();
    }

    @Override
    public void sortContents(int fromInclusive, int toExclusive) {
        IBackpackInventory inventory = getInventory();
        if (inventory != null) inventory.sortContents(fromInclusive, toExclusive);
    }

    @Override
    public boolean isInfiniteStorage() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.isInfiniteStorage();
    }

    @Override
    public long getTrueSlotCount(int slot) {
        IBackpackInventory inventory = getInventory();
        return isAvailable(slot, inventory) ? inventory.getTrueSlotCount(slot) : 0L;
    }

    @Override
    public void setClientTrueSlotCount(int slot, long count) {
        IBackpackInventory inventory = getInventory();
        if (isAvailable(slot, inventory)) inventory.setClientTrueSlotCount(slot, count);
    }

    @Override
    public void resetClientStorageMirror(int capacity) {
        IBackpackInventory inventory = getInventory();
        if (inventory != null) inventory.resetClientStorageMirror(capacity);
    }

    @Override
    public void applyClientSyncedVirtualSlot(int slot, ItemStack prototype, long count) {
        IBackpackInventory inventory = getInventory();
        if (isAvailable(slot, inventory)) {
            inventory.applyClientSyncedVirtualSlot(slot, prototype, count);
        }
    }

    @Override
    public boolean usesVirtualLongCounts() {
        IBackpackInventory inventory = getInventory();
        return inventory != null && inventory.usesVirtualLongCounts();
    }

    @Override
    public int getSlots() {
        return getCapacity();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IBackpackInventory inventory = getInventory();
        return isAvailable(slot, inventory) ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
        IBackpackInventory inventory = getInventory();
        return isAvailable(slot, inventory) ? inventory.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        IBackpackInventory inventory = getInventory();
        return isAvailable(slot, inventory) ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        IBackpackInventory inventory = getInventory();
        return isAvailable(slot, inventory) ? inventory.getSlotLimit(slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        IBackpackInventory inventory = getInventory();
        return !ItemBackpack.isForbiddenBackpackContent(stack)
                && isAvailable(slot, inventory) && inventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        IBackpackInventory inventory = getInventory();
        if ((stack.isEmpty() || !ItemBackpack.isForbiddenBackpackContent(stack)) && isAvailable(slot, inventory)) {
            inventory.setStackInSlot(slot, stack);
        }
    }
}
