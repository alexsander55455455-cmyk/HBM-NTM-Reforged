package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPoolsSatellite;

public class SatelliteLunarMiner extends SatelliteMiner {

    static {
        registerCargo(SatelliteLunarMiner.class, ItemPoolsSatellite.POOL_SAT_LUNAR);
    }

    @Override
    public float[] getColor() {
        return new float[] { 0.42F, 0.54F, 0.82F };
    }
}
