package com.hbm.module.portmanager;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePort.PortDef;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fixed-shape fluid ports following tank types, adapted from HbmMods X5846 (hbm). */
public class ModulePortManFluidAdaptive {
    private final TileEntityLoadedBase owner;
    private PortDef[] portReference;
    private PortDef shape;
    private FluidTankNTM[] inTanks = new FluidTankNTM[0], outTanks = new FluidTankNTM[0];
    private boolean[] inEnabled = new boolean[0], outEnabled = new boolean[0];
    private final Map<FluidType, TilePort> ports = new LinkedHashMap<>();

    public ModulePortManFluidAdaptive(TileEntityLoadedBase owner) { this.owner = owner; }
    public ModulePortManFluidAdaptive setInputTanks(FluidTankNTM... tanks) {
        inTanks = tanks;
        inEnabled = new boolean[tanks.length];
        Arrays.fill(inEnabled, true);
        return this;
    }
    public ModulePortManFluidAdaptive setOutputTanks(FluidTankNTM... tanks) {
        outTanks = tanks;
        outEnabled = new boolean[tanks.length];
        Arrays.fill(outEnabled, true);
        return this;
    }
    public void toggleInput(FluidTankNTM tank, boolean enabled) {
        for (int i = 0; i < inTanks.length; i++) if (inTanks[i] == tank) inEnabled[i] = enabled;
    }
    public void toggleOutput(FluidTankNTM tank, boolean enabled) {
        for (int i = 0; i < outTanks.length; i++) if (outTanks[i] == tank) outEnabled[i] = enabled;
    }
    public void destroy() {
        for (TilePort port : ports.values()) port.disableIfPresent(owner.getWorld());
        ports.clear();
    }
    public void update(PortDef[] defs) {
        if (owner.getWorld().isRemote) return;
        if (portReference != defs) {
            destroy();
            portReference = defs;
            shape = PortDef.combine(defs);
        }
        // One node per fluid, including shared input/output tanks. Duplicate nodes at the
        // same position would replace each other in nodespace and lose subscriptions.
        Map<FluidType, Integer> roles = new LinkedHashMap<>();
        if (owner instanceof IFluidReceiverMK2) collect(roles, inTanks, inEnabled, 1);
        if (owner instanceof IFluidProviderMK2) collect(roles, outTanks, outEnabled, 2);
        Iterator<Map.Entry<FluidType, TilePort>> iterator = ports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FluidType, TilePort> entry = iterator.next();
            if (!roles.containsKey(entry.getKey())) {
                entry.getValue().disableIfPresent(owner.getWorld());
                iterator.remove();
            }
        }
        for (Map.Entry<FluidType, Integer> entry : roles.entrySet()) {
            TilePort port = ports.get(entry.getKey());
            if (port == null) {
                port = new TilePort().setupOwner(owner).setupType(entry.getKey().getNetworkProvider()).setupPort(shape);
                ports.put(entry.getKey(), port);
            }
            port.update(owner.getWorld());
            if ((entry.getValue() & 1) != 0) port.checkSubscribe(owner.getWorld()); else port.unsubscribe();
            if ((entry.getValue() & 2) != 0) port.checkProvide(owner.getWorld()); else port.unprovide();
        }
    }
    private void collect(Map<FluidType, Integer> roles, FluidTankNTM[] tanks, boolean[] enabled, int role) {
        for (int i = 0; i < tanks.length; i++) {
            FluidType type = tanks[i].getTankType();
            if (enabled[i] && type != Fluids.NONE) roles.put(type, roles.getOrDefault(type, 0) | role);
        }
    }
}
