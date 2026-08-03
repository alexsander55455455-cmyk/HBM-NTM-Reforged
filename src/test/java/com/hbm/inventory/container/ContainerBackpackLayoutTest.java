package com.hbm.inventory.container;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerBackpackLayoutTest {

    @Test
    void everyRebalancedCapacityEndsOnACompleteRow() {
        Map<Integer, Integer> capacitiesAndColumns = new LinkedHashMap<>();
        capacitiesAndColumns.put(36, 9);
        capacitiesAndColumns.put(45, 9);
        capacitiesAndColumns.put(54, 9);
        capacitiesAndColumns.put(66, 11);
        capacitiesAndColumns.put(77, 11);
        capacitiesAndColumns.put(88, 11);
        capacitiesAndColumns.put(130, 13);
        capacitiesAndColumns.put(156, 13);
        capacitiesAndColumns.put(182, 13);

        for (Map.Entry<Integer, Integer> entry : capacitiesAndColumns.entrySet()) {
            int capacity = entry.getKey();
            int columns = ContainerBackpack.columnsForCapacity(capacity);
            assertEquals(entry.getValue().intValue(), columns, "Unexpected columns for " + capacity);
            assertEquals(0, capacity % columns, "Incomplete final row for " + capacity);
        }
    }

    @Test
    void everyCapacityTierAddsOnlyCompleteRowsForEveryGridWidth() {
        int[] baseCapacities = {36, 66, 130};
        for (int baseCapacity : baseCapacities) {
            int columns = ContainerBackpack.columnsForCapacity(baseCapacity);
            for (int tier = 1; tier <= 3; tier++) {
                int expandedCapacity = baseCapacity + columns * tier;
                assertEquals(0, expandedCapacity % columns,
                        "Tier " + tier + " left an incomplete row for " + baseCapacity);
            }
        }
    }

    @Test
    void controlRowsCompactWithoutTouchingSearchOrStorage() {
        assertEquals(61, ContainerBackpack.contentYForControls(false, false, true, false));
        assertEquals(65, ContainerBackpack.contentYForControls(true, false, true, false));
        assertEquals(87, ContainerBackpack.contentYForControls(true, true, true, false));
        assertEquals(87, ContainerBackpack.contentYForControls(false, false, true, true));
        assertEquals(4, ContainerBackpack.actionButtonY(false, false, false));
        assertEquals(26, ContainerBackpack.autoSortButtonY(true));
        assertEquals(48, ContainerBackpack.actionButtonY(true, true, true));
    }

    @Test
    void workbenchGridFitsLeftPanelAndArrowFitsUpgradeDrawer() {
        int gridLeft = ContainerBackpack.WORKBENCH_PANEL_LEFT + 11;
        int gridRight = gridLeft + 3 * 18;
        int resultLeft = ContainerBackpack.WORKBENCH_PANEL_LEFT + 29;
        int arrowLeft = ContainerBackpack.UPGRADE_DRAWER_SLOT_X - 1;

        assertTrue(gridLeft > ContainerBackpack.WORKBENCH_PANEL_LEFT);
        assertTrue(gridRight < ContainerBackpack.WORKBENCH_PANEL_RIGHT);
        assertTrue(resultLeft + 18 < ContainerBackpack.WORKBENCH_PANEL_RIGHT);
        assertTrue(arrowLeft >= -ContainerBackpack.UPGRADE_DRAWER_WIDTH);
        assertTrue(arrowLeft + ContainerBackpack.WORKBENCH_ARROW_WIDTH <= 0);
        assertTrue(ContainerBackpack.WORKBENCH_ARROW_HEIGHT < 18);
        int topPadding = ContainerBackpack.WORKBENCH_GRID_TOP - ContainerBackpack.WORKBENCH_PANEL_TOP;
        assertEquals(ContainerBackpack.WORKBENCH_RESULT_TOP + ContainerBackpack.WORKBENCH_SLOT_SIZE
                        + topPadding,
                ContainerBackpack.WORKBENCH_PANEL_BOTTOM);
        assertEquals(122, ContainerBackpack.WORKBENCH_PANEL_BOTTOM);
    }
}
