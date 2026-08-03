package com.hbm.items.tool;

import com.hbm.config.BackpackConfig;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.inventory.PocketHoleBackpackInventory;
import com.hbm.saveddata.PocketHoleBackpackSavedData;
import com.hbm.saveddata.PocketHoleBackpackSavedData.PocketHoleStorage;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A small infinite backpack with nine base slots and capacity-module expansion.
 * Each slot binds to the first exact item type placed into it and then stores an
 * unbounded (long) count of that type. Contents live in world saved data keyed
 * by a per-item UUID; a compact per-slot summary is cached in item NBT so
 * tooltips and the GUI overlay can show the true counts without shipping the
 * whole storage over the network.
 */
public class ItemPocketHoleBackpack extends ItemBackpack implements IAutomatedBackpack {

    public static final int VISIBLE_SLOTS = PocketHoleStorage.BASE_SLOTS;
    public static final String STORAGE_ID_TAG = "PocketHoleStorageId";
    private static final String SLOTS_TAG = "PocketHoleSlots";
    private static final String TOTAL_TAG = "PocketHoleTotal";
    private static final String OCCUPIED_TAG = "PocketHoleOccupied";
    private static final String RADIATION_TAG = "PocketHoleStoredRadiation";

    public ItemPocketHoleBackpack(String name) {
        super(name, VISIBLE_SLOTS, 1D, false);
        setCreativeTab(null);
    }

    @Override
    public @NotNull String getItemStackDisplayName(@NotNull ItemStack stack) {
        return TextFormatting.AQUA + super.getItemStackDisplayName(stack) + TextFormatting.RESET;
    }

    @Override
    public IBackpackInventory createInventory(ItemStack backpack, @Nullable World world) {
        if (!BackpackConfig.usesPocketStorage(backpack)) return super.createInventory(backpack, world);
        return PocketHoleBackpackInventory.create(this, backpack, world);
    }

    @Override
    public int getInitialViewCapacity() {
        return VISIBLE_SLOTS;
    }

    @Override
    public int getInitialViewCapacity(ItemStack backpack) {
        return getStorageSlots(backpack);
    }

    @Override
    public int getStorageSlots(ItemStack backpack) {
        return BackpackConfig.getStorageSlots(backpack, VISIBLE_SLOTS);
    }

    @Override
    public int getDroppedLavaSurvivalTicks() {
        return -1;
    }

    /** Kept for the handler's open-capacity hook and client GUI announcement. */
    public int getCachedVirtualSlotCount(ItemStack stack) {
        return getStorageSlots(stack);
    }

    @Override
    public int getFilledSlotCount(ItemStack stack) {
        if (!BackpackConfig.usesPocketStorage(stack)) return super.getFilledSlotCount(stack);
        return getCachedOccupiedSlots(stack);
    }

    @Override
    public boolean isEmptyForUpgrade(ItemStack stack, @Nullable World world) {
        if (!BackpackConfig.usesPocketStorage(stack)) return super.isEmptyForUpgrade(stack, world);
        UUID storageId = getStorageId(stack);
        if (world != null && !world.isRemote && storageId != null) {
            return PocketHoleBackpackSavedData.get(world).getStorage(storageId).isEmpty();
        }
        return getCachedTotalCount(stack) == 0L;
    }

