package com.hbm.inventory.container;

import com.hbm.inventory.TransferStrategy;
import com.hbm.inventory.slot.SlotBattery;
import com.hbm.inventory.slot.SlotFiltered;
import com.hbm.tileentity.machine.TileEntityMachineUUCreator;
import com.hbm.util.InventoryUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ContainerMachineUUCreator extends Container {

	private final TileEntityMachineUUCreator uuCreator;

	private static final TransferStrategy TRANSFER_STRATEGY = TransferStrategy.builder(4)
			.genericMachineRange(0)
			.build();

	public ContainerMachineUUCreator(InventoryPlayer invPlayer, TileEntityMachineUUCreator tedf) {
		uuCreator = tedf;

		this.addSlotToContainer(new SlotBattery(tedf.inventory, 0, 48, 53));
		this.addSlotToContainer(SlotFiltered.takeOnly(tedf.inventory, 1, 48, 69));
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 2, 112, 53));
		this.addSlotToContainer(SlotFiltered.takeOnly(tedf.inventory, 3, 112, 69));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 20));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142 + 20));
		}
	}

	@Override
	public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index) {
		return InventoryUtil.transferStack(this.inventorySlots, index, TRANSFER_STRATEGY, player);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return uuCreator.isUseableByPlayer(player);
	}
}