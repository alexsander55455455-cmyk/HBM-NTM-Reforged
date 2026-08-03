package com.hbm.inventory;

import com.hbm.capability.BackpackCapability;
import com.hbm.handler.BackpackHandler;
import com.hbm.items.tool.BackpackUpgradeType;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemBlackBoxBackpack;
import com.hbm.items.weapon.ItemGunBase;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class BackpackAmmoProvider {

    private static final String CLIENT_AMMO_SUMMARY_TAG = "BackpackAmmoSummary";
    private static final String SUMMARY_STACK_TAG = "Stack";
    private static final String SUMMARY_COUNT_TAG = "Count";

    private BackpackAmmoProvider() {
    }

    public static boolean isWeaponContext(InventoryPlayer inventory) {
        if (inventory == null || inventory.player == null) return false;
        return isWeapon(inventory.player.getHeldItemMainhand()) || isWeapon(inventory.player.getHeldItemOffhand());
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemGunBase || stack.getItem() instanceof ItemGunBaseNT) return true;
        String path = stack.getItem().getRegistryName() == null ? "" : stack.getItem().getRegistryName().getPath();
        return path.startsWith("gun_") || path.equals("boltgun");
    }

    public static boolean hasItem(InventoryPlayer inventory, Item item) {
        return isWeaponContext(inventory) && countMatching(inventory.player, stack -> stack.getItem() == item, 1) > 0;
    }

    public static int countItem(InventoryPlayer inventory, Item item) {
        return isWeaponContext(inventory)
                ? countMatching(inventory.player, stack -> stack.getItem() == item, Integer.MAX_VALUE) : 0;
    }

    public static boolean consumeItem(InventoryPlayer inventory, Item item) {
        return isWeaponContext(inventory) && extractMatching(inventory.player, stack -> stack.getItem() == item, 1) == 1;
    }

    public static ItemStack findFirst(EntityPlayer player, Predicate<ItemStack> matcher) {
        NBTTagList summary = getClientAmmoSummary(player);
        if (summary != null) {
            for (int index = 0; index < summary.tagCount(); index++) {
                ItemStack stack = new ItemStack(summary.getCompoundTagAt(index).getCompoundTag(SUMMARY_STACK_TAG));
                if (!stack.isEmpty() && matcher.test(stack)) return stack;
            }
            return ItemStack.EMPTY;
        }
        IBackpackInventory inventory = getAmmoInventory(player);
        if (inventory == null) return ItemStack.EMPTY;
        ItemStack[] result = {ItemStack.EMPTY};
        inventory.forEachStoredStack((stack, count) -> {
            if (result[0].isEmpty() && matcher.test(stack)) result[0] = stack.copy();
        });
        return result[0];
    }

    public static int countMatching(EntityPlayer player, Predicate<ItemStack> matcher, int limit) {
        NBTTagList summary = getClientAmmoSummary(player);
        if (summary != null) return countSummary(summary, matcher, limit);
        IBackpackInventory inventory = getAmmoInventory(player);
        if (inventory == null || limit <= 0) return 0;
        long[] count = {0L};
        inventory.forEachStoredStack((stack, storedCount) -> {
            if (count[0] < limit && matcher.test(stack)) {
                count[0] = Math.min((long) limit, count[0] + Math.max(0L, storedCount));
            }
        });
        return (int) Math.min(Integer.MAX_VALUE, count[0]);
    }

    public static int extractMatching(EntityPlayer player, Predicate<ItemStack> matcher, int amount) {
        IBackpackInventory inventory = getAmmoInventory(player);
        if (inventory == null || amount <= 0 || player.world.isRemote) return 0;
        int remaining = amount;
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            while (remaining > 0) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !matcher.test(stack)) break;
                ItemStack extracted = inventory.extractItem(slot, remaining, false);
                if (extracted.isEmpty()) break;
                remaining -= extracted.getCount();
            }
        }
        int extracted = amount - remaining;
        if (extracted > 0) BackpackHandler.syncEquipmentState(player);
        return extracted;
    }

    private static IBackpackInventory getAmmoInventory(EntityPlayer player) {
        if (player == null) return null;
        ItemStack backpack = BackpackCapability.getData(player).getEquippedBackpack();
        if (!(backpack.getItem() instanceof ItemBackpack item)
                || !BackpackUpgradeManager.hasUpgrade(backpack, BackpackUpgradeType.AMMO_FEEDER)) {
            return null;
        }
        if (item instanceof ItemBlackBoxBackpack blackBox && !blackBox.canAccess(backpack, player)) return null;
        return item.createInventory(backpack, player.world);
    }

    /**
     * Adds only item identities and aggregate counts required by weapon HUDs.
     * World-backed storage slots and their positions remain server-only.
     */
    public static ItemStack createClientSyncedBackpack(EntityPlayer player, ItemStack backpack) {
        if (backpack.isEmpty()) return ItemStack.EMPTY;
        ItemStack synced = backpack.copy();
        stripClientAmmoSummary(synced);

        IBackpackInventory inventory = getAmmoInventory(player);
        if (inventory == null || player.world.isRemote) return synced;

        List<SummaryEntry> entries = new ArrayList<>();
        inventory.forEachStoredStack((stack, count) -> mergeSummaryEntry(entries, stack, count));
        NBTTagList serialized = new NBTTagList();
        for (SummaryEntry entry : entries) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag(SUMMARY_STACK_TAG, entry.prototype.serializeNBT());
            tag.setLong(SUMMARY_COUNT_TAG, entry.count);
            serialized.appendTag(tag);
        }
        if (!synced.hasTagCompound()) synced.setTagCompound(new NBTTagCompound());
        synced.getTagCompound().setTag(CLIENT_AMMO_SUMMARY_TAG, serialized);
        return synced;
    }

    public static void stripClientAmmoSummary(ItemStack backpack) {
        if (backpack.hasTagCompound()) backpack.getTagCompound().removeTag(CLIENT_AMMO_SUMMARY_TAG);
    }

    private static NBTTagList getClientAmmoSummary(EntityPlayer player) {
        if (player == null || player.world == null || !player.world.isRemote) return null;
        ItemStack backpack = BackpackCapability.getData(player).getEquippedBackpack();
        if (!(backpack.getItem() instanceof ItemBackpack)
                || !BackpackUpgradeManager.hasUpgrade(backpack, BackpackUpgradeType.AMMO_FEEDER)
                || !backpack.hasTagCompound()
                || !backpack.getTagCompound().hasKey(CLIENT_AMMO_SUMMARY_TAG, Constants.NBT.TAG_LIST)) {
            return null;
        }
        return backpack.getTagCompound().getTagList(CLIENT_AMMO_SUMMARY_TAG, Constants.NBT.TAG_COMPOUND);
    }

    private static int countSummary(NBTTagList summary, Predicate<ItemStack> matcher, int limit) {
        if (limit <= 0) return 0;
        long count = 0L;
        for (int index = 0; index < summary.tagCount() && count < limit; index++) {
            NBTTagCompound entry = summary.getCompoundTagAt(index);
            ItemStack stack = new ItemStack(entry.getCompoundTag(SUMMARY_STACK_TAG));
            if (!stack.isEmpty() && matcher.test(stack)) {
                count = Math.min((long) limit, count + Math.max(0L, entry.getLong(SUMMARY_COUNT_TAG)));
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    private static void mergeSummaryEntry(List<SummaryEntry> entries, ItemStack stack, long count) {
        if (stack.isEmpty() || count <= 0L) return;
        ItemStack prototype = stack.copy();
        prototype.setCount(1);
        for (SummaryEntry entry : entries) {
            if (ItemStack.areItemsEqual(entry.prototype, prototype)
                    && ItemStack.areItemStackTagsEqual(entry.prototype, prototype)) {
                entry.count = entry.count > Long.MAX_VALUE - count ? Long.MAX_VALUE : entry.count + count;
                return;
            }
        }
        entries.add(new SummaryEntry(prototype, count));
    }

    private static final class SummaryEntry {
        private final ItemStack prototype;
        private long count;

        private SummaryEntry(ItemStack prototype, long count) {
            this.prototype = prototype;
            this.count = count;
        }
    }
}
