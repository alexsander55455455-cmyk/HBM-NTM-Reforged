package com.hbm.items.tool;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBackpackWorkbenchStateTest {

    @BeforeAll
    static void registerVanillaItems() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void workbenchPanelStateStaysOnTheExactBackpackStack() {
        Item item = new Item();
        ItemStack first = new ItemStack(item);
        ItemStack second = new ItemStack(item);

        assertFalse(ItemBackpack.isWorkbenchPanelOpen(first));
        ItemBackpack.setWorkbenchPanelOpen(first, true);
        assertTrue(ItemBackpack.isWorkbenchPanelOpen(first));
        assertFalse(ItemBackpack.isWorkbenchPanelOpen(second));
        ItemBackpack.setWorkbenchPanelOpen(first, false);
        assertFalse(ItemBackpack.isWorkbenchPanelOpen(first));
    }
}
