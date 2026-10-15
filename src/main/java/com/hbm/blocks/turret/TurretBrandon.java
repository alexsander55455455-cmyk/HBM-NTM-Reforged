package com.hbm.blocks.turret;

import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.turret.TileEntityTurretBrandon;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.List;

public class TurretBrandon extends TurretBaseNT {

  public TurretBrandon(Material materialIn, String s) {
    super(materialIn, s);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if (!world.isRemote && placer instanceof EntityPlayer) {
      TileEntity tile = findCoreTE(world, pos.up(getHeightOffset()));
      if (tile instanceof TileEntityTurretBrandon) {
        ((TileEntityTurretBrandon) tile).setOwner(placer.getUniqueID());
      }
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
    super.addInformation(stack, world, tooltip, flag);
    tooltip.add(I18n.format("desc.brandon.ammo"));
    tooltip.add(I18n.format("desc.brandon.firing"));
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    if (meta >= 12) return new TileEntityTurretBrandon();
    return new TileEntityProxyCombo(true, true, false);
  }
}
