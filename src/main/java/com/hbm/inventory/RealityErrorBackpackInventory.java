package com.hbm.inventory;

import com.hbm.items.ModItems;
import com.hbm.config.BackpackConfig;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/** Server-authoritative multi-slot casino storage used by the reality-error backpack. */
public abstract class RealityErrorBackpackInventory implements IBackpackInventory {

    private static final String SLOTS_TAG = "RealityErrorSlots";
    private static final String SLOT_TAG = "Slot";
    private static final String SLOT_STATE_TAG = "State";
    private static final String SLOT_REWARD_TAG = "Reward";
    private static final String LEGACY_STATE_TAG = "RealityErrorState";
    private static final String LEGACY_REWARD_TAG = "RealityErrorReward";
    private static final byte EMPTY = 0;
    private static final byte LOCKED = 1;
    private static final byte ROLLED = 2;
    private static final int GLITCH_COUNT = 1;
    private static final int MAX_REWARD_COUNT = 64;

    protected ItemStack backpack;
    protected final int capacity;

    private RealityErrorBackpackInventory(ItemStack backpack, int capacity) {
        this.backpack = backpack;
        this.capacity = Math.max(1, Math.min(BackpackConfig.MAX_STORAGE_SLOTS, capacity));
    }

    public static IBackpackInventory create(ItemRealityErrorBackpack backpackItem, ItemStack backpack,
                                            @Nullable World world) {
        int capacity = backpackItem.getStorageSlots(backpack);
        if (world != null && !world.isRemote) {
            return new Server(backpack, capacity, ModItems.reality_glitch, null);
        }
        return new Client(backpack, capacity);
    }

    static IBackpackInventory createForTest(ItemStack backpack, Item glitchItem, List<Item> rewards) {
        return new Server(backpack, 1, glitchItem, rewards);
    }

    static IBackpackInventory createForTest(ItemStack backpack, int capacity, Item glitchItem, List<Item> rewards) {
        return new Server(backpack, capacity, glitchItem, rewards);
    }

    public static boolean hasStoredValue(ItemStack backpack) {
        return countStoredValues(backpack) > 0;
    }

