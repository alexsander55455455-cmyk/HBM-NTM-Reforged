package com.hbm.items.tool;

import com.hbm.items.ItemBakedBase;
import com.hbm.main.MainRegistry;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBackpackUpgrade extends ItemBakedBase {

    private final BackpackUpgradeType type;
    private final int tier;
    private final int capacityBonus;
    private final double rangeBonus;
    private final boolean secret;

    public ItemBackpackUpgrade(String name, BackpackUpgradeType type, int tier,
                               int capacityBonus, double rangeBonus, boolean secret) {
        this(name, name, type, tier, capacityBonus, rangeBonus, secret);
    }

    public ItemBackpackUpgrade(String name, String texturePath, BackpackUpgradeType type, int tier,
                               int capacityBonus, double rangeBonus, boolean secret) {
        super(name, texturePath);
        this.type = type;
        this.tier = tier;
        this.capacityBonus = capacityBonus;
        this.rangeBonus = rangeBonus;
        this.secret = secret;
        setMaxStackSize(1);
        setCreativeTab(MainRegistry.consumableTab);
    }

    public BackpackUpgradeType getUpgradeType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    public int getCapacityBonus() {
        return capacityBonus;
    }

    public double getRangeBonus() {
        return rangeBonus;
    }

    public boolean isSecret() {
        return secret;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return secret && type != BackpackUpgradeType.AMMO_FEEDER;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack_upgrade.type." + type.name().toLowerCase()));
        if (tier > 0) {
            tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.backpack_upgrade.tier", tier));
        }
        if (capacityBonus > 0) {
            tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey(
                    "desc.backpack_upgrade.rows", Math.max(1, capacityBonus / 9)));
        }
        if (rangeBonus > 0D) {
            tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.backpack_upgrade.range", rangeBonus));
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack_upgrade.install"));
        if (secret) {
            tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("desc.backpack_upgrade.secret"));
        }
    }
}
