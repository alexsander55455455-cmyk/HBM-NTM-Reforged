package com.hbm.inventory.gui.element;

import com.hbm.render.util.GaugeUtil;

/** Compatibility facade for NTM CE addons. */
public final class GUIElements {

    private GUIElements() {
    }

    public static void drawSmoothGauge(int x, int y, double z, double progress, double tipLength, double backLength, double backSide, int color) {
        GaugeUtil.drawSmoothGauge(x, y, z, progress, tipLength, backLength, backSide, color);
    }
}
