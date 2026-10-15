package com.hbm.module.portmanager;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePort.PortDef;

/** Fixed-shape power ports, adapted from HbmMods X5846 (hbm). */
public class ModulePortManPower {
    protected final TileEntityLoadedBase owner;
    protected PortDef[] portReference;
    public TilePort[] powerPorts;
    public boolean input = true;
    public boolean output = true;

    public ModulePortManPower(TileEntityLoadedBase owner) { this.owner = owner; }
    public ModulePortManPower onlyInput() { input = true; output = false; return this; }
    public ModulePortManPower onlyOutput() { input = false; output = true; return this; }
    public void destroy() {
        if (powerPorts != null) for (TilePort port : powerPorts) port.disableIfPresent(owner.getWorld());
    }
    public void update(PortDef[] ports) {
        if (owner.getWorld().isRemote) return;
        if (portReference != ports) {
            destroy();
            portReference = ports;
            powerPorts = TilePort.manyToMany(owner, ports);
            for (TilePort port : powerPorts) port.setupType(Nodespace.THE_POWER_PROVIDER);
        }
        for (TilePort port : powerPorts) {
            port.update(owner.getWorld());
            if (input && owner instanceof IEnergyReceiverMK2) port.checkSubscribe(owner.getWorld()); else port.unsubscribe();
            if (output && owner instanceof IEnergyProviderMK2) port.checkProvide(owner.getWorld()); else port.unprovide();
        }
    }
}
