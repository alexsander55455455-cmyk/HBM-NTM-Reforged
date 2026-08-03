package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.config.BackpackConfig;
import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The powered end-game backpack. Manual storage works at any charge level;
 * only automatic collection consumes energy when required by the backpack config.
 */
public class ItemDineutroniumBackpack extends ItemBackpack implements IBatteryItem, IAutomatedBackpack {

    public static final int SLOTS = 182;
    public static final long MAX_CHARGE = 10_000_000L;
    public static final long CHARGE_RATE = 100_000L;
    public static final long DISCHARGE_RATE = 100_000L;
    public static final long AUTO_PICKUP_COST = 1_000L;
    private static final String CHARGE_TAG = "charge";

    public ItemDineutroniumBackpack(String name) {
        super(name, SLOTS, 1D, false);
    }

    @Override
    public IBackpackInventory createInventory(ItemStack backpack, @Nullable World world) {
        if (BackpackConfig.usesSparseStorage(backpack)) return super.createInventory(backpack, world);
        return new DineutroniumBackpackInventory(backpack, world == null || !world.isRemote);
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
        if (amount <= 0L) return;
        long current = getCharge(stack);
        long added = Math.min(amount, getMaxCharge(stack) - current);
        if (added > 0L) {
            getOrCreateTag(stack).setLong(CHARGE_TAG, current + added);
        }
    }

    @Override
    public void setCharge(ItemStack stack, long amount) {
        getOrCreateTag(stack).setLong(CHARGE_TAG, Math.max(0L, Math.min(getMaxCharge(stack), amount)));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        if (amount <= 0L) return;
        long current = getCharge(stack);
        if (current > 0L) {
            getOrCreateTag(stack).setLong(CHARGE_TAG, Math.max(0L, current - amount));
        }
    }

    @Override
    public long getCharge(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0L;
        return Math.max(0L, Math.min(getMaxCharge(stack), stack.getTagCompound().getLong(CHARGE_TAG)));
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return BackpackConfig.autoMagnetRequiresEnergy(stack)
                ? BackpackConfig.getAutoMagnetEnergyCapacity(stack) : 0L;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return CHARGE_RATE;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return DISCHARGE_RATE;
    }

    @Override
    public boolean canAutoPickup(ItemStack stack) {
        return BackpackUpgradeManager.canAutoPickup(stack);
    }

    @Override
    public boolean consumeAutoPickupEnergy(ItemStack stack) {
        return BackpackUpgradeManager.consumeAutoPickupEnergy(stack);
    }

    @Override
    public long getAutoPickupEnergyCost() {
        return AUTO_PICKUP_COST;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getMaxCharge(stack) > 0L && getCharge(stack) < getMaxCharge(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        long maximum = getMaxCharge(stack);
        return maximum <= 0L ? 0D : 1D - (double) getCharge(stack) / (double) maximum;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add((BackpackUpgradeManager.isAutoPickupEnabled(stack) ? TextFormatting.GREEN : TextFormatting.RED)
                + I18nUtil.resolveKey(BackpackUpgradeManager.isAutoPickupEnabled(stack)
                ? "desc.backpack.dineutronium.magnet_enabled"
                : "desc.backpack.dineutronium.magnet_disabled"));
        tooltip.add((BackpackUpgradeManager.isAutoSortEnabled(stack) ? TextFormatting.GREEN : TextFormatting.RED)
                + I18nUtil.resolveKey(BackpackUpgradeManager.isAutoSortEnabled(stack)
                ? "desc.backpack.dineutronium.sort_enabled"
                : "desc.backpack.dineutronium.sort_disabled"));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static class DineutroniumBackpackInventory extends BackpackInventory {
        private final ItemStack backpack;
        private final boolean authoritative;

        private DineutroniumBackpackInventory(ItemStack backpack, boolean authoritative) {
            super(backpack);
            this.backpack = backpack;
            this.authoritative = authoritative;
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
            BackpackUpgradeManager.setAutoPickupEnabled(backpack, enabled);
        }

        @Override
        public boolean supportsAutoSorting() {
            return BackpackUpgradeManager.supportsAutoSorting(backpack);
        }

        @Override
        public boolean supportsManualSorting() {
            return false;
        }

        @Override
        public boolean isAutoSortEnabled() {
            return BackpackUpgradeManager.isAutoSortEnabled(backpack);
        }

        @Override
        public void setAutoSortEnabled(boolean enabled) {
            boolean wasEnabled = isAutoSortEnabled();
            BackpackUpgradeManager.setAutoSortEnabled(backpack, enabled);
            if (authoritative && enabled && !wasEnabled) {
                sortContents();
            }
        }
    }
}
