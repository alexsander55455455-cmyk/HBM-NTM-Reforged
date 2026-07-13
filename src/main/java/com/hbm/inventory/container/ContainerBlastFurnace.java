package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotCraftingOutput;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerBlastFurnace extends ContainerBase {

    protected final TileEntityMachineBlastFurnace tile;

    public ContainerBlastFurnace(InventoryPlayer invPlayer, TileEntityMachineBlastFurnace furnace) {
        super(invPlayer, furnace.inventory);
        this.tile = furnace;

        this.addSlotToContainer(new SlotNonRetarded(furnace.inventory, 0, 80, 81));
        this.addSlotToContainer(new SlotNonRetarded(furnace.inventory, 1, 80, 27));
        this.addSlotToContainer(new SlotNonRetarded(furnace.inventory, 2, 80, 45));
        this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, furnace.inventory, 3, 134, 72));
        this.addSlotToContainer(new SlotCraftingOutput(invPlayer.player, furnace.inventory, 4, 134, 90));

        this.playerInv(invPlayer, 8, 140);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.isUseableByPlayer(player);
    }
}