package com.hbm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaminationUtilShockwaveTest {

    @Test
    void entityReceivesBlastOnlyWhenFrontCrossesItsDistance() {
        assertFalse(ContaminationUtil.isInsideExpandingShockFront(9.9, 10, 12));
        assertFalse(ContaminationUtil.isInsideExpandingShockFront(10, 10, 12));
        assertTrue(ContaminationUtil.isInsideExpandingShockFront(10.1, 10, 12));
        assertTrue(ContaminationUtil.isInsideExpandingShockFront(12, 10, 12));
        assertFalse(ContaminationUtil.isInsideExpandingShockFront(12.1, 10, 12));
    }
}
