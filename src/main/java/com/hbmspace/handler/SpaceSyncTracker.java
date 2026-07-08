package com.hbmspace.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpaceSyncTracker {

    public static final int KEEPALIVE_TICKS = 30;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private SpaceSyncTracker() {
    }

    public static State getOrCreate(UUID playerId) {
        return STATES.computeIfAbsent(playerId, id -> new State());
    }

    public static void remove(UUID playerId) {
        STATES.remove(playerId);
    }

    public static final class State {
        public int lastOxy;
        public boolean lastGravity;
        public boolean lastWarped;
        public int ticksSinceSync;
    }
}