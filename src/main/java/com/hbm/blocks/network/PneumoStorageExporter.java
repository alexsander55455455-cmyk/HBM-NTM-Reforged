package com.hbm.blocks.network;

import com.hbm.blocks.BlockContainerBakeableNormal;
import com.hbm.lib.InventoryHelper;
import com.hbm.main.MainRegistry;
import com.hbm.render.block.BlockBakeFrame;
import com.hbm.tileentity.network.TileEntityPneumoStorageExporter;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

public class PneumoStorageExporter extends BlockContainerBakeableNormal {

    public PneumoStorageExporter(String registryName) {
        super(Material.IRON, registryName, BlockBakeFrame.cubeAll("pneumatic_storage_exporter"));
    }

    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileEntityPneumoStorageExporter(); }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (!world.isRemote) FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        InventoryHelper.dropInventoryItems(world, pos, world.getTileEntity(pos), 9, 17);
        super.breakBlock(world, pos, state);
    }
}
