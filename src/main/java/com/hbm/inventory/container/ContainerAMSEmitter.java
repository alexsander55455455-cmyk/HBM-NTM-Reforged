package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityAMSEmitter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerAMSEmitter extends Container {

	private final TileEntityAMSEmitter amsEmitter;

	private int heat;
	private int warning;

	public ContainerAMSEmitter(InventoryPlayer invPlayer, TileEntityAMSEmitter tedf) {
		amsEmitter = tedf;

		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 0, 44, 17));
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 1, 44, 53));
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 2, 80, 53));
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 3, 116, 53));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(slotIndex);

		if(slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			result = stack.copy();

			if(slotIndex <= 3) {
				if(!this.mergeItemStack(stack, 4, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}

			if(stack.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
		}

		return result;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return amsEmitter.isUseableByPlayer(player);
	}

	@Override
	public void addListener(IContainerListener listener) {
		super.addListener(listener);
		listener.sendWindowProperty(this, 0, this.amsEmitter.heat);
		listener.sendWindowProperty(this, 2, this.amsEmitter.warning);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

		for(IContainerListener listener : this.listeners) {
			if(this.heat != this.amsEmitter.heat) {
				listener.sendWindowProperty(this, 0, this.amsEmitter.heat);
			}

			if(this.warning != this.amsEmitter.warning) {
				listener.sendWindowProperty(this, 2, this.amsEmitter.warning);
			}
		}

		this.heat = this.amsEmitter.heat;
		this.warning = this.amsEmitter.warning;
	}

	@Override
	public void updateProgressBar(int id, int data) {
		if(id == 0) {
			amsEmitter.heat = data;
		}
		if(id == 2) {
			amsEmitter.warning = data;
		}
	}
}