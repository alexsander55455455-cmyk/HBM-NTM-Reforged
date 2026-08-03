package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.lib.Library;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBackpackMagnetUpgrade extends ItemBackpackUpgrade implements IBatteryItem {

    private static final String CHARGE_TAG = "charge";

    private final long maxCharge;
    private final long transferRate;
    private final long pickupCost;
    private final double pickupRange;

    public ItemBackpackMagnetUpgrade(String name, int tier, long maxCharge, long transferRate,
                                     long pickupCost, double pickupRange, boolean secret) {
        this(name, name, tier, maxCharge, transferRate, pickupCost, pickupRange, secret);
    }

    public ItemBackpackMagnetUpgrade(String name, String texturePath, int tier, long maxCharge, long transferRate,
                                     long pickupCost, double pickupRange, boolean secret) {
        super(name, texturePath, BackpackUpgradeType.MAGNET, tier, 0, 0D, secret);
        this.maxCharge = maxCharge;
        this.transferRate = transferRate;
        this.pickupCost = pickupCost;
        this.pickupRange = pickupRange;
    }

    public long getPickupCost() {
        return pickupCost;
    }

    public double getPickupRange() {
        return pickupRange;
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
        if (amount > 0L) setCharge(stack, getCharge(stack) + amount);
    }

    @Override
    public void setCharge(ItemStack stack, long amount) {
        getOrCreateTag(stack).setLong(CHARGE_TAG, Math.max(0L, Math.min(maxCharge, amount)));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        if (amount > 0L) setCharge(stack, getCharge(stack) - amount);
    }

    @Override
    public long getCharge(ItemStack stack) {
        return stack.hasTagCompound()
                ? Math.max(0L, Math.min(maxCharge, stack.getTagCompound().getLong(CHARGE_TAG))) : 0L;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return transferRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return transferRate;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getCharge(stack) < maxCharge;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1D - (double) getCharge(stack) / (double) maxCharge;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack_upgrade.energy",
                Library.getShortNumber(getCharge(stack)), Library.getShortNumber(maxCharge)));
        tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.backpack_upgrade.pickup_range", pickupRange));
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack_upgrade.pickup_cost",
                Library.getShortNumber(pickupCost)));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }
}
