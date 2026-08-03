package com.hbm.items.special;

import com.hbm.items.ItemBakedBase;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public class ItemRealityGlitch extends ItemBakedBase {

    public ItemRealityGlitch(String name) {
        super(name);
        setCreativeTab(null);
        setMaxStackSize(64);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public @NotNull String getItemStackDisplayName(@NotNull ItemStack stack) {
        return ItemRealityErrorBackpack.glitchText("GLITCH");
    }
}
