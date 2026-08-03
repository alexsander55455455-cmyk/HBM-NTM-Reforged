package com.hbm.hazard.type;

import com.hbm.capability.HbmLivingCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazardTypeCoalTest {

    private static final double COAL_DUST_LEVEL = 3D;
    private static final int DEFAULT_HAZARD_INTERVAL = 5;
    private static final double NATURAL_RECOVERY_PER_SECOND = 20D;

    @Test
    void doseGrowsNonlinearlyAndMeetsTargetTimes() {
        double dose1 = HazardTypeCoal.calculateBlackLungDose(COAL_DUST_LEVEL, 1, DEFAULT_HAZARD_INTERVAL);
        double dose16 = HazardTypeCoal.calculateBlackLungDose(COAL_DUST_LEVEL, 16, DEFAULT_HAZARD_INTERVAL);
        double dose64 = HazardTypeCoal.calculateBlackLungDose(COAL_DUST_LEVEL, 64, DEFAULT_HAZARD_INTERVAL);

        assertEquals(8D, dose1, 0.000001D);
        assertEquals(14D, dose16, 0.000001D);
        assertEquals(22D, dose64, 0.000001D);
        assertTrue(dose1 < dose16);
        assertTrue(dose16 < dose64);

        assertMinutesToFirstSymptoms(dose1, 20D, 25D);
        assertMinutesToFirstSymptoms(dose16, 8D, 12D);
        assertMinutesToFirstSymptoms(dose64, 4D, 6D);
    }

    @Test
    void configuredHazardIntervalDoesNotChangeExpectedDosePerSecond() {
        double doseAtFiveTicks = HazardTypeCoal.calculateBlackLungDose(COAL_DUST_LEVEL, 16, 5);
        double doseAtTenTicks = HazardTypeCoal.calculateBlackLungDose(COAL_DUST_LEVEL, 16, 10);

        assertEquals(doseAtFiveTicks * (20D / 5D), doseAtTenTicks * (20D / 10D), 0.000001D);
    }

    private static void assertMinutesToFirstSymptoms(double dosePerApplication, double minimumMinutes, double maximumMinutes) {
        double applicationsPerSecond = 20D / DEFAULT_HAZARD_INTERVAL;
        double netDosePerSecond = dosePerApplication * applicationsPerSecond - NATURAL_RECOVERY_PER_SECOND;
        double symptomThreshold = HbmLivingCapability.EntityHbmProps.maxBlacklung * 0.25D;
        double minutes = symptomThreshold / netDosePerSecond / 60D;

        assertTrue(minutes >= minimumMinutes && minutes <= maximumMinutes,
                "Expected " + minimumMinutes + "-" + maximumMinutes + " minutes, got " + minutes);
    }
}
