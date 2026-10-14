package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;

public class BlockNoSpawn extends BlockBase {
    public BlockNoSpawn(Material material, String name) {
        super(material, name);
    }

    @Override
    public boolean canEntitySpawn(IBlockState state, Entity entity) {
        return false;
    }
}
