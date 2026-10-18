package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.entity.player.InventoryPlayer;


public class ContainerLaunchpadLambda extends ContainerBase {
    private final TileEntityLaunchpadLambda launcher;
    @Override public boolean canInteractWith(EntityPlayer player) { return launcher.isUseableByPlayer(player); }
	
	public ContainerLaunchpadLambda(InventoryPlayer invPlayer, TileEntityLaunchpadLambda launcher) {
		super(invPlayer, launcher.getCheckedInventory());
        this.launcher = launcher;
		
		//Lambda
		this.addSlotToContainer(new SlotNonRetarded(launcher.getCheckedInventory(), 0, 35, 17));
		//Satellite
		this.addSlotToContainer(new SlotNonRetarded(launcher.getCheckedInventory(), 1, 53, 17));
		//Kerosene IN
		this.addSlotToContainer(new SlotNonRetarded(launcher.getCheckedInventory(), 2, 107, 80));
		//Kerosene OUT
		this.addSlotToContainer(SlotFiltered.takeOnly(launcher.getCheckedInventory(), 3, 107, 98));
		//Oxyden IN
		this.addSlotToContainer(new SlotNonRetarded(launcher.getCheckedInventory(), 4, 125, 80));
		//Oxyden OUT
		this.addSlotToContainer(SlotFiltered.takeOnly(launcher.getCheckedInventory(), 5, 125, 98));
		//Battery
		this.addSlotToContainer(new SlotNonRetarded(launcher.getCheckedInventory(), 6, 89, 80));
		
		this.playerInv(invPlayer, 8, 144);
	}
}
