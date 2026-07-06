package com.hbm.tileentity.turret;

import com.hbm.interfaces.AutoRegister;

@AutoRegister
public class TileEntityTurretCheapo extends TileEntityTurretBase {

  public int spin;
  public int rotation;

  @Override
  public void update() {
    super.update();

    if (spin > 0) spin -= 1;

    rotation += spin;
    rotation = rotation % 360;
  }
}