    /**
     * Strips any inherited storage identity from a table-crafted result. Note
     * this hook only covers vanilla crafting/furnace/anvil output slots; the
     * assembly-machine recipe that actually produces this item does not reach
     * it, so this is defence in depth rather than the guarantee against shared
     * storage.
     */
    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            tag.removeTag(STORAGE_ID_TAG);
            tag.removeTag(SLOTS_TAG);
            tag.removeTag(TOTAL_TAG);
            tag.removeTag(OCCUPIED_TAG);
            tag.removeTag(RADIATION_TAG);
        }
    }

    // ---- cached summary (written server-side, read by tooltip + GUI overlay) ----

    public void setCachedSummary(ItemStack stack, PocketHoleStorage storage) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();

        NBTTagList slotList = new NBTTagList();
        int capacity = getStorageSlots(stack);
        for (int slot = 0; slot < capacity; slot++) {
            long count = storage.getSlotCount(slot);
            ItemStack prototype = storage.getSlotPrototype(slot);
            if (count <= 0L || prototype.isEmpty()) continue;
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setInteger("slot", slot);
            slotTag.setLong("count", count);
            slotTag.setTag("item", prototype.serializeNBT());
            slotList.appendTag(slotTag);
        }
        tag.setTag(SLOTS_TAG, slotList);
        tag.setLong(TOTAL_TAG, Math.max(0L, storage.getTotalCount()));
        tag.setInteger(OCCUPIED_TAG, storage.getOccupiedSlots());
        tag.setDouble(RADIATION_TAG, Math.max(0D, storage.getStoredRadiation()));
    }

    private NBTTagList getSlotList(ItemStack stack) {
        if (!stack.hasTagCompound()) return new NBTTagList();
        return stack.getTagCompound().getTagList(SLOTS_TAG, Constants.NBT.TAG_COMPOUND);
    }

    public long getCachedSlotCount(ItemStack stack, int slot) {
        NBTTagList slotList = getSlotList(stack);
        for (int index = 0; index < slotList.tagCount(); index++) {
            NBTTagCompound slotTag = slotList.getCompoundTagAt(index);
            if (slotTag.getInteger("slot") == slot) return Math.max(0L, slotTag.getLong("count"));
        }
        return 0L;
    }

    public ItemStack getCachedSlotPrototype(ItemStack stack, int slot) {
        NBTTagList slotList = getSlotList(stack);
        for (int index = 0; index < slotList.tagCount(); index++) {
            NBTTagCompound slotTag = slotList.getCompoundTagAt(index);
            if (slotTag.getInteger("slot") == slot) {
                ItemStack prototype = new ItemStack(slotTag.getCompoundTag("item"));
                if (!prototype.isEmpty()) prototype.setCount(1);
                return prototype;
            }
        }
        return ItemStack.EMPTY;
    }

    public int getCachedOccupiedSlots(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return Math.max(0, Math.min(getStorageSlots(stack), stack.getTagCompound().getInteger(OCCUPIED_TAG)));
    }

    public long getCachedTotalCount(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0L;
        return Math.max(0L, stack.getTagCompound().getLong(TOTAL_TAG));
    }

    // ---- storage identity -------------------------------------------------

    public UUID getOrCreateStorageId(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasUniqueId(STORAGE_ID_TAG)) {
            tag.setUniqueId(STORAGE_ID_TAG, UUID.randomUUID());
        }
        return tag.getUniqueId(STORAGE_ID_TAG);
    }

    @Nullable
    public UUID getStorageId(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasUniqueId(STORAGE_ID_TAG)
                ? stack.getTagCompound().getUniqueId(STORAGE_ID_TAG)
                : null;
    }

    public PocketHoleStorage getServerStorage(World world, ItemStack stack) {
        if (world.isRemote) {
            throw new IllegalStateException("Pocket-hole backpack storage is server-side only");
        }
        return PocketHoleBackpackSavedData.get(world).getStorage(getOrCreateStorageId(stack));
    }

    @Override
    public boolean canAutoPickup(ItemStack stack) {
        return BackpackUpgradeManager.isAutoPickupEnabled(stack);
    }

    @Override
    public boolean consumeAutoPickupEnergy(ItemStack stack) {
        return BackpackUpgradeManager.isAutoPickupEnabled(stack);
    }

    @Override
    public long getAutoPickupEnergyCost() {
        return 0L;
    }

    @Override
    protected double getContainedRadiation(ItemStack stack) {
        if (!BackpackConfig.usesPocketStorage(stack)) return super.getContainedRadiation(stack);
        if (!stack.hasTagCompound()) return 0D;
        return Math.max(0D, stack.getTagCompound().getDouble(RADIATION_TAG));
    }

    // ---- tooltip ----------------------------------------------------------

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (!BackpackConfig.usesPocketStorage(stack)) {
            super.addInformation(stack, world, tooltip, flag);
            return;
        }
        tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("container.hbm_backpack.capacity",
                TextFormatting.GREEN + String.valueOf(getStorageSlots(stack))));
        tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("container.hbm_backpack.filled",
                TextFormatting.GREEN + String.valueOf(getCachedOccupiedSlots(stack)),
                TextFormatting.DARK_GREEN + String.valueOf(getStorageSlots(stack))));
        tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.backpack.shielding", 100));
        addBuiltInMagnetEnergyInformation(stack, tooltip);
        tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.backpack.leakage_radiation",
                getLeakedRadiationText(stack)));
        tooltip.add((BackpackUpgradeManager.isAutoPickupEnabled(stack) ? TextFormatting.GREEN : TextFormatting.RED)
                + I18nUtil.resolveKey(BackpackUpgradeManager.isAutoPickupEnabled(stack)
                ? "container.hbm_backpack.magnet_on"
                : "container.hbm_backpack.magnet_off"));

        NBTTagList slotList = getSlotList(stack);
        if (slotList.tagCount() == 0) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + I18nUtil.resolveKey("desc.backpack.pocket_hole.unbound", ""));
        } else {
            for (int index = 0; index < slotList.tagCount(); index++) {
                NBTTagCompound slotTag = slotList.getCompoundTagAt(index);
                ItemStack prototype = new ItemStack(slotTag.getCompoundTag("item"));
                if (prototype.isEmpty()) continue;
                long count = Math.max(0L, slotTag.getLong("count"));
                tooltip.add(TextFormatting.LIGHT_PURPLE + " • " + TextFormatting.WHITE
                        + prototype.getDisplayName() + TextFormatting.GRAY + " × "
                        + TextFormatting.GREEN + count);
            }
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.equip_hint"));
    }
}
