package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.UniNodespace;
import net.minecraft.util.math.BlockPos;

@AutoRegister
public class TileEntityPipeExhaustAnchor extends TileEntityPipeAnchor {

    private final FluidType[] smokes = {Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON};
    private final FluidNode[] nodes = new FluidNode[3];

    @Override
    public NetworkType getNetworkType() {
        return NetworkType.EXHAUST;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        if (dir != ForgeDirection.getOrientation(getBlockMetadata()).getOpposite()) return false;
        return type == Fluids.SMOKE || type == Fluids.SMOKE_LEADED || type == Fluids.SMOKE_POISON;
    }

    @Override
    public void update() {
        if (world.isRemote || isInvalid()) return;
        for (int i = 0; i < smokes.length; i++) updateNode(i);
    }

    private void updateNode(int index) {
        if (nodes[index] == null || nodes[index].expired) {
            nodes[index] = UniNodespace.getNode(world, pos, smokes[index].getNetworkProvider());
            if (nodes[index] == null || nodes[index].expired) {
                nodes[index] = createNode(smokes[index]);
                UniNodespace.createNode(world, nodes[index]);
            }
        }
    }

    @Override
    protected void addNodeConnection(int x, int y, int z) {
        for (int i = 0; i < smokes.length; i++) {
            updateNode(i);
            nodes[i].recentlyChanged = true;
            nodes[i].addConnection(new DirPos(x, y, z, ForgeDirection.UNKNOWN));
        }
    }

    @Override
    protected void destroyNode(BlockPos position) {
        for (FluidType smoke : smokes) UniNodespace.destroyNode(world, position, smoke.getNetworkProvider());
    }
}
