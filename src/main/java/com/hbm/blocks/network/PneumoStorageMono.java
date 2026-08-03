package com.hbm.blocks.network;

import com.hbm.blocks.BlockContainerBakeableNormal;
import com.hbm.lib.InventoryHelper;
import com.hbm.main.MainRegistry;
import com.hbm.render.block.BlockBakeFrame;
import com.hbm.tileentity.network.TileEntityPneumoStorageMono;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import java.util.Random;

public class PneumoStorageMono extends BlockContainerBakeableNormal {

    public PneumoStorageMono(String registryName) {
        super(Material.IRON, registryName, BlockBakeFrame.cubeAll("pneumatic_storage_mono"));
    }

    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileEntityPneumoStorageMono(); }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        if (!world.isRemote) FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (!world.isRemote && willHarvest && !player.capabilities.isCreativeMode) {
            ItemStack drop = new ItemStack(this);
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityPneumoStorageMono mono) {
                NBTTagCompound contents = new NBTTagCompound();
                for (int i = 0; i < 3; i++) {
                    ItemStack type = mono.inventory.getStackInSlot(i);
                    if (!type.isEmpty()) contents.setTag("slot" + i, type.writeToNBT(new NBTTagCompound()));
                    contents.setInteger("amount" + i, mono.amounts[i]);
                }
                if (!contents.isEmpty()) drop.setTagCompound(contents);
            }
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), drop);
        }
        return world.setBlockToAir(pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityPneumoStorageMono mono && stack.hasTagCompound()) {
            NBTTagCompound contents = stack.getTagCompound();
            for (int i = 0; i < 3; i++) {
                mono.inventory.setStackInSlot(i, new ItemStack(contents.getCompoundTag("slot" + i)));
                mono.amounts[i] = Math.max(0, Math.min(TileEntityPneumoStorageMono.CAPACITY, contents.getInteger("amount" + i)));
            }
            mono.markDirty();
        }
        super.onBlockPlacedBy(world, pos, state, placer, stack);
    }

    @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return ItemStack.EMPTY.getItem(); }
}
