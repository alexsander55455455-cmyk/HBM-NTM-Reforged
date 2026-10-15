package com.hbm.tileentity.network;

import com.hbm.api.ntl.IPneumaticConnector;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.network.TileEntityPneumoTube.PneumaticNode;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.util.math.BlockPos;

@AutoRegister
public class TileEntityPneumaticPipeAnchor extends TileEntityPipeAnchor implements IPneumaticConnector {

    private PneumaticNode pneumaticNode;

    @Override
    public NetworkType getNetworkType() {
        return NetworkType.PNEUMATIC;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return false;
    }

    @Override
    public boolean canConnectPneumatic(ForgeDirection dir) {
        return dir == ForgeDirection.getOrientation(getBlockMetadata()).getOpposite();
    }

    @Override
    public void update() {
        if (!world.isRemote && !isInvalid()) updateNode();
    }

    private void updateNode() {
        if (pneumaticNode == null || pneumaticNode.expired) {
            pneumaticNode = UniNodespace.getNode(world, pos, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
            if (pneumaticNode == null || pneumaticNode.expired) {
                ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata()).getOpposite();
                pneumaticNode = new PneumaticNode(pos).setConnections(
                        new DirPos(pos.getX(), pos.getY(), pos.getZ(), ForgeDirection.UNKNOWN),
                        new DirPos(pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir));
                for (int[] link : connected) pneumaticNode.addConnection(new DirPos(link[0], link[1], link[2], ForgeDirection.UNKNOWN));
                UniNodespace.createNode(world, pneumaticNode);
            }
        }
    }

    @Override
    protected void addNodeConnection(int x, int y, int z) {
        updateNode();
        pneumaticNode.recentlyChanged = true;
        pneumaticNode.addConnection(new DirPos(x, y, z, ForgeDirection.UNKNOWN));
    }

    @Override
    protected void destroyNode(BlockPos position) {
        UniNodespace.destroyNode(world, position, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
    }
}
