package com.hbm.items.tool;

import com.hbm.saveddata.BlackHoleBackpackSavedData;
import com.hbm.config.BackpackConfig;
import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.inventory.BlackHoleBackpackInventory;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The item itself only keeps a storage UUID. Its contents live in
 * {@link BlackHoleBackpackSavedData}, preventing massive backpack NBT from
 * being copied into player sync packets.
 */
public class ItemBlackHoleBackpack extends ItemBackpack implements IAutomatedBackpack {

    public static final int SLOTS = 143;
    public static final String STORAGE_ID_TAG = "BlackHoleStorageId";
    public static final String BASE_CAPACITY_TAG = "BlackHoleBaseCapacity";
    private static final String FILLED_SLOTS_TAG = "BlackHoleFilledSlots";
    private static final String STORED_RADIATION_TAG = "BlackHoleStoredRadiation";
    private static final String SORT_POLICY_VERSION_TAG = "BlackHoleSortPolicyVersion";
    private static final int SORT_POLICY_VERSION = 1;

    public ItemBlackHoleBackpack(String name) {
        super(name, SLOTS, 1D, false);
    }

    @Override
    public boolean protectsDroppedItemDamage(DamageSource source, float amount) {
        return true;
    }

    @Override
    public int getDroppedLavaSurvivalTicks() {
        return -1;
    }

    @Override
    public @NotNull String getItemStackDisplayName(@NotNull ItemStack stack) {
        return TextFormatting.LIGHT_PURPLE + super.getItemStackDisplayName(stack) + TextFormatting.RESET;
    }

    @Override
    public IBackpackInventory createInventory(ItemStack backpack, @Nullable World world) {
        return BlackHoleBackpackInventory.create(this, backpack, world);
    }

    @Override
    public int getStorageSlots(ItemStack backpack) {
        return BackpackConfig.getStorageSlots(backpack, SLOTS);
    }

    @Override
    public void prepareServerStorage(ItemStack backpack, World world) {
        super.prepareServerStorage(backpack, world);
        if (!world.isRemote) {
            getOrCreateStorageId(backpack);
            NBTTagCompound tag = backpack.getTagCompound();
            BlackHoleBackpackSavedData.BackpackStorage storage = getServerStorage(world, backpack);
            if (!tag.hasKey(BASE_CAPACITY_TAG)) {
                tag.setInteger(BASE_CAPACITY_TAG, Math.max(SLOTS, storage.getSlots()));
            }
            if (tag.getInteger(SORT_POLICY_VERSION_TAG) < SORT_POLICY_VERSION) {
                if (BackpackUpgradeManager.isAutoSortEnabled(backpack)) {
                    storage.compactAndMergeStable(BackpackConfig.allowsOverstack(backpack));
                }
                tag.setInteger(SORT_POLICY_VERSION_TAG, SORT_POLICY_VERSION);
                setFilledSlotCount(backpack, storage.getFilledSlotCount());
                setStoredRadiation(backpack, storage.getStoredRadiation());
            }
        }
    }

    @Override
    public int getFilledSlotCount(ItemStack stack) {
        return stack.hasTagCompound() ? Math.max(0, stack.getTagCompound().getInteger(FILLED_SLOTS_TAG)) : 0;
    }

    public void setFilledSlotCount(ItemStack stack, int filledSlots) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(FILLED_SLOTS_TAG, Math.max(0, filledSlots));
    }

    public void setStoredRadiation(ItemStack stack, double radiation) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setDouble(STORED_RADIATION_TAG, Math.max(0D, radiation));
    }

    @Override
    protected double getContainedRadiation(ItemStack stack) {
        return stack.hasTagCompound() ? Math.max(0D, stack.getTagCompound().getDouble(STORED_RADIATION_TAG)) : 0D;
    }

    /**
     * Creates an identifier only when the backpack is first used server-side.
     * Copies of the same item retain the identifier and therefore point to the
     * same storage instead of duplicating its contents.
     */
    public UUID getOrCreateStorageId(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
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

    /** Server-only entry point for the future dynamic item handler. */
    public BlackHoleBackpackSavedData.BackpackStorage getServerStorage(World world, ItemStack stack) {
        if (world.isRemote) {
            throw new IllegalStateException("Black hole backpack storage is server-side only");
        }
        return BlackHoleBackpackSavedData.get(world).getStorage(getOrCreateStorageId(stack));
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
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        String green = TextFormatting.GREEN.toString();
        String capacity = BackpackConfig.hasInfiniteSlots(stack)
                ? "\u221E" : String.valueOf(getStorageSlots(stack));
        tooltip.add(green + I18nUtil.resolveKey("container.hbm_backpack.capacity", green + capacity));
        tooltip.add(green + I18nUtil.resolveKey("container.hbm_backpack.filled",
                green + getFilledSlotCount(stack), green + capacity));
        tooltip.add(green + I18nUtil.resolveKey("desc.backpack.leakage_radiation", getLeakedRadiationText(stack)));
        tooltip.add(green + I18nUtil.resolveKey("desc.backpack.black_hole.storage"));
        tooltip.add(green + I18nUtil.resolveKey("desc.backpack.black_hole.shielding"));
        tooltip.add(green + I18nUtil.resolveKey("desc.backpack.dropped.indestructible"));
        tooltip.add(green + I18nUtil.resolveKey(BackpackUpgradeManager.isAutoPickupEnabled(stack)
                ? "desc.backpack.black_hole.magnet_enabled"
                : "desc.backpack.black_hole.magnet_disabled"));
        tooltip.add(green + I18nUtil.resolveKey("desc.backpack.black_hole.automation"));
        addBuiltInMagnetEnergyInformation(stack, tooltip);
    }
}
