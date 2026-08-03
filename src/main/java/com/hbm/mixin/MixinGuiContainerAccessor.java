package com.hbm.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface MixinGuiContainerAccessor {

    @Accessor("guiLeft")
    int hbm$getGuiLeft();

    @Accessor("guiTop")
    int hbm$getGuiTop();

    @Accessor("inventorySlots")
    Container hbm$getInventorySlots();
}
