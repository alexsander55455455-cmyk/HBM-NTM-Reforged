package com.hbm.tileentity.turret;

import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.inventory.gui.GUITurretChekhov;
import com.hbm.tileentity.IGUIProvider;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@AutoRegister
public class TileEntityTurretBrandon extends TileEntityTurretBaseNT implements IGUIProvider {

  @Override
  public long getMaxPower() {
    return 10000;
  }

  @Override
  public void updateFiringTick() {
    // Placeholder: full Brandon boss-turret firing not ported yet.
  }

  @Override
  protected List<Integer> getAmmoList() {
    return Collections.emptyList();
  }

  @Override
  public String getDefaultName() {
    return "container.turretBrandon";
  }

  @Override
  public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
    return new ContainerTurretBase(player.inventory, this);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
    return new GUITurretChekhov(player.inventory, this);
  }
}