    public static int countStoredValues(ItemStack backpack) {
        if (backpack.isEmpty() || !backpack.hasTagCompound()) return 0;
        NBTTagCompound tag = backpack.getTagCompound();
        NBTTagList slots = tag.getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND);
        if (slots.tagCount() > 0) {
            int capacity = backpack.getItem() instanceof ItemRealityErrorBackpack item
                    ? item.getStorageSlots(backpack) : Integer.MAX_VALUE;
            int filled = 0;
            for (int index = 0; index < slots.tagCount(); index++) {
                NBTTagCompound entry = slots.getCompoundTagAt(index);
                int slot = entry.getInteger(SLOT_TAG);
                if (slot >= 0 && slot < capacity
                        && !new ItemStack(entry.getCompoundTag(SLOT_REWARD_TAG)).isEmpty()) filled++;
            }
            return filled;
        }
        byte legacyState = tag.getByte(LEGACY_STATE_TAG);
        if (legacyState == LOCKED || legacyState == ROLLED) return 1;
        return tag.getCompoundTag(ItemBackpack.INVENTORY_TAG)
                .getTagList("Items", Constants.NBT.TAG_COMPOUND).tagCount() > 0 ? 1 : 0;
    }

    public static List<ItemStack> takeOverflowItems(ItemStack backpack, int capacity) {
        List<ItemStack> overflow = new ArrayList<>();
        if (backpack.isEmpty() || !backpack.hasTagCompound()) return overflow;
        if (requiresLegacyMigration(backpack)) migrateLegacy(backpack, ModItems.reality_glitch);
        NBTTagCompound tag = backpack.getTagCompound();
        NBTTagList slots = tag.getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND);
        NBTTagList kept = new NBTTagList();
        for (int index = 0; index < slots.tagCount(); index++) {
            NBTTagCompound entry = slots.getCompoundTagAt(index);
            if (entry.getInteger(SLOT_TAG) < capacity) {
                kept.appendTag(entry.copy());
                continue;
            }
            ItemStack stored = new ItemStack(entry.getCompoundTag(SLOT_REWARD_TAG));
            if (!stored.isEmpty()) overflow.add(stored);
        }
        tag.setTag(SLOTS_TAG, kept);
        return overflow;
    }

    private static boolean requiresLegacyMigration(ItemStack backpack) {
        if (!backpack.hasTagCompound()) return false;
        NBTTagCompound tag = backpack.getTagCompound();
        if (tag.getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND).tagCount() > 0) return false;
        byte state = tag.getByte(LEGACY_STATE_TAG);
        if (state == LOCKED || state == ROLLED) return true;
        return tag.getCompoundTag(ItemBackpack.INVENTORY_TAG)
                .getTagList("Items", Constants.NBT.TAG_COMPOUND).tagCount() > 0;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getSlots() {
        return capacity;
    }

    @Override
    public boolean supportsAutoPickup() {
        return BackpackUpgradeManager.supportsAutoPickup(backpack);
    }

    @Override
    public boolean isAutoPickupEnabled() {
        return BackpackUpgradeManager.isAutoPickupEnabled(backpack);
    }

    @Override
    public void setAutoPickupEnabled(boolean enabled) {
        if (supportsAutoPickup()) {
            BackpackUpgradeManager.setAutoPickupEnabled(backpack, enabled);
        }
    }

    @Override
    public boolean supportsManualSorting() {
        return true;
    }

    @Override
    public boolean usesVirtualLongCounts() {
        return true;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot >= 0 && slot < capacity ? MAX_REWARD_COUNT : 0;
    }

    private static final class Server extends RealityErrorBackpackInventory {
        private final Item glitchItem;
        private final List<Item> fixedRewards;
        private final Random random = new Random();

        private Server(ItemStack backpack, int capacity, Item glitchItem, @Nullable List<Item> fixedRewards) {
            super(backpack, capacity);
            this.glitchItem = glitchItem;
            this.fixedRewards = fixedRewards == null ? null : new ArrayList<>(fixedRewards);
            migrateLegacy(backpack, glitchItem);
            validateState();
        }

        @Override
        public int getFilledSlotCount() {
            int filled = 0;
            for (int slot = 0; slot < capacity; slot++) {
                if (getState(slot) != EMPTY) filled++;
            }
            return filled;
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            for (int slot = 0; slot < capacity; slot++) {
                if (getState(slot) == EMPTY) return insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (!validSlot(slot)) return ItemStack.EMPTY;
            byte state = getState(slot);
            if (state != LOCKED && state != ROLLED) return ItemStack.EMPTY;
            ItemStack reward = getReward(slot);
            if (!reward.isEmpty()) reward.setCount(1);
            return reward;
        }

        @Override
        public long getTrueSlotCount(int slot) {
            if (!validSlot(slot)) return 0L;
            byte state = getState(slot);
            ItemStack reward = state == LOCKED || state == ROLLED ? getReward(slot) : ItemStack.EMPTY;
            return reward.isEmpty() ? 0L : reward.getCount();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!validSlot(slot) || stack.isEmpty() || getState(slot) != EMPTY
                    || ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            if (!simulate) writeSlot(slot, LOCKED, new ItemStack(glitchItem, GLITCH_COUNT));
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!validSlot(slot) || amount <= 0 || getState(slot) != ROLLED) return ItemStack.EMPTY;
            ItemStack reward = getReward(slot);
            if (reward.isEmpty()) {
                if (!simulate) clear(slot);
                return ItemStack.EMPTY;
            }
            int moved = Math.min(amount, Math.min(reward.getCount(), Math.max(1, reward.getMaxStackSize())));
            ItemStack extracted = reward.copy();
            extracted.setCount(moved);
            if (!simulate) {
                reward.shrink(moved);
                if (reward.isEmpty()) clear(slot);
                else writeSlot(slot, ROLLED, reward);
            }
            return extracted;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return validSlot(slot) && getState(slot) == EMPTY && !stack.isEmpty()
                    && !ItemBackpack.isForbiddenBackpackContent(stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (!validSlot(slot)) return;
            if (stack.isEmpty()) {
                if (getState(slot) == ROLLED) clear(slot);
            } else if (getState(slot) == EMPTY) {
                insertItem(slot, stack, false);
            }
        }

        @Override
        public void sortContents() {
            for (int slot = 0; slot < capacity; slot++) {
                if (getState(slot) != LOCKED) continue;
                ItemStack reward = pickReward();
                if (reward.isEmpty()) continue;
                int maxRewardCount = Math.min(MAX_REWARD_COUNT, Math.max(1, reward.getMaxStackSize()));
                reward.setCount(1 + random.nextInt(maxRewardCount));
                writeSlot(slot, ROLLED, reward);
            }
        }

        private ItemStack pickReward() {
            List<Item> rewards = fixedRewards != null ? fixedRewards : getRegisteredRewards();
            return rewards.isEmpty() ? ItemStack.EMPTY : new ItemStack(rewards.get(random.nextInt(rewards.size())));
        }

        private List<Item> getRegisteredRewards() {
            Collection<Item> registered = ForgeRegistries.ITEMS.getValuesCollection();
            List<Item> rewards = new ArrayList<>(registered.size());
            for (Item item : registered) {
                if (item != null && item != Items.AIR && item != glitchItem && item.getRegistryName() != null) {
                    rewards.add(item);
                }
            }
            return rewards;
        }

        private boolean validSlot(int slot) {
            return slot >= 0 && slot < capacity;
        }

        private void validateState() {
            for (int slot = 0; slot < capacity; slot++) {
                byte state = getState(slot);
                if (state == EMPTY) continue;
                ItemStack stored = getReward(slot);
                if (state == LOCKED) {
                    if (stored.getItem() != glitchItem || stored.getCount() != GLITCH_COUNT) {
                        writeSlot(slot, LOCKED, new ItemStack(glitchItem, GLITCH_COUNT));
                    }
                } else if (state != ROLLED || stored.isEmpty()) {
                    clear(slot);
                }
            }
        }

        private byte getState(int slot) {
            NBTTagCompound entry = getSlotEntry(backpack, slot);
            return entry == null ? EMPTY : entry.getByte(SLOT_STATE_TAG);
        }

        private ItemStack getReward(int slot) {
            NBTTagCompound entry = getSlotEntry(backpack, slot);
            return entry == null ? ItemStack.EMPTY : new ItemStack(entry.getCompoundTag(SLOT_REWARD_TAG));
        }

        private void writeSlot(int slot, byte state, ItemStack reward) {
            writeSlotEntry(backpack, slot, state, reward);
        }

        private void clear(int slot) {
            writeSlotEntry(backpack, slot, EMPTY, ItemStack.EMPTY);
        }
    }

    /** Rendering mirror only; gameplay remains server-authoritative. */
    private static final class Client extends RealityErrorBackpackInventory {
        private final ItemStack[] prototypes;
        private final long[] counts;

        private Client(ItemStack backpack, int capacity) {
            super(backpack, capacity);
            prototypes = new ItemStack[this.capacity];
            Arrays.fill(prototypes, ItemStack.EMPTY);
            counts = new long[this.capacity];
            if (!backpack.hasTagCompound()) return;
            migrateLegacy(backpack, ModItems.reality_glitch);
            for (int slot = 0; slot < this.capacity; slot++) {
                NBTTagCompound entry = getSlotEntry(backpack, slot);
                if (entry == null) continue;
                ItemStack stored = new ItemStack(entry.getCompoundTag(SLOT_REWARD_TAG));
                if (!stored.isEmpty()) {
                    counts[slot] = stored.getCount();
                    prototypes[slot] = stored.copy();
                    prototypes[slot].setCount(1);
                }
            }
        }

        @Override
        public int getFilledSlotCount() {
            int filled = 0;
            for (int slot = 0; slot < capacity; slot++) if (counts[slot] > 0L) filled++;
            return filled;
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (!validSlot(slot) || prototypes[slot].isEmpty() || counts[slot] <= 0L) return ItemStack.EMPTY;
            ItemStack displayed = prototypes[slot].copy();
            displayed.setCount(1);
            return displayed;
        }

        @Override
        public long getTrueSlotCount(int slot) {
            return validSlot(slot) ? Math.max(0L, counts[slot]) : 0L;
        }

        @Override
        public void setClientTrueSlotCount(int slot, long count) {
            if (validSlot(slot)) counts[slot] = Math.max(0L, count);
        }

        @Override
        public void resetClientStorageMirror(int capacity) {
            Arrays.fill(prototypes, ItemStack.EMPTY);
            Arrays.fill(counts, 0L);
        }

        @Override
        public void applyClientSyncedVirtualSlot(int slot, ItemStack prototype, long count) {
            if (!validSlot(slot)) return;
            prototypes[slot] = prototype.isEmpty() ? ItemStack.EMPTY : prototype.copy();
            if (!prototypes[slot].isEmpty()) prototypes[slot].setCount(1);
            counts[slot] = Math.max(0L, count);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (!validSlot(slot)) return;
            if (stack.isEmpty()) {
                prototypes[slot] = ItemStack.EMPTY;
                counts[slot] = 0L;
            } else {
                prototypes[slot] = stack.copy();
                prototypes[slot].setCount(1);
                if (counts[slot] <= 0L) counts[slot] = 1L;
            }
        }

        @Override
        public void sortContents() {
        }

        private boolean validSlot(int slot) {
            return slot >= 0 && slot < capacity;
        }
    }

    private static void migrateLegacy(ItemStack backpack, Item glitchItem) {
        if (!backpack.hasTagCompound()) backpack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = backpack.getTagCompound();
        if (tag.getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND).tagCount() == 0) {
            byte state = tag.getByte(LEGACY_STATE_TAG);
            ItemStack stored = new ItemStack(tag.getCompoundTag(LEGACY_REWARD_TAG));
            if (state == LOCKED) stored = new ItemStack(glitchItem, GLITCH_COUNT);
            if ((state == LOCKED || state == ROLLED) && !stored.isEmpty()) {
                writeSlotEntry(backpack, 0, state, stored);
            } else {
                NBTTagList legacy = tag.getCompoundTag(ItemBackpack.INVENTORY_TAG)
                        .getTagList("Items", Constants.NBT.TAG_COMPOUND);
                if (legacy.tagCount() > 0) {
                    writeSlotEntry(backpack, 0, LOCKED, new ItemStack(glitchItem, GLITCH_COUNT));
                }
            }
        }
        tag.removeTag(LEGACY_STATE_TAG);
        tag.removeTag(LEGACY_REWARD_TAG);
        tag.removeTag(ItemBackpack.INVENTORY_TAG);
    }

    @Nullable
    private static NBTTagCompound getSlotEntry(ItemStack backpack, int slot) {
        if (!backpack.hasTagCompound()) return null;
        NBTTagList slots = backpack.getTagCompound().getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < slots.tagCount(); index++) {
            NBTTagCompound entry = slots.getCompoundTagAt(index);
            if (entry.getInteger(SLOT_TAG) == slot) return entry;
        }
        return null;
    }

    private static void writeSlotEntry(ItemStack backpack, int slot, byte state, ItemStack reward) {
        if (!backpack.hasTagCompound()) backpack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = backpack.getTagCompound();
        NBTTagList current = tag.getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND);
        NBTTagList updated = new NBTTagList();
        for (int index = 0; index < current.tagCount(); index++) {
            NBTTagCompound entry = current.getCompoundTagAt(index);
            if (entry.getInteger(SLOT_TAG) != slot) updated.appendTag(entry.copy());
        }
        if (state != EMPTY && !reward.isEmpty()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger(SLOT_TAG, slot);
            entry.setByte(SLOT_STATE_TAG, state);
            entry.setTag(SLOT_REWARD_TAG, reward.serializeNBT());
            updated.appendTag(entry);
        }
        tag.setTag(SLOTS_TAG, updated);
    }
}
