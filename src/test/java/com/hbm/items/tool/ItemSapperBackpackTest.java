package com.hbm.items.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSapperBackpackTest {

    @Test
    void smallBlastsReceiveNinetyFivePercentProtection() {
        assertEquals(0.05F, ItemSapperBackpack.getExplosionMultiplier(10F), 0.0001F);
        assertEquals(0.05F, ItemSapperBackpack.getExplosionMultiplier(60F), 0.0001F);
    }

    @Test
    void protectionFallsContinuouslyUntilExtremeBlastsPassThrough() {
        float medium = ItemSapperBackpack.getExplosionMultiplier(110F);
        float heavy = ItemSapperBackpack.getExplosionMultiplier(230F);

        assertTrue(medium > 0.05F && medium < 0.75F);
        assertTrue(heavy > 0.75F && heavy < 1F);
        assertEquals(1F, ItemSapperBackpack.getExplosionMultiplier(300F), 0.0001F);
        assertEquals(1F, ItemSapperBackpack.getExplosionMultiplier(10_000F), 0.0001F);
    }
}
