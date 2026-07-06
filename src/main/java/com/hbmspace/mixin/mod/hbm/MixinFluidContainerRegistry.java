package com.hbmspace.mixin.mod.hbm;

import com.hbm.inventory.FluidContainerRegistry;
import com.hbmspace.blocks.ModBlocksSpace;
import com.hbmspace.items.ModItemsSpace;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidContainerRegistry.class, remap = false)
public class MixinFluidContainerRegistry {

	@Inject(method = "register", at = @At(value = "INVOKE", target = "Lcom/hbm/capability/NTMFluidCapabilityHandler;initialize()V"))
	private static void registerSpaceBuckets(CallbackInfo ci) {
		FluidContainerRegistry.registerContainer(new FluidContainerRegistry.FluidContainer(
				new ItemStack(ModItemsSpace.bucket_bromine),
				new ItemStack(Items.BUCKET),
				com.hbmspace.inventory.fluid.Fluids.BROMINE,
				1000
		));
		FluidContainerRegistry.registerContainer(new FluidContainerRegistry.FluidContainer(
				new ItemStack(ModBlocksSpace.bromine_block),
				new ItemStack(Items.BUCKET),
				com.hbmspace.inventory.fluid.Fluids.BROMINE,
				1000
		));
	}
}