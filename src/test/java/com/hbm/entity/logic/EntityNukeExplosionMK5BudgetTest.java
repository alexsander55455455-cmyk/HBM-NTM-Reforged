package com.hbm.entity.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityNukeExplosionMK5BudgetTest {

    @Test
    void craterUsesOnlyTimeLeftAfterEntityProcessing() {
        assertEquals(30, EntityNukeExplosionMK5.remainingExplosionBudgetMs(40, 0, 10_000_000));
        assertEquals(29, EntityNukeExplosionMK5.remainingExplosionBudgetMs(40, 0, 10_000_001));
        assertEquals(0, EntityNukeExplosionMK5.remainingExplosionBudgetMs(40, 0, 50_000_000));
        assertEquals(0, EntityNukeExplosionMK5.remainingExplosionBudgetMs(0, 0, 0));
    }
}
