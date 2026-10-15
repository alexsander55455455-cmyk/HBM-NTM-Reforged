package com.hbm.uninos;

import com.hbm.api.tile.ILoadedTile;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.tileentity.TileEntity;

import java.util.Random;

public abstract class NodeNet<R, P, L extends GenNode<N>, N extends NodeNet<R, P, L, N>> {
    /** Global random for figuring things out like random leftover distribution */
    public static Random rand = new Random();

    public boolean valid = true;
    public ReferenceLinkedOpenHashSet<L> links = new ReferenceLinkedOpenHashSet<>();

    public Object2LongOpenHashMap<R> receiverEntries = new Object2LongOpenHashMap<>();
    public Object2LongOpenHashMap<P> providerEntries = new Object2LongOpenHashMap<>();
    private final Object2IntOpenHashMap<R> receiverPorts = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<P> providerPorts = new Object2IntOpenHashMap<>();
    private long membershipVersion;

    // Addon overwrites can still check the public timestamp maps without calling *Expired.
    private static final long PORT_SUBSCRIPTION = Long.MAX_VALUE;

    public NodeNet() {
    }

    /// SUBSCRIBER HANDLING ///
    public boolean isSubscribed(R receiver) { return this.receiverEntries.containsKey(receiver); }
    public void addReceiver(R receiver) { this.receiverEntries.put(receiver, receiverPorts.containsKey(receiver) ? PORT_SUBSCRIPTION : System.currentTimeMillis()); }
    public void removeReceiver(R receiver) { this.receiverEntries.removeLong(receiver); }

    /// PROVIDER HANDLING ///
    public boolean isProvider(P provider) { return this.providerEntries.containsKey(provider); }
    public void addProvider(P provider) { this.providerEntries.put(provider, providerPorts.containsKey(provider) ? PORT_SUBSCRIPTION : System.currentTimeMillis()); }
    public void removeProvider(P provider) { this.providerEntries.removeLong(provider); }

    /** Port subscriptions live until explicitly detached; legacy subscriptions still time out. */
    public void attachReceiverPort(R receiver) {
        receiverPorts.addTo(receiver, 1);
        addReceiver(receiver);
    }

    public void detachReceiverPort(R receiver) {
        int count = receiverPorts.getInt(receiver);
        if (count > 1) receiverPorts.put(receiver, count - 1);
        else if (count == 1) {
            receiverPorts.removeInt(receiver);
            removeReceiver(receiver);
        }
    }

    public void attachProviderPort(P provider) {
        providerPorts.addTo(provider, 1);
        addProvider(provider);
    }

    public void detachProviderPort(P provider) {
        int count = providerPorts.getInt(provider);
        if (count > 1) providerPorts.put(provider, count - 1);
        else if (count == 1) {
            providerPorts.removeInt(provider);
            removeProvider(provider);
        }
    }

    public boolean receiverExpired(R receiver, long lastSeen, long now, long timeout) {
        return isBadLink(receiver) || (!receiverPorts.containsKey(receiver) && now - lastSeen > timeout);
    }

    public boolean providerExpired(P provider, long lastSeen, long now, long timeout) {
        return isBadLink(provider) || (!providerPorts.containsKey(provider) && now - lastSeen > timeout);
    }

    public long getMembershipVersion() { return membershipVersion; }

    /** A split can keep the same network object, so ports also observe this revision. */
    public void clearMemberships() {
        receiverEntries.clear();
        providerEntries.clear();
        receiverPorts.clear();
        providerPorts.clear();
        membershipVersion++;
    }

    /** Combines two networks into one */
    public void joinNetworks(NodeNet<R, P, L, N> network) {
        if (network == this || !network.isValid()) return;

        for (L conductor : network.links) forceJoinLink(conductor);
        network.links.clear();

        for (R connector : network.receiverEntries.keySet()) this.addReceiver(connector);
        for (P connector : network.providerEntries.keySet()) this.addProvider(connector);
        network.destroy();
    }

    /** Adds the node as part of this network's links */
    public NodeNet<R, P, L, N> joinLink(L node) {
        if (node.net != null) node.net.leaveLink(node);
        return forceJoinLink(node);
    }

    /** Adds the node as part of this network's links, skips the part about removing it from existing networks */
    public NodeNet<R, P, L, N> forceJoinLink(L node) {
        this.links.add(node);
        // noinspection unchecked
        node.setNet((N) this);
        return this;
    }

    /** Removes the specified node */
    public void leaveLink(L node) {
        node.setNet(null);
        this.links.remove(node);
        if (this.links.isEmpty()) {
            this.destroy(); // An empty network is invalid
        }
    }

    /// GENERAL POWER NET CONTROL ///
    public void invalidate() {
        if (!this.valid) return;
        this.valid = false;
        UniNodespace.removeActiveNet(this);
    }

    public boolean isValid() { return this.valid; }
    public void resetTrackers() { }
    public abstract void update();

    public void destroy() {
        this.invalidate();
        for (L link : this.links) {
            link.setNet(null);
        }
        this.links.clear();
        this.clearMemberships();
    }

    public static boolean isBadLink(Object o) {
        if (o instanceof ILoadedTile && !((ILoadedTile) o).isLoaded()) return true;
        return o instanceof TileEntity && ((TileEntity) o).isInvalid();
    }
}
