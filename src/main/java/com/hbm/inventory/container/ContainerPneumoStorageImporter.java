package com.hbm.inventory.container;

import com.hbm.tileentity.network.TileEntityPneumoStorageImporter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerPneumoStorageImporter extends ContainerBase {

    private final TileEntityPneumoStorageImporter importer;

    public ContainerPneumoStorageImporter(InventoryPlayer player, TileEntityPneumoStorageImporter importer) {
        super(player, importer.getCheckedInventory());
        this.importer = importer;
        addSlots(importer.getCheckedInventory(), 0, 62, 17, 3, 3);
        playerInv(player, 8, 103);
    }

    @Override public boolean canInteractWith(EntityPlayer player) { return importer.isUseableByPlayer(player); }
}
