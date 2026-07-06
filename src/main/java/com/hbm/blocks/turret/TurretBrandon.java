package com.hbm.blocks.turret;

import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.turret.TileEntityTurretBrandon;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TurretBrandon extends TurretBaseNT {

  public TurretBrandon(Material materialIn, String s) {
    super(materialIn, s);
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    if (meta >= 12) return new TileEntityTurretBrandon();
    return new TileEntityProxyCombo(true, true, false);
  }
}