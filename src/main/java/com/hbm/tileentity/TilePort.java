package com.hbm.tileentity;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.NodeNet;
import com.hbm.uninos.UniNodespace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Machine-owned UNINOS ports, adapted from HbmMods X5846 (hbm). */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TilePort<R, P, L extends GenNode<N>, N extends NodeNet<R, P, L, N>> {
    protected L node;
    protected N subscribedNet;
    private long subscribedVersion;
    private boolean receiving;
    private boolean providing;
    protected INetworkProvider<N> type;
    protected Object owner;
    public BlockPos[] positions;
    public DirPos[] connections;
    public boolean needsRebuild;
    protected boolean enabled = true;

    public TilePort<R, P, L, N> setupOwner(Object owner) { this.owner = owner; return this; }
    public TilePort<R, P, L, N> setupType(INetworkProvider<N> type) {
        if (this.type != type) { this.type = type; needsRebuild = true; }
        return this;
    }
    public TilePort<R, P, L, N> setupPort(PortDef def) {
        return setupPositions(def.portPositions).setupConnections(def.portConnections);
    }
    public TilePort<R, P, L, N> setupPositions(BlockPos... positions) {
        this.positions = positions; needsRebuild = true; return this;
    }
    public TilePort<R, P, L, N> setupConnections(DirPos... connections) {
        this.connections = connections; needsRebuild = true; return this;
    }

    public void update(World world) {
        if (world.isRemote) return;
        if (needsRebuild || !enabled || type == null) {
            disableIfPresent(world);
            needsRebuild = false;
        }
        if (!enabled || type == null || owner == null || positions == null || connections == null) return;
        if (node == null || node.expired) {
            detach();
            node = (L) type.provideNode(positions).setConnections(connections);
            UniNodespace.createNode(world, node);
        }
        N current = getNetwork();
        if (current != subscribedNet || (current != null && current.getMembershipVersion() != subscribedVersion)) {
            detach();
            subscribedNet = current;
            if (current != null) subscribedVersion = current.getMembershipVersion();
        }
    }

    public void checkSubscribe(World world) {
        if (subscribedNet != null && !receiving) {
            subscribedNet.attachReceiverPort((R) owner);
            receiving = true;
        }
    }
    public void checkProvide(World world) {
        if (subscribedNet != null && !providing) {
            subscribedNet.attachProviderPort((P) owner);
            providing = true;
        }
    }
    public void unsubscribe() {
        if (receiving && subscribedNet != null && subscribedVersion == subscribedNet.getMembershipVersion()) subscribedNet.detachReceiverPort((R) owner);
        receiving = false;
    }
    public void unprovide() {
        if (providing && subscribedNet != null && subscribedVersion == subscribedNet.getMembershipVersion()) subscribedNet.detachProviderPort((P) owner);
        providing = false;
    }
    private void detach() {
        unsubscribe();
        unprovide();
        subscribedNet = null;
    }
    public void disableIfPresent(World world) {
        detach();
        if (node != null) {
            if (!node.expired) UniNodespace.destroyNode(world, node);
            node = null;
        }
    }
    public void forceRebuild() { needsRebuild = true; }
    public void enable() { enabled = true; }
    public void disable() { enabled = false; }
    public N getNetwork() {
        return node != null && !node.expired && node.hasValidNet() ? node.net : null;
    }
    public static TilePort[] manyToMany(Object owner, PortDef... defs) {
        TilePort[] ports = new TilePort[defs.length];
        for (int i = 0; i < defs.length; i++) ports[i] = new TilePort().setupOwner(owner).setupPort(defs[i]);
        return ports;
    }

    public static class PortDef {
        public BlockPos[] portPositions;
        public DirPos[] portConnections;
        public PortDef atPos(BlockPos... positions) { portPositions = positions; return this; }
        public PortDef withCon(DirPos... connections) { portConnections = connections; return this; }
        public static PortDef make(int x, int y, int z, ForgeDirection... directions) {
            DirPos[] connections = new DirPos[directions.length];
            for (int i = 0; i < directions.length; i++) {
                ForgeDirection dir = directions[i];
                connections[i] = new DirPos(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, dir);
            }
            return new PortDef().atPos(new BlockPos(x, y, z)).withCon(connections);
        }
        public static PortDef combine(PortDef... defs) {
            List<BlockPos> positions = new ArrayList<>();
            List<DirPos> connections = new ArrayList<>();
            for (PortDef def : defs) {
                java.util.Collections.addAll(positions, def.portPositions);
                java.util.Collections.addAll(connections, def.portConnections);
            }
            return new PortDef().atPos(positions.toArray(new BlockPos[0])).withCon(connections.toArray(new DirPos[0]));
        }
    }
}
