package com.hbm.inventory.container;

import com.hbm.inventory.slot.SlotNonRetarded;

import net.minecraft.entity.player.InventoryPlayer;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import com.hbm.inventory.slot.SlotFiltered;
import net.minecraft.inventory.Slot;

public class ContainerLaunchpadSoyuz extends ContainerBase {
    private final TileEntityLaunchpadSoyuz launcher;
    @Override public boolean canInteractWith(EntityPlayer player) { return launcher.isUseableByPlayer(player); }
	
	public ContainerLaunchpadSoyuz(InventoryPlayer invPlayer, TileEntityLaunchpadSoyuz launcher) {
		super(invPlayer, launcher.getCheckedInventory());
        this.launcher = launcher;
        IItemHandler tedf = launcher.getCheckedInventory();
		
		//Soyuz
		this.addSlotToContainer(new SlotNonRetarded(tedf, 0, 98, 80));
		//Designator
		this.addSlotToContainer(new SlotNonRetarded(tedf, 1, 80, 80));
		//Satellite
		this.addSlotToContainer(new SlotNonRetarded(tedf, 2, 98, 26));
		//Landing module
		this.addSlotToContainer(new SlotNonRetarded(tedf, 3, 80, 26));
		//Kerosene IN
		this.addSlotToContainer(new SlotNonRetarded(tedf, 4, 152, 98));
		//Kerosene OUT
		this.addSlotToContainer(SlotFiltered.takeOnly(tedf, 5, 152, 116));
		//Oxyden IN
		this.addSlotToContainer(new SlotNonRetarded(tedf, 6, 170, 98));
		//Oxyden OUT
		this.addSlotToContainer(SlotFiltered.takeOnly(tedf, 7, 170, 116));
		//Battery
		this.addSlotToContainer(new SlotNonRetarded(tedf, 8, 134, 98));
		
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 6; j++) {
				this.addSlotToContainer(new SlotNonRetarded(tedf, j + i * 6 + 9, 44 - i * 18, 26 + j * 18));
			}
		}

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 17 + j * 18, 162 + i * 18));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 17 + i * 18, 220));
		}
	}
}
