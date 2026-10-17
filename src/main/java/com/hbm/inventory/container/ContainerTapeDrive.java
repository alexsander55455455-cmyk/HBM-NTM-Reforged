package com.hbm.inventory.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.IItemHandler;

public class ContainerTapeDrive extends ContainerBase {
    public ContainerTapeDrive(InventoryPlayer player, IItemHandler inventory) {
        super(player, inventory);
        addSlots(inventory, 0, 35, 27, 2, 6);
        playerInv(player, 8, 104);
    }
}
