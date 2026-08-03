package com.hbm.inventory;

import com.hbm.items.tool.ItemBlackHoleBackpack;
import com.hbm.saveddata.BlackHoleBackpackSavedData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

/** Shared item identity and compact client summary for sparse backpack storage. */
public final class BackpackVirtualStorage {

    public static final String STORAGE_ID_TAG = "BackpackVirtualStorageId";
    public static final String OVERSTACK_POLICY_TAG = "BackpackVirtualAllowOverstack";
    private static final String FILLED_SLOTS_TAG = "BackpackVirtualFilledSlots";
    private static final String STORED_RADIATION_TAG = "BackpackVirtualStoredRadiation";

    private BackpackVirtualStorage() {
    }

    public static UUID getOrCreateStorageId(ItemStack backpack) {
        if (backpack.getItem() instanceof ItemBlackHoleBackpack blackHole) {
            return blackHole.getOrCreateStorageId(backpack);
        }
        NBTTagCompound tag = getOrCreateTag(backpack);
        if (!tag.hasUniqueId(STORAGE_ID_TAG)) tag.setUniqueId(STORAGE_ID_TAG, UUID.randomUUID());
        return tag.getUniqueId(STORAGE_ID_TAG);
    }

    @Nullable
    public static UUID getStorageId(ItemStack backpack) {
        if (backpack.getItem() instanceof ItemBlackHoleBackpack blackHole) {
            return blackHole.getStorageId(backpack);
        }
        NBTTagCompound tag = backpack.getTagCompound();
        return tag != null && tag.hasUniqueId(STORAGE_ID_TAG) ? tag.getUniqueId(STORAGE_ID_TAG) : null;
    }

    public static BlackHoleBackpackSavedData.BackpackStorage getStorage(World world, ItemStack backpack) {
        if (world.isRemote) throw new IllegalStateException("Virtual backpack storage is server-side only");
        return BlackHoleBackpackSavedData.get(world).getStorage(getOrCreateStorageId(backpack));
    }

    public static void updateSummary(ItemStack backpack,
                                     BlackHoleBackpackSavedData.BackpackStorage storage) {
        if (backpack.getItem() instanceof ItemBlackHoleBackpack blackHole) {
            blackHole.setFilledSlotCount(backpack, storage.getFilledSlotCount());
            blackHole.setStoredRadiation(backpack, storage.getStoredRadiation());
            return;
        }
        NBTTagCompound tag = getOrCreateTag(backpack);
        tag.setInteger(FILLED_SLOTS_TAG, Math.max(0, storage.getFilledSlotCount()));
        tag.setDouble(STORED_RADIATION_TAG, Math.max(0D, storage.getStoredRadiation()));
    }

    public static int getCachedFilledSlots(ItemStack backpack) {
        NBTTagCompound tag = backpack.getTagCompound();
        return tag == null ? 0 : Math.max(0, tag.getInteger(FILLED_SLOTS_TAG));
    }

    public static double getCachedRadiation(ItemStack backpack) {
        NBTTagCompound tag = backpack.getTagCompound();
        return tag == null ? 0D : Math.max(0D, tag.getDouble(STORED_RADIATION_TAG));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }
}
