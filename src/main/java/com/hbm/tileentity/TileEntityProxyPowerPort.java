package com.hbm.tileentity;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.interfaces.AutoRegister;

/** Lets legacy machines find the node owned by a TilePort at this proxy. */
@AutoRegister
public class TileEntityProxyPowerPort extends TileEntityProxyCombo implements IEnergyConductorMK2 {
    public TileEntityProxyPowerPort() { power(); }
    @Override public Nodespace.PowerNode createNode() { return Nodespace.getNode(world, pos); }
}
