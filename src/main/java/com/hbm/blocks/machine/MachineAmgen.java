package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.machine.TileEntityMachineAmgen;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.world.World;

public class MachineAmgen extends BlockContainer {

    public MachineAmgen(Material material, String name) {
        super(material);
        this.setTranslationKey(name);
        this.setRegistryName(name);
        ModBlocks.ALL_BLOCKS.add(this);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityMachineAmgen();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}
