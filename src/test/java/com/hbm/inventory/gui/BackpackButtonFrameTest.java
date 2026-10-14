package com.hbm.inventory.gui;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BackpackButtonFrameTest {
    @Test void narrowWindowsFitTheWholeWorkbenchWithoutChangingTheStorageGrid() {
        for (int displayWidth : new int[]{1280, 1366, 1384, 1638, 1888, 1920}) {
            for (int scale = 1; scale <= 4; scale++) {
                for (int width : new int[]{178, 214, 250}) {
                    int needed = width + 106 + 8;
                    int fitted = GUIBackpack.guiScaleForWidth(displayWidth, scale, needed);
                    assertTrue(fitted >= 1 && fitted <= scale);
                    assertTrue(displayWidth / fitted >= needed);
                    if (fitted < scale) assertTrue(displayWidth / (fitted + 1) < needed);
                }
            }
        }
    }

    @Test void openDrawersAndWorkbenchAreIncludedInHorizontalCentering() {
        for (int screen : new int[]{346, 408, 480, 640}) {
            for (int width : new int[]{178, 214, 250}) {
                for (int extension : new int[]{0, 20, 46, 106}) {
                    if (width + extension > screen) continue;
                    int left = GUIBackpack.centeredPanelLeft(screen, width, extension);
                    int groupLeft = left - extension;
                    int rightMargin = screen - left - width;
                    assertTrue(groupLeft >= 0);
                    assertTrue(rightMargin >= 0);
                    assertTrue(Math.abs(groupLeft - rightMargin) <= 1,
                            "Center the complete interface so the side panel does not leave the window");
                }
            }
        }
    }

    @Test void compactButtonsKeepAllFourEdgesAndCoverEveryClickablePixel() {
        for (int width : new int[]{18, 20, 39, 80, 83, 116}) {
            for (int height : new int[]{12, 14, 20}) {
                for (int state = 0; state < 3; state++) {
                    Recorder button = new Recorder(width, height);
                    button.drawBackground(state);
                    int[][] coverage = new int[height][width];
                    boolean bottomBorder = false;
                    for (int[] tile : button.tiles) {
                        for (int y = tile[1]; y < tile[1] + tile[5]; y++) {
                            for (int x = tile[0]; x < tile[0] + tile[4]; x++) coverage[y][x]++;
                        }
                        if (tile[1] + tile[5] == height) {
                            assertEquals(46 + state * 20 + 20, tile[3] + tile[5],
                                    "The last texture row is the bottom border, not the truncated middle");
                            bottomBorder = true;
                        }
                    }
                    assertTrue(bottomBorder);
                    for (int[] row : coverage) for (int pixel : row) assertEquals(1, pixel);
                    assertEquals(0, button.tiles.get(0)[2]);
                    int[] right = button.tiles.get(1);
                    assertEquals(200, right[2] + right[4]);
                }
            }
        }
    }

    private static class Recorder extends BackpackButton {
        final List<int[]> tiles = new ArrayList<>();
        Recorder(int width, int height) { super(0, 0, 0, width, height, "test"); }
        @Override public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
            tiles.add(new int[]{x, y, u, v, width, height});
        }
    }
}
