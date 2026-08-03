package com.hbm.tileentity.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PneumaticStorageNbtTest {

    @Test
    void bulkStorageValidatesCorruptAmountArrays() {
        assertArrayEquals(new int[] { 0, 50_000, TileEntityPneumoStorageMono.CAPACITY },
                TileEntityPneumoStorageMono.sanitizeAmounts(new int[] { -10, 50_000, Integer.MAX_VALUE, 123 }));
        assertArrayEquals(new int[] { 25, 0, 0 }, TileEntityPneumoStorageMono.sanitizeAmounts(new int[] { 25 }));
    }

    @Test
    void exporterValidatesCorruptArraysAndModes() {
        assertEquals(TileEntityPneumoStorageExporter.MODE_FULL_REQUEST,
                TileEntityPneumoStorageExporter.normalizeRequestMode(99));
        assertEquals(TileEntityPneumoStorageExporter.MODE_AS_MUCH_AS_POSSIBLE,
                TileEntityPneumoStorageExporter.normalizeRequestMode(-4));
        assertArrayEquals(new int[] { 0, 4, 0, 0, 0, 0, 0, 0, 0 },
                TileEntityPneumoStorageExporter.normalizeSlotDelays(new int[] { -20, 4 }));
    }
}
