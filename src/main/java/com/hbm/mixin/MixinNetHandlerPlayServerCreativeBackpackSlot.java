package com.hbm.mixin;

import com.hbm.handler.BackpackHandler;
import com.hbm.inventory.BackpackEquipmentInventory;
import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creative sends direct slot snapshots instead of normal window-click
 * transactions. Vanilla only accepts ContainerPlayer slots 1..45, so the
 * capability-backed backpack slot added at the end needs an explicit,
 * validated server-side update.
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServerCreativeBackpackSlot {

    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "processCreativeInventoryAction", at = @At("RETURN"))
    private void hbm$updateCreativeBackpackSlot(CPacketCreativeInventoryAction packet, CallbackInfo ci) {
        int equipmentSlot = hbm$getEquipmentSlot();
        if (equipmentSlot < 0 || packet.getSlotId() != equipmentSlot) return;
        if (!player.interactionManager.isCreative()) return;

        ItemStack requested = packet.getStack();
        if (!requested.isEmpty() && !(requested.getItem() instanceof ItemBackpack)) {
            BackpackHandler.syncEquipmentState(player);
            return;
        }

        BackpackHandler.setEquippedBackpack(player, requested);
    }

    private int hbm$getEquipmentSlot() {
        for (int index = 0; index < player.inventoryContainer.inventorySlots.size(); index++) {
            Slot slot = player.inventoryContainer.inventorySlots.get(index);
            if (slot.inventory instanceof BackpackEquipmentInventory) {
                return index;
            }
        }
        return -1;
    }
}
