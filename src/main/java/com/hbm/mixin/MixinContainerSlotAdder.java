package com.hbm.mixin;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Container.class)
public interface MixinContainerSlotAdder {

    @Invoker("addSlotToContainer")
    Slot hbm$addSlotToContainer(Slot slot);
}
