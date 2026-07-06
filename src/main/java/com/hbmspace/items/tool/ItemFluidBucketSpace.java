package com.hbmspace.items.tool;

import com.hbm.main.MainRegistry;
import com.hbmspace.items.ModItemsSpace;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;

public class ItemFluidBucketSpace extends ItemBucket {

	public ItemFluidBucketSpace(Block fluidBlock, String name) {
		super(fluidBlock);
		this.setTranslationKey(name);
		this.setRegistryName(name);
		this.setCreativeTab(MainRegistry.blockTab);
		this.setContainerItem(Items.BUCKET);
		ModItemsSpace.ALL_ITEMS.add(this);
	}
}