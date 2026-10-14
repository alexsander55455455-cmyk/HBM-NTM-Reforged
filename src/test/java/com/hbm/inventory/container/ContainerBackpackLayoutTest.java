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
        for (int features = 0; features < 16; features++) {
            boolean pickup = (features & 1) != 0;
            boolean sort = (features & 2) != 0;
            boolean manual = (features & 4) != 0;
            boolean smuggler = (features & 8) != 0;
            int contentY = ContainerBackpack.contentYForControls(pickup, sort, manual, smuggler);
            int searchY = contentY - 16;
            assertTrue(searchY >= 30, "The header must be clear of search");
            if (sort) assertTrue(ContainerBackpack.autoSortButtonY(pickup) >= 30);
            if (manual || smuggler) {
                int actionY = ContainerBackpack.actionButtonY(pickup, sort, smuggler);
                assertTrue(actionY >= 30, "Controls must not cover the title or filled count");
                assertTrue(actionY + 12 < searchY, "Controls must not cover search");
            }
            assertTrue(contentY + 4 * 18 + 8 + 4 + 94 <= 270,
                    "A fully upgraded backpack must fit at 1080p GUI scale 4");
        }
        assertEquals(61, ContainerBackpack.contentYForControls(false, false, true, false));
        assertEquals(75, ContainerBackpack.contentYForControls(true, true, true, false));
        assertEquals(30, ContainerBackpack.autoSortButtonY(true));
        assertEquals(44, ContainerBackpack.actionButtonY(true, true, true));
    }

    @Test
    void everyControlCombinationFitsTheActualScaledWindowWithAMargin() {
        for (int features = 0; features < 16; features++) {
            int contentY = ContainerBackpack.contentYForControls((features & 1) != 0,
                    (features & 2) != 0, (features & 4) != 0, (features & 8) != 0);
            int nonStorageHeight = contentY + 8 + 4 + 94;
            for (int windowHeight : new int[]{240, 250, 252, 260, 270, 360, 540}) {
                int rows = ContainerBackpack.viewportRowsForHeight(windowHeight, nonStorageHeight);
                assertTrue(rows >= 1 && rows <= ContainerBackpack.MAX_VISIBLE_ROWS);
                assertTrue(nonStorageHeight + rows * 18 <= windowHeight - 8,
                        "The inventory and hotbar must fit, including upgrades, with an eight-pixel margin");
            }
        }
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
