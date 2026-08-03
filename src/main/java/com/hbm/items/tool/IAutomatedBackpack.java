package com.hbm.items.tool;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Shared automation switch for the late-game backpacks.
 *
 * The switch lives on the item stack so it stays with the backpack when it is
 * moved, dropped, or equipped by another player.
 */
public interface IAutomatedBackpack {

    String AUTO_PICKUP_TAG = "BackpackAutoPickup";
    String AUTO_SORT_TAG = "BackpackAutoSort";

    default boolean isAutoPickupEnabled(ItemStack stack) {
        return !stack.hasTagCompound() || !stack.getTagCompound().hasKey(AUTO_PICKUP_TAG)
                || stack.getTagCompound().getBoolean(AUTO_PICKUP_TAG);
    }

    default void setAutoPickupEnabled(ItemStack stack, boolean enabled) {
        NBTTagCompound tag = getOrCreateAutomationTag(stack);

        // Old backpacks used one switch for both actions. Preserve that old
        // sorting state the first time the new magnet switch is changed.
        if (!tag.hasKey(AUTO_SORT_TAG)) {
            tag.setBoolean(AUTO_SORT_TAG, isAutoPickupEnabled(stack));
        }
        tag.setBoolean(AUTO_PICKUP_TAG, enabled);
    }

    default boolean toggleAutoPickup(ItemStack stack) {
        boolean enabled = !isAutoPickupEnabled(stack);
        setAutoPickupEnabled(stack, enabled);
        return enabled;
    }

    /**
     * Whether accepted auto-picked stacks are sorted after insertion. Older
     * backpack stacks fall back to their original combined automation switch.
     */
    default boolean isAutoSortEnabled(ItemStack stack) {
        return !stack.hasTagCompound() || !stack.getTagCompound().hasKey(AUTO_SORT_TAG)
                ? isAutoPickupEnabled(stack)
                : stack.getTagCompound().getBoolean(AUTO_SORT_TAG);
    }

    default void setAutoSortEnabled(ItemStack stack, boolean enabled) {
        getOrCreateAutomationTag(stack).setBoolean(AUTO_SORT_TAG, enabled);
    }

    default boolean toggleAutoSort(ItemStack stack) {
        boolean enabled = !isAutoSortEnabled(stack);
        setAutoSortEnabled(stack, enabled);
        return enabled;
    }

    /** Returns whether this stack may currently collect a dropped item. */
    boolean canAutoPickup(ItemStack stack);

    /**
     * Called only after an item stack was actually transferred. Returns false
     * when the required energy was no longer available.
     */
    boolean consumeAutoPickupEnergy(ItemStack stack);

    long getAutoPickupEnergyCost();

    static NBTTagCompound getOrCreateAutomationTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
