package com.hbm.inventory;

import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BackpackEquipmentSlot extends Slot {

    /** Mirrors the leggings slot above the vanilla offhand/shield slot. */
    public static final int PLAYER_INVENTORY_X = 77;
    public static final int PLAYER_INVENTORY_Y = 44;

    public BackpackEquipmentSlot(EntityPlayer player, int x, int y) {
        super(new BackpackEquipmentInventory(player), 0, x, y);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBackpack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getSlotTexture() {
        // The empty icon is drawn directly by the inventory GUI mixins.
        // A direct texture keeps it out of the item atlas and preserves its transparency.
        return null;
    }
}
