package com.hbm.tileentity.network;

import com.hbm.api.ntl.IPneumaticConnector;
import com.hbm.api.ntl.StackCache;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import net.minecraft.util.ITickable;

public abstract class TileEntityPneumaticMachineBase extends TileEntityMachineBase implements ITickable, IPneumaticConnector, IGUIProvider {

    protected TileEntityPneumoTube.PneumaticNode node;
    public StackCache cache;

    protected TileEntityPneumaticMachineBase(int slots) {
        super(slots);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        if (node == null || node.expired) {
            if (cache != null) cache.dissolveCache();
            node = UniNodespace.getNode(world, pos, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
            if (node == null || node.expired) {
                node = new TileEntityPneumoTube.PneumaticNode(pos).setConnections(
                        new DirPos(pos.getX() + 1, pos.getY(), pos.getZ(), Library.POS_X),
                        new DirPos(pos.getX() - 1, pos.getY(), pos.getZ(), Library.NEG_X),
                        new DirPos(pos.getX(), pos.getY() + 1, pos.getZ(), Library.POS_Y),
                        new DirPos(pos.getX(), pos.getY() - 1, pos.getZ(), Library.NEG_Y),
                        new DirPos(pos.getX(), pos.getY(), pos.getZ() + 1, Library.POS_Z),
                        new DirPos(pos.getX(), pos.getY(), pos.getZ() - 1, Library.NEG_Z));
                UniNodespace.createNode(world, node);
            }
        }
        if (cache == null || cache.hasExpired) cache = new StackCache(pos.getX(), pos.getY(), pos.getZ());
        if (node != null && !node.expired && node.hasValidNet()) node.net.addStackCache(cache);
    }

    @Override
    public void invalidate() {
        detach(true);
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        detach(true);
        super.onChunkUnload();
    }

    private void detach(boolean destroyNode) {
        if (cache != null) cache.dissolveCache();
        cache = null;
        if (world != null && !world.isRemote && destroyNode && node != null) {
            UniNodespace.destroyNode(world, pos, PneumaticNetwork.THE_PNEUMATIC_PROVIDER);
        }
        node = null;
    }
}
